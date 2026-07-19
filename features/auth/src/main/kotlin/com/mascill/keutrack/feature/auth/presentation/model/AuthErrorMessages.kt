package com.mascill.keutrack.feature.auth.presentation.model

internal fun unknownAuthErrorMessage(message: String?, environment: String): String {
    return if (environment == DEV_FLAVOR) {
        message?.takeIf { it.isNotBlank() } ?: DEFAULT_UNKNOWN_ERROR
    } else {
        DEFAULT_UNKNOWN_ERROR
    }
}

private const val DEV_FLAVOR = "dev"
private const val DEFAULT_UNKNOWN_ERROR = "An unexpected error occurred."
