# Study 9 — 주식 시세 & 관심 종목(Watchlist)

study8에서 만든 "외부 API 프록시" 패턴을 이어서, 이번에는 **외부 실시간 데이터(주식 시세)** 와 **우리 DB에 저장된 데이터(로그인한 사용자의 관심 종목 목록)** 를 조합하는 기능을 만듭니다.

- 누구나 종목 시세를 검색할 수 있음 (study8의 날씨처럼 공개 API)
- 로그인한 사용자는 관심 종목을 우리 DB에 저장해두고, 조회할 때마다 최신 시세를 실시간으로 붙여서 보여줌

프론트엔드 파일은 `demo/src/main/resources/static/StudyHtmlFile4.html` 입니다.

---

## 1. 외부 API 조사 — 키 없이 쓸 수 있는 시세 API 찾기

주식 시세 API는 대부분 회원가입 후 API 키가 필요합니다(Alpha Vantage, Finnhub 등). 이번 실습에서는 **Yahoo Finance의 비공식 공개 엔드포인트**를 사용했습니다 — 키가 필요 없지만, 공식 문서가 없는 비공식 API라 언제든 바뀌거나 막힐 수 있다는 점은 감안해야 합니다 (실무라면 공식 유료/무료 API를 쓰는 게 안전합니다).

```bash
curl -A "Mozilla/5.0" "https://query1.finance.yahoo.com/v8/finance/chart/AAPL"
```
```json
{"chart":{"result":[{"meta":{
    "symbol":"AAPL","currency":"USD","longName":"Apple Inc.",
    "regularMarketPrice":315.34,"previousClose":316.22,
    "regularMarketChangePercent":-0.278,
    "regularMarketDayHigh":319.15,"regularMarketDayLow":309.9,
    "regularMarketVolume":64902991
}}]}}
```

> ⚠️ **실습 중 발견한 점**: `-A "Mozilla/5.0"`(User-Agent 헤더) 없이 요청하면 **429 Too Many Requests**가 돌아옵니다. Yahoo가 "브라우저처럼 보이지 않는" 요청을 차단하는 것으로 보입니다. 그래서 우리 서버가 이 API를 호출할 때도 브라우저를 흉내 낸 User-Agent를 직접 설정해줘야 합니다. 많은 공개 API/스크래핑 대상 사이트들이 이런 식으로 기본 User-Agent를 차단하니, 외부 API 연동 시 429/403이 뜨면 헤더부터 의심해보는 게 좋습니다.

존재하지 않는 종목 코드는 HTTP **404**로 응답합니다.
```bash
curl -A "Mozilla/5.0" "https://query1.finance.yahoo.com/v8/finance/chart/ZZZZNOTASTOCK"
# → 404 {"chart":{"result":null,"error":{"code":"Not Found",...}}}
```

---

## 2. 외부 API 클라이언트

```java
// client/YahooFinanceClient.java
@Component
public class YahooFinanceClient {

    private final RestClient restClient;

    public YahooFinanceClient(RestClient.Builder builder) {
        this.restClient = builder
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; SpringBootStudyBot/1.0)")
                .build();
    }

    public YahooChartResponse getChart(String symbol) {
        URI uri = UriComponentsBuilder.fromUriString("https://query1.finance.yahoo.com/v8/finance/chart/{symbol}")
                .buildAndExpand(symbol)
                .toUri();
        return restClient.get().uri(uri).retrieve().body(YahooChartResponse.class);
    }
}
```
- `RestClient.Builder.defaultHeader(...)` — 이 클라이언트로 보내는 **모든 요청에 공통으로 붙는 헤더**를 설정. study8의 `OpenMeteoClient`와 다르게, 이번엔 이렇게 헤더를 커스터마이징해야 했습니다.

```java
// client/YahooChartResponse.java (일부)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Meta(
        String symbol, String currency, String longName, String shortName,
        double regularMarketPrice, double previousClose, double regularMarketChangePercent,
        double regularMarketDayHigh, double regularMarketDayLow, long regularMarketVolume
) {}
```

---

## 3. 404와 그 외 오류를 구분해서 처리하기

