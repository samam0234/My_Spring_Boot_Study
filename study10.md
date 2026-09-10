# Study 10 — 페이지네이션 / 파일 업로드 / 실시간 알림 / API 문서화 + 응용 활동(main.html)

이번 study는 지금까지처럼 새 도메인 하나를 배우는 게 아니라, **웹 서비스에서 흔히 필요해지는 인프라성 기능 4가지**를 기존 기능들 위에 얹는 실습입니다. 그리고 마지막으로 지금까지 만든 모든 페이지를 한 곳에서 열어볼 수 있는 **`main.html` 허브 페이지**를 만듭니다.

1. 페이지네이션 — 게시판(study6) 목록에 적용
2. 파일 업로드 — 새 기능
3. 실시간 알림(SSE) — 쪽지(study7)와 연동
4. API 문서화 — Springdoc(Swagger UI)
5. **응용 활동**: `main.html`로 study6~10 페이지를 한 곳에 모으기

---

## 1. 페이지네이션 — `Pageable` / `Page<T>`

### 왜 필요할까?
게시글이 수천 개가 되면, `GET /posts` 한 번에 전부 내려주는 건 느리고 비효율적입니다. **몇 개씩 나눠서(페이지 단위로)** 가져오는 게 페이지네이션입니다.

### Repository — 파생 쿼리에 `Pageable` 추가
```java
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
```
파라미터에 `Pageable`을 추가하고 반환 타입을 `List<Post>` 대신 `Page<Post>`로 바꾸기만 하면, Spring Data JPA가 자동으로 `LIMIT`/`OFFSET`이 포함된 쿼리와 전체 개수를 세는 쿼리를 만들어줍니다.

### 우리만의 페이지 응답 DTO
Spring Data의 `Page<T>`를 컨트롤러에서 그대로 반환할 수도 있지만, 이 프로젝트는 지금까지 항상 **우리가 직접 정의한 Response DTO**로 감싸왔으므로 일관성을 위해 페이지 응답도 직접 만듭니다.
```java
public record PageResponse<T>(
        List<T> content, int page, int size,
        long totalElements, int totalPages, boolean hasNext
) {
    public static <T, S> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.hasNext()
        );
    }
}
```

### Controller
```java
@GetMapping
public PageResponse<PostResponse> findAll(@PageableDefault(size = 5) Pageable pageable) {
    return PageResponse.from(postService.findAll(pageable), PostResponse::from);
}
```
- `@PageableDefault(size = 5)` — 클라이언트가 `page`/`size`를 안 보내면 기본값(1페이지, 5개씩)을 사용
- 클라이언트는 `GET /posts?page=1&size=5` 처럼 쿼리 파라미터로 원하는 페이지를 요청

### 실제로 확인한 결과
```bash
curl "http://localhost:8080/posts?size=3"
```
```json
{"content":[...3개...],"page":0,"size":3,"totalElements":7,"totalPages":3,"hasNext":true}
```

> ⚠️ **기존 API를 바꾸는 변경(breaking change)**: `GET /posts`의 응답이 배열(`[...]`)에서 객체(`{"content":[...], ...}`)로 바뀌었습니다. 그래서 study6에서 만든 `StudyHtmlFile.html`도 함께 수정했습니다 — 목록을 그릴 때 `result.content`를 쓰고, "이전/다음" 버튼과 페이지 표시를 추가했습니다. API를 변경할 때는 이걸 사용하는 프론트엔드/테스트 코드도 함께 챙겨야 한다는 걸 다시 확인했습니다 (기존 `PostControllerTest`도 `$[0]` → `$.content[0]`로 수정).

---

## 2. 파일 업로드

### 엔티티 — 업로드된 파일의 메타데이터를 DB에 기록
디스크에 파일만 저장하면 나중에 "원래 파일 이름이 뭐였는지", "누가 올렸는지" 같은 정보를 알 수 없습니다. 그래서 파일 자체는 디스크에, **메타데이터는 DB에** 나눠서 저장합니다.
```java
@Entity
@Table(name = "uploaded_files")
public class UploadedFile {
    @Id @GeneratedValue Long id;
    @Column(unique = true) String storedName;   // 디스크에 저장된 실제 이름 (UUID.확장자)
    String originalName;                        // 사용자가 올린 원래 파일 이름
    String contentType;
    long size;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "uploader_id") User uploader;
    LocalDateTime uploadedAt;
}
```

