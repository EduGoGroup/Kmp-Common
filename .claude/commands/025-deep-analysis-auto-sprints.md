---
name: 025-deep-analysis-auto-sprints
description: Generar automáticamente todos los sprints de un proyecto
allowed-tools: MCPSearch, TodoWrite, Task, mcp__MCPEco__get_project_info, mcp__MCPEco__execution_session_manage, mcp__MCPEco__list_flows, mcp__MCPEco__get_flow_row
---

# Deep Analysis: Auto-Sprints

Meta-orquestador que analiza la constitución del proyecto y genera todos los sprints necesarios.

## 📥 Input

**Argumento requerido**: `project_id`
- Formato: `proj-<timestamp>` (ejemplo: `proj-1768611555348527000`)
- Ejemplo: `/025-deep-analysis-auto-sprints proj-1768611555348527000`

**Sesión**: Si el proyecto tiene `last_session_id`, se reusará automáticamente. Para forzar nueva sesión, usar proyecto sin sesión previa.

**Output esperado**:
```json
{
  "success": true,
  "error_code": null,
  "timestamp": "2026-01-22T...",
  "command": "025-deep-analysis-auto-sprints",
  "data": {
    "project_id": "proj-xxx",
    "session_id": "sess-xxx",
    "total_created": 3,
    "total_failed": 0,
    "results": [...]
  }
}
```

---

## ⚠️ REGLAS GENERALES (CRÍTICAS)

**SEGUIR EL PLAN AL PIE DE LA LETRA:**

1. **NO asumir cambios o mejoras** al flujo sin consultar al usuario primero
2. **Si detectas algo que debe cambiarse o mejorarse**, DETENER la ejecución y **PREGUNTAR AL USUARIO** usando AskUserQuestion
3. **NO modificar el orden de las fases** sin autorización explícita
4. **NO omitir pasos** aunque parezcan opcionales
5. **NO agregar pasos adicionales** sin consultar
6. **Si un agente falla**, registrar en logs y reportar al usuario, NO intentar soluciones alternativas sin autorización
7. **Actualizar TODO List** al inicio y final de cada fase usando TodoWrite para mantener tracking preciso del progreso

**Excepción**: Solo puedes proceder sin preguntar cuando:
- Un agente retorna error técnico conocido (ej: timeout, conexión) → Reintentar hasta 2 veces
- Un parámetro faltante obvio según contexto → Inferirlo y DOCUMENTAR la decisión en logs

**En caso de duda → PREGUNTAR AL USUARIO**

---

## 🔄 Flujo de Ejecución

### FASE -1: Cargar Herramientas MCP

**OBLIGATORIO**: Antes de ejecutar cualquier lógica, cargar TODAS las herramientas MCP requeridas.

```typescript
// Cargar herramientas MCP explícitamente
await MCPSearch({ query: "select:mcp__MCPEco__get_project_info" })
await MCPSearch({ query: "select:mcp__MCPEco__execution_session_manage" })
await MCPSearch({ query: "select:mcp__MCPEco__list_flows" })
await MCPSearch({ query: "select:mcp__MCPEco__get_flow_row" })

console.log("✅ Herramientas MCP cargadas correctamente")
```

### FASE -1.5: Inicializar TODO List

**OBLIGATORIO**: Crear TODO List para tracking de progreso antes de iniciar ejecución.

```typescript
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Validar input y obtener proyecto", activeForm: "Validando input", status: "pending" },
    { content: "Inicializar sesión de ejecución", activeForm: "Inicializando sesión", status: "pending" },
    { content: "Verificar sprints existentes", activeForm: "Verificando sprints", status: "pending" },
    { content: "Leer constitución del proyecto", activeForm: "Leyendo constitución", status: "pending" },
    { content: "Planear sprints", activeForm: "Planeando sprints", status: "pending" },
    { content: "Crear sprints en loop", activeForm: "Creando sprints", status: "pending" },
    { content: "Finalizar y retornar resultado", activeForm: "Finalizando", status: "pending" }
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
  throw new Error("MCP no disponible")
}
```

