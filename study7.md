# Study 7 — 로그인/회원가입 프론트엔드 & 쪽지(메시지) 기능

요청하신 순서대로 두 단계로 진행합니다.

1. **먼저**: 로그인/회원가입을 실제로 브라우저에서 할 수 있는 프론트엔드를 만든다 (study4의 JWT API를 프론트엔드에서 처음 사용)
2. **다음**: 로그인한 사용자끼리 쪽지를 주고받는 메시지 기능을 백엔드+프론트엔드로 만든다

프론트엔드 파일은 `demo/src/main/resources/static/StudyHtmlFile2.html` 입니다.

---

## 1단계 — 로그인/회원가입 프론트엔드

### 왜 로그인 상태를 프론트엔드에서 직접 관리해야 할까?

study4에서 만든 JWT 방식은 **Stateless**(서버가 로그인 상태를 기억하지 않음)입니다. 그 말은, 로그인 성공 후 서버가 내려준 토큰을 **클라이언트(브라우저)가 스스로 보관**하고 있다가, 이후 요청마다 매번 실어 보내야 한다는 뜻입니다. 이번 study에서 그 흐름을 처음으로 실제 브라우저에서 구현합니다.

```
1) 회원가입 폼 제출 → POST /auth/signup
2) 로그인 폼 제출   → POST /auth/login → 응답으로 token 받음
3) 받은 token을 localStorage에 저장
4) 이후 모든 요청에 Authorization: Bearer <token> 헤더를 자동으로 붙임
5) 로그아웃 → localStorage에서 token 삭제
```

### localStorage — 브라우저에 값을 저장하는 공간

`localStorage`는 브라우저가 제공하는 간단한 키-값 저장소로, 페이지를 새로고침하거나 탭을 닫았다 열어도 값이 유지됩니다(로그아웃하거나 직접 지우기 전까지). 그래서 로그인 토큰을 저장해두기에 적합합니다.

```javascript
localStorage.setItem("token", "eyJhbGciOi...");
localStorage.getItem("token");     // 값 읽기
localStorage.removeItem("token");  // 값 삭제(로그아웃)
```

> ⚠️ 실무에서는 `localStorage`에 토큰을 저장하는 방식이 XSS 공격에 취약할 수 있어 `httpOnly` 쿠키 등 다른 방식을 쓰기도 합니다. 이번 study는 개념 학습이 목적이라 가장 단순한 `localStorage` 방식을 사용합니다.

### 로그인 상태에 따라 화면 바꾸기

```javascript
function refreshAuthUi() {
    const token = getToken();
    const username = getUsername();

    if (token && username) {
        // 로그인 됨: 로그인/회원가입 폼 숨기고, 로그인 정보 + 쪽지 기능 보여주기
        document.getElementById("loggedInBar").style.display = "flex";
        document.getElementById("authForms").style.display = "none";
        document.getElementById("meUsername").textContent = username;
        document.getElementById("messageSections").style.display = "block";
    } else {
        // 로그인 안 됨: 반대로
        document.getElementById("loggedInBar").style.display = "none";
        document.getElementById("authForms").style.display = "grid";
        document.getElementById("messageSections").style.display = "none";
    }
}
```
- 로그인에 성공하면 `saveSession(token, username)`을 호출해 `localStorage`에 저장하고 `refreshAuthUi()`로 화면을 갱신합니다.
- 로그인 응답(`/auth/login`)에는 토큰만 들어있고 사용자 이름은 없으므로, **로그인 폼에 입력했던 아이디를 그대로 함께 저장**해서 화면에 "OO님으로 로그인됨"을 표시합니다.

### 인증이 필요한 요청을 위한 공통 함수
```javascript
async function api(path, options = {}, auth = false) {
    const headers = Object.assign({}, options.headers || {});
    if (auth) {
        const token = getToken();
        if (!token) throw new Error("로그인이 필요합니다.");
        headers["Authorization"] = "Bearer " + token;
    }

    const response = await fetch(path, Object.assign({}, options, { headers }));

    if (response.status === 403 && auth) {
        clearSession();
        refreshAuthUi();
        throw new Error("로그인이 만료되었거나 유효하지 않습니다. 다시 로그인해주세요.");
    }
    // ...이하 study6과 동일하게 JSON 파싱 및 에러 처리
}
```
- 세 번째 인자 `auth`가 `true`인 API(쪽지 관련)는 자동으로 `Authorization` 헤더를 붙입니다.
- 만약 인증이 필요한 요청인데 **403이 돌아오면** (토큰이 없거나 만료됨) 저장된 세션을 지우고 로그인 화면으로 돌려보냅니다. 이러면 토큰이 만료됐을 때 사용자가 "왜 안 되지?"하고 헤매지 않고 자연스럽게 다시 로그인하게 됩니다.

---

## 2단계 — 쪽지(메시지) 기능

### 백엔드: 연관관계 매핑을 처음 사용

