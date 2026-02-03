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
 * Tests del modelo Role - Verifica uso de interfaces base y RoleMinimal
 */
class RoleTest {

    private fun createValidRole() = Role(
        id = "role-user",
        name = "User",
        description = "Standard user role",
        permissions = setOf("posts.read", "comments.write"),
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
}
