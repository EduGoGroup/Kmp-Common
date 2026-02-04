package com.edugo.test.module.network

import io.ktor.client.plugins.*

/**
 * Mapea excepciones de Ktor y sistema a NetworkException tipadas.
 */
public object ExceptionMapper {

    /**
     * Convierte cualquier excepción a NetworkException apropiada.
     */
    public fun map(exception: Throwable): NetworkException {
        return when (exception) {
            // Ya es NetworkException
            is NetworkException -> exception

            // Timeout de Ktor
            is HttpRequestTimeoutException ->
                NetworkException.Timeout("Request timeout: ${exception.message}", exception)

            // SSL/TLS y otros errores - detectar por tipo o mensaje
            else -> mapByMessage(exception)
        }
    }

    /**
     * Mapea excepciones basándose en el mensaje cuando no hay tipo específico.
     */
    private fun mapByMessage(exception: Throwable): NetworkException {
        val message = exception.message?.lowercase() ?: ""
        val className = exception::class.simpleName?.lowercase() ?: ""

        return when {
            // SSL/TLS errors
            message.contains("ssl") || message.contains("certificate") ||
            className.contains("ssl") ->
                NetworkException.SslError(exception.message ?: "SSL error", exception)

            // DNS errors
            message.contains("dns") || message.contains("unknown host") ||
            message.contains("unable to resolve") || message.contains("unresolved") ||
            className.contains("unresolvedaddress") ->
                NetworkException.DnsFailure(exception.message ?: "DNS failure", exception)

            // Timeout errors
            message.contains("timeout") || message.contains("timed out") ||
            className.contains("timeout") ->
                NetworkException.Timeout(exception.message ?: "Timeout", exception)

            // Connection reset/closed
            message.contains("connection") &&
            (message.contains("refused") || message.contains("reset") || message.contains("closed")) ->
                NetworkException.ConnectionReset(exception.message ?: "Connection reset", exception)

            // No connection / network unreachable
            message.contains("no route") || message.contains("network is unreachable") ||
            message.contains("no address") || message.contains("connect") && message.contains("fail") ||
            className.contains("connectexception") || className.contains("noroute") ->
                NetworkException.NoConnection(exception.message ?: "No connection", exception)

            // Socket errors generalmente indican problemas de conexión
            className.contains("socket") && (message.contains("closed") || message.contains("reset")) ->
                NetworkException.ConnectionReset(exception.message ?: "Socket error", exception)

            // Fallback a ServerError para errores desconocidos
            else -> NetworkException.ServerError(
                statusCode = 500,
                message = exception.message ?: "Unknown network error",
                cause = exception
            )
        }
    }

    /**
     * Ejecuta bloque y mapea cualquier excepción a NetworkException.
     */
    public inline fun <T> runCatching(block: () -> T): kotlin.Result<T> {
        return try {
            kotlin.Result.success(block())
        } catch (e: Throwable) {
            kotlin.Result.failure(map(e))
        }
    }
}
