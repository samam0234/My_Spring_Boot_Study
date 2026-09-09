# Spring Boot 기초 개념 정리 (Study 0)

Spring Boot를 처음 배울 때 반드시 알아야 할 기본 개념과 용어를 정리한 문서입니다.

---

## 1. Spring과 Spring Boot의 차이

### Spring Framework란?
- 자바(Java) 기반 애플리케이션을 만들기 위한 **프레임워크**
- 핵심 목표: **객체(Bean)들을 개발자 대신 관리**해주는 컨테이너 제공 (IoC/DI)
- 웹, 데이터베이스, 보안, 트랜잭션 등 다양한 모듈을 제공하지만, 설정(XML, Java Config)이 매우 복잡함

### Spring Boot란?
- Spring Framework를 **더 쉽고 빠르게** 사용할 수 있도록 만든 도구
- 핵심 특징:
  1. **자동 설정(Auto Configuration)** — 필요한 설정을 자동으로 해줌
  2. **내장 서버(Embedded Server)** — Tomcat 등을 내장해서 별도 서버 설치 없이 실행 가능
  3. **의존성 관리(Starter)** — `spring-boot-starter-*` 로 필요한 라이브러리를 묶음으로 제공
  4. **독립 실행형(Standalone)** — `jar` 파일 하나로 실행 가능 (`java -jar app.jar`)

> 즉, **Spring Boot = Spring + 자동 설정 + 내장 서버 + 간편한 의존성 관리**

---

## 2. 핵심 개념: IoC와 DI

### IoC (Inversion of Control, 제어의 역전)
- 원래는 개발자가 직접 객체를 `new` 로 생성하고 관리함
- Spring에서는 **객체 생성과 생명주기 관리를 프레임워크(컨테이너)가 대신** 함
- 이 컨테이너를 **IoC 컨테이너** 또는 **ApplicationContext** 라고 부름

### DI (Dependency Injection, 의존성 주입)
- 객체가 필요로 하는 다른 객체(의존성)를 직접 만들지 않고 **외부에서 주입받는 것**
- 방법 3가지:
  ```java
  // 1) 필드 주입 (권장하지 않음)
  @Autowired
  private MemberService memberService;

  // 2) 세터 주입
  @Autowired
  public void setMemberService(MemberService memberService) {
      this.memberService = memberService;
  }

  // 3) 생성자 주입 (권장 ✅)
  private final MemberService memberService;

  public MemberController(MemberService memberService) {
      this.memberService = memberService;
  }
  ```
- **생성자 주입을 권장하는 이유**: 불변성 보장(`final`), 테스트 용이, 순환참조를 컴파일 시점에 발견 가능

### Bean이란?
- Spring IoC 컨테이너가 관리하는 객체
- `@Component`, `@Service`, `@Repository`, `@Controller`, `@Configuration` 등이 붙은 클래스나 `@Bean` 메서드로 등록된 객체가 Bean이 됨

---

## 3. 프로젝트 기본 구조

```
my-spring-boot-app
 ├─ src
 │   ├─ main
 │   │   ├─ java
 │   │   │   └─ com/example/demo
 │   │   │       ├─ DemoApplication.java   ← 메인 실행 클래스
 │   │   │       ├─ controller/            ← 요청을 받는 계층
 │   │   │       ├─ service/               ← 비즈니스 로직 계층
 │   │   │       ├─ repository/            ← DB 접근 계층
 │   │   │       └─ domain(entity)/        ← 데이터 모델(엔티티)
 │   │   └─ resources
 │   │       ├─ application.properties (or .yml)  ← 설정 파일
 │   │       ├─ static/                    ← 정적 리소스(css, js, image)
 │   │       └─ templates/                 ← 뷰 템플릿(Thymeleaf 등)
 │   └─ test                               ← 테스트 코드
 ├─ build.gradle (또는 pom.xml)             ← 빌드/의존성 관리 파일
 └─ README.md
```

### 메인 클래스
```java
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```
- `@SpringBootApplication` 은 아래 3개 어노테이션을 합친 것
  - `@Configuration` — 설정 클래스임을 표시
  - `@EnableAutoConfiguration` — 자동 설정 활성화
  - `@ComponentScan` — 같은 패키지 하위의 컴포넌트(Bean)들을 자동으로 스캔

---

## 4. 레이어드 아키텍처 (계층형 구조)

Spring Boot 웹 애플리케이션은 보통 다음 3계층으로 나눕니다.

| 계층 | 어노테이션 | 역할 |
|---|---|---|
| **Controller** | `@Controller`, `@RestController` | 클라이언트의 HTTP 요청을 받고 응답을 반환 |
| **Service** | `@Service` | 비즈니스 로직(핵심 처리 로직) 처리 |
| **Repository** | `@Repository` | 데이터베이스 접근(CRUD) 담당 |

