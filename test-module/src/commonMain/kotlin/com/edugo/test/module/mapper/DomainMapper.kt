package com.edugo.test.module.mapper

import com.edugo.test.module.core.Result

/**
 * Interface genérica para mapeo bidireccional entre DTOs y modelos de dominio.
 *
 * Esta interface define un contrato para conversiones type-safe entre objetos
 * de transferencia de datos (DTOs) y modelos de dominio, con soporte para
 * validación automática durante la conversión.
 *
 * ## Características
 *
 * - **Type-Safe**: Conversiones con tipos genéricos verificados en compilación
 * - **Bidireccional**: Soporta DTO → Domain y Domain → DTO
 * - **Validación Integrada**: toDomain() retorna Result<Domain> para manejar errores
 * - **Sin Validación en Reversa**: toDto() asume datos válidos del dominio
 * - **Composable**: Se puede combinar con otros mappers
 *
 * ## Uso Básico
 *
 * ```kotlin
 * data class UserDto(
 *     val email: String,
 *     val age: Int,
 *     val username: String
 * )
 *
 * data class User(
 *     val email: String,
 *     val age: Int,
 *     val username: String
 * ) : ValidatableModel {
 *     override fun validate(): Result<Unit> {
 *         return when {
 *             email.isBlank() -> failure("Email is required")
 *             !email.contains("@") -> failure("Invalid email format")
 *             age < 18 -> failure("User must be at least 18 years old")
 *             else -> success(Unit)
 *         }
 *     }
 * }
 *
 * object UserMapper : DomainMapper<UserDto, User> {
 *     override fun toDomain(dto: UserDto): Result<User> {
 *         val user = User(
 *             email = dto.email,
 *             age = dto.age,
 *             username = dto.username
 *         )
 *         return user.validate().map { user }
 *     }
 *
 *     override fun toDto(domain: User): UserDto {
 *         return UserDto(
 *             email = domain.email,
 *             age = domain.age,
 *             username = domain.username
 *         )
 *     }
 * }
 * ```
 *
 * ## Uso con Validación Acumulativa
 *
 * ```kotlin
 * object UserMapper : DomainMapper<UserDto, User> {
 *     override fun toDomain(dto: UserDto): Result<User> {
 *         val errors = mutableListOf<String>()
 *
 *         if (dto.email.isBlank()) {
 *             errors.add("Email is required")
 *         } else if (!dto.email.contains("@")) {
 *             errors.add("Invalid email format")
 *         }
 *
 *         if (dto.age < 18) {
 *             errors.add("User must be at least 18 years old")
 *         }
 *
 *         if (dto.username.length < 3) {
 *             errors.add("Username must be at least 3 characters")
 *         }
 *
 *         return if (errors.isEmpty()) {
 *             success(User(dto.email, dto.age, dto.username))
 *         } else {
 *             failure(errors.joinToString("; "))
 *         }
 *     }
 *
 *     override fun toDto(domain: User): UserDto {
 *         return UserDto(domain.email, domain.age, domain.username)
 *     }
 * }
 * ```
 *
 * ## Uso con Extension Functions
 *
 * ```kotlin
 * val dto = UserDto(email = "test@example.com", age = 25, username = "john")
 * val userResult = dto.toDomain(UserMapper)
 *
 * when (userResult) {
 *     is Result.Success -> println("User created: ${userResult.data}")
 *     is Result.Failure -> println("Validation failed: ${userResult.error}")
 * }
 * ```
 *
 * ## Mappers Anidados
 *
 * ```kotlin
 * data class AddressDto(val street: String, val city: String)
 * data class Address(val street: String, val city: String)
 *
 * object AddressMapper : DomainMapper<AddressDto, Address> {
 *     override fun toDomain(dto: AddressDto): Result<Address> {
 *         return when {
 *             dto.street.isBlank() -> failure("Street is required")
 *             dto.city.isBlank() -> failure("City is required")
 *             else -> success(Address(dto.street, dto.city))
 *         }
 *     }
 *
 *     override fun toDto(domain: Address): AddressDto {
 *         return AddressDto(domain.street, domain.city)
 *     }
 * }
 *
 * data class UserWithAddressDto(
 *     val name: String,
 *     val address: AddressDto
 * )
 *
 * data class UserWithAddress(
 *     val name: String,
 *     val address: Address
 * )
 *
 * object UserWithAddressMapper : DomainMapper<UserWithAddressDto, UserWithAddress> {
 *     override fun toDomain(dto: UserWithAddressDto): Result<UserWithAddress> {
 *         return AddressMapper.toDomain(dto.address).map { address ->
 *             UserWithAddress(dto.name, address)
 *         }
 *     }
 *
 *     override fun toDto(domain: UserWithAddress): UserWithAddressDto {
 *         return UserWithAddressDto(
 *             name = domain.name,
 *             address = AddressMapper.toDto(domain.address)
 *         )
 *     }
 * }
 * ```
 *
 * @param DTO Tipo del objeto de transferencia de datos (Data Transfer Object)
 * @param Domain Tipo del modelo de dominio
 *
 * @see Result Para entender el tipo de retorno de toDomain()
 * @see ValidatableModel Para modelos que requieren validación
 */
