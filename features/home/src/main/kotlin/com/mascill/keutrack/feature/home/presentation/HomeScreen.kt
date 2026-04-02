package com.mascill.keutrack.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.component.KeuTrackButtonStyle
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.home.presentation.model.SignOutState

/**
 * Home routing to handle screen that will be showing and to handle view model flow / live data
 * collection in case multiple screen with different condition need to show
 */
@Composable
fun HomeRouting(
    viewModel: HomeViewModel = hiltViewModel(),
    onSignOutSuccess: () -> Unit = {}
) {

    val signOutState by viewModel.signOutState.collectAsStateWithLifecycle()

    // Handle navigasi saat sign out sukses
    LaunchedEffect(signOutState) {
        if (signOutState is SignOutState.Success) {
            onSignOutSuccess()
        }
    }

    HomeScreen(
        signOutState = signOutState,
        onSignOutClick = { viewModel.signOut() }
    )
}

@Composable
private fun HomeScreen(
    signOutState: SignOutState = SignOutState.Idle,
    onSignOutClick: () -> Unit
) {
    val isLoading = signOutState is SignOutState.Loading
    val errorMessage = (signOutState as? SignOutState.Error)?.message

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KeuTrackTheme.contentColors.pageColor)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                KeuTrackCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    Text(
                        text = "Track your finances easily",
                        style = KeuTrackTheme.typography.bodyRegular16,
                        color = KeuTrackTheme.textColors.body
                    )
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = KeuTrackTheme.dangerColors.d500,
                            style = KeuTrackTheme.typography.bodyRegular14
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                KeuTrackButton(
                    text = "Sign Out",
                    onClick = onSignOutClick,
                    enabled = !isLoading,
                    style = KeuTrackButtonStyle.Secondary,
                    isLoading = isLoading,
                    leading = {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Sign Out Icon",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    KeuTrackTheme {
        HomeScreen(onSignOutClick = {})
    }
}