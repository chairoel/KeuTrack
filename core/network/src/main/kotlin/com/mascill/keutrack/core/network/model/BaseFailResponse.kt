package com.mascill.keutrack.core.network.model

import com.squareup.moshi.Json

/**
 * Base failed response
 */
data class BaseFailResponse(
    @Json(name = "message")
    val message: String? = ""
)

