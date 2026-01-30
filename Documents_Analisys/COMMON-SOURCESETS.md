# Estructura de Sourcesets Comunes - EduGo KMP

## Organizacion de commonMain

```
commonMain/kotlin/com/edugo/test/module/
  TestClass.kt              - Clase de ejemplo base
  AICapabilities.kt         - expect class para capacidades de AI
  Platform.kt               - expect class Platform (legacy)
  NetworkClient.kt          - Cliente de red compartido
  
  core/
    Result.kt               - Sealed class para manejo de resultados (Success/Error/Loading)
  
  network/
    HttpClientFactory.kt    - Factory para crear HttpClient con plugins configurados
  
  data/models/
    BaseModel.kt            - Interface base + SampleModel serializable
  
  platform/
    Platform.kt             - expect object: informacion de plataforma
    Dispatchers.kt          - expect object: coroutine dispatchers por plataforma
    Logger.kt               - expect object: sistema de logging multiplataforma
```

## Organizacion de commonTest

```
commonTest/kotlin/com/edugo/test/module/
  TestClassTest.kt          - Tests de TestClass.greeting()
  PlatformTest.kt           - Tests de Platform (legacy)
  
  core/
    ResultTest.kt           - Tests de Result sealed class y map()
  
  data/models/
    SampleModelTest.kt      - Tests de serializacion JSON
```

## Dependencias Comunes

### Production (commonMain)

| Dependencia | Version | Uso |
|-------------|---------|-----|
| kotlinx-coroutines-core | 1.9.0 | Async/await, Flow, Dispatchers |
| kotlinx-serialization-json | 1.7.3 | JSON parsing y serialization |
| kotlinx-datetime | 0.6.1 | Manejo de fechas multiplataforma |
| ktor-client-core | 3.1.3 | HTTP client base |
| ktor-client-content-negotiation | 3.1.3 | Content negotiation plugin |
| ktor-serialization-kotlinx-json | 3.1.3 | JSON serialization para Ktor |
| ktor-client-logging | 3.1.3 | Logging de requests/responses |

### Testing (commonTest)

| Dependencia | Version | Uso |
|-------------|---------|-----|
| kotlin-test | 2.1.20 | Framework de testing multiplataforma |
| kotlinx-coroutines-test | 1.9.0 | Utilities para testing de coroutines |
| ktor-client-mock | 3.1.3 | Mock client para testing de network |
| turbine | 1.2.0 | Testing de Flows |

## Bundles Utilizados

### En convention plugins (kmp.library.gradle.kts, kmp.android.gradle.kts):

```kotlin
// commonMain dependencies
implementation(libs.bundles.kotlinx.common)  // coroutines + serialization + datetime
implementation(libs.bundles.ktor.common)     // ktor core + plugins
```

## Patron expect/actual

### Declaraciones expect en commonMain

1. **platform/Platform.kt** - Informacion de plataforma
   - `val name: String` - Nombre de plataforma
   - `val osVersion: String` - Version del OS
   - `val isDebug: Boolean` - Modo debug

2. **platform/AppDispatchers.kt** - Dispatchers por plataforma
   - `val Main: CoroutineDispatcher` - UI dispatcher
   - `val IO: CoroutineDispatcher` - IO operations
   - `val Default: CoroutineDispatcher` - Computation

3. **platform/Logger.kt** - Logging multiplataforma
   - `fun debug(tag: String, message: String)`
   - `fun info(tag: String, message: String)`
   - `fun error(tag: String, message: String, throwable: Throwable?)`

### Implementaciones actual requeridas

Las siguientes implementaciones deben crearse en:
- `androidMain/` - Android platform
- `desktopMain/` - JVM/Desktop platform
- `jsMain/` - JavaScript platform (futuro)

## Comandos Utiles

### Compilacion

```bash
# Compilar codigo comun (metadata)
./gradlew :test-module:compileKotlinMetadata

# Compilar target especifico
./gradlew :test-module:compileKotlinDesktop
./gradlew :test-module:compileDebugKotlinAndroid

# Build completo
./gradlew :test-module:build
```

### Testing

```bash
# Todos los tests
./gradlew :test-module:allTests

# Tests por plataforma
./gradlew :test-module:desktopTest
./gradlew :test-module:testDebugUnitTest  # Android unit tests
```

### Dependencias

```bash
# Ver dependencias de commonMain
./gradlew :test-module:dependencies --configuration desktopCompileClasspath

# Ver dependencias de commonTest
./gradlew :test-module:dependencies --configuration desktopTestCompileClasspath
```

### Verificacion

```bash
# Ver sourcesets configurados
./gradlew :test-module:sourceSets

# Ver estructura de directorios
tree test-module/src
```

## Arquitectura de Paquetes

### core/
Clases y utilidades fundamentales del dominio:
- Result pattern para manejo de estados
- Extensiones y utilities comunes

### network/
Configuracion y factories de networking:
- HttpClientFactory con plugins preconfigurados
- Endpoints y configuracion de red

### data/models/
Modelos de datos serializables:
- Interfaces base (BaseModel)
- Data classes con @Serializable
- DTOs y entidades

### platform/
Abstracciones de APIs especificas de plataforma:
- expect/actual declarations
- Platform-specific utilities
- Dispatchers, logging, platform info

## Notas Tecnicas

1. **HttpClientFactory** esta diseñado para recibir engines especificos de plataforma:
   - Android: OkHttp engine
   - Desktop: CIO engine
   - JS: JS engine

2. **Result sealed class** proporciona type-safe handling de estados:
   - Success<T> para datos exitosos
   - Error para errores con excepciones
   - Loading para estados de carga

3. **Bundles** simplifican mantenimiento:
   - Actualizaciones centralizadas en gradle/libs.versions.toml
   - Menos declaraciones en convention plugins
   - Consistencia entre modulos

4. **Tests multiplataforma** en commonTest se ejecutan en todas las plataformas configuradas automaticamente.

## Estado Actual

✅ **Completado:**
- Estructura de paquetes organizada
- Dependencias comunes configuradas con bundles
- Result pattern implementado
- HttpClientFactory con plugins
- Tests basicos de serializacion y Result
- expect declarations para Platform, AppDispatchers, Logger

⏳ **Pendiente:**
- Implementaciones actual en androidMain, desktopMain
- Tests de HttpClientFactory (requiere engine mock)
- Tests de platform-specific features

## Referencias

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Ktor Client Documentation](https://ktor.io/docs/client.html)
- [kotlinx.serialization Guide](https://github.com/Kotlin/kotlinx.serialization)
- [Convention Plugins Guide](https://docs.gradle.org/current/samples/sample_convention_plugins.html)
