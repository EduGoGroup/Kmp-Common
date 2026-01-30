# Convention Plugins - EduGo KMP

## Plugins Disponibles

### kmp.library
Plugin base para módulos Kotlin Multiplatform sin Android.

**Uso:**
```kotlin
plugins {
    id("kmp.library")
}
```

**Configuración incluida:**
- Kotlin Multiplatform plugin
- JVM target con JDK 17
- Source sets: commonMain, commonTest, jvmMain, jvmTest
- Compiler options optimizados (`-Xcontext-receivers`, `-opt-in=kotlin.RequiresOptIn`)
- Java toolchain 17

### kmp.android
Plugin para módulos KMP con soporte Android.

**Uso:**
```kotlin
plugins {
    id("kmp.android")
}

android {
    namespace = "com.edugo.mi.modulo"  // REQUERIDO
}
```

**Configuración incluida:**
- Todo lo de kmp.library
- Android Library plugin
- compileSdk = 35, minSdk = 29 (NNAPI 1.2, Scoped Storage, TLS 1.3)
- Android target con JDK 17
- Source sets: androidMain, androidUnitTest
- JVM target adicional para tests

## Convención de Naming

| Plugin | Uso |
|--------|-----|
| `kmp.library` | Módulos pure Kotlin (sin Android) |
| `kmp.android` | Módulos con dependencias Android |

## Agregar Nuevo Módulo

1. Crear directorio: `mkdir -p mi-modulo/src/commonMain/kotlin`
2. Crear build.gradle.kts con plugin apropiado
3. Agregar a settings.gradle.kts: `include(":mi-modulo")`
4. Ejecutar: `./gradlew :mi-modulo:build`

**Ejemplo - Módulo Android:**
```kotlin
// mi-modulo/build.gradle.kts
plugins {
    id("kmp.android")
}

android {
    namespace = "com.edugo.mimodulo"
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
```

**Ejemplo - Módulo Pure Kotlin:**
```kotlin
// mi-modulo/build.gradle.kts
plugins {
    id("kmp.library")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.datetime)
            }
        }
    }
}
```

## Troubleshooting

### Plugin not found
Verificar que build-logic está incluido en settings.gradle.kts:
```kotlin
pluginManagement {
    includeBuild("build-logic")
}
```

### Namespace not set
Cada módulo Android debe definir su namespace:
```kotlin
android {
    namespace = "com.edugo.modulo"
}
```

### Java toolchain issues
Los convention plugins están configurados para Java 17. Asegúrate de tener JDK 17 instalado:
```bash
./gradlew -version  # Verifica la versión de Java
```

## Configuración de Android

Los convention plugins configuran automáticamente:
- **compileSdk**: 35
- **minSdk**: 29 (permite NNAPI 1.2 para IA On-Device)
- **targetSdk**: 35 (heredado de compileSdk)
- **Java compatibility**: VERSION_17

No es necesario especificar estos valores en cada módulo.

## Source Sets Disponibles

### kmp.library
- `commonMain` - Código común a todas las plataformas
- `commonTest` - Tests comunes
- `jvmMain` - Código específico JVM
- `jvmTest` - Tests JVM

### kmp.android
- `commonMain` - Código común a todas las plataformas
- `commonTest` - Tests comunes
- `androidMain` - Código específico Android
- `androidUnitTest` - Tests Android (JVM)
- `jvmMain` - Código específico JVM
- `jvmTest` - Tests JVM

## Referencias

- Gradle Version Catalog: `gradle/libs.versions.toml`
- Convention Plugins source: `build-logic/src/main/kotlin/`
- Kotlin: 2.1.20
- Android Gradle Plugin: 8.7.2
