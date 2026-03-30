package com.mascill.keutrack.core.domain.model

sealed class TokenResult {
    data class Success(val idToken: String) : TokenResult()
    data object Cancelled : TokenResult()

    sealed class Error : TokenResult() {
        data object Network : Error()
        data object NoCredential : Error()
        data class Unknown(val message: String?, val cause: Throwable? = null) : Error()
    }
}