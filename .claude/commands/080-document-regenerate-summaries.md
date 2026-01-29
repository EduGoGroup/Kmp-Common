---
name: 080-document-regenerate-summaries
description: Regenera todos los summaries de un documento usando agente especializado
color: purple
allowed-tools: Read, MCPSearch, Task, TodoWrite, mcp__MCPEco__get_document, mcp__MCPEco__update_summary, mcp__MCPEco__execution_session_manage
---

# Document: Regenerar Summaries

## 📥 Input del Usuario

```bash
/080-document-regenerate-summaries <document_id>
```

**Parámetros**:
- `document_id` (string, obligatorio): ID único del documento a procesar

**Ejemplos**:
```bash
# Regenerar summaries para un documento específico
/080-document-regenerate-summaries doc-guia-testing-ios-abc123

# Después de editar un documento manualmente
/080-document-regenerate-summaries doc-plan-autenticacion-jwt-def456
```

## 🎯 Flujo de Orquestación

Este comando orquesta la regeneración de summaries en **7 fases**:

1. **FASE -2**: Inicializar TODO List
2. **FASE -1**: Cargar Herramientas MCP (get_document, update_summary, execution_session_manage)
3. **FASE 0**: Validar que MCP MCPEco esté activo
4. **FASE 1**: Preprocesar Input y validar document_id
5. **FASE 2**: Iniciar Tracking de Ejecución (llm_execution_sessions)
6. **FASE 3**: Obtener Documento Completo
7. **FASE 4**: Invocar Agente Regenerador
8. **FASE 5**: Finalizar Tracking
9. **FASE 6**: Retornar Resultado

## Variables Globales

**NOTA: El siguiente codigo es pseudo-codigo ilustrativo. Claude debe interpretar la logica y ejecutar las herramientas correspondientes.**

```javascript
// Variables globales del comando
let SESSION_ID = null;
let CURRENT_STEP_ID = null;
let documentId = null;
let document = null;
```

## Manejo de Errores Global

**Patron try-catch para todo el flujo:**

```javascript
// PSEUDO-CODIGO: Wrapper de error handling para el comando completo
try {
  // Ejecutar FASES -2 a 6 (flujo principal)
  await ejecutarFlujoCompleto();
} catch (error) {
  // 1. Loggear error en session si existe
  if (SESSION_ID) {
    await mcp__MCPEco__execution_session_manage({
      action: "update_session_status",
      session_id: SESSION_ID,
      status: "failed",
      error_message: error.message
    });
  }
  
  // 2. Actualizar TODOs mostrando fallo
  await TodoWrite({
    todos: [
      // ... todos anteriores como completed o failed segun corresponda
      { content: "Error en ejecucion", activeForm: "Error en ejecucion", status: "in_progress" }
    ]
  });
  
  // 3. Retornar error estructurado
  return {
    success: false,
    error: error.message,
    error_code: error.code || "ERR_UNKNOWN",
    tracking: {
      session_id: SESSION_ID,
      status: "failed"
    }
  };
}
```

## 🔄 Flujo de Ejecución

### FASE -2: Inicializar TODO List

```javascript
TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP necesarias", activeForm: "Cargando herramientas MCP necesarias", status: "in_progress" },
    { content: "Validar que MCP MCPEco esté activo", activeForm: "Validando que MCP MCPEco esté activo", status: "pending" },
    { content: "Obtener documento completo", activeForm: "Obteniendo documento completo", status: "pending" },
    { content: "Regenerar summaries con agente", activeForm: "Regenerando summaries con agente", status: "pending" },
    { content: "Finalizar tracking de ejecución", activeForm: "Finalizando tracking de ejecución", status: "pending" }
  ]
});
```

### FASE -1: Cargar Herramientas MCP

**Buscar tools necesarios**:

```javascript
MCPSearch({ query: "select:mcp__MCPEco__get_document" });
MCPSearch({ query: "select:mcp__MCPEco__update_summary" });
MCPSearch({ query: "select:mcp__MCPEco__execution_session_manage" });
```

