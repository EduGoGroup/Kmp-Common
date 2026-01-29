# Planner Agents

Agentes especializados para descomponer user stories en tasks técnicas atómicas.

---

## 📋 Arquitectura

```
/031-planner-decompose-story (ORQUESTADOR)
    │
    ├─ FASE 0:  Validar MCP          → common/mcp-validator
    ├─ FASE 1:  Preprocesar input    → Script (STORY_ID)
    ├─ FASE 2:  Iniciar Tracking     → MCP directo (start_session)
    ├─ FASE 3:  Obtener contexto     → MCP directo (get_story, get_flow_row, get_flow, get_project)
    ├─ FASE 4:  Buscar documentación → common/search-local
    ├─ FASE 5:  Analizar story       → planner/story-analyzer-agent
    ├─ FASE 6:  Crear tasks          → planner/task-creator-agent
    ├─ FASE 7:  Finalizar tracking   → MCP directo (finish_session)
    └─ FASE 8:  Retornar resultado   → Script (JSON consolidado)
```

---

## 🤖 Agentes

### 1. story-analyzer-agent (sonnet)

**Responsabilidad**: Analizar story y generar plan de tasks.

**Input**:
- story_title, story_content, acceptance_criteria
- project_level, tech, kind
- flow_row_type (feature/fix)
- relevant_docs (opcional)

**Output**:
- proposed_tasks[] con title, description, dependencies, effort, complexity

**NO hace**:
- ❌ Llamadas MCP
- ❌ Crear tasks en BD
- ❌ Task() para delegar

---

### 2. task-creator-agent (haiku)

**Responsabilidad**: Insertar tasks en BD.

**Input**:
- story_id
- tasks[] (del analyzer)

**Output**:
- tasks_created, task_ids[]

**Solo hace**:
- ✅ create_tasks_batch via MCP

---

## 📊 Flujo de Datos

```
story_id → [MCP: get_story] → story_content
                                    ↓
                            [story-analyzer]
                                    ↓
                             proposed_tasks[]
                                    ↓
                            [task-creator]
                                    ↓
                              task_ids[]
```

---

## 🔗 Diferencias con Versión Anterior

| Aspecto | ANTES (Claude) | DESPUÉS (Claude4) |
|---------|----------------|-------------------|
| Tracking | En el agente | En el comando |
| MCP calls | En el agente (4+) | En el comando (contexto) + agente creador |
| Análisis | Un solo agente | Agente separado (analyzer) |
| Inserción | Mismo agente | Agente separado (creator) |
| Task() | Sí (para search, tracking) | NO (prohibido en agentes) |

---

**Versión**: 1.0
**Última actualización**: 2026-01-15
