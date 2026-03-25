package com.mascill.keutrack.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Generic network response for any type data [T].
 *
 * @param status A string description of the call status.
 * @param data The results returned by the call.
 */
@JsonClass(generateAdapter = true)
data class BaseSuccessListResponse<T>(
    @Json(name = "data")
    val data: List<T?>?,
    @Json(name = "status")
    val status: String?
)
