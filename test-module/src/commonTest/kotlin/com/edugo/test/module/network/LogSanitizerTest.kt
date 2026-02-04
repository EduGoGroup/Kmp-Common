package com.edugo.test.module.network

import kotlin.test.*

class LogSanitizerTest {

    // ==================== HEADERS ====================

    @Test
    fun sanitizeHeaders_masks_Authorization_header() {
        val headers = mapOf(
            "Authorization" to "Bearer secret-token-123",
            "Content-Type" to "application/json"
        )

        val sanitized = LogSanitizer.sanitizeHeaders(headers)

        assertEquals("[REDACTED]", sanitized["Authorization"])
        assertEquals("application/json", sanitized["Content-Type"])
    }

    @Test
    fun sanitizeHeaders_is_case_insensitive() {
        val headers = mapOf("authorization" to "Bearer token")
        val sanitized = LogSanitizer.sanitizeHeaders(headers)
        assertEquals("[REDACTED]", sanitized["authorization"])
    }

    @Test
    fun sanitizeHeaders_masks_X_Api_Key_header() {
        val headers = mapOf(
            "X-Api-Key" to "api-key-12345",
            "Accept" to "application/json"
        )

        val sanitized = LogSanitizer.sanitizeHeaders(headers)

        assertEquals("[REDACTED]", sanitized["X-Api-Key"])
        assertEquals("application/json", sanitized["Accept"])
    }

    @Test
    fun sanitizeHeaders_masks_cookie_headers() {
        val headers = mapOf(
            "Cookie" to "session=abc123",
            "Set-Cookie" to "token=xyz789"
        )

        val sanitized = LogSanitizer.sanitizeHeaders(headers)

        assertEquals("[REDACTED]", sanitized["Cookie"])
        assertEquals("[REDACTED]", sanitized["Set-Cookie"])
    }

    @Test
    fun sanitizeHeader_individual_returns_redacted_for_sensitive() {
        assertEquals("[REDACTED]", LogSanitizer.sanitizeHeader("Authorization", "Bearer token"))
        assertEquals("[REDACTED]", LogSanitizer.sanitizeHeader("x-api-key", "secret"))
    }

    @Test
    fun sanitizeHeader_individual_returns_value_for_non_sensitive() {
        assertEquals("application/json", LogSanitizer.sanitizeHeader("Content-Type", "application/json"))
        assertEquals("gzip", LogSanitizer.sanitizeHeader("Accept-Encoding", "gzip"))
    }

    // ==================== BODY ====================

    @Test
    fun sanitizeBody_masks_password_field() {
        val body = """{ "username": "john", "password": "secret123" }"""

        val sanitized = LogSanitizer.sanitizeBody(body)

        assertTrue(sanitized.contains("john"))
        assertFalse(sanitized.contains("secret123"))
        assertTrue(sanitized.contains("[REDACTED]"))
    }

    @Test
    fun sanitizeBody_masks_token_field() {
        val body = """{ "token": "abc123", "data": "value" }"""

        val sanitized = LogSanitizer.sanitizeBody(body)

        assertFalse(sanitized.contains("abc123"))
        assertTrue(sanitized.contains("value"))
    }

    @Test
    fun sanitizeBody_masks_multiple_sensitive_fields() {
        val body = """{ "password": "pass123", "token": "tok456", "api_key": "key789" }"""

        val sanitized = LogSanitizer.sanitizeBody(body)

        assertFalse(sanitized.contains("pass123"))
        assertFalse(sanitized.contains("tok456"))
        assertFalse(sanitized.contains("key789"))
        assertEquals(3, sanitized.split("[REDACTED]").size - 1) // 3 redactions
    }

    @Test
    fun sanitizeBody_masks_access_token_and_refresh_token() {
        val body = """{ "access_token": "access123", "refresh_token": "refresh456" }"""

        val sanitized = LogSanitizer.sanitizeBody(body)

        assertFalse(sanitized.contains("access123"))
        assertFalse(sanitized.contains("refresh456"))
    }

    @Test
    fun sanitizeBody_masks_credentials_field() {
        val body = """{ "credentials": "user:pass", "other": "data" }"""

        val sanitized = LogSanitizer.sanitizeBody(body)

        assertFalse(sanitized.contains("user:pass"))
        assertTrue(sanitized.contains("data"))
    }

    @Test
    fun sanitizeBody_preserves_non_sensitive_data() {
        val body = """{ "id": 123, "name": "John", "email": "john@example.com" }"""

        val sanitized = LogSanitizer.sanitizeBody(body)

        assertEquals(body, sanitized) // No changes
    }

    // ==================== URL ====================

    @Test
    fun sanitizeUrl_masks_password_query_param() {
        val url = "https://api.com/login?username=john&password=secret&page=1"

        val sanitized = LogSanitizer.sanitizeUrl(url)

        assertTrue(sanitized.contains("username=john"))
        assertFalse(sanitized.contains("password=secret"))
        assertTrue(sanitized.contains("password=[REDACTED]"))
        assertTrue(sanitized.contains("page=1"))
    }

    @Test
    fun sanitizeUrl_masks_token_query_param() {
        val url = "https://api.com/resource?token=secret123&id=456"

        val sanitized = LogSanitizer.sanitizeUrl(url)

        assertFalse(sanitized.contains("token=secret123"))
        assertTrue(sanitized.contains("token=[REDACTED]"))
        assertTrue(sanitized.contains("id=456"))
    }

    @Test
    fun sanitizeUrl_masks_api_key_query_param() {
        val url = "https://api.com/data?api_key=key123&format=json"

        val sanitized = LogSanitizer.sanitizeUrl(url)

        assertFalse(sanitized.contains("api_key=key123"))
        assertTrue(sanitized.contains("format=json"))
    }

    @Test
    fun sanitizeUrl_preserves_url_without_sensitive_params() {
        val url = "https://api.com/users?page=1&limit=10"

        val sanitized = LogSanitizer.sanitizeUrl(url)

        assertEquals(url, sanitized) // No changes
    }

    @Test
    fun sanitizeUrl_handles_url_without_query_params() {
        val url = "https://api.com/users/123"

        val sanitized = LogSanitizer.sanitizeUrl(url)

        assertEquals(url, sanitized) // No changes
    }
}
