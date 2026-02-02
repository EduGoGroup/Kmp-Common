package com.edugo.test.module.integration

import com.edugo.test.module.core.Result
import com.edugo.test.module.core.safeDecodeFromString
import com.edugo.test.module.core.flatMap
import com.edugo.test.module.core.map
import com.edugo.test.module.examples.User
import com.edugo.test.module.examples.UserDto
import com.edugo.test.module.examples.UserMapper
import com.edugo.test.module.extensions.sequence
import com.edugo.test.module.extensions.traverse
import com.edugo.test.module.extensions.zip3
import com.edugo.test.module.mapper.toDomain
import com.edugo.test.module.validation.accumulateValidationErrors
import com.edugo.test.module.validation.validateEmail
import com.edugo.test.module.validation.validateLengthRange
import com.edugo.test.module.validation.validateRange
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests de integración end-to-end del sistema de conversión y manejo de errores.
 *
 * Verifica:
 * - Flujo completo: JSON → DTO → Domain con validación
 * - Composición de operaciones: catchSerialization + flatMap + zip + sequence
 * - Edge cases: colecciones vacías, datos parcialmente válidos, errores anidados
 * - Integración de todos los módulos implementados
 */
class ConversionIntegrationTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ========== Test 1: Flujo Completo JSON → DTO → Domain ==========

    @Test
    fun `flujo completo deserializar JSON validar y mapear a dominio`() {
        // JSON de entrada
        val jsonString = """
            {
                "email": "john@example.com",
                "age": 25,
                "username": "johndoe"
            }
        """.trimIndent()

        // Paso 1: Deserializar JSON a DTO
        val dtoResult: Result<UserDto> = safeDecodeFromString(jsonString)

        // Paso 2: Mapear DTO a Domain (incluye validación)
        val userResult: Result<User> = dtoResult.flatMap { dto ->
            UserMapper.toDomain(dto)
        }

        // Verificar resultado exitoso
        assertIs<Result.Success<User>>(userResult)
        assertEquals("john@example.com", userResult.data.email)
        assertEquals(25, userResult.data.age)
        assertEquals("johndoe", userResult.data.username)
    }

    @Test
    fun `flujo completo falla en deserialización con JSON inválido`() {
        val invalidJson = """{ "email": "test", "age": "invalid" }"""

        val result = safeDecodeFromString<UserDto>(invalidJson)
            .flatMap { dto -> UserMapper.toDomain(dto) }

        // Debe fallar en la deserialización (cualquier mensaje de error es válido)
        assertIs<Result.Failure>(result)
        assertTrue(result.error.isNotEmpty())
    }

    @Test
    fun `flujo completo falla en validación con datos inválidos`() {
        val jsonString = """
            {
                "email": "invalid-email",
                "age": 15,
                "username": "ab"
            }
        """.trimIndent()

        val result = safeDecodeFromString<UserDto>(jsonString)
            .flatMap { dto -> UserMapper.toDomain(dto) }

        assertIs<Result.Failure>(result)
        // Debe contener múltiples errores (validación acumulativa)
        assertTrue(result.error.contains("email"))
        assertTrue(result.error.contains("Age"))
        assertTrue(result.error.contains("Username"))
    }

    // ========== Test 2: Composición Compleja de Operaciones ==========

    @Serializable
    data class ProductDto(val name: String, val price: Double, val stock: Int)
    data class Product(val name: String, val price: Double, val stock: Int)

    @Test
    fun `composición catchSerialization + flatMap + map`() {
        val jsonString = """{"name":"Laptop","price":999.99,"stock":10}"""

        val result = safeDecodeFromString<ProductDto>(jsonString)
            .flatMap { dto ->
                // Validar
                val validationResult = accumulateValidationErrors {
                    add(validateLengthRange(dto.name, 1, 100, "Name"))
                    add(validateRange(dto.price, 0.0, 1000000.0, "Price"))
                    add(validateRange(dto.stock, 0, 10000, "Stock"))
                }

                validationResult.map {
                    Product(dto.name, dto.price, dto.stock)
                }
            }

        assertIs<Result.Success<Product>>(result)
        assertEquals("Laptop", result.data.name)
        assertEquals(999.99, result.data.price)
        assertEquals(10, result.data.stock)
    }

    @Test
    fun `composición con zip3 para validar múltiples campos independientemente`() {
        @Serializable
        data class RegisterDto(
            val username: String,
            val email: String,
            val password: String
        )

        val jsonString = """
            {
                "username": "john123",
                "email": "john@test.com",
                "password": "securepass"
            }
        """.trimIndent()

        val result = safeDecodeFromString<RegisterDto>(jsonString)
            .flatMap { dto ->
                // Validar cada campo independientemente
                val usernameResult = if (dto.username.length >= 3) {
                    Result.Success(dto.username)
                } else {
                    Result.Failure("Username too short")
                }

                val emailResult = validateEmail(dto.email)?.let {
                    Result.Failure(it)
                } ?: Result.Success(dto.email)

                val passwordResult = if (dto.password.length >= 6) {
                    Result.Success(dto.password)
                } else {
                    Result.Failure("Password too short")
                }

                // Combinar con zip3
                zip3(usernameResult, emailResult, passwordResult) { user, email, pass ->
                    RegisterDto(user, email, pass)
                }
            }

        assertIs<Result.Success<RegisterDto>>(result)
    }

    // ========== Test 3: Sequence para Listas ==========

    @Test
    fun `deserializar lista de JSON y mapear todos a dominio con sequence`() {
        val jsonArray = """
            [
                {"email":"user1@test.com","age":25,"username":"user1"},
                {"email":"user2@test.com","age":30,"username":"user2"},
                {"email":"user3@test.com","age":35,"username":"user3"}
            ]
        """.trimIndent()

        val result = safeDecodeFromString<List<UserDto>>(jsonArray)
            .flatMap { dtos ->
                // Mapear cada DTO a dominio
                val userResults = dtos.map { dto -> UserMapper.toDomain(dto) }
                // Combinar todos los Results
                userResults.sequence()
            }

        assertIs<Result.Success<List<User>>>(result)
        assertEquals(3, result.data.size)
        assertEquals("user1@test.com", result.data[0].email)
        assertEquals("user2@test.com", result.data[1].email)
        assertEquals("user3@test.com", result.data[2].email)
    }

    @Test
    fun `sequence falla si uno de los elementos es inválido`() {
        val jsonArray = """
            [
                {"email":"user1@test.com","age":25,"username":"user1"},
                {"email":"invalid","age":30,"username":"user2"},
                {"email":"user3@test.com","age":35,"username":"user3"}
            ]
        """.trimIndent()

        val result = safeDecodeFromString<List<UserDto>>(jsonArray)
            .flatMap { dtos ->
                val userResults = dtos.map { dto -> UserMapper.toDomain(dto) }
                userResults.sequence()
            }

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("email"))
    }

    // ========== Test 4: Traverse para Transformación con Validación ==========

    @Test
    fun `traverse convierte y valida cada elemento de la lista`() {
        val jsonArray = """
            [
                {"email":"john@test.com","age":25,"username":"john"},
                {"email":"jane@test.com","age":30,"username":"jane"},
                {"email":"bob@test.com","age":28,"username":"bob"}
            ]
        """.trimIndent()

        val result = safeDecodeFromString<List<UserDto>>(jsonArray)
            .flatMap { dtos ->
                // Traverse aplica la transformación a cada elemento
                dtos.traverse { dto -> UserMapper.toDomain(dto) }
            }

        assertIs<Result.Success<List<User>>>(result)
        assertEquals(3, result.data.size)
    }

    // ========== Test 5: Edge Cases ==========

    @Test
    fun `maneja colección vacía correctamente`() {
        val emptyJson = "[]"

        val result = safeDecodeFromString<List<UserDto>>(emptyJson)
            .flatMap { dtos ->
                dtos.traverse { dto -> UserMapper.toDomain(dto) }
            }

        assertIs<Result.Success<List<User>>>(result)
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun `maneja objeto con campos faltantes usando valores por defecto`() {
        @Serializable
        data class OptionalFieldsDto(
            val name: String,
            val age: Int = 18,
            val email: String = "default@test.com"
        )

        val jsonWithMissingFields = """{"name":"John"}"""

        val result = safeDecodeFromString<OptionalFieldsDto>(jsonWithMissingFields)

        assertIs<Result.Success<OptionalFieldsDto>>(result)
        assertEquals("John", result.data.name)
        assertEquals(18, result.data.age)
        assertEquals("default@test.com", result.data.email)
    }

    @Test
    fun `flujo completo con serialización de ida y vuelta`() {
        // Crear usuario
        val originalUser = User(
            email = "test@example.com",
            age = 30,
            username = "testuser"
        )

        // Convertir a DTO
        val dto = UserMapper.toDto(originalUser)

        // Serializar a JSON
        val jsonString = json.encodeToString(dto)

        // Deserializar y convertir de vuelta a dominio
        val result = safeDecodeFromString<UserDto>(jsonString)
            .flatMap { deserializedDto -> UserMapper.toDomain(deserializedDto) }

        assertIs<Result.Success<User>>(result)
        assertEquals(originalUser.email, result.data.email)
        assertEquals(originalUser.age, result.data.age)
        assertEquals(originalUser.username, result.data.username)
    }

    // ========== Test 6: Escenarios Reales Complejos ==========

    @Serializable
    data class OrderDto(
        val items: List<OrderItemDto>,
        val customerEmail: String,
        val totalAmount: Double
    )

    @Serializable
    data class OrderItemDto(
        val productName: String,
        val quantity: Int,
        val price: Double
    )

    data class Order(
        val items: List<OrderItem>,
        val customerEmail: String,
        val totalAmount: Double
    )

    data class OrderItem(
        val productName: String,
        val quantity: Int,
        val price: Double
    )

    @Test
    fun `escenario real procesar orden con múltiples items`() {
        val orderJson = """
            {
                "items": [
                    {"productName":"Laptop","quantity":1,"price":999.99},
                    {"productName":"Mouse","quantity":2,"price":25.50}
                ],
                "customerEmail":"customer@test.com",
                "totalAmount":1050.99
            }
        """.trimIndent()

        val result = safeDecodeFromString<OrderDto>(orderJson)
            .flatMap { dto ->
                // Validar email del cliente
                val emailValidation = validateEmail(dto.customerEmail)?.let {
                    Result.Failure(it)
                } ?: Result.Success(dto.customerEmail)

                // Validar items (cada uno debe ser válido)
                val itemsResult = dto.items.traverse { itemDto ->
                    val validationResult = accumulateValidationErrors {
                        add(validateLengthRange(itemDto.productName, 1, 200, "Product name"))
                        add(validateRange(itemDto.quantity, 1, 1000, "Quantity"))
                        add(validateRange(itemDto.price, 0.01, 1000000.0, "Price"))
                    }

                    validationResult.map {
                        OrderItem(itemDto.productName, itemDto.quantity, itemDto.price)
                    }
                }

                // Combinar email y items
                zip3(emailValidation, itemsResult, Result.Success(dto.totalAmount)) { email, items, total ->
                    Order(items, email, total)
                }
            }

        assertIs<Result.Success<Order>>(result)
        assertEquals(2, result.data.items.size)
        assertEquals("customer@test.com", result.data.customerEmail)
        assertEquals(1050.99, result.data.totalAmount)
    }

    @Test
    fun `escenario real falla si item de orden es inválido`() {
        val invalidOrderJson = """
            {
                "items": [
                    {"productName":"","quantity":-1,"price":-10.0}
                ],
                "customerEmail":"customer@test.com",
                "totalAmount":0.0
            }
        """.trimIndent()

        val result = safeDecodeFromString<OrderDto>(invalidOrderJson)
            .flatMap { dto ->
                dto.items.traverse { itemDto ->
                    val validationResult = accumulateValidationErrors {
                        add(validateLengthRange(itemDto.productName, 1, 200, "Product name"))
                        add(validateRange(itemDto.quantity, 1, 1000, "Quantity"))
                        add(validateRange(itemDto.price, 0.01, 1000000.0, "Price"))
                    }

                    validationResult.map {
                        OrderItem(itemDto.productName, itemDto.quantity, itemDto.price)
                    }
                }
            }

        assertIs<Result.Failure>(result)
        // Debe contener múltiples errores
        assertTrue(result.error.contains("Product name"))
        assertTrue(result.error.contains("Quantity"))
        assertTrue(result.error.contains("Price"))
    }

    // ========== Test 7: Performance con Grandes Volúmenes ==========

    @Test
    fun `procesa 1000 registros eficientemente`() {
        // Generar 1000 DTOs válidos
        val dtos = List(1000) { i ->
            UserDto(
                email = "user$i@test.com",
                age = 20 + (i % 50),
                username = "user$i"
            )
        }

        // Serializar
        val jsonArray = json.encodeToString(dtos)

        // Deserializar y mapear
        val result = safeDecodeFromString<List<UserDto>>(jsonArray)
            .flatMap { deserializedDtos ->
                deserializedDtos.traverse { dto -> UserMapper.toDomain(dto) }
            }

        assertIs<Result.Success<List<User>>>(result)
        assertEquals(1000, result.data.size)
    }
}
