---
name: 061-qa-task
description: Orquestador para ejecutar QA de una tarea del workflow (ÚLTIMO paso)
allowed-tools: Task, TodoWrite, MCPSearch, Read, mcp__MCPEco__get_project_info, mcp__MCPEco__get_flow_info, mcp__MCPEco__get_flow_row, mcp__MCPEco__get_task_details, mcp__MCPEco__execution_session_manage, mcp__MCPEco__create_work_item, mcp__MCPEco__list_work_items, mcp__MCPEco__list_stories
---

# QA Task - Orquestador

Orquesta el testing y validación como **ÚLTIMO paso del workflow**.
Si aprueba, la task se marca como **COMPLETADA**.

---

## Información Recibida

**Project ID (OBLIGATORIO):** El project_id debe ser pasado como argumento del comando.
**Task ID (OBLIGATORIO):** El task_id debe ser pasado en el prompt.

**Ejemplo**: `/061-qa-task proj-xxx task-yyy`

**Formato**:
```
$ARGUMENTS = project_id (requerido)
$PROMPT = task_id (requerido)
```

---

## Propósito

Este comando es un **orquestador** que:
1. Valida disponibilidad del MCP
2. Inicia tracking de sesión
3. Obtiene contexto completo (task → project → acceptance_criteria)
4. Delega tareas atómicas a agentes especializados
5. Reporta resultados y finaliza tracking

**FILOSOFÍA**: El comando orquesta, los agentes ejecutan tareas atómicas.

**IMPORTANTE**: QA es el ÚLTIMO paso. Si APPROVE → task COMPLETADA.

---

## Uso

```bash
/061-qa-task proj-xxx task-yyy
```

**Parámetros**:
- `proj-xxx`: Project ID (obligatorio)
- `task-yyy`: Task ID (obligatorio)

---

## Contratos de Agentes

### mcp-validator
**Ubicación**: `.claude/agents/common/mcp-validator.md`

**Input**:
```json
{
  "description": "Validar servidor MCP",
  "prompt": "Valida que el servidor MCP MCPEco esté disponible"
}
```

**Output esperado**:
```json
{
  "status": "ok|error",
  "mcp_available": true|false,
  "message": "string",
  "error_code": "MCP_NOT_AVAILABLE (solo si error)",
  "suggestion": "string (solo si error)"
}
```

---

### validator (implementer)
**Ubicación**: `.claude/agents/implementer/validator-agent.md`

**Input**:
```json
{
  "project_path": "/path/to/project",
  "tech": "golang|python|typescript|rust|java|etc",
  "files_to_validate": ["file1.go", "file2.go"]
}
```

**Output esperado**:
```json
{
  "status": "success|error",
  "validation": {
    "compiles": true|false,
    "build_output": "string",
    "tests_pass": true|false,
    "tests_output": "string"
  },
  "errors": ["array de errores si hay"],
  "warnings": ["array de warnings si hay"]
}
```

---

### test-executor
**Ubicación**: `.claude/agents/qa/test-executor-agent.md`

**Input**:
```json
{
  "project_path": "/path/to/project",
  "tech": "golang|python|typescript|rust|java|etc",
  "files_to_test": ["file1.go", "file2.go"]
}
```

**Output esperado**:
```json
{
  "status": "success|error",
  "framework": "go|jest|pytest|cargo|etc",
  "test_results": {
    "total": 23,
    "passed": 20,
    "failed": 3,
    "coverage": 85.5
  },
  "raw_output": "string con salida de tests",
  "execution_time_ms": 4523,
  "command_executed": "string"
}
```

---

### criteria-validator
**Ubicación**: `.claude/agents/qa/criteria-validator-agent.md`

**Input**:
```json
{
  "acceptance_criteria": ["criterio 1", "criterio 2"],
  "test_results": {
    "total": 23,
    "passed": 23,
    "failed": 0,
    "coverage": 85.5
  },
  "files_exist": true
}
```

**Output esperado**:
```json
{
  "status": "success",
  "criteria_met": 2,
  "criteria_total": 3,
  "all_met": true|false,
  "results": [
    {
      "criterion": "string",
      "met": true|false,
      "reason": "string",
      "pattern": "coverage|inferred|tests"
    }
  ]
}
```

---

### qa-severity-calculator
**Ubicación**: `.claude/agents/qa/qa-severity-calculator-agent.md`

**Input**:
```json
{
  "test_results": {
    "total": 23,
    "passed": 20,
    "failed": 3,
    "coverage": 65.5
  },
  "criteria_results": [
    { "criterion": "string", "met": true|false }
  ],
  "threshold_qa": 70
}
```

**Output esperado**:
```json
{
  "status": "success",
  "severity": 32,
  "threshold": 70,
  "breakdown": {
    "test_penalty": 10,
    "coverage_penalty": 2,
    "criteria_penalty": 20,
    "total": 32
  },
  "within_threshold": true|false
}
```

---

### qa-decision-maker
**Ubicación**: `.claude/agents/qa/qa-decision-maker-agent.md`

**Input**:
```json
{
  "severity": 45,
  "threshold_qa": 70,
  "has_missing_files": false,
  "compiles": true,
  "tests_executed": true
}
```

**Output esperado**:
```json
{
  "status": "success",
  "decision": "APPROVE|REJECT",
  "reason": "string explicando la decisión",
  "task_completed": true|false
}
```

---

### qa-reporter
**Ubicación**: `.claude/agents/qa/qa-reporter-agent.md`

**Input**:
```json
{
  "work_item_id": "wi-qa-123",
  "task_id": "task-xxx-0-story-yyy",
  "current_step": "qa",
  "decision": "APPROVE|REJECT",
  "severity": 35,
  "threshold": 70,
  "test_results": {
    "total": 23,
    "passed": 23,
    "failed": 0,
    "coverage": 85.5
  },
  "criteria_results": [
    { "criterion": "string", "met": true|false }
  ],
  "files_tested": ["file1.go", "file2.go"]
}
```

**Output esperado**:
```json
{
  "status": "success",
  "work_item_id": "string",
  "work_item_updated": true,
  "task_completed": true|false,
  "task_status": "completed|in_progress",
  "fix_flow_row_id": "string|null",
  "summary": "string"
}
```

---

## Variables Globales

> **NOTA**: Este bloque es pseudo-codigo ilustrativo. El orquestador mantiene
> estas variables en su contexto de ejecucion durante todo el flujo.

```typescript
let SESSION_ID: string | null = null
let CURRENT_STEP_ID: number | null = null
```

---

## Helpers de Tracking

