package com.edugo.test.module.integration

import com.edugo.test.module.core.Result
import com.edugo.test.module.extensions.sequenceCollectingErrors
import com.edugo.test.module.extensions.traverseCollectingErrors
import com.edugo.test.module.extensions.zip3
import com.edugo.test.module.validation.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests de integración para el sistema de validación completo.
 *
 * Verifica:
 * - Validación acumulativa vs fail-fast
 * - Composición de validaciones con ValidationHelpers
 * - Integración de zip con validaciones
 * - Escenarios de validación de formularios complejos
 * - Edge cases de validación
 */
class ValidationIntegrationTest {

    // ========== Test 1: Validación Acumulativa Completa ==========

    data class CompleteUserForm(
        val email: String,
        val password: String,
        val confirmPassword: String,
        val username: String,
        val age: Int,
        val terms: Boolean
    )

    @Test
    fun `validación acumulativa de formulario completo captura todos los errores`() {
        val form = CompleteUserForm(
            email = "invalid-email",
            password = "123",
            confirmPassword = "456",
            username = "ab",
            age = 15,
            terms = false
        )

        val result = accumulateValidationErrors {
            add(validateEmail(form.email))
            add(validateMinLength(form.password, 8, "Password"))

            if (form.password != form.confirmPassword) {
                add("Passwords do not match")
            }

            add(validateLengthRange(form.username, 3, 20, "Username"))
            add(validateRange(form.age, 18, 120, "Age"))

            if (!form.terms) {
                add("Must accept terms and conditions")
            }
        }

        assertIs<Result.Failure>(result)

        // Debe contener todos los 6 errores
        val errorMessage = result.error
        assertTrue(errorMessage.contains("email"), "Should contain email error")
        assertTrue(errorMessage.contains("Password"), "Should contain password error")
        assertTrue(errorMessage.contains("Passwords do not match"), "Should contain password match error")
        assertTrue(errorMessage.contains("Username"), "Should contain username error")
        assertTrue(errorMessage.contains("Age"), "Should contain age error")
        assertTrue(errorMessage.contains("terms"), "Should contain terms error")
    }

    @Test
    fun `validación acumulativa pasa si todos los campos son válidos`() {
        val form = CompleteUserForm(
            email = "user@test.com",
            password = "securepass123",
            confirmPassword = "securepass123",
            username = "johndoe",
            age = 25,
            terms = true
        )

        val result = accumulateValidationErrors {
            add(validateEmail(form.email))
            add(validateMinLength(form.password, 8, "Password"))

            if (form.password != form.confirmPassword) {
                add("Passwords do not match")
            }

            add(validateLengthRange(form.username, 3, 20, "Username"))
            add(validateRange(form.age, 18, 120, "Age"))

            if (!form.terms) {
                add("Must accept terms and conditions")
            }
        }

        assertIs<Result.Success<Unit>>(result)
    }

    // ========== Test 2: Validación con zip3 para Independencia ==========

    @Test
    fun `zip3 valida tres campos independientemente pero combina resultados`() {
        data class LoginForm(val email: String, val password: String, val remember: Boolean)

        val form = LoginForm("user@test.com", "securepass", true)

        // Validar cada campo independientemente
        val emailResult = validateEmail(form.email)?.let {
            Result.Failure(it)
        } ?: Result.Success(form.email)

        val passwordResult = if (form.password.length >= 6) {
            Result.Success(form.password)
        } else {
            Result.Failure("Password must be at least 6 characters")
        }

        val rememberResult = Result.Success(form.remember)

        val result = zip3(emailResult, passwordResult, rememberResult) { email, pass, rem ->
            LoginForm(email, pass, rem)
        }

        assertIs<Result.Success<LoginForm>>(result)
    }

    @Test
    fun `zip3 retorna primer error cuando algún campo falla`() {
        val emailResult: Result<String> = Result.Failure("Invalid email")
        val passwordResult: Result<String> = Result.Failure("Password too short")
        val usernameResult: Result<String> = Result.Failure("Username taken")

        val result = zip3(emailResult, passwordResult, usernameResult) { _, _, _ ->
            "combined"
        }

        assertIs<Result.Failure>(result)
        assertEquals("Invalid email", result.error)
    }

    // ========== Test 3: Validación de Listas con sequenceCollectingErrors ==========

    data class BulkUserForm(val email: String, val age: Int)

