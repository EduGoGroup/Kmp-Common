# Deep Analysis Helper

Helper para asistir a los agentes de Deep Analysis en análisis técnico y descomposición.

**IMPORTANTE**: Este helper es referencia para los agentes especializados:
- `milestone-analyzer-agent.md`
- `story-creator-agent.md`
- `module-creator-agent.md`
- `root-cause-analyzer-agent.md`
- `fix-creator-agent.md`

---

## Constantes: Categorías de Módulos

```typescript
// Módulos esenciales para cualquier proyecto (siempre se incluyen)
const CORE_CATEGORIES = [
  "handler", "endpoint", "api", "core", "model", 
  "controller", "service", "main", "routes", "repository"
]

// Módulos opcionales (dependen del nivel y requisitos)
const OPTIONAL_CATEGORIES = [
  "middleware", "logging", "config", "makefile", 
  "docs", "readme", "error_handling", "utils"
]

// Módulos solo para Enterprise o cuando hay requisito explícito
const ENTERPRISE_ONLY = [
  "security", "cors", "monitoring", "metrics", "oauth",
  "authentication", "authorization", "rate_limit", 
  "caching", "distributed", "event_sourcing", "cqrs",
  "multi_tenant", "audit_log"
]

// Palabras clave para identificar categoría de un módulo
const MODULE_KEYWORDS = {
  core: ["endpoint", "handler", "api", "controller", "route", "service"],
  middleware: ["middleware", "interceptor", "filter"],
  security: ["security", "auth", "cors", "csrf", "helmet", "protection"],
  monitoring: ["metric", "monitoring", "tracing", "observability", "prometheus"],
  logging: ["log", "logger", "logging"],
  config: ["config", "configuration", "env", "settings"],
  testing: ["test", "spec", "mock", "fixture"]
}
```

---

## Función 1: Analizar Milestone

### Entrada
```typescript
{
  milestoneDescription: string,
  projectTech: string,
  projectKind: string,
  documentation: Document[]
}
```

### Proceso

#### Paso 1.1: Identificar Features Principales

**Lógica:**
```pseudo
FUNCTION extractFeatures(description: string) -> Feature[]:
  // Buscar patrones que indiquen features
  patterns = [
    "implementar X",
    "crear sistema de X",
    "agregar funcionalidad de X",
    "desarrollar X"
  ]

  features = []

  // Dividir descripción en párrafos o bullets
  sections = split_by_newlines_or_bullets(description)

  FOR EACH section IN sections:
    IF matches_feature_pattern(section):
      feature = {
        name: extract_feature_name(section),
        description: section,
        estimated_hours: estimate_hours(section)
      }
      features.append(feature)

  // Limitar a 5 features máximo por sprint
  RETURN features.slice(0, 5)
END FUNCTION
```

**Ejemplo:**

Milestone:
```
Implementar sistema de autenticación con JWT y crear CRUD
de productos con paginación. Agregar notificaciones por email.
```

Features extraídas:
```json
[
  {
    "name": "Sistema de Autenticación JWT",
    "description": "Implementar autenticación basada en tokens JWT",
    "estimated_hours": 16
  },
  {
    "name": "CRUD de Productos",
    "description": "Crear endpoints para CRUD con paginación",
    "estimated_hours": 12
  },
  {
    "name": "Notificaciones por Email",
    "description": "Agregar sistema de envío de emails",
    "estimated_hours": 8
  }
]
```

#### Paso 1.2: Detectar Riesgos Técnicos

**Categorías de Riesgos:**

| Categoría | Indicators | Nivel de Riesgo |
|-----------|-----------|-----------------| 
| **Escalabilidad** | "millones de usuarios", "alta carga" | Alto |
| **Integración** | "API externa", "servicio de terceros" | Medio |
| **Seguridad** | "autenticación", "encriptación", "PCI" | Alto |
| **Performance** | "tiempo real", "baja latencia" | Medio |
| **Complejidad** | "algoritmo complejo", "machine learning" | Alto |

