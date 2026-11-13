package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AuthPreferences
import com.example.data.RegistrationValidator
import com.example.ui.components.GoogleSignInButton
import com.example.ui.theme.ZentrixBackground
import com.example.ui.theme.ZentrixCardBorder
import com.example.ui.theme.ZentrixError
import com.example.ui.theme.ZentrixPrimaryBlue
import com.example.ui.theme.ZentrixSecondaryBlue
import com.example.ui.theme.ZentrixSuccess
import com.example.ui.theme.ZentrixSurface
import com.example.ui.theme.ZentrixTextPrimary
import com.example.ui.theme.ZentrixTextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    var showRegistrationWizard by remember { mutableStateOf(false) }

    if (showRegistrationWizard) {
        RegistrationWizardScreen(
            existingProfile = null,
            onBackToLogin = { showRegistrationWizard = false },
            onRegistrationCompleted = {
                showRegistrationWizard = false
                onAuthSuccess()
            }
        )
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authPrefs = remember(context) { AuthPreferences(context) }

    var identifierInput by remember { mutableStateOf(authPrefs.getRememberedIdentifier()) }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(authPrefs.isRememberMeEnabled()) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var resetCooldownSeconds by remember { mutableIntStateOf(0) }

    val scrollState = rememberScrollState()

    LaunchedEffect(resetCooldownSeconds) {
        if (resetCooldownSeconds > 0) {
            delay(1000L)
            resetCooldownSeconds -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZentrixBackground)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .testTag("auth_login_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Monogram Identity
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ZentrixSurface)
                    .border(2.dp, ZentrixPrimaryBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Z",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp
                    ),
                    color = ZentrixSecondaryBlue
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ZENTRIX",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = ZentrixTextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "STORE",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
                    color = ZentrixSecondaryBlue
                )
            }

            Text(
                text = stringResource(R.string.brand_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = ZentrixTextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Login Card
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                border = BorderStroke(1.dp, ZentrixCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Sign In to Your Account",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ZentrixTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Access courses, source code, PDF notes, and orders.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = ZentrixTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = identifierInput,
                        onValueChange = {
                            identifierInput = it
                            errorMessage = null
                            infoMessage = null
                        },
                        label = { Text("Email / Phone Number") },
                        placeholder = { Text("name@gmail.com or +919876543210", color = ZentrixTextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = ZentrixSecondaryBlue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = MaterialTheme.shapes.small,
                        colors = zentrixInputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_identifier_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            errorMessage = null
                            infoMessage = null
                        },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ZentrixPrimaryBlue)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { showPassword = !showPassword },
                                modifier = Modifier.testTag("login_toggle_password")
                            ) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password",
                                    tint = ZentrixTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = MaterialTheme.shapes.small,
                        colors = zentrixInputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    rememberMe = !rememberMe
                                    authPrefs.saveLoginPreference(rememberMe, identifierInput)
                                }
                                .padding(vertical = 4.dp)
                                .testTag("login_remember_me")
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { checked ->
                                    rememberMe = checked
                                    authPrefs.saveLoginPreference(checked, identifierInput)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = ZentrixPrimaryBlue,
                                    uncheckedColor = ZentrixTextSecondary
                                )
                            )
                            Text(
                                text = "Remember Me",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = ZentrixTextPrimary
                            )
                        }

                        TextButton(
                            onClick = { showForgotDialog = true },
                            modifier = Modifier.testTag("login_forgot_password_button")
                        ) {
                            Text(
                                text = "Forgot Password?",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = ZentrixSecondaryBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val trimmed = identifierInput.trim()
                            val isEmail = RegistrationValidator.isValidEmail(trimmed)
                            val digits = RegistrationValidator.extractDigits(trimmed)
                            val isPhone = digits.length in 8..15

                            if (!isEmail && !isPhone) {
                                errorMessage = "Enter a valid registered email address or mobile number."
                                return@Button
                            }
                            if (passwordInput.length < 8) {
                                errorMessage = "Password must be at least 8 characters."
                                return@Button
                            }

                            authPrefs.saveLoginPreference(rememberMe, trimmed)
                            // Password sign-in requires Email/Password & Phone Auth enabled manually in Firebase Console;
                            // AI Studio provisions Google Sign-In via Credential Manager directly below.
                            infoMessage = null
                            errorMessage = "Email/Phone password sign-in requires enabling Email/Password & Phone providers in Firebase Console. Please use Continue with Google below for instant verified access."
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZentrixPrimaryBlue,
                            contentColor = ZentrixTextPrimary
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_sign_in_button")
                    ) {
                        Text(
                            text = "Sign In",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            errorMessage = null
                            showRegistrationWizard = true
                        },
                        border = BorderStroke(1.dp, ZentrixSecondaryBlue.copy(alpha = 0.7f)),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_create_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = ZentrixSecondaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create New Account",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ZentrixTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = ZentrixError,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .testTag("auth_error_message")
                )
            }

            if (infoMessage != null) {
                Text(
                    text = infoMessage.orEmpty(),
                    color = ZentrixSuccess,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = ZentrixCardBorder)
                Text(
                    text = "  OR  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = ZentrixTextSecondary
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = ZentrixCardBorder)
            }

            Spacer(modifier = Modifier.height(8.dp))

            GoogleSignInButton(
                onAuthSuccess = {
                    errorMessage = null
                    onAuthSuccess()
                },
                onAuthError = { errorMsg -> errorMessage = errorMsg },
                onSignInStarted = { errorMessage = null }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "By signing in, you agree to our Terms of Service & Privacy Policy. All accounts and purchases are secured with Firebase zero-trust verification.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = ZentrixTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showForgotDialog) {
        var resetEmail by remember {
            mutableStateOf(
                if (RegistrationValidator.isValidEmail(identifierInput)) identifierInput.trim() else ""
            )
        }
        var resetFeedback by remember { mutableStateOf<String?>(null) }
        var isSendingReset by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("Reset Password", color = ZentrixTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter your registered email address to send a verified password reset link.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZentrixTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            resetFeedback = null
                        },
                        label = { Text("Registered Email") },
                        singleLine = true,
                        colors = zentrixInputColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (resetFeedback != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resetFeedback.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = ZentrixSecondaryBlue
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSendingReset && resetCooldownSeconds == 0,
                    onClick = {
                        val trimmed = resetEmail.trim()
                        if (!RegistrationValidator.isValidEmail(trimmed)) {
                            resetFeedback = "Enter a valid email address."
                            return@Button
                        }
                        isSendingReset = true
                        coroutineScope.launch {
                            try {
                                Firebase.auth.sendPasswordResetEmail(trimmed).await()
                                resetCooldownSeconds = 60
                                resetFeedback = "Password reset verification link sent to $trimmed."
                            } catch (e: FirebaseAuthException) {
                                resetFeedback = when (e.errorCode) {
                                    "ERROR_OPERATION_NOT_ALLOWED" ->
                                        "Password reset requires Email/Password Auth enabled in Firebase Console. You can sign in directly with Continue with Google."
                                    "ERROR_USER_NOT_FOUND" ->
                                        "No email/password user record found for $trimmed. Try Continue with Google or Create New Account."
                                    else -> e.localizedMessage ?: "Unable to send reset email."
                                }
                            } catch (e: Exception) {
                                resetFeedback = e.localizedMessage ?: "Failed to send password reset email."
                            } finally {
                                isSendingReset = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue)
                ) {
                    Text(if (resetCooldownSeconds > 0) "Wait ${resetCooldownSeconds}s" else "Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Close", color = ZentrixTextSecondary)
                }
            },
            containerColor = ZentrixSurface,
            shape = MaterialTheme.shapes.medium
        )
    }
}