interface DomainMapper<DTO, Domain> {
    /**
     * Convierte un DTO a modelo de dominio con validación.
     *
     * Este método debe realizar la conversión y validar que los datos del DTO
     * sean válidos según las reglas de negocio del dominio. Si la validación
     * falla, retorna [Result.Failure] con mensaje descriptivo. Si tiene éxito,
     * retorna [Result.Success] con la instancia del dominio.
     *
     * ## Responsabilidades
     *
     * - Mapear campos del DTO a propiedades del dominio
     * - Validar todas las reglas de negocio
     * - Retornar errores descriptivos en caso de falla
     * - Garantizar que el dominio retornado es válido
     *
     * ## Patrones de Validación
     *
     * **Fail-Fast (recomendado para validaciones simples):**
     * ```kotlin
     * override fun toDomain(dto: UserDto): Result<User> {
     *     return when {
     *         dto.email.isBlank() -> failure("Email is required")
     *         dto.age < 0 -> failure("Age cannot be negative")
     *         else -> success(User(dto.email, dto.age))
     *     }
     * }
     * ```
     *
     * **Validación Acumulativa (recomendado para UX):**
     * ```kotlin
     * override fun toDomain(dto: UserDto): Result<User> {
     *     val errors = buildList {
     *         if (dto.email.isBlank()) add("Email is required")
     *         if (dto.age < 0) add("Age cannot be negative")
     *     }
     *     return if (errors.isEmpty()) {
     *         success(User(dto.email, dto.age))
     *     } else {
     *         failure(errors.joinToString("; "))
     *     }
     * }
     * ```
     *
     * @param dto Instancia del DTO a convertir
     * @return [Result.Success] con el dominio si la validación pasa,
     *         [Result.Failure] con mensaje de error si falla
     */
    fun toDomain(dto: DTO): Result<Domain>

    /**
     * Convierte un modelo de dominio a DTO.
     *
     * Este método asume que el modelo de dominio ya es válido (fue validado
     * al crearse o al convertirse desde un DTO). Por lo tanto, no necesita
     * validación y retorna directamente el DTO.
     *
     * ## Responsabilidades
     *
     * - Mapear propiedades del dominio a campos del DTO
     * - No realizar validaciones (el dominio ya es válido)
     * - Serializar objetos complejos si es necesario
     *
     * ## Uso
     *
     * ```kotlin
     * val user = User(email = "test@example.com", age = 25)
     * val dto = UserMapper.toDto(user)
     * // dto puede ser serializado y enviado por red
     * ```
     *
     * @param domain Instancia del modelo de dominio a convertir
     * @return DTO con los datos del dominio
     */
    fun toDto(domain: Domain): DTO
}

/**
 * Extension function para convertir un DTO a dominio usando un mapper.
 *
 * Proporciona una sintaxis más fluida para conversiones:
 * ```kotlin
 * val userDto = UserDto(...)
 * val userResult = userDto.toDomain(UserMapper)
 * ```
 *
 * En lugar de:
 * ```kotlin
 * val userResult = UserMapper.toDomain(userDto)
 * ```
 *
 * @param mapper Mapper a usar para la conversión
 * @return Result con el modelo de dominio o error de validación
 */
fun <DTO, Domain> DTO.toDomain(mapper: DomainMapper<DTO, Domain>): Result<Domain> {
    return mapper.toDomain(this)
}

/**
 * Extension function para convertir un modelo de dominio a DTO usando un mapper.
 *
 * Proporciona una sintaxis más fluida para conversiones:
 * ```kotlin
 * val user = User(...)
 * val dto = user.toDto(UserMapper)
 * ```
 *
 * En lugar de:
 * ```kotlin
 * val dto = UserMapper.toDto(user)
 * ```
 *
 * @param mapper Mapper a usar para la conversión
 * @return DTO con los datos del dominio
 */
fun <DTO, Domain> Domain.toDto(mapper: DomainMapper<DTO, Domain>): DTO {
    return mapper.toDto(this)
}

/**
 * Extension function para convertir una lista de DTOs a dominios.
 *
 * Convierte todos los DTOs usando el mapper proporcionado. Si alguna
 * conversión falla, retorna el primer error encontrado (fail-fast).
 *
 * ```kotlin
 * val dtos = listOf(UserDto(...), UserDto(...), UserDto(...))
 * val usersResult = dtos.toDomainList(UserMapper)
 *
 * when (usersResult) {
 *     is Result.Success -> println("${usersResult.data.size} users created")
 *     is Result.Failure -> println("Conversion failed: ${usersResult.error}")
 * }
 * ```
 *
 * @param mapper Mapper a usar para cada conversión
 * @return Result con la lista de dominios o el primer error encontrado
 */
fun <DTO, Domain> List<DTO>.toDomainList(mapper: DomainMapper<DTO, Domain>): Result<List<Domain>> {
    val domains = mutableListOf<Domain>()
    for (dto in this) {
        when (val result = mapper.toDomain(dto)) {
            is Result.Success -> domains.add(result.data)
            is Result.Failure -> return result
            is Result.Loading -> return Result.Loading
        }
    }
    return Result.Success(domains)
}

/**
 * Extension function para convertir una lista de dominios a DTOs.
 *
 * Convierte todos los modelos de dominio a DTOs usando el mapper.
 *
 * ```kotlin
 * val users = listOf(User(...), User(...), User(...))
 * val dtos = users.toDtoList(UserMapper)
 * ```
 *
 * @param mapper Mapper a usar para cada conversión
 * @return Lista de DTOs
 */
fun <DTO, Domain> List<Domain>.toDtoList(mapper: DomainMapper<DTO, Domain>): List<DTO> {
    return map { mapper.toDto(it) }
}
