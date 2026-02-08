package com.edugo.test.module.auth.repository

import com.edugo.test.module.auth.model.LoginCredentials
import com.edugo.test.module.auth.model.LoginResponse
import com.edugo.test.module.core.Result

/**
 * Repositorio de autenticación que maneja las operaciones de red con el backend.
 *
 * Esta interface define el contrato para todas las operaciones de autenticación
 * que requieren comunicación con el servidor, incluyendo login, logout y refresh
 * de tokens.
 *
 * ## Responsabilidades
 *
 * - **Login**: Autenticar usuario con credenciales
 * - **Logout**: Invalidar sesión en el servidor
 * - **Refresh**: Renovar access token usando refresh token
 *
 * ## Implementaciones
 *
 * - [AuthRepositoryImpl]: Implementación real que usa [EduGoHttpClient]
 * - [StubAuthRepository]: Implementación fake para testing sin red
 *
 * ## Diseño y Arquitectura
 *
 * Este repositorio sigue el patrón Repository de Clean Architecture:
 * - **Interface en el dominio**: Define el contrato sin acoplar a implementación
 * - **Implementación en infraestructura**: Usa Ktor/HttpClient para networking
 * - **Testeable**: Permite inyectar stubs para testing sin red real
 *
 * ## Ejemplo de Uso en AuthService
 *
 * ```kotlin
 * class AuthServiceImpl(
 *     private val repository: AuthRepository,
 *     private val storage: EduGoStorage
 * ) : AuthService {
 *     override suspend fun login(credentials: LoginCredentials): LoginResult {
 *         return when (val result = repository.login(credentials)) {
 *             is Result.Success -> {
 *                 // Guardar tokens y user
 *                 saveAuthData(result.data)
 *                 LoginResult.Success(result.data)
 *             }
 *             is Result.Failure -> {
 *                 val error = AuthError.fromMessage(result.error)
 *                 LoginResult.Error(error)
 *             }
 *             is Result.Loading -> {
 *                 LoginResult.Error(AuthError.UnknownError("Unexpected state"))
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Ejemplo con Inyección de Dependencias
 *
 * ```kotlin
 * // Producción
 * val httpClient = EduGoHttpClient.create()
 * val authRepository: AuthRepository = AuthRepositoryImpl(
 *     httpClient = httpClient,
 *     baseUrl = "https://api.edugo.com"
 * )
 *
 * // Testing
 * val authRepository: AuthRepository = StubAuthRepository()
 * ```
 *
 * ## Manejo de Errores
 *
 * Todos los métodos retornan `Result<T>` que puede ser:
 * - `Result.Success<T>`: Operación exitosa con datos
 * - `Result.Failure`: Operación fallida con mensaje de error
 *
 * Los errores típicos incluyen:
 * - **401 Unauthorized**: Credenciales inválidas o token expirado
 * - **403 Forbidden**: Usuario inactivo o sin permisos
 * - **404 Not Found**: Usuario no existe
 * - **423 Locked**: Cuenta bloqueada
 * - **500/502/503**: Errores del servidor
 * - **Network errors**: Timeout, no conexión, DNS failure
 */
public interface AuthRepository {

