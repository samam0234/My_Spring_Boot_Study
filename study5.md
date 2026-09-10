# Study 5 — 테스트 코드 작성 (`@WebMvcTest`, `@SpringBootTest`, `MockMvc`)

지금까지는 매번 서버를 직접 `bootRun`으로 띄우고 curl로 확인했습니다. 이번 실습에서는 그 과정을 **자동화된 테스트 코드**로 대체합니다. 테스트 코드가 있으면 코드를 고칠 때마다 서버를 수동으로 켜고 curl을 다시 치지 않아도, `./gradlew test` 한 번으로 이전에 확인했던 모든 시나리오가 여전히 통과하는지 검증할 수 있습니다.

---

## 1. 테스트의 종류

| 구분 | 범위 | 이번 실습에서 사용하는 어노테이션 |
|---|---|---|
| **슬라이스(Slice) 테스트** | 애플리케이션의 특정 계층만 가볍게 띄움 (예: 웹 계층만) | `@WebMvcTest` |
| **통합(Integration) 테스트** | 실제 애플리케이션과 거의 동일하게 전체 스프링 컨텍스트를 띄움 (DB, 시큐리티 필터 포함) | `@SpringBootTest` |

- 슬라이스 테스트는 빠르지만, 컨트롤러가 실제 DB/보안 설정과 잘 맞물려 동작하는지는 보장하지 못합니다.
- 통합 테스트는 느리지만(컨텍스트를 통째로 띄우므로), 실제 동작에 훨씬 가깝습니다.
- 그래서 보통 **둘을 함께** 사용합니다: 간단한 컨트롤러 로직은 슬라이스 테스트로, 인증/DB가 얽힌 핵심 시나리오는 통합 테스트로.

### 테스트 코드의 기본 구조 — Given / When / Then
테스트 코드는 보통 3단계로 나눠서 생각합니다.
```
Given (준비) — 테스트에 필요한 데이터/상태를 만든다
When  (실행) — 실제로 테스트하고 싶은 동작을 실행한다
Then  (검증) — 결과가 기대한 대로인지 확인한다
```
이번 실습 코드에도 이 구조가 그대로 녹아 있습니다.

---

## 2. `@WebMvcTest` — 슬라이스 테스트로 `HelloController` 검증

`src/test/java/com/example/demo/controller/HelloControllerTest.java`
```java
package com.example.demo.controller;

import com.example.demo.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = HelloController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void hello() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, Spring Boot!"));
    }

    @Test
    void helloByQueryParam() throws Exception {
        mockMvc.perform(get("/hello/name").param("name", "철수"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, 철수! (query param)"));
    }

    @Test
    void helloByPathVariable() throws Exception {
        mockMvc.perform(get("/hello/영희"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, 영희! (path variable)"));
    }
}
```

### 코드 설명
| 요소 | 설명 |
|---|---|
| `@WebMvcTest(controllers = HelloController.class)` | `HelloController`와 웹 계층(디스패처 서블릿, `@ExceptionHandler` 등)만 가볍게 띄움. `@Service`, `@Repository` 같은 나머지 빈들은 로드하지 않음 → DB 연결도 필요 없음 |
| `MockMvc` | 실제로 톰캣을 띄우지 않고, HTTP 요청/응답을 흉내 내서 컨트롤러를 테스트하는 도구 |
| `mockMvc.perform(get("/hello"))` | `GET /hello` 요청을 시뮬레이션 |
| `.andExpect(status().isOk())` | 응답 상태 코드가 200인지 검증 |
| `.andExpect(content().string("..."))` | 응답 본문이 정확히 이 문자열인지 검증 |

---

## 3. 실습 중 실제로 만난 문제 ① — 슬라이스 테스트에서 시큐리티 필터 때문에 컨텍스트가 안 뜸

처음에는 `excludeFilters` 없이 아래처럼만 작성했습니다.
```java
@WebMvcTest(HelloController.class)
@AutoConfigureMockMvc(addFilters = false)
class HelloControllerTest { ... }
```

실행해보니 3개 테스트가 전부 실패했습니다. 에러 로그를 보니 원인은:
```
UnsatisfiedDependencyException: Error creating bean with name 'jwtAuthenticationFilter':
No qualifying bean of type 'com.example.demo.security.JwtTokenProvider' available
```

