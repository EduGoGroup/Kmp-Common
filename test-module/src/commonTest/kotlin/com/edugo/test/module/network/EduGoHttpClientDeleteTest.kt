package com.edugo.test.module.network

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.*

@Serializable
data class DeleteResponse(val success: Boolean, val message: String)

class EduGoHttpClientDeleteTest {

    @Test
    fun delete_uses_correct_http_method() = runTest {
        var capturedMethod: HttpMethod? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedMethod = request.method
            respond(
                """{"success": true, "message": "Deleted"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        val response: DeleteResponse = client.delete("https://api.test.com/users/1")

        assertEquals(HttpMethod.Delete, capturedMethod)
        assertTrue(response.success)
        assertEquals("Deleted", response.message)
    }

    @Test
    fun deleteNoResponse_handles_204_no_content() = runTest {
        var capturedMethod: HttpMethod? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedMethod = request.method
            respond("", status = HttpStatusCode.NoContent)
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)

        // Should not throw exception
        client.deleteNoResponse("https://api.test.com/users/1")

        assertEquals(HttpMethod.Delete, capturedMethod)
    }

    @Test
    fun deleteNoResponse_handles_200_ok_without_body() = runTest {
        var capturedMethod: HttpMethod? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedMethod = request.method
            respond("", status = HttpStatusCode.OK)
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)

        // Should not throw exception
        client.deleteNoResponse("https://api.test.com/users/1")

        assertEquals(HttpMethod.Delete, capturedMethod)
    }

    @Test
    fun delete_includes_custom_headers() = runTest {
        var capturedHeaders: Headers? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedHeaders = request.headers
            respond(
                """{"success": true, "message": "OK"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        val config = HttpRequestConfig.builder()
            .header("X-Request-ID", "abc123")
            .header("Authorization", "Bearer token")
            .build()

        client.delete<DeleteResponse>("https://api.test.com/users/1", config)

        assertEquals("abc123", capturedHeaders?.get("X-Request-ID"))
        assertEquals("Bearer token", capturedHeaders?.get("Authorization"))
    }

    @Test
    fun delete_includes_query_parameters() = runTest {
        var capturedUrl: String? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                """{"success": true, "message": "OK"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        val config = HttpRequestConfig.builder()
            .queryParam("force", "true")
            .build()

        client.delete<DeleteResponse>("https://api.test.com/users/1", config)

        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("force=true"), "URL should contain force=true")
    }

    @Test
    fun deleteNoResponse_includes_custom_headers() = runTest {
        var capturedHeaders: Headers? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedHeaders = request.headers
            respond("", status = HttpStatusCode.NoContent)
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        val config = HttpRequestConfig.builder()
            .header("Authorization", "Bearer admin-token")
            .build()

        client.deleteNoResponse("https://api.test.com/users/1", config)

        assertEquals("Bearer admin-token", capturedHeaders?.get("Authorization"))
    }
}
