import http from 'k6/http';
import { check, group, sleep } from 'k6';
import exec from 'k6/execution';

export const options = {
    scenarios: {
        nouveau_comptant: {
            executor: 'ramping-vus',
            startVUs: 1,
            stages: [
                { duration: '20s', target: 30 },
                { duration: '40s', target: 30 },
                { duration: '10s', target: 0  },
            ],
            gracefulRampDown: '10s',
            env: { SCENARIO: 'nouveau_comptant' },
        },
        nouveau_mobile_money: {
            executor: 'ramping-vus',
            startVUs: 1,
            stages: [
                { duration: '20s', target: 30 },
                { duration: '40s', target: 30 },
                { duration: '10s', target: 0  },
            ],
            gracefulRampDown: '10s',
            env: { SCENARIO: 'nouveau_mobile_money' },
        },
        nouveau_banque: {
            executor: 'ramping-vus',
            startVUs: 1,
            stages: [
                { duration: '20s', target: 30 },
                { duration: '40s', target: 30 },
                { duration: '10s', target: 0  },
            ],
            gracefulRampDown: '10s',
            env: { SCENARIO: 'nouveau_banque' },
        },
        // Chaque étudiant ne peut s'inscrire qu'une fois → shared-iterations : 1 itération par VU
        ancien_comptant: {
            executor: 'shared-iterations',
            vus: 20,
            iterations: 20,
            maxDuration: '1m10s',
            env: { SCENARIO: 'ancien_comptant' },
        },
        ancien_mobile_money: {
            executor: 'shared-iterations',
            vus: 20,
            iterations: 20,
            maxDuration: '1m10s',
            env: { SCENARIO: 'ancien_mobile_money' },
        },
        ancien_banque: {
            executor: 'shared-iterations',
            vus: 20,
            iterations: 20,
            maxDuration: '1m10s',
            env: { SCENARIO: 'ancien_banque' },
        },
    },
    thresholds: {
        'http_req_duration{scenario:nouveau_comptant}':     ['p(95)<800'],
        'http_req_duration{scenario:nouveau_mobile_money}': ['p(95)<800'],
        'http_req_duration{scenario:nouveau_banque}':       ['p(95)<800'],
        'http_req_duration{scenario:ancien_comptant}':      ['p(95)<500'],
        'http_req_duration{scenario:ancien_mobile_money}':  ['p(95)<500'],
        'http_req_duration{scenario:ancien_banque}':        ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

// ─── Configuration ────────────────────────────────────────────────────────────

const KEYCLOAK_URL          = __ENV.KEYCLOAK_URL          || 'http://localhost:8180';
const REALM                 = __ENV.REALM                 || 'scolarite';
const CLIENT_ID             = __ENV.CLIENT_ID             || 'client-frontend';
const USERNAME              = __ENV.USERNAME              || 'baye';
const PASSWORD              = __ENV.PASSWORD              || 'passer';
const CODE_ANNEE            = __ENV.CODE_ANNEE            || '2025-2026';
const MONTANT               = parseFloat(__ENV.MONTANT     || '0');
if (!MONTANT) throw new Error('MONTANT est obligatoire — passez -e MONTANT=<valeur>');

const ANNEE_SERVICE_URL     = 'http://localhost:8080/api/academic-years';
const SCHOOL_SERVICE_URL    = 'http://localhost:8094';
const ETUDIANT_SERVICE_URL  = 'http://localhost:8092/api/etudiants';
const INSCRIPTION_URL       = 'http://localhost:8091/api/inscriptions';

// ─── Auth ─────────────────────────────────────────────────────────────────────

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

function headers(token) {
    return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

// 20 étudiants par scénario "ancien" × 3 scénarios = 60 étudiants
const ANCIEN_VUS = 20;

// ─── Setup : provisioning ────────────────────────────────────────────────────

export function setup() {
    const token = getToken();
    const h     = headers(token);

    const classeId = __ENV.CLASSE_ID || '';
    if (!classeId) throw new Error('CLASSE_ID est obligatoire — passez -e CLASSE_ID=<uuid>');

    // 1. Vérifier que l'année est bien en InscriptionsOuvertes
    const annees = http.get(ANNEE_SERVICE_URL, { headers: h }).json();
    const annee  = Array.isArray(annees) && annees.find(a => a.code == CODE_ANNEE);
    check({ annee }, { 'année en InscriptionsOuvertes': () => annee && annee.statut === 'InscriptionsOuvertes' });
    if (!annee)                                  throw new Error(`Année ${CODE_ANNEE} introuvable`);
    if (annee.statut !== 'InscriptionsOuvertes') throw new Error(`Année ${CODE_ANNEE} statut="${annee.statut}" — inscriptions non ouvertes`);

    // 2. Récupérer le tarif actif et calculer le montant minimum
    const tarifRes = http.get(`${SCHOOL_SERVICE_URL}/api/classes/${classeId}/tarif-actif`, { headers: h });
    check(tarifRes, { 'classe avec tarif actif': (r) => r.status === 200 });
    if (tarifRes.status !== 200) throw new Error(`Classe ${classeId} sans tarif actif — ${tarifRes.body}`);

    console.log(`Montant utilisé pour les inscriptions : ${MONTANT}`);

    // 3. Attendre que l'inscription-service soit prêt (projection Kafka)
    let kafkaReady = false;
    for (let i = 0; i < 10; i++) {
        const ping = http.get(INSCRIPTION_URL, { headers: h });
        if (ping.status !== 503) { kafkaReady = true; break; }
        sleep(2);
    }
    check({ kafkaReady }, { 'inscription-service prêt': (v) => v.kafkaReady });

    // 4. Créer 3 pools de 20 étudiants — un par scénario "ancien"
    const ts = Date.now();
    const allEtudiantIds = [];

    function creerPool(scenarioTag) {
        const pool = [];
        for (let i = 0; i < ANCIEN_VUS; i++) {
            const res = http.post(
                ETUDIANT_SERVICE_URL,
                JSON.stringify({
                    nom:           `Diallo${scenarioTag}${i}`,
                    prenom:        `Mamadou${scenarioTag}${i}`,
                    dateNaissance: '1998-03-20',
                    email:         `ancien.${scenarioTag}.${ts}.${i}@ecole221.com`,
                    codeAnnee:     CODE_ANNEE,
                }),
                { headers: h }
            );
            if (res.status !== 201) {
                console.error(`Étudiant ${scenarioTag}[${i}] — ${res.status} : ${res.body}`);
                for (const id of allEtudiantIds) {
                    if (id) http.del(`${ETUDIANT_SERVICE_URL}/id/${id}`, null, { headers: h });
                }
                throw new Error(`Échec création étudiant ${scenarioTag}[${i}]`);
            }
            check(res, { [`étudiant ${scenarioTag}[${i}] créé`]: (r) => r.status === 201 });
            const id = res.json('id');
            pool.push(id);
            allEtudiantIds.push(id);
        }
        return pool;
    }

    const comptantIds    = creerPool('comptant');
    const mobilemoneyIds = creerPool('mobilemoney');
    const banqueIds      = creerPool('banque');

    http.get(INSCRIPTION_URL, { headers: h });

    return { token, classeId, comptantIds, mobilemoneyIds, banqueIds, allEtudiantIds };
}

// ─── Teardown : nettoyage des étudiants créés dans setup ─────────────────────

export function teardown(data) {
    const token = getToken();
    const h     = headers(token);

    for (const id of data.allEtudiantIds) {
        if (!id) continue;
        const res = http.del(`${ETUDIANT_SERVICE_URL}/id/${id}`, null, { headers: h });
        if (res.status !== 204) {
            console.error(`Suppression étudiant ${id} — ${res.status} : ${res.body}`);
        }
    }
}

// ─── Builders payload ────────────────────────────────────────────────────────

function comptant()     { return { type: 'COMPTANT' }; }
function mobileMoney()  { return { type: 'MOBILE_MONEY', operateur: 'ORANGE', referencePaiement: `REF-${__VU}-${__ITER}` }; }
function banque()       { return { type: 'BANQUE', nomBanque: 'BHS', numeroTransaction: `TXN-${__VU}-${__ITER}` }; }

function payloadNouvel(classeId, montant, moyen) {
    return {
        nouvelEtudiant: {
            nom:           `Nom${__VU}`,
            prenom:        `Prenom${__VU}`,
            dateNaissance: '2000-06-15',
            email:         `etudiant_${__VU}_${__ITER}@test.com`,
        },
        classeId,
        codeAnnee: CODE_ANNEE,
        montant,
        moyenPaiement: moyen,
    };
}

function payloadAncien(classeId, etudiantId, montant, moyen) {
    return {
        etudiantId,
        classeId,
        codeAnnee: CODE_ANNEE,
        montant,
        moyenPaiement: moyen,
    };
}

function postInscription(token, payload, label) {
    const res = http.post(
        INSCRIPTION_URL,
        JSON.stringify(payload),
        { headers: headers(token) }
    );
    if (res.status !== 201) {
        console.error(`${label} — ${res.status} : ${res.body}`);
        exec.test.abort(`Erreur inattendue sur ${label} — ${res.status} : ${res.body}`);
    }
    check(res, {
        [`${label} — 201`]:        (r) => r.status === 201,
        [`${label} — id présent`]: (r) => !!r.json('id'),
    });
}

// ─── Scénarios ───────────────────────────────────────────────────────────────

export default function run(data) {
    const { token, classeId, comptantIds, mobilemoneyIds, banqueIds } = data;
    const idx = exec.scenario.iterationInTest;

    switch (__ENV.SCENARIO) {
        case 'nouveau_comptant':
            group('Nouvel étudiant — Comptant', () =>
                postInscription(token, payloadNouvel(classeId, MONTANT, comptant()), 'nouveau/comptant'));
            break;

        case 'nouveau_mobile_money':
            group('Nouvel étudiant — Mobile Money', () =>
                postInscription(token, payloadNouvel(classeId, MONTANT, mobileMoney()), 'nouveau/mobile_money'));
            break;

        case 'nouveau_banque':
            group('Nouvel étudiant — Banque', () =>
                postInscription(token, payloadNouvel(classeId, MONTANT, banque()), 'nouveau/banque'));
            break;

        case 'ancien_comptant':
            group('Ancien étudiant — Comptant', () =>
                postInscription(token, payloadAncien(classeId, comptantIds[idx], MONTANT, comptant()), 'ancien/comptant'));
            break;

        case 'ancien_mobile_money':
            group('Ancien étudiant — Mobile Money', () =>
                postInscription(token, payloadAncien(classeId, mobilemoneyIds[idx], MONTANT, mobileMoney()), 'ancien/mobile_money'));
            break;

        case 'ancien_banque':
            group('Ancien étudiant — Banque', () =>
                postInscription(token, payloadAncien(classeId, banqueIds[idx], MONTANT, banque()), 'ancien/banque'));
            break;
    }
}