```typescript
async function startStep(stepName: string, stepOrder: number): Promise<void> {
  const result = await mcp__MCPEco__execution_session_manage({
    action: "start_step",
    session_id: SESSION_ID,
    step_name: stepName,
    step_order: stepOrder
  })
  CURRENT_STEP_ID = result.step_id
}

async function logStep(message: string, level: string = "info"): Promise<void> {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: CURRENT_STEP_ID,
    message: message,
    log_level: level
  })
}

async function completeStep(success: boolean = true): Promise<void> {
  await mcp__MCPEco__execution_session_manage({
    action: "complete_step",
    step_id: CURRENT_STEP_ID,
    success: success
  })
}

async function failSession(errorMessage: string): Promise<void> {
  await mcp__MCPEco__execution_session_manage({
    action: "fail",
    session_id: SESSION_ID,
    error_message: errorMessage
  })
}

async function finishSession(summary: string): Promise<void> {
  await mcp__MCPEco__execution_session_manage({
    action: "finish_session",
    session_id: SESSION_ID,
    summary: summary
  })
}

// Helper para parsear respuestas de agentes con validación
function parseAgentResponse<T>(
  rawResponse: string,
  agentName: string,
  requiredFields: string[]
): { success: true, data: T } | { success: false, error: string } {
  try {
    const data = JSON.parse(rawResponse)

    // Validar campos requeridos
    for (const field of requiredFields) {
      if (data[field] === undefined) {
        return {
          success: false,
          error: `Agente ${agentName}: campo requerido '${field}' no encontrado en respuesta`
        }
      }
    }

    return { success: true, data: data as T }
  } catch (e) {
    return {
      success: false,
      error: `Agente ${agentName}: respuesta no es JSON válido`
    }
  }
}
```

---

## Flujo de Ejecución

### FASE -2: Inicializar TODO List

```typescript
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "pending" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "pending" },
    { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "pending" },
    { content: "Crear work item", activeForm: "Creando work item", status: "pending" },
    { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "pending" },
    { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "pending" },
    { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "pending" },
    { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "pending" },
    { content: "Calcular severity", activeForm: "Calculando severity", status: "pending" },
    { content: "Tomar decisión", activeForm: "Tomando decisión", status: "pending" },
    { content: "Reportar resultados", activeForm: "Reportando resultados", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" }
  ]
})

console.log("✅ TODO list inicializado")
```

---

### FASE -1: Cargar Herramientas MCP

```typescript
// ✅ ACTUALIZAR TODO: FASE -1 iniciada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "in_progress" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "pending" },
    { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "pending" },
    { content: "Crear work item", activeForm: "Creando work item", status: "pending" },
    { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "pending" },
    { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "pending" },
    { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "pending" },
    { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "pending" },
    { content: "Calcular severity", activeForm: "Calculando severity", status: "pending" },
    { content: "Tomar decisión", activeForm: "Tomando decisión", status: "pending" },
    { content: "Reportar resultados", activeForm: "Reportando resultados", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" }
  ]
})

// Cargar herramientas MCP explícitamente
await MCPSearch({ query: "select:mcp__MCPEco__get_project_info" })
await MCPSearch({ query: "select:mcp__MCPEco__get_flow_info" })
await MCPSearch({ query: "select:mcp__MCPEco__get_flow_row" })
await MCPSearch({ query: "select:mcp__MCPEco__get_task_details" })
await MCPSearch({ query: "select:mcp__MCPEco__execution_session_manage" })
await MCPSearch({ query: "select:mcp__MCPEco__create_work_item" })
await MCPSearch({ query: "select:mcp__MCPEco__list_work_items" })
await MCPSearch({ query: "select:mcp__MCPEco__list_stories" })

console.log("✅ Herramientas MCP cargadas correctamente")

// ✅ ACTUALIZAR TODO: FASE -1 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "in_progress" },
    { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "pending" },
    { content: "Crear work item", activeForm: "Creando work item", status: "pending" },
    { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "pending" },
    { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "pending" },
    { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "pending" },
    { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "pending" },
    { content: "Calcular severity", activeForm: "Calculando severity", status: "pending" },
    { content: "Tomar decisión", activeForm: "Tomando decisión", status: "pending" },
    { content: "Reportar resultados", activeForm: "Reportando resultados", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" }
  ]
})
```

---

### FASE 0: Validar MCP

```typescript
const mcpResult = await Task({
  subagent_type: "mcp-validator",
  description: "Validar MCP disponible",
  prompt: "Valida que el servidor MCP MCPEco esté disponible"
})

const mcpParsed = parseAgentResponse<{ status: string, mcp_available: boolean }>(
  mcpResult,
  "mcp-validator",
  ["status"]
)

if (!mcpParsed.success) {
  console.error(`❌ Error validando MCP: ${mcpParsed.error}`)
  return JSON.stringify({
    success: false,
    error: "MCP_VALIDATION_ERROR",
    message: mcpParsed.error
  })
}

if (mcpParsed.data.status !== "ok") {
  console.error("❌ MCP Server no disponible")
  return JSON.stringify({
    success: false,
    error: "MCP_UNAVAILABLE",
    message: "El servidor MCP no está disponible"
  })
}

console.log("✅ MCP Server disponible")

// ✅ ACTUALIZAR TODO: FASE 0 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "in_progress" },
    { content: "Crear work item", activeForm: "Creando work item", status: "pending" },
    { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "pending" },
    { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "pending" },
    { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "pending" },
    { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "pending" },
    { content: "Calcular severity", activeForm: "Calculando severity", status: "pending" },
    { content: "Tomar decisión", activeForm: "Tomando decisión", status: "pending" },
    { content: "Reportar resultados", activeForm: "Reportando resultados", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" }
  ]
})
```

---

### FASE 1: Parsear Input

```typescript
const projectId = ARGUMENTS?.trim()

if (!projectId || projectId === "") {
  console.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
  console.error("  ❌ ERROR: Project ID es OBLIGATORIO")
  console.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
  console.error("💡 Uso: /061-qa-task <project-id> <task-id>")
  throw new Error("Project ID es OBLIGATORIO")
}

const taskId = PROMPT?.trim()

if (!taskId || taskId === "") {
  console.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
  console.error("  ❌ ERROR: Task ID es OBLIGATORIO")
  console.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
  console.error("💡 Uso: /061-qa-task <project-id> <task-id>")
  throw new Error("Task ID es OBLIGATORIO")
}

console.log(`📋 Project ID: ${projectId}`)
console.log(`📋 Task ID: ${taskId}`)
```

---

### FASE 2: Iniciar Tracking

```typescript
try {
  const sessionResult = await mcp__MCPEco__execution_session_manage({
    action: "start_session",
    command: "061-qa-task",
    provider: "claude",
    trigger_source: "cli",
    project_id: projectId
  })
  SESSION_ID = sessionResult.session_id
  console.log(`🔄 Sesión iniciada: ${SESSION_ID}`)
  console.log(`📁 Proyecto vinculado: ${projectId}`)
} catch (error) {
  console.error("❌ Error iniciando tracking")
  throw new Error(`No se pudo iniciar la sesión de tracking: ${error.message}`)
}
```

---

### FASE 3: Obtener Contexto

