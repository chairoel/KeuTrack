package com.mascill.keutrack.feature.auth.di

import com.mascill.keutrack.feature.auth.data.client.GoogleAuthClientImpl
import com.mascill.keutrack.feature.auth.domain.client.GoogleAuthClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindGoogleAuthClient(
        googleAuthClientImpl: GoogleAuthClientImpl
    ): GoogleAuthClient
}
