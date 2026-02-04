package com.edugo.test.module.network.interceptor

import io.ktor.client.request.*
import io.ktor.client.statement.*

/**
 * Interceptor para requests/responses HTTP.
 * Permite modificar requests antes de enviar y observar responses después de recibir.
 */
interface Interceptor {
    /**
     * Orden de ejecución (menor = primero).
     */
    val order: Int get() = 0

    /**
     * Intercepta el request antes de enviarlo.
     * Modifica el HttpRequestBuilder in-place.
     *
     * @param request Builder del request que se puede modificar directamente
     */
    suspend fun interceptRequest(request: HttpRequestBuilder) {}

    /**
     * Intercepta el response después de recibirlo.
     * HttpResponse es inmutable en Ktor, este método es solo para observación
     * (logging, métricas, etc).
     *
     * @param response Response recibido (solo lectura)
     */
    suspend fun interceptResponse(response: HttpResponse) {}

    /**
     * Llamado cuando ocurre un error durante la ejecución del request.
     *
     * @param request Builder del request que causó el error
     * @param exception Excepción capturada
     */
    suspend fun onError(request: HttpRequestBuilder, exception: Throwable) {}
}

/**
 * Cadena de interceptores que se ejecutan en orden.
 *
 * Los interceptores se ordenan por su propiedad `order` (menor = primero).
 * Para responses, se ejecutan en orden inverso.
 *
 * Ejemplo:
 * ```kotlin
 * val chain = InterceptorChain(listOf(
 *     HeadersInterceptor.jsonDefaults(),  // order = 10
 *     AuthInterceptor(tokenProvider)      // order = 20
 * ))
 *
 * // En request: Headers primero, luego Auth
 * // En response: Auth primero, luego Headers (inverso)
 * ```
 */
class InterceptorChain(
    private val interceptors: List<Interceptor> = emptyList()
) {
    private val sortedInterceptors = interceptors.sortedBy { it.order }

    /**
     * Ejecuta todos los interceptores de request en orden.
     * Modifica el HttpRequestBuilder in-place.
     *
     * @param request Builder del request que será modificado
     */
    suspend fun processRequest(request: HttpRequestBuilder) {
        for (interceptor in sortedInterceptors) {
            interceptor.interceptRequest(request)
        }
    }

    /**
     * Ejecuta todos los interceptores de response en orden inverso.
     * HttpResponse es inmutable, solo para observación.
     *
     * @param response Response recibido (solo lectura)
     */
    suspend fun processResponse(response: HttpResponse) {
        for (interceptor in sortedInterceptors.reversed()) {
            interceptor.interceptResponse(response)
        }
    }

    /**
     * Notifica error a todos los interceptores.
     *
     * @param request Builder del request que causó el error
     * @param exception Excepción capturada
     */
    suspend fun notifyError(request: HttpRequestBuilder, exception: Throwable) {
        for (interceptor in sortedInterceptors) {
            interceptor.onError(request, exception)
        }
    }

    /**
     * Crea nueva cadena agregando un interceptor.
     *
     * @param interceptor Interceptor a agregar
     * @return Nueva cadena con el interceptor agregado
     */
    fun plus(interceptor: Interceptor): InterceptorChain {
        return InterceptorChain(interceptors + interceptor)
    }

    companion object {
        /** Cadena vacía sin interceptores */
        val Empty = InterceptorChain()
    }
}
