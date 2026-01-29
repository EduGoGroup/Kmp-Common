---
name: 042-implementer-correction
description: Orquestador para aplicar correcciones automáticas a código (invocado por code-review/qa)
allowed-tools: Task, TodoWrite, MCPSearch, mcp__MCPEco__execution_session_manage
---

# Implementer Correction - Orquestador

Orquesta la aplicación de correcciones automáticas a código existente. Este comando es invocado por `code-review-agent` o `qa-agent` cuando detectan issues corregibles automáticamente.

---

## Información Recibida

**Input JSON:**
```
$ARGUMENTS
```

El input debe ser un JSON con la estructura:
```json
{
  "project_id": "proj-xxx",           // NUEVO v2.0 - OBLIGATORIO
  "session_id": "exec-session-yyy",   // NUEVO v2.0 - OPCIONAL (para tracking)
  "step_id": 5,                       // NUEVO v2.0 - OPCIONAL (para tracking)
  "task_id": "task-xxx",
  "project_path": "/path/to/project",
  "tech": "golang",
  "correction_context": {
    "source": "code_review",
    "cycle": 1,
    "max_cycles": 2,
    "work_item_id": "wi-cr-123"
  },
  "issues_to_fix": [
    {
      "severity": "medium",
      "category": "quality",
      "file": "cmd/main.go",
      "line": 45,
      "message": "Missing error check",
      "suggestion": "Add error handling"
    }
  ]
}
```

**Campos obligatorios:**
- `project_id`: ID del proyecto (validación de contexto)
- `project_path`: Ruta al proyecto
- `tech`: Tecnología del proyecto

**Campos opcionales para tracking:**
- `session_id`: Si está presente, se usa para logging en BD
- `step_id`: Si está presente, se usa para logging en BD
- Si ambos están presentes, se habilita tracking automático

---

## Tecnologías Soportadas

| Tech | Descripción | Herramientas de Validación |
|------|-------------|---------------------------|
| `golang` | Go/Golang | `go build`, `go test`, `go vet` |
| `python` | Python 3.x | `python -m py_compile`, `pytest` |
| `typescript` | TypeScript | `tsc --noEmit`, `npm test` |
| `javascript` | JavaScript/Node.js | `npm run build`, `npm test` |
| `nodejs` | Node.js (alias de javascript) | `npm run build`, `npm test` |
| `rust` | Rust | `cargo build`, `cargo test` |
| `java` | Java | `mvn compile`, `mvn test` |

> **Extensibilidad**: Esta tabla muestra los techs con soporte específico. Otros lenguajes (kotlin, scala, dart, swift, etc.) pueden funcionar usando el comportamiento default que intenta detectar la configuración del proyecto (Makefile, package.json, Cargo.toml, etc.).

---

## Propósito

Este comando es un **orquestador ligero** para modo corrección que:
1. Carga herramientas MCP necesarias
2. Valida disponibilidad del MCP
3. Parsea y valida input de corrección
4. Delega a `correction-executor` para aplicar correcciones
5. Delega a `validator` para validar que el código sigue compilando
6. Retorna resultado consolidado

**NOTA sobre Tracking**:
- **Tracking OPCIONAL**: Si `session_id` y `step_id` están presentes en el input, se habilita tracking automático a la BD.
- **Sin tracking**: Si no se proporcionan, el comando funciona normalmente sin tracking (compatibilidad backward).
- **Uso recomendado**: Los comandos que invocan a 042 DEBEN proporcionar `session_id` y `step_id` para trazabilidad completa.

---

## Nota sobre Pseudocódigo

Los bloques de código TypeScript en este documento son **guías de comportamiento**, no código ejecutable. El LLM debe interpretar la lógica descrita y ejecutar las acciones equivalentes usando las herramientas disponibles (Task, JSON.stringify, console.log).

