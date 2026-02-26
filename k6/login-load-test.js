import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 20 },  // 1단계: 100명 (서서히 압박)
        { duration: '1m',  target: 50 },  // 2단계: 200명 (본격적인 부하)
        { duration: '1m',  target: 100 },  // 3단계: 300명 (한계 지점 탐색)
        { duration: '30s', target: 0 },    // 종료
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],  // 기준을 더 엄격하게 (0.5초 이내)
        http_req_failed: ['rate<0.01'],    // 에러율 1% 미만 (에러가 나면 바로 경고)
    },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
    const loginUrl = `${BASE_URL}/api/v1/auth/login`;

    // [전략 1] 동일한 계정으로 동시 요청 (DB Lock 유도)
    // 모든 VU가 'user1@test.com'으로 로그인을 시도하게 두면,
    // DB에서 해당 유저의 '최근 로그인 시간' 등을 업데이트할 때 경합(Lock)이 생길 수 있습니다.
    const payload = JSON.stringify({
        email: 'user1@test.com',
        password: '1234',
    });

    const params = {
        headers: { 'Content-Type': 'application/json' },
        tags: { endpoint: 'login' },
    };

    const res = http.post(loginUrl, payload, params);

    check(res, {
        'login status 200': (r) => r.status === 200,
        'has token': (r) => r.json().data.accessToken !== undefined,
    });

    // [전략 2] 쉬는 시간 단축 (0.1초)
    // 서버가 숨 돌릴 틈 없이 요청을 쏟아붓게 만듭니다.
    sleep(0.1);
}