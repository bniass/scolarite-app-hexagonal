/**
 * Stress Test — montée progressive en charge jusqu'au point de rupture
 *
 * Paliers : 90 VUs → 150 → 200 → 300 → 500
 * Objectif : trouver le seuil où p(95) > 1000ms ou taux d'erreur > 1%
 *
 * Usage :
 *   k6 run stress-test.js \
 *     -e MONTANT=150000 \
 *     -e CLASSE_ID=<uuid> \
 *     [-e CODE_ANNEE=2025-2026] \
 *     [-e USERNAME=baye] [-e PASSWORD=passer]
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 30  },  // warm-up
        { duration: '1m',  target: 90  },  // baseline (niveau test nominal)
        { duration: '2m',  target: 90  },  // stabilisation baseline
        { duration: '1m',  target: 150 },  // montée palier 1
        { duration: '2m',  target: 150 },  // stabilisation
        { duration: '1m',  target: 200 },  // montée palier 2
        { duration: '2m',  target: 200 },  // stabilisation
        { duration: '1m',  target: 300 },  // montée palier 3
        { duration: '2m',  target: 300 },  // stabilisation
        { duration: '1m',  target: 500 },  // pic max
        { duration: '2m',  target: 500 },  // stabilisation pic
        { duration: '1m',  target: 0   },  // descente
    ],
    thresholds: {
        'http_req_duration{endpoint:metier}': ['p(95)<2000'],
        'http_req_failed{endpoint:metier}':   ['rate<0.05'],
    },
};

// ─── Configuration ────────────────────────────────────────────────────────────
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8180';
const REALM        = __ENV.REALM        || 'scolarite';
const CLIENT_ID    = __ENV.CLIENT_ID    || 'client-frontend';
const USERNAME     = __ENV.USERNAME     || 'baye';
const PASSWORD     = __ENV.PASSWORD     || 'passer';
const CODE_ANNEE   = __ENV.CODE_ANNEE   || '2025-2026';
const MONTANT      = parseFloat(__ENV.MONTANT || '0');
if (!MONTANT) throw new Error('MONTANT est obligatoire — passez -e MONTANT=<valeur>');

const RUN_ID          = Date.now();
const INSCRIPTION_URL = 'http://localhost:8091/api/inscriptions';
const PAIEMENT_URL    = __ENV.PAIEMENT_URL || 'http://localhost:8093';

// ─── Setup ───────────────────────────────────────────────────────────────────
export function setup() {
    const classeId = __ENV.CLASSE_ID || '';
    if (!classeId) throw new Error('CLASSE_ID est obligatoire — passez -e CLASSE_ID=<uuid>');

    const tokenRes = http.post(
        `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`,
        { grant_type: 'password', client_id: CLIENT_ID, username: USERNAME, password: PASSWORD }
    );
    const token = tokenRes.json('access_token');
    return { token, classeId };
}

function headers(token) {
    return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

// ─── Scénario principal ───────────────────────────────────────────────────────
export default function run({ token, classeId }) {
    // Inscription d'un nouvel étudiant
    const payload = JSON.stringify({
        nouvelEtudiant: {
            nom:           `Stress${__VU}`,
            prenom:        `Test${__ITER}`,
            dateNaissance: '2000-01-01',
            email:         `stress_${RUN_ID}_${__VU}_${__ITER}@test.com`,
        },
        classeId,
        codeAnnee: CODE_ANNEE,
    });

    const inscRes = http.post(INSCRIPTION_URL, payload, {
        headers: headers(token),
        tags: { endpoint: 'metier' },
    });

    const ok = check(inscRes, {
        'inscription 201': (r) => r.status === 201,
        'id présent':      (r) => !!r.json('id'),
    });

    if (!ok || inscRes.status !== 201) {
        sleep(1);
        return;
    }

    const inscriptionId = inscRes.json('id');

    // Polling dossier (Kafka async)
    const pollingParams = {
        headers: headers(token),
        tags: { endpoint: 'polling' },
        responseCallback: http.expectedStatuses(200, 404),
    };
    let dossierPret = false;
    for (let i = 0; i < 20; i++) {
        const ping = http.get(`${PAIEMENT_URL}/api/dossiers/${inscriptionId}`, pollingParams);
        if (ping.status === 200) { dossierPret = true; break; }
        sleep(1);
    }

    check({ dossierPret }, { 'dossier prêt': (v) => v.dossierPret });
    if (!dossierPret) return;

    // Versement
    const versRes = http.post(
        `${PAIEMENT_URL}/api/dossiers/${inscriptionId}/versements/distribuer`,
        JSON.stringify({
            montant: MONTANT,
            datePaiement: new Date().toISOString().slice(0, 10),
            moyen: { type: 'COMPTANT' },
        }),
        { headers: headers(token), tags: { endpoint: 'metier' } }
    );

    check(versRes, { 'versement 200': (r) => r.status === 200 || r.status === 201 });
}
