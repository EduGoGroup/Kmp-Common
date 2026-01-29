#!/usr/bin/env python3
"""
Helper para inferir metadata (tags, steps, kinds) de documentos markdown.

Uso:
    echo "$CONTENT" | python3 infer_metadata.py - '{"tags":[...],"steps":[...],"kinds":[...]}'
    python3 infer_metadata.py content.txt '{"tags":[...],"steps":[...],"kinds":[...]}'
"""

import sys
import json
import re


def infer_tags(content: str, available_tags: list) -> tuple:
    """Infiere tags basándose en keywords."""
    content_lower = content.lower()
    results = {}

    patterns = {
        'golang': ['golang', 'go ', 'func ', 'package ', 'struct {'],
        'python': ['python', 'def ', 'pip ', 'pytest', '__init__'],
        'nodejs': ['nodejs', 'node.js', 'npm', 'const ', 'require('],
        'testing': ['test', 'testing', 'assert', 'mock', 'unittest'],
        'qa': ['qa', 'quality assurance', 'test plan'],
        'standards': ['estándar', 'convención', 'guía', 'best practice'],
        'security': ['seguridad', 'security', 'auth', 'encryption'],
        'architecture': ['arquitectura', 'architecture', 'pattern', 'microservice'],
        'performance': ['performance', 'optimización', 'cache', 'benchmark']
    }

    for tag, keywords in patterns.items():
        if tag not in available_tags:
            continue
        matches = sum(1 for kw in keywords if kw in content_lower)
        if matches > 0:
            results[tag] = min(1.0, matches * 0.25)

    MIN_CONFIDENCE = 0.3
    filtered = [t for t, c in results.items() if c >= MIN_CONFIDENCE]
    return filtered, {t: c for t, c in results.items() if c >= MIN_CONFIDENCE}


def infer_steps(content: str, available_steps: list) -> tuple:
    """Infiere applies_to_steps."""
    content_lower = content.lower()
    results = {}

    has_code = bool(re.search(r'```[\s\S]*?```', content))

    if 'implementer' in available_steps and has_code:
        results['implementer'] = 0.9

    if 'code_review' in available_steps:
        if any(kw in content_lower for kw in ['review', 'best practice', 'lint']):
            results['code_review'] = 0.7

    if 'qa' in available_steps:
        if any(kw in content_lower for kw in ['test', 'qa', 'quality']):
            results['qa'] = 0.7

    if 'planner' in available_steps:
        if any(kw in content_lower for kw in ['plan', 'sprint', 'roadmap']):
            results['planner'] = 0.6

    if 'constitution' in available_steps:
        if any(kw in content_lower for kw in ['setup', 'config', 'bootstrap']):
            results['constitution'] = 0.6

    MIN_CONFIDENCE = 0.3
    filtered = [s for s, c in results.items() if c >= MIN_CONFIDENCE]
    return filtered, {s: c for s, c in results.items() if c >= MIN_CONFIDENCE}


def infer_kinds(content: str, available_kinds: list) -> tuple:
    """Infiere applies_to_kinds."""
    content_lower = content.lower()
    results = {}

    patterns = {
        'api': ['api', 'rest', 'graphql', 'endpoint', 'http'],
        'web': ['web', 'frontend', 'react', 'vue', 'html'],
        'cli': ['cli', 'command line', 'terminal', 'shell'],
        'mobile': ['mobile', 'ios', 'android', 'flutter'],
        'service': ['microservicio', 'service', 'backend', 'daemon']
    }

    for kind, keywords in patterns.items():
        if kind not in available_kinds:
            continue
        matches = sum(1 for kw in keywords if kw in content_lower)
        if matches > 0:
            results[kind] = min(1.0, matches * 0.25)

    MIN_CONFIDENCE = 0.3
    filtered = [k for k, c in results.items() if c >= MIN_CONFIDENCE]
    return filtered, {k: c for k, c in results.items() if c >= MIN_CONFIDENCE}


def main():
    if len(sys.argv) != 3:
        print(json.dumps({
            "error": "Invalid arguments",
            "usage": "python3 infer_metadata.py <file|-> <catalogs_json>"
        }), file=sys.stderr)
        sys.exit(1)

    # Leer contenido
    content_file = sys.argv[1]
    try:
        if content_file == '-':
            content = sys.stdin.read()
        else:
            with open(content_file, 'r', encoding='utf-8') as f:
                content = f.read()
    except Exception as e:
        print(json.dumps({"error": f"Read failed: {e}"}), file=sys.stderr)
        sys.exit(1)

    # Parsear catálogos
    try:
        catalogs = json.loads(sys.argv[2])
    except Exception as e:
        print(json.dumps({"error": f"JSON parse failed: {e}"}), file=sys.stderr)
        sys.exit(1)

    if not all(k in catalogs for k in ['tags', 'steps', 'kinds']):
        print(json.dumps({"error": "Missing keys: tags, steps, kinds"}), file=sys.stderr)
        sys.exit(1)

    # Inferir
    tags, tags_conf = infer_tags(content, catalogs['tags'])
    steps, steps_conf = infer_steps(content, catalogs['steps'])
    kinds, kinds_conf = infer_kinds(content, catalogs['kinds'])

    result = {
        "tags": tags,
        "steps": steps,
        "kinds": kinds,
        "confidence": {
            "tags": tags_conf,
            "steps": steps_conf,
            "kinds": kinds_conf
        },
        "stats": {
            "content_length": len(content),
            "tags_found": len(tags),
            "steps_found": len(steps),
            "kinds_found": len(kinds)
        }
    }

    print(json.dumps(result, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
