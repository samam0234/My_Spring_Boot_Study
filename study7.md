# study7. 고급 2 — 페이지와 검색

게시글이 늘면 한 번에 내려주지 않습니다. 목록은 페이지로 자르고, `q`가 있으면 제목, 본문, 작성자, 태그 이름을 찾습니다.

## 1. 페이지

`GET /posts`의 기본 크기는 5입니다.

```java
@GetMapping
public PageResponse<PostResponse> findAll(
        @RequestParam(required = false) String q,
        @PageableDefault(size = 5) Pageable pageable)
```

`PageResponse` 필드:

| 필드 | 뜻 |
|---|---|
| `content` | 이 페이지의 글 |
| `page` | 0부터 시작하는 번호 |
| `size` | 페이지 크기 |
| `totalElements` | 전체 개수 |
| `totalPages` | 전체 페이지 수 |
| `hasNext` | 다음 페이지가 있는지 |

정렬은 작성 시각 내림차순입니다.

```powershell
curl.exe "http://localhost:8080/posts?page=0&size=5"
```

## 2. 검색

`q`가 비어 있으면 전체 목록입니다. 값이 있으면 아래를 부분 일치로 봅니다.

- 제목, 본문, 작성자
- 태그에 붙은 이름

```powershell
curl.exe "http://localhost:8080/posts?q=jpa&page=0&size=5"
```

태그는 study5의 N:M입니다. 검색은 그 조인을 `EXISTS`로 확인해서, 태그가 여러 개여도 같은 글이 페이지 안에서 중복되지 않게 합니다.

## 3. 여기서 고정한 것

- 목록 API는 `Page`를 그대로 내보내지 않고 `PageResponse`로 바꿉니다.
- 검색어 파라미터 이름은 `q`입니다.
- 정렬 기준은 `createdAt` 내림차순입니다.
