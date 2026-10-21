# study9. 고급 4 — 외부 API

서버가 브라우저 대신 외부 API를 부릅니다. 키와 장애 처리를 서버에 두기 위해서입니다. 클라이언트는 `RestClient`입니다.

## 1. 날씨

`GET /weather?city=Seoul`

Open-Meteo 지오코딩으로 좌표를 찾고, 그 좌표의 현재 날씨를 `WeatherResponse`로 바꿉니다. 도시가 없으면 404(`CityNotFoundException`), 외부 호출이 실패하면 503입니다.

인증은 없습니다.

```powershell
curl.exe "http://localhost:8080/weather?city=Seoul"
```

## 2. 시세와 관심종목

`GET /stocks/{symbol}`은 Yahoo Finance 차트를 읽어 가격, 전일 대비, 고가, 저가, 거래량을 돌려줍니다. 종목이 없으면 404, 호출 실패는 503입니다. 이 조회는 공개입니다.

관심종목은 사용자마다 다릅니다.

| 메서드 | 경로 | 인증 |
|---|---|---|
| POST | `/watchlist` `{ "symbol": "AAPL" }` | 필요 |
| GET | `/watchlist` | 필요. 저장 종목의 현재 시세 목록 |
| DELETE | `/watchlist/{symbol}` | 필요 |

같은 사용자가 같은 심볼을 다시 넣으면 409입니다. 심볼은 저장 전에 대문자로 맞춥니다.

## 3. 여기서 고정한 것

- 외부 주소는 프론트에 두지 않습니다.
- 장애는 503, 대상 없음은 404로 나눕니다.
- 시세 조회는 공개, 관심종목 저장은 JWT입니다.