**Validar que se cargaron**:
```javascript
if (!mcp__MCPEco__get_document) {
  throw new Error("❌ Tool mcp__MCPEco__get_document no encontrado. Verifica que el servidor MCP esté corriendo.");
}

if (!mcp__MCPEco__update_summary) {
  throw new Error("❌ Tool mcp__MCPEco__update_summary no encontrado. Verifica que el servidor MCP esté corriendo.");
}

if (!mcp__MCPEco__execution_session_manage) {
  throw new Error("❌ Tool mcp__MCPEco__execution_session_manage no encontrado. Verifica que el servidor MCP esté corriendo.");
}
```

**Actualizar TODO**:
```javascript
TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP necesarias", activeForm: "Cargando herramientas MCP necesarias", status: "completed" },
    { content: "Validar que MCP MCPEco esté activo", activeForm: "Validando que MCP MCPEco esté activo", status: "in_progress" },
    { content: "Obtener documento completo", activeForm: "Obteniendo documento completo", status: "pending" },
    { content: "Regenerar summaries con agente", activeForm: "Regenerando summaries con agente", status: "pending" },
    { content: "Finalizar tracking de ejecución", activeForm: "Finalizando tracking de ejecución", status: "pending" }
  ]
});
```

### FASE 0: Validar que MCP MCPEco esté activo

**Usar agente validador**:

```javascript
const validatorResult = await Task({
  subagent_type: "general-purpose",
  description: "Validar MCP MCPEco activo",
  prompt: `Valida que el servidor MCP MCPEco esté activo y funcionando correctamente.

Ejecuta el siguiente tool de prueba:
- mcp__MCPEco__execution_session_manage con action: "get_stats"

Si el tool funciona correctamente, el servidor está activo.
Si falla, reporta el error y sugiere reiniciar el servidor MCP.

Retorna un objeto JSON:
{
  "success": true/false,
  "error": "mensaje de error si aplica"
}`
});

if (!validatorResult.success) {
  throw new Error(`❌ Servidor MCP MCPEco no está activo: ${validatorResult.error}\n\n💡 Sugerencia: Reinicia el servidor MCP con: make dev-mcp`);
}
```

**Actualizar TODO**:
```javascript
TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP necesarias", activeForm: "Cargando herramientas MCP necesarias", status: "completed" },
    { content: "Validar que MCP MCPEco esté activo", activeForm: "Validando que MCP MCPEco esté activo", status: "completed" },
    { content: "Obtener documento completo", activeForm: "Obteniendo documento completo", status: "in_progress" },
    { content: "Regenerar summaries con agente", activeForm: "Regenerando summaries con agente", status: "pending" },
    { content: "Finalizar tracking de ejecución", activeForm: "Finalizando tracking de ejecución", status: "pending" }
  ]
});
```

### FASE 1: Preprocesar Input

**Extraer document_id**:

```javascript
// Obtener document_id del primer argumento
const args = process.argv.slice(2);
const documentId = args[0];

// Validar que no esté vacío
if (!documentId || documentId.trim() === "") {
  throw new Error(`❌ Error: document_id no puede estar vacío

Uso: /080-document-regenerate-summaries <document_id>

Ejemplo:
  /080-document-regenerate-summaries doc-guia-testing-ios-abc123`);
}

console.log(`📄 Document ID: ${documentId}`);
```

### FASE 2: Iniciar Tracking de Ejecución

**Crear o recuperar session**:

```javascript
let SESSION_ID = null;
let CURRENT_STEP_ID = null;

// Crear nueva session
const sessionResult = await mcp__MCPEco__execution_session_manage({
  action: "create_session",
  command: "/080-document-regenerate-summaries",
  provider: "claude",
  trigger_source: "cli",
  project_id: null, // NULL porque documentos son independientes
  context_metadata: {
    document_id: documentId,
    entity_type: "document"
  }
});

SESSION_ID = sessionResult.session_id;

console.log(`🔄 Session creada: ${SESSION_ID}`);
```

