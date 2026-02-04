package com.edugo.test.module.network.retry

import com.edugo.test.module.network.NetworkException
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Configuración para retry automático de requests.
 *
 * @param maxRetries Número máximo de reintentos (default: 3)
 * @param initialDelay Delay inicial antes del primer retry (default: 1s)
 * @param maxDelay Delay máximo entre retries (default: 30s)
 * @param multiplier Factor de multiplicación para backoff (default: 2.0)
 * @param jitterFactor Factor de jitter aleatorio 0.0-1.0 (default: 0.1)
 * @param retryOnException Función que determina si reintentar en excepción
 * @param retryOnStatusCode Función que determina si reintentar en status code
 */
data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelay: Duration = 1.seconds,
    val maxDelay: Duration = 30.seconds,
    val multiplier: Double = 2.0,
    val jitterFactor: Double = 0.1,
    val retryOnException: (Throwable) -> Boolean = { isRetryableException(it) },
    val retryOnStatusCode: (Int) -> Boolean = { isRetryableStatusCode(it) }
) {
    init {
        require(maxRetries >= 0) { "maxRetries must be >= 0" }
        require(multiplier >= 1.0) { "multiplier must be >= 1.0" }
        require(jitterFactor in 0.0..1.0) { "jitterFactor must be in 0.0..1.0" }
    }

    /**
     * Calcula el delay para un intento específico con backoff exponencial y jitter.
     */
    fun calculateDelay(attempt: Int): Duration {
        val exponentialDelay = initialDelay * multiplier.pow(attempt)
        val cappedDelay = minOf(exponentialDelay, maxDelay)

        // Aplicar jitter: +/- jitterFactor%
        val jitter = cappedDelay.inWholeMilliseconds * jitterFactor * (Random.nextDouble() * 2 - 1)
        val finalDelayMs = (cappedDelay.inWholeMilliseconds + jitter).toLong().coerceAtLeast(0)

        return finalDelayMs.milliseconds
    }

    companion object {
        /** Configuración por defecto */
        val Default = RetryConfig()

        /** Sin retry */
        val NoRetry = RetryConfig(maxRetries = 0)

        /** Retry agresivo para operaciones críticas */
        val Aggressive = RetryConfig(
            maxRetries = 5,
            initialDelay = 500.milliseconds,
            maxDelay = 60.seconds
        )

        /**
         * Determina si una excepción es retriable.
         */
        fun isRetryableException(exception: Throwable): Boolean {
            return when (exception) {
                is NetworkException.Timeout -> true
                is NetworkException.NoConnection -> true
                is NetworkException.ConnectionReset -> true
                is NetworkException.DnsFailure -> true
                is NetworkException.ServerError -> exception.statusCode in 500..599
                else -> false
            }
        }

        /**
         * Determina si un status code es retriable.
         * 5xx son retriables, 429 (Too Many Requests) también.
         */
        fun isRetryableStatusCode(statusCode: Int): Boolean {
            return statusCode in 500..599 || statusCode == 429
        }
    }
}

// Extension para pow con Double
private fun Duration.times(factor: Double): Duration =
    (this.inWholeMilliseconds * factor).toLong().milliseconds

private fun Double.pow(n: Int): Double {
    var result = 1.0
    repeat(n) { result *= this }
    return result
}
