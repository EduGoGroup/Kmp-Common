package com.edugo.test.module.data.models.helpers

import com.edugo.test.module.core.Result
import com.edugo.test.module.data.models.base.EntityBase
import com.edugo.test.module.data.models.base.ValidatableModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Helpers para operaciones de patch (actualización parcial) de modelos.
 *
 * Este archivo proporciona funciones de utilidad para aplicar actualizaciones
 * parciales a modelos, donde solo los campos con valores no-null se actualizan.
 */

/**
 * Clase auxiliar que encapsula el resultado de una operación de patch.
 *
 * @param T Tipo del modelo patcheado
 * @property patched El modelo resultante del patch
 * @property appliedFields Lista de nombres de campos que fueron actualizados
 */
public data class PatchResult<T>(
    val patched: T,
    val appliedFields: List<String> = emptyList()
) {
    /**
     * Indica si se aplicó algún patch.
     */
    val hasPatches: Boolean
        get() = appliedFields.isNotEmpty()

    /**
     * Número de campos que fueron patcheados.
     */
    val patchCount: Int
        get() = appliedFields.size
}

/**
 * Helper para aplicar un patch solo si el valor no es null.
 *
 * Esta función aplica una transformación solo si el nuevo valor no es null.
 * Es la base para implementar actualizaciones parciales.
 *
 * Ejemplo:
 * ```kotlin
 * data class User(
 *     val id: String,
 *     val name: String,
 *     val email: String,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant
 * ) : EntityBase<String> {
 *
 *     data class Patch(
 *         val name: String? = null,
 *         val email: String? = null
 *     )
 *
 *     fun applyPatch(patch: Patch): User {
 *         return this
 *             .patchField(patch.name) { copy(name = it) }
 *             .patchField(patch.email) { copy(email = it) }
 *             .copy(updatedAt = Clock.System.now())
 *     }
 * }
 * ```
 *
 * @param newValue Nuevo valor (si es null, no se aplica el patch)
 * @param transform Función que aplica el nuevo valor al modelo
 * @return El modelo con el patch aplicado si newValue no es null, o el modelo sin cambios
 */
public inline fun <T, V> T.patchField(newValue: V?, transform: T.(V) -> T): T {
    return if (newValue != null) transform(newValue) else this
}

/**
 * Extension function para aplicar un patch con validación automática.
 *
 * Similar a patchField, pero valida el resultado si el modelo implementa ValidatableModel.
 *
 * Ejemplo:
 * ```kotlin
 * val user = User(id = "1", name = "John", email = "john@example.com", ...)
 *
 * val result = user.patchFieldWithValidation("invalid-email") { newEmail ->
 *     copy(email = newEmail, updatedAt = Clock.System.now())
 * }
 *
 * // result será Failure porque el email es inválido
 * ```
 *
 * @param newValue Nuevo valor (si es null, no se aplica el patch)
 * @param transform Función que aplica el nuevo valor al modelo
 * @return Result con el modelo patcheado si la validación pasa, Failure si falla
 */
public inline fun <T : ValidatableModel, V> T.patchFieldWithValidation(
    newValue: V?,
    transform: T.(V) -> T
): Result<T> {
    if (newValue == null) {
        return Result.Success(this)
    }

    val patched = transform(newValue)
    return when (val validation = patched.validate()) {
        is Result.Success -> Result.Success(patched)
        is Result.Failure -> validation
        is Result.Loading -> Result.Loading
    }
}

/**
 * Helper para aplicar múltiples patches de forma segura.
 *
 * Esta función aplica una lista de patches opcionales y solo actualiza
 * los campos que tienen valores no-null.
 *
 * Ejemplo:
 * ```kotlin
 * val patched = user.applyPatches(
 *     "name" to updates.name,
 *     "email" to updates.email,
 *     "age" to updates.age
 * ) { model, fieldName, value ->
 *     when (fieldName) {
 *         "name" -> model.copy(name = value as String)
 *         "email" -> model.copy(email = value as String)
 *         "age" -> model.copy(age = value as Int)
 *         else -> model
 *     }
 * }
 * ```
 *
 * @param patches Vararg de pares nombre-campo a valor
 * @param patcher Función que aplica un patch individual
 * @return El modelo con todos los patches aplicados
 */
