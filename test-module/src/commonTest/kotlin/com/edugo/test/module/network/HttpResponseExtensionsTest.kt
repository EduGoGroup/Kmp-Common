package com.edugo.test.module.network

import com.edugo.test.module.core.Result
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.*

@Serializable
data class TestData(val id: Int, val name: String)

@Serializable
data class TestRequest(val name: String)

class HttpResponseExtensionsTest {

    private fun createMockClient(
        responseBody: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ): HttpClient {
        return HttpClient(MockEngine { _ ->
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }
    }

    // ==================== HttpStatusCode Extensions ====================

    @Test
    fun isSuccess_returns_true_for_2xx() {
        assertTrue(HttpStatusCode.OK.isSuccess())
        assertTrue(HttpStatusCode.Created.isSuccess())
        assertTrue(HttpStatusCode.NoContent.isSuccess())
    }

    @Test
    fun isSuccess_returns_false_for_non_2xx() {
        assertFalse(HttpStatusCode.BadRequest.isSuccess())
        assertFalse(HttpStatusCode.NotFound.isSuccess())
        assertFalse(HttpStatusCode.InternalServerError.isSuccess())
    }

    @Test
    fun isClientError_returns_true_for_4xx() {
        assertTrue(HttpStatusCode.BadRequest.isClientError())
        assertTrue(HttpStatusCode.Unauthorized.isClientError())
        assertTrue(HttpStatusCode.NotFound.isClientError())
    }

    @Test
    fun isClientError_returns_false_for_non_4xx() {
        assertFalse(HttpStatusCode.OK.isClientError())
        assertFalse(HttpStatusCode.InternalServerError.isClientError())
    }

    @Test
    fun isServerError_returns_true_for_5xx() {
        assertTrue(HttpStatusCode.InternalServerError.isServerError())
        assertTrue(HttpStatusCode.BadGateway.isServerError())
        assertTrue(HttpStatusCode.ServiceUnavailable.isServerError())
    }

    @Test
    fun isServerError_returns_false_for_non_5xx() {
        assertFalse(HttpStatusCode.OK.isServerError())
        assertFalse(HttpStatusCode.NotFound.isServerError())
    }

    // ==================== getSafe ====================

    @Test
    fun getSafe_returns_Success_for_200_response() = runTest {
        val mockClient = createMockClient("""{ "id": 1, "name": "Test" }""")
        val client = EduGoHttpClient.withClient(mockClient)

        val result: Result<TestData> = client.getSafe("https://api.test.com/data")

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.id)
        assertEquals("Test", result.data.name)
    }

    @Test
    fun getSafe_returns_Failure_for_404_response() = runTest {
        val mockClient = createMockClient("""{ "error": "Not found" }""", HttpStatusCode.NotFound)
        val client = EduGoHttpClient.withClient(mockClient)

        val result: Result<TestData> = client.getSafe("https://api.test.com/data")

        assertTrue(result is Result.Failure)
    }

    @Test
    fun getSafe_returns_Failure_for_500_response() = runTest {
        val mockClient = createMockClient("""{ "error": "Server error" }""", HttpStatusCode.InternalServerError)
        val client = EduGoHttpClient.withClient(mockClient)

        val result: Result<TestData> = client.getSafe("https://api.test.com/data")

        assertTrue(result is Result.Failure)
    }

    @Test
    fun getSafe_returns_Failure_for_401_unauthorized() = runTest {
        val mockClient = createMockClient("""{ "error": "Unauthorized" }""", HttpStatusCode.Unauthorized)
        val client = EduGoHttpClient.withClient(mockClient)

        val result: Result<TestData> = client.getSafe("https://api.test.com/data")

        assertTrue(result is Result.Failure)
    }

    // ==================== postSafe ====================

