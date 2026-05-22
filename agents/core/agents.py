"""
Les trois agents du pipeline :

  CODER    → génère le code Java
  REVIEWER → vérifie l'architecture hexagonale
  WRITER   → écrit les fichiers sur disque

Chaque agent est une fonction qui prend un AgentState et retourne un AgentState
mis à jour. C'est la signature attendue par LangGraph.
"""

import json
import os
import re
from pathlib import Path

from langchain_anthropic import ChatAnthropic
from langchain_core.messages import HumanMessage, SystemMessage
from rich.console import Console

from config.project_context import PROJECT_CONTEXT
from core.state import AgentState, FileOutput

console = Console()

# Modèle Claude utilisé — surchargeable via la variable d'env CLAUDE_MODEL
_MODEL = os.getenv("CLAUDE_MODEL", "claude-sonnet-4-5-20250929")

llm = ChatAnthropic(model=_MODEL, max_tokens=8000)


# ═══════════════════════════════════════════════════════════
# Utilitaires
# ═══════════════════════════════════════════════════════════

def _extract_json(text: str) -> dict:
    """
    Extrait le JSON d'une réponse de Claude.

    Claude renvoie souvent du JSON entouré de ```json ... ```
    ou avec du texte avant/après. On nettoie.
    """
    # Cas 1 : bloc ```json
    match = re.search(r"```json\s*(.*?)\s*```", text, re.DOTALL)
    if match:
        return json.loads(match.group(1))

    # Cas 2 : bloc ``` simple
    match = re.search(r"```\s*(.*?)\s*```", text, re.DOTALL)
    if match:
        return json.loads(match.group(1))

    # Cas 3 : JSON brut — on cherche le premier { et le dernier }
    start = text.find("{")
    end = text.rfind("}")
    if start != -1 and end != -1:
        return json.loads(text[start:end + 1])

    raise ValueError(f"Impossible d'extraire du JSON de la réponse :\n{text[:500]}")


# ═══════════════════════════════════════════════════════════
# AGENT 1 : CODER
# ═══════════════════════════════════════════════════════════

def coder_agent(state: AgentState) -> dict:
    """Génère le code Java pour la phase courante."""

    console.print(f"[bold cyan]💻 Coder[/]    → génère le code pour [bold]{state['phase_title']}[/]")

    # Si on est dans une boucle de correction, on injecte le feedback
    correction_context = ""
    if state["iterations"] > 0 and state.get("review_feedback"):
        correction_context = (
            "\n\n═══════════════════════════════════════════════════════════\n"
            "CORRECTIONS À APPORTER (review précédente) :\n"
            f"{state['review_feedback']}\n"
            "═══════════════════════════════════════════════════════════\n"
        )

    system_msg = PROJECT_CONTEXT + """

Tu es un développeur Spring Boot Senior expert en architecture hexagonale.
Tu génères du code Java 21 COMPLET et FONCTIONNEL.

RÈGLES ABSOLUES :
- Pas d'annotations Spring dans le domain/
- Utiliser Lombok quand c'est demandé (@Getter, @Builder, @RequiredArgsConstructor)
- Les Value Objects de montants sont des records Java avec validation
- Les IDs étendent BaseId<UUID>
- Code complet, fonctionnel, prêt à compiler
- Tous les imports nécessaires en haut de chaque fichier
- Package déclaré en première ligne

FORMAT DE RÉPONSE OBLIGATOIRE — UNIQUEMENT du JSON :
{
  "files": [
    {
      "path": "chemin/relatif/depuis/racine/projet/Fichier.java",
      "content": "package com.ecole221...;\\n\\n// code Java complet ici"
    }
  ]
}

Pas de texte avant ou après. Juste le JSON.
"""

    response = llm.invoke([
        SystemMessage(content=system_msg),
        HumanMessage(content=state["prompt"] + correction_context),
    ])

    try:
        data = _extract_json(response.content)
        files: list[FileOutput] = data.get("files", [])
        console.print(f"            → {len(files)} fichier(s) généré(s)")
        return {"generated_files": files}
    except Exception as e:
        error = f"Coder parse error : {e}"
        console.print(f"[red]            ✗ {error}[/]")
        return {
            "generated_files": [],
            "errors": state["errors"] + [error],
        }


# ═══════════════════════════════════════════════════════════
# AGENT 2 : REVIEWER
# ═══════════════════════════════════════════════════════════

