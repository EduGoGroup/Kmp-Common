# Informe de Desviaciones - EduGo Apple Modules

> **Fecha**: 2026-01-28  
> **Proyecto**: proj-1768943256031988000 (EduGo Apple Modules)  
> **Sprints Analizados**: Sprint 1, Sprint 2, Sprint 3 (parcial)  
> **Estado**: REQUIERE ADAPTACIONES

---

## Resumen Ejecutivo

Tras analizar el codigo implementado versus la documentacion de especificaciones (`01-SWIFT-SETUP-PLAN.md`, `GUIA_LLM.md`) y las APIs del backend (`edu-admin/swagger.json`, `edu-mobile/swagger.json`), se identifican **desviaciones significativas** que requieren adaptacion para garantizar la alineacion con el backend y las especificaciones originales.

### Veredicto General

| Area | Estado | Impacto |
|------|--------|---------|
| Arquitectura de TIERs | ALINEADO | Bajo |
| Modelo User | DESVIACION CRITICA | Alto |
| Modelo Document | PARCIALMENTE ALINEADO | Medio |
| Sistema de Roles | DESVIACION CRITICA | Alto |
| Persistencia Local | ALINEADO | Bajo |
| Conformidad APIs Backend | DESVIACION CRITICA | Alto |

---

## 1. DESVIACIONES CRITICAS

### 1.1 Modelo User - Campos Faltantes vs API Backend

**Archivo**: `TIER-1-Core/Models/Sources/Models/Domain/User.swift`

**Implementacion Actual**:
```swift
public struct User {
    public let id: UUID
    public let name: String        // Campo unificado
    public let email: String
    public let isActive: Bool
    public let roleIDs: Set<UUID>  // Referencias a roles por UUID
}
```

**Esperado segun API Backend** (`edu-admin/swagger.json` - `CreateUserRequest` y `UserResponse`):
```json
{
    "email": "string",
    "first_name": "string",    // FALTANTE
    "last_name": "string",     // FALTANTE
    "password": "string",      // Para DTOs
    "role": "string"           // Role como STRING, no UUID
}
```

**Desviaciones Identificadas**:
1. `first_name` y `last_name` estan fusionados en `name` - el backend los maneja separados
2. `role` es un string ("admin", "teacher", "student", "guardian") NO un `Set<UUID>`
3. Faltan campos adicionales del backend como `created_at`, `updated_at`
4. El backend no usa `roleIDs` como UUIDs sino el sistema de `memberships`

**Impacto**: Los mappers no podran serializar/deserializar correctamente con el backend.

---

### 1.2 Sistema de Roles - Arquitectura Diferente

**Archivo**: `TIER-3-Domain/Roles/Sources/Roles/SystemRole.swift`

**Implementacion Actual**: Los roles estan correctamente definidos con valores alineados al backend:
```swift
public enum SystemRole: String, Codable, Sendable, CaseIterable {
    case admin = "admin"
    case teacher = "teacher"
    case student = "student"
    case guardian = "guardian"
}
```

**PROBLEMA**: El modelo `User` usa `roleIDs: Set<UUID>` pero deberia usar `role: SystemRole` directamente.

**API Backend - Memberships System**:
El backend usa un sistema de **memberships** para asignar roles:
```json
// MembershipResponse
{
    "id": "string",
    "user_id": "string",
    "unit_id": "string",
    "role": "string",         // "owner", "teacher", "assistant", "student", "guardian"
    "is_active": "boolean",
    "enrolled_at": "string",
    "withdrawn_at": "string"
}
```

**Desviacion**: Los roles son **contextuales** (por unidad academica), no globales en el usuario.

---

### 1.3 Document vs Material - Entidades Diferentes

**Implementacion Actual** (`Document.swift`):
```swift
public struct Document {
    public let id: UUID
    public let title: String
    public let content: String
    public let type: DocumentType     // lesson, assignment, quiz, syllabus, resource, announcement
    public let state: DocumentState   // draft, published, archived
    public let metadata: DocumentMetadata
    public let ownerID: UUID
    public let collaboratorIDs: Set<UUID>
}
```

