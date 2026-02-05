package com.edugo.test.module.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo que representa la información del usuario autenticado.
 *
 * Este modelo encapsula los datos del usuario que retorna el backend después
 * de una autenticación exitosa. Contiene información básica del perfil del
 * usuario y su rol en el sistema.
 *
 * ## Serialización y Compatibilidad con Backend
 *
 * Este modelo usa `@SerialName` para mapear entre convenciones de nomenclatura:
 * - Kotlin: camelCase (`firstName`, `lastName`, `fullName`, `schoolId`)
 * - Backend/JSON: snake_case (`first_name`, `last_name`, `full_name`, `school_id`)
 *
 * ### Formato JSON del Backend
 *
 * ```json
 * {
 *   "id": "user-123",
 *   "email": "john.doe@edugo.com",
 *   "first_name": "John",
 *   "last_name": "Doe",
 *   "full_name": "John Doe",
 *   "role": "student",
 *   "school_id": "school-456"
 * }
 * ```
 *
 * ## Roles Soportados
 *
 * El campo `role` puede contener:
 * - `"student"`: Estudiante
 * - `"teacher"`: Profesor
 * - `"admin"`: Administrador
 * - `"parent"`: Padre/Tutor
 *
 * ## Ejemplo de Uso Básico
 *
 * ```kotlin
 * // Recibido del backend después de login
 * val userInfo = AuthUserInfo(
 *     id = "user-123",
 *     email = "john.doe@edugo.com",
 *     firstName = "John",
 *     lastName = "Doe",
 *     fullName = "John Doe",
 *     role = "student",
 *     schoolId = "school-456"
 * )
 *
 * // Usar en UI
 * println("Welcome, ${userInfo.fullName}")
 * println("Role: ${userInfo.role}")
 * ```
 *
 * ## Uso con Storage
 *
 * ```kotlin
 * // Guardar en storage
 * val json = Json.encodeToString(AuthUserInfo.serializer(), userInfo)
 * storage.putString("auth_user", json)
 *
 * // Recuperar de storage
 * val storedJson = storage.getString("auth_user")
 * val userInfo = Json.decodeFromString<AuthUserInfo>(storedJson)
 * ```
 *
 * ## Uso con StateFlow (UI Reactiva)
 *
 * ```kotlin
 * // En ViewModel
 * private val _currentUser = MutableStateFlow<AuthUserInfo?>(null)
 * val currentUser: StateFlow<AuthUserInfo?> = _currentUser.asStateFlow()
 *
 * fun onLoginSuccess(userInfo: AuthUserInfo) {
 *     _currentUser.value = userInfo
 * }
 *
 * // En Composable
 * val user by viewModel.currentUser.collectAsState()
 * user?.let {
 *     Text("Welcome, ${it.fullName}")
 * }
 * ```
 *
 * @property id Identificador único del usuario en el sistema
 * @property email Correo electrónico del usuario
 * @property firstName Nombre del usuario
 * @property lastName Apellido del usuario
 * @property fullName Nombre completo del usuario (concatenación de firstName y lastName)
 * @property role Rol del usuario en el sistema (student, teacher, admin, parent)
 * @property schoolId Identificador de la escuela a la que pertenece el usuario (nullable)
 */
@Serializable
public data class AuthUserInfo(
    @SerialName("id")
    val id: String,

    @SerialName("email")
    val email: String,

    @SerialName("first_name")
    val firstName: String,

    @SerialName("last_name")
    val lastName: String,

    @SerialName("full_name")
    val fullName: String,

    @SerialName("role")
    val role: String,

    @SerialName("school_id")
    val schoolId: String? = null
) {

    /**
     * Verifica si el usuario tiene un rol específico.
     *
     * Útil para control de acceso y permisos en la UI.
     *
     * ```kotlin
     * if (userInfo.hasRole("admin")) {
     *     showAdminPanel()
     * }
     * ```
     *
     * @param roleName Nombre del rol a verificar
     * @return true si el usuario tiene ese rol
     */
    public fun hasRole(roleName: String): Boolean {
        return role.equals(roleName, ignoreCase = true)
    }

    /**
     * Verifica si el usuario es estudiante.
     */
    public fun isStudent(): Boolean = hasRole("student")

    /**
     * Verifica si el usuario es profesor.
     */
    public fun isTeacher(): Boolean = hasRole("teacher")

    /**
     * Verifica si el usuario es administrador.
     */
    public fun isAdmin(): Boolean = hasRole("admin")

    /**
     * Verifica si el usuario es padre/tutor.
     */
    public fun isParent(): Boolean = hasRole("parent")

    /**
     * Verifica si el usuario está asociado a una escuela.
     */
    public fun hasSchool(): Boolean = !schoolId.isNullOrBlank()

    /**
     * Obtiene las iniciales del usuario.
     *
     * Útil para mostrar avatares con iniciales.
     *
     * Ejemplo:
     * ```kotlin
     * val userInfo = AuthUserInfo(..., firstName = "John", lastName = "Doe", ...)
     * println(userInfo.getInitials()) // "JD"
     * ```
     *
     * @return Iniciales del usuario (máximo 2 caracteres)
     */
    public fun getInitials(): String {
        val first = firstName.firstOrNull()?.uppercase() ?: ""
        val last = lastName.firstOrNull()?.uppercase() ?: ""
        return "$first$last"
    }

    /**
     * Obtiene el nombre para mostrar en UI.
     *
     * Prioriza fullName si está disponible, sino concatena firstName y lastName.
     *
     * @return Nombre para mostrar
     */
    public fun getDisplayName(): String {
        return when {
            fullName.isNotBlank() -> fullName
            firstName.isNotBlank() && lastName.isNotBlank() -> "$firstName $lastName"
            firstName.isNotBlank() -> firstName
            lastName.isNotBlank() -> lastName
            else -> email
        }
    }

    /**
     * Obtiene una representación segura para logging.
     *
     * Oculta información sensible pero mantiene datos útiles para debugging.
     *
     * Ejemplo: "AuthUserInfo(id=user-123, email=john.doe@edugo.com, role=student)"
     */
    public fun toLogString(): String {
        return "AuthUserInfo(id=$id, email=$email, role=$role, hasSchool=${hasSchool()})"
    }

    companion object {
        /**
         * Roles disponibles en el sistema.
         */
        public object Roles {
            public const val STUDENT: String = "student"
            public const val TEACHER: String = "teacher"
            public const val ADMIN: String = "admin"
            public const val PARENT: String = "parent"
        }

        /**
         * Crea un usuario de ejemplo para tests.
         *
         * **IMPORTANTE**: Solo usar en tests, nunca en producción.
         *
         * @param id ID del usuario (default: test-user-123)
         * @param email Email del usuario (default: test@edugo.com)
         * @param firstName Nombre (default: Test)
         * @param lastName Apellido (default: User)
         * @param role Rol (default: student)
         * @param schoolId ID de escuela (default: test-school-456)
         */
        public fun createTestUser(
            id: String = "test-user-123",
            email: String = "test@edugo.com",
            firstName: String = "Test",
            lastName: String = "User",
            role: String = Roles.STUDENT,
            schoolId: String? = "test-school-456"
        ): AuthUserInfo {
            return AuthUserInfo(
                id = id,
                email = email,
                firstName = firstName,
                lastName = lastName,
                fullName = "$firstName $lastName",
                role = role,
                schoolId = schoolId
            )
        }
    }
}
