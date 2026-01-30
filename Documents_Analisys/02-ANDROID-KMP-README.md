# EduGo Android Modules - Kotlin Multiplatform

> **Versión**: 2.0.0  
> **Stack**: Android 8.0+ (API 26) | Kotlin 2.1.20 | Gradle 8.11 | JDK 17 LTS  
> **Status**: 🚧 En Planificación

---

Datos de entrada

* Ruta del Proyecto
  La actual
  /Users/jhoanmedina/source/EduGo/EduUI/Modules/Kmp-Common  

* Ruta de los comando slash
  /Users/jhoanmedina/source/EduGo/EduUI/Modules/Kmp-Common  

* Nivel del proyecto
  Enterprite



## 📋 Resumen Ejecutivo

Este proyecto define e implementa un **Kotlin Multiplatform (KMP)** modular con 8 módulos base para las aplicaciones Android de EduGo. La arquitectura está diseñada para máxima reutilización entre Android, JVM Desktop y preparación para Kotlin/JS.

### Objetivo Principal

Crear una biblioteca compartida de módulos Kotlin que:

- ✅ **Elimine duplicación** de código entre apps Android
- ✅ **Establezca estándares** de arquitectura y buenas prácticas
- ✅ **Aprovecha Kotlin 2.1.20** con K2 compiler y características modernas
- ✅ **Garantice type-safety** con manejo robusto de errores
- ✅ **Facilite testing** con módulos independientes y testables
- ✅ **Soporte multiplatforma** con posibilidad de compartir con JVM y JS

### Arquitectura de 4 Capas (TIER)

**TIER 0**: EduGoCommon (Base sin dependencias)  
**TIER 1**: EduGoLogger, EduGoModels (Core)  
**TIER 2**: EduGoNetwork, EduGoStorage (Infraestructura)  
**TIER 3**: EduGoAuth, EduGoRoles (Dominio)  
**TIER 4**: EduGoAPI, EduGoAnalytics (Aplicación)  

---

## 🎯 Reglas Clave

### Versiones NO Negociables

- **Kotlin 2.1.20**: K2 Compiler habilitado, características modernas
- **Gradle 8.11**: Con Version Catalogs y build logic
- **JDK 17 LTS**: Minimo requerido para desarrollo (compatible con Android minSdk 29 y KMP)
- **Android API 26+** (Android 8.0 Oreo): minSdk
- **Android API 35** (Android 15): targetSdk y compileSdk
- **AGP 8.7.2**: Android Gradle Plugin versión compatible

### Dependencias Base NO Negociables

```
✅ Obligatorias para KMP:
  • kotlinx-coroutines 1.10.2
  • kotlinx-serialization 1.8.1
  • Ktor Client 3.1.3
  • multiplatform-settings 1.3.0
  • Kermit 2.0.4 (logging)

✅ Específicas de Android:
  • AndroidX Core 1.15+
  • AndroidX Lifecycle 2.8+
  • AndroidX Activity 1.9+

❌ PROHIBIDAS (legacy):
  • Coroutines sin scope control
  • SerializationJson directo (usar Ktor)
  • SharedPreferences directo (usar multiplatform-settings)
  • Callback-based APIs antiguas
```

### Orden Estricto: TIER 0 → 1 → 2 → 3 → 4

Nunca implementar un tier sin tener completo el anterior.

### Alineación Backend

Los roles DEBEN coincidir exactamente: `admin`, `teacher`, `student`, `guardian`.

---

## 🔧 Flujo de Desarrollo

**Estructura de commits**: `[TIER-X] Módulo: Descripción`

**Definición de Done**: 
- Código + tests (80%+)
- Documentación técnica
- Build exitoso en Android API 26, 30, 35
- ktlint limpio (si aplica)

**Branching**: `main` (release) → `develop` (integración) → `feature/tierX-modulo` (desarrollo)

**Code Review**: 
- Verificar tier dependencies
- Validar API compatibility Android 26+
- Tests coverage mínimo 80%
- Manejo de errores con AppError
- Documentación completa

---

## 🏗️ Estándares de Desarrollo Kotlin/Android

### 1. Arquitectura Limpia (Clean Architecture)

**Estructura por módulo**:

