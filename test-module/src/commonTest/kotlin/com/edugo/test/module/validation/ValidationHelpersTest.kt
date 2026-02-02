package com.edugo.test.module.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Suite de tests para ValidationHelpers.
 *
 * Verifica:
 * - validateNotBlank con valores válidos e inválidos
 * - validateRange con Int, Long, Double
 * - validateEmail con formatos válidos e inválidos
 * - validateMinLength y validateMaxLength
 * - validateLengthRange
 * - validatePositive y validateNonNegative
 * - validatePattern con regex
 * - validateIn con valores permitidos
 * - validateNotEmpty, validateMinSize, validateMaxSize para colecciones
 * - Custom messages en todas las funciones
 */
class ValidationHelpersTest {

    // ========== Tests de validateNotBlank ==========

    @Test
    fun `validateNotBlank retorna null para valor válido`() {
        val result = validateNotBlank("valid value", "Field")
        assertNull(result)
    }

    @Test
    fun `validateNotBlank retorna error para string vacío`() {
        val result = validateNotBlank("", "Field")
        assertEquals("Field cannot be blank", result)
    }

    @Test
    fun `validateNotBlank retorna error para string con solo espacios`() {
        val result = validateNotBlank("   ", "Field")
        assertEquals("Field cannot be blank", result)
    }

    @Test
    fun `validateNotBlank retorna error para null`() {
        val result = validateNotBlank(null, "Field")
        assertEquals("Field cannot be blank", result)
    }

    @Test
    fun `validateNotBlank con custom message`() {
        val result = validateNotBlank("", "Field", "Custom error")
        assertEquals("Custom error", result)
    }

    // ========== Tests de validateRange (Int) ==========

    @Test
    fun `validateRange Int retorna null para valor en rango`() {
        val result = validateRange(50, 0, 100, "Age")
        assertNull(result)
    }

    @Test
    fun `validateRange Int retorna null para límite inferior`() {
        val result = validateRange(0, 0, 100, "Age")
        assertNull(result)
    }

    @Test
    fun `validateRange Int retorna null para límite superior`() {
        val result = validateRange(100, 0, 100, "Age")
        assertNull(result)
    }

    @Test
    fun `validateRange Int retorna error para valor menor al mínimo`() {
        val result = validateRange(-1, 0, 100, "Age")
        assertEquals("Age must be between 0 and 100", result)
    }

    @Test
    fun `validateRange Int retorna error para valor mayor al máximo`() {
        val result = validateRange(101, 0, 100, "Age")
        assertEquals("Age must be between 0 and 100", result)
    }

    @Test
    fun `validateRange Int con custom message`() {
        val result = validateRange(101, 0, 100, "Age", "Invalid age")
        assertEquals("Invalid age", result)
    }

    // ========== Tests de validateRange (Long) ==========

    @Test
    fun `validateRange Long retorna null para valor válido`() {
        val result = validateRange(5000L, 0L, 10000L, "Amount")
        assertNull(result)
    }

    @Test
    fun `validateRange Long retorna error para valor fuera de rango`() {
        val result = validateRange(15000L, 0L, 10000L, "Amount")
        assertEquals("Amount must be between 0 and 10000", result)
    }

    // ========== Tests de validateRange (Double) ==========

    @Test
    fun `validateRange Double retorna null para valor válido`() {
        val result = validateRange(5.5, 0.0, 10.0, "Price")
        assertNull(result)
    }

    @Test
    fun `validateRange Double retorna error para valor fuera de rango`() {
        val result = validateRange(-1.0, 0.0, 10.0, "Price")
        assertEquals("Price must be between 0.0 and 10.0", result)
    }

    @Test
    fun `validateRange Double con límites decimales`() {
        val result = validateRange(5.5, 5.0, 6.0, "Value")
        assertNull(result)
    }

    // ========== Tests de validateEmail ==========

    @Test
    fun `validateEmail retorna null para email válido`() {
        val result = validateEmail("test@example.com")
        assertNull(result)
    }

    @Test
    fun `validateEmail retorna null para email con subdomain`() {
        val result = validateEmail("user@mail.example.com")
        assertNull(result)
    }

    @Test
    fun `validateEmail retorna null para email con números`() {
        val result = validateEmail("user123@example456.com")
        assertNull(result)
    }