**원인**: `@WebMvcTest`는 `@Service`, `@Repository` 같은 빈은 로드하지 않지만, **`Filter` 타입의 빈(`@Component`가 붙은 `JwtAuthenticationFilter`)은 예외적으로 자동 스캔 대상에 포함**됩니다. 그런데 `JwtAuthenticationFilter`가 생성자에서 필요로 하는 `JwtTokenProvider`, `CustomUserDetailsService`는 (일반 `@Service`/`@Component`라서) 이 좁은 컨텍스트에 없으므로, `JwtAuthenticationFilter` 빈을 만들다가 실패한 것입니다.

`@AutoConfigureMockMvc(addFilters = false)`는 "MockMvc가 필터를 **실행**하지 않게" 만들어줄 뿐, 스프링 컨텍스트가 그 필터 **빈 자체를 만드는 것**까지 막아주지는 않습니다. 그래서 컨텍스트 생성 단계에서부터 실패했습니다.

**해결**: `excludeFilters`로 `JwtAuthenticationFilter`를 아예 컴포넌트 스캔 대상에서 제외했습니다.
```java
@WebMvcTest(
        controllers = HelloController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
```

**배울 점**: 슬라이스 테스트는 "가볍다"고 생각하기 쉽지만, 프로젝트에 시큐리티 필터처럼 전역적으로 동작하는 컴포넌트가 있으면 슬라이스 테스트에도 예상치 못하게 끼어들 수 있습니다. 에러 메시지의 `Caused by` 체인을 끝까지 따라가서 **진짜 원인이 되는 빈**을 찾아내는 습관이 중요합니다.

---

## 4. `@SpringBootTest` + `MockMvc` — `AuthController` 통합 테스트

회원가입/로그인은 DB(`UserRepository`)와 Spring Security 인증 로직이 실제로 맞물려 동작해야 의미가 있으므로, 슬라이스가 아니라 **전체 컨텍스트를 띄우는 통합 테스트**로 작성합니다.

`src/test/java/com/example/demo/controller/AuthControllerTest.java`
```java
package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.SignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signupSucceeds() throws Exception {
        SignupRequest request = new SignupRequest("tester1", "1234");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void signupWithDuplicateUsernameFails() throws Exception {
        String json = objectMapper.writeValueAsString(new SignupRequest("tester2", "1234"));

        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 존재하는 아이디입니다: tester2"));
    }

    @Test
    void signupWithTooShortFieldsFails() throws Exception {
        SignupRequest request = new SignupRequest("ab", "12");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        String signupJson = objectMapper.writeValueAsString(new SignupRequest("tester3", "1234"));
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signupJson))
                .andExpect(status().isCreated());

        String loginJson = objectMapper.writeValueAsString(new LoginRequest("tester3", "wrong"));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSucceedsAndReturnsToken() throws Exception {
        String signupJson = objectMapper.writeValueAsString(new SignupRequest("tester4", "1234"));
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signupJson))
                .andExpect(status().isCreated());

        String loginJson = objectMapper.writeValueAsString(new LoginRequest("tester4", "1234"));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
```

### 코드 설명
| 요소 | 설명 |
|---|---|
| `@SpringBootTest` | 실제 애플리케이션처럼 **전체 스프링 컨텍스트**(DB, 시큐리티 필터 포함)를 띄움 |
| `@AutoConfigureMockMvc` | `@SpringBootTest`에 진짜 서버(포트 열기) 대신 `MockMvc`를 함께 사용할 수 있도록 설정 |
| `@Transactional` | 각 테스트 메서드를 트랜잭션으로 감싸고, **테스트가 끝나면 자동으로 롤백**. 그래서 `tester1`, `tester2`로 가입해도 다음 테스트 실행 시 DB에 남아있지 않아 서로 영향을 주지 않음 ([study0.md](study0.md)에서 소개한 개념을 실제로 활용) |
| `objectMapper.writeValueAsString(request)` | DTO(record) 객체를 JSON 문자열로 직렬화해서 요청 본문에 넣음 |
| `jsonPath("$.message").value(...)` | JSON 응답 본문에서 `message` 필드 값을 꺼내 비교 (`$`는 JSON 최상위를 의미) |

