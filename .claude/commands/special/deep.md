---
name: deep
description: Descomponer story en tasks tecnicas
---

# Deep: Descomponer Story en Tasks

Descompone una story en tasks tecnicas detalladas y alineadas con el codigo actual.

---

## Configuracion del Proyecto

```
PROJECT_ID: proj-1769654677209332000
PROJECT_PATH: /Users/jhoanmedina/source/EduGo/EduUI/Modules/Kmp-Common
DOCS_PATH: /Users/jhoanmedina/source/EduGo/EduUI/Modules/Kmp-Common/Documents_Analisys
```

---

## Input

**Argumento**: `$ARGUMENTS` = story_id

**Uso**:
```bash
/special:deep story-como-desarrollador-quiero-validar-compilaci-n-cruz-2d54ff30
```

---

## Que se espera

1. Leer la informacion de la story desde MCP
2. Validar y explorar el codigo actual del proyecto
3. Si necesitas informacion del backend, consulta DOCS_PATH (swagger, APIs, etc.)
4. Crear tasks que sean:
   - Consistentes con el codigo actual
   - Detalladas con pasos especificos
   - Consolidadas (no micro-tasks, si varias se pueden unir, unelas)

---

## Guia de referencia

Adaptate segun el contexto usando como referencia:
`/Users/jhoanmedina/source/EduGo/EduUI/Modules/Kmp-Common/.claude/commands/031-planner-decompose-story.md`

Revisa los agentes que usa y como funciona el flujo completo.

---

## Output

```json
{
  "success": true
}
```
