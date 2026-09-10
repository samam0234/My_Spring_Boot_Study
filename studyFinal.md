# Study Final — 조건부 접근 제어 & 개인 저장소 & 한국시간 주/야간 판별

지금까지 만든 기능들(로그인/JWT, 쪽지)을 조합해서 **조건에 따라 다르게 동작하는 화면**을 만드는 마지막 응용 실습입니다.

**요구 조건**
1. 로그인 시 → 쪽지 기능을 바로 사용 가능
2. 비로그인 시 → 로그인/회원가입을 요청
3. 로그인·회원가입 페이지는 **따로** 만들 것
4. localStorage처럼 쓰되, 실제로는 **DB에 저장**되는 저장 기능
5. 한국시간(KST) 기준으로 **오후 10시 이후는 야간, 오전 7시 이후는 주간**임을 알려주는 시간 기능

새로 만든 파일:
- `demo/src/main/resources/static/LoginHtmlFile.html` — 전용 로그인/회원가입 페이지
- `demo/src/main/resources/static/StudyHtmlFileFinal.html` — 조건부 쪽지 + 저장소 + 시간 기능을 모은 페이지

---

## 1. 시간 기능 — 한국시간 기준 주간/야간 판별

### 설계 포인트: `LocalDateTime.now()`를 직접 쓰지 않는 이유

바로 `ZonedDateTime.now(ZoneId.of("Asia/Seoul"))`을 서비스 코드 안에서 호출해도 동작은 합니다. 하지만 그렇게 하면 **"지금이 몇 시인지"를 테스트에서 마음대로 바꿀 수 없습니다.** 예를 들어 "22시 정각에는 야간으로 판정되는지"를 테스트하려면, 실제로 시스템 시계가 22시가 될 때까지 기다려야 하는 셈입니다.

그래서 **`java.time.Clock`을 의존성으로 주입**받는 구조로 설계했습니다.

```java
// config/ClockConfig.java
@Configuration
public class ClockConfig {
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
```
```java
// service/TimeService.java
@Service
public class TimeService {
    private static final int NIGHT_START_HOUR = 22; // 오후 10시부터
    private static final int NIGHT_END_HOUR = 7;    // 오전 7시 전까지

    private final Clock clock;

    public TimeService(Clock clock) {
        this.clock = clock;
    }

    public TimeStatusResponse getStatus() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        boolean night = isNight(now.getHour());
        String period = night ? "야간" : "주간";
        String message = night ? "지금은 야간입니다." : "지금은 주간입니다.";
        return new TimeStatusResponse(now.format(FORMATTER), period, night, message);
    }

    private boolean isNight(int hour) {
        return hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR;
    }
}
```
운영 코드는 실제 시스템 시계를 쓰는 `Clock` 빈을 주입받지만, **테스트 코드에서는 원하는 시각으로 "고정된" `Clock`을 직접 만들어서 넣어줄 수 있습니다.** (아래 5장 테스트 코드 참고)

### Controller (공개 API)
```java
@RestController
@RequestMapping("/time")
public class TimeController {
    @GetMapping("/status")
    public TimeStatusResponse status() {
        return timeService.getStatus();
    }
}
```
로그인 여부와 무관하게 누구나 볼 수 있어야 하므로 `SecurityConfig`의 `permitAll`에 `/time/**`를 추가했습니다.

### 실제로 확인한 결과 (이번 실습 환경, 밤 11시 36분경)
```bash
curl http://localhost:8080/time/status
```
```json
{"currentTime":"2026-09-10 23:36:59","period":"야간","night":true,"message":"지금은 야간입니다."}
```

---

## 2. 저장 기능 — "localStorage처럼 쓰지만 실제로는 DB에"

브라우저의 `localStorage.setItem(key, value)` / `getItem(key)`와 똑같은 감각으로 쓸 수 있되, **서버 DB에 저장되어 다른 기기에서 로그인해도 데이터가 그대로 보이는** 저장소 기능을 만들었습니다.

