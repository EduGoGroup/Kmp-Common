# 🤖 Guía para LLMs - Cómo Ejecutar este Proyecto

## 📍 Punto de Inicio

Cuando recibas este proyecto para desarrollar, sigue este flujo:

### Fase 1: Lectura Inicial (5-10 min)

```
1. LEE: 01-SWIFT-README.md
   ↓
   ✅ Entiendes objetivos
   ✅ Conoces reglas clave
   ✅ Ves estándares Swift
   ✅ CRITICAL: Entiendes limitaciones Xcode
   
2. LEE: ⚙️ Sección "Consideraciones LLM - Configuración Manual en Xcode"
   ↓
   ✅ CRÍTICO: No todos los cambios se hacen por CLI
   ✅ Algunos requieren documentación Xcode PRIMERO
   ✅ NO comenzar código sin documentación Xcode lista
```

### Fase 2: Entender Limitaciones Xcode (5 min)

```
IMPORTANTES:
✅ Puedes crear archivos Swift, editar Package.swift, escribir tests
❌ NO puedes: Crear targets, añadir frameworks, configurar build settings

PROTOCOLO:
Si una tarea requiere configuración Xcode:
  1. PRIMERO: Crear documento CONFIGURACION_XCODE_[MODULO].md
  2. Pasos detallados y numerados
  3. Cómo verificar que está correcto
  4. LUEGO: Comenzar a codificar
```

### Fase 3: Leer Plan Detallado (10-15 min)

```
2. LEE: 01-SWIFT-SETUP-PLAN.md
   ↓
   Sección 1: ⚠️ Limitaciones CLI y Xcode (MUY IMPORTANTE)
   ↓
   Sección 2+: Stack, Módulos, etc.
   ↓
   Final: ✅ Checklist con PRE-REQUISITOS explícitos
```

---

## 🎯 Cuando Comiences a Trabajar

### Paso 1: Verificar PRE-REQUISITOS de la Tarea

Antes de CUALQUIER tarea, pregúntate:

```
¿Esta tarea requiere configuración Xcode?
  ↓
  SI → Crear CONFIGURACION_XCODE_[MODULO].md PRIMERO
  NO  → Proceder a codificar
```

### Paso 2: Si es Configuración Xcode

**DEBES CREAR**: `CONFIGURACION_XCODE_[MODULO].md` con:

```markdown
# CONFIGURACION_XCODE_EduGoNetwork.md

## Paso 1: Crear Target
- Instrucciones numeradas
- Qué hacer exactamente
- Screenshots si es necesario

## Paso 2: Añadir Frameworks
- Network.framework
- ...

## Verificación
Ejecutar en CLI:
\`\`\`bash
xcodebuild build -scheme EduGoNetwork
\`\`\`
Debe compilar sin errores.
```

**NO CONTINÚES** hasta que este documento esté listo.

### Paso 3: Si es Codificación

```
✅ Crear archivos Swift
✅ Implementar módulo siguiendo estándares
✅ Escribir tests con stubs
✅ Verificar build exitoso
✅ Commit con formato: [TIER-X] Módulo: Descripción
```

---

## 📋 Checklist por Tarea

Cuando recibas una tarea, verifica:

```
□ ¿Tengo documentación de Xcode si es necesaria?
□ ¿Sé exactamente qué debo implementar?
□ ¿Entiendo los estándares Swift a aplicar?
□ ¿Sé qué tier depende de cuáles otros?
□ ¿Puedo crear tests con stubs ANTES de implementar?
□ ¿Compilará correctamente en todas las plataformas?
```

Si alguno es NO, **pide aclaraciones** antes de empezar.

---

## 🏗️ Patrón de Implementación

**SIEMPRE** sigue este flujo:

### 1. Definir Protocol (Interfaz)

```swift
protocol UserRepositoryProtocol {
    func fetchUser(id: UUID) async throws -> User
}
```

### 2. Crear Stub (Para Testing)

```swift
class UserRepositoryStub: UserRepositoryProtocol {
    var mockUser: User?
    var mockError: Error?
    
    func fetchUser(id: UUID) async throws -> User {
        if let error = mockError { throw error }
        return mockUser ?? User.stub()
    }
}
```

