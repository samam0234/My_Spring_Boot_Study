# study8. 고급 3 — 테스트와 프로파일

## 1. 프로파일

설정 파일은 두 벌입니다.

| 파일 | 언제 |
|---|---|
| `application.properties` | 인자를 안 줄 때. SQLite |
| `application-postgres.properties` | `--spring.profiles.active=postgres` |

활성 프로파일은 공개 엔드포인트로 확인합니다.

```powershell
curl.exe http://localhost:8080/time/profile
```

프로파일을 지정하지 않으면 `activeProfiles`는 `default`입니다. 데이터소스 URL도 같이 돌려주므로 SQLite 파일인지 PostgreSQL인지를 구분할 수 있습니다. 비밀번호는 넣지 않습니다.

## 2. 테스트

`demo`에서:

```powershell
.\gradlew.bat test
```

컨트롤러 테스트는 `@SpringBootTest`와 `MockMvc`입니다. 보안 필터가 켜진 상태로 HTTP 상태와 JSON을 확인합니다.

| 테스트 | 확인하는 것 |
|---|---|
| `HelloControllerTest` | 실습 1의 세 URL |
| `MemberControllerTest` | 토큰 없이 401, 토큰이 있으면 CRUD, 잘못된 이메일은 400 |
| `PostControllerTest` | 공개 CRUD, 빈 제목 400, 최신순, 페이지, 태그, 검색 |
| `AuthControllerTest` | 회원가입과 로그인 |
| `MessageControllerTest` | 받은/보낸/대화 |
| `WeatherControllerTest`, `StockControllerTest` | 외부 API는 mock |
| `WatchlistControllerTest`, `FileControllerTest`, `StorageControllerTest` | 인증이 필요한 기능 |
| `NotificationControllerTest` | SSE |
| `TimeControllerTest` | 주간/야간과 프로파일 |

`@Transactional`이 붙은 테스트는 끝난 뒤 롤백합니다. `data.db`에 테스트 회원을 남기지 않으려는 장치입니다.

슬라이스 테스트가 더 필요하면 `@WebMvcTest`로 컨트롤러만 띄울 수 있습니다. 이 저장소는 보안 필터와 JPA를 함께 보려는 학습이라 통합 테스트가 기본입니다.

## 3. 여기서 고정한 것

- 환경 차이는 프로파일로 나눕니다. 코드에 `if (production)`을 넣지 않습니다.
- 테스트 명령은 `.\gradlew.bat test` 하나입니다.
