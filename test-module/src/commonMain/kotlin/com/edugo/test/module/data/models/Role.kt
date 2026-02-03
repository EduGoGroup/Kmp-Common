package com.edugo.test.module.data.models

import com.edugo.test.module.core.Result
import com.edugo.test.module.core.failure
import com.edugo.test.module.core.success
import com.edugo.test.module.data.models.base.EntityBase
import com.edugo.test.module.data.models.base.ValidatableModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo que representa un rol o permiso en el sistema.
 *
 * Un Role define un conjunto de permisos que pueden ser asignados a usuarios.
 * Este modelo demuestra el uso de interfaces base para crear entidades
 * validables y serializables con mínima duplicación de código.
 *
 * ## Composición de Interfaces
 *
 * Este modelo implementa:
 * - [EntityBase]<String>: Proporciona id, createdAt, updatedAt
 * - [ValidatableModel]: Proporciona método validate() con Result<Unit>
 * - [RoleMinimal]: Proporciona contrato mínimo (id, name) para uso ligero
 *
 * ## Serialización y Compatibilidad con Backend
 *
 * Este modelo usa `@SerialName` para mapear entre convenciones de nomenclatura:
 * - Kotlin: camelCase (`createdAt`, `updatedAt`, `isActive`)
 * - Backend/JSON: snake_case (`created_at`, `updated_at`, `is_active`)
 *
 * ### Serialización Parcial (Modo Ligero)
 *
 * Cuando Role viaja dentro de [User.roles], el backend puede enviar solo
 * las propiedades mínimas de [RoleMinimal]:
 *
 * ```json
 * {
 *   "id": "role-admin",
 *   "name": "Administrator"
 * }
 * ```
 *
 * Role se deserializará correctamente usando valores default para las propiedades opcionales:
 * - `description = ""`
 * - `permissions = emptySet()`
 * - `isActive = true`
 * - `createdAt/updatedAt = Clock.System.now()`
 *
 * ### Serialización Completa
 *
 * Cuando se trabaja directamente con roles, el JSON incluye todas las propiedades:
 *
 * ```json
 * {
 *   "id": "role-admin",
 *   "name": "Administrator",
 *   "description": "Full system access",
 *   "permissions": ["users.read", "users.write"],
 *   "is_active": true,
 *   "created_at": "2024-01-01T00:00:00Z",
 *   "updated_at": "2024-01-01T00:00:00Z"
 * }
 * ```
 *
 * ## Ejemplo de Uso
 *
 * ```kotlin
 * val adminRole = Role(
 *     id = "role-admin",
 *     name = "Administrator",
 *     description = "Full system access",
 *     permissions = setOf("users.read", "users.write", "system.manage"),
 *     createdAt = Clock.System.now(),
 *     updatedAt = Clock.System.now()
 * )
 *
 * // Validar antes de guardar
 * when (val result = adminRole.validate()) {
 *     is Result.Success -> repository.save(adminRole)
 *     is Result.Failure -> println("Invalid role: ${result.error}")
 * }
 * ```
 *
 * ## Serialización
 *
 * ```kotlin
 * // Serializar a JSON
 * val json = Json.encodeToString(adminRole)
 *
 * // Deserializar desde JSON
 * val restored = Json.decodeFromString<Role>(json)
 * ```
 *
 * ## Decisión de Diseño: ¿Por qué implementa RoleMinimal?
 *
 * Se decidió que Role implemente [RoleMinimal] en lugar de crear una clase
 * separada `RoleInfo` por las siguientes razones:
 *
 * 1. **Sin desincronización**: Una sola fuente de verdad
 * 2. **Sin duplicación**: No hay dos clases representando el mismo concepto
 * 3. **Sin conversiones manuales**: Role ya cumple el contrato de RoleMinimal
 * 4. **Serialización flexible**: El backend decide qué propiedades enviar
 * 5. **Type-safe**: El compilador garantiza compatibilidad
 *
 * @property id Identificador único del rol (formato recomendado: "role-{name}")
 * @property name Nombre del rol (ej: "Administrator", "User", "Guest")
 * @property description Descripción detallada del propósito del rol (default: "")
 * @property permissions Set de identificadores de permisos (ej: "users.read", "posts.write")
 * @property isActive Indica si el rol está activo y puede ser asignado
 * @property createdAt Timestamp de creación del rol (default: Clock.System.now())
 * @property updatedAt Timestamp de última actualización (default: Clock.System.now())
 *
 * @see RoleMinimal Interface que define el contrato mínimo
 * @see User Usa List<Role> aprovechando serialización parcial
 */