```
Client → Controller → Service → Repository → Database
```

예시 흐름:
```java
@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/{id}")
    public Member getMember(@PathVariable Long id) {
        return memberService.findMember(id);
    }
}

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member findMember(Long id) {
        return memberRepository.findById(id).orElseThrow();
    }
}

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
}
```

---

## 5. 자주 쓰는 핵심 어노테이션 정리

### 5.1 컴포넌트 등록 관련
| 어노테이션 | 설명 |
|---|---|
| `@Component` | 일반적인 Spring Bean으로 등록 |
| `@Controller` | 웹 MVC 컨트롤러 (View 반환) |
| `@RestController` | `@Controller` + `@ResponseBody`, JSON 등 데이터를 직접 반환 |
| `@Service` | 비즈니스 로직 계층 Bean |
| `@Repository` | DB 접근 계층 Bean, 예외를 Spring 예외로 변환해줌 |
| `@Configuration` | 설정 클래스, 내부에 `@Bean` 메서드 정의 |
| `@Bean` | 메서드가 반환하는 객체를 Bean으로 등록 |

### 5.2 의존성 주입 관련
| 어노테이션 | 설명 |
|---|---|
| `@Autowired` | 타입 기준으로 Bean을 자동 주입 |
| `@Qualifier` | 같은 타입의 Bean이 여러 개일 때 이름으로 지정 |
| `@Value` | 프로퍼티 값(application.properties)을 주입 |

### 5.3 웹 요청 처리(MVC) 관련
| 어노테이션 | 설명 |
|---|---|
| `@RequestMapping` | URL과 메서드를 매핑 (기본형) |
| `@GetMapping` | GET 요청 매핑 |
| `@PostMapping` | POST 요청 매핑 |
| `@PutMapping` | PUT 요청 매핑 (전체 수정) |
| `@PatchMapping` | PATCH 요청 매핑 (부분 수정) |
| `@DeleteMapping` | DELETE 요청 매핑 |
| `@PathVariable` | URL 경로의 값을 파라미터로 받음 (`/members/{id}`) |
| `@RequestParam` | 쿼리 파라미터를 받음 (`?name=abc`) |
| `@RequestBody` | HTTP 요청 본문(JSON 등)을 객체로 변환해서 받음 |
| `@ResponseBody` | 반환값을 View가 아니라 HTTP 응답 본문(JSON 등)으로 사용 |

### 5.4 JPA / 엔티티 관련
| 어노테이션 | 설명 |
|---|---|
| `@Entity` | 이 클래스가 DB 테이블과 매핑되는 엔티티임을 표시 |
| `@Table` | 매핑할 테이블 이름 지정 |
| `@Id` | 기본 키(PK) 지정 |
| `@GeneratedValue` | 기본 키 값을 자동 생성 (auto increment 등) |
| `@Column` | 컬럼 상세 설정 (이름, 길이, null 여부 등) |
| `@OneToMany` / `@ManyToOne` / `@OneToOne` / `@ManyToMany` | 엔티티 간 연관관계 매핑 |

### 5.5 검증(Validation) 관련
| 어노테이션 | 설명 |
|---|---|
| `@Valid` | 요청 객체에 대한 유효성 검사를 실행 |
| `@NotNull` | null 불가 |
| `@NotBlank` | 빈 문자열/공백 불가 (문자열 전용) |
| `@Size` | 길이/크기 제한 |
| `@Min` / `@Max` | 숫자 범위 제한 |

### 5.6 테스트 관련
| 어노테이션 | 설명 |
|---|---|
| `@SpringBootTest` | Spring Boot 전체 컨텍스트를 로드해서 테스트 |
| `@Test` | JUnit 테스트 메서드 표시 |
| `@Transactional` | 테스트/메서드를 트랜잭션으로 감싸고, 테스트 종료 후 롤백 |

---

## 6. 설정 파일 (`application.properties` / `application.yml`)

Spring Boot의 각종 설정값(포트, DB 정보 등)을 지정하는 파일. 두 방식 중 하나를 선택해서 사용합니다.

### properties 방식
```properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=1234
spring.jpa.hibernate.ddl-auto=update
```

### yml 방식 (계층 구조로 더 읽기 쉬움)
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: 1234
  jpa:
    hibernate:
      ddl-auto: update
