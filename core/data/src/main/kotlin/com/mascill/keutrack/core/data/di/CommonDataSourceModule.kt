package com.mascill.keutrack.core.data.di

import com.mascill.keutrack.core.data.datasource.AuthNetworkDataSource
import com.mascill.keutrack.core.data.datasource.AuthNetworkDataSourceImpl
import com.mascill.keutrack.core.data.datasource.PeriodPreferencesLocalDataSource
import com.mascill.keutrack.core.data.datasource.PeriodPreferencesLocalDataSourceImpl
import com.mascill.keutrack.core.data.datasource.UserProfileLocalDataSource
import com.mascill.keutrack.core.data.datasource.UserProfileLocalDataSourceImpl
import com.mascill.keutrack.core.data.datasource.WalletUiPreferencesLocalDataSource
import com.mascill.keutrack.core.data.datasource.WalletUiPreferencesLocalDataSourceImpl
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
    fun bindAuthNetworkDataSource(
        authNetworkDataSourceImpl: AuthNetworkDataSourceImpl
    ): AuthNetworkDataSource

    @Binds
    fun bindUserProfileLocalDataSource(
        impl: UserProfileLocalDataSourceImpl
    ): UserProfileLocalDataSource

    @Binds
    fun bindWalletUiPreferencesLocalDataSource(
        impl: WalletUiPreferencesLocalDataSourceImpl,
    ): WalletUiPreferencesLocalDataSource

    @Binds
    fun bindPeriodPreferencesLocalDataSource(
        impl: PeriodPreferencesLocalDataSourceImpl,
    ): PeriodPreferencesLocalDataSource
}