### 3. Escribir Tests

```swift
func testLoginWithValidCredentials() async throws {
    // Arrange
    repositoryStub.mockUser = User.stub()
    
    // Act
    let result = try await sut.login(...)
    
    // Assert
    XCTAssertNotNil(result.token)
}
```

### 4. Implementar Real

```swift
class UserRepository: UserRepositoryProtocol {
    // Implementación usando Network.framework
}
```

---

## ⚠️ Cosas que NUNCA Hagas

```
❌ NO: Comenzar código sin documentación Xcode lista (si requiere)
❌ NO: Usar dependencias externas (solo APIs nativas)
❌ NO: Implementar sin tests antes
❌ NO: Ignorar las reglas de TIER (respeta el orden)
❌ NO: Crear stubs DESPUÉS de la implementación
❌ NO: Olvidar @MainActor/@concurrent en concurrencia
❌ NO: Omitir Sendable en tipos compartidos entre hilos
❌ NO: Saltarse DocC comments en APIs públicas
```

---

## ✅ Definición de "Done" para Cada Tarea

Una tarea está completa cuando:

```
□ Código implementado 100%
□ Tests unitarios ≥ 80% cobertura
□ Tests de integración (si aplica)
□ DocC comments en APIs públicas
□ SwiftLint pasa sin warnings
□ Build exitoso en 5 plataformas (iOS, macOS, watchOS, tvOS, visionOS)
□ CI/CD verde
□ Commit con formato [TIER-X]
```

---

## 🚨 Si Necesitas Ayuda

Cuando te atranques:

1. **Revisa README** → Estándares y reglas
2. **Revisa SETUP-PLAN** → Detalles técnicos y ejemplos
3. **Revisa RESUMEN_CAMBIOS.md** → Qué cambió y por qué
4. **Busca similares** → Mira otro módulo similar
5. **Pregunta** → Especifica qué no entiendes

---

## 📞 Referencias Rápidas

| Pregunta | Respuesta |
|----------|-----------|
| ¿Qué versión de iOS? | iOS 26+ (NO retrocompatibilidad) |
| ¿Qué versión de Swift? | Swift 6.2 |
| ¿Qué versión de Xcode? | Xcode 26+ |
| ¿Dependencias? | CERO externas, solo APIs nativas |
| ¿Testing approach? | Test-First con stubs |
| ¿Orden implementación? | TIER 0 → 1 → 2 → 3 → 4 |
| ¿Roles del sistema? | admin, teacher, student, guardian |
| ¿Para Xcode? | Crear CONFIGURACION_XCODE_[X].md primero |
| ¿Formato commit? | [TIER-X] Módulo: Descripción |
| ¿Tests mínimos? | 80-85% cobertura según tier |

---

## 🎓 Ejemplo: Implementar EduGoCommon (TIER 0)

```
1. Leo README → Entiendo proyecto
2. Leo SETUP-PLAN → Veo código ejemplo de EduGoCommon
3. Reviso ⚙️ Consideraciones LLM → No requiere Xcode (SPM puro)
4. Creo archivos Swift en Sources/EduGoCommon/
5. Escribo tests en Tests/EduGoCommonTests/
6. Verifico: xcodebuild build (debe compilar)
7. Verifico: xcodebuild test (tests pasan)
8. Commit: [TIER-0] EduGoCommon: ErrorCodes y AppError
```

**Simple**: No hay configuración Xcode, puro código Swift.

---

## 📝 Ejemplo: Implementar EduGoNetwork (TIER 2)

```
1. Leo README → Entiendo proyecto
2. Reviso: Usa Network.framework (nativa iOS 26)
3. Reviso ⚙️: ¿Requiere Xcode? SÍ (framework linking)
   → Creo: CONFIGURACION_XCODE_NETWORK.md
   → Documento con pasos de Xcode
   → Paso de verificación
4. LUEGO: Creo código Swift
5. Tests con stubs
6. Build exitoso
7. Commit: [TIER-2] EduGoNetwork: HTTP client async
```

**Paso crítico**: Documentación Xcode PRIMERO.

---

**Última actualización**: 20 enero 2026  
**Para**: LLMs ejecutando este proyecto  
**Status**: ✅ Listo para comenzar
