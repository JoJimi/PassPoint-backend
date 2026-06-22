# PassPoint — AI 기반 CS/기술 면접 연습 백엔드

> 면접 질문을 검색하고, 텍스트·음성으로 답변하면 AI가 점수·피드백·모범답안을 돌려주는 서비스의 **Spring Boot 백엔드**입니다.
> 느린 외부 AI 작업(STT·LLM)을 Kafka로 비동기 분리하고, 블루그린 무중단 배포로 마무리하는 것을 핵심 목표로 잡았습니다.

---

## 목차

1. [한눈에 보기](#한눈에-보기)
2. [기술 스택](#기술-스택)
3. [시스템 아키텍처](#시스템-아키텍처)
4. [핵심 기능](#핵심-기능)
5. [API 명세](#api-명세)
6. [개발 일정 (주차별 진행)](#개발-일정-주차별-진행)
7. [테스트 전략](#테스트-전략)
8. [트러블 슈팅](#트러블-슈팅)
9. [향후 발전 방향](#향후-발전-방향)
10. [로컬 실행 방법](#로컬-실행-방법)

---

## 한눈에 보기

| 항목 | 내용 |
|------|------|
| 도메인 | 면접 질문 검색 / 답변 제출 / AI 피드백 / 학습 기록 / 인증 |
| 비동기 처리 | Kafka 파이프라인 (`audio.uploaded → feedback.requested → feedback.completed`) |
| 외부 AI | OpenAI STT(Whisper) + LLM 피드백(gpt-4o-mini, 구조화 JSON 출력) |
| 검색 | Elasticsearch + Nori(한글 형태소) |
| 캐시 | Redis (스트릭·오늘 푼 문제 수·JWT 블랙리스트·Refresh 토큰) |
| 알림 | FCM(Firebase Admin SDK) — "피드백 완료" 푸시 |
| 배포 | EC2 + ALB + CodeDeploy 블루그린 + GitHub Actions |
| 모니터링 | Actuator + Micrometer → Prometheus → Grafana, k6 부하 테스트 |

---

## 기술 스택

### 언어 / 프레임워크
- **Java 21**, **Spring Boot 4.0.6**
- Spring Web MVC, Spring Data JPA, Spring Security
- Spring for Apache Kafka — 비동기 STT/피드백 파이프라인
- Spring AI 2.0 — OpenAI 챗(피드백) / 오디오(STT) 연동
- Lombok, springdoc-openapi (Swagger)

### 데이터 / 저장소
- **PostgreSQL** — 영속 데이터 (users, questions, answers, feedbacks 등). `goodPoints`, `keywordPool` 등은 `jsonb` 컬럼 활용
- **Redis** — 스트릭/오늘 푼 문제 수(TTL·날짜 키), Refresh 토큰, 로그아웃 블랙리스트
- **Elasticsearch (+ Nori)** — 질문 검색·필터 (한글 형태소 분석)
- **AWS S3 / MinIO(로컬)** — 음성 녹음 파일 저장 (Presigned URL 직접 업로드)

### 인프라 / 배포 / 모니터링
- AWS EC2(앱용/인프라용 분리), ALB, CodeDeploy(블루그린), SSM Parameter Store(시크릿)
- GitHub Actions CI/CD, Docker / Docker Compose
- Actuator + Micrometer → Prometheus → Grafana, k6

### 외부 API
- OpenAI API (LLM 피드백 + STT)
- Google / Kakao OAuth (소셜 로그인)
- Firebase Cloud Messaging (푸시 알림)

---

## 시스템 아키텍처

### 비동기 파이프라인 (핵심 설계)

답변 제출은 즉시 `202 ACCEPTED`로 반환하고, 무거운 작업은 Kafka 컨슈머가 뒤에서 처리합니다.

```
[텍스트 답변]
  POST /answers ──tx1: 저장(PENDING) + feedback.requested 발행──▶ FeedbackWorker ──▶ LLM ──▶ DONE

[음성 답변]
  POST /answers ──tx1: 저장(PENDING) + audio.uploaded 발행──▶ SttWorker
       ──S3 다운로드 + Whisper STT──▶ feedback.requested ──▶ FeedbackWorker ──▶ LLM ──▶ DONE

[완료 후]
  feedback.completed 발행 ──▶ FeedbackCompletedConsumer ──▶ FCM 푸시
```

`feedback.requested`가 **텍스트·음성 답변이 합류하는 지점**입니다. 음성은 STT를 한 단계 더 거친 뒤 같은 토픽으로 들어오기 때문에, FeedbackWorker 이후 로직은 입력 종류와 무관하게 동일하게 재사용됩니다.

### 트랜잭션 경계 설계

각 워커는 **DB 트랜잭션과 외부 호출을 분리**합니다.

```
tx1 (짧은 트랜잭션): 멱등 체크 + 상태 전이 (PENDING → ANALYZING)
  ↓ 트랜잭션 밖
LLM / STT 호출 (수십 초 걸릴 수 있는 외부 I/O)
  ↓
tx2 (짧은 트랜잭션): 결과 저장 + 상태 전이(DONE/FAILED) + 다음 이벤트 발행
```

외부 호출을 트랜잭션 안에 묶으면 커넥션을 수십 초 점유하므로, 상태 전이만 짧게 커밋하고 외부 호출은 트랜잭션 밖에서 수행합니다.

### Dual-write 대응 (Outbox 경량 버전)

DB 커밋과 Kafka 발행을 한 트랜잭션으로 묶을 수 없는 문제(dual-write)에 대해, 서비스 계층은 트랜잭션 안에서 `KafkaPublishEvent`(Spring 애플리케이션 이벤트)만 발행하고, `AfterCommitKafkaPublisher`가 `@TransactionalEventListener(AFTER_COMMIT)`로 **커밋이 끝난 뒤에만** 실제 Kafka로 발행합니다. 롤백되면 메시지가 나가지 않습니다.

### 인증 흐름

- **구글**: 앱이 보낸 ID 토큰을 구글 공개키로 검증(서명·만료·audience)
- **카카오**: 액세스 토큰으로 `/v2/user/me` 호출 성공 여부로 검증 (JWT가 아니라 자체 검증 불가)
- **이메일**: BCrypt 해시 비교
- 공통: 로그인 성공 시 Access/Refresh 발급 → Refresh는 Redis 저장(TTL 14일, 재로그인 시 덮어쓰기=롤링), 로그아웃 시 Access를 남은 TTL만큼 블랙리스트 등록

---

## 핵심 기능

### 1. 질문 검색 (Elasticsearch + Nori)
키워드(multi_match: title·content)와 카테고리(term filter)를 bool 쿼리로 동적 조립합니다. Nori 형태소 분석 덕분에 `"가비지컬렉션"`처럼 붙여 써도 `"가비지 컬렉션"` 문서가 검색됩니다. 점수가 필요한 키워드는 `must`, 단순 거르기인 카테고리는 `filter`로 분리해 캐싱 이점을 살렸습니다.

### 2. 답변 제출 & 비동기 처리
텍스트/음성 모두 접수 즉시 `PENDING`으로 저장하고 202 반환. 상태는 `PENDING → (TRANSCRIBING) → ANALYZING → DONE/FAILED`로 전이되며, 클라이언트는 `GET /answers/{id}` 폴링 또는 FCM 푸시로 완료를 감지합니다.

### 3. AI 피드백 (구조화 출력 + 환각 방지)
- `BeanOutputConverter`로 JSON Schema를 강제해 LLM이 정해진 구조(`accuracyScore`, `structureScore`, `completenessScore`, `goodPoints`, `improvementPoints`, `coveredKeywords`)로만 응답하도록 함
- **총점은 LLM이 아니라 서버에서** 세부 점수 평균으로 계산 (`computeScore`)
- **모범답안은 LLM에 요청하지 않고** Question에 저장된 값을 그대로 사용 (비용·환각 절감)
- `coveredKeywords`는 `question.keywordPool`과 **교집합만 남겨** LLM이 없는 키워드를 만들어내는 환각을 차단

### 4. 학습 기록 & 스트릭 (역할 분리)
- **RDB(study_logs)**: 일자별 풀이 수 영속 (추이 분석용, `(user_id, study_date)` 유니크)
- **Redis**: 연속 학습일·오늘 풀이 수 캐시 (빠른 조회). 마지막 학습일이 어제면 +1, 끊겼으면 1로 리셋

### 5. FCM 푸시
`feedback.completed`를 **전용 컨슈머 그룹(`passpoint-fcm-group`)**으로 구독해 기존 폴링 흐름과 독립 동작. 한 사용자가 여러 기기 토큰을 가질 수 있어 `fcm_tokens` 1:N 설계. 발송 시 `UNREGISTERED`/`INVALID_ARGUMENT` 응답이 오면 죽은 토큰을 DB에서 자동 정리합니다.

---

## API 명세

전체 명세는 실행 후 `http://localhost:8080/swagger-ui.html`에서 확인할 수 있습니다.

| 컨트롤러 | 주요 엔드포인트 |
|----------|-----------------|
| **Auth** | `POST /auth/login/{google\|kakao\|email}`, `POST /auth/signup/email`, `POST /auth/refresh`, `POST /auth/logout` |
| **User** | `GET·PATCH /users/me`, `GET /users/me/stats`, `POST·DELETE /users/me/fcm-token` |
| **Question** | `GET /questions` (검색·필터), `GET /questions/{id}` |
| **Answer** | `POST /answers/audio/presignedurl`, `POST /answers`, `GET /answers/{id}` (폴링), `GET /answers`, `GET /questions/{id}/answers` |
| **Feedback** | `GET /feedbacks/{answerId}` |
| **Bookmark** | `POST /bookmarks`, `DELETE /bookmarks/{questionId}`, `GET /bookmarks` |
| **StudyLog** | `GET /study-logs/streak` |

> 모든 보호된 API는 JWT(`Authorization: Bearer ...`) 필요. 로그인/회원가입/갱신, Swagger, Actuator만 공개입니다.

---

## 개발 일정 (주차별 진행)

| 주차 | 목표 | 백엔드 핵심 산출물 |
|------|------|---------------------|
| **1주차** | 기반 + 인증 + 질문 검색 | Docker Compose 인프라, JPA 엔티티 9테이블, OAuth2+JWT 이식, ES 색인(Nori), Swagger |
| **2주차** | 답변·피드백 (동기 먼저) | 텍스트 답변 API, Spring AI 동기 피드백 + keyword_pool 환각 방지, 스트릭/통계, JUnit·Testcontainers 시작 |
| **3주차** | 비동기 파이프라인 + 음성 | S3 Presigned URL, Kafka 4토픽, STT 워커, 답변 status 흐름 + 폴링, Testcontainers(Kafka·Redis) |
| **4주차** | 배포 + 블루그린 + 마무리 | EC2/ALB/CodeDeploy 블루그린, GitHub Actions, Prometheus+Grafana, FCM 통합, k6 무중단 검증 |

**현재 상태**: 4주차 진행 중 — 배포 파이프라인 + FCM 푸시 end-to-end 검증 단계. 텍스트/음성 답변 → 피드백 비동기 흐름은 통합 테스트로 검증 완료.

---

## 테스트 전략

### 테스트 철학
- **유료/비결정적 외부 API(LLM·STT)만 `@MockitoBean`으로 교체**하고, DB·Redis·Kafka·S3는 **실제 컨테이너(Testcontainers)로 검증**합니다. 모킹을 최소화해 "실제로 도는지"를 확인합니다.
- 컨테이너는 **싱글톤 패턴**(static + JVM당 1회 `start()`)으로 띄워, `@MockitoBean` 조합 차이로 컨텍스트가 갈라져도 컨테이너는 1세트만 공유합니다.

### 현재 작성된 테스트

| 유형 | 대상 | 검증 내용 |
|------|------|-----------|
| 통합 | `AnswerFlowIntegrationTest` | 텍스트/음성 제출→폴링→DONE/FAILED 전 과정, score 정렬, 권한 차단, 스트릭 기록 (PostgreSQL+Redis+Kafka+MinIO) |
| 통합 | `BookmarkFlowIntegrationTest` | 즐겨찾기 등록(멱등)·조회·삭제 |
| 통합 | `QuestionSearchIntegrationTest` | Nori 형태소 검색, 카테고리 필터 |
| 통합 | `KafkaSmokeTest` | 발행→수신 스모크 |
| 단위 | `AnswerServiceTest` | 입력 검증, score 정렬 분기, 카테고리 필터 |
| 단위 | `AuthServiceTest` | 소셜/이메일 로그인·가입 분기, 이메일 중복 차단 |
| 단위 | `FeedbackWorkerTest` | 멱등성, 성공/실패 분기 |
| 단위 | `StudyLogServiceTest` | 스트릭 증가/리셋, TTL |
| 단위 | `SpringAiFeedbackGeneratorTest` | keyword_pool 교집합 필터(환각 방지) |

### 💡 추가하면 좋을 테스트 (피드백)

지금 비즈니스 가치가 높은데 테스트가 비어 있는 곳들입니다. 우선순위 순으로 제안합니다.

1. **`AuthService.reissue` — Refresh 토큰 롤링 & 재사용 탐지** (최우선)
   보안 핵심 로직인데 단위 테스트가 없습니다. ① 저장된 토큰과 일치하면 새 토큰 발급, ② 불일치(재사용 의심) 시 `RefreshTokenMisMatchException` + Redis에서 강제 삭제, ③ 저장된 게 없을 때(만료/로그아웃) 분기를 검증할 가치가 큽니다.

2. **`AuthService.logout` — 블랙리스트 등록**
   남은 TTL만큼만 Access 토큰이 블랙리스트에 들어가는지, `remaining <= 0`이면 등록을 건너뛰는지 확인하면 좋습니다.

3. **`FcmTokenService.registerToken` — 멱등/소유자 변경 분기**
   ① 같은 사용자+같은 토큰 → `touchUpdatedAt`, ② 다른 사용자 토큰(기기 재로그인) → `reassignOwner`, ③ 처음 보는 토큰 → 신규 저장. 세 분기가 모두 분기 커버 대상입니다.

4. **`FcmNotificationService` — 무효 토큰 자동 정리**
   `FirebaseMessagingException`의 errorCode가 `UNREGISTERED`/`INVALID_ARGUMENT`일 때만 `deleteByToken`이 호출되고, 그 외 에러(네트워크 등)에서는 토큰을 지우지 않는지 검증. `FirebaseMessaging`을 모킹하면 됩니다.

5. **`FeedbackCompletedConsumer` — 발송 실패 격리**
   한 건 발송 실패가 예외로 컨슈머를 멈추지 않고 로그만 남기고 삼키는지(이후 알림이 막히지 않는지) 확인.

6. **`JwtProvider` — 토큰 만료/위조**
   만료 토큰 → `ExpiredTokenException`, 변조 토큰 → `InvalidTokenException` 매핑은 인증의 기반이라 단위 테스트로 못 박아두면 안전합니다.

7. **`SttWorker` 멱등성** (`markTranscribing`가 PENDING이 아니면 null 반환 → 건너뜀)
   현재 통합 테스트의 happy/fail 경로는 있지만, **중복 메시지 수신 시 건너뛰는** 멱등 경로는 별도 단위 테스트로 보강하면 좋습니다.

---

## 트러블 슈팅

실제로 막혔던 지점과 해결책입니다. 면접에서 "왜 이렇게 했나"를 설명하기 좋은 포인트들입니다.

### 1. SSM 값의 Windows CRLF(`\r`) 문제
SSM Parameter Store에 Windows(Git Bash)에서 등록한 값에 `\r`이 붙어, `start_application.sh`에서 `-e KEY=172.31.x\r`로 컨테이너에 주입돼 호스트명이 깨지고 `UnknownHostException`이 발생했습니다.
→ 값 파싱 시 `value="${value%$'\r'}"`로 trailing `\r`을 제거.

### 2. SSM 조회 실패가 "조용히" 넘어가는 문제
IAM·경로 문제로 SSM에서 파라미터를 하나도 못 가져오면, 환경변수 없이 컨테이너가 떠서 원인 불명으로 죽었습니다.
→ `ENV_ARGS`가 비면 `exit 1`로 **즉시 실패(fail fast)**시켜 원인을 드러냄.

### 3. Docker 빌드 서머리에 base64 시크릿 노출
`docker/build-push-action`의 빌드 서머리 단계에서 base64 페이로드가 에러로 새는 버그가 있어, CI에서 `DOCKER_BUILD_SUMMARY: false`로 비활성화.

### 4. Firebase "FirebaseApp already exists"
테스트에서 컨텍스트가 여러 번 뜰 때 `FirebaseApp.initializeApp`이 중복 호출돼 실패.
→ `FirebaseApp.getApps().isEmpty()` 체크 후 없을 때만 초기화, 있으면 기존 인스턴스 재사용.

### 5. score 정렬 시 채점 미완료 답변 처리
점수는 `Answer`가 아닌 `Feedback`에 있어 `Pageable.sort`로 바로 정렬할 수 없었습니다. 또 채점이 안 끝난(score 없는) 답변이 정렬 방향에 따라 맨 앞에 오는 문제가 있었습니다.
→ score 전용 JPQL 쿼리로 분리하고 `ORDER BY f.score DESC NULLS LAST`로 미채점 답변은 **항상 마지막**에 오도록 고정.

### 6. dual-write (DB 커밋 ↔ Kafka 발행)
DB는 커밋됐는데 Kafka 발행이 실패하거나, 그 반대 상황이 생길 수 있었습니다.
→ 트랜잭션 안에서는 애플리케이션 이벤트만 발행하고, `AFTER_COMMIT` 시점에 실제 Kafka로 발행해 **롤백 시 메시지가 나가지 않도록** 보장.

### 7. 카카오 토큰 검증
카카오 액세스 토큰은 서명 검증 가능한 JWT가 아니라서 자체 검증이 불가능했습니다.
→ `/v2/user/me` 호출 성공 여부로 토큰 유효성을 대신 판단. 401이면 `OAuthAuthenticationFailedException`.

### 8. provider 간 이메일 충돌
같은 이메일을 이메일 가입과 구글/카카오 가입이 각각 만들면 계정이 쪼개집니다.
→ 신규 가입 시 `existsByEmail`로 provider 불문 이메일 중복을 차단.

---

## 향후 발전 방향

- **DLQ(Dead Letter Queue)**: 현재는 워커 실패 시 `FAILED` 마킹 후 종료. 재처리 큐를 두어 일시 장애(OpenAI 5xx 등)에 대해 자동 재시도하면 신뢰성을 높일 수 있음.
- **Outbox 테이블 정식 도입**: 지금은 `AFTER_COMMIT` 발행으로 dual-write를 완화했지만, 애플리케이션이 커밋 후 발행 직전에 죽으면 메시지 유실 가능. Outbox 테이블 + 별도 릴레이로 at-least-once를 보장할 수 있음.
- **추천/랜덤 질문, 성적 리포트, 카테고리별 질문 수** 등 명세에는 있으나 미구현 엔드포인트 완성.
- **태그(N:M) 연동**: 현재 `QuestionDocument.tags`는 빈 리스트로 색인 중. 태그 기반 필터 추가.
- **STT 토픽 분리**: 현재 `audio.transcribed` 토픽을 거치지 않고 SttWorker가 `feedback.requested`로 직접 이어줌. 단계를 명시적으로 나누면 관찰성·재처리가 쉬워짐.
- **테스트 커버리지 보강**: 위 [테스트 피드백](#-추가하면-좋을-테스트-피드백) 항목(특히 Refresh 롤링·FCM 분기) 우선 적용.
- **모니터링 알람**: Grafana에 5xx·컨슈머 lag·STT/LLM 실패율 알람 규칙 추가.

---

## 로컬 실행 방법

```bash
# 1. 인프라 기동 (PostgreSQL, Redis, Elasticsearch, Kafka)
docker compose up -d

# 2. 환경변수 준비 (.env — 반드시 gitignore 처리)
#   OPENAI_API_KEY, GOOGLE_CLIENT_ID, JWT_SECRET, FIREBASE_CREDENTIALS_JSON,
#   POSTGRES_*, S3_* 등

# 3. 애플리케이션 실행 (dev 프로필이 기본)
./gradlew bootRun

# 4. 테스트 (Testcontainers가 컨테이너를 자동 기동)
./gradlew test
```

- API 문서: `http://localhost:8080/swagger-ui.html`
- 메트릭: `http://localhost:8080/actuator/prometheus`

> ⚠️ 시크릿(OpenAI 키, Firebase 서비스 계정 키, JWT 시크릿, OAuth 클라이언트 시크릿)은 절대 커밋하지 마세요. 로컬은 `.env`, 운영은 SSM Parameter Store에서 주입됩니다. `.env`가 `.gitignore`에 포함되어 untracked 상태인지 꼭 확인하세요.
