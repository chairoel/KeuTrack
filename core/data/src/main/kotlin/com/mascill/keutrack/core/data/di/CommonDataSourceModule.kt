package com.mascill.keutrack.core.data.di

import com.mascill.keutrack.core.data.datasource.AuthNetworkDataSource
import com.mascill.keutrack.core.data.datasource.AuthNetworkDataSourceImpl
import com.mascill.keutrack.core.data.datasource.GoogleAuthDataSource
import com.mascill.keutrack.core.data.datasource.GoogleAuthDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


/**
 * Hilt Module class that contributes to the object graph [SingletonComponent] to bind interface
 * with real class implementation for Commonly used DataSource.
 *
 * @see Module
 */
@Module
@InstallIn(SingletonComponent::class)
interface CommonDataSourceModule {

    @Binds
    fun bindGoogleAuthDataSource(
        googleAuthDataSourceImpl: GoogleAuthDataSourceImpl
    ): GoogleAuthDataSource

    @Binds
    fun bindAuthNetworkDataSource(
        authNetworkDataSourceImpl: AuthNetworkDataSourceImpl
    ): AuthNetworkDataSource
}