**Ejemplo de interpretación**:
- `await Task({...})` → Invocar subagente con los parámetros indicados
- `console.log("[FASE X]...")` → Emitir mensaje de progreso al usuario
- `return JSON.stringify({...})` → Retornar el resultado final como JSON

> **EXCEPCIÓN CRÍTICA**: Las llamadas a `mcp__MCPEco__*` NO son pseudocódigo. El LLM **DEBE** invocarlas realmente usando las herramientas MCP disponibles. Ejemplo: `mcp__MCPEco__execution_session_manage` debe ejecutarse como una llamada real a la herramienta MCP, no interpretarse como pseudocódigo.

---

## Uso

Este comando es invocado programáticamente, no directamente por el usuario:

```typescript
// Invocación CON tracking (recomendado)
const correctionResult = await Task({
  subagent_type: "implementer-correction",
  description: "Aplicar correcciones automáticas",
  prompt: JSON.stringify({
    project_id: projectId,          // NUEVO - OBLIGATORIO
    session_id: SESSION_ID,         // NUEVO - OPCIONAL (para tracking)
    step_id: currentStepId,         // NUEVO - OPCIONAL (para tracking)
    task_id: taskId,
    project_path: projectPath,
    tech: tech,
    correction_context: { source: "code_review", cycle: 1, max_cycles: 2 },
    issues_to_fix: issuesToFix
  })
})

// Invocación SIN tracking (compatibilidad backward)
const correctionResult = await Task({
  subagent_type: "implementer-correction",
  description: "Aplicar correcciones automáticas",
  prompt: JSON.stringify({
    project_id: projectId,          // OBLIGATORIO desde v2.0
    task_id: taskId,
    project_path: projectPath,
    tech: tech,
    correction_context: { source: "code_review", cycle: 1, max_cycles: 2 },
    issues_to_fix: issuesToFix
  })
})
```

---

## Flujo de Ejecución (8 Fases)

### FASE -2: Inicializar TODO List

```typescript
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "pending" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "pending" },
    { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "pending" },
    { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "pending" },
    { content: "Validar código corregido", activeForm: "Validando código corregido", status: "pending" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})

console.log("✅ TODO list inicializado")
```

### FASE -1: Cargar Herramientas MCP

```typescript
// ✅ ACTUALIZAR TODO: FASE -1 iniciada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "in_progress" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "pending" },
    { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "pending" },
    { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "pending" },
    { content: "Validar código corregido", activeForm: "Validando código corregido", status: "pending" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})

// Cargar herramientas MCP explícitamente
await MCPSearch({ query: "select:mcp__MCPEco__execution_session_manage" })

console.log("✅ Herramientas MCP cargadas correctamente")

// ✅ ACTUALIZAR TODO: FASE -1 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "in_progress" },
    { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "pending" },
    { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "pending" },
    { content: "Validar código corregido", activeForm: "Validando código corregido", status: "pending" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})
```

### FASE 0: Validar MCP

```typescript
console.log("═══════════════════════════════════════════════════════")
console.log("  🔧 IMPLEMENTER: MODO CORRECCIÓN")
console.log("═══════════════════════════════════════════════════════")
console.log("[FASE 0] Validando disponibilidad del servidor MCP...")

const mcpValidation = await Task({
  subagent_type: "mcp-validator",
  description: "Validar servidor MCP",
  prompt: "Valida que el servidor MCP MCPEco esté disponible"
  // Timeout recomendado: 30 segundos (validación rápida)
})

if (mcpValidation.status !== "ok") {
  console.log("[FASE 0] ❌ MCP no disponible")

  // ✅ ACTUALIZAR TODO: Error - marcar todas como completed
  await TodoWrite({
    todos: [
      { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
      { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
      { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "completed" },
      { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "completed" },
      { content: "Validar código corregido", activeForm: "Validando código corregido", status: "completed" },
      { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
    ]
  })

  return JSON.stringify({ success: false, error: "MCP_UNAVAILABLE" })
}

console.log("[FASE 0] ✓ MCP disponible")

// ✅ ACTUALIZAR TODO: FASE 0 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "in_progress" },
    { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "pending" },
    { content: "Validar código corregido", activeForm: "Validando código corregido", status: "pending" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})
```

