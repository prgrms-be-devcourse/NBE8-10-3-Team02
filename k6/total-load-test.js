import http from 'k6/http';
import { check, sleep, group } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 50 },  // 30초 동안 50명까지 점진적 증가
        { duration: '1m', target: 100 }, // 1분 동안 100명 유지 (GCP 1GB 임계점 확인)
        { duration: '30s', target: 0 },   // 종료
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'], // 95%의 요청이 2초 이내에 오는지 감시
    },
};

const BASE_URL = 'http://localhost:8080/api/v1';

// --- 헬퍼 함수: 유저의 '생각 시간' 시뮬레이션 ---
function thinkTime() {
    // 1초에서 3초 사이의 랜덤한 시간을 쉬게 하여 유저들이 뿔뿔이 흩어지게 함
    sleep(Math.random() * 2 + 1);
}

export default function () {
    // 1. [익명 활동] 메인 페이지 접속 (모든 유저가 수행)
    group('1. Anonymous Browsing', function () {
        http.batch([
            ['GET', `${BASE_URL}/games/popular/igdb?limit=10`],
            ['GET', `${BASE_URL}/posts?size=10`],
            ['GET', `${BASE_URL}/genres`],
            ['GET', `${BASE_URL}/platforms`]
        ]);
        thinkTime();

        // [확률 분기] 70%의 유저만 검색을 시도함
        if (Math.random() < 0.7) {
            const searchTerms = ['mario', 'zelda', 'action', 'rpg', 'fps'];
            const term = searchTerms[Math.floor(Math.random() * searchTerms.length)];
            const searchRes = http.get(`${BASE_URL}/games/search?query=${term}`);
            check(searchRes, { 'search success': (r) => r.status === 200 });
            thinkTime();

            // [확률 분기] 검색한 유저 중 40%만 상세 페이지로 이동
            if (Math.random() < 0.4) {
                const targetId = Math.floor(Math.random() * 1000) + 1; // 랜덤 ID 시뮬레이션
                http.batch([
                    ['GET', `${BASE_URL}/games/${targetId}`],
                    ['GET', `${BASE_URL}/games/${targetId}/video`],
                    ['GET', `${BASE_URL}/games/${targetId}/similarGames`]
                ]);
                thinkTime();
            }
        }
    });

    // 2. [로그인] 50%의 유저만 로그인을 시도함 (도화님의 CPU 병목 체크)
    let token = '';
    let memberId = 2;
    let apiKey = '';

    if (Math.random() < 0.5) {
        group('2. Authentication', function () {
            const loginData = JSON.stringify({ email: 'user1@test.com', password: '1234' });
            const params = { headers: { 'Content-Type': 'application/json' } };
            const res = http.post(`${BASE_URL}/auth/login`, loginData, params);

            if (check(res, { 'login success': (r) => r.status === 200 })) {
                const data = res.json().data;
                token = data?.accessToken;
                apiKey = data?.apiKey;
                memberId = data?.memberId || 2;
            }
            thinkTime();
        });
    }

    // 3. [회원 전용 활동] 로그인에 성공한 유저만 수행
    if (token) {
        // [랜덤 선택] JWT 인증 혹은 ApiKey 인증 중 하나를 수행 (팀원분 병목 확인용)
        if (Math.random() < 0.5) {
            group('3-A. Member Activity (JWT)', function () {
                const authHeaders = { headers: { 'Authorization': `Bearer ${token}` } };
                http.batch([
                    ['GET', `${BASE_URL}/members/me`, null, authHeaders],
                    ['GET', `${BASE_URL}/recommendations`, null, authHeaders]
                ]);
            });
        } else {
            group('3-B. Member Activity (ApiKey)', function () {
                const apiHeaders = { headers: { 'X-API-KEY': apiKey } };
                http.get(`${BASE_URL}/recommendations?limit=10`, apiHeaders);
            });
        }
        thinkTime();

        // [확률 분기] 로그인 유저 중 30%만 라이브러리나 커뮤니티 활동을 함
        if (Math.random() < 0.3) {
            group('4. Deep Interaction', function () {
                const authHeaders = { headers: { 'Authorization': `Bearer ${token}` } };
                // 내 라이브러리 확인
                http.get(`${BASE_URL}/members/${memberId}/library`, authHeaders);
                // 게시글 좋아요 (POST 작업)
                http.post(`${BASE_URL}/posts/1/like`, null, authHeaders);
            });
            thinkTime();
        }
    }
}