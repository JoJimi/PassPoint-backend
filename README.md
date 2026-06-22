able# PassPoint

> AI 기반 CS/기술 면접 연습 안드로이드 앱의 백엔드 (1개월 개인 포트폴리오 프로젝트)

사용자가 면접 질문에 **텍스트 또는 음성**으로 답하면, LLM이 답변을 채점·피드백하고, 처리가 끝나면 푸시 알림으로 알려준다. 의도적으로 **단일 Spring Boot 모놀리식**으로 유지하되, 내부는 동기 → 비동기(Kafka) → 무중단 배포(Blue/Green)까지 단계적으로 발전시켰다.

---

## 핵심 하이라이트

- **무중단 배포 검증** — CodeDeploy Blue/Green + ALB 환경에서 배포 중 k6 부하 테스트: **23,544건 요청 중 실패 0.00%, p95 12.74ms** (5xx 0건)
- **느린 외부 호출 / 트랜잭션 분리** — LLM·STT 같은 수 초~수십 초짜리 호출을 `@Transactional` 밖으로 빼 커넥션 풀 고갈 방지 (`tx1 → 외부 호출 → tx2` 패턴)
- **완전 비동기 파이프라인** — `답변 제출 → Kafka → STT → LLM 피드백 → 완료 이벤트 → FCM 푸시`를 이벤트 기반으로 연결
- **동기→비동기 드롭인 교체** — `answers.status` enum과 `GET /answers/{id}` 응답 계약을 처음부터 고정해, 비동기 전환 후에도 클라이언트 인터페이스 불변

---

## 기술 스택

| 분류 | 사용 기술 |
|---|---|
| 언어 / 프레임워크 | Java, Spring Boot |
| 데이터 | PostgreSQL (jsonb), Redis (스트릭·카운터·캐시·로그아웃 블랙리스트) |
| 검색 | Elasticsearch 9.2.8 + Nori 한글 형태소 분석 (Docker 커스텀 이미지) |
| AI | Spring AI + OpenAI (gpt-4o-mini 피드백, Whisper STT) |
| 인증 | Spring Security + OAuth2(Google) + JWT |
| 비동기 | Apache Kafka (Spring for Kafka) |
| 알림 | Firebase Cloud Messaging (Admin SDK) |
| 스토리지 | AWS S3 (로컬은 MinIO로 동일 인터페이스) |
| 인프라 / 배포 | AWS EC2, Docker, CodeDeploy(Blue/Green), ALB, SSM Parameter Store |
| 모니터링 | Spring Actuator + Micrometer → Prometheus → Grafana |
| CI/CD | GitHub Actions (build·test·ECR push·CodeDeploy trigger) |
| 테스트 | JUnit 5, Mockito, Testcontainers (singleton 컨테이너) |
| 문서 | Swagger (springdoc-openapi) |

---

## 아키텍처

### 답변 처리 비동기 파이프라인

```
POST /api/v1/answers (202 PENDING 즉시 반환)
  │
  ├─ TEXT ─────────────────────────────┐
  │                                     ▼
  └─ VOICE → Kafka(audio.uploaded)      │
              → SttWorker               │
                (S3 다운로드 + Whisper)  │
                → Kafka(feedback.requested) ◄┘   ← 텍스트·음성 합류 지점
                     → FeedbackWorker
                        (LLM 피드백 생성, 트랜잭션 밖)
                        → Kafka(feedback.completed)
                             → FeedbackCompletedConsumer
                                → FCM 푸시 발송 (Firebase Admin SDK)

GET /api/v1/answers/{id}  ← 클라이언트는 폴링으로 DONE 확인 (FCM은 보조 알림)
```

`answers.status`: `PENDING → TRANSCRIBING(음성) → ANALYZING → DONE / FAILED`

### 배포 아키텍처 (4주차)

```
GitHub push → GitHub Actions (test → Docker build → ECR push → CodeDeploy trigger)
                                                                      │
                                                                      ▼
인프라 EC2 (상시)                          ALB ── CodeDeploy Blue/Green
 ├ PostgreSQL  ├ Redis                      │      ├ BLUE  target group (v1)
 ├ Elasticsearch ├ Kafka                    │      └ GREEN target group (v2)
 └ Prometheus + Grafana                     │            ↑ 헬스체크 통과 후 트래픽 전환
                                       앱 EC2 (Blue/Green 교체)
```

- **인프라 EC2 / 앱 EC2 분리**: 상태(state)를 가진 무거운 인프라는 상시 유지하고, 자주 교체되는 앱만 Blue/Green으로 배포 → 배포가 인프라를 건드리지 않음
- **비밀값은 SSM Parameter Store**에서 기동 시 로드 (이미지·코드에 하드코딩 없음), S3 접근은 EC2 IAM Role

---

## 주차별 진행

### 1주차 — 기반
- Spring Boot 프로젝트 셋업, Spring Security + OAuth2(Google) + JWT 인증
- `GlobalExceptionHandler` 구조화 에러 응답 포맷, 공통 `BaseEntity`(Auditing)
- 로그아웃은 Redis 블랙리스트로 토큰 무효화

