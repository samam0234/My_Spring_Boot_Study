# Study 8 — 외부 API 연동: 날씨 페이지

지금까지는 우리 DB(`Member`, `Post`, `Message`, `User`)에 저장된 데이터를 다뤘습니다. 이번 study에서는 **우리 서버가 외부의 다른 공개 API를 대신 호출해서, 그 결과를 가공해 클라이언트에 내려주는 패턴**을 다룹니다. 날씨 정보는 우리 DB에 없으니, 실시간으로 외부에서 가져와야 하는 대표적인 사례입니다.

프론트엔드 파일은 `demo/src/main/resources/static/StudyHtmlFile3.html` 입니다.

---

## 1. 왜 프론트엔드가 외부 API를 직접 호출하지 않고, 우리 서버를 거칠까?

브라우저 JS에서 바로 외부 날씨 API를 호출해도 기술적으론 가능합니다. 그런데도 보통 아래처럼 **백엔드가 중간에서 대신 호출(프록시)** 하는 구조를 많이 씁니다.

```
[예전 방식]  브라우저 ──────────────────────────► 외부 API
[이번 방식]  브라우저 ──► 우리 서버(Spring Boot) ──► 외부 API
```

| 이유 | 설명 |
|---|---|
| **API 키 은닉** | 많은 외부 API는 키(비밀값)가 필요합니다. 프론트엔드 JS 코드에 키를 넣으면 브라우저 개발자 도구에서 누구나 볼 수 있습니다. 서버에서만 키를 사용하면 클라이언트에 노출되지 않습니다. |
| **CORS 문제 회피** | 외부 API가 브라우저의 직접 호출(CORS)을 막아둔 경우가 많습니다. 서버끼리의 호출은 이 제약을 받지 않습니다. |
| **응답 가공/정리** | 외부 API의 응답 형식이 복잡하거나 우리 서비스에 필요 없는 정보가 많을 때, 서버에서 필요한 형태로 가공해서 내려줄 수 있습니다. |
| **여러 API 조합** | 이번 실습처럼 "도시 이름 → 위경도 변환"과 "위경도 → 날씨 조회"처럼 **여러 외부 API를 순서대로 호출**해서 하나의 결과로 합치는 로직은 서버에 있는 게 자연스럽습니다. |

