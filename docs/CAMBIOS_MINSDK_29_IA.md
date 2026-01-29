# Cambios Requeridos: Migración a minSdk 29 (Android 10) con Soporte IA

**Fecha**: 2026-01-29  
**Última actualización**: 2026-01-29  
**Razón**: Actualizar proyecto para usar Android 10+ como base, habilitando APIs modernas y preparación para IA on-device.  
**Decisión**: Mantener minSdk=29 para máximo alcance (~90%) con estrategia híbrida de IA.

---

## Resumen del Cambio

| Antes | Después | Razón |
|-------|---------|-------|
| minSdk=24 (Android 7) | **minSdk=29 (Android 10)** | Scoped Storage, NNAPI 1.2, TLS 1.3, Dark Theme nativo |
| Sin consideración IA | **Estrategia híbrida de IA** | ML Kit para todos + Gemini Nano para Android 14+ |

## Justificación de minSdk 29 (vs 33 o 34)

| minSdk | Alcance | IA Avanzada (14+) | Decisión |
|--------|---------|-------------------|----------|
| 29 | **~90%** | 37% usuarios | **ELEGIDO** - Máximo alcance para app educativa |
| 33 | ~52% | 71% usuarios | Descartado - Pierde estudiantes con dispositivos económicos |
| 34 | ~37% | 100% usuarios | Descartado - Muy restrictivo para contexto educativo |

### Estrategia de IA Híbrida

```kotlin
// Patrón expect/actual para detección de IA
// En commonMain
expect object AICapabilities {
    fun isGeminiNanoAvailable(): Boolean
    fun isMLKitAvailable(): Boolean
    fun getRecommendedAIProvider(): AIProvider
}

enum class AIProvider {
    GEMINI_NANO,  // Android 14+ con hardware compatible
    ML_KIT,       // Android 10+ (fallback)
    NONE
}

// En androidMain
actual object AICapabilities {
    actual fun isGeminiNanoAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE // API 34
        // TODO: Agregar check de hardware cuando Gemini Nano esté disponible
    }
    
    actual fun isMLKitAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q // API 29
    }
    
    actual fun getRecommendedAIProvider(): AIProvider = when {
        isGeminiNanoAvailable() -> AIProvider.GEMINI_NANO
        isMLKitAvailable() -> AIProvider.ML_KIT
        else -> AIProvider.NONE
    }
}
```

---

## 1. DOCUMENTOS A MODIFICAR

### 1.1 Documento de Constitución
**ID**: `doc-documento-de-constituci-n-edugo-android-modules-ko-3c2b6022`

**Sección a cambiar** (línea ~45-50):
```markdown
### 2.2 Android Targets
| Componente | Versión |
|-----------|---------| 
| **compileSdk** | 35 (Android 15) |
| **targetSdk** | 35 (Android 15) |
| **minSdk** | 26 (Android 8.0 Oreo) |  <!-- CAMBIAR -->
```

**Cambiar a**:
```markdown
### 2.2 Android Targets
| Componente | Versión |
|-----------|---------| 
| **compileSdk** | 35 (Android 15) |
| **targetSdk** | 35 (Android 15) |
| **minSdk** | 29 (Android 10) |

### 2.3 Preparación para IA On-Device
| Feature | API Mínima | Estado |
|---------|------------|--------|
| Neural Networks API 1.2 | 29 | Disponible |
| ML Model Binding | 30 | Runtime check |
| Gemini Nano / AI Core | 34+ | Runtime check (hardware específico) |

> **Nota**: Las APIs de IA avanzada (Gemini Nano) requieren Android 14+ y hardware compatible (Pixel 8+, Galaxy S24+). Usar detección en runtime.
```

**Agregar nueva sección después de "2.4 Dependencias PROHIBIDAS"**:
```markdown
### 2.5 APIs de Android 10+ Disponibles
Con minSdk 29, estas APIs están disponibles nativamente:
- **Scoped Storage**: Acceso seguro a archivos sin permisos legacy
- **Dark Theme API**: `Configuration.isNightModeActive()`
- **Biometric API**: Autenticación biométrica unificada
- **TLS 1.3**: Seguridad mejorada en networking
- **Neural Networks API 1.2**: Base para ML on-device
- **Gesture Navigation**: Edge-to-edge nativo
```

---

### 1.2 Guía de Setup y Configuración
**ID**: `doc-gu-a-de-setup-y-configuraci-n-kotlin-multiplatform-f6f555a4`

**Sección a cambiar** (línea ~20-25):
```markdown
### Plataformas Objetivo
- **Android**: API 26+ (minSdk) → API 35 (targetSdk/compileSdk)  <!-- CAMBIAR -->
```

**Cambiar a**:
```markdown
### Plataformas Objetivo
- **Android**: API 29+ (minSdk - Android 10) → API 35 (targetSdk/compileSdk)
  - Nota: minSdk 29 habilita Scoped Storage, TLS 1.3, NNAPI 1.2
```

