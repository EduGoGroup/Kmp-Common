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
}
