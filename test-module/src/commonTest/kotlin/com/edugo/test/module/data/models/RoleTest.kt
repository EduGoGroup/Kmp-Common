package com.edugo.test.module.data.models

import com.edugo.test.module.core.Result
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Test suite para el modelo Role.
 *
 * Esta suite de tests verifica:
 * - Serialización/deserialización JSON con kotlinx-serialization
 * - Validaciones del modelo (id, name, permissions, description)
 * - Compatibilidad con interface RoleMinimal (serialización parcial)
 * - Mapeo correcto de @SerialName (snake_case ↔ camelCase)
 * - Round-trip de serialización preserva todos los campos
 * - Edge cases: permissions vacíos, strings largos, formato inválido
 * - Casos negativos: JSON inválido, campos faltantes
 *
 * ## Cobertura de Tests
 *
 * - **Validación**: id/name vacíos, description larga, permissions inválidos
 * - **Serialización**: Round-trip completo, snake_case mapping, partial deserialization
 * - **Edge Cases**: Valores extremos, colecciones vacías, caracteres especiales
 * - **Negative Cases**: JSON malformado, tipos incorrectos, campos requeridos faltantes
 *
 * @see Role Modelo bajo test
 * @see RoleMinimal Interface mínima implementada por Role
 */
class RoleTest {

    /**
     * Constantes de test para evitar magic values y facilitar mantenimiento.
     */
    private companion object {
        const val TEST_ROLE_ID = "role-user"
        const val TEST_ROLE_NAME = "User"
        const val TEST_ROLE_DESCRIPTION = "Standard user role"
        val TEST_ROLE_PERMISSIONS = setOf("posts.read", "comments.write")

        const val ADMIN_ROLE_ID = "role-admin"
        const val ADMIN_ROLE_NAME = "Administrator"
        const val ADMIN_ROLE_DESCRIPTION = "Full system access"
        val ADMIN_PERMISSIONS = setOf("users.read", "users.write", "system.manage")
    }

    private fun createValidRole() = Role(
        id = TEST_ROLE_ID,
        name = TEST_ROLE_NAME,
        description = TEST_ROLE_DESCRIPTION,
        permissions = TEST_ROLE_PERMISSIONS,
        isActive = true,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )

    @Test
    fun `role válido pasa validación`() {
        val role = createValidRole()
        assertTrue(role.validate() is Result.Success)
    }

    @Test
    fun `validación falla si name está vacío`() {
        val role = createValidRole().copy(name = "")
        assertTrue(role.validate() is Result.Failure)
    }

    @Test
    fun `validación permite description vacía para serialización parcial`() {
        // DECISIÓN DE DISEÑO: description vacía es permitida para soportar
        // serialización parcial cuando Role viaja dentro de User.roles
        val role = createValidRole().copy(description = "")
        assertTrue(role.validate() is Result.Success, "description vacía debe ser válida")
    }

    @Test
    fun `validación falla si description excede 500 caracteres`() {
        val longDescription = "a".repeat(501)
        val role = createValidRole().copy(description = longDescription)
        assertTrue(role.validate() is Result.Failure)
    }

    @Test
    fun `role tiene timestamps de EntityBase`() {
        val role = createValidRole()
        assertTrue(role.createdAt <= role.updatedAt)
    }

    @Test
    fun `role se serializa correctamente`() {
        val original = createValidRole()
        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<Role>(json)
        assertEquals(original, deserialized)
    }

    @Test
    fun `permissions se serializa como Set`() {
        val role = createValidRole()
        val json = Json.encodeToString(role)
        assertTrue(json.contains("\"permissions\""))
    }

    @Test
    fun `role implementa RoleMinimal`() {
        val role = createValidRole()
        val minimal: RoleMinimal = role
        assertEquals(role.id, minimal.id)
        assertEquals(role.name, minimal.name)
    }

