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

/**
 * Suite de tests para AuditableModel.
 *
 * Verifica:
 * - Campos de auditoría (createdBy, updatedBy)
 * - Composición con EntityBase y ValidatableModel
 * - Preservación de createdBy en actualizaciones
 * - Serialización de información de auditoría
 * - Helper function createAuditInfo
 * - AuditInfo data class
 */
class AuditableModelTest {

    // ========== Test Entities ==========

    @Serializable
    data class SimpleAuditable(
        val title: String,
        override val createdBy: String,
        override val updatedBy: String
    ) : AuditableModel

    @Serializable
    data class AuditableEntity(
        override val id: String,
        val content: String,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        override val createdBy: String,
        override val updatedBy: String
    ) : EntityBase<String>, AuditableModel

    @Serializable
    data class FullyAuditedEntity(
        override val id: String,
        val title: String,
        val description: String,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        override val createdBy: String,
        override val updatedBy: String
    ) : EntityBase<String>, AuditableModel, ValidatableModel {
        override fun validate(): Result<Unit> {
            return when {
                title.isBlank() -> failure("Title is required")
                createdBy.isBlank() -> failure("Creator information is required")
                updatedBy.isBlank() -> failure("Updater information is required")
                else -> success(Unit)
            }
        }
    }

    // ========== Tests de Propiedades Básicas ==========

    @Test
    fun `AuditableModel tiene propiedades createdBy y updatedBy`() {
        val auditable = SimpleAuditable(
            title = "Test",
            createdBy = "user-1",
            updatedBy = "user-1"
        )

        assertEquals("user-1", auditable.createdBy)
        assertEquals("user-1", auditable.updatedBy)
    }

    @Test
    fun `createdBy y updatedBy pueden ser diferentes`() {
        val auditable = SimpleAuditable(
            title = "Test",
            createdBy = "user-1",
            updatedBy = "user-2"
        )

        assertEquals("user-1", auditable.createdBy)
        assertEquals("user-2", auditable.updatedBy)
    }

    @Test
    fun `createdBy y updatedBy aceptan diferentes formatos de identificador`() {
        // Formato: email
        val auditable1 = SimpleAuditable(
            title = "Test 1",
            createdBy = "john@example.com",
            updatedBy = "jane@example.com"
        )

        // Formato: username
        val auditable2 = SimpleAuditable(
            title = "Test 2",
            createdBy = "johndoe",
            updatedBy = "janedoe"
        )

        // Formato: ID estructurado
        val auditable3 = SimpleAuditable(
            title = "Test 3",
            createdBy = "user-123|john@example.com",
            updatedBy = "user-456|jane@example.com"
        )

        assertEquals("john@example.com", auditable1.createdBy)
        assertEquals("johndoe", auditable2.createdBy)
        assertEquals("user-123|john@example.com", auditable3.createdBy)
    }

    // ========== Tests de Composición con EntityBase ==========

    @Test
    fun `AuditableModel se compone correctamente con EntityBase`() {
        val entity = AuditableEntity(
            id = "doc-123",
            content = "Document content",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "user-1",
            updatedBy = "user-1"
        )

        // Verificar propiedades de EntityBase
        assertEquals("doc-123", entity.id)
        assertNotNull(entity.createdAt)
        assertNotNull(entity.updatedAt)

        // Verificar propiedades de AuditableModel
        assertEquals("user-1", entity.createdBy)
        assertEquals("user-1", entity.updatedBy)
    }

    @Test
    fun `createdBy se preserva en actualizaciones`() {
        val original = AuditableEntity(
            id = "doc-1",
            content = "Original content",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "user-1",
            updatedBy = "user-1"
        )

        // Simular actualización por un usuario diferente
        val updated = original.copy(
            content = "Updated content",
            updatedAt = Clock.System.now(),
            updatedBy = "user-2"
        )

        // createdBy NO debe cambiar
        assertEquals("user-1", updated.createdBy)
        // updatedBy debe reflejar el nuevo usuario
        assertEquals("user-2", updated.updatedBy)
        // El contenido debe actualizarse
        assertEquals("Updated content", updated.content)
    }

    @Test
    fun `múltiples actualizaciones preservan createdBy original`() {
        var entity = AuditableEntity(
            id = "doc-1",
            content = "Version 1",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "creator-user",
            updatedBy = "creator-user"
        )

        // Primera actualización
        entity = entity.copy(
            content = "Version 2",
            updatedAt = Clock.System.now(),
            updatedBy = "editor-1"
        )

        // Segunda actualización
        entity = entity.copy(
            content = "Version 3",
            updatedAt = Clock.System.now(),
            updatedBy = "editor-2"
        )

        // Tercera actualización
        entity = entity.copy(
            content = "Version 4",
            updatedAt = Clock.System.now(),
            updatedBy = "editor-3"
        )

        // createdBy debe seguir siendo el usuario original
        assertEquals("creator-user", entity.createdBy)
        // updatedBy debe ser el último editor
        assertEquals("editor-3", entity.updatedBy)
        assertEquals("Version 4", entity.content)
    }

