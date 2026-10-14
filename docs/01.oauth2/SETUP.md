# OAuth2 로컬 실행

| 모듈 | 역할 | 주소 |
|------|------|------|
| module-auth | Authorization Server (OIDC) | http://localhost:9000 |
| module-api | Resource Server | http://localhost:8090 |
| module-client | OAuth2 Client (Thymeleaf, BFF) | http://127.0.0.1:8080 |

## 실행

PostgreSQL을 띄우고(`docs/00.infra/podman`) `.env`에 `DB_PASSWORD`를 채운 뒤, 아래 순서로 실행한다.
module-client는 기동할 때 Authorization Server의 Discovery 문서를 읽으므로 module-auth를 먼저 띄운다.

```sh
./gradlew :module-auth:bootRun
./gradlew :module-api:bootRun
./gradlew :module-client:bootRun
```

브라우저에서 **http://127.0.0.1:8080** 으로 접속하고 `admin` / `admin`으로 로그인한다.
local 프로파일에서 module-auth가 클라이언트(`mvptest-client`)와 admin 사용자를 등록한다.

- `localhost:8080`으로 접속하면 안 된다. 쿠키는 포트를 구분하지 않으므로 Authorization Server(localhost:9000)와
  세션 쿠키가 섞여 로그인이 실패한다.
- 8080 포트를 쓸 수 없으면 두 곳을 함께 바꾼다.
  - module-client: `--server.port=8081`
  - module-auth: `--myapp.auth.local-client.base-url=http://127.0.0.1:8081` (redirect_uri 등록값)

## 흐름

1. module-client가 Authorization Code + PKCE로 로그인한다. 토큰은 서버 세션에만 보관한다.
2. module-auth가 발급하는 토큰
   - `sub`: 사용자 ID(UUID). 로그인 아이디는 UserInfo의 `preferred_username`으로 제공한다.
   - access token `aud`: `mvptest-api`, `client_id`: 요청한 클라이언트, `authorities`: 역할과 권한
   - access token 5분, refresh token 8시간(사용할 때마다 교체)
3. module-client가 `ApiClient`(HTTP Service Client)로 module-api를 호출하면 access token이 Bearer 헤더로 붙는다.
   만료되면 refresh token으로 갱신한다.
4. module-api는 서명, `iss`, `aud`, 만료를 검증하고 `authorities` 클레임을 권한으로 변환한다.
5. 로그아웃하면 module-client 세션과 module-auth 세션을 함께 끝낸다(RP-Initiated Logout).

## 운영 환경 변수

| 모듈 | 변수 | 설명 |
|------|------|------|
| 공통 | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | 데이터베이스 |
| module-auth | `AUTH_ISSUER_URI` | issuer (예: `https://auth.example.com`) |
| module-auth | `JWK_CERTIFICATE`, `JWK_PRIVATE_KEY` | 토큰 서명 키 PEM (파일 경로 또는 내용) |
| module-api | `AUTH_ISSUER_URI` | 토큰 발급자 |
| module-client | `AUTH_ISSUER_URI`, `OAUTH_CLIENT_SECRET`, `API_BASE_URL` | Authorization Server, 클라이언트 secret, API 주소 |

운영 환경의 클라이언트는 local 등록 코드가 아니라 별도 절차로 `T_OAUTH2_CLIENT`에 등록한다.

토큰 서명 키는 아래처럼 만들 수 있다. 키를 바꾸면 이미 발급한 토큰은 검증되지 않는다.

```sh
openssl req -x509 -newkey rsa:2048 -nodes -days 3650 -subj "/CN=mvptest-auth" \
  -keyout jwk-private-key.pem -out jwk-certificate.pem
```
