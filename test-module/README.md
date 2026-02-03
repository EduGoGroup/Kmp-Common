# Test Module - Result Extensions & Domain Patterns

Módulo de utilidades para manejo de errores, conversión DTO/Domain, validación y extensiones funcionales en Kotlin Multiplatform.

## Características

- **Result Extensions**: Operaciones funcionales sobre `Result<T>` (map, flatMap, zip, recover)
- **Combinadores**: zip3-5 para combinar múltiples resultados
- **Extensiones de Colecciones**: sequence, traverse, partition para listas de resultados
- **Patrón DomainMapper**: Conversión bidireccional entre DTOs y modelos de dominio con validación
- **ValidationHelpers**: 20+ funciones de validación reutilizables
- **Validación Acumulativa**: Sistema para colectar todos los errores de validación
- **Serialización Segura**: Extensiones para kotlinx.serialization con manejo de errores
- **AppError Integration**: Errores estructurados con códigos y detalles

## Instalación

```kotlin
// En tu build.gradle.kts
dependencies {
    implementation(project(":test-module"))
}
```

## Uso Rápido

### Result Extensions Básicas

```kotlin
import com.edugo.test.module.extensions.*

// map: Transformar valor exitoso
val result: Result<Int> = Result.Success(10)
val doubled: Result<Int> = result.map { it * 2 }  // Success(20)

// flatMap: Encadenar operaciones que pueden fallar
val validated: Result<User> = loadUserDto()
    .flatMap { dto -> validateUser(dto) }
    .flatMap { user -> saveToDatabase(user) }

// recover: Proporcionar valor por defecto
val config: Config = loadConfig().recover { Config.default() }

// onSuccess/onFailure: Efectos secundarios
result
    .onSuccess { logger.info("Éxito: $it") }
    .onFailure { logger.error("Error: $it") }
```

### Combinación de Resultados

```kotlin
import com.edugo.test.module.extensions.*

// zip: Combinar 2 resultados
val user: Result<User> = name.zip(email) { n, e -> User(n, e) }

// zip3-5: Combinar 3-5 resultados
val profile: Result<UserProfile> = zip3(
    loadBasicInfo(id),
    loadPreferences(id),
    loadStats(id)
) { info, prefs, stats ->
    UserProfile(info, prefs, stats)
}
```

### Manejo de Colecciones

```kotlin
import com.edugo.test.module.extensions.*

// sequence: List<Result<T>> → Result<List<T>> (fail-fast)
val results: List<Result<Int>> = listOf(Success(1), Success(2), Success(3))
val combined: Result<List<Int>> = results.sequence()  // Success([1, 2, 3])

// sequenceCollectingErrors: Acumula todos los errores
val withErrors: List<Result<Int>> = listOf(
    Success(1),
    Failure("Error A"),
    Failure("Error B")
)
val accumulated: Result<List<Int>> = withErrors.sequenceCollectingErrors(separator = "; ")
// Failure("Error A; Error B")

// traverse: Aplicar función que puede fallar
val users: Result<List<User>> = userIds.traverse { loadUser(it) }

// traverseCollectingErrors: Con acumulación de errores
val validated: Result<List<Email>> = emails.traverseCollectingErrors(includeIndex = true) {
    validateEmail(it)
}

// partition: Separar éxitos y fallos
val (successes, failures) = results.partition()
```

### DomainMapper - Conversión DTO/Domain

```kotlin
import com.edugo.test.module.mapper.*
import com.edugo.test.module.validation.*

@Serializable
data class UserDto(
    val username: String,
    val email: String,
    val age: Int
)

data class User(
    val username: String,
    val email: String,
    val age: Int
)

object UserMapper : DomainMapper<UserDto, User> {
    override fun toDomain(dto: UserDto): Result<User> {
        // Validación acumulativa: muestra TODOS los errores
        val errors = accumulateValidationErrors(separator = "\n") {
            add(validateEmail(dto.email))
            add(validateRange(dto.age, 18, 120, "edad"))
            add(validateLengthRange(dto.username, 3, 30, "username"))
        }
        
        if (errors != null) {
            return Result.Failure(errors)
        }
        
        return Result.Success(User(dto.username, dto.email, dto.age))
    }
    
    override fun toDto(domain: User): UserDto {
        return UserDto(domain.username, domain.email, domain.age)
    }
}

// Uso
val dto = UserDto("juan", "juan@example.com", 25)
val user: Result<User> = UserMapper.toDomain(dto)

// Con extensiones
val user2: Result<User> = dto.toDomain(UserMapper)
val dtoBack: UserDto = user.toDto(UserMapper)
```

