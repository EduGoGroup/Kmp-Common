package com.edugo.test.module.auth.jwt

/**
 * Resultado del parsing de un token JWT.
 */
public sealed class JwtParseResult {
    /**
     * Parsing exitoso - claims extraídos correctamente.
     * NOTA: No significa que el token sea válido, solo que se pudo parsear.
     */
    public data class Success(val claims: JwtClaims) : JwtParseResult()

    /**
     * Token tiene formato inválido (no es JWT válido).
     */
    public data class InvalidFormat(val reason: String) : JwtParseResult()

    /**
     * Token vacío o null.
     */
    public data object EmptyToken : JwtParseResult()
}

// Extension functions
public val JwtParseResult.isSuccess: Boolean
    get() = this is JwtParseResult.Success

public val JwtParseResult.claimsOrNull: JwtClaims?
    get() = (this as? JwtParseResult.Success)?.claims

public fun JwtParseResult.getClaimsOrThrow(): JwtClaims = when (this) {
    is JwtParseResult.Success -> claims
    is JwtParseResult.InvalidFormat -> throw IllegalArgumentException("Invalid JWT: $reason")
    is JwtParseResult.EmptyToken -> throw IllegalArgumentException("Empty token")
}
