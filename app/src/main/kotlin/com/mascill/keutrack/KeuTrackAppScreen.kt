package com.mascill.keutrack

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.navigation.KeuTrackAppState
import com.mascill.keutrack.navigation.KeuTrackNavHost
import com.mascill.keutrack.navigation.rememberKeuTrackAppState

/**
 * App Main Screen / main activity screen
 */
@Composable
fun KeuTrackAppScreen(appState: KeuTrackAppState = rememberKeuTrackAppState()) {
    KeuTrackTheme {
        Scaffold(
            scaffoldState = appState.scaffoldState,
            snackbarHost = {
                SnackbarHost(hostState = it) { data ->
                    Snackbar(
                        snackbarData = data,
                        actionColor = Color.White,
                        backgroundColor = KeuTrackTheme.dangerColors.d900
                    )
                }
            }
        ) { innerPadding ->
            KeuTrackNavHost(
                modifier = Modifier.padding(innerPadding),
                appState = appState
            )
        }

    }
}