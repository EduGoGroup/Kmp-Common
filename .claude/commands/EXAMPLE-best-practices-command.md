---
name: EXAMPLE-best-practices-command
description: Comando ejemplo con mejores prácticas según documentación oficial enero 2026
allowed-tools: Task, TodoWrite, MCPSearch, Read, Glob, Grep, mcp__MCPEco__get_project_info, mcp__MCPEco__execution_session_manage
---

# 📚 Comando Slash Ejemplo - Mejores Prácticas Enero 2026

**Propósito**: Comando de referencia que demuestra la configuración correcta según la documentación oficial de Claude Code.

**Autor**: Sistema de Auditoría MCPEco
**Fecha**: 22 de enero de 2026
**Documentación base**: https://code.claude.com/docs/en/skills.md

---

## ✅ Configuración del Frontmatter

```yaml
---
name: EXAMPLE-best-practices-command
description: Comando ejemplo con mejores prácticas según documentación oficial enero 2026
allowed-tools: Task, TodoWrite, MCPSearch, Read, Glob, Grep, mcp__MCPEco__get_project_info, mcp__MCPEco__execution_session_manage
---
```

### 📋 Checklist de Frontmatter

- ✅ **name**: Identificador único del comando (kebab-case)
- ✅ **description**: Descripción clara de 1-2 líneas
- ✅ **allowed-tools**: Lista explícita de herramientas permitidas

### 🔍 Explicación de `allowed-tools`

| Herramienta | Propósito en este Comando |
|-------------|---------------------------|
| `Task` | Delegar a subagentes especializados |
| `TodoWrite` | Tracking visual del progreso para el usuario |
| `MCPSearch` | Cargar herramientas MCP antes de usarlas |
| `Read` | Leer archivos de configuración o contexto |
| `Glob` | Buscar archivos por patrón |
| `Grep` | Buscar contenido específico en archivos |
| `mcp__MCPEco__get_project_info` | Herramienta MCP específica (ejemplo) |
| `mcp__MCPEco__execution_session_manage` | Tracking de sesión (ejemplo) |

**⚠️ IMPORTANTE**:
- Declarar SOLO las herramientas que el comando REALMENTE usará
- No copiar/pegar listas de otros comandos sin revisar
- Cada herramienta MCP debe declararse explícitamente

---

## 📥 Input del Usuario

**Formato recomendado**:

```
/EXAMPLE-best-practices-command <arg1> <arg2>
```

**Variables disponibles**:
- `$ARGUMENTS` - Argumentos pasados después del nombre del comando
- `$PROMPT` - Texto adicional del usuario

**Validación de input**:

```typescript
// OBLIGATORIO: Validar input al inicio
const args = $ARGUMENTS?.trim()
const userPrompt = $PROMPT?.trim()

if (!args || args === "") {
  throw new Error("❌ ERROR: Argumento requerido\n💡 Uso: /EXAMPLE-best-practices-command <argumento>")
}
```

---

## 🔄 Flujo de Ejecución Recomendado

### FASE -1: Inicializar TODO List (OBLIGATORIO)

```typescript
// ✅ CREAR TODO LIST PARA TRACKING VISUAL
await TodoWrite({
  todos: [
    {
      content: "Validar entrada del usuario",
      activeForm: "Validando entrada",
      status: "in_progress"
    },
    {
      content: "Cargar herramientas MCP necesarias",
      activeForm: "Cargando herramientas MCP",
      status: "pending"
    },
    {
      content: "Ejecutar tarea principal",
      activeForm: "Ejecutando tarea principal",
      status: "pending"
    },
    {
      content: "Validar resultados",
      activeForm: "Validando resultados",
      status: "pending"
    }
  ]
})
```

**¿Por qué es obligatorio?**
- Da visibilidad al usuario del progreso
- Permite debugging más fácil
- Demuestra profesionalidad

---

### FASE 0: Cargar Herramientas MCP (SI SE USAN)

```typescript
// ✅ CARGAR HERRAMIENTAS MCP EXPLÍCITAMENTE
console.log("🔧 Cargando herramientas MCP...")

await MCPSearch({ query: "select:mcp__MCPEco__get_project_info" })
await MCPSearch({ query: "select:mcp__MCPEco__execution_session_manage" })

console.log("✅ Herramientas MCP cargadas")

// ✅ ACTUALIZAR TODO
await TodoWrite({
  todos: [
    { content: "Validar entrada del usuario", activeForm: "Validando entrada", status: "completed" },
    { content: "Cargar herramientas MCP necesarias", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Ejecutar tarea principal", activeForm: "Ejecutando tarea principal", status: "in_progress" },
    { content: "Validar resultados", activeForm: "Validando resultados", status: "pending" }
  ]
})
```