```
module-name/
├── src/
│   ├── commonMain/kotlin/
│   │   ├── com/edugo/module/
│   │   │   ├── api/                # APIs públicas
│   │   │   │   ├── models/         # DTOs y domain models
│   │   │   │   ├── managers/       # Facades públicas
│   │   │   │   └── repositories/   # Interfaces de repositorio
│   │   │   ├── domain/             # Lógica de negocio
│   │   │   │   ├── entities/       # Entidades del dominio
│   │   │   │   └── usecases/       # Casos de uso
│   │   │   └── internal/           # Implementación interna
│   │   │       ├── services/       # Servicios concretos
│   │   │       ├── repositories/   # Implementaciones repo
│   │   │       ├── mappers/        # Transformación de datos
│   │   │       └── extensions/     # Extensiones privadas
│   │
│   ├── androidMain/kotlin/
│   │   └── com/edugo/module/
│   │       ├── android/
│   │       │   └── di/             # DI específico Android
│   │       └── extensions/         # Extensiones Android
│   │
│   ├── commonTest/kotlin/
│   │   └── com/edugo/module/
│   │       ├── stubs/              # Stubs para testing
│   │       ├── fixtures/           # Datos de prueba
│   │       └── *Tests.kt           # Tests unitarios
│   │
│   └── androidTest/kotlin/
│       └── com/edugo/module/
│           └── *AndroidTests.kt    # Tests de integración Android
│
├── build.gradle.kts                # Config del módulo
└── README.md                        # Documentación local
```

**Principios**:

- Separación clara entre interfaces e implementación
- Las clases public solo exponen lo necesario
- Inversión de dependencias: dependencias inyectadas, no creadas
- Sin referencias circulares entre módulos
- Una sola responsabilidad por clase/interface

### 2. Patterns: Interface-First Design

**Patrón obligatorio**: Protocol-Oriented Design (usando Interfaces)

```kotlin
// 1. Definir interface primero (contrato)
interface UserRepositoryContract {
    suspend fun fetchUser(id: String): User
    suspend fun saveUser(user: User)
}

// 2. Crear stub para testing
class UserRepositoryStub(
    var mockUser: User? = null,
    var mockError: Exception? = null
) : UserRepositoryContract {
    var fetchUserCalled = false
    
    override suspend fun fetchUser(id: String): User {
        fetchUserCalled = true
        mockError?.let { throw it }
        return mockUser ?: User.stub()
    }
    
    override suspend fun saveUser(user: User) {
        // Stub implementation
    }
}

// 3. Implementar en código real
class UserRepository(
    private val httpClient: HttpClientContract,
    private val cache: CacheContract
) : UserRepositoryContract {
    override suspend fun fetchUser(id: String): User {
        return try {
            httpClient.get<User>("/users/$id")
        } catch (e: Exception) {
            cache.get<User>("user_$id") 
                ?: throw AppError.networkError("Failed to fetch user")
        }
    }
    
    override suspend fun saveUser(user: User) {
        httpClient.post("/users", user)
        cache.set("user_${user.id}", user)
    }
}

// 4. Inyectar en dependencias
class AuthManager(
    private val userRepository: UserRepositoryContract
) {
    suspend fun login(email: String, password: String): AuthTokens {
        val user = userRepository.fetchUser(email)
        return generateTokens(user)
    }
}
```

### 3. Manejo de Errores Estandarizado

```kotlin
// ErrorCode debe ser el mismo en backend y frontend
enum class ErrorCode(val value: Int) {
    // Network errors 1xxx
    NETWORK_TIMEOUT(1002),
    NETWORK_CONNECTION_FAILED(1003),
    
    // Auth errors 2xxx
    AUTH_TOKEN_EXPIRED(2001),
    AUTH_INVALID_CREDENTIALS(2002),
    AUTH_UNAUTHORIZED(2003),
    
    // Storage errors 4xxx
    STORAGE_WRITE_FAILED(4002),
    STORAGE_READ_FAILED(4003),
    
    // Generic error
    UNKNOWN(9999)
}

// Wrapper estándar para todos los errores
data class AppError(
    val code: ErrorCode,
    val message: String,
    val details: String? = null,
    val cause: Throwable? = null
) : Exception("[$code] $message${details?.let { ": $it" } ?: ""}")

// Extensiones para crear errores comunes
fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is IOException -> AppError(
        code = ErrorCode.NETWORK_CONNECTION_FAILED,
        message = "Network error occurred",
        cause = this
    )
    else -> AppError(
        code = ErrorCode.UNKNOWN,
        message = this.message ?: "Unknown error",
        cause = this
    )
}

// En funciones, siempre usar try-catch
suspend fun login(email: String, password: String): AuthTokens {
    return try {
        val response = httpClient.post<LoginResponse>("/login", mapOf(
            "email" to email,
            "password" to password
        ))
        response.tokens
    } catch (e: Exception) {
        throw e.toAppError()
    }
}
```

