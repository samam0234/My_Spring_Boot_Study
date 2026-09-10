# Study 3 — DTO 분리 & 예외 처리 & 입력값 검증

[study2.md](study2.md)에서 만든 Member CRUD API는 Entity(`Member`)를 요청/응답에 그대로 사용했고, 존재하지 않는 회원을 조회하면 그냥 `404`만 반환하고 이유는 알려주지 않았습니다. 이번 실습에서는 다음 3가지를 개선합니다.

1. **DTO 분리** — Entity를 그대로 노출하지 않고 `MemberRequest`/`MemberResponse`로 주고받기
2. **예외 처리** — `@ExceptionHandler` / `@ControllerAdvice`로 에러를 일관된 형식으로 응답
3. **입력값 검증** — `@Valid`로 이름 필수 입력, 이메일 형식을 자동 검증

---

## 1. 왜 Entity를 그대로 API에 노출하면 안 될까?

study2에서는 이렇게 했습니다.
```java
@PostMapping
public ResponseEntity<Member> create(@RequestBody Member member) { ... }
```

이 방식의 문제점:
- **DB 구조가 그대로 외부에 노출됨** — Entity에 컬럼을 추가/변경하면 API 응답 모양이 의도치 않게 바뀜
- **검증 로직을 넣기 애매함** — Entity는 JPA 매핑이 주 목적이라, 검증 어노테이션을 섞으면 책임이 뒤섞임
- **요청/응답에서 원하는 필드가 다를 수 있음** — 예: 응답에는 `id`를 포함하지만 요청(생성 시점)에는 `id`가 없어야 함

그래서 실무에서는 계층 간 데이터 전달에 **DTO(Data Transfer Object)** 를 사용합니다 ([study0.md](study0.md) 용어 사전 참고).

```
Client ⇄ DTO(Request/Response) ⇄ Controller ⇄ Service ⇄ Entity ⇄ Repository ⇄ DB
```

---

## 2. 의존성 추가 — 검증 라이브러리

`build.gradle`
```groovy
dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-webmvc'
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'org.springframework.boot:spring-boot-starter-validation'   // ← 추가
	runtimeOnly 'com.h2database:h2'
	testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

`spring-boot-starter-validation`은 `@NotBlank`, `@Email`, `@Size` 같은 **Bean Validation** 어노테이션과 그 구현체(Hibernate Validator)를 제공합니다. 이게 없으면 `@Valid`를 붙여도 검증이 동작하지 않습니다.

---

## 3. 요청/응답 DTO 만들기

`src/main/java/com/example/demo/dto/MemberRequest.java`
```java
package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MemberRequest(

        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email
) {
}
```

`src/main/java/com/example/demo/dto/MemberResponse.java`
```java
package com.example.demo.dto;

import com.example.demo.domain.Member;

public record MemberResponse(Long id, String name, String email) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getEmail());
    }
}
```

`src/main/java/com/example/demo/dto/ErrorResponse.java`
```java
package com.example.demo.dto;

public record ErrorResponse(String message) {
}
```

### 코드 설명
| 요소 | 설명 |
|---|---|
| `record` | Java 16+ 문법. 필드, 생성자, getter(`name()`, `email()`), `equals/hashCode/toString`을 자동으로 만들어주는 불변(immutable) 데이터 클래스. DTO처럼 "값을 담기만 하는" 객체에 잘 어울림 |
| `@NotBlank` | `null`, `""`, 공백(`"  "`) 모두 허용하지 않음 (문자열 전용) |
| `@Email` | 이메일 형식(`xxx@yyy.zzz`)인지 검사 |
| `MemberResponse.from(member)` | Entity → DTO 변환을 한 곳에 모아두는 정적 팩토리 메서드. 변환 로직이 여기저기 흩어지는 것을 방지 |
| `MemberRequest`에는 `id`가 없음 | 생성 시점에는 클라이언트가 id를 정할 수 없고, 서버(DB)가 채번하기 때문 |

---

## 4. 커스텀 예외 만들기

`src/main/java/com/example/demo/exception/MemberNotFoundException.java`
```java
package com.example.demo.exception;

