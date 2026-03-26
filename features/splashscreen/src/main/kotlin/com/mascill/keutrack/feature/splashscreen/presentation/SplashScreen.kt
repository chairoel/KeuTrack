package com.mascill.keutrack.feature.splashscreen.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.splashscreen.R

/**
 * Splash routing to handle screen that will be showing and to handle view model flow / live data
 * collection in case multiple screen with different condition need to show
 */
@Composable
fun SplashRouting(
    navToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val isFetchConfig by viewModel.isFetchConfig.collectAsStateWithLifecycle(true)

    if (!isFetchConfig) navToHome.invoke()

    SplashScreen()
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
    name = "Landscape",
    widthDp = 640,
    heightDp = 360,
    showBackground = true
)
@Composable
private fun SplashScreenPreview() {
    KeuTrackTheme {
        SplashScreen()
    }
}