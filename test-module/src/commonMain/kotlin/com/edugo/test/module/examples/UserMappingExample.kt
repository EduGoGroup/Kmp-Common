package com.edugo.test.module.examples

import com.edugo.test.module.core.Result
import com.edugo.test.module.core.success
import com.edugo.test.module.data.models.base.ValidatableModel
import com.edugo.test.module.mapper.DomainMapper
import com.edugo.test.module.mapper.toDomain
import com.edugo.test.module.mapper.toDto
import com.edugo.test.module.validation.accumulateValidationErrors
import com.edugo.test.module.validation.validateEmail
import com.edugo.test.module.validation.validateLengthRange
import com.edugo.test.module.validation.validateRange
import kotlinx.serialization.Serializable

/**
 * Ejemplo completo de mapeo DTO/Domain con validación integrada.
 *
 * Este archivo demuestra cómo usar el sistema de mappers con validación
 * para convertir DTOs (datos de red/API) a modelos de dominio.
 *
 * ## Casos de Uso Demostrados
 *
 * - DTO simple con validación básica
 * - Modelo de dominio con ValidatableModel
 * - Mapper bidireccional con validación acumulativa
 * - Conversión DTO → Domain con Result<T>
 * - Conversión Domain → DTO sin validación
 *
 * ## Flujo de Datos
 *
 * ```
 * API/Network → UserDto → UserMapper.toDomain() → Result<User>
 *                                ↓ (validation)
 *                         email valid?
 *                         age in range?
 *                         username length OK?
 *                                ↓
 *                         Success(User) | Failure(errors)
 *
 * Domain Layer → User → UserMapper.toDto() → UserDto → API/Network
 *                       (no validation needed)
 * ```
 */

/**
 * DTO para transferir datos de usuario desde/hacia la API.
 *
 * Este DTO es un simple data class serializable que representa
 * los datos en tránsito. No contiene lógica de negocio ni validación.
 *
 * ## Características
 *
 * - Serializable con kotlinx.serialization
 * - Sin validación (la validación ocurre al convertir a Domain)
 * - Campos primitivos/simples para facilitar serialización
 * - Puede tener valores inválidos temporalmente
 *
 * ## Uso
 *
 * ```kotlin
 * // Desde JSON
 * val json = """{"email":"test@example.com","age":25,"username":"john"}"""
 * val dto = Json.decodeFromString<UserDto>(json)
 *
 * // A JSON
 * val json = Json.encodeToString(dto)
 * ```
 */
@Serializable
public data class UserDto(
    /**
     * Email del usuario.
     * Debe ser un email válido según las reglas de negocio.
     */
    val email: String,

    /**
     * Edad del usuario en años.
     * Debe estar entre 18 y 120 según las reglas de negocio.
     */
    val age: Int,

    /**
     * Nombre de usuario único.
     * Debe tener entre 3 y 30 caracteres según las reglas de negocio.
     */
    val username: String
)

/**
 * Modelo de dominio para usuario con validación integrada.
 *
 * Este modelo representa un usuario válido en el dominio de la aplicación.
 * Implementa ValidatableModel para garantizar que todos los datos son válidos.
 *
 * ## Reglas de Negocio
 *
 * - Email: Debe tener formato válido (contener @, tener local y domain parts)
 * - Age: Debe estar entre 18 y 120 años (usuarios mayores de edad)
 * - Username: Debe tener entre 3 y 30 caracteres
 *
 * ## Garantías
 *
 * - Una instancia de User siempre tiene datos válidos
 * - La validación se ejecuta al crear desde DTO
 * - El dominio puede asumir datos válidos sin re-validar
 *
 * ## Uso
 *
 * ```kotlin
 * val user = User(
 *     email = "test@example.com",
 *     age = 25,
 *     username = "john_doe"
 * )
 *
 * // Validar explícitamente
 * when (val result = user.validate()) {
 *     is Result.Success -> println("User is valid")
 *     is Result.Failure -> println("Validation error: ${result.error}")
 * }
 * ```
 */
public data class User(
    val email: String,
    val age: Int,
    val username: String
) : ValidatableModel {

    /**
     * Valida las reglas de negocio del usuario.
     *
     * Implementa validación fail-fast que retorna el primer error encontrado.
     * Para validación acumulativa, usar el mapper que implementa este patrón.
     *
     * @return Success si todos los campos son válidos, Failure con el primer error
     */
    override fun validate(): Result<Unit> {
        // Validar email
        validateEmail(email)?.let { return Result.Failure(it) }

        // Validar edad
        validateRange(age, 18, 120, "Age")?.let { return Result.Failure(it) }

        // Validar username
        validateLengthRange(username, 3, 30, "Username")?.let { return Result.Failure(it) }

        return success(Unit)
    }

    companion object {
        /**
         * Edad mínima permitida para usuarios.
         */
        public const val MIN_AGE = 18

        /**
         * Edad máxima permitida para usuarios.
         */
        public const val MAX_AGE = 120

        /**
         * Longitud mínima del username.
         */
        public const val USERNAME_MIN_LENGTH = 3

        /**
         * Longitud máxima del username.
         */
        public const val USERNAME_MAX_LENGTH = 30
    }
}