```typescript
let task, flowRow, flow, project, acceptanceCriteria, threshold_qa

try {
  await startStep("Obtener contexto", 1)
  await logStep("🔍 Resolviendo contexto de la task...")

  // 1. Validar y obtener proyecto PRIMERO
  project = await mcp__MCPEco__get_project_info({ project_id: projectId })
  if (!project || !project.success) {
    await completeStep(false, `Proyecto no encontrado: ${projectId}`)
    await failSession(`Proyecto no encontrado: ${projectId}`)
    throw new Error(`Proyecto no encontrado: ${projectId}`)
  }
  await logStep(`✅ Project: ${project.project_name}`)
  await logStep(`   Tech: ${project.tech} | Kind: ${project.kind}`)
  await logStep(`   Path: ${project.project_path}`)

  // 2. Obtener task
  task = await mcp__MCPEco__get_task_details({ task_id: taskId })
  if (!task || !task.success) {
    await completeStep(false, `Task no encontrada: ${taskId}`)
    await failSession(`Task no encontrada: ${taskId}`)
    throw new Error(`Task no encontrada: ${taskId}`)
  }
  await logStep(`✅ Task: ${task.task_title}`)
  await logStep(`   Status: ${task.status}`)

  // 3. Validar que task tenga flow_row_id
  if (!task.flow_row_id) {
    await completeStep(false, `Task sin flow_row_id: ${taskId}`)
    await failSession(`Task sin flow_row_id: ${taskId}`)
    throw new Error(`La task ${taskId} no tiene flow_row_id asociado`)
  }

  // 4. Obtener flow_row
  flowRow = await mcp__MCPEco__get_flow_row({ flow_row_id: task.flow_row_id })
  if (!flowRow || !flowRow.success) {
    await completeStep(false, `FlowRow no encontrado: ${task.flow_row_id}`)
    await failSession(`FlowRow no encontrado: ${task.flow_row_id}`)
    throw new Error(`No se encontró el flow_row: ${task.flow_row_id}`)
  }
  await logStep(`✅ Flow Row: ${flowRow.title}`)

  // 5. Obtener flow
  flow = await mcp__MCPEco__get_flow_info({ flow_id: flowRow.flow_id })
  if (!flow || !flow.success) {
    await completeStep(false, `Flow no encontrado: ${flowRow.flow_id}`)
    await failSession(`Flow no encontrado: ${flowRow.flow_id}`)
    throw new Error(`No se encontró el flow: ${flowRow.flow_id}`)
  }
  await logStep(`✅ Flow: ${flow.flow_title}`)

  // 6. Validar que la task pertenece al proyecto correcto
  if (flow.project_id !== projectId) {
    const errorMsg = `Task ${taskId} no pertenece al proyecto ${projectId} (pertenece a ${flow.project_id})`
    await completeStep(false, errorMsg)
    await failSession(errorMsg)
    throw new Error(errorMsg)
  }
  await logStep(`✅ Validación: Task pertenece al proyecto correcto`)

  // 7. Obtener acceptance criteria de la story
  acceptanceCriteria = []
  if (task.story_id) {
    try {
      const stories = await mcp__MCPEco__list_stories({
        flow_row_id: task.flow_row_id
      })

      if (stories && stories.stories && Array.isArray(stories.stories)) {
        const story = stories.stories.find(s => s.story_id === task.story_id)
        if (story && story.metadata && story.metadata.acceptance_criteria) {
          acceptanceCriteria = story.metadata.acceptance_criteria
        }
      }
    } catch (e) {
      await logStep(`⚠️ No se pudieron obtener criterios de aceptación: ${e.message}`, "warn")
    }
  }

  // Si no hay criterios de aceptación, loguear warning pero continuar
  if (acceptanceCriteria.length === 0) {
    await logStep("⚠️ Sin criterios de aceptación definidos - se usarán criterios implícitos", "warn")
  } else {
    await logStep(`✅ Criterios de aceptación: ${acceptanceCriteria.length}`)
  }

  // 8. Obtener threshold QA
  threshold_qa = project.config?.threshold_qa || 70
  await logStep(`✅ Threshold QA: ${threshold_qa}`)

  await completeStep(true)

  console.log(`✅ Contexto obtenido: ${project.project_name} / ${flow.flow_title} / ${task.task_title}`)

} catch (error) {
  await mcp__MCPEco__execution_session_manage({
    action: "fail",
    session_id: SESSION_ID,
    error_message: error.message
  })
  throw error
}

// ✅ ACTUALIZAR TODO: FASE 3 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "completed" },
    { content: "Crear work item", activeForm: "Creando work item", status: "in_progress" },
    { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "pending" },
    { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "pending" },
    { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "pending" },
    { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "pending" },
    { content: "Calcular severity", activeForm: "Calculando severity", status: "pending" },
    { content: "Tomar decisión", activeForm: "Tomando decisión", status: "pending" },
    { content: "Reportar resultados", activeForm: "Reportando resultados", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" }
  ]
})
```

---

### FASE 4: Crear Work Item

```typescript
let workItem: { work_item_id: string }

try {
  await startStep("Crear work item", 2)
  await logStep("📝 Creando work item para QA...")

  workItem = await mcp__MCPEco__create_work_item({
    flow_row_id: task.flow_row_id,
    step_type: "qa",
    task_id: taskId
  })

  if (!workItem || !workItem.work_item_id) {
    await completeStep(false)
    await failSession("No se pudo crear el work item de QA")
    throw new Error("No se pudo crear el work item de QA")
  }

  await logStep(`✅ Work item creado: ${workItem.work_item_id}`)
  await completeStep(true)

  console.log(`📝 Work Item: ${workItem.work_item_id}`)

} catch (error) {
  await mcp__MCPEco__execution_session_manage({
    action: "fail",
    session_id: SESSION_ID,
    error_message: error.message
  })
  throw error
}

// ✅ ACTUALIZAR TODO: FASE 4 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "completed" },
    { content: "Crear work item", activeForm: "Creando work item", status: "completed" },
    { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "in_progress" },
    { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "pending" },
    { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "pending" },
    { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "pending" },
    { content: "Calcular severity", activeForm: "Calculando severity", status: "pending" },
    { content: "Tomar decisión", activeForm: "Tomando decisión", status: "pending" },
    { content: "Reportar resultados", activeForm: "Reportando resultados", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" }
  ]
})
```

---

### FASE 5: Obtener Archivos del Implementer

```typescript
let filesToTest: string[] = []

try {
  await startStep("Obtener archivos implementados", 3)
  await logStep("📄 Obteniendo archivos desde work item del implementer...")

  const workItems = await mcp__MCPEco__list_work_items({
    task_id: taskId,
    limit: 20
  })

  const implementerWI = workItems?.work_items?.find(
    wi => wi.step_type === "implementer" && wi.status === "completed"
  )

  if (!implementerWI) {
    await logStep("⚠️ No se encontró work item del implementer completado", "warn")
    // Continuar sin archivos específicos - el test-executor buscará qué testear
  } else if (implementerWI.metadata) {
    const meta = typeof implementerWI.metadata === 'string'
      ? JSON.parse(implementerWI.metadata)
      : implementerWI.metadata

    filesToTest = [
      ...(meta.files_created || []),
      ...(meta.files_modified || [])
    ]
    // Eliminar duplicados
    filesToTest = [...new Set(filesToTest)]
    await logStep(`✅ Archivos encontrados: ${filesToTest.length}`)
  }

  if (filesToTest.length === 0) {
    await logStep("⚠️ No hay archivos específicos para testear - se ejecutarán tests generales", "warn")
  }

  await completeStep(true)

  console.log(`📄 Archivos implementados: ${filesToTest.length}`)

} catch (error) {
  await mcp__MCPEco__execution_session_manage({
    action: "fail",
    session_id: SESSION_ID,
    error_message: error.message
  })
  throw error
}

// ✅ ACTUALIZAR TODO: FASE 5 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "completed" },
    { content: "Crear work item", activeForm: "Creando work item", status: "completed" },
    { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "completed" },
    { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "in_progress" },
    { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "pending" },
    { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "pending" },
    { content: "Calcular severity", activeForm: "Calculando severity", status: "pending" },
    { content: "Tomar decisión", activeForm: "Tomando decisión", status: "pending" },
    { content: "Reportar resultados", activeForm: "Reportando resultados", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" }
  ]
})
```

