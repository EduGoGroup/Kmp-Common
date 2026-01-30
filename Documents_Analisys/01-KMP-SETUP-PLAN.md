# Plan de Setup: edugo-kmp-shared

> **Version del documento**: 2.0.0  
> **Ultima actualizacion**: 2026-01-02  
> **Estado**: Plan completo listo para implementacion

---

## Resumen Ejecutivo

Este documento define el plan completo para crear el proyecto **edugo-kmp-shared**, 
un proyecto Kotlin Multiplatform con multiples modulos independientes equivalente a `edugo-shared` de Go.

### Plataformas Objetivo
- **Android**: API 35+ (Android 15)
- **JVM Desktop**: Para aplicaciones de escritorio
- **Kotlin/JS**: Web (con preparacion para WASM futuro)

### Stack Tecnologico Base (NO NEGOCIABLE)

| Componente | Version | Notas |
|------------|---------|-------|
| **Kotlin** | 2.1.20 | K2 Compiler habilitado |
| **Gradle** | 8.11 | Con Version Catalogs |
| **JDK** | 17 LTS | Minimo requerido (compatible con Android minSdk 29 y KMP) |
| **Android compileSdk** | 35 | Android 15 |
| **Android targetSdk** | 35 | Android 15 |
| **Android minSdk** | 26 | Android 8.0 Oreo |
| **AGP** | 8.7.2 | Android Gradle Plugin |

---

## 1. Matriz de Compatibilidad de Dependencias

### 1.1 Versiones Verificadas (Enero 2026)

| Dependencia | Version | Kotlin 2.1.20 | Notas |
|-------------|---------|---------------|-------|
| **kotlinx-coroutines** | 1.10.2 | ✅ Compatible | Companion version para Kotlin 2.1.0 |
| **kotlinx-serialization** | 1.8.1 | ✅ Compatible | Requiere Kotlin 2.1.0+ |
| **kotlinx-datetime** | 0.6.2 | ✅ Compatible | Usar 0.6.x para compatibilidad |
| **Ktor** | 3.1.3 | ✅ Compatible | Soporte SSE, WASM-JS |
| **Kermit** | 2.0.4 | ✅ Compatible | Ultima version estable |
| **multiplatform-settings** | 1.3.0 | ✅ Compatible | Actualizado a Kotlin 2.1.0, AGP 8.7.2 |
| **kotlin.uuid** | stdlib | ✅ Nativo | Incluido en Kotlin 2.1+ (Experimental) |

### 1.2 Notas de Compatibilidad Importantes

1. **UUID Nativo**: La libreria `benasher44/uuid` fue archivada en mayo 2025. Kotlin 2.1+ incluye `kotlin.uuid.Uuid` nativo.

2. **kotlinx-datetime 0.7.x**: Elimina `kotlinx.datetime.Instant` a favor de `kotlin.time.Instant`. Para maxima compatibilidad, usar 0.6.2.

3. **Ktor 3.1.x**: Requiere kotlinx-coroutines 1.10.x y kotlinx-serialization 1.8.x.

4. **multiplatform-settings 1.3.0**: Actualizado a Kotlin 2.1.0, Gradle 8.11 y AGP 8.7.2.

---

## 2. Estructura Completa de Versiones (libs.versions.toml)

