package com.edugo.test.module.extensions

import com.edugo.test.module.core.Result

/**
 * Extensiones para transformar colecciones de/hacia Result con manejo inteligente de errores.
 *
 * Estas extensiones permiten trabajar con colecciones de Result de forma funcional,
 * con opciones para fail-fast o acumular todos los errores.
 *
 * ## Características
 *
 * - **sequence()**: `List<Result<T>>` → `Result<List<T>>`
 * - **traverse()**: `List<T>` + `(T) -> Result<R>` → `Result<List<R>>`
 * - **Fail-Fast**: Se detiene en el primer error (comportamiento por defecto)
 * - **Collect-All**: Acumula todos los errores antes de fallar
 * - **Performance**: O(n) sin copias innecesarias
 *
 * ## Uso Básico
 *
 * ```kotlin
 * // Sequence: Combinar Results
 * val results: List<Result<User>> = users.map { validateUser(it) }
 * val allUsers: Result<List<User>> = results.sequence()
 *
 * // Traverse: Aplicar transformación
 * val userIds = listOf("1", "2", "3")
 * val users: Result<List<User>> = userIds.traverse { id -> fetchUser(id) }
 * ```
 */

/**
 * Transforma una lista de Result<T> en un Result<List<T>> (fail-fast).
 *
 * Si todos los Results son Success, retorna Success con la lista de valores.
 * Si alguno es Failure, retorna el primer Failure encontrado.
 * Si alguno es Loading (y ninguno es Failure), retorna Loading.
 *
 * ## Ejemplo con Validación
 *
 * ```kotlin
 * data class User(val name: String, val email: String)
 * data class UserDto(val name: String, val email: String)
 *
 * val dtos = listOf(
 *     UserDto("John", "john@example.com"),
 *     UserDto("Jane", "jane@example.com")
 * )
 *
 * val validatedUsers: List<Result<User>> = dtos.map { dto ->
 *     zip2(
 *         validateName(dto.name),
 *         validateEmail(dto.email)
 *     ) { name, email -> User(name, email) }
 * }
 *
 * val result: Result<List<User>> = validatedUsers.sequence()
 * ```
 *
 * ## Ejemplo con API Calls
 *
 * ```kotlin
 * val userIds = listOf("1", "2", "3", "4", "5")
 * val userResults = userIds.map { id -> fetchUser(id) }
 * val allUsers: Result<List<User>> = userResults.sequence()
 * // Falla si algún fetchUser falló
 * ```
 *
 * @return Success con lista de valores si todos son Success, o el primer error encontrado
 */
fun <T> List<Result<T>>.sequence(): Result<List<T>> {
    // Chequear Failure primero (fail-fast)
    forEach { result ->
        if (result is Result.Failure) return result
    }

    // Chequear Loading segundo
    forEach { result ->
        if (result is Result.Loading) return Result.Loading
    }

    // Todos deben ser Success - extraer valores
    val values = map { result ->
        when (result) {
            is Result.Success -> result.data
            else -> return Result.Failure("Unexpected state in sequence")
        }
    }

    return Result.Success(values)
}

/**
 * Transforma una lista de Result<T> en un Result<List<T>> acumulando TODOS los errores.
 *
 * A diferencia de sequence() que es fail-fast, esta función acumula todos los errores
 * encontrados y los concatena con un separador.
 *
 * ## Ejemplo de Validación de Formulario
 *
 * ```kotlin
 * val fields = listOf(
 *     validateEmail(form.email),
 *     validatePassword(form.password),
 *     validateUsername(form.username),
 *     validateAge(form.age)
 * )
 *
 * val result = fields.sequenceCollectingErrors()
 * // Si falla: "Invalid email format; Password too short; Age must be positive"
 * ```
 *
 * ## Uso con Validación Compleja
 *
 * ```kotlin
 * data class Product(val name: String, val price: Double, val stock: Int)
 *
 * val products = productDtos.map { dto ->
 *     zip3(
 *         validateName(dto.name),
 *         validatePrice(dto.price),
 *         validateStock(dto.stock)
 *     ) { name, price, stock -> Product(name, price, stock) }
 * }
 *
 * val result = products.sequenceCollectingErrors()
 * // Muestra TODOS los errores de TODOS los productos
 * ```
 *
 * @param separator Separador entre mensajes de error (default: "; ")
 * @return Success con lista de valores si todos son Success, o Failure con todos los errores
 */
fun <T> List<Result<T>>.sequenceCollectingErrors(separator: String = "; "): Result<List<T>> {
    val errors = mutableListOf<String>()
    val values = mutableListOf<T>()
    var hasLoading = false

    forEach { result ->
        when (result) {
            is Result.Success -> values.add(result.data)
            is Result.Failure -> errors.add(result.error)
            is Result.Loading -> hasLoading = true
        }
    }

    return when {
        errors.isNotEmpty() -> Result.Failure(errors.joinToString(separator))
        hasLoading -> Result.Loading
        else -> Result.Success(values)
    }
}

