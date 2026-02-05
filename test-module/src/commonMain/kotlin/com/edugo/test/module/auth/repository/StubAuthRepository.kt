package com.edugo.test.module.auth.repository

import com.edugo.test.module.auth.model.AuthUserInfo
import com.edugo.test.module.auth.model.LoginCredentials
import com.edugo.test.module.auth.model.LoginResponse
import com.edugo.test.module.core.ErrorCode
import com.edugo.test.module.core.Result
import kotlinx.coroutines.delay

/**
 * Implementación stub del repositorio de autenticación para testing.
 *
 * Esta implementación simula las respuestas del backend sin realizar llamadas
 * de red reales. Es útil para:
 * - Tests unitarios que no requieren red
 * - Desarrollo offline
 * - Prototipado rápido de UI
 * - Tests de integración con comportamiento controlado
 *
 * ## Comportamiento por Defecto
 *
 * - **Login exitoso**: `test@edugo.com` / `password123`
 * - **Login fallido**: Cualquier otra combinación
 * - **Logout**: Siempre exitoso
 * - **Refresh**: Siempre exitoso con tokens nuevos
 *
 * ## Configuración de Comportamiento
 *
 * Puedes controlar el comportamiento del stub mediante propiedades:
 *
 * ```kotlin
 * val stub = StubAuthRepository()
 *
 * // Simular error de red
 * stub.simulateNetworkError = true
 * val result = stub.login(credentials) // Result.Failure(network error)
 *
 * // Simular delay (latencia)
 * stub.simulateDelay = 1000 // 1 segundo
 * val result = stub.login(credentials) // Espera 1 segundo antes de responder
 *
 * // Restaurar comportamiento normal
 * stub.simulateNetworkError = false
 * stub.simulateDelay = 0
 * ```
 *
 * ## Credenciales de Prueba
 *
 * ```kotlin
 * // Login exitoso
 * val validCredentials = LoginCredentials(
 *     email = "test@edugo.com",
 *     password = "password123"
 * )
 *
 * // Login fallido
 * val invalidCredentials = LoginCredentials(
 *     email = "wrong@edugo.com",
 *     password = "wrongpass"
 * )
 * ```
 *
 * ## Ejemplo de Uso en Tests
 *
 * ```kotlin
 * class AuthServiceTest {
 *     private lateinit var repository: AuthRepository
 *     private lateinit var service: AuthService
 *
 *     @BeforeTest
 *     fun setup() {
 *         repository = StubAuthRepository()
 *         service = AuthServiceImpl(repository, storage)
 *     }
 *
 *     @Test
 *     fun `login with valid credentials succeeds`() = runTest {
 *         val credentials = LoginCredentials("test@edugo.com", "password123")
 *         val result = service.login(credentials)
 *
 *         assertTrue(result is LoginResult.Success)
 *         assertEquals("test@edugo.com", result.getOrNull()?.user?.email)
 *     }
 *
 *     @Test
 *     fun `login with invalid credentials fails`() = runTest {
 *         val credentials = LoginCredentials("wrong@edugo.com", "wrong")
 *         val result = service.login(credentials)
 *
 *         assertTrue(result is LoginResult.Error)
 *     }
 *
 *     @Test
 *     fun `login with network error fails gracefully`() = runTest {
 *         val stub = repository as StubAuthRepository
 *         stub.simulateNetworkError = true
 *
 *         val credentials = LoginCredentials("test@edugo.com", "password123")
 *         val result = service.login(credentials)
 *
 *         assertTrue(result is LoginResult.Error)
 *     }
 * }
 * ```
 *
 * ## Ejemplo de Uso en Desarrollo
 *
 * ```kotlin
 * // En configuración de debug/preview
 * val authRepository: AuthRepository = if (BuildConfig.DEBUG) {
 *     StubAuthRepository()
 * } else {
 *     AuthRepositoryImpl.create(baseUrl = "https://api.edugo.com")
 * }
 * ```
 */
public class StubAuthRepository : AuthRepository {

    /**
     * Si es true, todas las operaciones retornan error de red.
     */
    public var simulateNetworkError: Boolean = false

    /**
     * Delay en milisegundos para simular latencia de red.
     * Default: 0 (sin delay)
     */
    public var simulateDelay: Long = 0

    /**
     * Email válido para login exitoso.
     * Cualquier otro email fallará.
     */
    public var validEmail: String = "test@edugo.com"

    /**
     * Password válido para login exitoso.
     * Cualquier otro password fallará.
     */
    public var validPassword: String = "password123"

