package com.mascill.keutrack.core.data.di

import com.mascill.keutrack.core.data.mapper.AuthUserMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt Module class that contributes to the object graph [SingletonComponent] to provides
 * Common data Mapper that can be used in every screen.
 *
 * @see Module
 */
@Module
@InstallIn(SingletonComponent::class)
class CommonMapperModule {
    /**
     * Create a provider method binding for [AuthUserMapper].
     *
     * @return Instance of auth related data mapper.
     * @see Provides
     */
    @Provides
    fun provideAuthUserMapper(): AuthUserMapper = AuthUserMapper()
}