**⚠️ IMPORTANTE**:
- SIEMPRE cargar herramientas MCP con `MCPSearch` antes de usarlas
- NUNCA asumir que están disponibles automáticamente
- Usar `select:<nombre-exacto>` para cargar una herramienta específica

---

### FASE 1: Invocar Herramientas MCP Directamente (NO Simular)

```typescript
// ✅ CORRECTO: Invocar la herramienta MCP real
const projectInfo = await mcp__MCPEco__get_project_info({
  project_id: projectId
})

if (!projectInfo || !projectInfo.success) {
  throw new Error(`Proyecto no encontrado: ${projectId}`)
}

console.log(`✅ Proyecto: ${projectInfo.project_name}`)
```

```typescript
// ❌ INCORRECTO: Simular o inventar datos
const projectInfo = {
  project_name: "Proyecto Ejemplo",
  tech: "golang",
  // ... datos inventados/stub
}
```

**Regla de oro**:
- Si la herramienta está en `allowed-tools` → USARLA
- Si no está en `allowed-tools` → NO mencionarla ni usarla
- NUNCA simular invocaciones

---

### FASE 2: Delegar a Subagentes (SI ES NECESARIO)

```typescript
// ✅ CORRECTO: Delegar a subagente especializado
const result = await Task({
  subagent_type: "example-worker-agent",  // Debe existir en .claude/agents/
  description: "Ejecutar tarea ejemplo",   // 3-5 palabras
  prompt: JSON.stringify({
    // Pasar contexto estructurado
    project_path: projectInfo.project_path,
    tech: projectInfo.tech,
    specific_param: "valor"
  })
})

// Parsear resultado del agente
const agentResult = JSON.parse(result)

if (agentResult.status !== "success") {
  throw new Error(`Agente falló: ${agentResult.error_message}`)
}
```

**⚠️ IMPORTANTE**:
- Siempre pasar `prompt` como JSON.stringify() si es objeto
- El subagente debe existir en `.claude/agents/`
- El subagente debe tener `tools` declarado en su frontmatter

---

### FASE 3: Validar Resultados (OBLIGATORIO)

```typescript
// ✅ VALIDACIÓN DE RESULTADOS
console.log("🧪 Validando resultados...")

// Si el agente dice que creó archivos, VERIFICAR físicamente
if (agentResult.files_created && agentResult.files_created.length > 0) {
  const firstFile = agentResult.files_created[0].path

  // Leer el archivo para confirmar que existe
  try {
    const content = await Read({ file_path: firstFile })
    console.log(`✅ Archivo confirmado: ${firstFile}`)
  } catch (error) {
    throw new Error(`❌ VALIDACIÓN FALLÓ: Archivo reportado pero no existe: ${firstFile}`)
  }
}

// ✅ ACTUALIZAR TODO
await TodoWrite({
  todos: [
    { content: "Validar entrada del usuario", activeForm: "Validando entrada", status: "completed" },
    { content: "Cargar herramientas MCP necesarias", activeForm: "Cargando herramientas MCP", status: "completed" },
    { content: "Ejecutar tarea principal", activeForm: "Ejecutando tarea principal", status: "completed" },
    { content: "Validar resultados", activeForm: "Validando resultados", status: "completed" }
  ]
})
```

**Regla de oro**:
- NUNCA confiar ciegamente en el output de un agente
- SIEMPRE validar archivos físicos si el agente dice que los creó
- SIEMPRE verificar comandos bash si el agente dice que los ejecutó

---

### FASE 4: Retornar Resultado (JSON Estructurado)

```typescript
// ✅ RETORNAR JSON ESTRUCTURADO
return JSON.stringify({
  success: true,
  command: "EXAMPLE-best-practices-command",
  result: {
    // Datos específicos del comando
    project_id: projectId,
    project_name: projectInfo.project_name,
    files_created: agentResult.files_created.length,
    files_validated: true  // Confirmado físicamente
  },
  summary: `Comando ejecutado exitosamente para ${projectInfo.project_name}`,
  timestamp: new Date().toISOString()
}, null, 2)
```

---

## 🛡️ Manejo de Errores

### Errores de Validación de Input

```typescript
try {
  // Validar input
  if (!projectId || projectId === "") {
    throw new Error("project_id es requerido")
  }
} catch (error) {
  return JSON.stringify({
    success: false,
    error_code: "INVALID_INPUT",
    error_message: error.message,
    usage: "/EXAMPLE-best-practices-command <project-id>"
  }, null, 2)
}
```

