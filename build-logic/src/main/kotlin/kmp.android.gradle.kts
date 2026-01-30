import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

// Constantes de configuración
val VERSION_CATALOG_NAME = "libs"
val COMPILE_SDK = 35
val MIN_SDK = 29
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
                    .orElseThrow { IllegalStateException("kotlinx-common bundle not found in catalog") })

                // Ktor Common (client core + plugins)
                implementation(libs.findBundle("ktor-common")
                    .orElseThrow { IllegalStateException("ktor-common bundle not found in catalog") })
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
        val androidMain by getting {
            dependencies {
                // Ktor engine para Android (OkHttp)
                implementation(libs.findLibrary("ktor-client-okhttp")
                    .orElseThrow { IllegalStateException("ktor-client-okhttp not found in catalog") })

                // Coroutines con Dispatchers.Main para UI
                implementation(libs.findLibrary("kotlinx-coroutines-android")
                    .orElseThrow { IllegalStateException("kotlinx-coroutines-android not found in catalog") })
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.findLibrary("mockk-android")
                    .orElseThrow { IllegalStateException("mockk-android not found in catalog") })
                implementation(libs.findLibrary("kotlin-test-junit")
                    .orElseThrow { IllegalStateException("kotlin-test-junit not found in catalog") })
            }
        }
        val desktopMain by getting {
            dependencies {
                // Ktor engine para JVM Desktop (CIO - Coroutine I/O)
                implementation(libs.findLibrary("ktor-client-cio")
                    .orElseThrow { IllegalStateException("ktor-client-cio not found in catalog") })

                // Coroutines con Dispatchers.Swing para Desktop GUI
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
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-receivers",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}
