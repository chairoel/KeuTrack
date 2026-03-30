package com.mascill.keutrack.core.data.repository

import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.mascill.keutrack.core.data.datasource.GoogleAuthDataSource
import com.mascill.keutrack.core.domain.model.TokenResult
import com.mascill.keutrack.core.domain.repository.GoogleAuthRepository
import java.util.concurrent.CancellationException
import javax.inject.Inject

class GoogleAuthRepositoryImpl @Inject constructor(
    private val googleAuthDataSource: GoogleAuthDataSource
) : GoogleAuthRepository {

    override suspend fun getGoogleIdToken(): TokenResult {
        return try {
            val idToken = googleAuthDataSource.getGoogleIdToken()
            TokenResult.Success(idToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            TokenResult.Cancelled
        } catch (e: NoCredentialException) {
            TokenResult.Error.NoCredential
        } catch (e: GetCredentialException) {
            if (e.message?.contains("network", ignoreCase = true) == true) {
                TokenResult.Error.Network
            } else {
                TokenResult.Error.Unknown(e.message, e)
            }
        } catch (e: Exception) {
            TokenResult.Error.Unknown(e.message, e)
        }
    }
}
