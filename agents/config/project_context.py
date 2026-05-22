"""
Contexte projet partagé entre tous les agents.

Ce contexte est injecté dans CHAQUE prompt envoyé à Claude pour que
les agents respectent strictement l'architecture du projet.
"""

PROJECT_CONTEXT = """
PROJET : scolarite-app-hexagonal (Spring Boot multi-module)
PACKAGE DE BASE : com.ecole221
JAVA : 21 (release 23 dans le pom)
SPRING BOOT : 3.5.13
LOMBOK : disponible (provided)

═══════════════════════════════════════════════════════════
ARCHITECTURE HEXAGONALE STRICTE (Ports & Adapters)
═══════════════════════════════════════════════════════════

RÈGLES INTOUCHABLES :
- AUCUNE annotation Spring dans le domain/
- AUCUNE dépendance du domain vers l'infrastructure
- Les ports (interfaces) sont dans application/port/in et application/port/out
- Les adapters (implémentations) sont dans infrastructure/
- Kafka utilise OBLIGATOIREMENT l'Outbox Pattern

STRUCTURE D'UN MICROSERVICE :

domain/
  model/         → Agrégats (étendent AggregateRoot<XxxId>)
  valueobject/   → Value Objects (les IDs étendent BaseId<UUID>)
  event/         → Domain Events (implémentent DomainEvent<T>)
  exception/     → XxxException, XxxNotFoundException (étendent DomainException)
  enums/         → Enums du domaine
  state/         → State Pattern (si nécessaire)

application/
  command/       → Commands (records Java immuables)
  port/in/       → Use Case interfaces (une par use case)
  port/out/      → Repository ports + OutboxPort
  usecase/       → Implémentations des use cases (@Service)

infrastructure/
  persistence/
    entity/      → JPA entities (@Entity, @Table)
    mapper/      → Mappers domain ↔ JPA
    adapter/     → Implémentations des Repository ports
    repository/  → Spring Data JPA interfaces
  outbox/
    entity/      → OutboxEventJpaEntity, OutboxStatus
    adapter/     → OutboxAdapter (implémente OutboxPort)
    publisher/   → OutboxDomainEventPublisher
    repository/  → OutboxEventJpaRepository
  web/
    controller/  → @RestController
    dto/         → XxxRequest, XxxResponse (records Java)
    mapper/      → Mappers entre commands et DTOs
    exception/   → ApiError, GlobalControllerAdvice

═══════════════════════════════════════════════════════════
CLASSES COMMUNES DÉJÀ DISPONIBLES (module common-service)
═══════════════════════════════════════════════════════════

Package : com.ecole221.common

- BaseEntity<ID>               → entité de base
- AggregateRoot<ID>            → racine d'agrégat avec domain events
- DomainEvent<T>               → interface event domaine
- DomainEventPublisher<T>      → interface publisher
- DomainException              → exception de base
- BaseId<T>                    → value object ID de base

═══════════════════════════════════════════════════════════
MODULE DE RÉFÉRENCE
═══════════════════════════════════════════════════════════

annee-academique-service est le module le plus complet et sert
de modèle architectural. Tous les nouveaux modules doivent suivre
sa structure.
"""
