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
    fun error_containsException() {
        val exception = RuntimeException("test error")
        val result: Result<String> = Result.Error(exception)
        assertIs<Result.Error>(result)
        assertEquals(exception, result.exception)
        assertEquals("test error", result.exception.message)
    }

    @Test
    fun map_transformsSuccessData() {
        val result: Result<Int> = Result.Success(5)
        val mapped = result.map { it * 2 }
        assertIs<Result.Success<Int>>(mapped)
        assertEquals(10, mapped.data)
    }

    @Test
    fun map_preservesError() {
        val result: Result<Int> = Result.Error(RuntimeException("error"))
        val mapped = result.map { it * 2 }
        assertIs<Result.Error>(mapped)
        assertEquals("error", mapped.exception.message)
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
}