### 엔티티 — 사용자별 key-value 저장
```java
@Entity
@Table(name = "storage_items", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "item_key"}))
public class StorageItem {
    @Id @GeneratedValue Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id") User owner;
    @Column(name = "item_key") String itemKey;
    @Column(length = 4000) String itemValue;
    LocalDateTime updatedAt;

    public void updateValue(String itemValue) {
        this.itemValue = itemValue;
        this.updatedAt = LocalDateTime.now();
    }
}
```
`(owner_id, item_key)` 조합에 유니크 제약을 걸어서, **한 사용자는 같은 key를 중복해서 가질 수 없게** 했습니다 (localStorage가 같은 key에 `setItem`하면 덮어쓰는 것과 동일한 동작을 흉내내기 위함).

### Service — Upsert(있으면 수정, 없으면 생성) 패턴
```java
@Transactional
public StorageResponse save(String username, String key, String value) {
    Optional<StorageItem> existing = storageItemRepository.findByOwner_UsernameAndItemKey(username, key);

    StorageItem item;
    if (existing.isPresent()) {
        item = existing.get();
        item.updateValue(value);              // 있으면: 값만 갱신
    } else {
        User owner = userRepository.findByUsername(username)...;
        item = new StorageItem(owner, key, value);   // 없으면: 새로 생성
    }
    return StorageResponse.from(storageItemRepository.save(item));
}
```
"있으면 수정, 없으면 생성"을 한 번에 처리하는 것을 **Upsert**(Update + Insert)라고 부릅니다. `localStorage.setItem()`이 항상 "덮어쓰기"로 동작하는 것과 똑같은 사용감을 주기 위한 설계입니다. study9에서 배운 대로, DB에 값을 변경하는 로직이라 `@Transactional`을 붙였습니다.

### API
| 메서드 | 경로 | 설명 |
|---|---|---|
| `PUT` | `/storage/{key}` | 저장(upsert) — `localStorage.setItem(key, value)`에 대응 |
| `GET` | `/storage` | 내 모든 저장 항목 목록 |
| `GET` | `/storage/{key}` | 특정 key 조회 — `localStorage.getItem(key)`에 대응 |
| `DELETE` | `/storage/{key}` | 삭제 — `localStorage.removeItem(key)`에 대응 |

모두 로그인이 필요합니다 (개인 저장소이므로 `permitAll`에 넣지 않음).

### 실제로 확인한 결과
```bash
curl -X PUT /storage/memo1 -H "Authorization: Bearer $TOKEN" -d '{"value":"안녕하세요 저장 테스트입니다"}'
# → 200 {"key":"memo1","value":"안녕하세요 저장 테스트입니다","updatedAt":"..."}

curl -X PUT /storage/memo1 -H "Authorization: Bearer $TOKEN" -d '{"value":"수정된 값입니다"}'
# → 200 {"key":"memo1","value":"수정된 값입니다",...}   (같은 key라 새로 생기지 않고 값만 바뀜)

curl /storage -H "Authorization: Bearer $TOKEN"
# → [{"key":"memo1","value":"수정된 값입니다",...}]   (1개만 존재 — upsert 확인)

curl /storage/nonexistent -H "Authorization: Bearer $TOKEN"
# → 404 {"message":"저장된 항목을 찾을 수 없습니다: nonexistent"}

curl -X DELETE /storage/memo1 -H "Authorization: Bearer $TOKEN"   # → 204
curl /storage -H "Authorization: Bearer $TOKEN"                   # → []
```

---

## 3. 전용 로그인/회원가입 페이지 — `LoginHtmlFile.html`

지금까지는 `StudyHtmlFile2.html`(study7)에 로그인/회원가입과 쪽지 기능이 함께 있었습니다. 이번엔 **로그인/회원가입만 담당하는 페이지를 따로** 만들었습니다.

- 로그인 폼 / 회원가입 폼을 탭처럼 전환
- 로그인 성공 시 `localStorage`에 `token`, `username` 저장 (다른 모든 페이지와 공유되는 방식, study7과 동일)
- 이미 로그인되어 있으면 폼 대신 "허브로 이동" / "로그아웃" 화면을 보여줌

```javascript
document.getElementById("loginForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const response = await fetch("/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password })
    });
    const body = await response.json();
    if (!response.ok) throw new Error(body.message);

    localStorage.setItem("token", body.token);
    localStorage.setItem("username", username);
    // 이후 StudyHtmlFileFinal.html 등 다른 페이지에서 바로 로그인 상태로 인식됨
});
```

---

## 4. 조건부 화면 — `StudyHtmlFileFinal.html`

같은 페이지 안에서 **로그인 여부에 따라 보여주는 내용을 완전히 다르게** 만들었습니다.

