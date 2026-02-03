package com.edugo.test.module.config

import kotlinx.serialization.json.Json

/**
 * Configuraciones predefinidas de Json para diferentes casos de uso en serialización de dominio.
 *
 * Este objeto proporciona configuraciones Json reutilizables optimizadas para diferentes
 * escenarios de serialización de datos de dominio, DTOs y modelos. Estas configuraciones
 * son independientes de la configuración HTTP definida en `HttpClientFactory`.
 *
 * ## Diferencia con HttpClientFactory
 *
 * - **JsonConfig**: Para serialización de datos de dominio, DTOs, persistencia, cache, etc.
 * - **HttpClientFactory.json**: Para serialización HTTP específica del cliente Ktor
 *
 * No mezclar ambas configuraciones. Cada una está optimizada para su propósito específico.
 *
 * ## Uso
 *
 * ```kotlin
 * @Serializable
 * data class User(val id: String, val name: String)
 *
 * // Usar configuración por defecto
 * val json = JsonConfig.Default.encodeToString(User.serializer(), user)
 *
 * // Usar configuración pretty para debugging
 * val prettyJson = JsonConfig.Pretty.encodeToString(User.serializer(), user)
 * println(prettyJson) // JSON formateado con indentación
 *
 * // Usar configuración estricta para validación
 * try {
 *     val validated = JsonConfig.Strict.decodeFromString<User>(jsonString)
 * } catch (e: SerializationException) {
 *     // Falla si hay claves desconocidas
 * }
 * ```
 */
public object JsonConfig {
    /**
     * Configuración por defecto balanceada para uso general.
     *
     * Esta configuración es la recomendada para la mayoría de casos de uso:
     * - Serialización de DTOs
     * - Persistencia de datos
     * - Cache de objetos
     * - Intercambio de datos entre capas
     *
     * ## Características
     *
     * - `ignoreUnknownKeys = true`: Tolera campos extra en JSON (flexibilidad)
     * - `isLenient = false`: Requiere JSON válido (seguridad)
     * - `prettyPrint = false`: Compacto para producción (eficiencia)
     * - `encodeDefaults = true`: Incluye valores por defecto (completitud)
     *
     * ## Ejemplo
     *
     * ```kotlin
     * val user = User(id = "123", name = "John")
     * val json = JsonConfig.Default.encodeToString(user)
     * // {"id":"123","name":"John"}
     * ```
     *
     * Inicializada lazy para optimizar memoria y startup time.
     */
    public val Default: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = false
            prettyPrint = false
            encodeDefaults = true
        }
    }

    /**
     * Configuración con formato legible (pretty print) para debugging y logs.
     *
     * Recomendada para:
     * - Logs de desarrollo
     * - Debugging de datos
     * - Archivos de configuración leídos por humanos
     * - Ejemplos en documentación
     *
     * ## Características
     *
     * - `ignoreUnknownKeys = true`: Tolera campos extra
     * - `isLenient = false`: Requiere JSON válido
     * - `prettyPrint = true`: Formato con indentación legible
     * - `encodeDefaults = true`: Incluye valores por defecto
     *
     * ## Ejemplo
     *
     * ```kotlin
     * val user = User(id = "123", name = "John")
     * val json = JsonConfig.Pretty.encodeToString(user)
     * println(json)
     * // {
     * //   "id": "123",
     * //   "name": "John"
     * // }
     * ```
     *
     * **⚠️ Advertencia**: No usar en producción por el tamaño adicional del JSON.
     *
     * Inicializada lazy - solo se crea si se usa.
     */
    public val Pretty: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = false
            prettyPrint = true
            encodeDefaults = true
        }
    }

    /**
     * Configuración estricta para validación rigurosa.
     *
     * Falla si encuentra claves desconocidas en el JSON. Útil para:
     * - Validación de contratos de API
     * - Detección de errores de integración
     * - Migraciones de esquema
     * - Testing de compatibilidad
     *
     * ## Características
     *
     * - `ignoreUnknownKeys = false`: Falla con claves extra (validación estricta)
     * - `isLenient = false`: Requiere JSON válido
     * - `prettyPrint = false`: Compacto
     * - `encodeDefaults = true`: Incluye valores por defecto
     *
     * ## Ejemplo
     *
     * ```kotlin
     * val validJson = """{"id":"123","name":"John"}"""
     * val invalidJson = """{"id":"123","name":"John","extra":"field"}"""
     *
     * val user1 = JsonConfig.Strict.decodeFromString<User>(validJson)
     * // ✓ Éxito
     *
     * try {
     *     val user2 = JsonConfig.Strict.decodeFromString<User>(invalidJson)
     * } catch (e: SerializationException) {
     *     // ✗ Falla: campo "extra" no esperado
     * }
     * ```
     *
     * **⚠️ Advertencia**: Usar solo en testing o validación explícita.
     * No recomendado para producción por baja tolerancia a cambios.
     *
     * Inicializada lazy - solo se crea si se usa.
     */
    public val Strict: Json by lazy {
        Json {
            ignoreUnknownKeys = false
            isLenient = false
            prettyPrint = false
            encodeDefaults = true
        }
    }

    /**
     * Configuración permisiva para datos externos de terceros.
     *
     * Tolera JSON no estándar y claves desconocidas. Útil para:
     * - Consumo de APIs externas con formato relajado
     * - Datos de terceros no controlados
     * - Migración de sistemas legacy
     * - JSON generado manualmente (sin garantías de formato)
     *
     * ## Características
     *
     * - `ignoreUnknownKeys = true`: Tolera campos extra
     * - `isLenient = true`: Acepta JSON no estándar (trailing commas, comentarios, strings sin comillas para valores especiales)
     * - `prettyPrint = false`: Compacto
     * - `encodeDefaults = false`: No incluye valores por defecto (compatibilidad)
     *
     * ## Ejemplo
     *
     * ```kotlin
     * // JSON con formato no estándar (trailing comma)
     * val relaxedJson = """{"id":"123","name":"John","age":30,}"""
     *
     * val user = JsonConfig.Lenient.decodeFromString<User>(relaxedJson)
     * // ✓ Éxito - tolera JSON no estándar
     * ```
     *
     * **⚠️ Advertencia**: Solo usar para datos externos no controlados.
     * No recomendado para serialización interna por seguridad.
     *
     * Inicializada lazy - solo se crea si se usa.
     */
    public val Lenient: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
            encodeDefaults = false
        }
    }
}
