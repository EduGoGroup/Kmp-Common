package com.edugo.test.module.validation

/**
 * Helpers de validación reutilizables y componibles.
 *
 * Este archivo contiene funciones de validación comunes que pueden ser
 * usadas en cualquier parte del código para validar datos de entrada.
 * Todas las funciones retornan null si la validación pasa, o un mensaje
 * de error String si falla.
 *
 * ## Características
 *
 * - **Reutilizables**: Se pueden usar en modelos, mappers, DTOs
 * - **Componibles**: Se pueden combinar múltiples validaciones
 * - **Type-Safe**: Verificación de tipos en compilación
 * - **Null-safe**: Manejan valores null explícitamente
 * - **Customizables**: Permiten mensajes de error personalizados
 *
 * ## Uso Básico
 *
 * ```kotlin
 * data class User(val email: String, val age: Int) : ValidatableModel {
 *     override fun validate(): Result<Unit> {
 *         validateEmail(email)?.let { return failure(it) }
 *         validateRange(age, 18, 120, "Age")?.let { return failure(it) }
 *         return success(Unit)
 *     }
 * }
 * ```
 *
 * ## Validación Acumulativa
 *
 * ```kotlin
 * data class User(val email: String, val age: Int) : ValidatableModel {
 *     override fun validate(): Result<Unit> {
 *         val errors = listOfNotNull(
 *             validateEmail(email),
 *             validateRange(age, 18, 120, "Age")
 *         )
 *         return if (errors.isEmpty()) {
 *             success(Unit)
 *         } else {
 *             failure(errors.joinToString("; "))
 *         }
 *     }
 * }
 * ```
 */

