# Planner Helper

Helper para asistir al agente Planner en la descomposición de stories en tasks.

## Propósito

Proporcionar funciones de utilidad para:
1. Analizar y clasificar user stories
2. Descomponer stories en tasks técnicas
3. Establecer dependencias entre tasks
4. Validar descomposiciones
5. Ajustar descomposiciones inválidas
6. **Detectar actividades disfrazadas de tareas** (NUEVO)
7. **Aplicar límites según nivel del proyecto** (NUEVO)

---

## CONCEPTO CLAVE: TAREA vs ACTIVIDAD

### Definiciones

| Concepto | Definición | Criterios | Ejemplos |
|----------|------------|-----------|----------|
| **TAREA** | Unidad de trabajo con **valor de negocio verificable** | - Puede ser revisada independientemente<br>- Tiene un entregable claro<br>- El cliente/usuario puede validar<br>- **Mínimo 1 hora** de trabajo | "Implementar endpoint POST /greet"<br>"Crear suite de tests del módulo auth" |
| **ACTIVIDAD** | Paso técnico **interno** de una tarea | - No tiene valor de negocio independiente<br>- Es un prerequisito técnico<br>- No se puede "entregar" por sí sola<br>- Generalmente **< 1 hora** | "Crear struct de request"<br>"Agregar validación de input"<br>"Manejar errores HTTP" |

### Regla de Oro

> **Si no puedes demostrar al cliente que la "tarea" está completa sin mencionar otras tareas, entonces es una ACTIVIDAD, no una TAREA.**

### Ejemplos

**INCORRECTO (5 tareas fragmentadas)**:
```
1. Crear ResponseWriter wrapper           (1h)  ← ACTIVIDAD
2. Implementar generación de Request ID   (1h)  ← ACTIVIDAD
3. Implementar redacción de headers       (1h)  ← ACTIVIDAD
4. Implementar middleware de logging      (2h)  ← TAREA
5. Crear tests unitarios                  (2h)  ← TAREA
```

**CORRECTO (2 tareas con actividades integradas)**:
```
1. Implementar Logging Middleware completo (4h)  ← TAREA
   Actividades incluidas:
   - Crear ResponseWriter wrapper
   - Implementar Request ID generator
   - Implementar sanitización de headers
   Entregable: Middleware funcional

2. Crear tests de Logging Middleware (2h)        ← TAREA
   Actividades incluidas:
   - Tests unitarios de cada componente
   - Tests de integración
   Entregable: Suite de tests con cobertura > 80%
```

---

## Constantes: Límites por Nivel

```typescript
// NUEVO: Reglas de validación POR NIVEL
const VALIDATION_RULES_BY_LEVEL = {
  mvp: {
    minTasks: 1,           // ✅ Permitir 1 tarea si es suficiente
    maxTasks: 4,
    minHoursPerTask: 1,    // ✅ Mínimo 1 hora (no actividades)
    maxHoursPerTask: 4
  },
  standard: {
    minTasks: 1,           // ✅ Mínimo flexible
    maxTasks: 6,
    minHoursPerTask: 1,
    maxHoursPerTask: 6
  },
  enterprise: {
    minTasks: 2,
    maxTasks: 8,
    minHoursPerTask: 1,
    maxHoursPerTask: 8
  }
}

// Indicadores de ACTIVIDAD (no deben ser tareas separadas)
const ACTIVITY_INDICATORS = [
  "crear struct",
  "crear modelo sin endpoint",
  "agregar validación",
  "manejar errores",
  "crear wrapper",
  "crear helper",
  "configurar variable",
  "definir constante",
  "crear interfaz",
  "agregar import"
]

// Indicadores de TAREA (sí deben ser tareas)
const TASK_INDICATORS = [
  "implementar endpoint",
  "crear tests",
  "implementar funcionalidad",
  "integrar con",
  "migrar",
  "crear servicio completo",
  "implementar middleware"
]
```

---

## Función 1: Analizar Story

