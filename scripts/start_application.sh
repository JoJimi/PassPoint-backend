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
  ENV_ARGS+=("-e" "${key}=${value}")
done < <(aws ssm get-parameters-by-path --region "$AWS_REGION" --path "$SSM_PATH" --recursive --with-decryption \
            --query 'Parameters[].[Name,Value]' --output text)

docker run -d \
  --name passpoint-app \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  "${ENV_ARGS[@]}" \
  "$IMAGE"
