package com.edugo.test.module.auth.jwt

import kotlinx.datetime.Instant

/**
 * Resultado de la validación de un token JWT.
 * Representa validación REAL (verificada por backend), no solo parsing.
 */
public sealed class JwtValidationResult {
    /**
     * Token válido - verificado por el backend.
     */
    public data class Valid(
        val userId: String,
        val email: String,
        val role: String,
        val schoolId: String?,
        val expiresAt: Instant
    ) : JwtValidationResult()

    /**
     * Token inválido con razón específica.
     */
    public data class Invalid(val reason: InvalidReason) : JwtValidationResult()

    /**
     * Error de red - no se pudo validar (usar validación local como fallback).
     */
    public data class NetworkError(val message: String) : JwtValidationResult()

    /**
     * Razones por las que un token puede ser inválido.
     */
    public sealed class InvalidReason {
        /** Token ha expirado */
        public data object Expired : InvalidReason()

        /** Token fue revocado (logout, cambio de password, etc.) */
        public data object Revoked : InvalidReason()

        /** Token tiene formato inválido o firma incorrecta */
        public data object Malformed : InvalidReason()

        /** Usuario asociado está inactivo */
        public data object UserInactive : InvalidReason()

        /** Otra razón */
        public data class Other(val message: String) : InvalidReason()
    }
}

// Extension functions
public val JwtValidationResult.isValid: Boolean
    get() = this is JwtValidationResult.Valid

public val JwtValidationResult.isInvalid: Boolean
    get() = this is JwtValidationResult.Invalid

public val JwtValidationResult.isNetworkError: Boolean
    get() = this is JwtValidationResult.NetworkError

public fun JwtValidationResult.getValidOrNull(): JwtValidationResult.Valid? =
    this as? JwtValidationResult.Valid