### Entrada
```typescript
{
  title: string,
  content: string,
  acceptanceCriteria: string[],
  tech: string,
  kind: string,
  isFixStory: boolean
}
```

### Proceso

#### Paso 1.1: Clasificar Tipo de Story

**Categorías:**

| Tipo | Keywords en Título/Contenido |
|------|------------------------------|
| `authentication` | "login", "registro", "auth", "JWT", "OAuth" |
| `crud_create` | "crear", "POST", "insertar", "nuevo" |
| `crud_read` | "listar", "GET", "buscar", "filtrar", "paginación" |
| `crud_update` | "actualizar", "PUT", "modificar", "editar" |
| `crud_delete` | "eliminar", "DELETE", "borrar" |
| `integration` | "integración", "API externa", "servicio externo" |
| `notification` | "notificación", "email", "webhook", "push" |
| `fix` | (cuando isFixStory = true) |

**Lógica:**
```pseudo
FUNCTION classifyStory(title: string, content: string, isFixStory: boolean) -> string:
  IF isFixStory:
    RETURN "fix"

  combined = title + " " + content
  combined_lower = combined.toLowerCase()

  // Buscar por categoría
  FOR EACH category, keywords IN STORY_CATEGORIES:
    FOR EACH keyword IN keywords:
      IF keyword IN combined_lower:
        RETURN category

  // Default
  RETURN "generic"
END FUNCTION
```

#### Paso 1.2: Determinar Complejidad

**Factores:**

| Factor | Peso | Indicadores |
|--------|------|-------------|
| **Número de criterios** | 40% | Más de 6 criterios = complejo |
| **Longitud de contenido** | 30% | Más de 300 palabras = complejo |
| **Keywords de complejidad** | 30% | "algoritmo", "optimización", "complejo" |

```pseudo
FUNCTION determineComplexity(content: string, criteriaCount: number) -> string:
  score = 0

  // Factor 1: Criterios de aceptación
  IF criteriaCount >= 6:
    score += 40
  ELSE IF criteriaCount >= 4:
    score += 20

  // Factor 2: Longitud
  word_count = count_words(content)
  IF word_count > 300:
    score += 30
  ELSE IF word_count > 150:
    score += 15

  // Factor 3: Keywords
  IF "algoritmo" OR "optimización" OR "complejo" IN content:
    score += 30

  // Clasificación
  IF score >= 60:
    RETURN "complex"
  ELSE IF score >= 30:
    RETURN "medium"
  ELSE:
    RETURN "simple"
END FUNCTION
```

#### Paso 1.3: Sugerir Número de Tasks (POR NIVEL)

**NUEVO: Matriz por NIVEL de proyecto (no por complejidad)**

Las siguientes son **SUGERENCIAS**, no mínimos obligatorios.
El número real depende del nivel del proyecto y la complejidad real.

| Tipo | MVP | Standard | Enterprise |
|------|-----|----------|------------|
| **CRUD simple** | 1-2 | 2-3 | 3-4 |
| **CRUD con relaciones** | 2-3 | 3-4 | 4-6 |
| **Authentication básica** | 2-3 | 3-4 | 4-6 |
| **Authentication OAuth** | 3-4 | 4-6 | 6-8 |
| **Integration** | 2-3 | 3-5 | 5-8 |
| **Fix** | 1-2 | 2-3 | 2-4 |

### Regla: Menos es Más
- Prefiere 2 tareas bien definidas a 5 tareas fragmentadas
- Cada tarea debe tener valor de negocio verificable
- Las actividades internas van en la DESCRIPCIÓN, no como tareas separadas

