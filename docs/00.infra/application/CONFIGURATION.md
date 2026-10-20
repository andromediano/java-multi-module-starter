# Application 설정

Spring Boot 애플리케이션 설정 파일(`*.yaml`)의 구성 방식과 작성 규칙을 정리한다.

## 원칙

1. **확장자는 `.yaml`로 통일한다.** `.yml`, `.properties`를 섞어 쓰지 않는다.
2. **실행 모듈은 `application.yaml` 하나로 구성한다.** `application-api.yaml`처럼 모듈 이름으로 파일을 나누지 않는다.
3. **라이브러리 모듈은 `application-{모듈}.yaml`(`application-core.yaml`, `application-domain.yaml`)로 두고, 실행 모듈이 `spring.config.import`로 가져온다.**
   - 라이브러리 jar 안에 `application.yaml`이 있으면 classpath에서 하나만 읽혀 설정이 사라진다.
   - `application-*` 이름을 쓰는 이유는 IntelliJ가 이 이름만 Spring Boot 설정 파일로 인식해 자동완성과 검증을 해주기 때문이다.
   - 이 이름은 Spring Boot의 프로파일 전용 파일 규칙과 같다. **`core`, `domain`을 프로파일 이름으로 쓰지 않는다.**
     (`spring.profiles.active`, `spring.profiles.include`에 넣으면 import와 별개로 다시 로드된다)
4. **프로파일은 실행 환경(`local`, `dev`, `prod`)만 나타낸다.** 모듈 이름을 프로파일로 쓰지 않는다.
5. **기본 문서는 운영에 안전한 값으로 두고, `local` 문서에서만 개발 편의 설정을 켠다.**
6. **환경마다 다른 값과 비밀값은 환경변수로 받는다.** 기본 문서에는 기본값 없이 `${DB_PASSWORD}`처럼 두어, 운영에서 누락되면 기동 시 바로 실패하게 한다.

## 파일 구성

| 모듈 | 종류 | 파일 | 포트 | 가져오는 설정 |
|---|---|---|---|---|
| module-core | 라이브러리 | `application-core.yaml` | - | - |
| module-domain | 라이브러리 | `application-domain.yaml` | - | - |
| module-api | 실행 | `application.yaml` | 8090 | `application-core.yaml`, `application-domain.yaml` |
| module-auth | 실행 | `application.yaml` | 9000 | `application-core.yaml`, `application-domain.yaml` |
| module-client | 실행 | `application.yaml` | 8080 | - |
| module-scheduler | 실행 | `application.yaml` | - (웹 서버 없음) | - |

### 각 파일이 담당하는 설정

| 파일 | 담당 |
|---|---|
| `application-core.yaml` | multipart, MVC, Thymeleaf 기본값, 사용자 정의 속성(`myapp.*`) |
| `application-domain.yaml` | datasource, HikariCP, JPA/Hibernate |
| `module-api/application.yaml` | 서버 포트, 애플리케이션 이름, OAuth2 Resource Server |
| `module-auth/application.yaml` | 서버 포트, 애플리케이션 이름 |
| `module-client/application.yaml` | 서버 포트, 애플리케이션 이름, OAuth2 Client, Thymeleaf 캐시 |
| `module-scheduler/application.yaml` | 애플리케이션 이름 |

## 파일 작성 형식

한 파일 안에서 `---`로 문서를 나누고, 환경별 차이는 `spring.config.activate.on-profile` 문서에 둔다.
환경별 차이가 커지면 그때 `application-prod.yaml`처럼 파일을 분리한다.

```yaml
# 기본 문서: 모든 환경에 적용. 운영에 안전한 값만 둔다.
server:
  port: 8090

spring:
  application:
    name: module-api
  profiles:
    default: local          # 프로파일을 지정하지 않으면 local로 실행
  config:
    import:
      - classpath:application-core.yaml
      - classpath:application-domain.yaml
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${AUTH_ISSUER_URI}   # 기본값 없음: 운영에서 누락 시 기동 실패

---

# local 문서: 로컬 개발에서만 적용
spring:
  config:
    activate:
      on-profile: local
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000

logging:
  level:
    org.springframework.security: DEBUG
```

## 프로파일

