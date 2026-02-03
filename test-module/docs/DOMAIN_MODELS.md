# Domain Models - Guía Completa de Serialización

**Versión:** 1.0.0
**Última actualización:** 2026-02-03
**Autor:** Sistema MCPEco - Workflow Development

---

## Tabla de Contenidos

1. [Introducción](#introducción)
2. [Modelos Implementados](#modelos-implementados)
3. [Role - Modelo de Roles](#role---modelo-de-roles)
4. [User - Modelo de Usuarios](#user---modelo-de-usuarios)
5. [AuthToken - Modelo de Autenticación](#authtoken---modelo-de-autenticación)
6. [Serialización y Compatibilidad Backend](#serialización-y-compatibilidad-backend)
7. [Validaciones](#validaciones)
8. [Schema Evolution](#schema-evolution)
9. [Ejemplos de Código](#ejemplos-de-código)
10. [Troubleshooting](#troubleshooting)
11. [Mejores Prácticas](#mejores-prácticas)
12. [Extensibilidad](#extensibilidad)

---

## Introducción

Este módulo implementa tres modelos de dominio serializables que demuestran el uso de **kotlinx-serialization** para comunicación con backends REST, compatibilidad cross-platform (JVM, Android, iOS), y patrones avanzados de validación y auditoría.

### Tecnologías Utilizadas

- **kotlinx-serialization** v1.7.3: Serialización JSON multiplataforma
- **kotlinx-datetime** v0.6.1: Manejo de timestamps Instant
- **@Serializable**: Anotación para generación automática de serializers
- **@SerialName**: Mapeo entre convenciones de nomenclatura (snake_case ↔ camelCase)

### Objetivos de Diseño

1. **Compatibilidad Backend**: Mapeo automático snake_case/camelCase
2. **Type-Safety**: Validaciones en tiempo de compilación y runtime
3. **Cross-Platform**: Mismo código funciona en JVM, Android, iOS
4. **Composición**: Interfaces base reutilizables (EntityBase, ValidatableModel, etc.)
5. **Schema Evolution**: Deserialización robusta con campos faltantes o extra

---

## Modelos Implementados

| Modelo      | Propósito                          | Interfaces Implementadas                                    |
|-------------|------------------------------------|-------------------------------------------------------------|
| **Role**    | Rol de usuario con permisos        | `EntityBase`, `ValidatableModel`, `RoleMinimal`             |
| **User**    | Usuario del sistema                | `EntityBase`, `ValidatableModel`, `AuditableModel`, `SoftDeletable` |
| **AuthToken** | Token JWT de autenticación       | `ValidatableModel`                                          |

### Diagrama de Relaciones

```
User
  ├─ roles: List<Role>  (Relación 1:N)
  └─ email, username, displayName

Role (implementa RoleMinimal)
  ├─ id, name (propiedades mínimas)
  └─ description, permissions, isActive

AuthToken
  ├─ token: String (JWT)
  ├─ expiresAt: Instant
  └─ refreshToken: String? (opcional)
```

---

## Role - Modelo de Roles

### Estructura

```kotlin
@Serializable
public data class Role(
    @SerialName("id") override val id: String,
    @SerialName("name") override val name: String,
    @SerialName("description") val description: String = "",
    @SerialName("permissions") val permissions: Set<String> = emptySet(),
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") override val createdAt: Instant = Clock.System.now(),
    @SerialName("updated_at") override val updatedAt: Instant = Clock.System.now()
) : EntityBase<String>, ValidatableModel, RoleMinimal
```

### Propiedades

| Propiedad      | Tipo            | Requerido | Default              | Descripción                                  |
|----------------|-----------------|-----------|----------------------|----------------------------------------------|
| `id`           | `String`        | ✅        | -                    | Identificador único del rol                  |
| `name`         | `String`        | ✅        | -                    | Nombre del rol (ej: "Administrator")         |
| `description`  | `String`        | ❌        | `""`                 | Descripción del rol (max 500 caracteres)     |
| `permissions`  | `Set<String>`   | ❌        | `emptySet()`         | Permisos en formato "resource.action"        |
| `isActive`     | `Boolean`       | ❌        | `true`               | Si el rol está activo                        |
| `createdAt`    | `Instant`       | ❌        | `Clock.System.now()` | Timestamp de creación                        |
| `updatedAt`    | `Instant`       | ❌        | `Clock.System.now()` | Timestamp de última actualización            |

### Ejemplo JSON (Serialización)

```json
{
  "id": "role-admin",
  "name": "Administrator",
  "description": "Full system access",
  "permissions": ["users.read", "users.write", "system.manage"],
  "is_active": true,
  "created_at": "2024-01-01T12:00:00Z",
  "updated_at": "2024-01-01T12:00:00Z"
}
```

### RoleMinimal - Serialización Parcial

**DECISIÓN DE DISEÑO**: Role implementa la interfaz `RoleMinimal` que define solo `id` y `name`. Esto permite al backend enviar versiones ligeras de Role:

```json
{
  "id": "role-user",
  "name": "User"
}
```

Al deserializar, las propiedades opcionales usan sus valores default automáticamente.

### Validaciones

```kotlin
override fun validate(): Result<Unit> {
    if (id.isBlank()) return failure("Role id no puede estar vacío")
    if (name.isBlank()) return failure("Role name no puede estar vacío")
    if (description.length > 500) return failure("description no puede exceder 500 caracteres")

    // Validar formato de permissions: "resource.action"
    val invalidPermissions = permissions.filter { !it.matches(Regex("^[a-z_]+\\.[a-z_]+$")) }
    if (invalidPermissions.isNotEmpty()) {
        return failure("Permissions inválidos: ${invalidPermissions.joinToString()}")
    }

    return success()
}
```

---

## User - Modelo de Usuarios

### Estructura

```kotlin
@Serializable
public data class User(
    @SerialName("id") override val id: String,
    @SerialName("email") val email: String,
    @SerialName("username") val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("roles") val roles: List<Role> = emptyList(),
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    @SerialName("metadata") val metadata: Map<String, String> = emptyMap(),
    @SerialName("created_at") override val createdAt: Instant = Clock.System.now(),
    @SerialName("updated_at") override val updatedAt: Instant = Clock.System.now(),
    @SerialName("created_by") override val createdBy: String = "system",
    @SerialName("updated_by") override val updatedBy: String = "system",
    @SerialName("deleted_at") override val deletedAt: Instant? = null
) : EntityBase<String>, ValidatableModel, AuditableModel, SoftDeletable
```

### Propiedades

| Propiedad          | Tipo                 | Requerido | Default              | Descripción                                  |
|--------------------|----------------------|-----------|----------------------|----------------------------------------------|
| `id`               | `String`             | ✅        | -                    | Identificador único del usuario              |
| `email`            | `String`             | ✅        | -                    | Email válido (formato validado)              |
| `username`         | `String`             | ✅        | -                    | Username único (3-50 caracteres)             |
| `displayName`      | `String`             | ✅        | -                    | Nombre para mostrar en UI                    |
| `roles`            | `List<Role>`         | ❌        | `emptyList()`        | Roles asignados al usuario                   |
| `isActive`         | `Boolean`            | ❌        | `true`               | Si el usuario está activo                    |
| `profileImageUrl`  | `String?`            | ❌        | `null`               | URL de imagen de perfil (opcional)           |
| `metadata`         | `Map<String,String>` | ❌        | `emptyMap()`         | Metadata adicional flexible                  |
| `createdAt`        | `Instant`            | ❌        | `Clock.System.now()` | Timestamp de creación                        |
| `updatedAt`        | `Instant`            | ❌        | `Clock.System.now()` | Timestamp de última actualización            |
| `createdBy`        | `String`             | ❌        | `"system"`           | Usuario que creó este registro               |
| `updatedBy`        | `String`             | ❌        | `"system"`           | Usuario que actualizó por última vez         |
| `deletedAt`        | `Instant?`           | ❌        | `null`               | Timestamp de eliminación (soft delete)       |

### Ejemplo JSON (Serialización)

```json
{
  "id": "user-123",
  "email": "john.doe@example.com",
  "username": "johndoe",
  "display_name": "John Doe",
  "roles": [
    {
      "id": "role-admin",
      "name": "Administrator"
    },
    {
      "id": "role-user",
      "name": "User"
    }
  ],
  "is_active": true,
  "profile_image_url": "https://example.com/avatars/john.jpg",
  "metadata": {
    "department": "Engineering",
    "location": "Remote"
  },
  "created_at": "2024-01-15T08:30:00Z",
  "updated_at": "2024-01-20T14:45:00Z",
  "created_by": "admin-user",
  "updated_by": "admin-user",
  "deleted_at": null
}
```

### Relación User ↔ Role

**DECISIÓN DE DISEÑO IMPORTANTE**: `roles` es de tipo `List<Role>`, NO `List<String>`.

**Razones**:
1. **Type-Safety**: Compilador garantiza que solo objetos Role válidos estén en la lista
2. **Escalabilidad**: Acceso directo a propiedades del rol (name, permissions) sin llamadas adicionales
3. **Sin desincronización**: No hay conversión Role ↔ String, evita inconsistencias
4. **Serialización flexible**: Backend puede enviar Role completo o RoleMinimal (id, name)

Gracias a que Role implementa RoleMinimal con defaults, el backend puede enviar versiones ligeras.

### Métodos Helper

```kotlin
public fun hasRole(roleId: String): Boolean = roles.any { it.id == roleId }
public fun hasAnyRole(vararg roleIds: String): Boolean = roles.any { it.id in roleIds }
public fun hasAllRoles(vararg roleIds: String): Boolean = roleIds.all { hasRole(it) }
```

### Validaciones

```kotlin
override fun validate(): Result<Unit> {
    if (id.isBlank()) return failure("User id no puede estar vacío")

    // Validar formato de email
    if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
        return failure("Email inválido: $email")
    }

    if (username.length < 3 || username.length > 50) {
        return failure("Username debe tener entre 3 y 50 caracteres")
    }

    if (roles.isEmpty()) {
        return failure("User debe tener al menos un rol")
    }

    // Validar todos los roles
    roles.forEach { role ->
        role.validate().onFailure { error ->
            return failure("Role inválido: $error")
        }
    }

    return success()
}
```

---

## AuthToken - Modelo de Autenticación

### Estructura

```kotlin
@Serializable
public data class AuthToken(
    @SerialName("token") val token: String,
    @SerialName("expires_at") val expiresAt: Instant = Clock.System.now(),
    @SerialName("refresh_token") val refreshToken: String? = null
) : ValidatableModel
```

### Propiedades

| Propiedad      | Tipo      | Requerido | Default              | Descripción                                  |
|----------------|-----------|-----------|----------------------|----------------------------------------------|
| `token`        | `String`  | ✅        | -                    | Token JWT                                    |
| `expiresAt`    | `Instant` | ❌        | `Clock.System.now()` | Timestamp de expiración del token            |
| `refreshToken` | `String?` | ❌        | `null`               | Refresh token opcional                       |

### Ejemplo JSON (Serialización)

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U",
  "expires_at": "2024-12-31T23:59:59Z",
  "refresh_token": "refresh_abc123xyz"
}
```

### DECISIÓN DE DISEÑO: NO implementa EntityBase

AuthToken **NO** implementa `EntityBase` porque los tokens son **efímeros** (no son entidades persistentes):

- No necesitan `createdAt`/`updatedAt` (usan `expiresAt` directamente)
- No tienen `id` único persistente
- Su ciclo de vida es corto (minutos/horas, no días/meses)

### Métodos Helper

```kotlin
public fun isExpired(): Boolean = Clock.System.now() >= expiresAt

public fun isValid(): Boolean = validate() is Result.Success && !isExpired()

public fun hasRefreshToken(): Boolean = !refreshToken.isNullOrBlank()

public fun timeUntilExpiration(): Duration = expiresAt - Clock.System.now()

public fun toLogString(): String {
    val tokenPreview = if (token.length > 10) {
        "${token.take(4)}...${token.takeLast(2)}"
    } else "***"
    return "AuthToken(token=$tokenPreview, expires=$expiresAt, hasRefresh=${hasRefreshToken()})"
}
```

### Validaciones

```kotlin
override fun validate(): Result<Unit> {
    if (token.isBlank()) return failure("Token no puede estar vacío")

    // refreshToken puede ser null, pero si está presente no puede estar vacío
    if (refreshToken != null && refreshToken.isBlank()) {
        return failure("refreshToken debe ser null o no-vacío")
    }

    return success()
}
```

### Seguridad - toLogString()

**IMPORTANTE**: NUNCA loggear el token completo en producción. Usar `toLogString()`:

```kotlin
val token = AuthToken(...)
logger.info(token.toLogString())  // ✅ Correcto
logger.info("Token: ${token.token}")  // ❌ MAL - Security leak!
```

---

## Serialización y Compatibilidad Backend

### @SerialName - Mapeo de Convenciones

Los modelos usan **@SerialName** para mapear entre convenciones:

- **Kotlin (código)**: camelCase (`displayName`, `createdAt`, `isActive`)
- **Backend/JSON**: snake_case (`display_name`, `created_at`, `is_active`)

```kotlin
@SerialName("display_name")
val displayName: String
```

### Configuración JSON

```kotlin
val json = Json {
    ignoreUnknownKeys = true  // Schema evolution: ignorar campos extras
    prettyPrint = false       // JSON compacto
    encodeDefaults = false    // No serializar campos con valores default (reduce payload)
}
```

### Tabla de Mapeo Snake_Case ↔ CamelCase

| Kotlin (camelCase)    | JSON (snake_case)     |
|-----------------------|-----------------------|
| `displayName`         | `display_name`        |
| `isActive`            | `is_active`           |
| `profileImageUrl`     | `profile_image_url`   |
| `createdAt`           | `created_at`          |
| `updatedAt`           | `updated_at`          |
| `createdBy`           | `created_by`          |
| `updatedBy`           | `updated_by`          |
| `deletedAt`           | `deleted_at`          |
| `expiresAt`           | `expires_at`          |
| `refreshToken`        | `refresh_token`       |

### Serialización de Instant (ISO-8601)

`kotlinx.datetime.Instant` serializa automáticamente a formato **ISO-8601**:

```json
{
  "created_at": "2024-01-15T08:30:00Z",
  "expires_at": "2024-12-31T23:59:59.123456Z"
}
```

Soporta precisión de microsegundos automáticamente.

### Serialización de Campos con Valores Default

**IMPORTANTE**: Por defecto, `encodeDefaults = false` omite campos con valores default para reducir payload:

```kotlin
val user = User(
    id = "user-1",
    email = "test@example.com",
    username = "test",
    displayName = "Test",
    isActive = true,  // ← Valor default, NO se serializa
    createdBy = "system"  // ← Valor default, NO se serializa
)

json.encodeToString(user)
// Resultado: {"id":"user-1","email":"test@example.com","username":"test","display_name":"Test"}
```

Para forzar serialización de todos los campos, configurar `encodeDefaults = true`:

```kotlin
val json = Json { encodeDefaults = true }
```

---

## Validaciones

### Patrón Validatable

Todos los modelos implementan `ValidatableModel`:

```kotlin
public interface ValidatableModel {
    public fun validate(): Result<Unit>
}
```

### Validaciones por Modelo

#### Role

- ✅ `id` no vacío
- ✅ `name` no vacío
- ✅ `description` max 500 caracteres
- ✅ `permissions` formato "resource.action" (ej: "users.read")

#### User

- ✅ `id` no vacío
- ✅ `email` formato válido (regex)
- ✅ `username` 3-50 caracteres
- ✅ `roles` al menos 1 rol
- ✅ Todos los roles válidos (recursivo)

#### AuthToken

- ✅ `token` no vacío
- ✅ `refreshToken` null o no-vacío (no permite string vacío)

### Cuándo Validar

```kotlin
// OPCIÓN 1: Validar antes de serializar (recomendado para enviar a backend)
val user = User(...)
user.validate().onSuccess {
    val json = Json.encodeToString(user)
    api.createUser(json)
}.onFailure { error ->
    logger.error("Validación falló: $error")
}

// OPCIÓN 2: Validar después de deserializar (recomendado desde backend)
val user = Json.decodeFromString<User>(jsonFromBackend)
user.validate().onFailure { error ->
    logger.warn("Backend envió datos inválidos: $error")
}
```

---

## Schema Evolution

### Agregar Campos Nuevos

**Backend agrega campo nuevo**: Cliente lo ignora automáticamente con `ignoreUnknownKeys = true`:

```json
{
  "id": "user-1",
  "email": "test@example.com",
  "username": "test",
  "display_name": "Test",
  "new_field_from_backend": "ignored by client"
}
```

Cliente deserializa sin error, ignorando `new_field_from_backend`.

### Remover Campos

**Backend remueve campo opcional**: Cliente usa valor default:

```kotlin
// Backend antes:
{"id": "user-1", "email": "...", "is_active": true}

// Backend después (removió is_active):
{"id": "user-1", "email": "..."}

// Cliente deserializa:
val user = Json.decodeFromString<User>(json)
// user.isActive = true (usa default)
```

**Backend remueve campo requerido**: Deserialización falla con `MissingFieldException`.

**Solución**: Convertir campo a opcional con default antes de que backend lo remueva:

```kotlin
// ANTES (campo requerido):
val username: String

// DESPUÉS (campo opcional con default):
val username: String = ""
```

### Cambiar Tipo de Campo

**NO soportado directamente**. Requiere versionado de API o transformación custom.

Estrategias:

1. **Agregar campo nuevo con tipo diferente**:
   ```kotlin
   @SerialName("age") val age: Int = 0  // Deprecated
   @SerialName("age_v2") val ageV2: String = "0"  // Nuevo
   ```

2. **Usar deserialización custom con transformación**

### Versioning de API

Recomendación: Usar versionado de API en URL o header:

```
GET /api/v1/users/:id  (retorna User v1)
GET /api/v2/users/:id  (retorna User v2)
```

Cada versión puede tener su propio modelo Kotlin.

---

## Ejemplos de Código

### Crear y Serializar User

```kotlin
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.datetime.Clock

val user = User(
    id = "user-123",
    email = "john.doe@example.com",
    username = "johndoe",
    displayName = "John Doe",
    roles = listOf(
        Role(id = "role-admin", name = "Administrator")
    ),
    isActive = true,
    createdAt = Clock.System.now(),
    updatedAt = Clock.System.now(),
    createdBy = "system",
    updatedBy = "system"
)

// Validar antes de serializar
user.validate().onSuccess {
    val json = Json.encodeToString(user)
    println(json)
    // {"id":"user-123","email":"john.doe@example.com",...}
}
```

### Deserializar y Validar

```kotlin
val jsonFromBackend = """
{
  "id": "user-456",
  "email": "jane@example.com",
  "username": "janedoe",
  "display_name": "Jane Doe",
  "roles": [
    {"id": "role-user", "name": "User"}
  ]
}
""".trimIndent()

val user = Json.decodeFromString<User>(jsonFromBackend)

// Validar datos del backend
user.validate().onFailure { error ->
    logger.error("Backend envió datos inválidos: $error")
}
```

### Trabajar con AuthToken

```kotlin
val token = AuthToken(
    token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    expiresAt = Clock.System.now() + 1.hours,
    refreshToken = "refresh_abc123"
)

// Verificar si está expirado
if (token.isExpired()) {
    println("Token expirado, usar refresh token")
    // Lógica de refresh
}

// Verificar si es válido (validación + no expirado)
if (token.isValid()) {
    // Usar token para API calls
}

// Tiempo hasta expiración
val remaining = token.timeUntilExpiration()
println("Token expira en ${remaining.inWholeMinutes} minutos")

// Logging seguro
logger.info(token.toLogString())  // ✅ NO muestra token completo
```

### Factory Methods para Tests

```kotlin
// Role
val testRole = Role.createAdminRole()
val customRole = Role(id = "role-test", name = "Test Role")

// User
val testUser = User.createTestUser()
val adminUser = User.createAdminUser(
    id = "admin-1",
    email = "admin@example.com"
)

// AuthToken
val testToken = AuthToken.createTestToken(
    durationSeconds = 3600,  // 1 hora
    includeRefresh = true
)
```

---

## Troubleshooting

### Error: MissingFieldException

**Síntoma**:
```
kotlinx.serialization.MissingFieldException: Field 'display_name' is required for type with serial name 'User', but it was missing
```

**Causa**: El JSON del backend no incluye un campo requerido (sin default).

**Solución**:
1. Verificar que el JSON del backend incluye todos los campos requeridos
2. O agregar valor default al campo en el modelo:
   ```kotlin
   @SerialName("display_name")
   val displayName: String = ""  // Ahora es opcional
   ```

### Error: SerializationException (tipo incorrecto)

**Síntoma**:
```
kotlinx.serialization.json.internal.JsonDecodingException: Expected JsonPrimitive at element, but had JsonArray
```

**Causa**: El tipo del campo en JSON no coincide con el tipo en Kotlin.

**Solución**:
1. Verificar el JSON recibido con logs
2. Ajustar el tipo en Kotlin para coincidir con backend
3. O usar deserialización custom con transformación

### Campos con Default No Aparecen en JSON

**Síntoma**: `created_by`, `updated_by`, `is_active` no aparecen en JSON serializado.

**Causa**: `encodeDefaults = false` (default de kotlinx-serialization).

**Solución**:
- **Opción 1** (recomendado): Usar valores diferentes a defaults para forzar serialización:
  ```kotlin
  val user = User(
      ...,
      isActive = false,  // Diferente al default true
      createdBy = "admin"  // Diferente al default "system"
  )
  ```
- **Opción 2**: Configurar `encodeDefaults = true` (aumenta tamaño de payload):
  ```kotlin
  val json = Json { encodeDefaults = true }
  ```

### Instant No Serializa Correctamente

**Síntoma**: `Instant` aparece como objeto en JSON en lugar de string ISO-8601.

**Causa**: No se está usando `kotlinx-datetime.Instant`, sino `java.time.Instant`.

**Solución**: Importar y usar `kotlinx.datetime.Instant`:
```kotlin
import kotlinx.datetime.Instant  // ✅ Correcto
import java.time.Instant  // ❌ Incorrecto para KMP
```

### Validación Falla Pero No Sé Por Qué

**Síntoma**: `validate()` retorna `Result.Failure` pero el mensaje no es claro.

**Solución**: Usar pattern matching para extraer el error:
```kotlin
user.validate().onFailure { error ->
    println("Validación falló: $error")
    // Log detallado del objeto
    println("User: $user")
}
```

---

## Mejores Prácticas

### 1. Siempre Validar en Boundaries

```kotlin
// ✅ CORRECTO: Validar al deserializar desde backend
val user = Json.decodeFromString<User>(jsonFromBackend)
user.validate().onFailure { error ->
    logger.error("Backend data invalid: $error")
}

// ✅ CORRECTO: Validar antes de serializar hacia backend
user.validate().onSuccess {
    val json = Json.encodeToString(user)
    api.createUser(json)
}
```

### 2. Usar toLogString() para Datos Sensibles

```kotlin
// ❌ MAL: Expone token completo en logs
logger.info("Token: ${authToken.token}")

// ✅ CORRECTO: Usa preview seguro
logger.info(authToken.toLogString())
// Output: "AuthToken(token=eyJh...U, expires=2024-12-31T23:59:59Z, hasRefresh=true)"
```

### 3. Configurar ignoreUnknownKeys = true

```kotlin
// ✅ CORRECTO: Robusto ante cambios en backend
val json = Json { ignoreUnknownKeys = true }

// ❌ MAL: Falla si backend agrega campos nuevos
val json = Json { ignoreUnknownKeys = false }
```

### 4. Usar Valores Default para Schema Evolution

```kotlin
// ✅ CORRECTO: Backward compatible
@SerialName("profile_image_url")
val profileImageUrl: String? = null

// ❌ MAL: Requiere que backend siempre envíe este campo
@SerialName("profile_image_url")
val profileImageUrl: String
```

### 5. Preferir Inmutabilidad (data class con val)

```kotlin
// ✅ CORRECTO: Inmutable, thread-safe
data class User(val id: String, ...)

// ❌ MAL: Mutable, no thread-safe
data class User(var id: String, ...)
```

### 6. Usar Factory Methods para Tests

```kotlin
// ✅ CORRECTO: Legible y reutilizable
val user = User.createTestUser()

// ❌ MAL: Verboso, error-prone
val user = User(
    id = "test-1",
    email = "test@example.com",
    username = "test",
    displayName = "Test User",
    roles = listOf(Role(...)),
    // ... 10 más campos
)
```

---

## Extensibilidad

### Agregar Formato Protobuf

Para agregar soporte a **Protocol Buffers**:

1. Agregar dependencia `kotlinx-serialization-protobuf`:
   ```kotlin
   implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.7.3")
   ```

2. Los modelos NO requieren cambios (ya tienen `@Serializable`):
   ```kotlin
   import kotlinx.serialization.protobuf.ProtoBuf

   val user = User(...)
   val bytes = ProtoBuf.encodeToByteArray(user)
   val decoded = ProtoBuf.decodeFromByteArray<User>(bytes)
   ```

### Agregar Formato CBOR

Para formato binario **CBOR**:

```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.7.3")

import kotlinx.serialization.cbor.Cbor

val user = User(...)
val bytes = Cbor.encodeToByteArray(user)
val decoded = Cbor.decodeFromByteArray<User>(bytes)
```

### Custom Serializers

Para tipos custom, crear serializer:

```kotlin
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

// Uso:
@Serializable
data class User(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    ...
)
```

---

## Referencias

- [kotlinx-serialization Documentation](https://github.com/Kotlin/kotlinx.serialization)
- [kotlinx-datetime Documentation](https://github.com/Kotlin/kotlinx-datetime)
- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [JSON Schema Evolution Best Practices](https://martin.kleppmann.com/2012/12/05/schema-evolution-in-avro-protocol-buffers-thrift.html)

---

**Última Revisión**: 2026-02-03
**Versión del Módulo**: 1.0.0
**Mantenedor**: Sistema MCPEco - Workflow Development
