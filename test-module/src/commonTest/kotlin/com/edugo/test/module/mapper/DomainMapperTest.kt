package com.edugo.test.module.mapper

import com.edugo.test.module.core.ErrorCode
import com.edugo.test.module.core.Result
import com.edugo.test.module.core.success
import com.edugo.test.module.examples.User
import com.edugo.test.module.examples.UserDto
import com.edugo.test.module.examples.UserMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Suite de tests para DomainMapper y sus extensiones.
 *
 * Verifica:
 * - Conversión DTO → Domain con validación
 * - Conversión Domain → DTO sin validación
 * - Extension functions (toDomain, toDto, toDomainList, toDtoList)
 * - Integración con AppError
 * - Conversiones batch (listas)
 * - MapperExtensions avanzadas
 * - Ejemplo UserMapper con validación acumulativa
 */
class DomainMapperTest {

    // ========== Tests de UserMapper - Conversiones Básicas ==========

    @Test
    fun `toDomain convierte DTO válido a User exitosamente`() {
        val dto = UserDto(
            email = "test@example.com",
            age = 25,
            username = "testuser"
        )

        val result = UserMapper.toDomain(dto)

        assertIs<Result.Success<User>>(result)
        assertEquals("test@example.com", result.data.email)
        assertEquals(25, result.data.age)
        assertEquals("testuser", result.data.username)
    }