```javascript
function refreshLoginBar() {
    const loggedIn = !!(getToken() && getUsername());

    // 로그인 상태 표시줄 갱신
    ...

    // 쪽지 섹션: 로그인 안 됐으면 잠금 안내, 됐으면 실제 기능
    document.getElementById("messageLocked").style.display = loggedIn ? "none" : "block";
    document.getElementById("messageUnlocked").style.display = loggedIn ? "block" : "none";

    // 저장소 섹션도 동일한 방식
    document.getElementById("storageLocked").style.display = loggedIn ? "none" : "block";
    document.getElementById("storageUnlocked").style.display = loggedIn ? "block" : "none";

    if (loggedIn) {
        loadInbox();     // 로그인 상태면 바로 받은 쪽지함을 불러옴
        loadStorage();   // 저장소 목록도 바로 불러옴
    }
}
```

| 로그인 안 함 | 로그인 함 |
|---|---|
| "로그인해야 쪽지를 보낼 수 있습니다" + `LoginHtmlFile.html`로 가는 링크 | 쪽지 작성 폼 + 받은 쪽지함이 곧바로 표시됨 |
| "로그인해야 저장소를 쓸 수 있습니다" + 로그인 링크 | 저장소 입력 폼 + 저장된 목록이 곧바로 표시됨 |

토큰이 만료되었거나 유효하지 않아 API가 `403`을 반환하면, 자동으로 로그아웃 처리하고 잠금 화면으로 되돌아가도록 만들었습니다 (study7·9에서 쓴 것과 같은 패턴).

### 시간(주/야간) 배너 — 로그인 여부와 무관하게 항상 표시
```javascript
async function loadTimeStatus() {
    const response = await fetch("/time/status");
    const status = await response.json();

    const banner = document.getElementById("timeBanner");
    banner.className = status.night ? "night" : "day";   // 배경색까지 낮/밤에 맞춰 전환
    document.getElementById("timeIcon").textContent = status.night ? "🌙" : "☀️";
    document.getElementById("timePeriod").textContent = status.message;
}

loadTimeStatus();
setInterval(loadTimeStatus, 30000);  // 30초마다 자동 갱신
```
로그인 여부와 무관하게 페이지 상단에 항상 표시되고, 30초마다 자동으로 다시 확인해서 시간이 바뀌면 배너 색(밝은 파랑 ↔ 짙은 남색)과 문구가 함께 바뀝니다.

---

## 5. 테스트 코드 — `Clock` 덕분에 가능해진 순수 단위 테스트

study5에서 "슬라이스 테스트 vs 통합 테스트"를 다뤘는데, 이번엔 그보다 더 가벼운 **스프링 컨텍스트조차 필요 없는 순수 단위 테스트**를 처음 작성했습니다.

```java
class TimeServiceTest {

    private TimeService serviceAt(int hour, int minute) {
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        ZonedDateTime fixedNow = ZonedDateTime.of(2026, 9, 10, hour, minute, 0, 0, seoul);
        Clock clock = Clock.fixed(fixedNow.toInstant(), seoul);
        return new TimeService(clock);   // ← new로 직접 생성, @SpringBootTest 없음
    }

    @Test void hour22IsNight()       { assertThat(serviceAt(22, 0).getStatus().night()).isTrue(); }
    @Test void hour7IsDay()          { assertThat(serviceAt(7, 0).getStatus().night()).isFalse(); }
    @Test void hour6_59IsStillNight(){ assertThat(serviceAt(6, 59).getStatus().night()).isTrue(); }
    @Test void hour21_59IsStillDay() { assertThat(serviceAt(21, 59).getStatus().night()).isFalse(); }
    @Test void midnightIsNight()     { assertThat(serviceAt(0, 0).getStatus().night()).isTrue(); }
    @Test void noonIsDay()           { assertThat(serviceAt(12, 0).getStatus().night()).isFalse(); }
}
```

`Clock.fixed(...)`로 "2026-09-10 22:00 KST"처럼 원하는 시각을 딱 고정해서 `TimeService`에 주입하면, **실제 시간과 무관하게 경계값(21:59는 주간, 22:00은 야간, 06:59는 야간, 07:00은 주간)을 정확히 검증**할 수 있습니다. `@SpringBootTest`가 없어서 DB도, 웹 서버도 뜨지 않고 순식간에 실행됩니다.

