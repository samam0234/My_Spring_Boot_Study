# study2. 실습 2 — REST, DTO, Validation

실습 1은 문자열을 돌려줬습니다. 실습 2는 JSON으로 회원을 받고, 잘못된 값은 컨트롤러에 들어가기 전에 거절합니다.

코드는 `MemberController`, `MemberRequest`, `MemberResponse`입니다. 저장은 study4의 JPA가 맡지만, 요청과 응답의 모양은 여기서 고정합니다.

## 1. 엔드포인트

회원 API는 로그인이 필요합니다. 토큰을 받는 방법은 study6입니다. 모양만 먼저 봅니다.

| 메서드 | 경로 | 본문 |
|---|---|---|
| POST | `/members` | `{ "name": "민아", "email": "mina@example.com" }` |
| GET | `/members` | 목록 |
| GET | `/members/{id}` | 한 명 |
| PUT | `/members/{id}` | 수정 |
| DELETE | `/members/{id}` | 204, 본문 없음 |

## 2. DTO를 나누는 이유

엔티티를 그대로 받으면 클라이언트가 `id`나 나중에 생길 관리자 플래그를 넣을 수 있습니다. 요청 DTO와 응답 DTO를 나눕니다.

`MemberRequest`는 생성과 수정에 씁니다.

```java
public record MemberRequest(
        @NotBlank(message = "이름은 필수입니다.") String name,
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.") String email
) {}
```

`MemberResponse`는 `id`, `name`, `email`만 내보냅니다.

## 3. Validation

컨트롤러 인자에 `@Valid`가 있어야 어노테이션이 동작합니다.

```java
@PostMapping
public ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberRequest request) {
    ...
}
```

실패하면 400입니다. 메시지 형식은 study3의 `GlobalExceptionHandler`가 맞춥니다.

게시글도 같은 규칙입니다. `PostRequest`는 제목, 내용, 작성자가 비어 있으면 거절합니다.

```powershell
curl.exe -X POST http://localhost:8080/posts `
  -H "Content-Type: application/json" `
  -d "{\"title\":\"\",\"content\":\"본문\",\"author\":\"민아\"}"
```

401이 아니라 400이 나와야 검증이 먼저 탄 것입니다. `/posts`는 인증 없이 열려 있습니다.

## 4. 여기서 고정한 것

- API는 엔티티가 아니라 DTO를 받습니다.
- `@NotBlank`, `@Email`과 `@Valid`로 입력 검증을 합니다.
- 검증 실패의 HTTP 상태는 400입니다.
