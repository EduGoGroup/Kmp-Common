package com.edugo.test.module.auth.repository

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Respuesta del endpoint /v1/auth/verify.
 * Mapea directamente la estructura del backend.
 */
@Serializable
public data class TokenVerificationResponse(
    @SerialName("valid")
    val valid: Boolean,

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("email")
    val email: String? = null,

    @SerialName("role")
    val role: String? = null,

    @SerialName("school_id")
    val schoolId: String? = null,

    @SerialName("expires_at")
    val expiresAt: String? = null,

    @SerialName("error")
    val error: String? = null
)

/**
 * Request para el endpoint /v1/auth/verify.
 */
@Serializable
public data class TokenVerificationRequest(
    @SerialName("token")
    val token: String
)