**Funciones helper para tracking**:

```javascript
// Helper: Iniciar un step
async function startStep(stepName, stepOrder) {
  const result = await mcp__MCPEco__execution_session_manage({
    action: "create_step",
    session_id: SESSION_ID,
    step_name: stepName,
    step_order: stepOrder
  });
  CURRENT_STEP_ID = result.step_id;
  return result;
}

// Helper: Registrar log en el step actual
async function logStep(message, logLevel = "info") {
  await mcp__MCPEco__execution_session_manage({
    action: "append_log",
    step_id: CURRENT_STEP_ID,
    message: message,
    log_level: logLevel
  });
}

// Helper: Completar step actual
async function completeStep(success = true, errorMessage = null) {
  await mcp__MCPEco__execution_session_manage({
    action: "complete_step",
    step_id: CURRENT_STEP_ID,
    success: success,
    error_message: errorMessage
  });
}
```

### FASE 3: Obtener Documento Completo

**Iniciar step de obtención**:

```javascript
await startStep("get_document", 1);
await logStep(`Obteniendo documento: ${documentId}`);

let document = null;

try {
  document = await mcp__MCPEco__get_document({ document_id: documentId });
  
  if (!document) {
    throw new Error(`Documento no encontrado: ${documentId}`);
  }
  
  if (!document.content || document.content.trim() === "") {
    throw new Error(`El documento ${documentId} no tiene contenido para analizar`);
  }
  
  await logStep(`Documento obtenido: ${document.title} (${document.content.length} caracteres)`);
  await completeStep(true);
  
} catch (error) {
  await logStep(`Error obteniendo documento: ${error.message}`, "error");
  await completeStep(false, error.message);
  
  // Marcar session como failed
  await mcp__MCPEco__execution_session_manage({
    action: "update_session_status",
    session_id: SESSION_ID,
    status: "failed",
    error_message: error.message
  });
  
  throw new Error(`❌ Error al obtener documento: ${error.message}

💡 Sugerencias:
  1. Verifica que el document_id sea correcto
  2. Usa el tool get_document para buscar documentos disponibles
  3. Verifica que el documento tenga contenido`);
}

TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP necesarias", activeForm: "Cargando herramientas MCP necesarias", status: "completed" },
    { content: "Validar que MCP MCPEco esté activo", activeForm: "Validando que MCP MCPEco esté activo", status: "completed" },
    { content: "Obtener documento completo", activeForm: "Obteniendo documento completo", status: "completed" },
    { content: "Regenerar summaries con agente", activeForm: "Regenerando summaries con agente", status: "in_progress" },
    { content: "Finalizar tracking de ejecución", activeForm: "Finalizando tracking de ejecución", status: "pending" }
  ]
});
```

### FASE 4: Invocar Agente Regenerador

**Iniciar step de regeneración**:

```javascript
await startStep("regenerate_summaries", 2);
await logStep("Invocando agente document-regenerator");

const agentResult = await Task({
  subagent_type: "general-purpose",
  description: "Regenerar summaries del documento",
  prompt: `Regenera todos los summaries del documento usando EXCLUSIVAMENTE el MCP tool update_summary.

🚨 RESTRICCIONES CRÍTICAS:
❌ NO MODIFIQUES NINGÚN ARCHIVO DE CÓDIGO FUENTE
❌ NO CREES ARCHIVOS TEMPORALES .go
❌ NO COMPILES NI EJECUTES CÓDIGO GO
❌ NO MODIFIQUES document_service.go NI NINGÚN OTRO ARCHIVO
❌ NO USES Edit, Write, Bash para modificar código
✅ USA SOLO EL MCP TOOL: mcp__MCPEco__update_summary

**Documento a procesar**:
- ID: ${documentId}
- Título: ${document.title}
- Contenido (${document.content.length} caracteres):