/**
 * Valida que un String no esté vacío o en blanco.
 *
 * Verifica que el valor no sea null, no esté vacío (length == 0),
 * y no contenga solo espacios en blanco.
 *
 * ## Ejemplos
 *
 * ```kotlin
 * validateNotBlank("Hello", "Name") // null (válido)
 * validateNotBlank("", "Name") // "Name cannot be blank"
 * validateNotBlank("   ", "Name") // "Name cannot be blank"
 * validateNotBlank(null, "Name") // "Name cannot be blank"
 * ```
 *
 * ## Uso con Custom Message
 *
 * ```kotlin
 * validateNotBlank(username, "Username", "Please provide a username")
 * // Si falla: "Please provide a username"
 * ```
 *
 * @param value Valor a validar
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun validateNotBlank(
    value: String?,
    fieldName: String,
    customMessage: String? = null
): String? {
    return if (value.isNullOrBlank()) {
        customMessage ?: "$fieldName cannot be blank"
    } else {
        null
    }
}

/**
 * Valida que un valor numérico esté dentro de un rango.
 *
 * Verifica que el valor sea mayor o igual al mínimo y menor o igual
 * al máximo especificado (inclusivo en ambos extremos).
 *
 * ## Ejemplos
 *
 * ```kotlin
 * validateRange(25, 18, 120, "Age") // null (válido)
 * validateRange(15, 18, 120, "Age") // "Age must be between 18 and 120"
 * validateRange(150, 18, 120, "Age") // "Age must be between 18 and 120"
 * validateRange(18, 18, 120, "Age") // null (válido - límite inferior)
 * validateRange(120, 18, 120, "Age") // null (válido - límite superior)
 * ```
 *
 * ## Uso con Custom Message
 *
 * ```kotlin
 * validateRange(age, 0, 150, "Age", "Invalid age provided")
 * // Si falla: "Invalid age provided"
 * ```
 *
 * @param value Valor a validar
 * @param min Valor mínimo permitido (inclusivo)
 * @param max Valor máximo permitido (inclusivo)
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun validateRange(
    value: Int,
    min: Int,
    max: Int,
    fieldName: String,
    customMessage: String? = null
): String? {
    return if (value < min || value > max) {
        customMessage ?: "$fieldName must be between $min and $max"
    } else {
        null
    }
}

/**
 * Valida que un valor numérico de tipo Long esté dentro de un rango.
 *
 * Sobrecarga de validateRange para valores Long.
 *
 * @param value Valor a validar
 * @param min Valor mínimo permitido (inclusivo)
 * @param max Valor máximo permitido (inclusivo)
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun validateRange(
    value: Long,
    min: Long,
    max: Long,
    fieldName: String,
    customMessage: String? = null
): String? {
    return if (value < min || value > max) {
        customMessage ?: "$fieldName must be between $min and $max"
    } else {
        null
    }
}

/**
 * Valida que un valor numérico de tipo Double esté dentro de un rango.
 *
 * Sobrecarga de validateRange para valores Double.
 *
 * @param value Valor a validar
 * @param min Valor mínimo permitido (inclusivo)
 * @param max Valor máximo permitido (inclusivo)
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun validateRange(
    value: Double,
    min: Double,
    max: Double,
    fieldName: String,
    customMessage: String? = null
): String? {
    return if (value < min || value > max) {
        customMessage ?: "$fieldName must be between $min and $max"
    } else {
        null
    }
}

/**
 * Valida que un String tenga formato de email válido.
 *
 * Verifica que el email contenga exactamente un símbolo @ y que tenga
 * texto antes y después del @. No valida completamente según RFC 5322,
 * pero cubre los casos comunes.
 *
 * ## Reglas de Validación
 *
 * - Debe contener exactamente un @
 * - Debe tener al menos un carácter antes del @
 * - Debe tener al menos un carácter después del @
 * - No debe estar vacío o en blanco
 *
 * ## Ejemplos
 *
 * ```kotlin
 * validateEmail("test@example.com") // null (válido)
 * validateEmail("user@domain.co.uk") // null (válido)
 * validateEmail("invalid.email") // "Invalid email format"
 * validateEmail("@example.com") // "Invalid email format"
 * validateEmail("test@") // "Invalid email format"
 * validateEmail("test@@example.com") // "Invalid email format"
 * validateEmail("") // "Invalid email format"
 * ```
 *
 * ## Uso con Custom Message
 *
 * ```kotlin
 * validateEmail(email, "Please provide a valid email address")
 * // Si falla: "Please provide a valid email address"
 * ```
 *
 * @param email Email a validar
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun validateEmail(
    email: String?,
    customMessage: String? = null
): String? {
    if (email.isNullOrBlank()) {
        return customMessage ?: "Invalid email format"
    }

    val atCount = email.count { it == '@' }
    val atIndex = email.indexOf('@')

    return when {
        atCount != 1 -> customMessage ?: "Invalid email format"
        atIndex == 0 -> customMessage ?: "Invalid email format"
        atIndex == email.length - 1 -> customMessage ?: "Invalid email format"
        else -> null
    }
}

/**
 * Valida que un String tenga una longitud mínima.
 *
 * Verifica que el valor no sea null y tenga al menos la longitud mínima
 * especificada.
 *
 * ## Ejemplos
 *
 * ```kotlin
 * validateMinLength("hello", 3, "Username") // null (válido)
 * validateMinLength("hi", 3, "Username") // "Username must be at least 3 characters"
 * validateMinLength("", 3, "Username") // "Username must be at least 3 characters"
 * validateMinLength(null, 3, "Username") // "Username must be at least 3 characters"
 * ```
 *
 * @param value Valor a validar
 * @param minLength Longitud mínima permitida
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun validateMinLength(
    value: String?,
    minLength: Int,
    fieldName: String,
    customMessage: String? = null
): String? {
    return if (value == null || value.length < minLength) {
        customMessage ?: "$fieldName must be at least $minLength characters"
    } else {
        null
    }
}

/**
 * Valida que un String no exceda una longitud máxima.
 *
 * Verifica que el valor no sea null y no exceda la longitud máxima
 * especificada.
 *
 * ## Ejemplos
 *
 * ```kotlin
 * validateMaxLength("hello", 10, "Username") // null (válido)
 * validateMaxLength("verylongusername", 10, "Username") // "Username must be at most 10 characters"
 * validateMaxLength(null, 10, "Username") // "Username cannot be null"
 * ```
 *
 * @param value Valor a validar
 * @param maxLength Longitud máxima permitida
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun validateMaxLength(
    value: String?,
    maxLength: Int,
    fieldName: String,
    customMessage: String? = null
): String? {
    return when {
        value == null -> customMessage ?: "$fieldName cannot be null"
        value.length > maxLength -> customMessage ?: "$fieldName must be at most $maxLength characters"
        else -> null
    }
}

/**
 * Valida que un String tenga una longitud dentro de un rango.
 *
 * Combina validateMinLength y validateMaxLength.
 *
 * ## Ejemplos
 *
 * ```kotlin
 * validateLengthRange("hello", 3, 10, "Username") // null (válido)
 * validateLengthRange("hi", 3, 10, "Username") // "Username must be between 3 and 10 characters"
 * validateLengthRange("verylongusername", 3, 10, "Username") // "Username must be between 3 and 10 characters"
 * ```
 *
 * @param value Valor a validar
 * @param minLength Longitud mínima permitida
 * @param maxLength Longitud máxima permitida
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun validateLengthRange(
    value: String?,
    minLength: Int,
    maxLength: Int,
    fieldName: String,
    customMessage: String? = null
): String? {
    return when {
        value == null -> customMessage ?: "$fieldName cannot be null"
        value.length < minLength || value.length > maxLength ->
            customMessage ?: "$fieldName must be between $minLength and $maxLength characters"
        else -> null
    }
}

/**
 * Valida que un valor numérico sea positivo (mayor que cero).
 *
 * ## Ejemplos
 *
 * ```kotlin
 * validatePositive(10, "Amount") // null (válido)
 * validatePositive(0, "Amount") // "Amount must be positive"
 * validatePositive(-5, "Amount") // "Amount must be positive"
 * ```
 *
 * @param value Valor a validar
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun validatePositive(
    value: Int,
    fieldName: String,
    customMessage: String? = null
): String? {
    return if (value <= 0) {
        customMessage ?: "$fieldName must be positive"
    } else {
        null
    }
}

/**
 * Valida que un valor numérico Double sea positivo (mayor que cero).
 *
 * @param value Valor a validar
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun validatePositive(
    value: Double,
    fieldName: String,
    customMessage: String? = null
): String? {
    return if (value <= 0.0) {
        customMessage ?: "$fieldName must be positive"
    } else {
        null
    }
}

/**
 * Valida que un valor numérico no sea negativo (mayor o igual a cero).
 *
 * ## Ejemplos
 *
 * ```kotlin
 * validateNonNegative(10, "Quantity") // null (válido)
 * validateNonNegative(0, "Quantity") // null (válido)
 * validateNonNegative(-5, "Quantity") // "Quantity cannot be negative"
 * ```
 *
 * @param value Valor a validar
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun validateNonNegative(
    value: Int,
    fieldName: String,
    customMessage: String? = null
): String? {
    return if (value < 0) {
        customMessage ?: "$fieldName cannot be negative"
    } else {
        null
    }
}

/**
 * Valida que un valor numérico Double no sea negativo.
 *
 * @param value Valor a validar
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun validateNonNegative(
    value: Double,
    fieldName: String,
    customMessage: String? = null
): String? {
    return if (value < 0.0) {
        customMessage ?: "$fieldName cannot be negative"
    } else {
        null
    }
}

/**
 * Valida que un String coincida con un patrón regex.
 *
 * ## Ejemplos
 *
 * ```kotlin
 * validatePattern("123-456", Regex("\\d{3}-\\d{3}"), "Phone") // null (válido)
 * validatePattern("abc-def", Regex("\\d{3}-\\d{3}"), "Phone") // "Phone has invalid format"
 * ```
 *
 * @param value Valor a validar
 * @param pattern Expresión regular a coincidir
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun validatePattern(
    value: String?,
    pattern: Regex,
    fieldName: String,
    customMessage: String? = null
): String? {
    return if (value == null || !pattern.matches(value)) {
        customMessage ?: "$fieldName has invalid format"
    } else {
        null
    }
}

/**
 * Valida que un valor esté contenido en una colección de valores permitidos.
 *
 * ## Ejemplos
 *
 * ```kotlin
 * validateIn("admin", listOf("admin", "user", "guest"), "Role") // null (válido)
 * validateIn("superuser", listOf("admin", "user", "guest"), "Role")
 * // "Role must be one of: admin, user, guest"
 * ```
 *
 * @param value Valor a validar
 * @param allowedValues Colección de valores permitidos
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun <T> validateIn(
    value: T,
    allowedValues: Collection<T>,
    fieldName: String,
    customMessage: String? = null
): String? {
    return if (value !in allowedValues) {
        customMessage ?: "$fieldName must be one of: ${allowedValues.joinToString(", ")}"
    } else {
        null
    }
}

/**
 * Valida que una colección no esté vacía.
 *
 * ## Ejemplos
 *
 * ```kotlin
 * validateNotEmpty(listOf(1, 2, 3), "Items") // null (válido)
 * validateNotEmpty(emptyList(), "Items") // "Items cannot be empty"
 * validateNotEmpty(null, "Items") // "Items cannot be empty"
 * ```
 *
 * @param collection Colección a validar
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun <T> validateNotEmpty(
    collection: Collection<T>?,
    fieldName: String,
    customMessage: String? = null
): String? {
    return if (collection.isNullOrEmpty()) {
        customMessage ?: "$fieldName cannot be empty"
    } else {
        null
    }
}

/**
 * Valida que una colección tenga un tamaño mínimo.
 *
 * ## Ejemplos
 *
 * ```kotlin
 * validateMinSize(listOf(1, 2, 3), 2, "Tags") // null (válido)
 * validateMinSize(listOf(1), 2, "Tags") // "Tags must contain at least 2 items"
 * ```
 *
 * @param collection Colección a validar
 * @param minSize Tamaño mínimo permitido
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun <T> validateMinSize(
    collection: Collection<T>?,
    minSize: Int,
    fieldName: String,
    customMessage: String? = null
): String? {
    return if (collection == null || collection.size < minSize) {
        customMessage ?: "$fieldName must contain at least $minSize items"
    } else {
        null
    }
}

/**
 * Valida que una colección no exceda un tamaño máximo.
 *
 * ## Ejemplos
 *
 * ```kotlin
 * validateMaxSize(listOf(1, 2), 5, "Tags") // null (válido)
 * validateMaxSize(listOf(1, 2, 3, 4, 5, 6), 5, "Tags") // "Tags cannot contain more than 5 items"
 * ```
 *
 * @param collection Colección a validar
 * @param maxSize Tamaño máximo permitido
 * @param fieldName Nombre del campo para el mensaje de error
 * @param customMessage Mensaje de error personalizado (opcional)
 * @return null si la validación pasa, mensaje de error si falla
 */