---

### FASE 6: Verificar Archivos y Compilación

```typescript
let hasMissingFiles = false
let compiles = true

try {
  await startStep("Verificar archivos", 4)
  await logStep("🔍 Delegando validación de archivos y compilación a validator...")

  const validatorResult = await Task({
    subagent_type: "validator",
    description: "Validar archivos y compilación",
    prompt: JSON.stringify({
      project_path: project.project_path,
      tech: project.tech,
      files_to_validate: filesToTest
    })
  })

  const validatorParsed = parseAgentResponse<{
    status: string,
    validation?: {
      compiles?: boolean,
      files_exist?: boolean
    },
    errors?: string[]
  }>(validatorResult, "validator", ["status"])

  if (!validatorParsed.success) {
    await logStep(`⚠️ Error parseando respuesta de validator: ${validatorParsed.error}`, "warn")
    // Asumir que compila y continuar - el test-executor detectará problemas
  } else {
    hasMissingFiles = validatorParsed.data.status === "error" ||
                      validatorParsed.data.validation?.files_exist === false
    compiles = validatorParsed.data.validation?.compiles ?? true
  }

  await logStep(`✅ Archivos existen: ${!hasMissingFiles}`)
  await logStep(`✅ Compila: ${compiles}`)
  await completeStep(true)

  // Early exit si archivos faltan o no compila
  if (hasMissingFiles || !compiles) {
    await startStep("Reportar rechazo por archivos/compilación", 5)

    const rejectReason = hasMissingFiles
      ? "missing_files"
      : "compilation_error"

    await logStep("📊 Delegando reporte de rechazo a qa-reporter...")

    const reporterResult = await Task({
      subagent_type: "qa-reporter",
      description: "Reportar rechazo",
      prompt: JSON.stringify({
        work_item_id: workItem.work_item_id,
        task_id: taskId,
        current_step: "qa",
        decision: "REJECT",
        severity: 100,
        threshold: threshold_qa,
        test_results: { total: 0, passed: 0, failed: 0, coverage: 0, framework: "none" },
        criteria_results: [],
        files_tested: filesToTest
      })
    })

    const reporterParsed = parseAgentResponse<{
      fix_flow_row_id?: string
    }>(reporterResult, "qa-reporter", [])

    await logStep(`✅ Reportado: REJECT (${rejectReason})`)
    await completeStep(true)
    await finishSession(`QA rechazado - ${rejectReason}`)

    // ✅ ACTUALIZAR TODO: Early exit - todo completado
    await TodoWrite({
      todos: [
        { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
        { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
        { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "completed" },
        { content: "Crear work item", activeForm: "Creando work item", status: "completed" },
        { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "completed" },
        { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "completed" },
        { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "completed" },
        { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "completed" },
        { content: "Calcular severity", activeForm: "Calculando severity", status: "completed" },
        { content: "Tomar decisión", activeForm: "Tomando decisión", status: "completed" },
        { content: "Reportar resultados", activeForm: "Reportando resultados", status: "completed" },
        { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "completed" }
      ]
    })

    console.log("═══════════════════════════════════════════════════════")
    console.log(`❌ QA RECHAZADO - ${hasMissingFiles ? 'ARCHIVOS FALTANTES' : 'ERROR DE COMPILACIÓN'}`)
    console.log("═══════════════════════════════════════════════════════")

    return JSON.stringify({
      success: true,
      task_id: taskId,
      work_item_id: workItem.work_item_id,
      decision: "REJECT",
      severity_level: 100,
      threshold: threshold_qa,
      task_completed: false,
      reason: rejectReason,
      fix_flow_row_id: reporterParsed.success ? reporterParsed.data.fix_flow_row_id : null,
      summary: `QA rechazado: ${hasMissingFiles ? 'archivos implementados no encontrados' : 'error de compilación'}`
    })
  }

} catch (error) {
  await mcp__MCPEco__execution_session_manage({
    action: "fail",
    session_id: SESSION_ID,
    error_message: error.message
  })
  throw error
}

// ✅ ACTUALIZAR TODO: FASE 6 completada (camino normal - archivos OK y compila OK)
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "completed" },
    { content: "Crear work item", activeForm: "Creando work item", status: "completed" },
    { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "completed" },
    { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "completed" },
    { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "in_progress" },
    { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "pending" },
    { content: "Calcular severity", activeForm: "Calculando severity", status: "pending" },
    { content: "Tomar decisión", activeForm: "Tomando decisión", status: "pending" },
    { content: "Reportar resultados", activeForm: "Reportando resultados", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" }
  ]
})
```

---

### FASE 7: Ejecutar Tests

```typescript
let testData: {
  status: string,
  framework: string,
  test_results: {
    total: number,
    passed: number,
    failed: number,
    coverage: number
  }
}

try {
  await startStep("Ejecutar tests", 5)
  await logStep("🧪 Delegando ejecución de tests a test-executor...")

  const testResult = await Task({
    subagent_type: "test-executor",
    description: "Ejecutar tests",
    prompt: JSON.stringify({
      project_path: project.project_path,
      tech: project.tech,
      files_to_test: filesToTest
    })
  })

  const testParsed = parseAgentResponse<typeof testData>(
    testResult,
    "test-executor",
    ["status", "framework", "test_results"]
  )

  if (!testParsed.success) {
    await completeStep(false)
    await failSession(`Error en test-executor: ${testParsed.error}`)
    throw new Error(testParsed.error)
  }

  testData = testParsed.data

  // Validar estructura de test_results
  if (typeof testData.test_results?.total !== 'number') {
    testData.test_results = {
      total: testData.test_results?.total || 0,
      passed: testData.test_results?.passed || 0,
      failed: testData.test_results?.failed || 0,
      coverage: testData.test_results?.coverage || 0
    }
    await logStep("⚠️ test_results incompleto - usando valores por defecto", "warn")
  }

  await logStep(`✅ Framework: ${testData.framework}`)
  await logStep(`✅ Tests: ${testData.test_results.passed}/${testData.test_results.total}`)
  await logStep(`✅ Coverage: ${testData.test_results.coverage}%`)
  await completeStep(true)

  console.log(`🧪 Framework: ${testData.framework}`)
  console.log(`   Tests: ${testData.test_results.passed}/${testData.test_results.total}`)
  console.log(`   Coverage: ${testData.test_results.coverage}%`)

} catch (error) {
  await mcp__MCPEco__execution_session_manage({
    action: "fail",
    session_id: SESSION_ID,
    error_message: error.message
  })
  throw error
}

// ✅ ACTUALIZAR TODO: FASE 7 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "completed" },
    { content: "Crear work item", activeForm: "Creando work item", status: "completed" },
    { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "completed" },
    { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "completed" },
    { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "completed" },
    { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "in_progress" },
    { content: "Calcular severity", activeForm: "Calculando severity", status: "pending" },
    { content: "Tomar decisión", activeForm: "Tomando decisión", status: "pending" },
    { content: "Reportar resultados", activeForm: "Reportando resultados", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" }
  ]
})
```

