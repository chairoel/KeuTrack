package com.mascill.keutrack.core.network.model

/**
 * Generic response alias for API response
 *
 * @param S Success responses response
 * @param F Failed responses response
 */
typealias GenericResponse<S, F> = NetworkResponse<BaseSuccessResponse<S>, BaseFailResponse>

/**
 * Generic response for API response with List type and meta body data
 * @param S Success data response
 * @param F Failed data response
 */
typealias GenericListResponse<S, F> = NetworkResponse<BaseSuccessListResponse<S>, BaseFailResponse>