### FASE 1: Obtener Proyecto y Validar

```typescript
// 📋 TODO: Actualizar TodoWrite con status "in_progress" para esta fase

// Parsear argumentos (command-args viene como string)
const args = context.commandArgs?.trim()

if (!args) {
  throw new Error("❌ ERROR: project_id es OBLIGATORIO. Uso: /025-deep-analysis-auto-sprints <project_id>")
}

const projectId = args

// Obtener información del proyecto (incluye last_session_id)
const project = await mcp__MCPEco__get_project_info({ project_id: projectId })

if (!project || !project.success) {
  throw new Error(`❌ Proyecto no encontrado: ${projectId}`)
}

console.log(`✅ Proyecto: ${project.project_name} (${project.project_level})`)
```

### FASE 1.5: Inicializar Sesión de Ejecución

```typescript
let sessionId
let stepId

// Si el proyecto NO tiene sesión previa, crear una nueva
if (!project.last_session_id) {
  console.log("🔧 Creando nueva sesión de ejecución...")

  const sessionResult = await mcp__MCPEco__execution_session_manage({
    action: "start_session",
    command: "025-deep-analysis-auto-sprints",
    provider: "claude",
    project_id: projectId,
    trigger_source: "cli",
    context_metadata: {
      project_level: project.project_level,
      tech: project.tech,
      kind: project.kind
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
  step_name: "auto-sprints-generation",
  step_order: 1
})

if (!stepResult || !stepResult.step_id) {
  throw new Error("❌ Error creando step de ejecución")
}

stepId = stepResult.step_id
console.log(`✅ Step iniciado: ${stepId}`)
```

### FASE 2: Verificar Sprints Existentes

```typescript
// 📋 TODO: Actualizar TodoWrite con status "in_progress" para esta fase

const existingFlows = await mcp__MCPEco__list_flows({ project_id: project.project_id })

if (existingFlows?.flows?.length > 0) {
  console.log("⚠️ El proyecto ya tiene sprints:")
  existingFlows.flows.forEach((f, i) => console.log(`  ${i+1}. ${f.flow_name}`))
  console.log("No se crearán sprints adicionales.")
  return JSON.stringify({
    success: true,
    message: "Proyecto ya tiene sprints",
    existing_sprints: existingFlows.flows.length
  })
}
```

### FASE 3: Leer Constitución

```typescript
// 📋 TODO: Actualizar TodoWrite con status "in_progress" para esta fase

// Buscar constitución en base de datos usando agente search-local
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: `🔍 FASE 3: Buscando constitución del proyecto en base de datos`,
  log_level: "info"
})

const searchResult = await Task({
  subagent_type: "search-local",
  description: "Buscar constitución",
  prompt: JSON.stringify({
    entity_type: "project",
    entity_id: projectId,
    step_type: "deep_analysis"
  })
})

// VALIDACIÓN CRÍTICA: Este comando REQUIERE una constitución válida
if (!searchResult || searchResult.status !== "success" || !searchResult.documents_found || searchResult.documents_found === 0) {
  const errorMsg = "❌ ERROR CRÍTICO: No se encontró documento de constitución para el proyecto. El comando 025-deep-analysis-auto-sprints REQUIERE una constitución válida para generar sprints coherentes con la arquitectura del proyecto."

  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: stepId,
    message: errorMsg,
    log_level: "error"
  })

  await mcp__MCPEco__execution_session_manage({
    action: "fail",
    session_id: sessionId,
    step_id: stepId,
    error_message: errorMsg
  })

  console.error(errorMsg)
  console.error("\n📋 SOLUCIÓN:")
  console.error("1. Crear la constitución del proyecto usando /011-constitution-create-project")
  console.error("2. O asociar un documento existente con tag 'constitution' al proyecto")

  return JSON.stringify({
    success: false,
    error: "no_constitution_found",
    message: errorMsg
  }, null, 2)
}

// Buscar el documento específico de constitución (tag "constitution" en applies_to_steps o tags)
const constitutionDoc = searchResult.results?.find(
  doc => doc.tags?.includes("constitution")
) || searchResult.results?.[0]  // Fallback al primero si no tiene tag

if (!constitutionDoc) {
  const errorMsg = `❌ ERROR: Se encontraron ${searchResult.documents_found} documentos del proyecto, pero ninguno es válido.`

  await mcp__MCPEco__execution_session_manage({
    action: "fail",
    session_id: sessionId,
    step_id: stepId,
    error_message: errorMsg
  })

  console.error(errorMsg)
  console.error("Resultados:", JSON.stringify(searchResult.results, null, 2))

  return JSON.stringify({
    success: false,
    error: "no_valid_constitution",
    message: errorMsg
  }, null, 2)
}

const constitutionContent = constitutionDoc.summary || constitutionDoc.content

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: `✅ FASE 3: Constitución encontrada (document_id: ${constitutionDoc.document_id}, title: "${constitutionDoc.title}")`,
  log_level: "info"
})

console.log(`✅ Constitución encontrada: ${constitutionDoc.title}`)
```