### Errores de Herramientas MCP

```typescript
try {
  const projectInfo = await mcp__MCPEco__get_project_info({ project_id: projectId })

  if (!projectInfo || !projectInfo.success) {
    throw new Error(`Proyecto no encontrado: ${projectId}`)
  }
} catch (error) {
  return JSON.stringify({
    success: false,
    error_code: "MCP_TOOL_FAILED",
    error_message: `Error al obtener proyecto: ${error.message}`,
    suggestion: "Verifica que el project_id sea correcto"
  }, null, 2)
}
```

### Errores de Agentes

```typescript
try {
  const result = await Task({
    subagent_type: "example-worker-agent",
    description: "Ejecutar tarea",
    prompt: JSON.stringify({ /* ... */ })
  })

  const agentResult = JSON.parse(result)

  if (agentResult.status !== "success") {
    throw new Error(agentResult.error_message || "Agente falló sin mensaje de error")
  }
} catch (error) {
  return JSON.stringify({
    success: false,
    error_code: "AGENT_EXECUTION_FAILED",
    error_message: `Agente falló: ${error.message}`,
    suggestion: "Revisa los logs del agente para más detalles"
  }, null, 2)
}
```

---

## 📊 Logging y Tracking

### Console.log Estructurado

```typescript
// ✅ CORRECTO: Logging claro y estructurado
console.log("═══════════════════════════════════════════════")
console.log("  🎯 EJEMPLO: COMANDO CON MEJORES PRÁCTICAS")
console.log("═══════════════════════════════════════════════")
console.log("")
console.log("📥 Input:")
console.log(`   Project ID: ${projectId}`)
console.log("")
console.log("🔧 Ejecutando...")
// ... ejecución ...
console.log("")
console.log("✅ Resultado:")
console.log(`   Archivos creados: ${filesCreated}`)
console.log(`   Archivos validados: SÍ`)
console.log("")
```

### Tracking de Sesión (Opcional pero Recomendado)

```typescript
// Si el comando requiere tracking detallado
let sessionId = null

try {
  // Iniciar sesión
  const session = await mcp__MCPEco__execution_session_manage({
    action: "start_session",
    command: "EXAMPLE-best-practices-command",
    provider: "claude",
    trigger_source: "cli",
    project_id: projectId
  })

  sessionId = session.session_id
  console.log(`📊 Session ID: ${sessionId}`)

  // ... ejecutar comando ...

  // Finalizar sesión exitosamente
  await mcp__MCPEco__execution_session_manage({
    action: "finish_session",
    session_id: sessionId,
    summary: "Comando ejecutado exitosamente"
  })

} catch (error) {
  // Marcar sesión como fallida
  if (sessionId) {
    await mcp__MCPEco__execution_session_manage({
      action: "fail",
      session_id: sessionId,
      error_message: error.message
    })
  }
  throw error
}
```

---

## ✅ Checklist de Mejores Prácticas

Al crear un comando slash, verificar:

- [ ] **Frontmatter completo** con name, description, allowed-tools
- [ ] **allowed-tools** con SOLO las herramientas que se usarán
- [ ] **Validación de input** al inicio del comando
- [ ] **TODO list** creado al inicio para tracking visual
- [ ] **MCPSearch** para cargar herramientas MCP antes de usarlas
- [ ] **Invocación real** de herramientas MCP (no simulación)
- [ ] **Delegación a agentes** con prompt JSON.stringify()
- [ ] **Validación física** de resultados (archivos, comandos, etc.)
- [ ] **Manejo de errores** en cada fase crítica
- [ ] **Logging estructurado** para debugging
- [ ] **Retorno JSON** estructurado y consistente
- [ ] **Documentación** clara de input/output

---

## 🔗 Referencias

### Documentación Oficial

- **Skills**: https://code.claude.com/docs/en/skills.md
- **Sub-agents**: https://code.claude.com/docs/en/sub-agents.md
- **Tool System**: https://code.claude.com/docs/en/tools.md

### Archivos Relacionados

- **Informe de auditoría**: `docs/INFORME-PERMISOS-AGENTES-ENERO-2026.md`
- **Agente ejemplo**: `.claude/agents/EXAMPLE-best-practices-agent.md`

---

**Versión**: 1.0
**Última actualización**: 22 de enero de 2026
**Estado**: REFERENCIA - NO EJECUTAR EN PRODUCCIÓN
