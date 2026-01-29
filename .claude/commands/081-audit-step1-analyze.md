---
name: 081-audit-step1-analyze
description: PASO 1 - Analiza estado actual de un comando slash y sus dependencias
allowed-tools: Task, TodoWrite, Read, Glob, Grep, Write
---

# Auditoría de Comando Slash - PASO 1: Análisis

Analiza la estructura, procesos y agentes de un comando slash para preparar la auditoría.

## Input

```
/081-audit-step1-analyze <nombre-comando>
```

Ejemplo: `/081-audit-step1-analyze 011-constitution-create-project`

**Nota**: El nombre puede ser con o sin extensión `.md`

## Output

**Carpeta:** `analisis_comando_slash/<nombre-comando>/procesos_previos/`

**Archivos generados:**
- `01_estructura_comando.json` - Frontmatter, fases, agentes, herramientas
- `02_procesos_por_fase.json` - Análisis detallado de cada fase
- `03_agentes.json` - Análisis de cada agente referenciado

---

## Estructura de Output

### 01_estructura_comando.json

```json
{
  "comando": "<nombre>",
  "version": "<versión si existe>",
  "descripcion": "<descripción del frontmatter>",
  "frontmatter": {
    "name": "<nombre>",
    "description": "<descripción>",
    "allowed_tools": ["Task", "Read", ...]
  },
  "total_fases": 12,
  "fases": [
    { "orden": 0, "nombre": "Validar MCP", "agente": "common/mcp-validator" },
    { "orden": 1, "nombre": "Preprocesar Input", "agente": null }
  ],
  "subagent_types": [
    "common/mcp-validator",
    "constitution/analyzer-agent"
  ],
  "herramientas_mcp": [
    "mcp__MCPEco__execution_session_manage",
    "mcp__MCPEco__create_project"
  ],
  "advertencias_criticas": [
    "FASE 8 debe ser SECUENCIAL para evitar race conditions"
  ]
}
```

### 02_procesos_por_fase.json

```json
{
  "comando": "<nombre>",
  "total_fases": 12,
  "fases": [
    {
      "fase": 0,
      "nombre": "Validar MCP",
      "proposito": "Verificar que el servidor MCP está disponible",
      "agente": "common/mcp-validator",
      "modelo": "haiku",
      "mcp_tools": ["mcp__MCPEco__list_documents"],
      "entrada": {
        "tipo": "ninguna",
        "campos": []
      },
      "salida": {
        "campos": ["status", "mcp_available", "message"]
      },
      "validaciones": ["MCP responde correctamente"],
      "errores_posibles": ["Tool not found", "Connection refused", "Timeout"]
    }
  ],
  "manejo_errores": {
    "estrategia": "try-catch por fase con logging",
    "output_error": { "success": false, "error_phase": "X", "error_message": "..." }
  },
  "funciones_auxiliares": [
    { "nombre": "generateConstitutionContent", "proposito": "..." }
  ]
}
```

### 03_agentes.json

```json
{
  "comando": "<nombre>",
  "total_agentes": 8,
  "agentes": [
    {
      "nombre": "mcp-validator",
      "archivo": ".claude/agents/common/mcp-validator.md",
      "modelo": "haiku",
      "responsabilidad": "Validar disponibilidad del servidor MCP",
      "tools_permitidos": ["mcp__MCPEco__list_documents"],
      "prohibiciones": ["NO usar Bash", "NO intentar solucionar errores"],
      "input": {
        "requiere_parametros": false
      },
      "output": {
        "exito": { "status": "ok", "mcp_available": true },
        "error": { "status": "error", "error_code": "...", "error_message": "..." }
      },
      "errores_posibles": ["Tool not found", "Connection refused"]
    }
  ],
  "resumen_modelos": {
    "opus": ["analyzer-agent", "project-creator-agent"],
    "sonnet": ["search-local", "document-loader"],
    "haiku": ["mcp-validator", "document-finder-agent"]
  }
}
```

---

## Flujo de Ejecución

### FASE -1: Inicializar TODO List

```typescript
await TodoWrite({
  todos: [
    { content: "Validar input", status: "pending", activeForm: "Validando input" },
    { content: "Analizar estructura del comando", status: "pending", activeForm: "Analizando estructura" },
    { content: "Analizar procesos por fase", status: "pending", activeForm: "Analizando procesos" },
    { content: "Analizar agentes referenciados", status: "pending", activeForm: "Analizando agentes" },
    { content: "Consolidar y guardar resultados", status: "pending", activeForm: "Guardando resultados" }
  ]
})
```

### FASE 0: Validar Input

```typescript
const COMANDO = $ARGUMENTS?.trim().replace('.md', '')

if (!COMANDO) {
  return {
    success: false,
    error_code: "INVALID_INPUT",
    error_message: "Nombre del comando requerido",
    usage: "/081-audit-step1-analyze <nombre-comando>"
  }
}

const COMANDO_PATH = `.claude/commands/${COMANDO}.md`

// Verificar que el comando existe
const exists = await Glob({ pattern: COMANDO_PATH })
if (exists.length === 0) {
  return {
    success: false,
    error_code: "COMMAND_NOT_FOUND",
    error_message: `No se encontró el comando: ${COMANDO_PATH}`,
    suggestion: "Verificar que el nombre sea correcto y el archivo exista"
  }
}

console.log(`📋 Analizando comando: ${COMANDO}`)
```

### FASE 1: Analizar Estructura del Comando

