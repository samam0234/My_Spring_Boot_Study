# study4. 실습 4 — JPA, SQLite, PostgreSQL

Notion 목표는 Spring Data JPA와 MySQL 또는 PostgreSQL입니다. 기본 실행은 SQLite 파일입니다. 설치가 없고, 서버를 껐다 켜도 `demo/data.db`가 남습니다. PostgreSQL은 프로파일로 바꿉니다.

## 1. 의존성

`demo/build.gradle`에 있습니다.

- `spring-boot-starter-data-jpa`
- `org.xerial:sqlite-jdbc`
- `hibernate-community-dialects`의 `SQLiteDialect`
- `org.postgresql:postgresql` (postgres 프로파일에서 사용)

## 2. 기본 설정

`src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:sqlite:data.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

`ddl-auto=update`는 엔티티를 보고 테이블을 고칩니다. 컬럼을 지우는 용도로는 쓰지 않습니다. 학습용 설정입니다.

## 3. Repository

```java
public interface MemberRepository extends JpaRepository<Member, Long> {
}
```

`save`, `findById`, `findAll`, `deleteById`는 인터페이스만으로 생깁니다. 메서드 이름으로 쿼리를 추가할 수 있습니다. `PostRepository.findAllByOrderByCreatedAtDesc`가 그 예입니다.

엔티티는 `domain` 패키지입니다. `@Entity`와 `@Id`가 있어야 합니다. 기본 생성자는 JPA가 쓰므로 `protected`로 비워 둡니다.

## 4. PostgreSQL 프로파일

`src/main/resources/application-postgres.properties`가 같은 키를 덮어씁니다.

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/springboot
spring.datasource.username=spring
spring.datasource.password=spring
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

데이터베이스를 띄웁니다.

```powershell
cd demo
docker compose up -d
.\gradlew.bat bootRun --args="--spring.profiles.active=postgres"
```

계정은 로컬 학습용입니다. `compose.yaml`의 `spring` / `spring`, DB 이름은 `springboot`입니다. Docker가 없으면 SQLite 기본 실행을 그대로 씁니다.

MySQL을 쓸 때도 같은 방식입니다. 드라이버와 URL, dialect만 프로파일 파일에서 바꿉니다. 엔티티와 Repository는 그대로 둡니다.

## 5. 확인

```powershell
curl.exe http://localhost:8080/time/profile
```

`activeProfiles`가 비어 있으면 default(SQLite)입니다. postgres로 띄웠으면 `postgres`가 보여야 합니다. 이 엔드포인트의 설명은 study8에 있습니다.

## 6. 여기서 고정한 것

- 기본 DB는 SQLite 파일입니다. H2가 아닙니다.
- PostgreSQL은 `postgres` 프로파일과 `compose.yaml`로 전환합니다.
- 스키마는 엔티티와 `ddl-auto=update`가 맞춥니다.
