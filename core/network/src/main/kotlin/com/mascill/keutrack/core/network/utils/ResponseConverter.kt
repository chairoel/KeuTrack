package com.mascill.keutrack.core.network.utils

import com.mascill.keutrack.core.network.model.BaseFailResponse
import com.mascill.keutrack.core.network.model.DomainResult
import com.mascill.keutrack.core.network.model.NetworkResponse

/**
 * Helper function to convert API [NetworkResponse] to [DomainResult]
 *
 * @param S success network response
 * @param E error network response
 * @param D expected domain model
 */
suspend fun <S : Any, E : Any, D : Any> processResponse(
    response: NetworkResponse<S, E>,
    successBlock: suspend (S) -> DomainResult.Success<D>
): DomainResult<D> =
    when (response) {
        is NetworkResponse.Success -> successBlock(response.body)
        is NetworkResponse.ApiError -> {
            // Retrofit API should return [GenericResponse] which use [BaseFailResponse] for error
            // data class converted from error body.
            val body = response.body as? BaseFailResponse

            // Handle 404 http code as empty response
            if (response.httpCode == 404) {
                DomainResult.Empty
            } else {
                DomainResult.ApiError(response.httpCode, body?.message ?: "")
            }
        }

        is NetworkResponse.NetworkError -> DomainResult.NetworkError
        is NetworkResponse.UnknownError -> {
            // It's already handled in [NetworkResponseCall] method that this type of response
            // without error param / error param null, means the data is empty
            if (response.error == null) {
                DomainResult.Empty
            } else {
                DomainResult.UnknownError // for general error
            }
        }
    }

