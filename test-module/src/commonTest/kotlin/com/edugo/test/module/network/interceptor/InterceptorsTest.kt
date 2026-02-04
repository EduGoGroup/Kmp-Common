package com.edugo.test.module.network.interceptor

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class HeadersInterceptorTest {

    @Test
    fun `adds configured headers to request`() = runTest {
        val interceptor = HeadersInterceptor.builder()
            .userAgent("TestApp/1.0")
            .custom("X-Custom", "value")
            .build()

        val request = HttpRequestBuilder().apply { url("https://api.test.com") }
        interceptor.interceptRequest(request)

        assertEquals("TestApp/1.0", request.headers[HttpHeaders.UserAgent])
        assertEquals("value", request.headers["X-Custom"])
    }

    @Test
    fun `does not overwrite existing headers`() = runTest {
        val interceptor = HeadersInterceptor.builder()
            .userAgent("Default")
            .build()

        val request = HttpRequestBuilder().apply {
            url("https://api.test.com")
            header(HttpHeaders.UserAgent, "Existing")
        }

        interceptor.interceptRequest(request)

        assertEquals("Existing", request.headers[HttpHeaders.UserAgent])
    }

    @Test
    fun `jsonDefaults sets correct content type`() = runTest {
        val interceptor = HeadersInterceptor.jsonDefaults()
        val request = HttpRequestBuilder().apply { url("https://api.test.com") }

        interceptor.interceptRequest(request)

        assertTrue(request.headers[HttpHeaders.Accept]?.contains("application/json") == true)
    }

    @Test
    fun `builder allows multiple headers`() = runTest {
        val interceptor = HeadersInterceptor.builder()
            .userAgent("MyApp/2.0")
            .acceptLanguage("es-ES")
            .accept("application/json")
            .custom("X-Api-Version", "v1")
            .build()

        val request = HttpRequestBuilder().apply { url("https://api.test.com") }
        interceptor.interceptRequest(request)

        assertEquals("MyApp/2.0", request.headers[HttpHeaders.UserAgent])
        assertEquals("es-ES", request.headers[HttpHeaders.AcceptLanguage])
        assertEquals("application/json", request.headers[HttpHeaders.Accept])
        assertEquals("v1", request.headers["X-Api-Version"])
    }

    @Test
    fun `has correct order value`() {
        val interceptor = HeadersInterceptor.jsonDefaults()
        assertEquals(10, interceptor.order)
    }
}

class AuthInterceptorTest {