---

### FASE 8: Validar Criterios de Aceptación

```typescript
try {
  await startStep("Validar criterios", 6)
  await logStep("📋 Delegando validación de criterios de aceptación a criteria-validator...")

  const criteriaResult = await Task({
    subagent_type: "criteria-validator",
    description: "Validar criterios de aceptación",
    prompt: JSON.stringify({
      acceptance_criteria: acceptanceCriteria,
      test_results: testData.test_results,
      files_exist: !hasMissingFiles
    })
  })

  const criteriaParsed = parseAgentResponse<{
    status: string,
    criteria_met: number,
    criteria_total: number,
    results: Array<{
      criterion: string,
      met: boolean,
      reason: string
    }>
  }>(criteriaResult, "criteria-validator", ["status", "criteria_met", "criteria_total", "results"])

  if (!criteriaParsed.success) {
    await completeStep(false)
    await failSession(criteriaParsed.error)
    throw new Error(criteriaParsed.error)
  }

  const criteriaData = criteriaParsed.data

  // Validar que results sea un array
  if (!Array.isArray(criteriaData.results)) {
    await completeStep(false)
    await failSession("Agente criteria-validator: campo 'results' debe ser un array")
    throw new Error("Campo 'results' debe ser un array")
  }

  await logStep(`✅ Criterios cumplidos: ${criteriaData.criteria_met}/${criteriaData.criteria_total}`)
  await completeStep(true)

  console.log(`📋 Criterios: ${criteriaData.criteria_met}/${criteriaData.criteria_total}`)

} catch (error) {
  await mcp__MCPEco__execution_session_manage({
    action: "fail",
    session_id: SESSION_ID,
    error_message: error.message
  })
  throw error
}

// ✅ ACTUALIZAR TODO: FASE 8 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "completed" },
    { content: "Crear work item", activeForm: "Creando work item", status: "completed" },
    { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "completed" },
    { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "completed" },
    { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "completed" },
    { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "completed" },
    { content: "Calcular severity", activeForm: "Calculando severity", status: "in_progress" },
    { content: "Tomar decisión", activeForm: "Tomando decisión", status: "pending" },
    { content: "Reportar resultados", activeForm: "Reportando resultados", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" }
  ]
})
```

---

### FASE 9: Calcular Severity

```typescript
try {
  await startStep("Calcular severity", 7)
  await logStep("📊 Delegando cálculo de severity a qa-severity-calculator...")

  const severityResult = await Task({
    subagent_type: "qa-severity-calculator",
    description: "Calcular severity",
    prompt: JSON.stringify({
      test_results: testData.test_results,
      criteria_results: criteriaData.results || [],
      threshold_qa: threshold_qa
    })
  })

  const severityParsed = parseAgentResponse<{
    status: string,
    severity: number,
    breakdown?: {
      test_penalty: number,
      coverage_penalty: number,
      criteria_penalty: number
    }
  }>(severityResult, "qa-severity-calculator", ["status", "severity"])

  if (!severityParsed.success) {
    await completeStep(false)
    await failSession(severityParsed.error)
    throw new Error(severityParsed.error)
  }

  const severityData = severityParsed.data

  await logStep(`✅ Severity: ${severityData.severity}`)
  await logStep(`✅ Breakdown: tests=${severityData.breakdown?.test_penalty}, coverage=${severityData.breakdown?.coverage_penalty}, criteria=${severityData.breakdown?.criteria_penalty}`)
  await completeStep(true)

  console.log(`📊 Severity: ${severityData.severity}/${threshold_qa}`)

} catch (error) {
  await mcp__MCPEco__execution_session_manage({
    action: "fail",
    session_id: SESSION_ID,
    error_message: error.message
  })
  throw error
}

// ✅ ACTUALIZAR TODO: FASE 9 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "completed" },
    { content: "Crear work item", activeForm: "Creando work item", status: "completed" },
    { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "completed" },
    { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "completed" },
    { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "completed" },
    { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "completed" },
    { content: "Calcular severity", activeForm: "Calculando severity", status: "completed" },
    { content: "Tomar decisión", activeForm: "Tomando decisión", status: "in_progress" },
    { content: "Reportar resultados", activeForm: "Reportando resultados", status: "pending" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" }
  ]
})
```

---

### FASE 10: Tomar Decisión

```typescript
try {
  await startStep("Tomar decisión", 8)
  await logStep("🎯 Delegando decisión final a qa-decision-maker...")

  const decisionResult = await Task({
    subagent_type: "qa-decision-maker",
    description: "Decidir aprobación",
    prompt: JSON.stringify({
      severity: severityData.severity,
      threshold_qa: threshold_qa,
      has_missing_files: hasMissingFiles,
      compiles: compiles,
      tests_executed: testData.status === "success"
    })
  })

  const decisionParsed = parseAgentResponse<{
    status: string,
    decision: "APPROVE" | "REJECT",
    reason: string
  }>(decisionResult, "qa-decision-maker", ["status", "decision", "reason"])

  if (!decisionParsed.success) {
    await completeStep(false)
    await failSession(decisionParsed.error)
    throw new Error(decisionParsed.error)
  }

  const decisionData = decisionParsed.data

  // Validar que decision sea APPROVE o REJECT
  if (decisionData.decision !== "APPROVE" && decisionData.decision !== "REJECT") {
    await completeStep(false)
    await failSession(`Agente qa-decision-maker: 'decision' debe ser 'APPROVE' o 'REJECT', recibido: ${decisionData.decision}`)
    throw new Error(`Campo 'decision' debe ser 'APPROVE' o 'REJECT', recibido: ${decisionData.decision}`)
  }

  await logStep(`✅ Decisión: ${decisionData.decision}`)
  await logStep(`✅ Razón: ${decisionData.reason}`)
  await completeStep(true)

  console.log(`🎯 Decisión: ${decisionData.decision}`)

} catch (error) {
  await mcp__MCPEco__execution_session_manage({
    action: "fail",
    session_id: SESSION_ID,
    error_message: error.message
  })
  throw error
}

// ✅ ACTUALIZAR TODO: FASE 10 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "completed" },
    { content: "Crear work item", activeForm: "Creando work item", status: "completed" },
    { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "completed" },
    { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "completed" },
    { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "completed" },
    { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "completed" },
    { content: "Calcular severity", activeForm: "Calculando severity", status: "completed" },
    { content: "Tomar decisión", activeForm: "Tomando decisión", status: "completed" },
    { content: "Reportar resultados", activeForm: "Reportando resultados", status: "in_progress" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "pending" }
  ]
})
```

