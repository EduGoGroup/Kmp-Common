# Arquitectura de Jerarquia de Sourcesets - EduGo KMP

## Configuracion

La jerarquia de sourcesets esta configurada automaticamente via:

```properties
# gradle.properties
kotlin.mpp.applyDefaultHierarchyTemplate=true
kotlin.mpp.androidSourceSetLayoutVersion=2
```

Ademas, se configura un intermediate sourceset `jvmSharedMain` manualmente en el convention plugin `kmp.android.gradle.kts`.

## Jerarquia Visual

```
                    commonMain
                        |
                        v
                  jvmSharedMain
                        |
           +------------+------------+
           |                         |
      androidMain               desktopMain
```

### Jerarquia de Tests

```
                    commonTest
                        |
                        v
                  jvmSharedTest
                        |
           +------------+------------+
           |                         |
    androidUnitTest            desktopTest
```

## Sourcesets y sus dependencias

| Sourceset | Depende de | Descripcion |
|-----------|-----------|-------------|
| commonMain | - | Codigo Kotlin puro, todas las plataformas |
| commonTest | commonMain | Tests compartidos |
| jvmSharedMain | commonMain | Codigo JVM compartido (Android + Desktop) |
| jvmSharedTest | commonTest | Tests JVM compartidos |
| androidMain | jvmSharedMain | Codigo especifico Android |
| androidUnitTest | androidMain, jvmSharedTest | Tests Android |
| desktopMain | jvmSharedMain | Codigo especifico Desktop |
| desktopTest | desktopMain, jvmSharedTest | Tests Desktop |

## Visibilidad de codigo

### Desde commonMain:
- **Visible en:** TODOS los sourcesets
- **Usar para:** Interfaces, expect declarations, logica de negocio pura
- **Ejemplo:** `SharedClass`, `Platform` (expect)

### Desde jvmSharedMain:
- **Visible en:** androidMain, desktopMain, jvmSharedTest
- **Usar para:** Codigo que usa java.* APIs compartidas
- **NO usar:** APIs especificas de Android (android.*)
- **Ejemplo:** `JvmShared`, `JvmUtils`

### Desde androidMain:
- **Visible en:** SOLO androidMain y androidUnitTest
- **Usar para:** Context, Activity, APIs de Android
- **Ejemplo:** `Platform.android.kt`, `Logger.android.kt`

### Desde desktopMain:
- **Visible en:** SOLO desktopMain y desktopTest
- **Usar para:** Swing, AWT, APIs de Desktop
- **Ejemplo:** `Platform.jvm.kt`, `Logger.jvm.kt`

## Estructura de Directorios

```
test-module/src/
├── commonMain/kotlin/           # Codigo compartido (Kotlin puro)
│   └── com/edugo/test/module/
│       ├── SharedClass.kt       # Validacion de visibilidad
│       ├── Platform.kt          # expect class
│       └── platform/            # expect objects
├── commonTest/kotlin/           # Tests compartidos
│   └── com/edugo/test/module/
│       └── VisibilityTest.kt
├── jvmSharedMain/kotlin/        # Codigo JVM compartido
│   └── com/edugo/test/module/
│       ├── JvmShared.kt
│       └── JvmUtils.kt
├── jvmSharedTest/kotlin/        # Tests JVM compartidos
│   └── com/edugo/test/module/
│       └── JvmSharedVisibilityTest.kt
├── androidMain/kotlin/          # Implementaciones Android
│   └── com/edugo/test/module/
│       ├── AndroidUsage.kt      # Validacion de visibilidad
│       ├── Platform.android.kt  # actual class
│       └── platform/            # actual objects
├── androidUnitTest/kotlin/      # Tests Android
│   └── com/edugo/test/module/
│       └── AndroidVisibilityTest.kt
├── desktopMain/kotlin/          # Implementaciones Desktop
│   └── com/edugo/test/module/
│       ├── DesktopUsage.kt      # Validacion de visibilidad
│       ├── Platform.jvm.kt      # actual class
│       └── platform/            # actual objects
└── desktopTest/kotlin/          # Tests Desktop
    └── com/edugo/test/module/
        └── DesktopVisibilityTest.kt
```

## Comandos utiles

```bash
# Ver jerarquia de sourcesets
./gradlew :test-module:sourceSets

# Compilar por target
./gradlew :test-module:compileDebugKotlinAndroid
./gradlew :test-module:compileKotlinDesktop

# Compilar metadata (solo common)
./gradlew :test-module:compileKotlinMetadata

# Ejecutar tests de visibilidad
./gradlew :test-module:desktopTest --tests "*VisibilityTest*"
./gradlew :test-module:testDebugUnitTest --tests "*VisibilityTest*"

# Compilar todo
./gradlew :test-module:assemble
```

## Reglas de ubicacion de codigo

| Tipo de codigo | Ubicacion | Ejemplo |
|----------------|-----------|---------|
| Kotlin puro | commonMain | `expect class`, interfaces |
| java.* APIs compartidas | jvmSharedMain | `File`, `URL`, `System` |
| android.* APIs | androidMain | `Context`, `Log`, `Build` |
| Swing/AWT | desktopMain | `JFrame`, `SwingUtilities` |
| Tests compartidos | commonTest | `kotlin.test` assertions |
| Tests platform-specific | androidUnitTest / desktopTest | Mocking, platform APIs |

## Template automatico vs Manual

### Automatico (applyDefaultHierarchyTemplate=true)
- Crea automaticamente relaciones dependsOn entre sourcesets
- Genera intermediate sourcesets para targets similares
- Propagacion correcta de dependencias

### Manual (jvmSharedMain)
- Configurado explicitamente en `kmp.android.gradle.kts`
- Permite compartir codigo java.* entre Android y Desktop
- Requiere `dependsOn()` explicito

```kotlin
// kmp.android.gradle.kts
val jvmSharedMain by creating {
    dependsOn(commonMain)
}
val androidMain by getting {
    dependsOn(jvmSharedMain)
}
val desktopMain by getting {
    dependsOn(jvmSharedMain)
}
```

## Warnings Conocidos

Al usar `dependsOn()` explicito junto con el template automatico, Gradle muestra:

```
Warning: Default Kotlin Hierarchy Template Not Applied Correctly
```

Esto es esperado y no afecta la funcionalidad. El warning indica que se estan usando configuraciones manuales que sobreescriben el template.

## Referencias

- [Kotlin Multiplatform Hierarchy Template](https://kotlinlang.org/docs/multiplatform-hierarchy.html)
- [Source Set Layout](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets)
- [Expect/Actual Declarations](https://kotlinlang.org/docs/multiplatform-expect-actual.html)
