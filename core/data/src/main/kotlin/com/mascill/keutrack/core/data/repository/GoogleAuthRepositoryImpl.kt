package com.mascill.keutrack.core.data.repository

import com.mascill.keutrack.core.data.datasource.GoogleAuthDataSource
import com.mascill.keutrack.core.domain.model.GoogleSignInResult
import com.mascill.keutrack.core.domain.repository.GoogleAuthRepository
import javax.inject.Inject

class GoogleAuthRepositoryImpl @Inject constructor(
    private val googleAuthDataSource: GoogleAuthDataSource
) : GoogleAuthRepository {
    override suspend fun getGoogleIdToken(): GoogleSignInResult {
        return googleAuthDataSource.getGoogleIdToken()
    }
}