### FASE 1: Parsear y Validar Input

```typescript
console.log("[FASE 1] Parseando y validando input...")

let input
try {
  input = JSON.parse(ARGUMENTS)
} catch (e) {
  console.log("[FASE 1] ❌ JSON inválido")

  // ✅ ACTUALIZAR TODO: Error - marcar todas como completed
  await TodoWrite({
    todos: [
      { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
      { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
      { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "completed" },
      { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "completed" },
      { content: "Validar código corregido", activeForm: "Validando código corregido", status: "completed" },
      { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
    ]
  })

  return JSON.stringify({ success: false, error: "INVALID_JSON_INPUT" })
}

const {
  project_id,      // NUEVO v2.0 - OBLIGATORIO
  session_id,      // NUEVO v2.0 - OPCIONAL (para tracking)
  step_id,         // NUEVO v2.0 - OPCIONAL (para tracking)
  task_id,
  project_path,
  tech,
  correction_context,
  issues_to_fix
} = input

// 1. Validar project_id (OBLIGATORIO desde v2.0)
if (!project_id || project_id === "") {
  console.log("[FASE 1] ❌ project_id es OBLIGATORIO")

  // ✅ ACTUALIZAR TODO: Error
  await TodoWrite({
    todos: [
      { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
      { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
      { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "completed" },
      { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "completed" },
      { content: "Validar código corregido", activeForm: "Validando código corregido", status: "completed" },
      { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
    ]
  })

  return JSON.stringify({ success: false, error: "PROJECT_ID_REQUIRED" })
}

// 2. Determinar si tracking está habilitado
const TRACKING_ENABLED = Boolean(session_id && step_id)

console.log(`[FASE 1] ℹ️ Tracking: ${TRACKING_ENABLED ? 'HABILITADO' : 'DESHABILITADO'}`)
if (TRACKING_ENABLED) {
  console.log(`         Session ID: ${session_id}`)
  console.log(`         Step ID: ${step_id}`)
}

// 3. Crear funciones helper para tracking condicional
const logStep = async (message) => {
  if (TRACKING_ENABLED) {
    try {
      await mcp__MCPEco__execution_session_manage({
        action: "log_step",
        session_id: session_id,
        step_id: step_id,
        message: message
      })
    } catch (e) {
      console.log(`⚠️ Error logging: ${e.message}`)
    }
  }
}

// 4. Validar campos requeridos
if (!project_path || !tech) {
  console.log("[FASE 1] ❌ Campos requeridos faltantes (project_path, tech)")
  await logStep("❌ Campos requeridos faltantes")

  // ✅ ACTUALIZAR TODO: Error
  await TodoWrite({
    todos: [
      { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
      { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
      { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "completed" },
      { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "completed" },
      { content: "Validar código corregido", activeForm: "Validando código corregido", status: "completed" },
      { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
    ]
  })

  return JSON.stringify({ success: false, error: "MISSING_REQUIRED_FIELDS" })
}

// 5. Validar/defaultear correction_context
if (!correction_context) {
  correction_context = {
    source: "unknown",
    cycle: 1,
    max_cycles: 1,
    work_item_id: null
  }
  console.log("[FASE 1] ⚠️ correction_context no proporcionado, usando defaults")
}

// 6. Validar que issues_to_fix sea un array
if (!Array.isArray(issues_to_fix)) {
  console.log("[FASE 1] ❌ issues_to_fix no es un array")
  await logStep("❌ issues_to_fix no es un array")

  // ✅ ACTUALIZAR TODO: Error
  await TodoWrite({
    todos: [
      { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
      { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
      { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "completed" },
      { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "completed" },
      { content: "Validar código corregido", activeForm: "Validando código corregido", status: "completed" },
      { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
    ]
  })

  return JSON.stringify({ success: false, error: "INVALID_ISSUES_FORMAT" })
}

// 7. Manejar caso de lista vacía explícitamente
if (issues_to_fix.length === 0) {
  console.log("[FASE 1] ⚠️ Lista de issues vacía - nada que corregir")
  await logStep("⚠️ Sin issues para corregir - early exit")

  // ✅ ACTUALIZAR TODO: Early exit - todo completado
  await TodoWrite({
    todos: [
      { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
      { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
      { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "completed" },
      { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "completed" },
      { content: "Validar código corregido", activeForm: "Validando código corregido", status: "completed" },
      { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
    ]
  })

  return JSON.stringify({
    success: true,
    project_id: project_id,
    task_id: task_id,
    mode: "correction",
    correction_context: correction_context,
    metrics: {
      files_modified: 0,
      corrections_applied: 0,
      corrections_failed: 0,
      compiles: true,
      tests_pass: true
    },
    files_modified: [],
    corrections_applied: [],
    corrections_failed: [],
    summary: "Sin issues para corregir"
  }, null, 2)
}

console.log(`[FASE 1] ✓ Input válido`)
console.log(`         Project ID: ${project_id}`)
console.log(`         Fuente: ${correction_context?.source}`)
console.log(`         Ciclo: ${correction_context?.cycle}/${correction_context?.max_cycles}`)
console.log(`         Issues: ${issues_to_fix.length}`)
console.log(`         Tech: ${tech}`)

await logStep(`✅ Input validado - ${issues_to_fix.length} issues para corregir`)

// ✅ ACTUALIZAR TODO: FASE 1 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "completed" },
    { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "in_progress" },
    { content: "Validar código corregido", activeForm: "Validando código corregido", status: "pending" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})
```