### Validación

```kotlin
import com.edugo.test.module.validation.*

// Funciones individuales (retornan String? - null si válido)
validateNotBlank("Juan", "nombre")  // null (válido)
validateEmail("invalid")  // "Email inválido: no contiene '@'"
validateRange(25, 18, 120, "edad")  // null (válido)
validatePattern("abc123", "^[a-z]+$", "username", "Solo letras")  // Error

// Validación acumulativa
val errors = accumulateValidationErrors(separator = "\n") {
    add(validateNotBlank(user.name, "nombre"))
    add(validateEmail(user.email))
    add(validateRange(user.age, 18, 120, "edad"))
    add(validateLengthRange(user.password, 8, 50, "contraseña"))
}

if (errors != null) {
    // Muestra: "El campo nombre no puede estar vacío\nEmail inválido\n..."
    println(errors)
}

// Validación condicional
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

### Serialización Segura

```kotlin
import com.edugo.test.module.extensions.*
import kotlinx.serialization.json.Json

// catchSerialization: Convierte excepciones en Result.Failure
val user: Result<User> = catchSerialization {
    Json.decodeFromString<User>(jsonString)
}

// safeDecodeFromString: Extension function
val user2: Result<User> = safeDecodeFromString(jsonString)

// safeEncodeToString: Extension function
val json: Result<String> = user.safeEncodeToString()

// Flujo completo: JSON → DTO → Domain
val domainUser: Result<User> = safeDecodeFromString<UserDto>(jsonString)
    .flatMap { dto -> UserMapper.toDomain(dto) }
    .onSuccess { user -> logger.info("Usuario creado: $user") }
    .onFailure { error -> logger.error("Error: $error") }
```

## Arquitectura

### Estructura del Proyecto

```
test-module/
├── src/
│   ├── commonMain/
│   │   └── kotlin/com/edugo/test/module/
│   │       ├── extensions/
│   │       │   ├── ResultExtensions.kt           # map, flatMap, zip, recover
│   │       │   ├── ResultCombinators.kt          # zip3, zip4, zip5
│   │       │   ├── CollectionResultExtensions.kt # sequence, traverse, partition
│   │       │   └── SerializationExtensions.kt    # catchSerialization, safe*
│   │       ├── mapper/
│   │       │   ├── DomainMapper.kt              # Interface + extensions
│   │       │   └── MapperExtensions.kt          # AppError integration
│   │       ├── validation/
│   │       │   ├── ValidationHelpers.kt         # 20+ validation functions
│   │       │   └── AccumulativeValidation.kt    # Error accumulation
│   │       ├── examples/
│   │       │   └── UserMappingExample.kt        # Ejemplo completo
│   │       └── models/
│   │           ├── Result.kt                    # Success, Failure, Loading
│   │           ├── AppError.kt                  # Errores estructurados
│   │           └── ValidatableModel.kt          # Interface de validación
│   └── commonTest/
│       └── kotlin/com/edugo/test/module/
│           ├── extensions/
│           │   ├── ResultExtensionsTest.kt
│           │   ├── ResultCombinatorsTest.kt
│           │   └── CollectionResultExtensionsTest.kt
│           ├── mapper/
│           │   └── DomainMapperTest.kt
│           ├── validation/
│           │   ├── ValidationHelpersTest.kt
│           │   └── AccumulativeValidationTest.kt
│           └── integration/
│               ├── ConversionIntegrationTest.kt    # Tests end-to-end
│               └── ValidationIntegrationTest.kt    # Tests de validación
└── docs/
    ├── error-handling-guide.md    # Guía completa de manejo de errores
    └── mapper-patterns.md         # Guía de patrones de mappers
```

## Documentación Completa

### Guías

- **[Guía de Manejo de Errores](docs/error-handling-guide.md)**: Patrones y mejores prácticas para `Result<T>`
- **[Guía de Patrones de Mappers](docs/mapper-patterns.md)**: DomainMapper, validación y conversión DTO/Domain
- **[Modelos de Dominio y Serialización](docs/DOMAIN_MODELS.md)**: User, Role, AuthToken con kotlinx-serialization, @SerialName, validaciones y schema evolution

### Conceptos Clave

#### Result<T>

Tipo sellado para representar operaciones que pueden fallar:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val error: String) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}
```

#### DomainMapper<DTO, Domain>

Patrón para conversión bidireccional con validación:

