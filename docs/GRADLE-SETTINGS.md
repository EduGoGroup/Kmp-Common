# Configuración de Gradle Settings - EduGo KMP

## Información del Proyecto

- **Nombre**: EduGo-KMP-Modules
- **Gradle**: 8.11
- **Kotlin**: 2.1.20
- **Android Gradle Plugin**: 8.7.2
- **Java**: 17 (requerido por AGP 8.7). 21 LTS opcional para toolchains JVM/desktop
- **Android minSdk**: 29

## Estructura del Archivo `settings.gradle.kts`

### 1. pluginManagement

Configura dónde Gradle busca plugins y en qué orden:

```kotlin
pluginManagement {
    // Incluir build-logic para Convention Plugins
    includeBuild("build-logic")
    
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
```

**Orden de resolución de plugins**:
1. `build-logic/` - Convention Plugins locales (incluido en settings)
2. `google()` - Plugins de Android (AGP, AndroidX)
3. `mavenCentral()` - Plugins de terceros (Kotlin, Ktor, etc.)
4. `gradlePluginPortal()` - Plugins oficiales de Gradle

**Filtrado de contenido**: El repositorio `google()` está configurado para incluir solo paquetes específicos de Android/Google/AndroidX, mejorando el rendimiento de resolución.

### 2. dependencyResolutionManagement

Configura cómo se resuelven las dependencias del proyecto:

```kotlin
dependencyResolutionManagement {
    // Forzar uso centralizado de repositorios
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    
    repositories {
        google()
        mavenCentral()
    }
    
    // Version Catalog: gradle/libs.versions.toml (auto-detectado por Gradle 8.11+)
}
```

**Características clave**:
- **repositoriesMode**: `FAIL_ON_PROJECT_REPOS`
  - Impide que módulos individuales definan sus propios repositorios
  - Centraliza toda la configuración de repositorios en `settings.gradle.kts`
  - Mejora la seguridad y consistencia del proyecto

- **Version Catalogs**: Auto-detectado por Gradle 8.11+ desde `gradle/libs.versions.toml` (sin bloque explícito para evitar doble import)
  - Archivo: `gradle/libs.versions.toml`
  - Acceso a versiones: `libs.versions.X`
  - Acceso a plugins: `libs.plugins.X`
  - Acceso a librerías: `libs.X` o `libs.bundles.X`

### 3. Configuración del Proyecto Raíz

```kotlin
rootProject.name = "EduGo-KMP-Modules"

// Habilitar type-safe project accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Build cache local
buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, ".gradle/build-cache")
    }
}
```

**Características**:
- **TYPESAFE_PROJECT_ACCESSORS**: Habilita acceso type-safe a proyectos (ej: `projects.core.common`)
- **Build cache local**: Almacena resultados de compilación para acelerar builds incrementales

### 4. Estructura de Módulos

```kotlin
// Incluir módulos cuando existan
// include(":core:common")
// include(":core:network")
// include(":feature:auth")
```

Los módulos se incluirán aquí conforme se creen siguiendo la arquitectura TIER.

## Orden de Resolución de Dependencias

### Para Plugins
1. Convention Plugins en `build-logic/` (includeBuild)
2. Repositorios de plugins en orden declarado:
   - Google → Maven Central → Gradle Plugin Portal
3. Settings plugins se evalúan antes que project plugins

### Para Dependencias
1. Version Catalog (`gradle/libs.versions.toml`) centraliza versiones/coordenadas (no cambia el orden de repositorios)
2. Repositorios en orden declarado:
   - Google → Maven Central

## Comandos de Diagnóstico

| Comando | Propósito |
|---------|-----------|
| `./gradlew projects` | Listar todos los módulos incluidos en el proyecto |
| `./gradlew buildEnvironment` | Ver plugins aplicados y árbol de dependencias del classpath |
| `./gradlew dependencies` | Ver árbol completo de dependencias del proyecto |
| `./gradlew dependencies --configuration compileClasspath` | Ver solo dependencias de compilación |
| `./gradlew --refresh-dependencies` | Forzar re-descarga de todas las dependencias |
| `./gradlew help` | Validar que la configuración de settings es correcta |

## Validación de Configuración

### Estado Actual (2026-01-29)
✅ **settings.gradle.kts validado correctamente con JDK 17.0.17**

**Resultados de validación**:
- `./gradlew projects` - ✅ Root project sin subproyectos; included build `build-logic`
- `./gradlew buildEnvironment` - ✅ Plugins detectados: Kotlin Multiplatform 2.1.20, AGP 8.7.2, Kotlin Serialization
- Configuration cache habilitado y funcionando

### Plugins Detectados
- `org.jetbrains.kotlin.multiplatform:2.1.20`
- `com.android.library:8.7.2`
- `com.android.application:8.7.2`
- `org.jetbrains.kotlin.plugin.serialization:2.1.20`

## Notas Importantes

### Java / JDK
- **AGP 8.7** requiere JDK 17 para ejecutar Gradle en proyectos Android.
- **Gradle 8.11** soporta ejecución con JDK 21, útil para toolchains JVM/desktop si se necesita target 21.

### Convention Plugins
El directorio `build-logic/` ya está incluido en `settings.gradle.kts` y listo para Convention Plugins. Esto permite:
- Reutilizar configuración entre módulos
- Aplicar configuraciones estándar de KMP
- Mantener builds consistentes

### JAVA_HOME
Si encuentras errores de `JAVA_HOME`, asegúrate de configurarlo correctamente:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
```

Si usas JDK 21, ajusta la ruta a `temurin-21.jdk`.

### Configuration Cache
El proyecto tiene habilitado el configuration cache de Gradle, lo que mejora significativamente el rendimiento de builds incrementales.

## Próximos Pasos

1. Crear Convention Plugins dentro de `build-logic/src/main/kotlin`
2. Definir toolchains JVM si se requiere target 21 para escritorio
3. Agregar módulos siguiendo la arquitectura TIER:
   - `:core:common` (TIER 0)
   - `:core:network` (TIER 2)
   - `:feature:auth` (TIER 3)

## Referencias

- [Gradle Version Catalogs](https://docs.gradle.org/current/userguide/platforms.html)
- [Gradle Settings API](https://docs.gradle.org/current/dsl/org.gradle.api.initialization.Settings.html)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Android Gradle Plugin](https://developer.android.com/build)
