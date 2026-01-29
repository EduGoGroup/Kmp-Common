# Informe de Adaptacion: Arquitectura Claude - Swift a Kotlin/Android

## Resumen Ejecutivo

Este informe identifica los archivos dentro de la carpeta `.claude` que contienen ejemplos y configuraciones especificas de Swift/Apple y que requieren adaptacion para proyectos Android/Kotlin (KMP - Kotlin Multiplatform).

---

## Archivos Identificados

### Archivo PRINCIPAL que Requiere Modificacion Completa

| Archivo | Prioridad | Impacto |
|---------|-----------|---------|
| `.claude/agents/implementer/validator-agent.md` | **CRITICA** | Archivo 100% orientado a Swift |

### Archivos con Referencias Menores a Swift

| Archivo | Prioridad | Tipo de Referencia |
|---------|-----------|-------------------|
| `.claude/agents/code-review/code-analyzer-agent.md` | Baja | Tabla de tecnologias soportadas |
| `.claude/commands/042-implementer-correction.md` | Baja | Tabla de tecnologias |
| `.claude/agents/deep-analysis/impact-filter-agent.md` | Baja | Ejemplos de proyectos iOS |
| `.claude/agents/constitution/document-finder-agent.md` | Baja | Tabla de clasificacion de tecnologias |
| `.claude/commands/011-constitution-create-project.md` | Baja | Mencion de tecnologias |

---

## Analisis Detallado

### 1. validator-agent.md (CRITICO - Requiere Reescritura Completa)

**Ubicacion**: `.claude/agents/implementer/validator-agent.md`

**Estado Actual**: Completamente orientado a Swift/Apple con:
- Swift Package Manager (SPM)
- Xcode Projects (.xcodeproj, .xcworkspace)
- XCTest framework
- Comandos: `swift build`, `swift test`, `xcodebuild`
- Deteccion de simuladores iOS
- Parsing de errores Swift

**Cambios Necesarios para Kotlin/Android**:

| Aspecto Swift | Equivalente Kotlin/Android |
|---------------|---------------------------|
| Swift Package Manager | Gradle (Kotlin DSL) |
| Package.swift | build.gradle.kts / settings.gradle.kts |
| .xcodeproj/.xcworkspace | Proyecto Gradle |
| `swift build` | `./gradlew build` o `./gradlew assemble` |
| `swift test` | `./gradlew test` o `./gradlew connectedCheck` |
| XCTest | JUnit / Kotest / Android Instrumented Tests |
| Xcode Simulator | Android Emulator / Robolectric |
| .swift files | .kt files |
| iOS/macOS destinations | Android variants (debug/release) |

**Secciones a Modificar**:

1. **Titulo y Descripcion** (linea 1-10):
   - Cambiar "Swift/Apple" por "Kotlin/Android/KMP"

2. **Stack Tecnologico** (linea 23-27):
   - Reemplazar SPM, Xcode, XCTest por Gradle, Kotlin, JUnit

3. **Entrada Esperada** (linea 32-45):
   - Cambiar `project_type: "spm"` por `project_type: "gradle"` o `project_type: "kmp"`
   - Cambiar extensiones `.swift` por `.kt`

4. **Deteccion de Proyecto** (linea 70-100):
   - Buscar `build.gradle.kts`, `settings.gradle.kts` en lugar de Package.swift
   - Detectar modulos KMP vs Android puro

5. **Seleccion de Destino** (linea 105-140):
   - Reemplazar logica de simuladores iOS por:
     - Variantes de Android (debug/release)
     - Deteccion de emuladores Android
     - Configuracion de Robolectric para tests

6. **Comandos de Build** (linea 200-280):
   ```kotlin
   // En lugar de:
   // swift build --package-path "${project_path}"
   
   // Usar:
   ./gradlew :module:build
   ./gradlew :module:assembleDebug
   ./gradlew :shared:build  // Para KMP
   ```

7. **Comandos de Test** (linea 300-380):
   ```kotlin
   // En lugar de:
   // swift test --package-path "${project_path}"
   
   // Usar:
   ./gradlew :module:test                    // Unit tests
   ./gradlew :module:connectedDebugAndroidTest  // Instrumented
   ./gradlew :shared:allTests               // KMP multiplataforma
   ```

8. **Parser de Errores** (linea 400-450):
   - Patron Swift: `/path/file.swift:line:column: error: message`
   - Patron Kotlin: `e: /path/file.kt: (line, column): error message`
   - Errores de Gradle: `FAILURE: Build failed with an exception`

9. **Timeouts** (linea 150-160):
   | Operacion | Tiempo Kotlin/Android |
   |-----------|----------------------|
   | `./gradlew build` | 300s (mas lento que Swift) |
   | `./gradlew test` | 180s |
   | `./gradlew connectedCheck` | 600s (emulador lento) |

10. **Casos de Prueba** (linea 500-700):
    - Adaptar todos los 7 casos de Swift a Kotlin
    - Agregar casos especificos de KMP (shared module)