```toml
# gradle/libs.versions.toml
# edugo-kmp-shared - Version Catalog
# Ultima verificacion: 2026-01-02

[versions]
# Kotlin & Core
kotlin = "2.1.20"
kotlinx-coroutines = "1.10.2"
kotlinx-serialization = "1.8.1"
kotlinx-datetime = "0.6.2"

# Ktor
ktor = "3.1.3"

# Logging
kermit = "2.0.4"

# Storage
multiplatform-settings = "1.3.0"

# Android
android-gradle = "8.7.2"
android-compileSdk = "35"
android-minSdk = "26"
android-targetSdk = "35"

# Build
java-target = "17"
jvm-target = "17"

[libraries]
# ============================================
# Kotlinx Libraries
# ============================================

# Coroutines
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-swing = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-swing", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }

# Serialization
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-serialization-core = { module = "org.jetbrains.kotlinx:kotlinx-serialization-core", version.ref = "kotlinx-serialization" }

# DateTime
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }

# ============================================
# Ktor Client
# ============================================

# Core
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
ktor-client-auth = { module = "io.ktor:ktor-client-auth", version.ref = "ktor" }

# Serialization
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }

# Engines - Platform Specific
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-java = { module = "io.ktor:ktor-client-java", version.ref = "ktor" }
ktor-client-js = { module = "io.ktor:ktor-client-js", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }

# ============================================
# Logging
# ============================================
kermit = { module = "co.touchlab:kermit", version.ref = "kermit" }
kermit-crashlytics = { module = "co.touchlab:kermit-crashlytics", version.ref = "kermit" }

# ============================================
# Storage / Settings
# ============================================
multiplatform-settings = { module = "com.russhwolf:multiplatform-settings", version.ref = "multiplatform-settings" }
multiplatform-settings-no-arg = { module = "com.russhwolf:multiplatform-settings-no-arg", version.ref = "multiplatform-settings" }
multiplatform-settings-coroutines = { module = "com.russhwolf:multiplatform-settings-coroutines", version.ref = "multiplatform-settings" }
multiplatform-settings-serialization = { module = "com.russhwolf:multiplatform-settings-serialization", version.ref = "multiplatform-settings" }

# ============================================
# Testing
# ============================================
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotlin-test-junit = { module = "org.jetbrains.kotlin:kotlin-test-junit", version.ref = "kotlin" }

# ============================================
# Build Logic
# ============================================
kotlin-gradle-plugin = { module = "org.jetbrains.kotlin:kotlin-gradle-plugin", version.ref = "kotlin" }
android-gradle-plugin = { module = "com.android.tools.build:gradle", version.ref = "android-gradle" }

[bundles]
# Bundle para modulo network
ktor-common = [
    "ktor-client-core",
    "ktor-client-content-negotiation",
    "ktor-client-logging",
    "ktor-serialization-kotlinx-json"
]

# Bundle para modulo storage
settings-common = [
    "multiplatform-settings",
    "multiplatform-settings-coroutines"
]

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
android-library = { id = "com.android.library", version.ref = "android-gradle" }
android-application = { id = "com.android.application", version.ref = "android-gradle" }
```

---

## 3. Estructura del Proyecto

```
edugo-kmp-shared/
├── .github/
│   └── workflows/
│       ├── ci.yml                      # Build y tests
│       ├── release.yml                 # Crear releases
│       └── publish.yml                 # Publicar a Maven
│
├── gradle/
│   ├── libs.versions.toml              # Version Catalog
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── build-logic/                        # Convention Plugins
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       ├── kmp.library.gradle.kts      # Plugin base KMP
│       ├── kmp.android.gradle.kts      # Config Android
│       └── kmp.publishing.gradle.kts   # Config publicacion
│
├── modules/
│   ├── common/                         # EduGoCommon - Modulo base
│   ├── logger/                         # EduGoLogger
│   ├── network/                        # EduGoNetwork
│   ├── storage/                        # EduGoStorage
│   ├── auth/                           # EduGoAuth
│   ├── roles/                          # EduGoRoles
│   ├── models/                         # EduGoModels
│   ├── api/                            # EduGoAPI
│   └── analytics/                      # EduGoAnalytics
│
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── README.md
└── CHANGELOG.md
```

---

## 4. Archivos de Configuracion Raiz

### 4.1 settings.gradle.kts

```kotlin
// settings.gradle.kts
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "edugo-kmp-shared"

// Modulos
include(":modules:common")
include(":modules:logger")
include(":modules:network")
include(":modules:storage")
include(":modules:auth")
include(":modules:roles")
include(":modules:models")
include(":modules:api")
include(":modules:analytics")
```

### 4.2 build.gradle.kts (raiz)

```kotlin
// build.gradle.kts (root)
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
}

allprojects {
    group = "com.edugo.shared"
    version = "1.0.0-SNAPSHOT"
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
```

### 4.3 gradle.properties

