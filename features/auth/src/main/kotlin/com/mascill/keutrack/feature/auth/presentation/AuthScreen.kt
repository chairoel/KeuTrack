package com.mascill.keutrack.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.component.KeuTrackButtonStyle
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.component.KeuTrackTextField
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.auth.R
import com.mascill.keutrack.feature.auth.presentation.model.AuthState

@Composable
fun AuthRouting(
    viewModel: AuthViewModel = hiltViewModel(),
    navigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val authUIState by viewModel.authUIState.collectAsStateWithLifecycle()

    HandleAuthState(
        authState = authUIState.authState,
        navigateToHome = navigateToHome,
        onStateConsumed = viewModel::resetState
    )

    AuthScreen(
        authState = authUIState.authState,
        onSignInClick = { viewModel.signInWithGoogle(context) },
        onEmailLoginClick = { }
    )
}

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
    onSignInClick: () -> Unit,
    onEmailLoginClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KeuTrackTheme.contentColors.pageColor)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 48.dp)
            ) {
                // --- Brand Identity ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        KeuTrackTheme.primaryColors.primary500,
                                        KeuTrackTheme.primaryColors.primary700
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wallet),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "KeuTrack",
                        style = KeuTrackTheme.typography.headingBold24,
                        color = KeuTrackTheme.primaryColors.primary500,
                        letterSpacing = (-1).sp
                    )
                }

                // --- Welcome Message ---
                Text(
                    text = "Welcome Back",
                    style = KeuTrackTheme.typography.headingBold30,
                    color = KeuTrackTheme.textColors.title,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Step into your financial atelier.",
                    style = KeuTrackTheme.typography.bodyRegular16,
                    color = KeuTrackTheme.textColors.body,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(64.dp))

                KeuTrackCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        KeuTrackTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "Email Address",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_att_24dp),
                                    contentDescription = null,
                                    tint = KeuTrackTheme.semanticColors.onSurfaceVariant
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        KeuTrackTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "Password",
                            visualTransformation =
                                if (passwordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_lock_24),
                                    contentDescription = null,
                                    tint = KeuTrackTheme.semanticColors.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Text(
                                        text = if (passwordVisible) "Hide" else "Show",
                                        style = KeuTrackTheme.typography.bodyBold14,
                                        color = KeuTrackTheme.semanticColors.onSurfaceVariant
                                    )
                                }
                            }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                        ) {
                            Text(
                                text = "Forgot Password?",
                                style = KeuTrackTheme.typography.bodyBold14,
                                color = KeuTrackTheme.primaryColors.primary500
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        KeuTrackButton(
                            text = "Login",
                            onClick = onEmailLoginClick,
                            style = KeuTrackButtonStyle.Primary,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Divider(
                                modifier = Modifier.weight(1f),
                                color = KeuTrackTheme.semanticColors.outlineVariantGhost
                            )
                            Text(
                                text = "OR",
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = KeuTrackTheme.typography.bodyBold14,
                                color = KeuTrackTheme.semanticColors.onSurfaceVariant
                            )
                            Divider(
                                modifier = Modifier.weight(1f),
                                color = KeuTrackTheme.semanticColors.outlineVariantGhost
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        KeuTrackButton(
                            text = "Login with Google",
                            onClick = onSignInClick,
                            style = KeuTrackButtonStyle.Secondary,
                            isLoading = authState is AuthState.Loading || authState is AuthState.Success,
                            modifier = Modifier.fillMaxWidth(),
                            leading = {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_info_details),
                                    contentDescription = "Google Icon",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.Unspecified
                                )
                            }
                        )

                        if (authState is AuthState.Error) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = authState.message,
                                color = KeuTrackTheme.dangerColors.d500,
                                style = KeuTrackTheme.typography.bodyRegular14
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // --- Footer & Decorative Elements ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row {
                        Text(
                            text = "Don't have an account?",
                            style = KeuTrackTheme.typography.bodyRegular14,
                            color = KeuTrackTheme.textColors.body
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Register",
                            style = KeuTrackTheme.typography.bodyBold14,
                            color = KeuTrackTheme.primaryColors.primary500
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Auth Screen - Idle")
@Composable
fun AuthScreenPreview() {
    KeuTrackTheme {
        AuthScreen(
            authState = AuthState.Idle,
            onSignInClick = {},
            onEmailLoginClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Auth Screen - Loading")
@Composable
fun AuthScreenLoadingPreview() {
    KeuTrackTheme {
        AuthScreen(
            authState = AuthState.Loading,
            onSignInClick = {},
            onEmailLoginClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Auth Screen - Error")
@Composable
fun AuthScreenErrorPreview() {
    KeuTrackTheme {
        AuthScreen(
            authState = AuthState.Error("Maaf, koneksi internet bermasalah. Silakan coba lagi."),
            onSignInClick = {},
            onEmailLoginClick = {}
        )
    }
}