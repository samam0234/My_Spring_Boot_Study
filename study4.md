# Study 4 — Spring Security & JWT 로그인 구현

[study3.md](study3.md)까지는 `/members` API가 누구나 호출할 수 있었습니다. 이번 실습에서는 **회원가입 / 로그인 / 로그인한 사람만 API 호출 가능**하도록 Spring Security와 JWT(JSON Web Token)를 도입합니다.

---

## 1. 핵심 개념

### 인증(Authentication) vs 인가(Authorization)
| 용어 | 의미 | 예시 |
|---|---|---|
| **인증(Authentication)** | "너 누구야?"를 확인하는 것 | 아이디/비밀번호로 로그인 |
| **인가(Authorization)** | "너 이거 해도 돼?"를 확인하는 것 | 로그인은 했지만 관리자 API는 admin만 접근 가능 |

### 세션 방식 vs 토큰(JWT) 방식
| | 세션(Session) 방식 | JWT(토큰) 방식 |
|---|---|---|
| 로그인 정보 저장 위치 | 서버 메모리/DB (세션 저장소) | 클라이언트가 토큰을 들고 다님 |
| 서버 상태 | Stateful (서버가 사용자를 "기억") | **Stateless** (서버는 아무것도 기억 안 함) |
| 확장성 | 서버가 여러 대면 세션 공유 필요 | 서버가 여러 대여도 토큰만 검증하면 됨 |
| 이번 실습에서 사용 | ❌ | ✅ |

### JWT(JSON Web Token) 구조
JWT는 `헤더.페이로드.서명` 3부분을 점(`.`)으로 이어붙인 문자열입니다.
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjaHVsc29vIiwiaWF0IjoxNzg5MDIxMTUzLCJleHAiOjE3ODkwMjQ3NTN9.4_9ZlbieW67gposGFxz7vy9UV356FW57-GofvgeB4ys
└──────── Header ────────┘ └──────────────── Payload ────────────────┘ └──────── Signature ────────┘
```
- **Header**: 어떤 알고리즘으로 서명했는지 (`HS256` 등)
- **Payload**: 담고 싶은 정보 (여기서는 로그인한 사용자의 `username`, 발급 시각, 만료 시각)
- **Signature**: 서버만 아는 비밀 키로 만든 서명. 이게 있어서 클라이언트가 payload를 조작해도 서버가 위조를 알아챌 수 있음

로그인에 성공하면 서버가 이 토큰을 발급해주고, 이후 클라이언트는 요청마다 `Authorization: Bearer <토큰>` 헤더에 담아 보냅니다.

---

## 2. 전체 흐름 미리보기

```
1) 회원가입   POST /auth/signup  {username, password}         → 201
2) 로그인     POST /auth/login   {username, password}         → 200 + {token: "..."}
3) API 호출   GET  /members      Header: Authorization: Bearer <token>  → 200
   (토큰 없이 호출하면 → 403)
```

---

## 3. 의존성 추가

`build.gradle`
```groovy
dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-webmvc'
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'org.springframework.boot:spring-boot-starter-validation'
	implementation 'org.springframework.boot:spring-boot-starter-security'   // ← 추가
	implementation 'io.jsonwebtoken:jjwt-api:0.13.0'                         // ← 추가
	runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.13.0'                           // ← 추가 (JWT 실제 구현체)
	runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.13.0'                        // ← 추가 (JSON 직렬화)
	runtimeOnly 'com.h2database:h2'
	runtimeOnly 'org.springframework.boot:spring-boot-h2console'             // ← 추가 (H2 콘솔 자동 설정)
	testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

