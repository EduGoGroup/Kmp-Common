package com.edugo.test.module.validation

import com.edugo.test.module.core.Result
import com.edugo.test.module.core.failure
import com.edugo.test.module.core.success

/**
 * Sistema de validación acumulativa que recolecta todos los errores.
 *
 * A diferencia del patrón fail-fast de ValidatableModel, este sistema
 * acumula todos los errores de validación antes de retornar, proporcionando
 * mejor UX al mostrar todos los problemas al usuario de una vez.
 *
 * ## Características
 *
 * - **Acumula Errores**: Recolecta todos los errores, no solo el primero
 * - **DSL Fluido**: Sintaxis clara y expresiva para validaciones
 * - **Type-Safe**: Verificación de tipos en compilación
 * - **Composable**: Se puede combinar con ValidationHelpers
 * - **Performance**: Eficiente con lazy evaluation
 *
 * ## Uso Básico
 *
 * ```kotlin
 * data class UserDto(val email: String, val age: Int, val username: String)
 *
 * fun validateUserDto(dto: UserDto): Result<Unit> {
 *     return accumulateValidationErrors {
 *         add(validateEmail(dto.email))
 *         add(validateRange(dto.age, 18, 120, "Age"))
 *         add(validateLengthRange(dto.username, 3, 30, "Username"))
 *     }
 * }
 * ```
 *
 * ## Uso en Mapper
 *
 * ```kotlin
 * object UserMapper : DomainMapper<UserDto, User> {
 *     override fun toDomain(dto: UserDto): Result<User> {
 *         val validationResult = accumulateValidationErrors {
 *             add(validateEmail(dto.email))
 *             add(validateRange(dto.age, 18, 120, "Age"))
 *             add(validateNotBlank(dto.username, "Username"))
 *         }
 *
 *         return validationResult.flatMap {
 *             success(User(dto.email, dto.age, dto.username))
 *         }
 *     }
 *
 *     override fun toDto(domain: User): UserDto {
 *         return UserDto(domain.email, domain.age, domain.username)
 *     }
 * }
 * ```
 *
 * ## Validación Condicional
 *
 * ```kotlin
 * fun validateProduct(product: ProductDto): Result<Unit> {
 *     return accumulateValidationErrors {
 *         add(validateNotBlank(product.name, "Name"))
 *         add(validatePositive(product.price, "Price"))
 *
 *         // Validación condicional
 *         if (product.onSale) {
 *             add(validateRange(product.discount, 0, 100, "Discount"))
 *         }
 *
 *         // Validación de colección
 *         if (product.tags.isEmpty()) {
 *             add("Product must have at least one tag")
 *         }
 *     }
 * }
 * ```
 */

/**
 * Builder para acumular errores de validación.
 *
 * Esta clase proporciona un DSL fluido para agregar errores de validación
 * y construir el resultado final.
 */
public class ValidationErrorAccumulator {
    private val errors = mutableListOf<String>()

    /**
     * Agrega un error de validación si no es null.
     *
     * Si el error es null, se ignora (validación pasó).
     * Si el error es un String, se agrega a la lista de errores.
     *
     * @param error Mensaje de error o null si la validación pasó
     */
    public fun add(error: String?) {
        error?.let { errors.add(it) }
    }

    /**
     * Agrega múltiples errores de validación.
     *
     * Útil cuando se tienen múltiples validaciones en una sola expresión.
     *
     * ```kotlin
     * add(listOf(
     *     validateEmail(email),
     *     validateNotBlank(name, "Name")
     * ))
     * ```
     *
     * @param errors Lista de errores (null entries se ignoran)
     */
    public fun add(errors: List<String?>) {
        errors.forEach { add(it) }
    }

    /**
     * Construye el resultado de validación.
     *
     * Si no hay errores, retorna Success(Unit).
     * Si hay errores, retorna Failure con todos los errores concatenados.
     *
     * @param separator Separador entre errores (default: "; ")
     * @return Result con éxito o todos los errores acumulados
     */
    public fun build(separator: String = "; "): Result<Unit> {
        return if (errors.isEmpty()) {
            success(Unit)
        } else {
            failure(errors.joinToString(separator))
        }
    }

    /**
     * Retorna la lista inmutable de errores acumulados.
     *
     * Útil para testing o para procesar los errores de forma personalizada.
     *
     * @return Lista inmutable de errores
     */
    public fun getErrors(): List<String> = errors.toList()

    /**
     * Verifica si hay errores acumulados.
     *
     * @return true si hay al menos un error, false si no hay errores
     */
    public fun hasErrors(): Boolean = errors.isNotEmpty()

    /**
     * Cuenta cuántos errores se han acumulado.
     *
     * @return Número de errores acumulados
     */
    public fun errorCount(): Int = errors.size

    /**
     * Limpia todos los errores acumulados.
     *
     * Útil si se quiere reutilizar el mismo acumulador.
     */
    public fun clear() {
        errors.clear()
    }
}

/**
 * DSL function para acumular errores de validación.
 *
 * Esta función proporciona un builder scope donde se pueden agregar
 * múltiples validaciones y automáticamente construye el resultado.
 *
 * ## Ejemplos
 *
 * ```kotlin
 * // Ejemplo básico
 * val result = accumulateValidationErrors {
 *     add(validateEmail(email))
 *     add(validateRange(age, 18, 120, "Age"))
 * }
 *
 * // Con separador custom
 * val result = accumulateValidationErrors(separator = " | ") {
 *     add(validateEmail(email))
 *     add(validateRange(age, 18, 120, "Age"))
 * }
 *
 * // Con validaciones condicionales
 * val result = accumulateValidationErrors {
 *     add(validateNotBlank(name, "Name"))
 *
 *     if (requiresEmail) {
 *         add(validateEmail(email))
 *     }
 *
 *     if (items.isEmpty()) {
 *         add("At least one item is required")
 *     }
 * }
 * ```
 *
 * @param separator Separador entre errores (default: "; ")
 * @param block Bloque de código que agrega validaciones
 * @return Result.Success si no hay errores, Result.Failure con todos los errores si hay
 */