**Lógica:**
```pseudo
FUNCTION detectRisks(features: Feature[], tech: string, kind: string) -> Risk[]:
  risks = []

  FOR EACH feature IN features:
    // Riesgos por keywords
    IF "API externa" IN feature.description:
      risks.append({
        category: "integración",
        description: "Dependencia de servicio externo puede afectar disponibilidad",
        level: "medio"
      })

    IF "autenticación" IN feature.description:
      risks.append({
        category: "seguridad",
        description: "Implementación de auth requiere validación exhaustiva",
        level: "alto"
      })

    // Riesgos por combinación tech + feature
    IF tech == "nodejs" AND "tiempo real" IN feature.description:
      risks.append({
        category: "performance",
        description: "Node.js puede tener limitaciones en CPU-intensive tasks",
        level: "medio"
      })

  RETURN risks
END FUNCTION
```

#### Paso 1.3: Estimar Esfuerzo

**Matriz de Estimación:**

| Complejidad | CRUD Simple | API con Lógica | Sistema Completo |
|-------------|-------------|----------------|------------------|
| **Baja** | 4-8h | 8-12h | 16-24h |
| **Media** | 8-12h | 12-20h | 24-40h |
| **Alta** | 12-20h | 20-32h | 40-80h |

**Lógica:**
```pseudo
FUNCTION estimateFeatureEffort(feature: Feature, tech: string) -> number:
  baseHours = 8 // Default

  // Ajustar por keywords
  IF "CRUD" IN feature.name:
    baseHours = 10
  IF "autenticación" OR "auth" IN feature.name:
    baseHours = 16
  IF "integración" OR "API externa" IN feature.description:
    baseHours += 8
  IF "tiempo real" OR "websocket" IN feature.description:
    baseHours += 12

  // Ajustar por tech (estos son EJEMPLOS, inferir para otras tecnologías)
  multiplier = {
    "golang": 0.9,  // Go es rápido de desarrollar
    "python": 0.8,  // Python es muy productivo
    "rust": 1.3,    // Rust tiene curva de aprendizaje
    "nodejs": 1.0   // Baseline
    // Para otras tecnologías, inferir multiplicador similar
  }

  RETURN baseHours * (multiplier[tech] || 1.0)
END FUNCTION
```

### Salida
```typescript
{
  features: [
    {
      name: "Sistema de Autenticación JWT",
      description: "...",
      estimated_hours: 16,
      feasibility: "viable",
      risks: ["Implementación correcta de JWT crítica para seguridad"],
      impact: "alto",
      dependencies: ["Base de datos para usuarios", "Librería JWT"],
      security: ["Hashear contraseñas con bcrypt", "Validar tokens en cada request"],
      guides: []
    }
  ],
  risks: [
    "Integración con servicio de email puede tener latencia",
    "Escalabilidad de BD para catálogo de productos"
  ],
  estimatedEffort: 36
}
```

---

## Función 2: Descomponer Feature en Stories

### Entrada
```typescript
{
  feature: Feature,
  projectKind: string
}
```

### Proceso

**Patrones de Descomposición:**

#### Para Features de Autenticación:
```
Story 1: Registro de usuarios
Story 2: Login y generación de token
Story 3: Middleware de autenticación
Story 4: Endpoint de logout
```

#### Para Features de CRUD:
```
Story 1: Create - Crear entidad
Story 2: Read - Leer entidad (con paginación)
Story 3: Update - Actualizar entidad
Story 4: Delete - Eliminar entidad
```

#### Para Features de Integración:
```
Story 1: Cliente HTTP para API externa
Story 2: Manejo de errores y retry logic
Story 3: Cache de respuestas (opcional)
Story 4: Testing de integración
```

**NOTA**: Estos son patrones COMUNES, no listas exhaustivas. El LLM debe inferir patrones similares para otros tipos de features.

### Salida
```typescript
[
  {
    title: "Implementar registro de usuarios",
    content: `
# Historia: Registro de Usuarios

Como usuario nuevo, quiero poder registrarme en el sistema, para acceder a las funcionalidades.

## Criterios de Aceptación

