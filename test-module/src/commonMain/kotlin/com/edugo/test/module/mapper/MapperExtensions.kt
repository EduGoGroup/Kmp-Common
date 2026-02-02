package com.edugo.test.module.mapper

import com.edugo.test.module.core.AppError
import com.edugo.test.module.core.ErrorCode
import com.edugo.test.module.core.Result
import com.edugo.test.module.core.flatMap

/**
 * Extensiones para integrar DomainMapper con AppError y Result.
 *
 * Estas extensiones facilitan la conversión entre DTOs y modelos de dominio
 * con manejo de errores estructurado usando AppError y ErrorCode.
 *
 * ## Características
 *
 * - **Integración con AppError**: Convierte errores de validación a AppError
 * - **ErrorCode Type-Safe**: Usa códigos de error tipados
 * - **Metadata Enriquecida**: Agrega contexto adicional a los errores
 * - **Composable**: Se puede encadenar con otras operaciones Result
 *
 * ## Uso Básico
 *
 * ```kotlin
 * val dto = UserDto(email = "invalid", age = 15)
 * val result = dto.toDomainWithAppError(UserMapper)
 *
 * when (result) {
 *     is Result.Success -> println("User: ${result.data}")
 *     is Result.Failure -> {
 *         val appError = result.toAppError()
 *         println("Error: ${appError.code} - ${appError.message}")
 *     }
 * }
 * ```
 */

/**
 * Convierte un DTO a dominio y envuelve errores en AppError.
 *
 * Si la conversión falla, convierte el error de validación en un AppError
 * con código VALIDATION_ERROR y metadata adicional.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val dto = UserDto(email = "", age = 25)
 * val result = dto.toDomainWithAppError(
 *     mapper = UserMapper,
 *     errorCode = ErrorCode.VALIDATION_INVALID_EMAIL,
 *     details = mapOf("field" to "email", "value" to dto.email)
 * )
 * ```
 *
 * @param mapper Mapper a usar para la conversión
 * @param errorCode Código de error a usar si la validación falla (default: VALIDATION_ERROR)
 * @param details Metadata adicional a incluir en el AppError
 * @return Result con el dominio o un Failure con mensaje de AppError
 */
fun <DTO, Domain> DTO.toDomainWithAppError(
    mapper: DomainMapper<DTO, Domain>,
    errorCode: ErrorCode = ErrorCode.VALIDATION_INVALID_INPUT,
    details: Map<String, String> = emptyMap()
): Result<Domain> {
    return when (val result = mapper.toDomain(this)) {
        is Result.Success -> result
        is Result.Failure -> {
            val appError = AppError(
                code = errorCode,
                message = result.error,
                details = details,
                cause = null
            )
            // Generar mensaje detallado manualmente
            val detailedMessage = buildString {
                append("[${appError.code.name}] ${appError.message}")
                if (appError.details.isNotEmpty()) {
                    append(" (")
                    append(appError.details.entries.joinToString(", ") { "${it.key}=${it.value}" })
                    append(")")
                }
            }
            Result.Failure(detailedMessage)
        }
        is Result.Loading -> Result.Loading
    }
}

/**
 * Convierte una lista de DTOs a dominios con manejo de AppError.
 *
 * Si alguna conversión falla, retorna el primer error como AppError.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val dtos = listOf(UserDto(...), UserDto(...))
 * val result = dtos.toDomainListWithAppError(UserMapper)
 * ```
 *
 * @param mapper Mapper a usar para cada conversión
 * @param errorCode Código de error base (se enriquece con índice del item)
 * @param includeIndex Si true, agrega el índice del item que falló a los details
 * @return Result con lista de dominios o Failure con AppError
 */
fun <DTO, Domain> List<DTO>.toDomainListWithAppError(
    mapper: DomainMapper<DTO, Domain>,
    errorCode: ErrorCode = ErrorCode.VALIDATION_INVALID_INPUT,
    includeIndex: Boolean = true
): Result<List<Domain>> {
    val domains = mutableListOf<Domain>()
    forEachIndexed { index, dto ->
        val details = if (includeIndex) {
            mapOf("index" to index.toString(), "total" to size.toString())
        } else {
            emptyMap()
        }

        when (val result = dto.toDomainWithAppError(mapper, errorCode, details)) {
            is Result.Success -> domains.add(result.data)
            is Result.Failure -> return result
            is Result.Loading -> return Result.Loading
        }
    }
    return Result.Success(domains)
}

