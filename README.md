# Kmp-Common

Common utilities library for Kotlin Multiplatform projects targeting JVM, JS, iOS, Android, and Desktop.

## Features

- **Type-Safe Error Handling**: `Result<T>` monad with Success/Failure/Loading states
- **Rich Error Context**: `AppError` with typed error codes, messages, and cause tracking
- **JSON Serialization**: Safe serialization/deserialization with fluent extension functions
- **Input Validation**: Comprehensive validators with both Boolean and Result-based APIs
- **Functional Utilities**: map, flatMap, recover, and more for Result composition
- **Multiplatform**: Works across JVM, JS, iOS, Android, and Desktop

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.edugo.test.module:kmp-common:1.0.0")
}
```

## Quick Start

### JSON Serialization

```kotlin
import com.edugo.test.module.serialization.*
import kotlinx.serialization.Serializable

@Serializable
data class Usuario(val nombre: String, val email: String)

// Serializar
val usuario = Usuario("Juan", "juan@ejemplo.com")
val json: Result<String> = usuario.toJson()

// Deserializar
val usuarioResult: Result<Usuario> = json.flatMap { it.fromJson<Usuario>() }
```

### Validation

```kotlin
import com.edugo.test.module.validators.*

// Boolean API
if (email.isValidEmail()) {
    println("Email válido")
}

// Result API
email.validateEmail()
    .onSuccess { validEmail ->
        procesarEmail(validEmail)
    }
    .onFailure { error ->
        mostrarError(error.message)
    }
```

### Error Handling

```kotlin
import com.edugo.test.module.core.*
import com.edugo.test.module.errors.*

fun obtenerUsuario(id: Int): Result<Usuario> {
    return if (id > 0) {
        success(Usuario(id, "Juan"))
    } else {
        failure(AppError.validation(
            code = ErrorCode.VALIDATION_FAILED,
            message = "ID debe ser mayor a 0"
        ))
    }
}

// Encadenar operaciones
val resultado = obtenerUsuario(123)
    .flatMap { usuario -> procesarUsuario(usuario) }
    .map { usuario -> usuario.nombre }
    .onFailure { error -> 
        log.error("Error: ${error.message}") 
    }
```

## Documentation

### Comprehensive Guides

Detailed guides are available in the `docs/guides/` directory:

- **[JSON Serialization Guide](docs/guides/JSON_SERIALIZATION.md)**
  - Extension functions (`.toJson()`, `.fromJson<T>()`)
  - JsonConfig configurations (Default, Pretty, Strict, Lenient)
  - Error handling patterns
  - Best practices and anti-patterns

- **[Validation Guide](docs/guides/VALIDATION.md)**
  - Base validation functions
  - Extension functions (Boolean and Result-based)
  - Fail-fast vs accumulative validation
  - ValidatableModel interface
  - Multi-field validation
  - Integration with Result<T>

- **[Error Handling Guide](docs/guides/ERROR_HANDLING.md)**
  - Result monad (Success/Failure/Loading)
  - AppError and ErrorCode
  - Error propagation patterns
  - Recovery and fallback strategies
  - Logging and debugging

- **[Examples Guide](docs/guides/EXAMPLES.md)**
  - User registration flow
  - Form validation with accumulative errors
  - API client with retry logic
  - Multi-level validation
  - Data pipeline transformations
  - Configuration with fallbacks
  - Checkout form validation

### Core APIs

#### Result<T>

Type-safe monad for success/failure/loading states:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()
    data class Loading(val progress: Float? = null) : Result<Nothing>()
}
```

**Key Functions:**
- `map`: Transform success value
- `flatMap`: Chain operations that return Result
- `mapError`: Transform error
- `recover`: Provide fallback on error
- `getOrNull`: Extract value or null
- `getOrElse`: Extract value or default
- `onSuccess/onFailure/onLoading`: Side effects

#### AppError

Rich error context:

```kotlin
data class AppError(
    val code: ErrorCode,
    val message: String,
    val details: String? = null,
    val cause: Throwable? = null,
    val timestamp: Long,
    val retryable: Boolean
)
```

**Factory Methods:**
- `AppError.validation()`: Validation errors (3000-3999)
- `AppError.network()`: Network errors (1000-1999)
- `AppError.authentication()`: Auth errors (2000-2999)
- `AppError.business()`: Business logic errors (4000-4999)
- `AppError.system()`: System errors (5000-5999)

#### ErrorCode

Typed error codes organized by category:

```kotlin
// Network (1000-1999)
NETWORK_TIMEOUT, NETWORK_NO_CONNECTION, NETWORK_SERVER_ERROR

// Auth (2000-2999)
AUTH_INVALID_CREDENTIALS, AUTH_TOKEN_EXPIRED, AUTH_INSUFFICIENT_PERMISSIONS

// Validation (3000-3999)
VALIDATION_INVALID_EMAIL, VALIDATION_OUT_OF_RANGE, VALIDATION_INVALID_UUID

// Business (4000-4999)
BUSINESS_RESOURCE_NOT_FOUND, BUSINESS_DUPLICATE_ENTRY

// System (5000-5999)
SYSTEM_UNEXPECTED_ERROR, SYSTEM_DATABASE_ERROR

// Serialization (6000-6999)
SYSTEM_SERIALIZATION_ERROR, SYSTEM_DESERIALIZATION_ERROR
```

#### Serialization Extensions

Fluent API for JSON operations:

