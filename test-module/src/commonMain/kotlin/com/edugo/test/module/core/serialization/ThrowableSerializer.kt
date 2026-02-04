package com.edugo.test.module.core.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer

/**
 * Representación serializable de un Throwable.
 *
 * Dado que Throwable no puede ser serializado directamente (no es una data class
 * y puede contener referencias circulares), esta clase captura la información
 * esencial para poder reconstruir el contexto del error después de la deserialización.
 *
 * @property className Nombre completo de la clase de la excepción
 * @property message Mensaje de error (puede ser null)
 * @property stackTrace Stack trace formateado como string
 */
@Serializable
public data class SerializableThrowable(
    val className: String,
    val message: String?,
    val stackTrace: String
) {
    companion object {
        /**
         * Convierte un Throwable en su representación serializable.
         */
        public fun from(throwable: Throwable): SerializableThrowable {
            return SerializableThrowable(
                // Use simpleName for JS compatibility (qualifiedName not supported in JS)
                className = throwable::class.simpleName ?: "UnknownException",
                message = throwable.message,
                stackTrace = throwable.stackTraceToString()
            )
        }
    }

    /**
     * Reconstruye una excepción genérica con la información serializada.
     *
     * Nota: No puede recrear la excepción original exacta, pero proporciona
     * un RuntimeException con toda la información relevante.
     */
    public fun toThrowable(): Throwable {
        return RuntimeException(
            "[$className] ${message ?: "(no message)"}\n$stackTrace"
        )
    }
}

/**
 * Serializer customizado para Throwable que usa SerializableThrowable como intermediario.
 *
 * Este serializer permite que campos Throwable? sean serializados y deserializados
 * sin perder información crítica del error.
 *
 * Uso:
 * ```kotlin
 * @Serializable
 * data class MyError(
 *     val code: Int,
 *     @Serializable(with = ThrowableSerializer::class)
 *     val cause: Throwable?
 * )
 * ```
 */
public object ThrowableSerializer : KSerializer<Throwable?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Throwable") {
        element<String?>("className")
        element<String?>("message")
        element<String?>("stackTrace")
    }

    override fun serialize(encoder: Encoder, value: Throwable?) {
        encoder.encodeStructure(descriptor) {
            if (value != null) {
                val serializable = SerializableThrowable.from(value)
                encodeStringElement(descriptor, 0, serializable.className)
                encodeNullableSerializableElement(descriptor, 1, serializer<String?>(), serializable.message)
                encodeStringElement(descriptor, 2, serializable.stackTrace)
            } else {
                encodeNullableSerializableElement(descriptor, 0, serializer<String?>(), null)
                encodeNullableSerializableElement(descriptor, 1, serializer<String?>(), null)
                encodeNullableSerializableElement(descriptor, 2, serializer<String?>(), null)
            }
        }
    }

    override fun deserialize(decoder: Decoder): Throwable? {
        return decoder.decodeStructure(descriptor) {
            var className: String? = null
            var message: String? = null
            var stackTrace: String? = null

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> className = decodeNullableSerializableElement(descriptor, 0, serializer<String?>())
                    1 -> message = decodeNullableSerializableElement(descriptor, 1, serializer<String?>())
                    2 -> stackTrace = decodeNullableSerializableElement(descriptor, 2, serializer<String?>())
                    -1 -> break
                    else -> error("Unexpected index: $index")
                }
            }

            if (className != null && stackTrace != null) {
                SerializableThrowable(className, message, stackTrace).toThrowable()
            } else {
                null
            }
        }
    }
}
