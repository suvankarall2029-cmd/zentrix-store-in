package com.example.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.RegistrationValidator
import com.example.data.ZentrixRepository
import com.example.model.RegistrationDraft
import com.example.model.UserProfile
import com.example.ui.components.onGoogleSignInClicked
import com.example.ui.theme.ZentrixBackground
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

@Composable
fun RegistrationWizardScreen(
    existingProfile: UserProfile? = null,
    onBackToLogin: () -> Unit,
    onRegistrationCompleted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val repository = remember(context) {
        val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
        ZentrixRepository(db)
    }

    val initialUser = Firebase.auth.currentUser
    var draft by remember {
        val fullNameSeed = existingProfile?.fullName?.takeIf { it.isNotBlank() }
            ?: initialUser?.displayName.orEmpty()
        val parts = fullNameSeed.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val firstSeed = existingProfile?.firstName?.takeIf { it.isNotBlank() } ?: parts.firstOrNull().orEmpty()
        val lastSeed = existingProfile?.lastName?.takeIf { it.isNotBlank() }
            ?: if (parts.size > 1) parts.last() else ""
        val middleSeed = existingProfile?.middleName?.takeIf { it.isNotBlank() }
            ?: if (parts.size > 2) parts.subList(1, parts.size - 1).joinToString(" ") else ""

        mutableStateOf(
            RegistrationDraft(
                firstName = RegistrationValidator.formatPersonName(firstSeed),
                middleName = RegistrationValidator.formatPersonName(middleSeed),
                lastName = RegistrationValidator.formatPersonName(lastSeed),
                email = existingProfile?.email?.takeIf { it.isNotBlank() && !it.endsWith("@zentrixstore.app") }
                    ?: initialUser?.email.orEmpty(),
                emailVerified = initialUser?.isEmailVerified == true
            )
        )
    }

    var currentStep by remember { mutableIntStateOf(1) }
    var otpCodeInput by remember { mutableStateOf("") }
    var otpCooldownSeconds by remember { mutableIntStateOf(0) }
    var otpAttemptCount by remember { mutableIntStateOf(0) }
    var isVerifyingContact by remember { mutableStateOf(false) }
    var contactStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    LaunchedEffect(otpCooldownSeconds) {
        if (otpCooldownSeconds > 0) {
            delay(1000L)
            otpCooldownSeconds -= 1
        }
    }

    fun navigateBack() {
        if (currentStep > 1) {
            currentStep -= 1
        } else {
            onBackToLogin()
        }
    }

    BackHandler {
        navigateBack()
    }

    val step1Validation = remember(draft.firstName, draft.middleName, draft.lastName) {
        RegistrationValidator.validateStep1(draft.firstName, draft.middleName, draft.lastName)
    }
    val step2Validation = remember(draft.email, draft.countryCode, draft.phoneLocal) {
        RegistrationValidator.validateStep2(draft.email, draft.countryCode, draft.phoneLocal)
    }
    val passwordEvaluation = remember(draft.password, draft.confirmPassword) {
        RegistrationValidator.evaluatePassword(draft.password, draft.confirmPassword)
    }
    val step4Validation = remember(draft.computedAge, draft.purpose, draft.otherPurpose, draft.interests) {
        RegistrationValidator.validateStep4(draft.computedAge, draft.purpose, draft.otherPurpose, draft.interests)
    }

    suspend fun saveRegistrationToBackend() {
        isSubmitting = true
        submitError = null
        val sanitizedDraft = draft.copy(password = "", confirmPassword = "")
        val result = repository.completeRegistration(sanitizedDraft)
        isSubmitting = false
        if (result.isSuccess) {
            // Wipe sensitive password values from in-memory state immediately
            draft = sanitizedDraft
            onRegistrationCompleted()
        } else {
            submitError = result.exceptionOrNull()?.message ?: "Failed to finalize registration."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZentrixBackground)
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
            .testTag("registration_wizard_screen")
    ) {
        RegistrationProgressHeader(
            currentStep = currentStep,
            totalSteps = 4,
            onBack = { navigateBack() }
        )

        Spacer(modifier = Modifier.height(18.dp))

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it / 4 } + fadeOut())
                }
            },
            label = "registrationStepTransition"
        ) { step ->
            when (step) {
                1 -> RegistrationStep1PersonalInfo(
                    draft = draft,
                    validation = step1Validation,
                    onDraftChange = { draft = it },
                    onContinue = { currentStep = 2 }
                )

                2 -> RegistrationStep2ContactInfo(
                    draft = draft,
                    validation = step2Validation,
                    otpCodeInput = otpCodeInput,
                    onOtpCodeChange = { otpCodeInput = it },
                    otpCooldownSeconds = otpCooldownSeconds,
                    isVerifying = isVerifyingContact,
                    statusMessage = contactStatusMessage,
                    onDraftChange = {
                        draft = it
                        contactStatusMessage = null
                    },
                    onVerifyEmailWithGoogle = {
                        isVerifyingContact = true
                        contactStatusMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                val authUser = Firebase.auth.currentUser
                                val authEmail = authUser?.email.orEmpty()
                                coroutineScope.launch {
                                    val available = if (authEmail.isNotBlank()) {
                                        repository.checkContactAvailable("EMAIL", authEmail, authUser?.uid).getOrDefault(true)
                                    } else true
                                    isVerifyingContact = false
                                    if (!available) {
                                        contactStatusMessage = "This email is already linked to another account."
                                    } else {
                                        draft = draft.copy(
                                            email = authEmail.ifBlank { draft.email },
                                            emailVerified = authUser?.isEmailVerified == true || authEmail.isNotBlank()
                                        )
                                        contactStatusMessage = "Email verified via Google."
                                    }
                                }
                            },
                            onAuthError = { err ->
                                isVerifyingContact = false
                                contactStatusMessage = err
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isVerifyingContact = false
                            }
                        )
                    },
                    onSendPhoneOtp = {
                        if (otpAttemptCount >= 5) {
                            contactStatusMessage = "Too many OTP requests. Please wait before trying again."
                            return@RegistrationStep2ContactInfo
                        }
                        val activity = context.findHostActivity()
                        if (activity == null) {
                            contactStatusMessage = "Unable to start phone verification without an active window."
                            return@RegistrationStep2ContactInfo
                        }
                        val e164Phone = RegistrationValidator.normalizePhoneNumber(draft.countryCode, draft.phoneLocal)
                        isVerifyingContact = true
                        contactStatusMessage = "Sending verification code to $e164Phone..."
                        otpAttemptCount += 1

                        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                coroutineScope.launch {
                                    try {
                                        val user = Firebase.auth.currentUser
                                        if (user != null) {
                                            user.linkWithCredential(credential).await()
                                        } else {
                                            Firebase.auth.signInWithCredential(credential).await()
                                        }
                                        draft = draft.copy(phoneVerified = true)
                                        contactStatusMessage = "Phone number verified automatically."
                                    } catch (e: Exception) {
                                        Log.w("ZentrixReg", "Phone link/signIn failed", e)
                                        contactStatusMessage = e.localizedMessage ?: "Phone verification failed."
                                    } finally {
                                        isVerifyingContact = false
                                    }
                                }
                            }

                            override fun onVerificationFailed(e: FirebaseException) {
                                isVerifyingContact = false
                                Log.w("ZentrixReg", "Phone verification failed: ${e.message}", e)
                                contactStatusMessage = e.localizedMessage?.takeIf { it.isNotBlank() }
                                    ?: "Phone OTP requires Phone Authentication enabled in Firebase Console. You can also verify using Google Email above."
                            }

                            override fun onCodeSent(
                                verificationId: String,
                                token: PhoneAuthProvider.ForceResendingToken
                            ) {
                                isVerifyingContact = false
                                otpCooldownSeconds = 60
                                draft = draft.copy(verificationId = verificationId)
                                contactStatusMessage = "OTP sent to $e164Phone. Enter the 6-digit code below."
                            }
                        }

                        val options = PhoneAuthOptions.newBuilder(Firebase.auth)
                            .setPhoneNumber(e164Phone)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(activity)
                            .setCallbacks(callbacks)
                            .build()
                        PhoneAuthProvider.verifyPhoneNumber(options)
                    },
                    onVerifyPhoneOtp = {
                        val verId = draft.verificationId
                        if (verId.isNullOrBlank() || otpCodeInput.length != 6) {
                            contactStatusMessage = "Enter the 6-digit OTP sent to your mobile number."
                            return@RegistrationStep2ContactInfo
                        }
                        isVerifyingContact = true
                        coroutineScope.launch {
                            try {
                                val credential = PhoneAuthProvider.getCredential(verId, otpCodeInput)
                                val current = Firebase.auth.currentUser
                                if (current != null) {
                                    current.linkWithCredential(credential).await()
                                } else {
                                    Firebase.auth.signInWithCredential(credential).await()
                                }
                                draft = draft.copy(phoneVerified = true)
                                contactStatusMessage = "Phone OTP verified successfully."
                            } catch (e: Exception) {
                                contactStatusMessage = e.localizedMessage ?: "Invalid OTP code. Please try again."
                            } finally {
                                isVerifyingContact = false
                            }
                        }
                    },
                    onContinue = {
                        coroutineScope.launch {
                            isVerifyingContact = true
                            val uid = Firebase.auth.currentUser?.uid
                            if (draft.email.isNotBlank()) {
                                val ok = repository.checkContactAvailable("EMAIL", draft.email, uid).getOrDefault(true)
                                if (!ok) {
                                    isVerifyingContact = false
                                    contactStatusMessage = "This email address is already registered."
                                    return@launch
                                }
                            }
                            if (draft.phoneLocal.isNotBlank()) {
                                val e164 = RegistrationValidator.normalizePhoneNumber(draft.countryCode, draft.phoneLocal)
                                val ok = repository.checkContactAvailable("PHONE", e164, uid).getOrDefault(true)
                                if (!ok) {
                                    isVerifyingContact = false
                                    contactStatusMessage = "This phone number is already registered."
                                    return@launch
                                }
                            }
                            isVerifyingContact = false
                            currentStep = 3
                        }
                    }
                )

                3 -> RegistrationStep3Password(
                    draft = draft,
                    evaluation = passwordEvaluation,
                    onDraftChange = { draft = it },
                    onContinue = { currentStep = 4 }
                )

                4 -> RegistrationStep4AboutYou(
                    draft = draft,
                    validation = step4Validation,
                    isSubmitting = isSubmitting,
                    submitError = submitError,
                    onDraftChange = { draft = it },
                    onFinish = {
                        if (Firebase.auth.currentUser != null) {
                            coroutineScope.launch {
                                saveRegistrationToBackend()
                            }
                        } else {
                            // Anchor real verified Firebase Auth session via Credential Manager Google Sign-In before writing profile
                            isSubmitting = true
                            submitError = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    coroutineScope.launch {
                                        saveRegistrationToBackend()
                                    }
                                },
                                onAuthError = { err ->
                                    isSubmitting = false
                                    submitError = err
                                },
                                scope = coroutineScope,
                                onAuthCancelled = {
                                    isSubmitting = false
                                    submitError = "Please complete account verification to finalize registration."
                                }
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