def reviewer_agent(state: AgentState) -> dict:
    """Review le code pour vérifier l'architecture hexagonale."""

    console.print(f"[bold yellow]🔍 Reviewer[/] → vérifie la qualité du code")

    if not state["generated_files"]:
        console.print("[red]            ✗ Aucun fichier à reviewer[/]")
        return {
            "review_approved": False,
            "review_feedback": "Aucun fichier généré par le coder.",
            "iterations": state["iterations"] + 1,
        }

    # On envoie tous les fichiers au reviewer
    files_dump = "\n\n".join(
        f"=== {f['path']} ===\n{f['content']}"
        for f in state["generated_files"]
    )

    system_msg = PROJECT_CONTEXT + """

Tu es un expert en architecture hexagonale et code review Spring Boot.
Tu vérifies STRICTEMENT le code généré selon ces critères :

CRITÈRES BLOQUANTS :
1. Aucune annotation Spring (@Service, @Component, @Autowired, @Repository, etc.) dans le domain/
2. Aucune annotation JPA (@Entity, @Column, etc.) dans le domain/
3. Les IDs étendent bien BaseId<UUID>
4. Les imports sont corrects et présents
5. Les packages déclarés correspondent au chemin du fichier
6. Le code compile (syntaxe Java 21 correcte)
7. Les Value Objects valident leurs invariants dans le constructeur

CRITÈRES SOUPLES (à signaler mais non bloquants) :
- Nommage cohérent
- Lombok correctement utilisé
- Pas de logique métier mal placée

FORMAT DE RÉPONSE OBLIGATOIRE — UNIQUEMENT du JSON :
{
  "approved": true | false,
  "score": 0-100,
  "violations": ["violation bloquante 1", "violation bloquante 2"],
  "suggestions": ["suggestion 1", "suggestion 2"]
}

approved = true si AUCUNE violation bloquante.
score >= 85 → considéré comme acceptable.
"""

    human_msg = (
        f"Phase reviewée : {state['phase_title']}\n"
        f"Fichiers attendus : {state['files_expected']}\n"
        f"Fichiers générés  : {len(state['generated_files'])}\n\n"
        f"CODE À REVIEWER :\n\n{files_dump}"
    )

    response = llm.invoke([
        SystemMessage(content=system_msg),
        HumanMessage(content=human_msg),
    ])

    try:
        review = _extract_json(response.content)
        approved = bool(review.get("approved", False))
        score = review.get("score", 0)

        # On accepte si approved=true OU score >= 85
        if not approved and score >= 85:
            approved = True

        feedback = json.dumps(review, indent=2, ensure_ascii=False)

        status = "[green]✓ approuvé[/]" if approved else "[red]✗ corrections nécessaires[/]"
        console.print(f"            → score {score}/100, {status}")

        return {
            "review_approved": approved,
            "review_feedback": feedback,
            "iterations": state["iterations"] + 1,
        }
    except Exception as e:
        error = f"Reviewer parse error : {e}"
        console.print(f"[yellow]            ⚠ {error} — on continue quand même[/]")
        # En cas d'erreur de parsing, on laisse passer
        return {
            "review_approved": True,
            "review_feedback": response.content,
            "iterations": state["iterations"] + 1,
            "errors": state["errors"] + [error],
        }


# ═══════════════════════════════════════════════════════════
# AGENT 3 : WRITER
# ═══════════════════════════════════════════════════════════

def writer_agent(state: AgentState) -> dict:
    """Écrit les fichiers générés sur disque."""

    console.print(f"[bold green]✍️  Writer[/]   → écrit les fichiers sur disque")

    project_root = os.getenv("PROJECT_ROOT")
    if not project_root:
        error = "Variable d'environnement PROJECT_ROOT non définie"
        console.print(f"[red]            ✗ {error}[/]")
        return {"errors": state["errors"] + [error]}

    root = Path(project_root)
    if not root.is_dir():
        error = f"PROJECT_ROOT n'existe pas : {project_root}"
        console.print(f"[red]            ✗ {error}[/]")
        return {"errors": state["errors"] + [error]}

    written = 0
    for f in state["generated_files"]:
        full_path = root / f["path"]
        full_path.parent.mkdir(parents=True, exist_ok=True)
        full_path.write_text(f["content"], encoding="utf-8")
        console.print(f"            [green]✓[/] {f['path']}")
        written += 1

    console.print(f"            → [bold]{written}[/] fichier(s) écrit(s)")
    return {}
