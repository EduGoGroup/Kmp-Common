# Version Catalog - EduGo KMP

Documentación del Version Catalog centralizado para el proyecto Kotlin Multiplatform.

## Ubicación

```
gradle/libs.versions.toml
```

## Estructura

### [versions]

Define las versiones de todas las dependencias del proyecto:

```toml
[versions]
kotlin = "2.1.20"
agp = "8.7.2"
ktor = "3.1.3"
kotlinx-coroutines = "1.9.0"
kotlinx-serialization = "1.7.3"
```

### [libraries]

Define las dependencias con su grupo y artefacto:

```toml
[libraries]
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
```

**Uso en build.gradle.kts:**

```kotlin
dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.coroutines.core)
}
```

### [plugins]

Define plugins de Gradle:

```toml
[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
android-library = { id = "com.android.library", version.ref = "agp" }
```

**Uso en build.gradle.kts:**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}
```

### [bundles]

Agrupa dependencias relacionadas para importar múltiples librerías con una sola línea:

```toml
[bundles]
kotlinx-common = ["kotlinx-coroutines-core", "kotlinx-serialization-json", "kotlinx-datetime"]
ktor-common = ["ktor-client-core", "ktor-client-content-negotiation", "ktor-serialization-kotlinx-json", "ktor-client-logging"]
```

**Uso:**

```kotlin
dependencies {
    implementation(libs.bundles.kotlinx.common)
    implementation(libs.bundles.ktor.common)
}
```

## Convención de Naming

| Tipo | Convención | Ejemplo TOML | Acceso Kotlin |
|------|-----------|--------------|---------------|
| Versions | kebab-case | `kotlinx-coroutines` | N/A |
| Libraries | kebab-case con prefijo | `ktor-client-core` | `libs.ktor.client.core` |
| Plugins | kebab-case | `kotlin-multiplatform` | `libs.plugins.kotlin.multiplatform` |
| Bundles | descripción del grupo | `ktor-common` | `libs.bundles.ktor.common` |

## Acceso en Kotlin DSL

La conversión de TOML a Kotlin DSL sigue estas reglas:

| TOML (kebab-case) | Kotlin DSL (dot notation) |
|-------------------|---------------------------|
| `ktor-client-core` | `libs.ktor.client.core` |
| `kotlinx-coroutines-core` | `libs.kotlinx.coroutines.core` |
| `kotlin-multiplatform` (plugin) | `libs.plugins.kotlin.multiplatform` |
| `ktor-common` (bundle) | `libs.bundles.ktor.common` |

## Versiones Actuales

### Core

| Dependencia | Versión | Notas |
|-------------|---------|-------|
| Kotlin | 2.1.20 | K2 Compiler |
| AGP | 8.7.2 | Android Gradle Plugin |
| KSP | 2.1.20-1.0.29 | Kotlin Symbol Processing |

### KotlinX

| Dependencia | Versión |
|-------------|---------|
| kotlinx-coroutines | 1.9.0 |
| kotlinx-serialization | 1.7.3 |
| kotlinx-datetime | 0.6.1 |
| kotlinx-io | 0.6.0 |

### Networking

| Dependencia | Versión |
|-------------|---------|
| Ktor | 3.1.3 |

### Android

| Dependencia | Versión |
|-------------|---------|
| compileSdk | 35 |
| targetSdk | 35 |
| minSdk | 29 |
| androidx-core | 1.15.0 |
| androidx-lifecycle | 2.8.7 |

### Testing

| Dependencia | Versión |
|-------------|---------|
| JUnit | 4.13.2 |
| MockK | 1.13.13 |
| Turbine | 1.2.0 |

## Bundles Disponibles

| Bundle | Contenido |
|--------|-----------|
| `kotlinx-common` | coroutines-core, serialization-json, datetime |
| `ktor-common` | client-core, content-negotiation, serialization-kotlinx-json, logging |
| `testing-common` | kotlin-test, coroutines-test, turbine |
| `compose-core` | ui, graphics, material3, tooling-preview |
| `kotlinx-coroutines` | coroutines-core, coroutines-android |
| `kotlinx-serialization` | serialization-core, serialization-json |
| `multiplatform-settings-common` | settings, settings-coroutines, settings-serialization |

## Agregar Nueva Dependencia

1. **Agregar versión** en `[versions]` (si es nueva):
   ```toml
   [versions]
   nueva-lib = "1.0.0"
   ```

2. **Agregar library** en `[libraries]`:
   ```toml
   [libraries]
   nueva-lib = { group = "com.example", name = "nueva-lib", version.ref = "nueva-lib" }
   ```

3. **Opcionalmente agregar a un bundle**:
   ```toml
   [bundles]
   mi-bundle = ["nueva-lib", "otra-lib"]
   ```

4. **Usar en build.gradle.kts**:
   ```kotlin
   dependencies {
       implementation(libs.nueva.lib)
       // o con bundle
       implementation(libs.bundles.mi.bundle)
   }
   ```

## Actualizar Versiones

1. Modificar versión en `[versions]`:
   ```toml
   [versions]
   kotlin = "2.1.21"  # actualizado
   ```

2. Ejecutar refresh de dependencias:
   ```bash
   ./gradlew dependencies --refresh-dependencies
   ```

3. Verificar compatibilidad:
   ```bash
   ./gradlew build
   ```

## Validación

Comandos útiles para validar el version catalog:

```bash
# Verificar que el catalog se carga correctamente
./gradlew dependencies --configuration compileClasspath

# Ver todas las dependencias del proyecto
./gradlew :build-logic:dependencies

# Verificar plugins disponibles
./gradlew buildEnvironment

# Build completo para validar integración
./gradlew build
```

## Integración con build-logic

El version catalog se comparte con `build-logic` mediante:

**build-logic/settings.gradle.kts:**
```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
```

**build-logic/build.gradle.kts:**
```kotlin
dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.gradle.plugin)
}
```

## Referencias

- [Gradle Version Catalogs](https://docs.gradle.org/current/userguide/platforms.html)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Android Gradle Plugin](https://developer.android.com/build)
