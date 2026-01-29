---
name: 011-constitution-create-project
description: Orquesta la creación de un proyecto usando agentes troceados.
allowed-tools: Task, mcp__MCPEco__execution_session_manage, Read, mcp__MCPEco__get_document, MCPSearch, TodoWrite, Glob, Grep
---

# Constitution: Crear Proyecto (Orquestador)

Orquesta la creación de proyectos delegando a agentes especializados.

**IMPORTANTE**: Comunícate en español.

## 🎯 Flujo de Orquestación (12 Fases)

### FASE -1: Inicializar TODO List (Tracking Visual)

```typescript
// OBLIGATORIO: Crear TODO list para tracking visual del usuario
// Esto permite al usuario ver el progreso en tiempo real

await TodoWrite({
  todos: [
    { content: "Validar conectividad MCP", status: "pending", activeForm: "Validando conectividad MCP" },
    { content: "Preprocesar input del usuario", status: "pending", activeForm: "Preprocesando input" },
    { content: "Iniciar tracking de sesión", status: "pending", activeForm: "Iniciando tracking" },
    { content: "Analizar descripción del proyecto", status: "pending", activeForm: "Analizando descripción" },
    { content: "Crear proyecto en base de datos", status: "pending", activeForm: "Creando proyecto" },
    { content: "Vincular tracking al proyecto", status: "pending", activeForm: "Vinculando tracking" },
    { content: "Determinar documentos necesarios", status: "pending", activeForm: "Determinando documentos" },
    { content: "Buscar documentos existentes", status: "pending", activeForm: "Buscando documentos existentes" },
    { content: "Crear documentos faltantes", status: "pending", activeForm: "Creando documentos faltantes" },
    { content: "Asociar documentos al proyecto", status: "pending", activeForm: "Asociando documentos" },
    { content: "Finalizar tracking de sesión", status: "pending", activeForm: "Finalizando tracking" },
    { content: "Generar resultado final", status: "pending", activeForm: "Generando resultado final" }
  ]
})

console.log("═".repeat(60))
console.log("🚀 INICIANDO: Constitution - Crear Proyecto")
console.log("═".repeat(60))
```

---

## 📥 Input

```
/011-constitution-create-project Crear una API REST en Golang para gestión de inventario
```

O con archivo:
```
/011-constitution-create-project /path/to/descripcion.md
```

O con tracking pre-existente (para encadenar comandos):
```json
{
  "description": "Crear una API REST en Golang...",
  "session_id": "sess-xxx"  // Opcional: reutilizar sesión existente
}
```

## 📤 Output Final

```json
{
  "success": true,
  "project_id": "proj-xxx",
  "project_slug": "api-inventario",
  "project_name": "API de Inventario",
  "tech": "golang",
  "kind": "api",
  "project_level": "standard",
  "folder_path": "/path/to/project",
  "analysis": {
    "config": {
      "threshold_code_review": 70,
      "threshold_qa": 80,
      "max_alt_flow_depth": 2,
      "auto_retry_dev_attempts": 1
    },
    "limits": { "max_sprints": 3, "max_flow_rows_per_sprint": 5, "max_total_tasks": 50 },
    "complexity": "medium",
    "requirements": ["API REST", "Autenticación", "Base de datos"]
  },
  "documents": {
    "created": 3,
    "existing": 1,
    "associated": 4,
    "failed": 0
  },
  "tracking": {
    "session_id": "sess-xxx",
    "status": "completed"
  },
  "next_steps": {
    "immediate": "Ejecutar /021-deep-analysis-create-sprint"
  }
}
```

## ⚠️ ADVERTENCIAS CRÍTICAS

### 🚫 NO usar Task() en paralelo para creación de documentos

**PROHIBIDO**:
```typescript
// ❌ MAL: Crear documentos en paralelo
const promises = documentsToCreate.map(doc =>
  Task({ subagent_type: "document-loader", ... })
)
await Promise.all(promises)
```

**CORRECTO**:
```typescript
// ✅ BIEN: Crear documentos secuencialmente
for (const doc of documentsToCreate) {
  await Task({ subagent_type: "document-loader", ... })
}
```

**Razón**: Race conditions en creación de tags duplicados causan pérdida de documentos.

---

### FASE 0: Validar MCP

