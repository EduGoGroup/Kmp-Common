package com.edugo.test.module.extensions

import com.edugo.test.module.core.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Suite de tests para ResultCombinators (zip3, zip4, zip5).
 *
 * Verifica:
 * - zip3 con diferentes combinaciones de Success/Failure/Loading
 * - zip4 con diferentes combinaciones
 * - zip5 con diferentes combinaciones
 * - Extension functions (this.zip3, this.zip4, this.zip5)
 * - Type-safety con tipos diferentes
 * - Fail-fast behavior (primer error)
 * - Loading priority (después de Failure)
 * - Casos edge y performance
 */
class ResultCombinatorsTest {

    // ========== Tests de zip3 ==========

    @Test
    fun `zip3 combina tres Success exitosamente`() {
        val a: Result<String> = Result.Success("Hello")
        val b: Result<Int> = Result.Success(42)
        val c: Result<Boolean> = Result.Success(true)

        val result = zip3(a, b, c) { str, num, bool ->
            "$str-$num-$bool"
        }

        assertIs<Result.Success<String>>(result)
        assertEquals("Hello-42-true", result.data)
    }

    @Test
    fun `zip3 retorna primer Failure si el primero falla`() {
        val a: Result<String> = Result.Failure("Error A")
        val b: Result<Int> = Result.Success(42)
        val c: Result<Boolean> = Result.Success(true)

        val result = zip3(a, b, c) { str, num, bool ->
            "$str-$num-$bool"
        }

        assertIs<Result.Failure>(result)
        assertEquals("Error A", result.error)
    }

    @Test
    fun `zip3 retorna segundo Failure si el segundo falla`() {
        val a: Result<String> = Result.Success("Hello")
        val b: Result<Int> = Result.Failure("Error B")
        val c: Result<Boolean> = Result.Success(true)

        val result = zip3(a, b, c) { str, num, bool ->
            "$str-$num-$bool"
        }

        assertIs<Result.Failure>(result)
        assertEquals("Error B", result.error)
    }

    @Test
    fun `zip3 retorna tercer Failure si el tercero falla`() {
        val a: Result<String> = Result.Success("Hello")
        val b: Result<Int> = Result.Success(42)
        val c: Result<Boolean> = Result.Failure("Error C")

        val result = zip3(a, b, c) { str, num, bool ->
            "$str-$num-$bool"
        }

        assertIs<Result.Failure>(result)
        assertEquals("Error C", result.error)
    }

    @Test
    fun `zip3 retorna primer Failure si múltiples fallan`() {
        val a: Result<String> = Result.Failure("Error A")
        val b: Result<Int> = Result.Failure("Error B")
        val c: Result<Boolean> = Result.Failure("Error C")

        val result = zip3(a, b, c) { str, num, bool ->
            "$str-$num-$bool"
        }

        assertIs<Result.Failure>(result)
        assertEquals("Error A", result.error)
    }

    @Test
    fun `zip3 retorna Loading si el primero es Loading`() {
        val a: Result<String> = Result.Loading
        val b: Result<Int> = Result.Success(42)
        val c: Result<Boolean> = Result.Success(true)

        val result = zip3(a, b, c) { str, num, bool ->
            "$str-$num-$bool"
        }

        assertIs<Result.Loading>(result)
    }

    @Test
    fun `zip3 retorna Loading si el segundo es Loading`() {
        val a: Result<String> = Result.Success("Hello")
        val b: Result<Int> = Result.Loading
        val c: Result<Boolean> = Result.Success(true)

        val result = zip3(a, b, c) { str, num, bool ->
            "$str-$num-$bool"
        }

        assertIs<Result.Loading>(result)
    }

    @Test
    fun `zip3 prioriza Failure sobre Loading`() {
        val a: Result<String> = Result.Failure("Error A")
        val b: Result<Int> = Result.Loading
        val c: Result<Boolean> = Result.Success(true)

        val result = zip3(a, b, c) { str, num, bool ->
            "$str-$num-$bool"
        }

        assertIs<Result.Failure>(result)
        assertEquals("Error A", result.error)
    }

    @Test
    fun `zip3 extension function combina correctamente`() {
        val a: Result<String> = Result.Success("Test")
        val b: Result<Int> = Result.Success(123)
        val c: Result<Boolean> = Result.Success(false)

        val result = a.zip3(b, c) { str, num, bool ->
            Triple(str, num, bool)
        }

        assertIs<Result.Success<Triple<String, Int, Boolean>>>(result)
        assertEquals(Triple("Test", 123, false), result.data)
    }

    // ========== Tests de zip4 ==========

    @Test
    fun `zip4 combina cuatro Success exitosamente`() {
        val a: Result<String> = Result.Success("A")
        val b: Result<String> = Result.Success("B")
        val c: Result<String> = Result.Success("C")
        val d: Result<String> = Result.Success("D")

        val result = zip4(a, b, c, d) { s1, s2, s3, s4 ->
            "$s1$s2$s3$s4"
        }

        assertIs<Result.Success<String>>(result)
        assertEquals("ABCD", result.data)
    }