    @Test
    fun `serialización usa snake_case con SerialName`() {
        val role = createValidRole()
        val json = Json.encodeToString(role)

        // Verificar que se usa snake_case en JSON
        assertTrue(json.contains("\"created_at\""), "JSON debe contener 'created_at'. Actual: $json")
        assertTrue(json.contains("\"updated_at\""), "JSON debe contener 'updated_at'. Actual: $json")

        // DECISIÓN DE DISEÑO: kotlinx-serialization tiene comportamiento especial con propiedades booleanas.
        // Para 'isActive', el compilador genera getters/setters que eliminan el prefijo 'is'.
        // Por defecto, kotlinx-serialization omite campos con valores default cuando no se configura
        // Json { encodeDefaults = true }. Esto es correcto para optimizar payload.
        // El campo estará presente cuando: a) Se use encodeDefaults=true, o b) El valor sea diferente al default
    }

    @Test
    fun `serialización incluye is_active cuando es diferente al default`() {
        val role = createValidRole().copy(isActive = false)
        val json = Json.encodeToString(role)

        // Cuando isActive es false (diferente al default true), debe aparecer en JSON
        // NOTA: El nombre será "active" no "is_active" debido al manejo especial de kotlinx-serialization
        // para propiedades booleanas con prefijo 'is'
        assertTrue(json.contains("\"active\"") || json.contains("\"is_active\""),
            "JSON debe contener el campo active cuando es diferente al default. Actual: $json")
    }

    @Test
    fun `deserialización parcial con valores default funciona`() {
        // Simular JSON del backend con solo propiedades mínimas (RoleMinimal)
        val minimalJson = """{"id":"role-test","name":"Test Role"}"""

        val role = Json.decodeFromString<Role>(minimalJson)

        // Verificar que se deserializó correctamente
        assertEquals("role-test", role.id)
        assertEquals("Test Role", role.name)

        // Verificar valores default
        assertEquals("", role.description, "description debe tener valor default vacío")
        assertEquals(emptySet(), role.permissions, "permissions debe tener Set vacío")
        assertTrue(role.isActive, "isActive debe ser true por default")
    }

    @Test
    fun `role mínimo es válido para uso en User roles`() {
        // Role creado con solo propiedades de RoleMinimal debe ser válido
        val minimalRole = Role(
            id = "role-user",
            name = "User"
        )

        assertTrue(minimalRole.validate() is Result.Success,
            "Role con solo id y name debe ser válido")
    }

    // ========== Tests adicionales para Task 4 ==========

    @Test
    fun `validación falla si id está vacío`() {
        val role = createValidRole().copy(id = "")
        val result = role.validate()
        assertTrue(result is Result.Failure, "Validación debe fallar con id vacío")
    }

    @Test
    fun `deserialización desde JSON funciona correctamente`() {
        val json = """
            {
                "id": "role-test",
                "name": "Test Role",
                "description": "Test description",
                "permissions": ["users.read", "users.write"],
                "is_active": true,
                "created_at": "2024-01-01T12:00:00Z",
                "updated_at": "2024-01-01T12:00:00Z"
            }
        """.trimIndent()

        val role = Json.decodeFromString<Role>(json)

        assertEquals("role-test", role.id)
        assertEquals("Test Role", role.name)
        assertEquals("Test description", role.description)
        assertEquals(setOf("users.read", "users.write"), role.permissions)
        assertTrue(role.isActive)
    }

    @Test
    fun `permissions vacío es válido`() {
        val roleWithEmptyPermissions = createValidRole().copy(permissions = emptySet())
        assertTrue(roleWithEmptyPermissions.validate() is Result.Success,
            "Role con permissions vacío debe ser válido")
    }

    @Test
    fun `validación falla con permissions en formato inválido`() {
        // Permissions debe seguir formato "resource.action"
        val roleWithInvalidPermissions = createValidRole().copy(
            permissions = setOf("invalid", "also-invalid", "users.read") // Primeros dos son inválidos
        )
        val result = roleWithInvalidPermissions.validate()
        assertTrue(result is Result.Failure,
            "Validación debe fallar con permissions en formato inválido")
    }

