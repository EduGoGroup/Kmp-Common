# Levels Helper

Define límites y configuraciones según nivel de proyecto (MVP/Standard/Enterprise).

---

## 📊 Tabla de Límites por Nivel

| Métrica | MVP | Standard | Enterprise |
|---------|-----|----------|------------|
| `max_sprints` | 1 | 3 | 8 |
| `max_flow_rows_per_sprint` | 3 | 6 | 10 |
| `max_stories_per_flow_row` | 2 | 4 | 6 |
| `max_tasks_per_story` | 5 | 8 | 12 |
| `max_total_tasks` | 15 | 50 | 200 |
| `threshold_code_review` | 70 | 50 | 35 |
| `threshold_qa` | 70 | 50 | 35 |
| `max_fix_depth` | 2 | 3 | 4 |

---

## 🔧 Configuraciones por Nivel

### MVP

```json
{
  "project_level": "mvp",
  "limits": {
    "max_sprints": 1,
    "max_flow_rows_per_sprint": 3,
    "max_stories_per_flow_row": 2,
    "max_tasks_per_story": 5,
    "max_total_tasks": 15
  },
  "thresholds": {
    "code_review": 70,
    "qa": 70,
    "max_fix_depth": 2
  },
  "allowed_modules": ["core", "tests"],
  "forbidden_modules": [
    "security_middleware", "cors", "monitoring", 
    "oauth", "distributed", "multi_region"
  ],
  "soft_retry": {
    "enabled": true,
    "soft_threshold": 25,
    "max_retries": 2
  }
}
```

### Standard

```json
{
  "project_level": "standard",
  "limits": {
    "max_sprints": 3,
    "max_flow_rows_per_sprint": 6,
    "max_stories_per_flow_row": 4,
    "max_tasks_per_story": 8,
    "max_total_tasks": 50
  },
  "thresholds": {
    "code_review": 50,
    "qa": 50,
    "max_fix_depth": 3
  },
  "allowed_modules": [
    "core", "tests", "config", "logging", 
    "error_handling", "validation"
  ],
  "forbidden_modules": [
    "distributed", "multi_region", "event_sourcing"
  ],
  "soft_retry": {
    "enabled": true,
    "soft_threshold": 30,
    "max_retries": 2
  }
}
```

### Enterprise

```json
{
  "project_level": "enterprise",
  "limits": {
    "max_sprints": 8,
    "max_flow_rows_per_sprint": 10,
    "max_stories_per_flow_row": 6,
    "max_tasks_per_story": 12,
    "max_total_tasks": 200
  },
  "thresholds": {
    "code_review": 35,
    "qa": 35,
    "max_fix_depth": 4
  },
  "allowed_modules": "all",
  "forbidden_modules": [],
  "soft_retry": {
    "enabled": true,
    "soft_threshold": 35,
    "max_retries": 3
  }
}
```

---

## 🔄 Sistema de Soft Retry

El soft retry permite corrección automática para issues leves SIN crear fix_flow_row.

### Flujo de Decisión

```
severity = 0              → APROBAR directo
0 < severity ≤ SOFT_THR   → SOFT RETRY (corrección inline)
severity > SOFT_THR       → RECHAZAR (crear fix_flow_row)
```

### Thresholds por Nivel

| Nivel | Soft Threshold | Descripción |
|-------|----------------|-------------|
| MVP | 25 | Issues muy severos para soft retry |
| Standard | 30 | Balance |
| Enterprise | 35 | Más permisivo |

---

## 📋 Funciones de Utilidad

### `getLimitsForLevel(level)`

Retorna los límites para un nivel específico.

### `validateAgainstLimits(entity, level)`

Valida una entidad contra los límites del nivel.

### `shouldTriggerSoftRetry(severity, level)`

Determina si un issue debe usar soft retry.

### `getDefaultConfig(level)`

Retorna la configuración por defecto para un nivel.

---

## 🎯 Uso en Agentes

Los agentes deben consultar este helper para:

1. **milestone-analyzer**: Límite de módulos propuestos
2. **flow-creator**: Límite de sprints
3. **impact-filter**: Módulos permitidos/prohibidos
4. **story-creator**: Límite de stories por módulo
5. **depth-validator**: Profundidad máxima de fixes

---

**Versión**: 2.0
**Última actualización**: 2026-01-16
