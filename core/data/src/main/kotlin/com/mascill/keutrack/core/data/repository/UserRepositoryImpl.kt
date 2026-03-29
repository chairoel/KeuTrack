package com.mascill.keutrack.core.data.repository

import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.RequiresPermission
import com.mascill.keutrack.core.data.datasource.AuthNetworkDataSource
import com.mascill.keutrack.core.data.mapper.AuthUserMapper
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val authDataSource: AuthNetworkDataSource,
    private val connectivityManager: ConnectivityManager,
    private val mapper: AuthUserMapper
) : UserRepository {

    override fun getCurrentUser(): Flow<User?> = flow {
        emit(mapper.mapToDomainOrNull(authDataSource.getCurrentUser()))
    }

    override suspend fun signInWithGoogle(idToken: String): User? {
        val dto = authDataSource.signInWithGoogle(idToken)
        return dto?.let { mapper.mapToDomain(it) }
    }

    @RequiresPermission(ACCESS_NETWORK_STATE)
    override suspend fun signOut() {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val isConnected =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        if (!isConnected) throw IOException("No internet connection")

        authDataSource.signOut()
    }

    override suspend fun syncUserProfile() {
        // TODO: Implement user profile sync to Firestore
    }
}
