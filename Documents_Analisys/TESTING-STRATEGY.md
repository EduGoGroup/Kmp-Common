# Testing Strategy - EduGo KMP Modules

Este documento describe la estrategia de testing para el proyecto Kotlin Multiplatform.

## Source Sets de Testing

| Source Set | Target | Test Runner | Uso |
|------------|--------|-------------|-----|
| `commonTest` | Todas las plataformas | kotlin.test | Tests compartidos que se ejecutan en todas las plataformas |
| `jvmTest` | JVM | JUnit | Tests específicos de JVM con MockK |
| `androidUnitTest` | Android | JUnit + Android Test | Tests unitarios de Android con MockK-Android |
| `desktopTest` | Desktop (JVM) | JUnit | Tests específicos de Desktop con MockK |
| `jsTest` | JavaScript | Mocha (Node) / Karma (Browser) | Tests para plataforma JS |
| `jsNodeTest` | JS Node | Mocha | Tests específicos para Node.js |
| `jsBrowserTest` | JS Browser | Karma + Chrome Headless | Tests específicos para navegador |

## Comandos Disponibles

### Ejecutar Todos los Tests
```bash
# Todos los tests del proyecto
./gradlew allTests

# Tests de un módulo específico
./gradlew :test-module:allTests
./gradlew :test-module-full:allTests
```

### Tests por Plataforma
```bash
# JVM
./gradlew :test-module:jvmTest

# Android Unit Tests
./gradlew :test-module:testDebugUnitTest
./gradlew :test-module:testReleaseUnitTest

# Desktop
./gradlew :test-module-full:desktopTest

# JavaScript
./gradlew :test-module-full:jsNodeTest
./gradlew :test-module-full:jsBrowserTest
```

### Code Coverage (Kover)
```bash
# Generar reporte HTML de coverage
./gradlew koverHtmlReport

# Generar reporte XML de coverage
./gradlew koverXmlReport

# Verificar thresholds de coverage
./gradlew koverVerify
```

## Dependencias de Testing

### Common (todas las plataformas)
| Dependencia | Versión | Uso |
|-------------|---------|-----|
| `kotlin-test` | 2.1.20 | Framework de testing base de Kotlin |
| `kotlinx-coroutines-test` | 1.9.0 | Testing de coroutines |
| `turbine` | 1.2.0 | Testing de Kotlin Flow |

### JVM / Desktop
| Dependencia | Versión | Uso |
|-------------|---------|-----|
| `mockk` | 1.13.13 | Mocking para JVM |
| `kotlin-test-junit` | 2.1.20 | Integración con JUnit |

### Android
| Dependencia | Versión | Uso |
|-------------|---------|-----|
| `mockk-android` | 1.13.13 | Mocking para Android |
| `kotlin-test-junit` | 2.1.20 | Integración con JUnit |

### JavaScript
| Dependencia | Versión | Uso |
|-------------|---------|-----|
| `kotlin-test-js` | 2.1.20 | Framework de testing para JS |

## Ubicación de Reportes

| Tipo | Ubicación |
|------|-----------|
| Tests JVM | `build/reports/tests/jvmTest/index.html` |
| Tests Android Debug | `build/reports/tests/testDebugUnitTest/index.html` |
| Tests Desktop | `build/reports/tests/desktopTest/index.html` |
| Tests JS Node | `build/reports/tests/jsNodeTest/index.html` |
| Tests JS Browser | `build/reports/tests/jsBrowserTest/index.html` |
| All Tests | `build/reports/tests/allTests/index.html` |
| Kover HTML | `build/reports/kover/html/index.html` |
| Kover XML | `build/reports/kover/report.xml` |

## Estructura de Módulos de Test

```
test-module/                    # Módulo Android + JVM
├── src/
│   ├── commonMain/            # Código compartido
│   ├── commonTest/            # Tests compartidos
│   ├── jvmMain/               # Código JVM
│   ├── jvmTest/               # Tests JVM específicos
│   ├── androidMain/           # Código Android
│   └── androidUnitTest/       # Tests Android específicos

test-module-full/              # Módulo Desktop + JS
├── src/
│   ├── commonMain/            # Código compartido
│   ├── commonTest/            # Tests compartidos
│   ├── desktopMain/           # Código Desktop
│   ├── desktopTest/           # Tests Desktop específicos
│   ├── jsMain/                # Código JavaScript
│   └── jsTest/                # Tests JS específicos
```

## Convention Plugins

| Plugin | Descripción |
|--------|-------------|
| `kmp.android` | Configura targets Android + JVM con dependencias de testing |
| `kmp.library` | Configura target JVM puro con dependencias de testing |
| `kmp.full` | Configura targets Desktop + JS con test runners |
| `kover` | Configura code coverage con reportes XML/HTML |

## Criterios de Calidad

- **Cobertura mínima**: 80% en código nuevo
- **Tests compartidos**: La mayoría de tests deben estar en `commonTest`
- **Tests específicos**: Solo para validar APIs nativas de cada plataforma
- **Expect/Actual**: Deben tener tests que validen la implementación en cada plataforma
