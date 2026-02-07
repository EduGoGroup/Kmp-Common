# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**EduGo KMP Shared** - A Kotlin Multiplatform library providing common utilities for educational applications across JVM, Android, JS, iOS, and Desktop platforms.

**Key Technologies:**
- Kotlin 2.1.20 with Multiplatform support
- Gradle 8.11+ with Convention Plugins
- Android Gradle Plugin 8.7.2
- Java 17 LTS (JVM target)
- Ktor 3.1.3 for networking
- kotlinx.serialization for JSON

## Build Commands

### Testing

```bash
# Run all tests (all platforms)
./gradlew test

# Run tests for specific platform
./gradlew desktopTest        # JVM Desktop tests
./gradlew androidUnitTest    # Android unit tests
./gradlew jsTest             # JavaScript tests (Node.js + Browser)

# Run tests for specific module
./gradlew :test-module:test
./gradlew :test-module-full:test
```

### Building

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :test-module:build
./gradlew :test-module-full:build

# Clean build
./gradlew clean build
```

### Code Coverage

```bash
# Generate coverage report (Kover)
./gradlew koverHtmlReport

# View report at: build/reports/kover/html/index.html
```

### Linting and Verification

```bash
# Compile check without tests
./gradlew compileKotlinJvm compileKotlinAndroid

# Verify all targets compile
./gradlew assemble
```

## Architecture

### Multiplatform Source Set Hierarchy

The project uses a **custom hierarchy** with `jvmSharedMain` for shared JVM code:

```
commonMain
├── androidMain (via jvmSharedMain)
├── desktopMain (via jvmSharedMain)
└── jsMain (direct)

jvmSharedMain (intermediate source set)
├── androidMain
└── desktopMain
```

**Important:** `kotlin.mpp.applyDefaultHierarchyTemplate=false` is set in `gradle.properties`. The project uses a custom hierarchy to share JVM-specific code between Android and Desktop platforms through `jvmSharedMain`.

### Core Package Structure

**test-module** (Android + Desktop + JS):
- `core/` - Result monad, AppError, ErrorCode, serialization extensions
- `network/` - HTTP client (Ktor), interceptors, retry logic, auth integration
- `storage/` - Multiplatform key-value storage with serialization support
- `auth/` - Authentication service with token refresh management
- `validation/` - Input validators (email, UUID, ranges, patterns)
- `platform/` - Platform-specific implementations (Logger, Dispatchers)
- `data/models/` - Domain models with base interfaces (EntityBase, ValidatableModel, AuditableModel, SoftDeletable)
- `mapper/` - Domain mapping utilities

**test-module-full** (All platforms including iOS):
- Simpler module for testing full multiplatform support

### Convention Plugins

Located in `build-logic/src/main/kotlin/`:

1. **kmp.android** - For modules with Android support
   - Targets: Android, JVM Desktop, JavaScript
   - Configures: compileSdk=35, minSdk=29, JVM=17
   - Includes: jvmSharedMain intermediate source set
   - Auto-includes: Ktor, kotlinx bundles, platform-specific engines

2. **kmp.full** - For full multiplatform (adds iOS)
   - All targets: Android, iOS, JVM, JS

3. **kmp.library** - Pure Kotlin (JVM only)
   - Targets: JVM only
   - No Android dependencies

4. **kover** - Code coverage configuration

**Usage Pattern:**
```kotlin
plugins {
    id("kmp.android")  // or kmp.full, kmp.library
    kotlin("plugin.serialization")
}

