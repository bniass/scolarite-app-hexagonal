# Agents IA — Génération automatique de school-service

Pipeline d'agents Python qui génère le code Java du module `school-service`
en respectant strictement l'architecture hexagonale du projet.

## Architecture du pipeline

```
   ┌──────────┐     ┌──────────┐     ┌──────────┐
   │  CODER   │ ──→ │ REVIEWER │ ──→ │  WRITER  │ → END
   └──────────┘     └────┬─────┘     └──────────┘
         ↑               │ KO (max 2 corrections)
         └───────────────┘
```

- **Coder** : génère le code Java à partir d'un prompt précis
- **Reviewer** : vérifie l'architecture hexagonale (annotations Spring interdites dans `domain/`, etc.)
- **Writer** : écrit les fichiers validés sur disque dans `school-service/`

## Phases

Le code de `school-service/domain/` est généré en 5 phases ordonnées :

| Ordre | Phase          | Contenu                                                        |
|------:|----------------|----------------------------------------------------------------|
| 1     | `exceptions`   | `SchoolException`, `SchoolNotFoundException`                   |
| 2     | `enums`        | `Cycle` (LICENCE, MASTER), `Niveau` (L1, L2, L3, M1, M2)       |
| 3     | `valueobjects` | IDs + `Mensualite`, `FraisInscription`, `AutresFrais`          |
| 4     | `events`       | `FiliereCreeEvent`, `TarifCreeEvent`, etc.                     |
| 5     | `aggregates`   | `Filiere`, `Classe`, `Tarif`, `ClasseTarif`                    |

## Installation

### 1. Créer l'environnement virtuel

Dans le terminal IntelliJ, **depuis la racine du projet** :

```bash
cd agents
/opt/homebrew/bin/python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### 2. Configurer le `.env`

```bash
cp .env.example .env
```

Édite `agents/.env` et mets ta clé API Anthropic :

```
ANTHROPIC_API_KEY=sk-ant-xxxxxxxxxxxx
PROJECT_ROOT=/Users/macbookpro/IdeaProjects/scolarite-app-hexagonal
```

## Lancer le pipeline

### Depuis le terminal

```bash
cd agents
source venv/bin/activate

# Lister les phases disponibles
python main.py --list

# Lancer une seule phase (recommandé pour apprendre)
python main.py --phase exceptions
python main.py --phase enums
python main.py --phase valueobjects
python main.py --phase events
python main.py --phase aggregates

# Toutes les phases d'un coup
python main.py
```

### Depuis IntelliJ (Run Configurations)

1. **Installer le plugin Python** : `Preferences → Plugins → Marketplace → "Python"`
2. **Ajouter un module Python** : `File → Project Structure → Modules → + → Python` pointant sur `agents/`
3. **Configurer l'interpréteur** : `Project Structure → SDKs → +` choisir `agents/venv/bin/python`
4. **Créer les Run Configurations** :
   - `Run → Edit Configurations → + → Python`
   - Nom : `Agent — Exceptions`
   - Script : `agents/main.py`
   - Parameters : `--phase exceptions`
   - Working directory : `agents/`
   - Variables d'environnement : déjà chargées depuis `.env`
5. Répéter pour chaque phase

## Structure du dossier

```
agents/
├── .env.example                  → modèle à committer
├── .env                          → vraies valeurs (ignoré par git)
├── .gitignore
├── requirements.txt
├── README.md
├── main.py                       → point d'entrée CLI
│
├── config/
│   └── project_context.py        → contexte projet partagé entre agents
│
├── prompts/
│   └── school_domain_prompts.py  → prompts par phase
│
└── core/
    ├── state.py                  → AgentState (LangGraph)
    ├── agents.py                 → coder, reviewer, writer
    └── graph.py                  → assemblage du pipeline
```

## Ajouter une nouvelle phase

1. Ajoute une entrée dans le dict `PHASES` de `prompts/school_domain_prompts.py`
2. Ajoute la phase à `DEFAULT_ORDER` dans `main.py` à la bonne position
3. Lance : `python main.py --phase <ton_nom>`

Les agents (`coder`, `reviewer`, `writer`) sont génériques — ils n'ont pas besoin
d'être modifiés. C'est le prompt qui pilote l'agent.
