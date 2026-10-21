# study10. 고급 5 — 파일, 실시간 알림, API 문서

## 1. 파일

업로드는 인증이 필요합니다. 한도 5MB.

```powershell
curl.exe -X POST http://localhost:8080/files `
  -H "Authorization: Bearer 토큰" `
  -F "file=@C:\path\to\note.txt"
```

응답의 `url`은 `/files/{storedName}`입니다. 다운로드 GET은 공개입니다. 저장 파일 이름은 서버가 만들고, 원본 이름의 `..` 경로는 받아들이지 않습니다. 없는 파일은 404입니다.

파일은 `demo/uploads/`에 있습니다. 이 디렉터리는 git에 넣지 않습니다.

## 2. SSE

`GET /notifications/stream`은 `text/event-stream`입니다. 토큰은 쿼리 `token`으로도 받습니다. 브라우저 `EventSource`가 `Authorization` 헤더를 못 붙이기 때문입니다.

연결이 유지되는 동안 서버가 알림 이벤트를 보냅니다. 토큰이 없으면 401입니다.

## 3. OpenAPI

springdoc이 컨트롤러를 읽어 문서를 만듭니다.

- JSON: `http://localhost:8080/v3/api-docs`
- 화면: `http://localhost:8080/swagger-ui.html`

경로를 바꿀 때는 컨트롤러와 DTO가 기준입니다. 문서를 손으로 따라 적지 않습니다.

## 4. 여기서 고정한 것

- 업로드는 인증, 다운로드는 공개, 크기 한도는 5MB입니다.
- 실시간 채널은 SSE 하나입니다.
- API 문서는 springdoc이 생성합니다.
