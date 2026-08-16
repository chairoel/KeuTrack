package com.mascill.keutrack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mascill.keutrack.core.designsystem.component.snackbar.KeuTrackSnackbarOverlay
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
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                KeuTrackNavHost(
                    modifier = Modifier.fillMaxSize(),
                    appState = appState,
                )
                KeuTrackSnackbarOverlay(hostState = appState.snackbarHostState)
            }
        }
    }
}
