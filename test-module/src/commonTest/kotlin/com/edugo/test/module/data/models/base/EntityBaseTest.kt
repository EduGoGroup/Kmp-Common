package com.edugo.test.module.data.models.base

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

/**
 * Suite de tests para EntityBase<ID>.
 *
 * Verifica:
 * - Genericidad con diferentes tipos de ID (String, Long, Int)
 * - Propiedades básicas (id, createdAt, updatedAt)
 * - Serialización/deserialización con kotlinx.serialization
 * - Inmutabilidad de las propiedades
 * - Composición con otras interfaces
 */
class EntityBaseTest {

    // ========== Test Entities ==========

    @Serializable
    data class TestEntityString(
        override val id: String,
        val name: String,
        override val createdAt: Instant,
        override val updatedAt: Instant
    ) : EntityBase<String>

    @Serializable
    data class TestEntityLong(
        override val id: Long,
        val value: Int,
        override val createdAt: Instant,
        override val updatedAt: Instant
    ) : EntityBase<Long>

    @Serializable
    data class TestEntityInt(
        override val id: Int,
        val data: String,
        override val createdAt: Instant,
        override val updatedAt: Instant
    ) : EntityBase<Int>

    // ========== Tests de Genericidad ==========

    @Test
    fun `EntityBase funciona con ID tipo String`() {
        val entity = TestEntityString(
            id = "test-123",
            name = "Test Entity",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        assertEquals("test-123", entity.id)
        assertEquals("Test Entity", entity.name)
        assertNotNull(entity.createdAt)
        assertNotNull(entity.updatedAt)
    }

    @Test
    fun `EntityBase funciona con ID tipo Long`() {
        val entity = TestEntityLong(
            id = 123456789L,
            value = 42,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        assertEquals(123456789L, entity.id)
        assertEquals(42, entity.value)
        assertNotNull(entity.createdAt)
        assertNotNull(entity.updatedAt)
    }

    @Test
    fun `EntityBase funciona con ID tipo Int`() {
        val entity = TestEntityInt(
            id = 999,
            data = "some data",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        assertEquals(999, entity.id)
        assertEquals("some data", entity.data)
        assertNotNull(entity.createdAt)
        assertNotNull(entity.updatedAt)
    }

    // ========== Tests de Propiedades ==========

    @Test
    fun `createdAt y updatedAt se inicializan correctamente`() {
        val now = Clock.System.now()
        val entity = TestEntityString(
            id = "test-1",
            name = "Test",
            createdAt = now,
            updatedAt = now
        )

        assertEquals(now, entity.createdAt)
        assertEquals(now, entity.updatedAt)
    }

    @Test
    fun `updatedAt puede ser diferente a createdAt`() {
        val created = Clock.System.now()
        // Simulamos un delay pequeño
        val updated = Clock.System.now()

        val entity = TestEntityString(
            id = "test-1",
            name = "Test",
            createdAt = created,
            updatedAt = updated
        )

        assertEquals(created, entity.createdAt)
        assertEquals(updated, entity.updatedAt)
        // updatedAt debería ser >= createdAt en la práctica
        assertTrue(entity.updatedAt >= entity.createdAt)
    }

    @Test
    fun `id es accesible como propiedad`() {
        val entity = TestEntityString(
            id = "accessible-id",
            name = "Test",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val retrievedId: String = entity.id
        assertEquals("accessible-id", retrievedId)
    }

    // ========== Tests de Inmutabilidad ==========

    @Test
    fun `copy crea nueva instancia con id inmutable`() {
        val original = TestEntityString(
            id = "original-id",
            name = "Original",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val copied = original.copy(
            name = "Modified"
        )

        // ID se preserva (inmutabilidad)
        assertEquals(original.id, copied.id)
        // Otros campos pueden cambiar
        assertEquals("Modified", copied.name)
        assertEquals("Original", original.name)
    }

    @Test
    fun `copy actualiza updatedAt pero preserva createdAt`() {
        val created = Clock.System.now()
        val original = TestEntityString(
            id = "test-1",
            name = "Original",
            createdAt = created,
            updatedAt = created
        )

        val newUpdatedAt = Clock.System.now()
        val updated = original.copy(
            name = "Updated",
            updatedAt = newUpdatedAt
        )

        // createdAt se preserva
        assertEquals(created, updated.createdAt)
        // updatedAt cambia
        assertEquals(newUpdatedAt, updated.updatedAt)
    }

    // ========== Tests de Serialización ==========

    @Test
    fun `EntityBase se serializa correctamente con kotlinx serialization`() {
        val entity = TestEntityString(
            id = "serialize-test",
            name = "Test Entity",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val json = Json.encodeToString(entity)

        // Verificar que el JSON contiene las propiedades esperadas
        assertTrue(json.contains("\"id\":\"serialize-test\""))
        assertTrue(json.contains("\"name\":\"Test Entity\""))
        assertTrue(json.contains("\"createdAt\""))
        assertTrue(json.contains("\"updatedAt\""))
    }

    @Test
    fun `EntityBase se deserializa correctamente con kotlinx serialization`() {
        val original = TestEntityString(
            id = "deserialize-test",
            name = "Original",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<TestEntityString>(json)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.createdAt, deserialized.createdAt)
        assertEquals(original.updatedAt, deserialized.updatedAt)
    }

    @Test
    fun `serialización round-trip mantiene la integridad de los datos`() {
        val original = TestEntityLong(
            id = 987654321L,
            value = 100,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        // Serializar -> Deserializar
        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<TestEntityLong>(json)

        // Verificar que los datos son idénticos
        assertEquals(original, deserialized)
        assertEquals(original.id, deserialized.id)
        assertEquals(original.value, deserialized.value)
        assertEquals(original.createdAt, deserialized.createdAt)
        assertEquals(original.updatedAt, deserialized.updatedAt)
    }

    @Test
    fun `serialización de diferentes tipos de ID funciona correctamente`() {
        val stringEntity = TestEntityString(
            id = "string-id",
            name = "String",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val longEntity = TestEntityLong(
            id = 12345L,
            value = 50,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val intEntity = TestEntityInt(
            id = 999,
            data = "int data",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        // Todas las entidades deben serializarse sin errores
        val json1 = Json.encodeToString(stringEntity)
        val json2 = Json.encodeToString(longEntity)
        val json3 = Json.encodeToString(intEntity)

        assertTrue(json1.isNotEmpty())
        assertTrue(json2.isNotEmpty())
        assertTrue(json3.isNotEmpty())

        // Y deserializarse correctamente
        val deserialized1 = Json.decodeFromString<TestEntityString>(json1)
        val deserialized2 = Json.decodeFromString<TestEntityLong>(json2)
        val deserialized3 = Json.decodeFromString<TestEntityInt>(json3)

        assertEquals(stringEntity, deserialized1)
        assertEquals(longEntity, deserialized2)
        assertEquals(intEntity, deserialized3)
    }

    // ========== Tests de Composición ==========

    @Serializable
    data class CompositeEntity(
        override val id: String,
        val title: String,
        override val createdAt: Instant,
        override val updatedAt: Instant
    ) : EntityBase<String>

    @Test
    fun `EntityBase se puede usar en composición con otras interfaces`() {
        // Este test simplemente verifica que la composición compila
        // Los tests específicos de composición están en otros archivos
        val entity = CompositeEntity(
            id = "composite-1",
            title = "Composite Test",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        // Verificar que funciona como EntityBase
        val entityBase: EntityBase<String> = entity
        assertEquals("composite-1", entityBase.id)
        assertNotNull(entityBase.createdAt)
        assertNotNull(entityBase.updatedAt)
    }

    @Test
    fun `múltiples entidades pueden compartir la misma interface EntityBase`() {
        val entity1 = TestEntityString(
            id = "entity-1",
            name = "First",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val entity2 = TestEntityString(
            id = "entity-2",
            name = "Second",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        // Ambas son EntityBase<String>
        val entities: List<EntityBase<String>> = listOf(entity1, entity2)

        assertEquals(2, entities.size)
        assertEquals("entity-1", entities[0].id)
        assertEquals("entity-2", entities[1].id)
    }

    // ========== Tests de Casos de Uso Reales ==========

    @Test
    fun `simulación de creación y actualización de entidad`() {
        // Crear entidad nueva
        val created = Clock.System.now()
        val entity = TestEntityString(
            id = "user-123",
            name = "Original Name",
            createdAt = created,
            updatedAt = created
        )

        assertEquals("Original Name", entity.name)
        assertEquals(created, entity.createdAt)
        assertEquals(created, entity.updatedAt)

        // Simular actualización
        val updated = Clock.System.now()
        val modifiedEntity = entity.copy(
            name = "Updated Name",
            updatedAt = updated
        )

        assertEquals("Updated Name", modifiedEntity.name)
        assertEquals(created, modifiedEntity.createdAt) // No cambia
        assertEquals(updated, modifiedEntity.updatedAt) // Cambia
        assertTrue(modifiedEntity.updatedAt >= modifiedEntity.createdAt)
    }

    @Test
    fun `lista de entidades con diferentes IDs`() {
        val entities = listOf(
            TestEntityString(
                id = "string-1",
                name = "First",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            ),
            TestEntityString(
                id = "string-2",
                name = "Second",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            ),
            TestEntityString(
                id = "string-3",
                name = "Third",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
        )

        assertEquals(3, entities.size)
        assertEquals(setOf("string-1", "string-2", "string-3"), entities.map { it.id }.toSet())
    }
}
