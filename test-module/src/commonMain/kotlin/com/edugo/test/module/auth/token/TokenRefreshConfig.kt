package com.edugo.test.module.auth.token

/**
 * Configuración para el comportamiento del [TokenRefreshManager].
 *
 * Esta clase permite personalizar el comportamiento del refresh de tokens,
 * incluyendo cuándo refrescar, cuántos reintentos hacer, y si soportar
 * token rotation.
 *
 * ## Uso con Valores por Defecto
 *
 * ```kotlin
 * val manager = TokenRefreshManagerImpl(
 *     repository = authRepository,
 *     storage = storage,
 *     config = TokenRefreshConfig.DEFAULT  // Valores por defecto
 * )
 * ```
 *
 * ## Configuración Personalizada
 *
 * ```kotlin
 * val customConfig = TokenRefreshConfig(
 *     refreshThresholdSeconds = 600,  // Refrescar 10 min antes de expirar
 *     maxRetryAttempts = 5,           // Hasta 5 reintentos
 *     retryDelayMs = 2000,            // Esperar 2 segundos entre reintentos
 *     enableTokenRotation = true      // Soportar rotation si el backend lo provee
 * )
 * ```
 *
 * ## Configuración para Ambientes Diferentes
 *
 * ```kotlin
 * // Desarrollo: más agresivo con retries
 * val devConfig = TokenRefreshConfig(
 *     refreshThresholdSeconds = 60,   // 1 minuto antes
 *     maxRetryAttempts = 10,
 *     retryDelayMs = 500
 * )
 *
 * // Producción: más conservador
 * val prodConfig = TokenRefreshConfig(
 *     refreshThresholdSeconds = 300,  // 5 minutos antes
 *     maxRetryAttempts = 3,
 *     retryDelayMs = 1000
 * )
 * ```
 *
 * @property refreshThresholdSeconds Segundos antes de expiración para considerar refresh
 * @property maxRetryAttempts Máximo número de reintentos en caso de error de red
 * @property retryDelayMs Delay base para retry en millisegundos (se duplica en cada intento)
 * @property enableTokenRotation Si el backend soporta token rotation, actualizar refresh token
 */
