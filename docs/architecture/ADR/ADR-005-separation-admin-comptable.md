# ADR-005 — Séparation des rôles Admin Scolaire / Comptable

**Date :** 2025  
**Statut :** Accepté  
**Décideurs :** Équipe métier + Équipe technique

---

## Contexte

Dans les établissements scolaires, les personnes qui gèrent les inscriptions (admin scolaire) et celles qui encaissent les paiements (comptable) sont distinctes. L'implémentation initiale mélangeait les deux rôles dans un seul endpoint.

## Décision

**Séparation stricte des responsabilités :**

- L'**Admin Scolaire** crée les inscriptions **sans saisir aucune information de paiement** (pas de montant, pas de moyen)
- Le **Comptable** effectue les versements séparément, sur le dossier créé automatiquement par Kafka

**Flux résultant :**

```
Admin → POST /api/inscriptions   (sans montant)
                  ↓ Kafka
Paiement → dossier initialisé automatiquement

Comptable → POST /api/dossiers/{inscriptionId}/versements/distribuer
```

**Endpoints distincts, sécurisés par rôle :**

| Endpoint | Rôle requis |
|----------|-------------|
| `POST /api/inscriptions` | `admin`, `super` |
| `POST /api/inscriptions/{id}/annuler` | `admin`, `super` |
| `POST /api/inscriptions/{id}/changer-classe` | `admin`, `super` |
| `POST /api/dossiers/{id}/versements/distribuer` | `admin`, `super` |
| `GET /api/dossiers/{id}` | `admin`, `super`, `user` |

## Conséquences positives

- **Modèle métier fidèle** : reflète exactement la réalité organisationnelle
- **Séparation des préoccupations** : `inscrption-service` ne traite que le cycle de vie de l'inscription, `paiement-service` traite le cycle financier
- **Audit** : les versements sont tracés séparément des inscriptions — on sait qui a inscrit et qui a encaissé
- **Prévention des erreurs** : l'admin ne peut pas accidentellement créer un dossier avec un mauvais montant

## Conséquences négatives

- **Flux en deux étapes** : l'admin doit attendre que le comptable valide avant que l'inscription soit CONFIRMEE
- **Latence Kafka** : le dossier de paiement n'est disponible qu'après la livraison du message Kafka (1-5 secondes en pratique)

## Alternatives rejetées

- **Création inscription + premier versement en un seul appel** : simplifie l'API mais mélange les responsabilités et empêche le workflow de validation séparé