/**
 * Mapper bidireccional entre UserDto y User con validación acumulativa.
 *
 * Este mapper implementa DomainMapper para proporcionar conversión type-safe
 * entre el DTO y el modelo de dominio. La validación es acumulativa, es decir,
 * recolecta todos los errores antes de retornar.
 *
 * ## Características
 *
 * - **Validación Acumulativa**: Recolecta todos los errores de validación
 * - **Type-Safe**: Conversión verificada en compilación
 * - **Bidireccional**: Soporta DTO → Domain y Domain → DTO
 * - **Sin Copias Innecesarias**: Conversión directa sin overhead
 *
 * ## Uso
 *
 * ```kotlin
 * // DTO a Domain con validación
 * val dto = UserDto(email = "test@example.com", age = 25, username = "john")
 * val userResult = UserMapper.toDomain(dto)
 *
 * when (userResult) {
 *     is Result.Success -> {
 *         val user = userResult.data
 *         println("User created: ${user.username}")
 *     }
 *     is Result.Failure -> {
 *         println("Validation errors: ${userResult.error}")
 *         // Si múltiples errores: "Invalid email format; Age must be between 18 and 120"
 *     }
 * }
 *
 * // Domain a DTO sin validación
 * val user = User(...)
 * val dto = UserMapper.toDto(user)
 * ```
 *
 * ## Validación Acumulativa vs Fail-Fast
 *
 * Este mapper usa validación acumulativa para mejor UX:
 * - Muestra TODOS los errores al usuario de una vez
 * - El usuario puede corregir todos los problemas sin múltiples intentos
 * - Especialmente útil en formularios con múltiples campos
 *
 * Ejemplo con múltiples errores:
 * ```kotlin
 * val dto = UserDto(
 *     email = "invalid",      // Error: no contiene @
 *     age = 15,               // Error: menor que 18
 *     username = "ab"         // Error: menor que 3 caracteres
 * )
 * val result = UserMapper.toDomain(dto)
 * // Retorna: "Invalid email format; Age must be between 18 and 120; Username must be between 3 and 30 characters"
 * ```
 */
public object UserMapper : DomainMapper<UserDto, User> {

    /**
     * Convierte UserDto a User con validación acumulativa.
     *
     * Valida todos los campos y recolecta todos los errores antes de retornar.
     * Si hay al menos un error, retorna Failure con todos los errores concatenados.
     * Si no hay errores, crea y retorna la instancia de User.
     *
     * ## Proceso de Validación
     *
     * 1. Validar formato de email
     * 2. Validar rango de edad (18-120)
     * 3. Validar longitud de username (3-30)
     * 4. Si hay errores, concatenarlos con "; "
     * 5. Si no hay errores, crear User
     *
     * ## Ejemplos
     *
     * **Conversión exitosa:**
     * ```kotlin
     * val dto = UserDto(email = "test@example.com", age = 25, username = "john")
     * val result = toDomain(dto)
     * // Result.Success(User(email="test@example.com", age=25, username="john"))
     * ```
     *
     * **Con un error:**
     * ```kotlin
     * val dto = UserDto(email = "invalid", age = 25, username = "john")
     * val result = toDomain(dto)
     * // Result.Failure("Invalid email format")
     * ```
     *
     * **Con múltiples errores:**
     * ```kotlin
     * val dto = UserDto(email = "invalid", age = 15, username = "ab")
     * val result = toDomain(dto)
     * // Result.Failure("Invalid email format; Age must be between 18 and 120; Username must be between 3 and 30 characters")
     * ```
     *
     * @param dto DTO a convertir (puede contener datos inválidos)
     * @return Success con User si todos los campos son válidos,
     *         Failure con mensaje de error concatenado si hay errores
     */
    override fun toDomain(dto: UserDto): Result<User> {
        // Acumular todos los errores de validación
        val validationResult = accumulateValidationErrors {
            add(validateEmail(dto.email))
            add(validateRange(dto.age, User.MIN_AGE, User.MAX_AGE, "Age"))
            add(validateLengthRange(dto.username, User.USERNAME_MIN_LENGTH, User.USERNAME_MAX_LENGTH, "Username"))
        }

        // Si hay errores, retornar falla
        if (validationResult is Result.Failure) {
            return validationResult
        }

        // Si no hay errores, crear el usuario
        return success(User(
            email = dto.email,
            age = dto.age,
            username = dto.username
        ))
    }

