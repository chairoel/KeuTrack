package com.mascill.keutrack.core.network.model

/**
 * Generic domain result
 *
 * @param D success domain model
 */
sealed class DomainResult<out D : Any> {
    /**
     * Success domain result with body
     */
    data class Success<D : Any>(val data: D) : DomainResult<D>()

    /**
     * Empty domain result
     */
    object Empty : DomainResult<Nothing>()

    /**
     * Failure domain result
     */
    data class ApiError(val httpCode: Int, val message: String) : DomainResult<Nothing>()

    /**
     * Network / internet connection error
     */
    object NetworkError : DomainResult<Nothing>()

    /**
     * Any Exception error (except IOException). For example, json parsing error
     */
    object UnknownError : DomainResult<Nothing>()
}