> ⚠️ `spring-boot-starter-security`를 추가하는 순간, **기본적으로 모든 API가 인증을 요구**하도록 바뀝니다. 아무 설정도 하지 않으면 이전 study의 `/hello`, `/members`가 전부 막힙니다. 그래서 아래에서 어떤 경로를 허용할지 직접 설정해야 합니다.
>
> ⚠️ Spring Boot 4.x부터는 H2 콘솔 자동 설정이 `spring-boot-starter-data-jpa`에서 분리되어 **`spring-boot-h2console`** 모듈로 따로 존재합니다. study2에서는 데이터소스가 자동 구성되어 우연히 동작했지만, Security를 도입해 경로별 접근 제어를 명시하는 시점에 이 의존성이 빠져 있으면 H2 콘솔 관련 자동 설정 클래스 자체가 없어 `PathRequest.toH2Console()` 같은 헬퍼도 쓸 수 없습니다. 이번에 명시적으로 추가했습니다.

`application.properties`에 JWT 비밀 키와 만료 시간을 추가합니다.
```properties
# JWT (실습용 시크릿 키. 실제 서비스에서는 환경변수 등 외부에서 주입해야 함)
jwt.secret=hL1943sr4X27M8toMX+FNilwsf21cZRCXQqNGMHiUrY=
jwt.expiration=3600000
```
- `jwt.secret`: HS256 서명에 사용할 Base64 인코딩된 256비트 키
- `jwt.expiration`: 토큰 유효 시간(ms). `3600000`ms = 1시간

---

## 4. 회원(User) 엔티티 & Repository

`src/main/java/com/example/demo/domain/User.java`
```java
package com.example.demo.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    protected User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
```

> ⚠️ **실습 중 실제로 겪은 문제**: 테이블 이름을 지정하지 않으면 클래스 이름(`User`) 그대로 테이블 이름이 `USER`가 되는데, **`USER`는 H2를 포함한 대부분의 SQL 데이터베이스에서 예약어(reserved keyword)** 라서 테이블 생성 시 문법 오류가 납니다. `@Table(name = "users")`로 테이블 이름을 바꿔서 해결했습니다. (실제로 이 오류가 나서 회원가입 API가 알 수 없는 `403`을 반환하는 걸 직접 겪고 고쳤습니다 — 뒤에서 다시 설명합니다.)

`src/main/java/com/example/demo/repository/UserRepository.java`
```java
package com.example.demo.repository;

import com.example.demo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

---

## 5. 요청/응답 DTO

```java
// dto/SignupRequest.java
public record SignupRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min = 3, message = "아이디는 3자 이상이어야 합니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 4, message = "비밀번호는 4자 이상이어야 합니다.")
        String password
) {}
```
```java
// dto/LoginRequest.java
public record LoginRequest(
        @NotBlank(message = "아이디는 필수입니다.") String username,
        @NotBlank(message = "비밀번호는 필수입니다.") String password
) {}
```
```java
// dto/TokenResponse.java
public record TokenResponse(String token) {}
```

그리고 아이디 중복 가입을 위한 커스텀 예외도 하나 추가합니다 ([study3.md](study3.md)의 예외 처리 패턴을 그대로 재사용).
```java
// exception/DuplicateUsernameException.java
public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String username) {
        super("이미 존재하는 아이디입니다: " + username);
    }
}
```

---

## 6. JWT 토큰 발급/검증 — `JwtTokenProvider`

`src/main/java/com/example/demo/security/JwtTokenProvider.java`
```java
package com.example.demo.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long validityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long validityInMilliseconds) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.validityInMilliseconds = validityInMilliseconds;
    }

    public String createToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

### 코드 설명
| 요소 | 설명 |
|---|---|
| `@Value("${jwt.secret}")` | `application.properties`의 값을 필드/생성자 파라미터에 주입 |
| `Keys.hmacShaKeyFor(...)` | Base64 문자열을 HMAC 서명용 `SecretKey` 객체로 변환 |
| `Jwts.builder().subject(username)...signWith(key).compact()` | 토큰 생성. `subject`에 사용자를 식별할 값(여기선 username)을 담음 |
| `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` | 서명을 검증하면서 토큰을 파싱. 서명이 다르거나 만료되면 예외 발생 |
| `validateToken` | 파싱 중 예외가 나는지 여부로 유효성 판단 |

