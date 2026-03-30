package com.mascill.keutrack.core.data.datasource

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mascill.keutrack.core.network.utils.NetworkNativeWrapper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CancellationException
import javax.inject.Inject

class GoogleAuthDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nativeWrapper: NetworkNativeWrapper
) : GoogleAuthDataSource {
    private val credentialManager = CredentialManager.create(context)

    override suspend fun getGoogleIdToken(): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(nativeWrapper.getGoogleServerClientId())
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(request = request, context = context)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                throw IllegalStateException("Unexpected credential type")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("GoogleAuthDataSource", "getGoogleIdToken failed", e)
            throw e
        }
    }
}