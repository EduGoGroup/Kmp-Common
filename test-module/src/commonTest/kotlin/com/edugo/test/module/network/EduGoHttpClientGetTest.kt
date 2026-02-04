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
data class TestUser(val id: Int, val name: String)

class EduGoHttpClientGetTest {

    private fun createMockClient(
        responseJson: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ): HttpClient {
        return HttpClient(MockEngine { _ ->
            respond(
                content = responseJson,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun get_deserializes_response_correctly() = runTest {
        val mockClient = createMockClient("""{"id": 1, "name": "John"}""")
        val client = EduGoHttpClient.withClient(mockClient)

        val user: TestUser = client.get("https://api.test.com/user/1")

        assertEquals(1, user.id)
        assertEquals("John", user.name)
    }

    @Test
    fun get_includes_custom_headers() = runTest {
        var capturedHeaders: Headers? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedHeaders = request.headers
            respond(
                """{"id": 1, "name": "Test"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        val config = HttpRequestConfig.builder()
            .header("Authorization", "Bearer token123")
            .build()

        client.get<TestUser>("https://api.test.com/user", config)

        assertEquals("Bearer token123", capturedHeaders?.get("Authorization"))
    }

    @Test
    fun get_includes_query_parameters() = runTest {
        var capturedUrl: String? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                """{"id": 1, "name": "Test"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        val config = HttpRequestConfig.builder()
            .queryParam("page", "1")
            .queryParam("limit", "10")
            .build()

        client.get<TestUser>("https://api.test.com/users", config)

        assertNotNull(capturedUrl)
        assertTrue(capturedUrl!!.contains("page=1"), "URL should contain page=1")
        assertTrue(capturedUrl!!.contains("limit=10"), "URL should contain limit=10")
    }

    @Test
    fun get_uses_correct_http_method() = runTest {
        var capturedMethod: HttpMethod? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedMethod = request.method
            respond(
                """{"id": 1, "name": "Test"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        client.get<TestUser>("https://api.test.com/user/1")

        assertEquals(HttpMethod.Get, capturedMethod)
    }
}
