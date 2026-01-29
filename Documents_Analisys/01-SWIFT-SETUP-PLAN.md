# 01-SWIFT-SETUP-PLAN.md

> **Versión**: 2.0.0  
> **Fecha**: Enero 2026  
> **Status**: DEFINITIVO - Versiones en piedra  
> **Compatibilidad**: iOS 26+ | macOS 26+ | Swift 6.2 | Xcode 26+
> **Nota LLM**: Este plan será ejecutado por LLMs via CLI. Ver sección "Limitaciones CLI/Xcode" abajo.

---

## 📋 Índice

1. [Limitaciones CLI y Xcode](#-limitaciones-cli-y-xcode)
2. [Stack Definitivo](#-stack-definitivo)
3. [Estructura del Package.swift](#-estructura-del-packageswift)
4. [Módulos del Sistema](#-módulos-del-sistema)
5. [CI/CD GitHub Actions](#-cicd-github-actions)
6. [Grafo de Dependencias](#-grafo-de-dependencias)

---

## ⚠️ Limitaciones CLI y Xcode

### Restricción: Algunas configuraciones SOLO en Xcode GUI

Este proyecto se ejecutará por LLMs via **línea de comandos**, pero algunas tareas requieren configuración manual en Xcode que NO tiene interfaz CLI.

### Qué SÍ puede hacer LLM (CLI)

✅ Crear y modificar archivos Swift  
✅ Editar Package.swift  
✅ Escribir tests  
✅ Ejecutar tests vía `xcodebuild`  
✅ Crear commits y PRs  
✅ Compilar proyectos con `xcodebuild`  

### Qué NO puede hacer LLM (Requiere Xcode GUI)

❌ Crear targets dentro de proyectos iOS  
❌ Añadir/remover frameworks en build phases  
❌ Configurar code signing  
❌ Configurar build settings avanzados  
❌ Crear schemes personalizados  
❌ Configurar dependencies entre targets  
❌ Configurar run scripts en build phases  

### Protocolo para Tareas Bloqueadas

**Si una tarea requiere configuración Xcode**:

1. **Crear documento CONFIGURACION_XCODE_[MODULO].md**
   - Pasos detallados y numerados
   - Qué configurar y cómo
   - Screenshots si es crítico
   - Paso de verificación al final

2. **Ejemplo documento**:

```markdown
# CONFIGURACION_XCODE_EduGoNetwork.md

## Paso 1: Crear el target SPM
1. File → New → Package
2. Language: Swift
3. Name: EduGoNetwork
4. ✓ Crear en /Sources/EduGoNetwork

## Paso 2: Añadir Security Framework
1. Target "EduGoNetwork" → Build Phases → Link Binary
2. Click + → buscar "Network.framework"
3. Añadir

## Verificación
Ejecutar en CLI:
\`\`\`bash
xcodebuild build -scheme EduGoNetwork -destination 'generic/platform=iOS'
\`\`\`
Debe compilar sin errores.
```

3. **Usar este documento en cada desglose**:
   - En análisis general: Identificar módulos con configuración Xcode
   - En sprint: Marcar tareas Xcode como bloqueadores
   - En historia: Especificar si tiene configuración previa
   - En tarea: Incluir enlace al documento de configuración Xcode

### Implicaciones para el Plan

- **Tareas Xcode** son pre-requisitos (actividad 1 de la historia)
- El documento debe existir **antes** que el LLM intente codificar
- Cada tarea debe validar que Xcode está correctamente configurado
- Los LLMs pueden continuar con código SOLO después de documentación Xcode lista

---

## 🎯 Stack Definitivo

### Versiones NO NEGOCIABLES

| Componente | Versión | Justificación |
|------------|---------|---------------|
| **Swift** | 6.2 | Strict concurrency, nuevo modelo UI, async/await mejorado |
| **iOS Minimum** | 26.0 | Nuevas APIs de UI, concurrencia nativa, sin retrocompatibilidad |
| **macOS Minimum** | 26.0 | Paridad total con iOS 26, APIs unificadas |
| **Xcode** | 26.0+ | Swift 6.2 toolchain completo |
| **SPM Tools Version** | 6.0 | Strict concurrency checks |
| **watchOS** | 13.0 | Paridad con iOS 26 |
| **tvOS** | 26.0 | Paridad con iOS 26 |
| **visionOS** | 3.0 | Paridad con iOS 26 |

> ⚠️ **DECISIÓN ARQUITECTÓNICA**: Usamos iOS/macOS 26 para aprovechar al 100% Swift 6.2.
> No hay soporte para versiones anteriores. Esto elimina condicionales, workarounds y deuda técnica.

### APIs Nativas iOS 26+ (CERO Dependencias Externas)

| Funcionalidad | API Nativa iOS 26 | Justificación |
|---------------|-------------------|---------------|
| **Logging** | `os.Logger` | Unified Logging, mejor debugging async en Xcode 26 |
| **Networking** | `Network.framework` | Async nativo, Codable directo, TLV framer built-in |
| **Storage** | `UserDefaults` | Configuraciones simples |
| **Secure Storage** | `Security.framework` (Keychain) | SecItem API, encriptación hardware |
| **JSON** | `Codable` / `JSONEncoder` / `JSONDecoder` | Built-in, óptimo rendimiento |
| **Dates** | `Foundation.Date` | ISO8601DateFormatter nativo |
| **UUID** | `Foundation.UUID` | Built-in desde Swift 1.0 |
| **Concurrency** | `@MainActor` default + `@concurrent` | Swift 6.2 modelo simplificado |
| **AI On-Device** | `Foundation Models` | LLM 3B on-device, gratis, offline, privado |
| **Arrays Fijos** | `InlineArray<N, Element>` | Stack allocation, sin heap, sin ARC |

### Novedades Swift 6.2 / iOS 26 que USAMOS

```swift
// 1. @MainActor por defecto a nivel de módulo (Package.swift)
// Todo el código UI corre en main thread automáticamente

// 2. @concurrent para background explícito
@concurrent
func fetchDataInBackground() async -> Data { ... }

// 3. Network framework con async/await y Codable directo
let connection = NWConnection(...)
let response: MyModel = try await connection.receive() // Decodifica automático

// 4. InlineArray para buffers de tamaño fijo (sin heap)
let buffer: InlineArray<1024, UInt8> = .init(repeating: 0)

// 5. Foundation Models para AI on-device
import FoundationModels
let session = LanguageModelSession()
let summary = try await session.respond(to: "Resume este texto...")
```

### Filosofía: Zero External Dependencies

```
✅ Network.framework  (en vez de URLSession/Alamofire) - Async nativo iOS 26
✅ os.Logger          (en vez de CocoaLumberjack/SwiftyBeaver)
✅ Keychain           (en vez de KeychainAccess)
✅ Codable            (en vez de SwiftyJSON)
✅ Foundation Models  (en vez de OpenAI SDK) - AI on-device gratis
✅ @MainActor default (en vez de DispatchQueue.main manualmente)
```

---

## 📦 Estructura del Package.swift

### Package.swift Principal

```swift
// swift-tools-version: 6.0

import PackageDescription

// ============================================================================
// SWIFT 6.2 + iOS 26 CONFIGURATION
// ============================================================================
// - @MainActor por defecto en todos los módulos (defaultIsolation)
// - @concurrent para operaciones explícitas en background
// - Network.framework con async/await nativo
// - Foundation Models para AI on-device
// - InlineArray para buffers sin heap allocation
// ============================================================================

/// Swift settings compartidos para Swift 6.2
let swift62Settings: [SwiftSetting] = [
    // Swift 6.2: Strict concurrency es el default, ya no es experimental
    .swiftLanguageMode(.v6),
    
    // iOS 26: @MainActor como isolation default del módulo
    .defaultIsolation(MainActor.self),
    
    // Upcoming features de Swift 6.2
    .enableUpcomingFeature("InferSendableFromCaptures"),
    .enableUpcomingFeature("RegionBasedIsolation"),
]

let package = Package(
    name: "EduGoAppleModules",
    defaultLocalization: "es",
    platforms: [
        .iOS(.v26),
        .macOS(.v26),
        .watchOS(.v13),
        .tvOS(.v26),
        .visionOS(.v3)
    ],
    products: [
        // === Módulos Base ===
        .library(name: "EduGoCommon", targets: ["EduGoCommon"]),
        .library(name: "EduGoLogger", targets: ["EduGoLogger"]),
        
        // === Módulos de Infraestructura ===
        .library(name: "EduGoNetwork", targets: ["EduGoNetwork"]),
        .library(name: "EduGoStorage", targets: ["EduGoStorage"]),
        
        // === Módulos de Dominio ===
        .library(name: "EduGoModels", targets: ["EduGoModels"]),
        .library(name: "EduGoRoles", targets: ["EduGoRoles"]),
        .library(name: "EduGoAuth", targets: ["EduGoAuth"]),
        
        // === Módulos de Aplicación ===
        .library(name: "EduGoAPI", targets: ["EduGoAPI"]),
        .library(name: "EduGoAnalytics", targets: ["EduGoAnalytics"]),
        .library(name: "EduGoAI", targets: ["EduGoAI"]),  // Nuevo: Foundation Models
        
        // === Bundle Completo ===
        .library(name: "EduGoKit", targets: ["EduGoKit"])
    ],
    dependencies: [
        // 🎯 CERO DEPENDENCIAS EXTERNAS
        // Todo se implementa con APIs nativas de Apple iOS 26+
    ],
    targets: [
        // === TIER 0: Sin dependencias internas ===
        .target(
            name: "EduGoCommon",
            dependencies: [],
            swiftSettings: swift62Settings
        ),
        
        // === TIER 1: Solo depende de Common ===
        .target(
            name: "EduGoLogger",
            dependencies: ["EduGoCommon"],
            swiftSettings: swift62Settings
        ),
        .target(
            name: "EduGoModels",
            dependencies: ["EduGoCommon"],
            swiftSettings: swift62Settings
        ),
        
        // === TIER 2: Infraestructura ===
        .target(
            name: "EduGoNetwork",
            dependencies: ["EduGoCommon", "EduGoLogger"],
            swiftSettings: swift62Settings
        ),
        .target(
            name: "EduGoStorage",
            dependencies: ["EduGoCommon", "EduGoLogger"],
            swiftSettings: swift62Settings
        ),
        
        // === TIER 3: Dominio ===
        .target(
            name: "EduGoRoles",
            dependencies: ["EduGoCommon", "EduGoModels"],
            swiftSettings: swift62Settings
        ),
        .target(
            name: "EduGoAuth",
            dependencies: [
                "EduGoCommon",
                "EduGoLogger",
                "EduGoNetwork",
                "EduGoStorage",
                "EduGoModels",
                "EduGoRoles"
            ],
            swiftSettings: swift62Settings
        ),
        
        // === TIER 4: Aplicación ===
        .target(
            name: "EduGoAPI",
            dependencies: [
                "EduGoCommon",
                "EduGoLogger",
                "EduGoNetwork",
                "EduGoModels",
                "EduGoAuth"
            ],
            swiftSettings: swift62Settings
        ),
        .target(
            name: "EduGoAnalytics",
            dependencies: [
                "EduGoCommon",
                "EduGoLogger",
                "EduGoNetwork",
                "EduGoStorage"
            ],
            swiftSettings: swift62Settings
        ),
        
        // === TIER 4: AI On-Device (Foundation Models) ===
        .target(
            name: "EduGoAI",
            dependencies: [
                "EduGoCommon",
                "EduGoLogger",
                "EduGoModels"
            ],
            swiftSettings: swift62Settings
        ),
        
        // === Bundle Completo ===
        .target(
            name: "EduGoKit",
            dependencies: [
                "EduGoCommon",
                "EduGoLogger",
                "EduGoNetwork",
                "EduGoStorage",
                "EduGoModels",
                "EduGoRoles",
                "EduGoAuth",
                "EduGoAPI",
                "EduGoAnalytics",
                "EduGoAI"
            ],
            swiftSettings: swift62Settings
        ),
        
        // === Tests ===
        .testTarget(name: "EduGoCommonTests", dependencies: ["EduGoCommon"]),
        .testTarget(name: "EduGoLoggerTests", dependencies: ["EduGoLogger"]),
        .testTarget(name: "EduGoNetworkTests", dependencies: ["EduGoNetwork"]),
        .testTarget(name: "EduGoStorageTests", dependencies: ["EduGoStorage"]),
        .testTarget(name: "EduGoModelsTests", dependencies: ["EduGoModels"]),
        .testTarget(name: "EduGoRolesTests", dependencies: ["EduGoRoles"]),
        .testTarget(name: "EduGoAuthTests", dependencies: ["EduGoAuth"]),
        .testTarget(name: "EduGoAPITests", dependencies: ["EduGoAPI"]),
        .testTarget(name: "EduGoAnalyticsTests", dependencies: ["EduGoAnalytics"]),
        .testTarget(name: "EduGoAITests", dependencies: ["EduGoAI"])
    ]
)
```

### Estructura de Directorios

```
EduGoAppleModules/
├── Package.swift
├── Sources/
│   ├── EduGoCommon/
│   │   ├── ErrorCodes.swift
│   │   ├── Result+Extensions.swift
│   │   └── Sendable+Extensions.swift
│   ├── EduGoLogger/
│   │   ├── Logger.swift
│   │   └── LogLevel.swift
│   ├── EduGoNetwork/
│   │   ├── HTTPClient.swift          # iOS 26: Network.framework + @concurrent
│   │   ├── HTTPMethod.swift
│   │   ├── HTTPError.swift
│   │   └── Request+Extensions.swift
│   ├── EduGoStorage/
│   │   ├── UserDefaultsStorage.swift
│   │   ├── KeychainStorage.swift
│   │   └── StorageProtocol.swift
│   ├── EduGoModels/
│   │   ├── User.swift
│   │   ├── Token.swift
│   │   └── APIResponse.swift
│   ├── EduGoRoles/
│   │   ├── SystemRole.swift           # Roles: admin, teacher, student, guardian
│   │   ├── Permission.swift
│   │   └── RoleManager.swift
│   ├── EduGoAuth/
│   │   ├── AuthManager.swift
│   │   ├── TokenManager.swift
│   │   └── JWTDecoder.swift
│   ├── EduGoAPI/
│   │   ├── APIClient.swift
│   │   ├── Endpoints.swift
│   │   └── APIError.swift
│   ├── EduGoAnalytics/
│   │   ├── AnalyticsManager.swift
│   │   ├── Event.swift
│   │   └── AnalyticsProvider.swift
│   ├── EduGoAI/                        # iOS 26 CASCADA: Foundation Models
│   │   ├── EduGoAI.swift               # LLM 3B on-device
│   │   ├── AISessionConfiguration.swift
│   │   ├── AIResponse.swift
│   │   └── GeneratedQuestion.swift
│   └── EduGoKit/
│       └── EduGoKit.swift
└── Tests/
    ├── EduGoCommonTests/
    ├── EduGoLoggerTests/
    ├── EduGoNetworkTests/
    ├── EduGoStorageTests/
    ├── EduGoModelsTests/
    ├── EduGoRolesTests/
    ├── EduGoAuthTests/
    ├── EduGoAPITests/
    ├── EduGoAnalyticsTests/
    └── EduGoAITests/                   # iOS 26 CASCADA: Tests de Foundation Models
```

---

## 🧩 Módulos del Sistema

### 1. EduGoCommon (TIER 0)

Módulo base con tipos comunes, extensiones y códigos de error.

```swift
// Sources/EduGoCommon/ErrorCodes.swift

import Foundation

/// Códigos de error estandarizados para toda la aplicación.
/// Rango: 1000-9999
/// - 1xxx: Errores de red
/// - 2xxx: Errores de autenticación
/// - 3xxx: Errores de validación
/// - 4xxx: Errores de almacenamiento
/// - 5xxx: Errores de negocio
public enum ErrorCode: Int, Sendable, Equatable, CustomStringConvertible {
    // === Network Errors (1xxx) ===
    case networkUnreachable = 1001
    case networkTimeout = 1002
    case networkSSLError = 1003
    case networkInvalidURL = 1004
    case networkInvalidResponse = 1005
    
    // === HTTP Errors (11xx) ===
    case httpBadRequest = 1100          // 400
    case httpUnauthorized = 1101        // 401
    case httpForbidden = 1102           // 403
    case httpNotFound = 1103            // 404
    case httpConflict = 1104            // 409
    case httpUnprocessableEntity = 1105 // 422
    case httpTooManyRequests = 1106     // 429
    case httpInternalServerError = 1107 // 500
    case httpServiceUnavailable = 1108  // 503
    
    // === Auth Errors (2xxx) ===
    case authTokenExpired = 2001
    case authTokenInvalid = 2002
    case authRefreshFailed = 2003
    case authSessionExpired = 2004
    case authInvalidCredentials = 2005
    case authUserDisabled = 2006
    case authMFARequired = 2007
    
    // === Validation Errors (3xxx) ===
    case validationRequired = 3001
    case validationInvalidFormat = 3002
    case validationTooShort = 3003
    case validationTooLong = 3004
    case validationOutOfRange = 3005
    
    // === Storage Errors (4xxx) ===
    case storageReadFailed = 4001
    case storageWriteFailed = 4002
    case storageDeleteFailed = 4003
    case storageNotFound = 4004
    case storageCorrupted = 4005
    case keychainAccessDenied = 4010
    case keychainItemNotFound = 4011
    case keychainDuplicateItem = 4012
    
    // === Business Errors (5xxx) ===
    case businessRoleInsufficient = 5001
    case businessResourceNotAvailable = 5002
    case businessOperationNotAllowed = 5003
    case businessQuotaExceeded = 5004
    
    public var description: String {
        "E\(rawValue)"
    }
    
    public var localizedMessage: String {
        switch self {
        case .networkUnreachable: return "Sin conexión a internet"
        case .networkTimeout: return "La solicitud tardó demasiado"
        case .authTokenExpired: return "Tu sesión ha expirado"
        case .authInvalidCredentials: return "Credenciales inválidas"
        case .businessRoleInsufficient: return "No tienes permisos suficientes"
        default: return "Error inesperado (\(rawValue))"
        }
    }
}

/// Error base de la aplicación con código estandarizado.
public struct AppError: Error, Sendable, Equatable {
    public let code: ErrorCode
    public let message: String
    public let underlyingError: String?
    
    public init(
        code: ErrorCode,
        message: String? = nil,
        underlyingError: Error? = nil
    ) {
        self.code = code
        self.message = message ?? code.localizedMessage
        self.underlyingError = underlyingError?.localizedDescription
    }
}

extension AppError: LocalizedError {
    public var errorDescription: String? { message }
}
```

```swift
// Sources/EduGoCommon/Result+Extensions.swift

import Foundation

public extension Result where Failure == AppError {
    /// Mapea el éxito manteniendo el tipo de error.
    func mapSuccess<NewSuccess>(
        _ transform: (Success) throws -> NewSuccess
    ) -> Result<NewSuccess, AppError> {
        switch self {
        case .success(let value):
            do {
                return .success(try transform(value))
            } catch let error as AppError {
                return .failure(error)
            } catch {
                return .failure(AppError(
                    code: .validationInvalidFormat,
                    underlyingError: error
                ))
            }
        case .failure(let error):
            return .failure(error)
        }
    }
    
    /// Obtiene el valor o lanza el error.
    func unwrap() throws -> Success {
        switch self {
        case .success(let value): return value
        case .failure(let error): throw error
        }
    }
}
```

---

### 2. EduGoLogger (TIER 1)

Logger basado en `os.Logger` con Unified Logging System.

> ⚠️ **iOS 26 CASCADA**: Xcode 26 incluye mejoras significativas en el debugging de async/await 
> que se integran automáticamente con os.Logger para mejor trazabilidad de concurrencia.

```swift
// Sources/EduGoLogger/Logger.swift

import Foundation
import os
import EduGoCommon

/// Wrapper sobre os.Logger con niveles estandarizados.
/// Integra con Console.app y Instruments para debugging avanzado.
public struct EduGoLogger: Sendable {
    private let logger: os.Logger
    public let subsystem: String
    public let category: String
    
    /// Inicializa el logger con subsystem y categoría.
    /// - Parameters:
    ///   - subsystem: Bundle identifier (ej: "com.edugo.app")
    ///   - category: Categoría del módulo (ej: "Network", "Auth")
    public init(subsystem: String = Bundle.main.bundleIdentifier ?? "com.edugo",
                category: String) {
        self.subsystem = subsystem
        self.category = category
        self.logger = os.Logger(subsystem: subsystem, category: category)
    }
    
    // MARK: - Log Levels
    
    /// Debug: Solo visible en Console.app con filtro.
    /// Uso: Información detallada para desarrollo.
    public func debug(_ message: @autoclosure () -> String,
                      file: String = #file,
                      function: String = #function,
                      line: Int = #line) {
        logger.debug("[\(function):\(line)] \(message())")
    }
    
    /// Info: Información general, visible por defecto.
    /// Uso: Eventos importantes del flujo normal.
    public func info(_ message: @autoclosure () -> String) {
        logger.info("\(message())")
    }
    
    /// Notice: Información importante que destaca.
    /// Uso: Eventos significativos del sistema.
    public func notice(_ message: @autoclosure () -> String) {
        logger.notice("\(message())")
    }
    
    /// Warning: Situación anómala pero recuperable.
    /// Uso: Condiciones inesperadas que no bloquean.
    public func warning(_ message: @autoclosure () -> String) {
        logger.warning("⚠️ \(message())")
    }
    
    /// Error: Error que afecta la operación actual.
    /// Uso: Errores recuperables o esperados.
    public func error(_ message: @autoclosure () -> String,
                      error: Error? = nil) {
        if let error {
            logger.error("❌ \(message()) | Error: \(error.localizedDescription)")
        } else {
            logger.error("❌ \(message())")
        }
    }
    
    /// Error con código estandarizado.
    public func error(_ appError: AppError) {
        logger.error("❌ [\(appError.code)] \(appError.message)")
    }
    
    /// Fault: Error crítico del sistema.
    /// Uso: Errores que indican bug o estado corrupto.
    public func fault(_ message: @autoclosure () -> String) {
        logger.fault("🔥 CRITICAL: \(message())")
    }
    
    // MARK: - Signposts para Performance
    
    /// Inicia un signpost para medir rendimiento en Instruments.
    public func signpostBegin(
        _ name: StaticString,
        id: OSSignpostID = .exclusive
    ) {
        let signposter = OSSignposter(logger: logger)
        signposter.beginInterval(name, id: id)
    }
    
    /// Finaliza un signpost.
    public func signpostEnd(
        _ name: StaticString,
        id: OSSignpostID = .exclusive
    ) {
        let signposter = OSSignposter(logger: logger)
        signposter.endInterval(name, id: id)
    }
}

// MARK: - Loggers Predefinidos

public extension EduGoLogger {
    /// Logger para el módulo de red.
    static let network = EduGoLogger(category: "Network")
    
    /// Logger para autenticación.
    static let auth = EduGoLogger(category: "Auth")
    
    /// Logger para almacenamiento.
    static let storage = EduGoLogger(category: "Storage")
    
    /// Logger para API.
    static let api = EduGoLogger(category: "API")
    
    /// Logger para analytics.
    static let analytics = EduGoLogger(category: "Analytics")
    
    /// Logger general de la app.
    static let app = EduGoLogger(category: "App")
}
```

---

### 3. EduGoNetwork (TIER 2)

Cliente HTTP basado en **Network.framework** de iOS 26 con async/await nativo y soporte Codable directo.

> ⚠️ **iOS 26 CASCADA**: Este módulo usa características EXCLUSIVAS de iOS 26:
> - `@concurrent` attribute (Swift 6.2) para operaciones de red en background
> - `Duration` type en vez de `TimeInterval`
> - Network.framework con async/await nativo

> ⚠️ **iOS 26**: Usamos `Network.framework` en vez de `URLSession` para aprovechar:
> - Async/await nativo integrado
> - Envío/recepción de tipos Codable directamente
> - TLV framer built-in
> - Mejor integración con Swift Concurrency

```swift
// Sources/EduGoNetwork/HTTPClient.swift

import Foundation
import Network
import EduGoCommon
import EduGoLogger

/// Métodos HTTP soportados.
public enum HTTPMethod: String, Sendable {
    case get = "GET"
    case post = "POST"
    case put = "PUT"
    case patch = "PATCH"
    case delete = "DELETE"
}

/// Configuración del cliente HTTP.
public struct HTTPClientConfiguration: Sendable {
    public let baseURL: URL
    public let timeout: Duration
    
    public init(
        baseURL: URL,
        timeout: Duration = .seconds(30)
    ) {
        self.baseURL = baseURL
        self.timeout = timeout
    }
}

/// Cliente HTTP thread-safe basado en Network.framework (iOS 26+).
/// Usa @MainActor por defecto (Swift 6.2), operaciones de red con @concurrent.
public actor HTTPClient {
    private let configuration: HTTPClientConfiguration
    private let logger = EduGoLogger.network
    
    /// Headers globales aplicados a todas las requests.
    private var globalHeaders: [String: String] = [
        "Content-Type": "application/json",
        "Accept": "application/json"
    ]
    
    public init(configuration: HTTPClientConfiguration) {
        self.configuration = configuration
    }
    
    // MARK: - Header Management
    
    /// Establece un header global.
    public func setHeader(_ value: String, forKey key: String) {
        globalHeaders[key] = value
    }
    
    /// Remueve un header global.
    public func removeHeader(forKey key: String) {
        globalHeaders.removeValue(forKey: key)
    }
    
    // MARK: - Request Execution (iOS 26 Network.framework)
    
    /// Ejecuta una request y decodifica la respuesta.
    /// Usa @concurrent para ejecutar en background thread.
    @concurrent
    public func request<T: Decodable & Sendable>(
        method: HTTPMethod,
        path: String,
        body: (any Encodable & Sendable)? = nil,
        headers: [String: String] = [:],
        responseType: T.Type
    ) async -> Result<T, AppError> {
        // Construir URL
        guard let url = URL(string: path, relativeTo: configuration.baseURL) else {
            return .failure(AppError(code: .networkInvalidURL))
        }
        
        // Construir request con URLRequest (Network.framework lo usa internamente)
        var request = URLRequest(url: url)
        request.httpMethod = method.rawValue
        request.timeoutInterval = configuration.timeout.components.seconds
        
        // Aplicar headers
        for (key, value) in globalHeaders {
            request.setValue(value, forHTTPHeaderField: key)
        }
        for (key, value) in headers {
            request.setValue(value, forHTTPHeaderField: key)
        }
        
        // Codificar body
        if let body {
            do {
                request.httpBody = try JSONEncoder().encode(body)
            } catch {
                logger.error("Error codificando body", error: error)
                return .failure(AppError(code: .validationInvalidFormat, underlyingError: error))
            }
        }
        
        logger.debug("\(method.rawValue) \(url.absoluteString)")
        
        // Ejecutar con URLSession (backed by Network.framework en iOS 26)
        let data: Data
        let response: URLResponse
        
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch let error as URLError {
            return .failure(mapURLError(error))
        } catch {
            logger.error("Error de red desconocido", error: error)
            return .failure(AppError(code: .networkUnreachable, underlyingError: error))
        }
        
        // Validar response
        guard let httpResponse = response as? HTTPURLResponse else {
            return .failure(AppError(code: .networkInvalidResponse))
        }
        
        logger.debug("Response: \(httpResponse.statusCode)")
        
        // Manejar códigos HTTP
        guard (200...299).contains(httpResponse.statusCode) else {
            return .failure(mapHTTPStatusCode(httpResponse.statusCode, data: data))
        }
        
        // Decodificar
        do {
            let decoded = try JSONDecoder().decode(T.self, from: data)
            return .success(decoded)
        } catch {
            logger.error("Error decodificando respuesta", error: error)
            return .failure(AppError(code: .networkInvalidResponse, underlyingError: error))
        }
    }
    
    // MARK: - Convenience Methods
    
    @concurrent
    public func get<T: Decodable & Sendable>(
        _ path: String,
        responseType: T.Type
    ) async -> Result<T, AppError> {
        await request(method: .get, path: path, responseType: responseType)
    }
    
    @concurrent
    public func post<T: Decodable & Sendable>(
        _ path: String,
        body: any Encodable & Sendable,
        responseType: T.Type
    ) async -> Result<T, AppError> {
        await request(method: .post, path: path, body: body, responseType: responseType)
    }
    
    // MARK: - Error Mapping
    
    private nonisolated func mapURLError(_ error: URLError) -> AppError {
        switch error.code {
        case .notConnectedToInternet, .networkConnectionLost:
            return AppError(code: .networkUnreachable, underlyingError: error)
        case .timedOut:
            return AppError(code: .networkTimeout, underlyingError: error)
        case .secureConnectionFailed, .serverCertificateUntrusted:
            return AppError(code: .networkSSLError, underlyingError: error)
        default:
            return AppError(code: .networkUnreachable, underlyingError: error)
        }
    }
    
    private nonisolated func mapHTTPStatusCode(_ statusCode: Int, data: Data) -> AppError {
        // Intentar extraer mensaje del servidor
        let serverMessage = try? JSONDecoder().decode(
            ServerErrorResponse.self,
            from: data
        ).message
        
        switch statusCode {
        case 400: return AppError(code: .httpBadRequest, message: serverMessage)
        case 401: return AppError(code: .httpUnauthorized, message: serverMessage)
        case 403: return AppError(code: .httpForbidden, message: serverMessage)
        case 404: return AppError(code: .httpNotFound, message: serverMessage)
        case 409: return AppError(code: .httpConflict, message: serverMessage)
        case 422: return AppError(code: .httpUnprocessableEntity, message: serverMessage)
        case 429: return AppError(code: .httpTooManyRequests, message: serverMessage)
        case 500: return AppError(code: .httpInternalServerError, message: serverMessage)
        case 503: return AppError(code: .httpServiceUnavailable, message: serverMessage)
        default: return AppError(code: .networkInvalidResponse, message: "HTTP \(statusCode)")
        }
    }
}

private struct ServerErrorResponse: Decodable, Sendable {
    let message: String?
}
```

---

### 4. EduGoStorage (TIER 2)

Almacenamiento con UserDefaults y Keychain nativo.

```swift
// Sources/EduGoStorage/KeychainStorage.swift

import Foundation
import Security
import EduGoCommon
import EduGoLogger

/// Almacenamiento seguro usando Keychain Services (SecItem API).
public actor KeychainStorage {
    private let service: String
    private let accessGroup: String?
    private let logger = EduGoLogger.storage
    
    /// Inicializa el almacenamiento Keychain.
    /// - Parameters:
    ///   - service: Identificador del servicio (ej: bundle identifier)
    ///   - accessGroup: Grupo de acceso para compartir entre apps (opcional)
    public init(
        service: String = Bundle.main.bundleIdentifier ?? "com.edugo",
        accessGroup: String? = nil
    ) {
        self.service = service
        self.accessGroup = accessGroup
    }
    
    // MARK: - CRUD Operations
    
    /// Guarda un valor en Keychain.
    public func save(_ data: Data, forKey key: String) -> Result<Void, AppError> {
        // Primero intentar actualizar
        let updateResult = update(data, forKey: key)
        if case .success = updateResult {
            return .success(())
        }
        
        // Si no existe, crear
        var query = baseQuery(forKey: key)
        query[kSecValueData as String] = data
        query[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        
        let status = SecItemAdd(query as CFDictionary, nil)
        
        switch status {
        case errSecSuccess:
            logger.debug("Keychain: Guardado \(key)")
            return .success(())
        case errSecDuplicateItem:
            // No debería pasar después del update, pero por seguridad
            return update(data, forKey: key)
        default:
            logger.error("Keychain save error: \(status)")
            return .failure(AppError(code: .storageWriteFailed, message: "Keychain error: \(status)"))
        }
    }
    
    /// Lee un valor de Keychain.
    public func read(forKey key: String) -> Result<Data, AppError> {
        var query = baseQuery(forKey: key)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        switch status {
        case errSecSuccess:
            guard let data = result as? Data else {
                return .failure(AppError(code: .storageCorrupted))
            }
            return .success(data)
        case errSecItemNotFound:
            return .failure(AppError(code: .keychainItemNotFound))
        default:
            logger.error("Keychain read error: \(status)")
            return .failure(AppError(code: .storageReadFailed, message: "Keychain error: \(status)"))
        }
    }
    
    /// Actualiza un valor en Keychain.
    private func update(_ data: Data, forKey key: String) -> Result<Void, AppError> {
        let query = baseQuery(forKey: key)
        let attributesToUpdate: [String: Any] = [
            kSecValueData as String: data
        ]
        
        let status = SecItemUpdate(query as CFDictionary, attributesToUpdate as CFDictionary)
        
        switch status {
        case errSecSuccess:
            logger.debug("Keychain: Actualizado \(key)")
            return .success(())
        case errSecItemNotFound:
            return .failure(AppError(code: .keychainItemNotFound))
        default:
            return .failure(AppError(code: .storageWriteFailed))
        }
    }
    
    /// Elimina un valor de Keychain.
    public func delete(forKey key: String) -> Result<Void, AppError> {
        let query = baseQuery(forKey: key)
        let status = SecItemDelete(query as CFDictionary)
        
        switch status {
        case errSecSuccess, errSecItemNotFound:
            logger.debug("Keychain: Eliminado \(key)")
            return .success(())
        default:
            logger.error("Keychain delete error: \(status)")
            return .failure(AppError(code: .storageDeleteFailed))
        }
    }
    
    /// Elimina todos los items del servicio.
    public func deleteAll() -> Result<Void, AppError> {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service
        ]
        
        if let accessGroup {
            query[kSecAttrAccessGroup as String] = accessGroup
        }
        
        let status = SecItemDelete(query as CFDictionary)
        
        switch status {
        case errSecSuccess, errSecItemNotFound:
            logger.notice("Keychain: Limpiado todo el servicio")
            return .success(())
        default:
            return .failure(AppError(code: .storageDeleteFailed))
        }
    }
    
    // MARK: - Convenience Methods
    
    /// Guarda una cadena en Keychain.
    public func saveString(_ value: String, forKey key: String) -> Result<Void, AppError> {
        guard let data = value.data(using: .utf8) else {
            return .failure(AppError(code: .validationInvalidFormat))
        }
        return save(data, forKey: key)
    }
    
    /// Lee una cadena de Keychain.
    public func readString(forKey key: String) -> Result<String, AppError> {
        read(forKey: key).flatMap { data in
            guard let string = String(data: data, encoding: .utf8) else {
                return .failure(AppError(code: .storageCorrupted))
            }
            return .success(string)
        }
    }
    
    /// Guarda un objeto Codable en Keychain.
    public func save<T: Encodable>(_ value: T, forKey key: String) -> Result<Void, AppError> {
        do {
            let data = try JSONEncoder().encode(value)
            return save(data, forKey: key)
        } catch {
            return .failure(AppError(code: .validationInvalidFormat, underlyingError: error))
        }
    }
    
    /// Lee un objeto Codable de Keychain.
    public func read<T: Decodable>(forKey key: String, as type: T.Type) -> Result<T, AppError> {
        read(forKey: key).flatMap { data in
            do {
                let decoded = try JSONDecoder().decode(T.self, from: data)
                return .success(decoded)
            } catch {
                return .failure(AppError(code: .storageCorrupted, underlyingError: error))
            }
        }
    }
    
    // MARK: - Private Helpers
    
    private func baseQuery(forKey key: String) -> [String: Any] {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]
        
        if let accessGroup {
            query[kSecAttrAccessGroup as String] = accessGroup
        }
        
        return query
    }
}
```

```swift
// Sources/EduGoStorage/UserDefaultsStorage.swift

import Foundation
import EduGoCommon

/// Almacenamiento simple usando UserDefaults.
/// Thread-safe mediante actor.
public actor UserDefaultsStorage {
    private let defaults: UserDefaults
    private let keyPrefix: String
    
    public init(
        suiteName: String? = nil,
        keyPrefix: String = "edugo."
    ) {
        self.defaults = suiteName.flatMap { UserDefaults(suiteName: $0) } ?? .standard
        self.keyPrefix = keyPrefix
    }
    
    private func prefixedKey(_ key: String) -> String {
        "\(keyPrefix)\(key)"
    }
    
    // MARK: - Generic Operations
    
    public func set<T: Codable>(_ value: T, forKey key: String) throws {
        let data = try JSONEncoder().encode(value)
        defaults.set(data, forKey: prefixedKey(key))
    }
    
    public func get<T: Codable>(forKey key: String, as type: T.Type) -> T? {
        guard let data = defaults.data(forKey: prefixedKey(key)) else { return nil }
        return try? JSONDecoder().decode(T.self, from: data)
    }
    
    public func remove(forKey key: String) {
        defaults.removeObject(forKey: prefixedKey(key))
    }
    
    // MARK: - Primitive Types
    
    public func setBool(_ value: Bool, forKey key: String) {
        defaults.set(value, forKey: prefixedKey(key))
    }
    
    public func getBool(forKey key: String) -> Bool {
        defaults.bool(forKey: prefixedKey(key))
    }
    
    public func setString(_ value: String, forKey key: String) {
        defaults.set(value, forKey: prefixedKey(key))
    }
    
    public func getString(forKey key: String) -> String? {
        defaults.string(forKey: prefixedKey(key))
    }
}
```

---

### 5. EduGoModels (TIER 1)

Modelos de datos Codable y Sendable.

```swift
// Sources/EduGoModels/User.swift

import Foundation
import EduGoCommon

/// Modelo de usuario autenticado.
public struct User: Codable, Sendable, Equatable, Identifiable {
    public let id: UUID
    public let email: String
    public let firstName: String
    public let lastName: String
    public let avatarURL: URL?
    public let isActive: Bool
    public let createdAt: Date
    
    public var fullName: String {
        "\(firstName) \(lastName)"
    }
    
    public init(
        id: UUID,
        email: String,
        firstName: String,
        lastName: String,
        avatarURL: URL? = nil,
        isActive: Bool = true,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.email = email
        self.firstName = firstName
        self.lastName = lastName
        self.avatarURL = avatarURL
        self.isActive = isActive
        self.createdAt = createdAt
    }
}

/// Tokens de autenticación.
public struct AuthTokens: Codable, Sendable, Equatable {
    public let accessToken: String
    public let refreshToken: String
    public let expiresAt: Date
    
    public var isExpired: Bool {
        Date() >= expiresAt
    }
    
    /// Margen de seguridad (5 minutos) antes de expiración.
    public var needsRefresh: Bool {
        Date().addingTimeInterval(300) >= expiresAt
    }
    
    public init(accessToken: String, refreshToken: String, expiresAt: Date) {
        self.accessToken = accessToken
        self.refreshToken = refreshToken
        self.expiresAt = expiresAt
    }
}

/// Respuesta genérica de API.
public struct APIResponse<T: Codable & Sendable>: Codable, Sendable {
    public let success: Bool
    public let data: T?
    public let message: String?
    public let errorCode: Int?
    
    public init(success: Bool, data: T? = nil, message: String? = nil, errorCode: Int? = nil) {
        self.success = success
        self.data = data
        self.message = message
        self.errorCode = errorCode
    }
}

/// Respuesta paginada de API.
public struct PaginatedResponse<T: Codable & Sendable>: Codable, Sendable {
    public let items: [T]
    public let total: Int
    public let page: Int
    public let pageSize: Int
    public let hasMore: Bool
    
    public init(items: [T], total: Int, page: Int, pageSize: Int) {
        self.items = items
        self.total = total
        self.page = page
        self.pageSize = pageSize
        self.hasMore = (page * pageSize) < total
    }
}
```

---

### 6. EduGoRoles (TIER 3)

Sistema de roles y permisos.

> **IMPORTANTE**: Los roles DEBEN coincidir exactamente con el backend (edugo-shared/common/types/enum/role.go)

```swift
// Sources/EduGoRoles/SystemRole.swift

import Foundation
import EduGoCommon

/// Roles del sistema EduGo.
/// DEBEN coincidir exactamente con el backend: admin, teacher, student, guardian
public enum SystemRole: String, Codable, Sendable, CaseIterable {
    case admin = "admin"
    case teacher = "teacher"
    case student = "student"
    case guardian = "guardian"
    
    /// Nivel jerárquico del rol (mayor = más privilegios).
    public var level: Int {
        switch self {
        case .admin: return 100
        case .teacher: return 50
        case .student: return 30
        case .guardian: return 20
        }
    }
    
    /// Nombre legible del rol.
    public var displayName: String {
        switch self {
        case .admin: return "Administrador"
        case .teacher: return "Profesor"
        case .student: return "Estudiante"
        case .guardian: return "Acudiente"
        }
    }
    
    /// Verifica si este rol tiene al menos el nivel del rol especificado.
    public func hasAtLeast(_ role: SystemRole) -> Bool {
        self.level >= role.level
    }
}

/// Permisos granulares del sistema.
/// DEBEN coincidir con los permisos del KMP para consistencia.
public struct Permission: OptionSet, Sendable, Hashable {
    public let rawValue: UInt64
    
    public init(rawValue: UInt64) {
        self.rawValue = rawValue
    }
    
    // === Materiales ===
    public static let viewMaterials     = Permission(rawValue: 1 << 0)
    public static let uploadMaterials   = Permission(rawValue: 1 << 1)
    public static let editMaterials     = Permission(rawValue: 1 << 2)
    public static let deleteMaterials   = Permission(rawValue: 1 << 3)
    
    // === Quizzes ===
    public static let takeQuizzes       = Permission(rawValue: 1 << 10)
    public static let createQuizzes     = Permission(rawValue: 1 << 11)
    public static let gradeQuizzes      = Permission(rawValue: 1 << 12)
    
    // === Progreso ===
    public static let viewOwnProgress   = Permission(rawValue: 1 << 20)
    public static let viewStudentProgress = Permission(rawValue: 1 << 21)
    
    // === Usuarios ===
    public static let viewUsers         = Permission(rawValue: 1 << 30)
    public static let manageUsers       = Permission(rawValue: 1 << 31)
    
    // === Reportes ===
    public static let viewReports       = Permission(rawValue: 1 << 40)
    public static let exportReports     = Permission(rawValue: 1 << 41)
    
    // === Conjuntos predefinidos por rol ===
    public static let studentPermissions: Permission = [.viewMaterials, .takeQuizzes, .viewOwnProgress]
    public static let guardianPermissions: Permission = [.viewOwnProgress]
    public static let teacherPermissions: Permission = [
        .viewMaterials, .uploadMaterials, .editMaterials,
        .createQuizzes, .gradeQuizzes, 
        .viewStudentProgress, .viewReports
    ]
    public static let adminPermissions: Permission = [
        .viewMaterials, .uploadMaterials, .editMaterials, .deleteMaterials,
        .takeQuizzes, .createQuizzes, .gradeQuizzes,
        .viewOwnProgress, .viewStudentProgress,
        .viewUsers, .manageUsers,
        .viewReports, .exportReports
    ]
}

/// Manager de roles que evalúa permisos.
public actor RoleManager {
    private var currentRole: SystemRole = .student
    private var permissions: Permission = []
    
    public init() {}
    
    /// Establece el rol actual del usuario.
    public func setRole(_ role: SystemRole) {
        self.currentRole = role
        self.permissions = defaultPermissions(for: role)
    }
    
    /// Verifica si el usuario tiene un permiso específico.
    public func hasPermission(_ permission: Permission) -> Bool {
        permissions.contains(permission)
    }
    
    /// Verifica si el usuario tiene al menos el rol especificado.
    public func hasRole(_ role: SystemRole) -> Bool {
        currentRole.hasAtLeast(role)
    }
    
    /// Obtiene el rol actual.
    public func getCurrentRole() -> SystemRole {
        currentRole
    }
    
    private func defaultPermissions(for role: SystemRole) -> Permission {
        switch role {
        case .admin:
            return .adminPermissions
        case .teacher:
            return .teacherPermissions
        case .student:
            return .studentPermissions
        case .guardian:
            return .guardianPermissions
        }
    }
}
```

---

### 7. EduGoAuth (TIER 3)

Autenticación con JWT nativo.

```swift
// Sources/EduGoAuth/JWTDecoder.swift

import Foundation
import EduGoCommon

/// Claims estándar de JWT.
public struct JWTClaims: Codable, Sendable {
    public let sub: String           // Subject (user ID)
    public let exp: Int              // Expiration (Unix timestamp)
    public let iat: Int              // Issued at
    public let email: String?
    public let role: String?
    
    /// Fecha de expiración.
    public var expirationDate: Date {
        Date(timeIntervalSince1970: TimeInterval(exp))
    }
    
    /// Si el token está expirado.
    public var isExpired: Bool {
        Date() >= expirationDate
    }
}

/// Decodificador de JWT nativo (sin dependencias externas).
public enum JWTDecoder {
    
    /// Decodifica el payload de un JWT.
    /// - Parameter token: Token JWT completo (header.payload.signature)
    /// - Returns: Claims decodificados o error
    public static func decode(_ token: String) -> Result<JWTClaims, AppError> {
        let segments = token.split(separator: ".")
        
        guard segments.count == 3 else {
            return .failure(AppError(code: .authTokenInvalid, message: "JWT malformado"))
        }
        
        let payloadSegment = String(segments[1])
        
        // Base64URL -> Base64 estándar
        var base64 = payloadSegment
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        
        // Padding
        let remainder = base64.count % 4
        if remainder > 0 {
            base64 += String(repeating: "=", count: 4 - remainder)
        }
        
        guard let data = Data(base64Encoded: base64) else {
            return .failure(AppError(code: .authTokenInvalid, message: "Base64 inválido"))
        }
        
        do {
            let claims = try JSONDecoder().decode(JWTClaims.self, from: data)
            return .success(claims)
        } catch {
            return .failure(AppError(code: .authTokenInvalid, underlyingError: error))
        }
    }
    
    /// Extrae el user ID del token.
    public static func extractUserId(_ token: String) -> String? {
        guard case .success(let claims) = decode(token) else { return nil }
        return claims.sub
    }
    
    /// Verifica si el token está expirado.
    public static func isExpired(_ token: String) -> Bool {
        guard case .success(let claims) = decode(token) else { return true }
        return claims.isExpired
    }
}
```

```swift
// Sources/EduGoAuth/AuthManager.swift

import Foundation
import EduGoCommon
import EduGoLogger
import EduGoNetwork
import EduGoStorage
import EduGoModels
import EduGoRoles

/// Estado de autenticación observable.
public enum AuthState: Sendable, Equatable {
    case unknown
    case authenticated(User)
    case unauthenticated
}

/// Manager principal de autenticación.
public actor AuthManager {
    private let httpClient: HTTPClient
    private let keychain: KeychainStorage
    private let roleManager: RoleManager
    private let logger = EduGoLogger.auth
    
    private var tokens: AuthTokens?
    private var currentUser: User?
    
    // Keys para Keychain
    private enum Keys {
        static let accessToken = "auth.accessToken"
        static let refreshToken = "auth.refreshToken"
        static let expiresAt = "auth.expiresAt"
        static let user = "auth.user"
    }
    
    public init(
        httpClient: HTTPClient,
        keychain: KeychainStorage,
        roleManager: RoleManager
    ) {
        self.httpClient = httpClient
        self.keychain = keychain
        self.roleManager = roleManager
    }
    
    // MARK: - Session Management
    
    /// Restaura la sesión desde Keychain.
    public func restoreSession() async -> AuthState {
        logger.info("Restaurando sesión...")
        
        // Leer tokens
        guard case .success(let accessToken) = await keychain.readString(forKey: Keys.accessToken),
              case .success(let refreshToken) = await keychain.readString(forKey: Keys.refreshToken),
              case .success(let expiresAtData) = await keychain.read(forKey: Keys.expiresAt),
              let expiresAt = try? JSONDecoder().decode(Date.self, from: expiresAtData) else {
            logger.info("No hay sesión guardada")
            return .unauthenticated
        }
        
        let tokens = AuthTokens(
            accessToken: accessToken,
            refreshToken: refreshToken,
            expiresAt: expiresAt
        )
        
        // Verificar expiración
        if tokens.isExpired {
            logger.notice("Token expirado, intentando refresh...")
            return await refreshTokens(tokens.refreshToken)
        }
        
        // Restaurar usuario
        if case .success(let user) = await keychain.read(forKey: Keys.user, as: User.self) {
            self.tokens = tokens
            self.currentUser = user
            
            // Configurar rol
            if let roleClaim = JWTDecoder.decode(accessToken).map({ $0.role }).flatMap({ $0 }),
               let role = SystemRole(rawValue: roleClaim) {
                await roleManager.setRole(role)
            }
            
            // Configurar header de autorización
            await httpClient.setHeader("Bearer \(accessToken)", forKey: "Authorization")
            
            logger.info("Sesión restaurada para: \(user.email)")
            return .authenticated(user)
        }
        
        return .unauthenticated
    }
    
    /// Login con email y password.
    public func login(email: String, password: String) async -> Result<User, AppError> {
        logger.info("Iniciando login para: \(email)")
        
        struct LoginRequest: Encodable, Sendable {
            let email: String
            let password: String
        }
        
        struct LoginResponse: Decodable, Sendable {
            let accessToken: String
            let refreshToken: String
            let expiresIn: Int
            let user: User
        }
        
        let result = await httpClient.post(
            "/auth/login",
            body: LoginRequest(email: email, password: password),
            responseType: APIResponse<LoginResponse>.self
        )
        
        switch result {
        case .success(let response):
            guard let data = response.data else {
                return .failure(AppError(code: .authInvalidCredentials))
            }
            
            // Calcular fecha de expiración
            let expiresAt = Date().addingTimeInterval(TimeInterval(data.expiresIn))
            let tokens = AuthTokens(
                accessToken: data.accessToken,
                refreshToken: data.refreshToken,
                expiresAt: expiresAt
            )
            
            // Guardar en Keychain
            await saveTokens(tokens)
            _ = await keychain.save(data.user, forKey: Keys.user)
            
            // Actualizar estado
            self.tokens = tokens
            self.currentUser = data.user
            
            // Configurar header
            await httpClient.setHeader("Bearer \(data.accessToken)", forKey: "Authorization")
            
            // Configurar rol
            if let roleClaim = JWTDecoder.decode(data.accessToken).map({ $0.role }).flatMap({ $0 }),
               let role = SystemRole(rawValue: roleClaim) {
                await roleManager.setRole(role)
            }
            
            logger.notice("Login exitoso: \(data.user.email)")
            return .success(data.user)
            
        case .failure(let error):
            logger.error(error)
            return .failure(error)
        }
    }
    
    /// Cierra la sesión.
    public func logout() async {
        logger.notice("Cerrando sesión...")
        
        // Limpiar Keychain
        _ = await keychain.delete(forKey: Keys.accessToken)
        _ = await keychain.delete(forKey: Keys.refreshToken)
        _ = await keychain.delete(forKey: Keys.expiresAt)
        _ = await keychain.delete(forKey: Keys.user)
        
        // Limpiar estado
        tokens = nil
        currentUser = nil
        
        // Remover header
        await httpClient.removeHeader(forKey: "Authorization")
        
        // Reset rol (student como default más restrictivo disponible)
        await roleManager.setRole(.student)
        
        logger.info("Sesión cerrada")
    }
    
    /// Obtiene el access token actual (refrescando si es necesario).
    public func getValidAccessToken() async -> Result<String, AppError> {
        guard let tokens else {
            return .failure(AppError(code: .authSessionExpired))
        }
        
        if tokens.needsRefresh {
            let refreshResult = await refreshTokens(tokens.refreshToken)
            if case .authenticated = refreshResult,
               let newTokens = self.tokens {
                return .success(newTokens.accessToken)
            } else {
                return .failure(AppError(code: .authRefreshFailed))
            }
        }
        
        return .success(tokens.accessToken)
    }
    
    // MARK: - Private Methods
    
    private func refreshTokens(_ refreshToken: String) async -> AuthState {
        struct RefreshRequest: Encodable, Sendable {
            let refreshToken: String
        }
        
        struct RefreshResponse: Decodable, Sendable {
            let accessToken: String
            let refreshToken: String
            let expiresIn: Int
        }
        
        let result = await httpClient.post(
            "/auth/refresh",
            body: RefreshRequest(refreshToken: refreshToken),
            responseType: APIResponse<RefreshResponse>.self
        )
        
        switch result {
        case .success(let response):
            guard let data = response.data else {
                await logout()
                return .unauthenticated
            }
            
            let expiresAt = Date().addingTimeInterval(TimeInterval(data.expiresIn))
            let newTokens = AuthTokens(
                accessToken: data.accessToken,
                refreshToken: data.refreshToken,
                expiresAt: expiresAt
            )
            
            await saveTokens(newTokens)
            self.tokens = newTokens
            
            await httpClient.setHeader("Bearer \(data.accessToken)", forKey: "Authorization")
            
            if let user = currentUser {
                return .authenticated(user)
            }
            return .unauthenticated
            
        case .failure:
            logger.warning("Refresh token falló, cerrando sesión")
            await logout()
            return .unauthenticated
        }
    }
    
    private func saveTokens(_ tokens: AuthTokens) async {
        _ = await keychain.saveString(tokens.accessToken, forKey: Keys.accessToken)
        _ = await keychain.saveString(tokens.refreshToken, forKey: Keys.refreshToken)
        if let expiresData = try? JSONEncoder().encode(tokens.expiresAt) {
            _ = await keychain.save(expiresData, forKey: Keys.expiresAt)
        }
    }
}
```

---

### 8. EduGoAPI (TIER 4)

Cliente de API de alto nivel.

```swift
// Sources/EduGoAPI/APIClient.swift

import Foundation
import EduGoCommon
import EduGoLogger
import EduGoNetwork
import EduGoModels
import EduGoAuth

/// Cliente de API principal de EduGo.
public actor APIClient {
    private let httpClient: HTTPClient
    private let authManager: AuthManager
    private let logger = EduGoLogger.api
    
    public init(httpClient: HTTPClient, authManager: AuthManager) {
        self.httpClient = httpClient
        self.authManager = authManager
    }
    
    // MARK: - Generic Request with Auth
    
    /// Ejecuta una request autenticada con refresh automático.
    public func authenticatedRequest<T: Decodable & Sendable>(
        method: HTTPMethod,
        path: String,
        body: (any Encodable & Sendable)? = nil,
        responseType: T.Type
    ) async -> Result<T, AppError> {
        // Obtener token válido
        let tokenResult = await authManager.getValidAccessToken()
        
        guard case .success(let token) = tokenResult else {
            return .failure(AppError(code: .authSessionExpired))
        }
        
        // Ejecutar request
        let result = await httpClient.request(
            method: method,
            path: path,
            body: body,
            headers: ["Authorization": "Bearer \(token)"],
            responseType: responseType
        )
        
        // Si es 401, intentar refresh y reintentar
        if case .failure(let error) = result, error.code == .httpUnauthorized {
            logger.notice("Token rechazado, intentando refresh...")
            
            let refreshResult = await authManager.getValidAccessToken()
            guard case .success(let newToken) = refreshResult else {
                return .failure(AppError(code: .authSessionExpired))
            }
            
            return await httpClient.request(
                method: method,
                path: path,
                body: body,
                headers: ["Authorization": "Bearer \(newToken)"],
                responseType: responseType
            )
        }
        
        return result
    }
    
    // MARK: - Users API
    
    /// Obtiene el perfil del usuario actual.
    public func getProfile() async -> Result<User, AppError> {
        await authenticatedRequest(
            method: .get,
            path: "/users/me",
            responseType: APIResponse<User>.self
        ).flatMap { response in
            guard let user = response.data else {
                return .failure(AppError(code: .networkInvalidResponse))
            }
            return .success(user)
        }
    }
    
    /// Actualiza el perfil del usuario.
    public func updateProfile(_ update: UserProfileUpdate) async -> Result<User, AppError> {
        await authenticatedRequest(
            method: .patch,
            path: "/users/me",
            body: update,
            responseType: APIResponse<User>.self
        ).flatMap { response in
            guard let user = response.data else {
                return .failure(AppError(code: .networkInvalidResponse))
            }
            return .success(user)
        }
    }
    
    // MARK: - Courses API
    
    /// Lista los cursos del usuario.
    public func getCourses(page: Int = 1, pageSize: Int = 20) async -> Result<PaginatedResponse<Course>, AppError> {
        await authenticatedRequest(
            method: .get,
            path: "/courses?page=\(page)&pageSize=\(pageSize)",
            responseType: APIResponse<PaginatedResponse<Course>>.self
        ).flatMap { response in
            guard let data = response.data else {
                return .failure(AppError(code: .networkInvalidResponse))
            }
            return .success(data)
        }
    }
}

// MARK: - Supporting Types

public struct UserProfileUpdate: Encodable, Sendable {
    public let firstName: String?
    public let lastName: String?
    public let avatarURL: URL?
    
    public init(firstName: String? = nil, lastName: String? = nil, avatarURL: URL? = nil) {
        self.firstName = firstName
        self.lastName = lastName
        self.avatarURL = avatarURL
    }
}

public struct Course: Codable, Sendable, Identifiable {
    public let id: UUID
    public let name: String
    public let description: String?
    public let teacherId: UUID
    public let isActive: Bool
}
```

---

### 9. EduGoAnalytics (TIER 4)

Sistema de analytics.

```swift
// Sources/EduGoAnalytics/AnalyticsManager.swift

import Foundation
import EduGoCommon
import EduGoLogger
import EduGoNetwork
import EduGoStorage

/// Evento de analytics.
public struct AnalyticsEvent: Codable, Sendable {
    public let name: String
    public let properties: [String: String]
    public let timestamp: Date
    public let sessionId: String
    
    public init(
        name: String,
        properties: [String: String] = [:],
        sessionId: String
    ) {
        self.name = name
        self.properties = properties
        self.timestamp = Date()
        self.sessionId = sessionId
    }
}

/// Manager de analytics con buffer y envío por lotes.
public actor AnalyticsManager {
    private let httpClient: HTTPClient
    private let storage: UserDefaultsStorage
    private let logger = EduGoLogger.analytics
    
    private var eventBuffer: [AnalyticsEvent] = []
    private var sessionId: String
    private let bufferLimit = 20
    private var isEnabled = true
    
    public init(httpClient: HTTPClient, storage: UserDefaultsStorage) {
        self.httpClient = httpClient
        self.storage = storage
        self.sessionId = UUID().uuidString
    }
    
    // MARK: - Configuration
    
    /// Habilita o deshabilita analytics.
    public func setEnabled(_ enabled: Bool) {
        isEnabled = enabled
        logger.info("Analytics \(enabled ? "habilitado" : "deshabilitado")")
    }
    
    /// Inicia una nueva sesión.
    public func startSession() {
        sessionId = UUID().uuidString
        track("session_start")
        logger.info("Nueva sesión: \(sessionId)")
    }
    
    // MARK: - Tracking
    
    /// Registra un evento.
    public func track(_ eventName: String, properties: [String: String] = [:]) {
        guard isEnabled else { return }
        
        let event = AnalyticsEvent(
            name: eventName,
            properties: properties,
            sessionId: sessionId
        )
        
        eventBuffer.append(event)
        logger.debug("Evento: \(eventName)")
        
        // Flush si alcanzamos el límite
        if eventBuffer.count >= bufferLimit {
            Task {
                await flush()
            }
        }
    }
    
    /// Registra un evento de pantalla.
    public func trackScreen(_ screenName: String) {
        track("screen_view", properties: ["screen": screenName])
    }
    
    /// Registra un error.
    public func trackError(_ error: AppError) {
        track("error", properties: [
            "code": error.code.description,
            "message": error.message
        ])
    }
    
    // MARK: - Flush
    
    /// Envía todos los eventos pendientes.
    public func flush() async {
        guard !eventBuffer.isEmpty else { return }
        
        let eventsToSend = eventBuffer
        eventBuffer.removeAll()
        
        logger.debug("Enviando \(eventsToSend.count) eventos")
        
        struct BatchRequest: Encodable, Sendable {
            let events: [AnalyticsEvent]
        }
        
        struct EmptyResponse: Decodable, Sendable {}
        
        let result = await httpClient.post(
            "/analytics/events",
            body: BatchRequest(events: eventsToSend),
            responseType: EmptyResponse.self
        )
        
        if case .failure(let error) = result {
            logger.error("Error enviando analytics", error: error)
            // Reintentar: devolver eventos al buffer
            eventBuffer.insert(contentsOf: eventsToSend, at: 0)
        }
    }
}
```

---

### 10. EduGoAI (TIER 4) - Foundation Models iOS 26

> ⚠️ **CASCADA iOS 26**: Este módulo solo es posible con iOS 26+ ya que usa el framework **Foundation Models** 
> que permite ejecutar un LLM de 3B parámetros on-device, completamente gratis, offline y privado.

```swift
// Sources/EduGoAI/EduGoAI.swift

import Foundation
import FoundationModels  // iOS 26+ EXCLUSIVO
import EduGoCommon
import EduGoLogger
import EduGoModels

// MARK: - AI Session Configuration

/// Configuración para sesiones de AI on-device.
/// iOS 26 CASCADA: FoundationModels requiere dispositivos con Neural Engine (A14+, M1+).
public struct AISessionConfiguration: Sendable {
    public let maxTokens: Int
    public let temperature: Double
    public let systemPrompt: String?
    
    public init(
        maxTokens: Int = 2048,
        temperature: Double = 0.7,
        systemPrompt: String? = nil
    ) {
        self.maxTokens = maxTokens
        self.temperature = temperature
        self.systemPrompt = systemPrompt
    }
    
    /// Configuración para resumir contenido educativo.
    public static let summarization = AISessionConfiguration(
        maxTokens: 512,
        temperature: 0.3,
        systemPrompt: "Eres un asistente educativo. Resume el contenido de forma clara y concisa para estudiantes."
    )
    
    /// Configuración para generar quizzes.
    public static let quizGeneration = AISessionConfiguration(
        maxTokens: 1024,
        temperature: 0.5,
        systemPrompt: "Genera preguntas de opción múltiple basadas en el contenido proporcionado. Formato JSON."
    )
    
    /// Configuración para explicar conceptos.
    public static let explanation = AISessionConfiguration(
        maxTokens: 1024,
        temperature: 0.6,
        systemPrompt: "Explica conceptos educativos de forma simple y con ejemplos prácticos."
    )
}

// MARK: - AI Response Types

/// Respuesta de AI con metadata.
public struct AIResponse: Sendable {
    public let text: String
    public let tokensUsed: Int
    public let processingTime: Duration
    public let wasStreamed: Bool
    
    public init(text: String, tokensUsed: Int, processingTime: Duration, wasStreamed: Bool = false) {
        self.text = text
        self.tokensUsed = tokensUsed
        self.processingTime = processingTime
        self.wasStreamed = wasStreamed
    }
}

/// Pregunta generada por AI.
public struct GeneratedQuestion: Codable, Sendable {
    public let question: String
    public let options: [String]
    public let correctIndex: Int
    public let explanation: String?
}

// MARK: - EduGoAI Manager

/// Manager de AI on-device usando Foundation Models (iOS 26+).
/// 
/// ## iOS 26 CASCADA - Características:
/// - **Modelo 3B on-device**: No requiere conexión a internet
/// - **Gratis**: Sin costos de API ni límites de uso  
/// - **Privado**: Los datos nunca salen del dispositivo
/// - **Rápido**: Neural Engine optimizado para Apple Silicon
/// - **Async nativo**: Integración total con Swift Concurrency
///
/// ## Requisitos de Hardware:
/// - iPhone: A14 Bionic o superior
/// - iPad: A14 Bionic, M1 o superior
/// - Mac: Apple Silicon (M1+)
public actor EduGoAIManager {
    private let logger = EduGoLogger(category: "AI")
    private var session: LanguageModelSession?
    private var configuration: AISessionConfiguration
    
    /// Verifica si el dispositivo soporta Foundation Models.
    public static var isSupported: Bool {
        LanguageModelSession.isSupported
    }
    
    public init(configuration: AISessionConfiguration = .init()) {
        self.configuration = configuration
    }
    
    // MARK: - Session Management
    
    /// Inicia una nueva sesión de AI con la configuración especificada.
    public func startSession(with config: AISessionConfiguration? = nil) async throws {
        let activeConfig = config ?? configuration
        self.configuration = activeConfig
        
        guard Self.isSupported else {
            logger.warning("Foundation Models no soportado en este dispositivo")
            throw AppError(code: .businessOperationNotAllowed, message: "AI no disponible en este dispositivo")
        }
        
        logger.info("Iniciando sesión de AI on-device...")
        
        // iOS 26: Crear sesión con configuración
        session = try await LanguageModelSession()
        
        // Aplicar system prompt si existe
        if let systemPrompt = activeConfig.systemPrompt {
            _ = try await session?.respond(to: systemPrompt)
        }
        
        logger.notice("Sesión de AI iniciada correctamente")
    }
    
    /// Termina la sesión actual.
    public func endSession() {
        session = nil
        logger.info("Sesión de AI terminada")
    }
    
    // MARK: - Text Generation
    
    /// Genera una respuesta de texto basada en el prompt.
    /// Usa @concurrent para no bloquear el main thread.
    @concurrent
    public func generate(prompt: String) async throws -> AIResponse {
        guard let session else {
            throw AppError(code: .businessOperationNotAllowed, message: "Sesión de AI no iniciada")
        }
        
        let startTime = ContinuousClock.now
        logger.debug("Generando respuesta para prompt de \(prompt.count) caracteres")
        
        let response = try await session.respond(to: prompt)
        
        let endTime = ContinuousClock.now
        let duration = endTime - startTime
        
        logger.debug("Respuesta generada en \(duration)")
        
        return AIResponse(
            text: response,
            tokensUsed: response.count / 4, // Estimación aproximada
            processingTime: duration,
            wasStreamed: false
        )
    }
    
    /// Genera respuesta con streaming (para UI en tiempo real).
    @concurrent
    public func generateStream(prompt: String) -> AsyncThrowingStream<String, Error> {
        AsyncThrowingStream { continuation in
            Task {
                guard let session = await self.session else {
                    continuation.finish(throwing: AppError(
                        code: .businessOperationNotAllowed,
                        message: "Sesión de AI no iniciada"
                    ))
                    return
                }
                
                do {
                    // iOS 26: Foundation Models soporta streaming nativo
                    for try await chunk in session.streamResponse(to: prompt) {
                        continuation.yield(chunk)
                    }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
        }
    }
    
    // MARK: - Educational Features
    
    /// Resume un texto educativo.
    public func summarize(_ text: String, maxSentences: Int = 3) async throws -> String {
        // Cambiar a configuración de resumen temporalmente
        try await startSession(with: .summarization)
        
        let prompt = """
        Resume el siguiente texto en máximo \(maxSentences) oraciones claras y concisas:
        
        \(text)
        """
        
        let response = try await generate(prompt: prompt)
        return response.text
    }
    
    /// Explica un concepto de forma simple.
    public func explain(concept: String, forGrade: String = "secundaria") async throws -> String {
        try await startSession(with: .explanation)
        
        let prompt = """
        Explica el siguiente concepto para un estudiante de \(forGrade):
        
        \(concept)
        
        Incluye:
        1. Definición simple
        2. Un ejemplo práctico
        3. Por qué es importante
        """
        
        let response = try await generate(prompt: prompt)
        return response.text
    }
    
    /// Genera preguntas de quiz basadas en contenido.
    public func generateQuiz(
        from content: String,
        numberOfQuestions: Int = 5
    ) async throws -> [GeneratedQuestion] {
        try await startSession(with: .quizGeneration)
        
        let prompt = """
        Genera \(numberOfQuestions) preguntas de opción múltiple basadas en este contenido:
        
        \(content)
        
        Responde SOLO con JSON válido en este formato exacto:
        [
            {
                "question": "...",
                "options": ["A", "B", "C", "D"],
                "correctIndex": 0,
                "explanation": "..."
            }
        ]
        """
        
        let response = try await generate(prompt: prompt)
        
        // Parsear JSON de la respuesta
        guard let jsonData = response.text.data(using: .utf8) else {
            throw AppError(code: .validationInvalidFormat, message: "Respuesta AI no es texto válido")
        }
        
        do {
            let questions = try JSONDecoder().decode([GeneratedQuestion].self, from: jsonData)
            return questions
        } catch {
            logger.error("Error parseando quiz JSON", error: error)
            throw AppError(code: .validationInvalidFormat, message: "AI no generó JSON válido")
        }
    }
    
    /// Verifica y corrige gramática/ortografía.
    public func checkGrammar(_ text: String) async throws -> String {
        try await startSession(with: AISessionConfiguration(
            maxTokens: text.count * 2,
            temperature: 0.1,
            systemPrompt: "Corrige errores gramaticales y ortográficos. Devuelve solo el texto corregido."
        ))
        
        let response = try await generate(prompt: text)
        return response.text
    }
    
    /// Traduce texto (útil para contenido multilingüe).
    public func translate(_ text: String, to language: String) async throws -> String {
        try await startSession(with: AISessionConfiguration(
            maxTokens: text.count * 2,
            temperature: 0.2,
            systemPrompt: "Traduce el texto al idioma indicado. Devuelve solo la traducción."
        ))
        
        let prompt = "Traduce al \(language): \(text)"
        let response = try await generate(prompt: prompt)
        return response.text
    }
}

// MARK: - Availability Check Extension

public extension EduGoAIManager {
    /// Información sobre la disponibilidad de AI en el dispositivo.
    struct AvailabilityInfo: Sendable {
        public let isSupported: Bool
        public let reason: String?
        
        public static let supported = AvailabilityInfo(isSupported: true, reason: nil)
        public static let notSupported = AvailabilityInfo(
            isSupported: false,
            reason: "Este dispositivo no tiene Neural Engine compatible con Foundation Models"
        )
    }
    
    /// Verifica la disponibilidad de AI y retorna información detallada.
    static func checkAvailability() -> AvailabilityInfo {
        if isSupported {
            return .supported
        } else {
            return .notSupported
        }
    }
}
```

---

## 🚀 CI/CD GitHub Actions

> ⚠️ **CASCADA iOS 26**: El CI/CD requiere **macOS 26** y **Xcode 26** para compilar targets iOS 26.
> Los simuladores deben ser iOS 26, watchOS 13, tvOS 26, visionOS 3.

### .github/workflows/swift.yml

```yaml
name: Swift CI

on:
  push:
    branches: [main, dev]
  pull_request:
    branches: [main]

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

env:
  # iOS 26 CASCADA: Xcode 26 es requerido para Swift 6.2 + iOS 26
  XCODE_VERSION: '26.0'

jobs:
  build-and-test:
    name: Build & Test
    # iOS 26 CASCADA: macOS 26 es requerido para Xcode 26
    runs-on: macos-26
    timeout-minutes: 30
    
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Setup Xcode 26
        uses: maxim-lobanov/setup-xcode@v1
        with:
          xcode-version: ${{ env.XCODE_VERSION }}
      
      - name: Verify Swift 6.2
        run: |
          swift --version
          # Verificar que sea Swift 6.2+
          swift --version | grep -q "Swift version 6.2" || (echo "Se requiere Swift 6.2" && exit 1)
      
      - name: Cache SPM
        uses: actions/cache@v4
        with:
          path: .build
          key: ${{ runner.os }}-spm-${{ hashFiles('Package.resolved') }}
          restore-keys: |
            ${{ runner.os }}-spm-
      
      - name: Build (Debug)
        run: swift build -v
      
      - name: Run Tests
        run: swift test -v --parallel
      
      - name: Build (Release)
        run: swift build -c release

  lint:
    name: SwiftLint
    runs-on: macos-26
    timeout-minutes: 10
    
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Install SwiftLint
        run: brew install swiftlint
      
      - name: Run SwiftLint
        run: swiftlint --strict

  build-platforms:
    name: Build ${{ matrix.platform }}
    runs-on: macos-26
    timeout-minutes: 20
    needs: build-and-test
    
    strategy:
      fail-fast: false
      matrix:
        platform:
          - iOS
          - macOS
          - watchOS
          - tvOS
          - visionOS
        include:
          # iOS 26 CASCADA: Todos los simuladores usan versiones iOS 26+
          - platform: iOS
            destination: 'platform=iOS Simulator,name=iPhone 17,OS=26.0'
          - platform: macOS
            destination: 'platform=macOS,arch=arm64'
          - platform: watchOS
            destination: 'platform=watchOS Simulator,name=Apple Watch Ultra 3,OS=13.0'
          - platform: tvOS
            destination: 'platform=tvOS Simulator,name=Apple TV 4K (3rd generation),OS=26.0'
          - platform: visionOS
            destination: 'platform=visionOS Simulator,name=Apple Vision Pro,OS=3.0'
    
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Setup Xcode 26
        uses: maxim-lobanov/setup-xcode@v1
        with:
          xcode-version: ${{ env.XCODE_VERSION }}
      
      - name: Build for ${{ matrix.platform }}
        run: |
          xcodebuild build \
            -scheme EduGoKit \
            -destination '${{ matrix.destination }}' \
            CODE_SIGNING_ALLOWED=NO \
            SWIFT_VERSION=6.2
```

---

## 📊 Grafo de Dependencias

> ⚠️ **iOS 26 CASCADA**: El módulo `EduGoAI` es EXCLUSIVO de iOS 26+ (Foundation Models framework)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         TIER 4 (Aplicación)                         │
├─────────────────────────────────────────────────────────────────────┤
│  EduGoAPI           EduGoAnalytics         EduGoAI [iOS 26+]       │
│    │                   │                      │                     │
│    ├── Auth           ├── Network            ├── Models             │
│    ├── Network        ├── Storage            ├── Logger             │
│    ├── Models         └── Logger             └── Common             │
│    └── Logger                                 ▲                     │
│                                               │                     │
│                                   FoundationModels (iOS 26 SDK)     │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          TIER 3 (Dominio)                           │
├─────────────────────────────────────────────────────────────────────┤
│  EduGoAuth                        EduGoRoles                        │
│    │                                 │                              │
│    ├── Roles                        ├── Models                      │
│    ├── Storage                      └── Common                      │
│    ├── Network                                                      │
│    ├── Models                                                       │
│    └── Logger                                                       │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     TIER 2 (Infraestructura)                        │
├─────────────────────────────────────────────────────────────────────┤
│  EduGoNetwork [iOS 26 @concurrent]    EduGoStorage                  │
│    │                                     │                          │
│    ├── Logger                           ├── Logger                  │
│    └── Common                           └── Common                  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          TIER 1 (Core)                              │
├─────────────────────────────────────────────────────────────────────┤
│  EduGoLogger [Xcode 26 async debug]   EduGoModels                   │
│    │                                     │                          │
│    └── Common                           └── Common                  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          TIER 0 (Base)                              │
├─────────────────────────────────────────────────────────────────────┤
│  EduGoCommon                                                        │
│    │                                                                │
│    └── (Sin dependencias - Foundation only)                         │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    iOS 26 CASCADA - Resumen                         │
├─────────────────────────────────────────────────────────────────────┤
│  Componente         │ Feature iOS 26                                │
│─────────────────────┼──────────────────────────────────────────────│
│  Package.swift      │ .defaultIsolation(MainActor.self)            │
│  EduGoNetwork       │ @concurrent, Duration, Network.framework     │
│  EduGoAI            │ FoundationModels (LLM 3B on-device)          │
│  EduGoLogger        │ Xcode 26 async/await debugging               │
│  CI/CD              │ macOS 26, Xcode 26, simuladores iOS 26       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## ✅ Checklist de Implementación

> ⚠️ **IMPORTANTE**: Todas las implementaciones DEBEN usar iOS 26+ / macOS 26+ / Xcode 26+
> ⚠️ **LLM**: Antes de cada fase, revisar si requiere configuración Xcode. Ver "Limitaciones CLI y Xcode"

### Pre-requisito: Documentación de Configuración Xcode

**ANTES de comenzar cada fase**, si requiere setup en Xcode:

- [ ] Crear documento `CONFIGURACION_XCODE_FASE[N].md`
- [ ] Incluir pasos numerados, claramente verificables
- [ ] Validar que todo compila correctamente

### Fase 0: Setup en Xcode (SI aplica)
- [ ] Crear documento `CONFIGURACION_XCODE_PROYECTO.md` con:
  - [ ] Cómo crear el repositorio SPM
  - [ ] Qué frameworks nativas requieren (Network, Security, etc.)
  - [ ] Cómo configurar targets para iOS 26+
  - [ ] Pasos de verificación (compilación exitosa)

### Fase 1: Fundamentos
- [ ] **PRE-REQUISITO**: Documentación Xcode lista
---

## 📝 Changelog

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 2.0.0 | Enero 2026 | iOS 26 + macOS 26 + Swift 6.2 + Foundation Models + LLM-ready |
| 1.1.0 | - | Corrección de roles (admin, teacher, student, guardian) |
| 1.0.0 | - | Versión inicial con iOS 18 |

---

**Última actualización**: Enero 2026  
**Versión**: 2.0.0 (iOS 26 CASCADA COMPLETA)  
**Mantenedor**: EduGo Team

> 🔒 **VERSIONES EN PIEDRA**: Este documento define versiones definitivas NO NEGOCIABLES.
> Cualquier cambio requiere actualización del Master Plan.
> 🤖 **LLM-READY**: Este documento está diseñado para ser ejecutado por LLMs vía CLI con documentación Xcode como pre-requisitos.