```

---

## 7. 빌드 도구: Gradle / Maven

Java 프로젝트의 의존성(라이브러리)과 빌드 과정을 관리하는 도구.

### Gradle (`build.gradle`) 예시
```groovy
plugins {
    id 'org.springframework.boot' version '3.3.0'
    id 'java'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.mysql:mysql-connector-j'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### 대표적인 Starter 목록
| Starter | 용도 |
|---|---|
| `spring-boot-starter-web` | REST API / 웹 MVC 개발 (내장 Tomcat 포함) |
| `spring-boot-starter-data-jpa` | JPA(ORM)를 이용한 DB 접근 |
| `spring-boot-starter-test` | JUnit 등 테스트 라이브러리 |
| `spring-boot-starter-security` | 인증/인가(로그인, 권한) |
| `spring-boot-starter-validation` | 요청 값 유효성 검증 |
| `spring-boot-starter-thymeleaf` | 서버 사이드 View 템플릿 엔진 |

---

## 8. 내장 서버(Embedded Server)

- Spring Boot는 기본적으로 **Tomcat**을 내장하고 있어, 별도로 WAS(Web Application Server)를 설치하지 않아도 됨
- `spring-boot-starter-web` 의존성만 추가하면 자동으로 포함됨
- `java -jar app.jar` 명령 하나로 웹 서버까지 함께 실행됨
- 다른 서버(Jetty, Undertow)로 교체도 가능

---

## 9. REST API 기본 개념

- **REST**: 자원(Resource)을 URL로 표현하고, HTTP Method로 행위를 표현하는 설계 방식
- **REST API**: REST 원칙을 따르는 API

| HTTP Method | 의미 | 예시 |
|---|---|---|
| GET | 조회 | `GET /members/1` |
| POST | 생성 | `POST /members` |
| PUT | 전체 수정 | `PUT /members/1` |
| PATCH | 부분 수정 | `PATCH /members/1` |
| DELETE | 삭제 | `DELETE /members/1` |

### HTTP 상태 코드 (자주 쓰는 것들)
| 코드 | 의미 |
|---|---|
| 200 OK | 요청 성공 |
| 201 Created | 자원 생성 성공 |
| 204 No Content | 성공했지만 반환할 내용 없음 |
| 400 Bad Request | 잘못된 요청 |
| 401 Unauthorized | 인증 실패 |
| 403 Forbidden | 권한 없음 |
| 404 Not Found | 자원 없음 |
| 500 Internal Server Error | 서버 오류 |

---

## 10. 용어 사전 (Glossary)

| 용어 | 설명 |
|---|---|
| **Bean** | Spring 컨테이너가 관리하는 객체 |
| **ApplicationContext** | Spring의 IoC 컨테이너 (Bean을 생성/관리) |
| **DI (Dependency Injection)** | 의존성 주입, 필요한 객체를 외부에서 받는 방식 |
| **IoC (Inversion of Control)** | 제어의 역전, 객체 생성/관리를 컨테이너가 대신함 |
| **AOP (Aspect Oriented Programming)** | 관점 지향 프로그래밍. 로깅, 트랜잭션 등 공통 관심사를 분리 |
| **DTO (Data Transfer Object)** | 계층 간 데이터 전달용 객체 |
| **Entity** | DB 테이블과 매핑되는 객체 (JPA) |
| **ORM (Object Relational Mapping)** | 객체와 관계형 DB 테이블을 매핑해주는 기술 (JPA/Hibernate) |
| **JPA (Java Persistence API)** | 자바의 ORM 표준 인터페이스 |
| **Hibernate** | JPA의 대표적인 구현체 |
| **트랜잭션(Transaction)** | 하나의 작업 단위. 모두 성공하거나 모두 실패(All or Nothing) |
| **MVC (Model-View-Controller)** | 프로그램을 모델/뷰/컨트롤러로 나누는 디자인 패턴 |
| **Starter** | Spring Boot가 제공하는 의존성 묶음 패키지 |
| **Auto Configuration** | 클래스패스를 분석해서 자동으로 설정해주는 Spring Boot 기능 |
| **Profile** | 개발/운영 등 환경별로 설정을 다르게 적용하는 기능 (`application-dev.yml` 등) |

---

## 11. 다음 학습 방향 제안

1. **study1.md**: 프로젝트 실습 — Spring Boot 프로젝트 생성 및 "Hello World" REST API 만들기
2. **study2.md**: Spring Data JPA로 DB 연동 (Entity, Repository, CRUD)
3. **study3.md**: 계층 간 DTO 변환, 예외 처리(`@ExceptionHandler`, `@ControllerAdvice`)
4. **study4.md**: Spring Security 기초 (인증/인가, JWT)
5. **study5.md**: 테스트 코드 작성 (단위 테스트 / 통합 테스트)

원하시면 위 순서대로 실습 위주의 다음 문서를 이어서 만들어드릴게요.
