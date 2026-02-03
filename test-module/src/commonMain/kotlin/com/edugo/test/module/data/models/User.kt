package com.edugo.test.module.data.models

import com.edugo.test.module.core.Result
import com.edugo.test.module.core.failure
import com.edugo.test.module.core.success
import com.edugo.test.module.data.models.base.AuditableModel
import com.edugo.test.module.data.models.base.EntityBase
import com.edugo.test.module.data.models.base.SoftDeletable
import com.edugo.test.module.data.models.base.ValidatableModel
import com.edugo.test.module.data.models.base.isDeleted
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo que representa un usuario del sistema.
 *
 * Este modelo demuestra el uso completo de múltiples interfaces base para
 * crear una entidad rica con validación, auditoría y soft delete, todo sin
 * duplicación de código.
 *
 * ## Composición de Interfaces
 *
 * Este modelo implementa:
 * - [EntityBase]<String>: Proporciona id, createdAt, updatedAt
 * - [ValidatableModel]: Proporciona validación con Result<Unit>
 * - [AuditableModel]: Proporciona createdBy, updatedBy para trazabilidad
 * - [SoftDeletable]: Proporciona deletedAt para eliminación lógica
 *
 * Esta composición demuestra el poder de "composition over inheritance"
 * donde el modelo obtiene múltiples capacidades sin jerarquías complejas.
 *
 * ## Serialización y Compatibilidad con Backend
 *
 * Este modelo usa `@SerialName` para mapear entre convenciones de nomenclatura:
 * - Kotlin: camelCase (`displayName`, `isActive`, `createdAt`, `profileImageUrl`)
 * - Backend/JSON: snake_case (`display_name`, `is_active`, `created_at`, `profile_image_url`)
 *
 * ### Relación con Role (DECISIÓN DE DISEÑO)
 *
 * La propiedad `roles` es de tipo `List<Role>` en lugar de `List<String>` por:
 *
 * 1. **Escalabilidad**: Permite acceso a propiedades del rol (name, permissions) sin llamadas adicionales
 * 2. **Type-safety**: Compilador garantiza que solo objetos Role válidos estén en la lista
 * 3. **Sin desincronización**: No hay conversión Role ↔ String, evita inconsistencias
 * 4. **Serialización flexible**: Backend puede enviar Role completo o solo RoleMinimal (id, name)
 *
 * Gracias a que Role implementa RoleMinimal con propiedades default, el backend puede
 * enviar versiones ligeras:
 *
 * ```json
 * {
 *   "id": "user-123",
 *   "email": "john@example.com",
 *   "roles": [
 *     {"id": "role-admin", "name": "Administrator"}
 *   ]
 * }
 * ```
 *
 * Y Role se deserializará correctamente con valores default para propiedades opcionales.
 *
 * ## Ejemplo de Uso Básico
 *
 * ```kotlin
 * val user = User(
 *     id = "user-123",
 *     email = "john.doe@example.com",
 *     username = "johndoe",
 *     displayName = "John Doe",
 *     roles = listOf(
 *         Role(id = "role-user", name = "User")
 *     ),
 *     isActive = true,
 *     createdAt = Clock.System.now(),
 *     updatedAt = Clock.System.now(),
 *     createdBy = "system",
 *     updatedBy = "system"
 * )
 *
 * // Validar antes de guardar
 * when (val result = user.validate()) {
 *     is Result.Success -> repository.save(user)
 *     is Result.Failure -> println("Invalid user: ${result.error}")
 * }
 * ```
 *
 * ## Ejemplo con Roles y Permisos
 *
 * ```kotlin
 * // Verificar permisos directamente desde User
 * if (user.roles.any { it.hasPermission("users.write") }) {
 *     // Usuario tiene permiso para escribir usuarios
 * }
 *
 * // Verificar por nombre de rol
 * if (user.hasRole("role-admin")) {
 *     // Usuario es administrador
 * }
 * ```
 *
 * ## Soft Delete
 *
 * ```kotlin
 * // Marcar como eliminado (soft delete)
 * val deleted = user.copy(deletedAt = Clock.System.now())
 *
 * // Verificar estado
 * if (user.isDeleted()) {
 *     println("User was deleted")
 * }
 * ```
 *
 * @property id Identificador único del usuario
 * @property email Correo electrónico (debe ser único y válido)
 * @property username Nombre de usuario (debe ser único)
 * @property displayName Nombre para mostrar en la UI
 * @property roles Lista de roles asignados al usuario (objetos Role completos)
 * @property isActive Indica si la cuenta está activa
 * @property profileImageUrl URL opcional de la imagen de perfil
 * @property metadata Map opcional para datos adicionales personalizados
 * @property createdAt Timestamp de creación (default: Clock.System.now())
 * @property updatedAt Timestamp de última actualización (default: Clock.System.now())
 * @property createdBy ID del usuario que creó esta cuenta (default: "system")
 * @property updatedBy ID del usuario que realizó la última actualización (default: "system")
 * @property deletedAt Timestamp de eliminación (null si no está eliminado)
 */