public inline fun <T> T.applyPatches(
    vararg patches: Pair<String, Any?>,
    patcher: (model: T, fieldName: String, value: Any) -> T
): T {
    var current = this

    for ((fieldName, value) in patches) {
        if (value != null) {
            current = patcher(current, fieldName, value)
        }
    }

    return current
}

/**
 * Helper para crear un patch builder fluido.
 *
 * Permite construir patches complejos de forma declarativa con validación
 * y actualización automática de timestamps.
 *
 * Ejemplo:
 * ```kotlin
 * val result = PatchBuilder(originalUser)
 *     .patch("name", updates.name) { copy(name = it) }
 *     .patch("email", updates.email) { copy(email = it) }
 *     .updateTimestampIfEntityBase()
 *     .validateAndBuild()
 * ```
 *
 * @param T Tipo del modelo
 * @property current Estado actual del modelo durante el build
 */
public class PatchBuilder<T>(private var current: T) {
    private val appliedFields = mutableListOf<String>()

    /**
     * Aplica un patch a un campo específico.
     *
     * @param fieldName Nombre del campo
     * @param newValue Nuevo valor (si es null, no se aplica)
     * @param transform Función que aplica el valor
     */
    public fun <V> patch(
        fieldName: String,
        newValue: V?,
        transform: T.(V) -> T
    ): PatchBuilder<T> {
        if (newValue != null) {
            current = current.transform(newValue)
            appliedFields.add(fieldName)
        }
        return this
    }

    /**
     * Aplica un patch condicional.
     *
     * @param condition Condición para aplicar el patch
     * @param fieldName Nombre del campo
     * @param transform Función que aplica el cambio
     */
    public fun patchIf(
        condition: Boolean,
        fieldName: String,
        transform: T.() -> T
    ): PatchBuilder<T> {
        if (condition) {
            current = current.transform()
            appliedFields.add(fieldName)
        }
        return this
    }

    /**
     * Actualiza el timestamp si el modelo implementa EntityBase.
     *
     * Esta función debe ser llamada al final del pipeline de patches.
     */
    public fun updateTimestampIfEntityBase(): PatchBuilder<T> {
        // El usuario debe aplicar esto en su copy() específico
        if (appliedFields.isNotEmpty()) {
            appliedFields.add("updatedAt")
        }
        return this
    }

    /**
     * Construye el resultado sin validación.
     */
    public fun build(): PatchResult<T> {
        return PatchResult(
            patched = current,
            appliedFields = appliedFields.toList()
        )
    }

    /**
     * Construye el resultado con validación si el modelo implementa ValidatableModel.
     */
    public fun validateAndBuild(): Result<PatchResult<T>> {
        if (current is ValidatableModel) {
            return when (val validation = (current as ValidatableModel).validate()) {
                is Result.Success -> Result.Success(build())
                is Result.Failure -> validation
                is Result.Loading -> Result.Loading
            }
        }
        return Result.Success(build())
    }
}

/**
 * Extension function para iniciar un PatchBuilder.
 *
 * Ejemplo:
 * ```kotlin
 * val result = user.buildPatch()
 *     .patch("name", updates.name) { copy(name = it) }
 *     .patch("email", updates.email) { copy(email = it) }
 *     .build()
 * ```
 */
public fun <T> T.buildPatch(): PatchBuilder<T> = PatchBuilder(this)

/**
 * Extension function para aplicar un patch a EntityBase preservando propiedades inmutables.
 *
 * Esta función asegura que durante el patch:
 * - El ID no cambia
 * - El createdAt no cambia
 * - El updatedAt se actualiza automáticamente si hubo cambios
 *
 * Ejemplo:
 * ```kotlin
 * data class User(
 *     override val id: String,
 *     val name: String,
 *     val email: String,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant
 * ) : EntityBase<String> {
 *
 *     data class Patch(
 *         val name: String? = null,
 *         val email: String? = null
 *     )
 *
 *     fun applyPatch(patch: Patch): User {
 *         return patchEntityBase { original ->
 *             original
 *                 .patchField(patch.name) { copy(name = it) }
 *                 .patchField(patch.email) { copy(email = it) }
 *         }
 *     }
 * }
 * ```
 *
 * @param patcher Función que aplica los patches específicos del modelo
 * @return El modelo patcheado con EntityBase properties correctamente manejadas
 */