**Sección a cambiar en libs.versions.toml** (línea ~115):
```toml
android-minSdk = "26"  <!-- CAMBIAR -->
```

**Cambiar a**:
```toml
android-minSdk = "29"
```

**Agregar nueva sección después de "## 9. Comandos Útiles"**:
```markdown
## 9.5 Detección de Features de IA en Runtime

Para usar APIs de IA avanzada cuando estén disponibles:

```kotlin
// En commonMain - expect/actual pattern
expect fun isAICoreAvailable(): Boolean

// En androidMain
actual fun isAICoreAvailable(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE // API 34
}

// Uso
if (isAICoreAvailable()) {
    // Usar Gemini Nano o AI Core APIs
} else {
    // Fallback a implementación estándar
}
```
```

---

## 2. STORIES A MODIFICAR

### 2.1 Story: build.gradle.kts raíz y gradle.properties
**ID**: `story-como-desarrollador-quiero-implementar-build-gradle-c56a9b0a`

**Texto actual**:
```
- [ ] Configurar Android properties: compileSdk, minSdk defaults
```

**Cambiar a**:
```
- [ ] Configurar Android properties: compileSdk=35, minSdk=29, targetSdk=35
- [ ] Documentar razón de minSdk=29: Scoped Storage, NNAPI 1.2, TLS 1.3
```

---

### 2.2 Story: Convention Plugins (kmp.library, kmp.android)
**ID**: `story-como-desarrollador-quiero-crear-convention-plugins-310d3e37`

**Texto actual**:
```
- [ ] Plugin kmp.android.gradle.kts con configuración Android: minSdk, compileSdk, namespace
```

**Cambiar a**:
```
- [ ] Plugin kmp.android.gradle.kts con configuración Android: minSdk=29, compileSdk=35, namespace
- [ ] Incluir comentario explicando minSdk=29 y preparación para IA
```

---

### 2.3 Story: sourcesets específicos androidMain/desktopMain/jsMain
**ID**: `story-como-desarrollador-quiero-implementar-sourcesets-e-0f54ff41`

**Texto actual**:
```
- [ ] Configurar android { } block con namespace, minSdk, compileSdk
```

**Cambiar a**:
```
- [ ] Configurar android { } block con namespace, minSdk=29, compileSdk=35
- [ ] Agregar expect/actual para detección de features Android 10+ (Scoped Storage, BiometricManager)
```

---

## 3. TAREAS (TASKS) A MODIFICAR

### 3.1 Task: Crear gradle.properties
**ID**: `task-crear-gradle-properties-con-jvm-args-y-flags-de-pe-140014a4`

**Sección a cambiar**:
```
### 4. Configuración Android
```properties
android.minSdk=24  <!-- Ya corregido en código, pero task description dice 24 -->
```

**Cambiar descripción a**:
```
### 4. Configuración Android
```properties
# Android 10+ base moderna para KMP con IA
android.compileSdk=35
android.targetSdk=35
android.minSdk=29

# Nota: minSdk=29 habilita:
# - Scoped Storage (acceso seguro a archivos)
# - TLS 1.3 (seguridad mejorada)
# - NNAPI 1.2 (base para ML on-device)
# - Dark Theme API nativa
# Para IA avanzada (Gemini Nano), usar runtime check en Android 14+
```

---

### 3.2 Task: Crear build.gradle.kts raíz
**ID**: `task-crear-build-gradle-kts-ra-z-con-plugins-y-configur-3288dc5a`

**Sin cambios necesarios** - Este task es sobre plugins, no sobre SDK versions.

---

### 3.3 Task: Validar build y documentar baseline
**ID**: `task-validar-build-y-documentar-baseline-de-performance-78f0b0d0`

**Agregar a criterios de aceptación**:
```
- [ ] Documentar minSdk=29 y sus beneficios en PERFORMANCE.md
- [ ] Verificar que el build funciona correctamente con API 29 como mínimo
```

---

## 4. FLOW_ROWS (MÓDULOS) A CONSIDERAR

### 4.1 edugo-network (Sprint 3)
**ID**: `fr-1769657471291782000`

**Consideración**: Con minSdk 29, TLS 1.3 está disponible por defecto. No se necesita configuración adicional de SSL.

**Agregar nota en implementación**:
```kotlin
// TLS 1.3 disponible nativamente en Android 10+ (minSdk 29)
// No se requiere configuración especial de SSLSocketFactory
```

---

### 4.2 edugo-storage (Sprint 3)
**ID**: `fr-1769657471829538000`

**Consideración**: Con minSdk 29, Scoped Storage está disponible. multiplatform-settings funciona igual, pero si se accede a archivos externos, usar APIs de Scoped Storage.

**Agregar nota en implementación**:
```kotlin
// Android 10+ (minSdk 29): Scoped Storage activo
// Para archivos en app-specific directory: no requiere permisos
// Para MediaStore: usar APIs de SAF o MediaStore
```

---

### 4.3 edugo-auth (Sprint 4)
**ID**: `fr-1769657735482006000`

