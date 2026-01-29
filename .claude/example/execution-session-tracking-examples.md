# Ejemplos de Uso: Execution Session Tracking (MCP Directo)

**Referencia**: ADR-002 - Decisión sobre Execution Session Tracker  
**Fecha**: 2026-01-14  
**MCP Tool**: `mcp__MCPEco__execution_session_manage`

---

## 📋 Resumen

Según ADR-002, el tracking de sesiones debe hacerse **llamando directamente al MCP tool**, sin usar agentes intermediarios. Esto elimina contexto innecesario y mejora la eficiencia.

---

## 🔧 Acciones Disponibles

| Acción | Propósito | Campos Requeridos |
|--------|-----------|-------------------|
| `start_session` | Iniciar nueva sesión | command, provider, trigger_source |
| `start_step` | Iniciar paso dentro de sesión | session_id, step_name, step_order |
| `log` | Registrar mensaje de progreso | step_id, message |
| `complete_step` | Finalizar paso | step_id |
| `link_project` | Vincular project_id a sesión | session_id, project_id |
| `warn` | Warning no fatal | step_id, message |
| `fail` | Error fatal (termina sesión) | session_id, error_message |
| `finish_session` | Completar sesión exitosamente | session_id, summary |

---

## 📖 Ejemplos por Acción

### 1. `start_session` - Iniciar Sesión

```typescript
// Iniciar sesión SIN project_id (proyecto aún no existe)
const startResult = await mcp__MCPEco__execution_session_manage({
  action: "start_session",
  command: "constitution-create-project",
  provider: "claude",
  trigger_source: "cli",
  project_id: null
})

// Respuesta:
// {
//   "success": true,
//   "session_id": "sess-abc-123",
//   "message": "Sesión iniciada"
// }

const SESSION_ID = startResult.session_id
```

**Campos:**
- `command` (string, requerido): Nombre del comando que inicia la sesión
- `provider` (string, requerido): "claude" | "gemini" | "copilot"
- `trigger_source` (string, requerido): "cli" | "web" | "orchestrator" | "auto"
- `project_id` (string | null, opcional): ID del proyecto si ya existe

---

### 2. `start_step` - Iniciar Paso

```typescript
// Iniciar primer paso de la sesión
const step1Result = await mcp__MCPEco__execution_session_manage({
  action: "start_step",
  session_id: SESSION_ID,
  step_name: "Análisis de proyecto",
  step_order: 1
})

// Respuesta:
// {
//   "success": true,
//   "step_id": "step-xyz-789",
//   "message": "Paso iniciado"
// }

const STEP_ID = step1Result.step_id
```

**Campos:**
- `session_id` (string, requerido): ID de la sesión
- `step_name` (string, requerido): Nombre descriptivo del paso
- `step_order` (number, requerido): Número de orden (1, 2, 3...)

---

### 3. `log` - Registrar Mensaje

```typescript
// Log de nivel info (default)
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: "📖 Leyendo descripción del proyecto...",
  level: "info"
})

// Log de nivel debug
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: "Variables cargadas: tech=golang, kind=api",
  level: "debug"
})

// Log de nivel warn
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: "⚠️ Archivo opcional no encontrado, usando defaults",
  level: "warn"
})

// Log de nivel error (pero NO termina la sesión)
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP_ID,
  message: "❌ Falló validación de schema, reintentando...",
  level: "error"
})
```

**Campos:**
- `step_id` (string, requerido): ID del paso actual
- `message` (string, requerido): Mensaje a registrar
- `level` (string, opcional): "debug" | "info" | "warn" | "error" (default: "info")

---

### 4. `complete_step` - Finalizar Paso

```typescript
// Completar paso exitosamente
await mcp__MCPEco__execution_session_manage({
  action: "complete_step",
  step_id: STEP_ID,
  success: true
})

// Completar paso con error (pero continuar sesión)
await mcp__MCPEco__execution_session_manage({
  action: "complete_step",
  step_id: STEP_ID,
  success: false,
  error_message: "No se pudo procesar archivo X"
})
```

**Campos:**
- `step_id` (string, requerido): ID del paso a completar
- `success` (boolean, opcional): true | false (default: true)
- `error_message` (string, opcional): Mensaje de error si success=false

