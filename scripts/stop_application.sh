#!/bin/bash
# 기존 컨테이너 정리 (첫 배포라 컨테이너가 없으면 그냥 통과)
docker stop passpoint-app 2>/dev/null || true
docker rm passpoint-app 2>/dev/null || true
