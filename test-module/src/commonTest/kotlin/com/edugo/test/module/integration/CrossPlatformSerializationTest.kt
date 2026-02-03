package com.edugo.test.module.integration

import com.edugo.test.module.data.models.AuthToken
import com.edugo.test.module.data.models.Role
import com.edugo.test.module.data.models.User
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.hours

/**
 * Tests de integración cross-platform para serialización JSON.
 *
 * PROPÓSITO:
 * Verificar que los modelos User, Role y AuthToken se serializan y deserializan
 * consistentemente en todas las plataformas target (JVM, Android, iOS).
 *
 * ESTRATEGIA:
 * - Tests corren en commonTest, por lo tanto se ejecutan en todas las plataformas
 * - Validar serialización round-trip preserva todos los datos
 * - Validar compatibilidad con JSON del backend (snake_case)
 * - Validar edge cases: colecciones vacías, nullable fields, Instant boundaries
 * - Validar schema evolution: deserialización con campos extra/faltantes
 *
 * DECISION DE DISEÑO:
 * No verificamos "byte-identical" JSON porque el orden de propiedades puede variar
 * entre implementaciones de Json. En su lugar, verificamos equivalencia estructural:
 * deserializar → comparar objetos resultantes.
 */
class CrossPlatformSerializationTest {

    // Configuración JSON para tests
    private val json = Json {
        ignoreUnknownKeys = true  // Schema evolution: ignorar campos extras del backend
        prettyPrint = false       // JSON compacto para tests
    }

    // ========== Test 1: Role - Serialización Cross-Platform ==========

    @Test
    fun `Role serializa y deserializa consistentemente en todas las plataformas`() {
        val originalRole = Role(
            id = "role-admin",
            name = "Administrator",
            description = "Full system access",
            permissions = setOf("users.read", "users.write", "system.manage"),
            isActive = true,
            createdAt = Instant.parse("2024-01-01T12:00:00Z"),
            updatedAt = Instant.parse("2024-01-01T12:00:00Z")
        )

        // Serializar a JSON
        val jsonString = json.encodeToString(originalRole)

        // Verificar que contiene claves en snake_case (compatibilidad backend)
        assertTrue(jsonString.contains("\"id\""), "JSON debe contener 'id'")
        assertTrue(jsonString.contains("\"name\""), "JSON debe contener 'name'")
        assertTrue(jsonString.contains("\"created_at\""), "JSON debe usar snake_case: 'created_at'")
        assertTrue(jsonString.contains("\"updated_at\""), "JSON debe usar snake_case: 'updated_at'")

        // Deserializar de vuelta
        val deserializedRole = json.decodeFromString<Role>(jsonString)

        // Verificar equivalencia estructural (round-trip)
        assertEquals(originalRole.id, deserializedRole.id, "id debe preservarse")
        assertEquals(originalRole.name, deserializedRole.name, "name debe preservarse")
        assertEquals(originalRole.description, deserializedRole.description, "description debe preservarse")
        assertEquals(originalRole.permissions, deserializedRole.permissions, "permissions debe preservarse")
        assertEquals(originalRole.isActive, deserializedRole.isActive, "isActive debe preservarse")
        assertEquals(originalRole.createdAt, deserializedRole.createdAt, "createdAt debe preservarse")
        assertEquals(originalRole.updatedAt, deserializedRole.updatedAt, "updatedAt debe preservarse")
    }

    @Test
    fun `Role mínimo (RoleMinimal) deserializa correctamente en todas las plataformas`() {
        // Simular JSON del backend con solo propiedades mínimas
        val minimalJson = """{"id":"role-user","name":"User"}"""

        val role = json.decodeFromString<Role>(minimalJson)

        assertEquals("role-user", role.id)
        assertEquals("User", role.name)
        assertEquals("", role.description, "description debe tener valor default vacío")
        assertEquals(emptySet(), role.permissions, "permissions debe tener Set vacío por default")
        assertTrue(role.isActive, "isActive debe ser true por default")
    }

    // ========== Test 2: User - Serialización Cross-Platform con Relaciones ==========

