package com.edugo.test.module.auth.jwt

import com.edugo.test.module.auth.repository.AuthRepository
import com.edugo.test.module.auth.repository.TokenVerificationResponse
import com.edugo.test.module.core.Result
import kotlinx.datetime.Instant

/**
 * Implementación de JwtValidator que combina parsing local con validación remota.
 */
public class JwtValidatorImpl(
    private val repository: AuthRepository
) : JwtValidator {

    override suspend fun validate(token: String): JwtValidationResult {
        // 1. Validación local rápida primero (evita llamada de red innecesaria)
        val parseResult = JwtParser.parse(token)
        if (parseResult is JwtParseResult.EmptyToken) {
            return JwtValidationResult.Invalid(JwtValidationResult.InvalidReason.Malformed)
        }
        if (parseResult is JwtParseResult.InvalidFormat) {
            return JwtValidationResult.Invalid(JwtValidationResult.InvalidReason.Malformed)
        }

        // 2. Si ya expiró localmente, no gastar llamada de red
        val claims = (parseResult as JwtParseResult.Success).claims
        if (claims.isExpired) {
            return JwtValidationResult.Invalid(JwtValidationResult.InvalidReason.Expired)
        }

        // 3. Validación completa contra backend
        return try {
            val result = repository.verifyToken(token)

            when {
                result is Result.Failure -> {
                    JwtValidationResult.NetworkError(result.error)
                }
                result is Result.Success && result.data.valid -> {
                    mapToValid(result.data)
                }
                result is Result.Success && !result.data.valid -> {
                    mapToInvalid(result.data)
                }
                else -> {
                    JwtValidationResult.Invalid(
                        JwtValidationResult.InvalidReason.Other("Unexpected response")
                    )
                }
            }
        } catch (e: Exception) {
            JwtValidationResult.NetworkError(e.message ?: "Unknown error")
        }
    }

    override fun quickValidate(token: String): JwtParseResult {
        return JwtParser.parse(token)
    }

    // --- Private helpers ---

    private fun mapToValid(response: TokenVerificationResponse): JwtValidationResult.Valid {
        return JwtValidationResult.Valid(
            userId = response.userId ?: "",
            email = response.email ?: "",
            role = response.role ?: "",
            schoolId = response.schoolId,
            expiresAt = response.expiresAt?.let { parseInstant(it) } ?: Instant.DISTANT_FUTURE
        )
    }

    private fun mapToInvalid(response: TokenVerificationResponse): JwtValidationResult.Invalid {
        val reason = when {
            response.error?.contains("expired", ignoreCase = true) == true ->
                JwtValidationResult.InvalidReason.Expired
            response.error?.contains("revoked", ignoreCase = true) == true ->
                JwtValidationResult.InvalidReason.Revoked
            response.error?.contains("inactive", ignoreCase = true) == true ->
                JwtValidationResult.InvalidReason.UserInactive
            response.error?.contains("invalid", ignoreCase = true) == true ->
                JwtValidationResult.InvalidReason.Malformed
            else ->
                JwtValidationResult.InvalidReason.Other(response.error ?: "Unknown error")
        }
        return JwtValidationResult.Invalid(reason)
    }

    private fun parseInstant(isoString: String): Instant {
        return try {
            Instant.parse(isoString)
        } catch (e: Exception) {
            Instant.DISTANT_FUTURE
        }
    }
}
