package com.edugo.test.module.network.retry

import com.edugo.test.module.network.NetworkException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RetryConfigTest {

    @Test
    fun `default configuration has correct values`() {
        val config = RetryConfig.Default

        assertEquals(3, config.maxRetries)
        assertEquals(1.seconds, config.initialDelay)
        assertEquals(30.seconds, config.maxDelay)
        assertEquals(2.0, config.multiplier)
        assertEquals(0.1, config.jitterFactor)
    }

    @Test
    fun `noRetry configuration has zero retries`() {
        val config = RetryConfig.NoRetry

        assertEquals(0, config.maxRetries)
    }

    @Test
    fun `aggressive configuration has correct values`() {
        val config = RetryConfig.Aggressive

        assertEquals(5, config.maxRetries)
        assertEquals(500.milliseconds, config.initialDelay)
        assertEquals(60.seconds, config.maxDelay)
    }

    @Test
    fun `requires non-negative maxRetries`() {
        assertFailsWith<IllegalArgumentException> {
            RetryConfig(maxRetries = -1)
        }
    }

    @Test
    fun `requires multiplier greater than or equal to 1`() {
        assertFailsWith<IllegalArgumentException> {
            RetryConfig(multiplier = 0.5)
        }
    }

    @Test
    fun `requires jitterFactor between 0 and 1`() {
        assertFailsWith<IllegalArgumentException> {
            RetryConfig(jitterFactor = -0.1)
        }

        assertFailsWith<IllegalArgumentException> {
            RetryConfig(jitterFactor = 1.5)
        }
    }

    @Test
    fun `jitterFactor at boundaries is valid`() {
        // Should not throw
        RetryConfig(jitterFactor = 0.0)
        RetryConfig(jitterFactor = 1.0)
    }

    @Test
    fun `calculateDelay returns initialDelay for first attempt`() {
        val config = RetryConfig(
            initialDelay = 1.seconds,
            multiplier = 2.0,
            jitterFactor = 0.0 // Sin jitter para test determinístico
        )

        val delay = config.calculateDelay(0)

        assertEquals(1.seconds, delay)
    }

    @Test
    fun `calculateDelay applies exponential backoff`() {
        val config = RetryConfig(
            initialDelay = 1.seconds,
            multiplier = 2.0,
            jitterFactor = 0.0 // Sin jitter para test determinístico
        )

        val delay1 = config.calculateDelay(1)
        val delay2 = config.calculateDelay(2)
        val delay3 = config.calculateDelay(3)

        assertEquals(2.seconds, delay1) // 1 * 2^1
        assertEquals(4.seconds, delay2) // 1 * 2^2
        assertEquals(8.seconds, delay3) // 1 * 2^3
    }

    @Test
    fun `calculateDelay respects maxDelay cap`() {
        val config = RetryConfig(
            initialDelay = 1.seconds,
            maxDelay = 5.seconds,
            multiplier = 2.0,
            jitterFactor = 0.0 // Sin jitter para test determinístico
        )

        val delay10 = config.calculateDelay(10) // 1 * 2^10 = 1024s, pero cap = 5s

        assertTrue(delay10 <= 5.seconds)
        assertEquals(5.seconds, delay10)
    }

    @Test
    fun `calculateDelay with jitter produces variable delays`() {
        val config = RetryConfig(
            initialDelay = 1.seconds,
            multiplier = 2.0,
            jitterFactor = 0.5 // 50% jitter
        )

        // Ejecutar varias veces y verificar que hay variabilidad
        val delays = List(10) { config.calculateDelay(1) }

        // Debería haber al menos 2 valores diferentes debido al jitter aleatorio
        val uniqueDelays = delays.toSet()
        assertTrue(uniqueDelays.size > 1, "Jitter should produce variable delays")

        // Todos los delays deberían estar dentro del rango esperado
        // 2s * (1 - 0.5) = 1s, 2s * (1 + 0.5) = 3s
        delays.forEach { delay ->
            assertTrue(delay >= 1.seconds, "Delay $delay should be >= 1s")
            assertTrue(delay <= 3.seconds, "Delay $delay should be <= 3s")
        }
    }

    @Test
    fun `calculateDelay never returns negative values`() {
        val config = RetryConfig(
            initialDelay = 100.milliseconds,
            jitterFactor = 1.0 // Máximo jitter
        )

        // Ejecutar muchas veces para asegurar que jitter negativo no produce delays negativos
        repeat(100) {
            val delay = config.calculateDelay(0)
            assertTrue(delay >= 0.milliseconds, "Delay should never be negative")
        }
    }
}

class RetryableExceptionTest {

    @Test
    fun `timeout exception is retryable`() {
        val exception = NetworkException.Timeout("Connection timeout", null)

        assertTrue(RetryConfig.isRetryableException(exception))
    }

    @Test
    fun `noConnection exception is retryable`() {
        val exception = NetworkException.NoConnection("No internet")

        assertTrue(RetryConfig.isRetryableException(exception))
    }

    @Test
    fun `connectionReset exception is retryable`() {
        val exception = NetworkException.ConnectionReset("Connection reset by peer")

        assertTrue(RetryConfig.isRetryableException(exception))
    }

