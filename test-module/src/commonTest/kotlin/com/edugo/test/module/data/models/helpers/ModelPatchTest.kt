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
 * Suite de tests para helpers de patch (actualización parcial) de modelos.
 *
 * Verifica:
 * - PatchOperation y sus operaciones
 * - patchField() para actualización condicional
 * - patchFieldWithValidation() con ValidatableModel
 * - applyPatches() para múltiples patches
 * - PatchBuilder para construcción fluida
 * - patchEntityBase() preservando propiedades inmutables
 * - patchAllowedFields() para seguridad
 * - Casos de preservación de createdAt y actualización de updatedAt
 */
class ModelPatchTest {

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

        data class Patch(
            val name: String? = null,
            val email: String? = null,
            val age: Int? = null
        )

        fun applyPatch(patch: Patch): TestEntity {
            return patchEntityBase { original ->
                original
                    .patchField(patch.name) { copy(name = it, updatedAt = Clock.System.now()) }
                    .patchField(patch.email) { copy(email = it, updatedAt = Clock.System.now()) }
                    .patchField(patch.age) { copy(age = it, updatedAt = Clock.System.now()) }
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

    // ========== Tests de PatchOperation ==========

    @Test
    fun `PatchOperation se crea correctamente`() {
        val patch = PatchOperation<TestEntity>(
            fields = mapOf("name" to "John", "age" to 30)
        )

        assertEquals(2, patch.fieldCount)
        assertTrue(patch.isNotEmpty)
    }

    @Test
    fun `PatchOperation hasField verifica existencia de campo`() {
        val patch = PatchOperation<TestEntity>(
            fields = mapOf("name" to "John")
        )

        assertTrue(patch.hasField("name"))
        assertFalse(patch.hasField("email"))
    }

    @Test
    fun `PatchOperation getField obtiene valor`() {
        val patch = PatchOperation<TestEntity>(
            fields = mapOf("name" to "John", "age" to 30)
        )

        assertEquals("John", patch.getField<String>("name"))
        assertEquals(30, patch.getField<Int>("age"))
        assertEquals(null, patch.getField<String>("email"))
    }

    @Test
    fun `PatchOperation withField agrega campo`() {
        val patch = PatchOperation<TestEntity>(fields = mapOf("name" to "John"))
        val updated = patch.withField("age", 30)

        assertEquals(1, patch.fieldCount)
        assertEquals(2, updated.fieldCount)
        assertTrue(updated.hasField("age"))
    }

    @Test
    fun `PatchOperation withoutField remueve campo`() {
        val patch = PatchOperation<TestEntity>(
            fields = mapOf("name" to "John", "age" to 30)
        )
        val updated = patch.withoutField("age")

        assertEquals(2, patch.fieldCount)
        assertEquals(1, updated.fieldCount)
        assertFalse(updated.hasField("age"))
    }

    @Test
    fun `PatchOperation vacío retorna isEmpty true`() {
        val patch = PatchOperation<TestEntity>(fields = emptyMap())

        assertTrue(patch.isEmpty)
        assertFalse(patch.isNotEmpty)
        assertEquals(0, patch.fieldCount)
    }

    // ========== Tests de PatchResult ==========

    @Test
    fun `PatchResult con patches retorna hasPatches true`() {
        val result = PatchResult(
            patched = SimpleModel("value"),
            appliedFields = listOf("value")
        )

        assertTrue(result.hasPatches)
        assertEquals(1, result.patchCount)
    }

    @Test
    fun `PatchResult sin patches retorna hasPatches false`() {
        val result = PatchResult(
            patched = SimpleModel("value"),
            appliedFields = emptyList()
        )

        assertFalse(result.hasPatches)
        assertEquals(0, result.patchCount)
    }

    // ========== Tests de patchField() ==========

    @Test
    fun `patchField aplica patch si valor no es null`() {
        val model = SimpleModel("original")
        val patched = model.patchField("updated") { copy(value = it) }

        assertEquals("updated", patched.value)
    }

    @Test
    fun `patchField no aplica patch si valor es null`() {
        val model = SimpleModel("original")
        val patched = model.patchField(null) { copy(value = it) }

        assertEquals("original", patched.value)
    }

    @Test
    fun `patchField se puede encadenar`() {
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val patched = entity
            .patchField("Jane") { copy(name = it) }
            .patchField("jane@example.com") { copy(email = it) }
            .patchField(null) { copy(age = it) } // No se aplica

        assertEquals("Jane", patched.name)
        assertEquals("jane@example.com", patched.email)
        assertEquals(30, patched.age) // No cambió
    }

    // ========== Tests de patchFieldWithValidation() ==========

    @Test
    fun `patchFieldWithValidation exitoso retorna Success`() {
        val model = SimpleModel("original")

        val result = model.patchFieldWithValidation("updated") {
            copy(value = it)
        }

        assertTrue(result is Result.Success)
        assertEquals("updated", (result as Result.Success).data.value)
    }

    @Test
    fun `patchFieldWithValidation con null retorna Success sin cambios`() {
        val model = SimpleModel("original")

        val result = model.patchFieldWithValidation(null) {
            copy(value = it)
        }

        assertTrue(result is Result.Success)
        assertEquals("original", (result as Result.Success).data.value)
    }

    @Test
    fun `patchFieldWithValidation con validación fallida retorna Failure`() {
        val model = SimpleModel("original")

        val result = model.patchFieldWithValidation("") {
            copy(value = it)
        }

        assertTrue(result is Result.Failure)
        assertEquals("Value cannot be blank", (result as Result.Failure).error)
    }

    // ========== Tests de applyPatches() ==========

    @Test
    fun `applyPatches aplica múltiples patches`() {
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val patched = entity.applyPatches(
            "name" to "Jane",
            "email" to "jane@example.com",
            "age" to 25
        ) { model, fieldName, value ->
            when (fieldName) {
                "name" -> model.copy(name = value as String)
                "email" -> model.copy(email = value as String)
                "age" -> model.copy(age = value as Int)
                else -> model
            }
        }

        assertEquals("Jane", patched.name)
        assertEquals("jane@example.com", patched.email)
        assertEquals(25, patched.age)
    }

    @Test
    fun `applyPatches ignora valores null`() {
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val patched = entity.applyPatches(
            "name" to "Jane",
            "email" to null, // Se ignora
            "age" to 25
        ) { model, fieldName, value ->
            when (fieldName) {
                "name" -> model.copy(name = value as String)
                "email" -> model.copy(email = value as String)
                "age" -> model.copy(age = value as Int)
                else -> model
            }
        }

        assertEquals("Jane", patched.name)
        assertEquals("john@example.com", patched.email) // No cambió
        assertEquals(25, patched.age)
    }

    // ========== Tests de PatchBuilder ==========

    @Test
    fun `PatchBuilder construye patch simple`() {
        val model = SimpleModel("original")

        val result = PatchBuilder(model)
            .patch("value", "updated") { copy(value = it) }
            .build()

        assertEquals("updated", result.patched.value)
        assertTrue(result.hasPatches)
        assertEquals(listOf("value"), result.appliedFields)
    }

    @Test
    fun `PatchBuilder patch ignora valores null`() {
        val model = SimpleModel("original")

        val result = PatchBuilder(model)
            .patch("value", null) { copy(value = it) }
            .build()

        assertEquals("original", result.patched.value)
        assertFalse(result.hasPatches)
    }

    @Test
    fun `PatchBuilder patchIf solo aplica si condición es true`() {
        val model = SimpleModel("original")

        val result1 = PatchBuilder(model)
            .patchIf(true, "value") { copy(value = "changed") }
            .build()

        val result2 = PatchBuilder(model)
            .patchIf(false, "value") { copy(value = "changed") }
            .build()

        assertEquals("changed", result1.patched.value)
        assertEquals("original", result2.patched.value)
    }

    @Test
    fun `PatchBuilder updateTimestampIfEntityBase registra cambio`() {
        val model = SimpleModel("original")

        val result = PatchBuilder(model)
            .patch("value", "updated") { copy(value = it) }
            .updateTimestampIfEntityBase()
            .build()

        assertTrue(result.appliedFields.contains("updatedAt"))
    }

    @Test
    fun `PatchBuilder validateAndBuild valida el resultado`() {
        val model = SimpleModel("original")

        // Patch válido
        val validResult = PatchBuilder(model)
            .patch("value", "updated") { copy(value = it) }
            .validateAndBuild()

        assertTrue(validResult is Result.Success)

        // Patch inválido
        val invalidResult = PatchBuilder(model)
            .patch("value", "") { copy(value = it) }
            .validateAndBuild()

        assertTrue(invalidResult is Result.Failure)
    }

    @Test
    fun `buildPatch extension function inicia builder`() {
        val model = SimpleModel("original")

        val result = model.buildPatch()
            .patch("value", "updated") { copy(value = it) }
            .build()

        assertEquals("updated", result.patched.value)
    }

    // ========== Tests de patchEntityBase() ==========

    @Test
    fun `patchEntityBase preserva ID`() {
        val entity = TestEntity(
            id = "immutable-id",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val patch = TestEntity.Patch(name = "Jane")
        val patched = entity.applyPatch(patch)

        assertEquals("immutable-id", patched.id)
        assertEquals("Jane", patched.name)
    }

    @Test
    fun `patchEntityBase preserva createdAt`() {
        val createdTime = Clock.System.now()
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = createdTime,
            updatedAt = createdTime
        )

        val patch = TestEntity.Patch(name = "Jane", age = 25)
        val patched = entity.applyPatch(patch)

        assertEquals(createdTime, patched.createdAt)
    }

    @Test
    fun `patchEntityBase actualiza updatedAt`() {
        val originalTime = Clock.System.now()
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = originalTime,
            updatedAt = originalTime
        )

        val patch = TestEntity.Patch(name = "Jane")
        val patched = entity.applyPatch(patch)

        assertTrue(patched.updatedAt > originalTime)
    }

    @Test
    fun `patchEntityBase lanza excepción si ID cambia`() {
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            entity.patchEntityBase { original ->
                original.copy(id = "different-id")
            }
        }

        assertTrue(exception.message!!.contains("ID cannot be changed during patch"))
    }