public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException(Long id) {
        super("회원을 찾을 수 없습니다. id=" + id);
    }
}
```

- `RuntimeException`을 상속한 **커스텀(사용자 정의) 예외**입니다. "회원이 없다"는 상황을 명확한 타입으로 표현합니다.
- `RuntimeException`(unchecked)을 상속했기 때문에, 호출하는 쪽에서 `throws`를 선언하거나 `try-catch`로 감쌀 필요가 없습니다.

---

## 5. 전역 예외 처리기 (`@RestControllerAdvice`)

`src/main/java/com/example/demo/exception/GlobalExceptionHandler.java`
```java
package com.example.demo.exception;

import com.example.demo.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemberNotFound(MemberNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }
}
```

### 코드 설명
| 요소 | 설명 |
|---|---|
| `@RestControllerAdvice` | `@ControllerAdvice` + `@ResponseBody`. 애플리케이션 **전역**에서 발생하는 예외를 한곳에서 가로채 처리 (`@RestController`가 "요청 처리 담당"이라면, 이건 "예외 처리 담당") |
| `@ExceptionHandler(MemberNotFoundException.class)` | 이 타입의 예외가 컨트롤러 어디서든 발생하면 이 메서드가 대신 응답을 만듦 |
| `MethodArgumentNotValidException` | `@Valid` 검증에 실패했을 때 Spring이 자동으로 던지는 예외. 이걸 잡아서 필드별 에러 메시지를 조합함 |

이렇게 분리하면 컨트롤러 코드에서 `try-catch`나 `if (없으면) 404 반환` 같은 코드가 사라지고, **정상 흐름(happy path)만 남아서 훨씬 읽기 쉬워집니다.**

---

## 6. Service 수정 — Optional 대신 예외를 던지기

`src/main/java/com/example/demo/service/MemberService.java`
```java
package com.example.demo.service;

import com.example.demo.domain.Member;
import com.example.demo.exception.MemberNotFoundException;
import com.example.demo.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member create(String name, String email) {
        return memberRepository.save(new Member(name, email));
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException(id));
    }

    public Member update(Long id, String name, String email) {
        Member member = findById(id);
        member.setName(name);
        member.setEmail(email);
        return memberRepository.save(member);
    }

    public void delete(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new MemberNotFoundException(id);
        }
        memberRepository.deleteById(id);
    }
}
```

- study2에서는 `Optional<Member>`를 반환해서 컨트롤러가 직접 404를 처리했지만, 이번엔 **없으면 예외를 던지는 방식**으로 바꿨습니다. `orElseThrow(() -> new MemberNotFoundException(id))`가 핵심 패턴입니다.
- `update()`가 내부적으로 `findById(id)`를 재사용하므로, 존재하지 않을 때의 예외 처리 로직이 한 곳에만 있습니다.

---

## 7. Controller 수정 — DTO + 검증 적용

`src/main/java/com/example/demo/controller/MemberController.java`
```java
package com.example.demo.controller;

