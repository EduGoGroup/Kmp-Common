---
name: 082-audit-step2-plan
description: PASO 2 - Planifica mejoras basándose en análisis previo y mejores prácticas
allowed-tools: Task, TodoWrite, Read, Glob, Grep, Write
---

# Auditoría de Comando Slash - PASO 2: Planificación

Prerrequisito: Ejecutar /081-audit-step1-analyze primero

## Input

```
/082-audit-step2-plan <nombre-comando>
```

Ejemplo: `/082-audit-step2-plan 011-constitution-create-project`

## Output

**Carpeta:** `analisis_comando_slash/<nombre-comando>/procesos_a_mejorar/`

**Archivos generados:**
- `01_evaluacion_comando.json` - Evaluación del comando con mejoras detalladas
- `02_evaluacion_agentes.json` - Evaluación de cada agente con mejoras detalladas
- `03_plan_consolidado.json` - Plan completo con TODAS las mejoras
- `04_resumen_ejecutivo.md` - Resumen legible para revisión rápida

---

## Estructura de Mejoras (OBLIGATORIA)

Cada mejora DEBE incluir estos campos:

```json
{
  "id": "M001",
  "prioridad": "CRITICA | ALTA | MEDIA | BAJA",
  "archivo": "ruta/al/archivo.md",
  "seccion": "Nombre de la sección a modificar",
  "problema": "Descripción clara del problema detectado",
  "cambio": "Cambio EXACTO a realizar (código o texto)",
  "justificacion": "Por qué es necesario este cambio según mejores prácticas"
}
```

### Prioridades

| Prioridad | Criterio |
|-----------|----------|
| **CRITICA** | Bloquea funcionamiento correcto o viola regla obligatoria |
| **ALTA** | Afecta robustez, mantenibilidad o consistencia significativamente |
| **MEDIA** | Mejora calidad, claridad o documentación |
| **BAJA** | Optimización menor o mejora opcional |

---

## Reglas para Definir Mejoras

### Ejemplos vs Listas Restrictivas

**PREFERIR EJEMPLOS** cuando:
- El agente debe clasificar/inferir información (tecnologías, tipos, categorías)
- Hay muchas posibilidades que no se pueden enumerar completamente
- El contexto puede cambiar con el tiempo (nuevas tecnologías, frameworks)

**Ejemplo CORRECTO** (guiar con ejemplos):
```markdown
## Ejemplos de Clasificación (para guiar inferencia)

| Tecnología | Términos relacionados (ejemplos) |
|------------|----------------------------------|
| spring_boot | spring, java, maven, gradle, jpa |
| dotnet | .net, c#, aspnet, entity framework |
| kotlin_multiplatform | kmm, kotlin, compose, ktor |

**IMPORTANTE**: Esta tabla es solo de EJEMPLO. Si recibes una tecnología 
no listada, debes INFERIR su clasificación basándote en tu conocimiento.
```

**Ejemplo INCORRECTO** (lista restrictiva):
```typescript
// ❌ MAL: Lista cerrada que falla con tecnologías no listadas
const TECH_TERMS = {
  "golang": ["go", "gin"],
  "python": ["django", "flask"],
  // Si viene "spring boot" → ERROR porque no está
}
```

**USAR LISTAS RESTRICTIVAS** solo cuando:
- Son valores del sistema/BD que NO pueden cambiar (status, tipos de entidad)
- Son opciones finitas y conocidas (niveles: mvp/standard/enterprise)
- Hay validación estricta requerida (códigos de error)

---

## Flujo de Ejecución

### FASE -1: Inicializar TODO List

```typescript
await TodoWrite({
  todos: [
    { content: "Cargar análisis PASO 1", status: "pending", activeForm: "Cargando análisis" },
    { content: "Cargar mejores prácticas", status: "pending", activeForm: "Cargando prácticas" },
    { content: "Evaluar comando", status: "pending", activeForm: "Evaluando comando" },
    { content: "Evaluar agentes", status: "pending", activeForm: "Evaluando agentes" },
    { content: "Consolidar plan", status: "pending", activeForm: "Consolidando plan" },
    { content: "Guardar resultados", status: "pending", activeForm: "Guardando resultados" }
  ]
})
```

### FASE 1: Cargar Análisis Previo

```typescript
const analisisPrevio = await Task({
  subagent_type: "general-purpose",
  description: "Cargar análisis PASO 1",
  prompt: `Lee TODOS los archivos en analisis_comando_slash/${comando}/procesos_previos/ y consolida:
    - Estructura del comando (fases, agentes, herramientas)
    - Análisis de cada fase
    - Análisis de cada agente
    Retorna JSON consolidado.`
})
```

### FASE 2: Cargar Mejores Prácticas

```typescript
const mejoresPracticas = await Task({
  subagent_type: "general-purpose",
  description: "Cargar mejores prácticas",
  prompt: `Lee estos archivos y extrae checklists:
    - .claude/commands/EXAMPLE-best-practices-command.md
    - .claude/agents/EXAMPLE-best-practices-agent.md
    
    Extrae:
    - checklist_comandos: criterios para comandos
    - checklist_agentes: criterios para agentes
    - patrones_recomendados
    - antipatrones
    
    Retorna JSON estructurado.`
})
```

### FASE 3: Evaluar Comando

