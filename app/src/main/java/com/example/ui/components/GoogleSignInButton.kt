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
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
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
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

private const val AUTH_TAG = "ZentrixAuth"

sealed interface AuthOutcome {
    data object Success : AuthOutcome
    data object Cancelled : AuthOutcome
    data class Failure(val message: String) : AuthOutcome
}

object AuthRequestCoordinator {
    private val inFlight = AtomicBoolean(false)

    fun tryAcquire(): Boolean = inFlight.compareAndSet(false, true)

    fun release() {
        inFlight.set(false)
    }

    fun isBusy(): Boolean = inFlight.get()
}

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
            if (isLoading || AuthRequestCoordinator.isBusy()) return@Button
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
                onAuthCancelled = {
                    isLoading = false
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
    if (!AuthRequestCoordinator.tryAcquire()) {
        return
    }

    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        Log.w(AUTH_TAG, "default_web_client_id missing during auto sign-in", e)
        AuthRequestCoordinator.release()
        onUnauthenticated()
        return
    }

    val activityContext = context.findActivity()
    if (activityContext == null) {
        AuthRequestCoordinator.release()
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
        } finally {
            AuthRequestCoordinator.release()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    if (!AuthRequestCoordinator.tryAcquire()) {
        onAuthCancelled()
        return
    }

    val clientId = try {
        context.getString(R.string.default_web_client_id).trim()
    } catch (e: Exception) {
        AuthRequestCoordinator.release()
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found.")
        return
    }

    if (clientId.isBlank()) {
        AuthRequestCoordinator.release()
        onAuthError("Google OAuth Web Client ID is empty in Firebase configuration.")
        return
    }

    val activityContext = context.findActivity()
    if (activityContext == null) {
        AuthRequestCoordinator.release()
        onAuthError("Unable to start Google Sign-In: no active window found.")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        val outcome: AuthOutcome = try {
            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            exchangeGoogleCredentialForFirebaseAuth(result.credential)
            AuthOutcome.Success
        } catch (e: CancellationException) {
            AuthRequestCoordinator.release()
            throw e
        } catch (e: GetCredentialCancellationException) {
            val detail = e.errorMessage?.toString()?.trim().orEmpty()
            Log.w(AUTH_TAG, "GetCredentialCancellationException: detail='$detail', msg='${e.message}'", e)
            if (isConfigurationOrBackendRejection(detail)) {
                AuthOutcome.Failure(
                    "Google Sign-In rejected by Play Services ($detail). Verify that this APK's SHA-1/SHA-256 fingerprint is registered in Firebase."
                )
            } else {
                // RESULT_CANCELED: user dismissed the account picker -> return cleanly without misleading error
                AuthOutcome.Cancelled
            }
        } catch (e: NoCredentialException) {
            Log.w(AUTH_TAG, "NoCredentialException: ${e.message}", e)
            AuthOutcome.Failure("No Google account found on this device. Please add a Google account in device Settings and try again.")
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.e(AUTH_TAG, "Provider configuration error: ${e.message}", e)
            AuthOutcome.Failure("Google Play Services credential provider is not available or misconfigured on this device.")
        } catch (e: GetCredentialUnsupportedException) {
            Log.e(AUTH_TAG, "Credential Manager unsupported: ${e.message}", e)
            AuthOutcome.Failure("Google Sign-In is not supported by this device's Play Services version.")
        } catch (e: GetCredentialInterruptedException) {
            Log.w(AUTH_TAG, "Sign-in interrupted: ${e.message}", e)
            AuthOutcome.Failure("Google Sign-In was interrupted. Please try again.")
        } catch (e: GetCredentialCustomException) {
            Log.e(AUTH_TAG, "Custom credential error (${e.type}): ${e.errorMessage}", e)
            AuthOutcome.Failure(
                e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
                    ?: "Google Sign-In failed (${e.type})."
            )
        } catch (e: GetCredentialException) {
            Log.e(AUTH_TAG, "Credential Manager error (${e.type}): ${e.errorMessage}", e)
            AuthOutcome.Failure(
                e.errorMessage?.toString()?.takeIf { it.isNotBlank() }
                    ?: "Google Sign-In failed. Check your network connection and Firebase OAuth configuration."
            )
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(AUTH_TAG, "Invalid Google ID token", e)
            AuthOutcome.Failure("Failed to parse Google ID token. Please try again.")
        } catch (e: FirebaseNetworkException) {
            Log.e(AUTH_TAG, "Network error during Firebase authentication", e)
            AuthOutcome.Failure("Network error. Please check your internet connection and try again.")
        } catch (e: FirebaseAuthException) {
            Log.e(AUTH_TAG, "Firebase Auth error (${e.errorCode}): ${e.message}", e)
            AuthOutcome.Failure(formatFirebaseAuthError(e))
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Unexpected sign-in error", e)
            AuthOutcome.Failure(e.localizedMessage ?: "Authentication failed. Please try again.")
        } finally {
            AuthRequestCoordinator.release()
        }

        when (outcome) {
            is AuthOutcome.Success -> onAuthSuccess()
            is AuthOutcome.Cancelled -> onAuthCancelled()
            is AuthOutcome.Failure -> onAuthError(outcome.message)
        }
    }
}

internal fun isConfigurationOrBackendRejection(detail: String): Boolean {
    if (detail.isBlank()) return false
    val lower = detail.lowercase()
    return lower.contains("developer") ||
        lower.contains("developer_error") ||
        lower.contains("28444") ||
        lower.contains("10:") ||
        lower.contains("16:") ||
        lower.contains("reauth") ||
        lower.contains("sha") ||
        lower.contains("fingerprint") ||
        lower.contains("client_id") ||
        lower.contains("configuration") ||
        lower.contains("apiexception") ||
        lower.contains("whitelisted") ||
        lower.contains("unauthorized")
}

private fun formatFirebaseAuthError(e: FirebaseAuthException): String {
    return when (e.errorCode) {
        "ERROR_INVALID_CREDENTIAL" -> "Invalid or expired Google credentials. Please select your account again."
        "ERROR_USER_DISABLED" -> "This account has been disabled. Please contact Zentrix Store support."
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "An account already exists with the same email under a different sign-in provider."
        "ERROR_OPERATION_NOT_ALLOWED" -> "Google Sign-In is not enabled in Firebase Authentication."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Network error while signing in to Firebase. Please check your connection."
        else -> e.localizedMessage ?: "Firebase authentication failed (${e.errorCode})."
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
                AuthRequestCoordinator.release()
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
