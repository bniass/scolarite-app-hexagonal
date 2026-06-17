# 05 — Sagas (Choreography)

Toutes les sagas utilisent la **chorégraphie** : pas de coordinateur central. Chaque service réagit aux événements Kafka et publie le suivant. L'Outbox Pattern garantit l'atomicité locale.

---

## SAGA 1 — Création d'inscription

### Flux nominal

```mermaid
sequenceDiagram
    actor Admin
    participant API as inscrption-service
    participant School as school-service
    participant Etudiant as etudiant-service
    participant DB_I as inscription DB
    participant Kafka
    participant Paiement as paiement-service
    participant DB_P as paiement DB

    Admin->>API: POST /api/inscriptions
    API->>School: GET /api/classes/{classeId}/tarif-actif
    School-->>API: TarifActifResult (frais, mensualite, autresFrais, niveau)

    alt Nouvel étudiant
        API->>Etudiant: POST /api/etudiants
        Etudiant-->>API: etudiantId
    end

    API->>DB_I: INSERT inscription (PENDING) + INSERT outbox_event
    Note over DB_I: Transaction atomique

    API-->>Admin: 201 Created {inscriptionId}

    DB_I-->>Kafka: inscription-creee (Outbox scheduler)

    Kafka->>Paiement: inscription-creee
    Paiement->>DB_P: INSERT dossier_paiement + lignes
    DB_P-->>Kafka: paiement-confirme{CONFIRME} (Outbox)

    Kafka->>API: paiement-confirme{CONFIRME}
    API->>DB_I: UPDATE inscription SET statut=CONFIRMEE
```

### Flux de compensation (échec paiement-service)

```mermaid
sequenceDiagram
    participant Kafka
    participant Paiement as paiement-service
    participant DB_P as paiement DB
    participant API as inscrption-service
    participant Etudiant as etudiant-service
    participant DB_I as inscription DB

    Kafka->>Paiement: inscription-creee
    Paiement->>DB_P: ✗ Erreur initialisation dossier
    DB_P-->>Kafka: paiement-confirme{ECHEC} (Outbox)

    Kafka->>API: paiement-confirme{ECHEC}
    API->>DB_I: UPDATE inscription SET statut=ECHOUEE
    API->>DB_I: DELETE inscription

    alt etudiantNouveau = true
        API->>Etudiant: DELETE /api/etudiants/id/{etudiantId}
    end
    Note over API: Étudiant existant → conservé
```

---

## SAGA 2 — Premier versement (confirmation)

```mermaid
sequenceDiagram
    actor Comptable
    participant API as paiement-service
    participant DB_P as paiement DB
    participant Kafka
    participant Insc as inscrption-service
    participant DB_I as inscription DB

    Comptable->>API: POST /api/dossiers/{inscriptionId}/versements/distribuer
    Note over API: Vérifie minimum : fraisInscription + autresFrais + mensualite

    API->>DB_P: UPDATE dossier (INITIALISE→ACTIF)<br/>INSERT versements + outbox_event
    Note over DB_P: Transaction atomique

    API-->>Comptable: 200 OK (DossierPaiementResponse)

    DB_P-->>Kafka: paiement-confirme{CONFIRME}

    Kafka->>Insc: paiement-confirme{CONFIRME}
    Insc->>DB_I: UPDATE inscription SET statut=CONFIRMEE
```

---

## SAGA 3 — Annulation administrative

```mermaid
sequenceDiagram
    actor Admin
    participant API as inscrption-service
    participant DB_I as inscription DB
    participant Kafka
    participant Paiement as paiement-service
    participant DB_P as paiement DB

    Admin->>API: POST /api/inscriptions/{id}/annuler
    Note over API: Vérifie : statut == PENDING<br/>(CONFIRMEE → rejet)

    API->>DB_I: UPDATE inscription SET statut=ANNULEE<br/>INSERT outbox_event (InscriptionAnnuleeEvent)
    Note over DB_I: Transaction atomique — soft delete

    API-->>Admin: 204 No Content

    DB_I-->>Kafka: inscription-annulee

    Kafka->>Paiement: inscription-annulee
    Paiement->>DB_P: DELETE dossier_paiement (cascade lignes+versements+moyens)
    Note over DB_P: Suppression physique
    Note over Paiement: Étudiant et inscription conservés
```