public fun <T> validateMaxSize(
    collection: Collection<T>?,
    maxSize: Int,
    fieldName: String,
    customMessage: String? = null
): String? {
    return if (collection != null && collection.size > maxSize) {
        customMessage ?: "$fieldName cannot contain more than $maxSize items"
    } else {
        null
    }
}

// ============================================================================
// EXTENSION FUNCTIONS - Convenience API
// These extension functions provide fluent syntax sugar over the validation helpers above.
// They come in two flavors:
// - isValidXxx(): Boolean - Simple boolean checks
// - validateXxx(): Result<T> - Result-based validation with error details
// ============================================================================

/**
 * Extension function que valida si un String es un email válido.
 *
 * Wrapper sobre [validateEmail] para API fluida con sintaxis de extension function.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val email = "user@example.com"
 * if (email.isValidEmail()) {
 *     println("Email válido")
 * }
 * ```
 *
 * @return true si el email es válido, false en caso contrario
 */
public inline fun String.isValidEmail(): Boolean = validateEmail(this) == null

/**
 * Extension function que valida un email y retorna Result<String>.
 *
 * Versión Result-based de [isValidEmail] que proporciona detalles del error.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val email = "invalid-email"
 * when (val result = email.validateEmail()) {
 *     is Result.Success -> println("Email válido: ${result.data}")
 *     is Result.Failure -> println("Error: ${result.error}")
 * }
 * ```
 *
 * @return Result.Success con el email si es válido, Result.Failure con mensaje si es inválido
 */
