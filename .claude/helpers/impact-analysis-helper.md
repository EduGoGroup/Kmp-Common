# Impact Analysis Helper

## Principio: Análisis Consciente de Impacto

> **"No fragmentes para organizar. Fragmenta cuando el valor técnico lo justifique,
> considerando tu nivel y el efecto multiplicador de tu decisión."**

Este helper define el proceso de auto-cuestionamiento que TODOS los agentes deben
ejecutar ANTES de crear entidades (sprints, flow_rows, stories, tasks).

**IMPORTANTE**: Este helper es referencia para los agentes especializados:
- `milestone-analyzer-agent.md`
- `impact-filter-agent.md`
- `module-creator-agent.md`
- `story-creator-agent.md`

---

## 1. El Efecto Multiplicador

### Jerarquía de Entidades

```
Sprint (×N)
  └── Flow_Row (×M)
       └── Story (×P)
            └── Task (×Q)

Total Tasks = N × M × P × Q
```

### Impacto por Nivel de Decisión

| Paso | Impacto | Explicación |
|------|---------|-------------|
| Sprint | **MÁXIMO** | +1 sprint multiplica TODO lo que sigue |
| Flow_Row | **ALTO** | +1 módulo multiplica stories y tasks |
| Story | **MEDIO** | +1 story multiplica solo tasks |
| Task | **MÍNIMO** | +1 task es solo +1 (impacto aislado) |

### Ejemplo del Efecto

```
Decisión conservadora:
1 sprint × 1 módulo × 1 story × 3 tasks = 3 tasks

Decisión fragmentada:
1 sprint × 3 módulos × 2 stories × 3 tasks = 18 tasks

El mismo proyecto: 6x diferencia por decisiones en niveles altos.
```

---

## 2. Matriz de Análisis: Nivel × Paso

### Comportamiento Esperado

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        MATRIZ DE ANÁLISIS                                   │
├─────────────┬───────────────┬───────────────┬───────────────┬──────────────┤
│   PASO      │     MVP       │   STANDARD    │  ENTERPRISE   │   EFECTO     │
├─────────────┼───────────────┼───────────────┼───────────────┼──────────────┤
│ Sprint      │ CUESTIONAR    │ CUESTIONAR    │ CUESTIONAR    │              │
│             │ SEVERAMENTE   │ MODERADAMENTE │ CON CRITERIO  │   MÁXIMO     │
│             │ "¿Por qué >1?"│ "¿Justifica?" │ "¿Es lógico?" │              │
├─────────────┼───────────────┼───────────────┼───────────────┼──────────────┤
│ Flow_Row    │ CUESTIONAR    │ CUESTIONAR    │ CUESTIONAR    │              │
│             │ SEVERAMENTE   │ MODERADAMENTE │ CON CRITERIO  │    ALTO      │
│             │ "¿Por qué >1?"│ "¿Agrupa?"    │ "¿Coherente?" │              │
├─────────────┼───────────────┼───────────────┼───────────────┼──────────────┤
│ Story       │ CUESTIONAR    │ BALANCE       │ PUEDE         │              │
│             │ "¿Unificable?"│ "¿Atómicas?"  │ DETALLAR MÁS  │   MEDIO      │
├─────────────┼───────────────┼───────────────┼───────────────┼──────────────┤
│ Task        │ MÁS LIBERTAD  │ BALANCE       │ PUEDE SER     │              │
│             │ (task compleja│ (task media)  │ GRANULAR      │   MÍNIMO     │
│             │  está bien)   │               │               │              │
└─────────────┴───────────────┴───────────────┴───────────────┴──────────────┘
```

### Interpretación

- **MVP + Sprint/Flow_Row**: Cuestionar severamente cada división. Una task compleja 
  es preferible a múltiples módulos.
  
- **Standard**: Balance. Ni ultra-minimalista ni sobre-fragmentado.

- **Enterprise**: Puede detallar más, pero NO dividir por dividir. Cada división
  debe aportar valor técnico, no solo organizativo.

---

## 3. Flujo de Decisión

### Función: `analizarImpacto(nivel, paso, propuesta)`

```
┌─────────────────────────────────────────────────────────────────┐
│ PASO 1: Obtener Contexto                                        │
│                                                                 │
│   nivel = proyecto.project_level  // mvp | standard | enterprise│
│   paso = entidad_a_crear          // sprint|flow_row|story|task │
│   cantidad_propuesta = propuesta.length                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ PASO 2: Determinar Intensidad de Cuestionamiento                │
│                                                                 │
│   intensidad = MATRIZ[nivel][paso]                              │
│                                                                 │
│   // Resultado: "severa" | "moderada" | "criterio" | "libertad" │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ PASO 3: Calcular Efecto Multiplicador                           │
│                                                                 │
│   SI paso == "sprint" OR paso == "flow_row":                    │
│       efecto = "ALTO - cada +1 multiplica todo lo siguiente"    │
│       umbral_justificacion = ALTO                               │
│                                                                 │
│   SI paso == "story":                                           │
│       efecto = "MEDIO - cada +1 multiplica solo tasks"          │
│       umbral_justificacion = MEDIO                              │
│                                                                 │
│   SI paso == "task":                                            │
│       efecto = "BAJO - cada +1 es solo +1"                      │
│       umbral_justificacion = BAJO                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ PASO 4: Auto-Cuestionamiento                                    │
│                                                                 │
│   PARA CADA entidad en propuesta:                               │
│       pregunta = generarPregunta(intensidad, entidad)           │
│       justificacion = evaluarJustificacion(entidad)             │
│                                                                 │
│       SI justificacion < umbral_justificacion:                  │
│           marcar_para_consolidar(entidad)                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│ PASO 5: Consolidar y Retornar                                   │
│                                                                 │
│   propuesta_final = consolidar(propuesta, marcadas)             │
│   RETORNAR propuesta_final                                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Preguntas de Auto-Cuestionamiento

