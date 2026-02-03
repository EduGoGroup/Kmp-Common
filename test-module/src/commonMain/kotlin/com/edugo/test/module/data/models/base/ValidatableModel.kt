package com.edugo.test.module.data.models.base

import com.edugo.test.module.core.Result
import com.edugo.test.module.core.failure
import com.edugo.test.module.core.success

/**
 * Interface para modelos que requieren validación de sus datos.
 *
 * Esta interface define un contrato para validar la consistencia e integridad
 * de los datos de un modelo. Las implementaciones deben verificar todas las
 * reglas de negocio y restricciones de datos, retornando un [Result] que
 * indica éxito o falla con mensaje descriptivo.
 *
 * ## Características
 *
 * - **Composable**: Se puede combinar con otras interfaces (EntityBase, AuditableModel, etc.)
 * - **Type-Safe**: Usa Result<Unit> para manejo de errores predecible
 * - **Reutilizable**: Las validaciones pueden ser llamadas en cualquier momento
 * - **Testeable**: Fácil de testear con diferentes casos de prueba
 *
 * ## Uso Básico
 *
 * ```kotlin
 * @Serializable
 * data class User(
 *     override val id: String,
 *     val email: String,
 *     val age: Int,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant
 * ) : EntityBase<String>, ValidatableModel {
 *
 *     override fun validate(): Result<Unit> {
 *         return when {
 *             email.isBlank() ->
 *                 failure("Email cannot be blank")
 *             !email.contains("@") ->
 *                 failure("Email must be a valid email address")
 *             age < 0 ->
 *                 failure("Age cannot be negative")
 *             age > 150 ->
 *                 failure("Age must be less than 150")
 *             else ->
 *                 success(Unit)
 *         }
 *     }
 * }
 * ```
 *
 * ## Validaciones Complejas
 *
 * Para modelos con múltiples validaciones, se recomienda separar la lógica:
 *
 * ```kotlin
 * @Serializable
 * data class Product(
 *     override val id: String,
 *     val name: String,
 *     val price: Double,
 *     val stock: Int,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant
 * ) : EntityBase<String>, ValidatableModel {
 *
 *     override fun validate(): Result<Unit> {
 *         validateName()?.let { return it }
 *         validatePrice()?.let { return it }
 *         validateStock()?.let { return it }
 *         return success(Unit)
 *     }
 *
 *     private fun validateName(): Result<Unit>? {
 *         return when {
 *             name.isBlank() -> failure("Product name is required")
 *             name.length > 100 -> failure("Product name is too long (max 100 chars)")
 *             else -> null
 *         }
 *     }
 *
 *     private fun validatePrice(): Result<Unit>? {
 *         return when {
 *             price < 0 -> failure("Price cannot be negative")
 *             price > 1_000_000 -> failure("Price exceeds maximum allowed")
 *             else -> null
 *         }
 *     }
 *
 *     private fun validateStock(): Result<Unit>? {
 *         return when {
 *             stock < 0 -> failure("Stock cannot be negative")
 *             else -> null
 *         }
 *     }
 * }
 * ```
 *
 * ## Uso con ErrorCode
 *
 * Para errores más estructurados, se puede usar ErrorCode:
 *
 * ```kotlin
 * import com.edugo.test.module.core.ErrorCode
 *
 * override fun validate(): Result<Unit> {
 *     return when {
 *         email.isBlank() ->
 *             Result.Failure(ErrorCode.VALIDATION_MISSING_FIELD.description)
 *         !email.matches(emailRegex) ->
 *             Result.Failure(ErrorCode.VALIDATION_INVALID_EMAIL.description)
 *         else ->
 *             success(Unit)
 *     }
 * }
 * ```
 *
 * ## Composición con Otras Interfaces
 *
 * ```kotlin
 * @Serializable
 * data class AuditedUser(
 *     override val id: String,
 *     val email: String,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant,
 *     override val createdBy: String,
 *     override val updatedBy: String
 * ) : EntityBase<String>, ValidatableModel, AuditableModel {
 *
 *     override fun validate(): Result<Unit> {
 *         // Validaciones específicas del modelo
 *         return when {
 *             email.isBlank() -> failure("Email is required")
 *             createdBy.isBlank() -> failure("Created by is required")
 *             else -> success(Unit)
 *         }
 *     }
 * }
 * ```
 *
 * ## Testing
 *
 * ```kotlin
 * @Test
 * fun `validation fails when email is blank`() {
 *     val user = User(
 *         id = "123",
 *         email = "",
 *         age = 25,
 *         createdAt = Clock.System.now(),
 *         updatedAt = Clock.System.now()
 *     )
 *
 *     val result = user.validate()
 *
 *     assertTrue(result is Result.Failure)
 *     assertEquals("Email cannot be blank", (result as Result.Failure).error)
 * }
 * ```
 *
 * ## Cuándo Validar
 *
 * - **Al crear**: Validar antes de persistir nuevas entidades
 * - **Al actualizar**: Validar después de modificar datos
 * - **En APIs**: Validar datos recibidos del cliente
 * - **Antes de operaciones críticas**: Validar antes de operaciones que no pueden fallar
 *
 * @see Result Para entender el tipo de retorno
 * @see EntityBase Para la interface base de entidades
 * @see AuditableModel Para información de auditoría
 */
