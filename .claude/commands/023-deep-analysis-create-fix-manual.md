---
name: 023-deep-analysis-create-fix-manual
description: Crear fix manualmente - infiere parámetros desde BD
allowed-tools: Task, TodoWrite, MCPSearch, mcp__MCPEco__get_project_info, mcp__MCPEco__execution_session_manage, mcp__MCPEco__get_flow_row, mcp__MCPEco__get_story, mcp__MCPEco__get_task_details, mcp__MCPEco__list_work_items
---

# Deep Analysis: Crear Fix (Manual)

Wrapper para crear fixes manualmente. Infiere parámetros desde la BD.

## 📥 Input

**Task ID rechazada (OBLIGATORIO):** El task_id debe ser pasado como argumento del comando.

**Ejemplo**: `/023-deep-analysis-create-fix-manual task-1768611555348527000`

---

## 🔄 Flujo de Ejecución

### FASE -2: Inicializar TODO List

**PRIMERO**: Inicializar lista de tareas para visibilidad del progreso.

```typescript
// ✅ TODO list con 9 items para tracking de progreso
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "pending" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "pending" },
    { content: "Validar task ID", activeForm: "Validando task ID", status: "pending" },
    { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "pending" },
    { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "pending" },
    { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "pending" },
    { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})

console.log("✅ TODO list inicializado con 9 items")
```

### FASE -1: Cargar Herramientas MCP

**OBLIGATORIO**: Antes de ejecutar cualquier lógica, cargar TODAS las herramientas MCP requeridas.

```typescript
// ✅ Actualizar TODO: FASE -2 completada, FASE -1 in_progress
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "in_progress" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "pending" },
    { content: "Validar task ID", activeForm: "Validando task ID", status: "pending" },
    { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "pending" },
    { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "pending" },
    { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "pending" },
    { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})

// Cargar herramientas MCP explícitamente
await MCPSearch({ query: "select:mcp__MCPEco__get_project_info" })
await MCPSearch({ query: "select:mcp__MCPEco__execution_session_manage" })
await MCPSearch({ query: "select:mcp__MCPEco__get_flow_row" })
await MCPSearch({ query: "select:mcp__MCPEco__get_story" })
await MCPSearch({ query: "select:mcp__MCPEco__get_task_details" })
await MCPSearch({ query: "select:mcp__MCPEco__list_work_items" })

console.log("✅ Herramientas MCP cargadas correctamente")

// ✅ Actualizar TODO: FASE -1 completada, FASE 0 in_progress
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "in_progress" },
    { content: "Validar task ID", activeForm: "Validando task ID", status: "pending" },
    { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "pending" },
    { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "pending" },
    { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "pending" },
    { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})
```

### FASE 0: Validar MCP

```typescript
const mcpResult = await Task({
  subagent_type: "mcp-validator",
  description: "Validar MCP",
  prompt: "Valida MCP"
})

if (mcpResult.status !== "ok") {
  // ✅ Actualizar TODO: Error - marcar todo como completado
  await TodoWrite({
    todos: [
      { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
      { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
      { content: "Validar task ID", activeForm: "Validando task ID", status: "completed" },
      { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "completed" },
      { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "completed" },
      { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "completed" },
      { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "completed" },
      { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "completed" },
      { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
    ]
  })

  throw new Error("MCP no disponible")
}

// ✅ Actualizar TODO: FASE 0 completada, FASE 1 in_progress
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Validar task ID", activeForm: "Validando task ID", status: "in_progress" },
    { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "pending" },
    { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "pending" },
    { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "pending" },
    { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})
```

### FASE 1: Validar Task ID

```typescript
// Parsear argumentos (command-args viene como string)
const args = ARGUMENTS?.trim()

if (!args) {
  // ✅ Actualizar TODO: Error - marcar todo como completado
  await TodoWrite({
    todos: [
      { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
      { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
      { content: "Validar task ID", activeForm: "Validando task ID", status: "completed" },
      { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "completed" },
      { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "completed" },
      { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "completed" },
      { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "completed" },
      { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "completed" },
      { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
    ]
  })

  console.error("❌ Task ID requerido")
  console.error("Uso: /023-deep-analysis-create-fix-manual <task_id>")
  throw new Error("❌ ERROR: task_id es OBLIGATORIO. Uso: /023-deep-analysis-create-fix-manual <task_id>")
}

const taskId = args
console.log(`✅ Task ID recibido: ${taskId}`)

// ✅ Actualizar TODO: FASE 1 completada, FASE 2 in_progress
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Validar task ID", activeForm: "Validando task ID", status: "completed" },
    { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "in_progress" },
    { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "pending" },
    { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "pending" },
    { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})
```

