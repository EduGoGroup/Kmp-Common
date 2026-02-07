package com.edugo.test.module.auth.service

import com.edugo.test.module.auth.repository.AuthRepository
import com.edugo.test.module.auth.repository.StubAuthRepository
import com.edugo.test.module.auth.token.TokenRefreshConfig
import com.edugo.test.module.storage.EduGoStorage
import com.edugo.test.module.storage.SafeEduGoStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.Clock
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
     * @param scope CoroutineScope para operaciones asíncronas (default: Dispatchers.Default con SupervisorJob)
     * @param refreshConfig Configuración de refresh (default: TokenRefreshConfig.DEFAULT)
     * @return AuthService configurado para testing
     */
    public fun createForTesting(
        validEmail: String = "test@edugo.com",
        validPassword: String = "password123",
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        refreshConfig: TokenRefreshConfig = TokenRefreshConfig.DEFAULT
    ): AuthService {
        val stubRepository = StubAuthRepository().apply {
            this.validEmail = validEmail
            this.validPassword = validPassword
        }

        // Storage en memoria con nombre único para evitar contaminación entre tests
        val uniqueStorageName = "test_auth_storage_${Clock.System.now().toEpochMilliseconds()}_${(0..999999).random()}"
        val memoryStorage = EduGoStorage.create(uniqueStorageName)
        val safeStorage = SafeEduGoStorage.wrap(memoryStorage)

        return AuthServiceImpl(
            repository = stubRepository,
            storage = safeStorage,
            scope = scope,
            refreshConfig = refreshConfig
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
     * @param scope CoroutineScope para operaciones asíncronas (default: Dispatchers.Default con SupervisorJob)
     * @param refreshConfig Configuración de refresh (default: TokenRefreshConfig.DEFAULT)
     * @return AuthService configurado
     */
    public fun createWithCustomComponents(
        repository: AuthRepository,
        storage: SafeEduGoStorage,
        json: Json = Json { ignoreUnknownKeys = true },
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        refreshConfig: TokenRefreshConfig = TokenRefreshConfig.DEFAULT
    ): AuthService {
        return AuthServiceImpl(repository, storage, json, scope, refreshConfig)
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
     * @param scope CoroutineScope para operaciones asíncronas (default: Dispatchers.Default con SupervisorJob)
     * @return AuthService que siempre retorna errores de red
     */
    public fun createForTestingWithNetworkError(
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    ): AuthService {
        val stubRepository = StubAuthRepository.createWithNetworkError()
        val uniqueStorageName = "test_auth_storage_network_error_${Clock.System.now().toEpochMilliseconds()}_${(0..999999).random()}"
        val memoryStorage = EduGoStorage.create(uniqueStorageName)
        val safeStorage = SafeEduGoStorage.wrap(memoryStorage)

        return AuthServiceImpl(
            repository = stubRepository,
            storage = safeStorage,
            scope = scope
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
     * @param scope CoroutineScope para operaciones asíncronas (default: Dispatchers.Default con SupervisorJob)
     * @return AuthService con delay simulado
     */
    public fun createForTestingWithDelay(
        delayMillis: Long,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    ): AuthService {
        val stubRepository = StubAuthRepository.createWithDelay(delayMillis)
        val uniqueStorageName = "test_auth_storage_delay_${Clock.System.now().toEpochMilliseconds()}_${(0..999999).random()}"
        val memoryStorage = EduGoStorage.create(uniqueStorageName)
        val safeStorage = SafeEduGoStorage.wrap(memoryStorage)

        return AuthServiceImpl(
            repository = stubRepository,
            storage = safeStorage,
            scope = scope
        )
    }

    /**
     * Crea un HttpClient configurado con auto-refresh de tokens en 401.
     *
     * Este método es una utilidad para crear HttpClient con la configuración
     * completa de auto-refresh integrado con el AuthService.
     *
     * ## Uso Recomendado
     *
     * ```kotlin
     * // En tu módulo de DI
     * single<AuthService> {
     *     AuthServiceFactory.createWithCustomComponents(
     *         repository = get(),
     *         storage = get()
     *     )
     * }
     *
     * single<HttpClient> {
     *     AuthServiceFactory.createHttpClientWithAutoRefresh(
     *         authService = get(),
     *         logLevel = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
     *     )
     * }
     * ```
     *
     * ## Comportamiento
     *
     * Cuando un request recibe 401 Unauthorized:
     * 1. El HttpCallValidator intercepta la respuesta
     * 2. Llama a `authService.tokenRefreshManager.forceRefresh()`
     * 3. Si el refresh es exitoso: actualiza el token en storage (retry manual necesario)
     * 4. Si el refresh falla: `authService.onSessionExpired` emite evento
     *
     * **IMPORTANTE**: Ktor lanza la excepción del 401 incluso después del refresh
     * exitoso. El código cliente debe decidir si reintentar manualmente.
     *
     * @param authService Servicio de autenticación con TokenRefreshManager
     * @param logLevel Nivel de logging HTTP (default: NONE)
     * @param connectTimeoutMs Timeout de conexión (default: 30s)
     * @param requestTimeoutMs Timeout de request (default: 60s)
     * @return HttpClient con auto-refresh configurado
     *
     * @see com.edugo.test.module.network.HttpClientFactory.createWithAutoRefresh Para más detalles
     */
    public fun createHttpClientWithAutoRefresh(
        authService: AuthService,
        logLevel: io.ktor.client.plugins.logging.LogLevel = io.ktor.client.plugins.logging.LogLevel.NONE,
        connectTimeoutMs: Long = 30_000L,
        requestTimeoutMs: Long = 60_000L
    ): io.ktor.client.HttpClient {
        return com.edugo.test.module.network.HttpClientFactory.createWithAutoRefresh(
            authService = authService,
            logLevel = logLevel,
            connectTimeoutMs = connectTimeoutMs,
            requestTimeoutMs = requestTimeoutMs
        )
    }
}
