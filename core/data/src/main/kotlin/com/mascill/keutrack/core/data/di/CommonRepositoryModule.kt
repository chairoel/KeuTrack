package com.mascill.keutrack.core.data.di

import com.mascill.keutrack.core.data.repository.GoogleAuthRepositoryImpl
import com.mascill.keutrack.core.data.repository.UserRepositoryImpl
import com.mascill.keutrack.core.domain.repository.GoogleAuthRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface CommonRepositoryModule {

    @Binds
    fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    fun bindGoogleAuthRepository(
        googleAuthRepositoryImpl: GoogleAuthRepositoryImpl
    ): GoogleAuthRepository
}