```typescript
// OBLIGATORIO: Cargar herramientas MCP ANTES de usarlas (Best Practice)
// MCPSearch valida conectividad Y carga las herramientas necesarias

// Actualizar TODO: FASE 0 en progreso
await TodoWrite({
  todos: [
    { content: "Validar conectividad MCP", status: "in_progress", activeForm: "Validando conectividad MCP" },
    { content: "Preprocesar input del usuario", status: "pending", activeForm: "Preprocesando input" },
    { content: "Iniciar tracking de sesión", status: "pending", activeForm: "Iniciando tracking" },
    { content: "Analizar descripción del proyecto", status: "pending", activeForm: "Analizando descripción" },
    { content: "Crear proyecto en base de datos", status: "pending", activeForm: "Creando proyecto" },
    { content: "Vincular tracking al proyecto", status: "pending", activeForm: "Vinculando tracking" },
    { content: "Determinar documentos necesarios", status: "pending", activeForm: "Determinando documentos" },
    { content: "Buscar documentos existentes", status: "pending", activeForm: "Buscando documentos existentes" },
    { content: "Crear documentos faltantes", status: "pending", activeForm: "Creando documentos faltantes" },
    { content: "Asociar documentos al proyecto", status: "pending", activeForm: "Asociando documentos" },
    { content: "Finalizar tracking de sesión", status: "pending", activeForm: "Finalizando tracking" },
    { content: "Generar resultado final", status: "pending", activeForm: "Generando resultado final" }
  ]
})

// Cargar herramientas MCP necesarias para este comando
const mcpTools = await MCPSearch({ query: "select:mcp__MCPEco__create_project", max_results: 1 })
if (!mcpTools || mcpTools.length === 0) {
  return {
    success: false,
    error_code: "ERR_MCP_UNAVAILABLE",
    error_message: "MCP no disponible: no se pudo cargar mcp__MCPEco__create_project",
    suggestion: "Verificar que el servidor MCP esté corriendo y configurado correctamente"
  }
}

// Cargar herramientas adicionales que se usarán en fases posteriores
await MCPSearch({ query: "select:mcp__MCPEco__execution_session_manage", max_results: 1 })
await MCPSearch({ query: "select:mcp__MCPEco__get_document", max_results: 1 })

console.log("[FASE 0] ✅ MCP validado y herramientas cargadas")
```

### FASE 1: Preprocesar Input

```typescript
console.log("[FASE 1] Preprocesando input del usuario...")

// Validar que $ARGUMENTS no esté vacío (fail-fast)
if (!$ARGUMENTS?.trim()) {
  return {
    success: false,
    error_code: "ERR_EMPTY_ARGUMENTS",
    error_message: "Argumento requerido: descripcion del proyecto",
    suggestion: "Uso: /011-constitution-create-project <descripcion del proyecto o ruta a archivo .md>"
  }
}

let projectDescription = $ARGUMENTS

if ($ARGUMENTS.includes('.md')) {
  const file = await Read({ file_path: $ARGUMENTS })
  projectDescription = file.content
}

const folderPath = process.cwd()

console.log("[FASE 1] Input preprocesado correctamente")
```

### FASE 2: Iniciar o Reutilizar Tracking (MODO RESILIENTE)

```typescript
console.log("[FASE 2] Iniciando tracking de sesion...")

// Extraer session_id del input si viene pre-creado (para encadenar comandos)
const receivedSessionId = typeof $ARGUMENTS === 'object' ? $ARGUMENTS.session_id : null

let SESSION_ID = null
let STEP_ID = null

if (receivedSessionId) {
  // CASO 1: Tracking pre-existente → Reutilizar
  SESSION_ID = receivedSessionId
  console.log(`✓ Reutilizando tracking existente: ${SESSION_ID}`)
  
  // Crear solo un nuevo step dentro de la sesión existente
  const stepResult = await mcp__MCPEco__execution_session_manage({
    action: "start_step",
    session_id: SESSION_ID,
    step_name: "constitution",
    step_order: 1
  })
  STEP_ID = stepResult.step_id
} else {
  // CASO 2: Sin tracking → Crear nuevo
  const sessionResult = await mcp__MCPEco__execution_session_manage({
    action: "start_session",
    command: "011-constitution-create-project",
    provider: "claude",
    trigger_source: "cli"
  })
  SESSION_ID = sessionResult.session_id

  const stepResult = await mcp__MCPEco__execution_session_manage({
    action: "start_step",
    session_id: SESSION_ID,
    step_name: "constitution",
    step_order: 1
  })
  STEP_ID = stepResult.step_id
}

console.log("[FASE 2] Tracking iniciado correctamente")
```

### FASE 3: Analizar Descripcion

