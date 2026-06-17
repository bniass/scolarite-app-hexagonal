# ADR-001 — Architecture Hexagonale (Ports & Adapters)

**Date :** 2025  
**Statut :** Accepté  
**Décideurs :** Équipe technique

---

## Contexte

Le projet Scolarite est un système multi-services avec des règles métier complexes (saga, transfert, compensation). Il doit être maintenable, testable unitairement sans infrastructure, et évolutif (changement de base de données, de broker, d'API Gateway).

## Décision

Chaque microservice adopte l'**architecture hexagonale** (Ports & Adapters) :

- La couche **Domain** ne dépend de rien — ni Spring, ni JPA, ni Jackson
- La couche **Application** définit les ports (interfaces) et les use cases
- La couche **Infrastructure** implémente les adapters (JPA, REST, Kafka, Outbox)

## Conséquences positives

- **Testabilité** : les use cases se testent avec de simples mocks (Mockito), sans Spring context
- **Isolation** : remplacer MySQL par PostgreSQL ne touche que les adapters JPA
- **Clarté** : un développeur qui ouvre `application/usecase/` comprend immédiatement la logique métier sans se noyer dans les détails techniques
- **DDD natif** : les agrégats émettent des DomainEvents via `AggregateRoot.addEvent()`, collectés par `pullDomainEvents()` dans les use cases

## Conséquences négatives

- **Verbosité** : plus de fichiers qu'une architecture en couches classique (port, adapter, mapper pour chaque concept)
- **Discipline** : la règle "aucun import Spring dans le domain" doit être vérifiée en revue de code

## Alternatives rejetées

- **Architecture en couches (Controller → Service → Repository)** : couplage fort, tests d'intégration obligatoires, difficulté à changer l'infrastructure
- **CQRS complet** : surcharge pour la taille actuelle du projet — décision reportée si besoin de read models séparés
