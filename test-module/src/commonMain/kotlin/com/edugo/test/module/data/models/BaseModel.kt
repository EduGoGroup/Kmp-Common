package com.edugo.test.module.data.models

import kotlinx.serialization.Serializable

/**
 * Interface base para modelos serializables.
 */
interface BaseModel {
    val id: String
}

/**
 * Ejemplo de modelo serializable.
 */
@Serializable
data class SampleModel(
    override val id: String,
    val name: String,
    val timestamp: Long
) : BaseModel
