# Guía de Serialización JSON

Esta guía explica cómo usar los helpers de serialización JSON disponibles en `test-module`.

## Tabla de Contenidos

- [Funciones Disponibles](#funciones-disponibles)
- [Extension Functions vs Funciones Base](#extension-functions-vs-funciones-base)
- [Configuraciones Json](#configuraciones-json)
- [Casos de Uso Comunes](#casos-de-uso-comunes)
- [Manejo de Errores](#manejo-de-errores)
- [Mejores Prácticas](#mejores-prácticas)

---

## Funciones Disponibles

### Extension Functions (Syntax Sugar)

```kotlin
// Serialización
fun <T> T.toJson(): Result<String>

// Deserialización
fun <T> String.fromJson(): Result<T>
```

**Uso:**
```kotlin
@Serializable
data class User(val id: String, val name: String)

val user = User(id = "123", name = "John")

// Serializar
val jsonResult = user.toJson()

// Deserializar
val json = """{"id":"123","name":"John"}"""
val userResult: Result<User> = json.fromJson()
```

### Funciones Base (Full-Featured)

```kotlin
// Serialización con captura de errores
fun <T> safeEncodeToString(value: T): Result<String>

// Deserialización con captura de errores
fun <T> safeDecodeFromString(json: String): Result<T>

// Captura de excepciones de serialización
fun <T> catchSerialization(block: () -> T): Result<T>

// Versión con AppError
fun <T> catchSerializationAsAppError(
    details: Map<String, String> = emptyMap(),
    block: () -> T
): Result<T>
```

---

## Extension Functions vs Funciones Base

### ¿Cuándo usar Extension Functions (`.toJson()` / `.fromJson()`)?

✅ **Usar cuando:**
- Quieres sintaxis fluida y legible
- Casos simples de serialización
- Encadenamientos con `map` / `flatMap`
- No necesitas configuración especial

```kotlin
// Ejemplo fluido
val result = user.toJson()
    .map { it.toByteArray() }
    .flatMap { sendToServer(it) }
```

### ¿Cuándo usar Funciones Base?

✅ **Usar cuando:**
- Necesitas agregar detalles contextuales al error
- Quieres usar `AppError` con códigos específicos
- Necesitas más control sobre el manejo de errores
- Casos complejos que requieren logging

```kotlin
// Ejemplo con contexto
val result = catchSerializationAsAppError(
    details = mapOf(
        "endpoint" to "/api/users",
        "operation" to "save"
    )
) {
    Json.encodeToString(user)
}
```

---

## Configuraciones Json

### Ubicación

- **JsonConfig**: `com.edugo.test.module.config.JsonConfig`
- **HttpClientFactory**: Solo para HTTP (NO usar para dominio)

### Configuraciones Disponibles

#### 1. `JsonConfig.Default` - Uso General

**Recomendada para:**
- Serialización de DTOs
- Persistencia de datos
- Cache de objetos
- Intercambio entre capas

```kotlin
val json = JsonConfig.Default.encodeToString(user)
// {"id":"123","name":"John"}
```

**Características:**
- `ignoreUnknownKeys = true` - Tolera campos extra
- `prettyPrint = false` - Compacto
- `encodeDefaults = true` - Incluye defaults

#### 2. `JsonConfig.Pretty` - Debugging

**Recomendada para:**
- Logs de desarrollo
- Debugging
- Documentación

```kotlin
val json = JsonConfig.Pretty.encodeToString(user)
// {
//   "id": "123",
//   "name": "John"
// }
```

⚠️ **No usar en producción** (tamaño mayor)

#### 3. `JsonConfig.Strict` - Validación

**Recomendada para:**
- Validación de contratos
- Testing de compatibilidad
- Detección de cambios de esquema

```kotlin
// Falla si hay campos extra
val user = JsonConfig.Strict.decodeFromString<User>(json)
```

⚠️ **No recomendada para producción** (baja tolerancia)

#### 4. `JsonConfig.Lenient` - Datos Externos

**Recomendada para:**
- Consumo de APIs externas
- Datos de terceros
- Sistemas legacy

```kotlin
val user = JsonConfig.Lenient.decodeFromString<User>(externalJson)
```

⚠️ **Solo para datos no controlados**

---

## Casos de Uso Comunes

### Caso 1: Serialización Simple

```kotlin
@Serializable
data class Product(val id: String, val price: Double)

val product = Product(id = "P123", price = 99.99)

// Opción A: Extension function
val json = product.toJson()

// Opción B: Función base
val json = safeEncodeToString(product)

when (json) {
    is Result.Success -> println(json.data)
    is Result.Failure -> println("Error: ${json.error}")
}
```

### Caso 2: Deserialización con Validación

```kotlin
@Serializable
data class User(val email: String, val age: Int) : ValidatableModel {
    override fun validate(): Result<Unit> {
        return when {
            !email.contains("@") -> failure("Invalid email")
            age < 0 -> failure("Invalid age")
            else -> success(Unit)
        }
    }
}

val json = """{"email":"user@example.com","age":25}"""

// 1. Deserializar
val userResult = json.fromJson<User>()

// 2. Validar
val validated = userResult.flatMap { user ->
    user.validate().map { user }
}

when (validated) {
    is Result.Success -> saveUser(validated.data)
    is Result.Failure -> showError(validated.error)
}
```

### Caso 3: Round-Trip Serialization

```kotlin
val originalUser = User(email = "test@example.com", age = 30)

val roundTrip = originalUser.toJson()
    .flatMap { json -> json.fromJson<User>() }
    .flatMap { user -> user.validate().map { user } }

when (roundTrip) {
    is Result.Success -> assertEquals(originalUser, roundTrip.data)
    is Result.Failure -> fail("Round trip failed: ${roundTrip.error}")
}
```

### Caso 4: Configuración Custom

```kotlin
// Para debugging con formato legible
val prettyJson = JsonConfig.Pretty.encodeToString(
    User.serializer(),
    user
)
println(prettyJson)

// Para validación estricta en tests
@Test
fun testApiContract() {
    val json = apiResponse()
    
    // Falla si el contrato cambió
    val user = JsonConfig.Strict.decodeFromString<User>(json)
    
    assertNotNull(user)
}
```

---

## Manejo de Errores

### Error Codes Relevantes

```kotlin
ErrorCode.SYSTEM_SERIALIZATION_ERROR // Error general de serialización
```

### Manejo con Result

```kotlin
val result = json.fromJson<User>()

when (result) {
    is Result.Success -> {
        val user = result.data
        // Usar el usuario
    }
    is Result.Failure -> {
        val error = result.error
        logger.error("Deserialización falló: $error")
        // Mostrar error al usuario
    }
    is Result.Loading -> {
        // No aplicable para serialización
    }
}
```

### Manejo con AppError

```kotlin
val result = catchSerializationAsAppError(
    details = mapOf("source" to "api", "model" to "User")
) {
    Json.decodeFromString<User>(json)
}

when (result) {
    is Result.Success -> processUser(result.data)
    is Result.Failure -> {
        // Convertir a AppError para análisis detallado
        val appError = AppError.fromCode(
            ErrorCode.SYSTEM_SERIALIZATION_ERROR,
            result.error
        )
        logger.error(appError.toString())
    }
}
```

---

## Mejores Prácticas

### ✅ DO

1. **Usa extension functions para casos simples**
   ```kotlin
   val json = user.toJson()
   val user = json.fromJson<User>()
   ```

2. **Valida después de deserializar**
   ```kotlin
   json.fromJson<User>()
       .flatMap { it.validate() }
   ```

3. **Usa JsonConfig.Default como estándar**
   ```kotlin
   JsonConfig.Default // Para 90% de casos
   ```

4. **Maneja errores explícitamente**
   ```kotlin
   when (result) {
       is Result.Success -> // ...
       is Result.Failure -> // ...
   }
   ```

5. **Agrega contexto en errores complejos**
   ```kotlin
   catchSerializationAsAppError(
       details = mapOf("operation" to "loadCache")
   ) { ... }
   ```

### ❌ DON'T

1. **No ignores errores de serialización**
   ```kotlin
   // ❌ MAL
   val json = user.toJson().getOrNull()
   
   // ✅ BIEN
   when (val result = user.toJson()) {
       is Result.Success -> use(result.data)
       is Result.Failure -> handleError(result.error)
   }
   ```

2. **No uses JsonConfig.Strict en producción**
   ```kotlin
   // ❌ MAL - Rompe con cambios de API
   JsonConfig.Strict.decodeFromString<User>(apiResponse)
   
   // ✅ BIEN - Tolera cambios
   JsonConfig.Default.decodeFromString<User>(apiResponse)
   ```

3. **No mezcles configuraciones Http y Domain**
   ```kotlin
   // ❌ MAL
   HttpClientFactory.json // NO usar para dominio
   
   // ✅ BIEN
   JsonConfig.Default // Para dominio
   ```

4. **No uses Pretty en producción**
   ```kotlin
   // ❌ MAL - JSON más grande
   JsonConfig.Pretty.encodeToString(data)
   
   // ✅ BIEN
   JsonConfig.Default.encodeToString(data)
   ```

---

## Ejemplos Adicionales

### Serialización de Listas

```kotlin
@Serializable
data class UserList(val users: List<User>)

val users = UserList(
    users = listOf(
        User(email = "a@example.com", age = 20),
        User(email = "b@example.com", age = 30)
    )
)

val json = users.toJson()
```

### Serialización de Maps

```kotlin
val preferences = mapOf(
    "theme" to "dark",
    "language" to "es"
)

val json = JsonConfig.Default.encodeToString(
    kotlinx.serialization.builtins.MapSerializer(
        String.serializer(),
        String.serializer()
    ),
    preferences
)
```

### Deserialización con Defaults

```kotlin
@Serializable
data class Config(
    val timeout: Int = 30,
    val retries: Int = 3
)

// JSON sin valores - usa defaults
val json = "{}"
val config = JsonConfig.Default.decodeFromString<Config>(json)
// config.timeout == 30 (default)
```

---

## Referencias

- [kotlinx.serialization Documentation](https://github.com/Kotlin/kotlinx.serialization)
- [Result Type Documentation](../core/Result.kt)
- [JsonConfig API](../../test-module/src/commonMain/kotlin/com/edugo/test/module/config/JsonConfig.kt)
- [Guía de Validación](./VALIDATION.md)
- [Guía de Manejo de Errores](./ERROR_HANDLING.md)