```typescript
console.log("[FASE 3] Analizando descripcion del proyecto...")

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: "🔍 FASE 3: Iniciando análisis de descripción del proyecto...",
  log_level: "info"
})

const analyzerResult = await Task({
  subagent_type: "constitution/analyzer-agent",
  description: "Analizar proyecto",
  prompt: JSON.stringify({
    project_description: projectDescription,
    folder_path: folderPath
  })
})

// Validar respuesta del agente
if (!analyzerResult || analyzerResult.status !== "success" || !analyzerResult.analysis) {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: STEP_ID,
    message: `❌ FASE 3: Analyzer agent falló: ${analyzerResult?.error_message || 'Respuesta inválida'}`,
    log_level: "error"
  })
  throw new Error(`Analyzer agent falló: ${analyzerResult?.error_message || 'Respuesta inválida'}`)
}

const analysis = analyzerResult.analysis
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: `✅ FASE 3: Análisis completado - ${analysis.tech}/${analysis.kind}/${analysis.level}`,
  log_level: "info"
})
console.log(`✓ Análisis: ${analysis.tech}/${analysis.kind}/${analysis.level}`)
```

### FASE 4: Crear Proyecto

```typescript
console.log("[FASE 4] Creando proyecto en base de datos...")

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: "🔍 FASE 4: Creando proyecto en base de datos...",
  log_level: "info"
})

const creatorResult = await Task({
  subagent_type: "constitution/project-creator-agent",
  description: "Crear proyecto",
  prompt: JSON.stringify({ analysis })
})

// Validar respuesta del agente
if (!creatorResult || creatorResult.status !== "success" || !creatorResult.project) {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: STEP_ID,
    message: `❌ FASE 4: Project creator falló: ${creatorResult?.error_message || 'Respuesta inválida'}`,
    log_level: "error"
  })
  throw new Error(`Project creator agent falló: ${creatorResult?.error_message || 'Respuesta inválida'}`)
}

const project = creatorResult.project

// Validar que project_id existe
if (!project || !project.project_id) {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: STEP_ID,
    message: "❌ FASE 4: project_id no retornado",
    log_level: "error"
  })
  throw new Error('Project creator agent no retornó project_id válido')
}

const PROJECT_ID = project.project_id
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: `✅ FASE 4: Proyecto creado - ${PROJECT_ID}`,
  log_level: "info"
})
console.log(`✓ Proyecto creado: ${PROJECT_ID}`)
```

### FASE 5: Vincular Tracking

```typescript
console.log("[FASE 5] Vinculando tracking al proyecto...")

await mcp__MCPEco__execution_session_manage({
  action: "link_project",
  session_id: SESSION_ID,
  project_id: PROJECT_ID
})

console.log("[FASE 5] Tracking vinculado correctamente")
```

### FASE 6: Determinar Documentos

```typescript
console.log("[FASE 6] Determinando documentos necesarios...")

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: "🔍 FASE 6: Determinando documentos necesarios según nivel del proyecto...",
  log_level: "info"
})

const finderResult = await Task({
  subagent_type: "constitution/document-finder-agent",
  description: "Buscar documentos",
  prompt: JSON.stringify({ project })
})

// Validar respuesta del agente
if (!finderResult || finderResult.status !== "success" || !finderResult.required_documents) {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: STEP_ID,
    message: `❌ FASE 6: Document finder falló: ${finderResult?.error_message || 'Respuesta inválida'}`,
    log_level: "error"
  })
  throw new Error(`Document finder agent falló: ${finderResult?.error_message || 'Respuesta inválida'}`)
}

const requiredDocuments = finderResult.required_documents
const constitutionDoc = finderResult.constitution_document
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: `✅ FASE 6: ${requiredDocuments.length} documentos necesarios identificados`,
  log_level: "info"
})
```

### FASE 7: Buscar Documentos Existentes