- `toDomain(dto)`: DTO → Domain con validación (retorna `Result<Domain>`)
- `toDto(domain)`: Domain → DTO sin validación (siempre exitoso)

**Filosofía**: DTOs pueden ser inválidos (vienen del exterior), Domain models siempre son válidos (invariantes garantizados).

#### Validación Fail-Fast vs Acumulativa

**Fail-Fast**: Detiene al primer error (útil para operaciones costosas)
```kotlin
validateEmail(email)?.let { return Result.Failure(it) }
validateAge(age)?.let { return Result.Failure(it) }
```

**Acumulativa**: Colecta TODOS los errores (mejor UX para formularios)
```kotlin
val errors = accumulateValidationErrors(separator = "\n") {
    add(validateEmail(email))
    add(validateAge(age))
    add(validateName(name))
}
// Muestra: "Email inválido\nEdad fuera de rango\nNombre muy corto"
```

## Patrones Comunes

### Patrón: JSON → DTO → Domain

```kotlin
fun processUserRegistration(jsonString: String): Result<User> {
    return safeDecodeFromString<UserDto>(jsonString)
        .flatMap { dto -> UserMapper.toDomain(dto) }
        .flatMap { user -> saveToDatabase(user) }
        .onSuccess { user -> sendWelcomeEmail(user) }
        .onFailure { error -> logger.error("Registration failed: $error") }
}
```

### Patrón: Validación de Formulario

```kotlin
fun validateRegistrationForm(form: RegistrationForm): Result<ValidatedForm> {
    val errors = accumulateValidationErrors(separator = "\n") {
        add(validateEmail(form.email))
        add(validateLengthRange(form.password, 8, 50, "contraseña"))
        add(validatePattern(form.username, "^[a-zA-Z0-9_]+$", "username"))
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

### Patrón: Procesamiento por Lotes

```kotlin
fun importUsers(csvFile: String): ImportResult {
    val rows = parseCsv(csvFile)
    
    // Opción 1: Todo o nada (fail-fast)
    val allUsers: Result<List<User>> = rows.traverse { row ->
        parseCsvRow(row).flatMap { dto -> UserMapper.toDomain(dto) }
    }
    
    // Opción 2: Procesa lo que puedas (partial)
    val (validUsers, invalidUsers) = rows
        .map { parseCsvRow(it).flatMap { dto -> UserMapper.toDomain(dto) } }
        .partition()
    
    database.insertAll(validUsers)
    logger.warn("${invalidUsers.size} usuarios inválidos")
}
```

### Patrón: Operaciones Independientes

```kotlin
// Usa zip para operaciones que se pueden ejecutar en paralelo
val dashboard: Result<Dashboard> = zip3(
    loadUserStats(userId),      // Independiente
    loadRecentOrders(userId),   // Independiente
    loadNotifications(userId)   // Independiente
) { stats, orders, notifications ->
    Dashboard(stats, orders, notifications)
}

// No uses flatMap para operaciones independientes
// ❌ MALO: flatMap implica dependencia secuencial
val dashboard = loadUserStats(userId)
    .flatMap { stats ->
        loadRecentOrders(userId).flatMap { orders ->
            loadNotifications(userId).map { notifications ->
                Dashboard(stats, orders, notifications)
            }
        }
    }
```

## Testing

El módulo incluye 300+ tests:

- **Tests Unitarios**: Cada función tiene tests exhaustivos
- **Tests de Integración**: Flujos completos end-to-end
- **Property-Based Tests**: Tests con datos generados

```bash
# Ejecutar todos los tests
./gradlew :test-module:allTests