지금까지의 엔티티는 다른 엔티티를 참조하지 않는 단순한 구조였습니다. 쪽지는 "누가(발신자) 누구에게(수신자) 보냈는가"를 표현해야 하므로, 처음으로 **엔티티 간 연관관계(`@ManyToOne`)**를 사용합니다.

```java
// domain/Message.java
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    // 생성자/getter 생략
}
```

| 요소 | 설명 |
|---|---|
| `@ManyToOne` | "여러 개의 `Message`가 하나의 `User`를 가리킬 수 있다"는 관계 (한 사람이 여러 쪽지를 보내거나 받을 수 있으니까) |
| `@JoinColumn(name = "sender_id")` | 실제 DB 테이블에 만들어지는 외래 키(FK) 컬럼 이름 |
| `fetch = FetchType.LAZY` | `Message`를 조회할 때 연결된 `User`를 **즉시 함께 조회하지 않고**, `getSender()`처럼 실제로 접근하는 시점에 조회함 (불필요한 조회를 줄이는 JPA의 성능 최적화 기본값 권장 설정) |

### Repository — 파생 쿼리와 직접 쓰는 JPQL

```java
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByReceiver_UsernameOrderByCreatedAtDesc(String receiverUsername);

    List<Message> findBySender_UsernameOrderByCreatedAtDesc(String senderUsername);

    @Query("""
            select m from Message m
            where (m.sender.username = :a and m.receiver.username = :b)
               or (m.sender.username = :b and m.receiver.username = :a)
            order by m.createdAt asc
            """)
    List<Message> findConversation(@Param("a") String usernameA, @Param("b") String usernameB);
}
```
- `findByReceiver_UsernameOrderByCreatedAtDesc` — 메서드 이름의 `Receiver_Username`은 "`Message`의 `receiver` 필드(연관관계) 안에 있는 `username` 필드"를 의미합니다. 언더스코어(`_`)로 연관관계를 넘어가는 프로퍼티임을 명시했습니다.
- `findConversation`은 조건이 "(내가 보내고 상대가 받았거나) 또는 (상대가 보내고 내가 받았거나)"처럼 메서드 이름만으로 표현하기 복잡해서, `@Query`로 **JPQL(Java Persistence Query Language)**을 직접 작성했습니다. SQL과 비슷하지만 테이블이 아니라 **엔티티/필드 이름**을 사용하는 것이 특징입니다.

### Service / Controller — 로그인한 사용자를 자동으로 발신자로

```java
@RestController
@RequestMapping("/messages")
public class MessageController {

    @PostMapping
    public MessageResponse send(@AuthenticationPrincipal UserDetails me, @Valid @RequestBody MessageRequest request) {
        Message saved = messageService.send(me.getUsername(), request.receiverUsername(), request.content());
        return MessageResponse.from(saved);
    }

    @GetMapping("/inbox")
    public List<MessageResponse> inbox(@AuthenticationPrincipal UserDetails me) { ... }

    @GetMapping("/sent")
    public List<MessageResponse> sent(@AuthenticationPrincipal UserDetails me) { ... }

    @GetMapping("/conversation/{username}")
    public List<MessageResponse> conversation(@AuthenticationPrincipal UserDetails me, @PathVariable String username) { ... }
}
```

| 요소 | 설명 |
|---|---|
| `@AuthenticationPrincipal UserDetails me` | 현재 요청을 보낸 **로그인된 사용자 정보**를 바로 주입받음. study4의 `JwtAuthenticationFilter`가 토큰을 검증하고 `SecurityContextHolder`에 등록해둔 `UserDetails`가 여기로 들어옴 |
| 발신자를 요청 본문이 아니라 `me`에서 가져옴 | 클라이언트가 "나는 철수야"라고 요청 본문에 적어 보내는 걸 그대로 믿으면, 다른 사람 이름으로 쪽지를 보낼 수 있는 보안 구멍이 생깁니다. **발신자는 반드시 토큰에서 검증된 값을 사용**해야 합니다 |

`/messages/**` 경로는 `SecurityConfig`의 `permitAll` 목록에 추가하지 **않았습니다** — 즉 기본 규칙인 `anyRequest().authenticated()`가 적용되어, 로그인(토큰) 없이는 접근할 수 없습니다. study6의 게시판(`/posts/**`)은 공개였지만, 쪽지는 원래 개인적인 기능이므로 이번엔 실제로 인증이 필요하게 만들었습니다.

### 받는 사람이 존재하지 않으면?
```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super("사용자를 찾을 수 없습니다: " + username);
    }
}
```
`GlobalExceptionHandler`에 핸들러를 추가해서 404로 응답하도록 했습니다 (study3부터 이어온 예외 처리 패턴).

---

## 3. 실행 & 실제로 검증한 내용

```powershell
cd demo
.\gradlew.bat bootRun
```

