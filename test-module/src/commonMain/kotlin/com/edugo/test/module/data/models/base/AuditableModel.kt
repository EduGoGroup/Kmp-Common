package com.edugo.test.module.data.models.base

import kotlinx.datetime.Instant

/**
 * Interface para modelos que requieren información de auditoría extendida.
 *
 * Esta interface complementa [EntityBase] agregando información sobre **quién**
 * realizó las operaciones de creación y actualización, no solo **cuándo**.
 * Es útil para sistemas que requieren trazabilidad completa de las operaciones.
 *
 * ## Características
 *
 * - **Trazabilidad Completa**: Registra quién y cuándo modificó cada entidad
 * - **Composable**: Se combina con EntityBase y ValidatableModel
 * - **Inmutable**: Las propiedades son val para garantizar integridad
 * - **Serializable**: Compatible con kotlinx.serialization
 *
 * ## Uso Básico
 *
 * ```kotlin
 * @Serializable
 * data class Document(
 *     override val id: String,
 *     val title: String,
 *     val content: String,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant,
 *     override val createdBy: String,
 *     override val updatedBy: String
 * ) : EntityBase<String>, AuditableModel
 * ```
 *
 * ## Con Validación
 *
 * ```kotlin
 * @Serializable
 * data class AuditedUser(
 *     override val id: String,
 *     val email: String,
 *     val role: String,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant,
 *     override val createdBy: String,
 *     override val updatedBy: String
 * ) : EntityBase<String>, AuditableModel, ValidatableModel {
 *
 *     override fun validate(): Result<Unit> {
 *         return when {
 *             email.isBlank() -> failure("Email is required")
 *             createdBy.isBlank() -> failure("Creator information is required")
 *             updatedBy.isBlank() -> failure("Updater information is required")
 *             else -> success(Unit)
 *         }
 *     }
 * }
 * ```
 *
 * ## Información de Usuario
 *
 * El campo debe contener información suficiente para identificar al usuario:
 *
 * ```kotlin
 * // Opción 1: User ID
 * createdBy = "user-123"
 *
 * // Opción 2: Email
 * createdBy = "john.doe@example.com"
 *
 * // Opción 3: Username
 * createdBy = "johndoe"
 *
 * // Opción 4: Información estructurada (requiere serialización custom)
 * createdBy = "user-123|john.doe@example.com"
 * ```
 *
 * ## Actualización de Auditoría
 *
 * Al actualizar una entidad auditable, se debe actualizar updatedBy y updatedAt:
 *
 * ```kotlin
 * fun updateDocument(
 *     document: Document,
 *     newContent: String,
 *     currentUserId: String
 * ): Document {
 *     return document.copy(
 *         content = newContent,
 *         updatedAt = Clock.System.now(),
 *         updatedBy = currentUserId
 *     )
 * }
 * ```
 *
 * ## Extensión para Auditoría Detallada
 *
 * Para casos que requieren más información:
 *
 * ```kotlin
 * @Serializable
 * data class AuditInfo(
 *     val userId: String,
 *     val userName: String,
 *     val userEmail: String,
 *     val ipAddress: String? = null,
 *     val userAgent: String? = null
 * )
 *
 * @Serializable
 * data class ExtendedAuditableDocument(
 *     override val id: String,
 *     val title: String,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant,
 *     override val createdBy: String,
 *     override val updatedBy: String,
 *     val createdByInfo: AuditInfo,
 *     val updatedByInfo: AuditInfo
 * ) : EntityBase<String>, AuditableModel
 * ```
 *
 * ## Historial de Cambios
 *
 * Para mantener historial completo:
 *
 * ```kotlin
 * @Serializable
 * data class AuditEntry(
 *     val timestamp: Instant,
 *     val userId: String,
 *     val action: String,
 *     val changes: Map<String, String>
 * )
 *
 * @Serializable
 * data class FullyAuditedEntity(
 *     override val id: String,
 *     val data: String,
 *     override val createdAt: Instant,
 *     override val updatedAt: Instant,
 *     override val createdBy: String,
 *     override val updatedBy: String,
 *     val auditTrail: List<AuditEntry> = emptyList()
 * ) : EntityBase<String>, AuditableModel
 * ```
 *
 * ## Testing
 *
 * ```kotlin
 * @Test
 * fun `audit info is preserved on update`() {
 *     val original = Document(
 *         id = "doc-1",
 *         title = "Test",
 *         content = "Original",
 *         createdAt = Clock.System.now(),
 *         updatedAt = Clock.System.now(),
 *         createdBy = "user-1",
 *         updatedBy = "user-1"
 *     )
 *
 *     val updated = original.copy(
 *         content = "Updated",
 *         updatedAt = Clock.System.now(),
 *         updatedBy = "user-2"
 *     )
 *
 *     // createdBy no debe cambiar
 *     assertEquals("user-1", updated.createdBy)
 *     // updatedBy debe reflejar el nuevo usuario
 *     assertEquals("user-2", updated.updatedBy)
 * }
 * ```
 *
 * ## Consideraciones de Seguridad
 *
 * - **No exponer información sensible**: Si el userId contiene información sensible,
 *   considerar usar un identificador opaco
 * - **Validar permisos**: Verificar que el usuario tiene permisos para modificar
 * - **Proteger contra spoofing**: El createdBy/updatedBy debe ser establecido por
 *   el backend, no confiarse del cliente
 *
 * @property createdBy Identificador del usuario que creó la entidad
 * @property updatedBy Identificador del usuario que realizó la última actualización
 *
 * @see EntityBase Para propiedades de entidad base
 * @see ValidatableModel Para agregar validación
 */