### FASE 4: Planear Sprints

```typescript
// 📋 TODO: Actualizar TodoWrite con status "in_progress" para esta fase

// Límites por nivel
const limits = {
  mvp: { min: 1, max: 1 },
  standard: { min: 2, max: 3 },
  enterprise: { min: 3, max: 8 }
}

const levelLimits = limits[project.project_level] || limits.standard

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: `🔍 FASE 4: Iniciando planificación de sprints (nivel: ${project.project_level}, rango: ${levelLimits.min}-${levelLimits.max})`,
  log_level: "info"
})

const planResult = await Task({
  subagent_type: "general-purpose",
  description: "Planear sprints",
  prompt: `
Analiza esta constitución y determina los sprints necesarios:

CONSTITUCIÓN:
${constitutionContent}

REGLAS:
- Nivel: ${project.project_level}
- Min sprints: ${levelLimits.min}
- Max sprints: ${levelLimits.max}
- Tech: ${project.tech}

Retorna SOLO un JSON:
{
  "total_sprints": number,
  "sprints": [
    { "order": 1, "milestone_description": "..." },
    { "order": 2, "milestone_description": "..." }
  ]
}
`
})

// Parsear resultado con parser robusto
let sprintPlan
try {
  // Intentar parsear directamente primero
  let jsonText = planResult

  // Si viene en markdown code block, extraerlo
  const codeBlockMatch = planResult.match(/```(?:json)?\s*(\{[\s\S]*?\})\s*```/)
  if (codeBlockMatch) {
    jsonText = codeBlockMatch[1]
  } else {
    // Buscar objeto JSON en el texto
    const jsonMatch = planResult.match(/\{[\s\S]*\}/)
    if (jsonMatch) {
      jsonText = jsonMatch[0]
    }
  }

  // Parsear JSON
  sprintPlan = JSON.parse(jsonText)

  // Validar estructura obligatoria
  if (!sprintPlan.total_sprints || !Array.isArray(sprintPlan.sprints)) {
    throw new Error("JSON inválido: falta 'total_sprints' o 'sprints' no es array")
  }

  // Validar que total_sprints coincida con el array
  if (sprintPlan.sprints.length !== sprintPlan.total_sprints) {
    throw new Error(`Inconsistencia: total_sprints=${sprintPlan.total_sprints} pero sprints.length=${sprintPlan.sprints.length}`)
  }

  // Validar cada sprint
  for (let i = 0; i < sprintPlan.sprints.length; i++) {
    const sprint = sprintPlan.sprints[i]
    if (!sprint.order || !sprint.milestone_description) {
      throw new Error(`Sprint ${i}: falta 'order' o 'milestone_description'`)
    }
  }

} catch (e) {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: stepId,
    message: `❌ FASE 4: Error parseando plan de sprints: ${e.message}\nRespuesta recibida: ${planResult.substring(0, 200)}...`,
    log_level: "error"
  })
  throw new Error(`Error parseando plan de sprints: ${e.message}`)
}

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: stepId,
  message: `✅ FASE 4: Plan completado exitosamente - ${sprintPlan.total_sprints} sprints planificados`,
  log_level: "info"
})

console.log("✅ Plan: " + sprintPlan.total_sprints + " sprints")
```

