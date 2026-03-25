package com.mascill.keutrack.core.network.model

import com.squareup.moshi.Json

/**
 * Generic network response for any type responses [T].
 *
 * @param status A string description of the call status.
 * @param message A more descriptive message of the failure call status.
 * @param data The results returned by the call.
 */
data class BaseSuccessResponse<T>(
    @Json(name = "status")
    val status: String?,
    @Json(name = "message")
    val message: String? = "",
    @Json(name = "data")
    val data: T?
)

