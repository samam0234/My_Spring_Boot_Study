# Spring Boot 학습 저장소

Spring Boot의 기본 개념부터 REST API, JPA, Spring Security(JWT), 테스트 코드, 그리고 프론트엔드(HTML/JS) 연동까지 단계별로 실습하며 정리하는 개인 학습 저장소입니다.

- `studyN.md` 문서에 개념 정리와 실습 과정, 실제로 실행해서 확인한 결과를 순서대로 기록합니다.
- `demo/`는 각 study에서 다루는 내용을 실제로 구현한 Spring Boot(Gradle) 프로젝트입니다. study가 진행될수록 `demo/` 코드도 함께 발전합니다.

## 기술 스택

- Java (Gradle 툴체인 기준 프로젝트 JDK에 맞춰 구성)
- Spring Boot 4.x (Web MVC, Data JPA, Validation, Security)
- H2 Database (인메모리, 로컬 실습용)
- JWT (io.jsonwebtoken / jjwt)
- JUnit 5, MockMvc (테스트)
- 순수 HTML/CSS/JavaScript (프론트엔드, 별도 프레임워크 없음)

## 프로젝트 실행

```powershell
cd demo
.\gradlew.bat bootRun
```

기본 포트는 `8080`이며, 실행 후 아래와 같이 확인할 수 있습니다.

- REST API: `http://localhost:8080/hello`, `http://localhost:8080/members`, `http://localhost:8080/posts` 등
- H2 콘솔: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:testdb`, User: `sa`, Password: 없음)
- 프론트엔드 페이지: `http://localhost:8080/StudyHtmlFile.html` (study6 게시판)

## 테스트 실행

```powershell
cd demo
.\gradlew.bat test
```

## 학습 문서 목차

| 문서 | 주제 | 핵심 내용 |
|---|---|---|
| [study0.md](study0.md) | Spring Boot 기초 개념 | Spring vs Spring Boot, IoC/DI, Bean, 계층 구조(Controller-Service-Repository), 주요 어노테이션, REST API 기본 개념, 용어 사전 |
| [study1.md](study1.md) | 프로젝트 생성 & Hello World | Spring Initializr로 프로젝트 생성, `HelloController`로 `@GetMapping`/`@RequestParam`/`@PathVariable` 실습, 서버 실행 및 curl 검증 |
| [study2.md](study2.md) | JPA 연동 & CRUD | H2 DB 연동, `Member` 엔티티/`JpaRepository`/Service/Controller로 회원 CRUD REST API 구현 |
| [study3.md](study3.md) | DTO 분리 & 예외 처리 & 검증 | Entity와 API 스펙(DTO) 분리, `@RestControllerAdvice`로 예외 처리 일원화, `@Valid`로 입력값 검증 |
| [study4.md](study4.md) | Spring Security & JWT | 회원가입/로그인, `BCryptPasswordEncoder`, JWT 발급/검증 필터, `SecurityFilterChain`으로 인증이 필요한 API 보호 |
| [study5.md](study5.md) | 테스트 코드 작성 | `@WebMvcTest` 슬라이스 테스트, `@SpringBootTest`+`MockMvc` 통합 테스트, 인증이 필요한 API의 토큰 발급 테스트 패턴 |
| [study6.md](study6.md) | 프론트엔드 연동 — 게시판 | `Post` CRUD 게시판 백엔드 + `StudyHtmlFile.html`(순수 HTML/JS, `fetch` API)로 실제 브라우저에서 동작 확인 |

### 앞으로의 계획

| 문서 | 주제 | 프론트엔드 파일 |
|---|---|---|
| study7.md | 메시지 기능 | `StudyHtmlFile2.html` |
| study8.md | 날씨 페이지 | `StudyHtmlFile3.html` |
| study9.md | 주식 기능 | `StudyHtmlFile4.html` |
| study10.md | 그 외 자주 쓰이는 웹 기술 | `StudyHtmlFile5.html` |

## 저장소 구조

```
Spring_Boot/
├─ study0.md ~ studyN.md   ← 학습 문서 (개념 + 실습 + 실제 실행 결과)
├─ demo/                   ← Spring Boot(Gradle) 실습 프로젝트
│   └─ src/main/resources/static/  ← study별 프론트엔드 HTML 파일
└─ .agent/                 ← 여러 AI 에이전트가 이 저장소에서 작업할 때 따르는 공통 가이드라인
```