    @Test
    fun `User con múltiples roles serializa y deserializa consistentemente`() {
        val originalUser = User(
            id = "user-123",
            email = "john@example.com",
            username = "johndoe",
            displayName = "John Doe",
            roles = listOf(
                Role(id = "role-admin", name = "Administrator"),
                Role(id = "role-user", name = "User")
            ),
            isActive = false,  // Diferente al default para forzar serialización
            createdAt = Instant.parse("2024-01-01T10:00:00Z"),
            updatedAt = Instant.parse("2024-01-01T10:00:00Z"),
            createdBy = "admin",  // Diferente al default para forzar serialización
            updatedBy = "admin",  // Diferente al default para forzar serialización
            deletedAt = null
        )

        // Serializar a JSON
        val jsonString = json.encodeToString(originalUser)

        // Verificar snake_case en JSON
        assertTrue(jsonString.contains("\"display_name\""), "JSON debe usar 'display_name'")
        assertTrue(jsonString.contains("\"created_at\""), "JSON debe usar 'created_at'")
        assertTrue(jsonString.contains("\"updated_at\""), "JSON debe usar 'updated_at'")
        assertTrue(jsonString.contains("\"created_by\""), "JSON debe usar 'created_by'")
        assertTrue(jsonString.contains("\"updated_by\""), "JSON debe usar 'updated_by'")

        // Verificar que roles es un array
        assertTrue(jsonString.contains("\"roles\":["), "roles debe ser un array JSON")

        // Deserializar
        val deserializedUser = json.decodeFromString<User>(jsonString)

        // Verificar equivalencia
        assertEquals(originalUser.id, deserializedUser.id)
        assertEquals(originalUser.email, deserializedUser.email)
        assertEquals(originalUser.username, deserializedUser.username)
        assertEquals(originalUser.displayName, deserializedUser.displayName)
        assertEquals(2, deserializedUser.roles.size, "Debe preservar 2 roles")
        assertEquals("role-admin", deserializedUser.roles[0].id)
        assertEquals("role-user", deserializedUser.roles[1].id)
        assertEquals(originalUser.isActive, deserializedUser.isActive)
        assertEquals(originalUser.createdAt, deserializedUser.createdAt)
        assertEquals(originalUser.updatedAt, deserializedUser.updatedAt)
    }

    @Test
    fun `User con colección vacía de roles deserializa correctamente`() {
        val jsonWithEmptyRoles = """
            {
                "id": "user-456",
                "email": "jane@example.com",
                "username": "janedoe",
                "display_name": "Jane Doe",
                "roles": [],
                "is_active": true,
                "created_at": "2024-01-01T12:00:00Z",
                "updated_at": "2024-01-01T12:00:00Z",
                "created_by": "system",
                "updated_by": "system"
            }
        """.trimIndent()

        val user = json.decodeFromString<User>(jsonWithEmptyRoles)

        assertEquals("user-456", user.id)
        assertEquals(emptyList(), user.roles, "roles vacío debe deserializarse correctamente")
    }

    // ========== Test 3: AuthToken - Serialización Cross-Platform con Instant ==========

    @Test
    fun `AuthToken con Instant serializa a ISO-8601 y deserializa consistentemente`() {
        val expiresAt = Instant.parse("2024-12-31T23:59:59Z")
        val originalToken = AuthToken(
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token",
            expiresAt = expiresAt,
            refreshToken = "refresh_abc123"
        )

        // Serializar
        val jsonString = json.encodeToString(originalToken)

        // Verificar snake_case
        assertTrue(jsonString.contains("\"expires_at\""), "JSON debe usar 'expires_at'")
        assertTrue(jsonString.contains("\"refresh_token\""), "JSON debe usar 'refresh_token'")

        // Verificar formato ISO-8601 para Instant
        assertTrue(jsonString.contains("2024-12-31T23:59:59"), "Instant debe estar en formato ISO-8601")

        // Deserializar
        val deserializedToken = json.decodeFromString<AuthToken>(jsonString)

        // Verificar equivalencia
        assertEquals(originalToken.token, deserializedToken.token)
        assertEquals(originalToken.expiresAt, deserializedToken.expiresAt, "Instant debe preservarse exactamente")
        assertEquals(originalToken.refreshToken, deserializedToken.refreshToken)
    }

    @Test
    fun `AuthToken con refreshToken null serializa correctamente en todas las plataformas`() {
        val originalToken = AuthToken(
            token = "test_token",
            expiresAt = Instant.parse("2024-12-31T23:59:59Z"),
            refreshToken = null
        )

        val jsonString = json.encodeToString(originalToken)
        val deserializedToken = json.decodeFromString<AuthToken>(jsonString)

        assertEquals(originalToken.token, deserializedToken.token)
        assertEquals(null, deserializedToken.refreshToken, "refreshToken null debe preservarse")
    }

    @Test
    fun `AuthToken con Instant en boundaries (epoch, futuro lejano) funciona correctamente`() {
        // Test con epoch (1970-01-01)
        val epochToken = AuthToken(
            token = "epoch_token",
            expiresAt = Instant.fromEpochSeconds(0),
            refreshToken = null
        )

        val epochJson = json.encodeToString(epochToken)
        val epochDeserialized = json.decodeFromString<AuthToken>(epochJson)
        assertEquals(epochToken.expiresAt, epochDeserialized.expiresAt, "Epoch debe preservarse")

        // Test con fecha futura lejana (año 2099)
        val futureToken = AuthToken(
            token = "future_token",
            expiresAt = Instant.parse("2099-12-31T23:59:59Z"),
            refreshToken = null
        )

        val futureJson = json.encodeToString(futureToken)
        val futureDeserialized = json.decodeFromString<AuthToken>(futureJson)
        assertEquals(futureToken.expiresAt, futureDeserialized.expiresAt, "Fecha futura debe preservarse")
    }

