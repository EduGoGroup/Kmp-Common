package com.edugo.test.module.data.models.base

import com.edugo.test.module.core.Result
import com.edugo.test.module.core.failure
import com.edugo.test.module.core.success
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Suite de tests para ValidatableModel.
 *
 * Verifica:
 * - Validaciones retornan Result<Unit> correcto
 * - Validaciones simples y complejas
 * - Composición con EntityBase
 * - Extension functions (validateAndThen, validateAll)
 * - Casos de éxito y falla
 */
class ValidatableModelTest {

    // ========== Test Entities ==========

    @Serializable
    data class SimpleValidatable(
        val value: String
    ) : ValidatableModel {
        override fun validate(): Result<Unit> {
            return when {
                value.isBlank() -> failure("Value cannot be blank")
                else -> success(Unit)
            }
        }
    }

    @Serializable
    data class ComplexValidatable(
        val email: String,
        val age: Int,
        val username: String
    ) : ValidatableModel {
        override fun validate(): Result<Unit> {
            return when {
                email.isBlank() ->
                    failure("Email is required")
                !email.contains("@") ->
                    failure("Email must contain @")
                age < 0 ->
                    failure("Age cannot be negative")
                age > 150 ->
                    failure("Age exceeds maximum (150)")
                username.length < 3 ->
                    failure("Username must be at least 3 characters")
                username.length > 30 ->
                    failure("Username must be at most 30 characters")
                else ->
                    success(Unit)
            }
        }
    }

    @Serializable
    data class EntityWithValidation(
        override val id: String,
        val name: String,
        val price: Double,
        override val createdAt: Instant,
        override val updatedAt: Instant
    ) : EntityBase<String>, ValidatableModel {
        override fun validate(): Result<Unit> {
            return when {
                name.isBlank() -> failure("Name is required")
                price < 0 -> failure("Price cannot be negative")
                price > 1_000_000 -> failure("Price exceeds maximum")
                else -> success(Unit)
            }
        }
    }

    @Serializable
    data class AlwaysValid(
        val data: String
    ) : ValidatableModel {
        override fun validate(): Result<Unit> = success(Unit)
    }

    // ========== Tests de Validación Simple ==========

    @Test
    fun `validación exitosa retorna Success`() {
        val model = SimpleValidatable(value = "valid value")
        val result = model.validate()

        assertTrue(result is Result.Success)
    }

    @Test
    fun `validación fallida retorna Failure con mensaje`() {
        val model = SimpleValidatable(value = "")
        val result = model.validate()

        assertTrue(result is Result.Failure)
        assertEquals("Value cannot be blank", (result as Result.Failure).error)
    }

    @Test
    fun `validación con valor no blank es exitosa`() {
        val model = SimpleValidatable(value = "some text")
        val result = model.validate()

        assertTrue(result is Result.Success)
    }

    // ========== Tests de Validación Compleja ==========

