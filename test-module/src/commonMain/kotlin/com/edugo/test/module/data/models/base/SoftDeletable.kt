package com.edugo.test.module.data.models.base

import kotlinx.datetime.Instant

/**
 * Interface para modelos que soportan eliminación lógica (soft delete).
 *
 * En lugar de eliminar físicamente los registros de la base de datos, el soft delete
 * marca los registros como eliminados usando un timestamp. Esto permite:
 * - Mantener historial completo
 * - Recuperar datos eliminados
 * - Auditoría de eliminaciones
 * - Cumplir con regulaciones de retención de datos
 *
 * ## Características
 *
 * - **No destructivo**: Los datos no se eliminan permanentemente
 * - **Recuperable**: Los registros pueden ser restaurados
 * - **Auditable**: Se mantiene el timestamp de eliminación
 * - **Composable**: Se combina con otras interfaces
 *
 * ## Uso Básico
 *
 * ```kotlin
 * @Serializable
 * data class User(
 *     override val id: String,
 *     val email: String,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant,
 *     override val deletedAt: Instant? = null
 * ) : EntityBase<String>, SoftDeletable
 * ```
 *
 * ## Con Auditoría Completa
 *
 * ```kotlin
 * @Serializable
 * data class AuditedDocument(
 *     override val id: String,
 *     val title: String,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant,
 *     override val createdBy: String,
 *     override val updatedBy: String,
 *     override val deletedAt: Instant? = null,
 *     val deletedBy: String? = null
 * ) : EntityBase<String>, AuditableModel, SoftDeletable
 * ```
 *
 * ## Operaciones de Soft Delete
 *
 * ### Marcar como eliminado
 * ```kotlin
 * fun deleteUser(user: User, deletedBy: String): User {
 *     return user.copy(
 *         deletedAt = Clock.System.now(),
 *         updatedAt = Clock.System.now()
 *     )
 * }
 * ```
 *
 * ### Restaurar
 * ```kotlin
 * fun restoreUser(user: User): User {
 *     return user.copy(
 *         deletedAt = null,
 *         updatedAt = Clock.System.now()
 *     )
 * }
 * ```
 *
 * ### Verificar estado
 * ```kotlin
 * if (user.isDeleted()) {
 *     println("User is deleted")
 * }
 *
 * if (user.isActive()) {
 *     println("User is active")
 * }
 * ```
 *
 * ## Filtrado en Consultas
 *
 * ```kotlin
 * // Obtener solo usuarios activos
 * val activeUsers = allUsers.filter { it.isActive() }
 *
 * // Obtener solo usuarios eliminados
 * val deletedUsers = allUsers.filter { it.isDeleted() }
 *
 * // Obtener usuarios eliminados en el último mes
 * val recentlyDeleted = allUsers.filter {
 *     it.deletedAt?.let { deletedTime ->
 *         val thirtyDaysAgo = Clock.System.now() - 30.days
 *         deletedTime > thirtyDaysAgo
 *     } ?: false
 * }
 * ```
 *
 * ## Con Validación
 *
 * ```kotlin
 * @Serializable
 * data class ValidatedEntity(
 *     override val id: String,
 *     val name: String,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant,
 *     override val deletedAt: Instant? = null
 * ) : EntityBase<String>, SoftDeletable, ValidatableModel {
 *
 *     override fun validate(): Result<Unit> {
 *         return when {
 *             name.isBlank() -> failure("Name is required")
 *             isDeleted() -> failure("Cannot validate deleted entity")
 *             else -> success(Unit)
 *         }
 *     }
 * }
 * ```
 *
 * ## Extensión con Razón de Eliminación
 *
 * ```kotlin
 * @Serializable
 * data class ExtendedSoftDeletable(
 *     override val id: String,
 *     val data: String,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant,
 *     override val deletedAt: Instant? = null,
 *     val deletedBy: String? = null,
 *     val deletionReason: String? = null
 * ) : EntityBase<String>, SoftDeletable
 * ```
 *
 * ## Purga de Datos Antiguos
 *
 * ```kotlin
 * // Política: Purgar registros eliminados hace más de 1 año
 * fun shouldPurge(entity: SoftDeletable, retentionDays: Int = 365): Boolean {
 *     return entity.deletedAt?.let { deletedTime ->
 *         val cutoffDate = Clock.System.now() - retentionDays.days
 *         deletedTime < cutoffDate
 *     } ?: false
 * }
 *
 * // Obtener registros que deben ser purgados
 * val toPurge = deletedEntities.filter { shouldPurge(it) }
 * ```
 *
 * ## Testing
 *
 * ```kotlin
 * @Test
 * fun `entity is marked as deleted after soft delete`() {
 *     val user = User(
 *         id = "user-1",
 *         email = "test@example.com",
 *         createdAt = Clock.System.now(),
 *         updatedAt = Clock.System.now(),
 *         deletedAt = null
 *     )
 *
 *     assertFalse(user.isDeleted())
 *     assertTrue(user.isActive())
 *
 *     val deleted = user.copy(deletedAt = Clock.System.now())
 *
 *     assertTrue(deleted.isDeleted())
 *     assertFalse(deleted.isActive())
 * }
 *
 * @Test
 * fun `entity can be restored after soft delete`() {
 *     val deleted = User(..., deletedAt = Clock.System.now())
 *     val restored = deleted.copy(deletedAt = null)
 *
 *     assertTrue(deleted.isDeleted())
 *     assertFalse(restored.isDeleted())
 * }
 * ```
 *
 * ## Consideraciones
 *
 * ### Ventajas
 * - Datos recuperables
 * - Auditoría completa
 * - Cumplimiento de regulaciones
 * - Análisis histórico
 *
 * ### Desventajas
 * - Mayor uso de almacenamiento
 * - Consultas más complejas (filtrar deletedAt != null)
 * - Necesidad de purga periódica
 *
 * ### Cuándo Usar
 * - Datos de usuarios (GDPR, compliance)
 * - Registros financieros
 * - Documentos importantes
 * - Cuando se requiere auditoría
 *
 * ### Cuándo NO Usar
 * - Datos temporales (caché, logs)
 * - Datos sensibles que deben eliminarse permanentemente
 * - Tablas con alto volumen de eliminaciones
 *
 * @property deletedAt Timestamp de cuándo fue eliminada la entidad, null si está activa
 *
 * @see EntityBase Para propiedades de entidad base
 * @see AuditableModel Para información de auditoría completa
 */