    // ========== Test 4: Schema Evolution - Campos Extra e Ignorados ==========

    @Test
    fun `deserialización ignora campos desconocidos del backend (schema evolution)`() {
        // JSON del backend con campos extras que no existen en el modelo
        val jsonWithExtraFields = """
            {
                "id": "role-test",
                "name": "Test Role",
                "description": "Test",
                "permissions": ["read"],
                "is_active": true,
                "created_at": "2024-01-01T12:00:00Z",
                "updated_at": "2024-01-01T12:00:00Z",
                "extra_field_1": "this should be ignored",
                "extra_field_2": 12345,
                "nested_extra": {
                    "foo": "bar"
                }
            }
        """.trimIndent()

        // Con ignoreUnknownKeys = true, debe deserializar sin error
        val role = json.decodeFromString<Role>(jsonWithExtraFields)

        assertEquals("role-test", role.id)
        assertEquals("Test Role", role.name)
        // Campos extra son ignorados sin error
    }

    @Test
    fun `deserialización con campos faltantes usa valores default`() {
        // JSON del backend con solo campos requeridos (id, email, username, display_name)
        // Los campos opcionales usan sus valores default
        val minimalUserJson = """
            {
                "id": "user-minimal",
                "email": "minimal@example.com",
                "username": "minimal",
                "display_name": "Minimal User"
            }
        """.trimIndent()

        val user = json.decodeFromString<User>(minimalUserJson)

        // Verificar campos requeridos deserializados
        assertEquals("user-minimal", user.id)
        assertEquals("minimal@example.com", user.email)
        assertEquals("minimal", user.username)
        assertEquals("Minimal User", user.displayName)

        // Verificar valores default para campos opcionales
        assertEquals(emptyList(), user.roles, "roles debe tener default emptyList()")
        assertTrue(user.isActive, "isActive debe tener default true")
        assertEquals(null, user.profileImageUrl, "profileImageUrl debe tener default null")
        assertEquals(emptyMap(), user.metadata, "metadata debe tener default emptyMap()")
        assertEquals("system", user.createdBy, "createdBy debe tener default 'system'")
        assertEquals("system", user.updatedBy, "updatedBy debe tener default 'system'")
        assertEquals(null, user.deletedAt, "deletedAt debe tener default null")
    }

    // ========== Test 5: Encoding UTF-8 y Caracteres Especiales ==========

    @Test
    fun `serialización maneja correctamente caracteres UTF-8 y especiales`() {
        val userWithUnicode = User(
            id = "user-unicode",
            email = "用户@example.com",  // Unicode en email
            username = "usuario_español",
            displayName = "José María 日本語 🎉",  // Acentos, japonés, emoji
            roles = listOf(Role(id = "role-1", name = "Administrador 中文")),
            isActive = true,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "system",
            updatedBy = "system"
        )

        // Serializar
        val jsonString = json.encodeToString(userWithUnicode)

        // Deserializar
        val deserialized = json.decodeFromString<User>(jsonString)

        // Verificar que caracteres especiales se preservan
        assertEquals(userWithUnicode.email, deserialized.email, "Email con unicode debe preservarse")
        assertEquals(userWithUnicode.displayName, deserialized.displayName, "DisplayName con unicode/emoji debe preservarse")
        assertEquals("Administrador 中文", deserialized.roles[0].name, "Role name con unicode debe preservarse")
    }

    // ========== Test 6: Compatibilidad Backend - JSON Completo ==========

    @Test
    fun `deserialización desde JSON backend real funciona correctamente`() {
        // Simular JSON exacto que vendría del backend
        val backendJson = """
            {
                "id": "user-backend-001",
                "email": "backend@example.com",
                "username": "backenduser",
                "display_name": "Backend User",
                "roles": [
                    {
                        "id": "role-admin",
                        "name": "Administrator",
                        "description": "Admin role",
                        "permissions": ["users.read", "users.write"],
                        "is_active": true,
                        "created_at": "2024-01-01T10:00:00Z",
                        "updated_at": "2024-01-01T10:00:00Z"
                    }
                ],
                "is_active": true,
                "created_at": "2024-01-15T08:30:00Z",
                "updated_at": "2024-01-20T14:45:00Z",
                "created_by": "admin-user",
                "updated_by": "admin-user",
                "deleted_at": null
            }
        """.trimIndent()

        val user = json.decodeFromString<User>(backendJson)

        // Verificar todos los campos deserializados correctamente
        assertEquals("user-backend-001", user.id)
        assertEquals("backend@example.com", user.email)
        assertEquals("backenduser", user.username)
        assertEquals("Backend User", user.displayName)
        assertEquals(1, user.roles.size)
        assertEquals("role-admin", user.roles[0].id)
        assertEquals("Administrator", user.roles[0].name)
        assertEquals(setOf("users.read", "users.write"), user.roles[0].permissions)
        assertTrue(user.isActive)
        assertEquals("admin-user", user.createdBy)
        assertEquals("admin-user", user.updatedBy)
        assertEquals(null, user.deletedAt)
    }