```properties
# gradle.properties

# Project
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

# Kotlin
kotlin.code.style=official
kotlin.incremental=true
kotlin.incremental.multiplatform=true

# K2 Compiler
kotlin.experimental.tryK2=true

# KMP
kotlin.mpp.stability.nowarn=true
kotlin.mpp.androidGradlePluginCompatibility.nowarn=true
kotlin.mpp.enableCInteropCommonization=true
kotlin.mpp.applyDefaultHierarchyTemplate=true

# Android
android.useAndroidX=true
android.nonTransitiveRClass=true
android.defaults.buildfeatures.buildconfig=false
```

---

## 5. Convention Plugins (build-logic)

### 5.1 build-logic/settings.gradle.kts

```kotlin
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
```

### 5.2 build-logic/build.gradle.kts

```kotlin
plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.android.gradle.plugin)
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "edugo.kmp.library"
            implementationClass = "KmpLibraryPlugin"
        }
    }
}
```

### 5.3 build-logic/src/main/kotlin/kmp.library.gradle.kts

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

kotlin {
    // Android Target
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                    freeCompilerArgs.addAll(
                        "-opt-in=kotlin.RequiresOptIn",
                        "-opt-in=kotlin.uuid.ExperimentalUuidApi"
                    )
                }
            }
        }
        publishLibraryVariants("release")
    }
    
    // JVM Desktop Target
    jvm("desktop") {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                    freeCompilerArgs.addAll(
                        "-opt-in=kotlin.RequiresOptIn",
                        "-opt-in=kotlin.uuid.ExperimentalUuidApi"
                    )
                }
            }
        }
    }
    
    // JavaScript Target
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.library()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
            }
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        
        val jsMain by getting
        val jsTest by getting
    }
    
    explicitApi()
}

android {
    namespace = "com.edugo.shared.${project.name}"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        buildConfig = false
    }
}
```

---

## 6. Ejemplos de Codigo por Modulo

### 6.1 modules/common

#### build.gradle.kts

```kotlin
plugins {
    id("kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }
    }
}
```

#### ErrorCode.kt

```kotlin
package com.edugo.shared.common.errors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class ErrorCode {
    // Autenticacion
    @SerialName("AUTH_UNAUTHORIZED") UNAUTHORIZED,
    @SerialName("AUTH_TOKEN_EXPIRED") TOKEN_EXPIRED,
    @SerialName("AUTH_TOKEN_INVALID") TOKEN_INVALID,
    @SerialName("AUTH_INVALID_CREDENTIALS") INVALID_CREDENTIALS,
    
    // Red
    @SerialName("NET_ERROR") NETWORK_ERROR,
    @SerialName("NET_TIMEOUT") TIMEOUT,
    @SerialName("NET_SERVER_ERROR") SERVER_ERROR,
    
    // Validacion
    @SerialName("VAL_FAILED") VALIDATION_FAILED,
    @SerialName("VAL_NOT_FOUND") NOT_FOUND,
    @SerialName("VAL_CONFLICT") CONFLICT,
    @SerialName("VAL_BAD_REQUEST") BAD_REQUEST,
    
    // Permisos
    @SerialName("PERM_FORBIDDEN") FORBIDDEN,
    @SerialName("PERM_INSUFFICIENT") INSUFFICIENT_PERMISSIONS,
    
    // General
    @SerialName("UNKNOWN") UNKNOWN
}
```

#### AppError.kt

```kotlin
package com.edugo.shared.common.errors

import kotlinx.serialization.Serializable

@Serializable
public data class AppError(
    val code: ErrorCode,
    override val message: String,
    val details: Map<String, String>? = null,
    val cause: String? = null
) : Exception(message) {
    
    public companion object {
        public fun network(message: String, cause: Throwable? = null): AppError =
            AppError(ErrorCode.NETWORK_ERROR, message, cause = cause?.message)
        
        public fun unauthorized(message: String = "No autorizado"): AppError =
            AppError(ErrorCode.UNAUTHORIZED, message)
        
        public fun tokenExpired(message: String = "Token expirado"): AppError =
            AppError(ErrorCode.TOKEN_EXPIRED, message)
        
        public fun validation(message: String, details: Map<String, String>? = null): AppError =
            AppError(ErrorCode.VALIDATION_FAILED, message, details)
        
        public fun notFound(resource: String): AppError =
            AppError(ErrorCode.NOT_FOUND, "$resource no encontrado")
        
        public fun timeout(message: String = "Tiempo de espera agotado"): AppError =
            AppError(ErrorCode.TIMEOUT, message)
        
        public fun unknown(cause: Throwable): AppError =
            AppError(ErrorCode.UNKNOWN, cause.message ?: "Error desconocido", cause = cause.toString())
    }
}
```

#### Result.kt

```kotlin
package com.edugo.shared.common.types