```pseudo
FUNCTION suggestTaskCount(storyType: string, complexity: string, levelConfig: object) -> number:
  level = levelConfig.level
  
  // NUEVA MATRIZ POR NIVEL
  matrix = {
    "crud_create": { mvp: 2, standard: 3, enterprise: 4 },
    "crud_read": { mvp: 2, standard: 3, enterprise: 4 },
    "crud_full": { mvp: 3, standard: 4, enterprise: 6 },
    "authentication": { mvp: 2, standard: 4, enterprise: 6 },
    "integration": { mvp: 2, standard: 4, enterprise: 6 },
    "fix": { mvp: 1, standard: 2, enterprise: 3 },
    "generic": { mvp: 2, standard: 3, enterprise: 4 }
  }
  
  baseCount = matrix[storyType][level] OR matrix["generic"][level]
  
  // Ajustar por complejidad (solo +1 si es complex)
  IF complexity == "complex":
    baseCount = min(baseCount + 1, levelConfig.limits.max_tasks_per_story)
  
  RETURN baseCount
END FUNCTION
```

### Salida
```typescript
{
  storyType: "authentication",
  complexity: "medium",
  suggestedTaskCount: 6
}
```

---

## Función 2: Descomponer Story

### Entrada
```typescript
{
  storyAnalysis: StoryAnalysis,
  acceptanceCriteria: string[],
  tech: string,
  kind: string,
  documentation: DocumentSummary[]
}
```

### Proceso

Usa **templates de descomposición** basados en `storyType`.

#### Template: Authentication

```typescript
const authenticationTemplate = [
  {
    title: "Crear modelo de Usuario en BD",
    description: "Definir schema y modelo de usuarios",
    estimatedHours: 2,
    dependsOnIndex: null,
    filesToModify: generateFilePaths("model", tech)
  },
  {
    title: "Implementar hasheo de contraseñas",
    description: "Usar bcrypt para hashear passwords",
    estimatedHours: 1,
    dependsOnIndex: 0,
    filesToModify: generateFilePaths("service", tech)
  },
  {
    title: "Implementar endpoint POST /auth/register",
    description: "Handler para registro de usuarios",
    estimatedHours: 3,
    dependsOnIndex: 1,
    filesToModify: generateFilePaths("handler", tech)
  },
  {
    title: "Implementar generación de JWT",
    description: "Crear tokens JWT al hacer login",
    estimatedHours: 2,
    dependsOnIndex: 1,
    filesToModify: generateFilePaths("service", tech)
  },
  {
    title: "Implementar endpoint POST /auth/login",
    description: "Handler para login con JWT",
    estimatedHours: 3,
    dependsOnIndex: 3,
    filesToModify: generateFilePaths("handler", tech)
  },
  {
    title: "Crear tests unitarios de autenticación",
    description: "Tests para registro, login y JWT",
    estimatedHours: 2,
    dependsOnIndex: 4,
    filesToModify: generateFilePaths("test", tech)
  }
]
```

#### Template: CRUD Create (POR NIVEL)

**NUEVO: Templates separados por nivel - actividades en descripción, no como tareas**

```typescript
const crudCreateTemplates = {
  // ═══════════════════════════════════════════════════════════════
  // MVP: 2 tareas (implementación + tests)
  // ═══════════════════════════════════════════════════════════════
  mvp: [
    {
      title: "Implementar endpoint POST /{{resource}}",
      description: `
# Implementar endpoint POST /{{resource}}

## Actividades incluidas
1. Crear modelo/struct de {{Entity}} con validaciones
2. Implementar handler HTTP
3. Agregar validación de input
4. Agregar manejo de errores HTTP

## Entregable
Endpoint funcional que crea {{entity}} en el sistema.

## Criterios de Completitud
- [ ] Endpoint responde a POST /{{resource}}
- [ ] Valida campos requeridos
- [ ] Retorna 201 Created con el recurso
- [ ] Retorna 400 Bad Request para input inválido
      `,
      estimatedHours: 3,
      dependsOnIndex: null,
      filesToModify: ["handlers/{{entity}}_handler.go", "models/{{entity}}.go"]
    },
    {
      title: "Crear tests del endpoint POST /{{resource}}",
      description: `
# Tests del endpoint POST /{{resource}}

## Actividades incluidas
1. Tests unitarios del handler
2. Tests de validación de input
3. Tests de casos de error

