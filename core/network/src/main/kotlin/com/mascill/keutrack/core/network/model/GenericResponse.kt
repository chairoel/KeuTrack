package com.mascill.keutrack.core.network.model

/**
 * Generic response alias for API response
 *
 * @param S Success responses response
 * @param F Failed responses response
 */
typealias GenericResponse<S, F> = NetworkResponse<BaseSuccessResponse<S>, BaseFailResponse>
