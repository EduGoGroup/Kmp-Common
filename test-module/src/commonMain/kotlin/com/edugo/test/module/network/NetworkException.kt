package com.edugo.test.module.network

import com.edugo.test.module.core.AppError
import com.edugo.test.module.core.ErrorCode

/**
 * Jerarquía de excepciones de red que mapean directamente a ErrorCode.
 * Facilita el manejo estructurado de errores HTTP y de conectividad.
 */
public sealed class NetworkException(
    message: String,
    public val errorCode: ErrorCode,
    cause: Throwable? = null
) : Exception(message, cause) {

    /** Error de timeout en la conexión o request */
    public class Timeout(
        message: String = "Request timed out",
        cause: Throwable? = null
    ) : NetworkException(message, ErrorCode.NETWORK_TIMEOUT, cause)

    /** Sin conexión a internet */
    public class NoConnection(
        message: String = "No network connection",
        cause: Throwable? = null
    ) : NetworkException(message, ErrorCode.NETWORK_NO_CONNECTION, cause)

    /** Error SSL/TLS */
    public class SslError(
        message: String = "SSL certificate error",
        cause: Throwable? = null
    ) : NetworkException(message, ErrorCode.NETWORK_SSL_ERROR, cause)

    /** Error de DNS */
    public class DnsFailure(
        message: String = "DNS resolution failed",
        cause: Throwable? = null
    ) : NetworkException(message, ErrorCode.NETWORK_DNS_FAILURE, cause)

    /** Conexión reseteada por el servidor */
    public class ConnectionReset(
        message: String = "Connection reset by server",
        cause: Throwable? = null
    ) : NetworkException(message, ErrorCode.NETWORK_CONNECTION_RESET, cause)

    /** Error HTTP del cliente (4xx) */
    public class ClientError(
        public val statusCode: Int,
        message: String,
        cause: Throwable? = null
    ) : NetworkException(message, ErrorCode.fromHttpStatus(statusCode), cause)

    /** Error HTTP del servidor (5xx) */
    public class ServerError(
        public val statusCode: Int,
        message: String,
        cause: Throwable? = null
    ) : NetworkException(message, ErrorCode.fromHttpStatus(statusCode), cause)

    /**
     * Convierte esta excepción a AppError.
     */
    public fun toAppError(): AppError = AppError(
        code = errorCode,
        message = message ?: errorCode.description,
        cause = cause
    )

    public companion object {
        /**
         * Crea NetworkException desde código HTTP.
         */
        public fun fromHttpStatus(statusCode: Int, message: String? = null): NetworkException {
            val msg = message ?: "HTTP $statusCode"
            return when (statusCode) {
                in 400..499 -> ClientError(statusCode, msg)
                in 500..599 -> ServerError(statusCode, msg)
                else -> ServerError(statusCode, msg)
            }
        }
    }
}