    @Test
    fun `zip4 retorna primer Failure`() {
        val a: Result<String> = Result.Success("A")
        val b: Result<String> = Result.Failure("Error B")
        val c: Result<String> = Result.Success("C")
        val d: Result<String> = Result.Failure("Error D")

        val result = zip4(a, b, c, d) { s1, s2, s3, s4 ->
            "$s1$s2$s3$s4"
        }

        assertIs<Result.Failure>(result)
        assertEquals("Error B", result.error)
    }

    @Test
    fun `zip4 maneja tipos diferentes`() {
        val a: Result<String> = Result.Success("Name")
        val b: Result<Int> = Result.Success(25)
        val c: Result<String> = Result.Success("email@test.com")
        val d: Result<Boolean> = Result.Success(true)

        data class User(val name: String, val age: Int, val email: String, val active: Boolean)

        val result = zip4(a, b, c, d) { name, age, email, active ->
            User(name, age, email, active)
        }

        assertIs<Result.Success<User>>(result)
        assertEquals("Name", result.data.name)
        assertEquals(25, result.data.age)
        assertEquals("email@test.com", result.data.email)
        assertEquals(true, result.data.active)
    }

    @Test
    fun `zip4 retorna Loading si alguno es Loading y ninguno es Failure`() {
        val a: Result<String> = Result.Success("A")
        val b: Result<String> = Result.Loading
        val c: Result<String> = Result.Success("C")
        val d: Result<String> = Result.Success("D")

        val result = zip4(a, b, c, d) { s1, s2, s3, s4 ->
            "$s1$s2$s3$s4"
        }

        assertIs<Result.Loading>(result)
    }

    @Test
    fun `zip4 extension function funciona correctamente`() {
        val a: Result<Int> = Result.Success(1)
        val b: Result<Int> = Result.Success(2)
        val c: Result<Int> = Result.Success(3)
        val d: Result<Int> = Result.Success(4)

        val result = a.zip4(b, c, d) { n1, n2, n3, n4 ->
            n1 + n2 + n3 + n4
        }

        assertIs<Result.Success<Int>>(result)
        assertEquals(10, result.data)
    }

    // ========== Tests de zip5 ==========

    @Test
    fun `zip5 combina cinco Success exitosamente`() {
        val a: Result<String> = Result.Success("One")
        val b: Result<String> = Result.Success("Two")
        val c: Result<String> = Result.Success("Three")
        val d: Result<String> = Result.Success("Four")
        val e: Result<String> = Result.Success("Five")

        val result = zip5(a, b, c, d, e) { s1, s2, s3, s4, s5 ->
            listOf(s1, s2, s3, s4, s5)
        }

        assertIs<Result.Success<List<String>>>(result)
        assertEquals(listOf("One", "Two", "Three", "Four", "Five"), result.data)
    }

    @Test
    fun `zip5 retorna primer Failure en orden`() {
        val a: Result<String> = Result.Success("A")
        val b: Result<String> = Result.Success("B")
        val c: Result<String> = Result.Failure("Error C")
        val d: Result<String> = Result.Success("D")
        val e: Result<String> = Result.Failure("Error E")

        val result = zip5(a, b, c, d, e) { s1, s2, s3, s4, s5 ->
            "$s1$s2$s3$s4$s5"
        }

        assertIs<Result.Failure>(result)
        assertEquals("Error C", result.error)
    }

    @Test
    fun `zip5 maneja tipos diferentes y complejos`() {
        data class Address(
            val street: String,
            val city: String,
            val state: String,
            val zip: String,
            val country: String
        )

        val street: Result<String> = Result.Success("123 Main St")
        val city: Result<String> = Result.Success("Springfield")
        val state: Result<String> = Result.Success("IL")
        val zip: Result<String> = Result.Success("62701")
        val country: Result<String> = Result.Success("USA")

        val result = zip5(street, city, state, zip, country) { s, c, st, z, co ->
            Address(s, c, st, z, co)
        }

        assertIs<Result.Success<Address>>(result)
        assertEquals("123 Main St", result.data.street)
        assertEquals("Springfield", result.data.city)
        assertEquals("IL", result.data.state)
        assertEquals("62701", result.data.zip)
        assertEquals("USA", result.data.country)
    }

    @Test
    fun `zip5 con todos los tipos de Result`() {
        val a: Result<Int> = Result.Success(1)
        val b: Result<Int> = Result.Failure("Error")
        val c: Result<Int> = Result.Loading
        val d: Result<Int> = Result.Success(4)
        val e: Result<Int> = Result.Success(5)

        val result = zip5(a, b, c, d, e) { n1, n2, n3, n4, n5 ->
            n1 + n2 + n3 + n4 + n5
        }

        // Failure tiene prioridad sobre Loading
        assertIs<Result.Failure>(result)
        assertEquals("Error", result.error)
    }

