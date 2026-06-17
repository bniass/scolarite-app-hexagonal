# ADR-006 — Règles du Transfert de Classe

**Date :** 2026  
**Statut :** Accepté  
**Décideurs :** Direction pédagogique + Équipe technique

---

## Contexte

Un étudiant inscrit peut demander à changer de classe en cours d'année. Il faut définir les conditions d'éligibilité, le traitement des paiements déjà effectués et le flux technique.

## Décision

### Règles métier (validées par la direction pédagogique)

1. **Seules les inscriptions `CONFIRMEE`** peuvent faire l'objet d'un transfert — un étudiant doit avoir déjà payé pour demander un changement
2. **Délai maximum : 3 mois** après la date de création de l'inscription (configurable : `inscription.transfert.delai-max-mois`)
3. **Niveau cible restreint : L1 ou M1 uniquement** — un transfert ne peut se faire qu'en début de cycle (Licence ou Master)
4. **Redistribution intégrale** : le total déjà payé est reporté sur le nouveau dossier, distribué dans l'ordre de priorité habituel (frais inscription → autres frais → mensualités)

### Flux technique (Saga)

```
POST /api/inscriptions/{id}/changer-classe
  → validation domaine (CONFIRMEE + délai + niveau L1/M1)
  → mise à jour inscription (nouvelle classeId, nouveaux tarifs)
  → publication inscription-transfere (Kafka)
  → paiement-service : supprime ancien dossier + crée nouveau + appliquerTransfert()
```

### Moyen de paiement `TRANSFERT`

Un nouveau `TypeMoyen.TRANSFERT` trace les versements issus d'un report de solde, distinct des versements directs (COMPTANT, MOBILE_MONEY, BANQUE).

### Cas limite — surplus

Si le total déjà payé dépasse le total dû dans la nouvelle classe (tarif moins cher), le surplus est plafonné au total dû. Le remboursement du surplus est traité manuellement par la comptabilité.

## Conséquences positives

- **Tracabilité** : l'historique des versements dans l'ancien dossier est supprimé mais le nouveau dossier retrace le report via le moyen `TRANSFERT`
- **Cohérence** : l'inscription reste CONFIRMEE — pas de re-confirmation nécessaire
- **Pas d'événement redondant** : `appliquerTransfert()` ne publie pas de `paiement-confirme` — évite une boucle de confirmation

## Conséquences négatives

- **Perte d'historique de l'ancien dossier** : les versements originaux (MOBILE_MONEY, BANQUE) sont supprimés avec l'ancien dossier — seul le montant total est reporté
- **Transfert de classe vers la même classe impossible** — non géré (le domaine ne le vérifie pas explicitement)

## Alternatives rejetées

- **Transfert pour inscriptions PENDING** : rejeté — un étudiant sans versement peut simplement se faire annuler et réinscrire
- **Transfert vers n'importe quel niveau** : rejeté par la direction — seuls L1 et M1 sont des points d'entrée de cycle
- **Conserver l'ancien dossier et créer un second dossier** : rejeté — deux dossiers actifs pour une même inscription est une incohérence comptable