### FASE 2: Obtener Task y Proyecto

```typescript
console.log("🔍 FASE 2: Obteniendo detalles de task rechazada...")

const taskResult = await mcp__MCPEco__get_task_details({ task_id: taskId })

if (!taskResult || !taskResult.task_id) {
  // ✅ Actualizar TODO: Error - marcar todo como completado
  await TodoWrite({
    todos: [
      { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
      { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
      { content: "Validar task ID", activeForm: "Validando task ID", status: "completed" },
      { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "completed" },
      { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "completed" },
      { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "completed" },
      { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "completed" },
      { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "completed" },
      { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
    ]
  })

  throw new Error("Task no encontrada: " + taskId)
}

const task = taskResult

// Obtener project_id desde la task
const projectId = task.project_id
if (!projectId) {
  // ✅ Actualizar TODO: Error - marcar todo como completado
  await TodoWrite({
    todos: [
      { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
      { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
      { content: "Validar task ID", activeForm: "Validando task ID", status: "completed" },
      { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "completed" },
      { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "completed" },
      { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "completed" },
      { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "completed" },
      { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "completed" },
      { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
    ]
  })

  throw new Error("Task no tiene project_id asociado")
}

// Obtener proyecto con get_project_info (incluye last_session_id)
const projectResult = await mcp__MCPEco__get_project_info({ project_id: projectId })

if (!projectResult || !projectResult.success || !projectResult.project_id) {
  // ✅ Actualizar TODO: Error - marcar todo como completado
  await TodoWrite({
    todos: [
      { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
      { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
      { content: "Validar task ID", activeForm: "Validando task ID", status: "completed" },
      { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "completed" },
      { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "completed" },
      { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "completed" },
      { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "completed" },
      { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "completed" },
      { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
    ]
  })

  throw new Error(`Proyecto no encontrado: ${projectId}`)
}

const project = projectResult

console.log(`✅ Task obtenida: ${task.task_id} (status: ${task.status})`)
console.log(`✅ Proyecto: ${project.project_name} (${project.project_level})`)

// ✅ Actualizar TODO: FASE 2 completada, FASE 2.5 in_progress
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Validar task ID", activeForm: "Validando task ID", status: "completed" },
    { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "completed" },
    { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "in_progress" },
    { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "pending" },
    { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})
```

### FASE 2.5: Inicializar Sesión de Ejecución

```typescript
let sessionId
let stepId

// Si el proyecto NO tiene sesión previa, crear una nueva
if (!project.last_session_id) {
  console.log("🔧 Creando nueva sesión de ejecución...")

  const sessionResult = await mcp__MCPEco__execution_session_manage({
    action: "start_session",
    command: "023-deep-analysis-create-fix-manual",
    provider: "claude",
    project_id: project.project_id,
    trigger_source: "cli",
    context_metadata: {
      task_id: taskId,
      task_status: task.status
    }
  })

  if (!sessionResult || !sessionResult.session_id) {
    throw new Error("❌ Error creando sesión de ejecución")
  }

  sessionId = sessionResult.session_id
  console.log(`✅ Sesión creada: ${sessionId}`)
} else {
  // Reusar sesión existente
  sessionId = project.last_session_id
  console.log(`♻️ Reusando sesión: ${sessionId}`)
}

// Iniciar nuevo step en la sesión
const stepResult = await mcp__MCPEco__execution_session_manage({
  action: "start_step",
  session_id: sessionId,
  step_name: "create-fix-manual",
  step_order: 1
})

if (!stepResult || !stepResult.step_id) {
  throw new Error("❌ Error creando step de ejecución")
}

stepId = stepResult.step_id
console.log(`✅ Step iniciado: ${stepId}`)

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: `🔍 FASE 2: Task obtenida exitosamente (status: ${task.status})`,
  log_level: "info"
})
```

### FASE 3: Validar Estado