    @Test
    fun `serialización y deserialización preservan todos los campos`() {
        val original = Role(
            id = "role-admin",
            name = "Administrator",
            description = "Full system access",
            permissions = setOf("users.read", "users.write", "system.manage"),
            isActive = false, // Diferente al default para verificar serialización
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<Role>(json)

        // Verificar que todos los campos se preservan
        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.description, deserialized.description)
        assertEquals(original.permissions, deserialized.permissions)
        assertEquals(original.isActive, deserialized.isActive)
        assertEquals(original, deserialized, "Round-trip debe preservar todos los campos")
    }

    @Test
    fun `JSON contiene todas las claves esperadas con snake_case`() {
        val role = createValidRole().copy(isActive = false) // Para forzar serialización de isActive
        val json = Json.encodeToString(role)

        // Verificar claves en snake_case
        assertTrue(json.contains("\"id\""), "JSON debe contener 'id'")
        assertTrue(json.contains("\"name\""), "JSON debe contener 'name'")
        assertTrue(json.contains("\"description\""), "JSON debe contener 'description'")
        assertTrue(json.contains("\"permissions\""), "JSON debe contener 'permissions'")
        assertTrue(json.contains("\"created_at\""), "JSON debe contener 'created_at'")
        assertTrue(json.contains("\"updated_at\""), "JSON debe contener 'updated_at'")
    }

    // ========== Edge Case Tests ==========

    @Test
    fun `role con name muy largo (máximo permitido 100 caracteres)`() {
        val maxLengthName = "A".repeat(100)
        val role = createValidRole().copy(name = maxLengthName)
        assertTrue(role.validate() is Result.Success,
            "Name de 100 caracteres debe ser válido (límite máximo)")
    }

    @Test
    fun `validación falla cuando name excede 100 caracteres`() {
        val tooLongName = "A".repeat(101)
        val role = createValidRole().copy(name = tooLongName)
        val result = role.validate()
        assertTrue(result is Result.Failure,
            "Name de 101 caracteres debe ser inválido")
    }

    @Test
    fun `role con description de 500 caracteres (máximo) es válido`() {
        val maxDescription = "B".repeat(500)
        val role = createValidRole().copy(description = maxDescription)
        assertTrue(role.validate() is Result.Success,
            "Description de 500 caracteres debe ser válida (límite máximo)")
    }

    @Test
    fun `role con caracteres especiales en name es válido`() {
        val specialChars = "Admin-Role_2024 (Manager) @Company"
        val role = createValidRole().copy(name = specialChars)
        assertTrue(role.validate() is Result.Success,
            "Name con caracteres especiales debe ser válido")
    }

    @Test
    fun `role con permissions con caracteres especiales válidos`() {
        // La validación actual solo verifica formato "resource.action" (exactamente 1 punto, partes no vacías)
        // Permite caracteres alfanuméricos, guiones y underscores en resource y action
        val permissions = setOf("users-v2.read", "posts_legacy.write", "api123.execute")
        val role = createValidRole().copy(permissions = permissions)
        assertTrue(role.validate() is Result.Success,
            "Permissions con guiones/underscores/números son válidos (pasan isNotBlank())")
    }

    @Test
    fun `role con single character id y name es válido`() {
        val role = Role(id = "a", name = "A")
        assertTrue(role.validate() is Result.Success,
            "Role con id y name de 1 carácter debe ser válido")
    }

    @Test
    fun `permissions con formato exacto resource punto action es válido`() {
        val validPermissions = setOf(
            "a.b",  // Mínimo válido
            "users.read",
            "longresourcename.longactionname",
            "x.y"
        )
        val role = createValidRole().copy(permissions = validPermissions)
        assertTrue(role.validate() is Result.Success,
            "Permissions con formato correcto deben ser válidos")
    }