    @Test
    fun `validación compleja exitosa con todos los campos válidos`() {
        val model = ComplexValidatable(
            email = "test@example.com",
            age = 25,
            username = "validuser"
        )

        val result = model.validate()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `validación falla si email está vacío`() {
        val model = ComplexValidatable(
            email = "",
            age = 25,
            username = "validuser"
        )

        val result = model.validate()
        assertTrue(result is Result.Failure)
        assertEquals("Email is required", (result as Result.Failure).error)
    }

    @Test
    fun `validación falla si email no contiene @`() {
        val model = ComplexValidatable(
            email = "invalidemail.com",
            age = 25,
            username = "validuser"
        )

        val result = model.validate()
        assertTrue(result is Result.Failure)
        assertEquals("Email must contain @", (result as Result.Failure).error)
    }

    @Test
    fun `validación falla si age es negativo`() {
        val model = ComplexValidatable(
            email = "test@example.com",
            age = -5,
            username = "validuser"
        )

        val result = model.validate()
        assertTrue(result is Result.Failure)
        assertEquals("Age cannot be negative", (result as Result.Failure).error)
    }

    @Test
    fun `validación falla si age excede máximo`() {
        val model = ComplexValidatable(
            email = "test@example.com",
            age = 200,
            username = "validuser"
        )

        val result = model.validate()
        assertTrue(result is Result.Failure)
        assertEquals("Age exceeds maximum (150)", (result as Result.Failure).error)
    }

    @Test
    fun `validación falla si username es muy corto`() {
        val model = ComplexValidatable(
            email = "test@example.com",
            age = 25,
            username = "ab"
        )

        val result = model.validate()
        assertTrue(result is Result.Failure)
        assertEquals("Username must be at least 3 characters", (result as Result.Failure).error)
    }

    @Test
    fun `validación falla si username es muy largo`() {
        val model = ComplexValidatable(
            email = "test@example.com",
            age = 25,
            username = "a".repeat(31)
        )

        val result = model.validate()
        assertTrue(result is Result.Failure)
        assertEquals("Username must be at most 30 characters", (result as Result.Failure).error)
    }

    @Test
    fun `validación con username en límite inferior es exitosa`() {
        val model = ComplexValidatable(
            email = "test@example.com",
            age = 25,
            username = "abc" // Exactamente 3 caracteres
        )

        val result = model.validate()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `validación con username en límite superior es exitosa`() {
        val model = ComplexValidatable(
            email = "test@example.com",
            age = 25,
            username = "a".repeat(30) // Exactamente 30 caracteres
        )

        val result = model.validate()
        assertTrue(result is Result.Success)
    }

    // ========== Tests de Composición con EntityBase ==========

    @Test
    fun `validación funciona con EntityBase composition`() {
        val entity = EntityWithValidation(
            id = "product-123",
            name = "Test Product",
            price = 99.99,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val result = entity.validate()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `validación falla si name está vacío en entity composition`() {
        val entity = EntityWithValidation(
            id = "product-123",
            name = "",
            price = 99.99,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val result = entity.validate()
        assertTrue(result is Result.Failure)
        assertEquals("Name is required", (result as Result.Failure).error)
    }

    @Test
    fun `validación falla si price es negativo en entity composition`() {
        val entity = EntityWithValidation(
            id = "product-123",
            name = "Test Product",
            price = -10.0,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val result = entity.validate()
        assertTrue(result is Result.Failure)
        assertEquals("Price cannot be negative", (result as Result.Failure).error)
    }

    @Test
    fun `validación falla si price excede máximo en entity composition`() {
        val entity = EntityWithValidation(
            id = "product-123",
            name = "Test Product",
            price = 2_000_000.0,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val result = entity.validate()
        assertTrue(result is Result.Failure)
        assertEquals("Price exceeds maximum", (result as Result.Failure).error)
    }

    // ========== Tests de Extension Functions ==========

    @Test
    fun `validateAndThen ejecuta acción si validación pasa`() {
        val model = SimpleValidatable(value = "valid")
        var actionExecuted = false

        val result = model.validateAndThen {
            actionExecuted = true
            success("Action completed")
        }

        assertTrue(actionExecuted)
        assertTrue(result is Result.Success)
        assertEquals("Action completed", (result as Result.Success).data)
    }

    @Test
    fun `validateAndThen no ejecuta acción si validación falla`() {
        val model = SimpleValidatable(value = "")
        var actionExecuted = false

        val result = model.validateAndThen {
            actionExecuted = true
            success("This should not execute")
        }

        // La acción no debe ejecutarse
        assertTrue(!actionExecuted)
        assertTrue(result is Result.Failure)
        assertEquals("Value cannot be blank", (result as Result.Failure).error)
    }

    @Test
    fun `validateAndThen propaga error de validación`() {
        val model = ComplexValidatable(
            email = "invalid",
            age = 25,
            username = "user"
        )

        val result = model.validateAndThen<String> {
            success("Should not reach here")
        }

        assertTrue(result is Result.Failure)
        assertEquals("Email must contain @", (result as Result.Failure).error)
    }

    @Test
    fun `validateAll retorna Success si todas las validaciones pasan`() {
        val models = listOf(
            SimpleValidatable(value = "first"),
            SimpleValidatable(value = "second"),
            SimpleValidatable(value = "third")
        )

        val result = models.validateAll()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `validateAll retorna Failure si una validación falla`() {
        val models = listOf(
            SimpleValidatable(value = "valid"),
            SimpleValidatable(value = ""), // Esta falla
            SimpleValidatable(value = "also valid")
        )

        val result = models.validateAll()
        assertTrue(result is Result.Failure)
        assertEquals("Value cannot be blank", (result as Result.Failure).error)
    }

    @Test
    fun `validateAll retorna el primer error encontrado`() {
        val models = listOf(
            ComplexValidatable(email = "invalid", age = 25, username = "user"), // Falla: no @
            ComplexValidatable(email = "test@test.com", age = -1, username = "user"), // Falla: age negativo
            ComplexValidatable(email = "valid@test.com", age = 25, username = "user")
        )

        val result = models.validateAll()
        assertTrue(result is Result.Failure)
        // Debe retornar el error del primer modelo que falla
        assertEquals("Email must contain @", (result as Result.Failure).error)
    }

    @Test
    fun `validateAll con lista vacía retorna Success`() {
        val models = emptyList<ValidatableModel>()
        val result = models.validateAll()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `validateAll con lista de un elemento válido retorna Success`() {
        val models = listOf(SimpleValidatable(value = "single"))
        val result = models.validateAll()
        assertTrue(result is Result.Success)
    }

    // ========== Tests de Casos de Uso Reales ==========

    @Test
    fun `validación antes de guardar en base de datos`() {
        val entity = EntityWithValidation(
            id = "new-product",
            name = "New Product",
            price = 49.99,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        // Simular validación antes de guardar
        val validationResult = entity.validate()
        assertTrue(validationResult is Result.Success)

        // Si pasa la validación, podríamos guardar
        val saveResult = entity.validateAndThen {
            // Aquí iría la lógica de guardado
            success(entity)
        }

        assertTrue(saveResult is Result.Success)
        assertEquals(entity, (saveResult as Result.Success).data)
    }

    @Test
    fun `validación rechaza datos inválidos antes de guardar`() {
        val entity = EntityWithValidation(
            id = "invalid-product",
            name = "", // Nombre vacío - inválido
            price = 49.99,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val validationResult = entity.validate()
        assertTrue(validationResult is Result.Failure)

        // El guardado no debería ejecutarse
        val saveResult = entity.validateAndThen {
            success(entity)
        }

        assertTrue(saveResult is Result.Failure)
        assertEquals("Name is required", (saveResult as Result.Failure).error)
    }

    @Test
    fun `validación múltiple de batch de entidades`() {
        val entities = listOf(
            EntityWithValidation(
                id = "prod-1",
                name = "Product 1",
                price = 10.0,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            ),
            EntityWithValidation(
                id = "prod-2",
                name = "Product 2",
                price = 20.0,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            ),
            EntityWithValidation(
                id = "prod-3",
                name = "Product 3",
                price = 30.0,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
        )

        val result = entities.validateAll()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `modelo siempre válido retorna Success`() {
        val model = AlwaysValid(data = "anything")
        val result = model.validate()
        assertTrue(result is Result.Success)
    }

    // ========== Tests de Valores Límite ==========

    @Test
    fun `validación con age en valor límite 0 es exitosa`() {
        val model = ComplexValidatable(
            email = "test@example.com",
            age = 0,
            username = "user"
        )

        val result = model.validate()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `validación con age en valor límite 150 es exitosa`() {
        val model = ComplexValidatable(
            email = "test@example.com",
            age = 150,
            username = "user"
        )

        val result = model.validate()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `validación con price en valor límite 0 es exitosa`() {
        val entity = EntityWithValidation(
            id = "prod-free",
            name = "Free Product",
            price = 0.0,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val result = entity.validate()
        assertTrue(result is Result.Success)
    }

    @Test
    fun `validación con price en valor límite máximo es exitosa`() {
        val entity = EntityWithValidation(
            id = "prod-expensive",
            name = "Expensive Product",
            price = 1_000_000.0,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val result = entity.validate()
        assertTrue(result is Result.Success)
    }
}
