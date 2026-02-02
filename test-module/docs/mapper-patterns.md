# Patrones de Mappers y Conversión DTO/Domain

Esta guía documenta los patrones de conversión entre DTOs (Data Transfer Objects) y modelos de dominio usando el patrón `DomainMapper`.

## Tabla de Contenidos

1. [Conceptos](#conceptos)
2. [DomainMapper Interface](#domainmapper-interface)
3. [Implementación Básica](#implementación-básica)
4. [Validación en Mappers](#validación-en-mappers)
5. [Extensiones Avanzadas](#extensiones-avanzadas)
6. [Patrones Comunes](#patrones-comunes)
7. [Integración con AppError](#integración-con-apperror)
8. [Mejores Prácticas](#mejores-prácticas)

## Conceptos

### DTO vs Domain Model

**DTO (Data Transfer Object)**:
- Usado para transferencia de datos (API, BD, serialización)
- Estructura plana y simple
- Puede tener valores opcionales o por defecto
- Anotado con `@Serializable` para kotlinx.serialization
- No tiene lógica de negocio

**Domain Model**:
- Representa conceptos del dominio del negocio
- Contiene lógica de negocio y validaciones
- Garantiza invariantes (reglas que siempre se cumplen)
- Puede tener estructura más compleja
- No necesariamente serializable

### ¿Por qué separar DTO y Domain?

```kotlin
// DTO - Estructura simple para API
@Serializable
data class UserDto(
    val username: String,
    val email: String,
    val age: Int,
    val role: String = "user"  // Valor por defecto
)

// Domain - Modelo con invariantes
data class User(
    val username: Username,  // Value object que garantiza validez
    val email: Email,        // Value object que garantiza formato
    val age: Age,            // Value object que garantiza rango 18-120
    val role: UserRole       // Enum, no String
) {
    fun canAccessAdminPanel(): Boolean = role == UserRole.ADMIN
    
    fun promote(): User = copy(role = UserRole.ADMIN)
}
```

**Ventajas de la separación**:
1. **Validación centralizada**: La conversión DTO → Domain valida todos los datos
2. **Invariantes garantizados**: Los modelos de dominio siempre son válidos
3. **Flexibilidad**: Puedes cambiar la API sin afectar la lógica de negocio
4. **Testing**: Modelos de dominio son fáciles de testear sin serialización

## DomainMapper Interface

La interfaz `DomainMapper` define el contrato de conversión bidireccional:

```kotlin
interface DomainMapper<DTO, Domain> {
    /**
     * Convierte DTO a Domain con validación.
     * Retorna Result.Failure si la validación falla.
     */
    fun toDomain(dto: DTO): Result<Domain>
    
    /**
     * Convierte Domain a DTO sin validación.
     * Siempre tiene éxito porque Domain ya es válido.
     */
    fun toDto(domain: Domain): DTO
}
```

### Decisiones de diseño

**¿Por qué `toDomain` retorna `Result` pero `toDto` no?**

```kotlin
// toDomain: DTO puede ser inválido (viene del exterior)
fun toDomain(dto: UserDto): Result<User> {
    if (dto.age < 18) {
        return Result.Failure("Usuario debe ser mayor de edad")
    }
    // ...
}

// toDto: Domain ya es válido (viene del interior)
fun toDto(domain: User): UserDto {
    return UserDto(
        username = domain.username.value,
        email = domain.email.value,
        age = domain.age.value
    )
    // No necesita validación, domain siempre es válido
}
```

## Implementación Básica

### Mapper Simple

```kotlin
@Serializable
data class ProductDto(
    val id: Long,
    val name: String,
    val price: Double
)

data class Product(
    val id: ProductId,
    val name: ProductName,
    val price: Money
)

object ProductMapper : DomainMapper<ProductDto, Product> {
    override fun toDomain(dto: ProductDto): Result<Product> {
        // Validación simple
        if (dto.price < 0) {
            return Result.Failure("El precio no puede ser negativo")
        }
        if (dto.name.isBlank()) {
            return Result.Failure("El nombre del producto no puede estar vacío")
        }
        
        return Result.Success(
            Product(
                id = ProductId(dto.id),
                name = ProductName(dto.name),
                price = Money(dto.price)
            )
        )
    }
    
    override fun toDto(domain: Product): ProductDto {
        return ProductDto(
            id = domain.id.value,
            name = domain.name.value,
            price = domain.price.amount
        )
    }
}
```

### Uso del Mapper

```kotlin
// DTO → Domain
val dto = ProductDto(id = 1, name = "Laptop", price = 999.99)
val product: Result<Product> = ProductMapper.toDomain(dto)

when (product) {
    is Result.Success -> println("Producto válido: ${product.data.name}")
    is Result.Failure -> println("Producto inválido: ${product.error}")
    is Result.Loading -> println("Cargando...")
}

// Domain → DTO
val domain = Product(ProductId(1), ProductName("Laptop"), Money(999.99))
val dto: ProductDto = ProductMapper.toDto(domain)
```

### Extensiones para facilitar uso

```kotlin
// Extensión en DTO
val product: Result<Product> = dto.toDomain(ProductMapper)

// Extensión en Domain
val dto: ProductDto = product.toDto(ProductMapper)

// Listas
val products: Result<List<Product>> = dtos.toDomainList(ProductMapper)
val dtos: List<ProductDto> = products.toDtoList(ProductMapper)
```

## Validación en Mappers

### Validación Acumulativa (Recomendada)

Muestra TODOS los errores de validación de una vez:

```kotlin
object UserMapper : DomainMapper<UserDto, User> {
    override fun toDomain(dto: UserDto): Result<User> {
        val errors = accumulateValidationErrors(separator = "\n") {
            add(validateEmail(dto.email))
            add(validateRange(dto.age, 18, 120, "edad"))
            add(validateLengthRange(dto.username, 3, 30, "username"))
            add(validatePattern(
                dto.username,
                "^[a-zA-Z0-9_]+$",
                "username",
                "Solo puede contener letras, números y guiones bajos"
            ))
        }
        
        if (errors != null) {
            return Result.Failure(errors)
        }
        
        return Result.Success(
            User(
                username = Username(dto.username),
                email = Email(dto.email),
                age = Age(dto.age)
            )
        )
    }
    
    override fun toDto(domain: User): UserDto {
        return UserDto(
            username = domain.username.value,
            email = domain.email.value,
            age = domain.age.value
        )
    }
}
```

**Resultado con múltiples errores**:
```
Email inválido: no contiene '@'
El campo edad debe estar entre 18 y 120
El campo username debe tener entre 3 y 30 caracteres
El campo username solo puede contener letras, números y guiones bajos
```

### Validación Fail-Fast

Detiene en el primer error (útil para validaciones costosas):

```kotlin
object OrderMapper : DomainMapper<OrderDto, Order> {
    override fun toDomain(dto: OrderDto): Result<Order> {
        // Valida campos simples primero
        validateNotEmpty(dto.items, "items")?.let { return Result.Failure(it) }
        validatePositive(dto.totalAmount, "total")?.let { return Result.Failure(it) }
        
        // Validaciones costosas solo si las simples pasaron
        val items = dto.items.traverse { itemDto ->
            ProductMapper.toDomain(itemDto)  // Puede ser costoso (DB lookup)
        }
        
        return items.map { validItems ->
            Order(validItems, Money(dto.totalAmount))
        }
    }
    
    override fun toDto(domain: Order): OrderDto {
        return OrderDto(
            items = domain.items.map { ProductMapper.toDto(it) },
            totalAmount = domain.totalAmount.amount
        )
    }
}
```

### Validación Condicional

```kotlin
object EmployeeMapper : DomainMapper<EmployeeDto, Employee> {
    override fun toDomain(dto: EmployeeDto): Result<Employee> {
        val errors = accumulateValidationErrors(separator = "\n") {
            add(validateNotBlank(dto.name, "nombre"))
            add(validateEmail(dto.email))
            
            // Validar solo si es manager
            add(validateIf(dto.isManager) {
                validateNotBlank(dto.department, "departamento")
            })
            
            // Validar solo si tiene beneficios
            add(validateIf(dto.hasBenefits) {
                validatePositive(dto.benefitsAmount, "monto de beneficios")
            })
        }
        
        if (errors != null) {
            return Result.Failure(errors)
        }
        
        return Result.Success(Employee(/* ... */))
    }
    
    override fun toDto(domain: Employee): EmployeeDto = TODO()
}
```

## Extensiones Avanzadas

### Conversión con AppError

Convierte errores de validación en `AppError` estructurado:

```kotlin
val user: Result<User> = dto.toDomainWithAppError(
    mapper = UserMapper,
    errorCode = ErrorCode.VALIDATION_INVALID_INPUT,
    details = mapOf("source" to "registration_form")
)

// Si falla, retorna:
// Result.Failure(
//     AppError(
//         code = VALIDATION_INVALID_INPUT,
//         message = "Email inválido\nEdad fuera de rango",
//         details = {"source": "registration_form"}
//     )
// )
```

### Conversión con Fallback

Usa un valor por defecto si la conversión falla:

```kotlin
val config: Config = configDto.toDomainWithFallback(
    mapper = ConfigMapper,
    fallback = Config.default()
)
```

### Conversión Parcial (Batch Processing)

Procesa una lista y separa éxitos y fallos:

```kotlin
val (validUsers, invalidUsers) = userDtos.toDomainPartial(UserMapper)

database.insertAll(validUsers)
logger.warn("${invalidUsers.size} usuarios inválidos: ${invalidUsers.joinToString()}")
```

### Conversión Ignorando Errores

Procesa solo los que se puedan convertir:

```kotlin
val validProducts: List<Product> = productDtos.toDomainListIgnoreErrors(ProductMapper)
```

### Conversión con Métricas

Obtén estadísticas sobre la conversión:

```kotlin
val (products, metrics) = productDtos.toDomainListWithMetrics(ProductMapper)

println("Éxitos: ${metrics.successCount}")
println("Fallos: ${metrics.failureCount}")
println("Total: ${metrics.totalCount}")
println("Tasa de éxito: ${metrics.successRate}%")
println("Errores: ${metrics.errors}")
```

## Patrones Comunes

### Patrón: Mapper con Dependencias

Cuando el mapper necesita servicios externos:

```kotlin
class UserMapper(
    private val countryRepository: CountryRepository,
    private val roleRepository: RoleRepository
) : DomainMapper<UserDto, User> {
    override fun toDomain(dto: UserDto): Result<User> {
        // Buscar país por código
        val country = countryRepository.findByCode(dto.countryCode)
            ?: return Result.Failure("País no encontrado: ${dto.countryCode}")
        
        // Buscar rol por ID
        val role = roleRepository.findById(dto.roleId)
            ?: return Result.Failure("Rol no encontrado: ${dto.roleId}")
        
        return Result.Success(User(dto.name, country, role))
    }
    
    override fun toDto(domain: User): UserDto {
        return UserDto(
            name = domain.name,
            countryCode = domain.country.code,
            roleId = domain.role.id
        )
    }
}
```

### Patrón: Mapper Jerárquico

Reutilizar mappers en otros mappers:

```kotlin
object AddressMapper : DomainMapper<AddressDto, Address> {
    override fun toDomain(dto: AddressDto): Result<Address> = TODO()
    override fun toDto(domain: Address): AddressDto = TODO()
}

object CompanyMapper : DomainMapper<CompanyDto, Company> {
    override fun toDomain(dto: CompanyDto): Result<Company> {
        // Validar campos propios
        val errors = accumulateValidationErrors {
            add(validateNotBlank(dto.name, "nombre"))
            add(validatePattern(dto.taxId, "^[0-9]{9}$", "NIF", "Debe tener 9 dígitos"))
        }
        if (errors != null) return Result.Failure(errors)
        
        // Convertir dirección usando su mapper
        val address = AddressMapper.toDomain(dto.address)
        
        // Combinar resultados
        return address.map { addr ->
            Company(
                name = CompanyName(dto.name),
                taxId = TaxId(dto.taxId),
                address = addr
            )
        }
    }
    
    override fun toDto(domain: Company): CompanyDto {
        return CompanyDto(
            name = domain.name.value,
            taxId = domain.taxId.value,
            address = AddressMapper.toDto(domain.address)
        )
    }
}
```

### Patrón: Mapper con Enums

Conversión segura de strings a enums:

```kotlin
enum class OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}

@Serializable
data class OrderDto(
    val id: Long,
    val status: String  // "pending", "confirmed", etc.
)

object OrderMapper : DomainMapper<OrderDto, Order> {
    override fun toDomain(dto: OrderDto): Result<Order> {
        val status = try {
            OrderStatus.valueOf(dto.status.uppercase())
        } catch (e: IllegalArgumentException) {
            return Result.Failure(
                "Estado inválido: ${dto.status}. Valores válidos: ${OrderStatus.entries.joinToString()}"
            )
        }
        
        return Result.Success(Order(OrderId(dto.id), status))
    }
    
    override fun toDto(domain: Order): OrderDto {
        return OrderDto(
            id = domain.id.value,
            status = domain.status.name.lowercase()
        )
    }
}
```

### Patrón: Mapper con Valores Opcionales

```kotlin
@Serializable
data class ProfileDto(
    val userId: Long,
    val bio: String? = null,
    val website: String? = null,
    val phoneNumber: String? = null
)

data class Profile(
    val userId: UserId,
    val bio: Bio?,
    val website: Website?,
    val phoneNumber: PhoneNumber?
)

object ProfileMapper : DomainMapper<ProfileDto, Profile> {
    override fun toDomain(dto: ProfileDto): Result<Profile> {
        // Validar campos opcionales solo si están presentes
        val errors = accumulateValidationErrors(separator = "\n") {
            dto.website?.let { add(validateUrl(it)) }
            dto.phoneNumber?.let { add(validatePhoneNumber(it)) }
            dto.bio?.let { add(validateMaxLength(it, 500, "bio")) }
        }
        
        if (errors != null) return Result.Failure(errors)
        
        return Result.Success(
            Profile(
                userId = UserId(dto.userId),
                bio = dto.bio?.let { Bio(it) },
                website = dto.website?.let { Website(it) },
                phoneNumber = dto.phoneNumber?.let { PhoneNumber(it) }
            )
        )
    }
    
    override fun toDto(domain: Profile): ProfileDto {
        return ProfileDto(
            userId = domain.userId.value,
            bio = domain.bio?.value,
            website = domain.website?.value,
            phoneNumber = domain.phoneNumber?.value
        )
    }
}
```

### Patrón: Mapper con Listas Anidadas

```kotlin
@Serializable
data class CartDto(
    val userId: Long,
    val items: List<CartItemDto>
)

@Serializable
data class CartItemDto(
    val productId: Long,
    val quantity: Int,
    val price: Double
)

object CartItemMapper : DomainMapper<CartItemDto, CartItem> {
    override fun toDomain(dto: CartItemDto): Result<CartItem> {
        val errors = accumulateValidationErrors {
            add(validatePositive(dto.quantity, "cantidad"))
            add(validatePositive(dto.price, "precio"))
        }
        if (errors != null) return Result.Failure(errors)
        
        return Result.Success(
            CartItem(
                productId = ProductId(dto.productId),
                quantity = Quantity(dto.quantity),
                price = Money(dto.price)
            )
        )
    }
    
    override fun toDto(domain: CartItem): CartItemDto = TODO()
}

object CartMapper : DomainMapper<CartDto, Cart> {
    override fun toDomain(dto: CartDto): Result<Cart> {
        validateNotEmpty(dto.items, "items")?.let { return Result.Failure(it) }
        
        // Opción 1: Fail-fast (detiene al primer error)
        val items = dto.items.traverse { CartItemMapper.toDomain(it) }
        
        // Opción 2: Acumular errores de todos los items
        // val items = dto.items.traverseCollectingErrors(includeIndex = true) {
        //     CartItemMapper.toDomain(it)
        // }
        
        return items.map { validItems ->
            Cart(UserId(dto.userId), validItems)
        }
    }
    
    override fun toDto(domain: Cart): CartDto {
        return CartDto(
            userId = domain.userId.value,
            items = domain.items.toDtoList(CartItemMapper)
        )
    }
}
```

## Integración con AppError

### Mapper que retorna AppError

```kotlin
object UserMapper : DomainMapper<UserDto, User> {
    override fun toDomain(dto: UserDto): Result<User> {
        val errors = accumulateValidationErrors(separator = "\n") {
            add(validateEmail(dto.email))
            add(validateRange(dto.age, 18, 120, "edad"))
        }
        
        if (errors != null) {
            val appError = AppError(
                code = ErrorCode.VALIDATION_INVALID_INPUT,
                message = "Datos de usuario inválidos",
                details = mapOf(
                    "errors" to errors,
                    "dto" to dto.toString()
                )
            )
            return Result.Failure(appError.message)  // O serializa appError a JSON
        }
        
        return Result.Success(User(/* ... */))
    }
    
    override fun toDto(domain: User): UserDto = TODO()
}
```

### Extensión que envuelve en AppError

Ya implementada en `MapperExtensions.kt`:

```kotlin
val user: Result<User> = userDto.toDomainWithAppError(
    mapper = UserMapper,
    errorCode = ErrorCode.VALIDATION_INVALID_INPUT,
    details = mapOf("source" to "api_request")
)
```

## Mejores Prácticas

### 1. Usa validación acumulativa para formularios

```kotlin
// ✅ BUENO: Usuario ve todos los errores
val errors = accumulateValidationErrors(separator = "\n") {
    add(validateEmail(dto.email))
    add(validateRange(dto.age, 18, 120, "edad"))
    add(validateNotBlank(dto.name, "nombre"))
}

// ❌ MALO: Usuario solo ve el primer error
if (!isValidEmail(dto.email)) return Result.Failure("Email inválido")
if (dto.age < 18) return Result.Failure("Debe ser mayor de edad")
// Usuario nunca ve el error de nombre si email falla
```

### 2. Usa fail-fast para operaciones costosas

```kotlin
// ✅ BUENO: Valida barato primero, costoso después
override fun toDomain(dto: UserDto): Result<User> {
    // Validaciones baratas primero
    validateEmail(dto.email)?.let { return Result.Failure(it) }
    validateRange(dto.age, 18, 120, "edad")?.let { return Result.Failure(it) }
    
    // Validación costosa solo si las baratas pasaron
    if (userRepository.existsByEmail(dto.email)) {
        return Result.Failure("Email ya registrado")
    }
    
    return Result.Success(User(/* ... */))
}
```

### 3. Reutiliza mappers

```kotlin
// ✅ BUENO: Reutiliza AddressMapper
object CompanyMapper : DomainMapper<CompanyDto, Company> {
    override fun toDomain(dto: CompanyDto): Result<Company> {
        return AddressMapper.toDomain(dto.address).map { address ->
            Company(dto.name, address)
        }
    }
    
    override fun toDto(domain: Company): CompanyDto = TODO()
}

// ❌ MALO: Duplica lógica de validación de dirección
object CompanyMapper : DomainMapper<CompanyDto, Company> {
    override fun toDomain(dto: CompanyDto): Result<Company> {
        // Duplica toda la validación de AddressMapper :(
        if (dto.address.street.isBlank()) return Result.Failure("Calle vacía")
        if (dto.address.city.isBlank()) return Result.Failure("Ciudad vacía")
        // ...
    }
}
```

### 4. Separa validación de construcción

```kotlin
// ✅ BUENO: Validación clara y separada
override fun toDomain(dto: UserDto): Result<User> {
    // 1. Validación
    val errors = accumulateValidationErrors(separator = "\n") {
        add(validateEmail(dto.email))
        add(validateRange(dto.age, 18, 120, "edad"))
    }
    if (errors != null) return Result.Failure(errors)
    
    // 2. Construcción (solo si validación pasó)
    return Result.Success(
        User(
            email = Email(dto.email),
            age = Age(dto.age)
        )
    )
}

// ❌ MALO: Validación mezclada con construcción
override fun toDomain(dto: UserDto): Result<User> {
    return Result.Success(
        User(
            email = if (isValidEmail(dto.email)) Email(dto.email) 
                    else return Result.Failure("Email inválido"),  // Confuso
            age = if (dto.age >= 18) Age(dto.age)
                  else return Result.Failure("Edad inválida")  // Difícil de leer
        )
    )
}
```

### 5. Documenta invariantes del dominio

```kotlin
/**
 * Representa un usuario válido del sistema.
 * 
 * Invariantes garantizados:
 * - email: formato válido con @ y dominio
 * - age: entre 18 y 120 años
 * - username: 3-30 caracteres, solo letras, números y _
 * - role: uno de los roles válidos del sistema
 */
data class User(
    val email: Email,
    val age: Age,
    val username: Username,
    val role: UserRole
)
```

### 6. Usa objects para mappers sin estado

```kotlin
// ✅ BUENO: object para mapper sin dependencias
object UserMapper : DomainMapper<UserDto, User> {
    override fun toDomain(dto: UserDto): Result<User> = TODO()
    override fun toDto(domain: User): UserDto = TODO()
}

// ✅ BUENO: class para mapper con dependencias
class OrderMapper(
    private val productRepository: ProductRepository
) : DomainMapper<OrderDto, Order> {
    override fun toDomain(dto: OrderDto): Result<Order> = TODO()
    override fun toDto(domain: Order): OrderDto = TODO()
}
```

### 7. Haz toDto siempre exitoso

```kotlin
// ✅ BUENO: toDto nunca falla
override fun toDto(domain: User): UserDto {
    return UserDto(
        username = domain.username.value,
        email = domain.email.value,
        age = domain.age.value
    )
}

// ❌ MALO: toDto con validación innecesaria
override fun toDto(domain: User): UserDto {
    // domain ya es válido, no necesita validación
    if (domain.age < 18) throw IllegalStateException("Esto nunca debería pasar")
    
    return UserDto(/* ... */)
}
```

### 8. Usa traverse para listas de conversiones

```kotlin
// ✅ BUENO: traverse
override fun toDomain(dto: OrderDto): Result<Order> {
    val items = dto.items.traverse { CartItemMapper.toDomain(it) }
    return items.map { Order(it) }
}

// ❌ MENOS EFICIENTE: map + sequence
override fun toDomain(dto: OrderDto): Result<Order> {
    val items = dto.items.map { CartItemMapper.toDomain(it) }.sequence()
    return items.map { Order(it) }
}
```

## Referencias

- **DomainMapper.kt**: Interfaz base
- **MapperExtensions.kt**: Extensiones avanzadas (AppError, fallback, partial, metrics)
- **ValidationHelpers.kt**: Funciones de validación
- **AccumulativeValidation.kt**: Sistema de acumulación de errores
- **UserMappingExample.kt**: Ejemplo completo de implementación

## Tests

Ver ejemplos de tests en:
- `DomainMapperTest.kt`: Tests unitarios del mapper
- `ValidationIntegrationTest.kt`: Tests de validación compleja
- `ConversionIntegrationTest.kt`: Tests de flujos completos
