# Deep Analysis Agents

Agentes especializados para análisis profundo, creación de sprints y manejo de fixes.

---

## 📂 Estructura

```
deep-analysis/
├── README.md                        (este archivo)
│
├── --- SPRINT FLOW ---
├── milestone-analyzer-agent.md      # Analiza milestone → propone módulos
├── flow-creator-agent.md            # Crea flow (sprint) en BD
├── impact-filter-agent.md           # Filtra módulos por impacto
├── module-creator-agent.md          # Crea flow_rows en BD
├── story-creator-agent.md           # Crea stories en BD
│
├── --- FIX FLOW ---
├── sanity-check-agent.md            # Valida si fix es necesario
├── depth-validator-agent.md         # Valida profundidad de fixes
├── root-cause-analyzer-agent.md     # Analiza causa raíz de issues
└── fix-creator-agent.md             # Crea fix flow_row + story
```

---

## 🏃 Sprint Flow

```
milestone-analyzer
       ↓
   flow-creator
       ↓
  impact-filter
       ↓
 module-creator
       ↓
  story-creator
```

### Agentes del Sprint Flow

| Agente | Input Principal | Output Principal |
|--------|-----------------|------------------|
| milestone-analyzer | milestone_description | proposed_modules[] |
| flow-creator | project_id, milestone_analysis | flow_id |
| impact-filter | proposed_modules[] | approved_modules[] |
| module-creator | flow_id, approved_modules[] | flow_rows_created[] |
| story-creator | flow_rows[] | stories_created |

---

## 🔧 Fix Flow

```
sanity-check
     ↓
depth-validator
     ↓
root-cause-analyzer
     ↓
  fix-creator
```

### Agentes del Fix Flow

| Agente | Input Principal | Output Principal |
|--------|-----------------|------------------|
| sanity-check | project_path, issues[] | proceed/skip |
| depth-validator | parent_flow_row_id | valid/exceeded |
| root-cause-analyzer | task_id, issues[] | analysis |
| fix-creator | flow_id, root_cause_analysis | fix_flow_row_id |

---

## 🎯 Responsabilidades

### Principio de Responsabilidad Única

Cada agente tiene UNA responsabilidad:
- **Análisis**: milestone-analyzer, root-cause-analyzer
- **Validación**: sanity-check, depth-validator, impact-filter
- **Creación en BD**: flow-creator, module-creator, story-creator, fix-creator

### Sin Tasks Internos

Los agentes NO pueden invocar `Task()` para delegar a otros agentes.
La orquestación se hace desde los comandos (021, 022, 023, 025).

---

## 📋 Modelos Asignados

| Agente | Modelo | Razón |
|--------|--------|-------|
| milestone-analyzer | sonnet | Análisis complejo |
| flow-creator | haiku | Creación simple |
| impact-filter | sonnet | Análisis de impacto |
| module-creator | haiku | Creación simple |
| story-creator | sonnet | Generación de contenido |
| sanity-check | haiku | Validación rápida |
| depth-validator | haiku | Validación simple |
| root-cause-analyzer | sonnet | Análisis complejo |
| fix-creator | haiku | Creación simple |

---

## 🔗 Integración con Comandos

| Comando | Agentes Utilizados |
|---------|-------------------|
| 021-create-sprint | milestone-analyzer → flow-creator → impact-filter → module-creator → story-creator |
| 022-create-fix | sanity-check → depth-validator → root-cause-analyzer → fix-creator |
| 023-create-fix-manual | (igual que 022) |
| 025-auto-sprints | (igual que 021, en loop) |

---

**Versión**: 2.0
**Última actualización**: 2026-01-16