    @Test
    fun `validateEmail retorna error para email sin @`() {
        val result = validateEmail("invalid.email.com")
        assertEquals("Invalid email format", result)
    }

    @Test
    fun `validateEmail retorna error para email con múltiples @`() {
        val result = validateEmail("test@@example.com")
        assertEquals("Invalid email format", result)
    }

    @Test
    fun `validateEmail retorna error para email sin local part`() {
        val result = validateEmail("@example.com")
        assertEquals("Invalid email format", result)
    }

    @Test
    fun `validateEmail retorna error para email sin domain`() {
        val result = validateEmail("test@")
        assertEquals("Invalid email format", result)
    }

    @Test
    fun `validateEmail retorna error para string vacío`() {
        val result = validateEmail("")
        assertEquals("Invalid email format", result)
    }

    @Test
    fun `validateEmail retorna error para null`() {
        val result = validateEmail(null)
        assertEquals("Invalid email format", result)
    }

    @Test
    fun `validateEmail con custom message`() {
        val result = validateEmail("invalid", "Please enter valid email")
        assertEquals("Please enter valid email", result)
    }

    // ========== Tests de validateMinLength ==========

    @Test
    fun `validateMinLength retorna null para longitud válida`() {
        val result = validateMinLength("hello", 3, "Username")
        assertNull(result)
    }

    @Test
    fun `validateMinLength retorna null para longitud exacta`() {
        val result = validateMinLength("abc", 3, "Username")
        assertNull(result)
    }

    @Test
    fun `validateMinLength retorna error para longitud insuficiente`() {
        val result = validateMinLength("ab", 3, "Username")
        assertEquals("Username must be at least 3 characters", result)
    }

    @Test
    fun `validateMinLength retorna error para null`() {
        val result = validateMinLength(null, 3, "Username")
        assertEquals("Username must be at least 3 characters", result)
    }

    @Test
    fun `validateMinLength con custom message`() {
        val result = validateMinLength("ab", 3, "Username", "Too short")
        assertEquals("Too short", result)
    }

    // ========== Tests de validateMaxLength ==========

    @Test
    fun `validateMaxLength retorna null para longitud válida`() {
        val result = validateMaxLength("hello", 10, "Username")
        assertNull(result)
    }

    @Test
    fun `validateMaxLength retorna null para longitud exacta`() {
        val result = validateMaxLength("1234567890", 10, "Username")
        assertNull(result)
    }

    @Test
    fun `validateMaxLength retorna error para longitud excesiva`() {
        val result = validateMaxLength("12345678901", 10, "Username")
        assertEquals("Username must be at most 10 characters", result)
    }

    @Test
    fun `validateMaxLength retorna error para null`() {
        val result = validateMaxLength(null, 10, "Username")
        assertEquals("Username cannot be null", result)
    }

    @Test
    fun `validateMaxLength con custom message`() {
        val result = validateMaxLength("verylongstring", 5, "Username", "Too long")
        assertEquals("Too long", result)
    }

    // ========== Tests de validateLengthRange ==========

    @Test
    fun `validateLengthRange retorna null para longitud válida`() {
        val result = validateLengthRange("hello", 3, 10, "Username")
        assertNull(result)
    }

    @Test
    fun `validateLengthRange retorna null para límite inferior`() {
        val result = validateLengthRange("abc", 3, 10, "Username")
        assertNull(result)
    }

    @Test
    fun `validateLengthRange retorna null para límite superior`() {
        val result = validateLengthRange("1234567890", 3, 10, "Username")
        assertNull(result)
    }

    @Test
    fun `validateLengthRange retorna error para longitud menor`() {
        val result = validateLengthRange("ab", 3, 10, "Username")
        assertEquals("Username must be between 3 and 10 characters", result)
    }

    @Test
    fun `validateLengthRange retorna error para longitud mayor`() {
        val result = validateLengthRange("12345678901", 3, 10, "Username")
        assertEquals("Username must be between 3 and 10 characters", result)
    }

    @Test
    fun `validateLengthRange retorna error para null`() {
        val result = validateLengthRange(null, 3, 10, "Username")
        assertEquals("Username cannot be null", result)
    }

