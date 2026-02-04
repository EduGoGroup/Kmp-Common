import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

// Constantes de configuración
val VERSION_CATALOG_NAME = "libs"

/**
 * Android SDK version to compile against.
 * SDK 35 = Android 15 (VanillaIceCream) - Latest stable as of 2024.
 * This determines which Android APIs are available during compilation.
 */
val COMPILE_SDK = 35

/**
 * Minimum Android version required to run the app.
 * SDK 29 = Android 10 - Covers ~85% of active devices (as of 2024).
 * Provides modern APIs (Scoped Storage, NNAPI 1.2, TLS 1.3) while maintaining broad compatibility.
 */
val MIN_SDK = 29

/**
 * JVM target version for Kotlin compilation.
 * Java 17 is the LTS version required by Android Gradle Plugin 8.x+.
 * Provides modern language features while ensuring compatibility with Android runtime.
 */
val JVM_TARGET = 17

// Acceso al version catalog desde included build
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named(VERSION_CATALOG_NAME)

kotlin {
    androidTarget {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.fromTarget(JVM_TARGET.toString()))
            }
        }
    }

    // JVM Desktop target - nombre específico para distinguir de otros JVM targets
    jvm("desktop") {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.fromTarget(JVM_TARGET.toString()))
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // KotlinX Common (coroutines, serialization, datetime)
                implementation(libs.findBundle("kotlinx-common")
                    .orElseThrow { IllegalStateException("Bundle 'kotlinx-common' not found in version catalog. Check gradle/libs.versions.toml") })

                // Ktor Common (client core + plugins)
                implementation(libs.findBundle("ktor-common")
                    .orElseThrow { IllegalStateException("Bundle 'ktor-common' not found in version catalog. Check gradle/libs.versions.toml") })
            }
        }
        val commonTest by getting {
            dependencies {
                // Testing framework multiplataforma
                implementation(kotlin("test"))

                // Ktor mock para testing de network
                implementation(libs.findLibrary("ktor-client-mock")
                    .orElseThrow { IllegalStateException("Library 'ktor-client-mock' not found in version catalog. Check gradle/libs.versions.toml") })

                // Coroutines test utilities
                implementation(libs.findLibrary("kotlinx-coroutines-test")
                    .orElseThrow { IllegalStateException("Library 'kotlinx-coroutines-test' not found in version catalog. Check gradle/libs.versions.toml") })

                // Flow testing
                implementation(libs.findLibrary("turbine")
                    .orElseThrow { IllegalStateException("Library 'turbine' not found in version catalog. Check gradle/libs.versions.toml") })
            }
        }

        // Intermediate sourceset para código JVM compartido entre Android y Desktop
        // Jerarquía: commonMain -> jvmSharedMain -> androidMain/desktopMain
        val jvmSharedMain by creating {
            dependsOn(commonMain)
            // Dependencias JVM comunes (APIs de java.* disponibles en ambos)
        }
        val jvmSharedTest by creating {
            dependsOn(commonTest)
        }

        val androidMain by getting {
            dependsOn(jvmSharedMain)
            dependencies {
                // Ktor engine para Android (OkHttp)
                implementation(libs.findLibrary("ktor-client-okhttp")
                    .orElseThrow { IllegalStateException("Library 'ktor-client-okhttp' not found in version catalog. Check gradle/libs.versions.toml") })

                // Coroutines con Dispatchers.Main para UI
                implementation(libs.findLibrary("kotlinx-coroutines-android")
                    .orElseThrow { IllegalStateException("Library 'kotlinx-coroutines-android' not found in version catalog. Check gradle/libs.versions.toml") })
            }
        }
        val androidUnitTest by getting {
            dependsOn(jvmSharedTest)
            dependencies {
                implementation(libs.findLibrary("mockk-android")
                    .orElseThrow { IllegalStateException("Library 'mockk-android' not found in version catalog. Check gradle/libs.versions.toml") })
                implementation(libs.findLibrary("kotlin-test-junit")
                    .orElseThrow { IllegalStateException("Library 'kotlin-test-junit' not found in version catalog. Check gradle/libs.versions.toml") })
            }
        }
        val desktopMain by getting {
            dependsOn(jvmSharedMain)
            dependencies {
                // Ktor engine para JVM Desktop (CIO - Coroutine I/O)
                implementation(libs.findLibrary("ktor-client-cio")
                    .orElseThrow { IllegalStateException("Library 'ktor-client-cio' not found in version catalog. Check gradle/libs.versions.toml") })

                // Coroutines con Dispatchers.Swing para Desktop GUI
                implementation(libs.findLibrary("kotlinx-coroutines-swing")
                    .orElseThrow { IllegalStateException("Library 'kotlinx-coroutines-swing' not found in version catalog. Check gradle/libs.versions.toml") })
            }
        }
        val desktopTest by getting {
            dependsOn(jvmSharedTest)
            dependencies {
                implementation(libs.findLibrary("mockk")
                    .orElseThrow { IllegalStateException("Library 'mockk' not found in version catalog. Check gradle/libs.versions.toml") })
                implementation(libs.findLibrary("kotlin-test-junit")
                    .orElseThrow { IllegalStateException("Library 'kotlin-test-junit' not found in version catalog. Check gradle/libs.versions.toml") })
            }
        }

    }

    jvmToolchain(JVM_TARGET)
}

// Configuración Android
configure<LibraryExtension> {
    compileSdk = COMPILE_SDK

    defaultConfig {
        minSdk = MIN_SDK  // NNAPI 1.2, Scoped Storage, TLS 1.3
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // NOTA: Cada módulo debe configurar su namespace en build.gradle.kts
    // Ejemplo: android { namespace = "com.edugo.tu.modulo" }
}

// Opciones de compilador comunes
// NOTA: Esta configuración está duplicada intencionalmente en kmp.library.gradle.kts
// para mantener la configuración explícita y clara en cada plugin.
// Si se agregan más flags o plugins, considerar extraer a un plugin base compartido.
// Ver: Issue #6 - Compiler options duplicados (LOW severity, refactorización futura)
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-receivers",  // Habilita context receivers (Kotlin experimental feature)
            "-opt-in=kotlin.RequiresOptIn"  // Permite uso de APIs experimentales sin warnings
        )
    }
}
