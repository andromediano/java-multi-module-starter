#!/usr/bin/env bash
# .env 값으로 *.template.yaml을 치환해 podman kube play에 넘길 YAML을 만든다.
# 값이 빠졌거나 잘못되면 파일을 만들지 않고 실패한다.
#
# 사용법: ./render.sh [env 파일] [출력 디렉터리]
#   기본값은 이 스크립트 옆의 .env와 이 스크립트가 있는 디렉터리다.
set -euo pipefail

# 셸에 export된 같은 이름의 변수가 섞이지 않도록 빈 환경에서 다시 실행한다
if [[ -z "${RENDER_CLEAN_ENV:-}" ]]; then
  exec env -i RENDER_CLEAN_ENV=1 PATH="$PATH" bash "$0" "$@"
fi

script_dir="$(cd "$(dirname "$0")" && pwd)"
env_file="${1:-$script_dir/.env}"
out_dir="${2:-$script_dir}"

if [[ ! -f "$env_file" ]]; then
  echo "env 파일이 없다: $env_file" >&2
  echo "  cp .env.example .env 후 값을 채운다" >&2
  exit 1
fi

set -a
# shellcheck source=/dev/null
source "$env_file"
set +a

templates=("$script_dir"/*.template.yaml)

# 템플릿이 참조하는 변수 이름을 모두 모은다
vars=$(grep -ohE '\$\{[A-Za-z_][A-Za-z0-9_]*\}' "${templates[@]}" | tr -d '${}' | sort -u)

errors=()
for var in $vars; do
  value="${!var:-}"
  if [[ -z "$value" ]]; then
    errors+=("$var: 값이 비어 있다")
    continue
  fi
  # 템플릿에서 큰따옴표 문자열 안에 들어가므로 YAML을 깨뜨리는 문자를 막는다
  if [[ "$value" == *[\"\\]* ]]; then
    errors+=("$var: \" 와 \\ 는 쓸 수 없다")
  fi
done

if [[ -n "${PROJECT:-}" && ! "$PROJECT" =~ ^[a-z0-9]([-a-z0-9]*[a-z0-9])?$ ]]; then
  errors+=("PROJECT: 소문자, 숫자, - 만 쓸 수 있다")
fi
for var in $vars; do
  if [[ "$var" == *_PORT && -n "${!var:-}" && ! "${!var}" =~ ^[0-9]+$ ]]; then
    errors+=("$var: 숫자여야 한다")
  fi
done
if [[ -n "${NEO4J_PASSWORD:-}" && ${#NEO4J_PASSWORD} -lt 8 ]]; then
  errors+=("NEO4J_PASSWORD: 8자 이상이어야 한다")
fi

if [[ ${#errors[@]} -gt 0 ]]; then
  echo "$env_file 값을 확인한다:" >&2
  printf '  - %s\n' "${errors[@]}" >&2
  exit 1
fi

# 시크릿이 담기므로 생성 파일은 본인만 읽을 수 있게 한다
umask 077
shell_format=$(printf '${%s} ' $vars)
for template in "${templates[@]}"; do
  output="$out_dir/$(basename "$template" .template.yaml).yaml"
  envsubst "$shell_format" < "$template" > "$output.tmp"
  if grep -nF '${' "$output.tmp" >&2; then
    rm -f "$output.tmp"
    echo "치환되지 않은 자리가 남았다: $template" >&2
    exit 1
  fi
  mv "$output.tmp" "$output"
  echo "생성: $output"
done
