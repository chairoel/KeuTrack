package com.mascill.keutrack.core.domain.model

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data object Cancelled : AuthResult()

    sealed class Error : AuthResult() {
        data object Network : Error()
        data object InvalidCredential : Error()
        data object UserNotFound : Error()
        data class Unknown(val message: String?) : Error()
    }
}