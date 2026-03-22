package com.mascill.keutrack.core.common.di

import com.mascill.keutrack.core.common.utils.CommonDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class Dispatcher(val keuTrackDispatcher: KeuTrackDispatcher)

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
object DispatcherModule {
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
}