import com.edugo.shared.common.errors.AppError

public sealed class Result<out T> {
    
    public data class Success<T>(val data: T) : Result<T>()
    public data class Failure(val error: AppError) : Result<Nothing>()
    
    public val isSuccess: Boolean get() = this is Success
    public val isFailure: Boolean get() = this is Failure
    
    public fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
    
    public fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw error
    }
    
    public inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }
    
    public inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Failure -> this
    }
    
    public inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    public inline fun onFailure(action: (AppError) -> Unit): Result<T> {
        if (this is Failure) action(error)
        return this
    }
    
    public companion object {
        public inline fun <T> runCatching(block: () -> T): Result<T> = try {
            Success(block())
        } catch (e: AppError) {
            Failure(e)
        } catch (e: Exception) {
            Failure(AppError.unknown(e))
        }
        
        public fun <T> success(value: T): Result<T> = Success(value)
        public fun failure(error: AppError): Result<Nothing> = Failure(error)
    }
}
```

### 6.2 modules/logger

```kotlin
// EduGoLogger.kt - Wrapper de Kermit
package com.edugo.shared.logger

import co.touchlab.kermit.Logger as KermitLogger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter

public class EduGoLogger private constructor(
    private val kermit: KermitLogger,
    private val defaultTag: String
) : Logger {
    
    override fun debug(message: String, tag: String?, throwable: Throwable?) {
        getLogger(tag).d(throwable) { message }
    }
    
    override fun info(message: String, tag: String?, throwable: Throwable?) {
        getLogger(tag).i(throwable) { message }
    }
    
    override fun warn(message: String, tag: String?, throwable: Throwable?) {
        getLogger(tag).w(throwable) { message }
    }
    
    override fun error(message: String, tag: String?, throwable: Throwable?) {
        getLogger(tag).e(throwable) { message }
    }
    
    override fun withTag(tag: String): Logger = 
        EduGoLogger(kermit.withTag(tag), tag)
    
    private fun getLogger(tag: String?): KermitLogger =
        if (tag != null && tag != defaultTag) kermit.withTag(tag) else kermit
    
    public companion object {
        private var instance: EduGoLogger? = null
        
        public fun getInstance(tag: String = "EduGo"): Logger {
            return instance ?: synchronized(this) {
                instance ?: createInstance(tag).also { instance = it }
            }
        }
        
        private fun createInstance(tag: String): EduGoLogger {
            val config = loggerConfigInit(platformLogWriter(), Severity.Debug)
            return EduGoLogger(KermitLogger(config, tag), tag)
        }
        
        public fun create(tag: String): Logger = getInstance().withTag(tag)
    }
}
```

### 6.3 modules/network

```kotlin
// EduGoHttpClient.kt
package com.edugo.shared.network

import com.edugo.shared.common.errors.AppError
import com.edugo.shared.common.errors.ErrorCode
import com.edugo.shared.common.types.Result
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

