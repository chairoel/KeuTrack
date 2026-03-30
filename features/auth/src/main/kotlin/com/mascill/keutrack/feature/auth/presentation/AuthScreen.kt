package com.mascill.keutrack.feature.auth.presentation

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.auth.presentation.model.AuthState

@Composable
fun AuthRouting(
    viewModel: AuthViewModel = hiltViewModel(),
    navigateToHome: () -> Unit
) {
    val authUIState by viewModel.authUIState.collectAsStateWithLifecycle()

    HandleAuthState(
        authState = authUIState.authState,
        navigateToHome = navigateToHome,
        onStateConsumed = viewModel::resetState
    )

    AuthScreen(
        authState = authUIState.authState,
        onSignInClick = { viewModel.signInWithGoogle() }
    )
}

/**
 * Consumes an [AuthState] and invokes the appropriate navigation callback.
 */
@Composable
private fun HandleAuthState(
    authState: AuthState,
    navigateToHome: () -> Unit,
    onStateConsumed: () -> Unit
) {
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            navigateToHome()
            onStateConsumed()
        }
    }
}

@Composable
fun AuthScreen(
    authState: AuthState,
    onSignInClick: () -> Unit
) {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KeuTrackTheme.contentColors.pageColor)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (authState) {
                is AuthState.Loading, is AuthState.Success -> {
                    CircularProgressIndicator(color = KeuTrackTheme.primaryColors.primary500)
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Welcome to KeuTrack",
                            style = KeuTrackTheme.typography.headingBold30,
                            color = KeuTrackTheme.textColors.title
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Track your finances easily",
                            style = KeuTrackTheme.typography.bodyRegular16,
                            color = KeuTrackTheme.textColors.body
                        )
                        Spacer(modifier = Modifier.height(48.dp))

                        Button(
                            onClick = onSignInClick,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = KeuTrackTheme.contentColors.formInput,
                                contentColor = KeuTrackTheme.textColors.title
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(56.dp).padding(horizontal = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle, // Placeholder
                                contentDescription = "Google Icon",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(
                                "Sign in with Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        if (authState is AuthState.Error) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = authState.message,
                                color = KeuTrackTheme.dangerColors.d500,
                                style = KeuTrackTheme.typography.bodyRegular16
                            )
                        }
                    }
                }
            }
        }
    }
}