### FASE 5: Crear Sprints (Loop)

```typescript
// 📋 TODO: Actualizar TodoWrite con status "in_progress" para esta fase

const results = []
let successCount = 0
let failureCount = 0

for (const sprintConfig of sprintPlan.sprints) {
  console.log(`\n📦 Creando Sprint ${sprintConfig.order}/${sprintPlan.total_sprints}...`)

  try {
    // Análisis del milestone
    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: stepId,
      message: `🔍 FASE 5.1: Analizando milestone para Sprint ${sprintConfig.order}/${sprintPlan.total_sprints}`,
      log_level: "info"
    })

    const analysisResult = await Task({
      subagent_type: "milestone-analyzer",
      description: `Analizar sprint ${sprintConfig.order}`,
      prompt: JSON.stringify({
        milestone_description: sprintConfig.milestone_description,
        tech: project.tech,
        kind: project.kind,
        project_level: project.project_level,
        project_path: project.project_path,
        project_name: project.project_name
      })
    })

    // Validación robusta de milestone-analyzer
    if (!analysisResult) {
      const errorMsg = `milestone-analyzer retornó null/undefined para Sprint ${sprintConfig.order}`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.1: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    if (!analysisResult.analysis) {
      const errorMsg = `milestone-analyzer no retornó campo 'analysis' para Sprint ${sprintConfig.order}. Respuesta: ${JSON.stringify(analysisResult).substring(0, 200)}`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.1: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    if (!analysisResult.analysis.proposed_modules || !Array.isArray(analysisResult.analysis.proposed_modules)) {
      const errorMsg = `milestone-analyzer.analysis no tiene 'proposed_modules' válido para Sprint ${sprintConfig.order}`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.1: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: stepId,
      message: `✅ FASE 5.1: Análisis de milestone completado para Sprint ${sprintConfig.order}`,
      log_level: "info"
    })

    // Crear flow
    const currentFlows = await mcp__MCPEco__list_flows({ project_id: project.project_id })

    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: stepId,
      message: `🔍 FASE 5.2: Creando flow para Sprint ${sprintConfig.order}/${sprintPlan.total_sprints}`,
      log_level: "info"
    })

    const flowResult = await Task({
      subagent_type: "flow-creator",
      description: "Crear flow",
      prompt: JSON.stringify({
        project_id: project.project_id,
        project_level: project.project_level,
        current_sprint_count: currentFlows?.flows?.length || 0,
        limits: project.config?.limits || {},
        milestone_analysis: analysisResult.analysis
      })
    })

    // Validación robusta de flow-creator
    if (!flowResult) {
      const errorMsg = `flow-creator retornó null/undefined para Sprint ${sprintConfig.order}`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.2: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    if (!flowResult.flow) {
      const errorMsg = `flow-creator no retornó campo 'flow' para Sprint ${sprintConfig.order}. Respuesta: ${JSON.stringify(flowResult).substring(0, 200)}`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.2: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    if (!flowResult.flow.flow_id) {
      const errorMsg = `flow-creator.flow no tiene 'flow_id' para Sprint ${sprintConfig.order}. Flow: ${JSON.stringify(flowResult.flow)}`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.2: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: stepId,
      message: `✅ FASE 5.2: Flow creado exitosamente (flow_id: ${flowResult.flow.flow_id})`,
      log_level: "info"
    })

    // Filtrar módulos
    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: stepId,
      message: `🔍 FASE 5.3: Filtrando módulos para Sprint ${sprintConfig.order}/${sprintPlan.total_sprints}`,
      log_level: "info"
    })

    const filterResult = await Task({
      subagent_type: "impact-filter",
      description: "Filtrar módulos",
      prompt: JSON.stringify({
        project_level: project.project_level,
        proposed_modules: analysisResult.analysis.proposed_modules,
        limits: project.config?.limits || {},
        milestone_description: sprintConfig.milestone_description
      })
    })

    // Validación robusta de impact-filter
    if (!filterResult) {
      const errorMsg = `impact-filter retornó null/undefined para Sprint ${sprintConfig.order}`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.3: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    if (!filterResult.filtering_result) {
      const errorMsg = `impact-filter no retornó campo 'filtering_result' para Sprint ${sprintConfig.order}. Respuesta: ${JSON.stringify(filterResult).substring(0, 200)}`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.3: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    if (!filterResult.filtering_result.approved_modules || !Array.isArray(filterResult.filtering_result.approved_modules)) {
      const errorMsg = `impact-filter.filtering_result no tiene 'approved_modules' válido para Sprint ${sprintConfig.order}`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.3: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    if (filterResult.filtering_result.approved_modules.length === 0) {
      const errorMsg = `impact-filter retornó 0 módulos aprobados para Sprint ${sprintConfig.order}. Posible problema de filtrado.`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `⚠️ FASE 5.3: ${errorMsg}`,
        log_level: "warn"
      })
    }

    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: stepId,
      message: `✅ FASE 5.3: Filtrado completado - ${filterResult.filtering_result.approved_modules.length} módulos aprobados`,
      log_level: "info"
    })

    // Crear módulos
    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: stepId,
      message: `🔍 FASE 5.4: Creando flow_rows para Sprint ${sprintConfig.order}/${sprintPlan.total_sprints}`,
      log_level: "info"
    })

    const moduleResult = await Task({
      subagent_type: "module-creator",
      description: "Crear flow_rows",
      prompt: JSON.stringify({
        flow_id: flowResult.flow.flow_id,
        approved_modules: filterResult.filtering_result.approved_modules,
        global_risks: analysisResult.analysis.global_risks || [],
        global_dependencies: analysisResult.analysis.global_dependencies || [],
        tech: project.tech,
        kind: project.kind
      })
    })

    // Validación robusta de module-creator
    if (!moduleResult) {
      const errorMsg = `module-creator retornó null/undefined para Sprint ${sprintConfig.order}`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.4: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    if (!moduleResult.flow_rows_created || !Array.isArray(moduleResult.flow_rows_created)) {
      const errorMsg = `module-creator no retornó campo 'flow_rows_created' válido para Sprint ${sprintConfig.order}. Respuesta: ${JSON.stringify(moduleResult).substring(0, 200)}`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.4: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    if (moduleResult.flow_rows_created.length === 0) {
      const errorMsg = `module-creator retornó 0 flow_rows para Sprint ${sprintConfig.order}. Esto es inesperado.`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.4: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: stepId,
      message: `✅ FASE 5.4: Flow_rows creados exitosamente - ${moduleResult.flow_rows_created.length} módulos`,
      log_level: "info"
    })

    // VALIDACIÓN CRÍTICA: Verificar que los flow_rows REALMENTE existen en BD
    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: stepId,
      message: `🔍 FASE 5.4.1: Verificando que flow_rows fueron persistidos en BD...`,
      log_level: "info"
    })

    const verificationErrors = []
    for (const flowRow of moduleResult.flow_rows_created) {
      try {
        // Intentar obtener el flow_row desde BD
        const verifyResult = await mcp__MCPEco__get_flow_row({
          flow_row_id: flowRow.flow_row_id
        })
        
        if (!verifyResult || !verifyResult.flow_row_id) {
          verificationErrors.push(flowRow.flow_row_id)
        }
      } catch (err) {
        verificationErrors.push(flowRow.flow_row_id)
      }
    }

    if (verificationErrors.length > 0) {
      const errorMsg = `ERROR CRÍTICO - El agente module-creator retornó IDs de flow_rows que NO fueron persistidos en la base de datos. Verificación: flow_row ${verificationErrors[0]} NO existe.`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.4: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(`El agente module-creator retornó IDs de flow_rows (${verificationErrors.join(', ')}) que NO fueron persistidos en la base de datos. Esto impide la creación de stories ya que la foreign key constraint falla. El agente module-creator debe ser corregido para PERSISTIR los flow_rows en BD antes de retornar los IDs.`)
    }

    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: stepId,
      message: `✅ FASE 5.4.1: Verificación completada - Todos los flow_rows existen en BD`,
      log_level: "info"
    })

    // Crear stories
    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: stepId,
      message: `🔍 FASE 5.5: Creando stories para Sprint ${sprintConfig.order}/${sprintPlan.total_sprints}`,
      log_level: "info"
    })

    const storyResult = await Task({
      subagent_type: "story-creator",
      description: "Crear stories",
      prompt: JSON.stringify({
        project_level: project.project_level,
        flow_rows: moduleResult.flow_rows_created,
        limits: project.config?.limits || {},
        tech: project.tech,
        kind: project.kind,
        milestone_description: sprintConfig.milestone_description
      })
    })

    // Validación robusta de story-creator
    if (!storyResult) {
      const errorMsg = `story-creator retornó null/undefined para Sprint ${sprintConfig.order}`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.5: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    if (!storyResult.stories_created || !Array.isArray(storyResult.stories_created)) {
      const errorMsg = `story-creator no retornó campo 'stories_created' válido para Sprint ${sprintConfig.order}. Respuesta: ${JSON.stringify(storyResult).substring(0, 200)}`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.5: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    if (storyResult.stories_created.length === 0) {
      const errorMsg = `story-creator retornó 0 stories para Sprint ${sprintConfig.order}. Esto es inesperado.`
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: stepId,
        message: `❌ FASE 5.5: ${errorMsg}`,
        log_level: "error"
      })
      throw new Error(errorMsg)
    }

    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: stepId,
      message: `✅ FASE 5.5: Stories creadas exitosamente - ${storyResult.stories_created.length} stories para Sprint ${sprintConfig.order}`,
      log_level: "info"
    })
    
    successCount++
    results.push({
      sprint: sprintConfig.order,
      status: "success",
      flow_id: flowResult.flow.flow_id,
      stories: storyResult.stories_created
    })
    
    console.log(`✅ Sprint ${sprintConfig.order} creado`)
    
  } catch (error) {
    failureCount++
    results.push({ sprint: sprintConfig.order, status: "failed", error: error.message })
    console.error(`❌ Error: ${error.message}`)
  }
  
  // Circuit breaker
  if (failureCount > sprintPlan.total_sprints / 2) {
    console.error("⚠️ CIRCUIT BREAKER: >50% fallaron")
    break
  }
}
```

