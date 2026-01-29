# Módulo QA

## Descripción

El módulo QA ejecuta testing y validación como **ÚLTIMO paso del workflow**.
Si aprueba, la task se marca como **COMPLETADA**.

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                    061-qa-task.md                               │
│                    (Comando Orquestador)                        │
└─────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ test-executor   │  │ criteria-       │  │ qa-severity-    │
│ (Sonnet)        │  │ validator       │  │ calculator      │
│                 │  │ (Haiku)         │  │ (Haiku)         │
│ Ejecuta tests   │  │ Valida AC       │  │ Calcula score   │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │                                        │
         └────────────────────┬───────────────────┘
                              ▼
                    ┌─────────────────┐
                    │ qa-decision-    │
                    │ maker (Haiku)   │
                    │                 │
                    │ APPROVE/REJECT  │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │ qa-reporter     │
                    │ (Haiku)         │
                    │                 │
                    │ Reporta a BD    │
                    └─────────────────┘
```

---

## Agentes del Módulo

| Agente | Modelo | Responsabilidad | Herramientas |
|--------|--------|-----------------|--------------|
| test-executor-agent.md | Sonnet | Ejecutar tests y parsear output | Bash, Read |
| criteria-validator-agent.md | Haiku | Validar criterios de aceptación | Ninguna |
| qa-severity-calculator-agent.md | Haiku | Calcular severity con fórmula QA | Ninguna |
| qa-decision-maker-agent.md | Haiku | Tomar decisión APPROVE/REJECT | Ninguna |
| qa-reporter-agent.md | Haiku | Reportar a BD y completar task | MCP Tools |

---

## Agentes Reutilizados (de otros módulos)

| Agente | Módulo | Uso en QA |
|--------|--------|-----------|
| mcp-validator.md | common | Validar disponibilidad del MCP |
| validator-agent.md | implementer | Verificar archivos existen y compilan |

---

## Fórmula de Severity

```
severity = min(100,
  (failed / total) * 100 * 0.8 +       // Tests fallidos (0-80 pts)
  max(0, (70 - coverage) * 0.3) +      // Coverage bajo (0-30 pts)
  unmetCriteria * 10                   // Criterios no cumplidos (10 pts c/u)
)
```

### Ejemplos

| Escenario | Tests | Coverage | Criterios | Severity |
|-----------|-------|----------|-----------|----------|
| Perfecto | 23/23 (100%) | 85% | 4/4 | 0 |
| Algunos fallos | 20/23 (87%) | 65% | 2/4 | ~32 |
| Crítico | 0/10 (0%) | 0% | 0/3 | 100 |

---

## Frameworks de Testing Soportados

| Framework | Detección | Comando |
|-----------|-----------|---------|
| npm/jest | `package.json` | `npm test --if-present` |
| go test | `go.mod` | `go test ./... -v -cover` |
| pytest | `requirements.txt` | `pytest --cov -v` |
| cargo | `Cargo.toml` | `cargo test` |

---

## Diferencias con Code-Review

| Aspecto | Code-Review | QA |
|---------|-------------|-----|
| Soft Retry | SÍ (2-3 ciclos) | NO |
| Next Step | "qa" | null (último) |
| Task Completed | Nunca | SÍ si aprueba |
| Fórmula Severity | Pesos por issue | Fórmula específica |
| Correcciones | correction-executor | No aplica |

---

## Comando Asociado

- `/061-qa-task` - Orquestador principal (ÚLTIMO paso del workflow)

---

## Flujo de Ejecución

```
FASE 0:  mcp-validator (validar MCP)
FASE 1:  Parsear $ARGUMENTS
FASE 2:  Iniciar Tracking (MCP directo)
FASE 3:  Obtener Contexto (task → flow_row → project)
FASE 4:  Crear Work Item
FASE 5:  Obtener Archivos Implementer
FASE 6:  validator (verificar archivos)
FASE 7:  test-executor (ejecutar tests)
FASE 8:  criteria-validator (validar AC)
FASE 9:  qa-severity-calculator (calcular score)
FASE 10: qa-decision-maker (decidir)
FASE 11: qa-reporter (reportar y completar)
FASE 12: Finalizar Tracking
```

---

## Versión

- **Versión**: 1.0.0
- **Migrado desde**: `LLMs/Claude/.claude/agents/qa-agent.md`
- **Fecha**: 2026-01-15
