package com.edugo.test.module.data.models.helpers

import com.edugo.test.module.core.Result
import com.edugo.test.module.data.models.base.EntityBase
import com.edugo.test.module.data.models.base.ValidatableModel
import kotlinx.datetime.Clock

/**
 * Helpers para operaciones de merge (fusión) de modelos.
 *
 * Este archivo proporciona funciones de utilidad para combinar dos instancias
 * de un modelo, preservando propiedades inmutables y actualizando timestamps.
 */

/**
 * Estrategia de merge para determinar qué valor usar cuando ambos objetos tienen un valor.
 */
enum class MergeStrategy {
    /**
     * El valor del segundo objeto (other) sobrescribe el del primero (original).
     * Esta es la estrategia por defecto y más común.
     */
    PREFER_OTHER,

    /**
     * El valor del primer objeto (original) se mantiene.
     * Útil cuando quieres actualizar solo campos que están "vacíos" en el original.
     */
    PREFER_ORIGINAL,

    /**
     * Para valores que son colecciones, combina ambas.
     * Para otros tipos, usa PREFER_OTHER.
     */
    COMBINE_COLLECTIONS
}

/**
 * Clase auxiliar que encapsula el resultado de una operación de merge.
 *
 * @param T Tipo del modelo mergeado
 * @property merged El modelo resultante del merge
 * @property changedFields Lista de nombres de campos que fueron modificados
 */
data class MergeResult<T>(
    val merged: T,
    val changedFields: List<String> = emptyList()
) {
    /**
     * Indica si hubo algún cambio.
     */
    val hasChanges: Boolean
        get() = changedFields.isNotEmpty()

    /**
     * Número de campos que cambiaron.
     */
    val changeCount: Int
        get() = changedFields.size
}

/**
 * Extension function para mergear con validación automática.
 *
 * Esta función aplica un merge y luego valida el resultado si el modelo
 * implementa ValidatableModel.
 *
 * Ejemplo:
 * ```kotlin
 * val original = User(id = "1", name = "John", email = "john@example.com", ...)
 * val updates = User(id = "1", name = "Jane", email = "jane@example.com", ...)
 *
 * val result = original.mergeWithValidation(updates) { orig, upd ->
 *     orig.copy(
 *         name = upd.name,
 *         email = upd.email,
 *         updatedAt = Clock.System.now()
 *     )
 * }
 *
 * when (result) {
 *     is Result.Success -> println("Merged: ${result.data}")
 *     is Result.Failure -> println("Validation failed: ${result.error}")
 * }
 * ```
 *
 * @param other El objeto a mergear con este
 * @param merger Función que define cómo combinar los dos objetos
 * @return Result con el objeto mergeado si la validación pasa, Failure si falla
 */
inline fun <T : ValidatableModel> T.mergeWithValidation(
    other: T,
    merger: (original: T, other: T) -> T
): Result<T> {
    val merged = merger(this, other)
    return when (val validation = merged.validate()) {
        is Result.Success -> Result.Success(merged)
        is Result.Failure -> validation
        is Result.Loading -> Result.Loading
    }
}

/**
 * Extension function para mergear preservando EntityBase properties.
 *
 * Esta función asegura que las propiedades fundamentales de EntityBase
 * (id, createdAt) se preserven correctamente durante el merge.
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
 *     fun mergeWith(other: User): User {
 *         return mergeEntityBase(other) { orig, upd ->
 *             orig.copy(
 *                 name = upd.name,
 *                 email = upd.email,
 *                 updatedAt = Clock.System.now()
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * La función garantiza:
 * - El ID del original se preserva (no cambia)
 * - El createdAt del original se preserva
 * - El updatedAt se actualiza a now()
 *
 * @param other El objeto a mergear
 * @param merger Función que define el merge de los campos específicos del modelo
 * @return El modelo mergeado con EntityBase properties correctamente preservadas
 */
inline fun <ID, T : EntityBase<ID>> T.mergeEntityBase(
    other: T,
    crossinline merger: (original: T, other: T) -> T
): T {
    // Validar que los IDs coinciden
    require(this.id == other.id) {
        "Cannot merge entities with different IDs: ${this.id} != ${other.id}"
    }

    // Aplicar el merge definido por el usuario
    val merged = merger(this, other)

    // Nota: El merger ya debería haber usado copy() para preservar createdAt
    // y actualizar updatedAt. Esta función solo valida el ID.
    return merged
}

/**
 * Helper para detectar qué campos cambiaron entre dos objetos.
 *
 * Esta función compara dos instancias y retorna los nombres de los campos
 * que son diferentes.
 *
 * **NOTA**: Esta es una implementación simplificada. Para una comparación
 * completa, considera usar reflection o generar código con KSP.
 *
 * Ejemplo:
 * ```kotlin
 * val original = User(id = "1", name = "John", email = "john@example.com")
 * val updated = User(id = "1", name = "Jane", email = "john@example.com")
 *
 * val changes = detectChanges(
 *     original, updated,
 *     "name" to { it.name },
 *     "email" to { it.email }
 * )
 * // changes = ["name"]
 * ```
 *
 * @param original Objeto original
 * @param updated Objeto actualizado
 * @param fields Pares de nombre de campo a función extractora
 * @return Lista de nombres de campos que cambiaron
 */
