# ADR-003 — Outbox Pattern

**Date :** 2025  
**Statut :** Accepté  
**Décideurs :** Équipe technique

---

## Contexte

Un service qui modifie sa base de données ET publie sur Kafka risque l'incohérence si le process crashe entre les deux opérations :
- **Cas 1** : DB commité, Kafka non publié → événement perdu
- **Cas 2** : Kafka publié, DB rollbackée → double publication sans correspondance

## Décision

Utilisation du **Transactional Outbox Pattern** :

1. La modification métier et l'insertion en table `outbox_event` sont dans **la même transaction DB**
2. Un scheduler `@Scheduled(fixedDelay=5000)` lit les événements `PENDING`/`FAILED` et les publie sur Kafka
3. Après publication réussie, le statut passe à `PUBLISHED`

```
Transaction atomique :
  INSERT inscription (PENDING)
  INSERT outbox_event (payload=InscriptionCreeeAvroModel, status=PENDING)

Scheduler (toutes les 5s) :
  SELECT * FROM outbox_event WHERE status IN ('PENDING','FAILED')
  → publish Kafka
  → UPDATE status = PUBLISHED
```

## Conséquences positives

- **At-least-once** : si le scheduler crashe après Kafka mais avant `UPDATE`, le message sera republié au prochain tick — les consumers doivent être idempotents
- **Atomicité locale** garantie : impossible d'avoir la DB mise à jour sans l'événement correspondant
- **Rejoue** : les messages `FAILED` sont réessayés automatiquement

## Conséquences négatives

- **Latence** : publication différée de 0 à 5 secondes
- **Table supplémentaire** par service
- **Idempotence obligatoire** côté consommateurs

## Alternatives rejetées

- **Publication directe dans le use case** : risque de perte d'événement en cas de crash entre DB commit et Kafka send
- **CDC (Change Data Capture) avec Debezium** : plus robuste mais overhead d'infrastructure (Debezium + connecteur MySQL) — à envisager si la latence 5s devient un problème