    @Test
    fun postSafe_returns_Success_for_201_created() = runTest {
        val mockClient = createMockClient("""{ "id": 1, "name": "Created" }""", HttpStatusCode.Created)
        val client = EduGoHttpClient.withClient(mockClient)

        val result: Result<TestData> = client.postSafe(
            "https://api.test.com/data",
            TestRequest("New")
        )

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.id)
        assertEquals("Created", result.data.name)
    }

    @Test
    fun postSafe_returns_Success_for_200_ok() = runTest {
        val mockClient = createMockClient("""{ "id": 2, "name": "Updated" }""", HttpStatusCode.OK)
        val client = EduGoHttpClient.withClient(mockClient)

        val result: Result<TestData> = client.postSafe(
            "https://api.test.com/data",
            TestRequest("Update")
        )

        assertTrue(result is Result.Success)
    }

    @Test
    fun postSafe_returns_Failure_for_400_bad_request() = runTest {
        val mockClient = createMockClient("""{ "error": "Bad request" }""", HttpStatusCode.BadRequest)
        val client = EduGoHttpClient.withClient(mockClient)

        val result: Result<TestData> = client.postSafe(
            "https://api.test.com/data",
            TestRequest("Invalid")
        )

        assertTrue(result is Result.Failure)
    }

    // ==================== putSafe ====================

    @Test
    fun putSafe_returns_Success_for_200_response() = runTest {
        val mockClient = createMockClient("""{ "id": 1, "name": "Updated" }""")
        val client = EduGoHttpClient.withClient(mockClient)

        val result: Result<TestData> = client.putSafe(
            "https://api.test.com/data/1",
            TestRequest("Updated")
        )

        assertTrue(result is Result.Success)
        assertEquals("Updated", (result as Result.Success).data.name)
    }

    @Test
    fun putSafe_returns_Failure_for_404_not_found() = runTest {
        val mockClient = createMockClient("""{ "error": "Not found" }""", HttpStatusCode.NotFound)
        val client = EduGoHttpClient.withClient(mockClient)

        val result: Result<TestData> = client.putSafe(
            "https://api.test.com/data/999",
            TestRequest("Update")
        )

        assertTrue(result is Result.Failure)
    }

    // ==================== patchSafe ====================

    @Test
    fun patchSafe_returns_Success_for_200_response() = runTest {
        val mockClient = createMockClient("""{ "id": 1, "name": "Patched" }""")
        val client = EduGoHttpClient.withClient(mockClient)

        val result: Result<TestData> = client.patchSafe(
            "https://api.test.com/data/1",
            TestRequest("Patched")
        )

        assertTrue(result is Result.Success)
        assertEquals("Patched", (result as Result.Success).data.name)
    }

    @Test
    fun patchSafe_returns_Failure_for_422_unprocessable() = runTest {
        val mockClient = createMockClient("""{ "error": "Unprocessable" }""", HttpStatusCode.UnprocessableEntity)
        val client = EduGoHttpClient.withClient(mockClient)

        val result: Result<TestData> = client.patchSafe(
            "https://api.test.com/data/1",
            TestRequest("Invalid")
        )

        assertTrue(result is Result.Failure)
    }

    // ==================== deleteSafe ====================

    @Test
    fun deleteSafe_returns_Success_for_200_response() = runTest {
        val mockClient = createMockClient("""{ "id": 1, "name": "Deleted" }""")
        val client = EduGoHttpClient.withClient(mockClient)

        val result: Result<TestData> = client.deleteSafe("https://api.test.com/data/1")

        assertTrue(result is Result.Success)
    }

    @Test
    fun deleteSafe_returns_Failure_for_403_forbidden() = runTest {
        val mockClient = createMockClient("""{ "error": "Forbidden" }""", HttpStatusCode.Forbidden)
        val client = EduGoHttpClient.withClient(mockClient)

        val result: Result<TestData> = client.deleteSafe("https://api.test.com/data/1")

        assertTrue(result is Result.Failure)
    }

    @Test
    fun deleteSafe_returns_Failure_for_404_not_found() = runTest {
        val mockClient = createMockClient("""{ "error": "Not found" }""", HttpStatusCode.NotFound)
        val client = EduGoHttpClient.withClient(mockClient)

        val result: Result<TestData> = client.deleteSafe("https://api.test.com/data/999")

        assertTrue(result is Result.Failure)
    }

    // ==================== Headers and Config ====================

    @Test
    fun getSafe_includes_custom_headers() = runTest {
        var capturedHeaders: Headers? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedHeaders = request.headers
            respond(
                """{ "id": 1, "name": "Test" }""",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) { json() }
        }

        val client = EduGoHttpClient.withClient(mockClient)
        val config = HttpRequestConfig.builder()
            .header("Authorization", "Bearer token123")
            .build()

        client.getSafe<TestData>("https://api.test.com/data", config)

        assertEquals("Bearer token123", capturedHeaders?.get("Authorization"))
    }

    @Test
    fun getSafe_includes_query_params() = runTest {
        var capturedUrl: String? = null
        val mockClient = HttpClient(MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                """{ "id": 1, "name": "Test" }""",
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

        client.getSafe<TestData>("https://api.test.com/data", config)

        assertTrue(capturedUrl?.contains("page=1") == true)
        assertTrue(capturedUrl?.contains("limit=10") == true)
    }
}