## Entregable
Suite de tests con cobertura > 80%
      `,
      estimatedHours: 2,
      dependsOnIndex: 0,
      filesToModify: ["handlers/{{entity}}_handler_test.go"]
    }
  ],
  
  // ═══════════════════════════════════════════════════════════════
  // Standard: 3 tareas (modelo + endpoint + tests)
  // ═══════════════════════════════════════════════════════════════
  standard: [
    {
      title: "Implementar modelo y validaciones de {{Entity}}",
      description: `
# Modelo y validaciones de {{Entity}}

## Actividades incluidas
1. Crear struct/modelo de {{Entity}}
2. Definir validaciones de campos
3. Crear migraciones de BD si aplica

## Entregable
Modelo listo para usar en handlers
      `,
      estimatedHours: 2,
      dependsOnIndex: null
    },
    {
      title: "Implementar endpoint POST /{{resource}}",
      description: `
# Endpoint POST /{{resource}}

## Actividades incluidas
1. Implementar handler HTTP
2. Integrar con modelo
3. Agregar manejo de errores

## Entregable
Endpoint funcional
      `,
      estimatedHours: 3,
      dependsOnIndex: 0
    },
    {
      title: "Crear tests completos de {{Entity}}",
      description: `
# Tests de {{Entity}}

## Actividades incluidas
1. Tests unitarios del modelo
2. Tests del handler
3. Tests de integración

## Entregable
Suite de tests con cobertura > 80%
      `,
      estimatedHours: 2,
      dependsOnIndex: 1
    }
  ],
  
  // ═══════════════════════════════════════════════════════════════
  // Enterprise: 4+ tareas (modelo + validación + endpoint + tests)
  // ═══════════════════════════════════════════════════════════════
  enterprise: [
    {
      title: "Implementar modelo de {{Entity}} con persistencia",
      description: "Modelo, migraciones, repository pattern",
      estimatedHours: 3,
      dependsOnIndex: null
    },
    {
      title: "Implementar validación y sanitización de {{Entity}}",
      description: "Validación avanzada, sanitización, reglas de negocio",
      estimatedHours: 2,
      dependsOnIndex: 0
    },
    {
      title: "Implementar endpoint POST /{{resource}} con audit",
      description: "Handler, audit log, eventos",
      estimatedHours: 3,
      dependsOnIndex: 1
    },
    {
      title: "Crear suite de tests de {{Entity}}",
      description: "Tests unitarios, integración, e2e",
      estimatedHours: 3,
      dependsOnIndex: 2
    }
  ]
}
```

#### Template: Fix

```typescript
const fixTemplate = [
  {
    title: "Implementar corrección",
    description: "Aplicar fix específico según root cause",
    estimatedHours: 2,
    dependsOnIndex: null
  },
  {
    title: "Agregar test de regresión",
    description: "Test que valide que error no vuelva a ocurrir",
    estimatedHours: 1,
    dependsOnIndex: 0
  },
  {
    title: "Actualizar documentación",
    description: "Documentar cambio si es necesario",
    estimatedHours: 0.5,
    dependsOnIndex: 0
  }
]
```

#### Función de Interpolación

```pseudo
FUNCTION interpolateTemplate(template: TaskTemplate[], context: object) -> Task[]:
  tasks = []

  FOR EACH taskTemplate IN template:
    task = {
      title: interpolate(taskTemplate.title, context),
      description: interpolate(taskTemplate.description, context),
      estimated_hours: taskTemplate.estimatedHours,
      depends_on_index: taskTemplate.dependsOnIndex,
      files_to_modify: taskTemplate.filesToModify
    }
    tasks.append(task)

  RETURN tasks
END FUNCTION
```

**Contexto de Interpolación:**
```typescript
{
  Entity: extractEntityName(storyTitle), // "Usuario", "Producto", etc.
  entity: extractEntityName(storyTitle).toLowerCase(),
  resource: pluralize(extractEntityName(storyTitle)).toLowerCase()
}
```

#### Generación de File Paths