```java
// service/StockService.java
public StockResponse getQuote(String symbol) {
    try {
        YahooChartResponse response = yahooFinanceClient.getChart(symbol);
        YahooChartResponse.Meta meta = response.chart().result().get(0).meta();
        return new StockResponse(
                meta.symbol(), meta.longName() != null ? meta.longName() : meta.shortName(), meta.currency(),
                meta.regularMarketPrice(), meta.previousClose(), meta.regularMarketChangePercent(),
                meta.regularMarketDayHigh(), meta.regularMarketDayLow(), meta.regularMarketVolume()
        );
    } catch (HttpClientErrorException.NotFound e) {
        throw new StockNotFoundException(symbol);
    } catch (RestClientException e) {
        throw new StockServiceUnavailableException();
    }
}
```
- `RestClient`는 외부 API가 4xx/5xx를 응답하면 자동으로 예외를 던집니다. `HttpClientErrorException.NotFound`는 그 중 **404 전용** 하위 타입이라, `catch` 순서를 좁은 타입(`NotFound`) → 넓은 타입(`RestClientException`) 순으로 배치해서 **"종목이 없다"(404)** 와 **"그 외 모든 문제"(503)** 를 구분했습니다. study8의 날씨 서비스보다 한 단계 더 세밀하게 오류를 나눈 것입니다.

---

## 4. 관심 종목(Watchlist) — 외부 데이터 + 우리 DB 조합

### 엔티티 & Repository
```java
@Entity
@Table(name = "watchlist_items")
public class WatchlistItem {
    @Id @GeneratedValue Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private LocalDateTime addedAt;
    // ...
}
```
`Message`(study7)와 마찬가지로 `@ManyToOne`으로 `User`와 연관관계를 맺습니다 — "이 관심 종목은 누구 것인가"를 표현합니다.

```java
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findByOwner_UsernameOrderByAddedAtAsc(String username);
    boolean existsByOwner_UsernameAndSymbol(String username, String symbol);
    void deleteByOwner_UsernameAndSymbol(String username, String symbol);
}
```

### Service — DB 목록을 조회한 뒤, 각 종목의 실시간 시세를 붙여서 반환
```java
@Service
public class WatchlistService {

    public void add(String username, String symbol) {
        String normalized = symbol.toUpperCase();
        if (watchlistItemRepository.existsByOwner_UsernameAndSymbol(username, normalized)) {
            throw new DuplicateWatchlistItemException(normalized);
        }
        stockService.getQuote(normalized);   // 존재하지 않는 종목이면 여기서 예외가 던져짐 (fail fast)

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        watchlistItemRepository.save(new WatchlistItem(owner, normalized));
    }

    public List<StockResponse> list(String username) {
        return watchlistItemRepository.findByOwner_UsernameOrderByAddedAtAsc(username).stream()
                .map(item -> stockService.getQuote(item.getSymbol()))   // ← DB의 심볼마다 실시간 시세 조회
                .toList();
    }

    @Transactional
    public void remove(String username, String symbol) {
        watchlistItemRepository.deleteByOwner_UsernameAndSymbol(username, symbol.toUpperCase());
    }
}
```

**핵심은 `list()` 메서드**입니다: 우리 DB에는 "어떤 사용자가 어떤 종목 코드를 관심 등록했는지"만 저장돼 있고, **실제 가격은 저장하지 않습니다**. 조회할 때마다 저장된 심볼 목록을 외부 API에 다시 물어봐서 항상 최신 가격을 보여줍니다. 이게 바로 "외부 실시간 데이터 + 우리 DB 데이터"를 조합하는 방식입니다.

> 참고(한계점): 관심 종목이 N개면 `list()` 호출마다 외부 API를 N번 호출합니다. 종목 수가 많아지면 느려지고, 외부 API 요청 제한에도 더 잘 걸립니다. 실무에서는 짧은 시간 동안 가격을 캐싱(`@Cacheable` 등)하거나, 여러 종목을 한 번에 조회하는 API가 있다면 그걸 쓰는 식으로 최적화합니다. 이번 study는 개념 학습이 목적이라 가장 단순한 방식을 그대로 뒀습니다.

