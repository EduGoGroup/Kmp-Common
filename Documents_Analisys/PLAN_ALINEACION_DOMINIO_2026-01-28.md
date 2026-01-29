# Plan de Alineacion de Modelos de Dominio con Backend

> **Fecha**: 2026-01-28  
> **Proyecto**: proj-1768943256031988000 (EduGo Apple Modules)  
> **Sprint Objetivo**: Sprint de Alineacion de Dominio  
> **Duracion Estimada**: 1 Sprint

---

## 1. Contexto del Problema

### 1.1 Situacion Actual

Los modelos de dominio implementados en los sprints 1-3 **no estan alineados** con las APIs del backend:

| Entidad | Estado Actual | Estado Esperado |
|---------|--------------|-----------------|
| User | `name` unico | `firstName` + `lastName` |
| User.roleIDs | `Set<UUID>` | Sistema de Memberships |
| Document | Documentos texto | Material (archivos PDF) |
| School | No existe | Entidad requerida |
| AcademicUnit | No existe | Entidad requerida |
| Membership | No existe | Entidad requerida |

### 1.2 APIs Backend de Referencia

**edu-admin API** (`swagger.json`):
- `/v1/users` - CRUD usuarios con `first_name`, `last_name`
- `/v1/schools` - Gestion de escuelas
- `/v1/units` - Unidades academicas
- `/v1/memberships` - Asignacion de roles contextuales
- `/v1/auth/*` - Autenticacion JWT

**edu-mobile API** (`swagger.json`):
- `/v1/materials` - Materiales educativos (archivos)
- `/v1/progress` - Progreso de estudiantes
- `/v1/attempts` - Intentos de evaluacion

---

## 2. Objetivo del Sprint

Refactorizar y alinear los modelos de dominio de TIER-1 (Models) para que:

1. **User** tenga estructura compatible con backend (`firstName`, `lastName`)
2. **Eliminar roleIDs** del modelo User (los roles son contextuales via Memberships)
3. **Crear Material** como entidad para archivos educativos
4. **Crear entidades core faltantes**: School, AcademicUnit, Membership
5. **Actualizar mappers y DTOs** para las nuevas estructuras
6. **Actualizar modelos SwiftData** de persistencia local
7. **Actualizar tests** para las nuevas estructuras

---

## 3. Historia de Usuario Principal

### Historia: Alinear Modelos de Dominio con APIs del Backend

**Como** desarrollador del equipo EduGo,  
**Quiero** que los modelos de dominio Swift esten alineados con las APIs del backend,  
**Para** garantizar serializacion/deserializacion correcta y evitar bugs de integracion.

### Criterios de Aceptacion

1. [ ] `User` tiene propiedades `firstName` y `lastName` (no `name`)
2. [ ] `User` NO tiene `roleIDs` - los roles se manejan via Membership
3. [ ] `User` tiene `createdAt` y `updatedAt`
4. [ ] Existe entidad `Material` con campos del swagger (status, fileURL, etc.)
5. [ ] Existe entidad `School` con campos del swagger
6. [ ] Existe entidad `AcademicUnit` con campos del swagger
7. [ ] Existe entidad `Membership` con role contextual
8. [ ] Mappers `UserMapper`, `MaterialMapper` funcionan correctamente
9. [ ] DTOs alineados con swagger del backend
10. [ ] Modelos SwiftData actualizados (`UserModel`, etc.)
11. [ ] Tests unitarios pasan al 80%+ cobertura
12. [ ] `swift build` compila sin warnings

---

## 4. Desglose de Tasks (Termino Medio)

### Task 1: Refactorizar User y crear entidades relacionadas (School, AcademicUnit)

**Descripcion**: Modificar `User.swift` para separar nombre, eliminar roleIDs, y crear entidades `School` y `AcademicUnit` que son necesarias para el contexto del usuario.

**Archivos a modificar/crear**:
- `Sources/Models/Domain/User.swift` - MODIFICAR
- `Sources/Models/Domain/School.swift` - CREAR
- `Sources/Models/Domain/AcademicUnit.swift` - CREAR
- `Sources/Models/DTOs/UserDTO.swift` - MODIFICAR
- `Sources/Models/DTOs/SchoolDTO.swift` - CREAR
- `Sources/Models/DTOs/AcademicUnitDTO.swift` - CREAR

**Estructura User Objetivo**:
```swift
public struct User: Sendable, Equatable, Identifiable, Codable, Hashable {
    public let id: UUID
    public let firstName: String
    public let lastName: String
    public let email: String
    public let isActive: Bool
    public let createdAt: Date
    public let updatedAt: Date
    
    public var fullName: String { "\(firstName) \(lastName)" }
}
```

