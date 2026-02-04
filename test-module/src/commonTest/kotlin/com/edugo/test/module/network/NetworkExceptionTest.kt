package com.edugo.test.module.network

import com.edugo.test.module.core.ErrorCode
import kotlin.test.*

class NetworkExceptionTest {

    // ==================== ERROR CODE MAPPING ====================

    @Test
    fun Timeout_maps_to_NETWORK_TIMEOUT_error_code() {
        val exception = NetworkException.Timeout("Request timed out")

        assertEquals(ErrorCode.NETWORK_TIMEOUT, exception.errorCode)
    }

    @Test
    fun NoConnection_maps_to_NETWORK_NO_CONNECTION_error_code() {
        val exception = NetworkException.NoConnection()

        assertEquals(ErrorCode.NETWORK_NO_CONNECTION, exception.errorCode)
    }

    @Test
    fun SslError_maps_to_NETWORK_SSL_ERROR_error_code() {
        val exception = NetworkException.SslError()

        assertEquals(ErrorCode.NETWORK_SSL_ERROR, exception.errorCode)
    }

    @Test
    fun DnsFailure_maps_to_NETWORK_DNS_FAILURE_error_code() {
        val exception = NetworkException.DnsFailure()

        assertEquals(ErrorCode.NETWORK_DNS_FAILURE, exception.errorCode)
    }

    @Test
    fun ConnectionReset_maps_to_NETWORK_CONNECTION_RESET_error_code() {
        val exception = NetworkException.ConnectionReset()

        assertEquals(ErrorCode.NETWORK_CONNECTION_RESET, exception.errorCode)
    }

    // ==================== HTTP STATUS CODES ====================

    @Test
    fun ClientError_404_maps_correctly() {
        val exception = NetworkException.ClientError(404, "Not Found")

        assertEquals(404, exception.statusCode)
        assertEquals("Not Found", exception.message)
    }

    @Test
    fun ClientError_401_maps_correctly() {
        val exception = NetworkException.ClientError(401, "Unauthorized")

        assertEquals(401, exception.statusCode)
    }

    @Test
    fun ServerError_500_maps_correctly() {
        val exception = NetworkException.ServerError(500, "Internal Server Error")

        assertEquals(500, exception.statusCode)
        assertEquals("Internal Server Error", exception.message)
    }

    @Test
    fun ServerError_503_maps_correctly() {
        val exception = NetworkException.ServerError(503, "Service Unavailable")

        assertEquals(503, exception.statusCode)
    }

    // ==================== toAppError() ====================

    @Test
    fun toAppError_converts_Timeout_correctly() {
        val exception = NetworkException.Timeout("Test timeout")

        val appError = exception.toAppError()

        assertEquals(ErrorCode.NETWORK_TIMEOUT, appError.code)
        assertEquals("Test timeout", appError.message)
    }

    @Test
    fun toAppError_converts_NoConnection_correctly() {
        val exception = NetworkException.NoConnection("No internet")

        val appError = exception.toAppError()

        assertEquals(ErrorCode.NETWORK_NO_CONNECTION, appError.code)
        assertEquals("No internet", appError.message)
    }

    @Test
    fun toAppError_preserves_cause() {
        val cause = RuntimeException("Original cause")
        val exception = NetworkException.Timeout("Timeout", cause)

        val appError = exception.toAppError()

        assertEquals(cause, appError.cause)
    }

    @Test
    fun toAppError_uses_default_message_when_null() {
        val exception = NetworkException.SslError()

        val appError = exception.toAppError()

        assertEquals("SSL certificate error", appError.message)
    }

    // ==================== fromHttpStatus() ====================

    @Test
    fun fromHttpStatus_creates_ClientError_for_4xx() {
        val exception = NetworkException.fromHttpStatus(401, "Unauthorized")

        assertTrue(exception is NetworkException.ClientError)
        assertEquals(401, (exception as NetworkException.ClientError).statusCode)
        assertEquals("Unauthorized", exception.message)
    }

    @Test
    fun fromHttpStatus_creates_ClientError_for_404() {
        val exception = NetworkException.fromHttpStatus(404, "Not Found")

        assertTrue(exception is NetworkException.ClientError)
        assertEquals(404, (exception as NetworkException.ClientError).statusCode)
    }

    @Test
    fun fromHttpStatus_creates_ServerError_for_5xx() {
        val exception = NetworkException.fromHttpStatus(503, "Service Unavailable")

        assertTrue(exception is NetworkException.ServerError)
        assertEquals(503, (exception as NetworkException.ServerError).statusCode)
    }

    @Test
    fun fromHttpStatus_creates_ServerError_for_500() {
        val exception = NetworkException.fromHttpStatus(500)

        assertTrue(exception is NetworkException.ServerError)
        assertEquals("HTTP 500", exception.message)
    }

    @Test
    fun fromHttpStatus_defaults_to_ServerError_for_unknown_codes() {
        val exception = NetworkException.fromHttpStatus(600, "Unknown")

        assertTrue(exception is NetworkException.ServerError)
    }

    // ==================== DEFAULT MESSAGES ====================

    @Test
    fun Timeout_has_default_message() {
        val exception = NetworkException.Timeout()
        assertEquals("Request timed out", exception.message)
    }

