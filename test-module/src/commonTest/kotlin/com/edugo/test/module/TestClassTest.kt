package com.edugo.test.module

import kotlin.test.Test
import kotlin.test.assertEquals

class TestClassTest {

    @Test
    fun greeting_returnsExpectedMessage() {
        val testClass = TestClass()
        assertEquals("Hello from KMP!", testClass.greeting())
    }
}