```typescript
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: "🔍 FASE 3: Validando que la task esté rechazada...",
  log_level: "info"
})

if (task.status !== "rejected") {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: stepId,
    message: `❌ FASE 3: Task no está rechazada (status: ${task.status})`,
    log_level: "error"
  })

  // ✅ Actualizar TODO: Error - marcar todo como completado
  await TodoWrite({
    todos: [
      { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
      { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
      { content: "Validar task ID", activeForm: "Validando task ID", status: "completed" },
      { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "completed" },
      { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "completed" },
      { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "completed" },
      { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "completed" },
      { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "completed" },
      { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
    ]
  })

  console.error("❌ Task no está rechazada")
  console.error("Estado actual: " + task.status)
  throw new Error("Solo se pueden crear fixes para tasks rechazadas")
}

// Obtener rejected_by desde metadata
const rejectedBy = task.metadata?.rejected_by || "code_review"

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: `✅ FASE 3: Task validada como rechazada (rejected_by: ${rejectedBy})`,
  log_level: "info"
})
```

### FASE 4: Obtener Story y Flow Row

```typescript
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: "🔍 FASE 4: Obteniendo story y flow row desde la BD...",
  log_level: "info"
})

// Obtener story para conseguir flow_row_id
// NOTA: La tool correcta es get_story (NO get_story_details)
const storyResult = await mcp__MCPEco__get_story({ story_id: task.story_id })
const flowRowId = storyResult.flow_row_id

// Obtener flow_row para conseguir flow_id
const flowRowResult = await mcp__MCPEco__get_flow_row({ flow_row_id: flowRowId })
const flowId = flowRowResult.flow_id

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: `✅ FASE 4: Jerarquía obtenida (flow_id: ${flowId}, flow_row_id: ${flowRowId})`,
  log_level: "info"
})

// ✅ Actualizar TODO: FASE 4 completada, FASE 5 in_progress
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Validar task ID", activeForm: "Validando task ID", status: "completed" },
    { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "completed" },
    { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "completed" },
    { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "in_progress" },
    { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})
```

### FASE 5: Obtener Work Items (Issues)

```typescript
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: "🔍 FASE 5: Obteniendo work items (issues) de la task...",
  log_level: "info"
})

let issues = []

try {
  const workItemsResult = await mcp__MCPEco__list_work_items({ task_id: taskId })

  if (workItemsResult?.work_items) {
    issues = workItemsResult.work_items
      .filter(wi => wi.metadata?.issues)
      .flatMap(wi => wi.metadata.issues)
  }

  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: stepId,
    message: `✅ FASE 5: Issues obtenidos (${issues.length} encontrados)`,
    log_level: "info"
  })
} catch (e) {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: stepId,
    message: "⚠️ FASE 5: No se encontraron work items, continuando sin issues",
    log_level: "info"
  })
  // Sin issues, continuar igual
}

// ✅ Actualizar TODO: FASE 5 completada, FASE 6 in_progress
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Validar task ID", activeForm: "Validando task ID", status: "completed" },
    { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "completed" },
    { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "completed" },
    { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "completed" },
    { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "in_progress" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})
```

### FASE 6: Ejecutar Flujo de Fix

