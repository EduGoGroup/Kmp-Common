# Auto-Refresh en Responses 401

Este documento describe cómo implementar auto-refresh automático de tokens cuando el backend retorna 401 Unauthorized usando **HttpCallValidator de Ktor**.

## Decisión de Diseño: HttpCallValidator vs Custom Interceptor

Hemos elegido usar `HttpCallValidator` de Ktor en lugar de crear un interceptor custom porque:

✅ **Ventajas**:
- Es la forma idiomática de Ktor para manejar errores HTTP
- No requiere modificar la arquitectura de `Interceptor` existente
- Permite retry de requests de forma nativa
- Mejor integración con el cliente HTTP

❌ **Desventaja de Interceptor Custom**:
- `Interceptor.interceptResponse()` actual es read-only (no puede modificar response)
- Requeriría breaking changes en toda la arquitectura

---

## Implementación con HttpCallValidator

### 1. Crear el HttpClient con Auto-Refresh

```kotlin
package com.edugo.app.network

import com.edugo.test.module.auth.service.AuthService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.http.*

/**
 * Crea un HttpClient con auto-refresh de tokens en 401.
 *
 * Cuando un request retorna 401:
 * 1. Intenta refrescar el token usando AuthService
 * 2. Si el refresh es exitoso, retry el request original con el nuevo token
 * 3. Si el refresh falla, propaga el 401 original
 */
fun createHttpClientWithAutoRefresh(authService: AuthService): HttpClient {
    return HttpClient {
        install(HttpCallValidator) {
            handleResponseExceptionWithRequest { exception, request ->
                // Solo manejar 401 Unauthorized
                val clientException = exception as? ResponseException
                if (clientException?.response?.status != HttpStatusCode.Unauthorized) {
                    return@handleResponseExceptionWithRequest
                }
                
                // Intentar refresh del token
                val newToken = authService.tokenRefreshManager.forceRefresh().getOrNull()
                
                if (newToken != null) {
                    // Retry el request con el nuevo token
                    request.headers {
                        remove(HttpHeaders.Authorization)
                        append(HttpHeaders.Authorization, "Bearer ${newToken.token}")
                    }
                } else {
                    // Refresh falló, propagar el 401 original
                    // El AuthService ya emitió onSessionExpired si corresponde
                    throw clientException
                }
            }
        }
        
        // Otros plugins (Json, ContentNegotiation, etc.)
    }
}
```

### 2. Integrar con AuthInterceptor

El `AuthInterceptor` existente sigue funcionando para agregar el token inicial:

```kotlin
val authInterceptor = AuthInterceptor(
    tokenProvider = authService,  // AuthService implementa TokenProvider
    autoRefresh = false  // Deshabilitamos auto-refresh aquí porque HttpCallValidator lo maneja
)
```

### 3. Flujo Completo

```
┌─────────────────────────────────────────────────────────────────────┐
│                         REQUEST FLOW                                 │
└─────────────────────────────────────────────────────────────────────┘

1. AuthInterceptor agrega header Authorization: Bearer <token>
   ↓
2. Request se envía al servidor
   ↓
3. Servidor retorna 401 Unauthorized (token expirado)
   ↓
4. HttpCallValidator intercepta el 401
   ↓
5. Llama a tokenRefreshManager.forceRefresh()
   ├─ Si éxito: Retry request con nuevo token → Response OK
   └─ Si falla: Propaga 401 → AuthService emite onSessionExpired
```

---

## Ejemplo de Uso en la Aplicación

### Configuración en DI (Koin)

```kotlin
package com.edugo.app.di

import org.koin.dsl.module

val networkModule = module {
    // AuthService
    single<AuthService> {
        AuthServiceFactory.createWithCustomComponents(
            repository = get(),
            storage = get(),
            scope = get()  // CoroutineScope from app
        )
    }
    
    // HttpClient con auto-refresh
    single<HttpClient> {
        createHttpClientWithAutoRefresh(authService = get())
    }
    
    // AuthInterceptor (opcional, para requests manuales)
    single<AuthInterceptor> {
        AuthInterceptor(
            tokenProvider = get<AuthService>(),
            autoRefresh = false  // HttpCallValidator lo maneja
        )
    }
}
```

### Observar Expiración de Sesión en la UI

