package com.edugo.test.module.network

import com.edugo.test.module.platform.DefaultLogger
import com.edugo.test.module.platform.TaggedLogger
import com.edugo.test.module.platform.withTag
import io.ktor.http.*
import kotlinx.datetime.Clock

/**
 * Logger especializado para tráfico HTTP.
 * Integra con el sistema de logging de EduGo y sanitiza datos sensibles.
 */
public class NetworkLogger(
    private val logger: TaggedLogger = DefaultLogger.withTag(DEFAULT_TAG),
    private val sanitizer: LogSanitizer = LogSanitizer
) : io.ktor.client.plugins.logging.Logger {

    private var requestStartTime: Long = 0

    override fun log(message: String) {
        // Sanitizar el mensaje antes de loguear
        val sanitizedMessage = sanitizer.sanitizeBody(message)
        logger.d(sanitizedMessage)
    }

    /**
     * Loguea request HTTP con información estructurada.
     */
    public fun logRequest(
        method: HttpMethod,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ) {
        requestStartTime = Clock.System.now().toEpochMilliseconds()

        val sanitizedUrl = sanitizer.sanitizeUrl(url)
        val sanitizedHeaders = sanitizer.sanitizeHeaders(headers)

        logger.i("--> ${method.value} $sanitizedUrl")

        if (sanitizedHeaders.isNotEmpty()) {
            logger.d("  Headers: $sanitizedHeaders")
        }

        body?.let {
            val sanitizedBody = sanitizer.sanitizeBody(it)
            logger.d("  Body: ${sanitizedBody.take(MAX_BODY_LOG_SIZE)}")
        }
    }

    /**
     * Loguea response HTTP con tiempo de respuesta.
     */
    public fun logResponse(
        statusCode: Int,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ) {
        val elapsed = Clock.System.now().toEpochMilliseconds() - requestStartTime
        val sanitizedUrl = sanitizer.sanitizeUrl(url)

        val statusIndicator = when {
            statusCode in 200..299 -> "[OK]"
            statusCode in 400..499 -> "[CLIENT_ERROR]"
            statusCode in 500..599 -> "[SERVER_ERROR]"
            else -> "[?]"
        }

        logger.i("<-- $statusIndicator $statusCode $sanitizedUrl (${elapsed}ms)")

        body?.let {
            val sanitizedBody = sanitizer.sanitizeBody(it)
            logger.d("  Response: ${sanitizedBody.take(MAX_BODY_LOG_SIZE)}")
        }
    }

    /**
     * Loguea error de red.
     */
    public fun logError(url: String, exception: Throwable) {
        val sanitizedUrl = sanitizer.sanitizeUrl(url)
        logger.e("[ERROR] $sanitizedUrl: ${exception.message}", exception)
    }

    public companion object {
        private const val DEFAULT_TAG = "EduGo.Network"
        private const val MAX_BODY_LOG_SIZE = 1000

        /** Instancia por defecto */
        public val Default: NetworkLogger = NetworkLogger()
    }
}