### FASE 6: Finalizar Step y Retornar Resultado

```typescript
// 📋 TODO: Actualizar TodoWrite con status "in_progress" para esta fase

console.log("")
console.log("═══════════════════════════════════════════════════════")
console.log(successCount === sprintPlan.total_sprints
  ? "  ✅ AUTO-SPRINTS COMPLETADO"
  : "  ⚠️ AUTO-SPRINTS PARCIAL")
console.log("═══════════════════════════════════════════════════════")
console.log(`📊 Creados: ${successCount}/${sprintPlan.total_sprints}`)

// Completar step de ejecución
await mcp__MCPEco__execution_session_manage({
  action: "complete_step",
  step_id: stepId,
  metadata: {
    total_planned: sprintPlan.total_sprints,
    total_created: successCount,
    total_failed: failureCount,
    success_rate: (successCount / sprintPlan.total_sprints) * 100
  }
})

console.log("✅ Step completado correctamente")

// Finalizar sesión si fue exitoso
if (successCount === sprintPlan.total_sprints) {
  await mcp__MCPEco__execution_session_manage({
    action: "finish_session",
    session_id: sessionId,
    summary: `Auto-sprints completado: ${successCount}/${sprintPlan.total_sprints} sprints creados para proyecto ${project.project_name}`
  })
  console.log("✅ Sesión finalizada")
} else if (failureCount > 0) {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: stepId,
    message: `Completado parcialmente: ${failureCount} sprints fallaron de ${sprintPlan.total_sprints} planificados`,
    log_level: "warn"
  })
}

return JSON.stringify({
  success: successCount > 0,
  error_code: successCount === 0 ? "SPRINT_CREATION_FAILED" : (failureCount > 0 ? "PARTIAL_SUCCESS" : null),
  timestamp: new Date().toISOString(),
  command: "025-deep-analysis-auto-sprints",
  data: {
    project_id: project.project_id,
    session_id: sessionId,
    step_id: stepId,
    total_planned: sprintPlan.total_sprints,
    total_created: successCount,
    total_failed: failureCount,
    results: results
  }
}, null, 2)
```

