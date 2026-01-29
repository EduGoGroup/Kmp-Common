# Módulo Implementer

Agentes especializados para la implementación de código en el workflow de desarrollo.

## Arquitectura

```
implementer/
├── README.md                      # Este archivo
├── code-executor-agent.md         # (Sonnet) Implementa código
├── validator-agent.md             # (Haiku) Valida compilación/tests
├── result-reporter-agent.md       # (Haiku) Actualiza BD via MCP
└── correction-executor-agent.md   # (Sonnet) Aplica correcciones
```

## Flujo Normal (041-implementer-task)

```
Comando Orquestador
│
├── FASE 0-4:  Preparación (MCP, contexto, work_item)
├── FASE 5:    Buscar documentación → common/search-local
├── FASE 6:    Implementar código   → code-executor-agent
├── FASE 7:    Validar código       → validator-agent
├── FASE 8:    Reportar resultados  → result-reporter-agent
└── FASE 9-10: Tracking + Retorno
```

## Flujo Corrección (042-implementer-correction)

```
Comando Orquestador
│
├── FASE 0-1: Validar MCP + Parsear input
├── FASE 2:   Aplicar correcciones → correction-executor-agent
├── FASE 3:   Validar correcciones → validator-agent
└── FASE 4:   Retornar resultado
```

## Agentes

### code-executor-agent (Sonnet)
- **Responsabilidad**: Implementar código según task_description
- **Tools**: Read, Write, Edit
- **Prohibido**: MCP calls, Task(), Bash

### validator-agent (Haiku)
- **Responsabilidad**: Ejecutar compilación y tests
- **Tools**: Bash (solo build/test)
- **Prohibido**: Modificar archivos, MCP calls, Task()

### result-reporter-agent (Haiku)
- **Responsabilidad**: Actualizar work_item y avanzar task en BD
- **Tools**: update_work_item_output, evaluate_work_item, advance_to_next_step
- **Prohibido**: Leer/escribir archivos, Task(), Bash

### correction-executor-agent (Sonnet)
- **Responsabilidad**: Aplicar correcciones específicas a archivos
- **Tools**: Read, Edit
- **Prohibido**: MCP calls, Task(), Bash

## Comandos Relacionados

- `041-implementer-task.md` - Orquestador modo normal
- `042-implementer-correction.md` - Orquestador modo corrección

## Principios

1. **Single Responsibility**: Cada agente hace UNA sola cosa
2. **Modo Silencioso**: Agentes retornan solo JSON
3. **Sin Task()**: Prohibido delegar a otros agentes
4. **Tracking Centralizado**: El comando maneja todo el tracking

---

**Versión**: 1.0
**Última actualización**: 2026-01-15