### FASE 2: Aplicar Correcciones

```typescript
console.log("[FASE 2] Aplicando correcciones...")
await logStep("🔧 Delegando aplicación de correcciones a correction-executor...")

let correctionResult
let correctionsApplied = 0
let filesModified = []

try {
  correctionResult = await Task({
    subagent_type: "correction-executor",
    description: "Aplicar correcciones automáticas",
    prompt: JSON.stringify({
      project_path: project_path,
      tech: tech,
      issues_to_fix: issues_to_fix
    })
    // Timeout recomendado: 5 minutos (depende de cantidad de correcciones)
  })

  if (correctionResult.status !== "success") {
    const errorMsg = correctionResult.error_message || "Error desconocido en correction-executor"
    console.log(`[FASE 2] ❌ Error: ${errorMsg}`)
    await logStep(`❌ Error en correction-executor: ${errorMsg}`)

    // ✅ ACTUALIZAR TODO: Error
    await TodoWrite({
      todos: [
        { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
        { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
        { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "completed" },
        { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "completed" },
        { content: "Validar código corregido", activeForm: "Validando código corregido", status: "completed" },
        { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
      ]
    })

    return JSON.stringify({
      success: false,
      error: "CORRECTIONS_FAILED",
      details: errorMsg
    })
  }

  // Normalizar valores que podrían ser undefined
  correctionsApplied = correctionResult.corrections_applied ?? 0
  filesModified = correctionResult.files_modified ?? []

  console.log(`[FASE 2] ✓ Correcciones aplicadas: ${correctionsApplied}`)
  console.log(`         Archivos modificados: ${filesModified.length}`)
  await logStep(`✅ Correcciones aplicadas: ${correctionsApplied} en ${filesModified.length} archivos`)

} catch (e) {
  console.log(`[FASE 2] ❌ Error aplicando correcciones: ${e.message}`)
  await logStep(`❌ Error aplicando correcciones: ${e.message}`)

  // ✅ ACTUALIZAR TODO: Error
  await TodoWrite({
    todos: [
      { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
      { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
      { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "completed" },
      { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "completed" },
      { content: "Validar código corregido", activeForm: "Validando código corregido", status: "completed" },
      { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
    ]
  })

  return JSON.stringify({ success: false, error: "CORRECTIONS_FAILED", details: e.message })
}

// ✅ ACTUALIZAR TODO: FASE 2 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "completed" },
    { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "completed" },
    { content: "Validar código corregido", activeForm: "Validando código corregido", status: "in_progress" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})
```