public interface SoftDeletable {
    /**
     * Timestamp de cuándo fue eliminada lógicamente la entidad.
     *
     * - **null**: La entidad está activa (no eliminada)
     * - **Instant**: La entidad fue eliminada en este momento
     *
     * Este valor debe establecerse al momento de realizar el soft delete
     * y debe ser null para entidades activas.
     *
     * Ejemplo de soft delete:
     * ```kotlin
     * val user = User(
     *     id = "user-123",
     *     email = "user@example.com",
     *     createdAt = Clock.System.now(),
     *     updatedAt = Clock.System.now(),
     *     deletedAt = null  // Usuario activo
     * )
     *
     * // Marcar como eliminado
     * val deletedUser = user.copy(
     *     deletedAt = Clock.System.now(),
     *     updatedAt = Clock.System.now()
     * )
     *
     * // Restaurar
     * val restoredUser = deletedUser.copy(
     *     deletedAt = null,
     *     updatedAt = Clock.System.now()
     * )
     * ```
     */
    val deletedAt: Instant?
}

/**
 * Extension function para verificar si una entidad está eliminada.
 *
 * @return true si deletedAt no es null, false en caso contrario
 */
public fun SoftDeletable.isDeleted(): Boolean = deletedAt != null

/**
 * Extension function para verificar si una entidad está activa (no eliminada).
 *
 * @return true si deletedAt es null, false en caso contrario
 */
public fun SoftDeletable.isActive(): Boolean = deletedAt == null

/**
 * Extension function para marcar una entidad como eliminada.
 *
 * Esta función es útil cuando se usa con data classes que implementan SoftDeletable.
 * Nota: Esta función retorna un Map con el campo a actualizar, no modifica la entidad.
 *
 * Ejemplo:
 * ```kotlin
 * val user = User(...)
 * val updates = user.markAsDeleted()
 * val deletedUser = user.copy(
 *     deletedAt = updates["deletedAt"] as Instant,
 *     updatedAt = updates["updatedAt"] as Instant
 * )
 * ```
 *
 * @return Map con los campos a actualizar (deletedAt, updatedAt)
 */
public fun SoftDeletable.markAsDeleted(): Map<String, Instant> {
    val now = kotlinx.datetime.Clock.System.now()
    return mapOf(
        "deletedAt" to now,
        "updatedAt" to now
    )
}

/**
 * Extension function para restaurar una entidad eliminada.
 *
 * Esta función retorna un Map con el campo a actualizar, no modifica la entidad.
 *
 * Ejemplo:
 * ```kotlin
 * val deletedUser = User(..., deletedAt = someTimestamp)
 * val updates = deletedUser.restore()
 * val restoredUser = deletedUser.copy(
 *     deletedAt = updates["deletedAt"] as Instant?,
 *     updatedAt = updates["updatedAt"] as Instant
 * )
 * ```
 *
 * @return Map con los campos a actualizar (deletedAt = null, updatedAt)
 */
public fun SoftDeletable.restore(): Map<String, Instant?> {
    return mapOf(
        "deletedAt" to null,
        "updatedAt" to kotlinx.datetime.Clock.System.now()
    )
}

/**
 * Extension function para filtrar solo entidades activas de una lista.
 *
 * Ejemplo:
 * ```kotlin
 * val allUsers: List<User> = getUsersFromDatabase()
 * val activeUsers = allUsers.onlyActive()
 * ```
 *
 * @return Lista conteniendo solo las entidades con deletedAt == null
 */
public fun <T : SoftDeletable> List<T>.onlyActive(): List<T> = filter { it.isActive() }

/**
 * Extension function para filtrar solo entidades eliminadas de una lista.
 *
 * Ejemplo:
 * ```kotlin
 * val allUsers: List<User> = getUsersFromDatabase()
 * val deletedUsers = allUsers.onlyDeleted()
 * ```
 *
 * @return Lista conteniendo solo las entidades con deletedAt != null
 */
public fun <T : SoftDeletable> List<T>.onlyDeleted(): List<T> = filter { it.isDeleted() }
