package com.edugo.test.module.platform

import kotlin.test.Test
import kotlin.test.assertNotNull

class LoggerJsTest {
    @Test
    fun createDefaultLogger_returns_JsLogger() {
        val logger = createDefaultLogger()
        assertNotNull(logger)
    }

    @Test
    fun jsLogger_can_log_debug_message() {
        val logger = createDefaultLogger()
        // Should not throw
        logger.d("TestTag", "Debug message from JS test")
    }

    @Test
    fun jsLogger_can_log_info_message() {
        val logger = createDefaultLogger()
        // Should not throw
        logger.i("TestTag", "Info message from JS test")
    }

    @Test
    fun jsLogger_can_log_warning_message() {
        val logger = createDefaultLogger()
        // Should not throw
        logger.w("TestTag", "Warning message from JS test")
    }

    @Test
    fun jsLogger_can_log_error_message() {
        val logger = createDefaultLogger()
        // Should not throw
        logger.e("TestTag", "Error message from JS test")
    }

    @Test
    fun jsLogger_can_log_with_throwable() {
        val logger = createDefaultLogger()
        val exception = RuntimeException("Test exception")
        // Should not throw
        logger.e("TestTag", "Error with exception", exception)
    }
}
