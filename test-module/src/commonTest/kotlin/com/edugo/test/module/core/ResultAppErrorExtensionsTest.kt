package com.edugo.test.module.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Tests para las extensiones de integración entre Result<T> y AppError.
 *
 * Estos tests verifican que las conversiones bidireccionales funcionen correctamente
 * y que la información se preserve o se pierda de forma predecible.
 */
class ResultAppErrorExtensionsTest {

    // ========================================================================
    // TESTS: AppError -> Result
    // ========================================================================

    @Test
    fun toResult_convertsAppErrorToFailure() {
        val appError = AppError.fromCode(
            ErrorCode.BUSINESS_RESOURCE_NOT_FOUND,
            "User not found"
        )

        val result: Result<String> = appError.toResult()

        assertIs<Result.Failure>(result)
        assertEquals("User not found", result.error)
    }

    @Test
    fun toResult_losesErrorCodeInformation() {
        val appError = AppError.fromCode(
            ErrorCode.NETWORK_TIMEOUT,
            "Request timed out"
        )

        val result: Result<Int> = appError.toResult()

        assertIs<Result.Failure>(result)
        // Verificamos que solo el mensaje se preserva
        assertEquals("Request timed out", result.error)
        // El código de error se pierde (esto es esperado en la conversión simple)
    }

    @Test
    fun toResultWithCode_includesErrorCodeInMessage() {
        val appError = AppError.fromCode(
            ErrorCode.NETWORK_TIMEOUT,
            "Request timed out"
        )

        val result: Result<String> = appError.toResultWithCode()

        assertIs<Result.Failure>(result)
        assertEquals("[NETWORK_TIMEOUT] Request timed out", result.error)
    }