**Estructura School Objetivo**:
```swift
public struct School: Sendable, Equatable, Identifiable, Codable, Hashable {
    public let id: UUID
    public let name: String
    public let code: String
    public let address: String?
    public let city: String?
    public let country: String
    public let contactEmail: String?
    public let contactPhone: String?
    public let subscriptionTier: SubscriptionTier  // free, basic, premium
    public let maxStudents: Int
    public let maxTeachers: Int
    public let isActive: Bool
    public let metadata: [String: Any]?
    public let createdAt: Date
    public let updatedAt: Date
}
```

**Estructura AcademicUnit Objetivo**:
```swift
public struct AcademicUnit: Sendable, Equatable, Identifiable, Codable, Hashable {
    public let id: UUID
    public let schoolID: UUID
    public let parentUnitID: UUID?
    public let type: UnitType  // grade, section, club, department
    public let displayName: String
    public let code: String?
    public let description: String?
    public let metadata: [String: Any]?
    public let createdAt: Date
    public let updatedAt: Date
    public let deletedAt: Date?
}
```

**Estimacion**: 4-5 horas

---

### Task 2: Crear Membership y MembershipRole para roles contextuales

**Descripcion**: Implementar el sistema de memberships que conecta usuarios con unidades academicas y asigna roles de forma contextual (no global).

**Archivos a crear**:
- `Sources/Models/Domain/Membership.swift` - CREAR
- `Sources/Models/Domain/MembershipRole.swift` - CREAR
- `Sources/Models/DTOs/MembershipDTO.swift` - CREAR
- `Sources/Models/Mappers/MembershipMapper.swift` - CREAR
- `Sources/Models/Protocols/MembershipRepositoryProtocol.swift` - CREAR

**Estructura Membership Objetivo**:
```swift
public struct Membership: Sendable, Equatable, Identifiable, Codable, Hashable {
    public let id: UUID
    public let userID: UUID
    public let unitID: UUID
    public let role: MembershipRole
    public let isActive: Bool
    public let enrolledAt: Date
    public let withdrawnAt: Date?
    public let validFrom: Date?
    public let validUntil: Date?
    public let createdAt: Date
    public let updatedAt: Date
}
```

**Estructura MembershipRole Objetivo**:
```swift
public enum MembershipRole: String, Codable, Sendable, CaseIterable {
    case owner = "owner"
    case teacher = "teacher"
    case assistant = "assistant"
    case student = "student"
    case guardian = "guardian"
}
```

**Estimacion**: 3-4 horas

---

### Task 3: Crear Material para archivos educativos (reemplaza Document conceptualmente)

**Descripcion**: Crear entidad `Material` que representa archivos educativos (PDFs, etc.) con todos los campos del swagger del backend mobile.

**Archivos a crear/modificar**:
- `Sources/Models/Domain/Material.swift` - CREAR
- `Sources/Models/Domain/MaterialStatus.swift` - CREAR (enum)
- `Sources/Models/DTOs/MaterialDTO.swift` - CREAR
- `Sources/Models/Mappers/MaterialMapper.swift` - CREAR
- `Sources/Models/Protocols/MaterialRepositoryProtocol.swift` - CREAR
- `Sources/Models/Validation/MaterialValidator.swift` - CREAR

**Estructura Material Objetivo**:
```swift
public struct Material: Sendable, Equatable, Identifiable, Codable, Hashable {
    public let id: UUID
    public let title: String
    public let description: String?
    public let subject: String?
    public let grade: String?
    public let status: MaterialStatus
    public let fileURL: URL?
    public let fileType: String?
    public let fileSizeBytes: Int?
    public let isPublic: Bool
    public let schoolID: UUID
    public let academicUnitID: UUID?
    public let uploadedByTeacherID: UUID
    public let processingStartedAt: Date?
    public let processingCompletedAt: Date?
    public let createdAt: Date
    public let updatedAt: Date
    public let deletedAt: Date?
}

public enum MaterialStatus: String, Codable, Sendable, CaseIterable {
    case uploaded = "uploaded"
    case processing = "processing"
    case ready = "ready"
    case failed = "failed"
}
```

**Nota**: `Document` existente se mantiene para documentos de texto internos; `Material` es para archivos educativos del backend.

**Estimacion**: 4-5 horas

---

### Task 4: Actualizar Mappers existentes (User, Document) y crear nuevos

**Descripcion**: Actualizar `UserMapper` para la nueva estructura y crear mappers para las nuevas entidades.

**Archivos a modificar/crear**:
- `Sources/Models/Mappers/UserMapper.swift` - MODIFICAR
- `Sources/Models/Mappers/SchoolMapper.swift` - CREAR
- `Sources/Models/Mappers/AcademicUnitMapper.swift` - CREAR

