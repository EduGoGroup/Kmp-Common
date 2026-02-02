package com.edugo.test.module.data.models.base

import com.edugo.test.module.core.Result
import com.edugo.test.module.core.failure
import com.edugo.test.module.core.success
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertNull

/**
 * Suite de tests para SoftDeletable y sus extension functions.
 *
 * Verifica:
 * - Propiedad deletedAt (null = activo, Instant = eliminado)
 * - Extension functions: isDeleted(), isActive(), markAsDeleted(), restore()
 * - Extension functions para listas: onlyActive(), onlyDeleted()
 * - Composición con EntityBase y AuditableModel
 * - Serialización de soft delete
 * - Casos de uso: eliminar, restaurar, filtrar
 */
class SoftDeletableTest {

    // ========== Test Entities ==========

    @Serializable
    data class SimpleSoftDeletable(
        val name: String,
        override val deletedAt: Instant? = null
    ) : SoftDeletable

    @Serializable
    data class SoftDeletableEntity(
        override val id: String,
        val title: String,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        override val deletedAt: Instant? = null
    ) : EntityBase<String>, SoftDeletable

    @Serializable
    data class FullSoftDeletableEntity(
        override val id: String,
        val content: String,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        override val createdBy: String,
        override val updatedBy: String,
        override val deletedAt: Instant? = null,
        val deletedBy: String? = null
    ) : EntityBase<String>, AuditableModel, SoftDeletable

    @Serializable
    data class ValidatableSoftDeletable(
        override val id: String,
        val data: String,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        override val deletedAt: Instant? = null
    ) : EntityBase<String>, SoftDeletable, ValidatableModel {
        override fun validate(): Result<Unit> {
            return when {
                data.isBlank() -> failure("Data is required")
                isDeleted() -> failure("Cannot validate deleted entity")
                else -> success(Unit)
            }
        }
    }

    // ========== Tests de Propiedad deletedAt ==========

    @Test
    fun `deletedAt es null para entidades activas`() {
        val entity = SimpleSoftDeletable(
            name = "Active Entity",
            deletedAt = null
        )

        assertNull(entity.deletedAt)
    }

    @Test
    fun `deletedAt contiene timestamp para entidades eliminadas`() {
        val deletedTime = Clock.System.now()
        val entity = SimpleSoftDeletable(
            name = "Deleted Entity",
            deletedAt = deletedTime
        )

        assertNotNull(entity.deletedAt)
        assertEquals(deletedTime, entity.deletedAt)
    }

    @Test
    fun `deletedAt por defecto es null`() {
        val entity = SimpleSoftDeletable(name = "Default Entity")
        assertNull(entity.deletedAt)
    }

    // ========== Tests de Extension Functions: isDeleted() y isActive() ==========

    @Test
    fun `isDeleted retorna false cuando deletedAt es null`() {
        val entity = SimpleSoftDeletable(
            name = "Active",
            deletedAt = null
        )

        assertTrue(!entity.isDeleted())
    }

    @Test
    fun `isDeleted retorna true cuando deletedAt no es null`() {
        val entity = SimpleSoftDeletable(
            name = "Deleted",
            deletedAt = Clock.System.now()
        )

        assertTrue(entity.isDeleted())
    }

    @Test
    fun `isActive retorna true cuando deletedAt es null`() {
        val entity = SimpleSoftDeletable(
            name = "Active",
            deletedAt = null
        )

        assertTrue(entity.isActive())
    }

    @Test
    fun `isActive retorna false cuando deletedAt no es null`() {
        val entity = SimpleSoftDeletable(
            name = "Deleted",
            deletedAt = Clock.System.now()
        )

        assertTrue(!entity.isActive())
    }

    @Test
    fun `isDeleted e isActive son opuestos`() {
        val activeEntity = SimpleSoftDeletable(name = "Active", deletedAt = null)
        val deletedEntity = SimpleSoftDeletable(name = "Deleted", deletedAt = Clock.System.now())

        // Para entidad activa
        assertTrue(activeEntity.isActive())
        assertTrue(!activeEntity.isDeleted())

        // Para entidad eliminada
        assertTrue(deletedEntity.isDeleted())
        assertTrue(!deletedEntity.isActive())
    }

    // ========== Tests de Extension Function: markAsDeleted() ==========

    @Test
    fun `markAsDeleted retorna Map con deletedAt y updatedAt`() {
        val entity = SimpleSoftDeletable(name = "Test")
        val updates = entity.markAsDeleted()

        assertTrue(updates.containsKey("deletedAt"))
        assertTrue(updates.containsKey("updatedAt"))
        assertNotNull(updates["deletedAt"])
        assertNotNull(updates["updatedAt"])
    }

