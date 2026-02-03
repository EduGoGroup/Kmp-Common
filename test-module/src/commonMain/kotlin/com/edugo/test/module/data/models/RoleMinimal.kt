package com.edugo.test.module.data.models

/**
 * Interface que define las propiedades mínimas de un rol.
 *
 * Esta interface establece el **contrato mínimo** que debe cumplir cualquier
 * representación de un rol en el sistema. Permite que diferentes implementaciones
 * (versiones completas o ligeras) compartan una interfaz común.
 *
 * ## Propósito
 *
 * El objetivo principal de esta interface es permitir que modelos como [User]
 * puedan contener información de roles sin necesidad de cargar el modelo completo
 * de [Role] con todas sus propiedades de auditoría y metadata.
 *
 * ## Diseño de Escalabilidad
 *
 * Esta interface fue diseñada para crecer en el futuro:
 * - **Mínimo actual**: Solo `id` y `name` (identificación básica)
 * - **Extensión futura**: Se pueden agregar propiedades adicionales
 *   (ej: `permissions`, `isActive`) y todas las implementaciones
 *   deberán incluirlas automáticamente
 *
 * ## Ejemplo de Uso
 *
 * ```kotlin
 * // Role completo implementa RoleMinimal
 * val fullRole: Role = Role(
 *     id = "role-admin",
 *     name = "Administrator",
 *     description = "Full access",
 *     permissions = setOf("users.read", "users.write"),
 *     // ... más propiedades
 * )
 *
 * // Usar como RoleMinimal (solo propiedades mínimas)
 * val minimal: RoleMinimal = fullRole
 * println("${minimal.id}: ${minimal.name}")
 * ```
 *
 * ## Serialización
 *
 * Cuando [Role] se serializa en contextos donde solo se necesitan
 * las propiedades mínimas (como en [User.roles]), el backend puede
 * enviar solo `id` y `name`:
 *
 * ```json
 * {
 *   "id": "role-admin",
 *   "name": "Administrator"
 * }
 * ```
 *
 * [Role] se deserializará correctamente usando valores default para
 * las propiedades adicionales.
 *
 * ## Decisión de Diseño
 *
 * **¿Por qué no crear una clase separada `RoleInfo`?**
 *
 * Se consideró crear una clase `RoleInfo` ligera, pero se descartó por:
 * - **Desincronización**: Dos clases representando el mismo concepto
 * - **Duplicación**: Mantener dos implementaciones en paralelo
 * - **Complejidad**: Conversiones manuales `Role.toRoleInfo()`
 *
 * La solución actual (interface + implementación única) evita estos problemas:
 * - **Una sola fuente de verdad**: Solo existe la clase [Role]
 * - **Sin desincronización**: Cambios en [Role] automáticamente cumplen el contrato
 * - **Sin conversiones**: [Role] implementa directamente [RoleMinimal]
 *
 * @property id Identificador único del rol
 * @property name Nombre del rol (ej: "Administrator", "User", "Guest")
 *
 * @see Role Implementación completa del modelo de rol
 * @see User Usa List<Role> donde Role implementa esta interface
 */
public interface RoleMinimal {
    /**
     * Identificador único del rol.
     *
     * Formato recomendado: "role-{name}" (ej: "role-admin", "role-user")
     */
    public val id: String

    /**
     * Nombre legible del rol para mostrar en UI.
     *
     * Ejemplos: "Administrator", "User", "Guest", "Moderator"
     */
    public val name: String
}
