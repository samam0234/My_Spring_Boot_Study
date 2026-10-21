# study6. 고급 1 — Spring Security와 JWT

회원, 쪽지, 저장소, 관심종목, 파일 업로드, 알림 스트림은 로그인 뒤에만 열립니다. 세션 쿠키 대신 JWT를 씁니다.

## 1. 구성

| 클래스 | 역할 |
|---|---|
| `SecurityConfig` | CSRF 끄기, 세션 STATELESS, 경로별 인가, JWT 필터 등록 |
| `JwtTokenProvider` | 발급과 서명 검증 |
| `JwtAuthenticationFilter` | `Authorization: Bearer`를 읽어 인증 객체를 채움 |
| `CustomUserDetailsService` | 토큰의 아이디로 `User` 조회 |
| `AuthService` | 회원가입(BCrypt)과 로그인 |

비밀번호는 `BCryptPasswordEncoder`로 해시합니다. 평문을 저장하지 않습니다.

시크릿과 만료는 `application.properties`의 `jwt.secret`, `jwt.expiration`입니다. 학습용 값입니다. 운영에서는 환경 변수로 바꿉니다.

## 2. 누가 열려 있나

인증 없이 허용하는 경로:

- 정적 HTML, `/error`
- `/auth/**`, `/hello/**`, `/posts/**`
- `/weather/**`, `/stocks/**`, `/time/**`
- `GET /files/**`
- Swagger `/v3/api-docs/**`, `/swagger-ui/**`

그 외 `/members`, `/messages`, `/storage`, `/watchlist`, 파일 업로드, `GET /notifications/stream`은 인증이 필요합니다.

## 3. 실습

```powershell
curl.exe -X POST http://localhost:8080/auth/signup `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"mina\",\"password\":\"password123\"}"

curl.exe -X POST http://localhost:8080/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"mina\",\"password\":\"password123\"}"
```

로그인 응답의 `token`을 다음 요청에 붙입니다.

```powershell
curl.exe http://localhost:8080/messages/inbox -H "Authorization: Bearer 여기에토큰"
```

토큰 없이 호출하면 401입니다. 아이디가 이미 있으면 회원가입은 409입니다.

## 4. 여기서 고정한 것

- 상태는 서버 세션이 아니라 JWT에 있습니다.
- 보호된 API는 `Authorization: Bearer`가 필요합니다.
- 게시글 조회와 작성은 학습을 위해 공개입니다. 최종 사이트의 쪽지와 개인 저장소는 보호됩니다.
