#!/usr/bin/env python3
"""
Corrige les fichiers OpenAPI générés par springdoc pour l'import WSO2 :
 - Downgrade openapi 3.1.0 → 3.0.3  (WSO2 ne supporte pas 3.1)
 - Renomme les titres
 - Corrige les servers (localhost → host.docker.internal)
 - Adapte les types nullable (3.1 → 3.0)
"""
import json, copy, sys, os

CONFIGS = {
    "inscription-api.json": {
        "title": "Inscription Service API",
        "server": "http://host.docker.internal:8091",
        "version": "v1"
    },
    "school-api.json": {
        "title": "School Service API",
        "server": "http://host.docker.internal:8094",
        "version": "v1"
    },
    "etudiant-api.json": {
        "title": "Etudiant Service API",
        "server": "http://host.docker.internal:8092",
        "version": "v1"
    },
    "paiement-api.json": {
        "title": "Paiement Service API",
        "server": "http://host.docker.internal:8093",
        "version": "v1"
    },
    "annee-api.json": {
        "title": "Annee Academique Service API",
        "server": "http://host.docker.internal:8090",
        "version": "v1"
    },
}

def fix_schema(schema):
    """Convertit les constructs OpenAPI 3.1 en 3.0."""
    if not isinstance(schema, dict):
        return schema

    result = {}
    for k, v in schema.items():
        # 3.1 : type peut être une liste ["string", "null"] → 3.0 : nullable: true
        if k == "type" and isinstance(v, list):
            types = [t for t in v if t != "null"]
            if "null" in v:
                result["nullable"] = True
            result["type"] = types[0] if len(types) == 1 else types
        # 3.1 : $schema → ignorer
        elif k == "$schema":
            pass
        # 3.1 : const → enum à 1 valeur
        elif k == "const":
            result["enum"] = [v]
        else:
            result[k] = fix_schema(v) if isinstance(v, (dict, list)) else v

    return result

def fix_list(lst):
    return [fix_schema(item) if isinstance(item, dict) else fix_list(item) if isinstance(item, list) else item for item in lst]

def deep_fix(obj):
    if isinstance(obj, dict):
        return fix_schema({k: deep_fix(v) for k, v in obj.items()})
    elif isinstance(obj, list):
        return fix_list(obj)
    return obj


base_dir = os.path.dirname(os.path.abspath(__file__))

for filename, cfg in CONFIGS.items():
    path = os.path.join(base_dir, filename)
    if not os.path.exists(path):
        print(f"⚠️  {filename} introuvable, ignoré")
        continue

    with open(path, "r", encoding="utf-8") as f:
        spec = json.load(f)

    # 1. Downgrade version OpenAPI
    spec["openapi"] = "3.0.3"

    # 2. Titre et version
    spec.setdefault("info", {})
    spec["info"]["title"] = cfg["title"]
    spec["info"]["version"] = cfg["version"]

    # 3. Servers
    spec["servers"] = [{"url": cfg["server"], "description": "Backend service"}]

    # 4. Adapter les types 3.1 → 3.0
    spec = deep_fix(spec)

    # 5. Injecter les paramètres de chemin manquants (requis par WSO2)
    import re
    for path, ops in spec.get("paths", {}).items():
        path_vars = re.findall(r'\{(\w+)\}', path)
        for method, op in ops.items():
            if method not in ("get", "post", "put", "delete", "patch"):
                continue
            existing = [p["name"] for p in op.get("parameters", []) + ops.get("parameters", []) if p.get("in") == "path"]
            for var in path_vars:
                if var not in existing:
                    op.setdefault("parameters", []).append({
                        "name": var,
                        "in": "path",
                        "required": True,
                        "schema": {"type": "string"}
                    })

    out_path = os.path.join(base_dir, filename.replace(".json", "-wso2.json"))
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(spec, f, indent=2, ensure_ascii=False)

    print(f"✅ {filename} → {os.path.basename(out_path)}")

print("\nFichiers prêts pour import WSO2.")
