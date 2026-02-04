package com.edugo.test.module.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class SynchronizationJsTest {
    @Test
    fun platformSynchronized_executes_block_in_js() {
        val lock = Any()
        var executed = false

        platformSynchronized(lock) {
            executed = true
        }

        assertEquals(true, executed)
    }

    @Test
    fun platformSynchronized_returns_block_result() {
        val lock = Any()
        val result = platformSynchronized(lock) {
            "test-result"
        }

        assertEquals("test-result", result)
    }
}
