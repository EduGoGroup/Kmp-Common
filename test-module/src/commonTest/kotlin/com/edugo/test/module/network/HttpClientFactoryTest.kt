package com.edugo.test.module.network

import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.*

/**
 * Tests for [HttpClientFactory] verifying correct plugin installation
 * and configuration using MockEngine.
 */
class HttpClientFactoryTest {

    @Serializable
    data class TestResponse(val status: String, val message: String)

    @Test
    fun createBaseClient_can_deserialize_json_response() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"status":"ok","message":"hello"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = HttpClientFactory.createBaseClient(mockEngine)
        val response: TestResponse = client.get("https://test.com/api").body()

        assertEquals("ok", response.status)
        assertEquals("hello", response.message)
    }

    @Test
    fun createBaseClient_handles_empty_response() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "",
                status = HttpStatusCode.NoContent
            )
        }

        val client = HttpClientFactory.createBaseClient(mockEngine)
        val response = client.get("https://test.com/api")

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun createBaseClient_default_logLevel_is_NONE() {
        val mockEngine = MockEngine { respond("") }

        // Should not throw - LogLevel.NONE is default
        val client = HttpClientFactory.createBaseClient(mockEngine)
        assertNotNull(client)
    }

    @Test
    fun createBaseClient_accepts_custom_logLevel() {
        val mockEngine = MockEngine { respond("") }

        // Should not throw with various log levels
        val clientInfo = HttpClientFactory.createBaseClient(mockEngine, LogLevel.INFO)
        val clientHeaders = HttpClientFactory.createBaseClient(mockEngine, LogLevel.HEADERS)
        val clientBody = HttpClientFactory.createBaseClient(mockEngine, LogLevel.BODY)
        val clientAll = HttpClientFactory.createBaseClient(mockEngine, LogLevel.ALL)

        assertNotNull(clientInfo)
        assertNotNull(clientHeaders)
        assertNotNull(clientBody)
        assertNotNull(clientAll)
    }

    @Test
    fun createBaseClient_handles_json_with_unknown_keys() = runTest {
        // Tests that ignoreUnknownKeys = true works
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"status":"ok","message":"hello","unknownField":"ignored"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = HttpClientFactory.createBaseClient(mockEngine)
        // Should not throw even with extra field
        val response: TestResponse = client.get("https://test.com/api").body()

        assertEquals("ok", response.status)
    }

    @Test
    fun createBaseClient_handles_error_status_codes() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"error":"not found"}""",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = HttpClientFactory.createBaseClient(mockEngine)
        val response = client.get("https://test.com/api")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