**API Backend - Material** (`edu-mobile/swagger.json - MaterialResponse`):
```json
{
    "id": "string",
    "title": "string",
    "description": "string",           // NO "content"
    "subject": "string",               // NUEVO campo
    "grade": "string",                 // NUEVO campo
    "status": "string",                // "uploaded", "processing", "ready", "failed"
    "file_url": "string",              // Material es para archivos
    "file_type": "string",
    "file_size_bytes": "integer",
    "is_public": "boolean",
    "school_id": "string",
    "academic_unit_id": "string",
    "uploaded_by_teacher_id": "string",
    "processing_started_at": "string",
    "processing_completed_at": "string",
    "created_at": "string",
    "updated_at": "string",
    "deleted_at": "string"
}
```

**Desviaciones Identificadas**:
1. `content` deberia ser `description`
2. Faltan campos criticos: `subject`, `grade`, `file_url`, `school_id`, `academic_unit_id`
3. `status` tiene valores diferentes a `DocumentState`
4. El modelo implementado es para documentos de texto; el backend es para **archivos educativos** (PDFs principalmente)
5. Sistema de versionado diferente (backend tiene `versions` endpoint)

---

## 2. DESVIACIONES MEDIAS

### 2.1 Entidades Faltantes del Backend

Segun las APIs, faltan implementar estas entidades de dominio:

| Entidad | API Backend | Estado |
|---------|-------------|--------|
| `School` | `/v1/schools` | NO IMPLEMENTADA |
| `AcademicUnit` | `/v1/units` | NO IMPLEMENTADA |
| `Membership` | `/v1/memberships` | NO IMPLEMENTADA |
| `Subject` | `/v1/subjects` | NO IMPLEMENTADA |
| `GuardianRelation` | `/v1/guardian-relations` | NO IMPLEMENTADA |
| `Assessment` | `/v1/materials/{id}/assessment` | NO IMPLEMENTADA |
| `Attempt` | `/v1/attempts/{id}/results` | NO IMPLEMENTADA |
| `Progress` | `/v1/progress` | NO IMPLEMENTADA |

### 2.2 AuthTokens - Alineacion Parcial

**Esperado segun API** (`LoginResponse`):
```json
{
    "access_token": "string",
    "refresh_token": "string",
    "expires_in": "integer",
    "token_type": "string",
    "user": {...}               // Usuario completo en login
}
```

**Recomendacion**: Verificar que `AuthTokens` incluya `token_type` y considere el campo `user` en la respuesta de login.

---

## 3. ASPECTOS ALINEADOS (Positivos)

### 3.1 Arquitectura de TIERs
La estructura de capas (TIER-0 a TIER-4) esta correctamente implementada segun el plan.

### 3.2 Swift 6.2 Strict Concurrency
Todos los modulos usan `StrictConcurrency=complete` correctamente.

### 3.3 Actors para Thread-Safety
Los repositorios locales (`LocalUserRepository`, `LocalDocumentRepository`) estan implementados como actors.

### 3.4 Sistema de Errores
`DomainError`, `RepositoryError`, `UseCaseError` estan bien definidos.

### 3.5 SwiftData Integration
`PersistenceContainerProvider` y los modelos `@Model` estan bien implementados.

### 3.6 Logger System
El sistema de logging con `os.Logger` es robusto y completo.

---

## 4. PLAN DE ADAPTACION RECOMENDADO

### Fase 1: Modelos de Dominio (PRIORIDAD ALTA)

#### 4.1.1 Adaptar User
```swift
// CAMBIOS REQUERIDOS
public struct User: Sendable, Equatable, Identifiable, Codable, Hashable {
    public let id: UUID
    public let firstName: String          // Separar name
    public let lastName: String           // Separar name
    public let email: String
    public let isActive: Bool
    public let createdAt: Date            // Agregar
    public let updatedAt: Date            // Agregar
    
    // fullName como computed property
    public var fullName: String { "\(firstName) \(lastName)" }
}
```

