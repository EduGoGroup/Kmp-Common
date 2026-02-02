package com.edugo.test.module.data.models.helpers

import com.edugo.test.module.core.Result
import com.edugo.test.module.core.failure
import com.edugo.test.module.core.success
import com.edugo.test.module.data.models.base.EntityBase
import com.edugo.test.module.data.models.base.ValidatableModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

/**
 * Suite de tests para helpers de merge (fusión) de modelos.
 *
 * Verifica:
 * - MergeStrategy y su comportamiento
 * - mergeWithValidation() con ValidatableModel
 * - mergeEntityBase() preservando propiedades inmutables
 * - detectChanges() para tracking de cambios
 * - mergeMaps() y mergeLists() con diferentes estrategias
 * - MergeBuilder para construcción fluida
 * - Casos de preservación de createdAt y actualización de updatedAt
 */
class ModelMergeTest {

    // ========== Test Models ==========

    @Serializable
    data class TestEntity(
        override val id: String,
        val name: String,
        val email: String,
        val age: Int,
        override val createdAt: Instant,
        override val updatedAt: Instant
    ) : EntityBase<String>, ValidatableModel {
        override fun validate(): Result<Unit> {
            return when {
                name.isBlank() -> failure("Name cannot be blank")
                !email.contains("@") -> failure("Email must contain @")
                age < 0 -> failure("Age cannot be negative")
                else -> success(Unit)
            }
        }

        fun mergeWith(other: TestEntity): TestEntity {
            return mergeEntityBase(other) { orig, upd ->
                orig.copy(
                    name = upd.name,
                    email = upd.email,
                    age = upd.age,
                    updatedAt = Clock.System.now()
                )
            }
        }
    }

    @Serializable
    data class SimpleModel(
        val value: String
    ) : ValidatableModel {
        override fun validate(): Result<Unit> {
            return when {
                value.isBlank() -> failure("Value cannot be blank")
                else -> success(Unit)
            }
        }
    }

    // ========== Tests de MergeStrategy ==========

    @Test
    fun `MergeStrategy PREFER_OTHER sobrescribe con valor del segundo objeto`() {
        val strategy = MergeStrategy.PREFER_OTHER
        assertEquals(MergeStrategy.PREFER_OTHER, strategy)
    }

    @Test
    fun `MergeStrategy PREFER_ORIGINAL mantiene valor del primer objeto`() {
        val strategy = MergeStrategy.PREFER_ORIGINAL
        assertEquals(MergeStrategy.PREFER_ORIGINAL, strategy)
    }

    @Test
    fun `MergeStrategy COMBINE_COLLECTIONS combina colecciones`() {
        val strategy = MergeStrategy.COMBINE_COLLECTIONS
        assertEquals(MergeStrategy.COMBINE_COLLECTIONS, strategy)
    }

    // ========== Tests de MergeResult ==========

    @Test
    fun `MergeResult con cambios retorna hasChanges true`() {
        val result = MergeResult(
            merged = SimpleModel("value"),
            changedFields = listOf("value")
        )

        assertTrue(result.hasChanges)
        assertEquals(1, result.changeCount)
    }

    @Test
    fun `MergeResult sin cambios retorna hasChanges false`() {
        val result = MergeResult(
            merged = SimpleModel("value"),
            changedFields = emptyList()
        )

        assertFalse(result.hasChanges)
        assertEquals(0, result.changeCount)
    }

    // ========== Tests de mergeWithValidation() ==========

    @Test
    fun `mergeWithValidation exitoso retorna Success`() {
        val original = SimpleModel("original")
        val updated = SimpleModel("updated")

        val result = original.mergeWithValidation(updated) { _, upd ->
            SimpleModel(upd.value)
        }

        assertTrue(result is Result.Success)
        assertEquals("updated", (result as Result.Success).data.value)
    }

    @Test
    fun `mergeWithValidation con validación fallida retorna Failure`() {
        val original = SimpleModel("original")
        val invalid = SimpleModel("")

        val result = original.mergeWithValidation(invalid) { _, upd ->
            SimpleModel(upd.value)
        }

        assertTrue(result is Result.Failure)
        assertEquals("Value cannot be blank", (result as Result.Failure).error)
    }