public inline fun <ID, T : EntityBase<ID>> T.patchEntityBase(
    crossinline patcher: (T) -> T
): T {
    val originalId = this.id
    val originalCreatedAt = this.createdAt

    val patched = patcher(this)

    // Validar que las propiedades inmutables no cambiaron
    require(patched.id == originalId) {
        "ID cannot be changed during patch: $originalId != ${patched.id}"
    }

    require(patched.createdAt == originalCreatedAt) {
        "createdAt cannot be changed during patch"
    }

    return patched
}

/**
 * Helper para aplicar un patch con updatedAt automático.
 *
 * Esta función es un wrapper conveniente que aplica el patch y actualiza
 * el timestamp automáticamente.
 *
 * Ejemplo:
 * ```kotlin
 * val patched = user.patchWithTimestamp { original ->
 *     original.copy(
 *         name = updates.name ?: original.name,
 *         email = updates.email ?: original.email
 *     )
 * }
 * // patched.updatedAt será Clock.System.now()
 * ```
 *
 * NOTA: El patcher debe incluir updatedAt en el copy()
 */
public inline fun <ID, T : EntityBase<ID>> T.patchWithTimestamp(
    crossinline patcher: (T, Instant) -> T
): T {
    return patcher(this, Clock.System.now())
}

/**
 * Extension function para aplicar un patch solo a campos específicos permitidos.
 *
 * Útil para implementar seguridad y prevenir que campos sensibles sean patcheados.
 *
 * Ejemplo:
 * ```kotlin
 * val allowedFields = setOf("name", "email")
 *
 * val patch = mapOf(
 *     "name" to "Jane",
 *     "email" to "jane@example.com",
 *     "role" to "admin"  // Este será ignorado
 * )
 *
 * val patched = user.patchAllowedFields(patch, allowedFields) { model, field, value ->
 *     when (field) {
 *         "name" -> model.copy(name = value as String)
 *         "email" -> model.copy(email = value as String)
 *         else -> model
 *     }
 * }
 * ```
 *
 * @param patches Map de nombre de campo a valor
 * @param allowedFields Set de nombres de campos permitidos
 * @param patcher Función que aplica un patch individual
 * @return El modelo con los patches permitidos aplicados
 */
public inline fun <T> T.patchAllowedFields(
    patches: Map<String, Any?>,
    allowedFields: Set<String>,
    patcher: (model: T, fieldName: String, value: Any) -> T
): T {
    var current = this

    for ((fieldName, value) in patches) {
        if (value != null && fieldName in allowedFields) {
            current = patcher(current, fieldName, value)
        }
    }

    return current
}

/**
 * Helper para crear un patch nulo seguro desde un objeto con propiedades opcionales.
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
 *
 * val user = getUser()
 * val patched = user.applyNullSafePatch(
 *     update.name?.let { "name" to it },
 *     update.age?.let { "age" to it }
 * ) { model, field, value ->
 *     when (field) {
 *         "name" -> model.copy(name = value as String)
 *         "age" -> model.copy(age = value as Int)
 *         else -> model
 *     }
 * }
 * ```
 *
 * @param patches Vararg de pares opcionales (pueden ser null)
 * @param patcher Función que aplica un patch individual
 * @return El modelo con los patches aplicados
 */
public inline fun <T> T.applyNullSafePatch(
    vararg patches: Pair<String, Any?>?,
    patcher: (model: T, fieldName: String, value: Any) -> T
): T {
    var current = this

    for (patch in patches) {
        if (patch != null) {
            val (fieldName, value) = patch
            if (value != null) {
                current = patcher(current, fieldName, value)
            }
        }
    }

    return current
}
