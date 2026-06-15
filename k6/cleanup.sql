-- ============================================================
-- Nettoyage post-test k6 — remet les bases à l'état initial
-- Usage : mysql -h 127.0.0.1 -P 8889 -u root -proot < k6/cleanup.sql
-- ============================================================

-- ── inscription-service ──────────────────────────────────────
USE `inscription-service`;

SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM outbox_event;
DELETE FROM inscription;
SET FOREIGN_KEY_CHECKS = 1;

-- ── etudiant-service ─────────────────────────────────────────
USE `etudiant-service`;

SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM outbox_events;
DELETE FROM etudiants
  WHERE email LIKE 'ancien.%@ecole221.com'   -- étudiants "ancien" créés par le setup
     OR email LIKE 'etudiant\_%@test.com';   -- étudiants "nouveau" créés par les VUs
SET FOREIGN_KEY_CHECKS = 1;

-- ── paiement-service ─────────────────────────────────────────
USE `paiement-service`;

SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM outbox_event;
DELETE FROM versement;
DELETE FROM moyen_paiement;
DELETE FROM ligne_paiement;
DELETE FROM dossier_paiement;
SET FOREIGN_KEY_CHECKS = 1;