### Controller
```java
@RestController
@RequestMapping("/stocks")
public class StockController {
    @GetMapping("/{symbol}")
    public StockResponse getQuote(@PathVariable String symbol) { ... }   // 공개
}

@RestController
@RequestMapping("/watchlist")
public class WatchlistController {
    @PostMapping   public ResponseEntity<Void> add(@AuthenticationPrincipal UserDetails me, @Valid @RequestBody WatchlistRequest request) { ... }
    @GetMapping    public List<StockResponse> list(@AuthenticationPrincipal UserDetails me) { ... }
    @DeleteMapping("/{symbol}") public ResponseEntity<Void> remove(@AuthenticationPrincipal UserDetails me, @PathVariable String symbol) { ... }
}
```
`/stocks/**`는 `SecurityConfig`의 `permitAll`에 추가해서 로그인 없이 시세 검색이 가능하게 했고, `/watchlist/**`는 추가하지 않아 `anyRequest().authenticated()`에 걸려 **로그인해야만** 쓸 수 있습니다 (study7의 `/messages`와 같은 방식).

---

## 5. 실습 중 실제로 만난 문제 — 커스텀 delete 쿼리 메서드가 트랜잭션 없이 실패함

**증상**: `DELETE /watchlist/AAPL`을 호출하면 **500 Internal Server Error**가 발생했습니다.

**원인 파악**: 서버 로그를 보니 정확한 원인이 나와 있었습니다.
```
jakarta.persistence.TransactionRequiredException:
No EntityManager with actual transaction available for current thread - cannot reliably process 'remove' call
```
`save()`, `findById()` 같은 `JpaRepository`의 **기본 제공 메서드**들은 Spring Data JPA가 내부적으로 이미 `@Transactional`을 붙여놓은 상태라 별다른 설정 없이도 잘 동작합니다. 하지만 제가 직접 선언한 **커스텀 파생 쿼리 메서드**(`deleteByOwner_UsernameAndSymbol`)처럼 **데이터를 변경하는(수정/삭제) 쿼리 메서드**는 이 자동 트랜잭션 처리 대상이 아닙니다. 이걸 호출하는 **Service 메서드 쪽에서 명시적으로 트랜잭션을 열어줘야** 합니다.

**해결**:
```java
@Transactional   // ← 추가
public void remove(String username, String symbol) {
    watchlistItemRepository.deleteByOwner_UsernameAndSymbol(username, symbol.toUpperCase());
}
```

**배울 점**: Spring Data JPA에서 `save`/`find`류는 대체로 트랜잭션 걱정 없이 쓸 수 있지만, **직접 선언한 `deleteBy...`, `updateBy...`류의 변경(mutating) 쿼리 메서드는 호출부에 `@Transactional`이 없으면 런타임에 이런 예외가 납니다.** 이건 실제로 실행해보기 전까지는 컴파일 타임에 전혀 알 수 없는 문제라, 이번처럼 curl로 직접 두드려보며 검증하는 과정이 중요하다는 걸 다시 확인했습니다.

---

## 6. 실행 & 실제로 검증한 내용

```powershell
cd demo
.\gradlew.bat bootRun
```

```bash
# 공개 시세 조회
curl "http://localhost:8080/stocks/AAPL"
# → {"symbol":"AAPL","name":"Apple Inc.","currency":"USD","price":315.34,...}

curl "http://localhost:8080/stocks/005930.KS"    # 삼성전자
# → {"symbol":"005930.KS","name":"Samsung Electronics Co., Ltd.","currency":"KRW",...}

curl "http://localhost:8080/stocks/ZZZZNOTASTOCK"
# → 404 {"message":"종목을 찾을 수 없습니다: ZZZZNOTASTOCK"}

# 관심 종목 (로그인 필요)
curl -X POST /watchlist -H "Authorization: Bearer $TOKEN" -d '{"symbol":"aapl"}'   # → 201 (소문자도 AAPL로 정규화)
curl -X POST /watchlist -H "Authorization: Bearer $TOKEN" -d '{"symbol":"AAPL"}'   # → 409 이미 등록됨
curl -X POST /watchlist -H "Authorization: Bearer $TOKEN" -d '{"symbol":"ZZZZNOTASTOCK"}'  # → 404 존재하지 않는 종목
curl /watchlist -H "Authorization: Bearer $TOKEN"
# → [{"symbol":"AAPL",...,"price":315.34,...}, {"symbol":"MSFT",...}]   (실시간 가격 포함)
curl -X DELETE /watchlist/AAPL -H "Authorization: Bearer $TOKEN"   # → 204
curl /watchlist -H "Authorization: Bearer $TOKEN"                 # → AAPL 빠지고 MSFT만 남음
```
모두 실제로 실행해서 확인했습니다.