각 테스트가 study4에서 curl로 직접 확인했던 시나리오와 정확히 대응됩니다: 정상 가입 → 중복 가입(409) → 검증 실패(400) → 잘못된 로그인(401) → 정상 로그인(토큰 발급).

---

## 5. `@SpringBootTest` + `MockMvc` — 인증이 필요한 `MemberController` 테스트

`/members` API는 토큰이 있어야 호출할 수 있으므로, 테스트에서도 **토큰을 직접 발급받아 헤더에 넣어야** 합니다. 회원가입 API를 거쳐도 되지만, 여기서는 테스트를 더 단순하게 만들기 위해 **Repository와 `JwtTokenProvider`를 직접 사용해서 테스트용 사용자와 토큰을 준비**합니다.

`src/test/java/com/example/demo/controller/MemberControllerTest.java`
```java
package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.dto.MemberRequest;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String token;

    @BeforeEach
    void setUp() {
        // Given: 인증에 사용할 사용자를 미리 만들어두고, 그 사용자 명의의 토큰을 직접 발급
        userRepository.save(new User("member-test-user", passwordEncoder.encode("1234")));
        token = jwtTokenProvider.createToken("member-test-user");
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/members"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createFindUpdateDeleteFlow() throws Exception {
        String createJson = objectMapper.writeValueAsString(new MemberRequest("Chulsoo", "chulsoo@example.com"));

        String response = mockMvc.perform(post("/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Chulsoo"))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/members/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("chulsoo@example.com"));

        String updateJson = objectMapper.writeValueAsString(new MemberRequest("Chulsoo Kim", "chulsoo.kim@example.com"));
        mockMvc.perform(put("/members/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chulsoo Kim"));

        mockMvc.perform(delete("/members/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/members/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("회원을 찾을 수 없습니다. id=" + id));
    }

    @Test
    void createWithInvalidEmailFails() throws Exception {
        String invalidJson = objectMapper.writeValueAsString(new MemberRequest("Younghee", "not-an-email"));

        mockMvc.perform(post("/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
```

### 코드 설명
| 요소 | 설명 |
|---|---|
| `@BeforeEach` | 각 테스트 메서드 실행 **직전**마다 호출됨. 여기서는 "인증된 사용자와 토큰"이라는 공통 준비 작업을 모아둠 |
| `jwtTokenProvider.createToken("member-test-user")` | 실제로 `/auth/login`을 호출하지 않고, study4에서 만든 `JwtTokenProvider` 빈을 테스트 코드에서 직접 주입받아 토큰을 생성 — 테스트를 더 짧고 목적에 집중되게 만들어줌 |
| `.header("Authorization", "Bearer " + token)` | study4에서 curl로 했던 `-H "Authorization: Bearer $TOKEN"`과 동일한 역할 |
| `createFindUpdateDeleteFlow()` | study2/3에서 curl로 검증했던 생성→조회→수정→삭제→삭제 후 조회(404) 흐름을 테스트 코드 하나로 그대로 재현 |

---

## 6. 실습 중 실제로 만난 문제 ② — Jackson 3.x로의 전면 변경

테스트 코드에서 JSON 직렬화를 위해 `ObjectMapper`를 쓰려고 처음엔 익숙한 대로 이렇게 썼습니다.
```java
import com.fasterxml.jackson.databind.ObjectMapper;
```
컴파일 오류: `package com.fasterxml.jackson.databind does not exist`.

**원인 파악**: Gradle 의존성 트리를 확인해보니(`./gradlew dependencies --configuration testCompileClasspath`), JSON 라이브러리가 `com.fasterxml.jackson.core:jackson-databind`가 아니라 **`tools.jackson.core:jackson-databind`** 로 잡혀 있었습니다. Spring Framework 7 / Spring Boot 4.x부터 Jackson 라이브러리가 **메이저 버전 3**으로 올라가면서, Maven 좌표(groupId)와 자바 패키지 이름이 `com.fasterxml.jackson.*` → **`tools.jackson.*`** 로 통째로 바뀌었습니다.

