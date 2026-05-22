# Architecture — scolarite-app-hexagonal

Multi-module **Maven** / **Spring Boot 3** / **Java 21**, suivant une **architecture hexagonale** (DDD) avec communication inter-services via **Kafka** et **Outbox Pattern**.

## 1. Modules Maven & dépendances

```mermaid
graph TD
    classDef parent fill:#1f2937,stroke:#111827,color:#fff,stroke-width:2px;
    classDef ms fill:#2563eb,stroke:#1e40af,color:#fff,stroke-width:2px;
    classDef shared fill:#059669,stroke:#065f46,color:#fff,stroke-width:2px;
    classDef infra fill:#7c3aed,stroke:#5b21b6,color:#fff,stroke-width:2px;
    classDef ext fill:#f59e0b,stroke:#b45309,color:#1f2937,stroke-width:2px;

    PARENT["scolarite-app-hexagonal<br/>(parent POM)"]:::parent

    subgraph Microservices
      ANNEE["annee-academique-service"]:::ms
      INSC["inscrption-service"]:::ms
    end

    subgraph "Modules partagés"
      COMMON["common-service"]:::shared
      AVRO["common-avro"]:::shared
      KAFKA["kafka-service"]:::infra
    end

    subgraph "Infrastructure externe"
      MYSQL[("MySQL")]:::ext
      BROKER[("Kafka cluster")]:::ext
      SR[("Schema Registry")]:::ext
    end

    PARENT --> ANNEE
    PARENT --> INSC
    PARENT --> COMMON
    PARENT --> AVRO
    PARENT --> KAFKA

    ANNEE --> COMMON
    ANNEE --> KAFKA
    ANNEE --> AVRO

    INSC --> COMMON
    INSC --> KAFKA
    INSC --> AVRO

    ANNEE -. JPA .-> MYSQL
    INSC  -. JPA .-> MYSQL
    ANNEE == "événements Avro" ==> BROKER
    INSC  == "événements Avro" ==> BROKER
    AVRO -. fournit les schémas à .-> SR
```

| Module | Rôle |
|---|---|
| `common-service` | Briques DDD partagées : `BaseEntity`, `AggregateRoot`, `BaseId`, `DomainEvent`, `DomainEventPublisher`, `DomainException`, `DomainConstants` |
| `common-avro` | Schémas Avro (`*.avsc`) + classes Java générées par `avro-maven-plugin` |
| `kafka-service` | Producteur / Consommateur Spring Kafka, helpers et config |
| `annee-academique-service` | Microservice métier (années académiques, inscriptions ouvertes/fermées, publication, clôture) |
| `inscrption-service` | Microservice métier (inscription des étudiants) |

## 2. Architecture hexagonale interne

Chaque microservice respecte la séparation `domain` / `application` / `infrastructure` imposée par `CLAUDE.md` :

```mermaid
graph LR
    classDef domain fill:#dc2626,stroke:#7f1d1d,color:#fff,stroke-width:2px;
    classDef app fill:#2563eb,stroke:#1e3a8a,color:#fff,stroke-width:2px;
    classDef port fill:#fbbf24,stroke:#92400e,color:#1f2937,stroke-width:2px;
    classDef infra fill:#7c3aed,stroke:#4c1d95,color:#fff,stroke-width:2px;

    subgraph DOMAIN
      AGG["AnneeAcademique (AggregateRoot)"]:::domain
      VO["CodeAnnee, DatesAnnee, …"]:::domain
      EVT["Domain Events"]:::domain
    end

    subgraph APP[APPLICATION]
      UC["Use Case Services"]:::app
      CMD["Commands"]:::app
    end

    subgraph PORTS
      PIN["Ports IN<br/>(UseCase interfaces)"]:::port
      POUT["Ports OUT<br/>Repository, Outbox,<br/>DomainEventPublisher"]:::port
    end

    subgraph INFRA[INFRASTRUCTURE]
      WEB["Web (Controller)"]:::infra
      JPA["Persistence JPA"]:::infra
      OUTBOX["Outbox + Publisher"]:::infra
    end

    WEB --> PIN --> UC --> AGG
    UC --> POUT
    POUT --> JPA
    POUT --> OUTBOX
```

### Règles respectées
- Aucun import Spring dans `domain/**` (vérifiable par `grep`)
- Toutes les dépendances pointent **vers le domain**, jamais l'inverse
- Les adapters (`infrastructure/**`) implémentent des ports définis dans `application/port/out/**`