# Solo tests comunes (multiplataforma)
./gradlew :test-module:cleanAllTests
```

### Cobertura

- `ResultExtensions`: 40+ tests
- `ResultCombinators`: 35+ tests  
- `CollectionResultExtensions`: 60+ tests
- `DomainMapper`: 50+ tests
- `ValidationHelpers`: 80+ tests
- `AccumulativeValidation`: 35+ tests
- **Integration Tests**: 32+ tests de flujos completos

## Mejores Prácticas

### 1. Usa validación acumulativa para formularios

✅ **BUENO**: Muestra todos los errores
```kotlin
val errors = accumulateValidationErrors(separator = "\n") {
    add(validateEmail(dto.email))
    add(validateRange(dto.age, 18, 120, "edad"))
    add(validateNotBlank(dto.name, "nombre"))
}
```

❌ **MALO**: Usuario solo ve el primer error
```kotlin
validateEmail(email)?.let { return Result.Failure(it) }
validateAge(age)?.let { return Result.Failure(it) }
```

### 2. Usa zip para operaciones independientes

✅ **BUENO**: Claridad y potencial paralelismo
```kotlin
val profile = zip3(loadInfo(id), loadPrefs(id), loadStats(id)) { i, p, s ->
    Profile(i, p, s)
}
```

❌ **MALO**: flatMap para operaciones independientes
```kotlin
loadInfo(id).flatMap { i ->
    loadPrefs(id).flatMap { p ->
        loadStats(id).map { s -> Profile(i, p, s) }
    }
}
```

### 3. Usa traverse en lugar de map + sequence

✅ **BUENO**: Más eficiente
```kotlin
val users = ids.traverse { loadUser(it) }
```

❌ **MENOS EFICIENTE**: Crea lista intermedia
```kotlin
val users = ids.map { loadUser(it) }.sequence()
```

### 4. Reutiliza mappers

✅ **BUENO**: Reutiliza AddressMapper
```kotlin
object CompanyMapper : DomainMapper<CompanyDto, Company> {
    override fun toDomain(dto: CompanyDto): Result<Company> {
        return AddressMapper.toDomain(dto.address).map { address ->
            Company(dto.name, address)
        }
    }
}
```

### 5. Haz toDto siempre exitoso

✅ **BUENO**: toDto nunca falla
```kotlin
override fun toDto(domain: User): UserDto {
    return UserDto(domain.username, domain.email, domain.age)
}
```

❌ **MALO**: Validación innecesaria
```kotlin
override fun toDto(domain: User): UserDto {
    if (domain.age < 18) throw IllegalStateException("Nunca debería pasar")
    return UserDto(/* ... */)
}
```

## API Reference

### Result Extensions

- `map<R>(transform: (T) -> R): Result<R>` - Transforma valor exitoso
- `flatMap<R>(transform: (T) -> Result<R>): Result<R>` - Encadena operaciones
- `recover(fallback: (String) -> T): T` - Proporciona valor por defecto
- `onSuccess(action: (T) -> Unit): Result<T>` - Efecto secundario en éxito
- `onFailure(action: (String) -> Unit): Result<T>` - Efecto secundario en fallo

### Combinadores

- `zip<A,B,R>(a: Result<A>, b: Result<B>, transform: (A,B) -> R): Result<R>`
- `zip3<A,B,C,R>(...): Result<R>`
- `zip4<A,B,C,D,R>(...): Result<R>`
- `zip5<A,B,C,D,E,R>(...): Result<R>`

### Colecciones

- `List<Result<T>>.sequence(): Result<List<T>>` - Fail-fast
- `List<Result<T>>.sequenceCollectingErrors(separator): Result<List<T>>` - Acumula errores
- `List<T>.traverse(transform: (T) -> Result<R>): Result<List<R>>` - Fail-fast
- `List<T>.traverseCollectingErrors(separator, includeIndex, transform): Result<List<R>>`
- `List<Result<T>>.partition(): Pair<List<T>, List<String>>` - Separa éxitos/fallos

### Validación

- `validateNotBlank(value, fieldName): String?`
- `validateEmail(value): String?`
- `validateRange(value, min, max, fieldName): String?`
- `validateMinLength(value, min, fieldName): String?`
- `validateMaxLength(value, max, fieldName): String?`
- `validateLengthRange(value, min, max, fieldName): String?`
- `validatePattern(value, regex, fieldName, message): String?`
- `validatePositive(value, fieldName): String?`
- `validateNonNegative(value, fieldName): String?`
- `validateNotEmpty(collection, fieldName): String?`
- `validateMinSize(collection, min, fieldName): String?`
- `validateMaxSize(collection, max, fieldName): String?`
- `validateIn(value, allowed, fieldName): String?`

### Validación Acumulativa

- `accumulateValidationErrors(separator, block): String?`
- `validateIf(condition, validation): String?`
- `validateAtLeastOne(vararg validations): String?`
- `validateAll(vararg validations): String?`

### Serialización

- `catchSerialization<T>(block: () -> T): Result<T>`
- `safeDecodeFromString<T>(jsonString): Result<T>`
- `Result<T>.safeEncodeToString(): Result<String>`

## Contribuir

1. Asegúrate de que todos los tests pasen: `./gradlew :test-module:allTests`
2. Añade tests para nuevas funcionalidades
3. Documenta nuevas funciones con KDoc
4. Actualiza las guías en `/docs` si es relevante

## Licencia

[Tu licencia aquí]

## Autores

[Tus autores aquí]
