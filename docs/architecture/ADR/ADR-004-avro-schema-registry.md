# ADR-004 — Avro + Schema Registry

**Date :** 2025  
**Statut :** Accepté  
**Décideurs :** Équipe technique

---

## Contexte

Les microservices échangent des événements via Kafka. Il faut choisir un format de sérialisation qui garantisse la compatibilité des messages dans le temps, tout en étant compact et performant.

## Décision

Utilisation d'**Apache Avro** avec le **Confluent Schema Registry** :

- Les schémas sont définis dans `common-avro/src/main/resources/avro/*.avsc`
- Le plugin `avro-maven-plugin` génère les classes Java à la compilation
- Le Schema Registry stocke les versions de schémas et valide la compatibilité
- Les producers enregistrent le schéma, les consumers le téléchargent au démarrage

**Module `common-avro` :**

```
common-avro/
└── src/main/resources/avro/
    ├── inscriptionCreeeAvro.avsc
    ├── inscriptionAnnuleeAvro.avsc
    ├── inscriptionTransfereAvro.avsc
    ├── paiementConfirmeAvro.avsc
    ├── etudiantCreeAvro.avsc
    └── ...
```

**Ordre de build obligatoire :** `mvn install -pl common-avro` avant de compiler les services.

## Conséquences positives

- **Contrat explicite** : le schéma `.avsc` est le contrat de communication — tout changement incompatible est détecté à la compilation
- **Compacité** : Avro est binaire — plus compact que JSON
- **Évolution** : compatibilité `BACKWARD` par défaut — ajout de champs optionnels sans casser les consumers

## Conséquences négatives

- **Couplage sur `common-avro`** : tous les services doivent importer ce module — un changement de schéma nécessite de re-builder tous les services
- **Debugging plus difficile** : les messages Kafka sont binaires — utiliser Confluent Control Center ou `kafka-avro-console-consumer` pour les lire
- **Schema Registry est un SPOF** : si indisponible, les producers ne peuvent pas sérialiser

## Alternatives rejetées

- **JSON** : pas de contrat strict, pas de validation automatique, plus volumineux
- **Protobuf** : alternative valable mais ecosystem moins intégré avec Confluent/Spring
- **CloudEvents + JSON** : bonne standardisation mais overhead de taille et pas de validation de schéma native
