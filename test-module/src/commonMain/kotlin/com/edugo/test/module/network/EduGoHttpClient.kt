package com.edugo.test.module.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Cliente HTTP type-safe para EduGo.
 *
 * Encapsula operaciones HTTP con serialización/deserialización automática
 * usando kotlinx.serialization. Proporciona una API fluida y type-safe
 * sobre Ktor Client.
 *
 * ## Uso básico
 *
 * ```kotlin
 * // Crear cliente con factory (recomendado)
 * val client = EduGoHttpClient(HttpClientFactory.create())
 *
 * // GET simple
 * val user: User = client.get("https://api.example.com/users/1")
 *
 * // GET con headers y query params
 * val config = HttpRequestConfig.builder()
 *     .header("Authorization", "Bearer token")
 *     .queryParam("page", "1")
 *     .build()
 * val users: List<User> = client.get("https://api.example.com/users", config)
 *
 * // Cerrar cuando ya no se necesite
 * client.close()
 * ```
 *
 * @param client HttpClient configurado (usar [HttpClientFactory.create])
 * @see HttpClientFactory Para crear clientes HTTP configurados
 * @see HttpRequestConfig Para personalizar requests individuales
 */
public class EduGoHttpClient(private val client: HttpClient) {

    /**
     * Realiza petición GET y deserializa respuesta al tipo especificado.
     *
     * La deserialización se realiza automáticamente usando kotlinx.serialization.
     * El tipo T debe ser serializable (@Serializable).
     *
     * ```kotlin
     * @Serializable
     * data class User(val id: Int, val name: String)
     *
     * val user: User = client.get("https://api.example.com/users/1")
     * ```
     *
     * @param T Tipo del objeto a deserializar (debe ser @Serializable)
     * @param url URL del endpoint
     * @param config Configuración opcional de headers y query params
     * @return Objeto deserializado del tipo T
     * @throws io.ktor.client.plugins.ClientRequestException Si el servidor retorna 4xx
     * @throws io.ktor.client.plugins.ServerResponseException Si el servidor retorna 5xx
     * @throws kotlinx.serialization.SerializationException Si la deserialización falla
     */
    public suspend inline fun <reified T> get(
        url: String,
        config: HttpRequestConfig = HttpRequestConfig.Default
    ): T {
        return client.get(url) {
            config.headers.forEach { (key, value) ->
                header(key, value)
            }
            config.queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
        }.body()
    }

    /**
     * Realiza petición POST con body serializado automáticamente.
     *
     * El body se serializa a JSON automáticamente usando kotlinx.serialization.
     * Tanto el tipo de request (T) como el de response (R) deben ser @Serializable.
     *
     * ```kotlin
     * @Serializable
     * data class CreateUserRequest(val name: String, val email: String)
     *
     * @Serializable
     * data class UserResponse(val id: Int, val name: String, val email: String)
     *
     * val request = CreateUserRequest("John", "john@example.com")
     * val user: UserResponse = client.post("https://api.example.com/users", request)
     * ```
     *
     * @param T Tipo del objeto a enviar en el body (debe ser @Serializable)
     * @param R Tipo del objeto a deserializar de la respuesta (debe ser @Serializable)
     * @param url URL del endpoint
     * @param body Objeto a serializar como JSON en el body
     * @param config Configuración opcional de headers y query params
     * @return Objeto deserializado del tipo R
     * @throws io.ktor.client.plugins.ClientRequestException Si el servidor retorna 4xx
     * @throws io.ktor.client.plugins.ServerResponseException Si el servidor retorna 5xx
     * @throws kotlinx.serialization.SerializationException Si la serialización/deserialización falla
     */
    public suspend inline fun <reified T, reified R> post(
        url: String,
        body: T,
        config: HttpRequestConfig = HttpRequestConfig.Default
    ): R {
        return client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
            config.headers.forEach { (key, value) ->
                header(key, value)
            }
            config.queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
        }.body()
    }

    /**
     * Realiza petición POST sin esperar body en respuesta.
     *
     * Útil para endpoints que retornan 201 Created o 204 No Content sin body.
     *
     * ```kotlin
     * @Serializable
     * data class LogEvent(val action: String, val timestamp: Long)
     *
     * val event = LogEvent("login", System.currentTimeMillis())
     * client.postNoResponse("https://api.example.com/events", event)
     * ```
     *
     * @param T Tipo del objeto a enviar en el body (debe ser @Serializable)
     * @param url URL del endpoint
     * @param body Objeto a serializar como JSON en el body
     * @param config Configuración opcional de headers y query params
     * @throws io.ktor.client.plugins.ClientRequestException Si el servidor retorna 4xx
     * @throws io.ktor.client.plugins.ServerResponseException Si el servidor retorna 5xx
     * @throws kotlinx.serialization.SerializationException Si la serialización falla
     */
    public suspend inline fun <reified T> postNoResponse(
        url: String,
        body: T,
        config: HttpRequestConfig = HttpRequestConfig.Default
    ) {
        client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
            config.headers.forEach { (key, value) ->
                header(key, value)
            }
            config.queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
        }
    }

    /**
     * Cierra el cliente HTTP y libera recursos.
     *
     * Debe llamarse cuando el cliente ya no sea necesario para evitar
     * memory leaks. Después de cerrar, el cliente no puede ser reutilizado.
     *
     * ```kotlin
     * val client = EduGoHttpClient(HttpClientFactory.create())
     * try {
     *     val data = client.get<Data>(url)
     * } finally {
     *     client.close()
     * }
     * ```
     */
    public fun close() {
        client.close()
    }
}