---

## 📋 Agentes Utilizados

| Fase | Agente |
|------|--------|
| 0 | mcp-validator |
| 3 | search-local |
| 4 | general-purpose |
| 5 | milestone-analyzer, flow-creator, impact-filter, module-creator, story-creator |

---

## 🧪 Testing

### Casos de Prueba

| Caso | Input | Resultado Esperado |
|------|-------|-------------------|
| Proyecto válido sin sprints | `/025-deep-analysis-auto-sprints proj-valid` | Sprints creados según nivel |
| Proyecto con sprints existentes | `/025-deep-analysis-auto-sprints proj-with-sprints` | Mensaje "ya tiene sprints" |
| Proyecto sin constitución | `/025-deep-analysis-auto-sprints proj-no-constitution` | Error con instrucciones |
| Project ID inválido | `/025-deep-analysis-auto-sprints invalid-id` | Error "Proyecto no encontrado" |
| Sin argumentos | `/025-deep-analysis-auto-sprints` | Error "project_id es OBLIGATORIO" |

### Verificación Manual

```bash
# 1. Verificar que el proyecto existe
mcp__MCPEco__get_project_info --project_id proj-xxx

# 2. Ejecutar comando
/025-deep-analysis-auto-sprints proj-xxx

# 3. Verificar sprints creados
mcp__MCPEco__list_flows --project_id proj-xxx
```