    // ========== Tests de Composición con ValidatableModel ==========

    @Test
    fun `AuditableModel se compone con ValidatableModel`() {
        val entity = FullyAuditedEntity(
            id = "entity-1",
            title = "Valid Title",
            description = "Description",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "user-1",
            updatedBy = "user-1"
        )

        val result = entity.validate()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `validación falla si createdBy está vacío`() {
        val entity = FullyAuditedEntity(
            id = "entity-1",
            title = "Valid Title",
            description = "Description",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "",
            updatedBy = "user-1"
        )

        val result = entity.validate()
        assertTrue(result is Result.Failure)
        assertEquals("Creator information is required", (result as Result.Failure).error)
    }

    @Test
    fun `validación falla si updatedBy está vacío`() {
        val entity = FullyAuditedEntity(
            id = "entity-1",
            title = "Valid Title",
            description = "Description",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "user-1",
            updatedBy = ""
        )

        val result = entity.validate()
        assertTrue(result is Result.Failure)
        assertEquals("Updater information is required", (result as Result.Failure).error)
    }

    @Test
    fun `validación completa verifica todos los campos incluyendo auditoría`() {
        // Caso válido
        val validEntity = FullyAuditedEntity(
            id = "entity-1",
            title = "Title",
            description = "Desc",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "user-1",
            updatedBy = "user-1"
        )
        assertTrue(validEntity.validate() is Result.Success)

        // Caso inválido: título vacío
        val invalidTitle = validEntity.copy(title = "")
        assertTrue(invalidTitle.validate() is Result.Failure)

        // Caso inválido: createdBy vacío
        val invalidCreator = validEntity.copy(createdBy = "")
        assertTrue(invalidCreator.validate() is Result.Failure)

        // Caso inválido: updatedBy vacío
        val invalidUpdater = validEntity.copy(updatedBy = "")
        assertTrue(invalidUpdater.validate() is Result.Failure)
    }

    // ========== Tests de Serialización ==========

    @Test
    fun `AuditableModel se serializa correctamente`() {
        val entity = AuditableEntity(
            id = "doc-1",
            content = "Test content",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "creator@example.com",
            updatedBy = "editor@example.com"
        )

        val json = Json.encodeToString(entity)

        assertTrue(json.contains("\"createdBy\":\"creator@example.com\""))
        assertTrue(json.contains("\"updatedBy\":\"editor@example.com\""))
    }

    @Test
    fun `AuditableModel se deserializa correctamente`() {
        val original = AuditableEntity(
            id = "doc-1",
            content = "Content",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "user-create",
            updatedBy = "user-update"
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<AuditableEntity>(json)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.content, deserialized.content)
        assertEquals(original.createdBy, deserialized.createdBy)
        assertEquals(original.updatedBy, deserialized.updatedBy)
    }

    @Test
    fun `serialización round-trip preserva información de auditoría`() {
        val original = FullyAuditedEntity(
            id = "entity-123",
            title = "Test Title",
            description = "Test Description",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = "original-creator",
            updatedBy = "latest-editor"
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<FullyAuditedEntity>(json)

        assertEquals(original, deserialized)
        assertEquals(original.createdBy, deserialized.createdBy)
        assertEquals(original.updatedBy, deserialized.updatedBy)
    }

    // ========== Tests de AuditInfo Helper ==========

    @Test
    fun `createAuditInfo crea objeto con información básica`() {
        val auditInfo = createAuditInfo(
            userId = "user-123",
            userName = "John Doe"
        )

        assertEquals("user-123", auditInfo.userId)
        assertEquals("John Doe", auditInfo.userName)
        assertNotNull(auditInfo.timestamp)
        assertEquals(null, auditInfo.ipAddress)
        assertEquals(null, auditInfo.userAgent)
    }

    @Test
    fun `createAuditInfo crea objeto con información completa`() {
        val auditInfo = createAuditInfo(
            userId = "user-456",
            userName = "Jane Smith",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        assertEquals("user-456", auditInfo.userId)
        assertEquals("Jane Smith", auditInfo.userName)
        assertEquals("192.168.1.1", auditInfo.ipAddress)
        assertEquals("Mozilla/5.0", auditInfo.userAgent)
        assertNotNull(auditInfo.timestamp)
    }

    @Test
    fun `AuditInfo se serializa correctamente`() {
        val auditInfo = createAuditInfo(
            userId = "user-789",
            userName = "Test User",
            ipAddress = "10.0.0.1"
        )

        val json = Json.encodeToString(auditInfo)

        assertTrue(json.contains("\"userId\":\"user-789\""))
        assertTrue(json.contains("\"userName\":\"Test User\""))
        assertTrue(json.contains("\"ipAddress\":\"10.0.0.1\""))
    }

    @Test
    fun `AuditInfo round-trip serialization`() {
        val original = createAuditInfo(
            userId = "user-111",
            userName = "Round Trip User",
            ipAddress = "127.0.0.1",
            userAgent = "Test Agent"
        )

        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<AuditInfo>(json)

        assertEquals(original.userId, deserialized.userId)
        assertEquals(original.userName, deserialized.userName)
        assertEquals(original.ipAddress, deserialized.ipAddress)
        assertEquals(original.userAgent, deserialized.userAgent)
        assertEquals(original.timestamp, deserialized.timestamp)
    }

    // ========== Tests de Casos de Uso Reales ==========

    @Test
    fun `simulación de creación de documento por usuario`() {
        val currentUserId = "user-create-123"

        val document = AuditableEntity(
            id = "new-doc",
            content = "Initial content",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = currentUserId,
            updatedBy = currentUserId
        )

        // En creación, createdBy y updatedBy son el mismo
        assertEquals(currentUserId, document.createdBy)
        assertEquals(currentUserId, document.updatedBy)
    }

    @Test
    fun `simulación de actualización de documento por otro usuario`() {
        val creatorId = "user-creator"
        val editorId = "user-editor"

        // Documento original creado por user-creator
        val original = AuditableEntity(
            id = "doc-1",
            content = "Original",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = creatorId,
            updatedBy = creatorId
        )

        // Actualización por user-editor
        val updated = original.copy(
            content = "Edited by another user",
            updatedAt = Clock.System.now(),
            updatedBy = editorId
        )

        assertEquals(creatorId, updated.createdBy)
        assertEquals(editorId, updated.updatedBy)
    }

    @Test
    fun `trazabilidad completa de múltiples editores`() {
        val creator = "alice"
        var document = AuditableEntity(
            id = "collaborative-doc",
            content = "V1 by Alice",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = creator,
            updatedBy = creator
        )

        // Bob edita
        document = document.copy(
            content = "V2 by Bob",
            updatedAt = Clock.System.now(),
            updatedBy = "bob"
        )

        // Charlie edita
        document = document.copy(
            content = "V3 by Charlie",
            updatedAt = Clock.System.now(),
            updatedBy = "charlie"
        )

        // Dave edita
        document = document.copy(
            content = "V4 by Dave",
            updatedAt = Clock.System.now(),
            updatedBy = "dave"
        )

        // El creador original siempre es Alice
        assertEquals("alice", document.createdBy)
        // El último editor es Dave
        assertEquals("dave", document.updatedBy)
    }

    @Test
    fun `auditoría con información estructurada de usuario`() {
        // Información estructurada: "userId|email"
        val userInfo = "user-123|john@example.com"

        val entity = AuditableEntity(
            id = "doc-structured",
            content = "Content",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = userInfo,
            updatedBy = userInfo
        )

        // Podemos extraer información del formato estructurado
        val parts = entity.createdBy.split("|")
        assertEquals(2, parts.size)
        assertEquals("user-123", parts[0])
        assertEquals("john@example.com", parts[1])
    }

    @Test
    fun `validación de permisos usando información de auditoría`() {
        val originalCreator = "user-owner"
        val currentEditor = "user-editor"

        val entity = AuditableEntity(
            id = "protected-doc",
            content = "Protected content",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            createdBy = originalCreator,
            updatedBy = originalCreator
        )

        // Función helper para verificar si el usuario puede editar
        fun canEdit(entity: AuditableEntity, userId: String): Boolean {
            // En un caso real, verificaríamos permisos más complejos
            return entity.createdBy == userId || userId == "admin"
        }

        // El creador puede editar
        assertTrue(canEdit(entity, originalCreator))

        // Otro usuario no puede editar (a menos que sea admin)
        assertTrue(!canEdit(entity, currentEditor))

        // Admin siempre puede editar
        assertTrue(canEdit(entity, "admin"))
    }

    @Test
    fun `lista de entidades auditables mantiene información de múltiples usuarios`() {
        val entities = listOf(
            AuditableEntity(
                id = "doc-1",
                content = "Content 1",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                createdBy = "user-1",
                updatedBy = "user-1"
            ),
            AuditableEntity(
                id = "doc-2",
                content = "Content 2",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                createdBy = "user-2",
                updatedBy = "user-3"
            ),
            AuditableEntity(
                id = "doc-3",
                content = "Content 3",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                createdBy = "user-1",
                updatedBy = "user-2"
            )
        )

        // Obtener usuarios únicos que han creado documentos
        val creators = entities.map { it.createdBy }.toSet()
        assertEquals(setOf("user-1", "user-2"), creators)

        // Obtener usuarios únicos que han editado documentos
        val editors = entities.map { it.updatedBy }.toSet()
        assertEquals(setOf("user-1", "user-2", "user-3"), editors)
    }
}