---

## 7. 사용자 조회 — `CustomUserDetailsService`

Spring Security는 로그인 검증 시 `UserDetailsService`라는 인터페이스를 통해 "이 아이디를 가진 사용자가 있는지, 비밀번호(해시)는 무엇인지"를 조회합니다.

```java
package com.example.demo.security;

import com.example.demo.domain.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_USER")
                .build();
    }
}
```
- 우리가 만든 `User`(엔티티)와 이름이 같은 **Spring Security 내장 클래스** `org.springframework.security.core.userdetails.User`가 있어서 헷갈리기 쉽습니다. 여기서는 우리 엔티티를 조회한 뒤, Spring Security가 이해할 수 있는 형태(`UserDetails`)로 변환해서 반환합니다.

---

## 8. 요청마다 토큰을 검사하는 필터 — `JwtAuthenticationFilter`

Spring Security는 요청이 들어올 때마다 여러 **필터(Filter)** 를 순서대로 통과시키는 구조입니다 (Filter Chain). 우리가 만든 필터를 그 중간에 끼워 넣어서, 토큰이 유효하면 "이 사용자는 인증된 사용자다"라고 표시해줍니다.

```java
package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

### 코드 설명
| 요소 | 설명 |
|---|---|
| `OncePerRequestFilter` | 요청 하나당 딱 한 번만 실행되도록 보장해주는 Spring의 필터 베이스 클래스 |
| `resolveToken` | `Authorization: Bearer xxx` 헤더에서 `xxx` 부분만 추출 |
| `SecurityContextHolder.getContext().setAuthentication(...)` | "이 요청은 이 사용자로 인증되었다"고 Spring Security에 등록. 이후 컨트롤러 등에서 `anyRequest().authenticated()` 조건을 통과하게 됨 |
| 토큰이 없거나 유효하지 않으면? | 그냥 아무것도 안 하고 다음 필터로 넘어감(`filterChain.doFilter`) → 이후 인가 단계에서 "인증 안 됨"으로 처리되어 걸러짐 |

---

## 9. 보안 설정 — `SecurityConfig`

```java
package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toH2Console()).permitAll()
                        .requestMatchers("/auth/**", "/hello/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### 코드 설명
| 요소 | 설명 |
|---|---|
| `PasswordEncoder` (`BCryptPasswordEncoder`) | 비밀번호를 평문으로 저장하지 않고 **단방향 해시**로 저장하기 위한 인코더. 회원가입 시 암호화, 로그인 시 "입력값을 암호화한 것 == 저장된 해시"인지 비교 |
| `csrf.disable()` | CSRF(Cross-Site Request Forgery) 보호는 **브라우저 세션/쿠키 기반** 인증에서 중요한데, 우리는 JWT(Stateless)를 쓰므로 비활성화 |
| `sessionCreationPolicy(STATELESS)` | 서버가 세션을 만들지 않겠다고 선언 (JWT 방식의 핵심) |
| `headers().frameOptions().disable()` | H2 콘솔이 `<iframe>`을 사용하는데, Spring Security가 기본적으로 프레임 사용을 막아서(clickjacking 방지) 이걸 풀어줌 |
| `PathRequest.toH2Console()` | 문자열 패턴(`"/h2-console/**"`)을 직접 쓰는 대신, Spring Boot가 제공하는 전용 `RequestMatcher`. H2 콘솔의 실제 경로가 바뀌어도(`spring.h2.console.path`) 이 헬퍼가 알아서 맞춰줌 |
| `requestMatchers("/auth/**", "/hello/**").permitAll()` | 이 경로들은 인증 없이 접근 허용 |
| `anyRequest().authenticated()` | 그 외 나머지 모든 요청(`/members/**` 등)은 인증 필요 |
| `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)` | 우리가 만든 JWT 필터를 Spring Security 기본 필터보다 앞에 배치 |

---

## 10. 회원가입/로그인 로직 — `AuthService`, `AuthController`

```java
// service/AuthService.java
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // 생성자 생략 (study0.md 참고 — 생성자 주입)

    public void signup(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException(username);
        }
        userRepository.save(new User(username, passwordEncoder.encode(rawPassword)));
    }

    public String login(String username, String rawPassword) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, rawPassword));
        return jwtTokenProvider.createToken(username);
    }
}
```
- `passwordEncoder.encode(rawPassword)` — 회원가입 시 **평문 비밀번호를 절대 그대로 저장하지 않고** 해시로 변환해서 저장
- `authenticationManager.authenticate(...)` — 내부적으로 `CustomUserDetailsService`로 사용자를 조회하고, `PasswordEncoder`로 비밀번호가 맞는지 검증. **틀리면 `BadCredentialsException`(`AuthenticationException`의 하위 타입)을 던짐** → [study3.md](study3.md)에서 만든 `GlobalExceptionHandler` 패턴을 그대로 확장해서 401로 응답 처리

```java
// controller/AuthController.java
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    // 생성자 생략

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request.username(), request.password());
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.username(), request.password());
        return new TokenResponse(token);
    }
}
```

`GlobalExceptionHandler`에도 두 가지 핸들러를 추가했습니다.
```java
@ExceptionHandler(DuplicateUsernameException.class)
public ResponseEntity<ErrorResponse> handleDuplicateUsername(DuplicateUsernameException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
}

@ExceptionHandler(AuthenticationException.class)
public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("아이디 또는 비밀번호가 올바르지 않습니다."));
}
```

---

## 11. 실행 & 전체 흐름 검증

```powershell
cd demo
.\gradlew.bat bootRun
```

아래는 이번 실습 환경에서 **실제로 실행해서 확인한 결과**입니다.

### 1) 토큰 없이 보호된 API 호출 → 차단
```bash
curl http://localhost:8080/members
# → 403 Forbidden (본문 없음)
```

### 2) 회원가입
```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"chulsoo","password":"1234"}'
# → 201 Created
```

### 3) 중복 아이디로 재가입 시도
```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"chulsoo","password":"1234"}'
```
```json
{"message":"이미 존재하는 아이디입니다: chulsoo"}   # 409 Conflict
```

### 4) 입력값 검증 실패
```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"ab","password":"12"}'
```
```json
{"message":"username: 아이디는 3자 이상이어야 합니다., password: 비밀번호는 4자 이상이어야 합니다."}   # 400
```

### 5) 잘못된 비밀번호로 로그인
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"chulsoo","password":"wrong"}'
```
```json
{"message":"아이디 또는 비밀번호가 올바르지 않습니다."}   # 401
```

### 6) 올바른 로그인 → 토큰 발급
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"chulsoo","password":"1234"}'
```
```json
{"token":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjaHVsc29vIiwiaWF0IjoxNzg5MDIxMTUzLCJleHAiOjE3ODkwMjQ3NTN9.4_9ZlbieW67gposGFxz7vy9UV356FW57-GofvgeB4ys"}
```

