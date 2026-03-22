package com.mascill.keutrack

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Base class for maintaining global application state.
 *
 * @see Application
 */
@HiltAndroidApp
class KeuTrackApp : Application() {
}
