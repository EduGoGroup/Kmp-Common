# Error Handling Infrastructure

This module provides a comprehensive, type-safe error handling infrastructure for Kotlin Multiplatform applications.

## Overview

The error handling system consists of two main components:

1. **ErrorCode** - Enumeration of standardized error categories
2. **AppError** - Rich error data class with context and traceability

## ErrorCode

`ErrorCode` provides a standardized way to categorize errors across the application.

### Available Error Codes

| Code | HTTP Status | Retryable | Description |
|------|-------------|-----------|-------------|
| `UNKNOWN` | 500 | No | Unexpected errors |
| `VALIDATION` | 400 | No | Input validation failures |
| `NETWORK` | - | Yes | Network connectivity issues |
| `NOT_FOUND` | 404 | No | Resource not found |
| `UNAUTHORIZED` | 401 | No | Authentication required |
| `FORBIDDEN` | 403 | No | Insufficient permissions |
| `SERVER_ERROR` | 500 | No | Server-side errors |
| `CONFLICT` | 409 | No | Resource conflicts |
| `RATE_LIMIT_EXCEEDED` | 429 | Yes | Too many requests |
| `SERVICE_UNAVAILABLE` | 503 | Yes | Service temporarily down |
| `TIMEOUT` | 408 | Yes | Operation timeout |
| `BAD_REQUEST` | 400 | No | Malformed request |
| `SERIALIZATION` | - | No | Data serialization errors |
| `PERSISTENCE` | - | No | Database/storage errors |
| `BUSINESS_LOGIC` | 422 | No | Business rule violations |

### Usage

```kotlin
// Check error properties
if (errorCode.isRetryable()) {
    // Retry the operation
}

if (errorCode.isClientError()) {
    // Handle client-side error
}

// Map HTTP status to error code
val errorCode = ErrorCode.fromHttpStatus(404) // Returns NOT_FOUND
```

## AppError

`AppError` is an immutable data class that encapsulates comprehensive error information.

### Properties

- `code: ErrorCode` - Categorized error code
- `message: String` - Human-readable error message
- `details: Map<String, Any?>` - Additional context (immutable)
- `cause: Throwable?` - Underlying exception (optional)
- `timestamp: Long` - When the error occurred

### Factory Methods

#### From Exception

```kotlin
try {
    performNetworkOperation()
} catch (e: IOException) {
    val error = AppError.fromException(
        exception = e,
        code = ErrorCode.NETWORK,
        details = mapOf("url" to apiUrl, "method" to "GET")
    )
}
```

#### From Error Code

```kotlin
val error = AppError.fromCode(
    code = ErrorCode.NOT_FOUND,
    customMessage = "User with ID 123 not found",
    details = mapOf("userId" to 123)
)
```

#### Validation Errors

```kotlin
val error = AppError.validation(
    message = "Email format is invalid",
    field = "email"
)
// Automatically adds "field" to details
```

#### Network Errors

```kotlin
try {
    makeApiCall()
} catch (e: IOException) {
    val error = AppError.network(
        cause = e,
        details = mapOf("endpoint" to "/api/users", "retries" to 3)
    )
}
```

#### Other Convenience Methods

```kotlin
// Timeout
AppError.timeout(
    message = "Request timed out after 30 seconds",
    details = mapOf("timeout" to "30s")
)

// Unauthorized
AppError.unauthorized(
    message = "Session has expired",
    details = mapOf("sessionId" to sessionId)
)

// Not Found
AppError.notFound(
    message = "Resource not found",
    details = mapOf("resourceId" to id, "type" to "User")
)

// Server Error
AppError.serverError(
    cause = exception,
    message = "Database connection failed",
    details = mapOf("database" to "users_db")
)
```

### Error Analysis

#### Root Cause

```kotlin
val rootCause = error.getRootCause()
println("Root cause: ${rootCause?.message}")
```

#### All Causes

```kotlin
error.getAllCauses().forEachIndexed { index, cause ->
    println("Cause $index: ${cause::class.simpleName}: ${cause.message}")
}
```

#### Stack Trace

```kotlin
logger.error("Error occurred:\n${error.getStackTraceString()}")
```

### Adding Context

```kotlin
val error = AppError.fromCode(ErrorCode.SERVER_ERROR, "Database error")

val enriched = error.withDetails(
    "userId" to currentUserId,
    "operation" to "updateProfile",
    "timestamp" to System.currentTimeMillis()
)
```

### User-Friendly Messages

```kotlin
val userMessage = error.toUserMessage()
// Returns sanitized, user-safe message
// e.g., "Please check your internet connection and try again"
```

### Error Classification

```kotlin
if (error.isRetryable()) {
    // Implement retry logic
}

if (error.isClientError()) {
    // Handle 4xx errors
}

if (error.isServerError()) {
    // Handle 5xx errors
}
```