---

### 5. `link_project` - Vincular Project ID

```typescript
// Después de crear el proyecto, vincularlo a la sesión
const project = await mcp__MCPEco__create_project({ ... })
const PROJECT_ID = project.project_id

await mcp__MCPEco__execution_session_manage({
  action: "link_project",
  session_id: SESSION_ID,
  project_id: PROJECT_ID
})
```

**Campos:**
- `session_id` (string, requerido): ID de la sesión
- `project_id` (string, requerido): ID del proyecto a vincular

**Uso típico**: Cuando la sesión inicia ANTES de crear el proyecto.

---

### 6. `warn` - Warning No Fatal

```typescript
// Registrar warning que permite continuar
await mcp__MCPEco__execution_session_manage({
  action: "warn",
  step_id: STEP_ID,
  message: "⚠️ No se encontró CLAUDE.md, usando configuración por defecto"
})
```

**Campos:**
- `step_id` (string, requerido): ID del paso actual
- `message` (string, requerido): Mensaje de warning

**Diferencia con `log(level: "warn")`**: `warn` es una acción dedicada que puede tener lógica adicional en el backend (ej: contadores de warnings).

---

### 7. `fail` - Error Fatal

```typescript
// Error fatal que termina la sesión
await mcp__MCPEco__execution_session_manage({
  action: "fail",
  session_id: SESSION_ID,
  error_message: "❌ Error fatal: No se puede conectar a la base de datos",
  step_id: STEP_ID  // Opcional: paso donde ocurrió el error
})

// IMPORTANTE: Después de `fail`, la sesión queda en estado FAILED
// NO llamar `finish_session` después de `fail`
```

**Campos:**
- `session_id` (string, requerido): ID de la sesión
- `error_message` (string, requerido): Descripción del error
- `step_id` (string, opcional): ID del paso donde ocurrió el error

---

### 8. `finish_session` - Completar Sesión

```typescript
// Finalizar sesión exitosamente
await mcp__MCPEco__execution_session_manage({
  action: "finish_session",
  session_id: SESSION_ID,
  summary: "✅ Proyecto creado exitosamente con 3 documentos"
})
```

**Campos:**
- `session_id` (string, requerido): ID de la sesión
- `summary` (string, requerido): Resumen de lo que se logró

---

## 🔄 Ejemplo Completo: Flujo Típico

```typescript
// ═══════════════════════════════════════════════════════════
// EJEMPLO: Crear proyecto con tracking completo
// ═══════════════════════════════════════════════════════════

// PASO 1: Iniciar sesión (sin project_id aún)
const sessionResult = await mcp__MCPEco__execution_session_manage({
  action: "start_session",
  command: "constitution-create-project",
  provider: "claude",
  trigger_source: "cli",
  project_id: null
})
const SESSION_ID = sessionResult.session_id

// PASO 2: Iniciar paso de análisis
const step1 = await mcp__MCPEco__execution_session_manage({
  action: "start_step",
  session_id: SESSION_ID,
  step_name: "Análisis de proyecto",
  step_order: 1
})
const STEP1_ID = step1.step_id

// PASO 3: Logs de progreso
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP1_ID,
  message: "📖 Leyendo descripción del proyecto...",
  level: "info"
})

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP1_ID,
  message: "🔍 Detectado: tech=golang, kind=api, level=standard",
  level: "info"
})

// PASO 4: Completar paso de análisis
await mcp__MCPEco__execution_session_manage({
  action: "complete_step",
  step_id: STEP1_ID,
  success: true
})

// PASO 5: Iniciar paso de creación
const step2 = await mcp__MCPEco__execution_session_manage({
  action: "start_step",
  session_id: SESSION_ID,
  step_name: "Crear proyecto en BD",
  step_order: 2
})
const STEP2_ID = step2.step_id

// PASO 6: Crear proyecto
const project = await mcp__MCPEco__create_project({
  project_slug: "mi-api",
  project_name: "Mi API",
  tech: "golang",
  kind: "api"
})
const PROJECT_ID = project.project_id

// PASO 7: Vincular project_id a la sesión
await mcp__MCPEco__execution_session_manage({
  action: "link_project",
  session_id: SESSION_ID,
  project_id: PROJECT_ID
})

await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: STEP2_ID,
  message: `✅ Proyecto creado: ${PROJECT_ID}`,
  level: "info"
})

// PASO 8: Completar paso de creación
await mcp__MCPEco__execution_session_manage({
  action: "complete_step",
  step_id: STEP2_ID,
  success: true
})

// PASO 9: Finalizar sesión
await mcp__MCPEco__execution_session_manage({
  action: "finish_session",
  session_id: SESSION_ID,
  summary: `✅ Proyecto ${PROJECT_ID} creado exitosamente`
})
```