**Consideración**: Con minSdk 29, BiometricPrompt está disponible de forma más completa.

**Agregar nota en implementación**:
```kotlin
// Android 10+ (minSdk 29): BiometricPrompt API completa
// Considerar agregar autenticación biométrica para tokens sensibles
```

---

### 4.4 edugo-analytics (Sprint 5)
**ID**: `fr-1769658456684572000`

**Consideración**: Preparar para integración con ML Kit y Gemini Nano en futuro.

**Agregar nota**:
```kotlin
// Preparación para IA:
// - Android 10-13: Usar ML Kit para inferencia local básica
// - Android 14+: Considerar Gemini Nano cuando hardware lo soporte
```

---

## 5. ARCHIVOS DE CÓDIGO YA ACTUALIZADOS

| Archivo | Estado | Notas |
|---------|--------|-------|
| `gradle.properties` | **ACTUALIZADO** | minSdk=29 con comentarios de IA |
| `gradle/libs.versions.toml` | **ACTUALIZADO** | Versiones alineadas con Constitución + SDK versions |
| `settings.gradle.kts` | **CREADO** | Configuración correcta |
| `build.gradle.kts` | **ACTUALIZADO** | Sin repos en allprojects |

### Versiones Actualizadas en libs.versions.toml

| Dependencia | Versión Anterior | Versión Actual | Razón |
|-------------|------------------|----------------|-------|
| kotlinx-coroutines | 1.9.0 | **1.10.2** | Alineado con Constitución |
| kotlinx-serialization | 1.7.3 | **1.8.1** | Requiere Kotlin 2.1+ |
| kotlinx-datetime | 0.6.1 | **0.6.2** | Última estable |
| ktor | 3.0.3 | **3.1.3** | Alineado con Constitución |
| kermit | (nuevo) | **2.0.4** | Logging multiplataforma |
| multiplatform-settings | (nuevo) | **1.3.0** | Storage multiplataforma |

### Nuevas Secciones Agregadas

```toml
# SDK Versions centralizadas
android-compileSdk = "35"
android-targetSdk = "35"
android-minSdk = "29"

# Nuevas librerías
kermit = "2.0.4"
multiplatform-settings = "1.3.0"

# Nuevos bundles
multiplatform-settings-common = [...]
```

---

## 6. RESUMEN DE CAMBIOS POR TIPO

### Documentos (7 total, 2 requieren cambios)
| Document ID | Título | Cambio Requerido |
|-------------|--------|------------------|
| `doc-documento-de-constituci-n-edugo-android-modules-ko-3c2b6022` | Constitución | **SI** - minSdk, sección IA |
| `doc-gu-a-de-setup-y-configuraci-n-kotlin-multiplatform-f6f555a4` | Setup Guide | **SI** - minSdk, libs.versions.toml |
| `doc-est-ndares-de-c-digo-kotlin-multiplatform-kmp-f294ef30` | Estándares Código | NO |
| `doc-gu-a-de-arquitectura-enterprise-kotlin-multiplatfo-0afa63ee` | Arquitectura | NO |
| `doc-gu-a-de-observabilidad-kotlin-multiplatform-kmp-10b21aa5` | Observabilidad | NO |
| `doc-gu-a-de-seguridad-kotlin-multiplatform-kmp-5b0eb748` | Seguridad | NO |
| `doc-gu-a-de-testing-avanzado-kotlin-multiplatform-kmp-814b5a8d` | Testing | NO |

### Stories (10 total, 3 requieren cambios)
| Story ID | Título | Cambio Requerido |
|----------|--------|------------------|
| `story-como-desarrollador-quiero-implementar-build-gradle-c56a9b0a` | build.gradle.kts | **SI** |
| `story-como-desarrollador-quiero-crear-convention-plugins-310d3e37` | Convention Plugins | **SI** |
| `story-como-desarrollador-quiero-implementar-sourcesets-e-0f54ff41` | Sourcesets | **SI** |
| Otras 7 stories | ... | NO |

### Tasks (3 revisadas, 1 requiere cambio)
| Task ID | Título | Cambio Requerido |
|---------|--------|------------------|
| `task-crear-gradle-properties-con-jvm-args-y-flags-de-pe-140014a4` | gradle.properties | **SI** - descripción dice minSdk=24 |
| `task-crear-build-gradle-kts-ra-z-con-plugins-y-configur-3288dc5a` | build.gradle.kts | NO |
| `task-validar-build-y-documentar-baseline-de-performance-78f0b0d0` | Performance | Agregar criterio |

---

## 7. PRÓXIMOS PASOS

1. **Manual**: Actualizar las stories y tasks mencionadas en la BD
2. **Manual**: Actualizar los 2 documentos en la BD
3. **Automático**: Al implementar cada módulo, el LLM leerá este documento y aplicará minSdk=29
4. **Futuro**: Cuando se implemente edugo-analytics, agregar detección de AI Core

---

**Documento generado**: 2026-01-29
**Autor**: Code Review Automation
