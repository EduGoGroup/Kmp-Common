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
data class CreateUserRequest(val name: String, val email: String)

@Serializable
data class UserResponse(val id: Int, val name: String, val email: String)

@Serializable
data class PatchRequest(val name: String)

class EduGoHttpClientPostPutPatchTest {

    @Test
    fun post_serializes_body_and_deserializes_response() = runTest {
        var capturedBody: String? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedBody = request.body.toByteArray().decodeToString()
            respond(
                """{"id": 1, "name": "John", "email": "john@test.com"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        val request = CreateUserRequest("John", "john@test.com")

        val response: UserResponse = client.post("https://api.test.com/users", request)

        assertEquals(1, response.id)
        assertEquals("John", response.name)
        assertNotNull(capturedBody)
        assertTrue(capturedBody!!.contains("John"), "Body should contain 'John'")
        assertTrue(capturedBody!!.contains("john@test.com"), "Body should contain email")
    }

    @Test
    fun post_sets_content_type_to_application_json() = runTest {
        var capturedContentType: ContentType? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedContentType = request.body.contentType
            respond(
                """{"id": 1, "name": "Test", "email": "test@test.com"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        client.post<CreateUserRequest, UserResponse>(
            "https://api.test.com/users",
            CreateUserRequest("Test", "test@test.com")
        )

        assertEquals(ContentType.Application.Json, capturedContentType)
    }

    @Test
    fun post_uses_correct_http_method() = runTest {
        var capturedMethod: HttpMethod? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedMethod = request.method
            respond(
                """{"id": 1, "name": "Test", "email": "test@test.com"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        client.post<CreateUserRequest, UserResponse>(
            "https://api.test.com/users",
            CreateUserRequest("Test", "test@test.com")
        )

        assertEquals(HttpMethod.Post, capturedMethod)
    }

    @Test
    fun put_uses_correct_http_method() = runTest {
        var capturedMethod: HttpMethod? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedMethod = request.method
            respond(
                """{"id": 1, "name": "Updated", "email": "updated@test.com"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        val response: UserResponse = client.put(
            "https://api.test.com/users/1",
            CreateUserRequest("Updated", "updated@test.com")
        )

        assertEquals(HttpMethod.Put, capturedMethod)
        assertEquals("Updated", response.name)
    }

    @Test
    fun put_serializes_body_correctly() = runTest {
        var capturedBody: String? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedBody = request.body.toByteArray().decodeToString()
            respond(
                """{"id": 1, "name": "Updated", "email": "updated@test.com"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        client.put<CreateUserRequest, UserResponse>(
            "https://api.test.com/users/1",
            CreateUserRequest("Updated", "updated@test.com")
        )

        assertNotNull(capturedBody)
        assertTrue(capturedBody!!.contains("Updated"), "Body should contain 'Updated'")
    }

    @Test
    fun patch_uses_correct_http_method() = runTest {
        var capturedMethod: HttpMethod? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedMethod = request.method
            respond(
                """{"id": 1, "name": "Patched", "email": "old@test.com"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        val response: UserResponse = client.patch(
            "https://api.test.com/users/1",
            PatchRequest("Patched")
        )

        assertEquals(HttpMethod.Patch, capturedMethod)
        assertEquals("Patched", response.name)
    }

    @Test
    fun patch_serializes_partial_body() = runTest {
        var capturedBody: String? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedBody = request.body.toByteArray().decodeToString()
            respond(
                """{"id": 1, "name": "Patched", "email": "old@test.com"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        client.patch<PatchRequest, UserResponse>(
            "https://api.test.com/users/1",
            PatchRequest("Patched")
        )

        assertNotNull(capturedBody)
        assertTrue(capturedBody!!.contains("Patched"), "Body should contain 'Patched'")
        assertFalse(capturedBody!!.contains("email"), "Partial body should not contain email")
    }
}