### 7) 발급받은 토큰으로 보호된 API 호출 → 성공
```bash
TOKEN="위에서 받은 토큰"

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/members
# → 200 OK, []  (아직 등록된 회원 없음)

curl -X POST http://localhost:8080/members \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Younghee","email":"younghee@example.com"}'
# → 201 Created, {"id":1,"name":"Younghee","email":"younghee@example.com"}
```

### 8) 조작되었거나 잘못된 토큰 → 차단
```bash
curl -H "Authorization: Bearer garbage.token.value" http://localhost:8080/members
# → 403 Forbidden
```

모든 케이스가 의도한 대로 동작하는 것을 확인했습니다.

---

## 12. 실습 중 실제로 만난 문제와 해결 (트러블슈팅)

이번 실습에서 실제로 겪었던 문제를 정리합니다. 이런 시행착오 자체가 좋은 학습 자료가 됩니다.

### 문제: 회원가입(`/auth/signup`)이 permitAll인데도 계속 403이 남
**증상**: `SecurityConfig`에서 `/auth/**`를 분명히 `permitAll()`로 설정했는데도 회원가입 요청이 403으로 거부됨.

**원인 파악 과정**: 서버 로그를 확인해보니 진짜 원인은 보안 설정이 아니라 **DB 계층**에 있었습니다.
```
Syntax error in SQL statement "create table [*]user (...)"; expected "identifier"
```
`User`라는 이름의 Entity가 기본적으로 `USER`라는 이름의 테이블에 매핑되는데, **`USER`는 H2(그리고 대부분의 관계형 DB)에서 예약어**라 테이블 생성 자체가 실패했습니다. 테이블이 없으니 회원 조회/저장 쿼리가 전부 실패했고, 그 예외가 Spring Security의 예외 처리 필터(`ExceptionTranslationFilter`)를 거치면서 겉보기엔 인증/인가 문제처럼 보이는 403으로 나타났습니다.

