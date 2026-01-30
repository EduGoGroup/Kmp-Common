import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
}

// Acceso al version catalog desde included build
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    // JVM Target
    jvm {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }

    // Source Sets comunes
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
        val jvmMain by getting {
            dependencies {
                // Ktor engine para JVM (CIO - Coroutine I/O)
                implementation(libs.findLibrary("ktor-client-cio")
                    .orElseThrow { IllegalStateException("Library 'ktor-client-cio' not found in version catalog. Check gradle/libs.versions.toml") })

                // Coroutines con Dispatchers.Swing para Desktop GUI
                implementation(libs.findLibrary("kotlinx-coroutines-swing")
                    .orElseThrow { IllegalStateException("Library 'kotlinx-coroutines-swing' not found in version catalog. Check gradle/libs.versions.toml") })
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.findLibrary("mockk")
                    .orElseThrow { IllegalStateException("Library 'mockk' not found in version catalog. Check gradle/libs.versions.toml") })
                implementation(libs.findLibrary("kotlin-test-junit")
                    .orElseThrow { IllegalStateException("Library 'kotlin-test-junit' not found in version catalog. Check gradle/libs.versions.toml") })
            }
        }
    }

    // Configuración del toolchain
    jvmToolchain(17)
}

// Opciones de compilador comunes
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-receivers",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}