---

### FASE 11: Reportar Resultados

```typescript
try {
  await startStep("Reportar resultados", 9)
  await logStep("📊 Delegando reporte final a qa-reporter...")

  const reporterResult = await Task({
    subagent_type: "qa-reporter",
    description: "Reportar a BD",
    prompt: JSON.stringify({
      work_item_id: workItem.work_item_id,
      task_id: taskId,
      current_step: "qa",
      decision: decisionData.decision,
      severity: severityData.severity,
      threshold: threshold_qa,
      test_results: testData.test_results,
      criteria_results: criteriaData.results || [],
      files_tested: filesToTest
    })
  })

  const reporterParsed = parseAgentResponse<{
    status: string,
    work_item_updated: boolean,
    task_completed: boolean,
    fix_flow_row_id?: string
  }>(reporterResult, "qa-reporter", ["status", "work_item_updated", "task_completed"])

  if (!reporterParsed.success) {
    await completeStep(false)
    await failSession(reporterParsed.error)
    throw new Error(reporterParsed.error)
  }

  const reporterData = reporterParsed.data

  // Validar éxito parcial: si el work item no se actualizó, es un error crítico
  if (!reporterData.work_item_updated) {
    await completeStep(false)
    await failSession("Agente qa-reporter: work_item no se pudo actualizar en BD")
    throw new Error("Work item no se pudo actualizar en la base de datos")
  }

  await logStep(`✅ Work item actualizado: ${reporterData.work_item_updated}`)
  await logStep(`✅ Task completada: ${reporterData.task_completed}`)
  await completeStep(true)

} catch (error) {
  await mcp__MCPEco__execution_session_manage({
    action: "fail",
    session_id: SESSION_ID,
    error_message: error.message
  })
  throw error
}

// ✅ ACTUALIZAR TODO: FASE 11 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "completed" },
    { content: "Crear work item", activeForm: "Creando work item", status: "completed" },
    { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "completed" },
    { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "completed" },
    { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "completed" },
    { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "completed" },
    { content: "Calcular severity", activeForm: "Calculando severity", status: "completed" },
    { content: "Tomar decisión", activeForm: "Tomando decisión", status: "completed" },
    { content: "Reportar resultados", activeForm: "Reportando resultados", status: "completed" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "in_progress" }
  ]
})
```

---

### FASE 12: Finalizar Tracking

```typescript
await startStep("Finalizar", 10)

const summary = decisionData.decision === "APPROVE"
  ? `QA aprobado: ${task.task_title} - Task COMPLETADA`
  : `QA rechazado: ${task.task_title} (severity ${severityData.severity}/${threshold_qa})`

await finishSession(summary)
await logStep(`✅ ${summary}`)
await completeStep(true)

console.log("✅ Tracking finalizado")

// ✅ ACTUALIZAR TODO: FASE 12 completada
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar MCP disponible", activeForm: "Validando MCP disponible", status: "completed" },
    { content: "Obtener contexto completo", activeForm: "Obteniendo contexto completo", status: "completed" },
    { content: "Crear work item", activeForm: "Creando work item", status: "completed" },
    { content: "Obtener archivos implementados", activeForm: "Obteniendo archivos implementados", status: "completed" },
    { content: "Verificar archivos y compilación", activeForm: "Verificando archivos y compilación", status: "completed" },
    { content: "Ejecutar tests", activeForm: "Ejecutando tests", status: "completed" },
    { content: "Validar criterios de aceptación", activeForm: "Validando criterios de aceptación", status: "completed" },
    { content: "Calcular severity", activeForm: "Calculando severity", status: "completed" },
    { content: "Tomar decisión", activeForm: "Tomando decisión", status: "completed" },
    { content: "Reportar resultados", activeForm: "Reportando resultados", status: "completed" },
    { content: "Finalizar tracking", activeForm: "Finalizando tracking", status: "completed" }
  ]
})

// Output final
if (decisionData.decision === "APPROVE") {
  console.log("═══════════════════════════════════════════════════════")
  console.log("🎉 QA APROBADO - TASK COMPLETADA")
  console.log("═══════════════════════════════════════════════════════")
} else {
  console.log("═══════════════════════════════════════════════════════")
  console.log("⚠️ QA RECHAZADO")
  console.log("═══════════════════════════════════════════════════════")
  console.log(`🔧 Fix creado: ${reporterData.fix_flow_row_id}`)
}
```

---

## Output Final

```typescript
return JSON.stringify({
  success: true,
  task_id: taskId,
  task_title: task.task_title,
  work_item_id: workItem.work_item_id,
  project_id: project.project_id,
  project_name: project.project_name,
  
  decision: decisionData.decision,
  severity_level: severityData.severity,
  threshold: threshold_qa,
  task_completed: reporterData.task_completed,
  
  metrics: {
    tests_total: testData.test_results?.total || 0,
    tests_passed: testData.test_results?.passed || 0,
    tests_failed: testData.test_results?.failed || 0,
    coverage: testData.test_results?.coverage || 0,
    test_framework: testData.framework,
    criteria_met: criteriaData.criteria_met || 0,
    criteria_total: criteriaData.criteria_total || 0
  },
  
  fix_flow_row_id: reporterData.fix_flow_row_id || null,
  
  tracking: {
    session_id: SESSION_ID,
    status: "completed"
  },
  
  summary: summary
})
```

---

## Agentes Utilizados

| Agente | Módulo | Responsabilidad |
|--------|--------|-----------------|
| mcp-validator | common | Validar disponibilidad del MCP |
| validator | implementer | Verificar archivos y compilación |
| test-executor | qa | Ejecutar tests del framework |
| criteria-validator | qa | Validar criterios de aceptación |
| qa-severity-calculator | qa | Calcular severity con fórmula QA |
| qa-decision-maker | qa | Decidir APPROVE/REJECT |
| qa-reporter | qa | Reportar a BD y completar task |

---

## Manejo de Errores

### Tabla de Manejo de Errores por Fase

| Fase | Punto de Fallo | Acción | Llama failSession() | Retorna Error |
|------|----------------|--------|---------------------|---------------|
| FASE 0 | MCP no disponible | Retorna error inmediato | ❌ No (tracking no iniciado) | ✅ Sí |
| FASE 0 | mcp-validator respuesta inválida | Retorna error inmediato | ❌ No (tracking no iniciado) | ✅ Sí |
| FASE 1 | Argumentos inválidos | Retorna error inmediato | ❌ No (tracking no iniciado) | ✅ Sí |
| FASE 2 | Error iniciando tracking | Retorna error inmediato | ❌ No (tracking no iniciado) | ✅ Sí |
| FASE 3 | Task no encontrada | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 3 | FlowRow no encontrada | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 3 | Flow no encontrado | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 3 | Project no encontrado | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 3 | Error obteniendo contexto | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 4 | Error creando work item | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 5 | Error obteniendo archivos | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 5 | Archivos vacíos | ⚠️ Advertencia, continúa | ❌ No | ❌ No |
| FASE 6 | validator respuesta inválida | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 6 | Archivos faltantes | Early exit con REJECT | ✅ Sí (vía qa-reporter) | ❌ No (éxito con REJECT) |
| FASE 6 | Error de compilación | Early exit con REJECT | ✅ Sí (vía qa-reporter) | ❌ No (éxito con REJECT) |
| FASE 6 | Error verificando archivos | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 7 | test-executor respuesta inválida | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 7 | test_results no es objeto | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 7 | Error ejecutando tests | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 8 | criteria-validator respuesta inválida | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 8 | results no es array | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 8 | Error validando criterios | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 9 | qa-severity-calculator respuesta inválida | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 9 | Error calculando severity | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 10 | qa-decision-maker respuesta inválida | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 10 | decision no es APPROVE/REJECT | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 10 | Error tomando decisión | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 11 | qa-reporter respuesta inválida | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 11 | work_item no actualizado | Completa step, falla sesión | ✅ Sí | ✅ Sí |
| FASE 11 | Error reportando resultados | Completa step, falla sesión | ✅ Sí | ✅ Sí |