### 4. Coroutines y Concurrency

**Reglas obligatorias**:

```kotlin
// ✅ SIEMPRE usar viewModelScope o lifecycleScope en Android
class LoginViewModel(
    private val authManager: AuthManager
) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val tokens = authManager.login(email, password)
                _uiState.value = LoginUiState.Success(tokens)
            } catch (e: AppError) {
                _uiState.value = LoginUiState.Error(e)
            }
        }
    }
}

// ✅ En módulos comunes, usar coroutineScope o supervisorScope
suspend fun fetchUserData(userId: String): UserData = coroutineScope {
    val userDeferred = async { userRepository.fetchUser(userId) }
    val postsDeferred = async { postRepository.fetchUserPosts(userId) }
    
    UserData(
        user = userDeferred.await(),
        posts = postsDeferred.await()
    )
}

// ✅ Para operaciones en background, usar Dispatchers.Default/IO
private suspend fun expensiveOperation(): Result = withContext(Dispatchers.Default) {
    // Operación costosa de CPU
    computeSomeData()
}

// ❌ NUNCA usar GlobalScope
// ❌ NUNCA usar launch sin scope
// ❌ NUNCA usar blocking calls como runBlocking en production
// ❌ NUNCA usar CoroutineScope(EmptyCoroutineContext)
```

### 5. Serialization con kotlinx-serialization

**Patrón obligatorio**: Usar JSON via Ktor, NO directamente

```kotlin
// ✅ Correcto: Usar Ktor client con Content Negotiation
import io.ktor.client.call.body
import io.ktor.client.request.post

class UserRepository(private val httpClient: HttpClient) {
    suspend fun login(credentials: Credentials): LoginResponse {
        return httpClient.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(credentials)
        }.body<LoginResponse>()
    }
}

// Modelos siempre con @Serializable
@Serializable
data class User(
    @SerialName("user_id")
    val id: String,
    val name: String,
    val email: String,
    @SerialName("roles")
    val userRoles: List<String> = emptyList()
)

@Serializable
data class Credentials(
    val email: String,
    val password: String
)

// ❌ Incorrecto: NO usar Json.decodeFromString directamente
// ❌ NO usar Json.encodeToString sin Ktor wrapper
```

### 6. Dependency Injection Pattern

```kotlin
// Usar constructor injection siempre
class AuthManager(
    private val userRepository: UserRepositoryContract,
    private val tokenManager: TokenManagerContract,
    private val logger: Logger
) {
    suspend fun login(email: String, password: String): AuthTokens {
        logger.info("Login attempt for $email")
        // Implementation
    }
}

// Para Android, crear factory functions o usar Hilt
object AuthManagerFactory {
    fun create(
        userRepository: UserRepositoryContract = UserRepository.instance,
        tokenManager: TokenManagerContract = TokenManager.instance
    ): AuthManager {
        return AuthManager(userRepository, tokenManager, Logger.default)
    }
}

// En Android con AndroidX:
class LoginActivity : AppCompatActivity() {
    private val authManager: AuthManager by lazy {
        AuthManagerFactory.create()
    }
    
    // ...
}

// ❌ NUNCA crear instancias dentro de funciones
// ❌ NUNCA usar properties mutables sin control
```

### 7. Naming Conventions Kotlin

```kotlin
// ✅ Clases/Interfaces/Data: PascalCase
class AuthManager { }
interface UserRepositoryContract { }
data class User(val id: String)

// ✅ Funciones/variables: camelCase
fun fetchUserData() { }
var isAuthenticated: Boolean = false

// ✅ Constantes: camelCase (NO UPPER_CASE)
const val DEFAULT_TIMEOUT_MS = 30_000L
val defaultRetryCount = 3

// ✅ Interfaces: NameContract o NameProtocol
interface HttpClientContract { }
interface StorageProtocol { }

// ✅ Funciones async sin sufijo "Async"
suspend fun fetchUser(): User  // ✅ Bien
fun fetchUserAsync(): User     // ❌ Redundante

// ✅ Booleans: predicados is/has/should
var isLoading: Boolean
var hasError: Boolean
var shouldRetry: Boolean

// ✅ Métodos privados con guión bajo prefijo (opcional)
private fun _parseToken(): String { }
```

