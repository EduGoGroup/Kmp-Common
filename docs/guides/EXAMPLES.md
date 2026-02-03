# Guía de Ejemplos

Esta guía presenta ejemplos completos end-to-end que integran serialización JSON, validación y manejo de errores en casos de uso reales.

## Tabla de Contenidos

- [Ejemplo 1: Registro de Usuario](#ejemplo-1-registro-de-usuario)
- [Ejemplo 2: Procesamiento de Formulario](#ejemplo-2-procesamiento-de-formulario)
- [Ejemplo 3: API Client con Retry](#ejemplo-3-api-client-con-retry)
- [Ejemplo 4: Validación Multi-Nivel](#ejemplo-4-validación-multi-nivel)
- [Ejemplo 5: Pipeline de Datos](#ejemplo-5-pipeline-de-datos)
- [Ejemplo 6: Configuración con Fallbacks](#ejemplo-6-configuración-con-fallbacks)
- [Ejemplo 7: Validación Acumulativa en Formulario](#ejemplo-7-validación-acumulativa-en-formulario)

## Ejemplo 1: Registro de Usuario

Este ejemplo muestra un flujo completo de registro: validación, procesamiento, serialización y persistencia.

```kotlin
import com.edugo.test.module.core.*
import com.edugo.test.module.validators.*
import com.edugo.test.module.serialization.*
import kotlinx.serialization.Serializable

@Serializable
data class RegistroRequest(
    val email: String,
    val password: String,
    val passwordConfirm: String,
    val nombre: String,
    val edad: Int
) : ValidatableModel {
    override fun validate(): AppError? {
        // Validaciones básicas
        validateEmail(email)?.let { return it }
        validateMinLength(password, 8)?.let { return it }
        validateMinLength(nombre, 2)?.let { return it }
        validateRange(edad, 18, 100)?.let { return it }
        
        // Validación cruzada
        if (!password.matchesPassword(passwordConfirm)) {
            return AppError.validation(
                code = ErrorCode.VALIDATION_PASSWORD_MISMATCH,
                message = "Las contraseñas no coinciden"
            )
        }
        
        return null
    }
}

@Serializable
data class Usuario(
    val id: String,
    val email: String,
    val nombre: String,
    val edad: Int
)

class UsuarioService {
    private val repository = UsuarioRepository()
    
    fun registrar(requestJson: String): Result<String> {
        return requestJson
            // 1. Deserializar
            .fromJson<RegistroRequest>()
            // 2. Validar
            .flatMap { request ->
                request.validate()?.let { failure(it) } ?: success(request)
            }
            // 3. Verificar que no existe
            .flatMap { request ->
                if (repository.existeEmail(request.email)) {
                    failure(AppError.business(
                        code = ErrorCode.BUSINESS_DUPLICATE_ENTRY,
                        message = "El email ya está registrado"
                    ))
                } else {
                    success(request)
                }
            }
            // 4. Crear usuario
            .map { request ->
                Usuario(
                    id = generateUUID(),
                    email = request.email,
                    nombre = request.nombre,
                    edad = request.edad
                )
            }
            // 5. Guardar en BD
            .flatMap { usuario ->
                repository.guardar(usuario)
            }
            // 6. Serializar respuesta
            .flatMap { usuario ->
                usuario.toJson()
            }
    }
}

// Uso
fun main() {
    val service = UsuarioService()
    
    val requestJson = """
        {
            "email": "juan@ejemplo.com",
            "password": "segura123",
            "passwordConfirm": "segura123",
            "nombre": "Juan Pérez",
            "edad": 25
        }
    """.trimIndent()
    
    val resultado = service.registrar(requestJson)
    
    resultado
        .onSuccess { usuarioJson ->
            println("✅ Usuario registrado exitosamente:")
            println(usuarioJson)
        }
        .onFailure { error ->
            println("❌ Error en el registro:")
            println("  Código: ${error.code}")
            println("  Mensaje: ${error.message}")
        }
}
```

**Output exitoso:**
```
✅ Usuario registrado exitosamente:
{"id":"550e8400-e29b-41d4-a716-446655440000","email":"juan@ejemplo.com","nombre":"Juan Pérez","edad":25}
```

**Output con error:**
```
❌ Error en el registro:
  Código: VALIDATION_INVALID_EMAIL
  Mensaje: Invalid email format
```

## Ejemplo 2: Procesamiento de Formulario

Formulario con validación acumulativa que muestra todos los errores a la vez.

```kotlin
import com.edugo.test.module.validators.*
import kotlinx.serialization.Serializable

@Serializable
data class FormularioContacto(
    val nombre: String,
    val email: String,
    val telefono: String,
    val mensaje: String,
    val edad: Int?
)

class FormularioValidator {
    fun validar(formulario: FormularioContacto): AccumulativeValidation {
        return AccumulativeValidation().apply {
            // Validar nombre
            addError(validateMinLength(formulario.nombre, 2))
            addError(validateMaxLength(formulario.nombre, 100))
            
            // Validar email
            addError(validateEmail(formulario.email))
            
            // Validar teléfono (solo dígitos, 10 caracteres)
            addError(validateNumeric(formulario.telefono))
            if (formulario.telefono.length != 10) {
                addError(AppError.validation(
                    code = ErrorCode.VALIDATION_INVALID_FORMAT,
                    message = "El teléfono debe tener exactamente 10 dígitos"
                ))
            }
            
            // Validar mensaje
            addError(validateMinLength(formulario.mensaje, 10))
            addError(validateMaxLength(formulario.mensaje, 500))
            
            // Validar edad (opcional)
            formulario.edad?.let { edad ->
                addError(validateRange(edad, 18, 120))
            }
        }
    }
}

// Uso
fun procesarFormulario(formularioJson: String): Result<String> {
    return formularioJson
        .fromJson<FormularioContacto>()
        .flatMap { formulario ->
            val validacion = FormularioValidator().validar(formulario)
            
            if (validacion.hasErrors()) {
                // Mostrar todos los errores
                val errores = validacion.getErrors()
                println("❌ Se encontraron ${errores.size} errores:")
                errores.forEachIndexed { index, error ->
                    println("  ${index + 1}. ${error.message}")
                }
                
                validacion.toResult(formulario)
            } else {
                success(formulario)
            }
        }
        .map { formulario ->
            "✅ Formulario procesado exitosamente para: ${formulario.nombre}"
        }
}

// Ejemplo con múltiples errores
fun main() {
    val formularioInvalido = """
        {
            "nombre": "J",
            "email": "correo-invalido",
            "telefono": "123",
            "mensaje": "Hola",
            "edad": 150
        }
    """.trimIndent()
    
    procesarFormulario(formularioInvalido)
    
    // Output:
    // ❌ Se encontraron 5 errores:
    //   1. Value must be at least 2 characters long
    //   2. Invalid email format
    //   3. El teléfono debe tener exactamente 10 dígitos
    //   4. Value must be at least 10 characters long
    //   5. Value 150 is out of range [18, 120]
}
```

## Ejemplo 3: API Client con Retry

Cliente HTTP que maneja errores de red con reintentos automáticos.

```kotlin
import com.edugo.test.module.core.*
import com.edugo.test.module.serialization.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

class ApiClient {
    private val maxRetries = 3
    private val retryDelayMs = 1000L
    
    suspend fun <T> get(
        url: String,
        maxRetries: Int = this.maxRetries
    ): Result<String> {
        var lastError: AppError? = null
        
        repeat(maxRetries) { intento ->
            println("🔄 Intento ${intento + 1}/$maxRetries: $url")
            
            val resultado = ejecutarRequest(url)
            
            // Si es éxito, retornar inmediatamente
            if (resultado is Result.Success) {
                println("✅ Request exitoso")
                return resultado
            }
            
            // Si es error
            if (resultado is Result.Failure) {
                lastError = resultado.error
                
                // Si no es reintenTable, fallar inmediatamente
                if (!resultado.error.retryable) {
                    println("❌ Error no reintenTable: ${resultado.error.message}")
                    return resultado
                }
                
                println("⚠️  Error reintenTable: ${resultado.error.message}")
                
                // Si no es el último intento, esperar
                if (intento < maxRetries - 1) {
                    val delay = retryDelayMs * (intento + 1)
                    println("⏳ Esperando ${delay}ms antes del siguiente intento...")
                    delay(delay)
                }
            }
        }
        
        println("❌ Todos los intentos fallaron")
        return failure(lastError ?: AppError.network(
            code = ErrorCode.NETWORK_UNKNOWN_ERROR,
            message = "Request falló después de $maxRetries intentos"
        ))
    }
    
    private fun ejecutarRequest(url: String): Result<String> {
        return try {
            // Simulación de llamada HTTP
            when {
                url.contains("timeout") -> {
                    failure(AppError.network(
                        code = ErrorCode.NETWORK_TIMEOUT,
                        message = "Request timeout after 30s"
                    ))
                }
                url.contains("500") -> {
                    failure(AppError.network(
                        code = ErrorCode.NETWORK_SERVER_ERROR,
                        message = "Internal server error"
                    ))
                }
                else -> {
                    success("""{"success": true, "data": "OK"}""")
                }
            }
        } catch (e: Exception) {
            failure(AppError.fromThrowable(
                throwable = e,
                code = ErrorCode.NETWORK_UNKNOWN_ERROR
            ))
        }
    }
}

// Uso
suspend fun main() {
    val client = ApiClient()
    
    // Request con timeout (reintenTable)
    val resultado1 = client.get("https://api.ejemplo.com/timeout")
    
    // Request exitoso
    val resultado2 = client.get("https://api.ejemplo.com/usuarios")
        .flatMap { json ->
            json.fromJson<ApiResponse<String>>()
        }
        .map { response ->
            response.data ?: "Sin datos"
        }
    
    resultado2.onSuccess { data ->
        println("Datos recibidos: $data")
    }
}
```

**Output:**
```
🔄 Intento 1/3: https://api.ejemplo.com/timeout
⚠️  Error reintenTable: Request timeout after 30s
⏳ Esperando 1000ms antes del siguiente intento...
🔄 Intento 2/3: https://api.ejemplo.com/timeout
⚠️  Error reintenTable: Request timeout after 30s
⏳ Esperando 2000ms antes del siguiente intento...
🔄 Intento 3/3: https://api.ejemplo.com/timeout
⚠️  Error reintenTable: Request timeout after 30s
❌ Todos los intentos fallaron
```

## Ejemplo 4: Validación Multi-Nivel

Validación en múltiples capas con reglas de negocio complejas.

```kotlin
import com.edugo.test.module.validators.*
import kotlinx.serialization.Serializable

@Serializable
data class Pedido(
    val usuarioId: String,
    val items: List<ItemPedido>,
    val metodoPago: String,
    val direccionEnvio: DireccionEnvio?
) : ValidatableModel {
    override fun validate(): AppError? {
        // Validar usuario
        if (!usuarioId.isValidUUID()) {
            return AppError.validation(
                code = ErrorCode.VALIDATION_INVALID_UUID,
                message = "ID de usuario inválido"
            )
        }
        
        // Validar items
        if (items.isEmpty()) {
            return AppError.validation(
                code = ErrorCode.VALIDATION_FAILED,
                message = "El pedido debe tener al menos un item"
            )
        }
        
        // Validar cada item
        items.forEachIndexed { index, item ->
            item.validate()?.let { error ->
                return AppError.validation(
                    code = error.code,
                    message = "Item ${index + 1}: ${error.message}"
                )
            }
        }
        
        // Validar método de pago
        if (metodoPago !in listOf("tarjeta", "efectivo", "transferencia")) {
            return AppError.validation(
                code = ErrorCode.VALIDATION_FAILED,
                message = "Método de pago no válido"
            )
        }
        
        // Validar dirección si el método no es "retiro"
        if (metodoPago != "retiro" && direccionEnvio == null) {
            return AppError.validation(
                code = ErrorCode.VALIDATION_REQUIRED_FIELD,
                message = "Dirección de envío requerida"
            )
        }
        
        direccionEnvio?.validate()?.let { return it }
        
        return null
    }
}

@Serializable
data class ItemPedido(
    val productoId: String,
    val cantidad: Int,
    val precio: Double
) : ValidatableModel {
    override fun validate(): AppError? {
        if (!productoId.isValidUUID()) {
            return AppError.validation(
                code = ErrorCode.VALIDATION_INVALID_UUID,
                message = "ID de producto inválido"
            )
        }
        
        validateRange(cantidad, 1, 100)?.let { return it }
        
        if (precio <= 0) {
            return AppError.validation(
                code = ErrorCode.VALIDATION_OUT_OF_RANGE,
                message = "El precio debe ser mayor a 0"
            )
        }
        
        return null
    }
}

@Serializable
data class DireccionEnvio(
    val calle: String,
    val numero: String,
    val ciudad: String,
    val codigoPostal: String
) : ValidatableModel {
    override fun validate(): AppError? {
        validateMinLength(calle, 3)?.let { return it }
        validateNotEmpty(numero)?.let { return it }
        validateMinLength(ciudad, 2)?.let { return it }
        validateNumeric(codigoPostal)?.let { return it }
        
        if (codigoPostal.length != 5) {
            return AppError.validation(
                code = ErrorCode.VALIDATION_INVALID_FORMAT,
                message = "El código postal debe tener 5 dígitos"
            )
        }
        
        return null
    }
}

class PedidoService {
    fun procesarPedido(pedidoJson: String): Result<String> {
        return pedidoJson
            // 1. Deserializar
            .fromJson<Pedido>()
            // 2. Validar estructura
            .flatMap { pedido ->
                pedido.validate()?.let { failure(it) } ?: success(pedido)
            }
            // 3. Validar reglas de negocio
            .flatMap { pedido ->
                validarReglasNegocio(pedido)
            }
            // 4. Calcular total
            .map { pedido ->
                val total = pedido.items.sumOf { it.precio * it.cantidad }
                "Pedido procesado. Total: $$total"
            }
    }
    
    private fun validarReglasNegocio(pedido: Pedido): Result<Pedido> {
        // Validar stock (simulado)
        val itemsSinStock = pedido.items.filter { it.cantidad > 50 }
        if (itemsSinStock.isNotEmpty()) {
            return failure(AppError.business(
                code = ErrorCode.BUSINESS_RULE_VIOLATION,
                message = "Algunos items no tienen suficiente stock",
                details = "Items sin stock: ${itemsSinStock.size}"
            ))
        }
        
        // Validar monto mínimo
        val total = pedido.items.sumOf { it.precio * it.cantidad }
        if (total < 100) {
            return failure(AppError.business(
                code = ErrorCode.BUSINESS_RULE_VIOLATION,
                message = "El monto mínimo de compra es $100",
                details = "Total actual: $$total"
            ))
        }
        
        return success(pedido)
    }
}
```

## Ejemplo 5: Pipeline de Datos

Pipeline de transformación y validación de datos con múltiples etapas.

```kotlin
import com.edugo.test.module.core.*
import com.edugo.test.module.serialization.*
import kotlinx.serialization.Serializable

@Serializable
data class DatosRaw(
    val email: String,
    val age: String, // Nota: viene como String
    val name: String,
    val phoneNumber: String
)

@Serializable
data class DatosNormalizados(
    val email: String,
    val edad: Int,
    val nombre: String,
    val telefono: String
)

@Serializable
data class DatosEnriquecidos(
    val email: String,
    val edad: Int,
    val nombre: String,
    val telefono: String,
    val segmento: String,
    val prioridad: String
)

class DataPipeline {
    fun procesar(jsonRaw: String): Result<String> {
        return jsonRaw
            // 1. Parsear datos raw
            .fromJson<DatosRaw>()
            .logStep("1. Parseado")
            
            // 2. Normalizar
            .flatMap { normalizar(it) }
            .logStep("2. Normalizado")
            
            // 3. Validar
            .flatMap { validar(it) }
            .logStep("3. Validado")
            
            // 4. Enriquecer
            .map { enriquecer(it) }
            .logStep("4. Enriquecido")
            
            // 5. Serializar resultado
            .flatMap { it.toJson() }
            .logStep("5. Serializado")
    }
    
    private fun normalizar(raw: DatosRaw): Result<DatosNormalizados> {
        // Convertir edad de String a Int
        val edad = raw.age.toIntOrNull()
            ?: return failure(AppError.validation(
                code = ErrorCode.VALIDATION_INVALID_FORMAT,
                message = "Edad debe ser un número"
            ))
        
        return success(DatosNormalizados(
            email = raw.email.lowercase().trim(),
            edad = edad,
            nombre = raw.name.trim()
                .split(" ")
                .joinToString(" ") { it.capitalize() },
            telefono = raw.phoneNumber.replace(Regex("[^0-9]"), "")
        ))
    }
    
    private fun validar(datos: DatosNormalizados): Result<DatosNormalizados> {
        return datos.email
            .validateEmail()
            .flatMap {
                validateRange(datos.edad, 18, 100)
                    ?.let { failure(it) }
                    ?: success(datos)
            }
            .flatMap {
                if (datos.telefono.length != 10) {
                    failure(AppError.validation(
                        code = ErrorCode.VALIDATION_INVALID_FORMAT,
                        message = "Teléfono debe tener 10 dígitos"
                    ))
                } else {
                    success(datos)
                }
            }
    }
    
    private fun enriquecer(datos: DatosNormalizados): DatosEnriquecidos {
        // Calcular segmento por edad
        val segmento = when {
            datos.edad < 25 -> "joven"
            datos.edad < 40 -> "adulto"
            datos.edad < 60 -> "maduro"
            else -> "senior"
        }
        
        // Calcular prioridad
        val prioridad = if (datos.edad > 60) "alta" else "normal"
        
        return DatosEnriquecidos(
            email = datos.email,
            edad = datos.edad,
            nombre = datos.nombre,
            telefono = datos.telefono,
            segmento = segmento,
            prioridad = prioridad
        )
    }
    
    private fun <T> Result<T>.logStep(step: String): Result<T> {
        when (this) {
            is Result.Success -> println("  ✅ $step: OK")
            is Result.Failure -> println("  ❌ $step: ${error.message}")
            is Result.Loading -> println("  ⏳ $step: Loading...")
        }
        return this
    }
}

// Uso
fun main() {
    val pipeline = DataPipeline()
    
    val rawJson = """
        {
            "email": "  JUAN@EJEMPLO.COM  ",
            "age": "35",
            "name": "juan pérez garcía",
            "phoneNumber": "(555) 123-4567"
        }
    """.trimIndent()
    
    println("🚀 Iniciando pipeline de datos...\n")
    
    val resultado = pipeline.procesar(rawJson)
    
    resultado.onSuccess { json ->
        println("\n✅ Pipeline completado exitosamente:")
        println(json)
    }.onFailure { error ->
        println("\n❌ Pipeline falló:")
        println("  Código: ${error.code}")
        println("  Mensaje: ${error.message}")
    }
}
```

**Output exitoso:**
```
🚀 Iniciando pipeline de datos...

  ✅ 1. Parseado: OK
  ✅ 2. Normalizado: OK
  ✅ 3. Validado: OK
  ✅ 4. Enriquecido: OK
  ✅ 5. Serializado: OK

✅ Pipeline completado exitosamente:
{"email":"juan@ejemplo.com","edad":35,"nombre":"Juan Pérez García","telefono":"5551234567","segmento":"adulto","prioridad":"normal"}
```

## Ejemplo 6: Configuración con Fallbacks

Sistema de configuración con múltiples fuentes y fallbacks.

```kotlin
import com.edugo.test.module.core.*
import com.edugo.test.module.serialization.*
import com.edugo.test.module.config.JsonConfig
import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val apiUrl: String,
    val timeout: Int,
    val maxRetries: Int,
    val enableLogging: Boolean
)

class ConfigLoader {
    // Configuración por defecto
    private val defaultConfig = AppConfig(
        apiUrl = "https://api.default.com",
        timeout = 30000,
        maxRetries = 3,
        enableLogging = false
    )
    
    fun cargar(): Result<AppConfig> {
        println("🔧 Cargando configuración...")
        
        return cargarRemota()
            .onFailure { println("  ⚠️  Remota falló: ${it.message}") }
            .recoverCatching {
                println("  🔄 Intentando configuración local...")
                cargarLocal()
            }
            .onFailure { println("  ⚠️  Local falló: ${it.message}") }
            .recoverCatching {
                println("  🔄 Intentando configuración de entorno...")
                cargarEntorno()
            }
            .onFailure { println("  ⚠️  Entorno falló: ${it.message}") }
            .recover {
                println("  ✅ Usando configuración por defecto")
                success(defaultConfig)
            }
            .onSuccess { config ->
                println("\n✅ Configuración cargada:")
                println("  API URL: ${config.apiUrl}")
                println("  Timeout: ${config.timeout}ms")
                println("  Max Retries: ${config.maxRetries}")
                println("  Logging: ${config.enableLogging}")
            }
    }
    
    private fun cargarRemota(): Result<AppConfig> {
        // Simular carga desde API remota
        return failure(AppError.network(
            code = ErrorCode.NETWORK_TIMEOUT,
            message = "No se pudo conectar al servidor de configuración"
        ))
    }
    
    private fun cargarLocal(): Result<AppConfig> {
        // Simular carga desde archivo local
        val configJson = """
            {
                "apiUrl": "https://api.local.com",
                "timeout": 60000,
                "maxRetries": 5,
                "enableLogging": true
            }
        """.trimIndent()
        
        return safeDecodeFromString<AppConfig>(
            json = configJson,
            format = JsonConfig.Default
        )
    }
    
    private fun cargarEntorno(): Result<AppConfig> {
        // Simular carga desde variables de entorno
        return failure(AppError.system(
            code = ErrorCode.SYSTEM_FILE_NOT_FOUND,
            message = "Variables de entorno no configuradas"
        ))
    }
}

// Uso
fun main() {
    val loader = ConfigLoader()
    val config = loader.cargar()
    
    // La aplicación siempre obtiene una configuración válida
    config.onSuccess { appConfig ->
        iniciarApp(appConfig)
    }
}

fun iniciarApp(config: AppConfig) {
    println("\n🚀 Aplicación iniciada con configuración:")
    println("  ${config.apiUrl}")
}
```

**Output:**
```
🔧 Cargando configuración...
  ⚠️  Remota falló: No se pudo conectar al servidor de configuración
  🔄 Intentando configuración local...

✅ Configuración cargada:
  API URL: https://api.local.com
  Timeout: 60000ms
  Max Retries: 5
  Logging: true

🚀 Aplicación iniciada con configuración:
  https://api.local.com
```

## Ejemplo 7: Validación Acumulativa en Formulario

Ejemplo completo de un formulario de checkout con validación acumulativa.

```kotlin
import com.edugo.test.module.validators.*
import com.edugo.test.module.serialization.*
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutForm(
    val email: String,
    val nombre: String,
    val apellido: String,
    val telefono: String,
    val tarjeta: String,
    val cvv: String,
    val direccion: String,
    val ciudad: String,
    val codigoPostal: String,
    val aceptaTerminos: Boolean
)

class CheckoutValidator {
    fun validarFormulario(form: CheckoutForm): ValidacionResultado {
        val validacion = AccumulativeValidation()
        val erroresPorCampo = mutableMapOf<String, String>()
        
        // Email
        validateEmail(form.email)?.let { error ->
            validacion.addError(error)
            erroresPorCampo["email"] = error.message
        }
        
        // Nombre
        validateMinLength(form.nombre, 2)?.let { error ->
            validacion.addError(error)
            erroresPorCampo["nombre"] = error.message
        }
        
        // Apellido
        validateMinLength(form.apellido, 2)?.let { error ->
            validacion.addError(error)
            erroresPorCampo["apellido"] = error.message
        }
        
        // Teléfono
        validateNumeric(form.telefono)?.let { error ->
            validacion.addError(error)
            erroresPorCampo["telefono"] = error.message
        }
        if (form.telefono.length != 10) {
            val error = AppError.validation(
                code = ErrorCode.VALIDATION_INVALID_FORMAT,
                message = "Debe tener 10 dígitos"
            )
            validacion.addError(error)
            erroresPorCampo["telefono"] = error.message
        }
        
        // Tarjeta (16 dígitos)
        validateNumeric(form.tarjeta)?.let { error ->
            validacion.addError(error)
            erroresPorCampo["tarjeta"] = error.message
        }
        if (form.tarjeta.length != 16) {
            val error = AppError.validation(
                code = ErrorCode.VALIDATION_INVALID_FORMAT,
                message = "Debe tener 16 dígitos"
            )
            validacion.addError(error)
            erroresPorCampo["tarjeta"] = error.message
        }
        
        // CVV (3 dígitos)
        validateNumeric(form.cvv)?.let { error ->
            validacion.addError(error)
            erroresPorCampo["cvv"] = error.message
        }
        if (form.cvv.length != 3) {
            val error = AppError.validation(
                code = ErrorCode.VALIDATION_INVALID_FORMAT,
                message = "Debe tener 3 dígitos"
            )
            validacion.addError(error)
            erroresPorCampo["cvv"] = error.message
        }
        
        // Dirección
        validateMinLength(form.direccion, 5)?.let { error ->
            validacion.addError(error)
            erroresPorCampo["direccion"] = error.message
        }
        
        // Ciudad
        validateMinLength(form.ciudad, 2)?.let { error ->
            validacion.addError(error)
            erroresPorCampo["ciudad"] = error.message
        }
        
        // Código postal
        validateNumeric(form.codigoPostal)?.let { error ->
            validacion.addError(error)
            erroresPorCampo["codigoPostal"] = error.message
        }
        if (form.codigoPostal.length != 5) {
            val error = AppError.validation(
                code = ErrorCode.VALIDATION_INVALID_FORMAT,
                message = "Debe tener 5 dígitos"
            )
            validacion.addError(error)
            erroresPorCampo["codigoPostal"] = error.message
        }
        
        // Términos
        if (!form.aceptaTerminos) {
            val error = AppError.validation(
                code = ErrorCode.VALIDATION_FAILED,
                message = "Debes aceptar los términos y condiciones"
            )
            validacion.addError(error)
            erroresPorCampo["aceptaTerminos"] = error.message
        }
        
        return ValidacionResultado(
            valido = !validacion.hasErrors(),
            errores = validacion.getErrors(),
            erroresPorCampo = erroresPorCampo
        )
    }
}

data class ValidacionResultado(
    val valido: Boolean,
    val errores: List<AppError>,
    val erroresPorCampo: Map<String, String>
)

class CheckoutService {
    private val validator = CheckoutValidator()
    
    fun procesarCheckout(formJson: String): Result<String> {
        return formJson
            .fromJson<CheckoutForm>()
            .flatMap { form ->
                val validacion = validator.validarFormulario(form)
                
                if (validacion.valido) {
                    success(form)
                } else {
                    // Mostrar errores por campo (útil para UI)
                    println("\n❌ Errores de validación (${validacion.errores.size}):\n")
                    validacion.erroresPorCampo.forEach { (campo, mensaje) ->
                        println("  • $campo: $mensaje")
                    }
                    
                    failure(AppError.validation(
                        code = ErrorCode.VALIDATION_FAILED,
                        message = "Formulario con errores",
                        details = "${validacion.errores.size} campos inválidos"
                    ))
                }
            }
            .map { form ->
                "✅ Checkout procesado exitosamente para ${form.nombre} ${form.apellido}"
            }
    }
}

// Uso
fun main() {
    val service = CheckoutService()
    
    // Formulario con varios errores
    val formInvalido = """
        {
            "email": "correo-invalido",
            "nombre": "J",
            "apellido": "P",
            "telefono": "123",
            "tarjeta": "1234",
            "cvv": "12",
            "direccion": "Cll",
            "ciudad": "M",
            "codigoPostal": "123",
            "aceptaTerminos": false
        }
    """.trimIndent()
    
    service.procesarCheckout(formInvalido)
    
    // Formulario válido
    val formValido = """
        {
            "email": "juan@ejemplo.com",
            "nombre": "Juan",
            "apellido": "Pérez",
            "telefono": "5551234567",
            "tarjeta": "1234567890123456",
            "cvv": "123",
            "direccion": "Calle Principal 123",
            "ciudad": "México",
            "codigoPostal": "12345",
            "aceptaTerminos": true
        }
    """.trimIndent()
    
    println("\n" + "=".repeat(50) + "\n")
    
    service.procesarCheckout(formValido).onSuccess { mensaje ->
        println("\n$mensaje")
    }
}
```

**Output:**
```
❌ Errores de validación (10):

  • email: Invalid email format
  • nombre: Value must be at least 2 characters long
  • apellido: Value must be at least 2 characters long
  • telefono: Debe tener 10 dígitos
  • tarjeta: Debe tener 16 dígitos
  • cvv: Debe tener 3 dígitos
  • direccion: Value must be at least 5 characters long
  • ciudad: Value must be at least 2 characters long
  • codigoPostal: Debe tener 5 dígitos
  • aceptaTerminos: Debes aceptar los términos y condiciones

==================================================

✅ Checkout procesado exitosamente para Juan Pérez
```

## Conclusión

Estos ejemplos demuestran cómo usar Kmp-Common para:

1. **Integrar serialización, validación y manejo de errores** en flujos completos
2. **Encadenar operaciones** de forma elegante con `flatMap` y `map`
3. **Manejar errores** de forma explícita y con contexto rico
4. **Validar datos** tanto fail-fast como acumulativo según el caso de uso
5. **Implementar patrones** como retry, fallbacks y pipelines de datos
6. **Proporcionar UX mejorado** mostrando todos los errores de validación a la vez

Usa estos ejemplos como base para implementar tus propios flujos de negocio de forma robusta y type-safe.