    /**
     * Usuario de prueba que se retorna en login exitoso.
     * Puede ser modificado para personalizar los tests.
     */
    public var testUser: AuthUserInfo = AuthUserInfo.createTestUser(
        id = "stub-user-123",
        email = "test@edugo.com",
        firstName = "Test",
        lastName = "User",
        role = "student"
    )

    override suspend fun login(credentials: LoginCredentials): Result<LoginResponse> {
        // Simular delay si está configurado
        if (simulateDelay > 0) {
            delay(simulateDelay)
        }

        // Simular error de red si está configurado
        if (simulateNetworkError) {
            return Result.Failure(ErrorCode.NETWORK_TIMEOUT.description)
        }

        // Validar credenciales
        return if (credentials.email == validEmail && credentials.password == validPassword) {
            // Login exitoso
            Result.Success(
                LoginResponse.createTestResponse(
                    user = testUser
                )
            )
        } else {
            // Credenciales inválidas
            Result.Failure(ErrorCode.AUTH_INVALID_CREDENTIALS.description)
        }
    }

    override suspend fun logout(accessToken: String): Result<Unit> {
        // Simular delay si está configurado
        if (simulateDelay > 0) {
            delay(simulateDelay)
        }

        // Simular error de red si está configurado
        if (simulateNetworkError) {
            return Result.Failure(ErrorCode.NETWORK_TIMEOUT.description)
        }

        // Logout siempre exitoso en stub
        return Result.Success(Unit)
    }

    override suspend fun refresh(refreshToken: String): Result<RefreshResponse> {
        // Simular delay si está configurado
        if (simulateDelay > 0) {
            delay(simulateDelay)
        }

        // Simular error de red si está configurado
        if (simulateNetworkError) {
            return Result.Failure(ErrorCode.NETWORK_TIMEOUT.description)
        }

        // Validar que el refresh token no esté vacío
        return if (refreshToken.isNotBlank()) {
            // Refresh exitoso con nuevo access token
            Result.Success(RefreshResponse.createTestResponse())
        } else {
            // Refresh token inválido
            Result.Failure(ErrorCode.AUTH_REFRESH_TOKEN_INVALID.description)
        }
    }

    /**
     * Resetea todas las configuraciones a sus valores por defecto.
     *
     * Útil para limpiar estado entre tests.
     *
     * ```kotlin
     * @AfterTest
     * fun cleanup() {
     *     (repository as StubAuthRepository).reset()
     * }
     * ```
     */
    public fun reset() {
        simulateNetworkError = false
        simulateDelay = 0
        validEmail = "test@edugo.com"
        validPassword = "password123"
        testUser = AuthUserInfo.createTestUser(
            id = "stub-user-123",
            email = "test@edugo.com",
            firstName = "Test",
            lastName = "User",
            role = "student"
        )
    }

    companion object {
        /**
         * Credenciales válidas por defecto.
         */
        public val VALID_CREDENTIALS: LoginCredentials = LoginCredentials(
            email = "test@edugo.com",
            password = "password123"
        )

        /**
         * Credenciales inválidas para testing.
         */
        public val INVALID_CREDENTIALS: LoginCredentials = LoginCredentials(
            email = "invalid@edugo.com",
            password = "wrongpassword"
        )

        /**
         * Refresh token válido para testing.
         */
        public const val VALID_REFRESH_TOKEN: String = "valid_refresh_token_123"

        /**
         * Refresh token inválido para testing.
         */
        public const val INVALID_REFRESH_TOKEN: String = ""

        /**
         * Access token válido para testing.
         */
        public const val VALID_ACCESS_TOKEN: String = "valid_access_token_123"

        /**
         * Crea instancia con configuración por defecto.
         */
        public fun create(): StubAuthRepository = StubAuthRepository()

        /**
         * Crea instancia que siempre retorna error de red.
         *
         * Útil para tests de manejo de errores de conectividad.
         */
        public fun createWithNetworkError(): StubAuthRepository {
            return StubAuthRepository().apply {
                simulateNetworkError = true
            }
        }

        /**
         * Crea instancia con delay específico.
         *
         * Útil para tests de timeout o loading states.
         */
        public fun createWithDelay(delayMillis: Long): StubAuthRepository {
            return StubAuthRepository().apply {
                simulateDelay = delayMillis
            }
        }

        /**
         * Crea instancia con usuario personalizado.
         *
         * Útil para tests que necesitan datos específicos del usuario.
         */
        public fun createWithUser(user: AuthUserInfo): StubAuthRepository {
            return StubAuthRepository().apply {
                testUser = user
                validEmail = user.email
            }
        }
    }
}
