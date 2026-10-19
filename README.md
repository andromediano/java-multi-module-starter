# java-multi-module-starter

Spring Boot 기반 멀티모듈 프로젝트를 빠르게 만들기 위한 스타터(템플릿) 저장소다.

이 저장소를 그대로 쓰는 것이 아니라, **복제한 뒤 프로젝트명·groupId·base package를 바꿔 새 프로젝트를 만드는 출발점**으로 쓴다.
사람이 직접 내려받아 구성할 수도 있고, 코딩 에이전트가 저장소를 가져가 입력값에 맞게 프로젝트를 생성할 수도 있다.

> **상태: 작업 중.** 모듈 구성, 빌드 컨벤션, 문서를 계속 다듬고 있다. 자동 생성 스크립트는 아직 없으며, 지금은 아래 [새 프로젝트 만들기](#새-프로젝트-만들기) 절차대로 손으로(또는 에이전트가) 치환한다.

## 목표

- Gradle 멀티모듈 구성, convention plugin, 버전 카탈로그 등 **매번 반복하는 초기 설정을 한 번에 끝낸다.**
- 모듈 간 의존 방향과 설정 파일 규칙을 **미리 정해 두어** 프로젝트가 커져도 구조가 흐트러지지 않게 한다.
- 인증 서버, 리소스 서버, 클라이언트, 스케줄러처럼 **자주 쓰는 실행 모듈을 실제로 동작하는 예시**로 제공하고, 필요한 것만 남겨 쓴다.
- 사람과 코딩 에이전트가 **같은 절차와 규칙**으로 프로젝트를 만들 수 있게 한다.

## 기술 스택

| 구분 | 버전 / 도구 |
|---|---|
| Java | 25 (Temurin, Gradle toolchain) |
| Build | Gradle 9.8.0 (Kotlin DSL), `build-logic` convention plugin, `gradle/libs.versions.toml` |
| Framework | Spring Boot 4.1.1 |
| Persistence | Spring Data JPA, QueryDSL (OpenFeign), PostgreSQL |
| Security | Spring Authorization Server, OAuth2 Resource Server, OAuth2 Client |
| 기타 | Lombok, MapStruct, Guava, Thymeleaf, Checkstyle |
| 로컬 인프라 | Podman (PostgreSQL, Redis, Neo4j), mise |

## 모듈 구성

| 모듈 | 종류 | 역할 | 포트 |
|---|---|---|---|
| `module-core` | 라이브러리 | 공통 DTO, 코드, 포맷터, 검증, 웹 공통 설정 | - |
| `module-domain` | 라이브러리 | JPA 엔티티, 리포지토리, 도메인 서비스, MapStruct 매퍼 | - |
| `module-auth` | 실행 | OAuth2 Authorization Server (OIDC) | 9000 |
| `module-api` | 실행 | OAuth2 Resource Server (REST API) | 8090 |
| `module-client` | 실행 | OAuth2 Client (Thymeleaf, BFF) | 8080 |
| `module-scheduler` | 실행 | 배치·스케줄 작업 (웹 서버 없음) | - |

### 의존 방향

```text
module-auth ─┐
             ├─▶ module-domain ──(api)──▶ module-core
module-api  ─┘

module-client, module-scheduler : 내부 모듈 의존 없음
```

`module-domain`은 `module-core`를 `api`로 노출하므로, `module-domain`을 쓰는 모듈은 `module-core`도 함께 쓸 수 있다.

- 의존은 실행 모듈 → `module-domain` → `module-core` 한 방향으로만 건다. 역방향이나 순환 의존을 만들지 않는다.
- 필요 없는 실행 모듈은 삭제하고 `settings.gradle.kts`의 `include`에서 함께 뺀다.

## 디렉터리 구조

```text
.
├── build-logic/               # convention plugin (common / library / application)
├── config/checkstyle/         # Checkstyle 규칙
├── docs/
│   ├── 00.infra/              # 애플리케이션 설정 규칙, Podman 로컬 인프라
│   └── 01.oauth2/             # OAuth2 모듈 로컬 실행
├── gradle/libs.versions.toml  # 의존성 버전 카탈로그
├── module-*/                  # 각 모듈
├── .env.example               # 로컬 환경변수 예시 (앱과 Podman이 함께 읽는다)
├── mise.toml                  # Java 버전, .env 로딩
└── settings.gradle.kts        # 루트 프로젝트명, 모듈 목록
```

### Convention plugin

모듈 `build.gradle.kts`에 공통 설정을 반복하지 않고 `build-logic`의 플러그인을 적용한다.

| 플러그인 | 내용 | 적용 대상 |
|---|---|---|
| `common-conventions` | Java toolchain, Checkstyle, JUnit Platform, Maven Central | 실행 모듈 |
| `library-conventions` | `common-conventions` + `java-library` | `module-core`, `module-domain` |
| `application-conventions` | `common-conventions` + `application` | (CLI 앱용 예비) |

의존성 버전은 `gradle/libs.versions.toml`에서만 관리한다. Spring Boot가 관리하는 라이브러리는 BOM 버전을 따른다.

## 빠른 시작

스타터 자체를 내려받아 바로 실행해 보는 방법이다.

```sh
git clone git@github.com:andromediano/java-multi-module-starter.git
cd java-multi-module-starter

cp .env.example .env              # DB 계정 등 값을 채운다
docs/00.infra/podman/render.sh    # Podman 매니페스트 생성
podman kube play docs/00.infra/podman/secret.yaml
podman kube play docs/00.infra/podman/pod.yaml

./gradlew build
```

- JDK는 `mise install`로 맞추거나 Gradle toolchain이 자동으로 내려받는다.
- 로컬 인프라 상세: [docs/00.infra/podman/SETUP.md](docs/00.infra/podman/SETUP.md)
- OAuth2 모듈 실행 순서와 로그인: [docs/01.oauth2/SETUP.md](docs/01.oauth2/SETUP.md)

## 새 프로젝트 만들기

### 입력값

| 항목 | 필수 | 예시 | 스타터 원래 값 |
|---|---|---|---|
| 프로젝트명 | ✅ | `order-service` | `java-multi-module-starter` |
| groupId / base package | ✅ | `com.example.order` | `io.opentest` |
| 남길 모듈과 의존 방향 | ✅ | `api → domain → core` | 위 [모듈 구성](#모듈-구성) |
| 모듈 이름 접두어 | | `order-` | `module-` |
| 추가 기술 (DB, 메시징 등) | | Kafka, Redis | PostgreSQL |

### 절차

1. **복제 후 Git 이력을 끊는다.**

   ```sh
   git clone --depth 1 git@github.com:andromediano/java-multi-module-starter.git <프로젝트명>
   cd <프로젝트명>
   rm -rf .git
   ```

2. **이름을 바꾼다.**
   - `settings.gradle.kts`의 `rootProject.name`
   - 패키지 디렉터리 `src/{main,test}/java/io/opentest/**`와 모든 `package`·`import` 선언
   - `@SpringBootApplication(scanBasePackages = "io.opentest")` 등 패키지 문자열
   - 각 `build.gradle.kts`의 저작권 헤더(`OPENTEST`)
   - `application*.yaml`의 `opentest` 흔적, `.idea/codeStyles/Project.xml`의 import 레이아웃 패키지
   - `.env`의 `PROJECT` (Podman 리소스 접두어)
   - 현재 루트 `group`이 지정되어 있지 않으므로, 배포가 필요하면 convention plugin에 `group`을 추가한다.

3. **모듈을 정리한다.** 쓰지 않는 예시 모듈은 디렉터리와 `include`를 함께 삭제한다. 모듈 이름을 바꾸면 `project(":...")` 참조도 함께 바꾼다.

4. **문서를 새 프로젝트에 맞춘다.**
   - `README.md`를 새 프로젝트 소개로 다시 쓴다.
   - `AGENTS.md`에서 "스타터로 새 프로젝트 만들기" 섹션을 지우고 "공통 규칙"만 남긴다.

5. **흔적을 확인한다.** 아래 결과가 비어 있어야 한다.

   ```sh
   grep -rn -i "opentest\|java-multi-module-starter" --exclude-dir={.git,.gradle,build} .
   ```

6. **빌드를 통과시킨다.**

   ```sh
   ./gradlew build
   ```

7. **새 저장소로 시작한다.**

   ```sh
   git init && git add . && git commit -m "chore: 스타터에서 <프로젝트명> 프로젝트를 생성한다"
   ```

## 코딩 에이전트 지침

코딩 에이전트용 지침은 [AGENTS.md](AGENTS.md)에 있다. 특정 도구에 묶이지 않도록 `AGENTS.md` 하나만 두고, 지침을 고칠 때도 이 파일만 수정한다.

- Codex, Cursor, GitHub Copilot 등 `AGENTS.md`를 지원하는 에이전트는 이 파일을 바로 읽는다.
- Claude Code는 2.1.277부터 `AGENTS.md`를 자동으로 읽는다. 단, 작업 디렉터리나 상위 디렉터리에 `CLAUDE.md`, `.claude/CLAUDE.md`, `CLAUDE.local.md`가 있으면 그 파일을 대신 읽는다.
- 그보다 낮은 버전을 쓰거나 개인 `CLAUDE.md`를 따로 둘 때는 그 파일에 `@AGENTS.md`를 넣어 가져온다.

`AGENTS.md`는 두 부분으로 나뉜다.

- **스타터로 새 프로젝트 만들기**: 위 절차를 에이전트가 수행할 때의 규칙. 생성이 끝난 프로젝트에서는 지운다.
- **공통 규칙**: 빌드, 모듈 의존 방향, 설정 파일, 검증 규칙. 생성된 프로젝트에도 그대로 남긴다.

## 문서

| 문서 | 내용 |
|---|---|
| [docs/00.infra/application/CONFIGURATION.md](docs/00.infra/application/CONFIGURATION.md) | `application*.yaml` 구성 방식, 프로파일, 환경변수 규칙 |
| [docs/00.infra/podman/SETUP.md](docs/00.infra/podman/SETUP.md) | Podman으로 PostgreSQL·Redis·Neo4j 로컬 인프라 띄우기 |
| [docs/01.oauth2/SETUP.md](docs/01.oauth2/SETUP.md) | 인증 서버·리소스 서버·클라이언트 로컬 실행 |

## 앞으로 할 일

- [ ] 프로젝트명·groupId·패키지를 입력받아 치환하는 생성 스크립트
- [ ] 루트 `group` / `version` 설정을 convention plugin으로 정리
- [ ] 예시 모듈 선택 설치(필요한 실행 모듈만 남기기) 지원