public inline fun String.validateEmail(): com.edugo.test.module.core.Result<String> =
    validateEmail(this)?.let { com.edugo.test.module.core.failure(it) }
        ?: com.edugo.test.module.core.success(this)

/**
 * Extension function que valida si un String es un UUID v4 válido.
 *
 * Valida que el string cumpla con el formato UUID v4:
 * - 8 dígitos hexadecimales
 * - guión
 * - 4 dígitos hexadecimales
 * - guión
 * - "4" seguido de 3 dígitos hexadecimales (versión 4)
 * - guión
 * - uno de [89ab] seguido de 3 dígitos hexadecimales (variante RFC 4122)
 * - guión
 * - 12 dígitos hexadecimales
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val uuid = "550e8400-e29b-41d4-a716-446655440000"
 * if (uuid.isValidUUID()) {
 *     println("UUID válido")
 * }
 * ```
 *
 * @return true si es un UUID v4 válido, false en caso contrario
 */
public inline fun String.isValidUUID(): Boolean {
    val uuidRegex = Regex(
        "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
        RegexOption.IGNORE_CASE
    )
    return this.matches(uuidRegex)
}

/**
 * Extension function que valida un UUID y retorna Result<String>.
 *
 * Versión Result-based de [isValidUUID] que proporciona detalles del error.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val uuid = "invalid-uuid"
 * when (val result = uuid.validateUUID()) {
 *     is Result.Success -> println("UUID válido: ${result.data}")
 *     is Result.Failure -> println("Error: ${result.error}")
 * }
 * ```
 *
 * @return Result.Success con el UUID si es válido, Result.Failure con mensaje si es inválido
 */