/**
 * Mapea un Result<Domain> a otro tipo usando una transformación.
 *
 * Útil para encadenar transformaciones después de convertir de DTO a Domain.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * data class UserDto(val email: String, val age: Int)
 * data class User(val email: String, val age: Int)
 * data class UserEntity(val id: String, val user: User)
 *
 * val dto = UserDto(...)
 * val entityResult = dto.toDomain(UserMapper)
 *     .mapDomain { user ->
 *         UserEntity(id = UUID.randomUUID().toString(), user = user)
 *     }
 * ```
 *
 * @param transform Función de transformación del dominio
 * @return Result con el dominio transformado
 */
inline fun <Domain, R> Result<Domain>.mapDomain(
    crossinline transform: (Domain) -> R
): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Failure -> this
        is Result.Loading -> Result.Loading
    }
}

/**
 * FlatMap específico para dominios que retorna Result.
 *
 * Útil para encadenar operaciones que pueden fallar después de la conversión.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val dto = UserDto(...)
 * val result = dto.toDomain(UserMapper)
 *     .flatMapDomain { user ->
 *         // Guardar en BD puede fallar
 *         saveUserToDatabase(user)
 *     }
 * ```
 *
 * @param transform Función que transforma el dominio y retorna otro Result
 * @return Result con el resultado de la transformación
 */
inline fun <Domain, R> Result<Domain>.flatMapDomain(
    crossinline transform: (Domain) -> Result<R>
): Result<R> {
    return flatMap { transform(it) }
}

/**
 * Valida y convierte un DTO a dominio con validación personalizada adicional.
 *
 * Permite agregar validaciones adicionales después de la conversión del mapper.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val dto = UserDto(email = "test@example.com", age = 25)
 * val result = dto.toDomainWithValidation(UserMapper) { user ->
 *     // Validación adicional: verificar si email ya existe en BD
 *     if (emailExistsInDatabase(user.email)) {
 *         Result.Failure("Email already registered")
 *     } else {
 *         Result.Success(user)
 *     }
 * }
 * ```
 *
 * @param mapper Mapper a usar para la conversión inicial
 * @param additionalValidation Validación adicional a aplicar después de la conversión
 * @return Result con el dominio validado o error
 */
inline fun <DTO, Domain> DTO.toDomainWithValidation(
    mapper: DomainMapper<DTO, Domain>,
    crossinline additionalValidation: (Domain) -> Result<Domain>
): Result<Domain> {
    return mapper.toDomain(this).flatMap { domain ->
        additionalValidation(domain)
    }
}

/**
 * Convierte un DTO a dominio con retry en caso de error específico.
 *
 * Útil para casos donde se puede intentar una conversión alternativa si falla.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val dto = UserDto(email = "test@example.com", age = 25)
 * val result = dto.toDomainWithFallback(
 *     primaryMapper = StrictUserMapper,
 *     fallbackMapper = LenientUserMapper
 * )
 * ```
 *
 * @param primaryMapper Mapper principal a intentar primero
 * @param fallbackMapper Mapper alternativo si el primero falla
 * @return Result del mapper principal, o del fallback si el primero falla
 */
fun <DTO, Domain> DTO.toDomainWithFallback(
    primaryMapper: DomainMapper<DTO, Domain>,
    fallbackMapper: DomainMapper<DTO, Domain>
): Result<Domain> {
    return when (val primaryResult = primaryMapper.toDomain(this)) {
        is Result.Success -> primaryResult
        is Result.Failure -> fallbackMapper.toDomain(this)
        is Result.Loading -> Result.Loading
    }
}

/**
 * Batch conversion con procesamiento parcial.
 *
 * Convierte una lista de DTOs a dominios, pero continúa procesando incluso
 * si algunos fallan. Retorna los éxitos y los errores por separado.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val dtos = listOf(
 *     UserDto(email = "valid@test.com", age = 25),
 *     UserDto(email = "invalid", age = 15),
 *     UserDto(email = "another@test.com", age = 30)
 * )
 *
 * val (successes, failures) = dtos.toDomainPartial(UserMapper)
 * println("Converted: ${successes.size}, Failed: ${failures.size}")
 * ```
 *
 * @param mapper Mapper a usar para cada conversión
 * @return Par con lista de éxitos y lista de errores (índice + mensaje)
 */