- [ ] Endpoint POST /api/auth/register acepta { email, password, name }
- [ ] Validar formato de email
- [ ] Validar contraseña (mínimo 8 caracteres)
- [ ] Hashear contraseña con bcrypt
- [ ] Guardar usuario en BD
- [ ] Retornar 201 Created con user_id
- [ ] Retornar 400 Bad Request si email ya existe

## Notas Técnicas

- Usar bcrypt para hashear (salt rounds = 10)
- Validar email con regex o librería
- DB: tabla users (id, email, password_hash, name, created_at)
    `
  }
]
```

---

## Función 3: Analizar Causa Raíz (Root Cause)

### Entrada
```typescript
{
  taskDescription: string,
  rejectionReason: string,
  severityLevel: number,
  workItems: WorkItem[],
  rejectedBy: "code_review" | "qa"
}
```

### Proceso

#### Paso 3.1: Clasificar Tipo de Error

**Categorías:**

| Tipo | Keywords | Typical Rejected By |
|------|----------|---------------------|
| `logic_error` | "lógica incorrecta", "resultado inesperado" | QA |
| `validation_missing` | "validación", "input no validado" | Code Review |
| `bug_implementation` | "bug", "excepción", "crash" | QA |
| `security_issue` | "vulnerabilidad", "inyección", "XSS" | Code Review |
| `performance_issue` | "lento", "timeout", "memoria" | QA |
| `test_failure` | "test falló", "assertion" | QA |
| `code_quality` | "duplicación", "complejidad", "refactor" | Code Review |

**NOTA**: Estos son tipos COMUNES. El LLM debe inferir categorías adicionales si encuentra errores que no encajan.

#### Paso 3.2: Determinar Causa Raíz

```pseudo
FUNCTION findRootCause(errorType: string, taskDesc: string, rejectionReason: string) -> string:
  // Analizar si el error viene de spec incompleta
  IF "validación" IN rejectionReason AND NOT "validación" IN taskDesc:
    RETURN "Spec no incluía requisitos de validación"

  // Analizar si el error viene de interpretación incorrecta
  IF errorType == "logic_error":
    RETURN "Lógica de negocio interpretada incorrectamente por implementer"

  // Analizar si es error de implementación
  IF errorType == "bug_implementation":
    RETURN "Error de programación (off-by-one, null pointer, etc.)"

  // Analizar si es problema de calidad
  IF errorType == "code_quality":
    RETURN "Código no sigue estándares del proyecto"

  // Default - el LLM debe analizar más a fondo
  RETURN "Causa raíz requiere análisis adicional del contexto"
END FUNCTION
```

#### Paso 3.3: Determinar Alcance del Fix

```pseudo
FUNCTION determineScope(errorType: string, workItems: WorkItem[]) -> string:
  // Contar archivos afectados
  affected_files = unique(workItems.map(wi => wi.file_path))

  IF affected_files.length == 1:
    RETURN "acotado" // 1 archivo

  IF affected_files.length <= 3:
    RETURN "medio" // 2-3 archivos

  RETURN "amplio" // 4+ archivos o refactor
END FUNCTION
```

#### Paso 3.4: Estimar Esfuerzo del Fix

**Matriz de Estimación:**

| Alcance | Security/Logic | Validation | Code Quality |
|---------|---------------|------------|--------------|
| **Acotado** | 4h | 2h | 1h |
| **Medio** | 8h | 4h | 3h |
| **Amplio** | 16h | 8h | 6h |

### Salida
```typescript
{
  error_type: "validation_missing",
  root_cause: "Spec no incluía requisitos de validación de formato de email",
  scope: "acotado",
  estimated_fix_hours: 2,
  fix_description: "Agregar validación de formato de email usando regex o librería de validación",
  affected_files: ["handlers/auth.go"]
}
```

---

## Función 4: Mapear Severity a Impact

### Entrada
```typescript
severityLevel: number // 0-100
```