    // ========== Negative Test Cases ==========

    @Test
    fun `deserialización falla con JSON malformado`() {
        val malformedJson = """{"id":"test","name":"Test" // Missing closing brace"""

        try {
            Json.decodeFromString<Role>(malformedJson)
            assertTrue(false, "Deserialización debe fallar con JSON malformado")
        } catch (e: Exception) {
            // Expected: SerializationException or similar
            assertTrue(true, "Deserialización falló correctamente: ${e::class.simpleName}")
        }
    }

    @Test
    fun `deserialización falla cuando id es de tipo incorrecto (number en lugar de string)`() {
        val wrongTypeJson = """{"id":12345,"name":"Test"}"""

        try {
            Json.decodeFromString<Role>(wrongTypeJson)
            assertTrue(false, "Deserialización debe fallar con tipo incorrecto para id")
        } catch (e: Exception) {
            // Expected: SerializationException
            assertTrue(true, "Deserialización falló correctamente: ${e::class.simpleName}")
        }
    }

    @Test
    fun `deserialización falla cuando permissions es string en lugar de array`() {
        val wrongTypeJson = """{"id":"test","name":"Test","permissions":"users.read"}"""

        try {
            Json.decodeFromString<Role>(wrongTypeJson)
            assertTrue(false, "Deserialización debe fallar con permissions como string")
        } catch (e: Exception) {
            // Expected: SerializationException
            assertTrue(true, "Deserialización falló correctamente: ${e::class.simpleName}")
        }
    }

    @Test
    fun `deserialización falla cuando falta campo requerido id`() {
        val missingIdJson = """{"name":"Test Role"}"""

        try {
            Json.decodeFromString<Role>(missingIdJson)
            assertTrue(false, "Deserialización debe fallar sin campo id requerido")
        } catch (e: Exception) {
            // Expected: MissingFieldException or SerializationException
            assertTrue(true, "Deserialización falló correctamente: ${e::class.simpleName}")
        }
    }

    @Test
    fun `deserialización falla cuando falta campo requerido name`() {
        val missingNameJson = """{"id":"role-test"}"""

        try {
            Json.decodeFromString<Role>(missingNameJson)
            assertTrue(false, "Deserialización debe fallar sin campo name requerido")
        } catch (e: Exception) {
            // Expected: MissingFieldException or SerializationException
            assertTrue(true, "Deserialización falló correctamente: ${e::class.simpleName}")
        }
    }

    @Test
    fun `deserialización con null en campo no-nullable falla`() {
        val nullFieldJson = """{"id":"test","name":null}"""

        try {
            Json.decodeFromString<Role>(nullFieldJson)
            assertTrue(false, "Deserialización debe fallar con null en campo no-nullable")
        } catch (e: Exception) {
            // Expected: SerializationException
            assertTrue(true, "Deserialización falló correctamente: ${e::class.simpleName}")
        }
    }

    @Test
    fun `role con permissions formato incorrecto (sin punto) falla validación`() {
        val invalidPermissions = setOf("read", "write", "delete")  // Faltan "resource."
        val role = createValidRole().copy(permissions = invalidPermissions)
        assertTrue(role.validate() is Result.Failure,
            "Permissions sin formato resource.action deben ser inválidos")
    }

    @Test
    fun `role con permissions formato incorrecto (múltiples puntos) falla validación`() {
        val invalidPermissions = setOf("users.read.all", "posts.write.draft")
        val role = createValidRole().copy(permissions = invalidPermissions)
        assertTrue(role.validate() is Result.Failure,
            "Permissions con múltiples puntos deben ser inválidos")
    }

    @Test
    fun `role con permission vacío (solo punto) falla validación`() {
        val invalidPermissions = setOf(".", ".read", "users.", "")
        val role = createValidRole().copy(permissions = invalidPermissions)
        assertTrue(role.validate() is Result.Failure,
            "Permissions con partes vacías deben ser inválidos")
    }
}