---

## 7. 프론트엔드 — `StudyHtmlFile4.html`

- **종목 검색**: 누구나 심볼(`AAPL`, `005930.KS` 등)을 입력하면 실시간 시세 카드가 나타납니다.
- **관심 종목 추가**: 검색 결과 카드의 "관심 종목에 추가" 버튼은 로그인 토큰(`localStorage`, study7과 동일한 방식)이 있어야 동작합니다.
- **내 관심 종목**: 로그인 상태면 등록된 종목들을 실시간 가격과 함께 목록으로 보여주고, 개별 삭제 버튼을 제공합니다.
- 로그인 여부에 따라 관심 종목 섹션과 "로그인이 필요합니다" 안내를 토글합니다.

`http://localhost:8080/StudyHtmlFile2.html`(study7)에서 먼저 로그인하면, **같은 브라우저의 `localStorage`를 공유**하므로 `StudyHtmlFile4.html`에서 로그인 없이 바로 관심 종목 기능을 쓸 수 있습니다. (이 부분도 실제 버튼 클릭은 직접 확인 부탁드립니다 — 저는 API 레벨 검증과 코드 리뷰로 확인했습니다.)

---

## 8. 테스트 코드

```java
// StockControllerTest — 실제 Yahoo Finance API 호출
class StockControllerTest {
    // getQuoteForKnownSymbolSucceeds(), getQuoteForUnknownSymbolFails()
}

// WatchlistControllerTest — 인증 + DB + 실제 외부 API가 모두 얽힌 통합 테스트
class WatchlistControllerTest {
    // requestWithoutTokenIsRejected()
    // addListAndRemoveFlow()          — 추가→조회→삭제→삭제 후 빈 목록 확인 (이번에 고친 버그의 회귀 테스트)
    // addDuplicateFails()             — 중복 추가 409
    // addInvalidSymbolFails()         — 존재하지 않는 종목 404
}
```

`./gradlew test` 실행 결과, study9에서 추가된 6개를 포함해 **총 29개 테스트 전부 통과**했습니다.

| 테스트 클래스 | 개수 |
|---|---|
| DemoApplicationTests | 1 |
| HelloControllerTest | 3 |
| AuthControllerTest | 5 |
| MemberControllerTest | 3 |
| PostControllerTest | 4 |
| MessageControllerTest | 4 |
| WeatherControllerTest | 3 |
| **StockControllerTest (신규)** | **2** |
| **WatchlistControllerTest (신규)** | **4** |

---

## 9. 정리

- study8에 이어 외부 API 프록시 패턴을 한 단계 더 발전시켜, **404와 기타 오류를 구분해서 처리**하는 방법을 익혔다
- `@ManyToOne` 연관관계로 "사용자별 관심 종목"을 DB에 저장하고, **저장된 목록 + 실시간 외부 데이터**를 조합해서 응답하는 구조를 만들었다
- 커스텀 `deleteBy...` 파생 쿼리 메서드에는 `@Transactional`이 필요하다는, 실제로 겪지 않으면 알기 어려운 Spring Data JPA의 동작 방식을 직접 확인했다
- 외부 API가 User-Agent 헤더 유무로 요청을 차단할 수 있다는 점과, `RestClient.Builder.defaultHeader(...)`로 공통 헤더를 설정하는 방법을 익혔다
- 실시간 데이터를 매번 다시 조회하는 방식의 한계(N번 외부 호출)와, 실무에서 흔히 쓰는 개선 방향(캐싱)을 짚어봤다

## 10. 다음 단계

**study10.md**에서는 `StudyHtmlFile5.html`과 함께, 지금까지 다루지 않은 **웹에서 자주 쓰이는 기술**을 다뤄볼 예정입니다. 후보로는 페이지네이션(목록 API에 `Pageable` 적용), 파일 업로드, 실시간 알림(WebSocket/SSE), API 문서화(Springdoc/Swagger) 등이 있습니다 — 어떤 주제로 진행할지는 다음에 말씀해주시면 그에 맞춰 진행하겠습니다.
