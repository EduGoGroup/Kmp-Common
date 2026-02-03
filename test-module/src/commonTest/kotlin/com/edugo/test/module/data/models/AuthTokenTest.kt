package com.edugo.test.module.data.models

import com.edugo.test.module.core.Result
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Tests del modelo AuthToken - Verifica serialización de Instant y validaciones
 */
class AuthTokenTest {

    private fun createValidToken() = AuthToken(
        token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token",
        expiresAt = Clock.System.now() + 1.hours,
        refreshToken = "refresh_token_abc123"
    )

    @Test
    fun `token válido pasa validación`() {
        val token = createValidToken()
        assertTrue(token.validate() is Result.Success)
    }

    @Test
    fun `validación falla si token está vacío`() {
        val token = createValidToken().copy(token = "")
        val result = token.validate()
        assertTrue(result is Result.Failure, "Validación debe fallar con token vacío")
    }

    @Test
    fun `validación falla si refreshToken está vacío cuando está presente`() {
        val token = createValidToken().copy(refreshToken = "")
        val result = token.validate()
        assertTrue(result is Result.Failure,
            "Validación debe fallar con refreshToken vacío (debe ser null o no-vacío)")
    }

    @Test
    fun `validación pasa si refreshToken es null`() {
        val token = createValidToken().copy(refreshToken = null)
        assertTrue(token.validate() is Result.Success,
            "refreshToken null debe ser válido")
    }

    @Test
    fun `serialización AuthToken a JSON funciona`() {
        val token = createValidToken()
        val json = Json.encodeToString(token)

        // Verificar que JSON contiene las claves esperadas
        assertTrue(json.contains("\"token\""), "JSON debe contener 'token'")
        assertTrue(json.contains("\"expires_at\""), "JSON debe contener 'expires_at'")
        assertTrue(json.contains("\"refresh_token\""), "JSON debe contener 'refresh_token'")
    }

    @Test
    fun `deserialización JSON a AuthToken funciona`() {
        val json = """
            {
                "token": "test_token_xyz",
                "expires_at": "2024-12-31T23:59:59Z",
                "refresh_token": "refresh_xyz"
            }
        """.trimIndent()

        val token = Json.decodeFromString<AuthToken>(json)

        assertEquals("test_token_xyz", token.token)
        assertEquals("refresh_xyz", token.refreshToken)
    }

    @Test
    fun `round-trip preserva todos los campos`() {
        val original = createValidToken()
        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<AuthToken>(json)

        assertEquals(original.token, deserialized.token)
        assertEquals(original.refreshToken, deserialized.refreshToken)
        // Instant puede perder precisión de nanosegundos en serialización ISO-8601
        // Verificar que la diferencia es mínima (< 1 segundo)
        val timeDiff = (original.expiresAt - deserialized.expiresAt).absoluteValue
        assertTrue(timeDiff < 1.seconds,
            "expiresAt debe preservarse con precisión de segundos. Diff: $timeDiff")
    }

