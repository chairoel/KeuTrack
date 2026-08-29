package com.mascill.keutrack.feature.splashscreen.presentation

import android.app.Activity
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.splashscreen.R
import com.mascill.keutrack.feature.splashscreen.presentation.model.NavigationState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val STATUS_BAR_INSET_TIMEOUT_MS = 200L

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
    val view = LocalView.current
    val density = LocalDensity.current
    val statusBars = WindowInsets.statusBars

    DisposableEffect(view) {
        val controller = statusBarController(view)
        controller?.hide(WindowInsetsCompat.Type.statusBars())
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    LaunchedEffect(isInit) {
        if (!isInit.value) {
            isInit.value = true
            viewModel.checkOnGoingNavigation()
        }
    }

    LaunchedEffect(splashUIState.navigationState) {
        val navigate = when (splashUIState.navigationState) {
            NavigationState.NavigateToHome -> navToHome
            NavigationState.NavigateToAuth -> navToAuth
            NavigationState.Idle -> return@LaunchedEffect
        }
        statusBarController(view)?.show(WindowInsetsCompat.Type.statusBars())
        withTimeoutOrNull(STATUS_BAR_INSET_TIMEOUT_MS) {
            snapshotFlow { statusBars.getTop(density) }.first { it > 0 }
        }
        navigate()
    }

    SplashScreen()
}

private fun statusBarController(view: View) =
    (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }

@Composable
private fun SplashScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(color = KeuTrackTheme.contentColors.pageColor)
    ) {
        Image(
            painter = painterResource(id = R.drawable.icon_launcher),
            contentDescription = "",
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.Center,
        )
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