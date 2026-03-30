package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.TokenResult

interface GoogleAuthRepository {
    suspend fun getGoogleIdToken(): TokenResult

}
