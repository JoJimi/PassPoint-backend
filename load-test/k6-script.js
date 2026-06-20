import http from 'k6/http';
import { check, sleep } from 'k6';

// 실행: k6 run -e ALB_DNS=<alb-dns> -e TOKEN=<jwt> k6-script.js
const ALB = __ENV.ALB_DNS;
const TOKEN = __ENV.TOKEN;

export const options = {
  stages: [
    { duration: '30s', target: 10 },   // ramp-up
    { duration: '4m', target: 50 },    // 유지 (이 구간 중간에 v2 블루그린 배포)
    { duration: '30s', target: 0 },    // ramp-down
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],    // 드롭/5xx 1% 미만 = 사실상 무중단
    http_req_duration: ['p(95)<500'],  // p95 < 500ms
  },
};

const authHeaders = { headers: { Authorization: `Bearer ${TOKEN}` } };

export default function () {
  // 1) 공개 헬스체크 (인증 불필요) - 항상 200이어야 함
  const health = http.get(`http://${ALB}/actuator/health`);
  check(health, { 'health 200': (r) => r.status === 200 });

  // 2) 질문 상세 (Postgres 경로, 인증)
  const detail = http.get(`http://${ALB}/api/v1/questions/1`, authHeaders);
  check(detail, { 'detail 2xx': (r) => r.status >= 200 && r.status < 300 });

  // 3) 질문 검색 (Elasticsearch 경로, 인증)
  const search = http.get(`http://${ALB}/api/v1/questions`, authHeaders);
  check(search, { 'search 2xx': (r) => r.status >= 200 && r.status < 300 });

  sleep(1);
}