```kotlin
// Extension functions
fun <T> T.toJson(): Result<String>
fun <T> String.fromJson(): Result<T>

// Base functions (for advanced use)
fun <T> safeEncodeToString(value: T, format: Json = Json.Default): Result<String>
fun <T> safeDecodeFromString(json: String, format: Json = Json.Default): Result<T>
```

#### JsonConfig

Pre-configured Json formats:

```kotlin
JsonConfig.Default  // Standard: ignoreUnknownKeys=true, strict, compact
JsonConfig.Pretty   // Human-readable: prettyPrint=true
JsonConfig.Strict   // Validation: ignoreUnknownKeys=false
JsonConfig.Lenient  // Permissive: isLenient=true, minimal encoding
```

#### Validation Extensions

**Boolean API** (for conditionals):
```kotlin
String.isValidEmail(): Boolean
String.isValidUUID(): Boolean
Comparable<T>.isInRange(min: T, max: T): Boolean
String.matchesPassword(confirmation: String): Boolean
```

**Result API** (for chaining):
```kotlin
String.validateEmail(): Result<String>
String.validateUUID(): Result<String>
String.validatePasswordMatch(confirmation: String): Result<Unit>
```

**Base Functions** (return AppError?):
```kotlin
validateEmail(email: String): AppError?
validateRange(value: T, min: T, max: T): AppError?
validateMinLength(value: String, minLength: Int): AppError?
validateMaxLength(value: String, maxLength: Int): AppError?
validateNotEmpty(value: String): AppError?
validateUrl(url: String): AppError?
validateNumeric(value: String): AppError?
validateAlphanumeric(value: String): AppError?
validatePattern(value: String, regex: Regex, errorMessage: String): AppError?
```

#### ValidatableModel

Interface for self-validating domain models:

```kotlin
interface ValidatableModel {
    fun validate(): AppError?
}

@Serializable
data class Usuario(
    val email: String,
    val edad: Int
) : ValidatableModel {
    override fun validate(): AppError? {
        validateEmail(email)?.let { return it }
        validateRange(edad, 18, 100)?.let { return it }
        return null
    }
}
```

#### AccumulativeValidation

Collect multiple validation errors:

```kotlin
val validacion = AccumulativeValidation().apply {
    addError(validateEmail(email))
    addError(validateRange(edad, 18, 100))
    addError(validateMinLength(nombre, 2))
}

if (validacion.hasErrors()) {
    val errores = validacion.getErrors() // List<AppError>
    // Mostrar todos los errores
}
```

## Architecture

### Package Structure

```
com.edugo.test.module
├── core              # Result monad and functional utilities
├── errors            # AppError, ErrorCode
├── serialization     # JSON serialization + extensions
├── validators        # Validation helpers + extensions
├── config            # JsonConfig configurations
└── http              # HTTP client utilities
```

### Design Principles

1. **Type Safety**: Errors are explicit in function signatures
2. **Composability**: Operations chain naturally with flatMap
3. **Fail-Fast by Default**: Stop at first error unless accumulation is needed
4. **Rich Context**: Errors include codes, messages, details, and causes
5. **Multiplatform**: All code works across all KMP targets
6. **Zero Dependencies**: Only kotlinx.serialization required

## Examples

### Complete User Registration Flow

```kotlin
@Serializable
data class RegistroRequest(
    val email: String,
    val password: String,
    val passwordConfirm: String
) : ValidatableModel {
    override fun validate(): AppError? {
        validateEmail(email)?.let { return it }
        validateMinLength(password, 8)?.let { return it }
        if (!password.matchesPassword(passwordConfirm)) {
            return AppError.validation(
                code = ErrorCode.VALIDATION_PASSWORD_MISMATCH,
                message = "Las contraseñas no coinciden"
            )
        }
        return null
    }
}

fun registrarUsuario(requestJson: String): Result<String> {
    return requestJson
        .fromJson<RegistroRequest>()
        .flatMap { request ->
            request.validate()?.let { failure(it) } ?: success(request)
        }
        .flatMap { request ->
            verificarEmailDisponible(request.email)
        }
        .map { request ->
            crearUsuario(request)
        }
        .flatMap { usuario ->
            usuario.toJson()
        }
}
```

### Form Validation with All Errors

```kotlin
fun validarFormulario(form: FormularioDTO): AccumulativeValidation {
    return AccumulativeValidation().apply {
        addError(validateEmail(form.email))
        addError(validateRange(form.edad, 18, 100))
        addError(validateMinLength(form.nombre, 2))
        addError(validateNumeric(form.telefono))
    }
}

val validacion = validarFormulario(form)
if (validacion.hasErrors()) {
    validacion.getErrors().forEach { error ->
        mostrarError(error.message)
    }
}
```

### API Client with Retry

```kotlin
suspend fun fetchData(maxRetries: Int = 3): Result<Data> {
    var lastError: AppError? = null
    
    repeat(maxRetries) { attempt ->
        val result = apiClient.getData()
        
        if (result is Result.Success) return result
        
        if (result is Result.Failure) {
            lastError = result.error
            if (!result.error.retryable) return result
            delay(1000L * (attempt + 1))
        }
    }
    
    return failure(lastError!!)
}
```

## Testing

All public APIs are thoroughly tested with kotlin.test. Run tests:

```bash
./gradlew test
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

[Your License Here]

## Support

For issues, questions, or contributions, please visit the project repository.