> 이게 바로 `LocalDateTime.now()`를 직접 호출하지 않고 `Clock`을 주입받게 설계한 이유입니다. "지금 시각"처럼 테스트마다 달라지는 값에 의존하는 로직은, 그 "지금"을 외부에서 주입 가능하게 만들어야 경계 조건을 안정적으로 테스트할 수 있습니다.

나머지 기능들은 기존 패턴대로 `@SpringBootTest` + `MockMvc` 통합 테스트로 작성했습니다 (`StorageControllerTest`: upsert/목록/단건조회/삭제, `TimeControllerTest`: 공개 API 응답 형식).

`./gradlew test` 실행 결과, studyFinal에서 추가된 11개(TimeServiceTest 6개 + StorageControllerTest 4개 + TimeControllerTest 1개)를 포함해 **총 47개 테스트 전부 통과**했습니다.

---

## 6. main.html 업데이트

`main.html` 허브에 새 페이지 2개를 카드로 추가했습니다.
- **로그인 / 회원가입** → `LoginHtmlFile.html`
- **종합 응용(Final)** → `StudyHtmlFileFinal.html`

허브 상단의 로그인 상태 표시줄도, 로그인 안 된 경우 이동하는 링크를 기존 `StudyHtmlFile2.html`에서 **`LoginHtmlFile.html`로 변경**했습니다 — 이제 로그인은 전용 페이지에서 하는 것이 "정석 경로"가 되었기 때문입니다.

---

## 7. 실행 & 최종 확인

```powershell
cd demo
.\gradlew.bat bootRun
```

브라우저에서 순서대로 확인해볼 수 있습니다.
1. `http://localhost:8080/main.html` — 허브에서 **로그인 / 회원가입** 카드 클릭
2. `LoginHtmlFile.html`에서 회원가입 → 로그인
3. 허브로 돌아가서 **종합 응용(Final)** 카드 클릭
4. `StudyHtmlFileFinal.html`에서 로그인 상태이므로 쪽지 작성/저장소 기능이 바로 보이는지 확인
5. 로그아웃 버튼을 눌러보고, 같은 페이지에서 쪽지/저장소 영역이 "로그인해야 합니다" 안내로 바뀌는지 확인
6. 상단 시간 배너가 현재 한국 시간 기준 주간/야간을 올바르게 보여주는지 확인

(브라우저 화면 조작은 제가 직접 볼 수 없어 API 레벨 검증과 코드 리뷰로 확인했습니다. 이상이 있으면 알려주세요.)

---

## 8. 정리

- `Clock`을 의존성으로 주입받는 설계로, "지금 시각"에 의존하는 로직도 안정적으로 단위 테스트할 수 있다는 것을 확인했다 — 스프링 컨텍스트 없이 순식간에 도는 첫 번째 테스트였다
- Upsert(있으면 수정, 없으면 생성) 패턴으로 `localStorage`와 비슷한 사용감의 서버 사이드 저장소를 구현했다
- 로그인/회원가입 로직을 별도 페이지(`LoginHtmlFile.html`)로 분리하고, 다른 모든 페이지가 같은 `localStorage` 세션을 공유하도록 설계했다
- 한 페이지 안에서 **로그인 여부에 따라 완전히 다른 UI**(잠금 안내 ↔ 실제 기능)를 보여주는 조건부 렌더링 패턴을 구현했다
- study0부터 여기까지 배운 개념(JPA, DTO, 예외 처리, JWT 인증, 연관관계, 테스트)을 모두 동원해서 하나의 완결된 기능을 만들었다

---

## 마치며

`study0.md`의 "Spring Boot가 뭔가요?"에서 출발해서, REST API·JPA·Spring Security·테스트 코드를 거쳐 게시판·쪽지·외부 API 연동·파일 업로드·실시간 알림·API 문서화까지, 그리고 이번 조건부 접근 제어와 개인 저장소까지 왔습니다. `main.html` 허브에서 그동안 만든 모든 기능을 한 화면에서 오갈 수 있습니다.

다음으로 더 해보고 싶은 것이 생기면 새로운 study로 이어가면 됩니다 — 배포(Docker), 역할 기반 권한(RBAC), 캐싱, CI/CD 등 후보는 study10.md 끝에 정리해두었습니다.
