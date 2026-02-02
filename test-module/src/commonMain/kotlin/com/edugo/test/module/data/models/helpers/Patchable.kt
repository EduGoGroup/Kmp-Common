package com.edugo.test.module.data.models.helpers

import com.edugo.test.module.core.Result
import com.edugo.test.module.data.models.base.ValidatableModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Interface marcadora para modelos que soportan operaciones de patch (actualización parcial).
 *
 * Esta interface sirve como marcador para identificar modelos que pueden ser
 * actualizados parcialmente. Los modelos que implementen esta interface deben
 * proporcionar una función de aplicación de patch específica para su tipo.
 *
 * ## Concepto de Patch
 *
 * Un "patch" es una actualización parcial donde solo se modifican los campos
 * que tienen valores no-null en el objeto patch, mientras que los valores null
 * indican "no cambiar este campo".
 *
 * ## Uso Típico
 *
 * ```kotlin
 * data class User(
 *     val id: String,
 *     val name: String,
 *     val email: String,
 *     val age: Int,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant
 * ) : EntityBase<String>, Patchable<User> {
 *
 *     data class Patch(
 *         val name: String? = null,
 *         val email: String? = null,
 *         val age: Int? = null
 *     )
 *
 *     fun applyPatch(patch: Patch): User {
 *         return copy(
 *             name = patch.name ?: name,
 *             email = patch.email ?: email,
 *             age = patch.age ?: age,
 *             updatedAt = Clock.System.now()
 *         )
 *     }
 * }
 * ```
 *
 * @param T El tipo del modelo que implementa esta interface
 */
interface Patchable<T>

/**
 * Interface marcadora para modelos que soportan operaciones de merge (fusión completa).
 *
 * Esta interface sirve como marcador para identificar modelos que pueden ser
 * fusionados con otra instancia del mismo tipo. A diferencia del patch, el merge
 * combina dos instancias completas.
 *
 * ## Concepto de Merge
 *
 * Un "merge" toma dos instancias completas y crea una nueva instancia combinando
 * los valores de ambas según una estrategia definida (típicamente el segundo
 * objeto sobrescribe al primero).
 *
 * ## Uso Típico
 *
 * ```kotlin
 * data class Configuration(
 *     val id: String,
 *     val theme: String,
 *     val language: String,
 *     val notifications: Boolean,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant
 * ) : EntityBase<String>, Mergeable<Configuration> {
 *
 *     fun mergeWith(other: Configuration): Configuration {
 *         return Configuration(
 *             id = id, // Preservar ID original
 *             theme = other.theme,
 *             language = other.language,
 *             notifications = other.notifications,
 *             createdAt = createdAt, // Preservar fecha de creación
 *             updatedAt = Clock.System.now() // Actualizar timestamp
 *         )
 *     }
 * }
 * ```
 *
 * @param T El tipo del modelo que implementa esta interface
 */
interface Mergeable<T>

/**
 * Data class genérica para representar operaciones de patch.
 *
 * Esta clase encapsula una operación de actualización parcial usando un Map
 * de campos a actualizar. Es útil cuando no quieres crear clases Patch específicas
 * para cada modelo.
 *
 * ## Uso
 *
 * ```kotlin
 * val userPatch = PatchOperation<User>(
 *     fields = mapOf(
 *         "name" to "New Name",
 *         "email" to "new@email.com"
 *     )
 * )
 * ```
 *
 * **NOTA**: Esta clase es más flexible pero menos type-safe que crear clases
 * Patch específicas. Úsala solo cuando necesites máxima flexibilidad.
 *
 * @param T El tipo del modelo a patchear
 * @property fields Map de nombre de campo a nuevo valor
 */
data class PatchOperation<T>(
    val fields: Map<String, Any?>
) {
    /**
     * Verifica si el patch contiene un campo específico.
     */
    fun hasField(fieldName: String): Boolean = fields.containsKey(fieldName)

    /**
     * Obtiene el valor de un campo del patch.
     */
    @Suppress("UNCHECKED_CAST")
    fun <R> getField(fieldName: String): R? = fields[fieldName] as? R

    /**
     * Retorna un nuevo PatchOperation con un campo adicional.
     */
    fun withField(fieldName: String, value: Any?): PatchOperation<T> {
        return copy(fields = fields + (fieldName to value))
    }

    /**
     * Retorna un nuevo PatchOperation sin un campo específico.
     */
    fun withoutField(fieldName: String): PatchOperation<T> {
        return copy(fields = fields - fieldName)
    }

    /**
     * Número de campos en el patch.
     */
    val fieldCount: Int
        get() = fields.size

    /**
     * Indica si el patch está vacío (no tiene campos).
     */
    val isEmpty: Boolean
        get() = fields.isEmpty()

    /**
     * Indica si el patch no está vacío.
     */
    val isNotEmpty: Boolean
        get() = fields.isNotEmpty()
}

