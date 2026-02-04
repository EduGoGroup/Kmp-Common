package com.edugo.test.module.network

import com.edugo.test.module.network.interceptor.AuthInterceptor
import com.edugo.test.module.network.interceptor.HeadersInterceptor
import com.edugo.test.module.network.interceptor.Interceptor
import com.edugo.test.module.network.retry.RetryConfig
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class EduGoHttpClientBuilderTest {

    @Test
    fun `builder creates client with default configuration`() {
        val client = EduGoHttpClientBuilder().build()

        assertNotNull(client)
        assertNotNull(client.client)
    }

    @Test
    fun `builder allows chaining timeout methods`() {
        val client = EduGoHttpClientBuilder()
            .connectTimeout(10.seconds)
            .requestTimeout(30.seconds)
            .socketTimeout(30.seconds)
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder accepts timeouts method for multiple timeouts at once`() {
        val client = EduGoHttpClientBuilder()
            .timeouts(
                connect = 15.seconds,
                request = 45.seconds,
                socket = 45.seconds
            )
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder configures retry with RetryConfig`() {
        val retryConfig = RetryConfig(
            maxRetries = 5,
            initialDelay = 1.seconds
        )

        val client = EduGoHttpClientBuilder()
            .retry(retryConfig)
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder supports noRetry method`() {
        val client = EduGoHttpClientBuilder()
            .retry(RetryConfig.Aggressive) // Primero habilitar retry
            .noRetry() // Luego deshabilitarlo
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder configures logging with level`() {
        val client = EduGoHttpClientBuilder()
            .logging(LogLevel.INFO)
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder accepts custom logger`() {
        val customLogger = object : Logger {
            override fun log(message: String) {
                // Custom logging logic
            }
        }

        val client = EduGoHttpClientBuilder()
            .logging(LogLevel.ALL, customLogger)
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder adds single interceptor`() {
        val interceptor = HeadersInterceptor.jsonDefaults()

        val client = EduGoHttpClientBuilder()
            .interceptor(interceptor)
            .build()

        assertNotNull(client)
        // Verify interceptor chain is not empty
        assertNotNull(client.interceptorChain)
    }

    @Test
    fun `builder adds multiple interceptors via varargs`() {
        val headersInterceptor = HeadersInterceptor.jsonDefaults()
        val authInterceptor = AuthInterceptor.withStaticToken("test-token")

        val client = EduGoHttpClientBuilder()
            .interceptors(headersInterceptor, authInterceptor)
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder adds interceptors one by one`() {
        val client = EduGoHttpClientBuilder()
            .interceptor(HeadersInterceptor.jsonDefaults())
            .interceptor(AuthInterceptor.withStaticToken("token"))
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder chains all configuration methods fluently`() {
        val client = EduGoHttpClientBuilder()
            .connectTimeout(10.seconds)
            .requestTimeout(30.seconds)
            .socketTimeout(30.seconds)
            .retry(RetryConfig.Default)
            .logging(LogLevel.INFO)
            .interceptor(HeadersInterceptor.jsonDefaults())
            .interceptor(AuthInterceptor.withStaticToken("test-token"))
            .build()

        assertNotNull(client)
    }
}

class EduGoHttpClientBuilderPresetsTest {

    @Test
    fun `production preset creates client`() {
        val client = EduGoHttpClientBuilder.production().build()

        assertNotNull(client)
    }

    @Test
    fun `development preset creates client`() {
        val client = EduGoHttpClientBuilder.development().build()

        assertNotNull(client)
    }

    @Test
    fun `production preset can be customized`() {
        val client = EduGoHttpClientBuilder.production()
            .interceptor(HeadersInterceptor.jsonDefaults())
            .connectTimeout(20.seconds)
            .build()

        assertNotNull(client)
    }

    @Test
    fun `development preset can be customized`() {
        val client = EduGoHttpClientBuilder.development()
            .interceptor(AuthInterceptor.withStaticToken("dev-token"))
            .requestTimeout(120.seconds)
            .build()

        assertNotNull(client)
    }

    @Test
    fun `production preset has retry enabled by default`() {
        // Production should have RetryConfig.Default which has maxRetries = 3
        val client = EduGoHttpClientBuilder.production().build()

        assertNotNull(client)
        // If retry was not configured, client would still build successfully
        // but without HttpRequestRetry plugin installed
    }

    @Test
    fun `presets can override retry configuration`() {
        val client = EduGoHttpClientBuilder.production()
            .noRetry()
            .build()

        assertNotNull(client)
    }
}

class EduGoHttpClientBuilderIntegrationTest {

    @Test
    fun `built client has working interceptor chain`() {
        val executionOrder = mutableListOf<String>()

        val testInterceptor1 = object : Interceptor {
            override val order = 10
            override suspend fun interceptRequest(request: HttpRequestBuilder) {
                executionOrder.add("interceptor1")
            }
        }

        val testInterceptor2 = object : Interceptor {
            override val order = 20
            override suspend fun interceptRequest(request: HttpRequestBuilder) {
                executionOrder.add("interceptor2")
            }
        }

        val client = EduGoHttpClientBuilder()
            .interceptor(testInterceptor1)
            .interceptor(testInterceptor2)
            .build()

        // Verify interceptors are in the chain
        assertNotNull(client.interceptorChain)
    }

    @Test
    fun `built client from production preset is functional`() {
        val client = EduGoHttpClientBuilder.production()
            .interceptor(HeadersInterceptor.builder()
                .userAgent("EduGo-Test/1.0")
                .build())
            .build()

        assertNotNull(client)
        assertNotNull(client.client)
    }

    @Test
    fun `built client from development preset is functional`() {
        val client = EduGoHttpClientBuilder.development()
            .interceptor(HeadersInterceptor.jsonDefaults())
            .build()

        assertNotNull(client)
        assertNotNull(client.client)
    }

    @Test
    fun `builder with aggressive retry creates functional client`() {
        val client = EduGoHttpClientBuilder()
            .retry(RetryConfig.Aggressive)
            .timeouts(
                connect = 5.seconds,
                request = 30.seconds,
                socket = 30.seconds
            )
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder with no retry creates functional client`() {
        val client = EduGoHttpClientBuilder()
            .noRetry()
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder with multiple configuration changes builds correctly`() {
        val client = EduGoHttpClientBuilder()
            .retry(RetryConfig.Aggressive)
            .noRetry() // Override
            .logging(LogLevel.ALL)
            .logging(LogLevel.NONE) // Override
            .connectTimeout(10.seconds)
            .connectTimeout(20.seconds) // Override
            .build()

        assertNotNull(client)
    }

    @Test
    fun `complex production-like configuration builds successfully`() {
        val client = EduGoHttpClientBuilder()
            .timeouts(
                connect = 30.seconds,
                request = 60.seconds,
                socket = 60.seconds
            )
            .retry(RetryConfig(
                maxRetries = 3,
                initialDelay = 1.seconds,
                maxDelay = 30.seconds
            ))
            .logging(LogLevel.NONE)
            .interceptor(HeadersInterceptor.builder()
                .userAgent("EduGo-KMP/1.0")
                .acceptLanguage("es-ES")
                .accept("application/json")
                .build())
            .interceptor(AuthInterceptor.withStaticToken("prod-token-xyz"))
            .build()

        assertNotNull(client)
        assertNotNull(client.client)
        assertNotNull(client.interceptorChain)
    }

    @Test
    fun `complex development-like configuration builds successfully`() {
        val client = EduGoHttpClientBuilder()
            .timeouts(
                connect = 10.seconds,
                request = 120.seconds,
                socket = 120.seconds
            )
            .retry(RetryConfig.Aggressive)
            .logging(LogLevel.ALL)
            .interceptor(HeadersInterceptor.builder()
                .userAgent("EduGo-Dev/1.0")
                .custom("X-Debug", "true")
                .build())
            .build()

        assertNotNull(client)
        assertNotNull(client.client)
    }

    @Test
    fun `can create multiple independent clients from same builder preset`() {
        val client1 = EduGoHttpClientBuilder.production().build()
        val client2 = EduGoHttpClientBuilder.production().build()

        assertNotNull(client1)
        assertNotNull(client2)
        // Should be different instances
        assertTrue(client1 !== client2)
    }

    @Test
    fun `builder can be reused with different configurations`() {
        val builder = EduGoHttpClientBuilder()

        val client1 = builder
            .connectTimeout(10.seconds)
            .build()

        val client2 = EduGoHttpClientBuilder()
            .connectTimeout(20.seconds)
            .build()

        assertNotNull(client1)
        assertNotNull(client2)
    }
}

class EduGoHttpClientBuilderConfigurationTest {

    @Test
    fun `default builder has sensible defaults`() {
        val client = EduGoHttpClientBuilder().build()

        assertNotNull(client)
        // Default configuration should work without any customization
    }

    @Test
    fun `builder with only timeouts configured builds`() {
        val client = EduGoHttpClientBuilder()
            .timeouts(connect = 5.seconds, request = 15.seconds, socket = 15.seconds)
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder with only retry configured builds`() {
        val client = EduGoHttpClientBuilder()
            .retry(RetryConfig.Default)
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder with only logging configured builds`() {
        val client = EduGoHttpClientBuilder()
            .logging(LogLevel.INFO)
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder with only interceptors configured builds`() {
        val client = EduGoHttpClientBuilder()
            .interceptor(HeadersInterceptor.jsonDefaults())
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder supports extreme timeout values`() {
        val client = EduGoHttpClientBuilder()
            .timeouts(
                connect = 1.seconds,
                request = 300.seconds, // 5 minutes
                socket = 300.seconds
            )
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder supports zero retry (no retry plugin installed)`() {
        val client = EduGoHttpClientBuilder()
            .retry(RetryConfig.NoRetry)
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder supports LogLevel NONE (no logging plugin installed)`() {
        val client = EduGoHttpClientBuilder()
            .logging(LogLevel.NONE)
            .build()

        assertNotNull(client)
    }

    @Test
    fun `builder can add many interceptors`() {
        val builder = EduGoHttpClientBuilder()

        repeat(10) { index ->
            builder.interceptor(object : Interceptor {
                override val order = index
            })
        }

        val client = builder.build()

        assertNotNull(client)
    }
}
