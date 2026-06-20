#!/bin/bash
# ALB가 GREEN으로 전환하기 전에, 컨테이너가 실제로 응답하는지 확인
# 실패(non-zero exit) 시 CodeDeploy가 배포를 실패 처리하고 자동 롤백한다
set -uo pipefail

for i in $(seq 1 30); do
  if curl -sf http://localhost:8080/actuator/health > /dev/null; then
    echo "Health check passed"
    exit 0
  fi
  sleep 2
done

echo "Health check failed after timeout"
exit 1