**해결**:
```java
import tools.jackson.databind.ObjectMapper;
```
로 바꾸니 정상적으로 컴파일/동작했습니다. `writeValueAsString`, `readTree` 등 자주 쓰는 메서드들은 이름이 그대로 유지되어 있어서, import 경로만 바꾸면 대부분 문제없이 동작했습니다.

**배울 점**: 오래된 블로그 글이나 예전에 배운 대로 `com.fasterxml.jackson...`을 그대로 입력하면 최신 Spring Boot(4.x) 환경에서는 컴파일조차 안 됩니다. 익숙한 import 문이라도 실제로 존재하는지 IDE/컴파일러가 알려주는 오류를 기준으로 확인하는 습관이 필요합니다. (이번 study3~4에서 겪었던 `PathRequest` 패키지 변경, H2 콘솔 모듈 분리와 같은 맥락의 "Boot 4.x 대개편"의 연장선입니다.)

---

## 7. 실행 & 결과

```powershell
cd demo
.\gradlew.bat test
```

이번 실습 환경에서 **실제로 실행한 결과**:
```
BUILD SUCCESSFUL

DemoApplicationTests   : 1 tests, 0 failures
HelloControllerTest    : 3 tests, 0 failures
AuthControllerTest     : 5 tests, 0 failures
MemberControllerTest   : 3 tests, 0 failures
────────────────────────────────────────────
총 12개 테스트 전부 통과
```

테스트 결과 리포트는 `demo/build/reports/tests/test/index.html`에서 브라우저로도 확인할 수 있습니다.

---

## 8. 지금까지 만든 테스트가 검증하는 것 (한눈에 보기)

| 테스트 클래스 | 검증 내용 |
|---|---|
| `DemoApplicationTests` | 스프링 컨텍스트가 정상적으로 뜨는지 (가장 기본적인 안전망) |
| `HelloControllerTest` | 쿼리 파라미터/경로 변수 매핑이 올바른지 ([study1](study1.md)) |
| `AuthControllerTest` | 회원가입 성공/중복/검증 실패, 로그인 성공/실패 ([study4](study4.md)) |
| `MemberControllerTest` | 인증 여부에 따른 접근 제어, CRUD 전체 흐름, 검증 실패 ([study2](study2.md)/[study3](study3.md)/[study4](study4.md)) |

이렇게 지금까지의 study 전체 내용이 테스트 코드 하나하나에 매핑됩니다. 앞으로 코드를 고치다 무언가를 실수로 망가뜨리면, `./gradlew test`가 어느 study의 어떤 시나리오가 깨졌는지 바로 알려줍니다.

---

## 9. 정리

- 슬라이스 테스트(`@WebMvcTest`)와 통합 테스트(`@SpringBootTest`)의 차이와 각각의 용도를 이해했다
- `MockMvc`로 실제 서버를 띄우지 않고도 HTTP 요청/응답을 검증하는 방법을 익혔다
- `@Transactional`을 테스트에 적용해서 테스트 간 데이터 오염을 막는 방법을 실습했다
- 인증이 필요한 API를 테스트할 때, Repository와 `JwtTokenProvider`를 직접 이용해 토큰을 준비하는 패턴을 익혔다
- 실습 중 실제로 만난 두 가지 문제(슬라이스 테스트의 시큐리티 필터 충돌, Jackson 3.x 패키지 변경)를 원인 분석부터 해결까지 기록했다
- `./gradlew test`로 12개 테스트 전부 통과를 실제로 확인했다

## 10. 다음 단계

지금까지 CRUD, 인증, 테스트까지 기본적인 백엔드 API 한 사이클을 모두 만들어봤습니다. 다음 학습 후보:

1. **연관관계 매핑** — `Member`가 여러 개의 글(`Post`)을 가지는 `@OneToMany`/`@ManyToOne` 관계 설계
2. **페이징/정렬** — 목록 조회 API에 `Pageable`, `Page<T>` 적용
3. **API 문서화** — Springdoc(OpenAPI/Swagger UI)으로 API 명세 자동 생성
4. **배포** — Dockerfile 작성, 컨테이너로 패키징해서 실행

원하시는 주제로 study6.md를 이어가면 됩니다.
