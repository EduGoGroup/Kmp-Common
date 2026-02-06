# Guía de Integración: Sistema de Refresh Token

Esta guía te muestra cómo integrar el sistema completo de refresh de tokens en tu aplicación EduGo.

## 🚀 Quick Start (3 pasos)

### 1. Configurar HttpClient con Auto-Refresh

```kotlin
// En tu módulo de DI (Koin, por ejemplo)
val networkModule = module {
    
    // AuthService (ya configurado)
    single<AuthService> {
        AuthServiceFactory.createWithCustomComponents(
            repository = get(),
            storage = get(),
            scope = get()  // CoroutineScope de la app
        )
    }
    
    // HttpClient con auto-refresh HABILITADO
    single<HttpClient> {
        HttpClientFactory.createWithAutoRefresh(
            authService = get(),
            logLevel = if (isDebugBuild()) LogLevel.INFO else LogLevel.NONE
        )
    }
}
```

### 2. Observar Expiración de Sesión en la UI

```kotlin
@Composable
fun App(
    authService: AuthService = koinInject()
) {
    val navigator = rememberNavigator()
    
    // IMPORTANTE: Observar cuando la sesión expira
    LaunchedEffect(Unit) {
        authService.onSessionExpired.collect {
            // Sesión expiró y no se pudo renovar
            // Navegar a login
            navigator.navigate(Screen.Login) {
                popUpTo(Screen.Main) { inclusive = true }
            }
        }
    }
    
    // Resto de tu app...
    NavHost(navigator) {
        // ... tus rutas
    }
}
```

### 3. Usar HttpClient en Repositories

```kotlin
class UserRepository(
    private val httpClient: HttpClient  // Inyectado con auto-refresh
) {
    suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            // Primer intento
            val response = httpClient.get("https://api.edugo.com/user/profile")
            Result.Success(response.body())
        } catch (e: ClientRequestException) {
            // Si fue 401, el token ya se refrescó automáticamente
            // Reintentamos manualmente
            if (e.response.status == HttpStatusCode.Unauthorized) {
                try {
                    val retryResponse = httpClient.get("https://api.edugo.com/user/profile")
                    Result.Success(retryResponse.body())
                } catch (retryError: Exception) {
                    Result.Failure(retryError.message ?: "Unauthorized")
                }
            } else {
                Result.Failure(e.message ?: "Request failed")
            }
        } catch (e: Exception) {
            Result.Failure(e.message ?: "Unknown error")
        }
    }
}
```

**Nota importante**: Cuando el HttpClient recibe un 401:
1. El `HttpCallValidator` automáticamente refresca el token y lo guarda en storage
2. La excepción 401 se propaga al código cliente
3. El cliente debe capturar la excepción y **reintentar manualmente** la petición
4. El segundo intento usará el token refrescado automáticamente

---

## ✨ ¡Listo! Ya tienes auto-refresh funcionando

Con estos 3 pasos, tu app ahora:
- ✅ Renueva tokens automáticamente cuando expiran (guardado en storage)
- ✅ Permite reintentar requests fallidos por 401 con el nuevo token
- ✅ Navega a login cuando la sesión es irrecuperable
- ✅ Maneja concurrencia (múltiples requests → un solo refresh)

**Importante**: El retry del request es **manual** - el código cliente debe capturar la excepción 401 y reintentar. El token ya estará refrescado automáticamente para el segundo intento.

---

## 📊 Flujo Completo

```
┌─────────────────────────────────────────────────────────────────┐
│                    USER MAKES A REQUEST                          │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │  HttpClient sends    │
                    │  request with token  │
                    └──────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
                ▼                             ▼
        ┌──────────────┐            ┌──────────────────┐
        │ 200 OK       │            │ 401 Unauthorized │
        │ Return data  │            │ Token expired    │
        └──────────────┘            └──────────────────┘
                                                │
                                                ▼
                                    ┌────────────────────────┐
                                    │ TokenRefreshManager    │
                                    │ attempts refresh       │
                                    └────────────────────────┘
                                                │
                               ┌────────────────┴────────────────┐
                               │                                 │
                               ▼                                 ▼
                    ┌──────────────────┐           ┌──────────────────────┐
                    │ Refresh SUCCESS  │           │ Refresh FAILED       │
                    │ Token saved      │           │ (token expired/      │
                    │ 401 propagated   │           │  revoked/network)    │
                    └──────────────────┘           └──────────────────────┘
                               │                                 │
                               ▼                                 ▼
                    ┌──────────────────┐           ┌──────────────────────┐
                    │ Client catches   │           │ onSessionExpired     │
                    │ 401 and retries  │           │ emitted              │
                    │ manually         │           └──────────────────────┘
                    └──────────────────┘                        │
                               │                                 ▼
                               ▼                     ┌──────────────────────┐
                    ┌──────────────────┐            │ UI navigates to      │
                    │ 200 OK on retry  │            │ login screen         │
                    │ Return data      │            └──────────────────────┘
                    └──────────────────┘
```

---

## ⚙️ Configuración Avanzada

### Personalizar Retry y Backoff

```kotlin
single<AuthService> {
    AuthServiceFactory.createWithCustomComponents(
        repository = get(),
        storage = get(),
        scope = get(),
        refreshConfig = TokenRefreshConfig(
            refreshThresholdSeconds = 300,  // Refrescar 5 min antes de expirar
            maxRetryAttempts = 3,           // Hasta 3 reintentos
            retryDelayMs = 1000,            // Delay base de 1 segundo
            enableTokenRotation = true      // Soportar rotation
        )
    )
}
```

### Usar Configuraciones Predefinidas

