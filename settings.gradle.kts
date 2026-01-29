/**
 * EduGo KMP Shared - Settings Configuration
 *
 * Configura el proyecto raíz, repositorios de plugins y version catalog.
 * Gradle 8.11+ | Kotlin 2.1.20
 */

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "EduGo-KMP-Shared"

// Habilitar version catalog (habilitado por defecto en Gradle 8+)
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Incluir módulos cuando existan
// include(":core:common")
// include(":core:network")
// include(":feature:auth")