public class EduGoHttpClient(
    private val baseUrl: String,
    private val tokenProvider: (suspend () -> String?)? = null
) {
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }
    
    private val client: HttpClient = createPlatformHttpClient().config {
        install(ContentNegotiation) { json(this@EduGoHttpClient.json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    }
    
    public suspend inline fun <reified T> get(
        path: String,
        queryParams: Map<String, String> = emptyMap()
    ): Result<T> = execute { client.get(path) { queryParams.forEach { parameter(it.key, it.value) } } }
    
    public suspend inline fun <reified T, reified B> post(
        path: String,
        body: B
    ): Result<T> = execute { client.post(path) { setBody(body) } }
    
    @PublishedApi
    internal suspend inline fun <reified T> execute(
        block: suspend () -> HttpResponse
    ): Result<T> = try {
        val response = block()
        if (response.status.isSuccess()) Result.success(response.body())
        else Result.failure(mapHttpError(response))
    } catch (e: AppError) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(AppError.unknown(e))
    }
    
    @PublishedApi
    internal fun mapHttpError(response: HttpResponse): AppError = when (response.status) {
        HttpStatusCode.Unauthorized -> AppError(ErrorCode.UNAUTHORIZED, "No autorizado")
        HttpStatusCode.Forbidden -> AppError(ErrorCode.FORBIDDEN, "Acceso denegado")
        HttpStatusCode.NotFound -> AppError(ErrorCode.NOT_FOUND, "Recurso no encontrado")
        else -> AppError(ErrorCode.UNKNOWN, "HTTP ${response.status.value}")
    }
    
    public fun close() { client.close() }
}

// Expect/Actual pattern para engines
public expect fun createPlatformHttpClient(): HttpClient
```

### 6.4 modules/roles

> **IMPORTANTE**: Los roles DEBEN coincidir exactamente con el backend (edugo-shared/common/types/enum/role.go)

```kotlin
// SystemRole.kt
package com.edugo.shared.roles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Roles del sistema EduGo.
 * DEBEN coincidir exactamente con el backend: admin, teacher, student, guardian
 */
@Serializable
public enum class SystemRole {
    @SerialName("admin") ADMIN,
    @SerialName("teacher") TEACHER,
    @SerialName("student") STUDENT,
    @SerialName("guardian") GUARDIAN;
    
    public val displayName: String get() = when (this) {
        ADMIN -> "Administrador"
        TEACHER -> "Profesor"
        STUDENT -> "Estudiante"
        GUARDIAN -> "Acudiente"
    }
    
    /** Nivel jerárquico del rol (mayor = más privilegios) */
    public val level: Int get() = when (this) {
        ADMIN -> 100
        TEACHER -> 50
        STUDENT -> 30
        GUARDIAN -> 20
    }
    
    public val permissions: Set<Permission> get() = RolePermissions.getPermissions(this)
    
    public fun hasPermission(permission: Permission): Boolean = permission in permissions
    
    /** Verifica si este rol tiene al menos el nivel del rol especificado */
    public fun hasAtLeast(role: SystemRole): Boolean = this.level >= role.level
    
    public companion object {
        /** Valida si un string corresponde a un rol válido */
        public fun fromString(value: String): SystemRole? = 
            entries.find { it.name.equals(value, ignoreCase = true) }
    }
}

// Permission.kt
@Serializable
public enum class Permission {
    @SerialName("materials:view") VIEW_MATERIALS,
    @SerialName("materials:upload") UPLOAD_MATERIALS,
    @SerialName("materials:edit") EDIT_MATERIALS,
    @SerialName("materials:delete") DELETE_MATERIALS,
    @SerialName("quizzes:take") TAKE_QUIZZES,
    @SerialName("quizzes:create") CREATE_QUIZZES,
    @SerialName("quizzes:grade") GRADE_QUIZZES,
    @SerialName("progress:view_own") VIEW_OWN_PROGRESS,
    @SerialName("progress:view_students") VIEW_STUDENT_PROGRESS,
    @SerialName("users:view") VIEW_USERS,
    @SerialName("users:manage") MANAGE_USERS,
    @SerialName("reports:view") VIEW_REPORTS,
    @SerialName("reports:export") EXPORT_REPORTS
}

// RolePermissions.kt
public object RolePermissions {
    private val permissionMap = mapOf(
        SystemRole.STUDENT to setOf(
            Permission.VIEW_MATERIALS, 
            Permission.TAKE_QUIZZES, 
            Permission.VIEW_OWN_PROGRESS
        ),
        SystemRole.GUARDIAN to setOf(
            Permission.VIEW_OWN_PROGRESS
        ),
        SystemRole.TEACHER to setOf(
            Permission.VIEW_MATERIALS, 
            Permission.UPLOAD_MATERIALS, 
            Permission.EDIT_MATERIALS,
            Permission.CREATE_QUIZZES, 
            Permission.GRADE_QUIZZES, 
            Permission.VIEW_STUDENT_PROGRESS, 
            Permission.VIEW_REPORTS
        ),
        SystemRole.ADMIN to Permission.entries.toSet() // Todos los permisos
    )
    
