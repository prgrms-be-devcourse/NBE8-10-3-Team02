import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 50 },  // 50명까지 점진적 증가
        { duration: '1m',  target: 100 }, // 100명 유지하며 ApiKey 병목 관찰
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'], // 지연 시간 임계치
    },
};

const BASE_URL = 'http://localhost:8080';

// [핵심] 테스트 시작 전 딱 한 번만 실행하여 '로그인 부하'를 제거합니다.
export function setup() {
    const res = http.post(`${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({ email: 'user1@test.com', password: '1234' }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    // 여기서 받은 키들을 아래 default 함수로 넘겨줍니다.
    return {
        accessToken: res.json().data.accessToken,
        apiKey: res.json().data.apiKey
    };
}

export default function (data) {
    // 확률적으로 상황을 나누어 대조군 테스트를 진행합니다.
    if (Math.random() < 0.5) {
        /**
         * [Scenario 1] 정상 로그인 상태 (AccessToken)
         * - 서버: DB 조회 없이 메모리에서 즉시 인증
         * - 기대: 응답 속도 매우 빠름, DB 커넥션 사용 0
         */
        const res = http.get(`${BASE_URL}/api/v1/members/me`, {
            headers: { 'Cookie': `accessToken=${data.accessToken}; apiKey=${data.apiKey}` }
        });
        check(res, { 'Stateless Auth Success': (r) => r.status === 200 });

    } else {
        /**
         * [Scenario 2] Dead Session 상황 (ApiKey 백업)
         * - 서버: 의도적 토큰 누락으로 'findByApiKey' DB 조회 강제
         * - 기대: DB 커넥션 점유 발생, 부하 시 지연 시간 폭발
         */
        const res = http.get(`${BASE_URL}/api/v1/members/me`, {
            headers: { 'Cookie': `apiKey=${data.apiKey}` } // accessToken 제외
        });
        check(res, { 'Dead Session Recovery Success': (r) => r.status === 200 });
    }

    sleep(0.1);
}