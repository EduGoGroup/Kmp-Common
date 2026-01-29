---
name: load-document
description: Carga documentos markdown al MCPGenerator-v2 usando herramientas MCP nativas
allowed-tools: Task, TodoWrite, MCPSearch, Read, mcp__MCPEco__create_document, mcp__MCPEco__list_tags, mcp__MCPEco__list_steps, mcp__MCPEco__list_kinds, mcp__MCPEco__generate_summary_embeddings
---

# Comando: /workflow:load-document

Orquestador para cargar documentos markdown al sistema.

**IMPORTANTE**: Comunícate en español.

---

## Variables Globales

```typescript
// Variables globales del comando - se inicializan en cada fase
let $ARGUMENTS: string           // Ruta de archivo o contenido markdown
let inputType: 'file' | 'content' // Tipo de entrada detectado
let content: string               // Contenido del documento
let preGeneratedMeta: object | null // Metadata pre-generada del front-matter
let cleanContent: string          // Contenido sin front-matter
let mcpToolsLoaded: boolean = false // Estado de carga de tools MCP
```

---

## FASE -2: Inicializar TODO List

```typescript
// Inicializar tracking de progreso con TodoWrite
await TodoWrite({
  todos: [
    { content: "Cargar herramientas MCP requeridas", status: "pending", activeForm: "Cargando herramientas MCP" },
    { content: "Validar argumentos de entrada", status: "pending", activeForm: "Validando argumentos" },
    { content: "Detectar tipo de entrada (archivo/contenido)", status: "pending", activeForm: "Detectando tipo de entrada" },
    { content: "Validar disponibilidad MCP", status: "pending", activeForm: "Validando servidor MCP" },
    { content: "Obtener y procesar contenido", status: "pending", activeForm: "Procesando contenido" },
    { content: "Ejecutar document-loader", status: "pending", activeForm: "Cargando documento al sistema" },
    { content: "Retornar resultado final", status: "pending", activeForm: "Generando respuesta" }
  ]
})
```

---

## FASE -1: Cargar Herramientas MCP

```typescript
// Cargar herramientas MCP necesarias ANTES de usarlas
// OBLIGATORIO: MCPSearch debe ejecutarse antes de cualquier llamada MCP

await TodoWrite({ todos: [...todos.map(t => 
  t.content === "Cargar herramientas MCP requeridas" 
    ? {...t, status: "in_progress"} 
    : t
)]})

try {
  // Cargar tools MCP del servidor MCPEco
  await MCPSearch({ query: "select:mcp__MCPEco__create_document", max_results: 1 })
  await MCPSearch({ query: "select:mcp__MCPEco__list_tags", max_results: 1 })
  await MCPSearch({ query: "select:mcp__MCPEco__list_steps", max_results: 1 })
  await MCPSearch({ query: "select:mcp__MCPEco__list_kinds", max_results: 1 })
  await MCPSearch({ query: "select:mcp__MCPEco__generate_summary_embeddings", max_results: 1 })
  
  mcpToolsLoaded = true
  
  await TodoWrite({ todos: [...todos.map(t => 
    t.content === "Cargar herramientas MCP requeridas" 
      ? {...t, status: "completed"} 
      : t
  )]})
} catch (error) {
  return {
    status: "error",
    error_code: "MCP_TOOLS_LOAD_FAILED",
    message: `Error al cargar herramientas MCP: ${error.message}`,
    suggestion: "Verificar que el servidor MCP esté configurado en .mcp.json"
  }
}
```

---

## Manejo de Errores Global

```typescript
// Wrapper try-catch para todo el flujo de ejecución
// Cada fase tiene su propio manejo de errores, pero este captura errores no esperados

async function executeCommand() {
  try {
    // === FASES 0-6 se ejecutan aquí ===
    // (ver secciones siguientes)
    
  } catch (unexpectedError) {
    // Error no capturado por las fases individuales
    return {
      status: "error",
      error_code: "UNEXPECTED_ERROR",
      message: `Error inesperado durante la ejecución: ${unexpectedError.message}`,
      stack: unexpectedError.stack,
      phase: "unknown",
      suggestion: "Revisar logs y reintentar. Si persiste, reportar el error."
    }
  }
}
```

---

## 📥 Argumentos

```
$ARGUMENTS = <ruta_archivo | contenido_directo>
```

**Detección automática:**
- Si contiene `.md` o estructura de path → Ruta de archivo
- Si es texto con contenido markdown → Contenido directo

---

## 🔄 Flujo de Ejecución

### FASE 0: Validar Argumentos

```typescript
if (!$ARGUMENTS || $ARGUMENTS.trim() === "") {
  return {
    status: "error",
    error_code: "MISSING_ARGUMENT",
    message: "Se requiere ruta de archivo o contenido markdown"
  }
}
```

---

### FASE 1: Detectar Tipo de Entrada

```typescript
const isFilePath = $ARGUMENTS.includes('.md') || 
                   $ARGUMENTS.includes('/') ||
                   $ARGUMENTS.match(/^[a-zA-Z0-9_\-\/]+\.[a-zA-Z]+$/)

const inputType = isFilePath ? 'file' : 'content'
```

---

### FASE 2: Validar MCP (mcp-validator)

```typescript
const mcpResult = await Task({
  subagent_type: "common/mcp-validator",
  description: "Validar disponibilidad MCP",
  prompt: JSON.stringify({
    required_tools: [
      "mcp__MCPEco__create_document",
      "mcp__MCPEco__list_tags",
      "mcp__MCPEco__list_steps",
      "mcp__MCPEco__list_kinds",
      "mcp__MCPEco__generate_summary_embeddings"
    ]
  })
})

if (mcpResult.status === "error") {
  return {
    status: "error",
    error_code: "MCP_VALIDATION_FAILED",
    message: mcpResult.message
  }
}
```

