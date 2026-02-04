package com.edugo.test.module.extensions

import com.edugo.test.module.core.Result

/**
 * Extensiones de combinación para múltiples Result<T> con type-safety completo.
 *
 * Estas funciones permiten combinar 3, 4 o 5 Results con diferentes tipos de forma
 * segura y eficiente. Extienden la funcionalidad de zip2 existente en Result.kt.
 *
 * ## Características
 *
 * - **Type-Safe**: Cada Result puede tener un tipo diferente
 * - **Fail-Fast**: Se detiene en el primer error encontrado
 * - **Loading-Aware**: Maneja correctamente el estado Loading
 * - **Inline Functions**: Sin overhead de performance
 *
 * ## Uso Básico
 *
 * ```kotlin
 * val name: Result<String> = validateName("John")
 * val age: Result<Int> = validateAge(25)
 * val email: Result<String> = validateEmail("john@example.com")
 *
 * val user: Result<User> = zip3(name, age, email) { n, a, e ->
 *     User(name = n, age = a, email = e)
 * }
 * ```
 *
 * ## Orden de Prioridad
 *
 * 1. **Failure**: Si algún Result es Failure, retorna el primero
 * 2. **Loading**: Si alguno es Loading (y ninguno es Failure), retorna Loading
 * 3. **Success**: Solo si todos son Success, aplica la transformación
 */

/**
 * Combina tres Results en uno usando una función de transformación.
 *
 * ## Ejemplo de Validación de Formulario
 *
 * ```kotlin
 * data class LoginForm(val username: String, val password: String, val remember: Boolean)
 *
 * val usernameResult = validateUsername(form.username)
 * val passwordResult = validatePassword(form.password)
 * val rememberResult = validateRemember(form.remember)
 *
 * val loginResult = zip3(usernameResult, passwordResult, rememberResult) { user, pass, rem ->
 *     LoginForm(username = user, password = pass, remember = rem)
 * }
 * ```
 *
 * ## Ejemplo con API Calls
 *
 * ```kotlin
 * val userResult = fetchUser(userId)
 * val postsResult = fetchUserPosts(userId)
 * val followersResult = fetchFollowers(userId)
 *
 * val profileResult = zip3(userResult, postsResult, followersResult) { user, posts, followers ->
 *     UserProfile(user = user, posts = posts, followers = followers)
 * }
 * ```
 *
 * @param a Primer Result
 * @param b Segundo Result
 * @param c Tercer Result
 * @param transform Función que combina los tres valores si todos son Success
 * @return Result con el valor transformado, o el primer error/Loading encontrado
 */
public inline fun <A, B, C, R> zip3(
    a: Result<A>,
    b: Result<B>,
    c: Result<C>,
    transform: (A, B, C) -> R
): Result<R> {
    // Fail-fast: Chequear errores primero en orden
    if (a is Result.Failure) return a
    if (b is Result.Failure) return b
    if (c is Result.Failure) return c

    // Loading: Segundo nivel de prioridad
    if (a is Result.Loading) return Result.Loading
    if (b is Result.Loading) return Result.Loading
    if (c is Result.Loading) return Result.Loading

    // Success: Todos deben ser Success
    return if (a is Result.Success && b is Result.Success && c is Result.Success) {
        Result.Success(transform(a.data, b.data, c.data))
    } else {
        Result.Failure("Unexpected state in zip3")
    }
}

/**
 * Combina cuatro Results en uno usando una función de transformación.
 *
 * ## Ejemplo de Validación de Registro
 *
 * ```kotlin
 * data class RegisterForm(
 *     val username: String,
 *     val email: String,
 *     val password: String,
 *     val confirmPassword: String
 * )
 *
 * val usernameResult = validateUsername(form.username)
 * val emailResult = validateEmail(form.email)
 * val passwordResult = validatePassword(form.password)
 * val confirmResult = validatePasswordMatch(form.password, form.confirmPassword)
 *
 * val registerResult = zip4(
 *     usernameResult,
 *     emailResult,
 *     passwordResult,
 *     confirmResult
 * ) { user, email, pass, _ ->
 *     RegisterForm(username = user, email = email, password = pass, confirmPassword = pass)
 * }
 * ```
 *
 * ## Ejemplo con Múltiples Validaciones
 *
 * ```kotlin
 * val product = zip4(
 *     validateName(dto.name),
 *     validatePrice(dto.price),
 *     validateStock(dto.stock),
 *     validateCategory(dto.category)
 * ) { name, price, stock, category ->
 *     Product(name, price, stock, category)
 * }
 * ```
 *
 * @param a Primer Result
 * @param b Segundo Result
 * @param c Tercer Result
 * @param d Cuarto Result
 * @param transform Función que combina los cuatro valores si todos son Success
 * @return Result con el valor transformado, o el primer error/Loading encontrado
 */