### Service — 저장, 그리고 두 가지 방어 로직
```java
@Service
public class FileService {

    public FileResponse upload(String uploaderUsername, MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("빈 파일은 업로드할 수 없습니다.");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "." + getExtension(originalName);

        Path target = uploadDir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        User uploader = userRepository.findByUsername(uploaderUsername)...;
        UploadedFile saved = uploadedFileRepository.save(
                new UploadedFile(storedName, originalName, file.getContentType(), file.getSize(), uploader));

        return FileResponse.from(saved);
    }

    public Resource loadAsResource(String storedName) {
        Path filePath = uploadDir.resolve(storedName).normalize();

        // 경로 조작(path traversal) 공격 방지
        if (!filePath.startsWith(uploadDir) || !Files.exists(filePath)) {
            throw new StoredFileNotFoundException(storedName);
        }
        return new FileSystemResource(filePath);
    }
}
```

| 방어 로직 | 설명 |
|---|---|
| **원래 파일명 대신 UUID로 저장** | 사용자가 올린 파일명을 그대로 쓰면 덮어쓰기 충돌이나, 파일명에 포함된 이상한 문자로 인한 문제가 생길 수 있음. 항상 새로운 무작위 이름으로 저장 |
| **`filePath.startsWith(uploadDir)` 체크** | `storedName`에 `../../etc/passwd` 같은 경로 조작 문자열이 들어와도, 정규화(`normalize()`) 후 업로드 디렉터리를 벗어나면 거부. 이런 취약점을 **경로 조작(Path Traversal)** 공격이라고 함 |

> 실제로 `curl "http://localhost:8080/files/..%2F..%2F..%2Fetc%2Fpasswd"` 로 테스트해봤는데, 우리 코드에 도달하기도 전에 **Tomcat이 자체적으로 400 Bad Request로 차단**했습니다. 즉 이번 방어는 이중 안전장치입니다 — 톰캣이 1차로 막고, 혹시 정규화된 형태로 통과하는 경우를 대비해 우리 코드가 2차로 한 번 더 검증합니다.

### Controller & 보안 설정
```java
@PostMapping
public ResponseEntity<FileResponse> upload(@AuthenticationPrincipal UserDetails me,
                                            @RequestParam("file") MultipartFile file) { ... }

@GetMapping("/{storedName}")
public ResponseEntity<Resource> download(@PathVariable String storedName) { ... }   // 인증 불필요
```
업로드(POST)는 로그인이 필요하지만, 다운로드(GET)는 누구나 가능해야 이미지 등을 웹페이지에 바로 표시할 수 있습니다. 그래서 `SecurityConfig`에 **HTTP 메서드까지 지정한 규칙**을 처음으로 추가했습니다.
```java
.requestMatchers(HttpMethod.GET, "/files/**").permitAll()   // GET만 공개
// POST /files는 지정하지 않았으므로 anyRequest().authenticated()에 걸려 로그인 필요
```

---

## 3. 실시간 알림 — Server-Sent Events(SSE)

### WebSocket 대신 SSE를 고른 이유
실시간으로 서버 → 클라이언트 알림을 보내는 방법은 대표적으로 WebSocket과 SSE 두 가지가 있습니다.

| | WebSocket | SSE (Server-Sent Events) |
|---|---|---|
| 방향 | 양방향 | **서버 → 클라이언트 단방향** |
| 구현 복잡도 | STOMP 등 별도 설정 필요 | 일반 HTTP 위에서 동작, Spring MVC의 `SseEmitter`만으로 충분 |
| 용도 | 채팅처럼 양방향 실시간 통신 | **알림처럼 서버가 일방적으로 push만 하면 되는 경우** |

"새 쪽지가 왔어요" 같은 알림은 서버 → 클라이언트 단방향이면 충분하므로, 구현이 훨씬 간단한 SSE를 선택했습니다.

