package com.edugo.test.module.auth.model

/**
 * Resultado del proceso de logout.
 *
 * Distingue entre logout completo, parcial (offline) e idempotente.
 *
 * ## Casos de uso
 *
 * - **Success**: Logout exitoso tanto local como remoto
 * - **PartialSuccess**: Logout local exitoso, pero el backend falló (offline o error de red)
 * - **AlreadyLoggedOut**: No había sesión activa (llamada idempotente)
 *
 * ## Ejemplo
 * ```kotlin
 * val result = authService.logoutWithDetails()
 * when (result) {
 *     is LogoutResult.Success -> {
 *         // Sesión cerrada completamente
 *         navigateToLogin()
 *     }
 *     is LogoutResult.PartialSuccess -> {
 *         // Sesión local cerrada, pero backend no confirmó
 *         // El token podría seguir válido en servidor hasta que expire
 *         showWarning("Logout offline: ${result.remoteError}")
 *         navigateToLogin()
 *     }
 *     is LogoutResult.AlreadyLoggedOut -> {
 *         // Ya estaba deslogueado (no-op)
 *         // Útil para detectar llamadas duplicadas
 *     }
 * }
 * ```
 */
public sealed class LogoutResult {
    /**
     * Logout exitoso: local y remoto completados.
     *
     * El storage local está limpio Y el backend confirmó la invalidación del token.
     */
    public object Success : LogoutResult()

    /**
     * Logout local exitoso, remoto falló (offline o error de red).
     *
     * La sesión local está limpia, pero el token podría seguir válido en servidor
     * hasta que expire naturalmente.
     *
     * @property remoteError Descripción del error al intentar notificar al backend
     */
    public data class PartialSuccess(
        val remoteError: String?
    ) : LogoutResult()

    /**
     * Ya no había sesión activa (llamada idempotente).
     *
     * El usuario ya estaba en estado Unauthenticated cuando se llamó logout.
     * No se realizó ninguna acción (ni local ni remota).
     */
    public object AlreadyLoggedOut : LogoutResult()
}

/**
 * Verifica si el logout fue completamente exitoso (local + remoto).
 */
public val LogoutResult.isSuccess: Boolean
    get() = this is LogoutResult.Success

/**
 * Verifica si el storage local fue limpiado.
 *
 * Retorna true para Success y PartialSuccess (ambos limpian local).
 * Retorna false para AlreadyLoggedOut (no había nada que limpiar).
 */
public val LogoutResult.localCleared: Boolean
    get() = this is LogoutResult.Success || this is LogoutResult.PartialSuccess

/**
 * Verifica si el logout fue parcial (local limpiado, remoto falló).
 */
public val LogoutResult.isPartial: Boolean
    get() = this is LogoutResult.PartialSuccess

/**
 * Verifica si la llamada fue idempotente (ya estaba deslogueado).
 */
public val LogoutResult.wasAlreadyLoggedOut: Boolean
    get() = this is LogoutResult.AlreadyLoggedOut