android {
    namespace = "com.edugo.your.module"  // REQUIRED for Android modules
}
```

### Key Design Patterns

**1. Result Monad (`core/Result.kt`)**
- Type-safe error handling with Success/Failure/Loading states
- Rich functional API: map, flatMap, recover, zip, combine
- Nullable conversions: `toResult()`, `toResultOrElse()`

**2. AppError (`core/AppError.kt`)**
- Structured errors with ErrorCode, message, details, cause, timestamp
- Factory methods: `AppError.validation()`, `AppError.network()`, `AppError.authentication()`
- Retryable flag for automatic retry logic

**3. Network Layer (`network/`)**
- `EduGoHttpClient`: Wrapper around Ktor HttpClient
- Token refresh: Automatic 401 handling with `TokenRefreshManager`
- Interceptors: Auth, Headers, Logging
- Platform-specific engines: OkHttp (Android), CIO (Desktop), JS (Browser/Node)

**4. Storage Layer (`storage/`)**
- Type-safe multiplatform key-value storage
- Serialization support via `toJson()` / `fromJson()`
- Flow-based reactivity: `StateFlowStorage` for reactive updates
- Async API: `AsyncEduGoStorage` with coroutines

**5. Validation (`validation/`)**
- Two APIs: Boolean (`isValidEmail()`) and Result-based (`validateEmail()`)
- Base validators return `AppError?`
- Extension functions for common patterns
- `AccumulativeValidation` for collecting multiple errors

### Important Files

- `build-logic/src/main/kotlin/kmp.android.gradle.kts` - Main convention plugin
- `gradle/libs.versions.toml` - Version catalog (ALL versions centralized here)
- `gradle.properties` - Gradle configuration and performance flags
- `test-module/src/commonMain/kotlin/com/edugo/test/module/core/Result.kt` - Core Result type
- `test-module/src/commonMain/kotlin/com/edugo/test/module/network/EduGoHttpClient.kt` - HTTP client

## Development Guidelines

### Adding a New Module

1. Create directory structure:
   ```bash
   mkdir -p my-module/src/commonMain/kotlin/com/edugo/mymodule
   ```

2. Create `my-module/build.gradle.kts`:
   ```kotlin
   plugins {
       id("kmp.android")  // or kmp.full
       kotlin("plugin.serialization")
   }

   android {
       namespace = "com.edugo.mymodule"
   }

   kotlin {
       sourceSets {
           val commonMain by getting {
               dependencies {
                   implementation(libs.kotlinx.coroutines.core)
               }
           }
       }
   }
   ```

3. Add to `settings.gradle.kts`:
   ```kotlin
   include(":my-module")
   ```

### Version Management

**ALL versions must be defined in `gradle/libs.versions.toml`**. Never hardcode versions in build files.

To add a dependency:
1. Add version to `[versions]` section
2. Add library to `[libraries]` section
3. Reference via `libs.my.dependency`

### Platform-Specific Code

Use `expect/actual` pattern for platform differences:

```kotlin
// commonMain
expect fun getPlatformName(): String

// androidMain
actual fun getPlatformName(): String = "Android"

// desktopMain (in jvmSharedMain if shared with Android)
actual fun getPlatformName(): String = "JVM"
```

### Testing Patterns

- Common tests go in `commonTest/`
- Platform-specific tests in `androidUnitTest/`, `desktopTest/`, `jsTest/`
- Use `kotlin("test")` for multiplatform test support
- Coroutines: use `kotlinx-coroutines-test` with `runTest {}`
- Flow testing: use Turbine library

### Code Style

- **Explicit API mode**: Currently disabled but planned for library projects
- All public APIs should have KDoc comments
- Use `public` modifier explicitly for public APIs
- Prefer immutable data classes with `val` properties

## Common Issues

### Namespace Not Set Error
Every Android module MUST define a namespace:
```kotlin
android {
    namespace = "com.edugo.module"
}
```

### Version Catalog Not Found
Ensure dependency exists in `gradle/libs.versions.toml` and reference correctly:
```kotlin
implementation(libs.kotlinx.coroutines.core)  // ✓ Correct
implementation("org.jetbrains.kotlinx:...")   // ✗ Avoid
```

### Source Set Not Found
For Android + Desktop modules, use `jvmSharedMain` for shared JVM code:
```kotlin
val jvmSharedMain by creating {
    dependsOn(commonMain)
}
val androidMain by getting {
    dependsOn(jvmSharedMain)
}
```

### JS Tests Failing
JS tests run in both browser (Karma) and Node.js (Mocha). Ensure code works in both environments or use platform-specific source sets.

## Technical Debt Notes

- **Custom Hierarchy**: Using `jvmSharedMain` instead of standard `jvmMain` (see `gradle.properties`)
  - TODO: Migrate to standard hierarchy when feasible
  - Tracked in: Sprint Deuda Técnica

- **Explicit API Mode**: Currently disabled in test-module
  - TODO: Re-enable after QA pass
  - See comment in `test-module/build.gradle.kts`

- **Compiler Options Duplication**: `-Xcontext-receivers` and opt-in flags duplicated across convention plugins
  - Low severity, consider extracting to shared base plugin
  - See: `kmp.android.gradle.kts` line 200

## Related Documentation

- `README.md` - Library overview and quick start
- `docs/guides/` - Comprehensive usage guides (JSON, Validation, Error Handling, Examples)
- `docs/CONVENTION-PLUGINS.md` - Convention plugins reference
- `docs/VERSION-CATALOG.md` - Version catalog documentation
- `docs/GRADLE-SETTINGS.md` - Gradle configuration details
