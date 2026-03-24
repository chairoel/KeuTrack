package com.mascill.keutrack.core.network.model

/**
 * Generic network response for any type responses [T].
 *
 * @param status A string description of the call status.
 * @param message A more descriptive message of the failure call status.
 * @param data The results returned by the call.
 */
data class BaseSuccessResponse<T>(
    val status: String?,
    val message: String? = "",
    val data: T?
)