\`\`\`
${document.content}
\`\`\`

**Metadata**:
${JSON.stringify({
  tags: document.tags,
  applies_to_steps: document.applies_to_steps,
  applies_to_kinds: document.applies_to_kinds
}, null, 2)}

**ÚNICO MECANISMO PERMITIDO**:

1. Para cada step type (planner, implementer, code_review, qa, constitution, deep_analysis):
   a) Genera un summary personalizado desde la perspectiva de ese step
   b) El summary debe ser conciso (200-300 palabras)
   c) Debe capturar los aspectos más relevantes del documento para ese step
   
2. Para cada summary generado, invoca el tool MCP (NO HAY OTRA FORMA):
   \`\`\`javascript
   await mcp__MCPEco__update_summary({
     document_id: "${documentId}",
     step_type: "<step>",
     summary: "<summary_generado>"
   })
   \`\`\`

3. El tool update_summary automáticamente:
   - Actualiza el summary en la BD
   - Regenera el embedding del summary usando Ollama
   - NO necesitas hacer NADA más
   
**Steps a procesar** (en orden):
1. planner
2. implementer
3. code_review
4. qa
5. constitution
6. deep_analysis

**PROCESO PASO A PASO**:

PASO 1: Generar summary para "planner"
PASO 2: Invocar mcp__MCPEco__update_summary con step_type="planner"
PASO 3: Generar summary para "implementer"
PASO 4: Invocar mcp__MCPEco__update_summary con step_type="implementer"
... (repetir para los 6 steps)

**Output esperado**:
Retorna un objeto JSON con:
{
  "success": true,
  "summaries_updated": 6,
  "steps_completed": ["planner", "implementer", "code_review", "qa", "constitution", "deep_analysis"],
  "steps_failed": [],
  "total_words": 1650
}

🚨 RECORDATORIO FINAL:
Si intentas modificar código, crear archivos .go temporales, o usar cualquier mecanismo
que NO SEA mcp__MCPEco__update_summary, este proceso FALLARÁ y causará problemas graves
en el sistema. USA SOLO EL MCP TOOL.`
});

if (!agentResult.success) {
  await logStep(`Fallo en regeneración: ${agentResult.error}`, "error");
  await completeStep(false, agentResult.error);
  
  await mcp__MCPEco__execution_session_manage({
    action: "update_session_status",
    session_id: SESSION_ID,
    status: "failed",
    error_message: agentResult.error
  });
  
  throw new Error(`❌ Error al regenerar summaries: ${agentResult.error}`);
}

await logStep(`Regeneración completada: ${agentResult.summaries_updated}/6 summaries actualizados`);
await completeStep(true);

TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP necesarias", activeForm: "Cargando herramientas MCP necesarias", status: "completed" },
    { content: "Validar que MCP MCPEco esté activo", activeForm: "Validando que MCP MCPEco esté activo", status: "completed" },
    { content: "Obtener documento completo", activeForm: "Obteniendo documento completo", status: "completed" },
    { content: "Regenerar summaries con agente", activeForm: "Regenerando summaries con agente", status: "completed" },
    { content: "Finalizar tracking de ejecución", activeForm: "Finalizando tracking de ejecución", status: "in_progress" }
  ]
});
```

### FASE 5: Finalizar Tracking

```javascript
const summary = `Regeneración completada exitosamente: ${agentResult.summaries_updated}/6 summaries actualizados para documento ${document.title}`;

await mcp__MCPEco__execution_session_manage({
  action: "complete_session",
  session_id: SESSION_ID,
  success: true,
  summary: summary
});

TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP necesarias", activeForm: "Cargando herramientas MCP necesarias", status: "completed" },
    { content: "Validar que MCP MCPEco esté activo", activeForm: "Validando que MCP MCPEco esté activo", status: "completed" },
    { content: "Obtener documento completo", activeForm: "Obteniendo documento completo", status: "completed" },
    { content: "Regenerar summaries con agente", activeForm: "Regenerando summaries con agente", status: "completed" },
    { content: "Finalizar tracking de ejecución", activeForm: "Finalizando tracking de ejecución", status: "completed" }
  ]
});
```

### FASE 6: Retornar Resultado Final

```javascript
const result = {
  success: true,
  document_id: documentId,
  document_title: document.title,
  summaries_updated: agentResult.summaries_updated,
  steps_completed: agentResult.steps_completed,
  steps_failed: agentResult.steps_failed || [],
  total_words: agentResult.total_words,
  tracking: {
    session_id: SESSION_ID,
    status: "completed"
  },
  next_steps: [
    "Los summaries han sido actualizados y sus embeddings regenerados",
    "El documento ahora puede ser encontrado por búsqueda semántica",
    "Los agentes pueden usar estos summaries para contexto específico por step"
  ]
};

console.log("\n✅ Regeneración completada exitosamente\n");
console.log(`📄 Documento: ${document.title}`);
console.log(`📊 Summaries actualizados: ${agentResult.summaries_updated}/6`);
console.log(`📝 Total palabras: ${agentResult.total_words}`);
console.log(`🔍 Session ID: ${SESSION_ID}`);

if (agentResult.steps_failed && agentResult.steps_failed.length > 0) {
  console.log(`\n⚠️  Advertencia: ${agentResult.steps_failed.length} steps fallaron:`);
  agentResult.steps_failed.forEach(step => console.log(`  - ${step}`));
}

console.log("\n📋 Próximos pasos:");
result.next_steps.forEach(step => console.log(`  • ${step}`));

return result;
```

## 🚫 Lo que NO Hace Este Comando

- ❌ NO crea nuevos documentos
- ❌ NO modifica el contenido del documento
- ❌ NO asocia el documento a entidades (projects, flows, etc.)
- ❌ NO valida la calidad del contenido
- ❌ NO traduce documentos
- ❌ NO genera documentación desde código

## ✅ Lo que SÍ Hace Este Comando

- ✅ Obtiene el documento completo por ID
- ✅ Invoca agente especializado para generar 6 summaries
- ✅ Actualiza cada summary en la base de datos
- ✅ Regenera embeddings automáticamente
- ✅ Registra la ejecución en llm_execution_sessions
- ✅ Maneja errores y tracking completo
- ✅ Retorna resultado estructurado con métricas

## 📋 Agentes Utilizados

1. **Validador MCP** (`general-purpose`): Valida que el servidor MCP esté activo
2. **Regenerador de Summaries** (`general-purpose`): Genera los 6 summaries y los actualiza

## 📤 Output Esperado

### Caso Éxito

```json
{
  "success": true,
  "document_id": "doc-guia-testing-ios-abc123",
  "document_title": "Guía de Testing para Aplicaciones iOS",
  "summaries_updated": 6,
  "steps_completed": [
    "planner",
    "implementer",
    "code_review",
    "qa",
    "constitution",
    "deep_analysis"
  ],
  "steps_failed": [],
  "total_words": 1650,
  "tracking": {
    "session_id": "exec-session-regenerate-summaries-xyz789",
    "status": "completed"
  },
  "next_steps": [
    "Los summaries han sido actualizados y sus embeddings regenerados",
    "El documento ahora puede ser encontrado por búsqueda semántica",
    "Los agentes pueden usar estos summaries para contexto específico por step"
  ]
}
```

### Caso Error

```json
{
  "success": false,
  "error": "Documento no encontrado: doc-inexistente-123",
  "tracking": {
    "session_id": "exec-session-regenerate-summaries-xyz789",
    "status": "failed"
  }
}
```

## 🔗 Integración con Sistema

Este comando se invoca automáticamente cuando:
- Usuario edita documento en Web UI (`/document/{id}/edit`)
- `regenerateSummariesAsync()` ejecuta este comando en background
- Se registra la ejecución en `llm_execution_sessions` con `project_id=NULL` y `document_id` en metadata
