# =============================================================================
# ProGuard Rules for test-module
# =============================================================================
# Reglas para consumidores de esta library que aseguran compatibilidad
# con ProGuard/R8 en builds release.
# =============================================================================

# -----------------------------------------------------------------------------
# Kermit Logger - Preservar logging en release
# -----------------------------------------------------------------------------
# Kermit 2.0.4 es compatible con ProGuard/R8 por defecto, pero preservamos
# las clases principales para asegurar que el logging funcione correctamente.

-keep class co.touchlab.kermit.** { *; }
-keepnames class co.touchlab.kermit.** { *; }

# Permitir que Kermit sea optimizado pero mantener las firmas de métodos
-keepclassmembers class co.touchlab.kermit.Logger {
    public <methods>;
}

# Preservar los LogWriters para que puedan ser instanciados correctamente
-keep class * implements co.touchlab.kermit.LogWriter { *; }

# -----------------------------------------------------------------------------
# KermitLogger y KermitConfig - Clases públicas del módulo
# -----------------------------------------------------------------------------
-keep class com.edugo.test.module.platform.KermitLogger { *; }
-keep class com.edugo.test.module.platform.KermitConfig { *; }

# Preservar métodos públicos de logging
-keepclassmembers class com.edugo.test.module.platform.KermitLogger {
    public <methods>;
}

# -----------------------------------------------------------------------------
# Reflection - Preservar anotaciones para debugging
# -----------------------------------------------------------------------------
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# -----------------------------------------------------------------------------
# Kotlin Metadata - Necesario para reflection de Kotlin
# -----------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
