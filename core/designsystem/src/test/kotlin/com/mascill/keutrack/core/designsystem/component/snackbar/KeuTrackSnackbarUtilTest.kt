package com.mascill.keutrack.core.designsystem.component.snackbar

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.designsystem.component.snackbar.model.KeuTrackSnackbarDuration
import com.mascill.keutrack.core.designsystem.component.snackbar.util.toMillis
import org.junit.Test

class KeuTrackSnackbarUtilTest {

    @Test
    fun `short duration is 4 seconds without accessibility manager`() {
        assertThat(KeuTrackSnackbarDuration.Short.toMillis(hasAction = false, accessibilityManager = null))
            .isEqualTo(4_000L)
    }

    @Test
    fun `long duration is 10 seconds without accessibility manager`() {
        assertThat(KeuTrackSnackbarDuration.Long.toMillis(hasAction = true, accessibilityManager = null))
            .isEqualTo(10_000L)
    }

    @Test
    fun `indefinite duration is max long without accessibility manager`() {
        assertThat(
            KeuTrackSnackbarDuration.Indefinite.toMillis(hasAction = false, accessibilityManager = null),
        ).isEqualTo(Long.MAX_VALUE)
    }
}
