# Constitution Helper

Funciones de utilidad para el módulo Constitution.

## 📚 Funciones Disponibles

### inferTech(description)
Identifica la tecnología principal del proyecto.

**Guía:**
- Busca menciones explícitas del lenguaje
- Detecta frameworks que implican tecnología (Django→Python, Spring→Java)
- Default: `nodejs`

**Tecnologías:** golang, python, rust, java, typescript, csharp, ruby, php, nodejs

---

### inferKind(description)
Detecta el tipo de proyecto.

| Tipo | Ejemplos |
|------|----------|
| `api` | REST, GraphQL, gRPC, microservicios |
| `web` | Frontends, SPAs, dashboards |
| `mobile` | iOS, Android, Flutter, React Native |
| `cli` | Herramientas de terminal |
| `desktop` | Electron, apps nativas |
| `service` | Workers, daemons, cron jobs |
| `library` | SDKs, packages, librerías |

**Default:** `api`

---

### extractRequirements(description) - INFERENCIA LIBRE

**NO es una lista fija de booleanos.** El LLM debe inferir libremente.

#### Principios:
1. **Detectar lo EXPLÍCITO** - Lo que menciona la descripción
2. **Inferir lo IMPLÍCITO** - Lo necesario aunque no se diga
3. **Nombres descriptivos** - Incluir detalles, no solo true/false
4. **Ser flexible** - Incluir requisitos nuevos no anticipados

#### Ejemplos:

**"API con login JWT y roles"**
```json
{ "autenticacion": "jwt", "sistema_roles": true, "api_rest": true, "base_de_datos": true }
```

**"Tienda online con carrito y pagos Stripe"**
```json
{ "carrito_compras": true, "pasarela_pago": "stripe", "catalogo_productos": true, "autenticacion": true }
```

**"App móvil para delivery"**
```json
{ "app_movil": true, "geolocalizacion": true, "tracking_tiempo_real": true, "notificaciones_push": true }
```

---

### estimateComplexity(description, requirements) - INFERENCIA LIBRE

**NO es una fórmula de puntos fija.** El LLM debe analizar como un arquitecto senior.

#### Output esperado:
```json
{
  "score": 0-100,
  "classification": "baja|media|alta",
  "factors": { "factor1": "explicación", "factor2": "explicación" },
  "justification": "Por qué tiene esta complejidad"
}
```

#### Factores a considerar:
1. **Dominio** - ¿Conocido (blog, e-commerce) o especializado (fintech, healthcare)?
2. **Requisitos técnicos** - ¿Cuántos y qué tan complejos?
3. **Integraciones** - ¿APIs externas, servicios de terceros?
4. **Escalabilidad** - ¿Alta disponibilidad, tiempo real?
5. **Incertidumbre** - ¿Descripción clara o ambigua?

#### Ejemplos:

**Complejidad BAJA (score: 15)**
```
"CRUD básico de tareas"
→ Dominio trivial, sin integraciones, uso simple
```

**Complejidad MEDIA (score: 42)**
```
"API de inventario con auth JWT y reportes PDF"
→ Stack típico, algunas integraciones, dominio conocido
```

**Complejidad ALTA (score: 85)**
```
"Plataforma de trading con 5 exchanges y compliance"
→ Dominio regulado, múltiples integraciones, tiempo real
```

**Complejidad OCULTA (score: 38)**
```
"App simple para compartir gastos"
→ Parece simple pero tiene: división de gastos, múltiples monedas, sincronización
```

#### Guía de Scores:
| Rango | Clasificación | Típicamente |
|-------|---------------|-------------|
| 0-25 | `baja` | POCs, scripts, CRUD básico |
| 26-55 | `media` | Apps típicas con auth, DB, integraciones |
| 56-100 | `alta` | Sistemas distribuidos, tiempo real, dominios complejos |

---

### mapComplexityToLevel(classification)
Mapea complejidad a nivel de proyecto.

```
baja   → mvp
media  → standard
alta   → enterprise
```

---

### validateCoherence(level, description) ⚠️ CRÍTICA

**Puede CAMBIAR el nivel inferido.**

#### Forzar a MVP si:
- Descripción < 20 palabras
- Contiene: "hola mundo", "poc", "demo", "prototipo", "ejemplo", "tutorial"
- Declara: "simple", "básico", "sencillo"
- Feature único: "un endpoint", "un solo", "única función"

#### Degradar de Enterprise si:
- NO contiene: "enterprise", "empresarial", "corporativo", "mission critical"

#### Output:
```json
{
  "original_level": "standard",
  "final_level": "mvp",
  "was_changed": true,
  "changes": ["Contiene 'demo' → forzado a MVP"]
}
```

---

### suggestConfiguration(level)
Retorna configuración según nivel.

| Nivel | threshold_code_review | threshold_qa | max_alt_flow_depth |
|-------|----------------------|--------------|-------------------|
| mvp | 70 | 70 | 2 |
| standard | 50 | 50 | 3 |
| enterprise | 35 | 35 | 5 |

---

### suggestLimits(level)
Retorna límites según nivel.

| Nivel | max_sprints | max_flow_rows | max_total_tasks |
|-------|-------------|---------------|-----------------|
| mvp | 1 | 3 | 15 |
| standard | 3 | 6 | 50 |
| enterprise | 8 | 10 | 200 |

---

## 🔑 Principio Clave: Inferencia Libre

Tanto `requirements` como `complexity` usan **inferencia libre**:

1. NO hay listas fijas de campos
2. NO hay fórmulas rígidas de puntos
3. El LLM analiza como un arquitecto
4. Detecta tanto lo explícito como lo implícito
5. **Si encuentra algo nuevo, lo incluye**

---

**Versión**: 2.0
**Cambios**:
- v1.0: Funciones básicas
- v1.1: Agregado extractRequirements() con booleanos fijos
- v2.0: Reescrito con inferencia libre para requirements Y complexity