### 8. Tests Mínimos por Módulo

```
TIER 0 (EduGoCommon)          → 100% cobertura (no tests, es base)
TIER 1 (Logger, Models)        → 80% cobertura
TIER 2 (Network, Storage)      → 85% cobertura
TIER 3 (Auth, Roles)           → 85% cobertura
TIER 4 (API, Analytics)        → 80% cobertura
```

**Estructura de test file**:

```kotlin
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class UserRepositoryTests {
    private lateinit var sut: UserRepository  // System Under Test
    private lateinit var httpClientStub: HttpClientStub
    private lateinit var cacheStub: CacheStub
    
    @BeforeTest
    fun setUp() {
        httpClientStub = HttpClientStub()
        cacheStub = CacheStub()
        sut = UserRepository(httpClientStub, cacheStub)
    }
    
    @AfterTest
    fun tearDown() {
        // Cleanup if needed
    }
    
    // Arrange-Act-Assert (AAA pattern)
    @Test
    fun fetchUser_withValidId_returnsUser() = runTest {
        // Arrange
        val userId = "user123"
        val expectedUser = User.stub(id = userId)
        httpClientStub.mockResponse = expectedUser
        
        // Act
        val result = sut.fetchUser(userId)
        
        // Assert
        assertEquals(expectedUser, result)
        assertTrue(httpClientStub.fetchCalled)
    }
    
    @Test
    fun fetchUser_withNetworkError_throwsAppError() = runTest {
        // Arrange
        httpClientStub.mockError = IOException("Network error")
        
        // Act & Assert
        val exception = assertFailsWith<AppError> {
            sut.fetchUser("user123")
        }
        assertEquals(ErrorCode.NETWORK_CONNECTION_FAILED, exception.code)
    }
    
    @Test
    fun fetchUser_withCacheHit_returnsCachedUser() = runTest {
        // Arrange
        val userId = "user123"
        val cachedUser = User.stub(id = userId)
        cacheStub.mockData = cachedUser
        httpClientStub.mockError = IOException()
        
        // Act
        val result = sut.fetchUser(userId)
        
        // Assert
        assertEquals(cachedUser, result)
        assertFalse(httpClientStub.fetchCalled)
    }
}

// Stubs y Fixtures
data class User(val id: String, val name: String, val email: String) {
    companion object {
        fun stub(
            id: String = UUID.randomUUID().toString(),
            name: String = "Test User",
            email: String = "test@test.com"
        ) = User(id, name, email)
    }
}

class HttpClientStub : HttpClientContract {
    var mockResponse: Any? = null
    var mockError: Exception? = null
    var fetchCalled = false
    
    override suspend fun get(path: String): Any {
        fetchCalled = true
        mockError?.let { throw it }
        return mockResponse ?: throw Exception("No mock response")
    }
}
```

### 9. Documentación KDoc

**Obligatorio para APIs públicas**:

```kotlin
/**
 * Realiza el login de un usuario con sus credenciales.
 *
 * Esta función envía una solicitud a la API para autenticar
 * al usuario y obtener tokens de acceso.
 *
 * @param email El correo electrónico del usuario
 * @param password La contraseña del usuario
 * @return Los tokens de autenticación (access + refresh)
 * @throws AppError con código [ErrorCode.AUTH_INVALID_CREDENTIALS] si falla la autenticación
 * @throws AppError con código [ErrorCode.NETWORK_TIMEOUT] si hay timeout
 *
 * Ejemplo de uso:
 * ```kotlin
 * try {
 *     val tokens = authManager.login("user@test.com", "password123")
 *     println("Login exitoso: ${tokens.accessToken}")
 * } catch (e: AppError) {
 *     println("Error: ${e.message}")
 * }
 * ```
 *
 * @see AuthTokens
 * @see ErrorCode
 */
suspend fun login(email: String, password: String): AuthTokens {
    // Implementation
}
```

### 10. Estructura de Build (build.gradle.kts)

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    // Targets
    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    jvm()  // Para desktop/testing
    
    // Sourceset común
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.multiplatform.settings)
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.java)
            }
        }
    }
}

