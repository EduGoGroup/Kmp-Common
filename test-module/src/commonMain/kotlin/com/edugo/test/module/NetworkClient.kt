package com.edugo.test.module

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(val message: String)

/**
 * Cliente de red multiplataforma usando Ktor Client.
 *
 * Este cliente demuestra el uso de:
 * - Ktor Client con engines específicos por plataforma
 * - Kotlinx Serialization para JSON
 * - Kotlinx Coroutines con Dispatchers
 */
class NetworkClient(private val client: HttpClient) {
    /**
     * Obtiene datos de una URL y retorna el contenido como String.
     *
     * Usa Dispatchers.Default para operaciones de red que no bloquean la UI.
     */
    suspend fun fetchData(url: String): String = withContext(Dispatchers.Default) {
        client.get(url).bodyAsText()
    }
}