import com.example.demo.domain.Member;
import com.example.demo.dto.MemberRequest;
import com.example.demo.dto.MemberResponse;
import com.example.demo.service.MemberService;
import jakarta.validation.Valid;
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
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberRequest request) {
        Member saved = memberService.create(request.name(), request.email());
        return ResponseEntity.status(201).body(MemberResponse.from(saved));
    }

    @GetMapping
    public List<MemberResponse> findAll() {
        return memberService.findAll().stream()
                .map(MemberResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public MemberResponse findById(@PathVariable Long id) {
        return MemberResponse.from(memberService.findById(id));
    }

    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return MemberResponse.from(memberService.update(id, request.name(), request.email()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 눈여겨볼 변화
- `@Valid @RequestBody MemberRequest request` — `@Valid`가 붙으면 Spring이 요청 본문을 객체로 변환한 직후 **자동으로 검증**을 수행합니다. 검증에 실패하면 컨트롤러 메서드 본문은 아예 실행되지 않고 `MethodArgumentNotValidException`이 던져집니다 (→ `GlobalExceptionHandler`가 처리).
- `findById`, `update` 메서드에 더 이상 `ResponseEntity`나 `orElseGet(...)`이 없습니다. 없으면 예외가 던져지고 → 예외 처리기가 알아서 404를 만들어주기 때문입니다.
- `memberService.findAll().stream().map(MemberResponse::from).toList()` — Entity 목록을 DTO 목록으로 변환하는 전형적인 패턴입니다.

---

## 8. 실행 & 검증

```powershell
cd demo
.\gradlew.bat bootRun
```

아래는 이번 실습 환경에서 **실제로 실행해서 확인한 결과**입니다.

### 1) 정상 생성
```bash
curl -X POST http://localhost:8080/members \
  -H "Content-Type: application/json" \
  -d '{"name":"Chulsoo","email":"chulsoo@example.com"}'
```
```json
{"id":1,"name":"Chulsoo","email":"chulsoo@example.com"}   # 201 Created
```

### 2) 검증 실패 (이름 없음 + 이메일 형식 오류)
```bash
curl -X POST http://localhost:8080/members \
  -H "Content-Type: application/json" \
  -d '{"name":"","email":"not-an-email"}'
```
```json
{"message":"name: 이름은 필수입니다., email: 이메일 형식이 올바르지 않습니다."}   # 400 Bad Request
```

### 3) 존재하지 않는 회원 조회 / 수정 / 삭제
```bash
curl http://localhost:8080/members/999
curl -X PUT http://localhost:8080/members/999 -H "Content-Type: application/json" -d '{"name":"X","email":"x@example.com"}'
curl -X DELETE http://localhost:8080/members/999
```
셋 다 동일하게:
```json
{"message":"회원을 찾을 수 없습니다. id=999"}   # 404 Not Found
```

### 4) 정상 조회
```bash
curl http://localhost:8080/members/1
```
```json
{"id":1,"name":"Chulsoo","email":"chulsoo@example.com"}   # 200 OK
```

모든 케이스가 의도한 상태 코드와 메시지로 응답하는 것을 확인했습니다.

---

## 9. study2 → study3 비교 요약

| 구분 | study2 | study3 |
|---|---|---|
| 요청/응답 타입 | Entity(`Member`) 직접 사용 | `MemberRequest` / `MemberResponse` DTO |
| 없는 회원 처리 | 컨트롤러에서 `Optional` + `orElseGet`으로 직접 404 | Service가 예외를 던지고 `@RestControllerAdvice`가 일괄 처리 |
| 잘못된 입력 (빈 이름 등) | 그대로 저장됨 (검증 없음) | `@Valid`가 막고 400 + 상세 메시지 반환 |
| 에러 응답 형식 | 없음 (본문 없이 상태 코드만) | `{"message": "..."}` 형식으로 일관됨 |

---

## 10. 정리

- Entity와 API 스펙(DTO)을 분리해서, DB 구조 변경이 API에 그대로 영향을 주지 않도록 만들었다
- `MemberNotFoundException` + `@RestControllerAdvice`로 "없는 리소스 조회 시 404" 같은 반복되는 예외 처리를 한 곳으로 모았다
- `@Valid` + Bean Validation 어노테이션(`@NotBlank`, `@Email`)으로 잘못된 입력을 컨트롤러 진입 전에 걸러냈다
- 실제로 서버를 띄워 정상/실패 케이스를 모두 curl로 검증했다

## 11. 다음 단계

**study4.md**에서는 Spring Security를 도입해서:
1. 인증(Authentication) / 인가(Authorization) 기본 개념
2. 로그인 API, 비밀번호 암호화(`PasswordEncoder`)
3. JWT를 이용한 토큰 기반 인증

을 다뤄볼 예정입니다.