    @Test
    fun `adds Bearer token to request`() = runTest {
        val interceptor = AuthInterceptor.withStaticToken("test-token-123")
        val request = HttpRequestBuilder().apply { url("https://api.test.com") }

        interceptor.interceptRequest(request)

        assertEquals("Bearer test-token-123", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `does not overwrite existing Authorization`() = runTest {
        val interceptor = AuthInterceptor.withStaticToken("new-token")
        val request = HttpRequestBuilder().apply {
            url("https://api.test.com")
            header(HttpHeaders.Authorization, "Bearer existing-token")
        }

        interceptor.interceptRequest(request)

        assertEquals("Bearer existing-token", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `withStaticToken does not auto-refresh`() = runTest {
        val interceptor = AuthInterceptor.withStaticToken("static-token")
        val request = HttpRequestBuilder().apply { url("https://api.test.com") }

        // Ejecutar varias veces, el token debe ser el mismo
        interceptor.interceptRequest(request)
        val firstToken = request.headers[HttpHeaders.Authorization]

        val request2 = HttpRequestBuilder().apply { url("https://api.test.com") }
        interceptor.interceptRequest(request2)
        val secondToken = request2.headers[HttpHeaders.Authorization]

        assertEquals(firstToken, secondToken)
        assertEquals("Bearer static-token", firstToken)
    }

    @Test
    fun `has correct order value`() {
        val interceptor = AuthInterceptor.withStaticToken("token")
        assertEquals(20, interceptor.order)
    }

    @Test
    fun `custom TokenProvider allows dynamic tokens`() = runTest {
        var tokenCounter = 0
        val tokenProvider = object : TokenProvider {
            override suspend fun getToken(): String = "token-${++tokenCounter}"
            override suspend fun refreshToken(): String = "refreshed-${++tokenCounter}"
            override suspend fun isTokenExpired(): Boolean = false
        }

        val interceptor = AuthInterceptor(tokenProvider, autoRefresh = false)

        val request1 = HttpRequestBuilder().apply { url("https://api.test.com") }
        interceptor.interceptRequest(request1)
        assertEquals("Bearer token-1", request1.headers[HttpHeaders.Authorization])

        val request2 = HttpRequestBuilder().apply { url("https://api.test.com") }
        interceptor.interceptRequest(request2)
        assertEquals("Bearer token-2", request2.headers[HttpHeaders.Authorization])
    }
}

class InterceptorChainTest {

    @Test
    fun `executes interceptors in order by order property`() = runTest {
        val executionOrder = mutableListOf<Int>()

        val interceptor1 = object : Interceptor {
            override val order = 1
            override suspend fun interceptRequest(request: HttpRequestBuilder) {
                executionOrder.add(1)
            }
        }

        val interceptor2 = object : Interceptor {
            override val order = 2
            override suspend fun interceptRequest(request: HttpRequestBuilder) {
                executionOrder.add(2)
            }
        }

        // Agregar en orden invertido para verificar que se ordena por `order`
        val chain = InterceptorChain(listOf(interceptor2, interceptor1))
        chain.processRequest(HttpRequestBuilder())

        assertEquals(listOf(1, 2), executionOrder)
    }

    @Test
    fun `empty chain does not fail`() = runTest {
        val chain = InterceptorChain.Empty
        val request = HttpRequestBuilder().apply { url("https://api.test.com") }

        // No debe lanzar excepción
        chain.processRequest(request)
    }

    @Test
    fun `plus creates new chain with added interceptor`() = runTest {
        val interceptor1 = HeadersInterceptor.jsonDefaults()
        val chain1 = InterceptorChain(listOf(interceptor1))

        val interceptor2 = AuthInterceptor.withStaticToken("token")
        val chain2 = chain1.plus(interceptor2)

        val request = HttpRequestBuilder().apply { url("https://api.test.com") }
        chain2.processRequest(request)

        // Ambos interceptores deben haber ejecutado
        assertNotNull(request.headers[HttpHeaders.Accept]) // De HeadersInterceptor
        assertNotNull(request.headers[HttpHeaders.Authorization]) // De AuthInterceptor
    }

    @Test
    fun `interceptors modify request in-place`() = runTest {
        val interceptor = HeadersInterceptor.builder()
            .custom("X-Test", "value1")
            .build()

        val chain = InterceptorChain(listOf(interceptor))
        val request = HttpRequestBuilder().apply { url("https://api.test.com") }

        chain.processRequest(request)

        assertEquals("value1", request.headers["X-Test"])
    }

    @Test
    fun `notifyError calls onError on all interceptors`() = runTest {
        val errorsCalled = mutableListOf<Int>()

        val interceptor1 = object : Interceptor {
            override val order = 1
            override suspend fun onError(request: HttpRequestBuilder, exception: Throwable) {
                errorsCalled.add(1)
            }
        }

        val interceptor2 = object : Interceptor {
            override val order = 2
            override suspend fun onError(request: HttpRequestBuilder, exception: Throwable) {
                errorsCalled.add(2)
            }
        }

        val chain = InterceptorChain(listOf(interceptor1, interceptor2))
        val request = HttpRequestBuilder().apply { url("https://api.test.com") }
        val exception = RuntimeException("Test error")

        chain.notifyError(request, exception)

        assertEquals(listOf(1, 2), errorsCalled)
    }
}