@Serializable
public data class Role(
    @SerialName("id")
    override val id: String,

    @SerialName("name")
    override val name: String,

    @SerialName("description")
    val description: String = "",

    @SerialName("permissions")
    val permissions: Set<String> = emptySet(),

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("created_at")
    override val createdAt: Instant = Clock.System.now(),

    @SerialName("updated_at")
    override val updatedAt: Instant = Clock.System.now()
) : EntityBase<String>, ValidatableModel, RoleMinimal {

    /**
     * Valida la consistencia e integridad de los datos del rol.
     *
     * ## Reglas de Validación
     *
     * 1. **ID**: No puede estar vacío
     * 2. **Name**: No puede estar vacío y debe tener máximo 100 caracteres
     * 3. **Description**: Si no está vacía, debe tener máximo 500 caracteres
     * 4. **Permissions**: Cada permiso debe seguir el formato "resource.action"
     *
     * **Nota**: A diferencia de versiones anteriores, `description` puede estar vacía
     * para soportar serialización parcial cuando Role viaja en User.roles.
     *
     * ## Ejemplos
     *
     * ```kotlin
     * // Rol válido completo
     * val valid = Role(
     *     id = "role-user",
     *     name = "User",
     *     description = "Standard user role",
     *     permissions = setOf("posts.read", "comments.write"),
     *     createdAt = Clock.System.now(),
     *     updatedAt = Clock.System.now()
     * )
     * valid.validate() // Result.Success
     *
     * // Rol válido mínimo (para uso en User.roles)
     * val minimal = Role(
     *     id = "role-user",
     *     name = "User"
     * )
     * minimal.validate() // Result.Success (description vacía es permitida)
     *
     * // Rol inválido - nombre vacío
     * val invalid = valid.copy(name = "")
     * invalid.validate() // Result.Failure("Role name cannot be blank")
     * ```
     *
     * @return [Result.Success] si todas las validaciones pasan,
     *         [Result.Failure] con mensaje descriptivo si alguna falla
     */
    override fun validate(): Result<Unit> {
        return when {
            id.isBlank() ->
                failure("Role ID cannot be blank")

            name.isBlank() ->
                failure("Role name cannot be blank")

            name.length > 100 ->
                failure("Role name is too long (max 100 characters)")

            description.length > 500 ->
                failure("Role description is too long (max 500 characters)")

            permissions.any { !isValidPermission(it) } ->
                failure("All permissions must follow 'resource.action' format")

            else ->
                success(Unit)
        }
    }

    /**
     * Valida que un permiso siga el formato correcto.
     *
     * Formato esperado: "resource.action"
     * - resource: nombre del recurso (ej: "users", "posts")
     * - action: acción sobre el recurso (ej: "read", "write", "delete")
     *
     * Ejemplos válidos:
     * - "users.read"
     * - "posts.write"
     * - "system.manage"
     * - "comments.delete"
     *
     * @param permission String a validar
     * @return true si el formato es válido
     */
    private fun isValidPermission(permission: String): Boolean {
        val parts = permission.split(".")
        return parts.size == 2 && parts.all { it.isNotBlank() }
    }

    /**
     * Verifica si este rol tiene un permiso específico.
     *
     * Ejemplo:
     * ```kotlin
     * val role = Role(...)
     * if (role.hasPermission("users.write")) {
     *     // Permitir escritura de usuarios
     * }
     * ```
     */
    public fun hasPermission(permission: String): Boolean {
        return permission in permissions
    }

    /**
     * Verifica si este rol tiene todos los permisos especificados.
     *
     * Ejemplo:
     * ```kotlin
     * if (role.hasAllPermissions("users.read", "users.write")) {
     *     // El rol puede leer y escribir usuarios
     * }
     * ```
     */
    public fun hasAllPermissions(vararg perms: String): Boolean {
        return perms.all { it in permissions }
    }

    /**
     * Verifica si este rol tiene al menos uno de los permisos especificados.
     *
     * Ejemplo:
     * ```kotlin
     * if (role.hasAnyPermission("users.read", "posts.read")) {
     *     // El rol puede leer al menos usuarios o posts
     * }
     * ```
     */
    public fun hasAnyPermission(vararg perms: String): Boolean {
        return perms.any { it in permissions }
    }

    companion object {
        /**
         * Permisos comunes predefinidos.
         */
        public object Permissions {
            // User management
            public const val USERS_READ = "users.read"
            public const val USERS_WRITE = "users.write"
            public const val USERS_DELETE = "users.delete"

            // Role management
            public const val ROLES_READ = "roles.read"
            public const val ROLES_WRITE = "roles.write"
            public const val ROLES_DELETE = "roles.delete"

            // System
            public const val SYSTEM_MANAGE = "system.manage"
            public const val SYSTEM_CONFIGURE = "system.configure"
        }

        /**
         * Roles predefinidos comunes.
         */
        public fun createAdminRole(createdAt: Instant, updatedAt: Instant): Role {
            return Role(
                id = "role-admin",
                name = "Administrator",
                description = "Full system access with all permissions",
                permissions = setOf(
                    Permissions.USERS_READ,
                    Permissions.USERS_WRITE,
                    Permissions.USERS_DELETE,
                    Permissions.ROLES_READ,
                    Permissions.ROLES_WRITE,
                    Permissions.ROLES_DELETE,
                    Permissions.SYSTEM_MANAGE,
                    Permissions.SYSTEM_CONFIGURE
                ),
                isActive = true,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }

        public fun createUserRole(createdAt: Instant, updatedAt: Instant): Role {
            return Role(
                id = "role-user",
                name = "User",
                description = "Standard user with basic permissions",
                permissions = setOf(
                    Permissions.USERS_READ
                ),
                isActive = true,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }

        public fun createGuestRole(createdAt: Instant, updatedAt: Instant): Role {
            return Role(
                id = "role-guest",
                name = "Guest",
                description = "Guest user with minimal permissions",
                permissions = emptySet(),
                isActive = true,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }
}