    @Test
    fun `markAsDeleted genera timestamps recientes`() {
        val before = Clock.System.now()
        val entity = SimpleSoftDeletable(name = "Test")
        val updates = entity.markAsDeleted()
        val after = Clock.System.now()

        val deletedAt = updates["deletedAt"] as Instant
        val updatedAt = updates["updatedAt"] as Instant

        assertTrue(deletedAt >= before)
        assertTrue(deletedAt <= after)
        assertTrue(updatedAt >= before)
        assertTrue(updatedAt <= after)
    }

    // ========== Tests de Extension Function: restore() ==========

    @Test
    fun `restore retorna Map con deletedAt null y updatedAt actual`() {
        val entity = SimpleSoftDeletable(
            name = "Deleted",
            deletedAt = Clock.System.now()
        )

        val updates = entity.restore()

        assertTrue(updates.containsKey("deletedAt"))
        assertTrue(updates.containsKey("updatedAt"))
        assertNull(updates["deletedAt"])
        assertNotNull(updates["updatedAt"])
    }

    // ========== Tests de Operaciones de Soft Delete ==========

    @Test
    fun `entidad puede ser marcada como eliminada`() {
        val active = SoftDeletableEntity(
            id = "entity-1",
            title = "Active Entity",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            deletedAt = null
        )

        assertTrue(active.isActive())

        // Marcar como eliminada
        val deleted = active.copy(
            deletedAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        assertTrue(deleted.isDeleted())
        assertTrue(!deleted.isActive())
        assertNotNull(deleted.deletedAt)
    }

    @Test
    fun `entidad eliminada puede ser restaurada`() {
        val deleted = SoftDeletableEntity(
            id = "entity-1",
            title = "Deleted Entity",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            deletedAt = Clock.System.now()
        )

        assertTrue(deleted.isDeleted())

        // Restaurar
        val restored = deleted.copy(
            deletedAt = null,
            updatedAt = Clock.System.now()
        )

        assertTrue(restored.isActive())
        assertTrue(!restored.isDeleted())
        assertNull(restored.deletedAt)
    }

    @Test
    fun `ciclo completo eliminar y restaurar`() {
        // 1. Entidad activa
        var entity = SoftDeletableEntity(
            id = "cycle-test",
            title = "Test",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            deletedAt = null
        )
        assertTrue(entity.isActive())

        // 2. Eliminar
        entity = entity.copy(
            deletedAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        assertTrue(entity.isDeleted())

        // 3. Restaurar
        entity = entity.copy(
            deletedAt = null,
            updatedAt = Clock.System.now()
        )
        assertTrue(entity.isActive())

        // 4. Eliminar de nuevo
        entity = entity.copy(
            deletedAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        assertTrue(entity.isDeleted())
    }

    // ========== Tests de Extension Functions para Listas ==========

    @Test
    fun `onlyActive filtra solo entidades activas`() {
        val entities = listOf(
            SimpleSoftDeletable(name = "Active 1", deletedAt = null),
            SimpleSoftDeletable(name = "Deleted 1", deletedAt = Clock.System.now()),
            SimpleSoftDeletable(name = "Active 2", deletedAt = null),
            SimpleSoftDeletable(name = "Deleted 2", deletedAt = Clock.System.now()),
            SimpleSoftDeletable(name = "Active 3", deletedAt = null)
        )

        val activeOnly = entities.onlyActive()

        assertEquals(3, activeOnly.size)
        assertTrue(activeOnly.all { it.isActive() })
        assertEquals(setOf("Active 1", "Active 2", "Active 3"), activeOnly.map { it.name }.toSet())
    }

    @Test
    fun `onlyDeleted filtra solo entidades eliminadas`() {
        val entities = listOf(
            SimpleSoftDeletable(name = "Active 1", deletedAt = null),
            SimpleSoftDeletable(name = "Deleted 1", deletedAt = Clock.System.now()),
            SimpleSoftDeletable(name = "Active 2", deletedAt = null),
            SimpleSoftDeletable(name = "Deleted 2", deletedAt = Clock.System.now()),
            SimpleSoftDeletable(name = "Active 3", deletedAt = null)
        )

        val deletedOnly = entities.onlyDeleted()

        assertEquals(2, deletedOnly.size)
        assertTrue(deletedOnly.all { it.isDeleted() })
        assertEquals(setOf("Deleted 1", "Deleted 2"), deletedOnly.map { it.name }.toSet())
    }

    @Test
    fun `onlyActive con lista vacía retorna lista vacía`() {
        val empty = emptyList<SimpleSoftDeletable>()
        val result = empty.onlyActive()
        assertEquals(0, result.size)
    }

    @Test
    fun `onlyDeleted con lista vacía retorna lista vacía`() {
        val empty = emptyList<SimpleSoftDeletable>()
        val result = empty.onlyDeleted()
        assertEquals(0, result.size)
    }

    @Test
    fun `onlyActive con todas eliminadas retorna lista vacía`() {
        val allDeleted = listOf(
            SimpleSoftDeletable(name = "D1", deletedAt = Clock.System.now()),
            SimpleSoftDeletable(name = "D2", deletedAt = Clock.System.now())
        )
        val result = allDeleted.onlyActive()
        assertEquals(0, result.size)
    }

    @Test
    fun `onlyDeleted con todas activas retorna lista vacía`() {
        val allActive = listOf(
            SimpleSoftDeletable(name = "A1", deletedAt = null),
            SimpleSoftDeletable(name = "A2", deletedAt = null)
        )
        val result = allActive.onlyDeleted()
        assertEquals(0, result.size)
    }

    // ========== Tests de Composición ==========

    @Test
    fun `SoftDeletable se compone con EntityBase`() {
        val entity = SoftDeletableEntity(
            id = "composed-1",
            title = "Composed",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            deletedAt = null
        )

        // Verifica EntityBase
        assertEquals("composed-1", entity.id)
        assertNotNull(entity.createdAt)
        assertNotNull(entity.updatedAt)

        // Verifica SoftDeletable
        assertNull(entity.deletedAt)
        assertTrue(entity.isActive())
    }

    @Test
    fun `SoftDeletable se compone con EntityBase y AuditableModel`() {
        val entity = FullSoftDeletableEntity(
            id = "full-1",
            content = "Content",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "user-1",
            updatedBy = "user-1",
            deletedAt = null,
            deletedBy = null
        )

        // Verifica EntityBase
        assertEquals("full-1", entity.id)

        // Verifica AuditableModel
        assertEquals("user-1", entity.createdBy)
        assertEquals("user-1", entity.updatedBy)

        // Verifica SoftDeletable
        assertNull(entity.deletedAt)
        assertTrue(entity.isActive())
    }

    @Test
    fun `soft delete con información de quién eliminó`() {
        val active = FullSoftDeletableEntity(
            id = "doc-1",
            content = "Document",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "creator",
            updatedBy = "creator",
            deletedAt = null,
            deletedBy = null
        )

        // Eliminar por un usuario específico
        val deleted = active.copy(
            deletedAt = Clock.System.now(),
            deletedBy = "admin-user",
            updatedAt = Clock.System.now(),
            updatedBy = "admin-user"
        )

        assertTrue(deleted.isDeleted())
        assertEquals("admin-user", deleted.deletedBy)
        assertEquals("admin-user", deleted.updatedBy)
        // createdBy se preserva
        assertEquals("creator", deleted.createdBy)
    }

    @Test
    fun `SoftDeletable se compone con ValidatableModel`() {
        val entity = ValidatableSoftDeletable(
            id = "val-1",
            data = "Valid data",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            deletedAt = null
        )

        val result = entity.validate()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `validación falla para entidades eliminadas`() {
        val deleted = ValidatableSoftDeletable(
            id = "val-2",
            data = "Some data",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            deletedAt = Clock.System.now()
        )

        val result = deleted.validate()
        assertTrue(result is Result.Failure)
        assertEquals("Cannot validate deleted entity", (result as Result.Failure).error)
    }

    // ========== Tests de Serialización ==========

    @Test
    fun `SoftDeletable se serializa con deletedAt null`() {
        val entity = SoftDeletableEntity(
            id = "ser-1",
            title = "Active",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            deletedAt = null
        )

        val json = Json.encodeToString(entity)
        // kotlinx.serialization omite campos null por defecto o los incluye como null
        // Verificamos que el JSON se genera correctamente
        assertTrue(json.contains("\"id\":\"ser-1\""))
        assertTrue(json.contains("\"title\":\"Active\""))
    }

    @Test
    fun `SoftDeletable se serializa con deletedAt timestamp`() {
        val deletedTime = Clock.System.now()
        val entity = SoftDeletableEntity(
            id = "ser-2",
            title = "Deleted",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            deletedAt = deletedTime
        )

        val json = Json.encodeToString(entity)
        assertTrue(json.contains("\"deletedAt\""))
        assertTrue(!json.contains("\"deletedAt\":null"))
    }

    @Test
    fun `serialización round-trip preserva estado de soft delete`() {
        val active = SimpleSoftDeletable(name = "Active", deletedAt = null)
        val deleted = SimpleSoftDeletable(name = "Deleted", deletedAt = Clock.System.now())

        // Active
        val activeJson = Json.encodeToString(active)
        val activeDeserialized = Json.decodeFromString<SimpleSoftDeletable>(activeJson)
        assertNull(activeDeserialized.deletedAt)
        assertTrue(activeDeserialized.isActive())

        // Deleted
        val deletedJson = Json.encodeToString(deleted)
        val deletedDeserialized = Json.decodeFromString<SimpleSoftDeletable>(deletedJson)
        assertNotNull(deletedDeserialized.deletedAt)
        assertTrue(deletedDeserialized.isDeleted())
    }

    // ========== Tests de Casos de Uso Reales ==========

    @Test
    fun `usuario elimina su propio registro`() {
        val user = FullSoftDeletableEntity(
            id = "user-123",
            content = "User data",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "user-123",
            updatedBy = "user-123",
            deletedAt = null,
            deletedBy = null
        )

        assertTrue(user.isActive())

        // Usuario se elimina a sí mismo
        val deleted = user.copy(
            deletedAt = Clock.System.now(),
            deletedBy = "user-123",
            updatedAt = Clock.System.now()
        )

        assertTrue(deleted.isDeleted())
        assertEquals("user-123", deleted.deletedBy)
    }

    @Test
    fun `admin elimina registro de otro usuario`() {
        val document = FullSoftDeletableEntity(
            id = "doc-456",
            content = "Document",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "user-original",
            updatedBy = "user-original",
            deletedAt = null,
            deletedBy = null
        )

        // Admin elimina el documento
        val deleted = document.copy(
            deletedAt = Clock.System.now(),
            deletedBy = "admin",
            updatedAt = Clock.System.now(),
            updatedBy = "admin"
        )

        assertTrue(deleted.isDeleted())
        assertEquals("user-original", deleted.createdBy)
        assertEquals("admin", deleted.deletedBy)
        assertEquals("admin", deleted.updatedBy)
    }

    @Test
    fun `restauración de registro eliminado`() {
        val deleted = FullSoftDeletableEntity(
            id = "restore-test",
            content = "Deleted content",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "creator",
            updatedBy = "deleter",
            deletedAt = Clock.System.now(),
            deletedBy = "deleter"
        )

        assertTrue(deleted.isDeleted())

        // Restaurar (deletedBy podría mantenerse o limpiarse según política)
        val restored = deleted.copy(
            deletedAt = null,
            deletedBy = null,
            updatedAt = Clock.System.now(),
            updatedBy = "restorer"
        )

        assertTrue(restored.isActive())
        assertNull(restored.deletedAt)
        assertNull(restored.deletedBy)
        assertEquals("restorer", restored.updatedBy)
    }

    @Test
    fun `filtrado de registros activos en consulta`() {
        val allRecords = listOf(
            SoftDeletableEntity(
                id = "1",
                title = "Active 1",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                deletedAt = null
            ),
            SoftDeletableEntity(
                id = "2",
                title = "Deleted 1",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                deletedAt = Clock.System.now()
            ),
            SoftDeletableEntity(
                id = "3",
                title = "Active 2",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                deletedAt = null
            )
        )

        // Simular query que solo retorna activos
        val activeRecords = allRecords.onlyActive()

        assertEquals(2, activeRecords.size)
        assertEquals(setOf("1", "3"), activeRecords.map { it.id }.toSet())
    }

    @Test
    fun `vista de papelera de reciclaje (solo eliminados)`() {
        val allRecords = listOf(
            SimpleSoftDeletable(name = "Active 1", deletedAt = null),
            SimpleSoftDeletable(name = "Deleted 1", deletedAt = Clock.System.now()),
            SimpleSoftDeletable(name = "Deleted 2", deletedAt = Clock.System.now()),
            SimpleSoftDeletable(name = "Active 2", deletedAt = null)
        )

        // Vista de papelera: solo mostrar eliminados
        val recycleBin = allRecords.onlyDeleted()

        assertEquals(2, recycleBin.size)
        assertEquals(setOf("Deleted 1", "Deleted 2"), recycleBin.map { it.name }.toSet())
    }

    @Test
    fun `estadísticas de eliminación`() {
        val records = listOf(
            SimpleSoftDeletable(name = "A1", deletedAt = null),
            SimpleSoftDeletable(name = "D1", deletedAt = Clock.System.now()),
            SimpleSoftDeletable(name = "A2", deletedAt = null),
            SimpleSoftDeletable(name = "D2", deletedAt = Clock.System.now()),
            SimpleSoftDeletable(name = "D3", deletedAt = Clock.System.now())
        )

        val totalRecords = records.size
        val activeCount = records.onlyActive().size
        val deletedCount = records.onlyDeleted().size

        assertEquals(5, totalRecords)
        assertEquals(2, activeCount)
        assertEquals(3, deletedCount)
        assertEquals(totalRecords, activeCount + deletedCount)
    }
}