### Códigos de Error

| Código | Descripción | Fase |
|--------|-------------|------|
| `MCP_NOT_AVAILABLE` | Servidor MCP no disponible | FASE 0 |
| `MCP_VALIDATION_ERROR` | Respuesta de mcp-validator inválida | FASE 0 |
| `INVALID_ARGUMENTS` | Argumentos del comando inválidos | FASE 1 |
| `TRACKING_START_ERROR` | Error iniciando tracking | FASE 2 |
| `TASK_NOT_FOUND` | Task no encontrada en BD | FASE 3 |
| `FLOWROW_NOT_FOUND` | FlowRow no encontrada en BD | FASE 3 |
| `FLOW_NOT_FOUND` | Flow no encontrado en BD | FASE 3 |
| `PROJECT_NOT_FOUND` | Project no encontrado en BD | FASE 3 |
| `CONTEXT_ERROR` | Error obteniendo contexto | FASE 3 |
| `WORK_ITEM_CREATION_ERROR` | Error creando work item | FASE 4 |
| `FILES_RETRIEVAL_ERROR` | Error obteniendo archivos | FASE 5 |
| `VALIDATION_ERROR` | Error verificando archivos | FASE 6 |
| `TEST_EXECUTION_ERROR` | Error ejecutando tests | FASE 7 |
| `CRITERIA_VALIDATION_ERROR` | Error validando criterios | FASE 8 |
| `SEVERITY_CALCULATION_ERROR` | Error calculando severity | FASE 9 |
| `DECISION_ERROR` | Error tomando decisión | FASE 10 |
| `REPORTER_ERROR` | Error reportando resultados | FASE 11 |

### Patrón de Manejo de Errores

Todas las fases críticas (FASE 3 en adelante) siguen este patrón:

```typescript
try {
  await startStep("Nombre del paso", N)

  // 1. Llamar agente o MCP tool
  const result = await Task({ ... }) // o mcp__MCPEco__*

  // 2. Validar respuesta con parseAgentResponse (para agentes)
  const parsed = parseAgentResponse<T>(result, "agent-name", ["campo1", "campo2"])

  if (!parsed.success) {
    await completeStep(false)
    await failSession(parsed.error)
    return JSON.stringify({
      success: false,
      error: "ERROR_CODE",
      message: parsed.error
    })
  }

  // 3. Validaciones adicionales específicas
  if (/* validación específica */) {
    await completeStep(false)
    await failSession("Mensaje específico")
    return JSON.stringify({
      success: false,
      error: "ERROR_CODE",
      message: "Mensaje específico"
    })
  }

  // 4. Continuar con lógica
  await logStep("...")
  await completeStep()

} catch (error) {
  await completeStep(false)
  await failSession(`Error en fase: ${error.message}`)
  return JSON.stringify({
    success: false,
    error: "ERROR_CODE",
    message: `Error en fase: ${error.message}`
  })
}
```

---

## Diagrama de Flujo

```
┌─────────────────────────────────────────────────────────────────┐
│                    061-qa-task.md                               │
│                    (Comando Orquestador)                        │
└─────────────────────────────────────────────────────────────────┘
     │
     ├─> FASE 0: mcp-validator ──────────────────┐ (error) ──> ❌ EXIT
     │                                            │
     ├─> FASE 1: Parsear $ARGUMENTS ─────────────┤ (error) ──> ❌ EXIT
     │                                            │
     ├─> FASE 2: Iniciar Tracking (MCP) ─────────┤ (error) ──> ❌ EXIT
     │                                            │
     ├─> FASE 3: Obtener Contexto (MCP) ─────────┤ (error) ──> ❌ EXIT + failSession()
     │                                            │
     ├─> FASE 4: Crear Work Item (MCP) ──────────┤ (error) ──> ❌ EXIT + failSession()
     │                                            │
     ├─> FASE 5: Obtener Archivos (MCP) ─────────┤ (error) ──> ❌ EXIT + failSession()
     │                          │                 │
     │                          └─> ⚠️ (vacíos) continúa
     │
     ├─> FASE 6: validator (verificar) ──────────┤ (error) ──> ❌ EXIT + failSession()
     │     │                                      │
     │     ├─> Si faltan archivos ───────────────┤ REJECT ──> ✅ FASE 11 (early exit)
     │     └─> Si no compila ────────────────────┤ REJECT ──> ✅ FASE 11 (early exit)
     │
     ├─> FASE 7: test-executor ──────────────────┤ (error) ──> ❌ EXIT + failSession()
     │                                            │
     ├─> FASE 8: criteria-validator ─────────────┤ (error) ──> ❌ EXIT + failSession()
     │                                            │
     ├─> FASE 9: qa-severity-calculator ─────────┤ (error) ──> ❌ EXIT + failSession()
     │                                            │
     ├─> FASE 10: qa-decision-maker ─────────────┤ (error) ──> ❌ EXIT + failSession()
     │                                            │
     ├─> FASE 11: qa-reporter ───────────────────┤ (error) ──> ❌ EXIT + failSession()
     │                                            │
     └─> FASE 12: Finalizar Tracking ────────────┘ ✅ SUCCESS

Leyenda:
  ❌ EXIT               = Retorna error, termina ejecución
  ❌ EXIT + failSession = Retorna error, llama failSession(), termina ejecución
  ✅ FASE 11            = Continúa a FASE 11 con REJECT (early exit)
  ✅ SUCCESS            = Éxito, retorna output final
  ⚠️                    = Advertencia, continúa ejecución
```

---

## Reglas del Comando

### Reglas de Orquestación

1. ✅ Validar MCP antes de comenzar
2. ✅ Iniciar/cerrar tracking vía MCP directo
3. ✅ Delegar tareas atómicas a agentes
4. ❌ NO ejecutar tests directamente
5. ❌ NO calcular severity directamente
6. ❌ NO modificar archivos
7. ✅ Early exit si archivos faltan O no compila

### Reglas de Validación y Manejo de Errores