    /**
     * Autentica un usuario con sus credenciales.
     *
     * Realiza una petición POST a `/v1/auth/login` con las credenciales
     * del usuario y retorna los tokens JWT junto con la información del usuario.
     *
     * ## Endpoint del Backend
     *
     * ```
     * POST /v1/auth/login
     * Content-Type: application/json
     *
     * Body:
     * {
     *   "email": "user@edugo.com",
     *   "password": "securePassword123"
     * }
     * ```
     *
     * ## Respuesta Exitosa (200 OK)
     *
     * ```json
     * {
     *   "access_token": "eyJhbGciOiJIUzI1NiI...",
     *   "expires_in": 3600,
     *   "refresh_token": "refresh_token_123",
     *   "token_type": "Bearer",
     *   "user": {
     *     "id": "user-123",
     *     "email": "user@edugo.com",
     *     "first_name": "John",
     *     "last_name": "Doe",
     *     "full_name": "John Doe",
     *     "role": "student",
     *     "school_id": "school-456"
     *   }
     * }
     * ```
     *
     * ## Errores Posibles
     *
     * - **400 Bad Request**: Datos de entrada inválidos (validación)
     * - **401 Unauthorized**: Credenciales incorrectas
     * - **403 Forbidden**: Usuario inactivo o sin acceso
     * - **404 Not Found**: Usuario no existe
     * - **423 Locked**: Cuenta bloqueada por seguridad
     * - **500 Internal Server Error**: Error del servidor
     * - **Network errors**: Timeout, no conexión, etc.
     *
     * ## Ejemplo de Uso
     *
     * ```kotlin
     * val credentials = LoginCredentials(
     *     email = "user@edugo.com",
     *     password = "password123"
     * )
     *
     * when (val result = repository.login(credentials)) {
     *     is Result.Success -> {
     *         val response = result.data
     *         println("Logged in: ${response.user.email}")
     *         println("Token: ${response.accessToken}")
     *     }
     *     is Result.Failure -> {
     *         println("Login failed: ${result.error}")
     *     }
     *     is Result.Loading -> {
     *         // No debería ocurrir en suspend function
     *     }
     * }
     * ```
     *
     * @param credentials Credenciales del usuario (email y password)
     * @return [Result.Success] con [LoginResponse] si el login es exitoso,
     *         [Result.Failure] con mensaje de error si falla
     */
    public suspend fun login(credentials: LoginCredentials): Result<LoginResponse>

    /**
     * Invalida la sesión del usuario en el servidor.
     *
     * Realiza una petición POST a `/v1/auth/logout` con el access token
     * en el header Authorization. El servidor invalidará el token y cualquier
     * sesión asociada.
     *
     * ## Endpoint del Backend
     *
     * ```
     * POST /v1/auth/logout
     * Authorization: Bearer <access_token>
     * ```
     *
     * ## Respuesta Exitosa (200 OK o 204 No Content)
     *
     * El servidor retorna éxito sin body, indicando que la sesión fue invalidada.
     *
     * ## Errores Posibles
     *
     * - **401 Unauthorized**: Token inválido o expirado
     * - **500 Internal Server Error**: Error del servidor
     * - **Network errors**: Timeout, no conexión, etc.
     *
     * **NOTA**: Los errores de red se ignoran típicamente en logout, ya que
     * el objetivo principal es limpiar el estado local. Si el servidor no
     * responde, se asume que eventualmente el token expirará.
     *
     * ## Ejemplo de Uso
     *
     * ```kotlin
     * val accessToken = "eyJhbGciOiJIUzI1NiI..."
     *
     * when (val result = repository.logout(accessToken)) {
     *     is Result.Success -> {
     *         println("Logout successful")
     *         // Limpiar storage local
     *         storage.remove("auth_token")
     *         storage.remove("auth_user")
     *     }
     *     is Result.Failure -> {
     *         println("Logout failed: ${result.error}")
     *         // Limpiar storage de todos modos
     *         storage.remove("auth_token")
     *         storage.remove("auth_user")
     *     }
     * }
     * ```
     *
     * @param accessToken Token de acceso JWT para invalidar
     * @return [Result.Success] con Unit si el logout es exitoso,
     *         [Result.Failure] con mensaje de error si falla
     */
    public suspend fun logout(accessToken: String): Result<Unit>