public inline fun accumulateValidationErrors(
    separator: String = "; ",
    block: ValidationErrorAccumulator.() -> Unit
): Result<Unit> {
    val accumulator = ValidationErrorAccumulator()
    accumulator.block()
    return accumulator.build(separator)
}

/**
 * Extension function para ValidatableModel que acumula errores de validación.
 *
 * Permite validar una lista de modelos ValidatableModel y acumular todos
 * los errores en lugar de retornar el primero (fail-fast).
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val users = listOf(user1, user2, user3)
 * val result = users.validateAllAccumulative()
 *
 * when (result) {
 *     is Result.Success -> println("All users valid")
 *     is Result.Failure -> println("Errors: ${result.error}")
 *     // Si falla: "User 1: Email is required; User 2: Age must be positive"
 * }
 * ```
 *
 * @param separator Separador entre errores (default: "; ")
 * @param itemPrefix Prefijo para cada item en el error (default: "Item")
 * @return Result.Success si todos son válidos, Result.Failure con todos los errores
 */
public fun List<com.edugo.test.module.data.models.base.ValidatableModel>.validateAllAccumulative(
    separator: String = "; ",
    itemPrefix: String = "Item"
): Result<Unit> {
    return accumulateValidationErrors(separator) {
        forEachIndexed { index, model ->
            val result = model.validate()
            if (result is Result.Failure) {
                add("$itemPrefix ${index + 1}: ${result.error}")
            }
        }
    }
}

/**
 * Combina múltiples Result<Unit> en uno solo.
 *
 * Si todos los resultados son Success, retorna Success.
 * Si alguno es Failure, acumula todos los errores.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val emailValidation = validateEmail(email)
 * val ageValidation = validateAge(age)
 * val nameValidation = validateName(name)
 *
 * val combined = combineValidations(emailValidation, ageValidation, nameValidation)
 * ```
 *
 * @param validations Resultados de validación a combinar
 * @param separator Separador entre errores (default: "; ")
 * @return Result.Success si todos son Success, Result.Failure con errores acumulados
 */
public fun combineValidations(
    vararg validations: Result<Unit>,
    separator: String = "; "
): Result<Unit> {
    return accumulateValidationErrors(separator) {
        validations.forEach { result ->
            if (result is Result.Failure) {
                add(result.error)
            }
        }
    }
}

/**
 * Extension function para ejecutar validación acumulativa en un objeto.
 *
 * Proporciona un contexto donde el receptor (this) es el objeto a validar,
 * y se pueden agregar múltiples validaciones sobre sus propiedades.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * data class UserDto(val email: String, val age: Int, val name: String)
 *
 * fun UserDto.validate(): Result<Unit> = validateWith {
 *     add(validateEmail(email))
 *     add(validateRange(age, 18, 120, "Age"))
 *     add(validateNotBlank(name, "Name"))
 * }
 * ```
 *
 * @param separator Separador entre errores (default: "; ")
 * @param block Bloque de validación con el receptor como contexto
 * @return Result.Success si todas las validaciones pasan, Result.Failure si alguna falla
 */
public inline fun <T> T.validateWith(
    separator: String = "; ",
    block: ValidationErrorAccumulator.(T) -> Unit
): Result<Unit> {
    return accumulateValidationErrors(separator) {
        block(this@validateWith)
    }
}

/**
 * Valida condicionalmente basado en un predicado.
 *
 * Solo ejecuta la validación si la condición es verdadera.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * accumulateValidationErrors {
 *     add(validateIf(isEmailRequired) {
 *         validateEmail(email)
 *     })
 *
 *     add(validateIf(isPremiumUser) {
 *         validateNotEmpty(preferences, "Preferences")
 *     })
 * }
 * ```
 *
 * @param condition Condición que determina si se ejecuta la validación
 * @param validation Función que retorna el error de validación o null
 * @return Error de validación si la condición es true y la validación falla, null en otro caso
 */
public inline fun validateIf(
    condition: Boolean,
    validation: () -> String?
): String? {
    return if (condition) validation() else null
}

/**
 * Valida que al menos una de las validaciones pase.
 *
 * Útil para validaciones alternativas (OR lógico).
 *
 * ## Ejemplo
 *
 * ```kotlin
 * // Usuario debe tener email O teléfono
 * val result = validateAtLeastOne(
 *     "User must have email or phone",
 *     { validateEmail(email) },
 *     { validatePhone(phone) }
 * )
 * ```
 *
 * @param errorMessage Mensaje de error si todas las validaciones fallan
 * @param validations Lista de funciones de validación
 * @return null si al menos una validación pasa, errorMessage si todas fallan
 */
public fun validateAtLeastOne(
    errorMessage: String,
    vararg validations: () -> String?
): String? {
    val allFailed = validations.all { it() != null }
    return if (allFailed) errorMessage else null
}

/**
 * Valida que todas las validaciones pasen (AND lógico).
 *
 * Retorna el primer error encontrado, o null si todas pasan.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val result = validateAll(
 *     { validateEmail(email) },
 *     { validateNotBlank(name, "Name") },
 *     { validateRange(age, 0, 150, "Age") }
 * )
 * ```
 *
 * @param validations Lista de funciones de validación
 * @return Primer error encontrado, o null si todas pasan
 */
public fun validateAll(vararg validations: () -> String?): String? {
    validations.forEach { validation ->
        validation()?.let { return it }
    }
    return null
}
