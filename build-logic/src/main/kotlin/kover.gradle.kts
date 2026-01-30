import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension

plugins {
    id("org.jetbrains.kotlinx.kover")
}

configure<KoverProjectExtension> {
    reports {
        total {
            xml {
                onCheck.set(true)
            }
            html {
                onCheck.set(true)
            }
        }

        filters {
            excludes {
                // Excluir clases generadas
                classes(
                    "*BuildConfig*",
                    "*_Impl*",
                    "*_Factory*",
                    "*Hilt_*",
                    "*_HiltModules*",
                    "*_MembersInjector*",
                    "*_GeneratedInjector*",
                    "*.Companion",
                    "*.DefaultImpls"
                )
                // Excluir paquetes de código generado
                packages(
                    "hilt_aggregated_deps",
                    "dagger.hilt.internal.aggregatedroot.codegen",
                    "*.di"
                )
            }
        }
    }
}