#### 4.1.2 Crear Material (renombrar Document)
```swift
public struct Material: Sendable, Equatable, Identifiable, Codable, Hashable {
    public let id: UUID
    public let title: String
    public let description: String
    public let subject: String?
    public let grade: String?
    public let status: MaterialStatus       // uploaded, processing, ready, failed
    public let fileURL: URL?
    public let fileType: String?
    public let fileSizeBytes: Int?
    public let isPublic: Bool
    public let schoolID: UUID
    public let academicUnitID: UUID?
    public let uploadedByTeacherID: UUID
    public let createdAt: Date
    public let updatedAt: Date
    public let deletedAt: Date?
}
```

#### 4.1.3 Crear Entidades Nuevas
- `School`
- `AcademicUnit`
- `Membership`
- `Subject`
- `GuardianRelation`

### Fase 2: Sistema de Memberships (PRIORIDAD ALTA)

Implementar el patron de memberships donde:
- Un `User` puede tener multiples `Membership`
- Cada `Membership` asocia un usuario a una `AcademicUnit` con un `role`
- Los roles son contextuales, no globales

```swift
public struct Membership: Sendable, Equatable, Identifiable, Codable {
    public let id: UUID
    public let userID: UUID
    public let unitID: UUID
    public let role: MembershipRole        // owner, teacher, assistant, student, guardian
    public let isActive: Bool
    public let enrolledAt: Date
    public let withdrawnAt: Date?
    public let validFrom: Date?
    public let validUntil: Date?
}
```

### Fase 3: DTOs y Mappers (PRIORIDAD MEDIA)

Actualizar los DTOs para que coincidan exactamente con las estructuras del backend swagger.

### Fase 4: Repositorios Remotos (PRIORIDAD MEDIA)

Implementar repositorios que consuman las APIs:
- `RemoteUserRepository`
- `RemoteMaterialRepository`
- `RemoteMembershipRepository`
- etc.

---

## 5. IMPACTO EN SPRINTS EXISTENTES

### Sprint 1: TIER 0 Foundation - SIN CAMBIOS
La base arquitectonica esta correcta.

### Sprint 2: TIER 1 Domain Layer - REQUIERE REFACTOR
- Adaptar `User` con `firstName`/`lastName`
- Renombrar/Adaptar `Document` a `Material`
- Crear entidades faltantes

### Sprint 3: TIER 1 Data Layer - IMPACTO PARCIAL
- Los repositorios locales necesitan actualizarse para las nuevas entidades
- `LocalUserRepository` y `LocalDocumentRepository` requieren ajustes

### Sprints 4+: PLANIFICAR
Los sprints futuros deben considerar:
- Sistema de Memberships
- Entidades de School/AcademicUnit
- Assessment/Progress

---

## 6. ACCIONES INMEDIATAS REQUERIDAS

### Alta Prioridad
1. [ ] Refactorizar `User` para separar `firstName`/`lastName`
2. [ ] Eliminar `roleIDs: Set<UUID>` del modelo `User`
3. [ ] Crear entidad `Material` alineada con el backend
4. [ ] Crear entidad `Membership` para sistema de roles contextuales
5. [ ] Actualizar mappers y DTOs

### Media Prioridad
6. [ ] Crear entidades `School`, `AcademicUnit`, `Subject`
7. [ ] Actualizar `UserModel` de SwiftData
8. [ ] Crear `MaterialModel` para SwiftData
9. [ ] Actualizar tests para nuevas estructuras

### Baja Prioridad
10. [ ] Crear entidades de Assessment/Progress
11. [ ] Implementar repositorios remotos
12. [ ] Documentar cambios

---

## 7. CONCLUSION

El proyecto tiene una **base arquitectonica solida** (TIERs, actors, concurrencia, errores tipados), pero los **modelos de dominio no estan alineados** con las APIs del backend. Las desviaciones principales son:

1. **User**: Campos diferentes (`name` vs `firstName`/`lastName`)
2. **Roles**: Sistema de memberships contextuales vs roles globales
3. **Document/Material**: Entidad conceptualmente diferente
4. **Entidades faltantes**: School, AcademicUnit, Membership, etc.

**Recomendacion**: Priorizar la correccion de las entidades de dominio antes de continuar con sprints adicionales, ya que afectaran toda la integracion con el backend.

---

**Autor**: Analisis automatizado  
**Revision**: Pendiente equipo tecnico
