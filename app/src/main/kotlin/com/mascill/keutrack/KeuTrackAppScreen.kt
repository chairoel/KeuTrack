package com.mascill.keutrack

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
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
    val darkTheme = isSystemInDarkTheme()
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
    KeuTrackTheme(darkTheme = darkTheme) {
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