---

## ⚠️ Manejo de Errores

### Error Recuperable (continuar sesión)

```typescript
try {
  await algunaOperacion()
} catch (error) {
  // Registrar warning y continuar
  await mcp__MCPEco__execution_session_manage({
    action: "warn",
    step_id: STEP_ID,
    message: `⚠️ ${error.message} - continuando con defaults`
  })
  // ... continuar con lógica alternativa
}
```

### Error Fatal (terminar sesión)

```typescript
try {
  await operacionCritica()
} catch (error) {
  // Registrar error fatal y terminar
  await mcp__MCPEco__execution_session_manage({
    action: "fail",
    session_id: SESSION_ID,
    error_message: `❌ Error fatal: ${error.message}`,
    step_id: STEP_ID
  })
  // NO continuar - la sesión está terminada
  return { success: false, error: error.message }
}
```

---

## 📊 Diagrama de Estados

```
┌─────────────┐
│   CREATED   │  ← start_session()
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  IN_PROGRESS│  ← start_step(), log(), complete_step()
└──────┬──────┘
       │
       ├────────────────┐
       │                │
       ▼                ▼
┌─────────────┐  ┌─────────────┐
│  COMPLETED  │  │   FAILED    │
└─────────────┘  └─────────────┘
  ↑ finish_session()  ↑ fail()
```

---

## 🚫 Errores Comunes

### ❌ Error: Llamar `finish_session` después de `fail`

```typescript
// INCORRECTO ❌
await mcp__MCPEco__execution_session_manage({ action: "fail", ... })
await mcp__MCPEco__execution_session_manage({ action: "finish_session", ... }) // ¡ERROR!

// CORRECTO ✅
await mcp__MCPEco__execution_session_manage({ action: "fail", ... })
return { success: false }  // Terminar sin llamar finish_session
```

### ❌ Error: Log sin step activo

```typescript
// INCORRECTO ❌
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: null,  // ¡ERROR! step_id es requerido
  message: "..."
})

// CORRECTO ✅
const step = await mcp__MCPEco__execution_session_manage({
  action: "start_step",
  session_id: SESSION_ID,
  step_name: "Mi paso",
  step_order: 1
})
await mcp__MCPEco__execution_session_manage({
  action: "log",
  step_id: step.step_id,
  message: "..."
})
```

### ❌ Error: Olvidar `complete_step` antes de `finish_session`

```typescript
// INCORRECTO ❌ (step queda abierto)
await mcp__MCPEco__execution_session_manage({ action: "start_step", ... })
await mcp__MCPEco__execution_session_manage({ action: "log", ... })
await mcp__MCPEco__execution_session_manage({ action: "finish_session", ... }) // Step sin cerrar

// CORRECTO ✅
await mcp__MCPEco__execution_session_manage({ action: "start_step", ... })
await mcp__MCPEco__execution_session_manage({ action: "log", ... })
await mcp__MCPEco__execution_session_manage({ action: "complete_step", ... }) // ← Cerrar step
await mcp__MCPEco__execution_session_manage({ action: "finish_session", ... })
```

---

## 📚 Referencias

- **ADR-002**: `/LLMs/Claude/.claude/docs/ADR-002-execution-session-tracker.md`
- **MCP Tool**: `mcp__MCPEco__execution_session_manage`
- **Agente deprecado**: `/LLMs/Claude/.claude/agents/execution-session-tracker.md`

---

**Última actualización**: 2026-01-14
