# SETUP

`.env`에 프로젝트 값을 채우고, `render.sh`로 템플릿에서 `secret.yaml`과 `pod.yaml`을 만든다.

```sh
cp .env.example .env   # 값을 채운다
./render.sh
```

```sh
podman kube play secret.yaml
```

```sh
podman kube play pod.yaml
```

템플릿이나 `.env`를 바꾼 뒤에는 다시 렌더링하고 교체한다.

```sh
./render.sh
podman kube play --replace secret.yaml
podman kube play --replace pod.yaml
```

계정 값(`POSTGRES_*`, `NEO4J_*`)은 볼륨이 처음 만들어질 때만 반영된다. 바꾼 값을 적용하려면 볼륨을 지우고 다시 띄운다.

```sh
podman kube down --force pod.yaml   # 파드와 볼륨 삭제
```
