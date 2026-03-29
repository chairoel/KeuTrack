package com.mascill.keutrack.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

                Text(
                    text = "Track your finances easily",
                    style = KeuTrackTheme.typography.bodyRegular16,
                    color = KeuTrackTheme.textColors.body
                )

                // Tampilkan pesan error jika ada
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = KeuTrackTheme.dangerColors.d500,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = onSignOutClick,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = KeuTrackTheme.contentColors.formInput,
                        contentColor = KeuTrackTheme.textColors.title
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .padding(horizontal = 32.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = KeuTrackTheme.textColors.title,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Sign Out Icon",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(
                            "Sign Out",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
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