## 3. Outbox Pattern

Les événements de domaine ne sont **jamais publiés directement** vers Kafka : ils transitent par une table `outbox_event` écrite **dans la même transaction** que l'agrégat, puis un `@Scheduled` (`OutboxPublisher`) les sérialise en Avro et les pousse vers Kafka.

```mermaid
sequenceDiagram
    participant U as UseCase
    participant R as Repository
    participant O as OutboxPort
    participant P as OutboxPublisher
    participant K as Kafka
    Note over U,O: Transaction unique
    U->>R: save(aggregate)
    U->>O: save(outboxEvent, status=PENDING)
    loop polling
      P->>O: findPending()
      P->>K: send(Avro)
      P->>O: markAsSent()
    end
```

## 4. Machine à états de `AnneeAcademique`

L'agrégat applique le **State Pattern** (`domain/state/`) : interface `EtatAnnee`, classe abstraite `AbstractEtatAnnee` qui interdit toute action par défaut (`interdit()` → `AnneeAcademiqueException`), et 5 classes d'état qui n'autorisent que leurs transitions légitimes.

```mermaid
stateDiagram-v2
    direction LR
    [*] --> AnneeBrouillon : creer(annee, datesAnnee)
    AnneeBrouillon --> AnneeBrouillon : modifier(dates)
    AnneeBrouillon --> AnneePubliee : publier()
    AnneePubliee --> InscriptionsOuvertes : ouvrirInscription()
    InscriptionsOuvertes --> InscriptionsFermees : cloturerInscription()
    InscriptionsFermees --> AnneeCloturee : cloturer()
    AnneeCloturee --> [*]
```

### Matrice des transitions autorisées

| État courant \ Action | `modifier` | `publier` | `ouvrirInscriptions` | `fermerInscriptions` | `cloturer` |
|---|---|---|---|---|---|
| **AnneeBrouillon** | reste `Brouillon` | → `Publiee` | interdit | interdit | interdit |
| **AnneePubliee** | interdit | interdit | → `InscriptionsOuvertes` | interdit | interdit |
| **InscriptionsOuvertes** | interdit | interdit | interdit | → `InscriptionsFermees` | interdit |
| **InscriptionsFermees** | interdit | interdit | interdit | interdit | → `AnneeCloturee` |
| **AnneeCloturee** | interdit | interdit | interdit | interdit | interdit (terminal) |

Toute action *interdite* lève `AnneeAcademiqueException("Action interdite : <action> dans l'état <Etat>")`.

### Événements émis par transition

| Transition | Domain event |
|---|---|
| `creer` | `AnneeAcademiqueCreeeEvent` (statut = `BROUILLON`) |
| `publier` | `AnneeAcademiquePublieeEvent` |
| `ouvrirInscription` | `InscriptionsOuvertesEvent` |
| `cloturerInscription` (fermer) | `InscriptionsFermeesEvent` |
| `cloturer` | `AnneeAcademiqueClotureeEvent` |

### Observations issues du code

- Dans `AnneeAcademique.modifier(...)`, `setData()` force l'état à `new AnneeBrouillon()` **avant** d'appeler `etatAnnee.modifier(...)`. La garde du State Pattern est donc **court-circuitée** : un appel à `modifier()` depuis n'importe quel état réinitialise silencieusement à `Brouillon` au lieu de lever `AnneeAcademiqueException`. À vérifier si c'est intentionnel.
- Toutes les méthodes publiques de `AnneeAcademique` (`modifier`, `publier`, `ouvrirInscription`, `cloturerInscription`, `cloturer`) émettent un `AnneeAcademiqueCreeeEvent` (avec un statut différent), alors que des classes dédiées existent dans `domain/event/` (`AnneeAcademiquePublieeEvent`, `InscriptionsOuvertesEvent`, etc.). La typologie des événements gagnerait à être homogénéisée.

## 5. Diagramme de séquence — Création d'une année académique

