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
 * Tests del modelo Role - Verifica uso de interfaces base
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
    fun `validación falla si description está vacía`() {
        val role = createValidRole().copy(description = "")
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
}
