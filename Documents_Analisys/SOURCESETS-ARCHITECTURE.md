# Arquitectura de Sourcesets - EduGo KMP

## Targets Configurados

### Plugin: kmp.android (test-module)

| Target | Sourceset | Descripcion |
|--------|-----------|-------------|
| Android | androidMain | Apps Android nativas |
| Desktop | desktopMain | Aplicaciones JVM Desktop |

### Plugin: kmp.full (test-module-full)

| Target | Sourceset | Descripcion |
|--------|-----------|-------------|
| Desktop | desktopMain | Aplicaciones JVM Desktop |
| JavaScript | jsMain | Web apps y Node.js |

## Estructura de Directorios

```
test-module/src/
├── commonMain/kotlin/       # Codigo compartido
├── commonTest/kotlin/       # Tests compartidos
├── androidMain/kotlin/      # Implementaciones Android
├── androidUnitTest/kotlin/  # Tests Android
├── desktopMain/kotlin/      # Implementaciones Desktop/JVM
└── desktopTest/kotlin/      # Tests Desktop

test-module-full/src/
├── commonMain/kotlin/       # Codigo compartido
├── commonTest/kotlin/       # Tests compartidos
├── desktopMain/kotlin/      # Implementaciones Desktop/JVM
├── desktopTest/kotlin/      # Tests Desktop
├── jsMain/kotlin/           # Implementaciones JavaScript
└── jsTest/kotlin/           # Tests JavaScript
```

## Patron expect/actual

### Platform

Declaracion basica de plataforma:

```kotlin
// commonMain
expect class Platform() {
    val name: String
}
expect fun getPlatformName(): String

// androidMain
actual class Platform actual constructor() {
    actual val name: String = "Android ${Build.VERSION.SDK_INT}"
}

// desktopMain
actual class Platform actual constructor() {
    actual val name: String = "JVM ${System.getProperty("java.version")}"
}
```

### AICapabilities

Deteccion de IA On-Device por plataforma:

| Plataforma | Gemini Nano | ML Kit | Provider Preferido |
|------------|-------------|--------|-------------------|
| Android 14+ (API 34+) | Si | Si | GEMINI_NANO |
| Android 10-13 (API 29-33) | No | Si | ML_KIT |
| Desktop | No | No | CLOUD_API |
| JavaScript | No | No | CLOUD_API |

```kotlin
// commonMain
expect object AICapabilities {
    fun isGeminiNanoAvailable(): Boolean
    fun isMLKitAvailable(): Boolean
    fun getPreferredAIProvider(): AIProvider
}

enum class AIProvider {
    GEMINI_NANO,  // Android 14+ con AICore
    ML_KIT,       // Android 5+
    CLOUD_API,    // Fallback cloud
    NONE          // Sin IA disponible
}
```

## Comandos de Compilacion

### Compilar targets individuales

```bash
# Android (debug)
./gradlew :test-module:compileDebugKotlinAndroid

# Desktop (JVM)
./gradlew :test-module:compileKotlinDesktop

# JavaScript (solo test-module-full)
./gradlew :test-module-full:compileKotlinJs

# Metadata (verifica expect/actual)
./gradlew :test-module:compileKotlinMetadata
```

### Compilar todo

```bash
# Assemble todos los targets
./gradlew :test-module:assemble

# Build completo con tests
./gradlew :test-module:build
```

### Ejecutar tests

```bash
# Tests Android
./gradlew :test-module:testDebugUnitTest

# Tests Desktop
./gradlew :test-module:desktopTest

# Tests JavaScript (solo test-module-full)
./gradlew :test-module-full:jsTest

# Todos los tests
./gradlew :test-module:allTests
```

## Convention Plugins

### kmp.android

- Targets: androidTarget, jvm("desktop")
- Uso: Modulos que necesitan Android + Desktop
- Dependencias: Ktor (OkHttp/CIO), Coroutines (Android/Swing)

### kmp.full

- Targets: jvm("desktop"), js(IR)
- Uso: Modulos multiplataforma sin Android
- Dependencias: Ktor (CIO/JS), Coroutines (Swing)

### kmp.library

- Targets: jvm
- Uso: Librerias JVM puras
- Dependencias: Ktor (CIO), Coroutines (Swing)

## Referencias

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Expect/Actual Declarations](https://kotlinlang.org/docs/multiplatform-expect-actual.html)
- [KMP Project Structure](https://kotlinlang.org/docs/multiplatform-discover-project.html)