@Serializable
public data class User(
    @SerialName("id")
    override val id: String,

    @SerialName("email")
    val email: String,

    @SerialName("username")
    val username: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("roles")
    val roles: List<Role> = emptyList(),

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("profile_image_url")
    val profileImageUrl: String? = null,

    @SerialName("metadata")
    val metadata: Map<String, String> = emptyMap(),

    @SerialName("created_at")
    override val createdAt: Instant = Clock.System.now(),

    @SerialName("updated_at")
    override val updatedAt: Instant = Clock.System.now(),

    @SerialName("created_by")
    override val createdBy: String = "system",

    @SerialName("updated_by")
    override val updatedBy: String = "system",

    @SerialName("deleted_at")
    override val deletedAt: Instant? = null
) : EntityBase<String>, ValidatableModel, AuditableModel, SoftDeletable {

    /**
     * Valida la consistencia e integridad de los datos del usuario.
     *
     * ## Reglas de Validación
     *
     * 1. **ID**: No puede estar vacío
     * 2. **Email**: Debe ser un email válido (contiene @ y .)
     * 3. **Username**:
     *    - No puede estar vacío
     *    - Debe tener entre 3 y 30 caracteres
     *    - Solo puede contener letras, números, guiones y guiones bajos
     * 4. **DisplayName**:
     *    - No puede estar vacío
     *    - Debe tener máximo 100 caracteres
     * 5. **Roles**: No puede estar vacía (al menos debe tener un rol)
     * 6. **Auditoría**: createdBy y updatedBy no pueden estar vacíos
     *
     * ## Ejemplos
     *
     * ```kotlin
     * // Usuario válido
     * val valid = User(
     *     id = "user-1",
     *     email = "john@example.com",
     *     username = "johndoe",
     *     displayName = "John Doe",
     *     roles = listOf(Role(id = "role-user", name = "User"))
     * )
     * valid.validate() // Result.Success
     *
     * // Usuario inválido - email inválido
     * val invalid = valid.copy(email = "not-an-email")
     * invalid.validate() // Result.Failure("Email must be valid...")
     * ```
     *
     * @return [Result.Success] si todas las validaciones pasan,
     *         [Result.Failure] con mensaje descriptivo si alguna falla
     */
    override fun validate(): Result<Unit> {
        return when {
            id.isBlank() ->
                failure("User ID cannot be blank")

            email.isBlank() ->
                failure("Email cannot be blank")

            !isValidEmail(email) ->
                failure("Email must be valid (contain @ and a domain)")

            username.isBlank() ->
                failure("Username cannot be blank")

            username.length < 3 ->
                failure("Username must be at least 3 characters")

            username.length > 30 ->
                failure("Username is too long (max 30 characters)")

            !isValidUsername(username) ->
                failure("Username can only contain letters, numbers, hyphens and underscores")

            displayName.isBlank() ->
                failure("Display name cannot be blank")

            displayName.length > 100 ->
                failure("Display name is too long (max 100 characters)")

            roles.isEmpty() ->
                failure("User must have at least one role")

            createdBy.isBlank() ->
                failure("Created by cannot be blank (audit requirement)")

            updatedBy.isBlank() ->
                failure("Updated by cannot be blank (audit requirement)")

            profileImageUrl != null && !isValidUrl(profileImageUrl) ->
                failure("Profile image URL must be a valid URL")

            else ->
                success(Unit)
        }
    }

    /**
     * Valida que un email tenga formato básico correcto.
     *
     * Esta es una validación simple que verifica:
     * - Contiene exactamente un @
     * - Tiene texto antes y después del @
     * - Tiene al menos un punto después del @
     *
     * Para validación de email más estricta, usar una librería especializada.
     */
    private fun isValidEmail(email: String): Boolean {
        val parts = email.split("@")
        if (parts.size != 2) return false
        val (local, domain) = parts
        return local.isNotBlank() && domain.contains(".") && domain.length > 3
    }

    /**
     * Valida que el username solo contenga caracteres permitidos.
     *
     * Caracteres permitidos:
     * - Letras (a-z, A-Z)
     * - Números (0-9)
     * - Guión (-)
     * - Guión bajo (_)
     */
    private fun isValidUsername(username: String): Boolean {
        return username.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    }

    /**
     * Valida que una URL tenga formato básico correcto.
     */
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    /**
     * Verifica si el usuario tiene un rol específico asignado.
     *
     * DECISIÓN DE DISEÑO: Se compara por role.id para mantener compatibilidad
     * con código existente que usaba roleId strings.
     *
     * Ejemplo:
     * ```kotlin
     * if (user.hasRole("role-admin")) {
     *     // Usuario es administrador
     * }
     * ```
     */
    public fun hasRole(roleId: String): Boolean {
        return roles.any { it.id == roleId }
    }

    /**
     * Verifica si el usuario tiene alguno de los roles especificados.
     *
     * Ejemplo:
     * ```kotlin
     * if (user.hasAnyRole("role-admin", "role-moderator")) {
     *     // Usuario tiene permisos elevados
     * }
     * ```
     */
    public fun hasAnyRole(vararg roleIds: String): Boolean {
        return roleIds.any { roleId -> roles.any { it.id == roleId } }
    }

    /**
     * Verifica si el usuario tiene todos los roles especificados.
     *
     * Ejemplo:
     * ```kotlin
     * if (user.hasAllRoles("role-user", "role-premium")) {
     *     // Usuario es premium
     * }
     * ```
     */
    public fun hasAllRoles(vararg roleIds: String): Boolean {
        return roleIds.all { roleId -> roles.any { it.id == roleId } }
    }

    /**
     * Verifica si el usuario puede ser modificado.
     *
     * Un usuario no puede ser modificado si:
     * - Está eliminado (soft delete)
     * - Está inactivo
     */
    public fun canBeModified(): Boolean {
        return isActive && !isDeleted()
    }

    /**
     * Obtiene un resumen corto del usuario para logging.
     *
     * Ejemplo: "User(id=user-123, username=johndoe, email=john@example.com)"
     */
    public fun toLogString(): String {
        return "User(id=$id, username=$username, email=$email)"
    }

    companion object {
        /**
         * IDs de roles comunes para facilitar referencias.
         */
        public object RoleIds {
            public const val ADMIN = "role-admin"
            public const val USER = "role-user"
            public const val GUEST = "role-guest"
            public const val MODERATOR = "role-moderator"
        }

        /**
         * Crea un usuario administrador de ejemplo.
         *
         * DECISIÓN DE DISEÑO: Usa Role.createAdminRole() del companion object
         * de Role para crear el rol completo con todos sus permisos.
         *
         * Útil para tests y datos de seed.
         */
        public fun createAdmin(
            id: String,
            email: String,
            username: String,
            createdAt: Instant,
            updatedAt: Instant,
            createdBy: String = "system"
        ): User {
            return User(
                id = id,
                email = email,
                username = username,
                displayName = username.capitalize(),
                roles = listOf(
                    Role.createAdminRole(createdAt, updatedAt)
                ),
                isActive = true,
                createdAt = createdAt,
                updatedAt = updatedAt,
                createdBy = createdBy,
                updatedBy = createdBy,
                deletedAt = null
            )
        }

        /**
         * Crea un usuario estándar de ejemplo.
         *
         * DECISIÓN DE DISEÑO: Usa Role.createUserRole() del companion object
         * de Role para crear el rol estándar con permisos básicos.
         *
         * Útil para tests y datos de seed.
         */
        public fun createStandardUser(
            id: String,
            email: String,
            username: String,
            createdAt: Instant,
            updatedAt: Instant,
            createdBy: String = "system"
        ): User {
            return User(
                id = id,
                email = email,
                username = username,
                displayName = username.capitalize(),
                roles = listOf(
                    Role.createUserRole(createdAt, updatedAt)
                ),
                isActive = true,
                createdAt = createdAt,
                updatedAt = updatedAt,
                createdBy = createdBy,
                updatedBy = createdBy,
                deletedAt = null
            )
        }
    }
}

/**
 * Extension function para capitalizar la primera letra de un String.
 */
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
