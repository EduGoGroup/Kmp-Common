# Kermit Logging - Guía Completa

Sistema de logging multiplataforma usando Kermit 2.0.4 como backend.

---

## Tabla de Contenidos

- [Introducción](#introducción)
- [Quick Start](#quick-start)
- [Configuración por Plataforma](#configuración-por-plataforma)
- [Uso Básico](#uso-básico)
- [Uso Avanzado](#uso-avanzado)
- [Testing](#testing)
- [ProGuard/R8](#proguardr8)
- [Troubleshooting](#troubleshooting)

---

## Introducción

KermitLogger es un wrapper multiplataforma sobre Kermit 2.0.4 que proporciona logging consistente en Android, JVM/Desktop y JavaScript.

### Características

✅ **Multiplataforma**: Android, JVM, JavaScript  
✅ **Configuración específica por plataforma**:
- Android: Logcat
- JVM: Console con colores ANSI  
- JS: Browser console / Node.js stdout

✅ **API simple y consistente**  
✅ **Múltiples loggers simultáneos**  
✅ **Formatters personalizables**  
✅ **Compatible con ProGuard/R8**

---

## Quick Start

### 1. Agregar Dependencia

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kermit) // 2.0.4
            }
        }
    }
}
```

### 2. Inicializar

```kotlin
// En el punto de entrada de tu aplicación

// Android - Application.onCreate()
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        KermitLogger.initialize()
    }
}

// JVM - main()
fun main() {
    KermitLogger.initialize()
    // ... tu aplicación
}

// JS - main()
fun main() {
    KermitLogger.initialize()
    // ... tu aplicación web
}
```

### 3. Usar

```kotlin
KermitLogger.debug("MyClass", "Debug message")
KermitLogger.info("MyClass", "Info message")
KermitLogger.error("MyClass", "Error occurred", exception)
```

---

## Configuración por Plataforma

### Android

**Salida**: Logcat (`android.util.Log`)

**Configuración automática**:
```kotlin
// KermitConfig.android.kt
actual object KermitConfig {
    actual fun createLogger(): Logger {
        return Logger.withTag("EduGo")
    }
}
```

**Ver logs**:
```bash
# Todos los logs
adb logcat | grep EduGo

# Solo errores
adb logcat *:E | grep EduGo
```

**Niveles de log**:
- `debug()` → `Log.d()`
- `info()` → `Log.i()`
- `error()` → `Log.e()`

---

### JVM/Desktop

**Salida**: Console con colores ANSI

**Colores por nivel**:
- 🔵 DEBUG: Cyan
- 🟢 INFO: Green
- 🟡 WARN: Yellow
- 🔴 ERROR: Red

**Configuración**:
```kotlin
// KermitConfig.jvm.kt
actual object KermitConfig {
    actual fun createLogger(): Logger {
        return Logger.withTag("EduGo")
    }
    
    // Helper para formateo personalizado
    fun formatWithColors(severity: Severity, tag: String, message: String): String {
        // ... implementación con códigos ANSI
    }
}
```

**Ejemplo de output**:
```
[DEBUG] Network: Request sent to /api/users
[INFO] Auth: User logged in successfully
[ERROR] Database: Failed to connect
```

---

### JavaScript

**Salida**: Browser Console (DevTools) o Node.js stdout

**Mapeo de niveles**:
- `debug()` → `console.log()` (gris en DevTools)
- `info()` → `console.info()` (azul en DevTools)
- `error()` → `console.error()` (rojo en DevTools)

**Configuración**:
```kotlin
// KermitConfig.js.kt
actual object KermitConfig {
    actual fun createLogger(): Logger {
        return Logger.withTag("EduGo")
    }
}
```

**Ver logs**:
- **Browser**: Abre DevTools (F12) → pestaña Console
- **Node.js**: Los logs aparecen en la terminal

---

## Uso Básico

### Logging Simple

```kotlin
import com.edugo.test.full.platform.KermitLogger

class NetworkClient {
    fun sendRequest() {
        KermitLogger.debug("NetworkClient", "Sending GET request")
        
        try {
            // ... operación de red
            KermitLogger.info("NetworkClient", "Request successful")
        } catch (e: Exception) {
            KermitLogger.error("NetworkClient", "Request failed", e)
        }
    }
}
```

### Con Excepciones

```kotlin
try {
    riskyOperation()
} catch (e: Exception) {
    // Incluye el stack trace automáticamente
    KermitLogger.error("MyClass", "Operation failed", e)
}
```

### Tags Jerárquicos

```kotlin
// Usar tags descriptivos para filtrar
KermitLogger.debug("EduGo.Network.HTTP", "Request sent")
KermitLogger.debug("EduGo.Auth.Login", "User credentials validated")
KermitLogger.debug("EduGo.Database.Query", "SELECT executed")
```

---

## Uso Avanzado

### Múltiples Loggers

```kotlin
import co.touchlab.kermit.Logger

class MyApp {
    // Loggers específicos por módulo
    private val networkLogger = Logger.withTag("Network")
    private val authLogger = Logger.withTag("Auth")
    private val dbLogger = Logger.withTag("Database")
    
    fun example() {
        networkLogger.d { "Fetching data" }
        authLogger.i { "User authenticated" }
        dbLogger.e { "Connection failed" }
    }
}
```

### Logger Personalizado

```kotlin
// Crear logger con tag específico
val customLogger = KermitConfig.createCustomLogger("CustomModule")

// Configurar en KermitLogger
KermitLogger.setLogger(customLogger)
```

### Lazy Evaluation

```kotlin
// El mensaje solo se evalúa si el nivel está habilitado
networkLogger.d { 
    "Expensive computation: ${expensiveOperation()}" 
}

// vs

// Siempre evalúa, aunque el log esté deshabilitado
networkLogger.d("Expensive: ${expensiveOperation()}")  // ❌ No óptimo
```

### Formateo JVM con Colores

```kotlin
// Solo en JVM/Desktop
import co.touchlab.kermit.Severity

val formatted = KermitConfig.formatWithColors(
    Severity.Error,
    "MyTag",
    "Error message"
)
println(formatted)  // Imprime con colores ANSI
```

---

## Testing

### Tests Comunes (todas las plataformas)

```kotlin
// commonTest
class KermitLoggerCommonTest {
    @Test
    fun testInitialization() {
        KermitLogger.initialize()
        val logger = KermitLogger.getLogger()
        assertNotNull(logger)
    }
    
    @Test
    fun testLoggingDoesntThrow() {
        KermitLogger.debug("Test", "Message")
        KermitLogger.info("Test", "Message")
        KermitLogger.error("Test", "Message")
    }
}
```

### Tests JVM

```kotlin
// desktopTest
class KermitLoggerJvmTest {
    @Test
    fun testAnsiColors() {
        val formatted = KermitConfig.formatWithColors(
            Severity.Info,
            "Tag",
            "Message"
        )
        assertTrue(formatted.contains("\u001B["))  // Códigos ANSI
    }
}
```

### Tests JavaScript

```kotlin
// jsTest
class KermitLoggerJsTest {
    @Test
    fun testJsConsoleLogging() {
        KermitLogger.initialize()
        
        // Estos aparecen en console del navegador
        KermitLogger.debug("JsTest", "Debug")
        KermitLogger.info("JsTest", "Info")
        KermitLogger.error("JsTest", "Error")
    }
}
```

### Ejecutar Tests

```bash
# Todos los tests
./gradlew :test-module-full:allTests

# Por plataforma
./gradlew :test-module-full:desktopTest      # JVM
./gradlew :test-module-full:jsBrowserTest    # JS (browser)
./gradlew :test-module-full:jsNodeTest       # JS (node)

# Con coverage
./gradlew :test-module-full:koverHtmlReport
```

---

## ProGuard/R8

### Android Release Build

Para builds de release con ProGuard/R8 habilitado, las reglas ya están configuradas:

**Archivo**: `test-module/consumer-rules.pro`

```proguard
# Kermit Logger - Preservar logging en release
-keep class co.touchlab.kermit.** { *; }
-keepnames class co.touchlab.kermit.** { *; }

# KermitLogger y KermitConfig - Clases públicas
-keep class com.edugo.test.module.platform.KermitLogger { *; }
-keep class com.edugo.test.module.platform.KermitConfig { *; }

# Preservar métodos públicos de logging
-keepclassmembers class com.edugo.test.module.platform.KermitLogger {
    public <methods>;
}
```

### Verificar Ofuscación

```bash
# Build release
./gradlew :test-module:assembleRelease

# Verificar mapping
cat test-module/build/outputs/mapping/release/mapping.txt | grep Kermit
```

---

## Troubleshooting

### Los logs no aparecen

**Android**:
```bash
# Verificar que Logcat está filtrando correctamente
adb logcat -c  # Limpiar buffer
adb logcat | grep "EduGo"
```

**JVM**:
```kotlin
// Verificar que System.out/err no están redirigidos
System.setOut(PrintStream(FileOutputStream(FileDescriptor.out)))
```

**JS**:
```javascript
// En browser, abrir DevTools (F12)
// Verificar que no hay filtros activos en Console
```

### Logs desaparecen en Release (Android)

**Verificar ProGuard rules**:
```bash
# Las reglas están en consumer-rules.pro
# Deben ser aplicadas automáticamente
```

### Colores ANSI no funcionan en JVM

```kotlin
// Algunos terminales no soportan ANSI
// En Windows, usar Windows Terminal o ConEmu
// En IntelliJ IDEA, habilitar "Emulate terminal in output console"
```

### Tests JS fallan

```bash
# Verificar que Node.js está instalado
node --version

# Limpiar cache de Gradle
./gradlew clean

# Ejecutar con --stacktrace
./gradlew :test-module-full:jsTest --stacktrace
```

---

## Mejores Prácticas

### ✅ DO

```kotlin
// Usar tags descriptivos
KermitLogger.debug("Network.HTTP", "Request sent")

// Lazy evaluation para operaciones costosas
logger.d { "Result: ${expensiveComputation()}" }

// Incluir excepciones en errores
KermitLogger.error("DB", "Query failed", exception)

// Loggers específicos por módulo
val logger = Logger.withTag("MyModule")
```

### ❌ DON'T

```kotlin
// Tag genérico
KermitLogger.debug("App", "Something happened")  // ❌

// String interpolation sin lazy
logger.d("Result: ${expensive()}")  // ❌ Siempre evalúa

// Perder información de excepción
KermitLogger.error("DB", exception.message ?: "Error")  // ❌

// Logging sensible en producción
KermitLogger.debug("Auth", "Password: $password")  // ❌ NUNCA
```

---

## Referencias

- [Kermit Documentation](https://kermit.touchlab.co/docs/)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Android Logging Best Practices](https://developer.android.com/studio/debug/am-logcat)

---

## Changelog

### v1.0.0 (2026-02-01)
- ✅ Integración completa de Kermit 2.0.4
- ✅ Soporte Android (Logcat)
- ✅ Soporte JVM (Console con ANSI colors)
- ✅ Soporte JavaScript (Browser + Node.js)
- ✅ ProGuard/R8 rules
- ✅ Suite completa de tests
- ✅ Documentación completa

---

**Última actualización**: 2026-02-01  
**Versión**: 1.0.0  
**Mantenedor**: EduGo Team
