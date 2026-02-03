package com.edugo.test.module.data.models

import com.edugo.test.module.core.Result
import com.edugo.test.module.data.models.base.isDeleted
import com.edugo.test.module.data.models.base.isActive
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests del modelo User - Verifica uso de interfaces base
 */
class UserTest {

    private fun createValidRole() = Role(
        id = "role-user",
        name = "User"
    )

    private fun createValidUser() = User(
        id = "user-123",
        email = "john@example.com",
        username = "johndoe",
        displayName = "John Doe",
        roles = listOf(createValidRole()),
        isActive = true,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
        createdBy = "system",
        updatedBy = "system",
        deletedAt = null
    )

    @Test
    fun `usuario válido pasa validación`() {
        val user = createValidUser()
        assertTrue(user.validate() is Result.Success)
    }

    @Test
    fun `validación falla si email es inválido`() {
        val user = createValidUser().copy(email = "invalid")
        assertTrue(user.validate() is Result.Failure)
    }

    @Test
    fun `validación falla si username es muy corto`() {
        val user = createValidUser().copy(username = "ab")
        assertTrue(user.validate() is Result.Failure)
    }

    @Test
    fun `usuario tiene timestamps de EntityBase`() {
        val user = createValidUser()
        assertTrue(user.createdAt <= user.updatedAt)
    }

    @Test
    fun `usuario tiene información de auditoría`() {
        val user = createValidUser()
        assertEquals("system", user.createdBy)
        assertEquals("system", user.updatedBy)
    }

    @Test
    fun `usuario soporta soft delete`() {
        var user = createValidUser()
        assertFalse(user.isDeleted())

        user = user.copy(deletedAt = Clock.System.now())
        assertTrue(user.isDeleted())
    }

    @Test
    fun `usuario se serializa correctamente`() {
        val original = createValidUser()
        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<User>(json)
        assertEquals(original, deserialized)
    }

    // ========== Tests adicionales para Task 5 ==========

    @Test
    fun `serialización User con roles produce JSON con roles nested`() {
        val user = createValidUser()
        val json = Json.encodeToString(user)

        // Verificar que roles está presente como array
        assertTrue(json.contains("\"roles\""), "JSON debe contener campo 'roles'")
        assertTrue(json.contains("["), "roles debe ser un array")

        // Verificar que roles contienen objetos con id y name
        assertTrue(json.contains("\"id\""), "Role debe tener campo 'id'")
        assertTrue(json.contains("\"name\""), "Role debe tener campo 'name'")
    }

    @Test
    fun `deserialización JSON con roles crea User correctamente`() {
        val json = """
            {
                "id": "user-456",
                "email": "jane@example.com",
                "username": "janedoe",
                "display_name": "Jane Doe",
                "roles": [
                    {"id": "role-admin", "name": "Administrator"},
                    {"id": "role-user", "name": "User"}
                ],
                "is_active": true,
                "created_at": "2024-01-01T12:00:00Z",
                "updated_at": "2024-01-01T12:00:00Z",
                "created_by": "system",
                "updated_by": "system"
            }
        """.trimIndent()

        val user = Json.decodeFromString<User>(json)

        assertEquals("user-456", user.id)
        assertEquals("jane@example.com", user.email)
        assertEquals(2, user.roles.size)
        assertEquals("role-admin", user.roles[0].id)
        assertEquals("Administrator", user.roles[0].name)
        assertEquals("role-user", user.roles[1].id)
        assertEquals("User", user.roles[1].name)
    }