interface AuditableModel {
    /**
     * Identificador del usuario que creó la entidad.
     *
     * Este valor debe establecerse una vez al crear la entidad y no debe cambiar.
     * Puede ser un user ID, email, username, o cualquier identificador único del usuario.
     *
     * Ejemplo:
     * ```kotlin
     * val document = Document(
     *     id = "doc-123",
     *     title = "Report",
     *     createdAt = Clock.System.now(),
     *     updatedAt = Clock.System.now(),
     *     createdBy = currentUser.id,  // "user-456"
     *     updatedBy = currentUser.id
     * )
     * ```
     */
    val createdBy: String

    /**
     * Identificador del usuario que realizó la última actualización.
     *
     * Este valor debe actualizarse cada vez que se modifica la entidad.
     * Al crear la entidad, debe ser igual a [createdBy].
     *
     * Ejemplo:
     * ```kotlin
     * val updated = document.copy(
     *     title = "Updated Report",
     *     updatedAt = Clock.System.now(),
     *     updatedBy = currentUser.id  // Puede ser diferente a createdBy
     * )
     * ```
     */
    val updatedBy: String
}

/**
 * Data class auxiliar para información de auditoría detallada.
 *
 * Útil cuando se necesita más contexto que solo el ID del usuario.
 *
 * ```kotlin
 * @Serializable
 * data class DetailedAuditInfo(
 *     val userId: String,
 *     val userName: String,
 *     val timestamp: Instant,
 *     val ipAddress: String? = null
 * )
 * ```
 */
@kotlinx.serialization.Serializable
data class AuditInfo(
    /**
     * Identificador único del usuario
     */
    val userId: String,

    /**
     * Nombre completo del usuario
     */
    val userName: String,

    /**
     * Timestamp de la operación
     */
    val timestamp: Instant,

    /**
     * Dirección IP desde donde se realizó la operación (opcional)
     */
    val ipAddress: String? = null,

    /**
     * User Agent del cliente (opcional)
     */
    val userAgent: String? = null
)

/**
 * Extension function para crear información de auditoría desde un usuario actual.
 *
 * Ejemplo:
 * ```kotlin
 * data class CurrentUser(val id: String, val name: String)
 *
 * val currentUser = CurrentUser("user-123", "John Doe")
 * val auditInfo = currentUser.toAuditInfo()
 * ```
 */
fun createAuditInfo(
    userId: String,
    userName: String,
    ipAddress: String? = null,
    userAgent: String? = null
): AuditInfo {
    return AuditInfo(
        userId = userId,
        userName = userName,
        timestamp = kotlinx.datetime.Clock.System.now(),
        ipAddress = ipAddress,
        userAgent = userAgent
    )
}