---

### FASE 3: Obtener Contenido

```typescript
let content: string

if (inputType === 'file') {
  // Verificar que archivo existe
  const fileCheck = await Read({ file_path: $ARGUMENTS })
  
  if (!fileCheck || fileCheck.error) {
    return {
      status: "error",
      error_code: "FILE_NOT_FOUND",
      message: `Archivo no encontrado: ${$ARGUMENTS}`
    }
  }
  
  content = fileCheck.content
} else {
  content = $ARGUMENTS
}

// Validar contenido no vacío
if (!content || content.trim() === "") {
  return {
    status: "error",
    error_code: "EMPTY_CONTENT",
    message: "El contenido del documento está vacío"
  }
}
```

---

### FASE 4: Parsear YAML Front-Matter (Opcional)

```typescript
interface PreGeneratedMeta {
  summaries?: Record<string, string>
  tags?: string[]
  applies_to_steps?: string[]
  applies_to_kinds?: string[]
}

let preGeneratedMeta: PreGeneratedMeta | null = null
let cleanContent = content

// Detectar front-matter
const frontMatterMatch = content.match(/^---\n([\s\S]*?)\n---\n([\s\S]*)$/)

if (frontMatterMatch) {
  const yamlPart = frontMatterMatch[1]
  cleanContent = frontMatterMatch[2]
  
  // Parsear YAML básico
  preGeneratedMeta = parseYamlFrontMatter(yamlPart)
}
```

---

### FASE 5: Ejecutar Document Loader

```typescript
const loaderInput = {
  content: cleanContent,
  source: inputType === 'file' ? $ARGUMENTS : 'direct_input',
  ...(preGeneratedMeta && { preGenerated: preGeneratedMeta })
}

const loaderResult = await Task({
  subagent_type: "common/document-loader",
  description: "Cargar documento al MCP",
  prompt: JSON.stringify(loaderInput)
})
```

---

### FASE 6: Retornar Resultado

```typescript
if (loaderResult.status === "success") {
  return {
    status: "success",
    result: {
      document_id: loaderResult.result.document_id,
      title: loaderResult.result.title,
      version: loaderResult.result.version,
      metadata: loaderResult.result.metadata,
      stats: loaderResult.result.stats,
      source: inputType === 'file' ? $ARGUMENTS : 'direct_input'
    },
    message: `Documento cargado exitosamente: ${loaderResult.result.title}`
  }
} else {
  return {
    status: "error",
    error_code: loaderResult.error_code || "LOADER_FAILED",
    message: loaderResult.message
  }
}
```

---

## 📤 Output Esperado

### ✅ Éxito:
```json
{
  "status": "success",
  "result": {
    "document_id": "doc-abc123",
    "title": "Mi Documento",
    "version": "1.0",
    "metadata": {
      "tags": ["golang", "api"],
      "steps": ["implementer", "code_review"],
      "kinds": ["api"]
    },
    "stats": {
      "content_length": 4532,
      "summaries_generated": 2
    },
    "source": "docs/ejemplo.md"
  },
  "message": "Documento cargado exitosamente: Mi Documento"
}
```

### ❌ Error:
```json
{
  "status": "error",
  "error_code": "FILE_NOT_FOUND | MCP_VALIDATION_FAILED | EMPTY_CONTENT | LOADER_FAILED",
  "message": "Descripción del error"
}
```

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                    load-document.md                             │
│                    (Comando Orquestador)                        │
└─────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                                         ▼
┌─────────────────┐                      ┌─────────────────┐
│ mcp-validator   │                      │ document-loader │
│ (common)        │                      │ (common)        │
│                 │                      │                 │
│ Valida MCP      │                      │ Procesa y carga │
└─────────────────┘                      └─────────────────┘
```

---

## 📋 Agentes Utilizados

| Agente | Ubicación | Responsabilidad |
|--------|-----------|-----------------|
| mcp-validator | common/mcp-validator.md | Validar disponibilidad herramientas MCP |
| document-loader | common/document-loader.md | Procesar markdown, inferir metadata, cargar |

---

## 🚫 Prohibiciones

- ❌ NO ejecutar lógica de carga directamente - delegar a document-loader
- ❌ NO modificar archivos del sistema
- ❌ NO omitir validación MCP
- ❌ NO ignorar errores de agentes

---

## 💡 Ejemplos de Uso

### Cargar archivo markdown:
```bash
/workflow:load-document docs/architecture/overview.md
```

### Cargar contenido directo:
```bash
/workflow:load-document "# Mi Título\n\nContenido del documento..."
```

### Cargar con metadata pre-generada:
```bash
/workflow:load-document "---
summaries:
  implementer: 'Resumen para implementación...'
tags:
  - golang
---
# Mi Documento
Contenido..."
```

---

## 📌 Notas

- El comando detecta automáticamente si el input es ruta o contenido
- Si existe YAML front-matter con summaries, se usan directamente
- El document-loader infiere metadata (tags, steps, kinds) del contenido
- Los embeddings se generan automáticamente al final

---

## Versión

- **Versión**: 4.1
- **Migrado desde**: `LLMs/Claude/.claude/commands/workflow/load-document.md`
- **Fecha**: 2026-01-23

### Changelog

- **v4.1** (2026-01-23): Mejoras según mejores prácticas:
  - Agregado `name` y `allowed-tools` al frontmatter
  - Agregada FASE -2: Inicialización de TODO List con TodoWrite
  - Agregada FASE -1: Carga explícita de herramientas MCP con MCPSearch
  - Agregada sección Variables Globales con pseudo-código
  - Agregado Manejo de Errores Global con try-catch
- **v4.0** (2026-01-15): Migración inicial desde Claude3