**Garanties :**
- L'inscription reste visible en base avec statut `ANNULEE` et `motifAnnulation`
- Si le dossier n'existe pas encore (Kafka lent), `deleteByInscriptionId` est no-op
- Si le message est rejoué (at-least-once), la suppression est idempotente

---

## SAGA 4 — Transfert de classe

```mermaid
sequenceDiagram
    actor Admin
    participant API as inscrption-service
    participant School as school-service
    participant DB_I as inscription DB
    participant Kafka
    participant Paiement as paiement-service
    participant DB_P as paiement DB

    Admin->>API: POST /api/inscriptions/{id}/changer-classe\n{nouvelleClasseId}

    API->>School: GET /api/classes/{nouvelleClasseId}/tarif-actif
    School-->>API: TarifActifResult (avec niveau)

    Note over API: Vérifie domaine :<br/>① statut == CONFIRMEE<br/>② creeLe + 3 mois > now<br/>③ niveau ∈ {L1, M1}

    API->>DB_I: UPDATE inscription (classeId, tarifs)<br/>INSERT outbox_event (InscriptionTransfereEvent)
    Note over DB_I: Transaction atomique

    API-->>Admin: 200 OK (InscriptionResponse mis à jour)

    DB_I-->>Kafka: inscription-transfere

    Kafka->>Paiement: inscription-transfere
    Note over Paiement: 1. Lire totalPayé ancien dossier
    Paiement->>DB_P: SELECT dossier WHERE inscription_id = ?
    DB_P-->>Paiement: totalPayé = Σ(montantPaye)
    Paiement->>DB_P: DELETE ancien dossier (cascade)
    Paiement->>DB_P: INSERT nouveau dossier (tarifs nouvelle classe)
    Note over Paiement: 2. Redistribution intégrale
    Paiement->>DB_P: appliquerTransfert(totalPayé, TRANSFERT)
    Note over Paiement: INITIALISE→ACTIF sans DossierInitialiseEvent\n(inscription déjà CONFIRMEE)
```

**Règles métier :**

| Condition | Comportement |
|-----------|-------------|
| totalPayé > totalDû nouvelle classe | Plafonné au totalDû — surplus non remboursé automatiquement |
| totalPayé = 0 | Impossible (l'inscription est CONFIRMEE = au moins 1 versement) |
| Kafka rejoue le message | L'ancien dossier n'existe plus → `deleteByInscriptionId` no-op → nouveau dossier recréé |

---

## Garanties transactionnelles

| Propriété | Mécanisme |
|-----------|-----------|
| **Atomicité locale** | Chaque service modifie sa DB + l'Outbox dans une seule transaction |
| **At-least-once delivery** | Kafka peut re-livrer — chaque consumer gère l'idempotence |
| **Pas de transactions distribuées** | 2PC interdit — compensation saga en cas d'échec |
| **Ordre des messages** | Partitionnement par `inscriptionId` → ordre garanti pour un agrégat donné |

---

## États agrégés des sagas

```mermaid
stateDiagram-v2
    [*] --> PENDING : POST /inscriptions
    PENDING --> CONFIRMEE : paiement-confirme{CONFIRME}
    PENDING --> ECHOUEE : paiement-confirme{ECHEC}
    PENDING --> ANNULEE : POST /annuler
    CONFIRMEE --> CONFIRMEE : transfert classe\n(inscription.classeId mis à jour)
    ECHOUEE --> [*] : supprimée physiquement
    ANNULEE --> [*] : soft delete conservé

    note right of PENDING
        Dossier paiement : INITIALISE
    end note
    note right of CONFIRMEE
        Dossier paiement : ACTIF ou CLOTURE
    end note
    note right of ANNULEE
        Dossier paiement : supprimé
    end note
```
