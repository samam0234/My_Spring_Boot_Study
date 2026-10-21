# study0. Spring Boot 기초이론

Notion 페이지 [Spring Boot](https://app.notion.com/p/3d6d7dec5e2e8173aac3d4c26b97894b)의 방향은 이렇습니다. Java 기반의 기업 표준 백엔드로, 계층(Controller — Service — Repository)과 JPA를 여기서 고정합니다.

이 문서는 명령을 치기 전에 지도를 그리는 단계입니다. 코드는 다음 문서부터 `demo/`에 쌓입니다.

## 1. Spring Boot란?

Spring은 Java로 서버 애플리케이션을 만들 때 쓰는 프레임워크입니다. 객체를 직접 `new` 하지 않고 컨테이너가 만들고 연결합니다. 웹, 데이터 접근, 보안, 트랜잭션이 모듈로 나뉘어 있습니다.

그 모듈을 하나씩 조립하면 설정이 길어집니다. Spring Boot는 그 조립의 기본값을 미리 정해 둔 도구입니다.

- **자동 설정.** classpath에 있는 라이브러리를 보고 DataSource, 보안, JSON 변환을 기본값으로 올립니다.
- **내장 서버.** Tomcat을 따로 설치하지 않습니다. `bootRun` 한 번으로 HTTP 서버가 뜹니다.
- **스타터.** `spring-boot-starter-webmvc`처럼 필요한 라이브러리를 묶음으로 가져옵니다.
- **실행 가능한 JAR.** `java -jar` 하나로 배포할 수 있습니다.

한 줄로 쓰면 **Spring Boot = Spring + 자동 설정 + 내장 서버 + 의존성 묶음**입니다.

### 다른 프레임워크와 비교

| 관심사 | Spring Boot | NestJS | Rails |
|---|---|---|---|
| 언어 | Java | TypeScript | Ruby |
| 구조 | Controller / Service / Repository | Module / Controller / Provider | MVC |
| 화면 | 기본은 JSON API. 화면은 따로 둠 | 기본은 JSON API | 기본은 ERB 템플릿 |
| DB | Spring Data JPA | TypeORM, Prisma 등을 선택 | Active Record |
| 설정 태도 | 자동 설정 위에 필요한 것만 덮어씀 | 데코레이터로 선언 | 이름 약속이 기본값 |

이 저장소는 API를 계층으로 나누고, 화면은 `project/client`의 React가 맡습니다. Rails처럼 서버 템플릿이 기본 화면은 아닙니다.

## 2. 계층

요청 하나는 아래 순서로 흐릅니다.

```
Client ⇄ DTO ⇄ Controller ⇄ Service ⇄ Entity ⇄ Repository ⇄ DB
```

| 계층 | 하는 일 | 하지 않을 일 |
|---|---|---|
| Controller | HTTP를 받고 상태 코드를 고름 | SQL, 비밀번호 해시 |
| DTO | API에 나가는 모양, 들어오는 모양 | 테이블 컬럼과 1:1로 붙박기 |
| Service | 업무 규칙. 중복 검사, 권한에 따른 분기 | `HttpServletRequest`를 붙들고 있기 |
| Entity | 테이블과 매핑되는 객체 | 클라이언트가 보내는 JSON을 그대로 받기 |
| Repository | 저장과 조회 | 문장으로 된 에러 메시지 조립 |

Controller가 Repository를 직접 부르면 검증과 예외가 컨트롤러마다 흩어집니다. 이 저장소는 Service를 가운데에 둡니다.

## 3. IoC와 DI

### IoC

원래는 개발자가 `new MemberService()`를 합니다. Spring에서는 컨테이너(ApplicationContext)가 객체의 생성과 수명을 맡습니다. 이 뒤집힘을 제어의 역전(Inversion of Control)이라고 부릅니다.

### DI

필요한 객체를 생성자 인자로 받으면 컨테이너가 넣어 줍니다. 이것을 의존성 주입(Dependency Injection)이라고 부릅니다.

```java
public MemberController(MemberService memberService) {
    this.memberService = memberService;
}
```

생성자 주입을 씁니다. 필드가 `final`이 되고, 테스트에서 가짜 서비스를 넣기 쉽고, 순환 참조가 빨리 드러납니다.

### Bean

컨테이너가 관리하는 객체가 Bean입니다. `@Component`, `@Service`, `@Repository`, `@RestController`, `@Configuration`이 붙은 클래스, 그리고 `@Bean` 메서드가 돌려주는 객체가 여기에 들어갑니다.

## 4. REST와 JPA를 이 저장소에서 고정하는 이유

Notion의 공통 스펙은 게시글 CRUD와 로그인입니다. 지금은 경로 앞에 `/api`를 붙이지 않습니다.

- 게시글: `/posts`
- 로그인: `/auth/signup`, `/auth/login`

JPA는 테이블을 Java 클래스로 다루는 표준입니다. Spring Data JPA는 `JpaRepository`를 상속하면 기본 CRUD 메서드를 만들어 줍니다. 로컬 기본 DB는 SQLite 파일(`demo/data.db`)입니다. PostgreSQL로 바꾸는 방법은 study4의 `postgres` 프로파일에 있습니다.

엔티티를 응답으로 바로 내보내지 않습니다. 비밀번호나 지연 로딩 필드가 JSON에 섞이지 않게 DTO만 내보냅니다.

## 5. 이 저장소의 진도

| 문서 | 역할 |
|---|---|
| study0 | 이 문서. 이론 |
| study1 | 프로젝트 생성, Gradle 명령, Hello API |
| study2 | REST, DTO, Validation |
| study3 | Service와 예외 |
| study4 | JPA, SQLite, PostgreSQL 프로파일 |
| study5 | 1:N, N:M |
| study6 | Security와 JWT |
| study7 | 페이지와 검색 |
| study8 | 테스트와 프로파일 |
| study9 | 외부 API |
| study10 | 파일, SSE, OpenAPI |
| studyFinal | 위 내용을 한 사이트로 묶기. API는 `project/server`, React는 `project/client` |

## 6. 용어

| 용어 | 뜻 |
|---|---|
| Starter | 관련 라이브러리 묶음 |
| Auto Configuration | classpath를 보고 적용되는 기본 설정 |
| DTO | API 경계의 자료 구조 |
| Entity | JPA가 테이블과 연결하는 클래스 |
| Profile | `application-{이름}.properties`로 환경을 나누는 장치 |
| JWT | 로그인 뒤에 서버가 발급하는 서명된 토큰 |
