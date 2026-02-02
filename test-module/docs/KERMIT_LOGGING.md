# Kermit Logging - Documentación de Integración

## Descripción General

Este módulo integra [Kermit 2.0.4](https://github.com/touchlab/Kermit) como sistema de logging multiplataforma para el proyecto EduGo. Kermit proporciona una API unificada de logging que funciona de manera consistente en Android, JVM/Desktop y JavaScript.

## Características

- ✅ **Multiplataforma**: Soporte completo para Android, JVM/Desktop y JavaScript
- ✅ **Configuración específica por plataforma**: Logcat en Android, Console con colores ANSI en JVM, console.log en JS
- ✅ **API simple y consistente**: Mismos métodos de logging en todas las plataformas
- ✅ **Formatters personalizables**: Soporte para timestamp, thread, class name
- ✅ **Thread-safe**: Inicialización idempotente y manejo seguro de concurrencia
- ✅ **ProGuard/R8 compatible**: Reglas incluidas para Android release builds

---

## Instalación

### 1. Dependencia Gradle

La dependencia de Kermit ya está configurada en `test-module/build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kermit)  // co.touchlab:kermit:2.0.4
            }
        }
    }
}
```

### 2. Verificar versión en `libs.versions.toml`

Asegúrate que tu catálogo de versiones incluye Kermit 2.0.4:

```toml
[versions]
kermit = "2.0.4"

[libraries]
kermit = { module = "co.touchlab:kermit", version.ref = "kermit" }
```

---

## Configuración por Plataforma

### Android

**Implementación**: `KermitConfig.android.kt`

```kotlin
actual object KermitConfig {
    actual fun createLogger(): Logger {
        return Logger.withTag("EduGo")
    }
}
```

**Características**:
- Usa `LogcatWriter` automáticamente (incluido en Kermit 2.0+)
- Los logs aparecen en Android Logcat
- Soporte de niveles: DEBUG, INFO, WARN, ERROR
- Filtrado por tag en Logcat

**Inicialización** (en `Application.onCreate()`):

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KermitLogger.initialize()
    }
}
```

### JVM/Desktop

**Implementación**: `KermitConfig.jvm.kt`

```kotlin
actual object KermitConfig {
    actual fun createLogger(): Logger {
        return Logger.withTag("EduGo")
    }
    
    fun formatWithColors(severity: Severity, tag: String, message: String): String {
        // Formatea con colores ANSI
    }
}
```

**Características**:
- Usa `ConsoleWriter` con salida a stdout/stderr
- Colores ANSI para mejor legibilidad:
  - 🔵 DEBUG/VERBOSE → Cyan
  - 🟢 INFO → Green  
  - 🟡 WARN → Yellow
  - 🔴 ERROR/ASSERT → Red
- Función auxiliar `formatWithColors()` para formateo personalizado

**Inicialización** (en `main()`):

```kotlin
fun main() {
    KermitLogger.initialize()
    // ... resto de la aplicación
}
```

### JavaScript (Browser/Node.js)

**Implementación**: `KermitConfig.js.kt`

```kotlin
actual object KermitConfig {
    actual fun createLogger(): Logger {
        return Logger.withTag("EduGo")
    }
}
```

**Características**:
- Usa `ConsoleWriter` que delega a funciones de console:
  - `DEBUG/VERBOSE` → `console.log()`
  - `INFO` → `console.info()`
  - `WARN` → `console.warn()`
  - `ERROR/ASSERT` → `console.error()`
- En navegador: logs visibles en DevTools
- En Node.js: logs en stdout/stderr

**Inicialización** (en `main()`):

```kotlin
fun main() {
    KermitLogger.initialize()
    // ... resto de la aplicación
}
```

---

## Uso Básico

### Inicialización

Inicializar **una vez** al inicio de la aplicación:

```kotlin
// Es idempotente - llamadas múltiples son seguras
KermitLogger.initialize()
```

### Logging Simple

```kotlin
// Debug - información detallada para desarrollo
KermitLogger.debug("NetworkClient", "Request sent to /api/users")

// Info - eventos importantes de la aplicación
KermitLogger.info("AuthManager", "User logged in successfully")

// Warn - advertencias que no son errores críticos
KermitLogger.warn("CacheManager", "Cache size approaching limit")

// Error - errores que requieren atención
KermitLogger.error("Repository", "Failed to save data")
```

### Logging con Excepciones

```kotlin
try {
    // Código que puede lanzar excepción
    performNetworkCall()
} catch (e: Exception) {
    KermitLogger.error("NetworkClient", "Request failed", e)
}
```

### Tags Recomendados

Usa nombres de clase o módulos como tags para facilitar el filtrado:

```kotlin
class UserRepository {
    fun saveUser(user: User) {
        KermitLogger.debug("UserRepository", "Saving user ${user.id}")
        // ...
    }
}

