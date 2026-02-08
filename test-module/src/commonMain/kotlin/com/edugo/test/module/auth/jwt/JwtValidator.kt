package com.edugo.test.module.auth.jwt

/**
 * Validador de tokens JWT.
 *
 * Combina validación local (rápida, offline) con validación remota (completa).
 *
 * ## Estrategia de Validación
 *
 * ### Para operaciones críticas (pagos, cambios de cuenta):
 * Usar `validate()` - consulta backend, verifica revocación.
 *
 * ### Para operaciones normales (mostrar UI, cache):
 * Usar `quickValidate()` - solo verifica estructura y expiración local.
 *
 * ## Ejemplo
 * ```kotlin
 * // Validación completa (requiere red)
 * when (val result = validator.validate(token)) {
 *     is JwtValidationResult.Valid -> {
 *         println("User: ${result.userId}")
 *     }
 *     is JwtValidationResult.Invalid -> {
 *         when (result.reason) {
 *             is InvalidReason.Expired -> forceLogout()
 *             is InvalidReason.Revoked -> forceLogout()
 *             else -> showError()
 *         }
 *     }
 *     is JwtValidationResult.NetworkError -> {
 *         // Fallback a validación local
 *         val local = validator.quickValidate(token)
 *     }
 * }
 * ```
 */
public interface JwtValidator {
    /**
     * Valida token contra el backend.
     * Requiere conexión de red. Verifica firma, expiración y revocación.
     *
     * @param token Token JWT a validar
     * @return Resultado de validación (Valid, Invalid, NetworkError)
     */
    public suspend fun validate(token: String): JwtValidationResult

    /**
     * Validación rápida local.
     * NO verifica firma ni revocación. Solo estructura y expiración.
     * Útil para decisiones rápidas o modo offline.
     *
     * @param token Token JWT a validar
     * @return Resultado de parsing (Success con claims, o error)
     */
    public fun quickValidate(token: String): JwtParseResult
}
