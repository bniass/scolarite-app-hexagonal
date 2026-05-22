"""
Point d'entrée du pipeline d'agents.

Usage :
  python main.py                          # toutes les phases dans l'ordre
  python main.py --phase exceptions       # une seule phase
  python main.py --list                   # liste les phases disponibles
"""

import argparse
import sys
from pathlib import Path

# Charge .env AVANT tout import qui utilise les variables
from dotenv import load_dotenv
load_dotenv()

from rich.console import Console
from rich.panel import Panel
from rich.table import Table

# Ajoute le dossier courant au PYTHONPATH pour les imports relatifs
sys.path.insert(0, str(Path(__file__).parent))

from core.graph import build_graph
from core.state import AgentState
from prompts.school_domain_prompts import PHASES, get_phase_prompt

console = Console()

# Ordre d'exécution recommandé (dépendances)
DEFAULT_ORDER = ["exceptions", "enums", "valueobjects", "events", "aggregates"]


def run_phase(phase_name: str) -> bool:
    """Lance le pipeline pour une phase. Retourne True si succès."""

    cfg = get_phase_prompt(phase_name)

    console.print()
    console.print(Panel(
        f"[bold]{cfg['title']}[/]\n[dim]{cfg['description']}[/]",
        title=f"🚀 Phase : {phase_name}",
        border_style="cyan",
    ))

    initial_state: AgentState = {
        "phase_name": phase_name,
        "phase_title": cfg["title"],
        "phase_description": cfg["description"],
        "prompt": cfg["prompt"],
        "files_expected": cfg["files_expected"],
        "generated_files": [],
        "review_feedback": None,
        "review_approved": False,
        "iterations": 0,
        "max_iterations": 2,
        "errors": [],
    }

    app = build_graph()
    final = app.invoke(initial_state)

    # Récap
    if final.get("errors"):
        console.print(f"[red]⚠ Erreurs : {final['errors']}[/]")
        return False

    console.print(f"[bold green]✓[/] Phase [bold]{phase_name}[/] terminée")
    return True


def list_phases():
    """Affiche les phases disponibles."""
    table = Table(title="Phases disponibles")
    table.add_column("Ordre", style="dim")
    table.add_column("Phase", style="cyan bold")
    table.add_column("Titre", style="white")
    table.add_column("Fichiers", justify="right", style="green")

    for i, name in enumerate(DEFAULT_ORDER, 1):
        cfg = PHASES[name]
        table.add_row(
            str(i),
            name,
            cfg["title"],
            str(cfg["files_expected"]),
        )
    console.print(table)


def main():
    parser = argparse.ArgumentParser(
        description="Pipeline d'agents IA pour générer le code de school-service",
    )
    parser.add_argument(
        "--phase",
        choices=list(PHASES.keys()),
        help="Lance une seule phase. Sans cet argument, toutes les phases sont exécutées dans l'ordre.",
    )
    parser.add_argument(
        "--list",
        action="store_true",
        help="Liste les phases disponibles et quitte.",
    )

    args = parser.parse_args()

    if args.list:
        list_phases()
        return

    if args.phase:
        ok = run_phase(args.phase)
        sys.exit(0 if ok else 1)

    # Toutes les phases
    console.print(Panel(
        "[bold]Exécution de toutes les phases dans l'ordre[/]",
        border_style="green",
    ))
    list_phases()

    for phase in DEFAULT_ORDER:
        if not run_phase(phase):
            console.print(f"[red]✗ Pipeline interrompu sur {phase}[/]")
            sys.exit(1)

    console.print()
    console.print(Panel(
        "[bold green]🎉 Toutes les phases terminées avec succès[/]",
        border_style="green",
    ))


if __name__ == "__main__":
    main()