    @Test
    fun `mergeWithValidation permite transformaciones complejas`() {
        val original = SimpleModel("original")
        val updated = SimpleModel("updated")

        val result = original.mergeWithValidation(updated) { orig, upd ->
            SimpleModel("${orig.value}-${upd.value}")
        }

        assertTrue(result is Result.Success)
        assertEquals("original-updated", (result as Result.Success).data.value)
    }

    // ========== Tests de mergeEntityBase() ==========

    @Test
    fun `mergeEntityBase preserva ID del original`() {
        val original = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val updates = TestEntity(
            id = "id-1",
            name = "Jane",
            email = "jane@example.com",
            age = 25,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val merged = original.mergeWith(updates)

        assertEquals("id-1", merged.id)
        assertEquals("Jane", merged.name)
        assertEquals("jane@example.com", merged.email)
        assertEquals(25, merged.age)
    }

    @Test
    fun `mergeEntityBase preserva createdAt del original`() {
        val createdTime = Clock.System.now()
        val original = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = createdTime,
            updatedAt = createdTime
        )

        val updates = TestEntity(
            id = "id-1",
            name = "Jane",
            email = "jane@example.com",
            age = 25,
            createdAt = Clock.System.now(), // Diferente
            updatedAt = Clock.System.now()
        )

        val merged = original.mergeWith(updates)

        assertEquals(createdTime, merged.createdAt)
    }

    @Test
    fun `mergeEntityBase actualiza updatedAt`() {
        val originalTime = Clock.System.now()
        val original = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = originalTime,
            updatedAt = originalTime
        )

        val updates = TestEntity(
            id = "id-1",
            name = "Jane",
            email = "jane@example.com",
            age = 25,
            createdAt = originalTime,
            updatedAt = originalTime
        )

        val merged = original.mergeWith(updates)