### NotificationService — 연결 관리
```java
@Service
public class NotificationService {
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String username) {
        SseEmitter emitter = new SseEmitter(0L); // 타임아웃 없음
        emitters.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(username, emitter));
        emitter.onTimeout(() -> remove(username, emitter));
        emitter.onError(e -> remove(username, emitter));

        emitter.send(SseEmitter.event().name("connected").data("connected"));
        return emitter;
    }

    public void notify(String username, String eventName, Object data) {
        List<SseEmitter> userEmitters = emitters.get(username);
        if (userEmitters == null) return;
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                remove(username, emitter);
            }
        }
    }
}
```
사용자 이름(username)별로 열려 있는 연결 목록을 메모리에 들고 있다가, 알림이 필요할 때 그 사용자의 모든 연결에 이벤트를 보냅니다.

### 기존 기능과 연동 — 쪽지를 보내면 알림 발송
```java
// MessageService.send()
Message saved = messageRepository.save(new Message(sender, receiver, content));
notificationService.notify(receiverUsername, "new-message", MessageResponse.from(saved));
return saved;
```
study7의 쪽지 기능 코드에 **딱 한 줄**을 추가해서 실시간 알림을 연결했습니다. 이렇게 관심사를 서비스별로 잘 나눠두면, 새 기능을 기존 코드에 최소한의 변경으로 얹을 수 있습니다.

### 실습 중 실제로 만난 문제 — 브라우저의 `EventSource`는 커스텀 헤더를 못 붙임

지금까지는 인증이 필요한 모든 요청에 `Authorization: Bearer <token>` 헤더를 붙이는 방식을 써왔습니다. 그런데 브라우저 표준 SSE 클라이언트인 **`EventSource` API는 요청 헤더를 커스터마이징할 방법이 없습니다.** (`fetch`와 달리 `new EventSource(url)`은 URL만 받습니다.)

**해결**: 토큰을 헤더 대신 **쿼리 파라미터**로 전달할 수 있도록 `JwtAuthenticationFilter`를 수정했습니다.
```java
private String resolveToken(HttpServletRequest request) {
    String bearer = request.getHeader("Authorization");
    if (bearer != null && bearer.startsWith("Bearer ")) {
        return bearer.substring(7);
    }
    // EventSource는 헤더를 못 붙이므로 쿼리 파라미터로 대체 지원
    return request.getParameter("token");
}
```
```javascript
new EventSource(`/notifications/stream?token=${encodeURIComponent(token)}`);
```

**배울 점**: 토큰을 URL에 노출하는 방식은 헤더보다 보안상 약간 덜 안전합니다(서버 로그, 브라우저 히스토리 등에 남을 수 있음). 하지만 `EventSource`의 구조적 한계 때문에 실무에서도 SSE/WebSocket 인증에 **쿼리 파라미터 토큰**이 흔히 쓰입니다. 다만 민감한 서비스라면 만료 시간이 아주 짧은 별도의 "일회용 스트리밍 토큰"을 발급하는 방식을 쓰기도 합니다.

### 실제로 검증한 내용 — 진짜로 실시간 동작하는지 확인
백그라운드에서 사용자 B로 SSE 연결을 열어두고, 다른 터미널에서 사용자 A가 B에게 쪽지를 보내봤습니다.
```bash
# 터미널 1: B로 SSE 연결
curl -N -H "Authorization: Bearer $TOKEN_B" http://localhost:8080/notifications/stream
# → event:connected  data:connected   (즉시 수신)

# 터미널 2: A가 B에게 쪽지 전송
curl -X POST /messages -H "Authorization: Bearer $TOKEN_A" -d '{"receiverUsername":"ssetest_b","content":"실시간 알림 테스트 메시지입니다"}'
```
```
# 터미널 1에 몇 초 뒤 추가로 도착한 내용
event:new-message
data:{"id":1,"senderUsername":"ssetest_a","receiverUsername":"ssetest_b","content":"실시간 알림 테스트 메시지입니다",...}
```
연결을 새로고침하지 않아도 실시간으로 이벤트가 도착하는 것을 직접 확인했습니다.

---

## 4. API 문서화 — Springdoc(Swagger UI)

```groovy
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1'
```
이 의존성 하나만 추가하면, Spring이 등록된 모든 `@RestController`의 매핑 정보를 분석해서 **API 문서를 자동으로 만들어줍니다.** 컨트롤러/DTO 코드를 따로 문서화할 필요가 거의 없습니다.