이번 실습 환경에서 curl로 **API 레벨을 실제로 검증**했습니다 (한글 본문은 study6에서 배운 대로 파일로 저장해서 `--data-binary`로 전송).

```bash
curl -X POST /auth/signup -d '{"username":"chulsoo","password":"1234"}'   # 201
curl -X POST /auth/signup -d '{"username":"younghee","password":"1234"}'  # 201

TOKEN_A=$(로그인 후 chulsoo의 토큰)
TOKEN_B=$(로그인 후 younghee의 토큰)

curl /messages/inbox                                                       # 토큰 없이 → 403

curl -X POST /messages -H "Authorization: Bearer $TOKEN_A" \
     -d '{"receiverUsername":"younghee","content":"안녕 영희야, 잘 지내?"}'
# → 200 {"id":1,"senderUsername":"chulsoo","receiverUsername":"younghee",...}

curl -X POST /messages -H "Authorization: Bearer $TOKEN_B" \
     -d '{"receiverUsername":"chulsoo","content":"응 잘 지내! 너는?"}'
# → 200 {"id":2,"senderUsername":"younghee","receiverUsername":"chulsoo",...}

curl /messages/inbox  -H "Authorization: Bearer $TOKEN_B"   # younghee가 받은 쪽지 → chulsoo의 메시지 1건
curl /messages/sent   -H "Authorization: Bearer $TOKEN_A"   # chulsoo가 보낸 쪽지 → 1건
curl /messages/conversation/younghee -H "Authorization: Bearer $TOKEN_A"
# → 두 메시지가 시간순(주고받은 순서)으로 모두 조회됨

curl -X POST /messages -H "Authorization: Bearer $TOKEN_A" \
     -d '{"receiverUsername":"no_such_user","content":"테스트"}'
# → 404 {"message":"사용자를 찾을 수 없습니다: no_such_user"}
```

모두 의도한 대로 동작했습니다.

브라우저에서는 `http://localhost:8080/StudyHtmlFile2.html`을 열어 회원가입 → 로그인 → 쪽지 보내기 → 받은/보낸 쪽지함 확인 → 특정 상대와의 대화 보기까지 직접 눌러보며 확인해보세요. (버튼 클릭 등 실제 화면 조작은 제가 직접 볼 수 없어 코드 작성과 API 레벨 검증까지 진행했습니다. 화면에서 이상 동작이 보이면 알려주세요.)

---

## 4. 테스트 코드

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MessageControllerTest {
    // requestWithoutTokenIsRejected()                     — 토큰 없이 /messages/inbox 접근 시 403
    // sendMessageAppearsInReceiverInboxAndSenderSent()     — 보낸 쪽지가 상대 받은쪽지함/내 보낸쪽지함에 나타나는지
    // conversationReturnsMessagesInBothDirectionsChronologically() — 대화 보기가 시간순으로 양방향 메시지를 보여주는지
    // sendToNonExistentUserFails()                         — 존재하지 않는 사용자에게 보내면 404
}
```

`./gradlew test` 실행 결과, study7에서 추가된 4개를 포함해 **총 20개 테스트 전부 통과**했습니다.

| 테스트 클래스 | 개수 |
|---|---|
| DemoApplicationTests | 1 |
| HelloControllerTest | 3 |
| AuthControllerTest | 5 |
| MemberControllerTest | 3 |
| PostControllerTest | 4 |
| **MessageControllerTest (신규)** | **4** |

---

## 5. 정리

- JWT 토큰을 `localStorage`에 저장하고, 로그인 상태에 따라 화면을 전환하는 프론트엔드 패턴을 처음으로 구현했다
- 인증이 필요한 요청에 자동으로 `Authorization` 헤더를 붙이고, 403이 오면 세션을 정리하고 재로그인을 유도하는 공통 처리 로직을 만들었다
- `@ManyToOne` 연관관계 매핑으로 "누가 누구에게" 관계를 표현하는 `Message` 엔티티를 만들었다
- 파생 쿼리(`findByReceiver_Username...`)와 직접 작성한 JPQL(`@Query`)을 모두 사용해봤다
- `@AuthenticationPrincipal`로 클라이언트가 조작할 수 없는, 검증된 "로그인한 사용자" 정보를 컨트롤러에서 바로 사용하는 법을 익혔다
- study6의 공개 게시판과 달리, 이번엔 실제로 로그인이 필요한 기능을 만들어 JWT 인증이 실제로 쓰이는 이유를 체감했다

## 6. 다음 단계

**study8.md**에서는 `StudyHtmlFile3.html`과 함께 **날씨 페이지**를 만들어봅니다. 이번엔 우리 DB에 저장된 데이터가 아니라, **외부 공개 API를 서버가 대신 호출해서(REST 클라이언트) 클라이언트에 내려주는 패턴**(예: 외부 날씨 API 연동, API 키를 프론트엔드에 노출하지 않고 백엔드에서 감추는 이유)을 다룰 예정입니다.
