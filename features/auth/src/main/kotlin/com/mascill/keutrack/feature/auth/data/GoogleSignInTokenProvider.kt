package com.mascill.keutrack.feature.auth.data

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mascill.keutrack.core.domain.model.TokenResult
import com.mascill.keutrack.core.network.utils.NetworkNativeWrapper
import java.util.concurrent.CancellationException
import javax.inject.Inject

class GoogleSignInTokenProvider @Inject constructor(
    private val nativeWrapper: NetworkNativeWrapper
) {
    suspend fun getGoogleIdToken(context: Context): TokenResult {
        val serverClientId = nativeWrapper.getGoogleServerClientId()
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val result = CredentialManager.create(context)
                .getCredential(request = request, context = context)
            extractIdToken(result.credential)
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

    private fun extractIdToken(credential: Credential): TokenResult {
        return if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            TokenResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            TokenResult.Error.Unknown("Unexpected credential type")
        }
    }
}
