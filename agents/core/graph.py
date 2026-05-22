"""
Pipeline LangGraph : orchestre les 3 agents.

Schéma :

    ┌──────────┐     ┌──────────┐     ┌──────────┐
    │  CODER   │ ──→ │ REVIEWER │ ──→ │  WRITER  │ → END
    └──────────┘     └────┬─────┘     └──────────┘
          ↑               │ KO (si iterations < max)
          └───────────────┘
"""

from langgraph.graph import StateGraph, END

from core.agents import coder_agent, reviewer_agent, writer_agent
from core.state import AgentState


def _route_after_review(state: AgentState) -> str:
    """
    Décide ce qu'on fait après la review :
      - approuvé          → writer
      - rejeté & retries OK → coder (correction)
      - rejeté & max atteint → writer (on continue quand même)
    """
    if state["review_approved"]:
        return "writer"

    if state["iterations"] >= state["max_iterations"]:
        return "writer"

    return "coder"


def build_graph():
    """Construit et compile le graphe LangGraph."""
    graph = StateGraph(AgentState)

    # Nœuds
    graph.add_node("coder",    coder_agent)
    graph.add_node("reviewer", reviewer_agent)
    graph.add_node("writer",   writer_agent)

    # Flux
    graph.set_entry_point("coder")
    graph.add_edge("coder", "reviewer")
    graph.add_conditional_edges(
        "reviewer",
        _route_after_review,
        {"coder": "coder", "writer": "writer"},
    )
    graph.add_edge("writer", END)

    return graph.compile()
