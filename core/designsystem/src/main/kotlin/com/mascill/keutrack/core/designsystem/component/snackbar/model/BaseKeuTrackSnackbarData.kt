package com.mascill.keutrack.core.designsystem.component.snackbar.model

import androidx.compose.ui.Alignment
import com.mascill.keutrack.core.designsystem.component.snackbar.KeuTrackSnackbarHostState
import com.mascill.keutrack.core.designsystem.model.KeuTrackSnackbarData

/**
 * One snackbar currently owned by [KeuTrackSnackbarHostState].
 */
interface BaseKeuTrackSnackbarData {
    val position: Alignment
    val data: KeuTrackSnackbarData

    fun performAction()

    fun dismiss()
}