    @Test
    fun `zip5 extension function funciona correctamente`() {
        val a: Result<Int> = Result.Success(10)
        val b: Result<Int> = Result.Success(20)
        val c: Result<Int> = Result.Success(30)
        val d: Result<Int> = Result.Success(40)
        val e: Result<Int> = Result.Success(50)

        val result = a.zip5(b, c, d, e) { n1, n2, n3, n4, n5 ->
            n1 + n2 + n3 + n4 + n5
        }

        assertIs<Result.Success<Int>>(result)
        assertEquals(150, result.data)
    }

    // ========== Tests de Casos de Uso Reales ==========

    @Test
    fun `zip3 para validación de formulario de login`() {
        data class LoginForm(val username: String, val password: String, val remember: Boolean)

        fun validateUsername(username: String): Result<String> {
            return if (username.length >= 3) {
                Result.Success(username)
            } else {
                Result.Failure("Username must be at least 3 characters")
            }
        }

        fun validatePassword(password: String): Result<String> {
            return if (password.length >= 6) {
                Result.Success(password)
            } else {
                Result.Failure("Password must be at least 6 characters")
            }
        }

        val usernameResult = validateUsername("john")
        val passwordResult = validatePassword("secret123")
        val rememberResult = Result.Success(true)

        val loginResult = zip3(usernameResult, passwordResult, rememberResult) { user, pass, rem ->
            LoginForm(user, pass, rem)
        }

        assertIs<Result.Success<LoginForm>>(loginResult)
        assertEquals("john", loginResult.data.username)
        assertEquals("secret123", loginResult.data.password)
        assertTrue(loginResult.data.remember)
    }

    @Test
    fun `zip4 para validación de registro completo`() {
        data class RegisterForm(
            val username: String,
            val email: String,
            val password: String,
            val confirmPassword: String
        )

        val username = Result.Success("johndoe")
        val email = Result.Success("john@example.com")
        val password = Result.Success("securepass123")
        val confirm = Result.Success("securepass123")

        val registerResult = zip4(username, email, password, confirm) { u, e, p, c ->
            RegisterForm(u, e, p, c)
        }

        assertIs<Result.Success<RegisterForm>>(registerResult)
    }

    @Test
    fun `zip5 para crear dirección completa`() {
        data class Address(
            val street: String,
            val city: String,
            val state: String,
            val zipCode: String,
            val country: String
        )

        val street = Result.Success("456 Oak Avenue")
        val city = Result.Success("Boston")
        val state = Result.Success("MA")
        val zipCode = Result.Success("02101")
        val country = Result.Success("USA")

        val addressResult = zip5(street, city, state, zipCode, country) { s, c, st, z, co ->
            Address(s, c, st, z, co)
        }

        assertIs<Result.Success<Address>>(addressResult)
        assertEquals("456 Oak Avenue", addressResult.data.street)
    }

    // ========== Tests de Performance y Edge Cases ==========

    @Test
    fun `zip3 no ejecuta transform si hay Failure`() {
        var transformExecuted = false

        val a: Result<String> = Result.Failure("Error")
        val b: Result<Int> = Result.Success(42)
        val c: Result<Boolean> = Result.Success(true)

        zip3(a, b, c) { _, _, _ ->
            transformExecuted = true
            "result"
        }

        assertTrue(!transformExecuted, "Transform should not execute on Failure")
    }

    @Test
    fun `zip4 con tipos complejos anidados`() {
        data class Nested(val value: String)
        data class Complex(val a: Nested, val b: Nested, val c: Nested, val d: Nested)

        val a = Result.Success(Nested("A"))
        val b = Result.Success(Nested("B"))
        val c = Result.Success(Nested("C"))
        val d = Result.Success(Nested("D"))

        val result = zip4(a, b, c, d) { n1, n2, n3, n4 ->
            Complex(n1, n2, n3, n4)
        }

        assertIs<Result.Success<Complex>>(result)
    }

    @Test
    fun `zip5 con listas como tipos`() {
        val a: Result<List<Int>> = Result.Success(listOf(1, 2, 3))
        val b: Result<List<Int>> = Result.Success(listOf(4, 5, 6))
        val c: Result<List<Int>> = Result.Success(listOf(7, 8, 9))
        val d: Result<List<Int>> = Result.Success(listOf(10, 11, 12))
        val e: Result<List<Int>> = Result.Success(listOf(13, 14, 15))

        val result = zip5(a, b, c, d, e) { l1, l2, l3, l4, l5 ->
            l1 + l2 + l3 + l4 + l5
        }

        assertIs<Result.Success<List<Int>>>(result)
        assertEquals(15, result.data.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15), result.data)
    }

    @Test
    fun `encadenamiento de zip functions`() {
        val a = Result.Success(1)
        val b = Result.Success(2)
        val c = Result.Success(3)

        val sumResult = zip3(a, b, c) { n1, n2, n3 -> n1 + n2 + n3 }

        val d = Result.Success(4)
        val e = Result.Success(5)

        val finalResult = zip3(sumResult, d, e) { sum, n4, n5 ->
            sum + n4 + n5
        }

        assertIs<Result.Success<Int>>(finalResult)
        assertEquals(15, finalResult.data)
    }
}
