package com.edugo.test.module.data.models.integration

import com.edugo.test.module.core.Result
import com.edugo.test.module.data.models.Role
import com.edugo.test.module.data.models.User
import com.edugo.test.module.data.models.base.isDeleted
import com.edugo.test.module.data.models.helpers.mergeEntityBase
import com.edugo.test.module.data.models.helpers.patchEntityBase
import com.edugo.test.module.data.models.helpers.patchField
import com.edugo.test.module.data.models.pagination.PagedResult
import com.edugo.test.module.data.models.pagination.paginate
import com.edugo.test.module.data.models.pagination.toPagedResult
import com.edugo.test.module.data.models.pagination.map as pagedMap
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests de integración User/Role - Demuestra composición y reutilización
 */
class ModelIntegrationTest {

    private fun createUser(id: String = "user-1") = User(
        id = id,
        email = "user$id@example.com",
        username = "user$id",
        displayName = "User $id",
        roles = listOf(Role(id = "role-user", name = "User")),
        isActive = true,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
        createdBy = "system",
        updatedBy = "system",
        deletedAt = null
    )

    private fun createRole(id: String = "role-custom") = Role(
        id = id,
        name = "Custom Role",
        description = "Custom role for testing",
        permissions = setOf("resource.read", "resource.write"),
        isActive = true,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )

    @Test
    fun `User implementa todas las interfaces correctamente`() {
        val user = createUser()

        assertTrue(user.id.isNotEmpty())
        assertTrue(user.createdAt <= user.updatedAt)
        assertTrue(user.validate() is Result.Success)
        assertTrue(user.createdBy.isNotEmpty())
        assertFalse(user.isDeleted())
    }

    @Test
    fun `Role implementa interfaces base correctamente`() {
        val role = createRole()

        assertTrue(role.id.isNotEmpty())
        assertTrue(role.validate() is Result.Success)
    }

    @Test
    fun `merge de User preserva propiedades inmutables`() {
        val original = createUser("user-1")
        val updates = createUser("user-1").copy(email = "new@example.com")

        val merged = original.mergeEntityBase(updates) { _, upd ->
            upd.copy(createdAt = original.createdAt, updatedAt = Clock.System.now())
        }

        assertEquals(original.id, merged.id)
        assertEquals(original.createdAt, merged.createdAt)
        assertEquals("new@example.com", merged.email)
    }

    @Test
    fun `patch de User actualiza solo campos especificados`() {
        val user = createUser()

        val patched = user.patchEntityBase { original ->
            original.patchField("new@example.com") {
                copy(email = it, updatedAt = Clock.System.now())
            }
        }

        assertEquals("new@example.com", patched.email)
        assertEquals(user.createdAt, patched.createdAt)
    }

    @Test
    fun `PagedResult funciona con User`() {
        val users = (1..25).map { createUser("user-$it") }
        val paged = users.paginate(page = 0, pageSize = 10)

        assertEquals(10, paged.items.size)
        assertEquals(25, paged.totalCount)
        assertTrue(paged.hasNextPage)
    }

    @Test
    fun `PagedResult funciona con Role`() {
        val roles = (1..15).map { createRole("role-$it") }
        val paged = roles.take(5).toPagedResult(page = 0, pageSize = 5, totalCount = 15)

        assertEquals(5, paged.items.size)
        assertEquals(3, paged.totalPages)
    }

    @Test
    fun `PagedResult se puede transformar con map`() {
        val users = (1..10).map { createUser("user-$it") }
        val paged = users.toPagedResult(page = 0, pageSize = 10, totalCount = 10)

        val ids = paged.pagedMap { it.id }

        assertEquals(10, ids.items.size)
        assertTrue(ids.items.all { it.startsWith("user-") })
    }

    @Test
    fun `serialización round-trip de User mantiene propiedades`() {
        val original = createUser().copy(
            profileImageUrl = "https://example.com/avatar.jpg",
            metadata = mapOf("theme" to "dark")
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<User>(json)

        assertEquals(original, deserialized)
    }

    @Test
    fun `serialización de PagedResult con User`() {
        val users = (1..5).map { createUser("user-$it") }
        val paged = users.toPagedResult(page = 0, pageSize = 5, totalCount = 5)

        val json = Json.encodeToString(paged)
        val deserialized = Json.decodeFromString<PagedResult<User>>(json)

        assertEquals(paged.totalCount, deserialized.totalCount)
    }

    @Test
    fun `flujo de actualización de usuario con validación`() {
        var user = createUser()
        assertTrue(user.validate() is Result.Success)

        user = user.copy(email = "updated@example.com", updatedAt = Clock.System.now())
        assertTrue(user.validate() is Result.Success)
    }

    @Test
    fun `flujo de soft delete de usuario`() {
        var user = createUser()
        assertFalse(user.isDeleted())

        user = user.copy(deletedAt = Clock.System.now())
        assertTrue(user.isDeleted())
    }

    @Test
    fun `paginación de usuarios con filtrado`() {
        val allUsers = (1..100).map { i ->
            createUser("user-$i").copy(isActive = i % 2 == 0)
        }

        val activeUsers = allUsers.filter { it.isActive }
        assertEquals(50, activeUsers.size)

        val pagedActive = activeUsers.paginate(page = 0, pageSize = 10)
        assertEquals(10, pagedActive.items.size)
        assertTrue(pagedActive.items.all { it.isActive })
    }

    @Test
    fun `composición múltiple demuestra reutilización`() {
        val user = createUser()

        // EntityBase: id, timestamps
        assertTrue(user.id.isNotEmpty())
        assertTrue(user.createdAt <= user.updatedAt)

        // ValidatableModel: validación
        assertTrue(user.validate() is Result.Success)

        // AuditableModel: trazabilidad
        assertEquals("system", user.createdBy)

        // SoftDeletable: eliminación lógica
        assertFalse(user.isDeleted())
    }
}