Trace complète du flux `POST /api/academic-years`, telle qu'implémentée dans le code :
`Controller → Mapper → CreerAnneeAcademiqueService (@Transactional) → AnneeAcademique.creer → Repository MySQL → OutboxPort` (transaction unique), puis publication Kafka **asynchrone** par `OutboxPublisher @Scheduled(5s)`.

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Ctrl as Controller
    participant UC as CreerAnneeAcademiqueService<br/>@Transactional
    participant Domain as AnneeAcademique
    participant Repo as Repository (MySQL)
    participant Outbox as OutboxPort
    participant Pub as OutboxPublisher<br/>@Scheduled(5s)
    participant K as Kafka

    Client->>Ctrl: POST /api/academic-years<br/>CreerAnneeAcademiqueRequest
    Note over Ctrl: Validation @Pattern("^[0-9]{4}$")
    Ctrl->>UC: executer(CreerAnneeAcademiqueCommand)

    rect rgba(37, 99, 235, 0.08)
      Note over UC,Outbox: Transaction unique
      UC->>Repo: findByCode(code)
      alt déjà présente
        UC-->>Ctrl: AnneeAcademiqueException
      else nouvelle
        UC->>Domain: AnneeAcademique.creer(code, dates)
        Domain->>Domain: checkDates() + checkDuree(9 mois)
        Domain->>Domain: changerEtat(AnneeBrouillon)
        Domain->>Domain: addEvent(AnneeAcademiqueCreeeEvent)
        Domain-->>UC: AnneeAcademique
        UC->>Repo: save(annee)
        UC->>Domain: pullDomainEvents()
        loop pour chaque event
          UC->>Outbox: save(event, PENDING)
        end
      end
    end
    Ctrl-->>Client: 201 Created + Location

    Note over Pub,K: Publication asynchrone (toutes les 5s)
    loop polling
      Pub->>Outbox: findTop50(PENDING, FAILED)
      Pub->>Pub: AvroSerializerUtil.fromBytes(payload)
      Pub->>K: send(topic, aggregateId, AvroModel)
      alt succès
        Pub->>Outbox: status = PUBLISHED
      else échec
        Pub->>Outbox: status = FAILED
      end
    end
```

### Détails du flux (numéros = code source)

1. **`AnneeAcademiqueController.creer`** (`infrastructure/web/controller`) — déclenche la validation Jakarta (`@NotBlank`, `@Pattern("^[0-9]{4}$")`) sur le `CreerAnneeAcademiqueRequest`, puis convertit en command via `AnneeAcademiqueMapper.toCommand`.
2. **`CreerAnneeAcademiqueService.executer`** (`application/usecase`) — annoté `@Transactional` (classe + méthode).
   - **Unicité** : `repository.findByCode(...)` → si présent, `AnneeAcademiqueException("Année académique déjà existante")`.
   - **Construction du VO `DatesAnnee`** à partir des 4 dates de la command.
   - **`AnneeAcademique.creer(codeAnnee, datesAnnee)`** (`domain/model`) :
     - `checkDates()` — début < fin, ouverture < fin inscription, ouverture ≤ début, fin inscription > début et ≤ fin.
     - `checkIfDureAnnneEstValide()` — exactement 9 mois.
     - Génère les 9 `MoisAcademique`, initialise `etatAnnee = new AnneeBrouillon()`, ajoute un `AnneeAcademiqueCreeeEvent(BROUILLON)` à la liste interne héritée de `AggregateRoot`.
   - **`repository.save(annee)`** → `AnneeAcademiqueRepositoryMySqlAdapter` (profile `mysql`) → mapper → `AnneeAcademiqueJpaRepository.save(...)`.
   - **`annee.pullDomainEvents().forEach(outboxPort::save)`** → écrit dans la table `outbox_event` avec `status = PENDING` et payload Avro sérialisé (`OutboxEventJpaAdapter` + `OutboxEventMapper`).
3. **Retour HTTP** : `201 Created` avec header `Location: /api/academic-years/{code}`.
4. **`OutboxPublisher.publish()`** (`infrastructure/event/outbox`) — `@Scheduled(fixedDelay = 5000)` :
   - `findTop50ByStatusInOrderByOccurredAtAsc(PENDING, FAILED)`
   - `AvroSerializerUtil.fromBytes(payload)` → `CreateAnneeAcademiqueAvroModel`
   - `kafkaMessageHelper.send(topic, aggregateId, avroModel)` (topic configuré via `${kafka-topics.anneeacademique-topic-request-name}`)
   - Mise à jour du statut : `PUBLISHED` si OK, `FAILED` sinon.

### Garanties

| Propriété | Mécanisme |
|---|---|
| **Atomicité** persistance + intent de publication | `@Transactional` couvre `save(annee)` ET `outboxPort.save(event)` |
| **Au moins une livraison** vers Kafka | Polling périodique de l'outbox + retry des `FAILED` |
| **Idempotence côté consommateur** | À assurer en aval (clé Kafka = `aggregateId`) |
| **Ordre des événements par agrégat** | `ORDER BY occurredAt ASC` + clé Kafka = `aggregateId` (même partition) |

### Observations utiles

- Le bean `DomainEventPublisher` (`OutboxDomainEventPublisher`) est **injecté dans `CreerAnneeAcademiqueService` mais jamais utilisé** : c'est `outboxPort::save` qui est appelé directement. Il y a donc deux chemins concurrents vers la table outbox dans le code — l'un est mort. À nettoyer.
- L'`OutboxPublisher` n'utilise que `CreateAnneeAcademiqueAvroModel` quelle que soit la nature de l'événement (`Publiee`, `InscriptionsOuvertes`, etc.). À combiner avec l'observation précédente sur l'unicité de l'event émis dans l'agrégat.

## 6. Modèle C4

Trois niveaux du modèle [C4](https://c4model.com) (Simon Brown) — Contexte, Conteneurs, Composants. Le niveau 4 (code) n'est pas représenté ici : il correspond aux classes Java déjà visibles dans les sections précédentes.

> ℹ️ Le support C4 de Mermaid est encore expérimental. Si le rendu échoue dans ton IDE, ouvre `architecture.html` dans un navigateur, ou utilise [mermaid.live](https://mermaid.live).

### 6.1 — Niveau 1 : Contexte

```mermaid
C4Context
    title C4 - Niveau 1 : Contexte système (Scolarité)

    Person(admin, "Administration scolaire", "Crée et pilote les années, ouvre/ferme les inscriptions")
    Person(etudiant, "Étudiant", "S'inscrit pour une année académique ouverte")

    System(scolarite, "Système Scolarité", "Plateforme de gestion des années académiques et des inscriptions (Spring Boot 3, Java 21, hexagonal)")

    System_Ext(mysql, "MySQL", "Persistance des agrégats et de la table outbox")
    System_Ext(kafka, "Kafka + Schema Registry", "Bus d'événements inter-services et registre Avro")

    Rel(admin, scolarite, "Gère les années académiques", "HTTPS / JSON")
    Rel(etudiant, scolarite, "S'inscrit", "HTTPS / JSON")
    Rel(scolarite, mysql, "Lit / écrit", "JDBC")
    Rel(scolarite, kafka, "Publie / consomme événements", "Avro / TCP")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

