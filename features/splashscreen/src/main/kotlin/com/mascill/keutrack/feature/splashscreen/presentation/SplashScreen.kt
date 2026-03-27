package com.mascill.keutrack.feature.splashscreen.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.splashscreen.R
import com.mascill.keutrack.feature.splashscreen.presentation.model.NavigationState

/**
 * Splash routing to handle screen that will be showing and to handle view model flow / live data
 * collection in case multiple screen with different condition need to show
 */
@Composable
fun SplashRouting(
    navToHome: () -> Unit,
    navToAuth: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val splashUIState by viewModel.splashUIState.collectAsStateWithLifecycle()
    val isInit = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isInit) {
        if (!isInit.value) {
            isInit.value = true
            viewModel.checkOnGoingNavigation()
        }
    }

    HandleStartingScreen(
        navigationState = splashUIState.navigationState,
        navToHome = navToHome,
        navToAuth = navToAuth
    )

    SplashScreen()
}

/**
 * Consumes a [NavigationState] and invokes the appropriate navigation callback.
 */
@Composable
private fun HandleStartingScreen(
    navigationState: NavigationState,
    navToHome: () -> Unit,
    navToAuth: () -> Unit,
) {
    when (navigationState) {
        is NavigationState.NavigateToHome -> navToHome()
        is NavigationState.NavigateToAuth -> navToAuth()
        is NavigationState.Idle -> { /* Do nothing, wait for user observation */ }
    }
}

@Composable
private fun SplashScreen() {
    Scaffold { innerPadding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(color = KeuTrackTheme.contentColors.pageColor)
                .padding(innerPadding)
        ) {
            Image(
                painter = painterResource(id = R.drawable.icon_launcher),
                contentDescription = "",
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.Center,
            )
        }
    }
}

@Preview(
    name = "Portrait",
    showBackground = true
)
@Composable
private fun SplashScreenPreview() {
    KeuTrackTheme {
        SplashScreen()
    }
}