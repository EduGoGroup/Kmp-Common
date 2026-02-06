package com.edugo.test.module.auth.service

import com.edugo.test.module.auth.repository.AuthRepository
import com.edugo.test.module.auth.repository.StubAuthRepository
import com.edugo.test.module.storage.EduGoStorage
import com.edugo.test.module.storage.SafeEduGoStorage
import kotlinx.serialization.json.Json

/**
 * Factory para crear instancias de [AuthService].
 *
 * Proporciona métodos convenientes para crear servicios de autenticación
 * con diferentes configuraciones, tanto para producción como para testing.
 *
 * ## Uso en Producción
 *
 * ```kotlin
 * // Con repositorio real (requiere AuthRepositoryImpl que aún no existe)
 * val authService = AuthServiceFactory.createWithCustomComponents(
 *     repository = AuthRepositoryImpl(httpClient, baseUrl),
 *     storage = SafeEduGoStorage.create()
 * )
 * ```
 *
 * ## Uso en Testing
 *
 * ```kotlin
 * // Con stub (sin red)
 * val authService = AuthServiceFactory.createForTesting()
 *
 * // Con credenciales personalizadas
 * val authService = AuthServiceFactory.createForTesting(
 *     validEmail = "custom@test.com",
 *     validPassword = "customPass123"
 * )
 * ```
 *
 * ## Uso con DI (Koin, Dagger, etc.)
 *
 * ```kotlin
 * // Koin
 * single<AuthService> {
 *     AuthServiceFactory.createWithCustomComponents(
 *         repository = get(),
 *         storage = get()
 *     )
 * }
 *
 * // Para tests
 * factory<AuthService> {
 *     AuthServiceFactory.createForTesting()
 * }
 * ```
 */
public object AuthServiceFactory {

    /**
     * Crea un AuthService para testing con stubs.
     *
     * Usa [StubAuthRepository] que simula respuestas del backend sin red.
     * El storage es en memoria (no persistente entre reinicios).
     *
     * ## Comportamiento del Stub
     *
     * - Login exitoso: email y password configurados (defaults: test@edugo.com / password123)
     * - Logout: Siempre exitoso
     * - Refresh: Siempre exitoso con nuevos tokens
     *
     * ## Ejemplo
     *
     * ```kotlin
     * @Test
     * fun testLogin() = runTest {
     *     val service = AuthServiceFactory.createForTesting()
     *
     *     val result = service.login(
     *         LoginCredentials("test@edugo.com", "password123")
     *     )
     *
     *     assertTrue(result is LoginResult.Success)
     * }
     * ```
     *
     * @param validEmail Email válido para el stub (default: "test@edugo.com")
     * @param validPassword Password válido para el stub (default: "password123")
     * @return AuthService configurado para testing
     */
    public fun createForTesting(
        validEmail: String = "test@edugo.com",
        validPassword: String = "password123"
    ): AuthService {
        val stubRepository = StubAuthRepository().apply {
            this.validEmail = validEmail
            this.validPassword = validPassword
        }

        // Storage en memoria con nombre único para evitar contaminación entre tests
        val uniqueStorageName = "test_auth_storage_${System.currentTimeMillis()}_${(0..999999).random()}"
        val memoryStorage = EduGoStorage.create(uniqueStorageName)
        val safeStorage = SafeEduGoStorage.wrap(memoryStorage)

        return AuthServiceImpl(
            repository = stubRepository,
            storage = safeStorage
        )
    }

    /**
     * Crea un AuthService con componentes personalizados.
     *
     * Este método permite inyección manual de dependencias, útil para:
     * - Configuraciones personalizadas
     * - Testing con mocks específicos
     * - Integración con frameworks de DI
     *
     * ## Ejemplo con Repository Real
     *
     * ```kotlin
     * // Cuando AuthRepositoryImpl esté implementado
     * val httpClient = EduGoHttpClient.create()
     * val repository = AuthRepositoryImpl(httpClient, "https://api.edugo.com")
     * val storage = SafeEduGoStorage.create()
     *
     * val authService = AuthServiceFactory.createWithCustomComponents(
     *     repository = repository,
     *     storage = storage
     * )
     * ```
     *
     * ## Ejemplo con Mocks
     *
     * ```kotlin
     * val mockRepository = mockk<AuthRepository>()
     * val mockStorage = mockk<SafeEduGoStorage>()
     *
     * val authService = AuthServiceFactory.createWithCustomComponents(
     *     repository = mockRepository,
     *     storage = mockStorage
     * )
     * ```
     *
     * @param repository Repositorio de autenticación (real o stub)
     * @param storage Storage seguro para persistencia
     * @param json Instancia de Json para serialización (default: Json con ignoreUnknownKeys)
     * @return AuthService configurado
     */
    public fun createWithCustomComponents(
        repository: AuthRepository,
        storage: SafeEduGoStorage,
        json: Json = Json { ignoreUnknownKeys = true }
    ): AuthService {
        return AuthServiceImpl(repository, storage, json)
    }

    /**
     * Crea un AuthService para testing con stub que simula errores de red.
     *
     * Útil para tests de manejo de errores de conectividad.
     *
     * ## Ejemplo
     *
     * ```kotlin
     * @Test
     * fun testNetworkError() = runTest {
     *     val service = AuthServiceFactory.createForTestingWithNetworkError()
     *
     *     val result = service.login(
     *         LoginCredentials("test@edugo.com", "password123")
     *     )
     *
     *     assertTrue(result is LoginResult.Error)
     *     assertTrue(result.getErrorOrNull() is AuthError.NetworkError)
     * }
     * ```
     *
     * @return AuthService que siempre retorna errores de red
     */
    public fun createForTestingWithNetworkError(): AuthService {
        val stubRepository = StubAuthRepository.createWithNetworkError()
        val uniqueStorageName = "test_auth_storage_network_error_${System.currentTimeMillis()}_${(0..999999).random()}"
        val memoryStorage = EduGoStorage.create(uniqueStorageName)
        val safeStorage = SafeEduGoStorage.wrap(memoryStorage)

        return AuthServiceImpl(
            repository = stubRepository,
            storage = safeStorage
        )
    }

    /**
     * Crea un AuthService para testing con delay simulado.
     *
     * Útil para tests de loading states y timeouts.
     *
     * ## Ejemplo
     *
     * ```kotlin
     * @Test
     * fun testLoadingState() = runTest {
     *     val service = AuthServiceFactory.createForTestingWithDelay(500)
     *
     *     launch {
     *         service.authState.collect { state ->
     *             if (state is AuthState.Loading) {
     *                 println("Loading state detected")
     *             }
     *         }
     *     }
     *
     *     service.login(LoginCredentials("test@edugo.com", "password123"))
     * }
     * ```
     *
     * @param delayMillis Delay en milisegundos
     * @return AuthService con delay simulado
     */
    public fun createForTestingWithDelay(delayMillis: Long): AuthService {
        val stubRepository = StubAuthRepository.createWithDelay(delayMillis)
        val uniqueStorageName = "test_auth_storage_delay_${System.currentTimeMillis()}_${(0..999999).random()}"
        val memoryStorage = EduGoStorage.create(uniqueStorageName)
        val safeStorage = SafeEduGoStorage.wrap(memoryStorage)

        return AuthServiceImpl(
            repository = stubRepository,
            storage = safeStorage
        )
    }
}
