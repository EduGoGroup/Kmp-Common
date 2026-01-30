package com.edugo.test.module

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestClassTest {

    @Test
    fun greeting_returnsExpectedMessage() {
        val testClass = TestClass()
        assertEquals("Hello from KMP!", testClass.greeting())
    }

    @Test
    fun greeting_isNotEmpty() {
        val testClass = TestClass()
        assertTrue(testClass.greeting().isNotEmpty())
    }
}
