package com.example.ui.components

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoogleSignInAuthTest {

    @Before
    fun setUp() {
        AuthRequestCoordinator.release()
    }

    @After
    fun tearDown() {
        AuthRequestCoordinator.release()
    }

    @Test
    fun `authRequestCoordinator prevents duplicate concurrent login requests`() {
        assertFalse(AuthRequestCoordinator.isBusy())

        assertTrue(AuthRequestCoordinator.tryAcquire())
        assertTrue(AuthRequestCoordinator.isBusy())

        // Second request while first is in flight must be rejected
        assertFalse(AuthRequestCoordinator.tryAcquire())
        assertTrue(AuthRequestCoordinator.isBusy())

        // Release allows a subsequent request
        AuthRequestCoordinator.release()
        assertFalse(AuthRequestCoordinator.isBusy())
        assertTrue(AuthRequestCoordinator.tryAcquire())
    }

    @Test
    fun `user cancellation details do not trigger configuration error`() {
        assertFalse(isConfigurationOrBackendRejection(""))
        assertFalse(isConfigurationOrBackendRejection("   "))
        assertFalse(isConfigurationOrBackendRejection("activity is cancelled by the user."))
        assertFalse(isConfigurationOrBackendRejection("User cancelled the dialog"))
        assertFalse(isConfigurationOrBackendRejection("The user dismissed the sign-in sheet"))
    }

    @Test
    fun `play services developer and sha rejection details are detected as failures`() {
        assertTrue(isConfigurationOrBackendRejection("[28444] Developer console is not set up correctly."))
        assertTrue(isConfigurationOrBackendRejection("10: Developer error"))
        assertTrue(isConfigurationOrBackendRejection("16: Caller not whitelisted"))
        assertTrue(isConfigurationOrBackendRejection("Google ApiException: 10"))
        assertTrue(isConfigurationOrBackendRejection("Invalid SHA-1 fingerprint for package"))
        assertTrue(isConfigurationOrBackendRejection("Missing OAuth client_id configuration"))
        assertTrue(isConfigurationOrBackendRejection("Reauth failed for selected account"))
        assertTrue(isConfigurationOrBackendRejection("Unauthorized client request"))
    }

    @Test
    fun `firebase google services configuration provides valid package and web client id`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.zentrixstore.app", context.packageName)

        val webClientId = context.getString(R.string.default_web_client_id)
        assertTrue(
            "Expected default_web_client_id to be a Google OAuth client ID",
            webClientId.endsWith(".apps.googleusercontent.com")
        )
    }
}