    @Test
    fun `patchEntityBase lanza excepción si createdAt cambia`() {
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            entity.patchEntityBase { original ->
                original.copy(createdAt = Clock.System.now())
            }
        }

        assertTrue(exception.message!!.contains("createdAt cannot be changed during patch"))
    }

    // ========== Tests de patchAllowedFields() ==========

    @Test
    fun `patchAllowedFields solo aplica campos permitidos`() {
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val patches = mapOf(
            "name" to "Jane",
            "email" to "jane@example.com",
            "age" to 25
        )

        val allowedFields = setOf("name", "email") // age NO está permitido

        val patched = entity.patchAllowedFields(patches, allowedFields) { model, field, value ->
            when (field) {
                "name" -> model.copy(name = value as String)
                "email" -> model.copy(email = value as String)
                "age" -> model.copy(age = value as Int)
                else -> model
            }
        }

        assertEquals("Jane", patched.name)
        assertEquals("jane@example.com", patched.email)
        assertEquals(30, patched.age) // No cambió porque no estaba en allowedFields
    }

    @Test
    fun `patchAllowedFields ignora valores null`() {
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val patches = mapOf(
            "name" to "Jane",
            "email" to null
        )

        val allowedFields = setOf("name", "email")

        val patched = entity.patchAllowedFields(patches, allowedFields) { model, field, value ->
            when (field) {
                "name" -> model.copy(name = value as String)
                "email" -> model.copy(email = value as String)
                else -> model
            }
        }

        assertEquals("Jane", patched.name)
        assertEquals("john@example.com", patched.email) // No cambió por null
    }

    // ========== Tests de applyNullSafePatch() ==========

    @Test
    fun `applyNullSafePatch aplica patches no-null`() {
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val nameUpdate: String? = "Jane"
        val emailUpdate: String? = null
        val ageUpdate: Int? = 25

        val patched = entity.applyNullSafePatch(
            nameUpdate?.let { "name" to it },
            emailUpdate?.let { "email" to it },
            ageUpdate?.let { "age" to it }
        ) { model, field, value ->
            when (field) {
                "name" -> model.copy(name = value as String)
                "email" -> model.copy(email = value as String)
                "age" -> model.copy(age = value as Int)
                else -> model
            }
        }

        assertEquals("Jane", patched.name)
        assertEquals("john@example.com", patched.email) // No cambió
        assertEquals(25, patched.age)
    }

    // ========== Tests de Helpers ==========

    @Test
    fun `toPatchOperation filtra valores null`() {
        val patch = toPatchOperation<TestEntity>(
            "name" to "John",
            "email" to null,
            "age" to 30
        )

        assertEquals(2, patch.fieldCount)
        assertTrue(patch.hasField("name"))
        assertFalse(patch.hasField("email"))
        assertTrue(patch.hasField("age"))
    }

    @Test
    fun `preserveCreatedAt retorna el mismo instant`() {
        val createdAt = Clock.System.now()
        val preserved = preserveCreatedAt(createdAt)

        assertEquals(createdAt, preserved)
    }

    @Test
    fun `updateTimestamp genera nuevo instant`() {
        val before = Clock.System.now()
        val timestamp = updateTimestamp()
        val after = Clock.System.now()

        assertTrue(timestamp >= before)
        assertTrue(timestamp <= after)
    }

    @Test
    fun `withUpdatedTimestamp proporciona timestamp actual`() {
        val before = Clock.System.now()

        val result = withUpdatedTimestamp { instant ->
            instant
        }

        val after = Clock.System.now()

        assertTrue(result >= before)
        assertTrue(result <= after)
    }

    // ========== Tests de Casos de Uso Reales ==========

    @Test
    fun `patch parcial actualiza solo campos especificados`() {
        val entity = TestEntity(
            id = "user-123",
            name = "John Doe",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val patch = TestEntity.Patch(
            name = "Jane Doe",
            email = null, // No actualizar email
            age = null // No actualizar age
        )

        val patched = entity.applyPatch(patch)

        assertEquals("Jane Doe", patched.name)
        assertEquals("john@example.com", patched.email) // Sin cambios
        assertEquals(30, patched.age) // Sin cambios
    }

    @Test
    fun `múltiples patches secuenciales`() {
        var entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        // Aplicar múltiples patches
        entity = entity.applyPatch(TestEntity.Patch(name = "Jane"))
        entity = entity.applyPatch(TestEntity.Patch(email = "jane@example.com"))
        entity = entity.applyPatch(TestEntity.Patch(age = 25))

        assertEquals("Jane", entity.name)
        assertEquals("jane@example.com", entity.email)
        assertEquals(25, entity.age)
    }

    @Test
    fun `patch con validación rechaza datos inválidos`() {
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val result = entity.buildPatch()
            .patch("name", "") { copy(name = it) }
            .validateAndBuild()

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error.contains("Name cannot be blank"))
    }

    @Test
    fun `patch builder con validación permite construcción fluida`() {
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val result = entity.buildPatch()
            .patch("name", "Jane") { copy(name = it) }
            .patch("age", 25) { copy(age = it) }
            .validateAndBuild()

        assertTrue(result is Result.Success)
        val patchResult = (result as Result.Success).data
        assertEquals("Jane", patchResult.patched.name)
        assertEquals(25, patchResult.patched.age)
        assertEquals(listOf("name", "age"), patchResult.appliedFields)
    }

    @Test
    fun `patch seguro solo permite campos específicos`() {
        val entity = TestEntity(
            id = "id-1",
            name = "John",
            email = "john@example.com",
            age = 30,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        // Usuario intenta patchear campos no permitidos
        val untrustedPatches = mapOf(
            "name" to "Hacker",
            "email" to "hacker@evil.com",
            "age" to 99,
            "id" to "fake-id" // Intentar cambiar ID (no permitido)
        )

        val allowedFields = setOf("name", "email") // Solo permitir name y email

        val patched = entity.patchAllowedFields(untrustedPatches, allowedFields) { model, field, value ->
            when (field) {
                "name" -> model.copy(name = value as String)
                "email" -> model.copy(email = value as String)
                "age" -> model.copy(age = value as Int)
                "id" -> model.copy(id = value as String) // Nunca debería ejecutarse
                else -> model
            }
        }

        assertEquals("Hacker", patched.name)
        assertEquals("hacker@evil.com", patched.email)
        assertEquals(30, patched.age) // No cambió
        assertEquals("id-1", patched.id) // No cambió (protegido)
    }
}