```kotlin
// Desarrollo: más agresivo
TokenRefreshConfig.DEVELOPMENT

// Producción: balanceado (default)
TokenRefreshConfig.DEFAULT

// Conservador: para redes lentas
TokenRefreshConfig.CONSERVATIVE

// Sin reintentos: para testing
TokenRefreshConfig.NO_RETRY
```

### Observar Fallos de Refresh (Opcional)

Si quieres reaccionar a fallos específicos de refresh:

```kotlin
@Composable
fun App(authService: AuthService) {
    // Observar fallos de refresh para mostrar mensajes específicos
    LaunchedEffect(Unit) {
        authService.tokenRefreshManager.onRefreshFailed.collect { reason ->
            when (reason) {
                is RefreshFailureReason.NetworkError -> {
                    snackbarHostState.showSnackbar(
                        message = "Error de conexión. Reintentando...",
                        duration = SnackbarDuration.Short
                    )
                }
                is RefreshFailureReason.TokenExpired,
                is RefreshFailureReason.TokenRevoked -> {
                    // onSessionExpired se emitirá automáticamente
                    // Este collect es solo para mostrar mensaje adicional
                    snackbarHostState.showSnackbar(
                        message = "Tu sesión ha expirado",
                        duration = SnackbarDuration.Short
                    )
                }
                else -> { /* Otros casos */ }
            }
        }
    }
}
```

---

## 🧪 Testing

### Test de Auto-Refresh

```kotlin
@Test
fun `repository handles 401 with auto-refresh successfully`() = runTest {
    // Setup
    val mockEngine = MockEngine { request ->
        when (request.url.encodedPath) {
            "/api/data" -> {
                // Primera llamada: 401
                if (requestCount == 0) {
                    respond(
                        content = "Unauthorized",
                        status = HttpStatusCode.Unauthorized
                    )
                } else {
                    // Retry después de refresh: 200
                    respond(
                        content = """{"data": "success"}""",
                        status = HttpStatusCode.OK
                    )
                }
            }
            "/auth/refresh" -> {
                // Refresh exitoso
                respond(
                    content = """{"access_token": "new_token", "expires_in": 3600}""",
                    status = HttpStatusCode.OK
                )
            }
            else -> error("Unexpected request: ${request.url}")
        }
    }
    
    val authService = AuthServiceFactory.createForTesting()
    val httpClient = HttpClientFactory.createWithAutoRefresh(authService)
    
    // Ejecutar
    val response = httpClient.get("https://api.edugo.com/api/data")
    
    // Verificar
    assertEquals(HttpStatusCode.OK, response.status)
}
```

---

## 📝 Notas Importantes

### 1. **No uses `AuthInterceptor.autoRefresh = true`**

El auto-refresh ahora lo maneja `HttpCallValidator` dentro de `HttpClientFactory.createWithAutoRefresh()`. Si usas `AuthInterceptor`, configúralo sin auto-refresh:

```kotlin
// ❌ NO HACER (conflicto)
val authInterceptor = AuthInterceptor(
    tokenProvider = authService,
    autoRefresh = true  // ❌ Conflicto con HttpCallValidator
)

// ✅ CORRECTO (si necesitas AuthInterceptor para otros casos)
val authInterceptor = AuthInterceptor(
    tokenProvider = authService,
    autoRefresh = false  // HttpCallValidator lo maneja
)
```

### 2. **Thread-Safety Garantizada**

Si tienes 10 requests simultáneos y todos reciben 401:
- Solo se ejecuta **1 refresh real**
- Los 10 requests esperan el resultado
- Los 10 reintenta con el mismo nuevo token

### 3. **onSessionExpired vs onRefreshFailed**

| Flow | Cuándo se emite | Acción UI |
|------|----------------|-----------|
| `onSessionExpired` | Token expiró/revocado de forma irrecuperable | Navegar a login |
| `onRefreshFailed` | Cualquier fallo de refresh (incluye errores de red) | Opcional: mostrar mensaje |

**Recomendación**: Solo observa `onSessionExpired` para navegación. `onRefreshFailed` es para debugging/métricas.

---

## 🎯 Checklist de Integración

Verifica que hayas completado:

- [ ] HttpClient creado con `createWithAutoRefresh()`
- [ ] `onSessionExpired` observado en la UI principal
- [ ] Navigation a login cuando la sesión expira
- [ ] Tests de auto-refresh implementados (opcional pero recomendado)
- [ ] Logging configurado apropiadamente (INFO en dev, NONE en prod)

---

## 🆘 Troubleshooting

### Problema: "Refresh loop infinito"

**Causa**: El endpoint de refresh también retorna 401.

**Solución**: Verifica que el endpoint `/auth/refresh` esté excluido del auto-refresh o que el backend acepte el refresh token correctamente.

### Problema: "onSessionExpired no se emite"

**Causa**: No estás observando el flow en el scope correcto.

**Solución**: Asegúrate de colectar en `LaunchedEffect(Unit)` en tu composable principal.

### Problema: "Multiple refreshes simultáneos"

**Causa**: Estás usando múltiples HttpClients con auto-refresh.

**Solución**: Usa una sola instancia singleton de HttpClient en tu DI.

---

## 📚 Referencias

- [AUTO_REFRESH_401.md](./AUTO_REFRESH_401.md) - Documentación técnica detallada
- [TokenRefreshManager](../../auth/token/TokenRefreshManager.kt) - Implementación del manager
- [HttpClientFactory](../../network/HttpClientFactory.kt) - Factory con auto-refresh

---

**¡Todo listo! Tu app ahora maneja refresh de tokens automáticamente. 🎉**
