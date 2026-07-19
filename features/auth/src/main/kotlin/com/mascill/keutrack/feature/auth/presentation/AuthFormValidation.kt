package com.mascill.keutrack.feature.auth.presentation

import android.util.Patterns

internal object AuthFormValidation {

    fun validateLogin(email: String, password: String): String? {
        val trimmedEmail = email.trim()
        return when {
            trimmedEmail.isBlank() -> "Please enter your email."
            !isValidEmail(trimmedEmail) -> "Please enter a valid email address."
            password.isBlank() -> "Please enter your password."
            else -> null
        }
    }

    fun validateRegister(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): String? {
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim()
        return when {
            trimmedName.isBlank() -> "Please enter your full name."
            trimmedEmail.isBlank() -> "Please enter your email."
            !isValidEmail(trimmedEmail) -> "Please enter a valid email address."
            password.isBlank() -> "Please enter your password."
            password.length < MIN_PASSWORD_LENGTH -> "Password must be at least 6 characters."
            password != confirmPassword -> "Passwords do not match."
            else -> null
        }
    }

    private fun isValidEmail(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private const val MIN_PASSWORD_LENGTH = 6
}