### Lógica
```pseudo
FUNCTION mapSeverityToImpact(severity: number) -> string:
  IF severity <= 30:
    RETURN "critico"  // Bloqueante, no puede continuar
  ELSE IF severity <= 50:
    RETURN "alto"     // Afecta funcionalidad core
  ELSE IF severity <= 70:
    RETURN "medio"    // Afecta UX pero no bloquea
  ELSE:
    RETURN "bajo"     // Issue menor, cosmético
END FUNCTION
```

---

## Función 5: Error Type a Tag

Convierte tipo de error en tag de documentación relevante.

```pseudo
FUNCTION errorTypeToTag(errorType: string) -> string:
  mapping = {
    "validation_missing": "validation",
    "security_issue": "security",
    "logic_error": "architecture",
    "code_quality": "standards",
    "bug_implementation": "testing",
    "performance_issue": "performance"
  }

  RETURN mapping[errorType] OR "development"
END FUNCTION
```

---

## Función 6: Evaluar Necesidad de Módulo

Determina si un módulo es necesario según el nivel del proyecto.

### Entrada
```typescript
{
  moduleName: string,
  moduleDescription: string,
  levelConfig: object  // Del levels-helper
}
```

### Proceso

```pseudo
FUNCTION evaluateModuleNecessity(moduleName: string, moduleDescription: string, levelConfig: object) -> object:
  module_lower = moduleName.toLowerCase()
  description_lower = moduleDescription.toLowerCase()
  level = levelConfig.level
  
  result = {
    include: true,
    reason: null,
    suggestion: null,
    category: null
  }
  
  // PASO 1: Categorizar el módulo
  result.category = categorizeModule(module_lower, description_lower)
  
  // PASO 2: Evaluar según nivel
  
  // Para MVP: solo módulos core y tests
  IF level == "mvp":
    IF result.category IN ENTERPRISE_ONLY:
      result.include = false
      result.reason = "No necesario para MVP - es funcionalidad enterprise"
      result.suggestion = "Omitir completamente o mover a sprint futuro si escala"
      RETURN result
    
    IF result.category IN OPTIONAL_CATEGORIES:
      // Verificar si es módulo de testing (sí permitido en MVP)
      IF NOT ("test" IN module_lower):
        result.include = false
        result.reason = "Opcional para MVP - integrar en módulo core"
        result.suggestion = "Integrar como parte del módulo core principal"
        RETURN result
  
  // Para Standard: core + opcionales básicos, no enterprise
  IF level == "standard":
    IF result.category IN ENTERPRISE_ONLY:
      // Verificar si hay requisito explícito (permite override)
      IF NOT hasExplicitRequirement(moduleDescription):
        result.include = false
        result.reason = "Solo necesario para Enterprise"
        result.suggestion = "Omitir o implementar versión simplificada"
        RETURN result
  
  // Enterprise: todo permitido
  IF level == "enterprise":
    result.include = true
    result.reason = "Enterprise permite todos los módulos"
  
  RETURN result
END FUNCTION
```

---

## Función 7: Filtrar Módulos por Requisitos Explícitos

Filtra lista de módulos según si tienen requisito explícito en el proyecto.

### Entrada
```typescript
{
  modules: Module[],
  projectRequirements: string[],  // Requisitos explícitos del proyecto
  levelConfig: object
}
```

### Salida
```json
{
  "included": [
    {
      "name": "Core API",
      "description": "Handler principal",
      "inclusion_reason": "Módulo core requerido"
    }
  ],
  "omitted": [
    {
      "name": "CORS Middleware",
      "reason": "Solo necesario para Enterprise",
      "suggestion": "Omitir o implementar versión simplificada"
    }
  ],
  "summary": {
    "total_proposed": 14,
    "total_included": 2,
    "total_omitted": 12,
    "reduction_pct": "85.7"
  }
}
```

---

## Función 8: Consolidar Historias Relacionadas

Agrupa y consolida historias que pertenecen a la misma funcionalidad.

### Umbrales por Nivel
```
MVP:        consolidar agresivamente (60%+ keywords en común)
Standard:   consolidar moderadamente (40%+)
Enterprise: consolidar poco (30%+)
```