**UserMapper actualizado**:
```swift
public struct UserMapper: MapperProtocol {
    public static func toDomain(_ dto: UserDTO) throws -> User {
        try User(
            id: dto.id,
            firstName: dto.firstName,
            lastName: dto.lastName,
            email: dto.email,
            isActive: dto.isActive ?? true,
            createdAt: dto.createdAt ?? Date(),
            updatedAt: dto.updatedAt ?? Date()
        )
    }
    
    public static func toDTO(_ domain: User) -> UserDTO {
        UserDTO(
            id: domain.id,
            firstName: domain.firstName,
            lastName: domain.lastName,
            email: domain.email,
            isActive: domain.isActive,
            createdAt: domain.createdAt,
            updatedAt: domain.updatedAt
        )
    }
}
```

**Estimacion**: 3-4 horas

---

### Task 5: Actualizar modelos SwiftData de LocalPersistence

**Descripcion**: Actualizar `UserModel` y crear modelos SwiftData para las nuevas entidades de persistencia local.

**Archivos a modificar/crear**:
- `Sources/LocalPersistence/Models/UserModel.swift` - MODIFICAR
- `Sources/LocalPersistence/Models/SchoolModel.swift` - CREAR
- `Sources/LocalPersistence/Models/MaterialModel.swift` - CREAR
- `Sources/LocalPersistence/Models/MembershipModel.swift` - CREAR
- `Sources/LocalPersistence/Mappers/UserPersistenceMapper.swift` - MODIFICAR
- `Sources/LocalPersistence/Mappers/MaterialPersistenceMapper.swift` - CREAR
- `Sources/LocalPersistence/Repositories/LocalMaterialRepository.swift` - CREAR

**UserModel actualizado**:
```swift
@Model
public final class UserModel {
    @Attribute(.unique) public var id: UUID
    public var firstName: String
    public var lastName: String
    public var email: String
    public var isActive: Bool
    public var createdAt: Date
    public var updatedAt: Date
}
```

**Estimacion**: 4-5 horas

---

### Task 6: Actualizar tests y crear tests para nuevas entidades

**Descripcion**: Actualizar todos los tests afectados por los cambios y crear tests para las nuevas entidades.

**Archivos a modificar/crear**:
- `Tests/ModelsTests/Domain/UserTests.swift` - MODIFICAR
- `Tests/ModelsTests/Domain/SchoolTests.swift` - CREAR
- `Tests/ModelsTests/Domain/AcademicUnitTests.swift` - CREAR
- `Tests/ModelsTests/Domain/MembershipTests.swift` - CREAR
- `Tests/ModelsTests/Domain/MaterialTests.swift` - CREAR
- `Tests/ModelsTests/Mappers/UserMapperTests.swift` - MODIFICAR
- `Tests/ModelsTests/Mappers/MaterialMapperTests.swift` - CREAR
- `Tests/LocalPersistenceTests/Repositories/LocalUserRepositoryTests.swift` - MODIFICAR
- `Tests/LocalPersistenceTests/Mappers/UserPersistenceMapperTests.swift` - MODIFICAR

**Cobertura objetivo**: 80%+ para cada entidad nueva.

**Estimacion**: 4-5 horas

---

## 5. Resumen de Tasks

| # | Task | Esfuerzo | Complejidad |
|---|------|----------|-------------|
| 1 | Refactorizar User + School + AcademicUnit | 4-5h | High |
| 2 | Crear Membership y MembershipRole | 3-4h | Medium |
| 3 | Crear Material para archivos educativos | 4-5h | High |
| 4 | Actualizar/crear Mappers | 3-4h | Medium |
| 5 | Actualizar modelos SwiftData | 4-5h | High |
| 6 | Actualizar/crear Tests | 4-5h | Medium |

**Total Estimado**: 22-28 horas

---

## 6. Dependencias entre Tasks

```
Task 1 (User, School, AcademicUnit)
    |
    v
Task 2 (Membership) ---> Depende de Task 1
    |
    v
Task 3 (Material) -----> Independiente
    |
    v
Task 4 (Mappers) ------> Depende de Tasks 1, 2, 3
    |
    v
Task 5 (SwiftData) ----> Depende de Tasks 1, 2, 3
    |
    v
Task 6 (Tests) --------> Depende de todas las anteriores
```

---

## 7. Archivos de Referencia del Backend

Para implementar correctamente, consultar:

- `Documents_Analisys/edu-admin/swagger.json` - Estructuras de User, School, Unit, Membership
- `Documents_Analisys/edu-mobile/swagger.json` - Estructuras de Material, Progress, Attempt
- `Documents_Analisys/INFORME_DESVIACIONES_2026-01-28.md` - Detalle de desviaciones

---

## 8. Criterios de Exito del Sprint

1. [ ] Todas las tasks completadas
2. [ ] `swift build` sin warnings
3. [ ] `swift test` pasa al 100%
4. [ ] Cobertura de tests >= 80%
5. [ ] Code review aprobado
6. [ ] Entidades alineadas con swagger del backend

---

**Documento generado para**: Sprint de Alineacion de Dominio  
**Uso**: Input para comando `/021-deep-analysis-create-sprint`