### 6.2 — Niveau 2 : Conteneurs

```mermaid
C4Container
    title C4 - Niveau 2 : Conteneurs (intérieur du Système Scolarité)

    Person(admin, "Administration", "")
    Person(etudiant, "Étudiant", "")

    System_Boundary(scolarite, "Système Scolarité") {
      Container(annee, "annee-academique-service", "Spring Boot 3 / Java 21", "API REST + use cases + Outbox. Source de vérité de l'agrégat AnneeAcademique")
      Container(inscription, "inscrption-service", "Spring Boot 3 / Java 21", "API REST des inscriptions. Consomme les événements d'année")
      ContainerDb(db, "MySQL", "MySQL 8", "annee_academique, mois_academique, outbox_event")
      ContainerQueue(broker, "Kafka cluster", "Confluent 7.5 (3 brokers + Zookeeper)", "Topics d'événements")
      Container(sr, "Schema Registry", "Confluent 7.5", "Schémas Avro")
    }

    Rel(admin, annee, "POST /api/academic-years, /publish, /open-enrollments, /close-enrollments, /close, /update", "HTTPS / JSON")
    Rel(etudiant, inscription, "POST /inscriptions", "HTTPS / JSON")
    Rel(annee, db, "Persiste agrégats + outbox", "JDBC")
    Rel(inscription, db, "Persiste inscriptions", "JDBC")
    Rel(annee, broker, "Publie événements (Outbox @Scheduled 5s)", "Avro / TCP")
    Rel(inscription, broker, "Consomme événements année", "Avro / TCP")
    Rel(broker, sr, "Schémas Avro", "HTTP")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

| Élément interne | Nature | Modules Maven impliqués |
|---|---|---|
| `annee-academique-service` | Container exécutable (Spring Boot app) | `annee-academique-service` + libs `common-service`, `kafka-service`, `common-avro` |
| `inscrption-service` | Container exécutable | `inscrption-service` + mêmes libs |
| `common-service`, `common-avro`, `kafka-service` | Libraries partagées (pas des conteneurs) | inclus dans le jar des deux services |

### 6.3 — Niveau 3 : Composants de `annee-academique-service`

```mermaid
C4Component
    title C4 - Niveau 3 : Composants du conteneur annee-academique-service

    Person(admin, "Administration", "")
    ContainerDb(db, "MySQL", "", "")
    ContainerQueue(broker, "Kafka", "", "")

    Container_Boundary(annee, "annee-academique-service") {
      Component(ctrl, "AnneeAcademiqueController", "Spring @RestController", "Expose les endpoints REST")
      Component(advice, "GlobalControllerAdvice", "@ControllerAdvice", "Exception -> ApiError")
      Component(mapper, "AnneeAcademiqueMapper", "Mapper statique", "Request -> Command")

      Component(uc, "Use Case Services<br/>Creer / Modifier / Publier /<br/>OuvrirInscription / FermerInscription /<br/>Cloturer", "Application @Service @Transactional", "Orchestre domaine, repo, outbox")

      Component(aggregate, "AnneeAcademique (Aggregate)", "Domain - POJO", "Invariants, State Pattern, événements")

      Component(repoPort, "AnneeAcademiqueRepository", "Port OUT", "Interface domaine")
      Component(outboxPort, "OutboxPort", "Port OUT", "Interface domaine")

      Component(repoAdapter, "MySqlAdapter", "@Component @Profile(mysql)", "Adapter JPA")
      Component(outboxAdapter, "OutboxEventJpaAdapter", "@Component", "INSERT outbox_event PENDING")
      Component(outboxPub, "OutboxPublisher", "@Service @Scheduled(5s)", "Outbox -> Kafka")
      Component(avroSer, "AvroSerializerUtil", "Avro", "Sérialisation des payloads")
    }

    Rel(admin, ctrl, "HTTPS / JSON")
    Rel(ctrl, mapper, "toCommand")
    Rel(ctrl, uc, "executer(command)")
    Rel(ctrl, advice, "throws")

    Rel(uc, aggregate, "creer / modifier / publier / ...")
    Rel(uc, repoPort, "save / findByCode")
    Rel(uc, outboxPort, "save(events)")

    Rel(repoPort, repoAdapter, "implémenté par")
    Rel(outboxPort, outboxAdapter, "implémenté par")

    Rel(repoAdapter, db, "JDBC")
    Rel(outboxAdapter, db, "JDBC")

    Rel(outboxPub, db, "SELECT / UPDATE outbox", "JDBC")
    Rel(outboxPub, avroSer, "fromBytes")
    Rel(outboxPub, broker, "send(topic, key, AvroModel)", "Avro / TCP")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