/**
 * Aplica una transformación que retorna Result a cada elemento de la lista (fail-fast).
 *
 * Esta es la versión fail-fast de traverse. Se detiene en el primer error encontrado.
 * Es equivalente a `map { transform(it) }.sequence()` pero más eficiente.
 *
 * ## Ejemplo de Conversión DTO
 *
 * ```kotlin
 * val dtos = listOf(
 *     UserDto("John", 25, "john@example.com"),
 *     UserDto("Jane", 30, "jane@example.com")
 * )
 *
 * val users: Result<List<User>> = dtos.traverse { dto ->
 *     UserMapper.toDomain(dto)
 * }
 * ```
 *
 * ## Ejemplo con API Fetching
 *
 * ```kotlin
 * val userIds = listOf("1", "2", "3")
 * val users: Result<List<User>> = userIds.traverse { id ->
 *     fetchUserFromApi(id)
 * }
 * // Falla en el primer fetch que falle
 * ```
 *
 * ## Ejemplo de Validación
 *
 * ```kotlin
 * val emails = listOf("john@test.com", "invalid", "jane@test.com")
 * val validated: Result<List<String>> = emails.traverse { email ->
 *     validateEmail(email).map { email }
 * }
 * // Falla en "invalid"
 * ```
 *
 * @param transform Función que transforma cada elemento en un Result
 * @return Success con lista de valores transformados, o el primer error encontrado
 */
inline fun <T, R> List<T>.traverse(transform: (T) -> Result<R>): Result<List<R>> {
    val results = mutableListOf<R>()

    for (item in this) {
        when (val result = transform(item)) {
            is Result.Success -> results.add(result.data)
            is Result.Failure -> return result
            is Result.Loading -> return Result.Loading
        }
    }

    return Result.Success(results)
}

/**
 * Aplica una transformación que retorna Result a cada elemento, acumulando TODOS los errores.
 *
 * A diferencia de traverse() que es fail-fast, esta función procesa TODOS los elementos
 * y acumula todos los errores encontrados.
 *
 * ## Ejemplo de Validación Masiva
 *
 * ```kotlin
 * val emails = listOf("john@test.com", "invalid1", "jane@test.com", "invalid2")
 * val result = emails.traverseCollectingErrors { email ->
 *     validateEmail(email)
 * }
 * // Falla con: "Invalid email format at index 1; Invalid email format at index 3"
 * ```
 *
 * ## Ejemplo de Conversión Batch con Errores Detallados
 *
 * ```kotlin
 * val dtos = listOf(dto1, dto2, dto3, dto4)
 * val result = dtos.traverseCollectingErrors { dto ->
 *     UserMapper.toDomain(dto)
 * }
 * // Muestra TODOS los DTOs que fallaron y por qué
 * ```
 *
 * @param separator Separador entre mensajes de error (default: "; ")
 * @param includeIndex Si true, agrega el índice del elemento que falló al mensaje
 * @param transform Función que transforma cada elemento en un Result
 * @return Success con lista de valores transformados, o Failure con todos los errores
 */
inline fun <T, R> List<T>.traverseCollectingErrors(
    separator: String = "; ",
    includeIndex: Boolean = true,
    transform: (T) -> Result<R>
): Result<List<R>> {
    val errors = mutableListOf<String>()
    val values = mutableListOf<R>()
    var hasLoading = false

    forEachIndexed { index, item ->
        when (val result = transform(item)) {
            is Result.Success -> values.add(result.data)
            is Result.Failure -> {
                val errorMsg = if (includeIndex) {
                    "Index $index: ${result.error}"
                } else {
                    result.error
                }
                errors.add(errorMsg)
            }
            is Result.Loading -> hasLoading = true
        }
    }

    return when {
        errors.isNotEmpty() -> Result.Failure(errors.joinToString(separator))
        hasLoading -> Result.Loading
        else -> Result.Success(values)
    }
}

/**
 * Filtra y transforma elementos que cumplen una condición, con manejo de errores.
 *
 * Combina filter y traverse en una sola operación eficiente.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val userIds = listOf("1", "2", "invalid", "3")
 * val result = userIds.filterTraverse { id ->
 *     if (id.toIntOrNull() != null) {
 *         fetchUser(id)
 *     } else {
 *         null
 *     }
 * }
 * // Solo procesa IDs válidos
 * ```
 *
 * @param transform Función que retorna Result<R> si el elemento debe incluirse, null si debe filtrarse
 * @return Success con lista de valores transformados que pasaron el filtro
 */