### Por Intensidad

#### Intensidad SEVERA (MVP en pasos altos)
```
- "¿Por qué necesito más de 1? ¿Cuál es la razón TÉCNICA?"
- "¿Puedo lograr TODO en una sola entidad?"
- "Si un revisor me pregunta por qué dividí, ¿qué respondo?"
- "¿Estoy dividiendo por organización o por necesidad técnica?"
```

#### Intensidad MODERADA (Standard, o MVP en pasos bajos)
```
- "¿Esta separación aporta valor real al desarrollo?"
- "¿Las entidades separadas tienen responsabilidades claramente distintas?"
- "¿Un desarrollador se beneficia de esta separación?"
```

#### Intensidad CON CRITERIO (Enterprise)
```
- "¿La separación es lógica y coherente con la arquitectura?"
- "¿Cada entidad tiene un propósito claro y diferenciado?"
- "¿La granularidad es apropiada para el equipo y proyecto?"
```

#### Intensidad LIBERTAD (Nivel Task en MVP)
```
- "¿La task es implementable en un tiempo razonable?"
- "¿El desarrollador puede entender la task sin contexto extra?"
```

---

## 5. Razones Válidas para Fragmentar

### Razones TÉCNICAS (Válidas)

1. **Dependencia Secuencial Obligatoria**
   - "B no puede iniciar hasta que A esté completo Y deployado"
   - Ejemplo: "No puedo crear endpoints hasta que la BD esté configurada"

2. **Dominio Técnico Diferente**
   - "A requiere expertise en X, B requiere expertise en Y"
   - Ejemplo: "Auth con JWT vs CRUD de usuarios"

3. **Riesgo de Conflicto de Código**
   - "A y B modifican los mismos archivos de forma incompatible"
   - Ejemplo: "Dos features que cambian el mismo middleware"

4. **Tamaño Excede Capacidad Cognitiva**
   - "Implementar todo junto requiere >8 horas de contexto continuo"
   - Ejemplo: "Sistema completo de pagos con múltiples proveedores"

### Razones ORGANIZATIVAS (NO Válidas para fragmentar)

- ❌ "Es más ordenado separarlo"
- ❌ "Queda más claro así"
- ❌ "Es mejor práctica tener módulos pequeños"
- ❌ "Así es más fácil de revisar"
- ❌ "Por si en el futuro necesitamos..."

---

## 6. Ejemplos por Nivel

### Proyecto: "API Health Check"

#### Si es MVP
```
Análisis:
- Nivel: MVP → cuestionar severamente
- Paso: flow_rows → efecto ALTO
- Propuesta inicial: 3 módulos (Setup, Server, Endpoint)

Auto-cuestionamiento (severo):
Q: "¿Por qué 3 módulos?"
A: "Para organizar mejor"
→ NO ES RAZÓN TÉCNICA

Q: "¿Hay dependencia secuencial obligatoria?"
A: "No, todo es código Go que puede estar junto"
→ NO JUSTIFICA

Q: "¿Dominios técnicos diferentes?"
A: "No, todo es backend Go básico"
→ NO JUSTIFICA

DECISIÓN: Consolidar en 1 módulo
RESULTADO: 1 módulo → 1 story → 2-3 tasks
```

#### Si es Standard
```
Análisis:
- Nivel: Standard → cuestionar moderadamente
- Paso: flow_rows → efecto ALTO
- Propuesta inicial: 3 módulos (Setup, Server, Endpoint)

Auto-cuestionamiento (moderado):
Q: "¿Esta separación aporta valor?"
A: "Setup es config inicial, Server es infraestructura, Endpoint es negocio"
→ PARCIALMENTE VÁLIDO

Q: "¿Pueden agruparse?"
A: "Setup + Server pueden ser 'Infraestructura', Endpoint es 'Negocio'"
→ CONSOLIDAR PARCIALMENTE

DECISIÓN: 2 módulos (Infraestructura, Negocio)
RESULTADO: 2 módulos → 2 stories → 2-3 tasks c/u
```

