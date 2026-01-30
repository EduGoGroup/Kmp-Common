import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
}

// Constantes de configuración
val VERSION_CATALOG_NAME = "libs"
val JVM_TARGET = 17

// Acceso al version catalog desde included build (convention plugins en buildSrc)
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named(VERSION_CATALOG_NAME)

kotlin {
    // Desktop (JVM) Target con nombre explícito
    jvm("desktop") {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.fromTarget(JVM_TARGET.toString()))
            }
        }
    }

    // JavaScript Target con IR compiler (modo recomendado para producción)
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        nodejs {
            testTask {
                useMocha()
            }
        }
        // Genera JS ejecutable
        binaries.executable()
    }

    // Source Sets
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Ktor Client
                implementation(libs.findLibrary("ktor-client-core")
                    .orElseThrow { IllegalStateException("ktor-client-core not found in catalog") })
                implementation(libs.findLibrary("ktor-client-content-negotiation")
                    .orElseThrow { IllegalStateException("ktor-client-content-negotiation not found in catalog") })
                implementation(libs.findLibrary("ktor-serialization-kotlinx-json")
                    .orElseThrow { IllegalStateException("ktor-serialization-kotlinx-json not found in catalog") })
                implementation(libs.findLibrary("ktor-client-logging")
                    .orElseThrow { IllegalStateException("ktor-client-logging not found in catalog") })

                // Serialization
                implementation(libs.findLibrary("kotlinx-serialization-core")
                    .orElseThrow { IllegalStateException("kotlinx-serialization-core not found in catalog") })
                implementation(libs.findLibrary("kotlinx-serialization-json")
                    .orElseThrow { IllegalStateException("kotlinx-serialization-json not found in catalog") })

                // Coroutines
                implementation(libs.findLibrary("kotlinx-coroutines-core")
                    .orElseThrow { IllegalStateException("kotlinx-coroutines-core not found in catalog") })
            }
        }
        val commonTest by getting {
            dependencies {
                // Testing framework multiplataforma
                implementation(kotlin("test"))

                // Ktor mock para testing de network
                implementation(libs.findLibrary("ktor-client-mock")
                    .orElseThrow { IllegalStateException("ktor-client-mock not found in catalog") })

                // Coroutines test utilities
                implementation(libs.findLibrary("kotlinx-coroutines-test")
                    .orElseThrow { IllegalStateException("kotlinx-coroutines-test not found in catalog") })

                // Flow testing
                implementation(libs.findLibrary("turbine")
                    .orElseThrow { IllegalStateException("turbine not found in catalog") })
            }
        }

        // Desktop (JVM) source sets
        val desktopMain by getting {
            dependencies {
                // Ktor engine para JVM/Desktop (CIO)
                implementation(libs.findLibrary("ktor-client-cio")
                    .orElseThrow { IllegalStateException("ktor-client-cio not found in catalog") })

                // Coroutines con Dispatchers.Swing
                implementation(libs.findLibrary("kotlinx-coroutines-swing")
                    .orElseThrow { IllegalStateException("kotlinx-coroutines-swing not found in catalog") })
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.findLibrary("mockk")
                    .orElseThrow { IllegalStateException("mockk not found in catalog") })
                implementation(libs.findLibrary("kotlin-test-junit")
                    .orElseThrow { IllegalStateException("kotlin-test-junit not found in catalog") })
            }
        }

        // JavaScript source sets
        val jsMain by getting {
            dependencies {
                // Ktor engine para JavaScript (usa fetch API)
                implementation(libs.findLibrary("ktor-client-js")
                    .orElseThrow { IllegalStateException("ktor-client-js not found in catalog") })
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }

    // Configuración del toolchain para JVM
    jvmToolchain(JVM_TARGET)
}

// Opciones de compilador comunes
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            // Context receivers: Permite pasar contextos implícitos sin parámetros (experimental)
            // Usado en: dependency injection patterns, coroutine scopes, logging contexts
            "-Xcontext-receivers",

            // OptIn: Permite usar APIs experimentales sin warnings de compilación
            // Necesario para: Kotlin Coroutines experimental APIs, KMP experimental features
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}