/**
 * Extension function para aplicar un patch con validación automática.
 *
 * Esta función genérica aplica una transformación de patch y luego valida
 * el resultado si el modelo implementa ValidatableModel.
 *
 * Ejemplo:
 * ```kotlin
 * val user = User(id = "1", name = "John", ...)
 * val result = user.applyPatchWithValidation { original ->
 *     original.copy(
 *         name = "Jane",
 *         updatedAt = Clock.System.now()
 *     )
 * }
 *
 * when (result) {
 *     is Result.Success -> println("Patch aplicado: ${result.data}")
 *     is Result.Failure -> println("Validación falló: ${result.error}")
 * }
 * ```
 *
 * @param transform Función que aplica el patch y retorna el modelo actualizado
 * @return Result con el modelo actualizado si la validación pasa, Failure si falla
 */
inline fun <T : ValidatableModel> T.applyPatchWithValidation(
    transform: (T) -> T
): Result<T> {
    val patched = transform(this)
    return when (val validation = patched.validate()) {
        is Result.Success -> Result.Success(patched)
        is Result.Failure -> validation
        is Result.Loading -> Result.Loading
    }
}

/**
 * Extension function para aplicar múltiples patches en secuencia con validación.
 *
 * Aplica cada transformación en orden y valida después de cada una.
 * Si alguna validación falla, retorna el error y no aplica los patches restantes.
 *
 * Ejemplo:
 * ```kotlin
 * val result = user.applyPatchesWithValidation(
 *     { it.copy(name = "Jane") },
 *     { it.copy(email = "jane@example.com") },
 *     { it.copy(age = 30, updatedAt = Clock.System.now()) }
 * )
 * ```
 *
 * @param transforms Vararg de funciones de transformación
 * @return Result con el modelo con todos los patches aplicados, o el primer error
 */
inline fun <T : ValidatableModel> T.applyPatchesWithValidation(
    vararg transforms: (T) -> T
): Result<T> {
    var current: T = this

    for (transform in transforms) {
        when (val result = current.applyPatchWithValidation(transform)) {
            is Result.Success -> current = result.data
            is Result.Failure -> return result
            is Result.Loading -> return Result.Loading
        }
    }

    return Result.Success(current)
}

/**
 * Extension function para crear un PatchOperation desde un modelo con valores opcionales.
 *
 * Útil para crear patches dinámicamente desde objetos con propiedades nullables.
 *
 * **NOTA**: Esta función requiere que proporciones manualmente los campos a incluir.
 *
 * Ejemplo:
 * ```kotlin
 * data class UserUpdate(
 *     val name: String?,
 *     val email: String?,
 *     val age: Int?
 * )
 *
 * val update = UserUpdate(name = "Jane", email = null, age = 30)
 * val patch = update.toPatchOperation<User>(
 *     "name" to update.name,
 *     "age" to update.age
 * )
 * // Solo incluye campos con valores no-null
 * ```
 *
 * @param pairs Pares de nombre de campo a valor
 * @return PatchOperation con los campos no-null
 */
fun <T> toPatchOperation(vararg pairs: Pair<String, Any?>): PatchOperation<T> {
    return PatchOperation(
        fields = pairs.filter { it.second != null }.toMap()
    )
}

/**
 * Helper para preservar el timestamp de creación durante actualizaciones.
 *
 * Esta función asegura que el createdAt no cambie durante updates.
 *
 * Ejemplo:
 * ```kotlin
 * val updated = original.copy(
 *     name = "New Name",
 *     createdAt = preserveCreatedAt(original.createdAt),
 *     updatedAt = updateTimestamp()
 * )
 * ```
 */
fun preserveCreatedAt(createdAt: Instant): Instant = createdAt

/**
 * Helper para generar un nuevo timestamp de actualización.
 *
 * Ejemplo:
 * ```kotlin
 * val updated = original.copy(
 *     name = "New Name",
 *     updatedAt = updateTimestamp()
 * )
 * ```
 */
fun updateTimestamp(): Instant = Clock.System.now()

/**
 * Extension function para actualizar el updatedAt de un modelo que implementa EntityBase.
 *
 * **NOTA**: Esta función retorna un mapa con el timestamp, no modifica el objeto.
 * Usa copy() para aplicar el cambio.
 *
 * Ejemplo:
 * ```kotlin
 * val updated = original.copy(
 *     name = "New Name",
 *     updatedAt = Clock.System.now()
 * )
 * ```
 */
fun <T> withUpdatedTimestamp(transform: (Instant) -> T): T {
    return transform(Clock.System.now())
}