    @Test
    fun NoConnection_has_default_message() {
        val exception = NetworkException.NoConnection()
        assertEquals("No network connection", exception.message)
    }

    @Test
    fun SslError_has_default_message() {
        val exception = NetworkException.SslError()
        assertEquals("SSL certificate error", exception.message)
    }

    @Test
    fun DnsFailure_has_default_message() {
        val exception = NetworkException.DnsFailure()
        assertEquals("DNS resolution failed", exception.message)
    }

    @Test
    fun ConnectionReset_has_default_message() {
        val exception = NetworkException.ConnectionReset()
        assertEquals("Connection reset by server", exception.message)
    }
}

class ExceptionMapperTest {

    // ==================== PASS-THROUGH ====================

    @Test
    fun map_returns_same_instance_for_NetworkException() {
        val original = NetworkException.Timeout("Already mapped")

        val mapped = ExceptionMapper.map(original)

        assertSame(original, mapped)
    }

    // ==================== TIMEOUT DETECTION ====================

    @Test
    fun maps_timeout_message_to_Timeout_exception() {
        val exception = Exception("Connection timeout occurred")

        val mapped = ExceptionMapper.map(exception)

        assertTrue(mapped is NetworkException.Timeout)
    }

    @Test
    fun maps_timed_out_message_to_Timeout_exception() {
        val exception = Exception("Request timed out")

        val mapped = ExceptionMapper.map(exception)

        assertTrue(mapped is NetworkException.Timeout)
    }

    // ==================== SSL DETECTION ====================

    @Test
    fun maps_SSL_message_to_SslError_exception() {
        val exception = Exception("SSL certificate validation failed")

        val mapped = ExceptionMapper.map(exception)

        assertTrue(mapped is NetworkException.SslError)
    }

    @Test
    fun maps_certificate_message_to_SslError_exception() {
        val exception = Exception("Invalid certificate chain")

        val mapped = ExceptionMapper.map(exception)

        assertTrue(mapped is NetworkException.SslError)
    }

    // ==================== DNS DETECTION ====================

    @Test
    fun maps_DNS_message_to_DnsFailure_exception() {
        val exception = Exception("DNS resolution failed")

        val mapped = ExceptionMapper.map(exception)

        assertTrue(mapped is NetworkException.DnsFailure)
    }

    @Test
    fun maps_unknown_host_message_to_DnsFailure_exception() {
        val exception = Exception("Unknown host: api.example.com")

        val mapped = ExceptionMapper.map(exception)

        assertTrue(mapped is NetworkException.DnsFailure)
    }

    @Test
    fun maps_unable_to_resolve_message_to_DnsFailure_exception() {
        val exception = Exception("Unable to resolve host: api.example.com")

        val mapped = ExceptionMapper.map(exception)

        assertTrue(mapped is NetworkException.DnsFailure)
    }

    // ==================== CONNECTION DETECTION ====================

    @Test
    fun maps_connection_refused_to_ConnectionReset() {
        val exception = Exception("Connection refused")

        val mapped = ExceptionMapper.map(exception)

        assertTrue(mapped is NetworkException.ConnectionReset)
    }

    @Test
    fun maps_connection_reset_to_ConnectionReset() {
        val exception = Exception("Connection reset by peer")

        val mapped = ExceptionMapper.map(exception)

        assertTrue(mapped is NetworkException.ConnectionReset)
    }

    @Test
    fun maps_connection_closed_to_ConnectionReset() {
        val exception = Exception("Connection closed unexpectedly")

        val mapped = ExceptionMapper.map(exception)

        assertTrue(mapped is NetworkException.ConnectionReset)
    }

    // ==================== NO CONNECTION DETECTION ====================

    @Test
    fun maps_no_route_to_NoConnection() {
        val exception = Exception("No route to host")

        val mapped = ExceptionMapper.map(exception)

        assertTrue(mapped is NetworkException.NoConnection)
    }

    @Test
    fun maps_network_unreachable_to_NoConnection() {
        val exception = Exception("Network is unreachable")

        val mapped = ExceptionMapper.map(exception)

        assertTrue(mapped is NetworkException.NoConnection)
    }

    // ==================== FALLBACK ====================

    @Test
    fun maps_unknown_exception_to_ServerError() {
        val exception = Exception("Some random error")

        val mapped = ExceptionMapper.map(exception)

        assertTrue(mapped is NetworkException.ServerError)
        assertEquals(500, (mapped as NetworkException.ServerError).statusCode)
    }

    @Test
    fun preserves_original_message_in_mapped_exception() {
        val exception = Exception("Custom error message")

        val mapped = ExceptionMapper.map(exception)

        assertEquals("Custom error message", mapped.message)
    }

    @Test
    fun preserves_original_cause_in_mapped_exception() {
        val exception = Exception("Error")

        val mapped = ExceptionMapper.map(exception)

        assertEquals(exception, mapped.cause)
    }

    // ==================== runCatching ====================

    @Test
    fun runCatching_returns_success_for_successful_block() {
        val result = ExceptionMapper.runCatching { "success" }

        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
    }

    @Test
    fun runCatching_returns_failure_with_mapped_exception() {
        val result = ExceptionMapper.runCatching<String> {
            throw Exception("Connection timeout")
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException.Timeout)
    }
}
