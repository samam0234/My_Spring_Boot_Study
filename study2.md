# Study 2 — Spring Data JPA로 DB 연동 & CRUD API 만들기

[study1.md](study1.md)에서 만든 `demo` 프로젝트에 이어서, 실제 데이터베이스와 연동되는 **Member(회원) CRUD REST API**를 만들어봅니다.

이번 실습에서는 별도의 DB 서버 설치 없이 실습이 가능하도록 **H2 인메모리 데이터베이스**를 사용합니다 (애플리케이션이 꺼지면 데이터도 함께 사라지는 가벼운 DB — 학습/테스트용으로 널리 쓰임).

---

## 1. 의존성 추가 (`build.gradle`)

```groovy
dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-webmvc'
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'   // ← 추가
	runtimeOnly 'com.h2database:h2'                                          // ← 추가
	testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

| 의존성 | 역할 |
|---|---|
| `spring-boot-starter-data-jpa` | JPA(ORM), Hibernate, Spring Data 관련 라이브러리를 묶어서 제공 |
| `com.h2database:h2` | H2 인메모리 데이터베이스 드라이버. `runtimeOnly`이므로 컴파일에는 필요 없고 실행 시점에만 필요 |

---

## 2. 설정 파일 (`application.properties`)

```properties
spring.application.name=demo

# H2 in-memory database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# H2 web console: http://localhost:8080/h2-console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

| 속성 | 설명 |
|---|---|
| `spring.datasource.url` | 연결할 DB 주소. `jdbc:h2:mem:testdb`는 이름이 `testdb`인 **메모리 DB**를 의미 |
| `spring.jpa.hibernate.ddl-auto` | 엔티티 클래스를 기준으로 테이블을 어떻게 관리할지 결정. `update`는 변경분만 반영(학습용으로 편함, 운영 환경에서는 보통 `validate`나 `none` + 마이그레이션 도구 사용) |
| `spring.jpa.show-sql` | Hibernate가 실행하는 SQL을 콘솔에 출력 (디버깅에 유용) |
| `spring.h2.console.enabled` | 브라우저로 DB를 직접 조회할 수 있는 H2 콘솔 활성화 |

---

## 3. Entity(엔티티) 만들기

`src/main/java/com/example/demo/domain/Member.java`

```java
package com.example.demo.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    protected Member() {
    }

    public Member(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
```

### 코드 설명
| 요소 | 설명 |
|---|---|
| `@Entity` | 이 클래스가 DB 테이블(`member`)과 매핑됨을 표시 |
| `@Id` | 기본 키(Primary Key) 필드 지정 |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | id 값을 DB가 자동으로 채번(auto-increment)하도록 위임 |
| `protected Member()` | JPA는 리플렉션으로 객체를 생성하기 위해 **파라미터 없는 생성자**가 반드시 필요함. 외부에서 함부로 못 쓰게 `protected`로 제한 |

> ⚠️ Spring Boot 3.x부터는 `javax.persistence.*`가 아니라 **`jakarta.persistence.*`** 를 사용합니다. Java EE가 Jakarta EE로 이름이 바뀌면서 패키지 경로도 바뀌었습니다.

---

## 4. Repository 만들기

`src/main/java/com/example/demo/repository/MemberRepository.java`

```java
package com.example.demo.repository;

import com.example.demo.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
```

- `JpaRepository<Member, Long>` 를 상속만 해도 `save`, `findAll`, `findById`, `deleteById`, `existsById` 등 **기본 CRUD 메서드를 자동으로 제공**받습니다. 구현체를 직접 작성하지 않아도 Spring이 런타임에 만들어줍니다.
- 제네릭의 `Member`는 다룰 엔티티 타입, `Long`은 그 엔티티의 PK(`id`) 타입입니다.

---

## 5. Service 만들기

`src/main/java/com/example/demo/service/MemberService.java`

```java
package com.example.demo.service;

import com.example.demo.domain.Member;
import com.example.demo.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member create(Member member) {
        return memberRepository.save(member);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Optional<Member> findById(Long id) {
        return memberRepository.findById(id);
    }

    public Optional<Member> update(Long id, Member updated) {
        return memberRepository.findById(id)
                .map(member -> {
                    member.setName(updated.getName());
                    member.setEmail(updated.getEmail());
                    return memberRepository.save(member);
                });
    }

    public boolean delete(Long id) {
        if (!memberRepository.existsById(id)) {
            return false;
        }
        memberRepository.deleteById(id);
        return true;
    }
}
```

- `update()`는 먼저 `findById`로 기존 회원을 찾고, 존재하면(`map`) 값을 바꾼 뒤 다시 `save()`로 저장합니다. 존재하지 않으면 `Optional.empty()`가 그대로 반환됩니다.
- `Optional<T>`을 반환값으로 쓰면 "값이 없을 수도 있다"는 것을 타입으로 표현할 수 있어서, 호출하는 쪽(Controller)에서 null 체크를 강제로 하게 만들 수 있습니다.

---

## 6. Controller 만들기 (REST API)

`src/main/java/com/example/demo/controller/MemberController.java`

