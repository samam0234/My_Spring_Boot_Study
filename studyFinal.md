# studyFinal. 작은 웹사이트

지금까지의 API를 한 화면에서 씁니다. 백엔드는 `demo/`, 화면은 `project/client/`입니다. React(Vite)이고 Spring 템플릿을 수정하지 않습니다.

사이트 이름은 **결**입니다. 게시판, 쪽지, 날씨, 시세, 파일, 개인 저장소를 한 레이아웃에 넣었습니다.

## 1. 실행

터미널을 두 개 엽니다.

```powershell
cd demo
.\gradlew.bat bootRun
```

```powershell
cd project\client
npm install
npm run dev
```

화면 주소는 `http://localhost:5173`입니다. Vite가 `/posts`, `/auth`, `/messages` 등을 `http://localhost:8080`으로 넘깁니다. 개발 서버를 거치지 않고 브라우저가 8080을 직접 부르면 `CorsConfig`가 `http://localhost:5173`을 허용합니다.

## 2. 화면과 진도

| 화면 | 쓰는 API | 진도 |
|---|---|---|
| 홈 | `/hello`, `/time/status`, `/time/profile` | study1, study8 |
| 게시판 | `/posts` 목록, 검색, 태그, 작성, 수정, 삭제 | study2, study5, study7 |
| 입장 | `/auth/signup`, `/auth/login` | study6 |
| 쪽지 | `/messages` | study5의 1:N, study6 |
| 둘러보기 | `/weather`, `/stocks`, `/watchlist` | study9 |
| 작업실 | `/members`, `/files`, `/storage` | study2, study6, study10 |

토큰은 `localStorage` 키 `gyeol.token`에 있습니다. 보호된 요청에 `Authorization: Bearer`를 붙입니다.

샘플로 쓸 계정은 없습니다. 입장 화면에서 아이디와 비밀번호를 만들면 됩니다. 아이디 3자 이상, 비밀번호 4자 이상입니다.

## 3. 게시판에서 확인할 흐름

1. 글을 쓰고 태그 `jpa`를 붙입니다.
2. 검색창에 `jpa`를 넣어 그 글만 나오는지 봅니다.
3. 페이지 크기는 서버 기본값 5입니다. 글을 더 올리면 다음 페이지가 생깁니다.
4. 로그인하지 않아도 글은 열립니다. 쪽지와 작업실은 로그인 뒤에 열립니다.

## 4. 정적 HTML

`demo/src/main/resources/static/`의 `StudyHtmlFile*.html`은 예전에 단계마다 쓰던 페이지입니다. 예시 사이트의 본편은 `project/client`입니다. Swagger는 그대로 `http://localhost:8080/swagger-ui.html`입니다.

## 5. 여기서 고정한 것

- 화면 프로젝트와 API 프로젝트를 폴더로 나눕니다.
- 화면은 이 저장소의 API만 부릅니다.
- 로그인 상태는 브라우저의 JWT입니다. 서버 세션이 아닙니다.