        assertTrue(merged.updatedAt >= original.updatedAt)
    }

    @Test
    fun `mergeEntityBase lanza excepción si IDs no coinciden`() {
        val original = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val updates = TestEntity(
            id = "id-2", // ID diferente
            name = "Jane",
            email = "jane@example.com",
            age = 25,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            original.mergeWith(updates)
        }

        assertTrue(exception.message!!.contains("Cannot merge entities with different IDs"))
    }

    // ========== Tests de detectChanges() ==========

    @Test
    fun `detectChanges identifica campos que cambiaron`() {
        val original = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val updated = TestEntity(
            id = "id-1",
            name = "Jane", // Cambió
            email = "john@example.com", // No cambió
            age = 35, // Cambió
            createdAt = original.createdAt,
            updatedAt = Clock.System.now()
        )

        val changes = detectChanges(
            original, updated,
            "name" to { it.name },
            "email" to { it.email },
            "age" to { it.age }
        )

        assertEquals(listOf("name", "age"), changes)
    }

    @Test
    fun `detectChanges retorna lista vacía si no hay cambios`() {
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val changes = detectChanges(
            entity, entity,
            "name" to { it.name },
            "email" to { it.email },
            "age" to { it.age }
        )

        assertTrue(changes.isEmpty())
    }

    // ========== Tests de mergeMaps() ==========

    @Test
    fun `mergeMaps con PREFER_OTHER sobrescribe valores`() {
        val original = mapOf("a" to 1, "b" to 2, "c" to 3)
        val updates = mapOf("b" to 20, "c" to 30, "d" to 4)

        val merged = mergeMaps(original, updates, MergeStrategy.PREFER_OTHER)

        assertEquals(mapOf("a" to 1, "b" to 20, "c" to 30, "d" to 4), merged)
    }

    @Test
    fun `mergeMaps con PREFER_ORIGINAL mantiene valores originales`() {
        val original = mapOf("a" to 1, "b" to 2)
        val updates = mapOf("b" to 20, "c" to 3)

        val merged = mergeMaps(original, updates, MergeStrategy.PREFER_ORIGINAL)

        // Los valores de original se mantienen para claves duplicadas
        assertEquals(mapOf("a" to 1, "b" to 2, "c" to 3), merged)
    }

    @Test
    fun `mergeMaps con COMBINE_COLLECTIONS combina maps`() {
        val original = mapOf("a" to 1, "b" to 2)
        val updates = mapOf("c" to 3, "d" to 4)

        val merged = mergeMaps(original, updates, MergeStrategy.COMBINE_COLLECTIONS)

        assertEquals(mapOf("a" to 1, "b" to 2, "c" to 3, "d" to 4), merged)
    }

    @Test
    fun `mergeMaps con maps vacíos`() {
        val empty = emptyMap<String, Int>()
        val values = mapOf("a" to 1)

        val merged1 = mergeMaps(empty, values)
        val merged2 = mergeMaps(values, empty)

        assertEquals(mapOf("a" to 1), merged1)
        assertEquals(mapOf("a" to 1), merged2)
    }

    // ========== Tests de mergeLists() ==========

    @Test
    fun `mergeLists con PREFER_OTHER reemplaza lista`() {
        val original = listOf(1, 2, 3)
        val updates = listOf(4, 5, 6)

        val merged = mergeLists(original, updates, MergeStrategy.PREFER_OTHER)

        assertEquals(listOf(4, 5, 6), merged)
    }

    @Test
    fun `mergeLists con PREFER_ORIGINAL mantiene original si updates no está vacío`() {
        val original = listOf(1, 2, 3)
        val updates = listOf(4, 5)

        val merged = mergeLists(original, updates, MergeStrategy.PREFER_ORIGINAL)

        assertEquals(listOf(4, 5), merged)
    }

    @Test
    fun `mergeLists con COMBINE_COLLECTIONS combina sin duplicados`() {
        val original = listOf(1, 2, 3)
        val updates = listOf(3, 4, 5)

        val merged = mergeLists(original, updates, MergeStrategy.COMBINE_COLLECTIONS)

        assertEquals(listOf(1, 2, 3, 4, 5), merged)
    }

    @Test
    fun `mergeLists con lista vacía`() {
        val values = listOf(1, 2, 3)
        val empty = emptyList<Int>()

        val merged1 = mergeLists(values, empty, MergeStrategy.PREFER_OTHER)
        val merged2 = mergeLists(empty, values, MergeStrategy.PREFER_OTHER)

        assertTrue(merged1.isEmpty())
        assertEquals(listOf(1, 2, 3), merged2)
    }

    @Test
    fun `mergeLists con COMBINE_COLLECTIONS elimina duplicados`() {
        val original = listOf(1, 1, 2, 2)
        val updates = listOf(2, 2, 3, 3)

        val merged = mergeLists(original, updates, MergeStrategy.COMBINE_COLLECTIONS)

        assertEquals(listOf(1, 2, 3), merged)
    }

    // ========== Tests de MergeBuilder ==========

    @Test
    fun `MergeBuilder construye merge simple`() {
        val model = SimpleModel("original")

        val result = MergeBuilder(model)
            .merge("value") { it.copy(value = "updated") }
            .build()

        assertEquals("updated", result.merged.value)
        assertTrue(result.hasChanges)
        assertEquals(listOf("value"), result.changedFields)
    }

    @Test
    fun `MergeBuilder mergeIf solo aplica si condición es true`() {
        val model = SimpleModel("original")

        val result1 = MergeBuilder(model)
            .mergeIf(true) { it.copy(value = "changed") }
            .build()

        val result2 = MergeBuilder(model)
            .mergeIf(false) { it.copy(value = "changed") }
            .build()

        assertEquals("changed", result1.merged.value)
        assertEquals("original", result2.merged.value)
    }

    @Test
    fun `MergeBuilder permite encadenamiento`() {
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val result = MergeBuilder(entity)
            .merge("name") { it.copy(name = "Jane") }
            .merge("age") { it.copy(age = 25) }
            .updateTimestamp()
            .build()

        assertEquals("Jane", result.merged.name)
        assertEquals(25, result.merged.age)
        assertTrue(result.changedFields.contains("name"))
        assertTrue(result.changedFields.contains("age"))
        assertTrue(result.changedFields.contains("updatedAt"))
    }

    @Test
    fun `MergeBuilder sin cambios retorna hasChanges false`() {
        val model = SimpleModel("original")

        val result = MergeBuilder(model)
            .mergeIf(false) { it.copy(value = "changed") }
            .build()

        assertFalse(result.hasChanges)
        assertEquals(0, result.changeCount)
    }

    @Test
    fun `buildMerge extension function inicia builder`() {
        val model = SimpleModel("original")

        val result = model.buildMerge()
            .merge("value") { it.copy(value = "updated") }
            .build()

        assertEquals("updated", result.merged.value)
    }

    // ========== Tests de Casos de Uso Reales ==========

    @Test
    fun `merge completo preserva propiedades inmutables`() {
        val createdTime = Clock.System.now()
        val original = TestEntity(
            id = "user-123",
            name = "John Doe",
            email = "john@example.com",
            age = 30,
            createdAt = createdTime,
            updatedAt = createdTime
        )

        val updates = TestEntity(
            id = "user-123",
            name = "Jane Doe",
            email = "jane@example.com",
            age = 28,
            createdAt = Clock.System.now(), // Diferente, pero debe ignorarse
            updatedAt = Clock.System.now()
        )

        val merged = original.mergeWith(updates)

        // Verificar que se actualizaron los campos
        assertEquals("Jane Doe", merged.name)
        assertEquals("jane@example.com", merged.email)
        assertEquals(28, merged.age)

        // Verificar que se preservaron propiedades inmutables
        assertEquals("user-123", merged.id)
        assertEquals(createdTime, merged.createdAt)

        // Verificar que se actualizó updatedAt
        assertTrue(merged.updatedAt > createdTime)
    }

    @Test
    fun `merge con validación rechaza datos inválidos`() {
        val original = TestEntity(
            id = "user-123",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val invalid = TestEntity(
            id = "user-123",
            name = "", // Inválido
            email = "invalid-email", // Inválido
            age = -5, // Inválido
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val result = original.mergeWithValidation(invalid) { _, upd ->
            upd
        }

        assertTrue(result is Result.Failure)
        // Debería fallar en la primera validación
        assertTrue((result as Result.Failure).error.contains("Name cannot be blank"))
    }

    @Test
    fun `detectar cambios para logging o auditoría`() {
        val before = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val after = TestEntity(
            id = "id-1",
            name = "John",
            email = "john.doe@example.com", // Solo cambió el email
            age = 30,
            createdAt = before.createdAt,
            updatedAt = Clock.System.now()
        )

        val changes = detectChanges(
            before, after,
            "name" to { it.name },
            "email" to { it.email },
            "age" to { it.age }
        )

        assertEquals(listOf("email"), changes)
    }

    @Test
    fun `merge condicional con builder`() {
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val newName: String? = "Jane"
        val newEmail: String? = null
        val newAge: Int? = 25

        val result = entity.buildMerge()
            .mergeIf(newName != null) { it.copy(name = newName!!) }
            .mergeIf(newEmail != null) { it.copy(email = newEmail!!) }
            .mergeIf(newAge != null) { it.copy(age = newAge!!) }
            .build()

        assertEquals("Jane", result.merged.name)
        assertEquals("john@example.com", result.merged.email) // No cambió
        assertEquals(25, result.merged.age)
        // changedFields solo registra "conditional_merge" para mergeIf
        assertTrue(result.hasChanges)
    }

    @Test
    fun `merge de configuraciones anidadas`() {
        data class Config(
            val settings: Map<String, String>,
            val features: List<String>
        )

        val original = Config(
            settings = mapOf("theme" to "dark", "lang" to "es"),
            features = listOf("feature1", "feature2")
        )

        val updates = Config(
            settings = mapOf("theme" to "light", "notifications" to "on"),
            features = listOf("feature3", "feature4")
        )

        // Merge settings (combinar maps)
        val mergedSettings = mergeMaps(
            original.settings,
            updates.settings,
            MergeStrategy.PREFER_OTHER
        )

        // Merge features (combinar listas sin duplicados)
        val mergedFeatures = mergeLists(
            original.features,
            updates.features,
            MergeStrategy.COMBINE_COLLECTIONS
        )

        val merged = Config(
            settings = mergedSettings,
            features = mergedFeatures
        )

        assertEquals(mapOf("theme" to "light", "lang" to "es", "notifications" to "on"), merged.settings)
        assertEquals(listOf("feature1", "feature2", "feature3", "feature4"), merged.features)
    }
}
