package com.mascill.keutrack.core.designsystem.model

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mascill.keutrack.core.designsystem.component.snackbar.model.KeuTrackSnackbarDuration

/**
 * UI model for a KeuTrack snackbar.
 *
 * @param message snackbar text
 * @param actionLabel trailing action text such as "X" or "Tutup"
 * @param duration how long the snackbar stays visible
 * @param tone default icon and colors when [icon], [backgroundColor], or [textColor] are omitted
 * @param icon leading icon; falls back to [tone] default
 * @param margin outer inset around the snackbar
 * @param backgroundColor snackbar surface; falls back to [tone] default
 * @param textColor color for icon, message, and action; falls back to [tone] default
 */
@Immutable
data class KeuTrackSnackbarData(
    val message: String,
    val actionLabel: String? = null,
    val duration: KeuTrackSnackbarDuration = KeuTrackSnackbarDuration.Short,
    val tone: KeuTrackSnackbarTone = KeuTrackSnackbarTone.Danger,
    val icon: ImageVector? = null,
    val margin: PaddingValues? = null,
    val backgroundColor: Color? = null,
    val textColor: Color? = null,
)
