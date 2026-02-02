# Guía de Manejo de Errores

Esta guía documenta los patrones de manejo de errores implementados en el módulo test-module usando el tipo `Result<T>`.

## Tabla de Contenidos

1. [Conceptos Básicos](#conceptos-básicos)
2. [Patrones de Transformación](#patrones-de-transformación)
3. [Combinación de Resultados](#combinación-de-resultados)
4. [Manejo de Colecciones](#manejo-de-colecciones)
5. [Validación](#validación)
6. [Serialización](#serialización)
7. [Mejores Prácticas](#mejores-prácticas)

## Conceptos Básicos

### El tipo Result<T>

`Result<T>` es un tipo sellado que representa una operación que puede tener éxito o fallar:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val error: String) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}
```

### Creación de Results

```kotlin
// Éxito
val success = Result.Success(42)

// Fallo
val failure = Result.Failure("Algo salió mal")

// Estado de carga
val loading = Result.Loading
```

## Patrones de Transformación

### map - Transformar el valor de éxito

Aplica una función al valor dentro de un `Success`, manteniendo los fallos y estados de carga:

```kotlin
val result: Result<Int> = Result.Success(10)
val doubled: Result<Int> = result.map { it * 2 }  // Success(20)

val failure: Result<Int> = Result.Failure("Error")
val mapped: Result<Int> = failure.map { it * 2 }  // Failure("Error")
```

**Cuándo usar**: Cuando necesitas transformar un valor exitoso sin riesgo de fallo adicional.

### flatMap - Encadenar operaciones que pueden fallar

Aplica una función que retorna otro `Result`, aplanando el resultado:

```kotlin
fun validateAge(age: Int): Result<Int> =
    if (age >= 18) Result.Success(age)
    else Result.Failure("Debe ser mayor de edad")

val userAge: Result<Int> = Result.Success(25)
val validated: Result<Int> = userAge.flatMap { validateAge(it) }  // Success(25)

val youngAge: Result<Int> = Result.Success(15)
val rejected: Result<Int> = youngAge.flatMap { validateAge(it) }  // Failure("Debe ser mayor de edad")
```

**Cuándo usar**: Para encadenar operaciones donde cada paso puede fallar.

### recover - Proporcionar un valor por defecto

Recupera de un fallo proporcionando un valor alternativo:

```kotlin
val config: Result<Config> = loadConfig()
val finalConfig: Config = config.recover { defaultConfig }
```

**Cuándo usar**: Cuando un fallo es aceptable y tienes un valor por defecto razonable.

### onSuccess / onFailure - Efectos secundarios

Ejecuta código sin modificar el `Result`:

```kotlin
result
    .onSuccess { value -> logger.info("Operación exitosa: $value") }
    .onFailure { error -> logger.error("Falló: $error") }
```

**Cuándo usar**: Para logging, métricas, o cualquier efecto secundario.

## Combinación de Resultados

### zip / combine - Combinar 2 resultados

Combina dos `Result` independientes:

```kotlin
val name: Result<String> = Result.Success("Juan")
val age: Result<Int> = Result.Success(30)

val user: Result<User> = name.zip(age) { n, a -> User(n, a) }
// Success(User("Juan", 30))
```

**Fallo rápido (fail-fast)**: Si alguno falla, el resultado es `Failure` con el primer error.

```kotlin
val name: Result<String> = Result.Failure("Nombre inválido")
val age: Result<Int> = Result.Success(30)

val user: Result<User> = name.zip(age) { n, a -> User(n, a) }
// Failure("Nombre inválido")
```

### zip3, zip4, zip5 - Combinar 3-5 resultados

Para combinar múltiples resultados independientes:

```kotlin
val firstName: Result<String> = Result.Success("Juan")
val lastName: Result<String> = Result.Success("Pérez")
val email: Result<String> = Result.Success("juan@example.com")

val profile: Result<UserProfile> = zip3(firstName, lastName, email) { fn, ln, em ->
    UserProfile(fn, ln, em)
}
// Success(UserProfile("Juan", "Pérez", "juan@example.com"))
```

**Extensiones disponibles**: También hay extensiones `.zip3()`, `.zip4()`, `.zip5()` en `Result<T>`.

### Cuándo usar zip vs flatMap

**Usa zip cuando**:
- Las operaciones son independientes (paralelizables)
- Necesitas todos los valores para construir el resultado
- Quieres fallo rápido con el primer error

**Usa flatMap cuando**:
- Las operaciones dependen de resultados anteriores
- Necesitas encadenar validaciones secuenciales

```kotlin
// zip - operaciones independientes
val combined = zip3(validateEmail(email), validateAge(age), validateName(name)) { e, a, n ->
    User(e, a, n)
}

// flatMap - operaciones secuenciales
val result = loadUser(id)
    .flatMap { user -> validatePermissions(user) }
    .flatMap { user -> performAction(user) }
```

## Manejo de Colecciones

### sequence - Convertir List<Result<T>> en Result<List<T>>

**Fallo rápido**: Convierte una lista de resultados en un resultado de lista. Falla al primer error:

```kotlin
val results: List<Result<Int>> = listOf(
    Result.Success(1),
    Result.Success(2),
    Result.Success(3)
)

val combined: Result<List<Int>> = results.sequence()
// Success([1, 2, 3])

val withFailure: List<Result<Int>> = listOf(
    Result.Success(1),
    Result.Failure("Error en item 2"),
    Result.Success(3)
)

val failed: Result<List<Int>> = withFailure.sequence()
// Failure("Error en item 2")
```

### sequenceCollectingErrors - Acumular todos los errores

**Acumulación de errores**: Procesa todos los items y colecta todos los errores:

```kotlin
val results: List<Result<Int>> = listOf(
    Result.Success(1),
    Result.Failure("Error A"),
    Result.Failure("Error B"),
    Result.Success(4)
)

val accumulated: Result<List<Int>> = results.sequenceCollectingErrors(separator = "; ")
// Failure("Error A; Error B")
```

**Cuándo usar**: Cuando quieres mostrar al usuario TODOS los errores de una vez (mejor UX).

### traverse - Aplicar función que puede fallar

Aplica una función `(T) -> Result<R>` a cada elemento:

```kotlin
val ids: List<Int> = listOf(1, 2, 3)

val users: Result<List<User>> = ids.traverse { id ->
    loadUser(id)  // Result<User>
}
```

**Fallo rápido**: Detiene al primer error.

### traverseCollectingErrors - Traverse con acumulación

Aplica la función a TODOS los elementos y acumula errores:

```kotlin
val emails: List<String> = listOf("a@test.com", "invalid", "b@test.com", "also-invalid")

val validated: Result<List<Email>> = emails.traverseCollectingErrors(
    separator = "\n",
    includeIndex = true
) { email ->
    validateEmail(email)
}
// Failure("[1] Email inválido: invalid\n[3] Email inválido: also-invalid")
```

**Cuándo usar**: Validación de formularios donde quieres mostrar todos los campos inválidos.

### partition - Separar éxitos y fallos

Divide una lista en dos listas: éxitos y fallos:

```kotlin
val results: List<Result<Int>> = listOf(
    Result.Success(1),
    Result.Failure("Error A"),
    Result.Success(3),
    Result.Failure("Error B")
)

val (successes, failures) = results.partition()
// successes = [1, 3]
// failures = ["Error A", "Error B"]
```

**Cuándo usar**: Procesamiento por lotes donde algunos fallos son aceptables.

## Validación

### DomainMapper - Conversión con validación

Interfaz para convertir entre DTO y modelos de dominio:

```kotlin
interface DomainMapper<DTO, Domain> {
    fun toDomain(dto: DTO): Result<Domain>
    fun toDto(domain: Domain): DTO
}
```

**Ejemplo**:

```kotlin
object UserMapper : DomainMapper<UserDto, User> {
    override fun toDomain(dto: UserDto): Result<User> {
        val errors = accumulateValidationErrors(separator = "\n") {
            add(validateEmail(dto.email))
            add(validateRange(dto.age, 18, 120, "edad"))
            add(validateLengthRange(dto.username, 3, 30, "username"))
        }
        
        return if (errors != null) {
            Result.Failure(errors)
        } else {
            Result.Success(User(dto.username, dto.email, dto.age))
        }
    }
    
    override fun toDto(domain: User): UserDto =
        UserDto(domain.username, domain.email, domain.age)
}
```

### ValidationHelpers - Funciones reutilizables

Colección de 20+ funciones de validación:

```kotlin
// Strings
validateNotBlank(value, fieldName)
validateMinLength(value, min, fieldName)
validateMaxLength(value, max, fieldName)
validateLengthRange(value, min, max, fieldName)
validateEmail(value)
validatePattern(value, regex, fieldName, message)

// Números
validateRange(value, min, max, fieldName)
validatePositive(value, fieldName)
validateNonNegative(value, fieldName)

// Colecciones
validateNotEmpty(collection, fieldName)
validateMinSize(collection, min, fieldName)
validateMaxSize(collection, max, fieldName)

// Enums
validateIn(value, allowed, fieldName)
```

**Todas retornan `String?`**: `null` si válido, mensaje de error si inválido.

### Validación Acumulativa

Para acumular múltiples errores de validación:

```kotlin
val errors = accumulateValidationErrors(separator = "\n") {
    add(validateNotBlank(user.name, "nombre"))
    add(validateEmail(user.email))
    add(validateRange(user.age, 18, 120, "edad"))
    add(validateLengthRange(user.password, 8, 50, "contraseña"))
}

if (errors != null) {
    return Result.Failure(errors)
}
```

### Validación Condicional

```kotlin
// Validar solo si se cumple una condición
validateIf(user.isPremium) {
    validateNotBlank(user.premiumCode, "código premium")
}

// Al menos una debe ser válida
validateAtLeastOne(
    validateNotBlank(user.email, "email"),
    validateNotBlank(user.phone, "teléfono")
)

// Todas deben ser válidas
validateAll(
    validateNotBlank(user.name, "nombre"),
    validateNotBlank(user.lastName, "apellido")
)
```

## Serialización

### catchSerialization - Manejo seguro de JSON

Convierte excepciones de serialización en `Result.Failure`:

```kotlin
import com.edugo.test.module.extensions.catchSerialization

val jsonString = """{"name":"Juan","age":30}"""

val result: Result<User> = catchSerialization {
    Json.decodeFromString<User>(jsonString)
}
// Success(User("Juan", 30))

val invalidJson = """{"name":"Juan","age":"invalid"}"""
val failed: Result<User> = catchSerialization {
    Json.decodeFromString<User>(invalidJson)
}
// Failure("Error de serialización: ...")
```

### Extensiones de serialización

```kotlin
// Decodificar con manejo de errores
val user: Result<User> = safeDecodeFromString(jsonString)

// Codificar con manejo de errores
val json: Result<String> = user.safeEncodeToString()

// Combinar serialización + validación
val domainUser: Result<User> = safeDecodeFromString<UserDto>(jsonString)
    .flatMap { dto -> UserMapper.toDomain(dto) }
```

## Mejores Prácticas

### 1. Usa tipos específicos de error cuando sea apropiado

```kotlin
// ✅ BUENO: Usa AppError para errores estructurados
sealed class AppError(val code: ErrorCode, val message: String, val details: Map<String, String>)

fun loadUser(id: Int): Result<User> {
    return Result.Failure(
        AppError(ErrorCode.NOT_FOUND, "Usuario no encontrado", mapOf("id" to id.toString()))
    )
}

// ⚠️ ACEPTABLE: String simple para errores internos
fun validateAge(age: Int): Result<Int> =
    if (age >= 18) Result.Success(age)
    else Result.Failure("La edad debe ser al menos 18")
```

### 2. Usa validación acumulativa para UX

```kotlin
// ✅ BUENO: Muestra todos los errores de una vez
val errors = accumulateValidationErrors(separator = "\n") {
    add(validateEmail(dto.email))
    add(validateRange(dto.age, 18, 120, "edad"))
    add(validateNotBlank(dto.name, "nombre"))
}

// ❌ MALO: Fallo rápido en formularios
fun validate(dto: UserDto): Result<User> =
    validateEmail(dto.email)
        .flatMap { validateRange(dto.age, 18, 120, "edad") }
        .flatMap { validateNotBlank(dto.name, "nombre") }
        // Usuario solo ve el primer error :(
```

### 3. Usa zip para operaciones independientes

```kotlin
// ✅ BUENO: Operaciones independientes con zip
val profile = zip3(
    loadUserBasicInfo(id),
    loadUserPreferences(id),
    loadUserStats(id)
) { info, prefs, stats ->
    UserProfile(info, prefs, stats)
}

// ❌ MALO: flatMap para operaciones independientes
val profile = loadUserBasicInfo(id)
    .flatMap { info ->
        loadUserPreferences(id).flatMap { prefs ->
            loadUserStats(id).map { stats ->
                UserProfile(info, prefs, stats)  // Difícil de leer
            }
        }
    }
```

### 4. Usa traverse en lugar de map + sequence

```kotlin
// ✅ BUENO: traverse
val users: Result<List<User>> = ids.traverse { loadUser(it) }

// ❌ MENOS EFICIENTE: map + sequence
val users: Result<List<User>> = ids.map { loadUser(it) }.sequence()
```

### 5. Maneja Loading apropiadamente

```kotlin
// ✅ BUENO: Considera todos los estados
when (val result = loadUser(id)) {
    is Result.Success -> showUser(result.data)
    is Result.Failure -> showError(result.error)
    is Result.Loading -> showSpinner()
}

// ❌ MALO: Ignora Loading
if (result is Result.Success) {
    showUser(result.data)
}
```

### 6. Usa recover para valores por defecto

```kotlin
// ✅ BUENO: Valor por defecto con recover
val config: Config = loadConfig().recover { defaultConfig }

// ❌ VERBOSO: Patrón when
val config: Config = when (val result = loadConfig()) {
    is Result.Success -> result.data
    else -> defaultConfig
}
```

### 7. Combina flatMap para flujos complejos

```kotlin
// ✅ BUENO: Flujo completo de JSON → DTO → Domain
val user: Result<User> = safeDecodeFromString<UserDto>(jsonString)
    .flatMap { dto -> UserMapper.toDomain(dto) }
    .onSuccess { user -> logger.info("Usuario creado: ${user.id}") }
    .onFailure { error -> logger.error("Fallo: $error") }
```

### 8. Usa partition para procesamiento parcial

```kotlin
// ✅ BUENO: Procesa lo que puedas, reporta lo que falló
val (validUsers, errors) = csvRows
    .map { parseUser(it) }
    .partition()

database.insertAll(validUsers)
logger.warn("${errors.size} usuarios inválidos: ${errors.joinToString()}")
```

## Patrones de Integración

### Patrón: JSON → DTO → Domain

```kotlin
fun processUserRegistration(jsonString: String): Result<User> {
    return safeDecodeFromString<UserDto>(jsonString)
        .flatMap { dto -> UserMapper.toDomain(dto) }
        .flatMap { user -> saveToDatabase(user) }
}
```

### Patrón: Validación de formulario completo

```kotlin
fun validateRegistrationForm(form: RegistrationForm): Result<ValidatedForm> {
    val errors = accumulateValidationErrors(separator = "\n") {
        add(validateEmail(form.email))
        add(validateLengthRange(form.password, 8, 50, "contraseña"))
        add(validatePattern(form.username, "^[a-zA-Z0-9_]+$", "username", "Solo letras, números y _"))
        add(validateRange(form.age, 18, 120, "edad"))
        add(validateNotBlank(form.firstName, "nombre"))
        add(validateNotBlank(form.lastName, "apellido"))
        if (form.acceptTerms != true) {
            add("Debes aceptar los términos y condiciones")
        }
    }
    
    return if (errors != null) {
        Result.Failure(errors)
    } else {
        Result.Success(ValidatedForm(form))
    }
}
```

### Patrón: Procesamiento por lotes

```kotlin
fun importUsers(csvFile: String): ImportResult {
    val rows = parseCsv(csvFile)
    
    val results = rows.traverseCollectingErrors(includeIndex = true) { row ->
        parseCsvRow(row)
            .flatMap { dto -> UserMapper.toDomain(dto) }
    }
    
    return when (results) {
        is Result.Success -> ImportResult.AllSuccess(results.data)
        is Result.Failure -> {
            // Intenta procesar lo que puedas
            val (successes, failures) = rows
                .map { parseCsvRow(it).flatMap { dto -> UserMapper.toDomain(dto) } }
                .partition()
            
            ImportResult.PartialSuccess(successes, failures)
        }
        is Result.Loading -> ImportResult.InProgress
    }
}
```

## Referencias

- **ResultExtensions.kt**: Funciones básicas (map, flatMap, zip, recover)
- **ResultCombinators.kt**: zip3, zip4, zip5
- **CollectionResultExtensions.kt**: sequence, traverse, partition
- **DomainMapper.kt**: Patrón de conversión DTO/Domain
- **ValidationHelpers.kt**: Funciones de validación reutilizables
- **AccumulativeValidation.kt**: Sistema de acumulación de errores
- **SerializationExtensions.kt**: catchSerialization, safeDecodeFromString

## Tests de Integración

Ver ejemplos completos en:
- `ConversionIntegrationTest.kt`: Flujos completos de conversión
- `ValidationIntegrationTest.kt`: Patrones de validación complejos
