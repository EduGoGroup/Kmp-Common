package com.edugo.test.module.network

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Factory para crear instancias de HttpClient configuradas.
 */
object HttpClientFactory {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    /**
     * Crea HttpClient con plugins base (sin engine).
     * El engine se proporciona en cada plataforma.
     */
    fun createBaseClient(engine: io.ktor.client.engine.HttpClientEngine): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }
}
