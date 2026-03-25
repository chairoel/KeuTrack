package com.mascill.keutrack.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

/**
 * Home routing to handle screen that will be showing and to handle view model flow / live data
 * collection in case multiple screen with different condition need to show
 */
@Composable
fun HomeRouting(viewModel: HomeViewModel = hiltViewModel()) {
    HomeScreen()
}

@Composable
private fun HomeScreen() {
    Scaffold { innerPadding ->
        Greeting(
            name = "Android guewww",
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(KeuTrackTheme.primaryColors.primary300)
    ) {
        Text(
            text = "Hello $name!",
            style = KeuTrackTheme.typography.headingBold30,
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingPreview() {
    KeuTrackTheme {
        HomeScreen()
    }
}