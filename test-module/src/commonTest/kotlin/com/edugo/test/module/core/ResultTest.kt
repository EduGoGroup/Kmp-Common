package com.edugo.test.module.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

class ResultTest {

    @Test
    fun success_containsData() {
        val result: Result<String> = Result.Success("test data")
        assertIs<Result.Success<String>>(result)
        assertEquals("test data", result.data)
    }

    @Test
    fun failure_containsErrorMessage() {
        val result: Result<String> = Result.Failure("test error")
        assertIs<Result.Failure>(result)
        assertEquals("test error", result.error)
        assertEquals("test error", result.getSafeMessage())
    }

    @Test
    fun map_transformsSuccessData() {
        val result: Result<Int> = Result.Success(5)
        val mapped = result.map { it * 2 }
        assertIs<Result.Success<Int>>(mapped)
        assertEquals(10, mapped.data)
    }

    @Test
    fun map_preservesFailure() {
        val result: Result<Int> = Result.Failure("error")
        val mapped = result.map { it * 2 }
        assertIs<Result.Failure>(mapped)
        assertEquals("error", mapped.error)
    }

    @Test
    fun success_withNullableData() {
        val result: Result<String?> = Result.Success(null)
        assertIs<Result.Success<String?>>(result)
        assertEquals(null, result.data)
    }

    @Test
    fun map_withNullableData() {
        val result: Result<String?> = Result.Success("test")
        val mapped = result.map { it?.uppercase() }
        assertIs<Result.Success<String?>>(mapped)
        assertEquals("TEST", mapped.data)
    }

    @Test
    fun map_chainingMultipleOperations() {
        val result: Result<Int> = Result.Success(5)
        val mapped = result.map { it * 2 }.map { it + 1 }
        assertIs<Result.Success<Int>>(mapped)
        assertEquals(11, mapped.data)
    }

    @Test
    fun catching_returnsSuccessWhenNoException() {
        val result = catching {
            Result.Success("success")
        }
        assertIs<Result.Success<String>>(result)
        assertEquals("success", result.data)
    }

    @Test
    fun catching_returnsFailureWhenExceptionThrown() {
        val result = catching<String> {
            throw RuntimeException("test exception")
        }
        assertIs<Result.Failure>(result)
        assertEquals("test exception", result.error)
    }

    @Test
    fun catching_usesGenericMessageWhenExceptionHasNoMessage() {
        val result = catching<String> {
            throw RuntimeException()
        }
        assertIs<Result.Failure>(result)
        assertEquals("An error occurred", result.error)
    }

    @Test
    fun catching_worksWithDifferentResultTypes() {
        val result = catching {
            val value = 10
            Result.Success(value * 2)
        }
        assertIs<Result.Success<Int>>(result)
        assertEquals(20, result.data)
    }
}
