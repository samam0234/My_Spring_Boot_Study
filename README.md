# Spring Boot 학습

Notion 페이지 [Spring Boot](https://app.notion.com/p/3d6d7dec5e2e8173aac3d4c26b97894b)의 방향대로 만든 학습 저장소입니다. Java 기업 표준 백엔드로, Controller — Service — Repository와 JPA를 고정합니다.

문서는 `study0.md`부터 `study10.md`, 마지막에 `studyFinal.md` 순서입니다.

| 폴더 | 하는 일 | 주소 |
|---|---|---|
| `demo/` | study1~10이 쌓인 Spring Boot 앱 | http://localhost:8080 |
| `project/server/` | studyFinal 사이트 API. 같은 계층과 JWT | http://localhost:8082 |
| `project/client/` | studyFinal 화면. React가 `project/server`를 부름 | http://localhost:5173 |

## 실행

```powershell
cd demo
.\gradlew.bat bootRun
```

사이트를 보려면 `project/server`를 8082로 띄우고 화면은 `project/client`입니다.

```powershell
cd project\server
.\gradlew.bat bootRun
```

```powershell
cd project\client
npm install
npm run dev
```

기본 DB는 각 앱 폴더의 SQLite `data.db`입니다. PostgreSQL은 그 폴더의 `compose.yaml`을 띄운 뒤 `.\gradlew.bat bootRun --args="--spring.profiles.active=postgres"`입니다. `demo`는 호스트 5432, `project/server`는 5433입니다.

테스트는 각 앱 폴더에서 `.\gradlew.bat test`입니다. 실습 문서는 http://localhost:8080/swagger-ui.html , 사이트 문서는 http://localhost:8082/swagger-ui.html 입니다.

## 진도

| 문서 | 주제 |
|---|---|
| [study0.md](study0.md) | 기초이론. 계층, IoC/DI, JPA를 여기에 고정하는 이유 |
| [study1.md](study1.md) | 만드는 방법, Gradle 명령, Hello |
| [study2.md](study2.md) | REST, DTO, Validation |
| [study3.md](study3.md) | Service와 예외 |
| [study4.md](study4.md) | JPA, SQLite, PostgreSQL 프로파일 |
| [study5.md](study5.md) | 1:N, N:M |
| [study6.md](study6.md) | Security, JWT |
| [study7.md](study7.md) | 페이지, 검색 |
| [study8.md](study8.md) | 테스트, 프로파일 |
| [study9.md](study9.md) | 외부 API. 날씨, 시세, 관심종목 |
| [study10.md](study10.md) | 파일, SSE, OpenAPI |
| [studyFinal.md](studyFinal.md) | 사이트 `결`. `project/server`와 React |

`demo/src/main/resources/static/`의 HTML은 이전 단계 페이지입니다. 예시 사이트의 본편은 `project/client`입니다.

JWT 시크릿과 PostgreSQL 비밀번호는 로컬 학습용입니다.