| 프로파일 | 용도 | 활성화 방법 |
|---|---|---|
| `local` | 로컬 개발 | 기본값(`spring.profiles.default`)이라 지정하지 않아도 켜진다 |
| `dev` | 개발 서버 | `SPRING_PROFILES_ACTIVE=dev` |
| `prod` | 운영 | `SPRING_PROFILES_ACTIVE=prod` |
| `test` | 테스트 | `@ActiveProfiles("test")` |

`dev`, `prod` 문서는 아직 없다. 기본 문서가 운영 기준이므로, 환경별로 다른 값은 환경변수로 넣는다.

### 환경별 주요 차이

| 설정 | 기본(운영) | local |
|---|---|---|
| DB 접속 정보 | `DB_*` 환경변수 | `DB_*` 환경변수 (루트 `.env`) |
| `spring.jpa.hibernate.ddl-auto` | `validate` | `update` |
| `spring.jpa.show-sql` | `false` | `true` |
| OAuth2 issuer | `AUTH_ISSUER_URI` 환경변수 | `http://localhost:9000` |
| OAuth2 client secret | `OAUTH_CLIENT_SECRET` 환경변수 | `secret` |
| Spring Security 로그 | 기본 | `DEBUG` |
| Thymeleaf 캐시(client) | 켬 | 끔 |

## 환경변수

| 이름 | 사용 모듈 | local | 운영 |
|---|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | api, auth (domain) | **필수** (루트 `.env`) | 필수 |
| `AUTH_ISSUER_URI` | api, auth, client | 불필요 | 필수 |
| `JWK_CERTIFICATE`, `JWK_PRIVATE_KEY` | auth | 불필요 | 필수 |
| `OAUTH_CLIENT_SECRET` | client | 불필요 | 필수 |
| `API_BASE_URL` | client | 불필요 | 필수 |
| `SPRING_PROFILES_ACTIVE` | 전체 | 불필요 | `prod` 등 |

운영에서는 `SPRING_PROFILES_ACTIVE`를 반드시 지정한다. 빠뜨리면 `spring.profiles.default`인 `local`이 켜져
`ddl-auto: update`와 로컬용 admin 사용자, OAuth2 클라이언트 등록이 동작한다.

DB URL은 `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}`으로 만든다.
`sslmode` 같은 JDBC 옵션이 필요하면 `SPRING_DATASOURCE_URL` 환경변수로 URL 전체를 지정한다. 환경변수가 yaml보다 우선한다.

## 로컬 실행

### 1. 환경변수 준비

루트 `.env` 하나를 앱과 [Podman 설정](../podman/SETUP.md)이 함께 읽는다.

```sh
cp .env.example .env    # DB_*, NEO4J_PASSWORD 값을 채운다
```

`.env`는 git에 올리지 않는다(`.gitignore` 등록됨).

- **터미널**: `mise.toml`의 `[env] _.file = ".env"` 설정으로 프로젝트 폴더에 들어오면 자동으로 불러온다. `mise.toml`을 처음 받으면 `mise trust`를 실행한다.
- **IntelliJ**: Dock이나 Toolbox로 실행하면 셸 환경변수를 받지 못한다. Run Configuration의 Environment variables에 `.env`를 지정하거나 mise 플러그인을 사용한다.

### 2. DB 준비

[Podman 설정](../podman/SETUP.md)으로 PostgreSQL을 실행한다.

기존 데이터 볼륨을 재사용하면 `DB_NAME` 데이터베이스가 생성되지 않는다. `database "..." does not exist` 오류가 나면 직접 만든다.

```sh
podman exec "$PROJECT-pod-postgres" sh -c 'createdb -U "$POSTGRES_USER" "$POSTGRES_DB"'
```

### 3. 실행

```sh
./gradlew :module-auth:bootRun     # 9000
./gradlew :module-api:bootRun      # 8090
./gradlew :module-client:bootRun   # 8080
```

client는 브라우저에서 **`http://127.0.0.1:8080`**으로 접속한다.
auth 서버에 등록된 redirect URI가 `127.0.0.1` 기준이고, `localhost`로 접속하면 client와 auth의 세션 쿠키가 충돌한다.

## 운영 실행

```sh
SPRING_PROFILES_ACTIVE=prod \
DB_HOST=<host> \
DB_PORT=5432 \
DB_NAME=<db> \
DB_USERNAME=<user> \
DB_PASSWORD=<password> \
AUTH_ISSUER_URI=https://<auth-host> \
java -jar module-api-0.0.1-SNAPSHOT.jar
```

