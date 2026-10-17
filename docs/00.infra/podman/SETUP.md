# SETUP

프로젝트 루트의 `.env`에 값을 채우고, `render.sh`로 템플릿에서 `secret.yaml`과 `pod.yaml`을 만든다.
같은 `.env`를 앱도 읽으므로 DB 접속 정보는 이 파일에서만 관리한다.

아래 명령은 모두 프로젝트 루트에서 실행한다.

```sh
cp .env.example .env   # 값을 채운다
docs/00.infra/podman/render.sh
```

```sh
podman kube play docs/00.infra/podman/secret.yaml
```

```sh
podman kube play docs/00.infra/podman/pod.yaml
```

템플릿이나 `.env`를 바꾼 뒤에는 다시 렌더링하고 교체한다.

```sh
docs/00.infra/podman/render.sh
podman kube play --replace docs/00.infra/podman/secret.yaml
podman kube play --replace docs/00.infra/podman/pod.yaml
```

계정 값(`POSTGRES_*`, `NEO4J_*`)은 볼륨이 처음 만들어질 때만 반영된다. 바꾼 값을 적용하려면 볼륨을 지우고 다시 띄운다.

```sh
podman kube down --force docs/00.infra/podman/pod.yaml   # 파드와 볼륨 삭제
```
