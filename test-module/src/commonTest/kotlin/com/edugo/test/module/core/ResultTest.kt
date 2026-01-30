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
    }
}
