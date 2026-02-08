package com.edugo.test.module.auth.jwt

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Claims extraídos de un token JWT.
 *
 * IMPORTANTE: Este modelo solo representa datos parseados del token.
 * NO garantiza que el token sea válido o no haya sido manipulado.
 * Para validación real, usar JwtValidator.
 *
 * ## Claims Estándar JWT (RFC 7519)
 * - sub (subject): Identificador del usuario
 * - iss (issuer): Emisor del token
 * - aud (audience): Audiencia destinataria
 * - exp (expiration): Tiempo de expiración
 * - iat (issued at): Tiempo de emisión
 * - nbf (not before): Token no válido antes de este tiempo
 * - jti (JWT ID): Identificador único del token
 *
 * ## Ejemplo de Uso
 * ```kotlin
 * val result = JwtParser.parse(token)
 * if (result is JwtParseResult.Success) {
 *     val claims = result.claims
 *     println("User: ${claims.userId}")
 *     println("Expired: ${claims.isExpired}")
 *     println("Role: ${claims.role}")
 * }
 * ```
 */
@Serializable
public data class JwtClaims(
    /** Subject - típicamente el user_id */
    val subject: String? = null,

    /** Issuer - emisor del token (ej: "edugo-api") */
    val issuer: String? = null,

    /** Audience - audiencia destinataria */
    val audience: String? = null,

    /** Expiration time */
    val expiresAt: Instant? = null,

    /** Issued at time */
    val issuedAt: Instant? = null,

    /** Not before time */
    val notBefore: Instant? = null,

    /** JWT ID - identificador único */
    val jwtId: String? = null,

    /** Claims personalizados (role, school_id, etc.) */
    val customClaims: Map<String, String> = emptyMap()
) {
    /**
     * Verifica si el token ha expirado basado en el claim exp.
     * Retorna false si no hay claim exp (asume no expira).
     */
    public val isExpired: Boolean
        get() = expiresAt?.let { Clock.System.now() >= it } ?: false

    /**
     * Verifica si el token aún no es válido (nbf > now).
     */
    public val isNotYetValid: Boolean
        get() = notBefore?.let { Clock.System.now() < it } ?: false

    /**
     * Verifica si el token está en su ventana de validez temporal.
     */
    public val isTemporallyValid: Boolean
        get() = !isExpired && !isNotYetValid

    // Convenience accessors para claims comunes de EduGo
    public val userId: String? get() = subject
    public val role: String? get() = customClaims["role"]
    public val schoolId: String? get() = customClaims["school_id"]
    public val email: String? get() = customClaims["email"]
}
