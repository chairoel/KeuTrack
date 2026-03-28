package com.mascill.keutrack.core.data.di

import com.mascill.keutrack.core.data.repository.UserRepositoryImpl
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.data.repository.GoogleAuthRepositoryImpl
import com.mascill.keutrack.core.data.datasource.GoogleAuthDataSource
import com.mascill.keutrack.core.data.datasource.GoogleAuthDataSourceImpl
import com.mascill.keutrack.core.domain.repository.GoogleAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    fun bindGoogleAuthDataSource(
        googleAuthDataSourceImpl: GoogleAuthDataSourceImpl
    ): GoogleAuthDataSource

    @Binds
    @Singleton
    fun bindGoogleAuthRepository(
        googleAuthRepositoryImpl: GoogleAuthRepositoryImpl
    ): GoogleAuthRepository
}