```kotlin
@Composable
fun MainScreen(
    authService: AuthService = koinInject(),
    navigator: Navigator = rememberNavigator()
) {
    // Observar expiración de sesión
    LaunchedEffect(Unit) {
        authService.onSessionExpired.collect {
            // Sesión expiró y no se pudo renovar, navegar a login
            navigator.navigate(Screen.Login) {
                popUpTo(Screen.Main) { inclusive = true }
            }
        }
    }
    
    // ... resto de la UI
}
```

---

## Testing del Auto-Refresh

### Test 1: Request 401 → Auto-Refresh Exitoso → Retry Exitoso

```kotlin
@Test
fun `401 triggers auto-refresh and retries successfully`() = runTest {
    // Setup
    val authService = AuthServiceFactory.createForTesting()
    val httpClient = createHttpClientWithAutoRefresh(authService)
    
    // Login inicial
    authService.login(LoginCredentials("test@edugo.com", "password123"))
    
    // Simular token expirado en el servidor (mock)
    val mockServer = MockWebServer()
    mockServer.enqueue(MockResponse().setResponseCode(401))  // Primera respuesta: 401
    mockServer.enqueue(MockResponse().setResponseCode(200).setBody("Success"))  // Retry: OK
    
    // Ejecutar request
    val response = httpClient.get("${mockServer.url}/api/data")
    
    // Verificar
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals(2, mockServer.requestCount)  // 2 requests: original + retry
}
```

### Test 2: Request 401 → Refresh Falla → onSessionExpired Emitido

```kotlin
@Test
fun `401 with failed refresh emits onSessionExpired`() = runTest {
    // Setup con refresh que falla
    val stubRepo = StubAuthRepository().apply {
        simulateNetworkError = true  // Simular fallo en refresh
    }
    val authService = AuthServiceFactory.createWithCustomComponents(
        repository = stubRepo,
        storage = SafeEduGoStorage.wrap(EduGoStorage.create())
    )
    
    authService.login(LoginCredentials("test@edugo.com", "password123"))
    
    // Colectar eventos de sesión expirada
    val sessionExpiredEvents = mutableListOf<Unit>()
    val job = launch {
        authService.onSessionExpired.toList(sessionExpiredEvents)
    }
    
    // Simular 401
    val httpClient = createHttpClientWithAutoRefresh(authService)
    try {
        httpClient.get("https://api.example.com/data")
    } catch (e: ResponseException) {
        // Esperamos 401
    }
    
    // Verificar que se emitió onSessionExpired
    advanceUntilIdle()
    assertEquals(1, sessionExpiredEvents.size)
    
    job.cancel()
}
```

---

## Consideraciones Importantes

### 1. **Thread-Safety**

El `TokenRefreshManager` ya garantiza que múltiples requests concurrentes que reciben 401 resultan en un solo refresh real. Todas las requests esperan el resultado del refresh y reintent con el mismo nuevo token.

### 2. **Evitar Loops Infinitos**

HttpCallValidator solo reintenta una vez por request. Si el retry también retorna 401, no se vuelve a intentar refresh (comportamiento por defecto de Ktor).

### 3. **Configuración de Retry**

El TokenRefreshManager tiene su propia configuración de retry con exponential backoff para errores de red durante el refresh. Esto es independiente del retry del HttpCallValidator.

---

## Migración desde AuthInterceptor

Si actualmente tienes un `AuthInterceptor` con auto-refresh:

1. ✅ **Mantén** `AuthInterceptor` para agregar el header Authorization inicial
2. ✅ **Desactiva** auto-refresh en el interceptor: `autoRefresh = false`
3. ✅ **Agrega** HttpCallValidator como se mostró arriba
4. ✅ **Elimina** lógica de refresh del interceptor

---

## Resumen

| Componente | Responsabilidad |
|------------|-----------------|
| `AuthInterceptor` | Agregar header Authorization con token actual |
| `HttpCallValidator` | Detectar 401, trigger refresh, retry request |
| `TokenRefreshManager` | Ejecutar refresh con sincronización y retry |
| `AuthService` | Coordinar todo y emitir onSessionExpired |

Esta arquitectura separa claramente las responsabilidades y aprovecha las capacidades nativas de Ktor.