```typescript
console.log("[FASE 7] Buscando documentos existentes...")

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: `🔍 FASE 7: Buscando ${requiredDocuments.length} documentos en base de datos local...`,
  log_level: "info"
})

const existingDocuments = []
const documentsToCreate = []

// ⚠️ NOTA: Las búsquedas SÍ pueden ser en paralelo (solo lectura, sin race condition)
// Pero por simplicidad y debugging, usar secuencial también es válido
for (const doc of requiredDocuments) {
  try {
    const searchResult = await Task({
      subagent_type: "common/search-local",
      description: `Buscar ${doc.id}`,
      prompt: JSON.stringify({
        search_method: "semantic",
        query: doc.search_queries.local.query,
        min_similarity: 0.5
      })
    })

    // Validar respuesta del agente
    if (searchResult && searchResult.status === "success" && searchResult.documents_found > 0) {
      existingDocuments.push({
        document_id: searchResult.results[0].document_id,
        ...doc
      })
      console.log(`✓ Encontrado: ${doc.id}`)
    } else {
      documentsToCreate.push(doc)
      console.log(`○ No encontrado: ${doc.id} → Se creará`)
    }
  } catch (error) {
    // Si falla la búsqueda, agregar a la lista de crear
    console.log(`⚠ Error buscando ${doc.id}: ${error.message} → Se creará`)
    documentsToCreate.push(doc)
  }
}

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: `✅ FASE 7: ${existingDocuments.length} existentes, ${documentsToCreate.length} por crear`,
  log_level: "info"
})
```

### FASE 8: Crear Documentos Faltantes

