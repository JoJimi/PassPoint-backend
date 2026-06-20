#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE_TAG=$(cat "$SCRIPT_DIR/IMAGE_TAG")
AWS_REGION="ap-northeast-2"
SSM_PATH="/passpoint/prod"

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_REGISTRY="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE="${ECR_REGISTRY}/passpoint:${IMAGE_TAG}"

aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_REGISTRY"
docker pull "$IMAGE"

# SSM Parameter Store(/passpoint/prod/*)에서 비밀값/설정값을 한 번에 가져와 -e 옵션으로 변환
# (OpenAI 키, JWT 시크릿, Firebase 서비스 계정 키 등 - 이미지에는 절대 포함하지 않음)
ENV_ARGS=()
while IFS=$'\t' read -r name value; do
  key="${name##*/}"
  value="${value%$'\r'}"   # Windows CRLF로 등록된 SSM 값의 trailing \r 제거 (안 떼면 "172.31.x\r"이 IP가 아니라 호스트명으로 인식돼 UnknownHostException)
  ENV_ARGS+=("-e" "${key}=${value}")
done < <(aws ssm get-parameters-by-path --region "$AWS_REGION" --path "$SSM_PATH" --recursive --with-decryption \
            --query 'Parameters[].[Name,Value]' --output text)

# SSM 조회가 비면(IAM/경로 문제) 환경변수 없이 떠서 조용히 실패하므로, 즉시 실패시켜 원인을 드러낸다
if [ ${#ENV_ARGS[@]} -eq 0 ]; then
  echo "ERROR: SSM에서 파라미터를 하나도 가져오지 못했습니다 ($SSM_PATH)" >&2
  exit 1
fi

docker run -d \
  --name passpoint-app \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  "${ENV_ARGS[@]}" \
  "$IMAGE"