android {
    namespace = "com.edugo.modulename"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

---

## 📚 Documentación Completa

Para detalles de implementación, consultar:

- **[01-KMP-SETUP-PLAN.md](01-KMP-SETUP-PLAN.md)**: Plan técnico completo
  - Stack definitivo y versiones
  - Código de los 8 módulos
  - CI/CD y dependencias

---

## ⚙️ Configuración Manual - Android Studio

> **NOTA**: Aunque KMP es principalmente CLI/Gradle, algunas tareas requieren configuración en Android Studio que facilita el desarrollo.

### Requisito para Sprints y Tareas

**En TODOS los sprints/historias/tareas**, especificar:

1. **¿Requiere configuración manual en Android Studio?** SÍ / NO

2. **Si es SÍ**, como PRIMERA actividad (antes de código) debe crearse:
   - Documento: `CONFIGURACION_ANDROID_[MODULO].md`
   - Con pasos DETALLADOS y VERIFICABLES:
     - Qué módulos crear/modificar
     - Qué sourcesets configurar (commonMain, androidMain, commonTest)
     - Qué build configurations needed
     - Run configurations para testing
     - Paso de verificación (cómo confirmar que está correcto)

### Ejemplos de Configuración Manual Posible

```
✅ Generalmente accesible vía CLI → Sin documento especial:
  • Crear módulos KMP
  • Configurar build.gradle.kts
  • Crear sourcesets
  • Escribir tests comunes
  • Ejecutar tests
  • Crear commits/PRs

⚠️ Recomendable desde IDE pero también CLI:
  • Configurar Run Configurations
  • Refactoring de paquetes
  • Debugging con breakpoints
  • Análisis de cobertura visual
```

---

## 🎯 Estado Actual del Proyecto

| TIER | Módulo | Status | Cobertura | Docs |
|------|--------|--------|-----------|------|
| 0 | EduGoCommon | 🔴 Pendiente | - | - |
| 1 | EduGoLogger | 🔴 Pendiente | - | - |
| 1 | EduGoModels | 🔴 Pendiente | - | - |
| 2 | EduGoNetwork | 🔴 Pendiente | - | - |
| 2 | EduGoStorage | 🔴 Pendiente | - | - |
| 3 | EduGoRoles | 🔴 Pendiente | - | - |
| 3 | EduGoAuth | 🔴 Pendiente | - | - |
| 4 | EduGoAPI | 🔴 Pendiente | - | - |
| 4 | EduGoAnalytics | 🔴 Pendiente | - | - |

**Leyenda**: 🔴 Pendiente | 🟡 En Progreso | 🟢 Completo

---

## 🔑 Comparación: Kotlin KMP vs Swift

| Aspecto | Kotlin KMP | Swift SPM |
|--------|-----------|----------|
| **Lenguaje** | Kotlin 2.1.20 | Swift 6.2 |
| **Multiplatforma** | Android, JVM, JS | iOS, macOS, tvOS, watchOS, visionOS |
| **Build System** | Gradle 8.11 | SwiftPM |
| **Testing** | kotlinx-test, JUnit | XCTest |
| **Serialization** | kotlinx-serialization | Codable |
| **Networking** | Ktor | Network.framework |
| **Logging** | Kermit | os.Logger |
| **Storage** | multiplatform-settings | Keychain |
| **Concurrency** | Coroutines | async/await + Actors |
| **DI Pattern** | Constructor injection | DI Pattern |
| **Strict Mode** | Null safety by default | Strict concurrency |
| **Min Language Feature** | Kotlin 2.1.0 | Swift 6.2 |

---

## 📞 Contacto y Soporte

- **Team**: EduGo Mobile Team
- **Repo**: https://github.com/edugo/edugo-kmp-shared
- **Issues**: https://github.com/edugo/edugo-kmp-shared/issues
- **Slack**: #mobile-kotlin-modules

---

## 🚀 Próximos Pasos

1. **Setup inicial del proyecto KMP**
   - Crear estructura base con Gradle
   - Configurar Version Catalog
   - Crear convention plugins

2. **Módulo TIER 0: EduGoCommon**
   - Excepciones base (AppError, ErrorCode)
   - Extensiones comunes
   - Utilities

3. **Módulo TIER 1: EduGoLogger**
   - Wrapper de Kermit
   - Integración con Crashlytics (Android)

4. **Módulo TIER 1: EduGoModels**
   - DTOs de toda la aplicación
   - Domain models
   - Serialization setup

5. **Continuar con TIERs 2, 3, 4...**

---

**Última actualización**: Enero 2026  
**Versión del README**: 2.0.0  
**Alineación con**: Swift SPM 2.0.0