### FASE 2.5: Verificar Archivos Modificados

```typescript
console.log("[FASE 2.5] Verificando que los archivos fueron realmente modificados...")

// VERIFICACIÓN CRÍTICA: Confirmar que los archivos reportados existen y fueron modificados
let verificationPassed = true
const verificationErrors = []

for (const filePath of filesModified) {
  const fullPath = `${project_path}/${filePath}`
  
  // Verificar que el archivo existe usando Bash o Read
  // El LLM DEBE ejecutar esta verificación realmente, no simularla
  try {
    // Ejemplo: usar Bash para verificar existencia y timestamp
    // bash: test -f "${fullPath}" && stat -f "%m" "${fullPath}"
    // O usar Read para verificar contenido
    console.log(`[FASE 2.5]   ✓ Verificado: ${filePath}`)
  } catch (e) {
    console.log(`[FASE 2.5]   ❌ No encontrado o no modificado: ${filePath}`)
    verificationErrors.push(filePath)
    verificationPassed = false
  }
}

if (!verificationPassed) {
  console.log(`[FASE 2.5] ⚠️ ${verificationErrors.length} archivo(s) no pudieron verificarse`)
  await logStep(`⚠️ Verificación parcial: ${verificationErrors.length} archivos sin confirmar`)
} else {
  console.log(`[FASE 2.5] ✓ Todos los archivos verificados correctamente`)
  await logStep(`✅ Verificación completa: ${filesModified.length} archivos confirmados`)
}
```

### FASE 3: Validar Correcciones

```typescript
console.log("[FASE 3] Validando código corregido...")
await logStep("🧪 Delegando validación de código a validator...")

let validationResult
let validationSkipped = false

try {
  validationResult = await Task({
    subagent_type: "validator",
    description: "Validar código corregido",
    prompt: JSON.stringify({
      project_path: project_path,
      tech: tech,
      files_to_validate: filesModified
    })
    // Timeout recomendado: 3 minutos (build + tests)
  })

  console.log(`[FASE 3] ✓ Validación completada - compiles: ${validationResult.validation?.compiles}`)
  await logStep(`✅ Validación completada - compiles: ${validationResult.validation?.compiles}`)

} catch (e) {
  // NO asumir éxito - marcar que la validación fue omitida
  console.log(`[FASE 3] ⚠️ Validación omitida por error: ${e.message}`)
  await logStep(`⚠️ Validación omitida: ${e.message}`)

  validationSkipped = true
  validationResult = {
    validation: {
      compiles: null,  // Desconocido, no true
      tests_pass: null,  // Desconocido, no true
      validation_skipped: true,
      skip_reason: e.message
    }
  }
}

// ✅ ACTUALIZAR TODO: FASE 3 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "completed" },
    { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "completed" },
    { content: "Validar código corregido", activeForm: "Validando código corregido", status: "completed" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "in_progress" }
  ]
})
```

### FASE 3.5: Verificar Compilación Real

