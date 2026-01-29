# Módulo Code-Review

## Descripción

El módulo code-review ejecuta revisiones automáticas de código con sistema de **Soft Retry** para correcciones menores. Sigue la arquitectura modular de Claude4 donde el comando orquestador maneja el ciclo principal y delega tareas atómicas a agentes especializados.

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                    051-code-review-task.md                      │
│                    (Comando Orquestador)                        │
│                                                                 │
│  Responsabilidades:                                             │
│  - Validar MCP disponible                                       │
│  - Iniciar/cerrar tracking                                      │
│  - Manejar ciclo de soft retry                                  │
│  - Delegar tareas atómicas a agentes                            │
└─────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ code-analyzer   │  │ severity-       │  │ decision-maker  │
│ (Sonnet)        │  │ calculator      │  │ (Haiku)         │
│                 │  │ (Haiku)         │  │                 │
│ Analiza código  │  │ Calcula score   │  │ APPROVE/REJECT  │
│ detecta issues  │  │ pondera issues  │  │ SOFT_RETRY      │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │                                        │
         └────────────────────┬───────────────────┘
                              ▼
                    ┌─────────────────┐
                    │ review-reporter │
                    │ (Haiku)         │
                    │                 │
                    │ Reporta a BD    │
                    │ via MCP         │
                    └─────────────────┘
```

---

## Agentes del Módulo

| Agente | Modelo | Responsabilidad |
|--------|--------|-----------------|
| `code-analyzer-agent.md` | Sonnet | Analizar archivos y detectar issues de seguridad, calidad y estilo |
| `severity-calculator-agent.md` | Haiku | Calcular severity ponderado según nivel del proyecto |
| `decision-maker-agent.md` | Haiku | Tomar decisión APPROVE/SOFT_RETRY/REJECT |
| `review-reporter-agent.md` | Haiku | Actualizar work_item y avanzar workflow via MCP |

---

## Agentes Reutilizados (de otros módulos)

| Agente | Módulo | Uso en Code-Review |
|--------|--------|-------------------|
| `mcp-validator.md` | common | Validar disponibilidad del MCP |
| `validator-agent.md` | implementer | Validar compilación y tests |
| `correction-executor-agent.md` | implementer | Aplicar correcciones en soft retry |

---

## Sistema de Soft Retry

El soft retry permite correcciones automáticas de issues menores antes de rechazar.

### Umbrales por Nivel de Proyecto

| Nivel | Soft Threshold | Max Retries | Issues Considerados |
|-------|---------------|-------------|---------------------|
| MVP | 25 | 2 | critical, high |
| Standard | 30 | 2 | critical, high, medium |
| Enterprise | 35 | 3 | todos |

### Pesos de Severity

```
critical: 40 puntos
high:     20 puntos
medium:   10 puntos
low:       5 puntos
style:     1 punto
```

### Flujo de Decisión

```
severity == 0                              → APPROVE directo
0 < severity ≤ soft_threshold (con cycles) → SOFT_RETRY
severity > threshold                       → REJECT
```

---

## Flujo de Datos

```
Input: task_id
         │
         ▼
┌─────────────────────────────────────────┐
│  1. Obtener contexto                    │
│     task → flow_row → flow → project    │
└─────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  2. Obtener archivos del implementer    │
│     (files_created + files_modified)    │
└─────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  3. CICLO DE REVISIÓN                   │
│     ├─ code-analyzer → issues[]         │
│     ├─ validator → compiles, tests      │
│     ├─ severity-calculator → severity   │
│     ├─ decision-maker → decision        │
│     │                                   │
│     └─ if SOFT_RETRY:                   │
│        ├─ correction-executor           │
│        └─ validator (verificar)         │
└─────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  4. review-reporter                     │
│     update_work_item + evaluate +       │
│     advance_to_next_step                │
└─────────────────────────────────────────┘
         │
         ▼
Output: {success, decision, severity, next_step}
```

---

## Herramientas por Agente

| Agente | Read | Write | Edit | Bash | MCP | Task() |
|--------|------|-------|------|------|-----|--------|
| code-analyzer | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| severity-calculator | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| decision-maker | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| review-reporter | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |

---

## Comando Asociado

- **051-code-review-task.md**: Orquestador principal que maneja todo el flujo incluyendo soft retry

---

## Versión

- **Versión**: 1.0.0
- **Migrado desde**: `LLMs/Claude/.claude/agents/code-review-agent.md`
- **Fecha**: 2026-01-15