    @Test
    fun `serialización a JSON backend tiene formato esperado`() {
        val user = User(
            id = "user-001",
            email = "test@example.com",
            username = "testuser",
            displayName = "Test User",
            roles = listOf(Role(id = "role-user", name = "User")),
            isActive = false,  // Diferente al default para forzar serialización
            createdAt = Instant.parse("2024-01-01T12:00:00Z"),
            updatedAt = Instant.parse("2024-01-01T12:00:00Z"),
            createdBy = "admin",
            updatedBy = "admin",
            deletedAt = null
        )

        val jsonString = json.encodeToString(user)

        // Verificar que todas las claves usan snake_case
        val requiredKeys = listOf(
            "\"id\"",
            "\"email\"",
            "\"username\"",
            "\"display_name\"",
            "\"roles\"",
            "\"created_at\"",
            "\"updated_at\"",
            "\"created_by\"",
            "\"updated_by\""
        )

        requiredKeys.forEach { key ->
            assertTrue(jsonString.contains(key), "JSON debe contener clave $key en snake_case")
        }

        // Verificar que NO contiene claves en camelCase
        assertFalse(jsonString.contains("\"displayName\""), "JSON NO debe contener camelCase")
        assertFalse(jsonString.contains("\"createdAt\""), "JSON NO debe contener camelCase")
        assertFalse(jsonString.contains("\"updatedAt\""), "JSON NO debe contener camelCase")
    }

    // ========== Test 7: Round-trip Completo con Todos los Modelos ==========

    @Test
    fun `round-trip completo User-Role-AuthToken preserva todos los datos`() {
        // Crear estructura completa
        val role1 = Role(
            id = "role-admin",
            name = "Administrator",
            description = "Full access",
            permissions = setOf("all"),
            isActive = true,
            createdAt = Instant.parse("2024-01-01T12:00:00Z"),
            updatedAt = Instant.parse("2024-01-01T12:00:00Z")
        )

        val role2 = Role(
            id = "role-user",
            name = "User",
            description = "Basic access",
            permissions = setOf("read"),
            isActive = true,
            createdAt = Instant.parse("2024-01-01T12:00:00Z"),
            updatedAt = Instant.parse("2024-01-01T12:00:00Z")
        )

        val user = User(
            id = "user-complete",
            email = "complete@example.com",
            username = "completeuser",
            displayName = "Complete User",
            roles = listOf(role1, role2),
            isActive = true,
            createdAt = Instant.parse("2024-01-15T10:00:00Z"),
            updatedAt = Instant.parse("2024-01-20T15:30:00Z"),
            createdBy = "system",
            updatedBy = "admin",
            deletedAt = null
        )

        val authToken = AuthToken(
            token = "complete.jwt.token",
            expiresAt = Instant.parse("2024-12-31T23:59:59Z"),
            refreshToken = "refresh_complete"
        )

        // Serializar todos
        val userJson = json.encodeToString(user)
        val tokenJson = json.encodeToString(authToken)

        // Deserializar todos
        val userDeserialized = json.decodeFromString<User>(userJson)
        val tokenDeserialized = json.decodeFromString<AuthToken>(tokenJson)

        // Verificar User completo
        assertEquals(user.id, userDeserialized.id)
        assertEquals(user.email, userDeserialized.email)
        assertEquals(2, userDeserialized.roles.size)
        assertEquals("role-admin", userDeserialized.roles[0].id)
        assertEquals("role-user", userDeserialized.roles[1].id)

        // Verificar Roles anidados
        assertEquals(role1.description, userDeserialized.roles[0].description)
        assertEquals(role1.permissions, userDeserialized.roles[0].permissions)
        assertEquals(role2.description, userDeserialized.roles[1].description)
        assertEquals(role2.permissions, userDeserialized.roles[1].permissions)

        // Verificar AuthToken
        assertEquals(authToken.token, tokenDeserialized.token)
        assertEquals(authToken.expiresAt, tokenDeserialized.expiresAt)
        assertEquals(authToken.refreshToken, tokenDeserialized.refreshToken)
    }
}