```typescript
// FASE 6.1: Sanity check
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: "🔍 FASE 6.1: Ejecutando sanity check para validar necesidad del fix...",
  log_level: "info"
})

const sanityResult = await Task({
  subagent_type: "sanity-check",
  description: "Validar necesidad",
  prompt: JSON.stringify({
    project_path: project?.project_path || ".",
    tech: project?.tech || "golang",
    issues: issues,
    circuit_breaker_data: null
  })
})

if (!sanityResult || sanityResult.status === "error") {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: stepId,
    message: `❌ FASE 6.1: Sanity check falló: ${sanityResult?.error_message || 'Respuesta inválida'}`,
    log_level: "error"
  })
  throw new Error(`Sanity check falló: ${sanityResult?.error_message}`)
}

if (sanityResult.status === "skip") {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: stepId,
    message: `✅ FASE 6.1: Sanity check completado - Fix no necesario: ${sanityResult.reason}`,
    log_level: "info"
  })
  console.log("ℹ️ Fix no necesario")
  return JSON.stringify({ success: true, skipped: true, reason: sanityResult.reason })
}

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: "✅ FASE 6.1: Sanity check completado - Fix es necesario",
  log_level: "info"
})

// FASE 6.2: Validar profundidad
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: "🔍 FASE 6.2: Validando profundidad de fix flow...",
  log_level: "info"
})

const depthResult = await Task({
  subagent_type: "depth-validator",
  description: "Validar profundidad",
  prompt: JSON.stringify({
    parent_flow_row_id: flowRowId,
    max_fix_depth: project?.config?.max_alt_flow_depth || 3
  })
})

if (!depthResult || depthResult.status === "error") {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: stepId,
    message: `❌ FASE 6.2: Depth validator falló: ${depthResult?.error_message || 'Respuesta inválida'}`,
    log_level: "error"
  })
  throw new Error(`Depth validator falló: ${depthResult?.error_message}`)
}

if (depthResult.status === "exceeded") {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: stepId,
    message: `❌ FASE 6.2: Profundidad máxima excedida (depth: ${depthResult.current_depth})`,
    log_level: "error"
  })
  return JSON.stringify({
    success: false,
    error: "MAX_DEPTH_EXCEEDED",
    current_depth: depthResult.current_depth
  })
}

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: `✅ FASE 6.2: Profundidad validada (depth: ${depthResult.current_depth})`,
  log_level: "info"
})

// FASE 6.3: Analizar causa raíz
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: "🔍 FASE 6.3: Analizando causa raíz del rechazo...",
  log_level: "info"
})

const rootCauseResult = await Task({
  subagent_type: "root-cause-analyzer",
  description: "Analizar causa raíz",
  prompt: JSON.stringify({
    task_id: taskId,
    rejected_by: rejectedBy,
    rejection_reason: task.metadata?.rejection_reason || "",
    severity_level: task.metadata?.severity_level || 5,
    issues: issues,
    tech: project?.tech || "golang",
    kind: project?.kind || "api"
  })
})

if (!rootCauseResult || !rootCauseResult.analysis) {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: stepId,
    message: `❌ FASE 6.3: Root cause analyzer falló: ${rootCauseResult?.error_message || 'Respuesta inválida'}`,
    log_level: "error"
  })
  throw new Error(`Root cause analyzer falló: ${rootCauseResult?.error_message}`)
}

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: "✅ FASE 6.3: Análisis de causa raíz completado",
  log_level: "info"
})

// FASE 6.4: Crear fix
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: "🔍 FASE 6.4: Creando fix flow row en la BD...",
  log_level: "info"
})

const fixResult = await Task({
  subagent_type: "fix-creator",
  description: "Crear fix",
  prompt: JSON.stringify({
    flow_id: flowId,
    parent_flow_row_id: flowRowId,
    task_id: taskId,
    rejected_by: rejectedBy,
    current_depth: depthResult.current_depth,
    root_cause_analysis: rootCauseResult.analysis,
    issues: issues
  })
})

if (!fixResult || !fixResult.fix || !fixResult.fix.fix_flow_row_id) {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: stepId,
    message: `❌ FASE 6.4: Fix creator falló: ${fixResult?.error_message || 'Respuesta inválida'}`,
    log_level: "error"
  })
  throw new Error(`Fix creator falló: ${fixResult?.error_message}`)
}

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: `✅ FASE 6.4: Fix creado exitosamente (${fixResult.fix.fix_name})`,
  log_level: "info"
})

// ✅ Actualizar TODO: FASE 6 completada, FASE 7 in_progress
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Validar task ID", activeForm: "Validando task ID", status: "completed" },
    { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "completed" },
    { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "completed" },
    { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "completed" },
    { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "completed" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "in_progress" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "pending" }
  ]
})
```

### FASE 7: Finalizar Step y Retornar Resultado

