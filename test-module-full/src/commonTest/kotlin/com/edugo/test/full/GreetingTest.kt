package com.edugo.test.full

import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingTest {
    @Test
    fun greetingReturnsExpectedMessage() {
        val greeting = Greeting()
        assertEquals("Hello from KMP Full!", greeting.greet())
    }
}