    // ========== Tests de validatePositive ==========

    @Test
    fun `validatePositive Int retorna null para valor positivo`() {
        val result = validatePositive(10, "Amount")
        assertNull(result)
    }

    @Test
    fun `validatePositive Int retorna error para cero`() {
        val result = validatePositive(0, "Amount")
        assertEquals("Amount must be positive", result)
    }

    @Test
    fun `validatePositive Int retorna error para negativo`() {
        val result = validatePositive(-5, "Amount")
        assertEquals("Amount must be positive", result)
    }

    @Test
    fun `validatePositive Double retorna null para valor positivo`() {
        val result = validatePositive(5.5, "Price")
        assertNull(result)
    }

    @Test
    fun `validatePositive Double retorna error para cero`() {
        val result = validatePositive(0.0, "Price")
        assertEquals("Price must be positive", result)
    }

    @Test
    fun `validatePositive con custom message`() {
        val result = validatePositive(-1, "Amount", "Value must be greater than zero")
        assertEquals("Value must be greater than zero", result)
    }

    // ========== Tests de validateNonNegative ==========

    @Test
    fun `validateNonNegative Int retorna null para valor positivo`() {
        val result = validateNonNegative(10, "Quantity")
        assertNull(result)
    }

    @Test
    fun `validateNonNegative Int retorna null para cero`() {
        val result = validateNonNegative(0, "Quantity")
        assertNull(result)
    }

    @Test
    fun `validateNonNegative Int retorna error para negativo`() {
        val result = validateNonNegative(-5, "Quantity")
        assertEquals("Quantity cannot be negative", result)
    }

    @Test
    fun `validateNonNegative Double retorna null para cero`() {
        val result = validateNonNegative(0.0, "Balance")
        assertNull(result)
    }

    @Test
    fun `validateNonNegative Double retorna error para negativo`() {
        val result = validateNonNegative(-1.5, "Balance")
        assertEquals("Balance cannot be negative", result)
    }

    // ========== Tests de validatePattern ==========

    @Test
    fun `validatePattern retorna null para patrón válido`() {
        val result = validatePattern("123-456", Regex("\\d{3}-\\d{3}"), "Phone")
        assertNull(result)
    }

    @Test
    fun `validatePattern retorna error para patrón inválido`() {
        val result = validatePattern("abc-def", Regex("\\d{3}-\\d{3}"), "Phone")
        assertEquals("Phone has invalid format", result)
    }

    @Test
    fun `validatePattern retorna error para null`() {
        val result = validatePattern(null, Regex("\\d+"), "Code")
        assertEquals("Code has invalid format", result)
    }

    @Test
    fun `validatePattern con patrón de email`() {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        val result = validatePattern("test@example.com", emailRegex, "Email")
        assertNull(result)
    }

    @Test
    fun `validatePattern con custom message`() {
        val result = validatePattern("invalid", Regex("\\d+"), "Code", "Must be numeric")
        assertEquals("Must be numeric", result)
    }

    // ========== Tests de validateIn ==========

    @Test
    fun `validateIn retorna null para valor permitido`() {
        val result = validateIn("admin", listOf("admin", "user", "guest"), "Role")
        assertNull(result)
    }

    @Test
    fun `validateIn retorna error para valor no permitido`() {
        val result = validateIn("superuser", listOf("admin", "user", "guest"), "Role")
        assertEquals("Role must be one of: admin, user, guest", result)
    }

    @Test
    fun `validateIn con números`() {
        val result = validateIn(5, listOf(1, 2, 3, 4, 5), "Priority")
        assertNull(result)
    }

    @Test
    fun `validateIn con custom message`() {
        val result = validateIn("invalid", listOf("a", "b", "c"), "Option", "Invalid option")
        assertEquals("Invalid option", result)
    }

    // ========== Tests de validateNotEmpty ==========

    @Test
    fun `validateNotEmpty retorna null para colección con elementos`() {
        val result = validateNotEmpty(listOf(1, 2, 3), "Items")
        assertNull(result)
    }

    @Test
    fun `validateNotEmpty retorna error para colección vacía`() {
        val result = validateNotEmpty(emptyList<Int>(), "Items")
        assertEquals("Items cannot be empty", result)
    }

