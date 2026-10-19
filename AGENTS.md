# AGENTS.md

코딩 에이전트가 이 저장소에서 작업할 때 따르는 지침이다. 프로젝트 소개, 모듈 구성, 실행 방법은 [README.md](README.md)를 본다.

## 스타터로 새 프로젝트 만들기

이 저장소는 Spring Boot 멀티모듈 프로젝트의 스타터다. 새 프로젝트를 만들어 달라는 요청을 받으면 README의 [새 프로젝트 만들기](README.md#새-프로젝트-만들기) 절차를 따른다.

> 생성을 마친 프로젝트에서는 이 섹션을 지운다. 아래 [공통 규칙](#공통-규칙)은 남긴다.

- 필수 입력값(프로젝트명, groupId/base package, 남길 모듈과 의존 방향)이 빠졌으면 **작업 전에 한 번에 모아서** 묻는다. 추측해서 채우지 않는다.
- `gradle init`, start.spring.io 등 다른 템플릿으로 처음부터 만들지 않는다. 반드시 이 저장소를 복제해서 시작한다.
- 쓰지 않는 예시 모듈은 디렉터리와 `settings.gradle.kts`의 `include`를 함께 삭제한다.
- `README.md`는 새 프로젝트 소개로 다시 쓰고, 이 파일에서는 이 섹션을 지운다. 두 문서에 스타터 이름이 남아 있으면 아래 grep이 비지 않는다.
- 이름 치환과 문서 정리 후 아래 결과가 비어 있어야 한다.

  ```sh
  grep -rn -i "opentest\|java-multi-module-starter" --exclude-dir={.git,.gradle,build} .
  ```

- `./gradlew build` 통과와 위 grep 결과가 비어 있는 것을 확인해야 완료로 본다.
- `git init`과 첫 커밋까지만 하고 push하지 않는다.
- 완료 후 모듈 트리, 의존 방향, 추가·삭제·변경한 항목, 빌드 결과를 보고한다.

## 공통 규칙

### 빌드

- 공통 빌드 설정은 `build-logic`의 convention plugin에 둔다. 모듈 `build.gradle.kts`에 중복해서 쓰지 않는다.
  - 라이브러리 모듈: `library-conventions`
  - 실행 모듈: `common-conventions`
- 의존성은 `gradle/libs.versions.toml`을 통해서만 추가한다. Spring Boot BOM이 관리하는 라이브러리는 버전을 적지 않는다.
- 요청이 없으면 다음은 바꾸지 않는다: `build-logic`의 컨벤션, Checkstyle 규칙(`config/checkstyle/checkstyle.xml`), Gradle Wrapper 버전, 카탈로그에 이미 있는 의존성 버전.
- 새로 추가한 라이브러리와 버전은 작업 결과에 명시한다.

### 모듈 의존 방향

- 실행 모듈 → `module-domain` → `module-core` 한 방향으로만 `project(":...")` 의존을 건다.
- 역방향이나 순환 의존이 필요해 보이면 작업을 멈추고 보고한다.
- 라이브러리 모듈은 `bootJar`를 끄고 일반 `jar`만 만든다.

### 설정 파일

[docs/00.infra/application/CONFIGURATION.md](docs/00.infra/application/CONFIGURATION.md)를 따른다. 요점은 다음과 같다.

- 확장자는 `.yaml`로 통일한다.
- 실행 모듈은 `application.yaml` 하나, 라이브러리 모듈은 `application-{모듈}.yaml`로 두고 실행 모듈이 `spring.config.import`로 가져온다.
- 프로파일은 실행 환경(`local`, `dev`, `prod`)만 나타낸다. 모듈 이름을 프로파일로 쓰지 않는다.
- 환경마다 다른 값과 비밀값은 환경변수로 받고, 기본 문서에는 기본값을 두지 않는다.
- 로컬 환경변수는 루트 `.env` 하나로 관리한다. `.env`는 커밋하지 않고, 키를 추가하면 `.env.example`에도 반영한다.

### 검증

```sh
./gradlew build                     # 컴파일, Checkstyle, 테스트
./gradlew :<모듈>:test              # 모듈 단위 테스트
./gradlew :<모듈>:bootRun           # 실행 모듈 기동
```

- 작업을 마치기 전에 `./gradlew build`를 통과시킨다. 실패하면 원인을 고치고 다시 실행한다.
- 실행에 DB 등 로컬 인프라가 필요하면 [docs/00.infra/podman/SETUP.md](docs/00.infra/podman/SETUP.md)를 따른다.