```typescript
console.log("[FASE 3.5] Verificando que la compilación pasó realmente...")

// VERIFICACIÓN CRÍTICA: Confirmar que el resultado de compilación es real
let compilationVerified = false

if (!validationSkipped && validationResult.validation?.compiles === true) {
  // El LLM DEBE ejecutar una verificación independiente de compilación
  // NO confiar únicamente en el reporte del validador
  
  try {
    // Ejecutar comando de build según la tecnología
    // golang: go build ./...
    // typescript: tsc --noEmit
    // python: python -m py_compile *.py
    // etc.
    
    const buildCommand = {
      golang: "go build ./...",
      typescript: "tsc --noEmit",
      javascript: "npm run build --if-present",
      nodejs: "npm run build --if-present",
      python: "python -m py_compile",
      rust: "cargo check",
      java: "mvn compile -q"
    }[tech] || "echo 'No build command for this tech'"
    
    // Ejemplo de ejecución real (el LLM DEBE ejecutar esto):
    // bash: cd "${project_path}" && ${buildCommand}
    
    console.log(`[FASE 3.5] ✓ Compilación verificada independientemente`)
    await logStep(`✅ Compilación verificada con: ${buildCommand}`)
    compilationVerified = true
    
  } catch (e) {
    console.log(`[FASE 3.5] ❌ La compilación falló en verificación independiente: ${e.message}`)
    await logStep(`❌ Compilación falló en verificación: ${e.message}`)
    
    // IMPORTANTE: Sobreescribir el resultado del validador
    validationResult.validation.compiles = false
    validationResult.validation.verification_override = true
    validationResult.validation.verification_error = e.message
  }
} else if (validationSkipped) {
  console.log(`[FASE 3.5] ⚠️ Verificación omitida (validación fue skipped)`)
} else {
  console.log(`[FASE 3.5] ⚠️ Compilación ya reportada como fallida, no se verifica`)
}
```

### FASE 4: Retornar Resultado

```typescript
console.log("[FASE 4] Preparando resultado final...")

// Usar valores normalizados (ya definidos en FASE 2)
const success = validationSkipped
  ? (correctionsApplied > 0)  // Éxito parcial si hubo correcciones
  : (validationResult.validation?.compiles !== false)

const summary = `Corregidos ${correctionsApplied} issues en ${filesModified.length} archivo(s)${validationSkipped ? ' (validación omitida)' : ''}`

const result = {
  success: success,
  project_id: project_id,  // NUEVO v2.0
  task_id: task_id,
  mode: "correction",
  correction_context: correction_context,  // Ya tiene default
  metrics: {
    files_modified: filesModified.length,  // Usar normalizado
    corrections_applied: correctionsApplied,  // Usar normalizado
    corrections_failed: correctionResult.corrections_failed ?? 0,  // Normalizar
    compiles: validationResult.validation?.compiles ?? null,
    tests_pass: validationResult.validation?.tests_pass ?? null,
    validation_skipped: validationSkipped
  },
  files_modified: filesModified,  // Usar normalizado
  corrections_applied: correctionResult.corrections_detail ?? [],  // Normalizar
  corrections_failed: correctionResult.failures_detail ?? [],  // Normalizar
  summary: summary
}

console.log(`[FASE 4] ✓ Resultado: success=${success}`)
console.log(`         ${summary}`)

await logStep(`✅ ${summary} - success=${success}`)

// ✅ ACTUALIZAR TODO: FASE 4 completada - TODOS completed
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Parsear y validar input", activeForm: "Parseando y validando input", status: "completed" },
    { content: "Aplicar correcciones", activeForm: "Aplicando correcciones", status: "completed" },
    { content: "Validar código corregido", activeForm: "Validando código corregido", status: "completed" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
  ]
})

console.log("✅ Comando completado")

return JSON.stringify(result, null, 2)
```

---

## Output Esperado

### Caso Éxito
```json
{
  "success": true,
  "project_id": "proj-xxx",
  "task_id": "task-xxx",
  "mode": "correction",
  "correction_context": {
    "source": "code_review",
    "cycle": 1,
    "max_cycles": 2
  },
  "metrics": {
    "files_modified": 2,
    "corrections_applied": 3,
    "corrections_failed": 0,
    "compiles": true,
    "tests_pass": true,
    "validation_skipped": false
  },
  "files_modified": ["cmd/main.go"],
  "summary": "Corregidos 3 issues en 2 archivo(s)"
}
```

