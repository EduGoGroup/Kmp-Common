# Estrategia de Dependencias por Sourceset

Documento que describe la configuración de dependencias específicas por plataforma en el proyecto KMP.

---

## Tabla de Dependencias por Sourceset

| Sourceset | Dependencias | Razón/Uso |
|-----------|--------------|-----------|
| **commonMain** | `ktor-client-core`<br>`ktor-client-content-negotiation`<br>`ktor-serialization-kotlinx-json`<br>`ktor-client-logging`<br>`kotlinx-serialization-core`<br>`kotlinx-serialization-json`<br>`kotlinx-coroutines-core` | APIs compartidas de networking, serialización y concurrencia para todas las plataformas |
| **androidMain** | `ktor-client-okhttp`<br>`kotlinx-coroutines-android` | **OkHttp**: Engine optimizado para Android con soporte HTTP/2 y connection pooling<br>**Coroutines Android**: Proporciona `Dispatchers.Main` para actualizaciones de UI |
| **jvmMain** | `ktor-client-cio`<br>`kotlinx-coroutines-swing` | **CIO**: Engine basado en coroutines para JVM puro, sin dependencias externas<br>**Coroutines Swing**: Proporciona `Dispatchers.Swing` para aplicaciones Desktop con UI |
| **jsMain** | `ktor-client-js` | Engine que usa Fetch API en browser y node-fetch en Node.js |
| **commonTest** | `kotlin-test`<br>`ktor-client-mock`<br>`kotlinx-coroutines-test`<br>`turbine` | **kotlin-test**: Framework de testing multiplataforma<br>**ktor-client-mock**: Simula respuestas HTTP para tests<br>**coroutines-test**: Utilidades para testing de coroutines<br>**turbine**: Testing de Flows |
| **jvmTest** | `mockk`<br>`kotlin-test-junit` | Mocking y assertions para JVM |
| **androidUnitTest** | `mockk-android`<br>`kotlin-test-junit` | Mocking y assertions para Android |

---

## Engines de Ktor por Plataforma

### Por qué usar diferentes engines

Ktor Client requiere un **engine** específico para cada plataforma que maneja las operaciones HTTP de bajo nivel:

| Plataforma | Engine | Razón de Elección |
|------------|--------|-------------------|
| **Android** | `ktor-client-okhttp` | - Optimizado para Android<br>- Soporte nativo de HTTP/2<br>- Connection pooling eficiente<br>- Cache de respuestas<br>- Ampliamente probado en producción |
| **JVM/Desktop** | `ktor-client-cio` | - Basado completamente en Kotlin Coroutines<br>- Sin dependencias externas pesadas<br>- Ideal para aplicaciones server-side y desktop<br>- Soporte completo de HTTP/1.1 y HTTP/2 |
| **JavaScript** | `ktor-client-js` | - Usa Fetch API nativa en browser<br>- Usa node-fetch en Node.js<br>- Integración perfecta con ecosistema JS |

---

## Dispatchers de Coroutines por Plataforma

### Android
```kotlin
Dispatchers.Main    // Para actualizaciones de UI (proporcionado por kotlinx-coroutines-android)
Dispatchers.IO      // Para operaciones de I/O
Dispatchers.Default // Para cálculos pesados
```

### JVM/Desktop
```kotlin
Dispatchers.Swing   // Para aplicaciones Swing (proporcionado por kotlinx-coroutines-swing)
Dispatchers.IO      // Para operaciones de I/O
Dispatchers.Default // Para cálculos pesados
```

### JavaScript
```kotlin
Dispatchers.Default // Único dispatcher disponible (single-threaded)
```

---

## Bundles Disponibles

En `gradle/libs.versions.toml` se definieron los siguientes bundles para facilitar el uso:

### Ktor por Plataforma
```toml
[bundles]
ktor-android = ["ktor-client-okhttp"]
ktor-desktop = ["ktor-client-cio"]
ktor-js = ["ktor-client-js"]
ktor-ios = ["ktor-client-darwin"]
ktor-common = ["ktor-client-core", "ktor-client-content-negotiation", "ktor-serialization-kotlinx-json", "ktor-client-logging"]
```

### Coroutines por Plataforma
```toml
coroutines-android = ["kotlinx-coroutines-core", "kotlinx-coroutines-android"]
coroutines-desktop = ["kotlinx-coroutines-core", "kotlinx-coroutines-swing"]
```

**Uso en build.gradle.kts:**
```kotlin
dependencies {
    implementation(libs.bundles.ktor.common)      // En commonMain
    implementation(libs.bundles.ktor.android)     // En androidMain
    implementation(libs.bundles.coroutines.android) // En androidMain
}
```

---

## Comandos de Verificación

### Verificar Resolución de Dependencias

```bash
# Ver dependencias de commonMain
./gradlew :test-module:dependencies --configuration commonMainApi

# Ver dependencias de androidMain
./gradlew :test-module:dependencies --configuration androidRuntimeClasspath

# Ver dependencias de jvmMain
./gradlew :test-module:dependencies --configuration jvmRuntimeClasspath

# Ver dependencias de jsMain (si aplica)
./gradlew :test-module:dependencies --configuration jsRuntimeClasspath
```

### Verificar Compilación por Plataforma

```bash
# Compilar Android target
./gradlew :test-module:compileKotlinAndroid

# Compilar JVM target
./gradlew :test-module:compileKotlinJvm

# Compilar JS target (si aplica)
./gradlew :test-module:compileKotlinJs

# Compilar Desktop target (si usa kmp.full)
./gradlew :test-module-full:compileKotlinDesktop
```

### Ejecutar Tests

```bash
# Ejecutar todos los tests
./gradlew :test-module:allTests

# Tests por plataforma
./gradlew :test-module:jvmTest
./gradlew :test-module:testDebugUnitTest  # Android
./gradlew :test-module-full:desktopTest
./gradlew :test-module-full:jsTest
```

---

## Ejemplo de Uso

Ver `test-module/src/commonMain/kotlin/com/edugo/test/module/NetworkClient.kt` para un ejemplo completo de:
- Uso de Ktor Client
- Kotlinx Serialization
- Kotlinx Coroutines con Dispatchers

```kotlin
package com.edugo.test.module

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(val message: String)

class NetworkClient(private val client: HttpClient) {
    suspend fun fetchData(url: String): String = withContext(Dispatchers.Default) {
        client.get(url).bodyAsText()
    }
}
```

---

## Criterios de Éxito

- [x] Todas las plataformas compilan sin errores
- [x] Dependencias se resuelven correctamente por sourceset
- [x] Engines de Ktor específicos por plataforma configurados
- [x] Dispatchers de coroutines disponibles según plataforma
- [x] Código de ejemplo funciona en cada target
- [x] Documentación actualizada

---

## Referencias

- [Ktor Client Documentation](https://ktor.io/docs/client.html)
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
- [Kotlinx Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
