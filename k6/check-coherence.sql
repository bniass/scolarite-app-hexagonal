-- ============================================================
-- Vérification cohérence inscription ↔ paiements
-- Usage : mysql -h 127.0.0.1 -P 8889 -u root -proot < k6/check-coherence.sql
-- ============================================================

-- ── 1. Vue d'ensemble des inscriptions ───────────────────────
SELECT
    BIN_TO_UUID(i.id)          AS inscription_id,
    BIN_TO_UUID(i.etudiant_id) AS etudiant_id,
    i.code_annee,
    i.statut                   AS statut_inscription,
    i.frais_inscription,
    i.autres_frais,
    i.mensualite,
    i.cree_le
FROM `inscription-service`.inscription i
ORDER BY i.cree_le DESC
LIMIT 20;

-- ── 2. Une inscription et son dossier paiement ───────────────
SELECT
    BIN_TO_UUID(i.id)          AS inscription_id,
    i.statut                   AS statut_inscription,
    BIN_TO_UUID(d.id)          AS dossier_id,
    d.statut                   AS statut_dossier,
    d.frais_inscription        AS d_frais_inscription,
    d.autres_frais             AS d_autres_frais,
    d.mensualite               AS d_mensualite
FROM `inscription-service`.inscription i
LEFT JOIN `paiement-service`.dossier_paiement d
    ON d.inscription_id = i.id
ORDER BY i.cree_le DESC
LIMIT 20;

-- ── 3. Lignes de paiement par dossier ────────────────────────
SELECT
    BIN_TO_UUID(d.inscription_id) AS inscription_id,
    l.type,
    l.ordre_reglement,
    l.mois_academique_mois,
    l.mois_academique_annee,
    l.montant_du,
    l.montant_paye,
    l.statut                      AS statut_ligne
FROM `paiement-service`.dossier_paiement d
JOIN `paiement-service`.ligne_paiement l ON l.dossier_id = d.id
ORDER BY d.inscription_id, l.type, l.ordre_reglement
LIMIT 50;

-- ── 4. Versements effectués ───────────────────────────────────
SELECT
    BIN_TO_UUID(d.inscription_id) AS inscription_id,
    l.type                        AS ligne_type,
    v.montant                     AS montant_verse,
    v.date_paiement,
    mp.type                       AS moyen_paiement
FROM `paiement-service`.versement v
JOIN `paiement-service`.ligne_paiement l  ON l.id = v.ligne_id
JOIN `paiement-service`.dossier_paiement d ON d.id = l.dossier_id
JOIN `paiement-service`.moyen_paiement mp ON mp.id = v.moyen_id
ORDER BY d.inscription_id, l.type
LIMIT 50;

-- ── 5. Contrôle cohérence : montant_du vs montant_paye ───────
SELECT
    BIN_TO_UUID(d.inscription_id) AS inscription_id,
    SUM(l.montant_du)             AS total_du,
    SUM(l.montant_paye)           AS total_paye,
    SUM(l.montant_du) - SUM(l.montant_paye) AS reste_a_payer,
    COUNT(CASE WHEN l.statut = 'PAYE'  THEN 1 END) AS lignes_payees,
    COUNT(CASE WHEN l.statut = 'APAYER' THEN 1 END) AS lignes_a_payer,
    COUNT(CASE WHEN l.statut = 'AVANCE' THEN 1 END) AS lignes_avance
FROM `paiement-service`.dossier_paiement d
JOIN `paiement-service`.ligne_paiement l ON l.dossier_id = d.id
GROUP BY d.inscription_id
ORDER BY d.inscription_id
LIMIT 20;

-- ── 6. Inscriptions sans dossier paiement (anomalie) ─────────
SELECT
    BIN_TO_UUID(i.id) AS inscription_id,
    i.statut,
    i.cree_le
FROM `inscription-service`.inscription i
LEFT JOIN `paiement-service`.dossier_paiement d ON d.inscription_id = i.id
WHERE d.id IS NULL;