object NetworkClient {
    fun fetch() {
        KermitLogger.info("NetworkClient", "Fetching data")
        // ...
    }
}
```

---

## Configuración Avanzada

### Severity Mínima

Puedes configurar un nivel mínimo de logging para filtrar mensajes:

```kotlin
import co.touchlab.kermit.Severity

// Solo registrar WARN y ERROR
KermitLogger.setMinSeverity(Severity.Warn)

// Estos NO aparecerán después del filtro
KermitLogger.debug("Tag", "Debug message")  // Ignorado
KermitLogger.info("Tag", "Info message")    // Ignorado

// Estos SÍ aparecerán
KermitLogger.warn("Tag", "Warning")         // Registrado
KermitLogger.error("Tag", "Error")          // Registrado
```

**Niveles disponibles** (de menor a mayor severidad):
1. `Severity.Verbose`
2. `Severity.Debug`
3. `Severity.Info`
4. `Severity.Warn`
5. `Severity.Error`
6. `Severity.Assert`

### Logger Personalizado

Puedes crear loggers con tags específicos:

```kotlin
// Android
val customLogger = KermitConfig.createCustomLogger("CustomModule")
KermitLogger.setLogger(customLogger)

// JVM
val jvmLogger = KermitConfig.createCustomLogger("BackendService")
KermitLogger.setLogger(jvmLogger)

// JS
val jsLogger = KermitConfig.createCustomLogger("FrontendApp")
KermitLogger.setLogger(jsLogger)
```

### Formateo con Colores (JVM)

En JVM, puedes usar la función de formateo con colores ANSI:

```kotlin
import co.touchlab.kermit.Severity

val formatted = KermitConfig.formatWithColors(
    severity = Severity.Error,
    tag = "DatabaseRepo",
    message = "Connection failed"
)
println(formatted)  // Imprime con colores ANSI en la terminal
```

---

## ProGuard/R8 (Android)

El módulo incluye reglas de ProGuard en `consumer-rules.pro`:

```proguard
# Kermit Logging
-keep class co.touchlab.kermit.** { *; }
-keepclassmembers class co.touchlab.kermit.** { *; }

# Mantener configuración platform-specific
-keep class com.edugo.test.module.platform.KermitConfig { *; }
-keep class com.edugo.test.module.platform.KermitLogger { *; }
```

Estas reglas previenen que ProGuard/R8 eliminen o ofusquen el código de Kermit durante builds de release.

**Nota**: Si necesitas optimización más agresiva, considera reglas más específicas según tu uso real.

---

## Testing

### Tests Comunes (Todas las Plataformas)

`commonTest/KermitLoggerCommonTest.kt`:
- ✅ Inicialización idempotente
- ✅ Métodos de logging (debug, info, warn, error)
- ✅ Manejo de excepciones
- ✅ Configuración de severidad mínima

### Tests Android

`androidUnitTest/KermitLoggerAndroidTest.kt`:
- ✅ Configuración de Logcat
- ✅ Logging con múltiples tags
- ✅ Manejo de excepciones en Android

### Tests JVM

`desktopTest/KermitLoggerJvmTest.kt`:
- ✅ Console logging con colores ANSI
- ✅ Formateo personalizado
- ✅ Verificación de códigos ANSI en output

### Tests JavaScript

`jsTest/KermitLoggerJsTest.kt`:
- ✅ Console.log/warn/error delegation
- ✅ Compatibilidad Browser/Node.js
- ✅ Logging secuencial

### Ejecutar Tests

```bash
# Todos los tests
./gradlew test

# Tests específicos de plataforma
./gradlew desktopTest          # JVM tests
./gradlew testDebugUnitTest    # Android tests
./gradlew jsTest               # JavaScript tests
```

---

## Ejemplos de Uso por Escenario

### 1. Logging en ViewModel (Android)

```kotlin
class UserViewModel : ViewModel() {
    init {
        KermitLogger.initialize()
    }
    