### 2주차 — 텍스트 답변 → AI 피드백 (동기 수직 슬라이스)
- 질문/답변/피드백/즐겨찾기 도메인, JWT 인증 적용
- **Elasticsearch + Nori**로 질문 키워드 검색 (동적 bool 쿼리는 ES 커스텀 레포, 단순 `user_id` 조회는 JPA로 분리)
- LLM 호출을 `@Transactional` 밖으로 빼는 패턴 확립
- 이후 비동기 전환을 대비해 `answers.status` enum과 폴링 엔드포인트를 미리 설계

### 3주차 — 비동기 전환 (가장 난이도 높은 주차)
- `POST /answers`를 동기(`201 DONE`) → 비동기(`202 PENDING`)로 전환, Kafka 워커가 백그라운드 처리
- 음성 답변: S3 Presigned URL 업로드 → `audio.uploaded` → STT(Whisper) → `feedback.requested`로 합류
- AFTER_COMMIT 이벤트 발행으로 dual-write 문제 대응, 멱등 상태 전이
- Testcontainers singleton 패턴으로 통합 테스트 고속화

### 4주차 — 배포 · 무중단 · 관측 · 푸시 알림 ⭐
- AWS EC2 + Docker, CodeDeploy **Blue/Green** + ALB로 무중단 배포
- GitHub Actions CI/CD (외부 유료 API는 mock, Testcontainers로 통합 테스트)
- Actuator → Prometheus → Grafana 모니터링
- **FCM 푸시 파이프라인 완성**: 피드백 완료 Kafka 이벤트 → 자동 푸시 발송, 무효 토큰 자동 정리·멱등 등록
- **k6 무중단 검증**: 배포 중 트래픽을 흘려 요청 드롭 0 / 5xx 0 증명

---

## 설계 의도 (면접 이야깃거리)

- **의도적 모놀리식** — 포트폴리오 데모가 목적이라 마이크로서비스로 쪼개지 않고 단일 Spring Boot로 유지. 비동기 분리는 Kafka 컨슈머 그룹으로 논리적으로만 나눔
- **느린 외부 호출과 트랜잭션 경계** — LLM/STT는 절대 트랜잭션 안에서 호출하지 않음 (`tx1 상태저장 → 트랜잭션 밖 외부호출 → tx2 결과저장+상태전이`)
- **저장소 선택 기준** — 동적 검색은 Elasticsearch, 단순 조회·페이징은 JPA. ES 패턴을 남용하지 않음
- **인프라/앱 EC2 분리** — 상태 있는 인프라는 유지하고 무상태 앱만 Blue/Green 교체
- **비밀값 관리** — 로컬은 `.env`, 운영은 SSM Parameter Store + IAM Role (코드·이미지에 자격증명 미포함)
- **동기→비동기 드롭인 교체** — 인터페이스(응답 계약) 고정, 내부 타이밍만 변경

---

## 무중단 배포 검증 결과 (k6)

배포가 진행되는 도중 ALB로 부하를 흘렸을 때:

```
✓ http_req_failed   rate=0.00%   (0 out of 23544)
✓ http_req_duration p(95)=12.74ms   (임계값 500ms)
✓ checks_succeeded  100.00%   (23544 / 23544)
```

Blue/Green 구조상 전환(또는 실패 시 롤백) 중에도 한쪽 인스턴스가 항상 서빙하므로 요청이 끊기지 않음을 데이터로 확인. Grafana HTTP 패널에서도 부하 구간 내내 5xx 시리즈가 발생하지 않음.

> 데모 검증 후 AWS 리소스는 과금 방지를 위해 전부 정리(teardown)함.

---

## 빌드 / 실행 / 테스트

```bash
# 로컬 인프라 기동 (PostgreSQL · Redis · Elasticsearch · Kafka · MinIO)
docker compose up -d

# 빌드
./gradlew build

# 단위 + 통합 테스트 (Testcontainers, Docker 필요)
./gradlew test

# 앱 실행 (기본 dev 프로파일)
./gradlew bootRun
```

환경변수는 `.env`로 주입 (DB·OpenAI·JWT·Google OAuth·Firebase·S3 등). API 문서는 실행 후 `/swagger-ui.html`.

---

## 디렉터리 구조

```
src/main/java/org/example/passpoint/
├─ domain/
│  ├─ answer/      # 답변 제출·이력, 비동기 워커(SttWorker, FeedbackWorker)
│  ├─ auth/        # OAuth2 로그인, JWT 발급·갱신·로그아웃
│  ├─ bookmark/    # 질문 즐겨찾기
│  ├─ fcmtoken/    # FCM 토큰 관리, 푸시 발송, 완료 이벤트 컨슈머
│  ├─ feedback/    # AI 피드백 생성·저장
│  ├─ question/    # 질문 검색(Elasticsearch)·조회
│  ├─ studylog/    # 학습 기록·스트릭(Redis)
│  ├─ tag/         # 태그
│  └─ user/        # 사용자
└─ global/         # 보안·JWT·Kafka·S3·예외·설정 등 공통

docker-compose.yml             # 로컬 개발 인프라
docker-compose.infra.yml       # 운영 인프라 EC2용
docker-compose.monitoring.yml  # Prometheus + Grafana
Dockerfile                     # 앱 멀티스테이지 빌드
appspec.yml, scripts/          # CodeDeploy Blue/Green 배포 훅
load-test/k6-script.js         # 무중단 검증 부하 스크립트
.github/workflows/ci.yml       # CI/CD 파이프라인
```