public inline fun String.validateUUID(): com.edugo.test.module.core.Result<String> =
    if (isValidUUID()) com.edugo.test.module.core.success(this)
    else com.edugo.test.module.core.failure("Invalid UUID format")

/**
 * Extension function que valida si un valor Comparable está dentro de un rango.
 *
 * Verifica que el valor sea mayor o igual al mínimo y menor o igual al máximo
 * (inclusivo en ambos extremos).
 *
 * ## Ejemplos
 *
 * ```kotlin
 * val age = 25
 * if (age.isInRange(18, 120)) {
 *     println("Edad válida")
 * }
 *
 * val price = 99.99
 * if (price.isInRange(0.0, 1000.0)) {
 *     println("Precio válido")
 * }
 * ```
 *
 * @param min Valor mínimo permitido (inclusivo)
 * @param max Valor máximo permitido (inclusivo)
 * @return true si el valor está en el rango [min, max], false en caso contrario
 */
public inline fun <T : Comparable<T>> T.isInRange(min: T, max: T): Boolean =
    this >= min && this <= max

/**
 * Extension function que valida coincidencia de passwords.
 *
 * Compara el password con el campo de confirmación para verificar que coincidan.
 * Útil en formularios de registro o cambio de contraseña.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val password = "secret123"
 * val confirmation = "secret123"
 *
 * if (password.matchesPassword(confirmation)) {
 *     println("Las contraseñas coinciden")
 * }
 * ```
 *
 * @param confirmation Password de confirmación a comparar
 * @return true si ambos passwords coinciden, false en caso contrario
 */
public inline fun String.matchesPassword(confirmation: String): Boolean =
    this == confirmation

/**
 * Extension function que valida coincidencia de passwords y retorna Result<Unit>.
 *
 * Versión Result-based de [matchesPassword] que proporciona detalles del error.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val password = "secret123"
 * val confirmation = "secret456"
 *
 * when (val result = password.validatePasswordMatch(confirmation)) {
 *     is Result.Success -> println("Contraseñas coinciden")
 *     is Result.Failure -> println("Error: ${result.error}")
 * }
 * ```
 *
 * @param confirmation Password de confirmación a comparar
 * @return Result.Success si coinciden, Result.Failure con mensaje si no coinciden
 */
public inline fun String.validatePasswordMatch(confirmation: String): com.edugo.test.module.core.Result<Unit> =
    if (matchesPassword(confirmation)) com.edugo.test.module.core.success(Unit)
    else com.edugo.test.module.core.failure("Passwords do not match")