```pseudo
FUNCTION generateFilePaths(component: string, tech: string) -> string[]:
  patterns = {
    "golang": {
      model: ["models/{{entity}}.go", "migrations/001_create_{{table}}.sql"],
      service: ["services/{{entity}}_service.go"],
      handler: ["handlers/{{entity}}_handler.go"],
      test: ["handlers/{{entity}}_handler_test.go"]
    },
    "python": {
      model: ["models/{{entity}}.py", "migrations/001_create_{{table}}.py"],
      service: ["services/{{entity}}_service.py"],
      handler: ["views/{{entity}}_view.py"],
      test: ["tests/test_{{entity}}.py"]
    }
  }

  RETURN patterns[tech][component]
END FUNCTION
```

### Salida
```typescript
{
  tasks: [
    {
      title: "Crear modelo de Usuario en BD",
      description: "Definir schema con campos: id, email, password_hash, name, created_at",
      estimated_hours: 2,
      depends_on_index: null,
      files_to_modify: ["models/user.go", "migrations/001_create_users.sql"]
    },
    // ... más tasks
  ],
  totalEstimatedHours: 11
}
```

---

## Función 3: Validar Descomposición (ACTUALIZADA)

### Entrada
```typescript
{
  decomposition: Decomposition,
  levelConfig: object,    // NUEVO: configuración del nivel
  storyComplexity: string // NUEVO: para advertencias
}
```

### Lógica NUEVA

```pseudo
FUNCTION validateDecomposition(decomposition: Decomposition, levelConfig: object, storyComplexity: string) -> ValidationResult:
  level = levelConfig.level
  rules = VALIDATION_RULES_BY_LEVEL[level]
  
  issues = []   // Errores bloqueantes
  warnings = [] // Advertencias (no bloquean)

  // ═══════════════════════════════════════════════════════════════
  // Validación 1: Máximo de tasks (SOLO máximo, ya no hay mínimo forzado)
  // ═══════════════════════════════════════════════════════════════
  IF decomposition.tasks.length > rules.maxTasks:
    issues.append({
      type: "MAX_TASKS_EXCEEDED",
      message: `Demasiadas tasks (${decomposition.tasks.length}), máximo para nivel ${level}: ${rules.maxTasks}`,
      suggestion: "Consolidar tareas relacionadas"
    })
  
  // WARNING (no error) si parece muy poco para historia compleja
  IF decomposition.tasks.length == 1 AND storyComplexity == "complex":
    warnings.append({
      type: "POSSIBLY_INSUFFICIENT",
      message: "Solo 1 tarea para historia compleja - ¿seguro que no falta algo?"
    })

  // ═══════════════════════════════════════════════════════════════
  // Validación 2: Horas por task (mínimo 1 hora - NO actividades)
  // ═══════════════════════════════════════════════════════════════
  FOR EACH task IN decomposition.tasks:
    IF task.estimated_hours < rules.minHoursPerTask:
      issues.append({
        type: "TASK_TOO_SMALL",
        task: task.title,
        message: `Task "${task.title}" muy pequeña (${task.estimated_hours}h) - ¿es una ACTIVIDAD?`,
        suggestion: "Integrar como actividad dentro de otra tarea"
      })

    IF task.estimated_hours > rules.maxHoursPerTask:
      warnings.append({
        type: "TASK_TOO_LARGE",
        task: task.title,
        message: `Task "${task.title}" es grande (${task.estimated_hours}h > ${rules.maxHoursPerTask}h)`,
        suggestion: "Considerar dividir por FUNCIONALIDAD (no por actividades técnicas)"
      })

  // ═══════════════════════════════════════════════════════════════
  // Validación 3: NUEVO - Detectar actividades disfrazadas de tareas
  // ═══════════════════════════════════════════════════════════════
  FOR EACH task IN decomposition.tasks:
    businessValue = hasBusinessValue(task)
    
    IF NOT businessValue.isTask:
      warnings.append({
        type: "POSSIBLE_ACTIVITY",
        task: task.title,
        message: `"${task.title}" parece una ACTIVIDAD, no una TAREA`,
        reason: businessValue.reason,
        suggestion: "Integrar como actividad en la descripción de otra tarea"
      })

  // ═══════════════════════════════════════════════════════════════
  // Validación 4: Dependencias cíclicas
  // ═══════════════════════════════════════════════════════════════
  IF hasCyclicDependencies(decomposition.tasks):
    issues.append({
      type: "CYCLIC_DEPENDENCIES",
      message: "Dependencias cíclicas detectadas, debe ser un DAG"
    })

  // ═══════════════════════════════════════════════════════════════
  // Validación 5: Testing task exists (warning, no error)
  // ═══════════════════════════════════════════════════════════════
  hasTestTask = decomposition.tasks.some(t => 
    t.title.toLowerCase().includes("test") OR
    t.title.toLowerCase().includes("prueba")
  )
  IF NOT hasTestTask:
    warnings.append({
      type: "MISSING_TESTS",
      message: "No hay task de testing explícita",
      suggestion: "Agregar task de tests o incluir tests en otra task"
    })

  RETURN {
    valid: issues.length == 0,
    issues: issues,
    warnings: warnings,
    summary: {
      task_count: decomposition.tasks.length,
      total_hours: sum(decomposition.tasks.map(t => t.estimated_hours)),
      possible_activities: warnings.filter(w => w.type == "POSSIBLE_ACTIVITY").length
    }
  }
END FUNCTION
```

