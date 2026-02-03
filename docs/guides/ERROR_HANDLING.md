# Guía de Manejo de Errores

Esta guía cubre el sistema completo de manejo de errores en Kmp-Common, incluyendo el tipo `Result<T>`, `AppError`, `ErrorCode`, y patrones de manejo de errores.

## Tabla de Contenidos

- [Introducción](#introducción)
- [Result Monad](#result-monad)
- [AppError](#apperror)
- [ErrorCode](#errorcode)
- [Patrones de Manejo de Errores](#patrones-de-manejo-de-errores)
- [Propagación de Errores](#propagación-de-errores)
- [Recovery y Fallbacks](#recovery-y-fallbacks)
- [Logging y Debugging](#logging-y-debugging)
- [Mejores Prácticas](#mejores-prácticas)

## Introducción

Kmp-Common usa un sistema de manejo de errores basado en tres pilares:

1. **`Result<T>`**: Monad que encapsula éxito/fracaso/carga
2. **`AppError`**: Clase que representa errores con contexto rico
3. **`ErrorCode`**: Enum con códigos de error tipados y categorizados

Este sistema proporciona:
- **Type-safety**: Errores en tiempo de compilación, no runtime
- **Composición**: Encadenar operaciones que pueden fallar
- **Contexto**: Errores con código, mensaje, detalles y causa raíz
- **Recuperación**: Múltiples estrategias de recovery

## Result Monad

`Result<T>` es un tipo algebraico con tres estados:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()
    data class Loading(val progress: Float? = null) : Result<Nothing>()
}
```

### Crear Result

```kotlin
import com.edugo.test.module.core.*

// Success
val exito: Result<String> = success("Operación exitosa")

// Failure
val fallo: Result<String> = failure(
    AppError.system(
        code = ErrorCode.SYSTEM_UNKNOWN_ERROR,
        message = "Algo salió mal"
    )
)

// Loading
val cargando: Result<String> = loading()
val cargandoConProgreso: Result<String> = loading(0.5f) // 50%
```

### Consumir Result

#### onSuccess / onFailure / onLoading

```kotlin
val resultado: Result<Usuario> = obtenerUsuario()

resultado
    .onSuccess { usuario ->
        println("Usuario obtenido: ${usuario.nombre}")
    }
    .onFailure { error ->
        println("Error: ${error.message}")
    }
    .onLoading { progress ->
        println("Cargando: ${progress?.let { "${it * 100}%" } ?: "..."}")
    }
```

#### when Expression

```kotlin
when (resultado) {
    is Result.Success -> println("Éxito: ${resultado.data}")
    is Result.Failure -> println("Error: ${resultado.error.message}")
    is Result.Loading -> println("Cargando...")
}
```

#### getOrNull / getOrElse / getOrThrow

```kotlin
// getOrNull: retorna el valor o null
val usuario: Usuario? = resultado.getOrNull()

// getOrElse: retorna el valor o un default
val usuario: Usuario = resultado.getOrElse { 
    Usuario.anonimo() 
}

// getOrThrow: lanza excepción si falla (usar con cuidado)
val usuario: Usuario = resultado.getOrThrow()
```

## AppError

`AppError` encapsula toda la información de un error.

### Estructura de AppError

```kotlin
data class AppError(
    val code: ErrorCode,
    val message: String,
    val details: String? = null,
    val cause: Throwable? = null,
    val timestamp: Long = currentTimeMillis(),
    val retryable: Boolean = code.retryable
)
```

### Crear AppError

#### Factory Methods

```kotlin
// Error de validación
val error = AppError.validation(
    code = ErrorCode.VALIDATION_INVALID_EMAIL,
    message = "Email inválido"
)

// Error de red
val error = AppError.network(
    code = ErrorCode.NETWORK_TIMEOUT,
    message = "Timeout en la conexión",
    details = "Servidor no respondió en 30s"
)

// Error de sistema
val error = AppError.system(
    code = ErrorCode.SYSTEM_UNKNOWN_ERROR,
    message = "Error inesperado",
    cause = exception
)

// Error de autenticación
val error = AppError.authentication(
    code = ErrorCode.AUTH_INVALID_CREDENTIALS,
    message = "Credenciales inválidas"
)

// Error de autorización
val error = AppError.authorization(
    code = ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS,
    message = "Sin permisos suficientes"
)

// Error de negocio
val error = AppError.business(
    code = ErrorCode.BUSINESS_RULE_VIOLATION,
    message = "No se puede procesar el pedido"
)
```

#### Desde Throwable

```kotlin
try {
    operacionPeligrosa()
} catch (e: Exception) {
    val error = AppError.fromThrowable(
        throwable = e,
        code = ErrorCode.SYSTEM_UNEXPECTED_ERROR
    )
}
```

### Propiedades de AppError

```kotlin
val error = AppError.network(
    code = ErrorCode.NETWORK_TIMEOUT,
    message = "Timeout"
)

// Código del error
println(error.code) // NETWORK_TIMEOUT

// Mensaje
println(error.message) // "Timeout"

// ¿Es reintenTable?
println(error.retryable) // true (network errors son retryable)

// Timestamp
println(error.timestamp) // milisegundos epoch

// Causa raíz (si existe)
error.cause?.printStackTrace()
```

## ErrorCode

`ErrorCode` es un enum que categoriza todos los errores posibles.

### Categorías de ErrorCode

Los códigos están organizados por rangos:

- **1000-1999**: Errores de red
- **2000-2999**: Errores de autenticación/autorización
- **3000-3999**: Errores de validación
- **4000-4999**: Errores de negocio
- **5000-5999**: Errores de sistema
- **6000-6999**: Errores de serialización

### ErrorCodes Comunes

#### Red (1000-1999)

```kotlin
ErrorCode.NETWORK_TIMEOUT           // 1000 - Timeout
ErrorCode.NETWORK_NO_CONNECTION     // 1001 - Sin conexión
ErrorCode.NETWORK_SERVER_ERROR      // 1002 - Error del servidor
ErrorCode.NETWORK_CLIENT_ERROR      // 1003 - Error del cliente
ErrorCode.NETWORK_UNKNOWN_HOST      // 1004 - Host desconocido
```

#### Autenticación/Autorización (2000-2999)

```kotlin
ErrorCode.AUTH_INVALID_CREDENTIALS   // 2000 - Credenciales inválidas
ErrorCode.AUTH_TOKEN_EXPIRED         // 2001 - Token expirado
ErrorCode.AUTH_INVALID_TOKEN         // 2002 - Token inválido
ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS // 2003 - Sin permisos
```

#### Validación (3000-3999)

```kotlin
ErrorCode.VALIDATION_FAILED          // 3000 - Validación general
ErrorCode.VALIDATION_REQUIRED_FIELD  // 3001 - Campo requerido
ErrorCode.VALIDATION_INVALID_FORMAT  // 3002 - Formato inválido
ErrorCode.VALIDATION_OUT_OF_RANGE    // 3003 - Fuera de rango
ErrorCode.VALIDATION_INVALID_EMAIL   // 3004 - Email inválido
ErrorCode.VALIDATION_INVALID_UUID    // 3007 - UUID inválido
ErrorCode.VALIDATION_PASSWORD_MISMATCH // 3008 - Contraseñas no coinciden
```

#### Negocio (4000-4999)

```kotlin
ErrorCode.BUSINESS_RULE_VIOLATION    // 4000 - Violación regla negocio
ErrorCode.BUSINESS_RESOURCE_NOT_FOUND // 4001 - Recurso no encontrado
ErrorCode.BUSINESS_DUPLICATE_ENTRY   // 4002 - Entrada duplicada
ErrorCode.BUSINESS_INSUFFICIENT_FUNDS // 4003 - Fondos insuficientes
```

#### Sistema (5000-5999)

```kotlin
ErrorCode.SYSTEM_UNEXPECTED_ERROR    // 5000 - Error inesperado
ErrorCode.SYSTEM_DATABASE_ERROR      // 5001 - Error de BD
ErrorCode.SYSTEM_FILE_NOT_FOUND      // 5002 - Archivo no encontrado
ErrorCode.SYSTEM_UNKNOWN_ERROR       // 5999 - Error desconocido
```

#### Serialización (6000-6999)

```kotlin
ErrorCode.SYSTEM_SERIALIZATION_ERROR   // 6000 - Error serialización
ErrorCode.SYSTEM_DESERIALIZATION_ERROR // 6001 - Error deserialización
```

### Propiedades de ErrorCode

```kotlin
val code = ErrorCode.NETWORK_TIMEOUT

// Código numérico
println(code.code) // 1000

// Mensaje por defecto
println(code.defaultMessage) // "Network request timed out"

// ¿Es reintenTable?
println(code.retryable) // true
```

## Patrones de Manejo de Errores

### Transformar Éxitos

#### map

Transforma el valor de éxito, preserva el error.

```kotlin
val resultado: Result<Int> = success(42)

val duplicado: Result<Int> = resultado.map { it * 2 }
// Success(84)

val fallo: Result<Int> = failure(AppError.validation(...))
val duplicado2: Result<Int> = fallo.map { it * 2 }
// Sigue siendo Failure (map no se ejecuta)
```

#### flatMap

Encadena operaciones que retornan `Result<T>`.

```kotlin
fun obtenerUsuario(id: Int): Result<Usuario> { ... }
fun obtenerPerfil(usuario: Usuario): Result<Perfil> { ... }
fun obtenerConfiguracion(perfil: Perfil): Result<Config> { ... }

val config: Result<Config> = obtenerUsuario(123)
    .flatMap { usuario -> obtenerPerfil(usuario) }
    .flatMap { perfil -> obtenerConfiguracion(perfil) }

// Si cualquier paso falla, se propaga el error
```

### Transformar Errores

#### mapError

Transforma el error, preserva el éxito.

```kotlin
val resultado: Result<String> = failure(
    AppError.network(
        code = ErrorCode.NETWORK_TIMEOUT,
        message = "Timeout"
    )
)

val mapeado: Result<String> = resultado.mapError { error ->
    // Enriquecer el error
    error.copy(
        message = "Error de conexión: ${error.message}",
        details = "Por favor, verifica tu conexión a internet"
    )
}
```

#### recover

Recupera de un error con un valor por defecto.

```kotlin
val resultado: Result<Usuario> = obtenerUsuario(123)

val conRecuperacion: Result<Usuario> = resultado.recover { error ->
    // Si falla, usar usuario anónimo
    success(Usuario.anonimo())
}

// Ahora siempre es Success
```

#### recoverCatching

Recupera de un error, pero la función de recovery puede fallar.

```kotlin
val resultado: Result<Config> = obtenerConfig()

val conRecuperacion: Result<Config> = resultado.recoverCatching { error ->
    // Intentar cargar config desde cache
    cargarConfigDesdeCache() // Result<Config>
}

// Si el recovery también falla, retorna el error del recovery
```

### Combinar Múltiples Results

#### zip

Combina dos `Result<T>` en uno solo.

```kotlin
val usuario: Result<Usuario> = obtenerUsuario()
val perfil: Result<Perfil> = obtenerPerfil()

val combinado: Result<Pair<Usuario, Perfil>> = usuario.zip(perfil)

combinado.onSuccess { (usuario, perfil) ->
    println("Usuario: ${usuario.nombre}, Perfil: ${perfil.tipo}")
}

// Si cualquiera falla, el resultado es Failure
```

#### combineResults (para listas)

```kotlin
val resultados: List<Result<Int>> = listOf(
    success(1),
    success(2),
    success(3)
)

val combinado: Result<List<Int>> = combineResults(resultados)
// Success(listOf(1, 2, 3))

val conError: List<Result<Int>> = listOf(
    success(1),
    failure(AppError.validation(...)),
    success(3)
)

val combinado2: Result<List<Int>> = combineResults(conError)
// Failure (primer error encontrado)
```

## Propagación de Errores

### Automática con flatMap

```kotlin
fun procesarPedido(pedidoId: Int): Result<Confirmacion> {
    return obtenerPedido(pedidoId)           // Result<Pedido>
        .flatMap { validarInventario(it) }    // Result<Pedido>
        .flatMap { procesarPago(it) }         // Result<Pago>
        .flatMap { generarConfirmacion(it) }  // Result<Confirmacion>
}

// Si cualquier paso falla, se propaga automáticamente
```

### Manual con getOrElse

```kotlin
fun procesarUsuario(id: Int): Result<String> {
    val usuario = obtenerUsuario(id).getOrElse { error ->
        // Manejar error y retornar early
        return failure(error)
    }
    
    val perfil = obtenerPerfil(usuario).getOrElse { error ->
        return failure(error)
    }
    
    return success("Usuario procesado: ${usuario.nombre}")
}
```

### Acumulación de Errores

Para casos donde quieres recolectar múltiples errores en lugar de fallar en el primero:

```kotlin
fun validarFormulario(form: FormDTO): Result<FormDTO> {
    val errores = mutableListOf<AppError>()
    
    validateEmail(form.email)?.let { errores.add(it) }
    validateRange(form.edad, 18, 100)?.let { errores.add(it) }
    validateMinLength(form.nombre, 2)?.let { errores.add(it) }
    
    return if (errores.isEmpty()) {
        success(form)
    } else {
        failure(
            AppError.validation(
                code = ErrorCode.VALIDATION_FAILED,
                message = "Errores de validación: ${errores.size}",
                details = errores.joinToString("\n") { it.message }
            )
        )
    }
}
```

## Recovery y Fallbacks

### Valores por Defecto

```kotlin
// Con getOrElse
val usuario: Usuario = obtenerUsuario(id).getOrElse {
    Usuario.anonimo()
}

// Con recover
val usuarioResult: Result<Usuario> = obtenerUsuario(id).recover {
    success(Usuario.anonimo())
}
```

### Fallback a Otra Fuente

```kotlin
fun obtenerConfig(): Result<Config> {
    return cargarConfigRemota()
        .recoverCatching { 
            cargarConfigLocal()
        }
        .recoverCatching {
            cargarConfigPorDefecto()
        }
}

// Intenta: remota -> local -> default
```

### Retry con Delay

```kotlin
suspend fun obtenerDatosConRetry(
    maxRetries: Int = 3
): Result<Data> {
    var lastError: AppError? = null
    
    repeat(maxRetries) { intento ->
        val resultado = obtenerDatos()
        
        if (resultado is Result.Success) {
            return resultado
        }
        
        if (resultado is Result.Failure) {
            lastError = resultado.error
            
            if (!resultado.error.retryable) {
                // Error no reintenTable, fallar inmediatamente
                return resultado
            }
            
            // Esperar antes del siguiente intento
            delay(1000L * (intento + 1))
        }
    }
    
    return failure(lastError ?: AppError.system(
        code = ErrorCode.SYSTEM_UNKNOWN_ERROR,
        message = "Falló después de $maxRetries intentos"
    ))
}
```

### Circuit Breaker Pattern

```kotlin
class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val resetTimeout: Long = 60000 // 1 minuto
) {
    private var failureCount = 0
    private var lastFailureTime = 0L
    private var isOpen = false
    
    fun <T> execute(operation: () -> Result<T>): Result<T> {
        // Si el circuit está abierto
        if (isOpen) {
            // Verificar si es tiempo de intentar de nuevo
            if (System.currentTimeMillis() - lastFailureTime > resetTimeout) {
                isOpen = false
                failureCount = 0
            } else {
                return failure(AppError.system(
                    code = ErrorCode.SYSTEM_CIRCUIT_BREAKER_OPEN,
                    message = "Servicio temporalmente no disponible"
                ))
            }
        }
        
        val resultado = operation()
        
        if (resultado is Result.Failure) {
            failureCount++
            lastFailureTime = System.currentTimeMillis()
            
            if (failureCount >= failureThreshold) {
                isOpen = true
            }
        } else {
            failureCount = 0
        }
        
        return resultado
    }
}

// Uso
val circuitBreaker = CircuitBreaker()

val resultado = circuitBreaker.execute {
    llamadaExterna()
}
```

## Logging y Debugging

### Logging de Errores

```kotlin
fun <T> Result<T>.logError(tag: String = "Result"): Result<T> {
    if (this is Result.Failure) {
        println("[$tag] Error: ${error.code} - ${error.message}")
        error.details?.let { println("[$tag] Detalles: $it") }
        error.cause?.printStackTrace()
    }
    return this
}

// Uso
val resultado = obtenerUsuario(123)
    .logError("ObtenerUsuario")
    .map { usuario -> usuario.nombre }
```

### Debugging con peek

```kotlin
fun <T> Result<T>.peek(block: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        block(data)
    }
    return this
}

// Uso
val resultado = obtenerUsuario(123)
    .peek { usuario -> println("DEBUG: Usuario obtenido: $usuario") }
    .flatMap { procesarUsuario(it) }
```

### Métricas de Errores

```kotlin
object ErrorMetrics {
    private val errorCounts = mutableMapOf<ErrorCode, Int>()
    
    fun track(error: AppError) {
        errorCounts[error.code] = (errorCounts[error.code] ?: 0) + 1
    }
    
    fun getStats(): Map<ErrorCode, Int> = errorCounts.toMap()
    
    fun reset() {
        errorCounts.clear()
    }
}

fun <T> Result<T>.trackError(): Result<T> {
    if (this is Result.Failure) {
        ErrorMetrics.track(error)
    }
    return this
}

// Uso
val resultado = obtenerUsuario(123)
    .trackError()
    .flatMap { procesarUsuario(it) }
```

## Mejores Prácticas

### ✅ DO - Buenas Prácticas

#### 1. Usa ErrorCodes específicos

```kotlin
// ✅ BIEN: ErrorCode específico con contexto
return failure(AppError.validation(
    code = ErrorCode.VALIDATION_INVALID_EMAIL,
    message = "Email '$email' no es válido"
))

// ❌ MAL: ErrorCode genérico sin contexto
return failure(AppError.validation(
    code = ErrorCode.VALIDATION_FAILED,
    message = "Error"
))
```

#### 2. Proporciona mensajes descriptivos

```kotlin
// ✅ BIEN: Mensaje claro y accionable
return failure(AppError.network(
    code = ErrorCode.NETWORK_TIMEOUT,
    message = "No se pudo conectar al servidor en 30 segundos",
    details = "Verifica tu conexión a internet e intenta nuevamente"
))

// ❌ MAL: Mensaje vago
return failure(AppError.network(
    code = ErrorCode.NETWORK_TIMEOUT,
    message = "Error de red"
))
```

#### 3. Preserva la causa raíz

```kotlin
// ✅ BIEN: Preserva la excepción original
try {
    operacionPeligrosa()
} catch (e: Exception) {
    return failure(AppError.fromThrowable(
        throwable = e,
        code = ErrorCode.SYSTEM_UNEXPECTED_ERROR
    ))
}

// ❌ MAL: Pierde información de la causa
try {
    operacionPeligrosa()
} catch (e: Exception) {
    return failure(AppError.system(
        code = ErrorCode.SYSTEM_UNEXPECTED_ERROR,
        message = "Error"
    ))
}
```

#### 4. Usa flatMap para encadenar operaciones

```kotlin
// ✅ BIEN: Encadenamiento fluido
fun procesarPedido(id: Int): Result<Confirmacion> {
    return obtenerPedido(id)
        .flatMap { validarPedido(it) }
        .flatMap { procesarPago(it) }
        .map { generarConfirmacion(it) }
}

// ❌ MAL: Desempaquetado manual
fun procesarPedido(id: Int): Result<Confirmacion> {
    val pedido = when (val r = obtenerPedido(id)) {
        is Result.Success -> r.data
        is Result.Failure -> return failure(r.error)
        else -> return loading()
    }
    
    val validado = when (val r = validarPedido(pedido)) {
        is Result.Success -> r.data
        is Result.Failure -> return failure(r.error)
        else -> return loading()
    }
    
    // ... más código repetitivo
}
```

#### 5. Maneja errores en el nivel apropiado

```kotlin
// ✅ BIEN: Manejo en el nivel correcto (UI/Controller)
class UsuarioViewModel {
    fun cargarUsuario(id: Int) {
        obtenerUsuario(id)
            .onSuccess { usuario ->
                _usuarioState.value = usuario
            }
            .onFailure { error ->
                _errorState.value = error.message
            }
    }
}

// ❌ MAL: Manejo prematuro en capa de datos
class UsuarioRepository {
    fun obtener(id: Int): Usuario? {
        return api.obtenerUsuario(id).onFailure { error ->
            // No manejar aquí, propagar hacia arriba
            showToast(error.message)
        }.getOrNull()
    }
}
```

### ❌ DON'T - Anti-Patrones

#### 1. No uses excepciones para control de flujo

```kotlin
// ❌ MAL: Excepciones para flujo
fun obtenerUsuario(id: Int): Usuario {
    if (id <= 0) throw IllegalArgumentException("ID inválido")
    return repository.find(id) ?: throw NotFoundException()
}

// ✅ BIEN: Result para errores esperados
fun obtenerUsuario(id: Int): Result<Usuario> {
    if (id <= 0) {
        return failure(AppError.validation(
            code = ErrorCode.VALIDATION_FAILED,
            message = "ID debe ser mayor a 0"
        ))
    }
    
    val usuario = repository.find(id)
    return usuario?.let { success(it) }
        ?: failure(AppError.business(
            code = ErrorCode.BUSINESS_RESOURCE_NOT_FOUND,
            message = "Usuario no encontrado"
        ))
}
```

#### 2. No ignores errores silenciosamente

```kotlin
// ❌ MAL: Ignora el error
val usuario = obtenerUsuario(id).getOrNull()
// Si falla, usuario es null y no hay indicación de qué pasó

// ✅ BIEN: Maneja o propaga el error
val usuario = obtenerUsuario(id)
    .onFailure { error ->
        log.error("Error obteniendo usuario: ${error.message}")
    }
    .getOrElse { Usuario.anonimo() }
```

#### 3. No crees ErrorCodes duplicados

```kotlin
// ❌ MAL: Crear error custom innecesario
val CUSTOM_EMAIL_ERROR = ErrorCode(9999, "Email error", false)

// ✅ BIEN: Usa ErrorCodes existentes
ErrorCode.VALIDATION_INVALID_EMAIL
```

#### 4. No mezcles excepciones y Result inconsistentemente

```kotlin
// ❌ MAL: Mezcla excepciones y Result
fun obtenerDatos(): Result<Data> {
    if (condicion) {
        throw IllegalStateException() // Excepción
    }
    return success(data) // Result
}

// ✅ BIEN: Consistencia con Result
fun obtenerDatos(): Result<Data> {
    if (condicion) {
        return failure(AppError.validation(...))
    }
    return success(data)
}
```

#### 5. No uses getOrThrow en código de producción

```kotlin
// ❌ MAL: getOrThrow puede crashear la app
val usuario = obtenerUsuario(id).getOrThrow()

// ✅ BIEN: Manejo explícito
val usuario = obtenerUsuario(id)
    .onFailure { error ->
        // Manejar error apropiadamente
        mostrarError(error)
        return
    }
    .getOrElse { Usuario.anonimo() }
```

## Resumen de APIs

| API | Propósito | Ejemplo |
|-----|-----------|---------|
| `success(T)` | Crear éxito | `success("ok")` |
| `failure(AppError)` | Crear fallo | `failure(error)` |
| `loading(Float?)` | Crear loading | `loading(0.5f)` |
| `map` | Transformar éxito | `.map { it * 2 }` |
| `flatMap` | Encadenar ops | `.flatMap { process(it) }` |
| `mapError` | Transformar error | `.mapError { enrich(it) }` |
| `recover` | Fallback | `.recover { success(default) }` |
| `getOrNull` | Valor o null | `result.getOrNull()` |
| `getOrElse` | Valor o default | `result.getOrElse { default }` |
| `onSuccess` | Side-effect | `.onSuccess { print(it) }` |
| `onFailure` | Side-effect | `.onFailure { log(it) }` |

## Conclusión

El sistema de manejo de errores de Kmp-Common proporciona:
- **Type-Safety**: Errores explícitos en la firma de funciones
- **Composición**: Encadenar operaciones que pueden fallar
- **Contexto Rico**: Errores con código, mensaje, detalles y causa
- **Recuperación**: Múltiples estrategias de fallback
- **Trazabilidad**: ErrorCodes categorizados para métricas y debugging

Usa `Result<T>` consistentemente en tu API y maneja errores explícitamente en el nivel apropiado de tu aplicación.
