"""
Prompts par phase pour le module school-service.

Chaque phase a un prompt précis et borné — l'agent ne génère
QUE ce qui est demandé, pas plus.

Phases :
  1. exceptions    → SchoolException, SchoolNotFoundException
  2. enums         → Cycle, Niveau
  3. valueobjects  → IDs (FiliereId, ClasseId, TarifId, ClasseTarifId)
                   + montants (Mensualite, FraisInscription, AutresFrais)
  4. aggregates    → Filiere, Classe, Tarif, ClasseTarif
  5. events        → FiliereCreeEvent, ClasseCreeEvent, TarifCreeEvent, etc.
"""

MODULE_NAME = "school-service"
BASE_PACKAGE = "com.ecole221.school.service"
BASE_PATH = "school-service/src/main/java/com/ecole221/school/service"


PHASES = {
    # ─────────────────────────────────────────────────────────
    "exceptions": {
        "title": "Exceptions du domaine",
        "description": "Exceptions métier de school-service",
        "files_expected": 2,
        "prompt": f"""
Génère les exceptions du domaine pour le module {MODULE_NAME}.

PACKAGE : {BASE_PACKAGE}.domain.exception
DOSSIER : {BASE_PATH}/domain/exception/

FICHIERS À GÉNÉRER :

1. SchoolException.java
   - Étend com.ecole221.common.exception.DomainException
   - Constructeur prenant un String message
   - Constructeur prenant un String message + Throwable cause

2. SchoolNotFoundException.java
   - Étend SchoolException
   - Constructeur prenant un String message
"""
    },

    # ─────────────────────────────────────────────────────────
    "enums": {
        "title": "Enums du domaine",
        "description": "Cycle (LICENCE, MASTER) et Niveau (L1, L2, L3, M1, M2)",
        "files_expected": 2,
        "prompt": f"""
Génère les enums du domaine pour le module {MODULE_NAME}.

PACKAGE : {BASE_PACKAGE}.domain.enums
DOSSIER : {BASE_PATH}/domain/enums/

FICHIERS À GÉNÉRER :

1. Cycle.java
   - Enum simple avec valeurs : LICENCE, MASTER

2. Niveau.java
   - Enum avec valeurs : L1, L2, L3, M1, M2
   - Chaque valeur porte :
       * un libellé (String) : "Licence 1", "Licence 2", ...
       * un Cycle associé    : L1/L2/L3 → LICENCE, M1/M2 → MASTER
   - Méthode publique : boolean appartientAuCycle(Cycle cycle)
   - Utiliser Lombok @Getter et @RequiredArgsConstructor
"""
    },

    # ─────────────────────────────────────────────────────────
    "valueobjects": {
        "title": "Value Objects du domaine",
        "description": "IDs (étendent BaseId<UUID>) + montants validés",
        "files_expected": 7,
        "prompt": f"""
Génère les Value Objects du domaine pour le module {MODULE_NAME}.

PACKAGE : {BASE_PACKAGE}.domain.valueobject
DOSSIER : {BASE_PATH}/domain/valueobject/

FICHIERS À GÉNÉRER :

IDS (étendent com.ecole221.common.valueobject.BaseId<UUID>) :
1. FiliereId.java       → constructeur public FiliereId(UUID value) {{ super(value); }}
2. ClasseId.java
3. TarifId.java
4. ClasseTarifId.java

MONTANTS (records Java avec validation) :
Chaque record valide dans son constructeur compact que le montant
n'est pas null et est strictement positif (> 0), sinon lance
{BASE_PACKAGE}.domain.exception.SchoolException.

5. Mensualite.java
   - record Mensualite(BigDecimal montant)
   - Validation : montant != null && montant > 0
   - Message d'erreur : "La mensualité doit être strictement positive"

6. FraisInscription.java
   - record FraisInscription(BigDecimal montant)
   - Validation : montant != null && montant > 0
   - Message d'erreur : "Les frais d'inscription doivent être strictement positifs"

7. AutresFrais.java
   - record AutresFrais(BigDecimal montant)
   - Validation : montant != null && montant >= 0 (peut être 0)
   - Message d'erreur : "Les autres frais ne peuvent pas être négatifs"
""",
    },

    # ─────────────────────────────────────────────────────────
    "events": {
        "title": "Domain Events",
        "description": "Events publiés par les agrégats du domaine",
        "files_expected": 7,
        "prompt": f"""
Génère les Domain Events pour le module {MODULE_NAME}.

PACKAGE : {BASE_PACKAGE}.domain.event
DOSSIER : {BASE_PATH}/domain/event/

CONTEXTE :
Tous les events implémentent com.ecole221.common.event.DomainEvent<T>
où T est le type de l'agrégat source.

FICHIERS À GÉNÉRER (un par event) :

Sur Filiere :
1. FiliereCreeEvent     (record contenant Filiere filiere + Instant occurredAt)
2. FiliereModifieeEvent

Sur Classe :
3. ClasseCreeEvent
4. ClasseModifieeEvent

Sur Tarif :
5. TarifCreeEvent
6. TarifArchiveEvent

Sur ClasseTarif :
7. TarifRattacheAClasseEvent (porte le ClasseTarif nouvellement créé)

CHAQUE EVENT :
- Est un record Java
- Implémente DomainEvent<T> du module common-service
- Méthode getOccurredAt() retourne un Instant
- Méthode getSource() retourne l'agrégat source
- Import les classes du domaine : com.ecole221.school.service.domain.model.*
""",
    },

    # ─────────────────────────────────────────────────────────
    "aggregates": {
        "title": "Agrégats du domaine",
        "description": "Filiere, Classe, Tarif, ClasseTarif avec règles métier",
        "files_expected": 4,
        "prompt": f"""
Génère les agrégats du domaine pour le module {MODULE_NAME}.

PACKAGE : {BASE_PACKAGE}.domain.model
DOSSIER : {BASE_PATH}/domain/model/

CONTEXTE :
Tous les agrégats étendent com.ecole221.common.entity.AggregateRoot<XxxId>.
Lombok autorisé : @Getter, @Builder.
PAS d'annotations Spring ni JPA dans ces classes.

═══════════════════════════════════════════════════════════
FICHIER 1 : Filiere.java
═══════════════════════════════════════════════════════════

Champs :
- String nomFiliere
- String codeFiliere
- Cycle cycle
- boolean actif

Méthodes :
- static Filiere creer(FiliereId id, String nom, String code, Cycle cycle)
  * Valide : nom non blank, code non blank, code <= 10 caractères
  * codeFiliere stocké en uppercase, trimmed
  * actif = true par défaut
  * Ajoute FiliereCreeEvent
  * Lance SchoolException si validation échoue
- void modifier(String nom, String code, Cycle cycle)
  * Mêmes validations
  * Ajoute FiliereModifieeEvent
- void desactiver() → actif = false

═══════════════════════════════════════════════════════════
FICHIER 2 : Tarif.java
═══════════════════════════════════════════════════════════

Champs :
- String libelle
- Mensualite mensualite
- FraisInscription fraisInscription
- AutresFrais autresFrais
- boolean archive

Méthodes :
- static Tarif creer(TarifId id, String libelle, Mensualite m, FraisInscription fi, AutresFrais af)
  * Valide : libelle non blank, tous les VO non null
  * archive = false par défaut
  * Ajoute TarifCreeEvent
- void archiver()
  * Lance SchoolException si déjà archivé
  * archive = true
  * Ajoute TarifArchiveEvent
- void validerPourRattachement()
  * Lance SchoolException si archive == true

═══════════════════════════════════════════════════════════
FICHIER 3 : Classe.java
═══════════════════════════════════════════════════════════

Champs :
- String nomClasse
- Niveau niveau
- FiliereId filiereId  (référence par ID, pas l'objet)

Méthodes :
- static Classe creer(ClasseId id, String nom, Niveau niveau, FiliereId filiereId)
  * Valide : nom non blank, niveau non null, filiereId non null
  * Ajoute ClasseCreeEvent
- void modifier(String nom, Niveau niveau)
  * Ajoute ClasseModifieeEvent
- void notifierRattachementTarif(ClasseTarif ct)
  * Ajoute TarifRattacheAClasseEvent

═══════════════════════════════════════════════════════════
FICHIER 4 : ClasseTarif.java
═══════════════════════════════════════════════════════════

Champs :
- ClasseId classeId
- TarifId tarifId
- LocalDate dateDebut
- LocalDate dateFin  (null tant qu'actif)
- boolean actif

Méthodes :
- static ClasseTarif creer(ClasseTarifId id, ClasseId classeId, TarifId tarifId)
  * dateDebut = LocalDate.now()
  * dateFin = null
  * actif = true
- void fermer()
  * actif = false
  * dateFin = LocalDate.now()
""",
    },
}


def get_phase_prompt(phase_name: str) -> dict:
    """Retourne la config d'une phase."""
    if phase_name not in PHASES:
        raise ValueError(
            f"Phase inconnue : {phase_name}. "
            f"Phases disponibles : {list(PHASES.keys())}"
        )
    return PHASES[phase_name]
