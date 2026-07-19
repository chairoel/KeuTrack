package com.mascill.keutrack.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.component.KeuTrackTextField
import com.mascill.keutrack.core.designsystem.model.KeuTrackButtonStyle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.auth.R
import com.mascill.keutrack.feature.auth.presentation.model.AuthMethod
import com.mascill.keutrack.feature.auth.presentation.model.AuthState
import com.mascill.keutrack.feature.auth.presentation.model.isBusy
import com.mascill.keutrack.feature.auth.presentation.model.isLoading

@Composable
fun RegisterRouting(
    viewModel: RegisterViewModel = hiltViewModel(),
    navigateToHome: () -> Unit,
    navigateToLogin: () -> Unit,
) {
    val context = LocalContext.current
    val authUIState by viewModel.authUIState.collectAsStateWithLifecycle()

    HandleRegisterAuthState(
        authState = authUIState.authState,
        navigateToHome = navigateToHome,
        onStateConsumed = viewModel::resetState
    )

    RegisterScreen(
        authState = authUIState.authState,
        onSignUpClick = { fullName, email, password, confirmPassword ->
            val validationError = AuthFormValidation.validateRegister(
                fullName = fullName,
                email = email,
                password = password,
                confirmPassword = confirmPassword,
            )
            if (validationError != null) {
                viewModel.showError(validationError)
            } else {
                viewModel.registerWithEmail(fullName.trim(), email.trim(), password)
            }
        },
        onSignInWithGoogleClick = { viewModel.signInWithGoogle(context) },
        onLoginClick = navigateToLogin,
    )
}

@Composable
private fun HandleRegisterAuthState(
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
fun RegisterScreen(
    authState: AuthState,
    onSignUpClick: (
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
    ) -> Unit,
    onSignInWithGoogleClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    val isBusy = authState.isBusy()
    val isEmailLoading = authState.isLoading(AuthMethod.Email)
    val isGoogleLoading = authState.isLoading(AuthMethod.Google)
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 48.dp)
            ) {
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

                Text(
                    text = "Create Account",
                    style = KeuTrackTheme.typography.headingBold30,
                    color = KeuTrackTheme.textColors.title,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Join the atelier of financial precision",
                    style = KeuTrackTheme.typography.bodyRegular16,
                    color = KeuTrackTheme.textColors.body,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                KeuTrackCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        KeuTrackTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            placeholder = "Full Name",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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

                        Spacer(modifier = Modifier.height(12.dp))

                        KeuTrackTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = "Confirm Password",
                            visualTransformation =
                                if (confirmPasswordVisible) {
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
                                IconButton(
                                    onClick = { confirmPasswordVisible = !confirmPasswordVisible }
                                ) {
                                    Text(
                                        text = if (confirmPasswordVisible) "Hide" else "Show",
                                        style = KeuTrackTheme.typography.bodyBold14,
                                        color = KeuTrackTheme.semanticColors.onSurfaceVariant
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        KeuTrackButton(
                            text = "Sign Up",
                            onClick = {
                                onSignUpClick(fullName, email, password, confirmPassword)
                            },
                            style = KeuTrackButtonStyle.Primary,
                            enabled = !isBusy || isEmailLoading,
                            isLoading = isEmailLoading,
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
                            text = "Sign Up with Google",
                            onClick = onSignInWithGoogleClick,
                            style = KeuTrackButtonStyle.Secondary,
                            enabled = !isBusy || isGoogleLoading,
                            isLoading = isGoogleLoading,
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

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row {
                        Text(
                            text = "Already have an account?",
                            style = KeuTrackTheme.typography.bodyRegular14,
                            color = KeuTrackTheme.textColors.body
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Login",
                            style = KeuTrackTheme.typography.bodyBold14,
                            color = KeuTrackTheme.primaryColors.primary500,
                            modifier = Modifier.clickable(onClick = onLoginClick)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "By creating an account, you agree to KeuTrack's Terms of Service " +
                            "and Privacy Policy. We treat your family data as a curated atelier of life.",
                        style = KeuTrackTheme.typography.bodyRegular14,
                        color = KeuTrackTheme.semanticColors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Register Screen - Idle")
@Composable
fun RegisterScreenPreview() {
    KeuTrackTheme {
        RegisterScreen(
            authState = AuthState.Idle,
            onSignUpClick = { _, _, _, _ -> },
            onSignInWithGoogleClick = {},
            onLoginClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Register Screen - Loading")
@Composable
fun RegisterScreenLoadingPreview() {
    KeuTrackTheme {
        RegisterScreen(
            authState = AuthState.Loading(AuthMethod.Email),
            onSignUpClick = { _, _, _, _ -> },
            onSignInWithGoogleClick = {},
            onLoginClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Register Screen - Error")
@Composable
fun RegisterScreenErrorPreview() {
    KeuTrackTheme {
        RegisterScreen(
            authState = AuthState.Error("No internet connection. Please try again."),
            onSignUpClick = { _, _, _, _ -> },
            onSignInWithGoogleClick = {},
            onLoginClick = {},
        )
    }
}