    @Test
    fun `round-trip con múltiples roles preserva todos los datos`() {
        val original = createValidUser().copy(
            roles = listOf(
                Role(id = "role-admin", name = "Administrator"),
                Role(id = "role-user", name = "User"),
                Role(id = "role-moderator", name = "Moderator")
            )
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<User>(json)

        assertEquals(3, deserialized.roles.size)
        assertEquals(original.roles[0].id, deserialized.roles[0].id)
        assertEquals(original.roles[1].id, deserialized.roles[1].id)
        assertEquals(original.roles[2].id, deserialized.roles[2].id)
        assertEquals(original, deserialized)
    }

    @Test
    fun `validación falla si id está vacío`() {
        val user = createValidUser().copy(id = "")
        val result = user.validate()
        assertTrue(result is Result.Failure, "Validación debe fallar con id vacío")
    }

    @Test
    fun `validación falla si roles está vacío`() {
        val user = createValidUser().copy(roles = emptyList())
        val result = user.validate()
        assertTrue(result is Result.Failure,
            "Validación debe fallar: User debe tener al menos un rol")
    }

    @Test
    fun `validación email válido con diferentes formatos correctos`() {
        val validEmails = listOf(
            "user@example.com",
            "test.user@domain.co.uk",
            "name+tag@company.org",
            "user123@test-domain.com"
        )

        validEmails.forEach { email ->
            val user = createValidUser().copy(email = email)
            assertTrue(user.validate() is Result.Success,
                "Email '$email' debe ser válido")
        }
    }

    @Test
    fun `validación email inválido con mensaje descriptivo`() {
        val invalidEmails = mapOf(
            "invalid" to "sin @",
            "@example.com" to "sin parte local",
            "user@" to "sin dominio",
            "user@domain" to "sin punto en dominio",
            "" to "vacío"
        )

        invalidEmails.forEach { (email, reason) ->
            val user = createValidUser().copy(email = email)
            val result = user.validate()
            assertTrue(result is Result.Failure,
                "Email '$email' ($reason) debe ser inválido")
        }
    }

    @Test
    fun `@SerialName mapea correctamente a snake_case`() {
        // Crear user con valores diferentes a defaults para forzar serialización
        val user = createValidUser().copy(
            isActive = false,
            createdBy = "admin", // Diferente al default "system"
            updatedBy = "admin"  // Diferente al default "system"
        )
        val json = Json.encodeToString(user)

        // Verificar mapeo de propiedades snake_case
        assertTrue(json.contains("\"display_name\""), "JSON debe contener 'display_name'. JSON: $json")
        assertTrue(json.contains("\"created_at\""), "JSON debe contener 'created_at'. JSON: $json")
        assertTrue(json.contains("\"updated_at\""), "JSON debe contener 'updated_at'. JSON: $json")
        assertTrue(json.contains("\"created_by\""), "JSON debe contener 'created_by'. JSON: $json")
        assertTrue(json.contains("\"updated_by\""), "JSON debe contener 'updated_by'. JSON: $json")

        // Verificar que NO contiene camelCase
        assertFalse(json.contains("\"displayName\""), "JSON NO debe contener 'displayName' en camelCase")
        assertFalse(json.contains("\"createdAt\""), "JSON NO debe contener 'createdAt' en camelCase")
        assertFalse(json.contains("\"updatedAt\""), "JSON NO debe contener 'updatedAt' en camelCase")
        assertFalse(json.contains("\"createdBy\""), "JSON NO debe contener 'createdBy' en camelCase")
    }

    @Test
    fun `roles se serializa como array de objetos Role completos`() {
        val user = createValidUser().copy(
            roles = listOf(
                Role(id = "role-1", name = "Role One"),
                Role(id = "role-2", name = "Role Two")
            )
        )
        val json = Json.encodeToString(user)

        // Verificar estructura de array
        assertTrue(json.contains("\"roles\":["), "roles debe ser un array JSON")

        // Verificar que cada role es un objeto con propiedades
        val roleCount = json.split("\"id\":").size - 1
        assertTrue(roleCount >= 2, "JSON debe contener al menos 2 objetos Role")
    }

    @Test
    fun `deserialización con roles vacíos usa default`() {
        val json = """
            {
                "id": "user-789",
                "email": "test@example.com",
                "username": "testuser",
                "display_name": "Test User"
            }
        """.trimIndent()

        val user = Json.decodeFromString<User>(json)

        assertEquals(emptyList(), user.roles, "roles debe tener valor default emptyList()")
        // Nota: La validación fallará porque requiere al menos un rol,
        // pero la deserialización funciona
    }

    @Test
    fun `serialización y deserialización preservan metadata`() {
        val original = createValidUser().copy(
            metadata = mapOf("key1" to "value1", "key2" to "value2")
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<User>(json)

        assertEquals(original.metadata, deserialized.metadata)
    }

    @Test
    fun `hasRole verifica correctamente roles por id`() {
        val user = createValidUser().copy(
            roles = listOf(
                Role(id = "role-admin", name = "Administrator"),
                Role(id = "role-user", name = "User")
            )
        )

        assertTrue(user.hasRole("role-admin"), "User debe tener role-admin")
        assertTrue(user.hasRole("role-user"), "User debe tener role-user")
        assertFalse(user.hasRole("role-guest"), "User no debe tener role-guest")
    }
}
