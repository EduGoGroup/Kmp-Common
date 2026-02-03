package com.edugo.test.module.data.models.base

import kotlinx.datetime.Instant

/**
 * Interface base genérica para todas las entidades del dominio.
 *
 * Esta interface define las propiedades fundamentales que toda entidad debe tener:
 * un identificador único y timestamps de auditoría. El tipo del identificador es
 * genérico, permitiendo usar String, Long, UUID u otros tipos según las necesidades.
 *
 * ## Características
 *
 * - **Genérico y Type-Safe**: Soporta cualquier tipo de ID (String, Long, UUID, etc.)
 * - **Inmutable**: Las propiedades son val (read-only) para garantizar inmutabilidad
 * - **Auditoría Automática**: Incluye timestamps de creación y actualización
 * - **Serializable**: Compatible con kotlinx.serialization cuando se implementa en data classes
 *
 * ## Uso Recomendado
 *
 * ```kotlin
 * @Serializable
 * data class User(
 *     override val id: String,
 *     val name: String,
 *     val email: String,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant
 * ) : EntityBase<String>
 *
 * @Serializable
 * data class Order(
 *     override val id: Long,
 *     val total: Double,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant
 * ) : EntityBase<Long>
 * ```
 *
 * ## Composición
 *
 * Esta interface se puede combinar con otras interfaces para crear modelos ricos:
 *
 * ```kotlin
 * @Serializable
 * data class Product(
 *     override val id: String,
 *     val name: String,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant
 * ) : EntityBase<String>, ValidatableModel {
 *     override fun validate(): Result<Unit> {
 *         return when {
 *             name.isBlank() -> failure("Product name cannot be blank")
 *             else -> success(Unit)
 *         }
 *     }
 * }
 * ```
 *
 * ## Migración desde BaseModel
 *
 * Si estás migrando desde la interface [com.edugo.test.module.data.models.BaseModel],
 * necesitas agregar los campos de timestamp:
 *
 * ```kotlin
 * // Antes
 * data class User(override val id: String, val name: String) : BaseModel
 *
 * // Después
 * data class User(
 *     override val id: String,
 *     val name: String,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant
 * ) : EntityBase<String>
 * ```
 *
 * @param ID El tipo del identificador único (String, Long, UUID, etc.)
 * @property id Identificador único de la entidad
 * @property createdAt Timestamp de cuándo fue creada la entidad (UTC)
 * @property updatedAt Timestamp de la última actualización de la entidad (UTC)
 *
 * @see ValidatableModel Para agregar validación de datos
 * @see AuditableModel Para información de auditoría extendida
 * @see SoftDeletable Para soft delete funcionalidad
 */
public interface EntityBase<ID> {
    /**
     * Identificador único de la entidad.
     *
     * El tipo de este identificador es genérico y puede ser:
     * - String: Para UUIDs o identificadores generados por el servidor
     * - Long: Para IDs auto-incrementales de bases de datos
     * - UUID: Para identificadores únicos universales
     * - Cualquier otro tipo que represente un identificador único
     */
    val id: ID

    /**
     * Timestamp de cuándo fue creada la entidad.
     *
     * Este valor debe establecerse una vez al crear la entidad y no debe cambiar.
     * Se recomienda usar Clock.System.now() para obtener el timestamp actual.
     *
     * Ejemplo:
     * ```kotlin
     * val user = User(
     *     id = "user-123",
     *     name = "John",
     *     createdAt = Clock.System.now(),
     *     updatedAt = Clock.System.now()
     * )
     * ```
     */
    val createdAt: Instant

    /**
     * Timestamp de la última actualización de la entidad.
     *
     * Este valor debe actualizarse cada vez que se modifica la entidad.
     * Para operaciones de actualización, se debe crear una copia con el nuevo timestamp:
     *
     * Ejemplo:
     * ```kotlin
     * val updatedUser = user.copy(
     *     name = "Jane",
     *     updatedAt = Clock.System.now()
     * )
     * ```
     */
    val updatedAt: Instant
}