public interface ValidatableModel {
    /**
     * Valida la consistencia e integridad de los datos del modelo.
     *
     * Este método debe verificar todas las reglas de negocio y restricciones
     * de datos aplicables al modelo. Si todas las validaciones pasan, retorna
     * [Result.Success] con Unit. Si alguna validación falla, retorna
     * [Result.Failure] con un mensaje descriptivo del error.
     *
     * ## Reglas Generales
     *
     * - Retornar el **primer error** encontrado (fail-fast)
     * - Mensajes de error deben ser **claros y descriptivos**
     * - No lanzar excepciones, usar Result
     * - Validar **todos los campos críticos**
     * - Considerar **reglas de negocio** además de formato
     *
     * ## Ejemplo Simple
     *
     * ```kotlin
     * override fun validate(): Result<Unit> {
     *     return when {
     *         name.isBlank() -> failure("Name is required")
     *         email.isBlank() -> failure("Email is required")
     *         !email.contains("@") -> failure("Invalid email format")
     *         else -> success(Unit)
     *     }
     * }
     * ```
     *
     * ## Ejemplo con Validación de Rangos
     *
     * ```kotlin
     * override fun validate(): Result<Unit> {
     *     return when {
     *         age < 18 -> failure("User must be at least 18 years old")
     *         age > 120 -> failure("Invalid age value")
     *         salary < 0 -> failure("Salary cannot be negative")
     *         else -> success(Unit)
     *     }
     * }
     * ```
     *
     * @return [Result.Success] con Unit si todas las validaciones pasan,
     *         [Result.Failure] con mensaje de error si alguna validación falla
     */
    public fun validate(): Result<Unit>
}

/**
 * Extension function para validar y ejecutar una acción solo si la validación pasa.
 *
 * Esta función de conveniencia permite validar un modelo y ejecutar una acción
 * solo si la validación es exitosa, propagando el error si falla.
 *
 * Ejemplo:
 * ```kotlin
 * val user = User(...)
 * val result = user.validateAndThen {
 *     // Esta acción solo se ejecuta si la validación pasa
 *     saveToDatabase(user)
 *     Result.Success(user)
 * }
 * ```
 *
 * @param action Acción a ejecutar si la validación pasa
 * @return El resultado de la acción si la validación pasa, o el error de validación
 */
public inline fun <T> ValidatableModel.validateAndThen(action: () -> Result<T>): Result<T> {
    return when (val validationResult = validate()) {
        is Result.Success -> action()
        is Result.Failure -> validationResult
        is Result.Loading -> Result.Loading
    }
}

/**
 * Extension function para validar una lista de modelos validables.
 *
 * Valida todos los modelos de la lista y retorna el primer error encontrado,
 * o Success si todos pasan la validación.
 *
 * Ejemplo:
 * ```kotlin
 * val users = listOf(user1, user2, user3)
 * val result = users.validateAll()
 * ```
 *
 * @return Success si todos los modelos pasan la validación, Failure con el primer error encontrado
 */
public fun List<ValidatableModel>.validateAll(): Result<Unit> {
    forEach { model ->
        val result = model.validate()
        if (result is Result.Failure) {
            return result
        }
    }
    return success(Unit)
}
