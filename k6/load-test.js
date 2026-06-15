import http from 'k6/http';
import { check } from 'k6';

export const options = {
    scenarios: {
        ramping: {
            executor: 'ramping-vus',
            startVUs: 1,
            stages: [
                { duration: '30s', target: 200  },  // baseline confirmée
                { duration: '30s', target: 500  },  // montée
                { duration: '30s', target: 1000 },  // charge élevée
                { duration: '60s', target: 1000 },  // plateau — observation stabilité
                { duration: '30s', target: 0    },  // descente
            ],
            gracefulRampDown: '10s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<200', 'p(99)<500'],  // SLO : p95 < 200ms, p99 < 500ms
        http_req_failed:   ['rate<0.01'],               // moins de 1% d'erreurs
    },
};

const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8180';
const REALM        = __ENV.REALM        || 'scolarite';
const CLIENT_ID    = __ENV.CLIENT_ID    || 'client-frontend';
const USERNAME     = __ENV.USERNAME     || 'baye';
const PASSWORD     = __ENV.PASSWORD     || 'passer';

function getToken() {
    const res = http.post(
        `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`,
        {
            grant_type: 'password',
            client_id:  CLIENT_ID,
            username:   USERNAME,
            password:   PASSWORD,
        }
    );

    check(res, { 'token obtenu': (r) => r.status === 200 });

    return res.json('access_token');
}

export function setup() {
    const token = getToken();

    // Warmup — le premier appel déclenche le fetch des JWK keys côté Spring Security
    http.get('http://localhost:8094/api/filieres', {
        headers: { Authorization: `Bearer ${token}` },
    });

    return { token };
}

export default function loadTest(data) {
    const res = http.get('http://localhost:8094/api/filieres', {
        headers: { Authorization: `Bearer ${data.token}` },
    });
    check(res, { 'status 2xx': (r) => r.status < 300 });
}