```typescript
console.log("[FASE 8] Creando documentos faltantes...")

// Limite de documentos para prevenir timeouts
const MAX_DOCUMENTS = 10
if (documentsToCreate.length > MAX_DOCUMENTS) {
  console.log(`[FASE 8] ADVERTENCIA: Limitando a ${MAX_DOCUMENTS} documentos (de ${documentsToCreate.length} solicitados)`)
  documentsToCreate = documentsToCreate.slice(0, MAX_DOCUMENTS)
}

const totalDocuments = documentsToCreate.length + 1
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: `🔍 FASE 8: Creando ${totalDocuments} documentos (1 constitución + ${documentsToCreate.length} técnicos)...`,
  log_level: "info"
})

const createdDocuments = []
const failedDocuments = []

// ============================================================================
// PARTE A: SIEMPRE CREAR DOCUMENTO DE CONSTITUCIÓN
// ============================================================================
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: `📝 [1/${totalDocuments}] Creando documento de constitución...`,
  log_level: "info"
})

const constitutionContent = generateConstitutionContent(project, analysis, projectDescription)
const constResult = await Task({
  subagent_type: "common/document-loader",
  description: "Crear constitución",
  prompt: JSON.stringify({ content: constitutionContent })
})

// Validar que document_id existe
if (!constResult?.result?.document_id) {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: STEP_ID,
    message: "❌ FASE 8: document-loader no retornó document_id para constitución",
    log_level: "error"
  })
  throw new Error('Failed to create constitution document: document_id no retornado')
}

// NUEVO: Verificar que el documento REALMENTE existe en la BD
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: `🔍 [1/${totalDocuments}] Verificando constitución en BD: ${constResult.result.document_id}`,
  log_level: "info"
})

const constVerify = await mcp__MCPEco__get_document({
  document_id: constResult.result.document_id
})

if (!constVerify || !constVerify.document_id) {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: STEP_ID,
    message: `❌ CRÍTICO: Constitución reportada como creada pero NO existe en BD: ${constResult.result.document_id}`,
    log_level: "error"
  })
  throw new Error(`Constitution document not found in DB after creation: ${constResult.result.document_id}`)
}

createdDocuments.push({ 
  document_id: constResult.result.document_id, 
  title: constVerify.title,
  priority: 0,
  type: 'constitution'
})

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: `✅ [1/${totalDocuments}] Constitución creada y verificada: ${constResult.result.document_id}`,
  log_level: "info"
})

// ============================================================================
// PARTE B: CREAR DOCUMENTOS TÉCNICOS FALTANTES (SECUENCIAL)
// ============================================================================
// ⚠️ CRÍTICO: Crear documentos UNO POR UNO (secuencial), NO en paralelo
// Razón: Evitar race conditions en creación de tags duplicados en document-loader
// Si documento 1 y 2 usan tag "swift", crear en paralelo causaría duplicate key error

for (let i = 0; i < documentsToCreate.length; i++) {
  const doc = documentsToCreate[i]
  const docNumber = i + 2 // +2 porque la constitución es el #1

  try {
    // -----------------------------------------------------------------------
    // Paso 1: Buscar contenido en internet
    // -----------------------------------------------------------------------
    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: STEP_ID,
      message: `📝 [${docNumber}/${totalDocuments}] Buscando contenido en internet para: ${doc.id}`,
      log_level: "info"
    })

    const internetResult = await Task({
      subagent_type: "common/search-internet",
      description: `Internet ${doc.id}`,
      prompt: JSON.stringify({ query: doc.search_queries.internet.query })
    })

    // Detectar si search-internet falló
    let content
    if (!internetResult || internetResult.status === "error" || !internetResult.results?.content) {
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: STEP_ID,
        message: `⚠️  [${docNumber}/${totalDocuments}] Búsqueda internet falló para ${doc.id}, usando contenido fallback`,
        log_level: "warn"
      })
      content = generateFallbackContent(doc)
    } else {
      content = internetResult.results.content
      await mcp__MCPEco__execution_session_manage({
        action: "log",
        step_id: STEP_ID,
        message: `✅ [${docNumber}/${totalDocuments}] Contenido obtenido de internet para ${doc.id}`,
        log_level: "info"
      })
    }

    // -----------------------------------------------------------------------
    // Paso 2: Crear documento
    // -----------------------------------------------------------------------
    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: STEP_ID,
      message: `📝 [${docNumber}/${totalDocuments}] Creando documento: ${doc.id}`,
      log_level: "info"
    })

    const loadResult = await Task({
      subagent_type: "common/document-loader",
      description: `Cargar ${doc.id}`,
      prompt: JSON.stringify({ content })
    })

    // Validar el resultado de document-loader
    if (!loadResult || loadResult.status !== "success") {
      throw new Error(`document-loader falló: ${loadResult?.error_message || 'Status no es success'}`)
    }

    if (!loadResult.result?.document_id) {
      throw new Error('document-loader no retornó document_id')
    }

    // -----------------------------------------------------------------------
    // Paso 3: VERIFICAR que el documento existe en la BD
    // -----------------------------------------------------------------------
    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: STEP_ID,
      message: `🔍 [${docNumber}/${totalDocuments}] Verificando ${doc.id} en BD: ${loadResult.result.document_id}`,
      log_level: "info"
    })

    const verifyResult = await mcp__MCPEco__get_document({
      document_id: loadResult.result.document_id
    })

    if (!verifyResult || !verifyResult.document_id) {
      throw new Error(`Documento reportado creado pero NO existe en BD: ${loadResult.result.document_id}`)
    }

    // -----------------------------------------------------------------------
    // Paso 4: Agregar a la lista de creados
    // -----------------------------------------------------------------------
    createdDocuments.push({ 
      document_id: loadResult.result.document_id,
      title: verifyResult.title,
      ...doc 
    })

    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: STEP_ID,
      message: `✅ [${docNumber}/${totalDocuments}] Documento creado y verificado: ${doc.id} → ${loadResult.result.document_id}`,
      log_level: "info"
    })

  } catch (error) {
    // -----------------------------------------------------------------------
    // Manejo de errores: Registrar y continuar (NO lanzar)
    // -----------------------------------------------------------------------
    failedDocuments.push({
      id: doc.id,
      error: error.message,
      timestamp: new Date().toISOString()
    })

    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: STEP_ID,
      message: `❌ [${docNumber}/${totalDocuments}] FALLÓ crear documento ${doc.id}: ${error.message}`,
      log_level: "error"
    })
    
    // CONTINUAR con el siguiente documento (no throw)
  }
}

// ============================================================================
// RESUMEN FINAL DE FASE 8
// ============================================================================
const successCount = createdDocuments.length
const failedCount = failedDocuments.length
const attemptedCount = totalDocuments

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: `✅ FASE 8: ${successCount}/${attemptedCount} documentos creados exitosamente, ${failedCount} fallaron`,
  log_level: "info"
})

if (failedCount > 0) {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: STEP_ID,
    message: `⚠️  FASE 8: Documentos que fallaron: ${failedDocuments.map(f => f.id).join(', ')}`,
    log_level: "warn"
  })
}

// Si la constitución está entre los creados, continuar aunque hayan fallado documentos técnicos
// Si la constitución NO fue creada, ya habríamos lanzado error arriba
if (successCount === 0) {
  throw new Error('FASE 8: Ningún documento fue creado exitosamente')
}


### FASE 9: Asociar Documentos

```typescript
console.log("[FASE 9] Asociando documentos al proyecto...")

const allDocuments = [
  ...existingDocuments.map(d => ({ ...d, source: "existing" })),
  ...createdDocuments.map(d => ({ ...d, source: "created" }))
]

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: `🔍 FASE 9: Asociando ${allDocuments.length} documentos al proyecto...`,
  log_level: "info"
})

const associatorResult = await Task({
  subagent_type: "constitution/document-associator-agent",
  description: "Asociar documentos",
  prompt: JSON.stringify({
    project_id: PROJECT_ID,
    project_level: project.project_level,
    documents: allDocuments
  })
})