### Salida
```json
{
  "stories": [...],
  "merge_log": [
    {
      "action": "MERGED",
      "original_count": 3,
      "original_titles": ["Implementar handler...", "Crear structs...", "Validar campo..."],
      "merged_title": "Implementar POST /greet",
      "similarity": 0.75
    }
  ],
  "summary": {
    "original_count": 15,
    "final_count": 3,
    "merged_count": 12,
    "reduction_pct": "80.0"
  }
}
```

---

## Función Compuesta: Analizar Milestone

```pseudo
FUNCTION analyzeMilestone(input: object) -> object:
  // Paso 1: Extraer features del milestone
  features = extractFeatures(input.milestoneDescription)
  
  // Paso 2: Detectar riesgos para cada feature
  allRisks = []
  FOR EACH feature IN features:
    risks = detectRisks([feature], input.projectTech, input.projectKind)
    allRisks = allRisks.concat(risks)
  END FOR
  
  // Paso 3: Estimar esfuerzo por feature
  FOR EACH feature IN features:
    feature.effort = estimateFeatureEffort(feature, input.projectTech)
  END FOR
  
  // Paso 4: Generar stories para cada feature
  allStories = []
  FOR EACH feature IN features:
    stories = breakdownFeature(feature, input.projectKind)
    allStories = allStories.concat(stories)
  END FOR
  
  // Paso 5: Calcular totales
  totalEffort = SUM(feature.effort FOR feature IN features)
  
  RETURN {
    features: features,
    risks: allRisks,
    allStories: allStories,
    totalEffort: totalEffort,
    featureCount: features.length,
    storyCount: allStories.length,
    riskCount: allRisks.length
  }
END FUNCTION
```

---

## Función Compuesta: Analizar Causa Raíz

```pseudo
FUNCTION analyzeRootCause(input: object) -> object:
  // Paso 1: Clasificar el tipo de error
  errorType = classifyError(input.rejectionReason, input.rejectedBy)
  
  // Paso 2: Encontrar la causa raíz
  rootCause = findRootCause(errorType, input.taskDescription, input.rejectionReason)
  
  // Paso 3: Determinar el alcance del fix
  scope = determineScope(errorType, input.workItems)
  
  // Paso 4: Estimar esfuerzo de corrección
  fixEffort = estimateFixEffort(errorType, scope)
  
  // Paso 5: Mapear severidad a impacto
  impact = mapSeverityToImpact(input.severityLevel)
  
  // Paso 6: Obtener tag asociado
  tag = errorTypeToTag(errorType)
  
  RETURN {
    errorType: errorType,
    rootCause: rootCause,
    scope: scope,
    fixEffort: fixEffort,
    impact: impact,
    suggestedTag: tag,
    priority: IF input.severityLevel > 70 THEN "high" ELSE IF input.severityLevel > 40 THEN "medium" ELSE "low"
  }
END FUNCTION
```

---

## Notas de Implementación

- Todas las funciones son determinísticas basadas en reglas
- NO hacen llamadas a MCP, solo análisis
- Retornan estructuras de datos que el agente usa para MCP calls
- Para casos ambiguos, retornar valores conservadores y dejar que el agente decida
- **Las listas de keywords y tipos son EJEMPLOS, no listas exhaustivas**
- **El LLM debe inferir patrones similares para casos no cubiertos**

---

## Uso desde Agentes

### En milestone-analyzer-agent.md:
```typescript
// Usar este helper para:
// - extractFeatures() al analizar descripción
// - detectRisks() para identificar riesgos
// - estimateFeatureEffort() para estimar esfuerzo
```

### En story-creator-agent.md:
```typescript
// Usar este helper para:
// - breakdownFeature() para descomponer en stories
// - consolidateRelatedStories() para agrupar relacionadas
```

### En root-cause-analyzer-agent.md:
```typescript
// Usar este helper para:
// - classifyError() para identificar tipo de error
// - findRootCause() para determinar causa raíz
// - determineScope() para establecer alcance
```

---

**Versión**: 2.0
**Última actualización**: 2026-01-16
**Origen**: Migrado desde `/LLMs/Claude/.claude/helpers/deep-analysis-helper.md`
