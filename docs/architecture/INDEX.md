# Index — Documents d'Architecture

## Documents principaux

| Document | Contenu |
|----------|---------|
| [01 — Contexte système](01-contexte-systeme.md) | Diagrammes C4 Level 1 & 2, ports réseau, principes directeurs |
| [02 — Bounded Contexts](02-bounded-contexts.md) | DDD — carte des contextes, langage ubiquitaire par service, relations inter-contextes |
| [03 — Architecture Hexagonale](03-architecture-hexagonale.md) | Ports & Adapters, structure de packages, règles de couche, exemples de code |
| [04 — Catalogue des événements](04-catalogue-evenements.md) | Topics Kafka, schémas Avro, idempotence, Outbox Pattern |
| [05 — Sagas](05-sagas.md) | Diagrammes de séquence des 4 sagas (création, confirmation, annulation, transfert) |
| [06 — Modèles de données](06-modeles-donnees.md) | Schémas ER par service, ordre de règlement, structure cascade JPA |
| [07 — Sécurité](07-securite.md) | Keycloak, WSO2, Spring Security, propagation JWT, Swagger OAuth2 |

## Architecture Decision Records (ADR)

| ADR | Décision | Statut |
|-----|----------|--------|
| [ADR-001](ADR/ADR-001-architecture-hexagonale.md) | Architecture Hexagonale (Ports & Adapters) | ✅ Accepté |
| [ADR-002](ADR/ADR-002-saga-choreographie.md) | Saga par Chorégraphie | ✅ Accepté |
| [ADR-003](ADR/ADR-003-outbox-pattern.md) | Outbox Pattern | ✅ Accepté |
| [ADR-004](ADR/ADR-004-avro-schema-registry.md) | Avro + Schema Registry | ✅ Accepté |
| [ADR-005](ADR/ADR-005-separation-admin-comptable.md) | Séparation Admin / Comptable | ✅ Accepté |
| [ADR-006](ADR/ADR-006-transfert-classe.md) | Règles du Transfert de Classe | ✅ Accepté |

---

## Vue rapide — Qui fait quoi

```
Créer une année académique   → annee-academique-service :8080
Créer une filière / classe   → school-service :8094
Affecter un tarif            → school-service :8094
Inscrire un étudiant         → inscrption-service :8091
Annuler une inscription      → inscrption-service :8091
Transférer vers une classe   → inscrption-service :8091
Encaisser un versement       → paiement-service :8093
Consulter le dossier         → paiement-service :8093
```

## Conventions de code

| Convention | Règle |
|-----------|-------|
| Factory d'agrégat | `Agrégat.creer(...)` émet des events, `Agrégat.reconstituer(...)` n'en émet pas |
| Commandes | Records immuables dans `application/command/` |
| Ports | Interfaces dans `application/port/in/` et `application/port/out/` |
| Événements domaine | Classes dans `domain/event/`, publiés via `addEvent()` |
| Collecte des events | `aggregat.pullDomainEvents().forEach(outboxPort::sauvegarder)` dans le use case |
| Mappers | Classe dédiée dans `infrastructure/persistence/mapper/` — séparation nette domaine ↔ JPA |
| Tests | Uniquement Mockito — zéro Spring context dans les tests de use case |
