package com.edugo.test.module.platform

/**
 * JVM implementation of Platform.
 */
public actual object Platform {
    actual val name: String = "JVM"

    actual val osVersion: String
        get() = System.getProperty("os.version") ?: "Unknown"

    actual val isDebug: Boolean
        get() = java.lang.management.ManagementFactory.getRuntimeMXBean()
            .inputArguments.any { it.contains("-agentlib:jdwp") }
}