#### Si es Enterprise
```
Análisis:
- Nivel: Enterprise → cuestionar con criterio
- Paso: flow_rows → efecto ALTO
- Propuesta inicial: 3 módulos (Setup, Server, Endpoint)

Auto-cuestionamiento (con criterio):
Q: "¿La separación es lógica?"
A: "Para un health check simple, 3 módulos es excesivo incluso en Enterprise"
→ ENTERPRISE NO SIGNIFICA FRAGMENTAR TODO

DECISIÓN: 1-2 módulos según arquitectura del equipo
NOTA: Enterprise permite más detalle donde APORTA VALOR, 
      no fragmenta por defecto
```

---

## 7. Reglas Clave

### Regla 1: Consciencia del Multiplicador
```
ANTES de crear entidades, calcular impacto:

"Si creo N entidades aquí, el efecto en tasks totales es:"
- Sprint: N × (promedio_modulos × promedio_stories × promedio_tasks)
- Flow_Row: N × (promedio_stories × promedio_tasks)
- Story: N × promedio_tasks
- Task: N (sin multiplicador)
```

### Regla 2: Intensidad según Nivel
```
MVP       → Cuestionar TODO, justificar cada división
Standard  → Balance, ni muy estricto ni muy permisivo
Enterprise → Criterio, permitir detalle donde aporte valor
```

### Regla 3: Efecto Disminuye al Bajar
```
En pasos altos (sprint, flow_row): SER CONSERVADOR
En pasos bajos (task): MÁS LIBERTAD

Una task compleja en MVP está BIEN.
Un módulo extra en MVP es PROBLEMA.
```

### Regla 4: Enterprise NO es "Dividir Todo"
```
Enterprise significa:
- Más funcionalidades permitidas
- Más detalle donde APORTA VALOR
- Mayor capacidad de complejidad

Enterprise NO significa:
- Fragmentar por defecto
- Crear módulos vacíos "por si acaso"
- Maximizar entidades hasta el límite
```

---

## 8. Checklist de Validación

### Antes de Crear Entidades

```markdown
□ ¿Identifiqué el nivel del proyecto? (mvp/standard/enterprise)
□ ¿Identifiqué qué paso estoy ejecutando? (sprint/flow_row/story/task)
□ ¿Calculé el efecto multiplicador de mi decisión?
□ ¿Apliqué la intensidad de cuestionamiento correcta?
□ ¿Cada división tiene justificación TÉCNICA (no organizativa)?
□ ¿Intenté consolidar antes de fragmentar?
□ ¿El resultado respeta los límites del nivel?
```

### Señales de Alerta

```markdown
⚠️ MVP con más de 1 sprint → REVISAR
⚠️ MVP con más de 2 módulos → REVISAR  
⚠️ Módulo con nombre "Setup" o "Configuración" → ¿Es necesario separarlo?
⚠️ Story que solo tiene 1 task → ¿Podría consolidarse con otra story?
⚠️ Múltiples módulos que tocan los mismos archivos → CONSOLIDAR
```

---

## 9. Integración con Agentes

### Para milestone-analyzer-agent

```markdown
ANTES de proponer módulos:
1. Consultar este helper
2. Aplicar flujo de decisión con paso="flow_row"
3. Validar con checklist
```

### Para impact-filter-agent

```markdown
AL filtrar módulos propuestos:
1. Aplicar matriz de análisis
2. Cuestionar según intensidad del nivel
3. Consolidar donde sea posible
```

### Para story-creator-agent

```markdown
ANTES de crear stories:
1. Consultar este helper
2. Aplicar flujo de decisión con paso="story"
3. Validar con checklist
```

### Para module-creator-agent

```markdown
ANTES de crear flow_rows:
1. Verificar que ya pasaron por impact-filter
2. Aplicar validación final
3. No crear más de lo aprobado
```

---

## 10. Resumen Ejecutivo

```
┌────────────────────────────────────────────────────────────────┐
│                   PRINCIPIO CENTRAL                            │
│                                                                │
│  "El límite configurado es un TOPE DE EMERGENCIA,              │
│   no una META a alcanzar."                                     │
│                                                                │
│  Siempre preguntarse:                                          │
│  1. ¿Qué nivel soy?                                            │
│  2. ¿Qué efecto tiene mi decisión?                             │
│  3. ¿Puedo lograr lo mismo con MENOS fragmentación?            │
│  4. ¿Mi razón para dividir es TÉCNICA o solo organizativa?     │
│                                                                │
│  En caso de duda: CONSOLIDAR                                   │
└────────────────────────────────────────────────────────────────┘
```

---

**Versión**: 2.0
**Última actualización**: 2026-01-16
**Origen**: Migrado desde `/LLMs/Claude/.claude/helpers/impact-analysis-helper.md`
