package com.edugo.test.module.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.logging.LogLevel
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
 * // Crear cliente (recomendado)
 * val client = EduGoHttpClient.create()
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
 * @param client HttpClient configurado (usar [create] o [withClient])
 * @see HttpClientFactory Para crear clientes HTTP configurados
 * @see HttpRequestConfig Para personalizar requests individuales
 */
public class EduGoHttpClient(@PublishedApi internal val client: HttpClient) {

    public companion object {
        /**
         * Crea una instancia de EduGoHttpClient con configuración por defecto.
         *
         * Usa el engine de plataforma automáticamente:
         * - **Android**: OkHttp
         * - **JVM/Desktop**: CIO
         * - **JS**: Js (Fetch API)
         *
         * ```kotlin
         * // Producción (sin logging)
         * val client = EduGoHttpClient.create()
         *
         * // Desarrollo (con logging)
         * val debugClient = EduGoHttpClient.create(logLevel = LogLevel.INFO)
         * ```
         *
         * @param logLevel Nivel de logging (default: NONE para producción)
         * @return Nueva instancia de EduGoHttpClient
         * @see HttpClientFactory.create Para más opciones de configuración
         */
        public fun create(logLevel: LogLevel = LogLevel.NONE): EduGoHttpClient {
            val client = HttpClientFactory.create(logLevel = logLevel)
            return EduGoHttpClient(client)
        }

        /**
         * Crea una instancia con HttpClient personalizado.
         *
         * Útil para testing con MockEngine o configuraciones especiales.
         *
         * ```kotlin
         * // Testing con MockEngine
         * val mockClient = HttpClient(MockEngine { request ->
         *     respond("""{"id": 1}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
         * }) { install(ContentNegotiation) { json() } }
         *
         * val client = EduGoHttpClient.withClient(mockClient)
         * ```
         *
         * @param client HttpClient configurado
         * @return Nueva instancia de EduGoHttpClient
         */
        public fun withClient(client: HttpClient): EduGoHttpClient {
            return EduGoHttpClient(client)
        }
    }

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
     * Realiza petición PUT con body serializado automáticamente.
     *
     * PUT reemplaza completamente el recurso en el servidor.
     * Usar cuando se envía el objeto completo actualizado.
     *
     * ```kotlin
     * @Serializable
     * data class User(val id: Int, val name: String, val email: String)
     *
     * val updatedUser = User(1, "John Updated", "john.updated@example.com")
     * val result: User = client.put("https://api.example.com/users/1", updatedUser)
     * ```
     *
     * @param T Tipo del objeto a enviar en el body (debe ser @Serializable)
     * @param R Tipo del objeto a deserializar de la respuesta (debe ser @Serializable)
     * @param url URL del endpoint
     * @param body Objeto completo a serializar como JSON
     * @param config Configuración opcional de headers y query params
     * @return Objeto deserializado del tipo R
     * @throws io.ktor.client.plugins.ClientRequestException Si el servidor retorna 4xx
     * @throws io.ktor.client.plugins.ServerResponseException Si el servidor retorna 5xx
     * @throws kotlinx.serialization.SerializationException Si la serialización/deserialización falla
     * @see patch Para actualizaciones parciales
     */
    public suspend inline fun <reified T, reified R> put(
        url: String,
        body: T,
        config: HttpRequestConfig = HttpRequestConfig.Default
    ): R {
        return client.put(url) {
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
     * Realiza petición PATCH con body serializado automáticamente.
     *
     * PATCH actualiza parcialmente el recurso en el servidor.
     * Usar cuando solo se envían los campos a modificar.
     *
     * ```kotlin
     * @Serializable
     * data class UserPatch(val name: String? = null, val email: String? = null)
     *
     * val patch = UserPatch(name = "New Name") // Solo actualiza el nombre
     * val result: User = client.patch("https://api.example.com/users/1", patch)
     * ```
     *
     * @param T Tipo del objeto parcial a enviar en el body (debe ser @Serializable)
     * @param R Tipo del objeto a deserializar de la respuesta (debe ser @Serializable)
     * @param url URL del endpoint
     * @param body Objeto parcial a serializar como JSON
     * @param config Configuración opcional de headers y query params
     * @return Objeto deserializado del tipo R
     * @throws io.ktor.client.plugins.ClientRequestException Si el servidor retorna 4xx
     * @throws io.ktor.client.plugins.ServerResponseException Si el servidor retorna 5xx
     * @throws kotlinx.serialization.SerializationException Si la serialización/deserialización falla
     * @see put Para reemplazos completos
     */
    public suspend inline fun <reified T, reified R> patch(
        url: String,
        body: T,
        config: HttpRequestConfig = HttpRequestConfig.Default
    ): R {
        return client.patch(url) {
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
     * Realiza petición DELETE y deserializa respuesta al tipo especificado.
     *
     * Útil cuando el servidor retorna información del recurso eliminado
     * o un objeto de confirmación.
     *
     * ```kotlin
     * @Serializable
     * data class DeleteResponse(val success: Boolean, val message: String)
     *
     * val result: DeleteResponse = client.delete("https://api.example.com/users/1")
     * ```
     *
     * @param T Tipo del objeto a deserializar (debe ser @Serializable)
     * @param url URL del recurso a eliminar
     * @param config Configuración opcional de headers y query params
     * @return Objeto deserializado del tipo T
     * @throws io.ktor.client.plugins.ClientRequestException Si el servidor retorna 4xx
     * @throws io.ktor.client.plugins.ServerResponseException Si el servidor retorna 5xx
     * @throws kotlinx.serialization.SerializationException Si la deserialización falla
     * @see deleteNoResponse Para DELETE sin body en respuesta (204 No Content)
     */
    public suspend inline fun <reified T> delete(
        url: String,
        config: HttpRequestConfig = HttpRequestConfig.Default
    ): T {
        return client.delete(url) {
            config.headers.forEach { (key, value) ->
                header(key, value)
            }
            config.queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
        }.body()
    }

    /**
     * Realiza petición DELETE sin esperar body en respuesta.
     *
     * Útil para endpoints que retornan 204 No Content o 200 OK sin body.
     * Es el caso más común para operaciones DELETE.
     *
     * ```kotlin
     * // Eliminar usuario sin esperar respuesta
     * client.deleteNoResponse("https://api.example.com/users/1")
     *
     * // Con headers de autorización
     * val config = HttpRequestConfig.builder()
     *     .header("Authorization", "Bearer token")
     *     .build()
     * client.deleteNoResponse("https://api.example.com/users/1", config)
     * ```
     *
     * @param url URL del recurso a eliminar
     * @param config Configuración opcional de headers y query params
     * @throws io.ktor.client.plugins.ClientRequestException Si el servidor retorna 4xx
     * @throws io.ktor.client.plugins.ServerResponseException Si el servidor retorna 5xx
     * @see delete Para DELETE con body en respuesta
     */
    public suspend fun deleteNoResponse(
        url: String,
        config: HttpRequestConfig = HttpRequestConfig.Default
    ) {
        client.delete(url) {
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