public data class TokenRefreshConfig(
    /**
     * Segundos antes de la expiración del token para considerar que necesita refresh.
     *
     * Por ejemplo, si un token expira a las 14:00:00 y este valor es 300 (5 minutos),
     * el manager considerará que el token necesita refresh a partir de las 13:55:00.
     *
     * **Valores típicos**:
     * - 60 (1 minuto): Muy agresivo, para desarrollo
     * - 300 (5 minutos): Recomendado para producción
     * - 600 (10 minutos): Conservador, para tokens de larga duración
     *
     * **Default**: 300 segundos (5 minutos)
     */
    val refreshThresholdSeconds: Int = 300,

    /**
     * Máximo número de reintentos en caso de error de red.
     *
     * Si el refresh falla por error de red (timeout, no connection), el manager
     * reintentará hasta este número de veces antes de emitir un [RefreshFailureReason].
     *
     * **IMPORTANTE**: Solo se reintentan errores de red. Errores como token inválido
     * o expirado NO se reintentan.
     *
     * **Valores típicos**:
     * - 0: Sin reintentos
     * - 3: Recomendado para producción
     * - 5-10: Para desarrollo o redes inestables
     *
     * **Default**: 3 reintentos
     */
    val maxRetryAttempts: Int = 3,

    /**
     * Delay base para retry en millisegundos.
     *
     * Usa exponential backoff: el delay se duplica en cada intento.
     *
     * **Ejemplo con retryDelayMs = 1000**:
     * - Intento 1: Sin delay
     * - Intento 2: 1000ms (1s)
     * - Intento 3: 2000ms (2s)
     * - Intento 4: 4000ms (4s)
     *
     * **Valores típicos**:
     * - 500ms: Rápido, para desarrollo
     * - 1000ms: Recomendado para producción
     * - 2000ms: Conservador, para redes lentas
     *
     * **Default**: 1000ms (1 segundo)
     */
    val retryDelayMs: Long = 1000,

    /**
     * Si el backend soporta token rotation, actualizar el refresh token.
     *
     * **Token Rotation**: Algunos backends retornan un NUEVO refresh token
     * en la respuesta de refresh, invalidando el anterior. Esto mejora
     * la seguridad al limitar el tiempo de vida de cada refresh token.
     *
     * **Flujo con rotation habilitado**:
     * 1. Cliente envía refresh_token_A
     * 2. Servidor responde con access_token_B + refresh_token_B (nuevo)
     * 3. Cliente guarda refresh_token_B y descarta refresh_token_A
     *
     * **Flujo sin rotation**:
     * 1. Cliente envía refresh_token_A
     * 2. Servidor responde solo con access_token_B
     * 3. Cliente mantiene refresh_token_A original
     *
     * **IMPORTANTE**: Solo habilitar si el backend realmente implementa rotation.
     * Si el backend NO envía nuevo refresh token pero esto está habilitado,
     * el manager mantendrá el token original (comportamiento seguro).
     *
     * **Default**: true (soportar rotation si está disponible)
     */
    val enableTokenRotation: Boolean = true
) {

    init {
        require(refreshThresholdSeconds >= 0) {
            "refreshThresholdSeconds debe ser >= 0, fue: $refreshThresholdSeconds"
        }
        require(maxRetryAttempts >= 0) {
            "maxRetryAttempts debe ser >= 0, fue: $maxRetryAttempts"
        }
        require(retryDelayMs >= 0) {
            "retryDelayMs debe ser >= 0, fue: $retryDelayMs"
        }
    }

    /**
     * Calcula el delay para un intento específico usando exponential backoff.
     *
     * ## Ejemplo
     *
     * ```kotlin
     * val config = TokenRefreshConfig(retryDelayMs = 1000)
     *
     * config.calculateRetryDelay(0)  // 0ms (sin delay en primer intento)
     * config.calculateRetryDelay(1)  // 1000ms
     * config.calculateRetryDelay(2)  // 2000ms
     * config.calculateRetryDelay(3)  // 4000ms
     * config.calculateRetryDelay(4)  // 8000ms
     * ```
     *
     * @param attempt Número de intento (0-indexed)
     * @return Delay en millisegundos para este intento
     */
    public fun calculateRetryDelay(attempt: Int): Long {
        if (attempt <= 0) return 0
        return retryDelayMs * (1 shl (attempt - 1))  // 2^(attempt-1) * retryDelayMs
    }

    /**
     * Verifica si aún quedan reintentos disponibles.
     *
     * @param currentAttempt Número de intento actual (0-indexed)
     * @return true si se pueden hacer más intentos
     */
    public fun hasRetriesLeft(currentAttempt: Int): Boolean {
        return currentAttempt < maxRetryAttempts
    }

    public companion object {
        /**
         * Configuración por defecto recomendada para producción.
         *
         * - Refresh 5 minutos antes de expiración
         * - Hasta 3 reintentos con exponential backoff
         * - Delay base de 1 segundo
         * - Token rotation habilitado
         */
        public val DEFAULT: TokenRefreshConfig = TokenRefreshConfig()

        /**
         * Configuración agresiva para desarrollo.
         *
         * - Refresh 1 minuto antes de expiración
         * - Hasta 10 reintentos
         * - Delay base de 500ms
         * - Token rotation habilitado
         */
        public val DEVELOPMENT: TokenRefreshConfig = TokenRefreshConfig(
            refreshThresholdSeconds = 60,
            maxRetryAttempts = 10,
            retryDelayMs = 500,
            enableTokenRotation = true
        )

        /**
         * Configuración conservadora para redes inestables.
         *
         * - Refresh 10 minutos antes de expiración
         * - Hasta 5 reintentos
         * - Delay base de 2 segundos
         * - Token rotation habilitado
         */
        public val CONSERVATIVE: TokenRefreshConfig = TokenRefreshConfig(
            refreshThresholdSeconds = 600,
            maxRetryAttempts = 5,
            retryDelayMs = 2000,
            enableTokenRotation = true
        )

        /**
         * Configuración sin reintentos.
         *
         * Útil para testing o cuando los reintentos se manejan en otra capa.
         */
        public val NO_RETRY: TokenRefreshConfig = TokenRefreshConfig(
            refreshThresholdSeconds = 300,
            maxRetryAttempts = 0,
            retryDelayMs = 0,
            enableTokenRotation = true
        )
    }
}
