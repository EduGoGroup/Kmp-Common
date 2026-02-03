# Guía de Validación

Esta guía cubre el sistema completo de validación en Kmp-Common, incluyendo helpers, extension functions, y patrones de validación fail-fast y acumulativa.

## Tabla de Contenidos

- [Introducción](#introducción)
- [Funciones de Validación Base](#funciones-de-validación-base)
- [Extension Functions](#extension-functions)
- [Patrones de Validación](#patrones-de-validación)
- [Validación en Modelos de Dominio](#validación-en-modelos-de-dominio)
- [Validación Multi-Campo](#validación-multi-campo)
- [Integración con Result](#integración-con-result)
- [Mejores Prácticas](#mejores-prácticas)

## Introducción

Kmp-Common proporciona un sistema de validación completo con tres niveles de API:

1. **Funciones Base** (`validateEmail`, `validateRange`, etc.) - Retornan `AppError?`
2. **Extension Functions Boolean** (`isValidEmail()`, `isInRange()`, etc.) - Retornan `Boolean`
3. **Extension Functions Result** (`validateEmail()`, `validateUUID()`, etc.) - Retornan `Result<T>`

Además, ofrece dos patrones de validación:
- **Fail-Fast**: Se detiene en el primer error
- **Acumulativa**: Recolecta todos los errores

## Funciones de Validación Base

Las funciones base están en `ValidationHelpers.kt` y retornan `AppError?` (null si es válido).

### Validación de Email

```kotlin
import com.edugo.test.module.validators.validateEmail

val email = "usuario@ejemplo.com"
val error = validateEmail(email)

if (error != null) {
    println("Email inválido: ${error.message}")
} else {
    println("Email válido")
}
```

**Cuándo usar:**
- Cuando necesitas el objeto `AppError` completo con código y detalles
- En lógica de validación personalizada donde null significa éxito
- Cuando estás construyendo tu propia capa de manejo de errores

### Validación de Rangos

```kotlin
import com.edugo.test.module.validators.validateRange

val edad = 25
val error = validateRange(edad, 18, 65)

if (error != null) {
    println("Edad fuera de rango: ${error.message}")
}
```

**Validadores disponibles:**
- `validateEmail(email: String): AppError?`
- `validateNotEmpty(value: String): AppError?`
- `validateMinLength(value: String, minLength: Int): AppError?`
- `validateMaxLength(value: String, maxLength: Int): AppError?`
- `validateRange(value: T, min: T, max: T): AppError?` donde `T : Comparable<T>`
- `validatePattern(value: String, regex: Regex, errorMessage: String): AppError?`
- `validateUrl(url: String): AppError?`
- `validateNumeric(value: String): AppError?`
- `validateAlphanumeric(value: String): AppError?`

## Extension Functions

Las extension functions proporcionan una API más fluida y ergonómica.

### Extension Functions Boolean

Retornan `true` si el valor es válido, `false` si no lo es.

```kotlin
val email = "usuario@ejemplo.com"

if (email.isValidEmail()) {
    println("Email válido")
} else {
    println("Email inválido")
}
```

**Cuándo usar:**
- En condicionales simples (`if`, `when`)
- En filtros de colecciones
- Cuando solo necesitas saber válido/inválido sin detalles del error

**Extension functions Boolean disponibles:**
```kotlin
String.isValidEmail(): Boolean
String.isValidUUID(): Boolean
Comparable<T>.isInRange(min: T, max: T): Boolean
String.matchesPassword(confirmation: String): Boolean
```

### Extension Functions Result

Retornan `Result.Success<T>` si es válido o `Result.Failure` con el error.

```kotlin
val email = "usuario@ejemplo.com"

val result = email.validateEmail()

result.onSuccess { validEmail ->
    println("Email procesado: $validEmail")
}.onFailure { error ->
    println("Error: ${error.message}")
}
```

**Cuándo usar:**
- Cuando necesitas encadenar validaciones con `flatMap`
- En flujos de trabajo que usan `Result<T>` consistentemente
- Cuando quieres propagar errores automáticamente
- En APIs que retornan `Result<T>`

**Extension functions Result disponibles:**
```kotlin
String.validateEmail(): Result<String>
String.validateUUID(): Result<String>
String.validatePasswordMatch(confirmation: String): Result<Unit>
```

## Patrones de Validación

### Fail-Fast (Por Defecto)

Se detiene en el primer error encontrado.

```kotlin
import com.edugo.test.module.validators.*
import com.edugo.test.module.core.*

data class Usuario(
    val email: String,
    val edad: Int,
    val nombre: String
)

fun validarUsuarioFailFast(usuario: Usuario): Result<Usuario> {
    return usuario.email
        .validateEmail()
        .flatMap { 
            validateRange(usuario.edad, 18, 100)
                ?.let { failure(it) }
                ?: success(usuario.edad)
        }
        .flatMap {
            validateMinLength(usuario.nombre, 2)
                ?.let { failure(it) }
                ?: success(usuario)
        }
}

// Uso
val usuario = Usuario("invalido", 15, "J")
val resultado = validarUsuarioFailFast(usuario)

// Solo obtendrás el primer error (email inválido)
resultado.onFailure { error ->
    println("Error: ${error.message}") // "Invalid email format"
}
```

**Cuándo usar:**
- Validación en formularios donde quieres mostrar un error a la vez
- Cuando la primera validación invalida hace innecesarias las demás
- Para optimizar performance (no ejecuta validaciones innecesarias)

### Validación Acumulativa

Recolecta todos los errores antes de retornar.

```kotlin
import com.edugo.test.module.validators.*

data class Usuario(
    val email: String,
    val edad: Int,
    val nombre: String
)

fun validarUsuarioAcumulativo(usuario: Usuario): AccumulativeValidation {
    return AccumulativeValidation().apply {
        addError(validateEmail(usuario.email))
        addError(validateRange(usuario.edad, 18, 100))
        addError(validateMinLength(usuario.nombre, 2))
    }
}

// Uso
val usuario = Usuario("invalido", 15, "J")
val validacion = validarUsuarioAcumulativo(usuario)

if (validacion.hasErrors()) {
    val errores = validacion.getErrors()
    println("Se encontraron ${errores.size} errores:")
    errores.forEach { error ->
        println("  - ${error.message}")
    }
    // Output:
    // Se encontraron 3 errores:
    //   - Invalid email format
    //   - Value 15 is out of range [18, 100]
    //   - Value must be at least 2 characters long
} else {
    println("Usuario válido")
}
```

**Cuándo usar:**
- Validación de formularios donde quieres mostrar todos los errores a la vez
- En APIs que retornan múltiples errores de validación
- Cuando el usuario necesita corregir múltiples campos
- Para mejorar UX mostrando todos los problemas de una vez

### Convertir Validación Acumulativa a Result

```kotlin
fun validarUsuarioResult(usuario: Usuario): Result<Usuario> {
    val validacion = AccumulativeValidation().apply {
        addError(validateEmail(usuario.email))
        addError(validateRange(usuario.edad, 18, 100))
        addError(validateMinLength(usuario.nombre, 2))
    }
    
    return validacion.toResult(usuario)
}

// Uso
val resultado = validarUsuarioResult(usuario)
resultado.onSuccess { usuarioValido ->
    println("Usuario válido: $usuarioValido")
}.onFailure { error ->
    // El error contiene todos los errores concatenados
    println("Errores: ${error.message}")
}
```

## Validación en Modelos de Dominio

La interfaz `ValidatableModel` permite que los modelos se auto-validen.

### Implementación Básica

```kotlin
import com.edugo.test.module.validators.*

data class Usuario(
    val email: String,
    val edad: Int,
    val nombre: String,
    val password: String,
    val passwordConfirm: String
) : ValidatableModel {
    override fun validate(): AppError? {
        // Fail-fast: retorna el primer error encontrado
        validateEmail(email)?.let { return it }
        validateRange(edad, 18, 100)?.let { return it }
        validateMinLength(nombre, 2)?.let { return it }
        validateMinLength(password, 8)?.let { return it }
        
        if (!password.matchesPassword(passwordConfirm)) {
            return AppError.validation(
                code = ErrorCode.VALIDATION_PASSWORD_MISMATCH,
                message = "Las contraseñas no coinciden"
            )
        }
        
        return null // null = válido
    }
}

// Uso
val usuario = Usuario(
    email = "usuario@ejemplo.com",
    edad = 25,
    nombre = "Juan",
    password = "segura123",
    passwordConfirm = "segura123"
)

val error = usuario.validate()
if (error != null) {
    println("Usuario inválido: ${error.message}")
} else {
    println("Usuario válido")
}
```

### Con Validación Acumulativa

```kotlin
data class Usuario(
    val email: String,
    val edad: Int,
    val nombre: String
) : ValidatableModel {
    override fun validate(): AppError? {
        val validacion = AccumulativeValidation().apply {
            addError(validateEmail(email))
            addError(validateRange(edad, 18, 100))
            addError(validateMinLength(nombre, 2))
        }
        
        return validacion.toSingleError()
    }
    
    fun getAllErrors(): List<AppError> {
        val validacion = AccumulativeValidation().apply {
            addError(validateEmail(email))
            addError(validateRange(edad, 18, 100))
            addError(validateMinLength(nombre, 2))
        }
        return validacion.getErrors()
    }
}

// Uso
val usuario = Usuario("invalido", 15, "J")

// Obtener primer error
val error = usuario.validate()

// O todos los errores
val todosLosErrores = usuario.getAllErrors()
println("Total de errores: ${todosLosErrores.size}")
```

## Validación Multi-Campo

### Validación Cruzada de Campos

```kotlin
data class RangoFecha(
    val fechaInicio: String,
    val fechaFin: String
) : ValidatableModel {
    override fun validate(): AppError? {
        // Validar formato individual
        validateNotEmpty(fechaInicio)?.let { return it }
        validateNotEmpty(fechaFin)?.let { return it }
        
        // Validación cruzada
        if (fechaInicio > fechaFin) {
            return AppError.validation(
                code = ErrorCode.VALIDATION_FAILED,
                message = "La fecha de inicio debe ser anterior a la fecha fin"
            )
        }
        
        return null
    }
}
```

### Validación Condicional

```kotlin
data class Pedido(
    val tipo: String,
    val direccionEnvio: String?,
    val email: String?
) : ValidatableModel {
    override fun validate(): AppError? {
        // Validación base
        validateNotEmpty(tipo)?.let { return it }
        
        // Validación condicional según el tipo
        when (tipo) {
            "envio" -> {
                if (direccionEnvio.isNullOrBlank()) {
                    return AppError.validation(
                        code = ErrorCode.VALIDATION_REQUIRED_FIELD,
                        message = "Dirección de envío requerida para pedidos con envío"
                    )
                }
            }
            "digital" -> {
                if (email.isNullOrBlank()) {
                    return AppError.validation(
                        code = ErrorCode.VALIDATION_REQUIRED_FIELD,
                        message = "Email requerido para pedidos digitales"
                    )
                }
                validateEmail(email)?.let { return it }
            }
        }
        
        return null
    }
}
```

### Validación de Colecciones

```kotlin
data class Formulario(
    val respuestas: List<String>
) : ValidatableModel {
    override fun validate(): AppError? {
        if (respuestas.isEmpty()) {
            return AppError.validation(
                code = ErrorCode.VALIDATION_FAILED,
                message = "Debe proporcionar al menos una respuesta"
            )
        }
        
        // Validar cada respuesta
        respuestas.forEachIndexed { index, respuesta ->
            validateMinLength(respuesta, 1)?.let { error ->
                return AppError.validation(
                    code = error.code,
                    message = "Respuesta ${index + 1}: ${error.message}"
                )
            }
        }
        
        return null
    }
}
```

## Integración con Result

### Encadenar Validaciones

```kotlin
import com.edugo.test.module.core.*
import com.edugo.test.module.validators.*

fun procesarRegistro(
    email: String,
    password: String,
    passwordConfirm: String
): Result<String> {
    return email
        .validateEmail()
        .flatMap { validEmail ->
            password.validatePasswordMatch(passwordConfirm)
                .map { validEmail }
        }
        .flatMap { validEmail ->
            // Aquí continuarías con la lógica de negocio
            success("Usuario registrado: $validEmail")
        }
}

// Uso
val resultado = procesarRegistro(
    email = "usuario@ejemplo.com",
    password = "segura123",
    passwordConfirm = "segura123"
)

resultado.onSuccess { mensaje ->
    println(mensaje)
}.onFailure { error ->
    println("Error en registro: ${error.message}")
}
```

### Validar y Transformar

```kotlin
fun procesarEmailNormalizado(email: String): Result<String> {
    return email
        .validateEmail()
        .map { it.lowercase().trim() }
}

// Uso
val emailProcesado = procesarEmailNormalizado("  Usuario@Ejemplo.COM  ")
emailProcesado.onSuccess { email ->
    println("Email normalizado: $email") // "usuario@ejemplo.com"
}
```

### Validación con Recuperación

```kotlin
fun procesarEdadConDefault(edad: Int): Result<Int> {
    return validateRange(edad, 18, 100)
        ?.let { failure<Int>(it) }
        ?: success(edad)
}

fun obtenerEdadODefecto(edad: Int): Int {
    return procesarEdadConDefault(edad)
        .getOrElse { 18 } // Valor por defecto si falla
}
```

## Mejores Prácticas

### ✅ DO - Buenas Prácticas

#### 1. Usa la API correcta para tu caso de uso

```kotlin
// Boolean para condicionales simples
if (email.isValidEmail()) {
    enviarCorreo(email)
}

// Result para encadenar operaciones
email.validateEmail()
    .flatMap { procesarUsuario(it) }
    .flatMap { guardarEnDB(it) }

// Funciones base cuando necesitas el AppError completo
val error = validateEmail(email)
if (error != null) {
    logError(error.code, error.message)
}
```

#### 2. Valida en el borde del sistema

```kotlin
// En el controller/endpoint
fun crearUsuario(request: CrearUsuarioRequest): Result<Usuario> {
    // Validar ANTES de pasar a la capa de negocio
    return request.email
        .validateEmail()
        .flatMap { servicioUsuarios.crear(request) }
}
```

#### 3. Usa ValidatableModel en entidades de dominio

```kotlin
data class Usuario(...) : ValidatableModel {
    override fun validate(): AppError? {
        // Validación centralizada en el modelo
        validateEmail(email)?.let { return it }
        return null
    }
}

// Valida antes de persistir
fun guardar(usuario: Usuario): Result<Usuario> {
    usuario.validate()?.let { return failure(it) }
    return repositorio.guardar(usuario)
}
```

#### 4. Usa validación acumulativa para formularios

```kotlin
// Mejor UX: muestra todos los errores a la vez
fun validarFormulario(form: FormularioDTO): AccumulativeValidation {
    return AccumulativeValidation().apply {
        addError(validateEmail(form.email))
        addError(validateRange(form.edad, 18, 100))
        addError(validateMinLength(form.nombre, 2))
    }
}
```

#### 5. Combina validaciones con lógica de negocio

```kotlin
fun registrarUsuario(email: String, password: String): Result<Usuario> {
    return email
        .validateEmail()
        .flatMap { validEmail ->
            // Validación de negocio
            if (repositorio.existeEmail(validEmail)) {
                failure(AppError.validation(
                    ErrorCode.VALIDATION_FAILED,
                    "Email ya registrado"
                ))
            } else {
                success(Usuario(validEmail, password))
            }
        }
}
```

### ❌ DON'T - Anti-Patrones

#### 1. No mezcles patrones de validación inconsistentemente

```kotlin
// ❌ MAL: Mezcla Boolean y Result sin coherencia
fun validar(email: String, edad: Int): Result<Unit> {
    if (!email.isValidEmail()) { // Boolean
        return failure(AppError.validation(...))
    }
    return validateRange(edad, 18, 100) // AppError?
        ?.let { failure(it) }
        ?: success(Unit)
}

// ✅ BIEN: Usa un patrón consistente
fun validar(email: String, edad: Int): Result<Unit> {
    return email
        .validateEmail()
        .flatMap { 
            validateRange(edad, 18, 100)
                ?.let { failure(it) }
                ?: success(Unit)
        }
}
```

#### 2. No valides demasiado tarde

```kotlin
// ❌ MAL: Validar después de procesar
fun procesarUsuario(email: String): Result<Usuario> {
    val usuario = Usuario(email) // Ya creado sin validar
    return email.validateEmail().map { usuario }
}

// ✅ BIEN: Validar antes de crear
fun procesarUsuario(email: String): Result<Usuario> {
    return email.validateEmail()
        .map { validEmail -> Usuario(validEmail) }
}
```

#### 3. No ignores errores de validación

```kotlin
// ❌ MAL: Ignorar el error
fun procesarEmail(email: String) {
    if (email.isValidEmail()) {
        enviar(email)
    }
    // Si no es válido, no hace nada (silencioso)
}

// ✅ BIEN: Manejar ambos casos
fun procesarEmail(email: String): Result<Unit> {
    return email.validateEmail()
        .flatMap { enviar(it) }
        .onFailure { error -> 
            log.error("Email inválido: ${error.message}")
        }
}
```

#### 4. No reimplementes validadores existentes

```kotlin
// ❌ MAL: Reimplementar validación
fun esEmailValido(email: String): Boolean {
    val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return email.matches(regex)
}

// ✅ BIEN: Usa los validadores existentes
fun esEmailValido(email: String): Boolean {
    return email.isValidEmail()
}
```

#### 5. No uses fail-fast cuando necesitas todos los errores

```kotlin
// ❌ MAL: Fail-fast en formulario (solo muestra 1 error)
fun validarFormulario(form: FormDTO): Result<FormDTO> {
    return form.email.validateEmail()
        .flatMap { form.edad.validateRange(18, 100) }
        .flatMap { form.nombre.validateMinLength(2) }
        .map { form }
}

// ✅ BIEN: Acumulativa para formularios
fun validarFormulario(form: FormDTO): AccumulativeValidation {
    return AccumulativeValidation().apply {
        addError(validateEmail(form.email))
        addError(validateRange(form.edad, 18, 100))
        addError(validateMinLength(form.nombre, 2))
    }
}
```

## Resumen de APIs

| API | Retorna | Caso de Uso |
|-----|---------|-------------|
| `validateEmail()` | `AppError?` | Necesitas el error completo con código |
| `isValidEmail()` | `Boolean` | Condicionales simples, filtros |
| `validateEmail()` | `Result<String>` | Encadenamiento con flatMap, APIs Result |
| `AccumulativeValidation` | `List<AppError>` | Formularios, múltiples errores |
| `ValidatableModel` | `AppError?` | Auto-validación en modelos de dominio |

## Conclusión

El sistema de validación de Kmp-Common proporciona:
- **Flexibilidad**: Tres niveles de API para diferentes casos de uso
- **Composición**: Se integra perfectamente con `Result<T>`
- **Claridad**: Patrones fail-fast y acumulativo según necesites
- **Type-Safety**: Validación en tiempo de compilación con tipos fuertes

Elige la API que mejor se adapte a tu caso de uso y mantén la consistencia en tu codebase.
