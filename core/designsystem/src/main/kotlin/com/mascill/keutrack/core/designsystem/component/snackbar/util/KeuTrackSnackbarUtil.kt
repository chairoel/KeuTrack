package com.mascill.keutrack.core.designsystem.component.snackbar.util

import androidx.compose.ui.platform.AccessibilityManager
import com.mascill.keutrack.core.designsystem.component.snackbar.model.KeuTrackSnackbarDuration

private const val LONG_DURATION_MS = 10_000L
private const val SHORT_DURATION_MS = 3_000L

/**
 * Converts [KeuTrackSnackbarDuration] to millis, stretching for accessibility timeout when needed.
 */
internal fun KeuTrackSnackbarDuration.toMillis(
    hasAction: Boolean,
    accessibilityManager: AccessibilityManager?,
): Long {
    val original = when (this) {
        KeuTrackSnackbarDuration.Indefinite -> Long.MAX_VALUE
        KeuTrackSnackbarDuration.Long -> LONG_DURATION_MS
        KeuTrackSnackbarDuration.Short -> SHORT_DURATION_MS
    }
    if (accessibilityManager == null) {
        return original
    }
    return accessibilityManager.calculateRecommendedTimeoutMillis(
        original,
        containsIcons = true,
        containsText = true,
        containsControls = hasAction,
    )
}