// Validar respuesta del agente
if (!associatorResult || associatorResult.status !== "success" || !associatorResult.associated) {
  await mcp__MCPEco__execution_session_manage({
    action: "log",
    step_id: STEP_ID,
    message: `❌ FASE 9: Document associator falló: ${associatorResult?.error_message || 'Respuesta inválida'}`,
    log_level: "error"
  })
  throw new Error(`Document associator agent falló: ${associatorResult?.error_message || 'Respuesta inválida'}`)
}

const associatedCount = associatorResult.associated.successful
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: `✅ FASE 9: ${associatedCount} documentos asociados exitosamente`,
  log_level: "info"
})
```

### FASE 10: Finalizar Tracking

```typescript
console.log("[FASE 10] Finalizando tracking de sesion...")

await mcp__MCPEco__execution_session_manage({
  action: "complete_step",
  step_id: STEP_ID
})

await mcp__MCPEco__execution_session_manage({
  action: "finish_session",
  session_id: SESSION_ID,
  summary: `Proyecto ${project.project_name} creado con ${associatedCount} documentos`
})

console.log("[FASE 10] Tracking finalizado correctamente")
```

### FASE 11: Retornar Resultado

```typescript
console.log("[FASE 11] Generando resultado final...")

console.log("=".repeat(60))
console.log("✅ PROYECTO CREADO EXITOSAMENTE")
console.log(`📁 ${project.project_name}`)
console.log(`🆔 ${PROJECT_ID}`)
console.log(`🔧 ${project.tech}/${project.kind}/${project.project_level}`)
console.log(`📄 ${associatedCount} documentos asociados`)
console.log("=".repeat(60))

return {
  success: true,
  project_id: PROJECT_ID,
  project_slug: project.project_slug,
  project_name: project.project_name,
  tech: project.tech,
  kind: project.kind,
  project_level: project.project_level,
  folder_path: project.folder_path,
  // NUEVO: Incluir datos de análisis para sistemas downstream
  analysis: {
    config: analysis.config,
    limits: analysis.limits,
    complexity: analysis.complexity,
    requirements: analysis.requirements
  },
  documents: {
    created: createdDocuments.length,
    existing: existingDocuments.length,
    associated: associatedCount,
    failed: 0
  },
  tracking: { session_id: SESSION_ID, status: "completed" },
  next_steps: { immediate: "Ejecutar /021-deep-analysis-create-sprint" },
  timestamp: new Date().toISOString()
}

console.log("=".repeat(60))
console.log("COMANDO FINALIZADO")
console.log("=".repeat(60))
```

## 🚨 Manejo de Errores

### Errores por Fase

Si ocurre un error en cualquier fase:

```typescript
try {
  // Código de la fase
} catch (error) {
  // 1. Loguear el error si tenemos STEP_ID
  if (STEP_ID) {
    await mcp__MCPEco__execution_session_manage({
      action: "log",
      step_id: STEP_ID,
      message: `❌ Error en FASE X: ${error.message}`,
      log_level: "error"
    })
  }
  
  // 2. Marcar sesión como fallida si tenemos SESSION_ID
  if (SESSION_ID) {
    await mcp__MCPEco__execution_session_manage({
      action: "fail",
      session_id: SESSION_ID,
      error_message: error.message
    })
  }
  
  // 3. Retornar error estructurado (formato estandarizado)
  return {
    success: false,
    error_code: "ERR_PHASE_X",
    error_phase: "FASE_X",
    error_message: error.message,
    suggestion: "Verificar logs de la sesión para más detalles",
    tracking: SESSION_ID ? { session_id: SESSION_ID, status: "failed" } : null
  }
}
```

### Validación de Respuestas de Agentes

Todas las respuestas de agentes deben validarse:

```typescript
function validateAgentResponse(result, requiredFields) {
  if (!result) {
    throw new Error('Agente no retornó respuesta')
  }
  if (result.status === 'error') {
    throw new Error(result.error_message || 'Error del agente')
  }
  for (const field of requiredFields) {
    if (!(field in result)) {
      throw new Error(`Campo requerido faltante: ${field}`)
    }
  }
  return true
}

