package com.edugo.test.module.auth.jwt

import kotlinx.datetime.Instant
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Parser de tokens JWT que extrae claims sin validar firma.
 *
 * ## Propósito
 * Lectura rápida de datos del token para uso local/offline.
 * Funciona igual en todas las plataformas (Android, iOS, Desktop, Web).
 *
 * ## Limitaciones
 * - NO valida la firma del token
 * - NO verifica si el token fue revocado
 * - Solo decodifica Base64 y parsea JSON
 *
 * ## Para Validación Real
 * Usar `JwtValidator.validate()` que consulta el backend.
 *
 * ## Estructura JWT
 * Un JWT tiene 3 partes separadas por punto:
 * - Header: {"alg": "HS256", "typ": "JWT"}
 * - Payload: {"sub": "user-123", "exp": 1234567890, ...}
 * - Signature: HMACSHA256(base64(header) + "." + base64(payload), secret)
 *
 * Este parser solo decodifica el Payload.
 */
public object JwtParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Decodifica el payload de un JWT y extrae los claims.
     * NO valida la firma - solo parsea.
     *
     * @param token Token JWT completo (header.payload.signature)
     * @return JwtParseResult con claims o error
     */
    @OptIn(ExperimentalEncodingApi::class)
    public fun parse(token: String): JwtParseResult {
        // Validar token no vacío
        if (token.isBlank()) {
            return JwtParseResult.EmptyToken
        }

        // Validar estructura básica (3 partes)
        val parts = token.split(".")
        if (parts.size != 3) {
            return JwtParseResult.InvalidFormat(
                "JWT must have 3 parts separated by dots, found ${parts.size}"
            )
        }

        val payloadBase64 = parts[1]

        // Decodificar Base64 (URL-safe)
        val payloadJson = try {
            val decoded = Base64.UrlSafe.decode(padBase64(payloadBase64))
            decoded.decodeToString()
        } catch (e: Exception) {
            return JwtParseResult.InvalidFormat("Failed to decode Base64: ${e.message}")
        }

        // Parsear JSON
        val jsonObject = try {
            json.parseToJsonElement(payloadJson).jsonObject
        } catch (e: Exception) {
            return JwtParseResult.InvalidFormat("Failed to parse JSON: ${e.message}")
        }

        // Extraer claims
        val claims = extractClaims(jsonObject)
        return JwtParseResult.Success(claims)
    }

    /**
     * Extrae claims asumiendo token válido. Lanza excepción si falla.
     */
    public fun parseOrThrow(token: String): JwtClaims {
        return parse(token).getClaimsOrThrow()
    }

    /**
     * Verifica estructura básica del JWT (3 partes separadas por punto).
     */
    public fun isValidStructure(token: String): Boolean {
        return token.isNotBlank() && token.split(".").size == 3
    }

    // --- Private helpers ---

    private fun extractClaims(json: JsonObject): JwtClaims {
        val standardKeys = setOf("sub", "iss", "aud", "exp", "iat", "nbf", "jti")

        // Extraer claims custom (todo lo que no es estándar)
        val customClaims = json.entries
            .filter { it.key !in standardKeys }
            .mapNotNull { (key, value) ->
                val stringValue = when (value) {
                    is JsonPrimitive -> value.contentOrNull
                    else -> value.toString()
                }
                stringValue?.let { key to it }
            }
            .toMap()

        return JwtClaims(
            subject = json["sub"]?.jsonPrimitive?.contentOrNull,
            issuer = json["iss"]?.jsonPrimitive?.contentOrNull,
            audience = json["aud"]?.jsonPrimitive?.contentOrNull,
            expiresAt = json["exp"]?.jsonPrimitive?.longOrNull?.let {
                Instant.fromEpochSeconds(it)
            },
            issuedAt = json["iat"]?.jsonPrimitive?.longOrNull?.let {
                Instant.fromEpochSeconds(it)
            },
            notBefore = json["nbf"]?.jsonPrimitive?.longOrNull?.let {
                Instant.fromEpochSeconds(it)
            },
            jwtId = json["jti"]?.jsonPrimitive?.contentOrNull,
            customClaims = customClaims
        )
    }

    /**
     * Base64 URL-safe puede omitir padding. Agregarlo si falta.
     */
    private fun padBase64(input: String): String {
        return when (input.length % 4) {
            2 -> "$input=="
            3 -> "$input="
            else -> input
        }
    }
}
