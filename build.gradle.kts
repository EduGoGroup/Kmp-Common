/**
 * EduGo KMP Shared - Root Build Configuration
 *
 * Configuración raíz del proyecto Kotlin Multiplatform.
 * Los plugins se declaran con apply false y se aplican en los módulos individuales.
 *
 * @see gradle/libs.versions.toml para versiones centralizadas
 * @see build-logic/ para convention plugins
 */

plugins {
    // Kotlin Multiplatform - aplicado en módulos via convention plugin
    alias(libs.plugins.kotlin.multiplatform) apply false

    // Android Library - para módulos con target Android
    alias(libs.plugins.android.library) apply false

    // Android Application - si se necesita app de demo
    alias(libs.plugins.android.application) apply false

    // Kotlin Serialization - para módulos que usan serialización
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    group = "com.edugo.shared"
    version = "1.0.0-SNAPSHOT"
}

subprojects {
    // Configuración común para todos los subproyectos
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlin.uuid.ExperimentalUuidApi"
            )
        }
    }
}

// La tarea 'clean' es provista automáticamente por LifecycleBasePlugin
// a través de los plugins Kotlin/Android en los subproyectos