---

### 2. code-analyzer-agent.md (Bajo Impacto)

**Ubicacion**: `.claude/agents/code-review/code-analyzer-agent.md`

**Cambio Requerido**: Agregar Kotlin a la tabla de tecnologias soportadas (linea ~290):

```markdown
| Tech | Extensiones | Analisis Especifico |
|------|-------------|---------------------|
| kotlin | .kt, .kts | Null safety, coroutines, sealed classes |
| android | .kt, .xml | Lifecycle, ViewBinding, Compose patterns |
```

---

### 3. 042-implementer-correction.md (Bajo Impacto)

**Ubicacion**: `.claude/commands/042-implementer-correction.md`

**Cambio Requerido**: Agregar Kotlin a la tabla de tecnologias (linea ~50):

```markdown
| Tech | Descripcion | Herramientas de Validacion |
|------|-------------|---------------------------|
| `kotlin` | Kotlin/JVM/Android | `./gradlew build`, `./gradlew test` |
| `android` | Android nativo | `./gradlew assembleDebug`, `./gradlew connectedCheck` |
| `kmp` | Kotlin Multiplatform | `./gradlew build`, `./gradlew allTests` |
```

---

### 4. impact-filter-agent.md (Bajo Impacto)

**Ubicacion**: `.claude/agents/deep-analysis/impact-filter-agent.md`

**Cambio Requerido**: Actualizar ejemplos de proyectos mobile (lineas ~200-250):

- Cambiar referencias a "iOS Swift 6.2" por "Android Kotlin KMP"
- Actualizar ejemplos de SwiftData por Room/SQLDelight
- Cambiar SwiftUI por Compose Multiplatform

---

### 5. document-finder-agent.md (Bajo Impacto)

**Ubicacion**: `.claude/agents/constitution/document-finder-agent.md`

**Estado Actual**: Ya tiene Kotlin en la tabla de tecnologias (linea ~100):
```markdown
| kotlin | kotlin | android, ktor, gradle |
```

**Cambio Sugerido**: Expandir para incluir KMP:
```markdown
| kotlin | kotlin | android, ktor, gradle, compose |
| kmp | kotlin | kmm, kotlin multiplatform, shared, compose, ktor |
```

---

## Plan de Accion Recomendado

### Fase 1: Archivo Critico (Prioridad Alta)

1. **Crear `validator-agent-kotlin.md`** como nuevo archivo basado en `validator-agent.md`
   - Mantener la estructura general
   - Reemplazar toda la logica Swift por Kotlin/Gradle
   - Agregar soporte para KMP (shared modules)

2. **O modificar `validator-agent.md`** para soportar multiples tecnologias:
   - Agregar deteccion automatica de proyecto (Swift vs Kotlin)
   - Bifurcar logica segun tecnologia detectada

### Fase 2: Actualizaciones Menores (Prioridad Media)

1. Actualizar tablas de tecnologias en los 4 archivos restantes
2. Agregar ejemplos de Kotlin/Android donde corresponda
3. Actualizar patterns de deteccion de errores

### Fase 3: Testing (Prioridad Alta post-implementacion)

1. Probar con proyecto Kotlin puro
2. Probar con proyecto KMP
3. Probar con proyecto Android (AGP)
4. Validar parsing de errores de Gradle

---

## Estructura Propuesta para validator-agent-kotlin.md

```markdown
---
name: validator-agent
description: Valida compilacion y ejecucion de tests de codigo Kotlin/Android/KMP
subagent_type: validator
tools: mcp__acp__Bash
model: sonnet
color: yellow
---

# Validator Agent - Kotlin/Android/KMP

## Stack Tecnologico
- Kotlin (Gradle con Kotlin DSL)
- Android Projects (build.gradle.kts)
- Kotlin Multiplatform (shared modules)
- JUnit / Kotest / Android Instrumented Tests

## Deteccion de Proyecto
1. Si existe `settings.gradle.kts` -> proyecto Gradle
2. Si existe `shared/build.gradle.kts` -> proyecto KMP
3. Si existe `app/build.gradle.kts` -> proyecto Android

## Comandos por Tipo
| Tipo | Build | Test |
|------|-------|------|
| Android | `./gradlew assembleDebug` | `./gradlew test` |
| KMP | `./gradlew :shared:build` | `./gradlew :shared:allTests` |
| Kotlin JVM | `./gradlew build` | `./gradlew test` |
```

---

## Conclusion

El archivo **`.claude/agents/implementer/validator-agent.md`** es el unico que requiere una reescritura completa para adaptar la arquitectura Claude de Swift a Kotlin/Android. Los demas archivos solo necesitan actualizaciones menores en tablas de tecnologias soportadas.

Se recomienda crear una version paralela `validator-agent-kotlin.md` o modificar el existente para soportar deteccion automatica de tecnologia.

---

**Documento generado**: 2026-01-28
**Proyecto**: Kmp-Common (Android/KMP)
**Origen**: Arquitectura Claude para Swift
