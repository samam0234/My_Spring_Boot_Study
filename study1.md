# Study 1 — 프로젝트 생성 & Hello World REST API 만들기

[study0.md](study0.md)에서 배운 개념을 실제 프로젝트로 실습해봅니다.
이 문서를 따라 하면 `D:\vscode\Spring_Boot\demo` 프로젝트가 만들어지고, 실행해서 브라우저/curl로 결과를 확인할 수 있습니다.

> 이 문서 작성 시점(2026년) 기준 환경: **Java 26**, **Spring Boot 4.1.1**, **Gradle 9.7.1**

---

## 1. 프로젝트 생성하기

Spring Boot 프로젝트를 만드는 가장 쉬운 방법은 **Spring Initializr**([https://start.spring.io](https://start.spring.io))를 사용하는 것입니다. 브라우저에서 직접 만들거나, 아래처럼 커맨드로도 만들 수 있습니다.

### 방법 A) 웹사이트에서 생성
1. https://start.spring.io 접속
2. 아래 옵션 선택
   - Project: **Gradle - Groovy**
   - Language: **Java**
   - Packaging: **Jar**
   - Dependencies: **Spring Web**
3. `GENERATE` 클릭 → zip 다운로드 → 압축 해제

### 방법 B) 커맨드로 생성 (이번 실습에서 사용한 방식)
```bash
curl -G "https://start.spring.io/starter.zip" \
  -d type=gradle-project \
  -d language=java \
  -d groupId=com.example \
  -d artifactId=demo \
  -d name=demo \
  -d packageName=com.example.demo \
  -d packaging=jar \
  -d javaVersion=17 \
  -d dependencies=web \
  -o demo.zip

unzip demo.zip
```

---

## 2. 생성된 프로젝트 구조

```
demo
 ├─ gradlew, gradlew.bat        ← Gradle Wrapper (Gradle 설치 없이 빌드 가능하게 해줌)
 ├─ build.gradle                ← 빌드/의존성 설정 파일
 ├─ settings.gradle
 └─ src
     ├─ main
     │   ├─ java/com/example/demo/DemoApplication.java   ← 메인 클래스
     │   └─ resources/application.properties             ← 설정 파일
     └─ test
         └─ java/com/example/demo/DemoApplicationTests.java
```

### build.gradle 내용
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.1.1'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

> ⚠️ **주의 (Spring Boot 4.x 변경점)**
> 예전 Spring Boot 2.x/3.x 튜토리얼에서는 웹 개발용 의존성이 `spring-boot-starter-web` 이었습니다.
> Spring Boot 4.x부터는 웹 MVC 전용 스타터 이름이 **`spring-boot-starter-webmvc`** 로 분리되었습니다(리액티브용 `spring-boot-starter-webflux`와 대비되는 이름 체계). 인터넷에서 오래된 글을 보고 따라 할 때 이 차이를 기억해두세요.

> ⚠️ **toolchain 관련 참고**
> `JavaLanguageVersion.of(17)` 처럼 로컬에 설치되지 않은 JDK 버전을 지정하면 Gradle이 "Cannot find a Java installation" 오류를 냅니다. 이 실습 환경에는 JDK 17이 없고 **JDK 26**만 설치되어 있어서, 로컬 JDK 버전(26)에 맞게 수정했습니다. 여러분 컴퓨터의 `java -version` 결과에 맞게 값을 바꾸면 됩니다.

---

## 3. 메인 클래스 (복습)

```java
package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```
- `@SpringBootApplication` 이 자동 설정 + 컴포넌트 스캔을 켜줍니다 ([study0.md](study0.md) 3번 참고).
- 이 클래스를 실행하면 내장 Tomcat이 뜨고, 기본 포트 **8080** 에서 요청을 받습니다.

---

## 4. Hello World REST API 만들기

`src/main/java/com/example/demo/controller/HelloController.java` 파일을 새로 만듭니다.

```java
package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    // GET /hello
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }

    // GET /hello/name?name=철수  (쿼리 파라미터)
    @GetMapping("/hello/name")
    public String helloByQueryParam(@RequestParam(defaultValue = "World") String name) {
        return "Hello, " + name + "! (query param)";
    }

    // GET /hello/철수  (경로 변수)
    @GetMapping("/hello/{name}")
    public String helloByPathVariable(@PathVariable String name) {
        return "Hello, " + name + "! (path variable)";
    }
}
```

### 코드 설명
| 요소 | 설명 |
|---|---|
| `@RestController` | 이 클래스의 모든 메서드 반환값을 **HTTP 응답 본문**으로 그대로 내려줌 (`@Controller` + `@ResponseBody`) |
| `@GetMapping("/hello")` | `GET /hello` 요청을 이 메서드가 처리하도록 매핑 |
| `@RequestParam` | URL 뒤에 붙는 `?key=value` 형태의 값을 받음. `defaultValue`로 기본값 지정 가능 |
| `@PathVariable` | URL 경로 자체에 포함된 값을 받음 (`{name}` ↔ 실제 값) |

같은 클래스에 컴포넌트 스캔 대상 패키지(`com.example.demo`) 하위에 두었기 때문에, 별도 등록 없이 `@SpringBootApplication`이 자동으로 Bean으로 인식합니다.

---

## 5. 실행하기

Windows에서는 `gradlew.bat`을 사용합니다.

```powershell
cd demo
.\gradlew.bat bootRun
```

정상적으로 실행되면 콘솔에 아래와 비슷한 로그가 나오고, 그 상태로 계속 떠 있습니다(서버가 요청을 기다리는 중).
```
Tomcat started on port 8080 (http) with context path '/'
Started DemoApplication in 1.5xx seconds
```

종료하려면 터미널에서 `Ctrl + C`를 누릅니다.

---

## 6. 테스트해보기

서버가 떠 있는 상태에서 새 터미널(또는 브라우저)로 요청을 보냅니다.

```bash
curl http://localhost:8080/hello
# → Hello, Spring Boot!

curl "http://localhost:8080/hello/name?name=철수"
# → Hello, 철수! (query param)

curl http://localhost:8080/hello/영희
# → Hello, 영희! (path variable)
```

브라우저에서는 그냥 주소창에 `http://localhost:8080/hello` 를 입력해도 동일하게 확인 가능합니다.

> ✅ 위 세 요청 모두 이번 실습 환경에서 직접 실행해서 정상 동작을 확인했습니다.

---

## 7. 자주 겪는 문제 (트러블슈팅)

| 증상 | 원인 / 해결 |
|---|---|
| `Cannot find a Java installation matching {languageVersion=...}` | `build.gradle`의 toolchain 버전이 로컬에 설치된 JDK와 다름 → `java -version`으로 확인 후 맞춰서 수정 |
| `Port 8080 was already in use` | 이미 같은 포트로 뜬 서버가 있음 → 기존 프로세스 종료 또는 `application.properties`에 `server.port=8081` 추가 |
| 한글 경로가 400 Bad Request | 브라우저/curl에서 한글을 URL 인코딩하지 않고 보냈을 때 발생 가능. 브라우저는 자동 인코딩하지만 일부 터미널 환경(Git Bash 등)에서는 직접 인코딩이 필요할 수 있음 |

---

## 8. 정리

- Spring Initializr로 프로젝트를 생성했다 (Gradle + Java + Spring Web)
- `@RestController`, `@GetMapping`, `@RequestParam`, `@PathVariable` 을 사용해 REST API 엔드포인트 3개를 만들었다
- `./gradlew bootRun` 으로 내장 Tomcat 서버를 실행하고, curl로 실제 응답을 확인했다

## 9. 다음 단계

**study2.md**에서는 Spring Data JPA를 추가해서 실제 데이터베이스(H2 또는 MySQL)와 연동하고, `Member` 엔티티에 대한 CRUD REST API를 만들어봅니다.