비밀값은 K8s Secret, Vault 등으로 주입한다. 파일로 마운트된 secret은 `spring.config.import: optional:configtree:/run/secrets/`로 읽을 수 있다.

## 주의할 점

### import한 파일의 값이 우선한다

`spring.config.import`로 가져온 파일의 값은 가져온 쪽(`application.yaml`)보다 **우선한다**.
`application-domain.yaml`에 있는 키를 실행 모듈의 `application.yaml`에서 다시 써도 덮어써지지 않는다.

- 라이브러리 파일(`application-core.yaml`, `application-domain.yaml`)에는 그 모듈이 책임지는 설정만 둔다.
- 실행 모듈마다 달라야 하는 값은 라이브러리 파일에 두지 말고, 실행 모듈 파일이나 환경변수로 넣는다.

### 파일에 `spring.profiles.active`를 쓰지 않는다

명령줄이나 환경변수로 `spring.profiles.active`를 지정하면 파일의 값을 **대체**한다.
파일에 `active: api`처럼 써두면 운영에서 `prod`를 지정하는 순간 그 설정이 빠진다. 기본 프로파일은 `spring.profiles.default`로 지정한다.

### 프로파일 문서 안에서는 프로파일을 지정할 수 없다

`spring.profiles.active`, `include`, `group`, `default`는 `on-profile` 문서나 `application-{profile}.yaml` 안에서 쓸 수 없다. 기본 문서에만 쓴다.

### 테스트의 `application.yaml`은 main 설정을 가린다

`src/test/resources/application.yaml`이 있으면 main의 `application.yaml`은 읽히지 않는다(같은 classpath 이름).
테스트 전용 값은 `application-test.yaml`과 `@ActiveProfiles("test")`로 분리하는 것을 권장한다.

### IntelliJ의 import 경로 오류 표시는 무시한다

IntelliJ에서 `spring.config.import`의 `classpath:application-core.yaml`, `classpath:application-domain.yaml`에
"Cannot resolve file" 오류가 표시된다.
IntelliJ가 다른 모듈의 resources 경로를 해석하지 못해 생기는 표시일 뿐이고, 런타임에는 정상적으로 로드된다.

**`optional:`을 붙여 이 표시를 없애지 않는다.**
`optional:`이 없으면 파일이 없을 때 기동 시 바로 실패하지만, 붙이면 설정이 빠진 채로 조용히 기동된다.
파일 이름 변경이나 모듈 의존성 정리로 파일이 빠졌을 때 알아차릴 수 없게 된다.
표시가 거슬리면 IntelliJ에서 해당 검사만 끈다. IDE에만 영향을 주고 런타임 안전장치는 유지된다.

## 새 설정을 추가할 때

| 추가할 설정 | 위치 |
|---|---|
| DB, JPA 관련 | `application-domain.yaml` |
| 여러 실행 모듈이 공통으로 쓰는 웹/MVC 설정 | `application-core.yaml` |
| 특정 실행 모듈에만 필요한 설정 | 그 모듈의 `application.yaml` |
| 로컬 개발에서만 필요한 설정 | 해당 파일의 `local` 문서 |
| 환경마다 다르거나 비밀인 값 | 환경변수(`${NAME}`) + 이 문서의 환경변수 표에 추가 |

## 남은 작업

- [ ] `application-core.yaml` 정리: 사용하지 않는 `myapp.*` 속성, Spring Boot 2.x 시절 주석, 정의되지 않은 `${myapp.app-code}` 참조
- [ ] `application-core.yaml`의 `on-profile: development` 문서를 `local`로 변경(현재 활성화되지 않음)
- [ ] `spring.messages.basename`에 `validation` 추가(`validation.properties`가 로드되지 않음)
- [ ] 테스트 설정을 `application-test.yaml`로 분리하거나 Testcontainers(`@ServiceConnection`) 도입
- [ ] 운영 스키마 관리를 위한 마이그레이션 도구(Flyway 등) 도입
- [ ] 필요 시 `dev`, `prod` 프로파일 문서 추가

## 참고

- [Spring Boot Reference – Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [Spring Boot Reference – Profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html)
