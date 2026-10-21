# study3. 실습 3 — Service와 예외

Controller는 HTTP만 다룹니다. "없으면 404", "이름이 겹치면 409" 같은 판단은 Service에 둡니다. 예외를 응답 JSON으로 바꾸는 일은 한곳(`GlobalExceptionHandler`)에 둡니다.

## 1. Service가 하는 일

`MemberService`를 예로 들면 이렇습니다.

- id로 찾고, 없으면 `MemberNotFoundException`
- 생성과 수정은 Repository에 넘기기 전에 값의 의미를 정리
- Controller는 Service가 돌려준 엔티티를 `MemberResponse`로 바꿈

게시글, 쪽지, 파일, 관심종목도 같은 자리입니다. Controller 시그니처에 `HttpServletRequest`를 넣지 않습니다.

## 2. 예외를 상태 코드로

`@RestControllerAdvice`가 던진 예외를 잡습니다.

| 상황 | 예외 | 상태 |
|---|---|---|
| 회원, 글, 사용자, 도시, 종목, 저장값, 파일 없음 | `*NotFoundException` | 404 |
| `@Valid` 실패 | `MethodArgumentNotValidException` | 400 |
| 아이디 중복, 관심종목 중복 | `DuplicateUsernameException`, `DuplicateWatchlistItemException` | 409 |
| 날씨/시세 외부 API 장애 | `*ServiceUnavailableException` | 503 |
| 로그인 실패 | `AuthenticationException` | 401 |
| 파일 형식이나 크기 | `InvalidFileException` | 400 |

응답 본문은 `ErrorResponse`입니다. 본문에 스택 트레이스를 넣지 않습니다.

## 3. 확인

없는 글:

```powershell
curl.exe -i http://localhost:8080/posts/999999
```

`404`와 `게시글을 찾을 수 없습니다`가 본문에 있으면 핸들러가 연결된 것입니다.

## 4. 여기서 고정한 것

- 업무 판단은 Service, HTTP 변환은 Advice입니다.
- 없는 자원은 404, 형식 오류는 400, 충돌은 409, 외부 장애는 503입니다.
