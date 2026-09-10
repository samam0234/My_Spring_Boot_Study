# Study 6 — 프론트엔드(HTML/JS)로 백엔드 확인하기: 간단한 게시판

지금까지는 curl이나 자동화된 테스트 코드로만 API를 확인했습니다. 이번 실습부터는 **진짜 HTML 페이지**를 만들어서, 브라우저에서 직접 글을 쓰고 목록을 보고 수정/삭제하면서 백엔드가 정말로 동작하는지 눈으로 확인합니다.

이번 스터디의 주제는 **게시판(Board)** 이고, 프론트엔드 파일은 `demo/src/main/resources/static/StudyHtmlFile.html` 입니다.

> 학습 로드맵(사용자 지정)
> - study6 — 게시판 (`StudyHtmlFile.html`)
> - study7 — 메시지 기능 (`StudyHtmlFile2.html`)
> - study8 — 날씨 페이지 (`StudyHtmlFile3.html`)
> - study9 — 주식 기능 (`StudyHtmlFile4.html`)
> - study10 — 그 외 자주 쓰이는 웹 기술 (`StudyHtmlFile5.html`)

---

## 1. Spring Boot는 어떻게 HTML 파일을 서빙할까?

Spring Boot는 `src/main/resources/static/` 폴더 아래에 있는 파일을 **별도 설정 없이 자동으로 정적 리소스**로 서빙합니다.

```
src/main/resources/static/StudyHtmlFile.html
                ↓
브라우저에서 http://localhost:8080/StudyHtmlFile.html 로 바로 접근 가능
```

같은 서버(`localhost:8080`)에서 HTML도 서빙하고 REST API(`/posts`)도 처리하기 때문에, 프론트엔드 JS 코드에서 별도의 CORS(교차 출처 리소스 공유) 설정 없이 바로 `fetch("/posts")`를 호출할 수 있습니다. (만약 HTML을 다른 포트/다른 서버에서 열었다면 CORS를 별도로 허용해줘야 합니다 — 이 주제는 이후 study에서 필요해지면 다룹니다.)

---

## 2. 게시판 백엔드 — study2/3에서 만든 패턴 재사용

`Post`(게시글) 기능은 study2의 `Member` CRUD, study3의 DTO/예외 처리 패턴을 그대로 재사용해서 만듭니다. 이미 배운 패턴이므로 빠르게 훑어봅니다.

### Entity
```java
// domain/Post.java
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Post(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
    // getter 생략
}
```
- `createdAt`/`updatedAt`을 엔티티 생성자와 `update()` 메서드 안에서 직접 관리합니다. (더 정교하게 하려면 JPA의 `@CreatedDate`/`@LastModifiedDate` + `@EnableJpaAuditing`을 쓸 수 있는데, 이건 다음 기회에 다룹니다.)

### Repository — 최신순 정렬
```java
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByOrderByCreatedAtDesc();
}
```
- 메서드 이름만으로 "`createdAt` 기준 내림차순 정렬해서 전체 조회"라는 쿼리가 자동 생성됩니다 (Spring Data JPA의 **쿼리 메서드** 기능).

### DTO — 생성용/수정용을 분리
```java
public record PostRequest(
        @NotBlank(message = "제목은 필수입니다.") String title,
        @NotBlank(message = "내용은 필수입니다.") String content,
        @NotBlank(message = "작성자는 필수입니다.") String author
) {}

public record PostUpdateRequest(
        @NotBlank(message = "제목은 필수입니다.") String title,
        @NotBlank(message = "내용은 필수입니다.") String content
) {}

public record PostResponse(
        Long id, String title, String content, String author,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static PostResponse from(Post post) { ... }
}
```
- 글 생성 시에는 작성자(`author`)가 필요하지만, **수정 시에는 작성자를 바꿀 일이 없으므로** `PostRequest`와 `PostUpdateRequest`를 분리했습니다. study3에서 배운 "DTO는 그 상황에 필요한 필드만 가진다"는 원칙을 한 단계 더 적용한 것입니다.

### Service / Controller
```java
@Service
public class PostService {
    public Post create(String title, String content, String author) { ... }
    public List<Post> findAll() { return postRepository.findAllByOrderByCreatedAtDesc(); }
    public Post findById(Long id) { ... } // 없으면 PostNotFoundException
    public Post update(Long id, String title, String content) { ... }
    public void delete(Long id) { ... }
}

@RestController
@RequestMapping("/posts")
public class PostController {
    @PostMapping    public ResponseEntity<PostResponse> create(@Valid @RequestBody PostRequest request) { ... }
    @GetMapping     public List<PostResponse> findAll() { ... }
    @GetMapping("/{id}") public PostResponse findById(@PathVariable Long id) { ... }
    @PutMapping("/{id}") public PostResponse update(@PathVariable Long id, @Valid @RequestBody PostUpdateRequest request) { ... }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { ... }
}
```
`MemberController`/`MemberService`와 구조가 완전히 동일합니다. `PostNotFoundException`도 `MemberNotFoundException`과 같은 방식으로 만들어서 `GlobalExceptionHandler`에 핸들러를 추가했습니다.

