package com.mascill.keutrack.core.designsystem.component.snackbar.model

import com.mascill.keutrack.core.designsystem.component.snackbar.KeuTrackSnackbar
import com.mascill.keutrack.core.designsystem.component.snackbar.KeuTrackSnackbarHost
import com.mascill.keutrack.core.designsystem.component.snackbar.KeuTrackSnackbarHostState

/**
 * Possible results of the [KeuTrackSnackbarHostState.showSnackbar] call.
 */
enum class KeuTrackSnackbarResult {
    /**
     * [KeuTrackSnackbar] that is shown has been dismissed either by timeout or by user.
     */
    Dismissed,

    /**
     * Action on the [KeuTrackSnackbar] has been clicked before the time out passed.
     */
    ActionPerformed,
}

/**
 * Possible durations of the [KeuTrackSnackbar] in [KeuTrackSnackbarHost].
 */
enum class KeuTrackSnackbarDuration {
    Short,
    Long,
    Indefinite,
}
