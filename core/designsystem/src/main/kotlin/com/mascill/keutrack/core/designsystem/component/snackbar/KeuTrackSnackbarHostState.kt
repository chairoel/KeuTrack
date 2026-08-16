package com.mascill.keutrack.core.designsystem.component.snackbar

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.mascill.keutrack.core.designsystem.component.snackbar.model.BaseKeuTrackSnackbarData
import com.mascill.keutrack.core.designsystem.component.snackbar.model.KeuTrackSnackbarResult
import com.mascill.keutrack.core.designsystem.model.KeuTrackSnackbarData
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Host state for [KeuTrackSnackbar]. Only one snackbar is shown at a time; callers queue on a mutex.
 */
@Stable
class KeuTrackSnackbarHostState {
    private val mutex = Mutex()

    var currentSnackbarData by mutableStateOf<BaseKeuTrackSnackbarData?>(null)
        private set

    /**
     * Shows a snackbar described by [data] at [position], suspending until it is dismissed
     * or the action is used.
     */
    suspend fun showSnackbar(
        data: KeuTrackSnackbarData,
        position: Alignment = Alignment.BottomCenter,
    ): KeuTrackSnackbarResult = mutex.withLock {
        try {
            return suspendCancellableCoroutine { continuation ->
                currentSnackbarData = BaseKeuTrackSnackbarDataImpl(
                    position = position,
                    data = data,
                    continuation = continuation,
                )
            }
        } finally {
            currentSnackbarData = null
        }
    }

    @Stable
    private class BaseKeuTrackSnackbarDataImpl(
        override val position: Alignment,
        override val data: KeuTrackSnackbarData,
        private val continuation: CancellableContinuation<KeuTrackSnackbarResult>,
    ) : BaseKeuTrackSnackbarData {

        override fun performAction() {
            if (continuation.isActive) continuation.resume(KeuTrackSnackbarResult.ActionPerformed)
        }

        override fun dismiss() {
            if (continuation.isActive) continuation.resume(KeuTrackSnackbarResult.Dismissed)
        }
    }
}
