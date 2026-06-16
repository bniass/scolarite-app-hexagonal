# Scolarite App — Hexagonal

Plateforme de gestion scolaire construite en **microservices** avec une **architecture hexagonale** (Ports & Adapters / DDD), communication asynchrone via **Apache Kafka**, persistance **MySQL** par service, et tests de charge via **k6 + Grafana**.

---

## Table des matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Architecture](#2-architecture)
3. [Microservices](#3-microservices)
4. [Communication inter-services](#4-communication-inter-services)
5. [Saga & compensation](#5-saga--compensation)
6. [Annulation d'inscription](#6-annulation-dinscription)
7. [Modèle de paiement](#7-modèle-de-paiement)
8. [Modules partagés](#8-modules-partagés)
9. [Infrastructure](#9-infrastructure)
10. [Démarrage rapide](#10-démarrage-rapide)
11. [Endpoints REST](#11-endpoints-rest)
12. [Variables de configuration](#12-variables-de-configuration)
13. [Sécurité — Keycloak + WSO2 API Gateway](#13-sécurité--keycloak--wso2-api-gateway)
14. [Tests de charge — k6](#14-tests-de-charge--k6)
15. [Observabilité — Grafana + Prometheus + InfluxDB](#15-observabilité--grafana--prometheus--influxdb)
16. [Décisions techniques](#16-décisions-techniques)

---

## 1. Vue d'ensemble

```
inscription → création étudiant → ouverture dossier paiement → versements
```

Un étudiant s'inscrit pour une année académique dans une classe. Le système :
- crée l'étudiant s'il est nouveau (via `etudiant-service`)
- ouvre son dossier de paiement (via `paiement-service`) à la réception de l'événement Kafka `inscription-creee`
- confirme l'inscription à la réception du premier versement (`paiement-confirme`)
- compense (rollback) en cas d'échec à n'importe quelle étape (Saga choreography)
- permet l'annulation administrative d'une inscription avec suppression physique des données de paiement

**Séparation des rôles :**
- **Admin Scolaire** : crée les inscriptions (sans information de paiement)
- **Comptable** : effectue les versements sur les dossiers

---

## 2. Architecture

### Principe hexagonal appliqué à chaque service

```
┌─────────────────────────────────────────────┐
│                   Service                   │
│                                             │
│  ┌──────────┐   ┌────────────┐   ┌───────┐  │
│  │  Web /   │   │  Domain    │   │  JPA  │  │
│  │  Kafka   │──▶│  Use Cases │──▶│  DB   │  │
│  │ (driving)│   │  (domain)  │   │(driven│  │
│  └──────────┘   └────────────┘   └───────┘  │
│       ▲               │               ▲     │
│       │         Port In/Out           │     │
└───────┼───────────────┼───────────────┼─────┘
        │               │               │
     REST/Kafka      Domaine pur      Adapters
```

Chaque service est structuré en trois couches strictes :

| Couche | Contenu |
|--------|---------|
| `domain` | Modèles, événements, exceptions, value objects — **aucune dépendance externe** |
| `application` | Use cases (ports in/out), commandes — logique métier pure |
| `infrastructure` | Adapters REST, Kafka, JPA, Outbox — détails techniques |

### Modules Maven

```
scolarite-app-hexagonal (parent POM)
├── common-service          ← entité AggregateRoot, utilitaires partagés
├── common-avro             ← schémas Avro (.avsc) pour Kafka
├── kafka-service           ← configuration Kafka commune (producer/consumer)
├── annee-academique-service
├── school-service
├── etudiant-service
├── inscrption-service
└── paiement-service
```

---

## 3. Microservices

### 3.1 `annee-academique-service` — port **8080**

Gère le cycle de vie des années académiques.

**États d'une année :**
```
CREEE → PUBLIEE → INSCRIPTIONS_OUVERTES → INSCRIPTIONS_FERMEES → CLOTUREE
```

**Use cases :**
| Use case | Description |
|----------|-------------|
| `CreerAnneeAcademique` | Crée une année avec ses mois académiques |
| `ModifierAnneeAcademique` | Modifie les informations |
| `PublierAnneeAcademique` | Rend l'année visible |
| `OuvrirInscription` | Ouvre les inscriptions |
| `FermerInscription` | Ferme les inscriptions |
| `CloturerAnneeScolaire` | Clôture définitivement l'année |

**Événements publiés :**
- `creer-annee-request` → notifie `inscrption-service` (projection locale)

---

### 3.2 `school-service` — port **8094**

Gère les filières, classes et tarifs.

**Use cases :**
| Use case | Description |
|----------|-------------|
| `CreerFiliere` | Crée une filière |
| `CreerClasse` | Crée une classe dans une filière |
| `CreerTarif` | Crée un tarif (frais inscription, mensualité, autres frais) |
| `AffecterTarif` | Affecte un tarif actif à une classe |
| `ConsulterTarifActif` | Retourne le tarif actif d'une classe |
| `ConsulterHistoriqueTarifs` | Liste l'historique des tarifs d'une classe |

> `inscrption-service` appelle `school-service` en HTTP synchrone pour récupérer le tarif actif lors d'une inscription.

---

### 3.3 `etudiant-service` — port **8092**

Gère le référentiel des étudiants.

**Use cases :**
| Use case | Description |
|----------|-------------|
| `CreerEtudiant` | Crée un étudiant (retourne l'UUID généré) |
| `ModifierEtudiant` | Met à jour les informations |
| `RechercherEtudiant` | Recherche par critères |
| `SupprimerEtudiant` | Supprime un étudiant (appelé en compensation saga) |

> En cas de rollback saga, l'étudiant créé lors de l'inscription est supprimé **uniquement s'il était nouveau** (flag `etudiantNouveau`).

---

### 3.4 `inscrption-service` — port **8091**

Orchestre la création d'une inscription. Point d'entrée principal du flux métier.

**Use cases :**
| Use case | Description |
|----------|-------------|
| `CreerInscription` | Crée l'inscription, résout l'étudiant, publie `inscription-creee` |
| `ConsulterInscription` | Retourne le détail d'une inscription |
| `ConfirmerInscription` | Passe le statut à `CONFIRMEE` (déclenché par Kafka `paiement-confirme`) |
| `AnnulerInscriptionAdmin` | Annulation administrative (soft delete) + publication `inscription-annulee` |
| `AnnulerInscription` | Compensation saga : supprime physiquement l'inscription + l'étudiant si nouveau |
| `SynchroniserAnneeAcademique` | Maintient la projection locale des années académiques |

**Statuts d'une inscription :**
```
PENDING → CONFIRMEE   (premier versement reçu)
        → ANNULEE     (annulation administrative — soft delete)
        → (supprimée) (compensation saga — suppression physique)
```

**Flag `etudiantNouveau` :**
Stocké en base. Indique si l'étudiant a été créé lors de cette inscription. Seul un étudiant nouveau est supprimé lors de la compensation saga.

---

### 3.5 `paiement-service` — port **8093**

Gère les dossiers de paiement et les versements.

**Use cases :**
| Use case | Description |
|----------|-------------|
| `InitialiserDossier` | Crée le dossier et génère les lignes (déclenché par Kafka `inscription-creee`) |
| `DistribuerVersement` | Distribue un montant sur les lignes non payées dans l'ordre |
| `ConsulterDossier` | Retourne l'état complet du dossier |
| `SupprimerDossier` | Supprime physiquement le dossier + lignes + versements (déclenché par Kafka `inscription-annulee`) |

**Statuts d'un dossier :**
```
INITIALISE → ACTIF (premier versement reçu → publie paiement-confirme CONFIRME)
           → CLOTURE (toutes les lignes payées)
```

**Structure d'un dossier :**
```
DossierPaiement
├── FRAIS_INSCRIPTION  (ordre -2)
├── AUTRES_FRAIS       (ordre -1)
└── MENSUALITE × N     (ordre selon mois académique)
```

**Ordre de règlement des mensualités :**
| Mois | Ordre |
|------|-------|
| Juin | 1 |
| Octobre | 2 |
| Novembre | 3 |
| Décembre | 4 |
| Janvier | 5 |
| Février | 6 |
| Mars | 7 |
| Avril | 8 |
| Mai | 9 |

**Statuts d'une ligne :**
| Statut | Condition |
|--------|-----------|
| `APAYER` | Aucun versement |
| `AVANCE` | Partiellement payée |
| `PAYE` | Intégralement payée |

**Historique des commentaires :**
Chaque versement appende un commentaire horodaté sur la ligne :
```
Octobre 2025 | 03/06/2026 : versé 80000 via MOBILE_MONEY [PAYÉ intégralement]
Décembre 2025 | 03/06/2026 : versé 40000 via MOBILE_MONEY [avance — restant : 40000]
             | 03/06/2026 : versé 40000 via COMPTANT [PAYÉ intégralement]
```

---

## 4. Communication inter-services

### Appels HTTP synchrones

| Appelant | Appelé | Endpoint | Objectif |
|----------|--------|----------|----------|
| `inscrption-service` | `school-service` | `GET /api/classes/{classeId}/tarif-actif` | Récupère le tarif actif |
| `inscrption-service` | `etudiant-service` | `POST /api/etudiants` | Crée un nouvel étudiant |
| `inscrption-service` | `etudiant-service` | `DELETE /api/etudiants/id/{id}` | Supprime l'étudiant (compensation) |

### Événements Kafka asynchrones

| Topic | Producteur | Consommateur | Déclencheur |
|-------|-----------|--------------|-------------|
| `inscription-creee` | `inscrption-service` | `paiement-service` | Inscription créée |
| `inscription-annulee` | `inscrption-service` | `paiement-service` | Annulation administrative |
| `paiement-confirme` (CONFIRME) | `paiement-service` | `inscrption-service` | Premier versement reçu |
| `paiement-confirme` (ECHEC) | `paiement-service` | `inscrption-service` | Échec initialisation dossier |
| `creer-annee-request` | `annee-academique-service` | `inscrption-service` | Année académique publiée |

### Schémas Avro (`common-avro`)

| Schéma | Champs principaux |
|--------|-------------------|
| `InscriptionCreeeAvroModel` | inscriptionId, etudiantId, classeId, codeAnnee, fraisInscription, mensualite, autresFrais, moisAcademiques |
| `InscriptionAnnuleeAvroModel` | inscriptionId |
| `PaiementConfirmeAvroModel` | inscriptionId, statut (`CONFIRME`/`ECHEC`), message |
| `EtudiantCreeAvroModel` | etudiantId, matricule, nom, prenom, dateNaissance, email |
| `EtudiantModifieAvroModel` | etudiantId, matricule, nom, prenom, dateNaissance |
| `CreateAnneeAcademiqueAvroModel` | codeAnnee, etatAnnee, moisAcademiques |

---

## 5. Saga & compensation

Le flux d'inscription utilise une **saga par chorégraphie** — chaque service réagit aux événements Kafka et publie le suivant.

### Flux nominal

```
Client
  │
  ▼
POST /api/inscriptions  (inscrption-service)
  │  ① créer étudiant si nouveau (HTTP → etudiant-service)
  │  ② sauvegarder inscription PENDING
  │  ③ publier inscription-creee → Outbox → Kafka
  │
  ▼
paiement-service (consomme inscription-creee)
  │  ④ initialiser dossier (statut INITIALISE)
  │
  ▼
POST /api/dossiers/{inscriptionId}/versements/distribuer  (paiement-service)
  │  ⑤ distribuer versement → dossier passe INITIALISE → ACTIF
  │  ⑥ publier paiement-confirme{CONFIRME} → Outbox → Kafka
  │
  ▼
inscrption-service (consomme paiement-confirme)
  │  ⑦ passer inscription → CONFIRMEE
```

### Flux de compensation (échec en ④)

```
paiement-service
  │  ✗ échec initialisation dossier
  │  publier paiement-confirme{ECHEC} → Outbox → Kafka
  │
  ▼
inscrption-service (consomme paiement-confirme ECHEC)
  │  supprimer inscription physiquement
  │  si etudiantNouveau=true → DELETE /api/etudiants/id/{id} (HTTP)
```

### Outbox Pattern

Chaque service publie ses événements via une table `outbox_event` en base de données, dans la **même transaction** que la modification métier. Un scheduler `@Scheduled(fixedDelay=5000)` lit la table et publie sur Kafka.

```
Transaction DB:
  INSERT/UPDATE entité + INSERT outbox_event  (atomique)

Scheduler (toutes les 5s):
  SELECT outbox_event WHERE status IN (PENDING, FAILED)
  → dispatch par eventType → publish Kafka
  → UPDATE outbox_event SET status=PUBLISHED
```

L'`OutboxPublisher` de `inscrption-service` dispatche selon le champ `eventType` :
- `InscriptionCreeeEvent` → topic `inscription-creee`
- `InscriptionAnnuleeEvent` → topic `inscription-annulee`

### Idempotence

`paiement-service` vérifie l'existence d'un dossier avant traitement :
```java
if (dossierRepository.trouverParInscriptionId(inscriptionId).isPresent()) {
    // message ignoré — déjà traité
    return;
}
```

---

## 6. Annulation d'inscription

### Règles métier

- Une inscription `CONFIRMEE` (au moins un versement) **ne peut pas** être annulée
- Une inscription `PENDING` peut être annulée par l'admin (soft delete)
- L'étudiant est **conservé** en base (historique)
- Les données de paiement sont **supprimées physiquement** (dossier + lignes + versements + moyens)

### Flux d'annulation

```
POST /api/inscriptions/{id}/annuler
  │  Corps : { "motif": "..." }
  │
  ▼
inscrption-service
  │  ① vérifier statut ≠ CONFIRMEE et ≠ ANNULEE
  │  ② inscription.statut = ANNULEE, annuleLe = now(), motifAnnulation = motif
  │  ③ sauvegarder inscription
  │  ④ publier inscription-annulee → Outbox → Kafka
  │
  ▼
paiement-service (consomme inscription-annulee)
  │  ⑤ deleteByInscriptionId → cascade JPA (lignes + versements + moyens)
```

### Endpoint

```
POST /api/inscriptions/{id}/annuler
Body: { "motif": "Erreur de saisie" }
Response: 204 No Content
```

---

## 7. Modèle de paiement

### Un versement = un seul moyen de paiement

```json
{
  "montant": 160000,
  "datePaiement": "2026-06-03",
  "moyen": {
    "type": "MOBILE_MONEY",
    "operateur": "Orange Money",
    "referencePaiement": "ref-abc123"
  }
}
```

**Types de moyen de paiement (`TypeMoyen`) :**

| Type | Champs requis |
|------|---------------|
| `MOBILE_MONEY` | `operateur`, `referencePaiement` |
| `BANQUE` | `nomBanque`, `numeroTransaction` |
| `COMPTANT` | _(aucun)_ |

### Distribution automatique

`POST /api/dossiers/{inscriptionId}/versements/distribuer` répartit le montant dans l'ordre :
`FRAIS_INSCRIPTION → AUTRES_FRAIS → MENSUALITE (ordre croissant)`

Le surplus d'une ligne est reversé sur la suivante jusqu'à épuisement.

### Règle du premier versement minimum

Le montant minimum du premier versement est :
```
fraisInscription + autresFrais + mensualite
```

### Modèle JPA

```
dossier_paiement
  └── ligne_paiement (1..N, cascade ALL + orphanRemoval)
        └── versement (0..N, cascade ALL + orphanRemoval)
              └── moyen_paiement (1..1, OneToOne, cascade ALL)
```

---

## 8. Modules partagés

### `common-service`
- `AggregateRoot<ID>` : classe de base pour les agrégats DDD (gestion des domain events via `addEvent` / `pullDomainEvents`)

### `common-avro`
- Contient tous les schémas `.avsc` Avro
- Génère les classes Java via le plugin `avro-maven-plugin`
- **À installer en premier** avant de compiler les autres modules : `mvn install -pl common-avro`

### `kafka-service`
- Configuration centralisée Kafka (producer / consumer)
- `KafkaProducerConfig`, `KafkaConsumerConfig`
- Paramétrable via `kafka-config`, `kafka-producer`, `kafka-consumer` dans `application.yml`

---

## 9. Infrastructure

### Démarrage de l'infrastructure (Docker Compose)

```bash
docker-compose up -d
```

| Conteneur | Port local | Rôle |
|-----------|-----------|------|
| `zookeeper-scolarite` | 2181 | Coordination Kafka |
| `kafka-broker-scolarite-1` | 19092 | Broker Kafka |
| `kafka-broker-scolarite-2` | 29092 | Broker Kafka |
| `kafka-broker-scolarite-3` | 39092 | Broker Kafka |
| `schema-registry-scolarite` | 8081 | Registre schémas Avro |
| `control-center-scolarite` | 9021 | UI monitoring Kafka |
| `keycloak-scolarite` | 8180 | Identity Provider (SSO) |
| `wso2am-scolarite` | 8280 / 8243 / 9443 | API Gateway |

> **Important** : ajouter `keycloak` au fichier hosts de la machine hôte :
> ```bash
> sudo sh -c 'echo "127.0.0.1 keycloak" >> /etc/hosts'
> ```

### Stack de monitoring (k6/)

```bash
cd k6
docker compose up -d
```

| Conteneur | Port local | Rôle |
|-----------|-----------|------|
| `k6-influxdb` | 8086 | Stockage métriques k6 |
| `k6-prometheus` | 9090 | Scraping métriques JVM |
| `k6-grafana` | 3001 | Dashboards |

### Bases de données MySQL

Chaque service possède sa propre base de données (isolation complète) :

| Service | Base de données |
|---------|----------------|
| `annee-academique-service` | `annee-academique-service` |
| `school-service` | `school-service` |
| `etudiant-service` | `etudiant-service` |
| `inscrption-service` | `inscription-service` |
| `paiement-service` | `paiement-service` |

Par défaut : `host=localhost:3306`, `username=root`, `password=root`

---

## 10. Démarrage rapide

### Prérequis

- Java 21+
- Maven 3.9+ (ou `./mvnw`)
- Docker & Docker Compose
- MySQL sur le port 3306
- k6 (`brew install k6` sur macOS)

### Étapes

```bash
# 1. Démarrer l'infrastructure Kafka + Keycloak + WSO2
docker-compose up -d

# 2. Démarrer la stack de monitoring
cd k6 && docker compose up -d && cd ..

# 3. Compiler les modules communs (common-avro en premier)
./mvnw install -pl common-service,common-avro,kafka-service

# 4. Démarrer les services
./mvnw spring-boot:run -pl annee-academique-service   # :8080
./mvnw spring-boot:run -pl school-service              # :8094
./mvnw spring-boot:run -pl etudiant-service            # :8092
./mvnw spring-boot:run -pl inscrption-service          # :8091
./mvnw spring-boot:run -pl paiement-service            # :8093
```

### Ordre recommandé pour un test complet

1. Créer une filière et une classe (`school-service`)
2. Créer un tarif et l'affecter à la classe (`school-service`)
3. Créer une année académique et ouvrir les inscriptions (`annee-academique-service`)
4. Créer une inscription (`inscrption-service`) — **sans information de paiement**
5. Attendre que le dossier soit initialisé (Kafka async ~1-2s)
6. Effectuer le premier versement (`paiement-service`)
7. Vérifier que l'inscription passe en `CONFIRMEE`
8. Effectuer des versements complémentaires si nécessaire

---

## 11. Endpoints REST

### `annee-academique-service` — :8080

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/annees` | Créer une année académique |
| `GET` | `/api/annees` | Lister les années |
| `PUT` | `/api/annees/{id}` | Modifier une année |
| `POST` | `/api/annees/{id}/publier` | Publier |
| `POST` | `/api/annees/{id}/ouvrir-inscriptions` | Ouvrir inscriptions |
| `POST` | `/api/annees/{id}/fermer-inscriptions` | Fermer inscriptions |
| `POST` | `/api/annees/{id}/cloturer` | Clôturer |

### `school-service` — :8094

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/filieres` | Créer une filière |
| `GET` | `/api/filieres` | Lister les filières |
| `POST` | `/api/classes` | Créer une classe |
| `GET` | `/api/classes` | Lister les classes |
| `GET` | `/api/classes/{classeId}/tarif-actif` | Tarif actif d'une classe |
| `POST` | `/api/tarifs` | Créer un tarif |
| `POST` | `/api/tarifs/{tarifId}/affecter/{classeId}` | Affecter un tarif |
| `GET` | `/api/tarifs/classes/{classeId}/historique` | Historique des tarifs |

### `etudiant-service` — :8092

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/etudiants` | Créer un étudiant |
| `PUT` | `/api/etudiants/{id}` | Modifier |
| `GET` | `/api/etudiants` | Rechercher |
| `DELETE` | `/api/etudiants/id/{id}` | Supprimer |

### `inscrption-service` — :8091

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/inscriptions` | Créer une inscription |
| `GET` | `/api/inscriptions/{id}` | Consulter une inscription |
| `POST` | `/api/inscriptions/{id}/annuler` | Annuler administrativement |

**Corps de création — nouvel étudiant :**
```json
{
  "nouvelEtudiant": {
    "nom": "Fall",
    "prenom": "Mamadou",
    "dateNaissance": "2000-05-15",
    "email": "mamadoufall@ecole.sn"
  },
  "classeId": "uuid-classe",
  "codeAnnee": "2025-2026"
}
```

**Corps de création — étudiant existant :**
```json
{
  "etudiantId": "uuid-etudiant",
  "classeId": "uuid-classe",
  "codeAnnee": "2025-2026"
}
```

**Corps d'annulation :**
```json
{ "motif": "Erreur de saisie" }
```

### `paiement-service` — :8093

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/api/dossiers/{inscriptionId}` | Consulter le dossier |
| `POST` | `/api/dossiers/{inscriptionId}/versements/distribuer` | Effectuer un versement |

**Corps de versement :**
```json
{
  "montant": 160000,
  "datePaiement": "2026-06-03",
  "moyen": {
    "type": "MOBILE_MONEY",
    "operateur": "Orange Money",
    "referencePaiement": "ref-abc"
  }
}
```

**Exemples de moyen :**
```json
{ "type": "COMPTANT" }
{ "type": "BANQUE", "nomBanque": "CBAO", "numeroTransaction": "TXN-001" }
```

---

## 12. Variables de configuration

Variables communes à tous les services :

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/<nom-base>
    username: root
    password: root
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://keycloak:8180/realms/scolarite/protocol/openid-connect/certs
          issuer-uri: http://keycloak:8180/realms/scolarite

kafka-config:
  bootstrap-servers: localhost:19092,localhost:29092,localhost:39092
  schema-registry-url: http://localhost:8081
  num-of-partitions: 3
  replication-factor: 3

kafka-producer:
  acks: all
  retry-count: 2147483647
  idempotence: true
  compression-type: snappy

kafka-consumer:
  auto-offset-reset: earliest
  auto-commit: false
  concurrency: 3

kafka-topics:
  inscription-creee-topic: inscription-creee
  inscription-annulee-topic: inscription-annulee
  paiement-confirme-topic: paiement-confirme

app:
  keycloak:
    public-url: ${APP_KEYCLOAK_PUBLIC_URL:http://localhost:8180}  # URL navigateur pour Swagger
    realm: ${APP_KEYCLOAK_REALM:scolarite}

management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,info,metrics
  metrics:
    tags:
      application: ${spring.application.name}
```

---

## 13. Sécurité — Keycloak + WSO2 API Gateway

### Architecture

```
Client (mobile / web / Swagger)
        │
        │ 1. POST /token → Keycloak (http://localhost:8180)
        │    ← access_token JWT signé RS256
        │
        │ 2. GET|POST /api/... → WSO2 Gateway (8243 HTTPS / 8280 HTTP)
        │    Authorization: Bearer <token>
        ▼
  ┌──────────────┐       ┌─────────────────┐
  │  WSO2 AM     │──────▶│  Keycloak 24    │
  │  Gateway     │ JWKS  │  realm:scolarite│
  │  Key Manager │◀──────│  :8180          │
  └──────┬───────┘       └─────────────────┘
         │ forward + Authorization: Bearer (enable_outbound_auth_header=true)
    ┌────┴────────────────────────────────────┐
    ▼         ▼           ▼                  ▼
inscrption  school    etudiant           paiement
:8091       :8094      :8092              :8093
  (valident le JWT avec la clé publique Keycloak via JWKS)
```

### Configuration Keycloak

```bash
docker-compose up -d keycloak-scolarite
# Admin : http://localhost:8180 — admin/admin
```

**Realm :** `scolarite`

**Rôles :**
| Rôle | Usage |
|------|-------|
| `super` | Accès total (POST, PUT, DELETE, GET) |
| `admin` | Gestion courante (POST, PUT, GET selon service) |
| `user` | Lecture seule (GET) |

**Utilisateurs :**
| Utilisateur | Rôles |
|-------------|-------|
| `baye` | `super`, `admin`, `user` |
| `mouha` | `admin`, `user` |
| `barham` | `user` |

**Client `client-frontend` :**
- Standard flow ON, Direct access grants ON
- Web Origins : `http://localhost:8080` (et ports des autres services) pour autoriser Swagger OAuth2

**Client `wso2-dcr` :**
- Client authentication ON, Service accounts roles ON
- Service account roles : `create-client`, `manage-clients`, `view-clients` (realm-management)

### Swagger — Authentification OAuth2

Chaque service expose Swagger UI avec un formulaire de connexion OAuth2 :

```
http://localhost:8091/swagger-ui/index.html  (inscrption-service)
http://localhost:8093/swagger-ui/index.html  (paiement-service)
...
```

Cliquer **Authorize** → saisir `client_id=client-frontend`, `username`, `password`.

Pour obtenir un token manuellement :
```bash
curl -s -X POST http://localhost:8180/realms/scolarite/protocol/openid-connect/token \
  -d 'grant_type=password&client_id=client-frontend&username=baye&password=<pass>' \
  | jq -r .access_token
```

### Configuration WSO2

```bash
docker-compose up -d wso2am
# Publisher  : https://localhost:9443/publisher  — admin/admin
# DevPortal  : https://localhost:9443/devportal
# Admin      : https://localhost:9443/admin
```

**Patch obligatoire** (transmettre le token au backend) :
```bash
docker exec wso2am-scolarite bash -c "echo -e '\n[apim.oauth_config]\nenable_outbound_auth_header = true' >> /home/wso2carbon/wso2am-4.7.0/repository/conf/deployment.toml"
docker restart wso2am-scolarite
```

**Key Manager Keycloak dans WSO2 :**
- Well-known URL : `http://keycloak:8180/realms/scolarite/.well-known/openid-configuration`
- Self Validate JWT : ✅
- Client ID/Secret : ceux du client `wso2-dcr`

**Endpoints backend à configurer dans WSO2 :**
| Service | URL backend |
|---------|------------|
| `annee-academique-service` | `http://host.docker.internal:8080` |
| `inscrption-service` | `http://host.docker.internal:8091` |
| `etudiant-service` | `http://host.docker.internal:8092` |
| `paiement-service` | `http://host.docker.internal:8093` |
| `school-service` | `http://host.docker.internal:8094` |

> Sur Linux : remplacer `host.docker.internal` par `172.17.0.1`

**Matrice des droits par service :**
| Service | POST/PUT | DELETE | GET |
|---------|----------|--------|-----|
| `annee-academique-service` | `super` | — | `admin`, `super` |
| `school-service` | `super` | `super` | `admin`, `super`, `user` |
| `etudiant-service` | `admin`, `super` | `super` | `admin`, `super`, `user` |
| `inscrption-service` | `admin`, `super` | `super` | `admin`, `super`, `user` |
| `paiement-service` | `admin`, `super` | — | `admin`, `super`, `user` |

---

## 14. Tests de charge — k6

### Prérequis

```bash
brew install k6  # macOS
cd k6 && docker compose up -d  # InfluxDB + Prometheus + Grafana
```

### Tests disponibles

| Fichier | Objectif | Durée |
|---------|----------|-------|
| `inscription-test.js` | Test nominal — 6 scénarios (nouveau/ancien × 3 moyens) | ~1m30 |
| `stress-test.js` | Montée en charge 90→500 VUs jusqu'au point de rupture | ~16m |
| `soak-test.js` | Charge constante 90 VUs — détection fuites mémoire | ~18m |

### Paramètres obligatoires

| Paramètre | Description | Exemple |
|-----------|-------------|---------|
| `MONTANT` | Montant du versement initial | `150000` |
| `CLASSE_ID` | UUID de la classe à inscrire | `uuid-v4` |
| `CODE_ANNEE` | Code de l'année académique | `2025-2026` (défaut) |
| `USERNAME` / `PASSWORD` | Identifiants Keycloak | `baye` / `passer` (défaut) |

### Lancement

```bash
cd k6

# Test nominal
k6 run --config config.json inscription-test.js \
  -e MONTANT=150000 -e CLASSE_ID=<uuid>

# Stress test
k6 run --config config.json stress-test.js \
  -e MONTANT=150000 -e CLASSE_ID=<uuid>

# Soak test (15 min)
k6 run --config config.json soak-test.js \
  -e MONTANT=150000 -e CLASSE_ID=<uuid>
```

### Thresholds (test nominal)

| Scénario | Threshold | Valeurs observées |
|----------|-----------|-------------------|
| `nouveau_*` | p(95) < 800ms | ~35-45ms |
| `ancien_*` | p(95) < 1000ms | ~110-150ms |
| `http_req_failed{endpoint:metier}` | rate < 1% | 0% |

> Les requêtes de polling Kafka (404 attendus pendant l'attente asynchrone) sont taguées `endpoint:polling` et **exclues** des thresholds.

### Architecture des scénarios

```
setup()
  ├── vérifier année en InscriptionsOuvertes
  ├── vérifier classe avec tarif actif
  └── créer 3 pools de 20 étudiants (ancien_comptant, ancien_mobile_money, ancien_banque)

run() par VU
  ├── inscrire (POST /api/inscriptions)          → tagué endpoint:metier
  ├── polling dossier (GET /api/dossiers/{id})   → tagué endpoint:polling (404 OK)
  └── verser (POST /api/dossiers/{id}/versements/distribuer) → tagué endpoint:metier

teardown()
  └── supprimer les 60 étudiants créés en setup
```

---

## 15. Observabilité — Grafana + Prometheus + InfluxDB

### Accès

| Interface | URL | Usage |
|-----------|-----|-------|
| Grafana | http://localhost:3001 | Dashboards |
| Prometheus | http://localhost:9090 | Métriques JVM brutes |
| InfluxDB | http://localhost:8086 | Métriques k6 brutes |
| Confluent Control Center | http://localhost:9021 | Monitoring Kafka |

### Dashboards Grafana à importer

| Dashboard | ID | Datasource | Contenu |
|-----------|----|------------|---------|
| k6 Load Testing Results | `2587` | InfluxDB | VUs, req/s, p95, erreurs |
| JVM Micrometer | `4701` | Prometheus | Heap, GC, threads, connexions DB |

### Métriques JVM exposées

Chaque service Spring Boot expose `/actuator/prometheus` (sans authentification) avec :
- `jvm_memory_used_bytes` — utilisation heap/non-heap
- `jvm_gc_pause_seconds` — pauses GC
- `jvm_threads_live_threads` — threads actifs
- `hikaricp_connections_active` — connexions DB pool
- `http_server_requests_seconds` — latences REST par endpoint

### Prometheus — vérification

```bash
# Targets UP
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[].health'

# Métriques disponibles
curl -s "http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes{application=\"inscription-service\"}"
```

### Nettoyage données k6 entre runs

```sql
-- inscrption-service DB
DELETE FROM outbox_event WHERE status IN ('PENDING', 'FAILED');
```

```bash
# Vider les métriques InfluxDB entre runs (optionnel)
curl -X POST "http://localhost:8086/query?db=k6" --data-urlencode "q=DROP SERIES FROM /.*/"
```

---

## 16. Décisions techniques

| Décision | Justification |
|----------|---------------|
| Architecture hexagonale | Isolation totale du domaine métier, testabilité, indépendance des frameworks |
| Saga choreography | Pas de coordinateur central, couplage minimal entre services |
| Outbox Pattern | Garantie de publication des événements même en cas de crash (atomicité DB + Kafka) |
| Avro + Schema Registry | Contrat de message versionné et évolutif entre services |
| Séparation Admin / Comptable | Inscription sans paiement — le Comptable effectue les versements séparément |
| `etudiantNouveau` flag | Distingue les étudiants créés lors de l'inscription (rollbackables) des existants |
| Annulation soft delete | L'historique est conservé ; seules les données de paiement sont supprimées physiquement |
| `inscription-annulee` saga | Kafka choreography pour la suppression async du dossier paiement |
| Un seul moyen de paiement par versement | Simplicité du modèle, traçabilité claire, évite les conflits Hibernate |
| Distribution automatique des versements | Le client ne manipule pas les identifiants internes des lignes |
| `DossierInitialiseEvent` au premier versement | Évite la confirmation immédiate de l'inscription sans versement réel |
| Tag `endpoint:metier` / `endpoint:polling` | Sépare les métriques métier des 404 de polling Kafka dans k6 |
| `RUN_ID = Date.now()` dans k6 | Évite les collisions d'email entre runs successifs |
| Bearer auth Swagger configurable | URL Keycloak injectée via `app.keycloak.public-url` pour compatibilité hors Docker |