// Uso:
validateAgentResponse(analyzerResult, ['status', 'analysis'])
validateAgentResponse(creatorResult, ['status', 'project'])
validateAgentResponse(finderResult, ['status', 'required_documents'])
validateAgentResponse(associatorResult, ['status', 'associated'])
```

## 📋 Funciones Auxiliares

### generateConstitutionContent()

```typescript
function generateConstitutionContent(project, analysis, description) {
  return `# Documento de Constitución: ${project.project_name}

## Prompt Original
${description}

## Metadata del Proyecto
- **Project ID**: ${project.project_id}
- **Slug**: ${project.project_slug}
- **Tecnología**: ${project.tech}
- **Tipo**: ${project.kind}
- **Nivel**: ${project.project_level}
- **Ruta**: ${project.folder_path}

## Configuración
- **Threshold Code Review**: ${analysis.config.threshold_code_review}%
- **Threshold QA**: ${analysis.config.threshold_qa}%
- **Max Fix Depth**: ${analysis.config.max_alt_flow_depth}

## Límites del Proyecto
- **Max Sprints**: ${analysis.limits.max_sprints}
- **Max Flow Rows por Sprint**: ${analysis.limits.max_flow_rows_per_sprint}
- **Max Tasks Total**: ${analysis.limits.max_total_tasks}

## Resúmenes por Step

### Para Planner
Proyecto ${project.project_name} (${project.tech}/${project.kind}). Nivel: ${project.project_level}. 
Límites: máximo ${analysis.limits.max_sprints} sprints, ${analysis.limits.max_flow_rows_per_sprint} flow_rows/sprint.
Mantener tasks atómicas y bien definidas.

### Para Implementer
Implementar código para ${project.project_name}. Stack: ${project.tech}. 
Tipo: ${project.kind}. Seguir mejores prácticas de ${project.tech}.
Thresholds de calidad: CR ${analysis.config.threshold_code_review}%, QA ${analysis.config.threshold_qa}%.

### Para Code Review
Revisar código de ${project.project_name} (${project.tech}).
Threshold de aprobación: ${analysis.config.threshold_code_review}%.
Verificar: seguridad, performance, mantenibilidad, adherencia a patrones.

### Para QA
Testing de ${project.project_name}. Stack: ${project.tech}, Tipo: ${project.kind}.
Threshold de aprobación: ${analysis.config.threshold_qa}%.
Validar: funcionalidad, edge cases, integración, regresión.
`
}
```

### generateFallbackContent()

```typescript
function generateFallbackContent(doc) {
  return `# ${doc.title}

> Documento generado automáticamente. Actualizar con contenido específico.

## Información
- **ID**: ${doc.id}
- **Prioridad**: ${doc.priority}
- **Keywords**: ${doc.keywords.join(', ')}

## Contenido Pendiente

Este documento requiere contenido específico para:
${doc.keywords.map(k => `- ${k}`).join('\n')}

## Notas
Documento creado como placeholder. Se recomienda actualizar con documentación real.
`
}
```

## 📊 Diagrama de Flujo

```
FASE -1 → TodoWrite (Inicializar tracking visual)
FASE 0  → MCPSearch (Validar y cargar herramientas MCP)
FASE 1  → Read (si es archivo)
FASE 2  → MCP (execution_session_manage) [MODO RESILIENTE]
FASE 3  → constitution/analyzer-agent
FASE 4  → constitution/project-creator-agent
FASE 5  → MCP (link_project)
FASE 6  → constitution/document-finder-agent
FASE 7  → common/search-local (×N)
FASE 8  → common/search-internet + document-loader (×N)
FASE 9  → constitution/document-associator-agent
FASE 10 → MCP (finish_session)
FASE 11 → Resultado (incluye analysis)
```

## 🚫 Lo que NO Hace

- NO analiza descripcion (analyzer-agent)
- NO crea proyectos directamente (project-creator-agent)
- NO busca documentos directamente (search-local, search-internet)
- NO crea documentos directamente (document-loader)
- NO asocia documentos directamente (document-associator-agent)

## ✅ Lo que SI Hace

- Orquesta el flujo completo
- Maneja tracking (inicio, logs, fin) con modo resiliente
- Pasa datos entre agentes
- Maneja errores en cada fase
- Reporta resultado consolidado con analysis

---

## Checklist de Mejores Practicas

### Herramientas MCP
- [ ] Todas las herramientas MCP declaradas en `allowed-tools`
- [ ] MCPSearch ejecutado ANTES de usar cualquier herramienta MCP
- [ ] Herramientas cargadas en FASE 0 (no durante ejecucion)

### Tracking y Logging
- [ ] TodoWrite inicializado en FASE -1
- [ ] Logging con formato `[FASE N]` consistente
- [ ] Separadores visuales al inicio y fin del comando
- [ ] Timestamp incluido en output final

### Validacion de Input
- [ ] $ARGUMENTS validado como no vacio
- [ ] Estructura JSON validada si aplica
- [ ] Fail-fast para inputs invalidos

### Manejo de Errores
- [ ] Estructura de error estandarizada: `{success, error_code, error_message, suggestion}`
- [ ] Errores logueados via execution_session_manage
- [ ] Session marcada como failed en caso de error

### Agentes
- [ ] Respuestas de agentes validadas (status, campos requeridos)
- [ ] Errores de agentes propagados correctamente
- [ ] Datos pasados entre agentes en formato JSON

### Limites y Seguridad
- [ ] MAX_DOCUMENTS aplicado para prevenir timeouts
- [ ] Documentos creados secuencialmente (no en paralelo)
- [ ] Verificacion de existencia post-creacion

---

## Testing

### Caso 1: Proyecto Simple (MVP)

**Input:**
```
/011-constitution-create-project Crear una landing page simple en HTML/CSS
```

**Output Esperado:**
```json
{
  "success": true,
  "project_level": "mvp",
  "tech": "html",
  "kind": "web",
  "documents": { "created": 3, "existing": 0, "associated": 3, "failed": 0 }
}
```

### Caso 2: Proyecto Standard (API)

**Input:**
```
/011-constitution-create-project Crear una API REST en Golang para gestion de inventario con autenticacion JWT
```

**Output Esperado:**
```json
{
  "success": true,
  "project_level": "standard",
  "tech": "golang",
  "kind": "api",
  "documents": { "created": 4, "existing": 0, "associated": 4, "failed": 0 }
}
```

### Caso 3: Proyecto Enterprise

**Input:**
```
/011-constitution-create-project Crear un sistema de microservicios en Kubernetes con observabilidad, seguridad enterprise y CI/CD completo
```

**Output Esperado:**
```json
{
  "success": true,
  "project_level": "enterprise",
  "tech": "kubernetes",
  "kind": "infrastructure",
  "documents": { "created": 6, "existing": 0, "associated": 6, "failed": 0 }
}
```

### Caso 4: Error - Input Vacio

**Input:**
```
/011-constitution-create-project
```

**Output Esperado:**
```json
{
  "success": false,
  "error_code": "ERR_EMPTY_ARGUMENTS",
  "error_message": "Argumento requerido: descripcion del proyecto",
  "suggestion": "Uso: /011-constitution-create-project <descripcion del proyecto o ruta a archivo .md>"
}
```

### Caso 5: Error - MCP No Disponible

**Condicion:** Servidor MCP no configurado o caido

**Output Esperado:**
```json
{
  "success": false,
  "error_code": "ERR_MCP_UNAVAILABLE",
  "error_message": "MCP no disponible: no se pudo cargar mcp__MCPEco__create_project",
  "suggestion": "Verificar que el servidor MCP este corriendo y configurado correctamente"
}
```

### Validacion Manual

Para validar que el comando funciona correctamente:

1. **Pre-requisitos:**
   - Servidor MCP corriendo (`make dev-mcp`)
   - Base de datos con migraciones aplicadas (`make migrate`)

2. **Ejecutar comando:**
   ```bash
   # Desde Claude Code
   /011-constitution-create-project Crear una API REST en Python para gestion de tareas
   ```

3. **Verificar en BD:**
   ```sql
   -- Verificar proyecto creado
   SELECT * FROM projects ORDER BY created_at DESC LIMIT 1;
   
   -- Verificar documentos asociados
   SELECT d.title, pd.priority 
   FROM project_documents pd 
   JOIN documents d ON pd.document_id = d.document_id 
   WHERE pd.project_id = '<project_id>';
   ```

---

**Version**: 1.6
**Cambios**:
- v1.6: **Mejoras BAJA** - Agregado Glob/Grep a allowed-tools (M011), seccion Testing con casos de prueba (M013).
- v1.5: **Mejoras MEDIA** - Logging estructurado con [FASE N], validacion de $ARGUMENTS vacio, MAX_DOCUMENTS=10, checklist de mejores practicas, timestamp en output.
- v1.4: **Best Practices** - Agregada FASE -1 con TodoWrite para tracking visual. FASE 0 ahora usa MCPSearch directamente en lugar de mcp-validator agent.
- v1.3: **CRITICO** - Agregada advertencia sobre creacion secuencial (NO paralela) de documentos en FASE 8. Previene race conditions en tags duplicados.
- v1.2: Modo resiliente de tracking + analysis en resultado final
- v1.1: Agentes Troceados + Manejo de Errores Mejorado