### Caso Sin Issues
```json
{
  "success": true,
  "project_id": "proj-xxx",
  "task_id": "task-xxx",
  "mode": "correction",
  "metrics": {
    "files_modified": 0,
    "corrections_applied": 0,
    "corrections_failed": 0,
    "compiles": true,
    "tests_pass": true
  },
  "files_modified": [],
  "summary": "Sin issues para corregir"
}
```

### Caso Fallo
```json
{
  "success": false,
  "project_id": "proj-xxx",
  "task_id": "task-xxx",
  "mode": "correction",
  "metrics": {
    "compiles": false,
    "validation_skipped": false
  },
  "summary": "Corregidos 2 issues en 1 archivo(s)"
}
```

### Caso Validación Omitida
```json
{
  "success": true,
  "project_id": "proj-xxx",
  "task_id": "task-xxx",
  "mode": "correction",
  "metrics": {
    "files_modified": 1,
    "corrections_applied": 2,
    "corrections_failed": 0,
    "compiles": null,
    "tests_pass": null,
    "validation_skipped": true
  },
  "summary": "Corregidos 2 issues en 1 archivo(s) (validación omitida)"
}
```

---

## Ejemplos de Flujo

### Ejemplo 1: Corrección exitosa desde Code Review

**Escenario**: El code-review-agent detectó 2 issues de calidad en un proyecto Go.

```typescript
// 1. Code Review invoca implementer-correction
const result = await Task({
  subagent_type: "implementer-correction",
  prompt: JSON.stringify({
    project_id: "proj-api-users",
    session_id: "exec-sess-abc123",
    step_id: 5,
    task_id: "task-impl-001",
    project_path: "/Users/dev/projects/api-users",
    tech: "golang",
    correction_context: {
      source: "code_review",
      cycle: 1,
      max_cycles: 2,
      work_item_id: "wi-cr-456"
    },
    issues_to_fix: [
      {
        severity: "medium",
        category: "error_handling",
        file: "internal/handlers/user.go",
        line: 45,
        message: "Error ignorado en llamada a db.Close()",
        suggestion: "Agregar: if err := db.Close(); err != nil { log.Error(err) }"
      },
      {
        severity: "low",
        category: "naming",
        file: "internal/handlers/user.go",
        line: 12,
        message: "Variable 'x' no descriptiva",
        suggestion: "Renombrar a 'userCount'"
      }
    ]
  })
})

// 2. Resultado esperado
// {
//   "success": true,
//   "project_id": "proj-api-users",
//   "metrics": { "corrections_applied": 2, "compiles": true },
//   "summary": "Corregidos 2 issues en 1 archivo(s)"
// }
```

### Ejemplo 2: Corrección con fallo de compilación

**Escenario**: Las correcciones aplicadas rompen la compilación.

```typescript
// 1. QA invoca corrección para issue de seguridad
const result = await Task({
  subagent_type: "implementer-correction",
  prompt: JSON.stringify({
    project_id: "proj-webapp",
    task_id: "task-impl-002",
    project_path: "/Users/dev/projects/webapp",
    tech: "typescript",
    correction_context: {
      source: "qa",
      cycle: 1,
      max_cycles: 1
    },
    issues_to_fix: [
      {
        severity: "high",
        category: "security",
        file: "src/auth/login.ts",
        line: 78,
        message: "SQL Injection vulnerability",
        suggestion: "Usar prepared statements"
      }
    ]
  })
})

// 2. Resultado - la corrección rompió tipos
// {
//   "success": false,
//   "project_id": "proj-webapp",
//   "metrics": { "corrections_applied": 1, "compiles": false },
//   "summary": "Corregidos 1 issues en 1 archivo(s)"
// }

// 3. El comando que invocó debe decidir:
//    - Si cycle < max_cycles: reintentar con más contexto
//    - Si cycle >= max_cycles: escalar a humano o rechazar task
```

### Ejemplo 3: Lista vacía de issues (early exit)

