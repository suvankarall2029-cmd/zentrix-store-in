package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.theme.ZentrixCardBorder
import com.example.ui.theme.ZentrixPrimaryBlue
import com.example.ui.theme.ZentrixSurfaceVariant
import com.example.ui.theme.ZentrixTextPrimary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "ZentrixAuth"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun GoogleSignInButton(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onSignInStarted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (isLoading) return@Button
            isLoading = true
            onSignInStarted()
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                },
                onAuthError = { errorMsg ->
                    isLoading = false
                    onAuthError(errorMsg)
                },
                scope = coroutineScope,
                onAuthCancelled = { cancelMsg ->
                    isLoading = false
                    onAuthError(cancelMsg)
                }
            )
        },
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = ZentrixSurfaceVariant,
            contentColor = ZentrixTextPrimary
        ),
        border = BorderStroke(1.dp, ZentrixCardBorder),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = ZentrixPrimaryBlue,
                strokeWidth = 2.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Google Sign In Icon",
                    tint = ZentrixPrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = context.getString(R.string.sign_in_with_google),
                    style = MaterialTheme.typography.titleMedium,
                    color = ZentrixTextPrimary
                )
            }
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        Log.w(AUTH_TAG, "default_web_client_id missing during auto sign-in", e)
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val activityContext: Context = context.findActivity() ?: context

    scope.launch {
        try {
            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            exchangeGoogleCredentialForFirebaseAuth(result.credential)
            onAuthSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(AUTH_TAG, "Silent auto sign-in unavailable: ${e.javaClass.simpleName}: ${e.message}")
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: (String) -> Unit = {}
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }

    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val activityContext = context.findActivity()
    if (activityContext == null) {
        onAuthError("Unable to start Google Sign-In: no active window found.")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            exchangeGoogleCredentialForFirebaseAuth(result.credential)
            onAuthSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            val detail = e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
            val userMsg = if (detail != null && !detail.contains("cancel", ignoreCase = true)) {
                "Google Sign-In could not complete: $detail"
            } else {
                "Sign-in was cancelled. Please select a Google account to continue."
            }
            onAuthCancelled(userMsg)
        } catch (e: NoCredentialException) {
            Log.w(AUTH_TAG, "No Google credentials available: ${e.message}", e)
            onAuthError("No Google account found on this device. Please add a Google account in device Settings and retry.")
        } catch (e: GetCredentialException) {
            Log.e(AUTH_TAG, "Credential Manager error (${e.type}): ${e.errorMessage}", e)
            onAuthError(e.errorMessage?.toString()?.takeIf { it.isNotBlank() } ?: "Google Sign-In failed. Please check your connection and try again.")
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(AUTH_TAG, "Invalid Google ID token", e)
            onAuthError("Failed to verify Google ID token. Please try again.")
        } catch (e: FirebaseAuthException) {
            Log.e(AUTH_TAG, "Firebase Auth error (${e.errorCode}): ${e.message}", e)
            onAuthError(e.localizedMessage ?: "Firebase authentication failed (${e.errorCode}).")
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed unexpectedly", e)
            onAuthError(e.localizedMessage ?: "Sign-in failed. Please try again.")
        }
    }
}

private suspend fun exchangeGoogleCredentialForFirebaseAuth(credential: Credential) {
    if (credential !is CustomCredential || credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        throw IllegalStateException("Unexpected credential type returned by Google Sign-In.")
    }
    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
    val googleIdToken = googleIdTokenCredential.idToken
    if (googleIdToken.isBlank()) {
        throw IllegalStateException("Google Sign-In returned an empty ID token.")
    }
    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
    val authResult = withContext(NonCancellable) {
        Firebase.auth.signInWithCredential(authCredential).await()
    }
    if (authResult.user == null) {
        throw IllegalStateException("Firebase authentication completed without an active user session.")
    }
}

fun signOutUser(
    context: Context,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    val credentialManager = CredentialManager.create(context)
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