public inline fun <A, B, C, D, R> zip4(
    a: Result<A>,
    b: Result<B>,
    c: Result<C>,
    d: Result<D>,
    transform: (A, B, C, D) -> R
): Result<R> {
    // Fail-fast: Chequear errores primero en orden
    if (a is Result.Failure) return a
    if (b is Result.Failure) return b
    if (c is Result.Failure) return c
    if (d is Result.Failure) return d

    // Loading: Segundo nivel de prioridad
    if (a is Result.Loading) return Result.Loading
    if (b is Result.Loading) return Result.Loading
    if (c is Result.Loading) return Result.Loading
    if (d is Result.Loading) return Result.Loading

    // Success: Todos deben ser Success
    return if (a is Result.Success && b is Result.Success && c is Result.Success && d is Result.Success) {
        Result.Success(transform(a.data, b.data, c.data, d.data))
    } else {
        Result.Failure("Unexpected state in zip4")
    }
}

/**
 * Combina cinco Results en uno usando una función de transformación.
 *
 * ## Ejemplo de Validación de Dirección Completa
 *
 * ```kotlin
 * data class Address(
 *     val street: String,
 *     val city: String,
 *     val state: String,
 *     val zipCode: String,
 *     val country: String
 * )
 *
 * val addressResult = zip5(
 *     validateStreet(dto.street),
 *     validateCity(dto.city),
 *     validateState(dto.state),
 *     validateZipCode(dto.zipCode),
 *     validateCountry(dto.country)
 * ) { street, city, state, zip, country ->
 *     Address(street, city, state, zip, country)
 * }
 * ```
 *
 * ## Ejemplo de Configuración Compleja
 *
 * ```kotlin
 * val config = zip5(
 *     loadDatabaseConfig(),
 *     loadCacheConfig(),
 *     loadApiConfig(),
 *     loadLoggingConfig(),
 *     loadSecurityConfig()
 * ) { db, cache, api, logging, security ->
 *     AppConfig(db, cache, api, logging, security)
 * }
 * ```
 *
 * @param a Primer Result
 * @param b Segundo Result
 * @param c Tercer Result
 * @param d Cuarto Result
 * @param e Quinto Result
 * @param transform Función que combina los cinco valores si todos son Success
 * @return Result con el valor transformado, o el primer error/Loading encontrado
 */
public inline fun <A, B, C, D, E, R> zip5(
    a: Result<A>,
    b: Result<B>,
    c: Result<C>,
    d: Result<D>,
    e: Result<E>,
    transform: (A, B, C, D, E) -> R
): Result<R> {
    // Fail-fast: Chequear errores primero en orden
    if (a is Result.Failure) return a
    if (b is Result.Failure) return b
    if (c is Result.Failure) return c
    if (d is Result.Failure) return d
    if (e is Result.Failure) return e

    // Loading: Segundo nivel de prioridad
    if (a is Result.Loading) return Result.Loading
    if (b is Result.Loading) return Result.Loading
    if (c is Result.Loading) return Result.Loading
    if (d is Result.Loading) return Result.Loading
    if (e is Result.Loading) return Result.Loading

    // Success: Todos deben ser Success
    return if (
        a is Result.Success &&
        b is Result.Success &&
        c is Result.Success &&
        d is Result.Success &&
        e is Result.Success
    ) {
        Result.Success(transform(a.data, b.data, c.data, d.data, e.data))
    } else {
        Result.Failure("Unexpected state in zip5")
    }
}

/**
 * Extension function para combinar el Result actual con otros dos.
 *
 * Proporciona sintaxis fluida tipo builder:
 * ```kotlin
 * validateName("John")
 *     .zip3(validateAge(25), validateEmail("john@example.com")) { name, age, email ->
 *         User(name, age, email)
 *     }
 * ```
 *
 * @receiver El primer Result (this)
 * @param b Segundo Result
 * @param c Tercer Result
 * @param transform Función que combina los tres valores
 * @return Result con el valor transformado
 */
@kotlin.jvm.JvmName("zip3Ext")
public inline fun <A, B, C, R> Result<A>.zip3(
    b: Result<B>,
    c: Result<C>,
    transform: (A, B, C) -> R
): Result<R> = zip3(this, b, c, transform)

/**
 * Extension function para combinar el Result actual con otros tres.
 *
 * @receiver El primer Result (this)
 * @param b Segundo Result
 * @param c Tercer Result
 * @param d Cuarto Result
 * @param transform Función que combina los cuatro valores
 * @return Result con el valor transformado
 */
@kotlin.jvm.JvmName("zip4Ext")
public inline fun <A, B, C, D, R> Result<A>.zip4(
    b: Result<B>,
    c: Result<C>,
    d: Result<D>,
    transform: (A, B, C, D) -> R
): Result<R> = zip4(this, b, c, d, transform)

/**
 * Extension function para combinar el Result actual con otros cuatro.
 *
 * @receiver El primer Result (this)
 * @param b Segundo Result
 * @param c Tercer Result
 * @param d Cuarto Result
 * @param e Quinto Result
 * @param transform Función que combina los cinco valores
 * @return Result con el valor transformado
 */
@kotlin.jvm.JvmName("zip5Ext")
public inline fun <A, B, C, D, E, R> Result<A>.zip5(
    b: Result<B>,
    c: Result<C>,
    d: Result<D>,
    e: Result<E>,
    transform: (A, B, C, D, E) -> R
): Result<R> = zip5(this, b, c, d, e, transform)