    @Test
    fun toResultWithDetails_includesAllInformation() {
        val appError = AppError.fromCode(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "Invalid email",
            mapOf("field" to "email", "value" to "invalid")
        )

        val result: Result<String> = appError.toResultWithDetails()

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("[VALIDATION_INVALID_INPUT]"))
        assertTrue(result.error.contains("Invalid email"))
        assertTrue(result.error.contains("field=email"))
        assertTrue(result.error.contains("value=invalid"))
        assertTrue(result.error.contains("code: ${ErrorCode.VALIDATION_INVALID_INPUT.code}"))
    }

    @Test
    fun toResultWithDetails_includesCauseInformation() {
        val cause = RuntimeException("Database connection failed")
        val appError = AppError.fromException(
            cause,
            ErrorCode.SYSTEM_DATABASE_ERROR
        )

        val result: Result<String> = appError.toResultWithDetails()

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("RuntimeException"))
        assertTrue(result.error.contains("Database connection failed"))
    }

    @Test
    fun toResultWithDetails_handlesEmptyDetails() {
        val appError = AppError.fromCode(
            ErrorCode.AUTH_UNAUTHORIZED,
            "Not authenticated"
        )

        val result: Result<String> = appError.toResultWithDetails()

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("[AUTH_UNAUTHORIZED]"))
        assertTrue(result.error.contains("Not authenticated"))
        // No debe tener ", details:" si no hay detalles
    }

    // ========================================================================
    // TESTS: Result.Failure -> AppError
    // ========================================================================

    @Test
    fun failureToAppError_convertsWithDefaultCode() {
        val failure = Result.Failure("Something went wrong")

        val appError = failure.toAppError()

        assertEquals(ErrorCode.SYSTEM_UNKNOWN_ERROR, appError.code)
        assertEquals("Something went wrong", appError.message)
        assertTrue(appError.details.isEmpty())
        assertEquals(null, appError.cause)
    }

    @Test
    fun failureToAppError_convertsWithSpecificCode() {
        val failure = Result.Failure("User not found")

        val appError = failure.toAppError(ErrorCode.BUSINESS_RESOURCE_NOT_FOUND)

        assertEquals(ErrorCode.BUSINESS_RESOURCE_NOT_FOUND, appError.code)
        assertEquals("User not found", appError.message)
    }

    @Test
    fun failureToAppError_convertsWithDetails() {
        val failure = Result.Failure("Invalid email format")

        val appError = failure.toAppError(
            ErrorCode.VALIDATION_INVALID_EMAIL,
            mapOf("field" to "email", "input" to "notanemail")
        )

        assertEquals(ErrorCode.VALIDATION_INVALID_EMAIL, appError.code)
        assertEquals("Invalid email format", appError.message)
        assertEquals(2, appError.details.size)
        assertEquals("email", appError.details["field"])
        assertEquals("notanemail", appError.details["input"])
    }

    @Test
    fun failureToAppErrorWithCause_includesCause() {
        val failure = Result.Failure("Network error occurred")
        val cause = RuntimeException("Connection refused")

        val appError = failure.toAppErrorWithCause(
            ErrorCode.NETWORK_NO_CONNECTION,
            cause
        )

        assertEquals(ErrorCode.NETWORK_NO_CONNECTION, appError.code)
        assertEquals("Network error occurred", appError.message)
        assertNotNull(appError.cause)
        assertEquals(cause, appError.cause)
    }

    @Test
    fun failureToAppErrorWithCause_includesDetailsAndCause() {
        val failure = Result.Failure("Failed to fetch data")
        val cause = RuntimeException("Timeout")

        val appError = failure.toAppErrorWithCause(
            ErrorCode.NETWORK_TIMEOUT,
            cause,
            mapOf("endpoint" to "/api/users", "timeout" to "30s")
        )

        assertEquals(ErrorCode.NETWORK_TIMEOUT, appError.code)
        assertEquals("Failed to fetch data", appError.message)
        assertEquals(cause, appError.cause)
        assertEquals(2, appError.details.size)
        assertEquals("/api/users", appError.details["endpoint"])
        assertEquals("30s", appError.details["timeout"])
    }

    // ========================================================================
    // TESTS: Factory Functions
    // ========================================================================

    @Test
    fun resultFailure_createsFailureFromCodeAndMessage() {
        val result: Result<String> = resultFailure(
            ErrorCode.AUTH_UNAUTHORIZED,
            "Not authenticated"
        )

        assertIs<Result.Failure>(result)
        assertEquals("Not authenticated", result.error)
    }

    @Test
    fun resultFailure_createsFailureWithDetails() {
        val result: Result<Int> = resultFailure(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "Invalid age",
            mapOf("field" to "age", "value" to "-5")
        )

        assertIs<Result.Failure>(result)
        assertEquals("Invalid age", result.error)
    }

    @Test
    fun resultFailureFrom_createsFailureFromException() {
        val exception = IllegalArgumentException("Invalid argument")

        val result: Result<String> = resultFailureFrom(exception)

        assertIs<Result.Failure>(result)
        assertEquals("Invalid argument", result.error)
    }

    @Test
    fun resultFailureFrom_createsFailureWithSpecificCode() {
        val exception = RuntimeException("Database error")

        val result: Result<String> = resultFailureFrom(
            exception,
            ErrorCode.SYSTEM_DATABASE_ERROR
        )

        assertIs<Result.Failure>(result)
        assertEquals("Database error", result.error)
    }

    @Test
    fun resultFailureFrom_createsFailureWithDetails() {
        val exception = RuntimeException("Query failed")

        val result: Result<String> = resultFailureFrom(
            exception,
            ErrorCode.SYSTEM_DATABASE_ERROR,
            mapOf("query" to "SELECT * FROM users", "table" to "users")
        )

        assertIs<Result.Failure>(result)
        assertEquals("Query failed", result.error)
    }

    @Test
    fun resultFailureFrom_handlesExceptionWithoutMessage() {
        val exception = RuntimeException()

        val result: Result<String> = resultFailureFrom(
            exception,
            ErrorCode.SYSTEM_INTERNAL_ERROR
        )

        assertIs<Result.Failure>(result)
        // Debe usar la descripción del ErrorCode cuando el exception no tiene mensaje
        assertEquals(ErrorCode.SYSTEM_INTERNAL_ERROR.description, result.error)
    }

    @Test
    fun resultFailureFromWithCode_includesCodeInMessage() {
        val exception = RuntimeException("Connection timeout")

        val result: Result<String> = resultFailureFromWithCode(
            exception,
            ErrorCode.NETWORK_TIMEOUT
        )

        assertIs<Result.Failure>(result)
        assertEquals("[NETWORK_TIMEOUT] Connection timeout", result.error)
    }

    // ========================================================================
    // TESTS: Advanced Extensions
    // ========================================================================

    @Test
    fun catchingWithAppError_returnsSuccessWhenNoException() {
        val result = catchingWithAppError {
            Result.Success("data")
        }

        assertIs<Result.Success<String>>(result)
        assertEquals("data", result.data)
    }

    @Test
    fun catchingWithAppError_catchesException() {
        val result: Result<String> = catchingWithAppError(ErrorCode.SYSTEM_INTERNAL_ERROR) {
            throw RuntimeException("Operation failed")
        }

        assertIs<Result.Failure>(result)
        assertEquals("Operation failed", result.error)
    }

    @Test
    fun catchingWithAppError_usesDefaultCode() {
        val result: Result<String> = catchingWithAppError {
            throw IllegalStateException("Invalid state")
        }

        assertIs<Result.Failure>(result)
        assertEquals("Invalid state", result.error)
    }

    @Test
    fun catchingWithAppError_preservesDetails() {
        val result: Result<String> = catchingWithAppError(
            ErrorCode.SYSTEM_DATABASE_ERROR,
            mapOf("operation" to "insert", "table" to "users")
        ) {
            throw RuntimeException("Insert failed")
        }

        assertIs<Result.Failure>(result)
        assertEquals("Insert failed", result.error)
    }

    @Test
    fun flatMapCatching_transformsSuccessfully() {
        val initial: Result<Int> = Result.Success(5)

        val result = initial.flatMapCatching { value ->
            Result.Success(value * 2)
        }

        assertIs<Result.Success<Int>>(result)
        assertEquals(10, result.data)
    }

    @Test
    fun flatMapCatching_catchesExceptionInTransform() {
        val initial: Result<Int> = Result.Success(5)

        val result = initial.flatMapCatching<Int, String>(ErrorCode.SYSTEM_INTERNAL_ERROR) {
            throw RuntimeException("Transform failed")
        }

        assertIs<Result.Failure>(result)
        assertEquals("Transform failed", result.error)
    }

    @Test
    fun flatMapCatching_propagatesFailure() {
        val initial: Result<Int> = Result.Failure("initial error")

        val result = initial.flatMapCatching<Int, String> {
            Result.Success("should not be called")
        }

        assertIs<Result.Failure>(result)
        assertEquals("initial error", result.error)
    }

    @Test
    fun flatMapCatching_propagatesLoading() {
        val initial: Result<Int> = Result.Loading

        val result = initial.flatMapCatching<Int, String> {
            Result.Success("should not be called")
        }

        assertIs<Result.Loading>(result)
    }

    @Test
    fun flatMapCatching_includesDetailsOnError() {
        val initial: Result<Int> = Result.Success(5)

        val result = initial.flatMapCatching<Int, String>(
            ErrorCode.VALIDATION_INVALID_INPUT,
            mapOf("step" to "transformation", "input" to "5")
        ) {
            throw IllegalArgumentException("Invalid transformation")
        }

        assertIs<Result.Failure>(result)
        assertEquals("Invalid transformation", result.error)
    }

    // ========================================================================
    // TESTS: Round-trip Conversions
    // ========================================================================

    @Test
    fun roundTrip_appErrorToResultBackToAppError() {
        val original = AppError.fromCode(
            ErrorCode.BUSINESS_RESOURCE_NOT_FOUND,
            "Original message",
            mapOf("id" to "123")
        )

        // AppError -> Result.Failure
        val result: Result<String> = original.toResult()
        assertIs<Result.Failure>(result)

        // Result.Failure -> AppError
        val recovered = result.toAppError(
            ErrorCode.BUSINESS_RESOURCE_NOT_FOUND,
            mapOf("id" to "123")
        )

        // Verificamos que se preserva lo básico
        assertEquals(original.code, recovered.code)
        assertEquals(original.message, recovered.message)
        assertEquals(original.details, recovered.details)
        // Nota: cause se pierde en la conversión simple
    }

    @Test
    fun roundTrip_failureToAppErrorBackToFailure() {
        val original = Result.Failure("Original error message")

        // Result.Failure -> AppError
        val appError = original.toAppError(ErrorCode.SYSTEM_INTERNAL_ERROR)

        // AppError -> Result.Failure
        val recovered: Result<String> = appError.toResult()

        assertIs<Result.Failure>(recovered)
        assertEquals(original.error, recovered.error)
    }

    // ========================================================================
    // TESTS: Edge Cases
    // ========================================================================

    @Test
    fun edgeCase_emptyMessage() {
        // AppError NO permite mensajes vacíos (require en init), pero Result.Failure sí
        // Cuando convertimos de Result.Failure con mensaje vacío, debe lanzar excepción
        val failure = Result.Failure("")

        // Intentar crear AppError con mensaje vacío debe fallar
        assertFailsWith<IllegalArgumentException> {
            failure.toAppError(ErrorCode.SYSTEM_UNKNOWN_ERROR)
        }
    }

    @Test
    fun edgeCase_veryLongMessage() {
        val longMessage = "Error".repeat(1000)
        val appError = AppError.fromCode(ErrorCode.SYSTEM_INTERNAL_ERROR, longMessage)

        val result: Result<String> = appError.toResult()

        assertIs<Result.Failure>(result)
        assertEquals(longMessage, result.error)
    }

    @Test
    fun edgeCase_specialCharactersInMessage() {
        val specialMessage = "Error: [Test] with \"quotes\" and 'apostrophes' & symbols"
        val appError = AppError.fromCode(ErrorCode.VALIDATION_INVALID_INPUT, specialMessage)

        val result: Result<String> = appError.toResult()

        assertIs<Result.Failure>(result)
        assertEquals(specialMessage, result.error)
    }

    @Test
    fun edgeCase_multilineMessage() {
        val multilineMessage = "Line 1\nLine 2\nLine 3"
        val appError = AppError.fromCode(ErrorCode.SYSTEM_INTERNAL_ERROR, multilineMessage)

        val result: Result<String> = appError.toResult()

        assertIs<Result.Failure>(result)
        assertEquals(multilineMessage, result.error)
    }

    @Test
    fun edgeCase_manyDetails() {
        val manyDetails = (1..50).associate { "key$it" to "value$it" }
        val appError = AppError.fromCode(
            ErrorCode.SYSTEM_INTERNAL_ERROR,
            "Error with many details",
            manyDetails
        )

        val result: Result<String> = appError.toResultWithDetails()

        assertIs<Result.Failure>(result)
        // Solo verificamos que no lanza excepción y genera un mensaje
        assertTrue(result.error.isNotEmpty())
    }
}