---

## 3. 보안 설정 변경 — 게시판은 로그인 없이 공개

`SecurityConfig`에 두 가지를 추가했습니다.

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers(PathRequest.toH2Console()).permitAll()
        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
        .requestMatchers("/*.html", "/auth/**", "/hello/**", "/posts/**").permitAll()
        .anyRequest().authenticated())
```

| 추가한 규칙 | 이유 |
|---|---|
| `"/posts/**"` | 이번 게시판은 누구나 글을 읽고 쓸 수 있는 **공개 게시판**으로 설계했습니다 (로그인 기능을 다시 요구하면 프론트엔드가 복잡해지므로, 이번 study의 초점인 "프론트-백엔드 연동"에 집중하기 위함). 로그인한 사용자만 글을 쓰게 하려면 study4의 JWT를 그대로 여기에도 적용하면 됩니다. |
| `"/*.html"` | 우리가 만든 `StudyHtmlFile.html` 같은 최상위 정적 HTML 파일에 접근을 허용합니다. |

> ⚠️ **실습 중 실제로 만난 문제**: 처음엔 `PathRequest.toStaticResources().atCommonLocations()` 만 추가하면 될 줄 알았는데, `/StudyHtmlFile.html`에 접근하면 여전히 **403**이 떴습니다. 원인은 `atCommonLocations()`가 커버하는 범위가 `/css/**`, `/js/**`, `/images/**`, `/webjars/**`, `/favicon.ico` 처럼 **정해진 하위 경로들**이지, `static/` 바로 아래에 놓인 임의의 HTML 파일까지 포함하지는 않기 때문이었습니다. 그래서 `"/*.html"` 패턴을 별도로 추가해서 해결했습니다. 앞으로 만들 `StudyHtmlFile2.html` ~ `StudyHtmlFile5.html`도 이 패턴 덕분에 추가 설정 없이 바로 접근 가능합니다.

---

## 4. 프론트엔드 — `StudyHtmlFile.html`

전체 파일은 `demo/src/main/resources/static/StudyHtmlFile.html`에 있습니다. 핵심 부분만 짚어봅니다.

### 4-1. 공통 API 호출 함수
```javascript
const API_BASE = "/posts";

async function api(path, options) {
    const response = await fetch(API_BASE + path, options);
    const isJson = response.headers.get("content-type")?.includes("application/json");
    const body = isJson ? await response.json() : null;

    if (!response.ok) {
        const message = body && body.message ? body.message : `요청 실패 (HTTP ${response.status})`;
        throw new Error(message);
    }
    return body;
}
```
- `fetch`는 브라우저 기본 내장 함수로, HTTP 요청을 보내고 `Promise`를 반환합니다.
- `response.ok`는 상태 코드가 200번대인지 여부. 아니면(400/404 등) `GlobalExceptionHandler`가 만들어준 `{"message": "..."}` 형식의 에러 바디를 그대로 꺼내서 예외로 던집니다. → study3에서 만든 통일된 에러 응답 형식이 프론트엔드에서도 그대로 활용됩니다.

### 4-2. 목록 조회 & 렌더링
```javascript
async function loadPosts() {
    const posts = await api("", { method: "GET" });
    renderPostList(posts);
}

function renderPostList(posts) {
    postList.innerHTML = "";
    posts.forEach((post) => {
        const li = document.createElement("li");
        li.innerHTML = `
            <div class="post-title">${escapeHtml(post.title)}</div>
            <div class="post-meta">${escapeHtml(post.author)} · ${formatDate(post.createdAt)}</div>
        `;
        li.addEventListener("click", () => openDetail(post.id));
        postList.appendChild(li);
    });
}
```
- `GET /posts` 응답(JSON 배열)을 받아서, `<li>` 요소를 하나씩 만들어 목록에 추가합니다.
- `escapeHtml()`로 사용자가 입력한 제목/작성자를 이스케이프 처리합니다. 이렇게 하지 않으면 누군가 제목에 `<script>...</script>` 같은 내용을 넣었을 때 그대로 실행되는 **XSS(Cross-Site Scripting)** 취약점이 생길 수 있습니다.

### 4-3. 글쓰기 폼 제출
```javascript
document.getElementById("createForm").addEventListener("submit", async (e) => {
    e.preventDefault(); // 폼의 기본 동작(페이지 새로고침)을 막음
    const title = document.getElementById("title").value;
    const author = document.getElementById("author").value;
    const content = document.getElementById("content").value;

    await api("", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title, author, content })
    });
    e.target.reset();
    await loadPosts();
});
```
- `e.preventDefault()`가 없으면 폼 제출 시 브라우저가 페이지를 새로고침하며 이동해버립니다 (SPA 방식이 아닌 전통적 폼 동작).
- 입력값을 JS 객체로 모아 `JSON.stringify`로 문자열로 바꾼 뒤 `POST /posts`로 전송 — 우리가 `@RequestBody PostRequest`로 받는 바로 그 JSON입니다.
- 성공하면 폼을 비우고(`reset()`) 목록을 다시 불러옵니다.

### 4-4. 상세 보기 / 수정 / 삭제
같은 패턴으로 `GET /posts/{id}`(상세), `PUT /posts/{id}`(수정), `DELETE /posts/{id}`(삭제)를 호출합니다. 삭제 전에는 `confirm()`으로 브라우저 기본 확인창을 띄워 실수를 방지합니다.

---

## 5. 실행 & 브라우저에서 확인하기

```powershell
cd demo
.\gradlew.bat bootRun
```

브라우저에서 다음 주소로 접속합니다.
```
http://localhost:8080/StudyHtmlFile.html
```

- 글쓰기 폼에 제목/작성자/내용을 입력하고 "등록" → 목록에 새 글이 나타남
- 글 목록에서 항목 클릭 → 상세 내용이 아래에 표시됨
- "수정" → 폼으로 바뀌고, 값 바꾼 뒤 "저장" → 목록/상세가 갱신됨
- "삭제" → 확인창 후 삭제되고 목록에서 사라짐

> 📌 저는 코드 작성 후 **API 자체는 curl로, 정적 파일 서빙은 HTTP 응답 헤더/본문으로 직접 확인**했습니다(아래 6장 참고). 다만 저는 실제 브라우저 화면을 눈으로 볼 수는 없으므로, 버튼 클릭이나 폼 동작 같은 **실제 UI 상호작용은 여러분이 브라우저에서 직접 열어 확인해봐야** 합니다. 위 시나리오대로 한 번씩 눌러보시면서 정상 동작하는지 확인해보세요.

---

## 6. 실제로 검증한 내용 (API 레벨)

이번 실습 환경에서 서버를 띄우고 다음을 실제로 확인했습니다.

```bash
curl http://localhost:8080/posts                          # → 200, []
curl -X POST http://localhost:8080/posts -H "Content-Type: application/json" \
     --data-binary @post1.json                             # → 201, {"id":1,"title":"첫 글",...}
curl -X POST http://localhost:8080/posts -H "Content-Type: application/json" \
     --data-binary @post2.json                             # → 201, {"id":2,"title":"두번째 글",...}
curl http://localhost:8080/posts                          # → 200, [두번째 글, 첫 글]  (최신순)
curl http://localhost:8080/posts/1                        # → 200
curl -X PUT http://localhost:8080/posts/1 -H "Content-Type: application/json" \
     --data-binary @post1_update.json                      # → 200, 제목/내용 변경됨
curl -X POST http://localhost:8080/posts -d '{"title":"","content":"x","author":"x"}' \
     -H "Content-Type: application/json"                   # → 400, "title: 제목은 필수입니다."
curl -X DELETE http://localhost:8080/posts/2               # → 204
curl http://localhost:8080/posts/2                         # → 404, "게시글을 찾을 수 없습니다. id=2"
curl -i http://localhost:8080/StudyHtmlFile.html            # → 200, Content-Type: text/html
```
모두 의도한 대로 동작했습니다.

---

## 7. 실습 중 실제로 만난 문제 — curl로 한글 본문을 보내면 403이 떴던 이유

게시판이니 만큼 제목/내용에 한글을 넣어 테스트하는 게 자연스러운데, 처음에 아래처럼 커맨드에 한글을 직접 넣어 테스트했더니 **원인을 알 수 없는 403**이 발생했습니다.

```bash
curl -X POST http://localhost:8080/posts \
  -H "Content-Type: application/json" \
  -d '{"title":"첫 글","content":"안녕하세요","author":"철수"}'
# → 403 Forbidden (본문 없음)
```

그런데 똑같은 요청을 영문으로 바꾸면 정상 동작(`201 Created`)했고, `GlobalExceptionHandler`가 만드는 JSON 에러 바디도 없이 **완전히 빈 403**이었습니다 (컨트롤러까지 도달하기 전에 막혔다는 뜻).

**원인 파악**: `curl -v`로 실제 전송되는 바이트를 확인해보니 `Content-Length`는 정상적으로 계산되어 있었습니다. 문제는 Spring이나 우리 코드가 아니라, **이 실습 환경(Windows + Git Bash)에서 커맨드에 직접 입력한 한글(멀티바이트 UTF-8 문자)이 `curl.exe`에 전달되는 과정에서 깨지는 문제**였습니다. 셸이 한글을 명령줄 인자로 넘길 때 인코딩이 어긋나면서, 실제로 전송되는 바이트가 손상되어 서버(정확히는 톰캣/스프링 시큐리티 계층)가 요청 자체를 비정상으로 판단해 즉시 거부한 것으로 보입니다.

**해결**: 한글이 포함된 JSON 본문은 **파일로 먼저 저장한 뒤 `--data-binary @파일경로`로 전송**하면 셸의 인자 인코딩 문제를 완전히 우회할 수 있습니다.
```bash
# post1.json 파일에 정확한 UTF-8 바이트로 저장해두고
curl -X POST http://localhost:8080/posts \
  -H "Content-Type: application/json" \
  --data-binary @post1.json
# → 201 Created (정상)
```

**배울 점**:
- 이건 Spring Boot/Spring Security의 버그가 아니라 **터미널 환경 특유의 인자 인코딩 문제**입니다. 브라우저(fetch)로 보내는 요청에는 이런 문제가 없습니다 — 브라우저는 폼/JS에서 만든 문자열을 항상 올바른 UTF-8로 인코딩해서 보냅니다.
- 그래서 이번 study6의 실제 UI 동작 검증(한글 게시글 작성 등)은 curl이 아니라 **브라우저에서 직접** 해보시는 게 맞습니다. curl 테스트에서 한글 때문에 이상한 403을 만나면, "코드가 잘못됐나?"보다 먼저 "터미널 인코딩 문제인가?"를 의심하고, 필요하면 파일로 저장해서 보내보는 습관을 들이면 좋습니다.

---

## 8. 테스트 코드 추가

study5에서 배운 대로 `PostControllerTest`도 추가했습니다 (`/posts`는 로그인이 필요 없으므로 토큰 없이 바로 테스트).

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostControllerTest {
    // requestsWithoutTokenAreAllowed()   — 로그인 없이 GET /posts 접근 가능한지
    // createFindUpdateDeleteFlow()       — 생성→조회→수정→삭제→삭제 후 조회(404) 전체 흐름
    // createWithBlankTitleFails()        — 제목 없이 생성 시 400
    // listIsOrderedByLatestFirst()       — 목록이 최신순으로 정렬되는지
}
```

`./gradlew test` 실행 결과, 이번 study6에서 추가된 4개를 포함해 **총 16개 테스트 전부 통과**했습니다.

| 테스트 클래스 | 개수 |
|---|---|
| DemoApplicationTests | 1 |
| HelloControllerTest | 3 |
| AuthControllerTest | 5 |
| MemberControllerTest | 3 |
| **PostControllerTest (신규)** | **4** |

---

## 9. 정리

- Spring Boot가 `src/main/resources/static/`의 파일을 자동으로 서빙한다는 것을 이용해, 별도 프론트엔드 서버 없이 백엔드와 한 번에 실행되는 HTML 페이지를 만들었다
- study2/3의 CRUD + DTO + 예외 처리 패턴을 재사용해서 게시판(`Post`) 백엔드를 빠르게 구현했다
- `fetch` API로 브라우저에서 REST API를 호출하고, 결과를 DOM에 그려주는 기본적인 프론트엔드 로직을 작성했다
- `SecurityConfig`에서 정적 리소스/공개 API에 대한 접근 규칙을 상황에 맞게 조정하는 법을 익혔다
- 실습 중 겪은 실제 문제(정적 리소스 매처의 범위, 터미널 인자 인코딩 문제)를 원인 분석부터 해결까지 기록했다

## 10. 다음 단계

**study7.md**에서는 `StudyHtmlFile2.html`과 함께 **메시지(쪽지/채팅) 기능**을 만들어봅니다. 로그인한 사용자 간에 메시지를 주고받는 기능을 다루게 되므로, study4에서 만든 JWT 로그인을 프론트엔드에서 실제로 사용하는 방법(로그인 폼 → 토큰을 `localStorage`에 저장 → 이후 요청에 자동으로 토큰 첨부)도 함께 다룰 예정입니다.