```typescript
const evalComando = await Task({
  subagent_type: "general-purpose",
  description: "Evaluar comando vs prácticas",
  prompt: `Compara el comando con las mejores prácticas.
    
    Para cada práctica no cumplida, genera una mejora con:
    - id: "M001", "M002", etc.
    - prioridad: CRITICA/ALTA/MEDIA/BAJA
    - archivo: ruta del comando
    - seccion: sección específica a modificar
    - problema: qué está mal
    - cambio: cambio EXACTO a realizar
    - justificacion: por qué según mejores prácticas
    
    Retorna:
    {
      "puntaje": 0-100,
      "cumple": ["lista de prácticas cumplidas"],
      "no_cumple": ["lista de prácticas no cumplidas"],
      "mejoras": [array de mejoras con estructura completa]
    }`
})
```

### FASE 4: Evaluar Agentes

```typescript
const evalAgentes = await Task({
  subagent_type: "general-purpose",
  description: "Evaluar agentes vs prácticas",
  prompt: `Para CADA agente del comando, evalúa contra mejores prácticas.
    
    Genera mejoras con IDs únicos por agente:
    - AA001, AA002... para analyzer-agent
    - PC001, PC002... para project-creator-agent
    - DF001, DF002... para document-finder-agent
    - etc.
    
    Cada mejora debe tener la estructura completa:
    id, prioridad, archivo, seccion, problema, cambio, justificacion
    
    Retorna:
    {
      "resumen": { "total_agentes": N, "puntaje_promedio": N },
      "evaluaciones": [
        {
          "agente": "nombre",
          "puntaje": 0-100,
          "cumple": [...],
          "no_cumple": [...],
          "mejoras": [array con estructura completa]
        }
      ]
    }`
})
```

### FASE 5: Consolidar Plan

```typescript
const planConsolidado = await Task({
  subagent_type: "general-purpose",
  description: "Consolidar plan mejoras",
  prompt: `Consolida TODAS las mejoras en un plan ejecutable.
    
    Agrupa en fases de ejecución:
    1. Correcciones Críticas (todas las CRITICA)
    2. Alta Prioridad - Comando
    3. Alta Prioridad - Agentes
    4. Media Prioridad - Comando
    5. Media Prioridad - Agentes
    6. Baja Prioridad - Opcionales
    
    Retorna:
    {
      "plan_mejoras": {
        "total_mejoras": N,
        "por_prioridad": { "CRITICA": N, "ALTA": N, "MEDIA": N, "BAJA": N },
        "archivos_afectados": [lista de archivos]
      },
      "fases_ejecucion": [
        { "fase": 1, "nombre": "...", "mejoras": ["M001", ...], "minutos": N }
      ],
      "mejoras_comando": [array con TODAS las mejoras del comando],
      "mejoras_agentes": [array con TODAS las mejoras de agentes]
    }`
})
```

### FASE 6: Guardar Resultados

Guardar en `analisis_comando_slash/<comando>/procesos_a_mejorar/`:

**01_evaluacion_comando.json:**
```json
{
  "comando": "<nombre>",
  "puntaje": N,
  "cumple": [...],
  "no_cumple": [...],
  "mejoras": [
    {
      "id": "M001",
      "prioridad": "CRITICA",
      "archivo": "...",
      "seccion": "...",
      "problema": "...",
      "cambio": "...",
      "justificacion": "..."
    }
  ]
}
```

**02_evaluacion_agentes.json:**
```json
{
  "resumen": { "total_agentes": N, "puntaje_promedio": N },
  "evaluaciones": [
    {
      "agente": "nombre",
      "archivo": "ruta",
      "puntaje": N,
      "mejoras": [/* estructura completa */]
    }
  ]
}
```

**03_plan_consolidado.json:**
```json
{
  "plan_mejoras": { /* resumen */ },
  "fases_ejecucion": [ /* fases con mejoras */ ],
  "mejoras_comando": [ /* TODAS las mejoras del comando con detalle */ ],
  "mejoras_agentes": [ /* TODAS las mejoras de agentes con detalle */ ]
}
```

**04_resumen_ejecutivo.md:**
```markdown
# Auditoría PASO 2: Plan de Mejoras

## Comando: <nombre>

### Resumen
- Puntaje Comando: N/100
- Puntaje Promedio Agentes: N/100
- Total Mejoras: N
- Tiempo Estimado: N horas

### Top 5 Mejoras Críticas/Altas
1. ID - Descripción breve
...

### Próximo Paso
Ejecutar /083-audit-step3-execute <nombre-comando>
```

---

## Criterios de Evaluación

### Para Comandos

| Criterio | Prioridad si falta |
|----------|-------------------|
| FASE -1 (TODO List) | CRITICA |
| MCPSearch antes de MCP tools | CRITICA |
| allowed-tools completo | CRITICA |
| Validación de input | ALTA |
| Manejo de errores estructurado | ALTA |
| Output JSON con campos requeridos | ALTA |
| Logging estructurado | MEDIA |
| Documentación de testing | BAJA |

### Para Agentes

| Criterio | Prioridad si falta |
|----------|-------------------|
| subagent_type en frontmatter | CRITICA |
| tools declarado (aunque sea vacío) | CRITICA |
| Validación de input | ALTA |
| Output de error documentado | ALTA |
| Flujo numerado | MEDIA |
| Sección de testing | BAJA |

---

## Siguiente Paso

```
/083-audit-step3-execute <nombre-comando>
```

Aplica las mejoras planificadas en este paso.