## Integration with Result

AppError works seamlessly with the Result type:

```kotlin
suspend fun fetchUser(userId: Int): Result<User> = catching {
    try {
        val user = api.getUser(userId)
        Result.Success(user)
    } catch (e: IOException) {
        val error = AppError.network(e, mapOf("userId" to userId))
        Result.Failure(error.message)
    } catch (e: Exception) {
        val error = AppError.fromException(e, ErrorCode.UNKNOWN)
        Result.Failure(error.message)
    }
}
```

## Best Practices

### 1. Use Appropriate Error Codes

```kotlin
// Good
AppError.validation("Email is required", field = "email")

// Bad
AppError.fromCode(ErrorCode.UNKNOWN, "Email is required")
```

### 2. Include Contextual Details

```kotlin
// Good
AppError.notFound(
    message = "User not found",
    details = mapOf(
        "userId" to userId,
        "searchedBy" to "id",
        "requestId" to requestId
    )
)

// Less helpful
AppError.notFound("User not found")
```

### 3. Preserve Exception Chains

```kotlin
// Good
try {
    processData()
} catch (e: Exception) {
    AppError.fromException(e, ErrorCode.SERVER_ERROR)
}

// Bad - loses stack trace
AppError.fromCode(ErrorCode.SERVER_ERROR, e.message ?: "Error")
```

### 4. Enrich Errors as They Propagate

```kotlin
fun repositoryLayer() {
    try {
        // ...
    } catch (e: Exception) {
        throw AppError.fromException(e, ErrorCode.PERSISTENCE)
    }
}

fun serviceLayer(userId: Int) {
    val error = try {
        repositoryLayer()
    } catch (e: AppError) {
        e.withDetails("userId" to userId, "layer" to "service")
    }
}
```

### 5. Use toUserMessage() for UI

```kotlin
// In ViewModel or Presenter
fun handleError(error: AppError) {
    uiState.value = UiState.Error(error.toUserMessage())
    
    // Log full details for debugging
    logger.error("Error: ${error.toString()}\n${error.getStackTraceString()}")
}
```

## Thread Safety

Both `ErrorCode` and `AppError` are thread-safe:
- `ErrorCode` is an enum (immutable by definition)
- `AppError` is a data class with all `val` properties
- The `details` map is copied defensively to ensure immutability

## Multiplatform Support

The error handling system works across all Kotlin Multiplatform targets:
- JVM (including Android)
- iOS
- JavaScript
- Native

Platform-specific timestamp implementation is handled transparently through expect/actual.

## Examples

### Complete Error Handling Flow

```kotlin
suspend fun updateUserProfile(userId: Int, profile: ProfileData): Result<User> = catching {
    // Validation
    if (profile.email.isBlank()) {
        return Result.Failure(
            AppError.validation("Email is required", field = "email").message
        )
    }
    
    try {
        val user = api.updateProfile(userId, profile)
        Result.Success(user)
    } catch (e: IOException) {
        val error = AppError.network(
            cause = e,
            details = mapOf(
                "userId" to userId,
                "operation" to "updateProfile"
            )
        )
        Result.Failure(error.toUserMessage())
    } catch (e: HttpException) {
        val code = ErrorCode.fromHttpStatus(e.statusCode)
        val error = AppError.fromException(e, code)
            .withDetails("userId" to userId)
        Result.Failure(error.toUserMessage())
    }
}
```

### Error Logging

```kotlin
fun logError(error: AppError) {
    logger.error {
        buildString {
            appendLine("Error occurred:")
            appendLine("  Code: ${error.code}")
            appendLine("  Message: ${error.message}")
            if (error.details.isNotEmpty()) {
                appendLine("  Details: ${error.details}")
            }
            appendLine("  Timestamp: ${error.timestamp}")
            appendLine("  Retryable: ${error.isRetryable()}")
            
            if (error.cause != null) {
                appendLine("\nException chain:")
                appendLine(error.getStackTraceString())
            }
        }
    }
}
```

## Migration Guide

If you're using simple string-based errors:

### Before
```kotlin
Result.Failure("Network error occurred")
```

### After
```kotlin
val error = AppError.fromCode(ErrorCode.NETWORK)
Result.Failure(error.message)
```

Or with more context:
```kotlin
val error = AppError.network(
    cause = exception,
    details = mapOf("endpoint" to "/api/users")
)
Result.Failure(error.toUserMessage())
```

## Testing

The module includes comprehensive tests. See:
- `ErrorCodeTest.kt` - Tests for ErrorCode enum
- `AppErrorTest.kt` - Tests for AppError data class

Run tests:
```bash
./gradlew :test-module:allTests
```