```typescript
const estructuraResult = await Task({
  subagent_type: "general-purpose",
  description: "Analizar estructura comando",
  prompt: `Lee el archivo .claude/commands/${COMANDO}.md y extrae:

  1. **Frontmatter**: name, description, allowed-tools
  2. **Fases**: Lista todas las fases con su número, nombre y agente asociado
  3. **Subagent_types**: Lista todos los agentes que invoca (ej: common/mcp-validator)
  4. **Herramientas MCP**: Lista todas las herramientas mcp__* que usa
  5. **Advertencias críticas**: Cualquier WARNING o CRÍTICO mencionado

  Retorna JSON con la estructura especificada en el comando.`
})
```

### FASE 2: Analizar Procesos por Fase

```typescript
const procesosResult = await Task({
  subagent_type: "general-purpose",
  description: "Analizar procesos por fase",
  prompt: `Analiza CADA FASE del comando .claude/commands/${COMANDO}.md

  Para cada fase extrae:
  - **proposito**: qué hace esta fase
  - **agente**: qué agente usa (si aplica)
  - **modelo**: qué modelo LLM usa el agente (haiku/sonnet/opus)
  - **mcp_tools**: herramientas MCP que usa
  - **entrada**: tipo y campos que recibe
  - **salida**: campos que produce
  - **validaciones**: qué valida antes/después
  - **errores_posibles**: qué puede fallar

  También extrae:
  - **manejo_errores**: estrategia global de errores
  - **funciones_auxiliares**: funciones helper definidas

  Retorna JSON estructurado.`
})
```

### FASE 3: Analizar Agentes

```typescript
// Obtener lista de agentes del análisis de estructura
const agentes = estructuraResult.subagent_types || []

const agentesAnalisis = []

for (const agente of agentes) {
  // Determinar ruta del agente
  const agentePath = `.claude/agents/${agente}.md`
  
  const agenteResult = await Task({
    subagent_type: "general-purpose",
    description: `Analizar agente ${agente}`,
    prompt: `Lee el archivo ${agentePath} y extrae:

    1. **Metadata**: nombre, modelo, versión
    2. **Responsabilidad**: qué hace exactamente (1-2 líneas)
    3. **Tools permitidos**: lista de herramientas que puede usar
    4. **Prohibiciones**: lista de lo que NO debe hacer
    5. **Input**: estructura JSON esperada
    6. **Output**: estructura JSON de éxito y error
    7. **Errores posibles**: tipos de error que puede retornar
    8. **Flujo**: pasos numerados de ejecución (si los tiene)

    Retorna JSON estructurado.`
  })
  
  agentesAnalisis.push({
    nombre: agente.split('/').pop(),
    archivo: agentePath,
    ...agenteResult
  })
}
```

### FASE 4: Consolidar y Guardar

```typescript
const OUTPUT_PATH = `analisis_comando_slash/${COMANDO}/procesos_previos`

// Crear carpeta
await Task({
  subagent_type: "general-purpose",
  description: "Crear carpeta output",
  prompt: `Crea la carpeta ${OUTPUT_PATH} si no existe usando Write.`
})

// Guardar 01_estructura_comando.json
await Write({
  file_path: `${OUTPUT_PATH}/01_estructura_comando.json`,
  content: JSON.stringify({
    comando: COMANDO,
    ...estructuraResult
  }, null, 2)
})

// Guardar 02_procesos_por_fase.json
await Write({
  file_path: `${OUTPUT_PATH}/02_procesos_por_fase.json`,
  content: JSON.stringify({
    comando: COMANDO,
    ...procesosResult
  }, null, 2)
})

// Guardar 03_agentes.json
await Write({
  file_path: `${OUTPUT_PATH}/03_agentes.json`,
  content: JSON.stringify({
    comando: COMANDO,
    total_agentes: agentesAnalisis.length,
    agentes: agentesAnalisis,
    resumen_modelos: agruparPorModelo(agentesAnalisis)
  }, null, 2)
})

console.log('\n' + '═'.repeat(60))
console.log('✅ ANÁLISIS PASO 1 COMPLETADO')
console.log(`📁 Resultados en: ${OUTPUT_PATH}`)
console.log(`📊 Fases analizadas: ${procesosResult.total_fases}`)
console.log(`🤖 Agentes analizados: ${agentesAnalisis.length}`)
console.log('═'.repeat(60))
```

---

## Manejo de Errores

### Si el comando no existe:

```json
{
  "success": false,
  "error_code": "COMMAND_NOT_FOUND",
  "error_message": "No se encontró el comando: .claude/commands/xxx.md",
  "suggestion": "Verificar que el nombre sea correcto"
}
```

### Si un agente no existe:

```typescript
// Registrar advertencia pero continuar
console.log(`⚠️ Agente no encontrado: ${agentePath} - Saltando`)
agentesAnalisis.push({
  nombre: agente,
  archivo: agentePath,
  error: "Archivo no encontrado"
})
```

### Si falla el análisis de una fase:

```typescript
// Registrar error pero continuar con siguiente fase
fasesAnalisis.push({
  fase: i,
  error: `No se pudo analizar: ${error.message}`
})
```

---

## Qué Analiza

| Elemento | Qué extrae |
|----------|------------|
| **Comando** | Frontmatter, fases, flujo, advertencias |
| **Fases** | Propósito, entrada/salida, validaciones, errores |
| **Agentes** | Responsabilidad, tools, prohibiciones, output |

## Qué NO Analiza

- No evalúa si el comando sigue mejores prácticas (eso es PASO 2)
- No modifica ningún archivo (eso es PASO 3)
- No ejecuta el comando real

---

## Siguiente Paso

```
/082-audit-step2-plan <nombre-comando>
```

Compara el análisis con mejores prácticas y genera plan de mejoras.
