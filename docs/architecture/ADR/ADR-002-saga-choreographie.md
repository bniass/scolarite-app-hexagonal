# ADR-002 — Saga par Chorégraphie

**Date :** 2025  
**Statut :** Accepté  
**Décideurs :** Équipe technique

---

## Contexte

L'inscription d'un étudiant implique plusieurs services (inscription, etudiant, paiement). Si l'un échoue, les autres doivent compenser. Il faut choisir entre **orchestration** (coordinateur central) et **chorégraphie** (réaction aux événements).

## Décision

Utilisation de la **saga par chorégraphie** sur Kafka :

- Chaque service publie un événement après son opération locale
- Les autres services réagissent à ces événements sans être explicitement appelés
- Les compensations sont déclenchées par des événements d'échec (`paiement-confirme{ECHEC}`)

## Conséquences positives

- **Pas de SPOF** : pas de coordinateur central qui peut tomber
- **Couplage minimal** : les services ne se connaissent que via le contrat Avro
- **Scalabilité** : les consommateurs Kafka scalent indépendamment

## Conséquences négatives

- **Visibilité** : le flux global n'est pas lisible dans un seul endroit — nécessite la lecture des consumers Kafka de chaque service
- **Debugging** : tracer un flux nécessite de corréler les logs par `inscriptionId` sur plusieurs services

## Alternatives rejetées

- **Saga par orchestration (Saga Orchestrator)** : coordinateur central (ex: Temporal, Conductor) — overhead d'infrastructure, SPOF, complexité accrue pour la taille actuelle du projet
- **2PC (Two-Phase Commit)** : interdit dans les architectures microservices — bloque les ressources, couplage fort, incompatible avec la scalabilité