    @Test
    fun `toDomain falla con email inválido`() {
        val dto = UserDto(
            email = "invalid-email",
            age = 25,
            username = "testuser"
        )

        val result = UserMapper.toDomain(dto)

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("Invalid email format"))
    }

    @Test
    fun `toDomain falla con edad menor al mínimo`() {
        val dto = UserDto(
            email = "test@example.com",
            age = 15,
            username = "testuser"
        )

        val result = UserMapper.toDomain(dto)

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("Age must be between 18 and 120"))
    }

    @Test
    fun `toDomain falla con edad mayor al máximo`() {
        val dto = UserDto(
            email = "test@example.com",
            age = 150,
            username = "testuser"
        )

        val result = UserMapper.toDomain(dto)

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("Age must be between 18 and 120"))
    }

    @Test
    fun `toDomain falla con username muy corto`() {
        val dto = UserDto(
            email = "test@example.com",
            age = 25,
            username = "ab"
        )

        val result = UserMapper.toDomain(dto)

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("Username must be between 3 and 30 characters"))
    }

    @Test
    fun `toDomain falla con username muy largo`() {
        val dto = UserDto(
            email = "test@example.com",
            age = 25,
            username = "a".repeat(31)
        )

        val result = UserMapper.toDomain(dto)

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("Username must be between 3 and 30 characters"))
    }

    @Test
    fun `toDomain con múltiples errores acumula todos`() {
        val dto = UserDto(
            email = "no-at-sign",
            age = 15,
            username = "ab"
        )

        val result = UserMapper.toDomain(dto)

        assertIs<Result.Failure>(result)
        // Debe contener los 3 errores
        assertTrue(result.error.contains("Invalid email format"))
        assertTrue(result.error.contains("Age must be between 18 and 120"))
        assertTrue(result.error.contains("Username must be between 3 and 30 characters"))
    }

    @Test
    fun `toDomain acepta edad en límite inferior`() {
        val dto = UserDto(
            email = "test@example.com",
            age = 18,
            username = "testuser"
        )

        val result = UserMapper.toDomain(dto)
        assertIs<Result.Success<User>>(result)
    }

    @Test
    fun `toDomain acepta edad en límite superior`() {
        val dto = UserDto(
            email = "test@example.com",
            age = 120,
            username = "testuser"
        )

        val result = UserMapper.toDomain(dto)
        assertIs<Result.Success<User>>(result)
    }

    @Test
    fun `toDomain acepta username en límite inferior`() {
        val dto = UserDto(
            email = "test@example.com",
            age = 25,
            username = "abc"
        )

        val result = UserMapper.toDomain(dto)
        assertIs<Result.Success<User>>(result)
    }

    @Test
    fun `toDomain acepta username en límite superior`() {
        val dto = UserDto(
            email = "test@example.com",
            age = 25,
            username = "a".repeat(30)
        )

        val result = UserMapper.toDomain(dto)
        assertIs<Result.Success<User>>(result)
    }

    @Test
    fun `toDto convierte User a DTO sin validación`() {
        val user = User(
            email = "test@example.com",
            age = 25,
            username = "testuser"
        )

        val dto = UserMapper.toDto(user)

        assertEquals("test@example.com", dto.email)
        assertEquals(25, dto.age)
        assertEquals("testuser", dto.username)
    }

    // ========== Tests de Extension Functions ==========

    @Test
    fun `extension toDomain convierte DTO a dominio`() {
        val dto = UserDto(
            email = "test@example.com",
            age = 25,
            username = "testuser"
        )

        val result = dto.toDomain(UserMapper)

        assertIs<Result.Success<User>>(result)
        assertEquals("testuser", result.data.username)
    }

    @Test
    fun `extension toDto convierte dominio a DTO`() {
        val user = User(
            email = "test@example.com",
            age = 25,
            username = "testuser"
        )

        val dto = user.toDto(UserMapper)

        assertEquals("testuser", dto.username)
    }

    @Test
    fun `toDomainList convierte lista de DTOs exitosamente`() {
        val dtos = listOf(
            UserDto("test1@example.com", 25, "user1"),
            UserDto("test2@example.com", 30, "user2"),
            UserDto("test3@example.com", 35, "user3")
        )

        val result = dtos.toDomainList(UserMapper)

        assertIs<Result.Success<List<User>>>(result)
        assertEquals(3, result.data.size)
        assertEquals("user1", result.data[0].username)
        assertEquals("user2", result.data[1].username)
        assertEquals("user3", result.data[2].username)
    }

    @Test
    fun `toDomainList falla con el primer error`() {
        val dtos = listOf(
            UserDto("test1@example.com", 25, "user1"),
            UserDto("invalid", 30, "user2"),  // Email inválido
            UserDto("test3@example.com", 35, "user3")
        )

        val result = dtos.toDomainList(UserMapper)

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("Invalid email format"))
    }

    @Test
    fun `toDomainList con lista vacía retorna Success con lista vacía`() {
        val dtos = emptyList<UserDto>()
        val result = dtos.toDomainList(UserMapper)

        assertIs<Result.Success<List<User>>>(result)
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun `toDtoList convierte lista de dominios a DTOs`() {
        val users = listOf(
            User("test1@example.com", 25, "user1"),
            User("test2@example.com", 30, "user2"),
            User("test3@example.com", 35, "user3")
        )

        val dtos = users.toDtoList(UserMapper)

        assertEquals(3, dtos.size)
        assertEquals("user1", dtos[0].username)
        assertEquals("user2", dtos[1].username)
        assertEquals("user3", dtos[2].username)
    }

    // ========== Tests de MapperExtensions con AppError ==========

    @Test
    fun `toDomainWithAppError convierte exitosamente`() {
        val dto = UserDto("test@example.com", 25, "testuser")
        val result = dto.toDomainWithAppError(UserMapper)

        assertIs<Result.Success<User>>(result)
    }

    @Test
    fun `toDomainWithAppError falla con AppError estructurado`() {
        val dto = UserDto("invalid", 25, "testuser")
        val result = dto.toDomainWithAppError(
            mapper = UserMapper,
            errorCode = ErrorCode.VALIDATION_INVALID_EMAIL,
            details = mapOf("field" to "email", "value" to "invalid")
        )

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("VALIDATION_INVALID_EMAIL"))
    }

    @Test
    fun `toDomainListWithAppError convierte lista exitosamente`() {
        val dtos = listOf(
            UserDto("test1@example.com", 25, "user1"),
            UserDto("test2@example.com", 30, "user2")
        )

        val result = dtos.toDomainListWithAppError(UserMapper)

        assertIs<Result.Success<List<User>>>(result)
        assertEquals(2, result.data.size)
    }

    @Test
    fun `toDomainListWithAppError falla con índice del item`() {
        val dtos = listOf(
            UserDto("test1@example.com", 25, "user1"),
            UserDto("invalid", 30, "user2"),
            UserDto("test3@example.com", 35, "user3")
        )

        val result = dtos.toDomainListWithAppError(UserMapper, includeIndex = true)

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("VALIDATION_INVALID_INPUT"))
    }

    // ========== Tests de MapperExtensions Avanzadas ==========

    @Test
    fun `mapDomain transforma el resultado exitosamente`() {
        val dto = UserDto("test@example.com", 25, "testuser")
        val result = dto.toDomain(UserMapper)
            .mapDomain { user -> user.username.uppercase() }

        assertIs<Result.Success<String>>(result)
        assertEquals("TESTUSER", result.data)
    }

    @Test
    fun `mapDomain propaga el error`() {
        val dto = UserDto("invalid", 25, "testuser")
        val result = dto.toDomain(UserMapper)
            .mapDomain { user -> user.username.uppercase() }

        assertIs<Result.Failure>(result)
    }

    @Test
    fun `flatMapDomain encadena operaciones exitosamente`() {
        val dto = UserDto("test@example.com", 25, "testuser")
        val result = dto.toDomain(UserMapper)
            .flatMapDomain { user ->
                if (user.age >= 18) {
                    success("Adult: ${user.username}")
                } else {
                    Result.Failure("Not an adult")
                }
            }

        assertIs<Result.Success<String>>(result)
        assertEquals("Adult: testuser", result.data)
    }

    @Test
    fun `flatMapDomain propaga error de validación`() {
        val dto = UserDto("invalid", 25, "testuser")
        val result = dto.toDomain(UserMapper)
            .flatMapDomain { user ->
                success(user.username)
            }

        assertIs<Result.Failure>(result)
    }

    @Test
    fun `flatMapDomain puede fallar en la transformación`() {
        val dto = UserDto("test@example.com", 25, "testuser")
        val result = dto.toDomain(UserMapper)
            .flatMapDomain { user ->
                Result.Failure("Something went wrong in transformation")
            }

        assertIs<Result.Failure>(result)
        assertEquals("Something went wrong in transformation", result.error)
    }

    @Test
    fun `toDomainWithValidation aplica validación adicional`() {
        val dto = UserDto("test@example.com", 25, "testuser")
        val result = dto.toDomainWithValidation(UserMapper) { user ->
            if (user.username.startsWith("test")) {
                Result.Failure("Username cannot start with 'test'")
            } else {
                success(user)
            }
        }

        assertIs<Result.Failure>(result)
        assertEquals("Username cannot start with 'test'", result.error)
    }

    @Test
    fun `toDomainWithValidation pasa si validación adicional es exitosa`() {
        val dto = UserDto("test@example.com", 25, "validuser")
        val result = dto.toDomainWithValidation(UserMapper) { user ->
            if (user.username.startsWith("test")) {
                Result.Failure("Username cannot start with 'test'")
            } else {
                success(user)
            }
        }

        assertIs<Result.Success<User>>(result)
        assertEquals("validuser", result.data.username)
    }

    @Test
    fun `toDomainWithFallback usa mapper primario si tiene éxito`() {
        val dto = UserDto("test@example.com", 25, "testuser")
        val result = dto.toDomainWithFallback(
            primaryMapper = UserMapper,
            fallbackMapper = UserMapper
        )

        assertIs<Result.Success<User>>(result)
    }

    @Test
    fun `toDomainWithFallback usa fallback si primario falla`() {
        val lenientMapper = object : DomainMapper<UserDto, User> {
            override fun toDomain(dto: UserDto): Result<User> {
                // Mapper que siempre tiene éxito
                return success(User(
                    email = dto.email.ifBlank { "default@example.com" },
                    age = dto.age.coerceIn(18, 120),
                    username = dto.username.ifBlank { "default" }
                ))
            }
            override fun toDto(domain: User) = UserMapper.toDto(domain)
        }

        val dto = UserDto("invalid", 15, "ab")
        val result = dto.toDomainWithFallback(
            primaryMapper = UserMapper,
            fallbackMapper = lenientMapper
        )

        assertIs<Result.Success<User>>(result)
    }

    // ========== Tests de Conversión Parcial ==========

    @Test
    fun `toDomainPartial procesa todos los items`() {
        val dtos = listOf(
            UserDto("test1@example.com", 25, "user1"),
            UserDto("invalid", 30, "user2"),
            UserDto("test3@example.com", 35, "user3")
        )

        val (successes, failures) = dtos.toDomainPartial(UserMapper)

        assertEquals(2, successes.size)
        assertEquals(1, failures.size)
        assertEquals(1, failures[0].first) // Índice 1 falló
        assertTrue(failures[0].second.contains("Invalid email format"))
    }

    @Test
    fun `toDomainListIgnoreErrors filtra solo éxitos`() {
        val dtos = listOf(
            UserDto("test1@example.com", 25, "user1"),
            UserDto("invalid", 30, "user2"),
            UserDto("test3@example.com", 35, "user3")
        )

        val users = dtos.toDomainListIgnoreErrors(UserMapper)

        assertEquals(2, users.size)
        assertEquals("user1", users[0].username)
        assertEquals("user3", users[1].username)
    }

    @Test
    fun `toDomainListIgnoreErrors retorna lista vacía si todos fallan`() {
        val dtos = listOf(
            UserDto("invalid1", 15, "ab"),
            UserDto("invalid2", 200, "cd")
        )

        val users = dtos.toDomainListIgnoreErrors(UserMapper)

        assertTrue(users.isEmpty())
    }

    // ========== Tests de Métricas ==========

    @Test
    fun `toDomainListWithMetrics retorna métricas correctas para conversión exitosa`() {
        val dtos = listOf(
            UserDto("test1@example.com", 25, "user1"),
            UserDto("test2@example.com", 30, "user2"),
            UserDto("test3@example.com", 35, "user3")
        )

        val (result, metrics) = dtos.toDomainListWithMetrics(UserMapper)

        assertIs<Result.Success<List<User>>>(result)
        assertEquals(3, metrics.total)
        assertEquals(3, metrics.successCount)
        assertEquals(0, metrics.failureCount)
        assertEquals(100.0, metrics.successRate)
        assertTrue(metrics.isFullSuccess)
        assertTrue(metrics.hasAnySuccess)
    }

    @Test
    fun `toDomainListWithMetrics retorna métricas correctas con fallos`() {
        val dtos = listOf(
            UserDto("test1@example.com", 25, "user1"),
            UserDto("invalid", 30, "user2"),
            UserDto("test3@example.com", 35, "user3")
        )

        val (result, metrics) = dtos.toDomainListWithMetrics(UserMapper)

        assertIs<Result.Failure>(result)
        assertEquals(3, metrics.total)
        assertEquals(2, metrics.successCount)
        assertEquals(1, metrics.failureCount)
        assertEquals(66.66666666666667, metrics.successRate, 0.0001)
        assertTrue(!metrics.isFullSuccess)
        assertTrue(metrics.hasAnySuccess)
    }

    @Test
    fun `toDomainListWithMetrics retorna métricas para lista vacía`() {
        val dtos = emptyList<UserDto>()
        val (result, metrics) = dtos.toDomainListWithMetrics(UserMapper)

        assertIs<Result.Success<List<User>>>(result)
        assertEquals(0, metrics.total)
        assertEquals(0, metrics.successCount)
        assertEquals(0, metrics.failureCount)
        assertEquals(0.0, metrics.successRate)
        assertTrue(metrics.isFullSuccess)
        assertTrue(!metrics.hasAnySuccess)
    }

    // ========== Tests de Round-Trip ==========

    @Test
    fun `round-trip conversion mantiene datos`() {
        val originalDto = UserDto("test@example.com", 25, "testuser")

        val userResult = originalDto.toDomain(UserMapper)
        assertIs<Result.Success<User>>(userResult)

        val finalDto = userResult.data.toDto(UserMapper)

        assertEquals(originalDto, finalDto)
    }

    @Test
    fun `múltiples round-trips mantienen datos`() {
        val dto1 = UserDto("test@example.com", 25, "testuser")

        val user1 = (dto1.toDomain(UserMapper) as Result.Success).data
        val dto2 = user1.toDto(UserMapper)
        val user2 = (dto2.toDomain(UserMapper) as Result.Success).data
        val dto3 = user2.toDto(UserMapper)

        assertEquals(dto1, dto2)
        assertEquals(dto2, dto3)
    }

    // ========== Tests de Casos Edge ==========

    @Test
    fun `toDomain con email con múltiples @`() {
        val dto = UserDto("test@@example.com", 25, "testuser")
        val result = UserMapper.toDomain(dto)

        assertIs<Result.Failure>(result)
        assertTrue(result.error.contains("Invalid email format"))
    }

    @Test
    fun `toDomain con email sin dominio`() {
        val dto = UserDto("test@", 25, "testuser")
        val result = UserMapper.toDomain(dto)

        assertIs<Result.Failure>(result)
    }

    @Test
    fun `toDomain con email sin local part`() {
        val dto = UserDto("@example.com", 25, "testuser")
        val result = UserMapper.toDomain(dto)

        assertIs<Result.Failure>(result)
    }

    @Test
    fun `toDomain con username con espacios`() {
        val dto = UserDto("test@example.com", 25, "user name")
        val result = UserMapper.toDomain(dto)

        // Debería pasar - solo validamos longitud, no contenido
        assertIs<Result.Success<User>>(result)
    }

    @Test
    fun `toDomain con username con caracteres especiales`() {
        val dto = UserDto("test@example.com", 25, "user_123")
        val result = UserMapper.toDomain(dto)

        assertIs<Result.Success<User>>(result)
    }

    @Test
    fun `performance test - conversión de 1000 DTOs válidos`() {
        val dtos = (1..1000).map { i ->
            UserDto("test$i@example.com", 25, "user$i")
        }

        val (result, metrics) = dtos.toDomainListWithMetrics(UserMapper)

        assertIs<Result.Success<List<User>>>(result)
        assertEquals(1000, result.data.size)
        assertEquals(1000, metrics.successCount)
        assertTrue(metrics.durationMs < 1000) // Debería ser rápido
    }
}
