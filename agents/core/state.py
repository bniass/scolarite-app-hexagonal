"""
État partagé entre les agents du pipeline LangGraph.

Chaque nœud du graphe lit et écrit dans ce state.
"""

from typing import TypedDict, List, Optional


class FileOutput(TypedDict):
    """Un fichier généré par l'agent Coder."""
    path: str       # chemin relatif depuis la racine du projet
    content: str    # contenu Java complet


class AgentState(TypedDict):
    """État partagé tout au long du pipeline."""

    # ── Input (fourni au démarrage) ────────────────────────
    phase_name: str          # ex: "exceptions", "enums", ...
    phase_title: str         # ex: "Exceptions du domaine"
    phase_description: str
    prompt: str              # le prompt construit pour cette phase
    files_expected: int      # nombre de fichiers attendus

    # ── Sortie du Coder ────────────────────────────────────
    generated_files: List[FileOutput]

    # ── Sortie du Reviewer ─────────────────────────────────
    review_feedback: Optional[str]
    review_approved: bool

    # ── Contrôle du pipeline ───────────────────────────────
    iterations: int          # nombre de boucles coder → reviewer effectuées
    max_iterations: int      # limite (défaut 2)
    errors: List[str]        # erreurs accumulées
