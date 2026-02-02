# Guía de Implementación de KermitConfig para JavaScript

## Estado Actual

La integración de Kermit 2.0.4 se completó exitosamente para las plataformas:
- ✅ **Android**: `KermitConfig.android.kt` (Logcat)
- ✅ **JVM/Desktop**: `KermitConfig.jvm.kt` (Console con ANSI colors)
- ⏳ **JavaScript**: Pendiente de implementación

## ¿Por qué no se implementó JS?

El módulo `test-module` utiliza el plugin de convención `kmp.android` que **solo soporta Android y Desktop (JVM)**:

```kotlin
// test-module/build.gradle.kts
plugins {
    id("kmp.android")  // Solo Android + JVM Desktop
    id("kover")
}
```

El plugin `kmp.android` configura únicamente estos targets:
- `androidTarget`
- `jvm("desktop")`

**No incluye** `js(IR)` en su configuración.

## Infraestructura JS Disponible

El proyecto **SÍ tiene soporte completo para JavaScript** a través del plugin `kmp.full`:

### Plugin kmp.full.gradle.kts

```kotlin
// build-logic/src/main/kotlin/kmp.full.gradle.kts
kotlin {
    jvm("desktop") { ... }
    
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport { enabled.set(true) }
            }
            testTask {
                useKarma { useChromeHeadless() }
            }
        }
        nodejs {
            testTask { useMocha() }
        }
        binaries.executable()
    }
}
```

### Módulo test-module-full

Ya existe un módulo preparado para multiplatform completo:

```
test-module-full/
├── build.gradle.kts  (usa kmp.full)
└── src/
    ├── commonMain/
    ├── desktopMain/
    └── jsMain/        ← Listo para KermitConfig.js.kt
```

## Cuándo Implementar KermitConfig.js.kt

La implementación de JS debe realizarse cuando:

1. **Opción A**: `test-module` migre de `kmp.android` a `kmp.full`
   - Cambiar plugin en `test-module/build.gradle.kts`
   - Crear `test-module/src/jsMain/kotlin/com/edugo/test/module/platform/KermitConfig.js.kt`

2. **Opción B**: Implementar directamente en `test-module-full`
   - Copiar `KermitLogger.kt` a `test-module-full/src/commonMain/`
   - Crear `test-module-full/src/jsMain/kotlin/com/edugo/test/module/platform/KermitConfig.js.kt`

## Implementación Sugerida de KermitConfig.js.kt

```kotlin
package com.edugo.test.module.platform

import co.touchlab.kermit.Logger

/**
 * Configuración de Kermit para JavaScript (Browser y Node.js).
 *
 * Usa ConsoleWriter de Kermit que delega a console.log/warn/error.
 * Los logs aparecen en la consola del navegador o terminal de Node.js.
 *
 * ## Características:
 * - Browser: console.log, console.warn, console.error
 * - Node.js: process.stdout, process.stderr
 * - Formateo automático con timestamp y tag
 *
 * ## Uso:
 * ```kotlin
 * // En el main() o punto de entrada
 * fun main() {
 *     KermitLogger.initialize()
 *     // ... resto de la aplicación
 * }
 * ```
 */
actual object KermitConfig {
    /**
     * Crea un Logger de Kermit configurado para JavaScript.
     *
     * En JS, Kermit 2.0+ usa automáticamente ConsoleWriter que delega
     * a console.log/warn/error en browser o Node.js.
     *
     * @return Logger de Kermit configurado para JS console
     */
    actual fun createLogger(): Logger {
        return Logger.withTag("EduGo")
    }

    /**
     * Crea un Logger personalizado con tag específico.
     *
     * @param tag Tag base para los logs
     * @return Logger de Kermit configurado
     */
    fun createCustomLogger(
        tag: String = "EduGo"
    ): Logger {
        return Logger.withTag(tag)
    }
}
```

## Dependencias Necesarias

Kermit 2.0.4 ya está agregado en `commonMain`, por lo que **no se requieren dependencias adicionales**:

```kotlin
// Ya configurado en test-module/build.gradle.kts
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kermit)  // ✅ Ya presente
            }
        }
    }
}
```

## Tests de Integración JS

La Task 2 ya contempla tests para JavaScript:

```
test-module/src/jsTest/kotlin/com/edugo/test/module/platform/KermitLoggerJsTest.kt
```

### Ejemplo de Test JS

```kotlin
package com.edugo.test.module.platform

import kotlin.test.Test
import kotlin.test.assertTrue

class KermitLoggerJsTest {
    @Test
    fun testJsLoggerInitialization() {
        KermitLogger.initialize()
        val logger = KermitLogger.getLogger()
        
        // Verificar que el logger no es nulo
        assertTrue(logger != null)
    }
    
    @Test
    fun testJsLogging() {
        KermitLogger.initialize()
        
        // Estos logs deberían aparecer en la consola del test runner
        KermitLogger.debug("TestTag", "Debug message from JS")
        KermitLogger.info("TestTag", "Info message from JS")
        KermitLogger.error("TestTag", "Error message from JS")
        
        // En JS, verificar logs requiere interceptar console.log
        // o usar herramientas de test específicas
    }
}
```

## Criterios de Completitud (Task 1)

De los criterios originales, falta completar:

- [ ] **JS usa console.log/warn/error apropiadamente** ← Pendiente
- [ ] Configuración platform-specific en jsMain ← Pendiente

Todo lo demás está implementado:
- [x] Dependencia Kermit 2.0.4 agregada
- [x] KermitLogger implementado
- [x] Android usa Logcat
- [x] JVM usa console con ANSI colors
- [x] Formatters personalizables
- [x] Helpers de inicialización
- [x] ProGuard rules

## Próximos Pasos

1. **Decidir estrategia**: ¿Migrar test-module a kmp.full o implementar en test-module-full?

2. **Crear KermitConfig.js.kt** según la plantilla de este documento

3. **Ejecutar tests JS**:
   ```bash
   ./gradlew :test-module-full:jsTest
   # o
   ./gradlew :test-module:jsTest  (si se migra a kmp.full)
   ```

4. **Verificar output** en la consola del navegador/Node.js

## Notas de Compatibilidad

- **Kermit 2.0.4** soporta completamente JavaScript (Browser + Node.js)
- **Logger.withTag()** funciona idénticamente en todas las plataformas
- **No se requieren** dependencias adicionales específicas de JS
- El **patrón expect/actual** es el mismo que Android y JVM

## Referencias

- [Kermit Documentation](https://kermit.touchlab.co/docs/)
- [Kotlin Multiplatform JS](https://kotlinlang.org/docs/js-overview.html)
- Plugin kmp.full: `/build-logic/src/main/kotlin/kmp.full.gradle.kts`
- Test module full: `/test-module-full/`

---

**Última actualización**: 2026-02-01  
**Estado**: JS pendiente - infraestructura lista  
**Prioridad**: Media (dependiente de decisión de arquitectura del proyecto)