    public fun getPermissions(role: SystemRole): Set<Permission> = permissionMap[role] ?: emptySet()
}
```

---

## 7. Grafo de Dependencias entre Modulos

```
                              ┌─────────┐
                              │ common  │ (Base)
                              └────┬────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         │                         │                         │
         ▼                         ▼                         ▼
    ┌─────────┐              ┌─────────┐              ┌─────────┐
    │ logger  │              │  roles  │              │ storage │
    └────┬────┘              └────┬────┘              └────┬────┘
         │                        │                        │
         │                        ▼                        │
         │                   ┌─────────┐                   │
         │                   │ models  │◄──────────────────┤
         │                   └────┬────┘                   │
         │                        │                        │
         ▼                        │                        ▼
    ┌─────────┐                   │                   ┌─────────┐
    │ network │◄──────────────────┼───────────────────│  auth   │
    └────┬────┘                   │                   └────┬────┘
         │                        │                        │
         └────────────────────────┼────────────────────────┘
                                  ▼
                             ┌─────────┐
                             │   api   │
                             └────┬────┘
                                  │
                                  ▼
                           ┌───────────┐
                           │ analytics │
                           └───────────┘
```

| Modulo | Dependencias Internas | Dependencias Externas |
|--------|----------------------|----------------------|
| common | - | kotlinx-serialization, kotlinx-datetime |
| logger | common | kermit |
| roles | common | kotlinx-serialization |
| storage | common | multiplatform-settings |
| models | common, roles | kotlinx-serialization, kotlinx-datetime |
| network | common, logger | ktor-client |
| auth | common, storage, logger | kotlinx-serialization, kotlinx-datetime |
| api | common, logger, network, auth, models | kotlinx-serialization |
| analytics | common, logger | kotlinx-serialization, kotlinx-datetime |

---

## 8. CI/CD con GitHub Actions

### .github/workflows/ci.yml

```yaml
name: CI

on:
  push:
    branches: [main, dev]
  pull_request:
    branches: [main, dev]

env:
  GRADLE_OPTS: "-Dorg.gradle.jvmargs=-Xmx4g"

jobs:
  build:
    name: Build & Test
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
      
      - name: Build All Modules
        run: ./gradlew build --no-daemon
      
      - name: Run All Tests
        run: ./gradlew allTests --no-daemon

  android-check:
    name: Android Lint
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew lintDebug --no-daemon

  js-tests:
    name: JavaScript Tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew jsBrowserTest --no-daemon
```

---

## 9. Comandos Utiles de Gradle

```bash
# Build completo
./gradlew build

# Solo compilar sin tests
./gradlew assemble

# Ejecutar todos los tests
./gradlew allTests

# Tests por plataforma
./gradlew androidTest
./gradlew desktopTest
./gradlew jsBrowserTest

# Limpiar
./gradlew clean

# Verificar dependencias
./gradlew dependencies

# Publicar localmente
./gradlew publishToMavenLocal
```

---

## 10. Fuentes y Referencias

### Documentacion Oficial
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Kotlin 2.1.20 Release Notes](https://kotlinlang.org/docs/whatsnew2120.html)
- [Ktor 3.1 Documentation](https://ktor.io/docs/releases.html)
- [Kermit Logging](https://kermit.touchlab.co/docs/)
- [Multiplatform Settings](https://github.com/russhwolf/multiplatform-settings)

### Versiones Verificadas (Enero 2026)
- kotlinx-coroutines: 1.10.2
- kotlinx-serialization: 1.8.1
- kotlinx-datetime: 0.6.2
- Ktor: 3.1.3
- Kermit: 2.0.4
- multiplatform-settings: 1.3.0
- kotlin.uuid: Nativo en Kotlin 2.1+

---

**Estado**: Plan completo listo para implementacion  
**Version del documento**: 2.0.0  
**Ultima actualizacion**: 2026-01-02  
**Autor**: Agente KMP