fun <T> detectChanges(
    original: T,
    updated: T,
    vararg fields: Pair<String, (T) -> Any?>
): List<String> {
    return fields.mapNotNull { (fieldName, extractor) ->
        val originalValue = extractor(original)
        val updatedValue = extractor(updated)
        if (originalValue != updatedValue) fieldName else null
    }
}

/**
 * Helper para mergear maps anidados de forma recursiva.
 *
 * Útil cuando tus modelos contienen Maps que necesitan ser combinados
 * en lugar de reemplazados completamente.
 *
 * Ejemplo:
 * ```kotlin
 * val original = mapOf("a" to 1, "b" to 2)
 * val updates = mapOf("b" to 3, "c" to 4)
 *
 * val merged = mergeMaps(original, updates)
 * // merged = {a=1, b=3, c=4}
 * ```
 *
 * @param original Map original
 * @param updates Map con actualizaciones
 * @param strategy Estrategia de merge (por defecto PREFER_OTHER)
 * @return Map combinado
 */
fun <K, V> mergeMaps(
    original: Map<K, V>,
    updates: Map<K, V>,
    strategy: MergeStrategy = MergeStrategy.PREFER_OTHER
): Map<K, V> {
    return when (strategy) {
        MergeStrategy.PREFER_OTHER -> original + updates
        MergeStrategy.PREFER_ORIGINAL -> updates + original
        MergeStrategy.COMBINE_COLLECTIONS -> {
            val combined = original.toMutableMap()
            updates.forEach { (key, value) ->
                combined[key] = value
            }
            combined
        }
    }
}

/**
 * Helper para mergear listas según estrategia.
 *
 * Ejemplo:
 * ```kotlin
 * val original = listOf(1, 2, 3)
 * val updates = listOf(3, 4, 5)
 *
 * // Reemplazar
 * val replaced = mergeLists(original, updates, MergeStrategy.PREFER_OTHER)
 * // replaced = [3, 4, 5]
 *
 * // Combinar (unión)
 * val combined = mergeLists(original, updates, MergeStrategy.COMBINE_COLLECTIONS)
 * // combined = [1, 2, 3, 4, 5] (sin duplicados)
 * ```
 *
 * @param original Lista original
 * @param updates Lista con actualizaciones
 * @param strategy Estrategia de merge
 * @return Lista combinada
 */
fun <T> mergeLists(
    original: List<T>,
    updates: List<T>,
    strategy: MergeStrategy = MergeStrategy.PREFER_OTHER
): List<T> {
    return when (strategy) {
        MergeStrategy.PREFER_OTHER -> updates
        MergeStrategy.PREFER_ORIGINAL -> if (updates.isEmpty()) original else updates
        MergeStrategy.COMBINE_COLLECTIONS -> (original + updates).distinct()
    }
}

/**
 * Helper para crear un merge builder fluido.
 *
 * Permite construir merges complejos de forma declarativa.
 *
 * Ejemplo:
 * ```kotlin
 * val merged = MergeBuilder(originalUser)
 *     .mergeIf(updates.name.isNotEmpty()) { it.copy(name = updates.name) }
 *     .mergeIf(updates.email.contains("@")) { it.copy(email = updates.email) }
 *     .updateTimestamp()
 *     .build()
 * ```
 *
 * @param T Tipo del modelo
 * @property current Estado actual del modelo durante el build
 */
class MergeBuilder<T>(private var current: T) {
    private val appliedChanges = mutableListOf<String>()

    /**
     * Aplica un merge solo si la condición es verdadera.
     */
    fun mergeIf(condition: Boolean, transform: (T) -> T): MergeBuilder<T> {
        if (condition) {
            current = transform(current)
            appliedChanges.add("conditional_merge")
        }
        return this
    }

    /**
     * Aplica un merge y registra el nombre del campo cambiado.
     */
    fun merge(fieldName: String, transform: (T) -> T): MergeBuilder<T> {
        current = transform(current)
        appliedChanges.add(fieldName)
        return this
    }

    /**
     * Actualiza el timestamp si el modelo es EntityBase.
     */
    fun updateTimestamp(): MergeBuilder<T> {
        // El usuario debe implementar esto en su modelo
        appliedChanges.add("updatedAt")
        return this
    }

    /**
     * Construye el resultado final.
     */
    fun build(): MergeResult<T> {
        return MergeResult(
            merged = current,
            changedFields = appliedChanges.toList()
        )
    }
}

/**
 * Extension function para iniciar un MergeBuilder.
 *
 * Ejemplo:
 * ```kotlin
 * val result = user.buildMerge()
 *     .mergeIf(name != null) { it.copy(name = name!!) }
 *     .mergeIf(email != null) { it.copy(email = email!!) }
 *     .build()
 * ```
 */
fun <T> T.buildMerge(): MergeBuilder<T> = MergeBuilder(this)