inline fun <T, R> List<T>.filterTraverse(transform: (T) -> Result<R>?): Result<List<R>> {
    val results = mutableListOf<R>()

    for (item in this) {
        val result = transform(item) ?: continue
        when (result) {
            is Result.Success -> results.add(result.data)
            is Result.Failure -> return result
            is Result.Loading -> return Result.Loading
        }
    }

    return Result.Success(results)
}

/**
 * Particiona una lista de Results en éxitos y fallos.
 *
 * Útil para procesamiento parcial donde queremos separar los resultados exitosos
 * de los fallidos sin perder información.
 *
 * ## Ejemplo de Procesamiento Parcial
 *
 * ```kotlin
 * val dtos = listOf(dto1, dto2, dto3, dto4)
 * val results = dtos.map { UserMapper.toDomain(it) }
 * val (successes, failures) = results.partition()
 *
 * println("Converted: ${successes.size}, Failed: ${failures.size}")
 * successes.forEach { user -> saveToDatabase(user) }
 * failures.forEach { error -> logError(error) }
 * ```
 *
 * @return Par con lista de valores exitosos y lista de mensajes de error
 */
fun <T> List<Result<T>>.partition(): Pair<List<T>, List<String>> {
    val successes = mutableListOf<T>()
    val failures = mutableListOf<String>()

    forEach { result ->
        when (result) {
            is Result.Success -> successes.add(result.data)
            is Result.Failure -> failures.add(result.error)
            is Result.Loading -> { /* Ignorar Loading en partición */ }
        }
    }

    return successes to failures
}

/**
 * Aplica una transformación y particiona los resultados en éxitos y fallos.
 *
 * Combina traverse con partition para procesamiento parcial eficiente.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val emails = listOf("john@test.com", "invalid", "jane@test.com", "bad-email")
 * val (valid, invalid) = emails.traversePartition { email ->
 *     validateEmail(email)
 * }
 *
 * println("Valid: $valid")     // ["john@test.com", "jane@test.com"]
 * println("Invalid: $invalid") // ["Invalid email format", "Invalid email format"]
 * ```
 *
 * @param transform Función que transforma cada elemento en un Result
 * @return Par con lista de valores exitosos y lista de mensajes de error
 */
inline fun <T, R> List<T>.traversePartition(transform: (T) -> Result<R>): Pair<List<R>, List<String>> {
    val successes = mutableListOf<R>()
    val failures = mutableListOf<String>()

    forEach { item ->
        when (val result = transform(item)) {
            is Result.Success -> successes.add(result.data)
            is Result.Failure -> failures.add(result.error)
            is Result.Loading -> { /* Ignorar Loading en partición */ }
        }
    }

    return successes to failures
}

/**
 * Convierte solo los Results exitosos en valores, descartando errores.
 *
 * Útil cuando queremos procesar lo que podamos y descartar errores silenciosamente.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val results = listOf(Success(1), Failure("error"), Success(2), Success(3))
 * val values = results.collectSuccesses()  // [1, 2, 3]
 * ```
 *
 * @return Lista con solo los valores de los Results Success
 */
fun <T> List<Result<T>>.collectSuccesses(): List<T> {
    return mapNotNull { result ->
        when (result) {
            is Result.Success -> result.data
            else -> null
        }
    }
}

/**
 * Convierte solo los mensajes de error de los Results fallidos.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val results = listOf(Success(1), Failure("error1"), Success(2), Failure("error2"))
 * val errors = results.collectFailures()  // ["error1", "error2"]
 * ```
 *
 * @return Lista con solo los mensajes de error de los Results Failure
 */
fun <T> List<Result<T>>.collectFailures(): List<String> {
    return mapNotNull { result ->
        when (result) {
            is Result.Failure -> result.error
            else -> null
        }
    }
}

/**
 * Retorna true si todos los Results son Success.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val results = listOf(Success(1), Success(2), Success(3))
 * if (results.allSuccess()) {
 *     println("All operations succeeded!")
 * }
 * ```
 *
 * @return true si todos son Success, false en caso contrario
 */
fun <T> List<Result<T>>.allSuccess(): Boolean {
    return all { it is Result.Success }
}

/**
 * Retorna true si algún Result es Failure.
 *
 * @return true si hay al menos un Failure, false en caso contrario
 */
fun <T> List<Result<T>>.anyFailure(): Boolean {
    return any { it is Result.Failure }
}

/**
 * Cuenta cuántos Results son Success.
 *
 * @return Número de Results exitosos
 */
fun <T> List<Result<T>>.countSuccesses(): Int {
    return count { it is Result.Success }
}

/**
 * Cuenta cuántos Results son Failure.
 *
 * @return Número de Results fallidos
 */
fun <T> List<Result<T>>.countFailures(): Int {
    return count { it is Result.Failure }
}