    @Test
    fun `validateNotEmpty retorna error para null`() {
        val result = validateNotEmpty<Int>(null, "Items")
        assertEquals("Items cannot be empty", result)
    }

    @Test
    fun `validateNotEmpty con un solo elemento`() {
        val result = validateNotEmpty(listOf("single"), "Tags")
        assertNull(result)
    }

    // ========== Tests de validateMinSize ==========

    @Test
    fun `validateMinSize retorna null para tamaño válido`() {
        val result = validateMinSize(listOf(1, 2, 3), 2, "Tags")
        assertNull(result)
    }

    @Test
    fun `validateMinSize retorna null para tamaño exacto`() {
        val result = validateMinSize(listOf(1, 2), 2, "Tags")
        assertNull(result)
    }

    @Test
    fun `validateMinSize retorna error para tamaño insuficiente`() {
        val result = validateMinSize(listOf(1), 2, "Tags")
        assertEquals("Tags must contain at least 2 items", result)
    }

    @Test
    fun `validateMinSize retorna error para null`() {
        val result = validateMinSize<String>(null, 2, "Tags")
        assertEquals("Tags must contain at least 2 items", result)
    }

    // ========== Tests de validateMaxSize ==========

    @Test
    fun `validateMaxSize retorna null para tamaño válido`() {
        val result = validateMaxSize(listOf(1, 2, 3), 5, "Tags")
        assertNull(result)
    }

    @Test
    fun `validateMaxSize retorna null para tamaño exacto`() {
        val result = validateMaxSize(listOf(1, 2, 3, 4, 5), 5, "Tags")
        assertNull(result)
    }

    @Test
    fun `validateMaxSize retorna error para tamaño excesivo`() {
        val result = validateMaxSize(listOf(1, 2, 3, 4, 5, 6), 5, "Tags")
        assertEquals("Tags cannot contain more than 5 items", result)
    }

    @Test
    fun `validateMaxSize retorna null para null`() {
        val result = validateMaxSize<String>(null, 5, "Tags")
        assertNull(result)
    }

    // ========== Tests de Composición ==========

    @Test
    fun `múltiples validaciones usando listOfNotNull`() {
        val errors = listOfNotNull(
            validateEmail("test@example.com"),
            validateRange(25, 18, 120, "Age"),
            validateLengthRange("username", 3, 30, "Username")
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `múltiples validaciones capturan todos los errores`() {
        val errors = listOfNotNull(
            validateEmail("invalid"),
            validateRange(15, 18, 120, "Age"),
            validateLengthRange("ab", 3, 30, "Username")
        )

        assertEquals(3, errors.size)
        assertTrue(errors.any { it.contains("email") })
        assertTrue(errors.any { it.contains("Age") })
        assertTrue(errors.any { it.contains("Username") })
    }

    // ========== Tests de Casos Edge ==========

    @Test
    fun `validateEmail con email muy largo`() {
        val longEmail = "a".repeat(100) + "@example.com"
        val result = validateEmail(longEmail)
        assertNull(result) // Email válido aunque sea largo
    }

    @Test
    fun `validateRange con rangos negativos`() {
        val result = validateRange(-5, -10, 0, "Temperature")
        assertNull(result)
    }

    @Test
    fun `validateLengthRange con rango de un solo carácter`() {
        val result = validateLengthRange("a", 1, 1, "Code")
        assertNull(result)
    }

    @Test
    fun `validatePattern con string vacío y patrón que permite vacío`() {
        val result = validatePattern("", Regex(".*"), "Optional")
        assertNull(result)
    }

    @Test
    fun `validateIn con colección vacía`() {
        val result = validateIn("value", emptyList(), "Field")
        assertEquals("Field must be one of: ", result)
    }

    @Test
    fun `validateMinSize con tamaño 0`() {
        val result = validateMinSize(emptyList<String>(), 0, "Items")
        assertNull(result)
    }

    @Test
    fun `validateMaxSize con tamaño 0 permite solo colecciones vacías`() {
        val result1 = validateMaxSize(emptyList<String>(), 0, "Items")
        assertNull(result1)

        val result2 = validateMaxSize(listOf("item"), 0, "Items")
        assertEquals("Items cannot contain more than 0 items", result2)
    }
}
