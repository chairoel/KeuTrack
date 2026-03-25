package com.mascill.keutrack.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Base failed response
 */
@JsonClass(generateAdapter = true)
data class BaseFailResponse(
    @Json(name = "message")
    val message: String? = ""
)

