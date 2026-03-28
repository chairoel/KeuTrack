package com.mascill.keutrack.core.common.di

import com.mascill.keutrack.core.common.BuildConfig
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class Dispatcher(val keuTrackDispatcher: KeuTrackDispatcher)

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class Environment

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class BuildTypeDebug

enum class KeuTrackDispatcher {
    IO,
    Main,
    Computation
}

/**
 * Module Class that provide dispatcher for hilt DI
 */
@Module
@InstallIn(SingletonComponent::class)
object CommonGlobalModule {
    @Dispatcher(KeuTrackDispatcher.IO)
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Dispatcher(KeuTrackDispatcher.Main)
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Dispatcher(KeuTrackDispatcher.Computation)
    @Provides
    fun provideComputationDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /**
     * function that provide coroutine custom dispatcher (ex: io, main)
     */
    @Singleton
    @Provides
    fun provideDispatcher(
        @Dispatcher(KeuTrackDispatcher.IO) ioDispatcher: CoroutineDispatcher,
        @Dispatcher(KeuTrackDispatcher.Main) mainDispatcher: CoroutineDispatcher,
        @Dispatcher(KeuTrackDispatcher.Computation) computationDispatcher: CoroutineDispatcher
    ) = CommonDispatcher(
        io = ioDispatcher,
        main = mainDispatcher,
        computation = computationDispatcher
    )

    /**
     * Create a provider method binding for Application Scope, used for fire and forget background
     * task
     *
     * @see Provides
     */
    @Singleton
    @Provides
    @ApplicationScope
    fun provideApplicationScope(
        dispatcher: CommonDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher.io)

    /**
     * Create a provider method binding for keutrack environment
     */
    @Environment
    @Provides
    fun provideEnvironment(): String = BuildConfig.FLAVOR

    /**
     * Create a provider method binding for keutrack to check if BuildType isDebug or not
     */
    @BuildTypeDebug
    @Provides
    fun provideBuildTypeDebug(): Boolean = BuildConfig.DEBUG

}