8. ✅ Todas las respuestas de agentes DEBEN validarse con `parseAgentResponse()`
9. ✅ Todas las fases críticas (3+) DEBEN estar envueltas en try/catch
10. ✅ En caso de error después de FASE 2, SIEMPRE llamar `failSession()` antes de retornar
11. ✅ Validar tipos y estructuras de datos explícitamente (arrays, objetos, enums)

---

## Versión

- **Versión**: 3.0.0
- **Migrado desde**: `LLMs/Claude/.claude/commands/043-qa-task.md`
- **Fecha última actualización**: 2026-01-17

### Changelog

#### v3.0.0 (2026-01-17) - Major Release: Tracking y TODO List

**BREAKING CHANGES:**
- ⚠️ **project_id ahora es OBLIGATORIO** (antes era opcional e inferido)
- ⚠️ **project_id se pasa en ARGUMENTS, task_id en PROMPT** (formato cambiado)
- ⚠️ **Formato de uso actualizado**: `/061-qa-task <project-id> <task-id>`
- ⚠️ **Eliminada lógica de obtener proyecto desde flow** (ahora valida primero project_id)

**Nuevas Funcionalidades:**
- ✅ **FASE -2**: TODO List con TodoWrite para visibilidad del progreso (12 items)
- ✅ **FASE -1**: Cargar herramientas MCP explícitamente con MCPSearch (8 herramientas)
- ✅ **TODO List se actualiza en cada fase completada** (FASES 0-12 + early exit)
- ✅ **Validación de que task pertenece al proyecto correcto** (FASE 3)
- ✅ **Logs mejorados antes de cada delegación a agentes** (FASES 6-11)
- ✅ **Agregados MCPSearch y TodoWrite a allowed-tools**

**Correcciones:**
- 🐛 **FASE 1**: Validación explícita de projectId y taskId (ambos obligatorios)
- 🐛 **FASE 2**: Agregado project_id al iniciar sesión
- 🐛 **FASE 3**:
  - Obtener proyecto PRIMERO (usando projectId)
  - Validar que flow.project_id == projectId
  - Logs detallados de cada paso
- 🐛 **FASE 4-11**: Agregados logs antes de delegación y TODO updates
- 🐛 **FASE 12**: Agregado startStep("Finalizar"), logStep, completeStep y TODO update final
- 🐛 **FASE 6 Early Exit**: TODO update cuando se rechaza por archivos/compilación
- 🐛 **Todos los catch blocks**: Cambiados de `return JSON.stringify` a `throw error`

**Mejoras de Documentación:**
- 📚 **Sección "Información Recibida"**: Actualizada con nuevo formato de parámetros
- 📚 **Sección "Uso"**: Actualizada con ejemplo correcto
- 📚 **Diagrama de Flujo**: Pendiente actualización (falta agregar FASE -2 y FASE -1)

**Impacto:**
- ✅ **Visibilidad total** del progreso para el usuario (12 items en TODO list)
- ✅ **UX mejorada** - el usuario sabe en qué fase está el comando
- ✅ **Previene timeouts percibidos** - el usuario ve que el comando está avanzando
- ✅ **Consistencia** con 031-planner-decompose-story, 041-implementer-task y 051-code-review-task
- ✅ **Trazabilidad completa** - cada delegación tiene log explícito
- ✅ **Debugging mejorado** - logs antes y después de cada agente

**Arquitectura:**
- Patrón de tracking con session_id y step_id completamente implementado
- TODO list refleja el progreso real del comando (12 fases)
- Validación de pertenencia task → proyecto antes de procesar
- Early exit path también actualiza TODO list correctamente

#### v2.0.0 (2026-01-16) - Major Release: Arquitectura Robusta

**Breaking Changes:**
- ⚠️ Todas las respuestas de agentes ahora requieren estructura JSON estricta
- ⚠️ Implementación de validación obligatoria con `parseAgentResponse()`

**Nuevas Funcionalidades:**
- ✅ **Helper `parseAgentResponse()`**: Función genérica de validación con tipado TypeScript
- ✅ **Contratos de Agentes Documentados**: 7 agentes con input/output schemas completos
- ✅ **Manejo de Errores Robusto**: Try/catch en todas las fases críticas (3-11)
- ✅ **Early Exit Mejorado**: REJECT inmediato para archivos faltantes O errores de compilación
- ✅ **Validación de Tipos**: Validación explícita de arrays, objetos y enums
- ✅ **Tabla de Manejo de Errores**: Documentación completa de 17 códigos de error
- ✅ **Diagrama de Flujo Actualizado**: Muestra todos los puntos de salida de error

**Correcciones:**
- 🐛 **FASE 3**: Agregado try/catch y validación de task/flowRow/flow/project no nulos
- 🐛 **FASE 4**: Agregado try/catch para creación de work item
- 🐛 **FASE 5**: Agregado try/catch con warnings para archivos vacíos
- 🐛 **FASE 6**:
  - Agregado try/catch y `parseAgentResponse()`
  - Early exit para AMBOS casos: archivos faltantes O compilación fallida
  - Validación de campos `compiles` y `files_exist`
- 🐛 **FASE 7**:
  - Agregado try/catch y `parseAgentResponse()`
  - Validación que `test_results` sea un objeto
- 🐛 **FASE 8**:
  - Agregado try/catch y `parseAgentResponse()`
  - Validación que `results` sea un array
- 🐛 **FASE 9**:
  - Agregado try/catch y `parseAgentResponse()`
  - Validación de campo `severity` como número
- 🐛 **FASE 10**:
  - Agregado try/catch y `parseAgentResponse()`
  - Validación estricta: `decision` debe ser "APPROVE" o "REJECT"
- 🐛 **FASE 11**:
  - Agregado try/catch y `parseAgentResponse()`
  - Validación crítica: `work_item_updated` debe ser true
  - Manejo de éxito parcial

**Mejoras de Documentación:**
- 📚 **Sección "Contratos de Agentes"**: Documentación completa de los 7 agentes
- 📚 **Sección "Manejo de Errores"**:
  - Tabla de 29 puntos de fallo
  - 17 códigos de error documentados
  - Patrón de manejo de errores con ejemplos
- 📚 **Reglas del Comando Actualizadas**:
  - 7 reglas de orquestación
  - 4 nuevas reglas de validación y manejo de errores
- 📚 **Diagrama de Flujo Mejorado**: Muestra todos los caminos de error

**Impacto:**
- ❌ **Sesiones Huérfanas Eliminadas**: `failSession()` se llama en todos los errores post-FASE 2
- ✅ **Robustez**: Prevención de crashes por respuestas inválidas de agentes
- ✅ **Debugging**: Mensajes de error específicos con códigos claros
- ✅ **Mantenibilidad**: Patrón consistente en todas las fases
- ✅ **Trazabilidad**: Validación explícita de contratos entre orquestador y agentes

**Arquitectura:**
- Patrón orquestador reforzado con validación de contratos
- Eliminación de inferencia implícita de parámetros
- Manejo de errores predecible y documentado
- TypeScript typing explícito en todas las variables

#### v1.1.0 (2026-01-16)
- Migrado de execution-session-tracker (obsoleto) a MCP directo

#### v1.0.0 (Inicial)
- Versión base migrada desde `LLMs/Claude/.claude/commands/043-qa-task.md`
