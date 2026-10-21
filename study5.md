# study5. 실습 5 — 연관 관계 1:N, N:M

Notion에 남아 있던 항목이 엔티티 관계입니다. 이 저장소에는 두 가지가 있습니다.

- **1:N.** 사용자 한 명의 쪽지가 여러 개. 사용자 한 명의 관심종목이 여러 개.
- **N:M.** 게시글 여러 개와 태그 여러 개. 가운데 테이블은 `post_tags`.

## 1. 1:N

`Message`는 보낸 사람과 받는 사람을 `User`로 가리킵니다.

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "sender_id", nullable = false)
private User sender;
```

`WatchlistItem.owner`도 같은 모양입니다. 다수 쪽이 외래 키를 가집니다. `User` 쪽에 컬렉션을 억지로 달지 않았습니다. API는 "이 사용자의 받은 쪽지"처럼 다수 쪽에서 조회합니다.

지연 로딩이라서 응답을 만들 때 세션이 필요합니다. Spring Boot 기본값인 Open Session in View로 컨트롤러에서 `message.getSender().getUsername()`이 동작합니다.

## 2. N:M

`Post.tags`와 `Tag.posts`입니다.

```java
@ManyToMany
@JoinTable(
        name = "post_tags",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
private Set<Tag> tags = new LinkedHashSet<>();
```

태그는 이름으로 재사용합니다. 같은 이름이면 새 행을 만들지 않고 `TagRepository.findByName`으로 찾습니다.

글 생성 본문에 넣을 수 있습니다.

```json
{
  "title": "계층",
  "content": "Controller와 Service를 나눈다.",
  "author": "민아",
  "tags": ["jpa", "계층"]
}
```

태그를 생략하면 빈 목록입니다. 수정 본문의 `tags`를 생략하면 기존 태그를 유지합니다. 빈 배열을 보내면 태그를 지웁니다.

응답의 `tags`는 이름 문자열 배열입니다. `Tag` 엔티티 전체를 내보내지 않습니다.

## 3. 확인

```powershell
curl.exe -X POST http://localhost:8080/posts `
  -H "Content-Type: application/json" `
  -d "{\"title\":\"관계\",\"content\":\"N대M\",\"author\":\"민아\",\"tags\":[\"jpa\"]}"
```

응답 JSON에 `"tags":["jpa"]`가 있으면 조인 테이블까지 저장된 것입니다.

## 4. 여기서 고정한 것

- 1:N은 `Message`, `WatchlistItem`의 `@ManyToOne`입니다.
- N:M은 `Post`와 `Tag`의 `@ManyToMany`와 `post_tags`입니다.
- API는 태그 이름만 주고받습니다.
