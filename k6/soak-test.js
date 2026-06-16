/**
 * Soak Test — charge constante pendant 15 minutes
 *
 * Objectif : détecter les fuites mémoire, la dégradation des performances
 * dans le temps, et l'épuisement du pool de connexions DB.
 *
 * Usage :
 *   k6 run soak-test.js \
 *     -e MONTANT=150000 \
 *     -e CLASSE_ID=<uuid> \
 *     [-e CODE_ANNEE=2025-2026] \
 *     [-e USERNAME=baye] [-e PASSWORD=passer]
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

// Métriques custom pour suivre la dégradation dans le temps
const inscriptionDuration = new Trend('inscription_duration', true);
const versementDuration   = new Trend('versement_duration', true);
const kafkaDelay          = new Trend('kafka_delay_ms', true);

export const options = {
    stages: [
        { duration: '2m',  target: 90 },   // montée progressive
        { duration: '15m', target: 90 },   // charge constante — zone de soak
        { duration: '1m',  target: 0  },   // descente
    ],
    thresholds: {
        // Les métriques ne doivent pas se dégrader dans le temps
        'http_req_duration{endpoint:metier}': ['p(95)<1500'],
        'http_req_failed{endpoint:metier}':   ['rate<0.01'],
        // Métriques custom
        'inscription_duration': ['p(95)<500'],
        'versement_duration':   ['p(95)<500'],
        'kafka_delay_ms':       ['p(95)<15000'],  // Kafka doit livrer en <15s
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
    return { token: tokenRes.json('access_token'), classeId };
}

function headers(token) {
    return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

// ─── Scénario principal ───────────────────────────────────────────────────────
export default function run({ token, classeId }) {
    const payload = JSON.stringify({
        nouvelEtudiant: {
            nom:           `Soak${__VU}`,
            prenom:        `Test${__ITER}`,
            dateNaissance: '2000-01-01',
            email:         `soak_${RUN_ID}_${__VU}_${__ITER}@test.com`,
        },
        classeId,
        codeAnnee: CODE_ANNEE,
    });

    // Inscription
    const t0      = Date.now();
    const inscRes = http.post(INSCRIPTION_URL, payload, {
        headers: headers(token),
        tags: { endpoint: 'metier' },
    });
    inscriptionDuration.add(Date.now() - t0);

    const ok = check(inscRes, {
        'inscription 201': (r) => r.status === 201,
        'id présent':      (r) => !!r.json('id'),
    });
    if (!ok || inscRes.status !== 201) { sleep(1); return; }

    const inscriptionId = inscRes.json('id');

    // Polling Kafka — on mesure le délai réel de livraison
    const pollingParams = {
        headers: headers(token),
        tags: { endpoint: 'polling' },
        responseCallback: http.expectedStatuses(200, 404),
    };
    const kafkaStart = Date.now();
    let dossierPret  = false;
    for (let i = 0; i < 20; i++) {
        const ping = http.get(`${PAIEMENT_URL}/api/dossiers/${inscriptionId}`, pollingParams);
        if (ping.status === 200) { dossierPret = true; break; }
        sleep(1);
    }
    kafkaDelay.add(Date.now() - kafkaStart);

    check({ dossierPret }, { 'dossier prêt': (v) => v.dossierPret });
    if (!dossierPret) return;

    // Versement
    const t1      = Date.now();
    const versRes = http.post(
        `${PAIEMENT_URL}/api/dossiers/${inscriptionId}/versements/distribuer`,
        JSON.stringify({
            montant: MONTANT,
            datePaiement: new Date().toISOString().slice(0, 10),
            moyen: { type: 'COMPTANT' },
        }),
        { headers: headers(token), tags: { endpoint: 'metier' } }
    );
    versementDuration.add(Date.now() - t1);

    check(versRes, { 'versement 200': (r) => r.status === 200 || r.status === 201 });
}
