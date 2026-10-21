# study1. 만드는 방법, 명령어, 실습 1

[study0.md](study0.md)의 계층을 빈 프로젝트에서 확인합니다. 실습 1의 결과물은 `GET /hello`입니다.

환경은 **Java 26**, **Spring Boot 4.1.1**, **Gradle**입니다. 프로젝트 위치는 `demo/`입니다.

## 1. 프로젝트 만들기

브라우저에서 [https://start.spring.io](https://start.spring.io)를 엽니다.

- Project: Gradle - Groovy
- Language: Java
- Spring Boot: 4.1.x
- Packaging: Jar
- Java: 26
- Dependencies: Spring Web

이 저장소의 `demo/`는 그 결과 위에 Data JPA, Validation, Security가 더해진 상태입니다. 처음부터 다시 받으려면 아래를 씁니다. 이미 `demo/`가 있으면 덮어쓰지 않습니다.

```powershell
curl.exe -G "https://start.spring.io/starter.zip" `
  -d type=gradle-project `
  -d language=java `
  -d groupId=com.example `
  -d artifactId=demo `
  -d name=demo `
  -d packageName=com.example.demo `
  -d packaging=jar `
  -d javaVersion=17 `
  -d dependencies=web `
  -o demo.zip
```

Java 버전은 생성 시점의 Initializr 목록을 따릅니다. 이 저장소는 `build.gradle`의 toolchain을 26으로 고정합니다.

## 2. 자주 쓰는 명령

`demo/`에서 실행합니다.

| 명령 | 하는 일 |
|---|---|
| `.\gradlew.bat bootRun` | 서버 실행. 기본 포트 8080 |
| `.\gradlew.bat test` | 테스트 |
| `.\gradlew.bat build` | 컴파일, 테스트, JAR 생성 |
| `.\gradlew.bat bootRun --args="--spring.profiles.active=postgres"` | PostgreSQL 프로파일로 실행 (study4) |
| Ctrl+C | 서버 종료 |

Wrapper(`gradlew.bat`)가 Gradle을 받아서 쓰기 때문에 Gradle을 따로 설치하지 않아도 됩니다.

## 3. 처음 보이는 파일

```
demo
├─ gradlew.bat
├─ build.gradle
├─ settings.gradle
└─ src
   ├─ main/java/com/example/demo/DemoApplication.java
   └─ main/resources/application.properties
```

`DemoApplication`의 `@SpringBootApplication`이 컴포넌트 스캔과 자동 설정의 시작점입니다.

## 4. 실습 1 — Hello

`HelloController`는 세 가지 매핑을 보여 줍니다.

| 요청 | 결과 |
|---|---|
| `GET /hello` | `Hello, Spring Boot!` |
| `GET /hello/name?name=민아` | 쿼리 파라미터 |
| `GET /hello/민아` | 경로 변수 |

서버를 띄운 뒤 확인합니다.

```powershell
cd demo
.\gradlew.bat bootRun
```

다른 터미널에서:

```powershell
curl.exe http://localhost:8080/hello
curl.exe "http://localhost:8080/hello/name?name=mina"
curl.exe http://localhost:8080/hello/mina
```

문자열을 그대로 돌려주는 것은 학습용입니다. 이후 API는 JSON DTO를 돌려줍니다.

## 5. 여기서 고정한 것

- 빌드 도구는 Gradle Wrapper입니다.
- 애플리케이션 시작점은 `DemoApplication`입니다.
- HTTP 입구는 `@RestController`와 `@GetMapping`입니다.