fun <DTO, Domain> List<DTO>.toDomainPartial(
    mapper: DomainMapper<DTO, Domain>
): Pair<List<Domain>, List<Pair<Int, String>>> {
    val successes = mutableListOf<Domain>()
    val failures = mutableListOf<Pair<Int, String>>()

    forEachIndexed { index, dto ->
        when (val result = mapper.toDomain(dto)) {
            is Result.Success -> successes.add(result.data)
            is Result.Failure -> failures.add(index to result.error)
            is Result.Loading -> { /* Ignorar loading en batch */ }
        }
    }

    return successes to failures
}

/**
 * Convierte DTOs a dominios y filtra solo los exitosos.
 *
 * Similar a toDomainPartial pero solo retorna los éxitos, descartando
 * silenciosamente los errores.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val dtos = listOf(validDto, invalidDto, anotherValidDto)
 * val users = dtos.toDomainListIgnoreErrors(UserMapper)
 * // Retorna solo los usuarios válidos
 * ```
 *
 * @param mapper Mapper a usar para cada conversión
 * @return Lista de dominios convertidos exitosamente
 */
fun <DTO, Domain> List<DTO>.toDomainListIgnoreErrors(
    mapper: DomainMapper<DTO, Domain>
): List<Domain> {
    return mapNotNull { dto ->
        when (val result = mapper.toDomain(dto)) {
            is Result.Success -> result.data
            is Result.Failure -> null
            is Result.Loading -> null
        }
    }
}

/**
 * Convierte una lista de DTOs con métricas de conversión.
 *
 * Retorna el resultado junto con estadísticas de la conversión.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val dtos = listOf(dto1, dto2, dto3)
 * val (result, metrics) = dtos.toDomainListWithMetrics(UserMapper)
 *
 * println("Total: ${metrics.total}")
 * println("Success: ${metrics.successCount}")
 * println("Failures: ${metrics.failureCount}")
 * ```
 *
 * @param mapper Mapper a usar para cada conversión
 * @return Par con el resultado y las métricas de conversión
 */
fun <DTO, Domain> List<DTO>.toDomainListWithMetrics(
    mapper: DomainMapper<DTO, Domain>
): Pair<Result<List<Domain>>, ConversionMetrics> {
    val startTime = System.currentTimeMillis()
    val (successes, failures) = toDomainPartial(mapper)
    val endTime = System.currentTimeMillis()

    val metrics = ConversionMetrics(
        total = size,
        successCount = successes.size,
        failureCount = failures.size,
        durationMs = endTime - startTime,
        failures = failures
    )

    val result = if (failures.isEmpty()) {
        Result.Success(successes)
    } else {
        Result.Failure("Conversion failed for ${failures.size} items: ${failures.joinToString("; ") { "${it.first}: ${it.second}" }}")
    }

    return result to metrics
}

/**
 * Métricas de conversión batch.
 *
 * Contiene estadísticas sobre una operación de conversión masiva.
 *
 * @property total Número total de items procesados
 * @property successCount Número de conversiones exitosas
 * @property failureCount Número de conversiones fallidas
 * @property durationMs Duración de la conversión en milisegundos
 * @property failures Lista de índices y mensajes de error de los items que fallaron
 */
data class ConversionMetrics(
    val total: Int,
    val successCount: Int,
    val failureCount: Int,
    val durationMs: Long,
    val failures: List<Pair<Int, String>>
) {
    /**
     * Tasa de éxito como porcentaje (0.0 a 100.0).
     */
    val successRate: Double
        get() = if (total == 0) 0.0 else (successCount.toDouble() / total) * 100.0

    /**
     * Indica si la conversión fue 100% exitosa.
     */
    val isFullSuccess: Boolean
        get() = failureCount == 0

    /**
     * Indica si hubo al menos un éxito.
     */
    val hasAnySuccess: Boolean
        get() = successCount > 0
}