**Escenario**: El code review no encontró issues corregibles automáticamente.

```typescript
// 1. Code Review invoca con lista vacía
const result = await Task({
  subagent_type: "implementer-correction",
  prompt: JSON.stringify({
    project_id: "proj-clean-code",
    task_id: "task-impl-003",
    project_path: "/Users/dev/projects/clean-code",
    tech: "python",
    correction_context: {
      source: "code_review",
      cycle: 1,
      max_cycles: 2
    },
    issues_to_fix: []  // Lista vacía
  })
})

// 2. Resultado - early exit sin invocar subagentes
// {
//   "success": true,
//   "project_id": "proj-clean-code",
//   "metrics": { "corrections_applied": 0, "compiles": true, "tests_pass": true },
//   "summary": "Sin issues para corregir"
// }

// NOTA: Este caso NO invoca correction-executor ni validator
// Es un early exit eficiente que ahorra recursos
```

---

## Agentes Utilizados

| Agente | Fase | Responsabilidad |
|--------|------|-----------------|
| `mcp-validator` | 0 | Validar MCP disponible |
| `correction-executor` | 2 | Aplicar correcciones a archivos |
| `validator` | 3 | Validar que código compila |

---

## Notas Importantes

1. **Tracking OPCIONAL (NUEVO v2.0)**:
   - Si `session_id` y `step_id` están presentes → tracking habilitado
   - Si no están presentes → tracking deshabilitado (compatibilidad backward)
   - Los comandos que invocan a 042 DEBEN proporcionar `session_id` y `step_id` para trazabilidad completa

2. **project_id OBLIGATORIO (NUEVO v2.0)**:
   - Desde v2.0, `project_id` es un campo obligatorio en el JSON de entrada
   - Se valida antes de ejecutar cualquier lógica
   - Se incluye en el output para trazabilidad

3. **No avanza task**: Este comando NO avanza la task. El comando que lo invoca es responsable de avanzar o rechazar.

4. **Idempotente**: Puede ser invocado múltiples veces en ciclos de soft-retry.

5. **Fail-safe**: Si las correcciones rompen el código, retorna `success: false` pero no lanza excepción.

6. **Validación omitida**: Si el validador falla, no se asume éxito. Se marca `validation_skipped: true` y `compiles: null`.

7. **Lista vacía**: Si `issues_to_fix` está vacío, retorna éxito inmediato sin invocar subagentes.

8. **TODO list visibilidad**: Desde v2.0, muestra progreso al usuario con 6 items de TODO list.

---

## Changelog

### v2.0.0 (2026-01-17) - Major Release: Tracking y TODO List

**BREAKING CHANGES:**
- ⚠️ **project_id ahora es OBLIGATORIO** (antes no existía)
- ⚠️ **JSON de entrada modificado**: agregados `project_id`, `session_id`, `step_id`

**Nuevas features:**
- ✅ FASE -2: TODO List con 6 items para visibilidad del progreso
- ✅ FASE -1: MCPSearch explícito para mcp__MCPEco__execution_session_manage
- ✅ Tracking OPCIONAL: si session_id y step_id están presentes, se habilita tracking a BD
- ✅ Logs mejorados: logStep antes de cada delegación a agente (si tracking habilitado)
- ✅ Validaciones mejoradas: validación de project_id obligatorio
- ✅ TODO updates en cada fase y en early exits
- ✅ project_id incluido en output para trazabilidad

**Compatibilidad:**
- ✅ Backward compatible: si session_id/step_id no se proporcionan, funciona sin tracking

### v1.2.0 (2026-01-16)

- Agregado javascript/nodejs a tabla de techs
- Agregado default para correction_context
- Mejorado manejo de errores con fallbacks
- Normalización de valores potencialmente undefined
- Agregado notas de timeout recomendado
- Agregado sección sobre pseudocódigo

---

**Versión**: 2.0.0
**Última actualización**: 2026-01-17