**해결**: Entity에 `@Table(name = "users")`를 추가해서 테이블 이름을 예약어와 겹치지 않게 바꿈.

**배울 점**: 인증/인가 관련 에러가 나면 보안 설정부터 의심하기 쉽지만, **실제로는 그 아래 계층(DB, 쿼리)의 예외가 보안 필터를 통과하면서 다른 상태 코드로 보일 수 있다**는 점을 기억해두면 좋습니다. 항상 서버 콘솔 로그의 스택 트레이스(진짜 원인)를 먼저 확인하는 습관이 중요합니다.

### 참고: 토큰 없이 요청했을 때 왜 401이 아니라 403일까?
일반적으로 REST API에서는 "인증 안 됨" = 401, "인증은 됐는데 권한 없음" = 403으로 구분합니다. 그런데 이번 실습에서는 토큰이 없어도 403이 나옵니다. 이는 `httpBasic()`이나 `formLogin()` 같은 별도의 "인증 방식"을 설정하지 않으면, Spring Security가 인증되지 않은 요청에 기본적으로 `Http403ForbiddenEntryPoint`를 사용하기 때문입니다. 필요하다면 `AuthenticationEntryPoint`를 커스텀 구현해서 401을 반환하도록 바꿀 수 있습니다 (다음 학습 후보로 남겨둡니다).

---

## 13. 정리

- 인증/인가 개념과 세션 방식 대비 JWT(Stateless) 방식의 차이를 이해했다
- `spring-boot-starter-security` + `jjwt` 라이브러리로 회원가입/로그인/토큰 검증을 구현했다
- `BCryptPasswordEncoder`로 비밀번호를 안전하게 저장했다
- `SecurityFilterChain`으로 어떤 경로를 공개하고 어떤 경로를 보호할지 직접 설정했다
- `OncePerRequestFilter`를 상속한 커스텀 필터로 매 요청마다 JWT를 검사하도록 만들었다
- 실제로 서버를 띄워 회원가입 → 로그인 → 토큰 발급 → 인증된 API 호출까지 전 과정을 curl로 검증했고, 그 과정에서 만난 실제 오류(`USER` 예약어 충돌)와 원인 분석 과정까지 기록했다

## 14. 다음 단계

**study5.md**에서는 지금까지 만든 기능들에 대한 **테스트 코드**를 작성해봅니다.
1. `@SpringBootTest` / `MockMvc`를 이용한 통합 테스트
2. `@WebMvcTest`로 Controller만 가볍게 테스트하기
3. 인증이 필요한 API를 테스트할 때 토큰을 발급받아 헤더에 넣는 방법