이번 실습에서 사용하는 [Open-Meteo](https://open-meteo.com)는 회원가입/API 키 없이 무료로 쓸 수 있는 날씨 API라 키 은닉을 직접 시연하긴 어렵지만, **여러 외부 API를 조합하는 패턴**과 **API 키가 필요한 서비스였다면 어떻게 감췄을지**는 아래에서 함께 설명합니다.

---

## 2. 외부 API 확인

먼저 실제로 curl로 확인한 외부 API 응답입니다.

**① 지오코딩(도시 이름 → 위경도)**
```bash
curl "https://geocoding-api.open-meteo.com/v1/search?name=Seoul&count=1&language=ko&format=json"
```
```json
{"results":[{"id":1835848,"name":"서울특별시","latitude":37.566,"longitude":126.9784, ... ,"country":"대한민국", ...}]}
```

**② 날씨 조회(위경도 → 현재 날씨)**
```bash
curl "https://api.open-meteo.com/v1/forecast?latitude=37.5665&longitude=126.9780&current=temperature_2m,wind_speed_10m,weather_code,relative_humidity_2m&timezone=Asia/Seoul"
```
```json
{"current":{"time":"2026-09-10T20:30","temperature_2m":19.3,"wind_speed_10m":3.3,"weather_code":0,"relative_humidity_2m":68}}
```

우리 서버는 ①로 도시 이름을 좌표로 바꾸고, 그 좌표로 ②를 호출해서 현재 날씨를 가져온 뒤, 하나의 응답으로 합쳐서 클라이언트에 내려줍니다.

> 참고로 존재하지 않는 도시를 검색하면 `results` 필드 자체가 응답에서 빠집니다(`{"generationtime_ms":...}`만 옴). 이 경우를 "도시를 찾을 수 없음"으로 처리해야 합니다.

---

## 3. 의존성 추가 — `RestClient`

Spring Boot 4.x에서는 외부 HTTP API를 호출하는 동기(synchronous) 클라이언트로 **`RestClient`** 를 권장합니다 (예전의 `RestTemplate`은 유지보수 모드로 전환됨).

```groovy
implementation 'org.springframework.boot:spring-boot-starter-restclient'
```

---

## 4. 외부 API 응답을 받을 DTO

외부 API가 내려주는 JSON은 필드가 많고 스네이크 케이스(`temperature_2m`)를 쓰기 때문에, 우리가 실제로 쓸 필드만 골라 별도의 record로 받습니다.

```java
// client/GeocodingResponse.java
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeocodingResponse(List<GeocodingResult> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodingResult(String name, double latitude, double longitude, String country) {
    }
}
```

```java
// client/ForecastResponse.java
@JsonIgnoreProperties(ignoreUnknown = true)
public record ForecastResponse(Current current) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Current(
            String time,
            @JsonProperty("temperature_2m") double temperature,
            @JsonProperty("wind_speed_10m") double windSpeed,
            @JsonProperty("weather_code") int weatherCode,
            @JsonProperty("relative_humidity_2m") int humidity
    ) {
    }
}
```

| 요소 | 설명 |
|---|---|
| `@JsonIgnoreProperties(ignoreUnknown = true)` | 응답에 우리가 정의하지 않은 필드(`elevation`, `timezone` 등)가 더 있어도 무시하고 무시하도록. 외부 API 응답을 다룰 때는 항상 붙여주는 게 안전합니다 (API가 필드를 추가해도 우리 코드가 깨지지 않음) |
| `@JsonProperty("temperature_2m")` | JSON의 스네이크 케이스 필드 이름과 우리 코드의 camelCase 필드 이름을 매핑 |
| `List<GeocodingResult> results` | 도시를 못 찾으면 이 필드 자체가 응답에서 빠지므로, 역직렬화 후 `null`이 될 수 있음 (아래 Service에서 처리) |

---

## 5. 외부 API를 호출하는 클라이언트

```java
// client/OpenMeteoClient.java
@Component
public class OpenMeteoClient {

    private final RestClient restClient;

    public OpenMeteoClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public GeocodingResponse geocode(String city) {
        URI uri = UriComponentsBuilder.fromUriString("https://geocoding-api.open-meteo.com/v1/search")
                .queryParam("name", city)
                .queryParam("count", 1)
                .queryParam("language", "ko")
                .queryParam("format", "json")
                .build()
                .toUri();

        return restClient.get().uri(uri).retrieve().body(GeocodingResponse.class);
    }

    public ForecastResponse forecast(double latitude, double longitude) {
        URI uri = UriComponentsBuilder.fromUriString("https://api.open-meteo.com/v1/forecast")
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", "temperature_2m,wind_speed_10m,weather_code,relative_humidity_2m")
                .queryParam("timezone", "Asia/Seoul")
                .build()
                .toUri();

        return restClient.get().uri(uri).retrieve().body(ForecastResponse.class);
    }
}
```

### 코드 설명
| 요소 | 설명 |
|---|---|
| `RestClient.Builder` | Spring Boot가 자동 설정해서 주입해주는 빌더. `.build()`로 실제 클라이언트를 만듦 |
| `UriComponentsBuilder` | 쿼리 파라미터를 문자열로 직접 이어붙이지 않고, 각 값을 안전하게 URL 인코딩해서 URI를 조립해주는 Spring 유틸리티. 도시 이름에 특수문자나 한글이 들어가도 안전 |
| `.get().uri(uri).retrieve().body(Xxx.class)` | `RestClient`의 기본 사용 패턴: GET 요청 → 응답을 지정한 타입으로 역직렬화 |

`@Component`로 등록해서, 이 클라이언트를 여러 서비스에서도 재사용할 수 있게 만들었습니다 (지금은 `WeatherService`에서만 쓰지만).

---

## 6. 두 API를 조합하는 Service

```java
@Service
public class WeatherService {

    private final OpenMeteoClient openMeteoClient;

    public WeatherResponse getWeather(String city) {
        try {
            GeocodingResponse geocoding = openMeteoClient.geocode(city);
            if (geocoding.results() == null || geocoding.results().isEmpty()) {
                throw new CityNotFoundException(city);
            }
            GeocodingResponse.GeocodingResult location = geocoding.results().get(0);

            ForecastResponse forecast = openMeteoClient.forecast(location.latitude(), location.longitude());
            ForecastResponse.Current current = forecast.current();

            return new WeatherResponse(
                    location.name(), location.country(),
                    current.temperature(), current.humidity(), current.windSpeed(),
                    describeWeatherCode(current.weatherCode()), current.time()
            );
        } catch (RestClientException e) {
            throw new WeatherServiceUnavailableException();
        }
    }

    private String describeWeatherCode(int code) {
        if (code == 0) return "맑음";
        if (code == 1 || code == 2) return "대체로 맑음";
        if (code == 3) return "흐림";
        // ... (WMO 날씨 코드를 한글 설명으로 변환)
        return "알 수 없음";
    }
}
```

- **① 지오코딩 → ② 날씨 조회를 순서대로 호출**하는 조합 로직이 Service에 있습니다. 컨트롤러는 이 복잡한 과정을 전혀 몰라도 됩니다.
- `results`가 비어있으면(=도시를 못 찾으면) `CityNotFoundException`을 던집니다.
- 외부 API 호출 중 네트워크 오류, 타임아웃, 5xx 응답 등은 모두 `RestClientException`(과 그 하위 타입)으로 던져지는데, 이를 잡아서 우리 서비스의 언어로 `WeatherServiceUnavailableException`으로 바꿔줍니다. **외부 서비스가 죽어도 우리 API 사용자에게는 우리만의 일관된 에러 형식으로 알려주는 것**이 포인트입니다.
- `weatherCode`(WMO 날씨 코드, 정수)를 한글 설명으로 바꾸는 것도 서버의 역할로 뒀습니다 — 이러면 프론트엔드는 숫자 코드의 의미를 몰라도 되고, 나중에 다른 프론트엔드(예: 모바일 앱)를 추가해도 이 로직을 재사용할 수 있습니다.

---

## 7. Controller

```java
@RestController
@RequestMapping("/weather")
public class WeatherController {

    @GetMapping
    public WeatherResponse getWeather(@RequestParam String city) {
        return weatherService.getWeather(city);
    }
}
```
`/weather?city=Seoul` 형태로 호출합니다. 로그인 여부와 무관하게 누구나 쓸 수 있는 기능이라 `SecurityConfig`의 `permitAll` 목록에 `/weather/**`를 추가했습니다.

---

## 8. 실행 & 실제로 검증한 내용

```powershell
cd demo
.\gradlew.bat bootRun
```

```bash
curl "http://localhost:8080/weather?city=Seoul"
# → {"city":"서울특별시","country":"대한민국","temperature":19.3,"humidity":68,"windSpeed":3.3,"description":"맑음","observedAt":"2026-09-10T20:30"}

curl "http://localhost:8080/weather?city=Tokyo"
# → {"city":"도쿄","country":"일본","temperature":18.9,...,"description":"대체로 맑음",...}

curl "http://localhost:8080/weather?city=%EB%B6%80%EC%82%B0"   # "부산"을 URL 인코딩
# → {"city":"Pusan","country":"대한민국",...}   (Open-Meteo가 보유한 지명 표기라 로마자로 나올 수 있음)

curl "http://localhost:8080/weather?city=asdfqwerzzzznotacity"
# → 404 {"message":"도시를 찾을 수 없습니다: asdfqwerzzzznotacity"}
```
모두 실제 외부 API를 거쳐 정상적으로 동작하는 것을 확인했습니다.

---

## 9. 실습 중 실제로 만난 문제 — `/error`가 시큐리티에 막혀서 엉뚱한 403이 나옴

**증상**: `city` 파라미터 없이 `/weather`만 호출했더니(`@RequestParam`이 필수라 원래는 **400**이 나와야 정상), 뜬금없이 **403 Forbidden**(본문 없음)이 나왔습니다. `/weather/**`는 분명히 `permitAll`에 들어있는데도요.

**원인 파악**: 로그를 보니 Spring MVC는 정확히 `MissingServletRequestParameterException`을 잡아서 **정상적으로 처리("Resolved")** 하고 있었습니다.
```
WARN ... DefaultHandlerExceptionResolver : Resolved [MissingServletRequestParameterException: Required request parameter 'city' ... is not present]
```
문제는 그다음이었습니다. Spring Boot는 이런 예외를 처리할 때 내부적으로 `/error` 경로로 **다시 포워딩(forward)** 해서 최종 에러 응답(JSON)을 만듭니다. 그런데 Spring Security는 기본적으로 **이 내부 포워딩 요청에도 시큐리티 필터를 다시 적용**합니다. 우리 설정엔 `/error`가 `permitAll` 목록에 없었기 때문에 `anyRequest().authenticated()` 규칙에 걸려서, "인증 안 된 사용자"로 취급되어 403으로 막혀버린 것입니다. 즉, **진짜 문제(파라미터 누락)는 400이어야 했는데, 그 에러를 보여주려는 시도 자체가 보안 규칙에 또 막힌 것**입니다.

**해결**:
```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/error").permitAll()   // ← 추가
        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
        .requestMatchers("/*.html", "/auth/**", "/hello/**", "/posts/**", "/weather/**").permitAll()
        .anyRequest().authenticated())
```

**부수 효과(개선)**: 이 수정 덕분에, 기존에 인증 없이 `/members` 같은 보호된 API를 호출했을 때 **본문 없이 텅 빈 403**만 오던 것도, 이제 아래처럼 **정상적인 JSON 에러 응답**으로 바뀌었습니다.
```json
{"timestamp":"2026-09-10T11:45:32.679Z","status":403,"error":"Forbidden","path":"/members"}
```

**배울 점**: `anyRequest().authenticated()` 같은 "기본적으로 다 막기" 규칙을 쓸 때는, `/error`처럼 **프레임워크가 내부적으로 사용하는 경로**도 빠뜨리지 않았는지 확인해야 합니다. 이건 Spring Security를 쓰는 프로젝트에서 꽤 흔히 발생하는 설정 실수입니다.

---

## 10. 프론트엔드 — `StudyHtmlFile3.html`

```javascript
async function fetchWeather(city) {
    const response = await fetch("/weather?city=" + encodeURIComponent(city));
    const body = await response.json();

    if (!response.ok) {
        throw new Error(body.message || `요청 실패 (HTTP ${response.status})`);
    }
    renderWeather(body);
}
```
- `encodeURIComponent(city)`로 한글/공백이 섞인 도시 이름도 안전하게 쿼리 파라미터로 보냅니다.
- 날씨 설명(`"맑음"`, `"비"` 등) 문자열을 이모지로 매핑해서 카드 형태로 보여줍니다.
- 예시 도시 버튼(Seoul/Tokyo/Paris/New York)을 눌러 바로 조회해볼 수 있게 만들었습니다.

브라우저에서 `http://localhost:8080/StudyHtmlFile3.html`을 열어 도시 이름을 검색해보세요. (이번에도 실제 버튼 클릭/화면은 제가 직접 볼 수 없어 API 레벨 검증과 코드 리뷰로 확인했습니다.)

---

## 11. 테스트 코드에 대한 트레이드오프

```java
@SpringBootTest
@AutoConfigureMockMvc
class WeatherControllerTest {
    // getWeatherForKnownCitySucceeds()   — 실제 Open-Meteo API를 호출해서 서울 날씨 조회 성공
    // getWeatherForUnknownCityFails()    — 존재하지 않는 도시 → 404
    // missingCityParamReturnsBadRequest()— city 파라미터 누락 → 400 (이번에 고친 버그의 회귀 테스트)
}
```

이번 테스트는 **진짜 외부 API를 호출**합니다. 지금까지 이 프로젝트의 다른 테스트들과 일관되게 "실제로 동작하는지"를 검증하는 방식을 따랐지만, 이 방식에는 트레이드오프가 있습니다.

| | 실제 API 호출 (이번 방식) | Mock으로 대체 (`MockRestServiceServer` 등) |
|---|---|---|
| 장점 | 외부 API가 실제로 우리 코드와 잘 맞물려 동작하는지까지 확인 | 네트워크 없이도 항상 빠르고 안정적으로 실행됨 |
| 단점 | 네트워크가 없거나 외부 API가 잠깐 장애나면 테스트도 실패함 | 외부 API의 실제 스펙 변경을 테스트가 못 잡아냄 |

실무에서는 보통 이 둘을 함께 씁니다: 평소 CI에서 도는 테스트는 Mock으로 빠르고 안정적으로, 그리고 가끔(배포 전 등) 실제 API를 호출하는 별도의 테스트를 돌리는 식입니다. 이번 study에서는 학습 목적상 실제 호출 방식을 유지했습니다.

`./gradlew test` 실행 결과, study8에서 추가된 3개를 포함해 **총 23개 테스트 전부 통과**했습니다.

| 테스트 클래스 | 개수 |
|---|---|
| DemoApplicationTests | 1 |
| HelloControllerTest | 3 |
| AuthControllerTest | 5 |
| MemberControllerTest | 3 |
| PostControllerTest | 4 |
| MessageControllerTest | 4 |
| **WeatherControllerTest (신규)** | **3** |

---

## 12. 정리

- 서버가 외부 공개 API를 대신 호출해서 클라이언트에 가공된 데이터를 내려주는 **백엔드 프록시 패턴**을 구현했다
- `RestClient` + `UriComponentsBuilder`로 외부 API를 안전하게 호출하는 방법을 익혔다
- 서로 다른 두 외부 API(지오코딩 → 날씨 조회)를 서비스 계층에서 순서대로 조합하는 로직을 작성했다
- 외부 API 실패(`RestClientException`)를 우리 서비스만의 에러 형식으로 변환해서 클라이언트에는 일관된 에러 응답을 제공했다
- 실습 중 실제로 만난 `Spring Security가 /error 포워딩까지 막아버리는` 문제를 원인 분석부터 해결까지 진행했고, 그 과정에서 기존 API들의 에러 응답 품질도 함께 개선됐다
- 외부 API를 호출하는 테스트를 작성할 때의 트레이드오프(실제 호출 vs Mock)를 정리했다

## 13. 다음 단계

**study9.md**에서는 `StudyHtmlFile4.html`과 함께 **주식(증권) 기능**을 만들어봅니다. 이번 study8의 "외부 API 프록시" 패턴을 이어서 사용하되, 이번엔 여러 종목을 관심 목록(watchlist)으로 우리 DB에 저장하는 기능도 함께 다루며 "외부 실시간 데이터 + 우리 DB에 저장된 데이터"를 조합하는 방법을 배워볼 예정입니다.