```java
@Configuration
@OpenAPIDefinition(info = @Info(title = "Spring Boot Study API", version = "v1", description = "..."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
```
- `@OpenAPIDefinition`으로 문서 제목/설명 지정
- `@SecurityScheme`으로 "이 API들은 JWT Bearer 토큰으로 인증한다"는 걸 문서에 명시 → Swagger UI 화면에 **"Authorize" 버튼**이 생겨서, 토큰을 한 번 입력해두면 인증이 필요한 API도 화면에서 바로 테스트할 수 있습니다.

접속 경로:
- `http://localhost:8080/swagger-ui.html` — API를 직접 테스트해볼 수 있는 화면
- `http://localhost:8080/v3/api-docs` — 기계가 읽는 원본 명세(JSON)

`SecurityConfig`에도 이 경로들을 `permitAll`로 추가해야 로그인 없이 문서에 접근할 수 있습니다.
```java
.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
```

---

## 5. 테스트 코드

- `PostControllerTest`: 기존 `listIsOrderedByLatestFirst`를 새 응답 구조(`$.content[...]`)에 맞게 수정, `listIsPaginated` 신규 추가
- `FileControllerTest`: 업로드 없이/있이 권한 분기, 업로드 후 인증 없이 다운로드 가능한지, 존재하지 않는 파일 404
- `NotificationControllerTest`: 토큰 없이 접근 시 403, 토큰(헤더)과 토큰(쿼리 파라미터) 모두 비동기 연결이 정상 시작되는지

```java
@Test
void streamAcceptsTokenAsQueryParam() throws Exception {
    mockMvc.perform(get("/notifications/stream").param("token", token))
            .andExpect(request().asyncStarted());
}
```
> SSE처럼 오래 열려 있는 스트림은 MockMvc로 "실제 이벤트가 도착하는지"까지 검증하기는 까다로워서, 여기서는 "인증을 통과해서 비동기 연결이 시작되는지"까지만 자동화했고, 실제 이벤트 전달은 위 4번의 curl 실습으로 직접 확인했습니다.

`./gradlew test` 실행 결과, study10에서 추가/수정된 항목을 포함해 **총 36개 테스트 전부 통과**했습니다.

| 테스트 클래스 | 개수 |
|---|---|
| DemoApplicationTests | 1 |
| HelloControllerTest | 3 |
| AuthControllerTest | 5 |
| MemberControllerTest | 3 |
| PostControllerTest | 5 |
| MessageControllerTest | 4 |
| WeatherControllerTest | 3 |
| StockControllerTest | 2 |
| WatchlistControllerTest | 4 |
| **FileControllerTest (신규)** | **3** |
| **NotificationControllerTest (신규)** | **3** |

---

## 6. 응용 활동 — `main.html`로 전체 페이지 모으기

지금까지 만든 프론트엔드 페이지들을 한 곳에서 찾아볼 수 있도록 `demo/src/main/resources/static/main.html`을 만들었습니다.

```
http://localhost:8080/main.html
```

| 카드 | 연결된 페이지 | 로그인 필요 여부 |
|---|---|---|
| 게시판 | `StudyHtmlFile.html` (study6) | 불필요 |
| 로그인 & 쪽지 | `StudyHtmlFile2.html` (study7) | 회원가입/로그인은 당연히 불필요, 쪽지는 필요 |
| 날씨 | `StudyHtmlFile3.html` (study8) | 불필요 |
| 주식 & 관심 종목 | `StudyHtmlFile4.html` (study9) | 관심 종목만 필요 |
| 파일 업로드 & 실시간 알림 | `StudyHtmlFile5.html` (study10) | 필요 |
| API 문서 | `/swagger-ui.html` | 불필요 (문서 열람 자체는) |

### 페이지 간 로그인 상태 공유
모든 페이지가 같은 `localStorage` 키(`token`, `username`)를 사용하도록 설계했기 때문에, **`StudyHtmlFile2.html`에서 한 번 로그인하면 `main.html`을 포함한 다른 모든 페이지에서도 로그인 상태가 유지**됩니다. `main.html` 상단에는 현재 로그인 상태를 보여주는 바(bar)와 로그아웃 버튼을 뒀습니다.

