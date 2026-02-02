package com.edugo.test.module.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests para las extensiones de serialización con Result<T>.
 */
class SerializationExtensionsTest {

    @Serializable
    data class TestUser(
        val id: String,
        val name: String,
        val age: Int
    )

    @Serializable
    data class TestProduct(
        val id: String,
        val price: Double
    )

    // ========================================================================
    // TESTS: catchSerialization
    // ========================================================================

    @Test
    fun catchSerialization_deserializesValidJson() {
        val json = """{"id": "123", "name": "John Doe", "age": 30}"""

        val result: Result<TestUser> = catchSerialization {
            Json.decodeFromString<TestUser>(json)
        }

        assertIs<Result.Success<TestUser>>(result)
        assertEquals("123", result.data.id)
        assertEquals("John Doe", result.data.name)
        assertEquals(30, result.data.age)
    }

    @Test
    fun catchSerialization_capturesInvalidJsonError() {
        val invalidJson = """{"id": "123", "name": "John Doe"}"""  // Falta age

        val result: Result<TestUser> = catchSerialization {
            Json.decodeFromString<TestUser>(invalidJson)
        }

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("age") || result.error.contains("field"))
    }

    @Test
    fun catchSerialization_capturesMalformedJson() {
        val malformedJson = """{"id": "123", "name": "John Doe", "age": }"""  // JSON inválido

        val result: Result<TestUser> = catchSerialization {
            Json.decodeFromString<TestUser>(malformedJson)
        }

        assertIs<Result.Failure>(result)
        assertTrue(result.error.isNotEmpty())
    }

    @Test
    fun catchSerialization_capturesTypeMismatch() {
        val json = """{"id": "123", "name": "John Doe", "age": "not a number"}"""

        val result: Result<TestUser> = catchSerialization {
            Json.decodeFromString<TestUser>(json)
        }

        assertIs<Result.Failure>(result)
    }

    @Test
    fun catchSerialization_serializesObjectSuccessfully() {
        val user = TestUser(id = "123", name = "John Doe", age = 30)

        val result: Result<String> = catchSerialization {
            Json.encodeToString(TestUser.serializer(), user)
        }

        assertIs<Result.Success<String>>(result)
        assertTrue(result.data.contains("\"id\":\"123\""))
        assertTrue(result.data.contains("\"name\":\"John Doe\""))
        assertTrue(result.data.contains("\"age\":30"))
    }

    @Test
    fun catchSerialization_worksWithDifferentTypes() {
        val product = TestProduct(id = "P123", price = 99.99)

        val result: Result<String> = catchSerialization {
            Json.encodeToString(TestProduct.serializer(), product)
        }

        assertIs<Result.Success<String>>(result)
        assertTrue(result.data.contains("P123"))
    }

    // ========================================================================
    // TESTS: catchSerializationAsAppError
    // ========================================================================

    @Test
    fun catchSerializationAsAppError_deserializesSuccessfully() {
        val json = """{"id": "123", "name": "John Doe", "age": 30}"""

        val result: Result<TestUser> = catchSerializationAsAppError {
            Json.decodeFromString<TestUser>(json)
        }

        assertIs<Result.Success<TestUser>>(result)
        assertEquals("123", result.data.id)
    }

    @Test
    fun catchSerializationAsAppError_capturesSerializationException() {
        val invalidJson = """{"id": "123"}"""  // Faltan campos requeridos

        val result: Result<TestUser> = catchSerializationAsAppError {
            Json.decodeFromString<TestUser>(invalidJson)
        }

        assertIs<Result.Failure>(result)
        // El error debe mencionar serialization
        assertTrue(result.error.isNotEmpty())
    }

    @Test
    fun catchSerializationAsAppError_includesDetails() {
        val invalidJson = """{"invalid": "data"}"""
        val details = mapOf("endpoint" to "/api/users", "method" to "GET")

        val result: Result<TestUser> = catchSerializationAsAppError(details) {
            Json.decodeFromString<TestUser>(invalidJson)
        }

        assertIs<Result.Failure>(result)
        // El error debe contener información
        assertTrue(result.error.isNotEmpty())
    }

    // ========================================================================
    // TESTS: catchSerializationWithDetails
    // ========================================================================

    @Test
    fun catchSerializationWithDetails_includesContextInError() {
        val invalidJson = """{"id": "123"}"""
        val details = mapOf("file" to "config.json", "line" to "42")

        val result: Result<TestUser> = catchSerializationWithDetails(details) {
            Json.decodeFromString<TestUser>(invalidJson)
        }

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("file=config.json") || result.error.contains("details"))
        assertTrue(result.error.contains("line=42") || result.error.contains("details"))
    }

    @Test
    fun catchSerializationWithDetails_successDoesNotIncludeDetails() {
        val validJson = """{"id": "123", "name": "John", "age": 30}"""
        val details = mapOf("context" to "test")

        val result: Result<TestUser> = catchSerializationWithDetails(details) {
            Json.decodeFromString<TestUser>(validJson)
        }

        assertIs<Result.Success<TestUser>>(result)
        assertEquals("123", result.data.id)
    }

    // ========================================================================
    // TESTS: safeDecodeFromString
    // ========================================================================

    @Test
    fun safeDecodeFromString_deserializesValidJson() {
        val json = """{"id": "123", "name": "John Doe", "age": 30}"""

        val result: Result<TestUser> = safeDecodeFromString(json)

        assertIs<Result.Success<TestUser>>(result)
        assertEquals("123", result.data.id)
        assertEquals("John Doe", result.data.name)
    }

    @Test
    fun safeDecodeFromString_capturesInvalidJson() {
        val invalidJson = """{"id": "123"}"""

        val result: Result<TestUser> = safeDecodeFromString(invalidJson)

        assertIs<Result.Failure>(result)
    }

    @Test
    fun safeDecodeFromString_worksWithDifferentTypes() {
        val json = """{"id": "P123", "price": 99.99}"""

        val result: Result<TestProduct> = safeDecodeFromString(json)

        assertIs<Result.Success<TestProduct>>(result)
        assertEquals("P123", result.data.id)
        assertEquals(99.99, result.data.price)
    }

    @Test
    fun safeDecodeFromString_handlesEmptyString() {
        val result: Result<TestUser> = safeDecodeFromString("")

        assertIs<Result.Failure>(result)
    }

    @Test
    fun safeDecodeFromString_handlesNonJsonString() {
        val result: Result<TestUser> = safeDecodeFromString("not json at all")

        assertIs<Result.Failure>(result)
    }

    // ========================================================================
    // TESTS: safeEncodeToString
    // ========================================================================

    @Test
    fun safeEncodeToString_serializesObject() {
        val user = TestUser(id = "123", name = "John Doe", age = 30)

        val result: Result<String> = safeEncodeToString(user)

        assertIs<Result.Success<String>>(result)
        assertTrue(result.data.contains("\"id\":\"123\""))
        assertTrue(result.data.contains("\"name\":\"John Doe\""))
    }

    @Test
    fun safeEncodeToString_worksWithDifferentTypes() {
        val product = TestProduct(id = "P123", price = 99.99)

        val result: Result<String> = safeEncodeToString(product)

        assertIs<Result.Success<String>>(result)
        assertTrue(result.data.contains("P123"))
        assertTrue(result.data.contains("99.99"))
    }

    @Test
    fun safeEncodeToString_handlesNullableFields() {
        @Serializable
        data class OptionalUser(val id: String, val name: String?)

        val user = OptionalUser(id = "123", name = null)
        val result: Result<String> = safeEncodeToString(user)

        assertIs<Result.Success<String>>(result)
        assertTrue(result.data.contains("\"id\":\"123\""))
    }

    // ========================================================================
    // TESTS: Integration
    // ========================================================================

    @Test
    fun integration_roundTripSerialization() {
        val originalUser = TestUser(id = "123", name = "John Doe", age = 30)

        // Serializar
        val jsonResult: Result<String> = safeEncodeToString(originalUser)
        assertIs<Result.Success<String>>(jsonResult)

        // Deserializar
        val userResult: Result<TestUser> = safeDecodeFromString(jsonResult.data)
        assertIs<Result.Success<TestUser>>(userResult)

        // Verificar que son iguales
        assertEquals(originalUser, userResult.data)
    }

    @Test
    fun integration_chainedWithFlatMap() {
        val json = """{"id": "123", "name": "John Doe", "age": 30}"""

        val result = safeDecodeFromString<TestUser>(json)
            .flatMap { user ->
                // Simular una operación adicional
                if (user.age >= 18) {
                    Result.Success(user.name.uppercase())
                } else {
                    Result.Failure("User must be adult")
                }
            }

        assertIs<Result.Success<String>>(result)
        assertEquals("JOHN DOE", result.data)
    }

    @Test
    fun integration_chainedWithMap() {
        val json = """{"id": "123", "name": "John Doe", "age": 30}"""

        val result = safeDecodeFromString<TestUser>(json)
            .map { user -> user.name }

        assertIs<Result.Success<String>>(result)
        assertEquals("John Doe", result.data)
    }

    @Test
    fun integration_errorPropagationInChain() {
        val invalidJson = """{"id": "123"}"""

        val result = safeDecodeFromString<TestUser>(invalidJson)
            .map { user -> user.name }
            .flatMap { name -> Result.Success(name.uppercase()) }

        // El error debe propagarse sin ejecutar map ni flatMap
        assertIs<Result.Failure>(result)
    }

    // ========================================================================
    // TESTS: Edge Cases
    // ========================================================================

    @Test
    fun edgeCase_veryLargeJson() {
        val largeUser = TestUser(
            id = "123",
            name = "A".repeat(10000),  // Nombre muy largo
            age = 30
        )

        val encodeResult = safeEncodeToString(largeUser)
        assertIs<Result.Success<String>>(encodeResult)

        val decodeResult: Result<TestUser> = safeDecodeFromString(encodeResult.data)
        assertIs<Result.Success<TestUser>>(decodeResult)
        assertEquals(largeUser, decodeResult.data)
    }

    @Test
    fun edgeCase_specialCharactersInJson() {
        val json = """{"id": "123", "name": "John \"Doe\"", "age": 30}"""

        val result: Result<TestUser> = safeDecodeFromString(json)

        assertIs<Result.Success<TestUser>>(result)
        assertEquals("John \"Doe\"", result.data.name)
    }

    @Test
    fun edgeCase_unicodeCharacters() {
        val user = TestUser(id = "123", name = "José María 中文", age = 30)

        val encodeResult = safeEncodeToString(user)
        assertIs<Result.Success<String>>(encodeResult)

        val decodeResult: Result<TestUser> = safeDecodeFromString(encodeResult.data)
        assertIs<Result.Success<TestUser>>(decodeResult)
        assertEquals("José María 中文", decodeResult.data.name)
    }
}
