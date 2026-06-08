# Scolarite App — Hexagonal

Plateforme de gestion scolaire construite en **microservices** avec une **architecture hexagonale** (Ports & Adapters / DDD), communication asynchrone via **Apache Kafka** et persistance **MySQL** par service.

---

## Table des matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Architecture](#2-architecture)
3. [Microservices](#3-microservices)
4. [Communication inter-services](#4-communication-inter-services)
5. [Saga & compensation](#5-saga--compensation)
6. [Modèle de paiement](#6-modèle-de-paiement)
7. [Modules partagés](#7-modules-partagés)
8. [Infrastructure](#8-infrastructure)
9. [Démarrage rapide](#9-démarrage-rapide)
10. [Endpoints REST](#10-endpoints-rest)
11. [Variables de configuration](#11-variables-de-configuration)
12. [Sécurité — Keycloak + WSO2 API Gateway](#12-sécurité--keycloak--wso2-api-gateway)

---

## 1. Vue d'ensemble

```
inscription → création étudiant → ouverture dossier paiement → versements
```

Un étudiant s'inscrit pour une année académique dans une classe. Le système :
- crée l'étudiant s'il est nouveau (via `etudiant-service`)
- ouvre son dossier de paiement (via `paiement-service`)
- distribue le montant initial sur les lignes de paiement dans l'ordre académique
- compense (rollback) en cas d'échec à n'importe quelle étape (Saga choreography)

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

> `inscrption-service` appelle `school-service` en HTTP synchrone pour récupérer le tarif actif d'une classe lors d'une inscription.

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

> `inscrption-service` appelle ce service en HTTP synchrone. En cas de rollback saga, l'étudiant créé lors de l'inscription est supprimé **uniquement s'il était nouveau** (flag `etudiantNouveau`).

---

### 3.4 `inscrption-service` — port **8091**

Orchestre la création d'une inscription. Point d'entrée principal du flux métier.

**Use cases :**
| Use case | Description |
|----------|-------------|
| `CreerInscription` | Crée l'inscription, résout l'étudiant, publie l'événement |
| `ConsulterInscription` | Retourne le détail d'une inscription |
| `ConfirmerInscription` | Passe le statut à CONFIRMEE (consommé depuis Kafka) |
| `AnnulerInscription` | Compense : supprime l'inscription + l'étudiant si nouveau |
| `SynchroniserAnneeAcademique` | Maintient la projection locale des années académiques |

**Statuts d'une inscription :**
```
EN_ATTENTE → CONFIRMEE
           ↘ (supprimée physiquement si saga échoue)
```

**Contraintes de validation du montant initial :**
- `montant > 0`
- `montant ≤ total annuel` (frais inscription + autres frais + mensualité × nb mois)

**Flag `etudiantNouveau` :**  
Stocké en base. Indique si l'étudiant a été créé lors de cette inscription. Seul un étudiant nouveau est supprimé lors de la compensation saga.

---

### 3.5 `paiement-service` — port **8093**

Gère les dossiers de paiement et les versements.

**Use cases :**
| Use case | Description |
|----------|-------------|
| `InitialiserDossier` | Crée le dossier, génère les lignes, distribue le versement initial |
| `DistribuerVersement` | Distribue un montant sur les lignes non payées dans l'ordre |
| `ConsulterDossier` | Retourne l'état complet du dossier |

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

**Contraintes de versement :**
- `montant > 0`
- `montant ≤ total restant dû`

**Historique des commentaires :**  
Chaque versement appende un commentaire horodaté sur la ligne :
```
Octobre 2025 | 03/06/2026 : versé 80000 via MOBILE_MONEY [PAYÉ intégralement]
Décembre 2025 | 03/06/2026 : versé 40000 via MOBILE_MONEY [avance — restant : 40000] | 03/06/2026 : versé 40000 via COMPTANT [PAYÉ intégralement]
```

---

## 4. Communication inter-services

### Appels HTTP synchrones

| Appelant | Appelé | Endpoint | Objectif |
|----------|--------|----------|----------|
| `inscrption-service` | `school-service` | `GET /api/tarifs/classes/{classeId}/actif` | Récupère le tarif actif |
| `inscrption-service` | `etudiant-service` | `POST /api/etudiants` | Crée un nouvel étudiant |
| `inscrption-service` | `etudiant-service` | `DELETE /api/etudiants/{id}` | Supprime l'étudiant (compensation) |

### Événements Kafka asynchrones

| Topic | Producteur | Consommateur | Format | Déclencheur |
|-------|-----------|--------------|--------|-------------|
| `inscription-creee` | `inscrption-service` | `paiement-service` | Avro | Inscription créée |
| `paiement-confirme` (statut=`CONFIRME`) | `paiement-service` | `inscrption-service` | Avro | Dossier initialisé avec succès |
| `paiement-confirme` (statut=`ECHEC`) | `paiement-service` | `inscrption-service` | Avro | Échec initialisation dossier |
| `creer-annee-request` | `annee-academique-service` | `inscrption-service` | Avro | Année académique publiée |

### Schémas Avro (`common-avro`)

| Schéma | Champs principaux |
|--------|-------------------|
| `InscriptionCreeeAvroModel` | inscriptionId, etudiantId, classeId, codeAnnee, fraisInscription, mensualite, autresFrais, moisAcademiques |
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
  │  ② sauvegarder inscription EN_ATTENTE
  │  ③ publier InscriptionCreee → Outbox → Kafka
  │
  ▼
paiement-service (consomme inscription-creee)
  │  ④ initialiser dossier + distribuer versement initial
  │  ⑤ publier PaiementConfirme{statut=CONFIRME} → Outbox → Kafka
  │
  ▼
inscrption-service (consomme paiement-confirme)
  │  ⑥ passer inscription → CONFIRMEE
```

### Flux de compensation (échec en ④)

```
paiement-service
  │  ✗ échec initialisation dossier
  │  publier PaiementConfirme{statut=ECHEC} → Outbox → Kafka
  │
  ▼
inscrption-service (consomme paiement-confirme ECHEC)
  │  supprimer inscription physiquement
  │  si etudiantNouveau=true → DELETE /api/etudiants/{id} (HTTP)
```

### Outbox Pattern

Chaque service publie ses événements via une table `outbox` en base de données, dans la **même transaction** que la modification métier. Un scheduler `@Scheduled` lit la table et publie sur Kafka, puis marque les événements comme envoyés.

```
Transaction DB:
  INSERT inscription + INSERT outbox_event (atomique)

Scheduler (async):
  SELECT outbox_event WHERE sent=false
  → publish Kafka
  → UPDATE outbox_event SET sent=true
```

### Idempotence

`paiement-service` vérifie l'existence d'un dossier avant traitement :
```java
if (dossierRepository.trouverParInscriptionId(inscriptionId).isPresent()) {
    // message ignoré — déjà traité
    return;
}
```

---

## 6. Modèle de paiement

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

`POST /{inscriptionId}/versements/distribuer` répartit le montant dans l'ordre :  
`FRAIS_INSCRIPTION → AUTRES_FRAIS → MENSUALITE (ordre croissant)`

Le surplus d'une ligne est reversé sur la suivante jusqu'à épuisement.

### Modèle JPA

```
dossier_paiement
  └── ligne_paiement (1..N)
        └── versement (0..N)
              └── moyen_paiement (1..1, OneToOne)
```

---

## 7. Modules partagés

### `common-service`
- `AggregateRoot<ID>` : classe de base pour les agrégats DDD (gestion des domain events)

### `common-avro`
- Contient tous les schémas `.avsc` Avro
- Génère les classes Java via le plugin `avro-maven-plugin`
- Dépendance : tous les services qui produisent ou consomment des événements

### `kafka-service`
- Configuration centralisée Kafka (producer / consumer)
- `KafkaProducerConfig`, `KafkaConsumerConfig`
- Paramétrable via `kafka-config`, `kafka-producer`, `kafka-consumer` dans `application.yml`

---

## 8. Infrastructure

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

> Confluent Control Center accessible sur **http://localhost:9021**

> **Important** : ajouter `keycloak` au fichier hosts de la machine hôte pour que les tokens JWT aient le bon issuer :
> ```bash
> sudo sh -c 'echo "127.0.0.1 keycloak" >> /etc/hosts'
> ```

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
Configurable dans chaque `application.yml`.

---

## 9. Démarrage rapide

### Prérequis

- Java 21
- Maven 3.9+
- Docker & Docker Compose
- MySQL accessible sur le port 3306

### Étapes

```bash
# 1. Démarrer l'infrastructure Kafka
docker-compose up -d

# 2. Créer les bases de données MySQL
# (une base par service, voir tableau ci-dessus)

# 3. Compiler tous les modules (common-avro en premier)
./mvnw clean install -pl common-service,common-avro,kafka-service

# 4. Démarrer les services (dans n'importe quel ordre)
./mvnw spring-boot:run -pl annee-academique-service   # :8080
./mvnw spring-boot:run -pl school-service
./mvnw spring-boot:run -pl etudiant-service
./mvnw spring-boot:run -pl inscrption-service
./mvnw spring-boot:run -pl paiement-service
```

### Ordre recommandé pour un test complet

1. Créer une filière et une classe (`school-service`)
2. Créer un tarif et l'affecter à la classe (`school-service`)
3. Créer une année académique et ouvrir les inscriptions (`annee-academique-service`)
4. Créer une inscription avec versement initial (`inscrption-service`)
5. Consulter le dossier de paiement (`paiement-service`)
6. Effectuer des versements complémentaires (`paiement-service`)

---

## 10. Endpoints REST

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
| `POST` | `/api/tarifs` | Créer un tarif |
| `POST` | `/api/tarifs/{tarifId}/affecter/{classeId}` | Affecter un tarif |
| `GET` | `/api/tarifs/classes/{classeId}/actif` | Tarif actif d'une classe |
| `GET` | `/api/tarifs/classes/{classeId}/historique` | Historique des tarifs |

### `etudiant-service` — :8092

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/etudiants` | Créer un étudiant |
| `PUT` | `/api/etudiants/{id}` | Modifier |
| `GET` | `/api/etudiants` | Rechercher |
| `DELETE` | `/api/etudiants/{id}` | Supprimer |

### `inscrption-service` — :8091

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/inscriptions` | Créer une inscription |
| `GET` | `/api/inscriptions/{id}` | Consulter une inscription |

**Corps de création :**
```json
{
  "etudiantId": null,
  "nouvelEtudiant": {
    "nom": "Fall",
    "prenom": "Mamadou",
    "dateNaissance": "2000-05-15",
    "email": "mamadoufall@ecole.sn"
  },
  "classeId": "uuid-classe",
  "codeAnnee": "2025-2026",
  "montant": 260000,
  "typePaiement": "MOBILE_MONEY",
  "operateur": "Orange Money",
  "referencePaiement": "ref-xyz"
}
```

> `etudiantId` et `nouvelEtudiant` sont mutuellement exclusifs. Passer `etudiantId` pour un étudiant existant.

### `paiement-service` — :8093

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/api/dossiers/{inscriptionId}` | Consulter le dossier |
| `GET` | `/api/dossiers/{inscriptionId}/prochaine-ligne` | Prochaine ligne à payer |
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

## 11. Variables de configuration

Les variables communes à tous les services (à adapter dans chaque `application.yml`) :

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/<nom-base>
    username: root
    password: root

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
```

---

---

## 12. Sécurité — Keycloak + WSO2 API Gateway

### Architecture

```
Client (mobile / web / Swagger)
        │
        │ 1. POST /token → Keycloak (via hostname keycloak:8180)
        │    ← access_token JWT (iss=http://keycloak:8180/realms/scolarite)
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
inscrption  school    etudiant           paiement   annee-academique
:8091       :8094      :8092              :8093          :8080
  (valident le JWT avec la clé publique Keycloak via JWKS)
```

**Keycloak est l'autorité d'authentification.** WSO2 joue le rôle de gateway (routage, rate limiting, sécurité périmètre) et délègue la validation des tokens à Keycloak via JWKS (Self Validate JWT).

---

### Prérequis — fichier hosts

Ajouter `keycloak` à `/etc/hosts` sur la machine hôte pour que les tokens aient `iss=http://keycloak:8180/...` (requis par WSO2 et Spring Boot) :

```bash
sudo sh -c 'echo "127.0.0.1 keycloak" >> /etc/hosts'
```

---

### Configuration Keycloak

#### 1. Démarrer Keycloak

```bash
docker-compose up -d keycloak-scolarite
```

Keycloak Admin : **http://localhost:8180** — `admin / admin`

#### 2. Créer le realm `scolarite`

Admin Console → **Create Realm** → name: `scolarite`

#### 3. Créer les rôles realm

Realm Settings → **Roles** → Add :

| Rôle | Usage |
|------|-------|
| `super` | Accès total (POST, PUT, DELETE, GET) |
| `admin` | Gestion courante (POST, PUT, GET selon service) |
| `user` | Lecture seule (GET) |

#### 4. Créer les utilisateurs

**Users → Add User** pour chaque utilisateur, puis onglet **Credentials** (mot de passe `admin`, Temporary OFF) et onglet **Role Mappings** :

| Utilisateur | Rôles |
|-------------|-------|
| `baye` | `super`, `admin`, `user` |
| `mouha` | `admin`, `user` |
| `barham` | `user` |

#### 5. Créer le client `wso2-dcr`

**Clients → Create Client** :

| Champ | Valeur |
|-------|--------|
| Client ID | `wso2-dcr` |
| Client authentication | ON (confidential) |
| Service accounts roles | ON |
| Standard flow | OFF |

Onglet **Service account roles** → Assign role → Filter by clients → `realm-management` → ajouter `create-client`, `manage-clients`, `view-clients`.

Onglet **Credentials** → noter le `Client secret`.

Onglet **Client Scopes** → Add client scope → `openid` (Optional).

---

### Configuration WSO2

#### Démarrage

```bash
docker-compose up -d wso2am
```

> L'image `wso2am:4.7.0` doit être construite localement :
> ```bash
> git clone --depth 1 https://github.com/wso2/docker-apim.git /tmp/docker-apim
> cd /tmp/docker-apim/dockerfiles/ubuntu/apim
> docker build -t wso2am:4.7.0 .
> ```

Attendre ~2 min. Vérifier :
```bash
curl -k -o /dev/null -s -w "%{http_code}" https://localhost:9443/carbon/admin/login.jsp
# → 200
```

**Portails WSO2 :**

| Portail | URL | Identifiants |
|---------|-----|--------------|
| Publisher | https://localhost:9443/publisher | admin / admin |
| Developer Portal | https://localhost:9443/devportal | admin / admin |
| Admin Console | https://localhost:9443/admin | admin / admin |

#### Patch `deployment.toml` — transmettre le token au backend

Sans ce patch, WSO2 strip le header `Authorization` avant de forwarder vers Spring Boot :

```bash
docker exec wso2am-scolarite bash -c "echo -e '\n[apim.oauth_config]\nenable_outbound_auth_header = true' >> /home/wso2carbon/wso2am-4.7.0/repository/conf/deployment.toml"
docker restart wso2am-scolarite
```

#### Configurer Keycloak comme Key Manager

**Admin Console → Key Managers → Add Key Manager** :

| Champ | Valeur |
|-------|--------|
| Type | KeyCloak |
| Well-known URL | `http://keycloak:8180/realms/scolarite/.well-known/openid-configuration` |
| → cliquer **Import** | (auto-remplit les endpoints) |
| Issuer | `http://keycloak:8180/realms/scolarite` |
| Client ID | `wso2-dcr` |
| Client Secret | `<secret copié depuis Keycloak>` |
| Self Validate JWT | ✅ activé |

Sauvegarder.

#### Publier les APIs

**Étape 1 — Télécharger les specs OpenAPI** (services démarrés localement) :

```bash
curl http://localhost:8080/v3/api-docs -o annee-api.json
curl http://localhost:8091/v3/api-docs -o inscription-api.json
curl http://localhost:8092/v3/api-docs -o etudiant-api.json
curl http://localhost:8093/v3/api-docs -o paiement-api.json
curl http://localhost:8094/v3/api-docs -o school-api.json
```

**Étape 2 — Importer dans Publisher** :

1. **https://localhost:9443/publisher** → Create API → Import OpenAPI
2. Uploader le fichier JSON
3. Endpoint backend :

| Service | URL backend |
|---------|------------|
| `annee-academique-service` | `http://host.docker.internal:8080` |
| `inscrption-service` | `http://host.docker.internal:8091` |
| `etudiant-service` | `http://host.docker.internal:8092` |
| `paiement-service` | `http://host.docker.internal:8093` |
| `school-service` | `http://host.docker.internal:8094` |

> Sur Linux : remplacer `host.docker.internal` par `172.17.0.1`

4. Business Plans → `Unlimited`
5. **Deploy** → **Publish**

**Étape 3 — Créer une application et générer les clés** :

1. **https://localhost:9443/devportal** → Applications → New Application (`ScolariteApp`)
2. Subscriptions → s'abonner à chaque API
3. **Production Keys** → onglet **KEYCLOAK** → Generate Keys
4. Noter le `Consumer Key` et `Consumer Secret`

---

### Obtenir un token et appeler une API

#### 1. Obtenir un token depuis Keycloak

```bash
curl -s -X POST http://keycloak:8180/realms/scolarite/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "username=baye" \
  -d "password=admin" \
  -d "client_id=<CONSUMER_KEY>" \
  -d "client_secret=<CONSUMER_SECRET>"
```

Réponse :
```json
{
  "access_token": "eyJ...",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

#### 2. Appeler une API via WSO2 Gateway

```bash
TOKEN="eyJ..."

# Exemple : lister les années académiques
curl -k -X GET "https://localhost:8243/anneeA-academique-service-api/v1/api/academic-years" \
  -H "Authorization: Bearer $TOKEN"

# Exemple : créer une inscription
curl -k -X POST "https://localhost:8243/inscription-service-api/v1/api/inscriptions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ ... }'
```

---

### Validation JWT côté Spring Boot

Chaque service valide le JWT avec la **clé publique Keycloak** (JWKS) :

```yaml
# application.yml de chaque service
spring:
  main:
    allow-bean-definition-overriding: true
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://keycloak:8180/realms/scolarite/protocol/openid-connect/certs
          issuer-uri: http://keycloak:8180/realms/scolarite
```

Les rôles sont extraits depuis `realm_access.roles` du token JWT via un `JwtAuthenticationConverter` personnalisé dans chaque `SecurityConfig`.

**Matrice des droits par service :**

| Service | POST/PUT | DELETE | GET |
|---------|----------|--------|-----|
| `annee-academique-service` | `super` | — | `admin`, `super` |
| `school-service` | `super` | `super` | `admin`, `super`, `user` |
| `etudiant-service` | `admin`, `super` | `super` | `admin`, `super`, `user` |
| `inscrption-service` | `admin`, `super` | `super` | `admin`, `super`, `user` |
| `paiement-service` | `admin`, `super` | — | `admin`, `super`, `user` |

---

### Résumé du flux de sécurité

```
1. Client → POST http://keycloak:8180/realms/scolarite/protocol/openid-connect/token
            (username, password, client_id, client_secret)
            ← access_token JWT signé RS256, iss=http://keycloak:8180/realms/scolarite

2. Client → https://localhost:8243/<api-context>/v1/...
            Authorization: Bearer <access_token>

3. WSO2 Gateway → vérifie signature via JWKS Keycloak (Self Validate JWT)
                → si valide, forward la requête avec Authorization: Bearer intact

4. Spring Boot → vérifie à nouveau le JWT via JWKS Keycloak
              → extrait les rôles depuis realm_access.roles
              → autorise ou rejette selon SecurityConfig

5. Spring Boot → traitement métier → réponse JSON
```

---

## Décisions techniques notables

| Décision | Justification |
|----------|---------------|
| Architecture hexagonale | Isolation totale du domaine métier, testabilité, indépendance des frameworks |
| Saga choreography | Pas de coordinateur central, couplage minimal entre services |
| Outbox Pattern | Garantie de publication des événements même en cas de crash (atomicité DB + Kafka) |
| Avro + Schema Registry | Contrat de message versionné et évolutif entre services |
| `etudiantNouveau` flag | Distingue les étudiants créés lors de l'inscription (rollbackables) des existants |
| Un seul moyen de paiement par versement | Simplicité du modèle, traçabilité claire, évite les conflits Hibernate |
| Distribution automatique des versements | Le client ne manipule pas les identifiants internes des lignes |
| Commentaire horodaté par versement | Historique immuable des paiements directement sur chaque ligne |