---

## Función 3.1: NUEVA - Detectar Valor de Negocio

```pseudo
FUNCTION hasBusinessValue(task: object) -> object:
  titleLower = task.title.toLowerCase()
  
  result = {
    isTask: true,
    confidence: "high",
    reason: null
  }
  
  // ═══════════════════════════════════════════════════════════════
  // Detectar indicadores de ACTIVIDAD (NO es tarea)
  // ═══════════════════════════════════════════════════════════════
  FOR EACH indicator IN ACTIVITY_INDICATORS:
    IF indicator IN titleLower:
      result.isTask = false
      result.confidence = "high"
      result.reason = `Contiene indicador de actividad: "${indicator}"`
      RETURN result
  
  // ═══════════════════════════════════════════════════════════════
  // Detectar indicadores de TAREA (SÍ es tarea)
  // ═══════════════════════════════════════════════════════════════
  FOR EACH indicator IN TASK_INDICATORS:
    IF indicator IN titleLower:
      result.isTask = true
      result.confidence = "high"
      result.reason = `Contiene indicador de tarea: "${indicator}"`
      RETURN result
  
  // ═══════════════════════════════════════════════════════════════
  // Heurística: Si tiene >= 1 hora, probablemente es tarea
  // ═══════════════════════════════════════════════════════════════
  IF task.estimated_hours >= 1:
    result.isTask = true
    result.confidence = "medium"
    result.reason = "Duración >= 1 hora sugiere tarea"
  ELSE:
    result.isTask = false
    result.confidence = "medium"
    result.reason = "Duración < 1 hora sugiere actividad"
  
  RETURN result
END FUNCTION
```

### Salida
```typescript
{
  "valid": true,
  "issues": [],
  "warnings": [
    {
      "type": "POSSIBLE_ACTIVITY",
      "task": "Crear struct de Request",
      "message": "\"Crear struct de Request\" parece una ACTIVIDAD, no una TAREA",
      "reason": "Contiene indicador de actividad: \"crear struct\"",
      "suggestion": "Integrar como actividad en la descripción de otra tarea"
    }
  ],
  "summary": {
    "task_count": 3,
    "total_hours": 7,
    "possible_activities": 1
  }
}
```

---

## Función 4: Ajustar Descomposición

### Entrada
```typescript
{
  decomposition: Decomposition,
  issues: string[]
}
```

### Estrategias de Ajuste

#### Ajuste 1: Consolidar Tasks (si hay demasiadas)