```typescript
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: `✅ FASE 7: Fix creado exitosamente (${fixResult.fix.fix_name}, depth: ${fixResult.fix.depth})`,
  log_level: "info"
})

console.log("═══════════════════════════════════════════════════════")
console.log("  ✅ FIX CREADO EXITOSAMENTE (Manual)")
console.log("═══════════════════════════════════════════════════════")
console.log(`🔧 Fix: ${fixResult.fix.fix_name}`)
console.log(`📏 Profundidad: ${fixResult.fix.depth}`)
console.log(`📋 Inferido desde task: ${taskId}`)

// Completar step de ejecución
await mcp__MCPEco__execution_session_manage({
  action: "complete_step",
  step_id: stepId,
  metadata: {
    mode: "manual",
    fix_flow_row_id: fixResult.fix.fix_flow_row_id,
    fix_story_id: fixResult.fix.fix_story_id,
    fix_name: fixResult.fix.fix_name,
    depth: fixResult.fix.depth,
    inferred_from_task_id: taskId
  }
})

console.log("✅ Step completado correctamente")

// Finalizar sesión
await mcp__MCPEco__execution_session_manage({
  action: "finish_session",
  session_id: sessionId,
  summary: `Fix creado manualmente: ${fixResult.fix.fix_name} (profundidad: ${fixResult.fix.depth}, inferido desde task: ${taskId})`
})

console.log("✅ Sesión finalizada")

// ✅ Actualizar TODO: TODAS las fases completadas
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Validar task ID", activeForm: "Validando task ID", status: "completed" },
    { content: "Obtener task y proyecto", activeForm: "Obteniendo task y proyecto", status: "completed" },
    { content: "Obtener jerarquía (story, flow row)", activeForm: "Obteniendo jerarquía", status: "completed" },
    { content: "Obtener work items (issues)", activeForm: "Obteniendo work items", status: "completed" },
    { content: "Ejecutar flujo de fix completo", activeForm: "Ejecutando flujo de fix", status: "completed" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "completed" },
    { content: "Retornar resultado", activeForm: "Retornando resultado", status: "completed" }
  ]
})

return JSON.stringify({
  success: true,
  mode: "manual",
  fix_flow_row_id: fixResult.fix.fix_flow_row_id,
  fix_story_id: fixResult.fix.fix_story_id,
  fix_name: fixResult.fix.fix_name,
  depth: fixResult.fix.depth,
  inferred_from: taskId,
  session_id: sessionId,
  step_id: stepId
}, null, 2)
```

---

## 📋 Diferencia con 022

| Aspecto | 022 (Auto) | 023 (Manual) |
|---------|------------|--------------|
| Invocador | Orquestador Go | Usuario |
| Parámetros | Todos explícitos | Solo task_id |
| Inferencia | No | Sí (desde BD) |

---

## 🧪 Testing

### Prueba Manual

```bash
# 1. Ejecutar con task_id válida rechazada
/023-deep-analysis-create-fix-manual task-1234567890123456789

# 2. Verificar resultado esperado:
# - JSON con success: true
# - fix_flow_row_id presente
# - fix_story_id presente
```

### Casos de Error Esperados

| Input | Error Esperado |
|-------|----------------|
| Sin argumentos | `task_id es OBLIGATORIO` |
| task_id inexistente | `Task no encontrada` |
| task no rechazada | `Solo se pueden crear fixes para tasks rechazadas` |
| profundidad máxima | `MAX_DEPTH_EXCEEDED` |

### Checklist Post-Ejecución

- [ ] Fix flow_row creado en BD
- [ ] Fix story asociada
- [ ] Deep analysis creado
- [ ] Jerarquía padre-hijo correcta

---

**Versión**: 3.0.0
**Última actualización**: 2026-01-17

## 📋 Changelog

### v3.0.0 (2026-01-17) - v2.0 Pattern Migration

**🔴 BREAKING CHANGES**:

1. **get_active_project → get_project_info**
   - Eliminado: `mcp__MCPEco__get_active_project`
   - Agregado: `mcp__MCPEco__get_project_info`
   - Razón: Eliminación de variables globales (active_project), uso explícito de project_id
   - Inferencia: project_id se obtiene desde `task.project_id`

2. **ARGUMENTS Parsing**
   - Cambiado de `context.commandArgs` a `ARGUMENTS`
   - Formato sigue siendo: `/023 <task_id>`

**✅ Nuevas features**:

1. **FASE -2: TODO List**
   - 9 items para visibilidad del progreso del usuario
   - Tracking completo en cada fase
   - TodoWrite agregado a allowed-tools

2. **MCPSearch Explícito**
   - FASE -1 carga explícitamente todas las tools MCP requeridas
   - Evita errores de "tool not found"

3. **TODO Updates en Early Exits**
   - Todos los early exits (FASE 0, 1, 2, 3) marcan TODOs como completed
   - Usuario siempre ve estado final del comando

4. **Proyecto desde Task**
   - project_id inferido desde task.project_id
   - Validación explícita de project_id antes de llamar get_project_info
   - Manejo robusto de errores si task no tiene project_id asociado

**🔧 Cambios técnicos**:

- Todas las referencias `activeProject` → `project`
- Segunda llamada a get_active_project eliminada de FASE 6
- Consistencia con comandos 021 (v2.1), 022 (v3.0), 031, 041, 042, 051, 061

**Sin impacto en**:
- Usuario final: mismo comando `/023 <task_id>`
- Invocadores: no hay invocadores automáticos (solo usuario manual)

---

### v2.2 (2026-01-16)
- Agregado logging completo con execution_session_manage (18 logs en 7 fases)
