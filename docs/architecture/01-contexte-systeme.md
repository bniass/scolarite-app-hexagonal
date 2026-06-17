# 01 — Contexte système (C4 Level 1 & 2)

## Vue d'ensemble

La plateforme **Scolarite** est un système de gestion scolaire qui couvre le cycle de vie complet d'un étudiant :
inscription → paiement des frais → suivi comptable → transfert de classe.

Elle est construite comme un ensemble de **microservices autonomes**, communiquant via REST (synchrone) et Kafka (asynchrone), derrière un API Gateway WSO2.

---

## C4 — Niveau 1 : Contexte système

```mermaid
C4Context
    title Système Scolarite — Contexte

    Person(adminScolaire, "Admin Scolaire", "Gère les années académiques, classes, inscriptions")
    Person(comptable, "Comptable", "Saisit les versements, consulte le tableau de bord")
    Person(etudiant, "Étudiant / Parent", "Peut consulter son dossier via API Gateway")

    System(scolarite, "Plateforme Scolarite", "Gestion des inscriptions, paiements et référentiels scolaires")

    System_Ext(keycloak, "Keycloak", "Identity Provider — émission des tokens JWT")
    System_Ext(wso2, "WSO2 API Gateway", "Validation JWT, routage, rate limiting")
    System_Ext(mysql, "MySQL", "Bases de données (une par service)")
    System_Ext(kafka, "Apache Kafka", "Broker de messages pour la communication asynchrone")

    Rel(adminScolaire, wso2, "HTTPS / JWT")
    Rel(comptable, wso2, "HTTPS / JWT")
    Rel(etudiant, wso2, "HTTPS / JWT")
    Rel(wso2, scolarite, "HTTP (forward Bearer)")
    Rel(scolarite, mysql, "JDBC / JPA")
    Rel(scolarite, kafka, "Avro / Schema Registry")
    Rel(adminScolaire, keycloak, "Authentification (password flow)")
    Rel(comptable, keycloak, "Authentification (password flow)")
    Rel(wso2, keycloak, "Validation JWKS")
```

---

## C4 — Niveau 2 : Conteneurs

```mermaid
C4Container
    title Plateforme Scolarite — Conteneurs

    Person(adminScolaire, "Admin Scolaire")
    Person(comptable, "Comptable")

    System_Boundary(gw, "Couche d'accès") {
        Container(wso2, "WSO2 API Gateway", "Java / WSO2 AM 4.7", "Validation JWT, routage vers les microservices")
        Container(keycloak, "Keycloak 24", "Quarkus", "Identity Provider, émission JWT RS256, realm scolarite")
    }

    System_Boundary(core, "Microservices métier") {
        Container(annee, "annee-academique-service", "Spring Boot 3 / Java 21", "Années académiques, mois académiques — :8080")
        Container(school, "school-service", "Spring Boot 3 / Java 21", "Filières, classes, tarifs — :8094")
        Container(etudiant, "etudiant-service", "Spring Boot 3 / Java 21", "Référentiel étudiants — :8092")
        Container(inscription, "inscrption-service", "Spring Boot 3 / Java 21", "Inscriptions, transferts — :8091")
        Container(paiement, "paiement-service", "Spring Boot 3 / Java 21", "Dossiers paiement, versements — :8093")
    }

    System_Boundary(infra, "Infrastructure") {
        ContainerDb(dbAnnee, "inscription-annee-db", "MySQL 8", "annee-academique-service")
        ContainerDb(dbSchool, "school-service-db", "MySQL 8", "school-service")
        ContainerDb(dbEtudiant, "etudiant-service-db", "MySQL 8", "etudiant-service")
        ContainerDb(dbInsc, "inscription-service-db", "MySQL 8", "inscrption-service")
        ContainerDb(dbPaie, "paiement-service-db", "MySQL 8", "paiement-service")
        Container(kafka, "Kafka Cluster", "Apache Kafka 3.x (3 brokers)", "Topics Avro via Schema Registry")
        Container(registry, "Schema Registry", "Confluent", "Registre des schémas Avro")
    }

    Rel(adminScolaire, wso2, "HTTPS")
    Rel(comptable, wso2, "HTTPS")
    Rel(wso2, annee, "HTTP")
    Rel(wso2, school, "HTTP")
    Rel(wso2, etudiant, "HTTP")
    Rel(wso2, inscription, "HTTP")
    Rel(wso2, paiement, "HTTP")

    Rel(inscription, school, "REST / getTarifActif", "sync")
    Rel(inscription, etudiant, "REST / creerEtudiant, supprimerEtudiant", "sync")

    Rel(annee, kafka, "creer-annee-request")
    Rel(inscription, kafka, "inscription-creee, inscription-annulee, inscription-transfere")
    Rel(paiement, kafka, "paiement-confirme")

    Rel(kafka, inscription, "paiement-confirme, creer-annee-request")
    Rel(kafka, paiement, "inscription-creee, inscription-annulee, inscription-transfere")

    Rel(annee, dbAnnee, "JPA")
    Rel(school, dbSchool, "JPA")
    Rel(etudiant, dbEtudiant, "JPA")
    Rel(inscription, dbInsc, "JPA")
    Rel(paiement, dbPaie, "JPA")
    Rel(inscription, registry, "Avro")
    Rel(paiement, registry, "Avro")
```

---

## Principes directeurs

| Principe | Application |
|----------|-------------|
| **Isolation des données** | Chaque service possède sa propre base MySQL — aucune jointure inter-service |
| **Communication asynchrone préférée** | Kafka pour tout ce qui ne nécessite pas de réponse immédiate |
| **Communication synchrone minimale** | REST uniquement pour les lookups bloquants (tarif, création étudiant) |
| **Pas de couplage fort** | Aucun service n'importe les classes d'un autre service |
| **Sécurité périmétrique** | JWT validé à l'entrée (WSO2), re-validé dans chaque service Spring Security |
| **Observabilité** | Actuator + Prometheus + Grafana sur chaque service |

---

## Ports réseau

| Service | Port HTTP | Base de données |
|---------|-----------|----------------|
| `annee-academique-service` | 8080 | `annee-academique-service` |
| `inscrption-service` | 8091 | `inscription-service` |
| `etudiant-service` | 8092 | `etudiant-service` |
| `paiement-service` | 8093 | `paiement-service` |
| `school-service` | 8094 | `school-service` |
| Keycloak | 8180 | — |
| WSO2 Gateway HTTP | 8280 | — |
| WSO2 Gateway HTTPS | 8243 | — |
| Kafka broker 1 | 19092 | — |
| Schema Registry | 8081 | — |
| Grafana | 3001 | InfluxDB :8086 |
| Prometheus | 9090 | — |
