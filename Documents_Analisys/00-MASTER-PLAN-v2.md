# Master Plan: Mobile Shared Modules v2

## Resumen Ejecutivo

Plan para crear dos repositorios de módulos compartidos equivalentes a `edugo-shared` de Go:

| Proyecto | Plataforma | Stack |
|----------|------------|-------|
| **edugo-swift-shared** | iOS 18+, iPadOS 18+, macOS 15+ | Swift 6.2, SPM |
| **edugo-kmp-shared** | Android 8+ (API 26), Desktop, Web | Kotlin 2.1.x, KMP |

---

## Versiones Definidas (En Piedra)

### Swift/Apple Stack
| Componente | Versión | Notas |
|------------|---------|-------|
| Swift | **6.2** | Última versión estable |
| iOS/iPadOS mínimo | **18.0** | Aprovecha Swift 6 concurrency |
| macOS mínimo | **15.0** (Sequoia) | Sincronizado con iOS 18 |
| Xcode | **16.2+** | Requerido para Swift 6.2 |
| SPM Tools Version | **6.0** | Compatible con Swift 6 |

### Kotlin/KMP Stack
| Componente | Versión | Notas |
|------------|---------|-------|
| Kotlin | **2.1.20** | Última estable (Enero 2026) |
| Android compileSdk | **35** | Android 15 |
| Android targetSdk | **35** | Android 15 |
| Android minSdk | **26** | Android 8.0 (Oreo) ~95% cobertura |
| Gradle | **8.11** | Compatible con Kotlin 2.1 |
| JDK | **21** | LTS recomendado |
| AGP | **8.7.x** | Android Gradle Plugin |

### KMP Dependencies Base
| Librería | Versión | Propósito |
|----------|---------|-----------|
| Ktor | **3.1.x** | HTTP Client multiplataforma |
| Kotlinx Coroutines | **1.10.x** | Async/Concurrency |
| Kotlinx Serialization | **1.8.x** | JSON parsing |
| Kotlinx DateTime | **0.6.x** | Manejo de fechas |
| Kermit | **2.0.x** | Logging multiplataforma |
| Multiplatform Settings | **1.3.x** | Key-value storage |
| Benasher44 UUID | **0.8.x** | UUIDs multiplataforma |

---

## Decisiones Confirmadas

### Plataformas
- **Apple**: iOS/iPadOS 18+, macOS 15+ (aprovecha Swift 6 strict concurrency)
- **KMP es CENTRAL**: Android 8+, JVM Desktop, Kotlin/JS (Web)

### Tecnologías
- **HTTP Client**: Ktor 3.1.x (KMP), URLSession nativo (Swift)
- **Logging**: Kermit 2.0.x (KMP), os.Logger nativo (Swift)
- **Storage**: multiplatform-settings (KMP), Keychain nativo (Swift)
- **Telemetría**: Módulo independiente en ambos

### Arquitectura
- **DI**: NO en shared (responsabilidad del consumidor)
- **Sin dependencias externas en Swift** - todo nativo
- **Dependencias mínimas en KMP** - solo ecosistema JetBrains/Kotlin

---

## Estructura de Módulos

### Cross Modules (Reutilizables)

| Módulo | Swift | KMP | Propósito |
|--------|-------|-----|-----------|
| Common | EduGoCommon | modules/common | Errores, extensiones, tipos base |
| Logger | EduGoLogger | modules/logger | Logging multiplataforma |
| Network | EduGoNetwork | modules/network | HTTP client |
| Storage | EduGoStorage | modules/storage | Almacenamiento seguro |
| Auth | EduGoAuth | modules/auth | Gestión de tokens, JWT |
| Analytics | EduGoAnalytics | modules/analytics | Telemetría |

### Specific Modules (Dominio EduGo)

| Módulo | Swift | KMP | Propósito |
|--------|-------|-----|-----------|
| Roles | EduGoRoles | modules/roles | SystemRole, permisos |
| Models | EduGoModels | modules/models | User, Material, etc. |
| API | EduGoAPI | modules/api | Cliente API EduGo |

---

## Fases de Implementación

### Fase 1: Setup Inicial
- [ ] Crear repositorio `edugo-swift-shared`
- [ ] Crear repositorio `edugo-kmp-shared`
- [ ] Configurar CI/CD básico

### Fase 2: Módulos Base
- [ ] common (errores, tipos)
- [ ] logger
- [ ] storage

### Fase 3: Módulos de Red
- [ ] network
- [ ] auth (tokens, JWT)

### Fase 4: Dominio EduGo
- [ ] roles
- [ ] models
- [ ] api

### Fase 5: Telemetría
- [ ] analytics

---

## Documentos de Referencia

| Documento | Descripción |
|-----------|-------------|
| [01-SWIFT-SETUP-PLAN.md](./01-SWIFT-SETUP-PLAN.md) | Plan completo para Swift 6.2 |
| [01-KMP-SETUP-PLAN.md](./01-KMP-SETUP-PLAN.md) | Plan completo para KMP Kotlin 2.1 |

---

## Consistencia con edugo-shared (Go)

Los módulos deben mantener consistencia semántica con Go:

```
edugo-shared (Go)           → edugo-swift-shared      → edugo-kmp-shared
─────────────────────────────────────────────────────────────────────────
common/errors               → EduGoCommon/Errors      → modules/common/errors
common/types/enum/role      → EduGoRoles/SystemRole   → modules/roles
auth/                       → EduGoAuth               → modules/auth
logger/                     → EduGoLogger             → modules/logger
```

---

## Matriz de Compatibilidad

### Swift 6.2 Features Utilizados
- Strict Concurrency checking (complete)
- Sendable protocol enforcement
- Actor isolation
- async/await nativo
- Structured concurrency
- Mutex (iOS 18+)

### Kotlin 2.1 Features Utilizados
- K2 compiler (default)
- Explicit API mode
- Context receivers (experimental)
- Value classes
- Sealed interfaces
- Coroutines native integration

---

**Estado**: Plan aprobado, versiones definidas
**Última actualización**: 2026-01-02
