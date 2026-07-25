package com.mascill.keutrack

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Base class for maintaining global application state.
 *
 * Implements [Configuration.Provider] so WorkManager uses [HiltWorkerFactory]
 * for Phase 2 financial sync workers.
 *
 * @see Application
 */
@HiltAndroidApp
class KeuTrackApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