### Mapping C4 ↔ Architecture hexagonale

| Concept C4 (Niveau 3) | Couche hexagonale | Package |
|---|---|---|
| `Controller`, `Mapper`, `GlobalControllerAdvice` | Infrastructure (adapter IN) | `infrastructure/web/**` |
| `Use Case Services` | Application | `application/usecase/**` |
| `Port IN` (UseCase interfaces) | Application | `application/port/in/**` |
| `Aggregate AnneeAcademique`, `EtatAnnee`, Value Objects, Events | **Domain (cœur)** | `domain/**` |
| `Port OUT` (Repository, Outbox, DomainEventPublisher) | Application | `application/port/out/**` |
| `MySqlAdapter`, `OutboxEventJpaAdapter`, `OutboxPublisher`, `AvroSerializerUtil` | Infrastructure (adapters OUT) | `infrastructure/persistence/**`, `infrastructure/event/**` |

## Fichiers compagnons

- [`architecture-modules.mermaid`](./architecture-modules.mermaid) — diagramme modules isolé
- [`architecture-hexagonale.mermaid`](./architecture-hexagonale.mermaid) — diagramme hexagonal isolé
- [`annee-academique-state-machine.mermaid`](./annee-academique-state-machine.mermaid) — machine à états isolée
- [`sequence-creer-annee-academique.mermaid`](./sequence-creer-annee-academique.mermaid) — séquence création (version détaillée)
- [`c4-context.mermaid`](./c4-context.mermaid) — C4 niveau 1 (Contexte)
- [`c4-containers.mermaid`](./c4-containers.mermaid) — C4 niveau 2 (Conteneurs)
- [`c4-components-annee-academique.mermaid`](./c4-components-annee-academique.mermaid) — C4 niveau 3 (Composants)
- [`architecture.html`](./architecture.html) — rendu HTML autonome (ouvrir dans un navigateur)
