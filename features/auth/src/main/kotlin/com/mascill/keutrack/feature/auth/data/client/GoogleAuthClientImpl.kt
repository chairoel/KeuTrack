package com.mascill.keutrack.feature.auth.data.client

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.mascill.keutrack.feature.auth.domain.client.GoogleAuthClient
import com.mascill.keutrack.feature.auth.domain.client.GoogleSignInResult
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class GoogleAuthClientImpl @Inject constructor() : GoogleAuthClient {
    override suspend fun signIn(context: Context): GoogleSignInResult {
        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("100547827166-uqtn9is2df1k931808lm6ff8i79ns988.apps.googleusercontent.com")
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    GoogleSignInResult(idToken = idToken, error = null)
                } catch (e: GoogleIdTokenParsingException) {
                    Log.e("GoogleAuthClient", "Received an invalid google id token response", e)
                    GoogleSignInResult(idToken = null, error = "Invalid token response")
                }
            } else {
                Log.e("GoogleAuthClient", "Unexpected type of credential")
                GoogleSignInResult(idToken = null, error = "Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult(idToken = null, error = null) // Map cancellation to null/null
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuthClient", "GetCredentialException: ", e)
            GoogleSignInResult(idToken = null, error = e.message ?: "Sign in failed")
        } catch (e: CancellationException) {
            throw e // Propagate coroutine cancellation
        } catch (e: Exception) {
            Log.e("GoogleAuthClient", "General Exception: ", e)
            GoogleSignInResult(idToken = null, error = e.message ?: "An unexpected error occurred")
        }
    }
}