    fun loadUser(userId: String) {
        KermitLogger.debug("UserViewModel", "Loading user $userId")
        
        viewModelScope.launch {
            try {
                val user = repository.getUser(userId)
                KermitLogger.info("UserViewModel", "User loaded successfully")
            } catch (e: Exception) {
                KermitLogger.error("UserViewModel", "Failed to load user", e)
            }
        }
    }
}
```

### 2. Logging en Repository (Común)

```kotlin
class UserRepository {
    suspend fun saveUser(user: User): Result<Unit> {
        KermitLogger.debug("UserRepository", "Saving user ${user.id}")
        
        return try {
            api.saveUser(user)
            KermitLogger.info("UserRepository", "User saved successfully")
            Result.success(Unit)
        } catch (e: NetworkException) {
            KermitLogger.error("UserRepository", "Network error saving user", e)
            Result.failure(e)
        }
    }
}
```

### 3. Logging en Cliente HTTP (Multiplataforma)

```kotlin
class ApiClient {
    suspend fun fetch(endpoint: String): Response {
        KermitLogger.debug("ApiClient", "GET $endpoint")
        
        val startTime = getCurrentTime()
        
        return try {
            val response = httpClient.get(endpoint)
            val duration = getCurrentTime() - startTime
            
            KermitLogger.info("ApiClient", "GET $endpoint - ${response.status} (${duration}ms)")
            response
        } catch (e: Exception) {
            KermitLogger.error("ApiClient", "GET $endpoint failed", e)
            throw e
        }
    }
}
```

### 4. Logging en Aplicación Desktop (JVM)

```kotlin
fun main() {
    KermitLogger.initialize()
    KermitLogger.info("Application", "Starting EduGo Desktop v1.0.0")
    
    try {
        runApplication {
            // Compose Desktop app
        }
    } catch (e: Exception) {
        KermitLogger.error("Application", "Fatal error", e)
        exitProcess(1)
    }
}
```

### 5. Logging en Aplicación Web (JS)

```kotlin
fun main() {
    KermitLogger.initialize()
    KermitLogger.info("WebApp", "EduGo Web started")
    
    window.addEventListener("error") { event ->
        KermitLogger.error("WebApp", "Unhandled error: ${event.message}")
    }
    
    // Render app
}
```

---

## Mejores Prácticas

### ✅ DO

1. **Inicializar al inicio**: Llama a `initialize()` una vez al arrancar la app
2. **Usa tags descriptivos**: Nombres de clase o módulo facilitan el filtrado
3. **Incluye contexto**: IDs, nombres de usuario, estados relevantes
4. **Log excepciones**: Siempre pasa el `Throwable` al método `error()`
5. **Niveles apropiados**:
   - `debug()` → Información detallada de desarrollo
   - `info()` → Eventos importantes del flujo
   - `warn()` → Advertencias recuperables
   - `error()` → Errores que requieren atención

### ❌ DON'T

1. **No hagas log de datos sensibles**: Contraseñas, tokens, PII
2. **No uses logging en loops intensivos**: Puede degradar performance
3. **No reinicializes innecesariamente**: `initialize()` es idempotente pero evita llamadas redundantes
4. **No uses `println()`**: Usa siempre KermitLogger para consistencia

---

## Troubleshooting

### Problema: Los logs no aparecen

**Solución**:
1. Verifica que llamaste `KermitLogger.initialize()`
2. Verifica el filtro de severidad mínima
3. En Android: revisa filtros de Logcat por tag "EduGo"
4. En JVM: verifica que la salida de console está visible
5. En JS: abre DevTools del navegador (F12)

### Problema: ProGuard elimina Kermit en release

**Solución**:
Asegúrate que `consumer-rules.pro` está configurado en `build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}
```

### Problema: Colores ANSI no aparecen en JVM

**Solución**:
Algunos terminales no soportan ANSI. Usa un terminal moderno (IntelliJ IDEA console, VSCode terminal, iTerm2, etc.)

### Problema: Build falla por API no implementada

**Solución**:
Verifica que todas las plataformas tienen su `actual object KermitConfig` implementado:
- `androidMain/KermitConfig.android.kt`
- `desktopMain/KermitConfig.jvm.kt`
- `jsMain/KermitConfig.js.kt`

---

## Arquitectura Interna

```
commonMain/
  └── KermitLogger.kt           # Wrapper principal
      └── expect KermitConfig   # Declaración expect

androidMain/
  └── KermitConfig.android.kt   # actual: LogcatWriter

desktopMain/
  └── KermitConfig.jvm.kt       # actual: ConsoleWriter + ANSI

jsMain/
  └── KermitConfig.js.kt        # actual: ConsoleWriter
```

**Flujo de logging**:
1. App llama `KermitLogger.debug/info/warn/error()`
2. KermitLogger delega a instancia de Kermit (`KermitLoggerImpl`)
3. Kermit enruta al `LogWriter` específico de plataforma
4. LogWriter escribe a Logcat/Console/console según plataforma

---

## Referencias

- [Kermit GitHub](https://github.com/touchlab/Kermit)
- [Kermit Documentation](https://kermit.touchlab.co/)
- [Kotlin Multiplatform expect/actual](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html)

---

## Changelog

### v1.0.0 - Integración Inicial
- ✅ Kermit 2.0.4 integrado
- ✅ Soporte Android, JVM, JS
- ✅ Tests multiplataforma
- ✅ Documentación completa
- ✅ ProGuard rules
- ✅ Método `warn()` agregado
- ✅ `setMinSeverity()` funcional
- ✅ Inicialización idempotente