    /**
     * Renueva el access token usando un refresh token.
     *
     * Realiza una petición POST a `/v1/auth/refresh` con el refresh token
     * y retorna un nuevo access token. El refresh token NO se renueva,
     * se mantiene el mismo.
     *
     * ## Endpoint del Backend
     *
     * ```
     * POST /v1/auth/refresh
     * Content-Type: application/json
     *
     * Body:
     * {
     *   "refresh_token": "refresh_token_123"
     * }
     * ```
     *
     * ## Respuesta Exitosa (200 OK)
     *
     * ```json
     * {
     *   "access_token": "eyJhbGciOiJIUzI1NiI...",
     *   "expires_in": 3600,
     *   "token_type": "Bearer"
     * }
     * ```
     *
     * **IMPORTANTE**: El refresh token NO se incluye en la respuesta,
     * se debe mantener el refresh token original.
     *
     * ## Errores Posibles
     *
     * - **401 Unauthorized**: Refresh token inválido o expirado
     * - **403 Forbidden**: Usuario inactivo o sin acceso
     * - **500 Internal Server Error**: Error del servidor
     * - **Network errors**: Timeout, no conexión, etc.
     *
     * Si el refresh token es inválido o expirado, el usuario debe
     * volver a hacer login.
     *
     * ## Ejemplo de Uso
     *
     * ```kotlin
     * val refreshToken = "refresh_token_123"
     *
     * when (val result = repository.refresh(refreshToken)) {
     *     is Result.Success -> {
     *         val response = result.data
     *         // Crear nuevo AuthToken manteniendo el refresh token original
     *         val newAuthToken = response.toAuthToken(refreshToken)
     *         // Guardar nuevo token
     *         storage.putString("auth_token", Json.encodeToString(newAuthToken))
     *     }
     *     is Result.Failure -> {
     *         println("Refresh failed: ${result.error}")
     *         // Redirigir a login
     *         navigateToLogin()
     *     }
     * }
     * ```
     *
     * @param refreshToken Token de refresh para renovar la sesión
     * @return [Result.Success] con [RefreshResponse] si el refresh es exitoso,
     *         [Result.Failure] con mensaje de error si falla
     */
    public suspend fun refresh(refreshToken: String): Result<RefreshResponse>

    /**
     * Verifica un token contra el backend.
     *
     * Realiza una petición POST a `/v1/auth/verify` con el token JWT
     * y retorna información de validación del token.
     *
     * ## Endpoint del Backend
     *
     * ```
     * POST /v1/auth/verify
     * Content-Type: application/json
     *
     * Body:
     * {
     *   "token": "eyJhbGciOiJIUzI1NiI..."
     * }
     * ```
     *
     * ## Respuesta Exitosa (200 OK) - Token Válido
     *
     * ```json
     * {
     *   "valid": true,
     *   "user_id": "user-123",
     *   "email": "user@edugo.com",
     *   "role": "student",
     *   "school_id": "school-456",
     *   "expires_at": "2025-12-31T23:59:59Z"
     * }
     * ```
     *
     * ## Respuesta Exitosa (200 OK) - Token Inválido
     *
     * ```json
     * {
     *   "valid": false,
     *   "error": "Token has expired"
     * }
     * ```
     *
     * ## Errores Posibles
     *
     * - **400 Bad Request**: Token malformado o falta parámetro
     * - **429 Too Many Requests**: Rate limit excedido
     * - **500 Internal Server Error**: Error del servidor
     * - **Network errors**: Timeout, no conexión, etc.
     *
     * ## Ejemplo de Uso
     *
     * ```kotlin
     * val token = "eyJhbGciOiJIUzI1NiI..."
     *
     * when (val result = repository.verifyToken(token)) {
     *     is Result.Success -> {
     *         val response = result.data
     *         if (response.valid) {
     *             println("Token válido para: ${response.email}")
     *         } else {
     *             println("Token inválido: ${response.error}")
     *         }
     *     }
     *     is Result.Failure -> {
     *         println("Verification failed: ${result.error}")
     *     }
     * }
     * ```
     *
     * @param token Token JWT a verificar
     * @return [Result.Success] con [TokenVerificationResponse] si la petición es exitosa,
     *         [Result.Failure] con mensaje de error si falla
     */
    public suspend fun verifyToken(token: String): Result<TokenVerificationResponse>
}