---

**Versión**: 2.5
**Última actualización**: 2026-01-21
**Changelog v2.5**:
- **CRÍTICO - FIX REAL**: Agentes corregidos con MCPSearch en tools + model:sonnet (module-creator v2.3, flow-creator, story-creator)
- **RAZÓN**: MCPSearch requiere Sonnet 4+ (no funciona con Haiku). Frontmatter tools: MCPSearch carga herramientas automáticamente
- **REVERTIDO**: Eliminado PASO 0 incorrecto de v2.4 (no era necesario, el frontmatter tools: ya carga las herramientas)
**Changelog v2.4**:
- **CRÍTICO**: Agregada sección "REGLAS GENERALES" - NO asumir cambios sin preguntar al usuario
- **CRÍTICO**: Política estricta de seguimiento del plan - Si algo debe cambiarse, PREGUNTAR primero
- **MEJORA**: Definición clara de excepciones permitidas (reintentos, parámetros obvios)
**Changelog v2.3**:
- **CRÍTICO**: FASE 3 corregida - Formato correcto de invocación a search-local (entity_type + entity_id + step_type)
- **CRÍTICO**: Validación estricta de constitución - Si no existe, proceso termina con error (no fallback)
- **CRÍTICO**: Filtrado por tag "constitution" para garantizar documento correcto
- Mensajes de error descriptivos con pasos de solución
- **DEPENDENCIA**: Requiere agente search-local v2.3+ (prioriza entity sobre query)
**Changelog v2.2**: Parsing JSON robusto, validación exhaustiva de subagentes