```javascript
function refreshLoginBar() {
    const token = localStorage.getItem("token");
    const username = localStorage.getItem("username");
    // 있으면 "OO님으로 로그인됨", 없으면 로그인 페이지로 가는 링크 표시
}
```

이렇게 여러 개의 독립된 정적 HTML 페이지가, **같은 오리진(같은 서버)에서 서빙되고 같은 `localStorage`를 공유**하기 때문에 마치 하나의 앱처럼 동작합니다 — SPA(Single Page Application) 프레임워크 없이도 순수 HTML/JS만으로 여기까지 만들 수 있다는 걸 확인하는 것이 이번 응용 활동의 목적입니다.

---

## 7. 그 외 실습 중 확인한 사소하지만 중요한 것들

- **정적 파일은 즉시 반영되지 않음**: `src/main/resources/static/`에 새 HTML 파일을 추가해도, 이미 떠 있는 서버는 그 파일을 못 찾고 404를 냅니다(정적 리소스는 빌드 시점에 `build/resources/main`으로 복사되기 때문). 새 정적 파일을 추가했다면 **서버 재시작**이 필요합니다. (매번 재시작이 번거롭다면 `spring-boot-devtools`를 추가하면 자동 재시작/새로고침을 지원합니다 — 다음 기회에 다뤄볼 수 있습니다.)
- **Jackson 3.x deprecation**: `JsonNode.asText()`가 deprecated 되고 `asString()`으로 대체되었습니다. 컴파일 경고(warning)로 표시되므로, 경고가 뜨면 무시하지 말고 권장되는 대체 메서드를 확인하는 습관이 필요합니다.
- **`HttpStatus.PAYLOAD_TOO_LARGE`도 deprecated**: RFC 9110에서 413 상태 코드의 이름이 "Payload Too Large"에서 "Content Too Large"로 바뀌면서, Spring Framework 7의 `HttpStatus`도 `CONTENT_TOO_LARGE`를 새 이름으로 채택하고 기존 것은 사용 중단(deprecated) 처리했습니다.

---

## 8. 정리

- `Pageable`/`Page<T>`로 목록 API에 페이지네이션을 적용하고, API 응답 형식이 바뀌면 프론트엔드/테스트도 함께 맞춰야 한다는 점을 실습했다
- 파일을 디스크에 저장하고 메타데이터는 DB에 남기는 구조, 그리고 경로 조작 공격을 막는 방어 코드를 작성했다
- SSE(`SseEmitter`)로 서버 → 클라이언트 실시간 알림을 구현하고, 기존 쪽지 기능에 최소한의 변경으로 연결했다
- 브라우저 `EventSource`가 커스텀 헤더를 지원하지 않는다는 실제 제약을 만나고, 쿼리 파라미터 토큰 방식으로 해결했다
- Springdoc으로 API 문서를 자동 생성하고, JWT 인증 스킴을 문서에 명시해서 Swagger UI에서 바로 테스트 가능하게 만들었다
- **`main.html`로 study6~10의 모든 프론트엔드 페이지를 하나의 허브로 연결**하고, `localStorage` 공유를 통해 프레임워크 없이도 여러 페이지 간 로그인 상태를 유지하는 응용 활동을 완료했다

## 9. 다음 단계

여기까지 오면서 백엔드(REST API, 인증, DB, 외부 연동, 파일, 실시간, 문서화)와 프론트엔드(순수 HTML/JS 연동) 양쪽을 한 바퀴 돌았습니다. 다음으로 고려해볼 만한 주제:

1. **배포** — Dockerfile 작성, 클라우드(예: Fly.io, Render)에 실제로 띄워보기
2. **CI/CD** — GitHub Actions로 커밋할 때마다 자동으로 테스트 실행
3. **캐싱** — study9에서 언급했던, 외부 API 호출을 `@Cacheable`로 줄이는 최적화
4. **관리자 기능/권한 분리** — `ROLE_ADMIN` 같은 역할 기반 접근 제어(RBAC)

원하시는 주제로 다음 study를 이어가면 됩니다.
