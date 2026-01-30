import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
}

// Acceso al version catalog desde included build
val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    // JVM Desktop target - nombre específico para distinguir de otros JVM targets
    jvm("desktop") {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }

    // Source Sets con jerarquía consistente con kmp.android.gradle.kts
    // Jerarquía: commonMain -> jvmSharedMain -> desktopMain
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

        // Intermediate sourceset para código JVM compartido
        // Mantiene consistencia con kmp.android.gradle.kts para que módulos
        // library-only puedan compartir código con módulos Android
        val jvmSharedMain by creating {
            dependsOn(commonMain)
            // Dependencias JVM comunes (APIs de java.* disponibles)
        }
        val jvmSharedTest by creating {
            dependsOn(commonTest)
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

    // Configuración del toolchain
    jvmToolchain(17)
}

// Opciones de compilador comunes
// NOTA: Esta configuración está duplicada intencionalmente en kmp.android.gradle.kts
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
