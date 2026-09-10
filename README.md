# Spring Boot 학습 저장소

Spring Boot의 기본 개념부터 REST API, JPA, Spring Security(JWT), 테스트 코드, 외부 API 연동, 파일 업로드, 실시간 알림, API 문서화, 그리고 프론트엔드(HTML/JS) 연동까지 단계별로 실습하며 정리하는 개인 학습 저장소입니다.

- `studyN.md` 문서에 개념 정리와 실습 과정, 실제로 실행해서 확인한 결과를 순서대로 기록합니다.
- `demo/`는 각 study에서 다루는 내용을 실제로 구현한 Spring Boot(Gradle) 프로젝트입니다. study가 진행될수록 `demo/` 코드도 함께 발전합니다.

## 기술 스택

- Java (Gradle 툴체인 기준 프로젝트 JDK에 맞춰 구성)
- Spring Boot 4.x (Web MVC, Data JPA, Validation, Security, RestClient)
- SQLite (파일 기반 DB, `hibernate-community-dialects` 사용)
- JWT (io.jsonwebtoken / jjwt)
- Springdoc OpenAPI (Swagger UI, API 문서 자동화)
- JUnit 5, MockMvc, AssertJ (테스트)
- 순수 HTML/CSS/JavaScript (프론트엔드, 별도 프레임워크 없음)

## 프로젝트 실행

```powershell
cd demo
.\gradlew.bat bootRun
```

기본 포트는 `8080`이며, 실행 후 아래와 같이 확인할 수 있습니다.

- 학습 허브(모든 프론트엔드 페이지 모음): `http://localhost:8080/main.html`
- REST API 문서(Swagger UI): `http://localhost:8080/swagger-ui.html`
- 로그인/회원가입: `http://localhost:8080/LoginHtmlFile.html`
- DB 파일: `demo/data.db` (SQLite, 서버를 껐다 켜도 데이터 유지 — 초기화하려면 파일 삭제 후 재실행)

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
| [study2.md](study2.md) | JPA 연동 & CRUD | DB 연동, `Member` 엔티티/`JpaRepository`/Service/Controller로 회원 CRUD REST API 구현 |
| [study3.md](study3.md) | DTO 분리 & 예외 처리 & 검증 | Entity와 API 스펙(DTO) 분리, `@RestControllerAdvice`로 예외 처리 일원화, `@Valid`로 입력값 검증 |
| [study4.md](study4.md) | Spring Security & JWT | 회원가입/로그인, `BCryptPasswordEncoder`, JWT 발급/검증 필터, `SecurityFilterChain`으로 인증이 필요한 API 보호 |
| [study5.md](study5.md) | 테스트 코드 작성 | `@WebMvcTest` 슬라이스 테스트, `@SpringBootTest`+`MockMvc` 통합 테스트, 인증이 필요한 API의 토큰 발급 테스트 패턴 |
| [study6.md](study6.md) | 프론트엔드 연동 — 게시판 | `Post` CRUD 게시판 백엔드 + `StudyHtmlFile.html`(순수 HTML/JS, `fetch` API)로 실제 브라우저에서 동작 확인 |
| [study7.md](study7.md) | 로그인 프론트엔드 & 쪽지 | JWT 토큰을 `localStorage`에 저장하는 프론트엔드 패턴, `Message` 엔티티(`@ManyToOne`)로 1:1 쪽지 기능 구현 |
| [study8.md](study8.md) | 외부 API 연동 — 날씨 | `RestClient`로 Open-Meteo API(지오코딩+날씨) 호출, 백엔드 프록시 패턴, `/error` 시큐리티 트러블슈팅 |
| [study9.md](study9.md) | 주식 시세 & 관심 종목 | Yahoo Finance 연동, DB에 저장된 관심 종목 + 외부 실시간 시세 조합, 커스텀 delete 쿼리 `@Transactional` 이슈 해결 |
| [study10.md](study10.md) | 페이지네이션/파일업로드/실시간알림/API문서화 | `Pageable` 페이지네이션, 파일 업로드(경로 조작 방어), SSE 실시간 알림, Springdoc(Swagger UI), `main.html` 허브 페이지 |
| [studyFinal.md](studyFinal.md) | 조건부 접근 제어 & 개인 저장소 | 로그인 여부에 따른 조건부 UI, 전용 로그인 페이지, DB 기반 개인 저장소(upsert), `Clock` 주입 기반 한국시간 주/야간 판별 |

## 프론트엔드 페이지 목록

| 페이지 | 대응 문서 | 설명 |
|---|---|---|
| `main.html` | - | 모든 페이지를 모아 보여주는 허브 |
| `LoginHtmlFile.html` | studyFinal | 전용 로그인/회원가입 페이지 |
| `StudyHtmlFile.html` | study6 | 게시판 (페이지네이션 포함) |
| `StudyHtmlFile2.html` | study7 | 로그인/회원가입 + 쪽지 |
| `StudyHtmlFile3.html` | study8 | 날씨 검색 |
| `StudyHtmlFile4.html` | study9 | 주식 시세 & 관심 종목 |
| `StudyHtmlFile5.html` | study10 | 파일 업로드 & 실시간 알림 & API 문서 링크 |
| `StudyHtmlFileFinal.html` | studyFinal | 조건부 쪽지/저장소 + 주/야간 시간 표시 |

## 저장소 구조

```
Spring_Boot/
├─ study0.md ~ studyFinal.md   ← 학습 문서 (개념 + 실습 + 실제 실행 결과)
├─ demo/                       ← Spring Boot(Gradle) 실습 프로젝트
│   └─ src/main/resources/static/  ← study별 프론트엔드 HTML 파일
└─ .agent/                     ← 여러 AI 에이전트가 이 저장소에서 작업할 때 따르는 공통 가이드라인
```