```pseudo
FUNCTION consolidateTasks(tasks: Task[]) -> Task[]:
  // Buscar tasks similares y consolidar
  consolidated = []

  groups = groupSimilarTasks(tasks) // Por tipo: model, handler, test

  FOR EACH group IN groups:
    IF group.length > 1 AND group.totalHours < 4:
      // Consolidar grupo en 1 task
      consolidatedTask = {
        title: generateConsolidatedTitle(group),
        description: mergeDescriptions(group),
        estimated_hours: sum(group.map(t => t.estimated_hours)),
        depends_on_index: min(group.map(t => t.depends_on_index))
      }
      consolidated.append(consolidatedTask)
    ELSE:
      // Mantener tasks individuales
      consolidated.extend(group.tasks)

  RETURN consolidated
END FUNCTION
```

#### Ajuste 2: Manejar Tasks Grandes (ACTUALIZADO)

**NUEVO: NO dividir por actividades técnicas**

```pseudo
FUNCTION handleLargeTask(task: Task, levelConfig: object) -> object:
  maxHours = levelConfig.limits.max_hours_per_task OR 4
  
  // Si la tarea no es grande, mantenerla
  IF task.estimated_hours <= maxHours:
    RETURN { tasks: [task], action: "KEPT" }
  
  // ═══════════════════════════════════════════════════════════════
  // OPCIÓN A: Mantener como una sola tarea (PREFERIDA para MVP/Standard)
  // NO dividimos por "validación", "lógica", "errores" - eso son ACTIVIDADES
  // ═══════════════════════════════════════════════════════════════
  IF levelConfig.level IN ["mvp", "standard"]:
    // Advertir pero NO dividir automáticamente
    console.warn(`⚠️ Tarea grande detectada: "${task.title}" (${task.estimated_hours}h)`)
    console.warn(`   Considere dividir por FUNCIONALIDAD, no por ACTIVIDAD`)
    
    // Marcar con warning pero mantener
    task.metadata = task.metadata OR {}
    task.metadata.warning = "large_task"
    task.metadata.suggestion = "Considerar dividir por funcionalidad si es posible"
    
    RETURN { tasks: [task], action: "KEPT_WITH_WARNING" }
  
  // ═══════════════════════════════════════════════════════════════
  // OPCIÓN B: Intentar división FUNCIONAL (solo Enterprise)
  // Solo dividir si hay límites FUNCIONALES claros
  // ═══════════════════════════════════════════════════════════════
  IF levelConfig.level == "enterprise" AND task.estimated_hours > 8:
    // Buscar puntos de división FUNCIONALES
    functionalSplits = identifyFunctionalBoundaries(task)
    
    IF functionalSplits.found:
      // Ejemplo válido: "Implementar CRUD completo" →
      //   - "Implementar Create y Read" 
      //   - "Implementar Update y Delete"
      // NO VÁLIDO: "Validación", "Lógica", "Errores" (son actividades)
      
      newTasks = []
      FOR EACH split IN functionalSplits.boundaries:
        newTasks.append({
          title: `${task.title} - ${split.name}`,
          description: split.description,
          estimated_hours: split.hours,
          depends_on_index: split.dependsOn
        })
      
      console.log(`📝 Dividida tarea grande en ${newTasks.length} tareas funcionales`)
      RETURN { tasks: newTasks, action: "SPLIT_FUNCTIONAL" }
    ELSE:
      // No se encontraron divisiones funcionales, mantener
      task.metadata = task.metadata OR {}
      task.metadata.warning = "large_task_no_split"
      
      RETURN { tasks: [task], action: "KEPT_NO_FUNCTIONAL_SPLIT" }
  
  // Default: mantener
  RETURN { tasks: [task], action: "KEPT" }
END FUNCTION

// Identificar límites funcionales (no técnicos)
FUNCTION identifyFunctionalBoundaries(task: Task) -> object:
  title = task.title.toLowerCase()
  
  // Patrones de división FUNCIONAL válidos
  validSplitPatterns = {
    "crud completo": [
      { name: "Create y Read", percentage: 0.5 },
      { name: "Update y Delete", percentage: 0.5 }
    ],
    "crud": [
      { name: "Create y Read", percentage: 0.5 },
      { name: "Update y Delete", percentage: 0.5 }
    ],
    "autenticación completa": [
      { name: "Registro", percentage: 0.4 },
      { name: "Login y Token", percentage: 0.6 }
    ]
  }
  
  FOR EACH pattern, splits IN validSplitPatterns:
    IF pattern IN title:
      RETURN {
        found: true,
        boundaries: splits.map(s => ({
          name: s.name,
          hours: task.estimated_hours * s.percentage,
          description: `Parte funcional: ${s.name}`,
          dependsOn: null // Calcular en la creación
        }))
      }
  
  // No hay patrón funcional reconocido
  RETURN { found: false }
END FUNCTION
```