    @Test
    fun `Instant se serializa a formato ISO-8601`() {
        val token = createValidToken()
        val json = Json.encodeToString(token)

        // Verificar que la fecha tiene formato ISO-8601
        // Ejemplo: "2024-01-01T12:00:00Z" o "2024-01-01T12:00:00.123456Z"
        val regex = Regex(""""expires_at":"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z"""")
        assertTrue(regex.containsMatchIn(json),
            "expires_at debe tener formato ISO-8601. JSON: $json")
    }

    @Test
    fun `serialización con refreshToken null omite el campo o lo serializa como null`() {
        val token = createValidToken().copy(refreshToken = null)
        val json = Json.encodeToString(token)

        // kotlinx-serialization puede omitir campos null o serializarlos como null
        // Ambos son válidos
        assertTrue(
            !json.contains("\"refresh_token\"") || json.contains("\"refresh_token\":null"),
            "refresh_token debe ser omitido o null en JSON"
        )
    }

    @Test
    fun `serialización con refreshToken presente incluye el campo`() {
        val token = createValidToken().copy(refreshToken = "refresh_abc")
        val json = Json.encodeToString(token)

        assertTrue(json.contains("\"refresh_token\":\"refresh_abc\""),
            "JSON debe contener refresh_token cuando está presente")
    }

    @Test
    fun `@SerialName mapea correctamente a snake_case`() {
        val token = createValidToken()
        val json = Json.encodeToString(token)

        // Verificar mapeo snake_case
        assertTrue(json.contains("\"expires_at\""), "JSON debe contener 'expires_at'")
        assertTrue(json.contains("\"refresh_token\"") || !json.contains("refreshToken"),
            "JSON debe usar 'refresh_token' no 'refreshToken'")

        // Verificar que NO contiene camelCase
        assertFalse(json.contains("\"expiresAt\""),
            "JSON NO debe contener 'expiresAt' en camelCase")
    }

    @Test
    fun `isExpired retorna false para token futuro`() {
        val token = AuthToken(
            token = "test",
            expiresAt = Clock.System.now() + 1.hours
        )
        assertFalse(token.isExpired(), "Token que expira en el futuro no debe estar expirado")
    }

    @Test
    fun `isExpired retorna true para token pasado`() {
        val token = AuthToken(
            token = "test",
            expiresAt = Clock.System.now() - 1.hours
        )
        assertTrue(token.isExpired(), "Token que expiró en el pasado debe estar expirado")
    }

    @Test
    fun `isValid retorna true para token válido y no expirado`() {
        val token = createValidToken()
        assertTrue(token.isValid(), "Token válido y no expirado debe ser isValid=true")
    }

    @Test
    fun `isValid retorna false para token expirado`() {
        val token = AuthToken(
            token = "test",
            expiresAt = Clock.System.now() - 1.hours
        )
        assertFalse(token.isValid(), "Token expirado debe ser isValid=false")
    }

    @Test
    fun `isValid retorna false para token con estructura inválida`() {
        val token = AuthToken(
            token = "", // Token vacío es inválido
            expiresAt = Clock.System.now() + 1.hours
        )
        assertFalse(token.isValid(), "Token con estructura inválida debe ser isValid=false")
    }

    @Test
    fun `hasRefreshToken retorna true cuando refreshToken está presente`() {
        val token = createValidToken().copy(refreshToken = "refresh_abc")
        assertTrue(token.hasRefreshToken(), "Debe tener refresh token")
    }

    @Test
    fun `hasRefreshToken retorna false cuando refreshToken es null`() {
        val token = createValidToken().copy(refreshToken = null)
        assertFalse(token.hasRefreshToken(), "No debe tener refresh token cuando es null")
    }

    @Test
    fun `hasRefreshToken retorna false cuando refreshToken está vacío`() {
        val token = createValidToken().copy(refreshToken = "")
        assertFalse(token.hasRefreshToken(), "No debe tener refresh token cuando está vacío")
    }

    @Test
    fun `timeUntilExpiration retorna duración positiva para token futuro`() {
        val token = AuthToken(
            token = "test",
            expiresAt = Clock.System.now() + 1.hours
        )
        val remaining = token.timeUntilExpiration()
        assertTrue(remaining.isPositive(), "Duración debe ser positiva para token futuro")
        assertTrue(remaining.inWholeMinutes > 50, "Debe quedar aproximadamente 1 hora")
    }

    @Test
    fun `timeUntilExpiration retorna duración negativa para token expirado`() {
        val token = AuthToken(
            token = "test",
            expiresAt = Clock.System.now() - 1.hours
        )
        val remaining = token.timeUntilExpiration()
        assertTrue(remaining.isNegative(), "Duración debe ser negativa para token expirado")
    }

    @Test
    fun `createTestToken crea token válido con parámetros default`() {
        val token = AuthToken.createTestToken()

        assertTrue(token.validate() is Result.Success, "Token de test debe ser válido")
        assertTrue(token.token.isNotBlank(), "Token debe tener valor")
        assertTrue(token.hasRefreshToken(), "Token de test debe tener refreshToken por default")
        assertFalse(token.isExpired(), "Token de test no debe estar expirado")
    }

    @Test
    fun `createTestToken respeta parámetros personalizados`() {
        val token = AuthToken.createTestToken(
            durationSeconds = 7200, // 2 horas
            includeRefresh = false
        )

        assertTrue(token.validate() is Result.Success)
        assertFalse(token.hasRefreshToken(), "Token sin refresh según parámetro")
        val remaining = token.timeUntilExpiration()
        assertTrue(remaining.inWholeMinutes > 110, // Aproximadamente 2 horas
            "Duración debe ser cercana a 2 horas")
    }

    @Test
    fun `toLogString oculta token por seguridad`() {
        val token = createValidToken()
        val logString = token.toLogString()

        // Verificar que el token completo NO está en el log
        assertFalse(logString.contains(token.token),
            "toLogString NO debe contener el token completo por seguridad")

        // Verificar que contiene información útil
        assertTrue(logString.contains("AuthToken"), "Log debe identificar el tipo")
        assertTrue(logString.contains("expires"), "Log debe mencionar expiración")
    }

    @Test
    fun `deserialización parcial con valores default funciona`() {
        val json = """{"token":"test_token"}"""

        val token = Json.decodeFromString<AuthToken>(json)

        assertEquals("test_token", token.token)
        assertEquals(null, token.refreshToken, "refreshToken debe ser null por default")
        // expiresAt tendrá Clock.System.now() por default
    }
}
