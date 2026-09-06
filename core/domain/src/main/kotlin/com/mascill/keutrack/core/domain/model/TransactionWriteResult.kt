package com.mascill.keutrack.core.domain.model

sealed class TransactionWriteResult {
    data object Success : TransactionWriteResult()

    sealed class Error : TransactionWriteResult() {
        data object MissingId : Error()
        data object InvalidAmount : Error()
        data object MissingWallet : Error()
        data object MissingCategory : Error()
        data object NotFound : Error()
        data class Unknown(val cause: Throwable) : Error()
    }
}