**IMPORTANTE**: Las siguientes divisiones son INCORRECTAS (son actividades, no funcionalidades):
```
❌ "Task - Validación"
❌ "Task - Lógica Principal"  
❌ "Task - Manejo de Errores"
❌ "Task - Setup"
```

Las divisiones CORRECTAS son por funcionalidad:
```
✅ "CRUD - Create y Read"
✅ "CRUD - Update y Delete"
✅ "Auth - Registro de usuarios"
✅ "Auth - Login con JWT"
```

#### Ajuste 3: Agregar Task de Testing

```pseudo
FUNCTION addTestingTask(tasks: Task[]) -> Task[]:
  IF NOT hasTestTask(tasks):
    tasks.append({
      title: "Crear tests unitarios e integración",
      description: "Tests para validar funcionalidad implementada",
      estimated_hours: 2,
      depends_on_index: tasks.length - 1 // Depende de última task
    })

  RETURN tasks
END FUNCTION
```

### Salida
```typescript
{
  tasks: [ /* tasks ajustadas */ ],
  totalEstimatedHours: 14,
  adjustments_made: [
    "Consolidadas 3 tasks de validación en 1",
    "Dividida task 'Implementar todo' en 3 sub-tasks",
    "Agregada task de testing"
  ]
}
```

---

## Función 5: Extraer Criterios de Aceptación

### Entrada
```typescript
storyContent: string
```

### Lógica

```pseudo
FUNCTION extractAcceptanceCriteria(content: string) -> string[]:
  criteria = []

  // Buscar sección "Criterios de Aceptación"
  sections = content.split("##")

  FOR EACH section IN sections:
    IF "criterios de aceptación" IN section.toLowerCase():
      // Extraer bullets
      lines = section.split("\n")
      FOR EACH line IN lines:
        IF line.startsWith("- [ ]") OR line.startsWith("- "):
          criterion = line.replace(/^- \[ \] /, "").replace(/^- /, "").trim()
          criteria.append(criterion)

  RETURN criteria
END FUNCTION
```

### Salida
```typescript
[
  "Endpoint POST /api/auth/register acepta { email, password, name }",
  "Validar formato de email",
  "Retornar 201 Created con user_id"
]
```

---

## Uso desde Planner Agent

```typescript
// 1. Analizar story
const analysis = plannerHelper.analyzeStory({
  title: story.story_title,
  content: story.story_content,
  acceptanceCriteria: extractedCriteria,
  tech: "golang",
  kind: "api",
  isFixStory: false
})

// 2. Descomponer
const decomposition = plannerHelper.decomposeStory({
  storyAnalysis: analysis,
  acceptanceCriteria: extractedCriteria,
  tech: "golang",
  kind: "api",
  documentation: docSummaries
})

// 3. Validar
const validation = plannerHelper.validateDecomposition(decomposition, false)

// 4. Ajustar si necesario
if (!validation.valid) {
  decomposition = plannerHelper.adjustDecomposition(decomposition, validation.issues)
}

// 5. Usar tasks para crear en MCP
for (const taskData of decomposition.tasks) {
  await mcp_tool("create_task", { /* ... */ })
}
```

---

## Notas de Implementación

- Templates son punto de partida, NO rígidos
- Siempre adaptar al contexto específico de la story
- Priorizar atomicidad sobre número exacto de tasks
- Testing tasks son obligatorias (nunca omitir)
- File paths son sugerencias, no definitivos