```java
package com.example.demo.controller;

import com.example.demo.domain.Member;
import com.example.demo.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<Member> create(@RequestBody Member member) {
        Member saved = memberService.create(new Member(member.getName(), member.getEmail()));
        return ResponseEntity.status(201).body(saved);
    }

    @GetMapping
    public List<Member> findAll() {
        return memberService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Member> findById(@PathVariable Long id) {
        return memberService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Member> update(@PathVariable Long id, @RequestBody Member member) {
        return memberService.update(id, member)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return memberService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
```

### 코드 설명
| 요소 | 설명 |
|---|---|
| `@RequestMapping("/members")` | 클래스 레벨에 붙이면, 이 컨트롤러의 모든 메서드 URL 앞에 `/members`가 공통으로 붙음 |
| `@RequestBody Member member` | HTTP 요청 본문(JSON)을 `Member` 객체로 자동 변환 |
| `ResponseEntity<T>` | 응답 본문뿐 아니라 **HTTP 상태 코드**까지 직접 제어할 수 있는 타입 (`201 Created`, `404 Not Found` 등) |
| `.map(ResponseEntity::ok).orElseGet(...)` | `Optional`에 값이 있으면 `200 OK`로 감싸고, 없으면 `404 Not Found`를 반환 |

> 참고: 지금은 Entity(`Member`)를 요청/응답에 그대로 사용했습니다. 실무에서는 보통 Entity를 그대로 노출하지 않고 별도의 DTO(Data Transfer Object)로 변환해서 주고받습니다. 이 부분은 **study3.md**에서 다룰 예정입니다.

---

## 7. 실행 & 전체 CRUD 테스트

```powershell
cd demo
.\gradlew.bat bootRun
```

서버가 뜬 상태에서, 아래 순서로 curl 요청을 보내며 CRUD 전체 흐름을 확인합니다.
아래 결과는 이번 실습 환경에서 **실제로 실행해서 확인한 결과**입니다.

### 1) CREATE — 회원 2명 등록
```bash
curl -X POST http://localhost:8080/members \
  -H "Content-Type: application/json" \
  -d '{"name":"Chulsoo","email":"chulsoo@example.com"}'
# → {"name":"Chulsoo","email":"chulsoo@example.com","id":1}

curl -X POST http://localhost:8080/members \
  -H "Content-Type: application/json" \
  -d '{"name":"Younghee","email":"younghee@example.com"}'
# → {"name":"Younghee","email":"younghee@example.com","id":2}
```

### 2) READ — 전체 조회
```bash
curl http://localhost:8080/members
# → [{"name":"Chulsoo",...,"id":1},{"name":"Younghee",...,"id":2}]
```

### 3) READ — 단건 조회
```bash
curl http://localhost:8080/members/1
# → {"name":"Chulsoo","email":"chulsoo@example.com","id":1}
```

### 4) UPDATE — 수정
```bash
curl -X PUT http://localhost:8080/members/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Chulsoo Kim","email":"chulsoo.kim@example.com"}'
# → {"name":"Chulsoo Kim","email":"chulsoo.kim@example.com","id":1}
```

### 5) DELETE — 삭제
```bash
curl -X DELETE http://localhost:8080/members/2
# → 204 No Content

curl http://localhost:8080/members/2
# → 404 Not Found  (삭제되었으므로 조회 안 됨)
```

### 6) 최종 상태 확인
```bash
curl http://localhost:8080/members
# → [{"name":"Chulsoo Kim","email":"chulsoo.kim@example.com","id":1}]
```

---

## 8. H2 콘솔로 DB 직접 확인하기

서버가 실행된 상태에서 브라우저로 `http://localhost:8080/h2-console` 접속 후, JDBC URL에 `application.properties`에 적은 값과 동일하게 입력합니다.

- JDBC URL: `jdbc:h2:mem:testdb`
- User Name: `sa`
- Password: (비워둠)

`Connect`를 누르면 `MEMBER` 테이블에 실제로 저장된 데이터를 SQL로 조회할 수 있습니다.
```sql
SELECT * FROM MEMBER;
```

---

## 9. 정리

- `spring-boot-starter-data-jpa` + H2로 별도 DB 설치 없이 데이터베이스 연동을 실습했다
- `@Entity`, `@Id`, `@GeneratedValue`로 테이블과 매핑되는 클래스를 만들었다
- `JpaRepository`를 상속하는 것만으로 CRUD 메서드를 자동으로 얻었다 (직접 SQL을 작성하지 않음)
- Controller → Service → Repository 계층을 거쳐 실제로 생성/조회/수정/삭제가 되는 REST API를 완성하고, curl로 전 과정을 직접 검증했다

## 10. 다음 단계

**study3.md**에서는:
1. Entity를 그대로 노출하지 않고 **DTO(Request/Response)** 로 분리하는 방법
2. 존재하지 않는 회원을 조회했을 때 등, 예외 상황을 깔끔하게 처리하는 `@ExceptionHandler` / `@ControllerAdvice`
3. `@Valid`를 이용한 입력값 검증 (예: 이메일 형식, 이름 필수 입력)

을 다뤄봅니다.