    @Test
    fun `dnsFailure exception is retryable`() {
        val exception = NetworkException.DnsFailure("Cannot resolve host")

        assertTrue(RetryConfig.isRetryableException(exception))
    }

    @Test
    fun `serverError 5xx is retryable`() {
        val exception500 = NetworkException.ServerError(500, "Internal Server Error")
        val exception502 = NetworkException.ServerError(502, "Bad Gateway")
        val exception503 = NetworkException.ServerError(503, "Service Unavailable")

        assertTrue(RetryConfig.isRetryableException(exception500))
        assertTrue(RetryConfig.isRetryableException(exception502))
        assertTrue(RetryConfig.isRetryableException(exception503))
    }

    @Test
    fun `clientError 4xx is not retryable`() {
        val exception = NetworkException.ClientError(400, "Bad Request")

        assertFalse(RetryConfig.isRetryableException(exception))
    }

    @Test
    fun `unauthorized 401 exception is not retryable`() {
        val exception = NetworkException.ClientError(401, "Invalid token")

        assertFalse(RetryConfig.isRetryableException(exception))
    }

    @Test
    fun `generic exception is not retryable`() {
        val exception = RuntimeException("Generic error")

        assertFalse(RetryConfig.isRetryableException(exception))
    }

    @Test
    fun `nullPointerException is not retryable`() {
        val exception = NullPointerException("NPE")

        assertFalse(RetryConfig.isRetryableException(exception))
    }
}

class RetryableStatusCodeTest {

    @Test
    fun `status 500 is retryable`() {
        assertTrue(RetryConfig.isRetryableStatusCode(500))
    }

    @Test
    fun `status 502 is retryable`() {
        assertTrue(RetryConfig.isRetryableStatusCode(502))
    }

    @Test
    fun `status 503 is retryable`() {
        assertTrue(RetryConfig.isRetryableStatusCode(503))
    }

    @Test
    fun `status 504 is retryable`() {
        assertTrue(RetryConfig.isRetryableStatusCode(504))
    }

    @Test
    fun `status 429 Too Many Requests is retryable`() {
        assertTrue(RetryConfig.isRetryableStatusCode(429))
    }

    @Test
    fun `status 400 is not retryable`() {
        assertFalse(RetryConfig.isRetryableStatusCode(400))
    }

    @Test
    fun `status 401 is not retryable`() {
        assertFalse(RetryConfig.isRetryableStatusCode(401))
    }

    @Test
    fun `status 403 is not retryable`() {
        assertFalse(RetryConfig.isRetryableStatusCode(403))
    }

    @Test
    fun `status 404 is not retryable`() {
        assertFalse(RetryConfig.isRetryableStatusCode(404))
    }

    @Test
    fun `status 200 is not retryable`() {
        assertFalse(RetryConfig.isRetryableStatusCode(200))
    }

    @Test
    fun `all 5xx range is retryable`() {
        (500..599).forEach { statusCode ->
            assertTrue(
                RetryConfig.isRetryableStatusCode(statusCode),
                "Status $statusCode should be retryable"
            )
        }
    }
}

class ExponentialBackoffIntegrationTest {

    @Test
    fun `exponential backoff progression with realistic config`() {
        val config = RetryConfig(
            initialDelay = 1.seconds,
            maxDelay = 30.seconds,
            multiplier = 2.0,
            jitterFactor = 0.0
        )

        val delays = (0..5).map { attempt ->
            config.calculateDelay(attempt)
        }

        // Verify exponential progression
        assertEquals(1.seconds, delays[0])   // 1 * 2^0 = 1
        assertEquals(2.seconds, delays[1])   // 1 * 2^1 = 2
        assertEquals(4.seconds, delays[2])   // 1 * 2^2 = 4
        assertEquals(8.seconds, delays[3])   // 1 * 2^3 = 8
        assertEquals(16.seconds, delays[4])  // 1 * 2^4 = 16
        assertEquals(30.seconds, delays[5])  // 1 * 2^5 = 32, capped at 30
    }

    @Test
    fun `aggressive config has faster backoff`() {
        val aggressive = RetryConfig.Aggressive
        val default = RetryConfig.Default

        val aggressiveDelay1 = aggressive.calculateDelay(0)
        val defaultDelay1 = default.calculateDelay(0)

        // Aggressive should start faster
        assertTrue(aggressiveDelay1 < defaultDelay1)
    }

    @Test
    fun `custom retryOnException can be provided`() {
        var called = false
        val config = RetryConfig(
            retryOnException = { exception ->
                called = true
                exception is IllegalArgumentException
            }
        )

        val result = config.retryOnException(IllegalArgumentException("test"))

        assertTrue(called)
        assertTrue(result)
    }

    @Test
    fun `custom retryOnStatusCode can be provided`() {
        var called = false
        val config = RetryConfig(
            retryOnStatusCode = { statusCode ->
                called = true
                statusCode == 418 // I'm a teapot
            }
        )

        val result = config.retryOnStatusCode(418)

        assertTrue(called)
        assertTrue(result)
    }
}