    @Test
    fun `sequenceCollectingErrors acumula errores de múltiples usuarios`() {
        val forms = listOf(
            BulkUserForm("user1@test.com", 25),
            BulkUserForm("invalid", 15),
            BulkUserForm("user3@test.com", 30),
            BulkUserForm("bad-email", 200)
        )

        val validationResults = forms.map { form ->
            accumulateValidationErrors {
                add(validateEmail(form.email))
                add(validateRange(form.age, 18, 120, "Age"))
            }
        }

        val result = validationResults.sequenceCollectingErrors()

        assertIs<Result.Failure>(result)

        // Debe contener errores de los índices 1 y 3
        val errorMsg = result.error
        assertTrue(errorMsg.contains("email") || errorMsg.contains("Email"))
        assertTrue(errorMsg.contains("Age"))
    }

    // ========== Test 4: traverseCollectingErrors para Validación Masiva ==========

    @Test
    fun `traverseCollectingErrors valida y recolecta todos los errores con índices`() {
        val emails = listOf(
            "valid1@test.com",
            "invalid1",
            "valid2@test.com",
            "invalid2",
            "valid3@test.com"
        )

        val result = emails.traverseCollectingErrors { email ->
            validateEmail(email)?.let { Result.Failure(it) } ?: Result.Success(email)
        }

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("Index 1"))
        assertTrue(result.error.contains("Index 3"))
    }

    // ========== Test 5: Validación Condicional Compleja ==========

    @Test
    fun `validateIf aplica validación solo cuando condición es true`() {
        data class PaymentForm(
            val amount: Double,
            val useCredit: Boolean,
            val creditCardNumber: String?
        )

        val form = PaymentForm(
            amount = 100.0,
            useCredit = true,
            creditCardNumber = null
        )

        val result = accumulateValidationErrors {
            add(validatePositive(form.amount, "Amount"))

            add(validateIf(form.useCredit) {
                validateNotBlank(form.creditCardNumber, "Credit card number")
            })
        }

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("Credit card"))
    }

    @Test
    fun `validateIf no aplica validación cuando condición es false`() {
        data class PaymentForm(
            val amount: Double,
            val useCredit: Boolean,
            val creditCardNumber: String?
        )

        val form = PaymentForm(
            amount = 100.0,
            useCredit = false,
            creditCardNumber = null
        )

        val result = accumulateValidationErrors {
            add(validatePositive(form.amount, "Amount"))

            add(validateIf(form.useCredit) {
                validateNotBlank(form.creditCardNumber, "Credit card number")
            })
        }

        assertIs<Result.Success<Unit>>(result)
    }

    // ========== Test 6: validateAtLeastOne para Validación OR ==========

    @Test
    fun `validateAtLeastOne pasa si al menos una validación es exitosa`() {
        data class ContactForm(val email: String?, val phone: String?)

        val form = ContactForm(email = "test@example.com", phone = null)

        val result = accumulateValidationErrors {
            add(validateAtLeastOne(
                "Must provide email or phone",
                { validateEmail(form.email) },
                { validateNotBlank(form.phone, "Phone") }
            ))
        }

        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `validateAtLeastOne falla si todas las validaciones fallan`() {
        data class ContactForm(val email: String?, val phone: String?)

        val form = ContactForm(email = null, phone = null)

        val result = accumulateValidationErrors {
            add(validateAtLeastOne(
                "Must provide email or phone",
                { validateEmail(form.email) },
                { validateNotBlank(form.phone, "Phone") }
            ))
        }

        assertIs<Result.Failure>(result)
        assertEquals("Must provide email or phone", result.error)
    }

    // ========== Test 7: validateAll para Validación AND ==========

    @Test
    fun `validateAll retorna primer error encontrado`() {
        val result = accumulateValidationErrors {
            add(validateAll(
                { validateEmail("invalid") },
                { validateRange(15, 18, 120, "Age") },
                { validateLengthRange("ab", 3, 20, "Username") }
            ))
        }

        assertIs<Result.Failure>(result)
        assertEquals("Invalid email format", result.error)
    }

    // ========== Test 8: Validación de Colecciones ==========

    @Test
    fun `validateNotEmpty falla con lista vacía`() {
        val emptyList = emptyList<String>()

        val result = accumulateValidationErrors {
            add(validateNotEmpty(emptyList, "Tags"))
        }

        assertIs<Result.Failure>(result)
        assertEquals("Tags cannot be empty", result.error)
    }

    @Test
    fun `validateMinSize valida tamaño mínimo de colección`() {
        val tags = listOf("tag1")

        val result = accumulateValidationErrors {
            add(validateMinSize(tags, 2, "Tags"))
        }

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("at least 2"))
    }

    @Test
    fun `validateMaxSize valida tamaño máximo de colección`() {
        val tags = listOf("tag1", "tag2", "tag3", "tag4", "tag5", "tag6")

        val result = accumulateValidationErrors {
            add(validateMaxSize(tags, 5, "Tags"))
        }

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("more than 5"))
    }

    // ========== Test 9: Validación con Regex Patterns ==========

    @Test
    fun `validatePattern valida formato específico`() {
        val phonePattern = Regex("^\\d{3}-\\d{3}-\\d{4}$")

        val validPhone = "555-123-4567"
        val invalidPhone = "5551234567"

        val result1 = validatePattern(validPhone, phonePattern, "Phone")
        val result2 = validatePattern(invalidPhone, phonePattern, "Phone")

        assertEquals(null, result1)
        assertEquals("Phone has invalid format", result2)
    }

    // ========== Test 10: Escenario Real Complejo de Registro ==========

    data class FullRegistrationForm(
        val username: String,
        val email: String,
        val password: String,
        val confirmPassword: String,
        val age: Int,
        val country: String,
        val interests: List<String>,
        val acceptTerms: Boolean,
        val newsletter: Boolean
    )

    @Test
    fun `validación completa de formulario de registro con todos los helpers`() {
        val form = FullRegistrationForm(
            username = "john_doe_123",
            email = "john@example.com",
            password = "SecureP@ss123",
            confirmPassword = "SecureP@ss123",
            age = 25,
            country = "USA",
            interests = listOf("tech", "sports"),
            acceptTerms = true,
            newsletter = false
        )

        val result = accumulateValidationErrors {
            // Username: 3-20 caracteres
            add(validateLengthRange(form.username, 3, 20, "Username"))

            // Email: formato válido
            add(validateEmail(form.email))

            // Password: mínimo 8 caracteres
            add(validateMinLength(form.password, 8, "Password"))

            // Passwords coinciden
            if (form.password != form.confirmPassword) {
                add("Passwords do not match")
            }

            // Edad: 18-120
            add(validateRange(form.age, 18, 120, "Age"))

            // País: de lista permitida
            add(validateIn(form.country, listOf("USA", "Canada", "Mexico", "UK"), "Country"))

            // Intereses: al menos 1, máximo 5
            add(validateMinSize(form.interests, 1, "Interests"))
            add(validateMaxSize(form.interests, 5, "Interests"))

            // Terms: debe ser true
            if (!form.acceptTerms) {
                add("Must accept terms and conditions")
            }
        }

        assertIs<Result.Success<Unit>>(result)
    }

    @Test
    fun `validación completa captura múltiples errores en formulario inválido`() {
        val form = FullRegistrationForm(
            username = "ab",                    // Muy corto
            email = "invalid",                  // Sin @
            password = "123",                   // Muy corto
            confirmPassword = "456",            // No coincide
            age = 15,                           // Menor de edad
            country = "Invalid",                // No en lista
            interests = emptyList(),            // Vacío
            acceptTerms = false,                // No aceptado
            newsletter = false
        )

        val result = accumulateValidationErrors {
            add(validateLengthRange(form.username, 3, 20, "Username"))
            add(validateEmail(form.email))
            add(validateMinLength(form.password, 8, "Password"))

            if (form.password != form.confirmPassword) {
                add("Passwords do not match")
            }

            add(validateRange(form.age, 18, 120, "Age"))
            add(validateIn(form.country, listOf("USA", "Canada", "Mexico", "UK"), "Country"))
            add(validateMinSize(form.interests, 1, "Interests"))

            if (!form.acceptTerms) {
                add("Must accept terms and conditions")
            }
        }

        assertIs<Result.Failure>(result)

        // Verificar que contiene todos los errores
        val errorMsg = result.error
        assertTrue(errorMsg.contains("Username"))
        assertTrue(errorMsg.contains("email"))
        assertTrue(errorMsg.contains("Password"))
        assertTrue(errorMsg.contains("Passwords do not match"))
        assertTrue(errorMsg.contains("Age"))
        assertTrue(errorMsg.contains("Country"))
        assertTrue(errorMsg.contains("Interests"))
        assertTrue(errorMsg.contains("terms"))
    }
}