    /**
     * Convierte User a UserDto sin validación.
     *
     * El modelo de dominio (User) ya ha sido validado al crearse,
     * por lo que esta conversión no necesita validación.
     *
     * ## Uso
     *
     * ```kotlin
     * val user = User(email = "test@example.com", age = 25, username = "john")
     * val dto = toDto(user)
     * // UserDto(email="test@example.com", age=25, username="john")
     * ```
     *
     * @param domain Usuario del dominio (garantizado válido)
     * @return DTO con los datos del usuario
     */
    override fun toDto(domain: User): UserDto {
        return UserDto(
            email = domain.email,
            age = domain.age,
            username = domain.username
        )
    }
}

/**
 * Ejemplo de uso completo del sistema de mappers.
 *
 * Demuestra los casos de uso principales:
 * - Conversión exitosa DTO → Domain
 * - Conversión fallida con un error
 * - Conversión fallida con múltiples errores
 * - Conversión Domain → DTO
 * - Uso con extension functions
 */
public object UserMappingUsageExample {

    /**
     * Ejemplo de conversión exitosa.
     */
    public fun successfulConversion() {
        val dto = UserDto(
            email = "john.doe@example.com",
            age = 25,
            username = "john_doe"
        )

        when (val result = UserMapper.toDomain(dto)) {
            is Result.Success -> {
                val user = result.data
                println("✓ User created successfully")
                println("  Email: ${user.email}")
                println("  Age: ${user.age}")
                println("  Username: ${user.username}")
            }
            is Result.Failure -> {
                println("✗ Validation failed: ${result.error}")
            }
            is Result.Loading -> {
                println("⟳ Loading...")
            }
        }
    }

    /**
     * Ejemplo de conversión con un error.
     */
    public fun singleErrorConversion() {
        val dto = UserDto(
            email = "invalid-email",  // ✗ No contiene @
            age = 25,                 // ✓ Válido
            username = "john_doe"     // ✓ Válido
        )

        when (val result = UserMapper.toDomain(dto)) {
            is Result.Success -> {
                println("✓ User created (unexpected)")
            }
            is Result.Failure -> {
                println("✗ Validation failed: ${result.error}")
                // Output: "Invalid email format"
            }
            is Result.Loading -> {
                println("⟳ Loading...")
            }
        }
    }

    /**
     * Ejemplo de conversión con múltiples errores.
     */
    public fun multipleErrorsConversion() {
        val dto = UserDto(
            email = "no-at-sign",     // ✗ No contiene @
            age = 15,                 // ✗ Menor que 18
            username = "ab"           // ✗ Menor que 3 caracteres
        )

        when (val result = UserMapper.toDomain(dto)) {
            is Result.Success -> {
                println("✓ User created (unexpected)")
            }
            is Result.Failure -> {
                println("✗ Validation failed with multiple errors:")
                println("  ${result.error}")
                // Output: "Invalid email format; Age must be between 18 and 120; Username must be between 3 and 30 characters"
            }
            is Result.Loading -> {
                println("⟳ Loading...")
            }
        }
    }

    /**
     * Ejemplo de conversión Domain → DTO.
     */
    public fun domainToDtoConversion() {
        val user = User(
            email = "jane.smith@example.com",
            age = 30,
            username = "jane_smith"
        )

        val dto = UserMapper.toDto(user)
        println("✓ DTO created from domain")
        println("  Email: ${dto.email}")
        println("  Age: ${dto.age}")
        println("  Username: ${dto.username}")
    }

    /**
     * Ejemplo usando extension functions.
     */
    public fun usingExtensionFunctions() {
        val dto = UserDto(
            email = "test@example.com",
            age = 25,
            username = "testuser"
        )

        // Usar extension function toDomain()
        when (val result = dto.toDomain(UserMapper)) {
            is Result.Success -> {
                val user = result.data

                // Usar extension function toDto()
                val dtoBack = user.toDto(UserMapper)
                println("✓ Round-trip conversion successful")
                println("  Original DTO: $dto")
                println("  DTO after round-trip: $dtoBack")
                println("  Are equal: ${dto == dtoBack}")
            }
            is Result.Failure -> {
                println("✗ Conversion failed: ${result.error}")
            }
            is Result.Loading -> {
                println("⟳ Loading...")
            }
        }
    }
}
