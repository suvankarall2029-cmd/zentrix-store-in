package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PasswordEvaluation
import com.example.data.RegistrationConstants
import com.example.data.RegistrationValidator
import com.example.data.Step1Validation
import com.example.data.Step2Validation
import com.example.data.Step4Validation
import com.example.model.RegistrationDraft
import com.example.ui.theme.ZentrixBackground
import com.example.ui.theme.ZentrixCardBorder
import com.example.ui.theme.ZentrixError
import com.example.ui.theme.ZentrixPrimaryBlue
import com.example.ui.theme.ZentrixSecondaryBlue
import com.example.ui.theme.ZentrixSuccess
import com.example.ui.theme.ZentrixSurface
import com.example.ui.theme.ZentrixSurfaceVariant
import com.example.ui.theme.ZentrixTextPrimary
import com.example.ui.theme.ZentrixTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}

@Composable
fun zentrixInputColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = ZentrixSurface,
    unfocusedContainerColor = ZentrixSurface,
    focusedBorderColor = ZentrixPrimaryBlue,
    unfocusedBorderColor = ZentrixCardBorder,
    errorBorderColor = ZentrixError,
    focusedTextColor = ZentrixTextPrimary,
    unfocusedTextColor = ZentrixTextPrimary,
    focusedLabelColor = ZentrixSecondaryBlue,
    unfocusedLabelColor = ZentrixTextSecondary
)

@Composable
fun RegistrationProgressHeader(
    currentStep: Int,
    totalSteps: Int = 4,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("reg_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ZentrixTextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ZentrixPrimaryBlue.copy(alpha = 0.16f))
                    .border(1.dp, ZentrixPrimaryBlue.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Step $currentStep of $totalSteps",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ZentrixSecondaryBlue,
                    modifier = Modifier.testTag("reg_step_indicator")
                )
            }

            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        LinearProgressIndicator(
            progress = { currentStep.toFloat() / totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = ZentrixSecondaryBlue,
            trackColor = ZentrixSurfaceVariant
        )
    }
}

@Composable
fun RegistrationStep1PersonalInfo(
    draft: RegistrationDraft,
    validation: Step1Validation,
    onDraftChange: (RegistrationDraft) -> Unit,
    onContinue: () -> Unit
) {
    val formattedPreview = remember(draft.firstName, draft.middleName, draft.lastName) {
        RegistrationValidator.buildFullName(draft.firstName, draft.middleName, draft.lastName)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Create Your Account",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = ZentrixTextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Let's get to know you.",
            style = MaterialTheme.typography.bodyMedium,
            color = ZentrixTextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = draft.firstName,
            onValueChange = { onDraftChange(draft.copy(firstName = it)) },
            label = { Text("First Name *") },
            placeholder = { Text("Enter first name", color = ZentrixTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ZentrixSecondaryBlue) },
            isError = draft.firstName.isNotEmpty() && validation.firstNameError != null,
            supportingText = if (draft.firstName.isNotEmpty() && validation.firstNameError != null) {
                { Text(validation.firstNameError, color = ZentrixError) }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            shape = MaterialTheme.shapes.medium,
            colors = zentrixInputColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_first_name_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = draft.middleName,
            onValueChange = { onDraftChange(draft.copy(middleName = it)) },
            label = { Text("Middle Name (Optional)") },
            placeholder = { Text("Optional middle name", color = ZentrixTextSecondary) },
            isError = draft.middleName.isNotEmpty() && validation.middleNameError != null,
            supportingText = if (draft.middleName.isNotEmpty() && validation.middleNameError != null) {
                { Text(validation.middleNameError, color = ZentrixError) }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            shape = MaterialTheme.shapes.medium,
            colors = zentrixInputColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_middle_name_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = draft.lastName,
            onValueChange = { onDraftChange(draft.copy(lastName = it)) },
            label = { Text("Last Name *") },
            placeholder = { Text("Enter last name", color = ZentrixTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ZentrixPrimaryBlue) },
            isError = draft.lastName.isNotEmpty() && validation.lastNameError != null,
            supportingText = if (draft.lastName.isNotEmpty() && validation.lastNameError != null) {
                { Text(validation.lastNameError, color = ZentrixError) }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            shape = MaterialTheme.shapes.medium,
            colors = zentrixInputColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_last_name_input")
        )

        if (formattedPreview.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ZentrixSurfaceVariant),
                border = BorderStroke(1.dp, ZentrixCardBorder),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (validation.isValid) ZentrixSuccess else ZentrixTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Profile Display Name: $formattedPreview",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZentrixTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onDraftChange(
                    draft.copy(
                        firstName = RegistrationValidator.formatPersonName(draft.firstName),
                        middleName = RegistrationValidator.formatPersonName(draft.middleName),
                        lastName = RegistrationValidator.formatPersonName(draft.lastName)
                    )
                )
                onContinue()
            },
            enabled = validation.isValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = ZentrixPrimaryBlue,
                contentColor = ZentrixTextPrimary
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("reg_step1_continue")
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun RegistrationStep2ContactInfo(
    draft: RegistrationDraft,
    validation: Step2Validation,
    otpCodeInput: String,
    onOtpCodeChange: (String) -> Unit,
    otpCooldownSeconds: Int,
    isVerifying: Boolean,
    statusMessage: String?,
    onDraftChange: (RegistrationDraft) -> Unit,
    onVerifyEmailWithGoogle: () -> Unit,
    onSendPhoneOtp: () -> Unit,
    onVerifyPhoneOtp: () -> Unit,
    onContinue: () -> Unit
) {
    var showCountryMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Contact Information",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = ZentrixTextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Choose how you want to register. Enter email, mobile number, or both.",
            style = MaterialTheme.typography.bodyMedium,
            color = ZentrixTextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Option A: Gmail / Email
        Card(
            colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
            border = BorderStroke(
                1.dp,
                if (draft.emailVerified) ZentrixSuccess.copy(alpha = 0.6f) else ZentrixCardBorder
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Option A • Gmail / Email Address",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = ZentrixSecondaryBlue
                    )
                    if (draft.emailVerified) {
                        Text(
                            text = "VERIFIED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ZentrixSuccess
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = draft.email,
                    onValueChange = { onDraftChange(draft.copy(email = it, emailVerified = false)) },
                    label = { Text("Email Address") },
                    placeholder = { Text("example@gmail.com", color = ZentrixTextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ZentrixSecondaryBlue) },
                    isError = draft.email.isNotBlank() && validation.emailError != null,
                    supportingText = if (draft.email.isNotBlank() && validation.emailError != null) {
                        { Text(validation.emailError, color = ZentrixError) }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = MaterialTheme.shapes.small,
                    colors = zentrixInputColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_email_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onVerifyEmailWithGoogle,
                    enabled = !isVerifying,
                    border = BorderStroke(1.dp, if (draft.emailVerified) ZentrixSuccess else ZentrixPrimaryBlue),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reg_verify_email_google_button")
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (draft.emailVerified) ZentrixSuccess else ZentrixSecondaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (draft.emailVerified) "Email Verified via Google" else "Verify Email with Google",
                            color = if (draft.emailVerified) ZentrixSuccess else ZentrixTextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Option B: Phone Number
        Card(
            colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
            border = BorderStroke(
                1.dp,
                if (draft.phoneVerified) ZentrixSuccess.copy(alpha = 0.6f) else ZentrixCardBorder
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Option B • Mobile Number (OTP)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = ZentrixSecondaryBlue
                    )
                    if (draft.phoneVerified) {
                        Text(
                            text = "OTP VERIFIED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ZentrixSuccess
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { showCountryMenu = true },
                            border = BorderStroke(1.dp, ZentrixCardBorder),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("reg_country_code_selector")
                        ) {
                            Text(draft.countryCode, color = ZentrixTextPrimary)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select country code", tint = ZentrixTextSecondary)
                        }
                        DropdownMenu(
                            expanded = showCountryMenu,
                            onDismissRequest = { showCountryMenu = false },
                            containerColor = ZentrixSurfaceVariant
                        ) {
                            RegistrationConstants.COUNTRY_CODES.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text("${item.code}  ${item.countryName}", color = ZentrixTextPrimary) },
                                    onClick = {
                                        onDraftChange(draft.copy(countryCode = item.code, phoneVerified = false))
                                        showCountryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = draft.phoneLocal,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == ' ' || it == '-' }.take(14)
                            onDraftChange(draft.copy(phoneLocal = filtered, phoneVerified = false))
                        },
                        label = { Text("Mobile Number") },
                        placeholder = { Text("9876543210", color = ZentrixTextSecondary) },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = ZentrixPrimaryBlue) },
                        isError = draft.phoneLocal.isNotBlank() && validation.phoneError != null,
                        supportingText = if (draft.phoneLocal.isNotBlank() && validation.phoneError != null) {
                            { Text(validation.phoneError, color = ZentrixError) }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = MaterialTheme.shapes.small,
                        colors = zentrixInputColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reg_phone_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onSendPhoneOtp,
                        enabled = validation.hasPhone && validation.phoneError == null && otpCooldownSeconds == 0 && !isVerifying,
                        border = BorderStroke(1.dp, ZentrixPrimaryBlue),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("reg_send_otp_button")
                    ) {
                        Text(
                            text = if (otpCooldownSeconds > 0) "Resend in ${otpCooldownSeconds}s" else "Send OTP",
                            color = ZentrixTextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }

                if (!draft.verificationId.isNullOrBlank() && !draft.phoneVerified) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = otpCodeInput,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) onOtpCodeChange(it) },
                            label = { Text("6-Digit OTP") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = zentrixInputColors(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reg_otp_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onVerifyPhoneOtp,
                            enabled = otpCodeInput.length == 6 && !isVerifying,
                            colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("reg_verify_otp_button")
                        ) {
                            Text("Verify")
                        }
                    }
                }
            }
        }

        if (statusMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (draft.emailVerified || draft.phoneVerified) ZentrixSuccess else ZentrixSecondaryBlue
            )
        }

        if (validation.generalError != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = validation.generalError, color = ZentrixError, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onContinue,
            enabled = validation.isValid && !isVerifying,
            colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue, contentColor = ZentrixTextPrimary),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("reg_step2_continue")
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun RegistrationStep3Password(
    draft: RegistrationDraft,
    evaluation: PasswordEvaluation,
    onDraftChange: (RegistrationDraft) -> Unit,
    onContinue: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val strengthColor by animateColorAsState(
        targetValue = when (evaluation.strengthScore) {
            0, 1, 2 -> ZentrixError
            3 -> Color(0xFFF59E0B)
            4 -> ZentrixSecondaryBlue
            else -> ZentrixSuccess
        },
        label = "passwordStrengthColor"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Secure Your Account",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = ZentrixTextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Create a strong password to protect your Zentrix Store account.",
            style = MaterialTheme.typography.bodyMedium,
            color = ZentrixTextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = draft.password,
            onValueChange = { onDraftChange(draft.copy(password = it)) },
            label = { Text("Password *") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ZentrixSecondaryBlue) },
            trailingIcon = {
                IconButton(
                    onClick = { showPassword = !showPassword },
                    modifier = Modifier.testTag("reg_toggle_password")
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
            shape = MaterialTheme.shapes.medium,
            colors = zentrixInputColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_password_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = draft.confirmPassword,
            onValueChange = { onDraftChange(draft.copy(confirmPassword = it)) },
            label = { Text("Confirm Password *") },
            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = ZentrixPrimaryBlue) },
            trailingIcon = {
                IconButton(
                    onClick = { showConfirm = !showConfirm },
                    modifier = Modifier.testTag("reg_toggle_confirm_password")
                ) {
                    Icon(
                        imageVector = if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showConfirm) "Hide password" else "Show password",
                        tint = ZentrixTextSecondary
                    )
                }
            },
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            isError = draft.confirmPassword.isNotEmpty() && !evaluation.passwordsMatch,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = MaterialTheme.shapes.medium,
            colors = zentrixInputColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_confirm_password_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Password Strength", style = MaterialTheme.typography.labelLarge, color = ZentrixTextSecondary)
            Text(
                text = evaluation.strengthLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = strengthColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (evaluation.strengthScore / 5f).coerceIn(0.05f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = strengthColor,
            trackColor = ZentrixSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
            border = BorderStroke(1.dp, ZentrixCardBorder),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PasswordRuleItem("Minimum 8 characters", evaluation.hasMinLength)
                PasswordRuleItem("At least one uppercase letter (A-Z)", evaluation.hasUppercase)
                PasswordRuleItem("At least one lowercase letter (a-z)", evaluation.hasLowercase)
                PasswordRuleItem("At least one number (0-9)", evaluation.hasDigit)
                PasswordRuleItem("At least one special character (!@#\$%^&*)", evaluation.hasSpecialChar)
                PasswordRuleItem("Confirm password matches", evaluation.passwordsMatch)
            }
        }

        if (evaluation.errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = evaluation.errorMessage,
                color = ZentrixError,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onContinue,
            enabled = evaluation.isValid,
            colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue, contentColor = ZentrixTextPrimary),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("reg_step3_continue")
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun PasswordRuleItem(label: String, met: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (met) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (met) ZentrixSuccess else ZentrixTextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = if (met) ZentrixTextPrimary else ZentrixTextSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistrationStep4AboutYou(
    draft: RegistrationDraft,
    validation: Step4Validation,
    isSubmitting: Boolean,
    submitError: String?,
    onDraftChange: (RegistrationDraft) -> Unit,
    onFinish: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var purposeMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Tell Us About Yourself",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = ZentrixTextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Help us personalize your Zentrix Store experience.",
            style = MaterialTheme.typography.bodyMedium,
            color = ZentrixTextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Section A: Age / Date of Birth
        Text(
            text = "A. Your Age",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = ZentrixSecondaryBlue
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                border = BorderStroke(1.dp, ZentrixPrimaryBlue),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1.3f)
                    .height(56.dp)
                    .testTag("reg_dob_picker_button")
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = ZentrixSecondaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = draft.dateOfBirth.ifBlank { "Pick Birth Date" },
                    color = ZentrixTextPrimary,
                    fontSize = 13.sp
                )
            }

            OutlinedTextField(
                value = draft.ageInput,
                onValueChange = { raw ->
                    val digits = raw.filter(Char::isDigit).take(3)
                    val parsedAge = digits.toIntOrNull()
                    onDraftChange(
                        draft.copy(
                            ageInput = digits,
                            computedAge = parsedAge,
                            dateOfBirth = ""
                        )
                    )
                },
                label = { Text("Or Age") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium,
                colors = zentrixInputColors(),
                modifier = Modifier
                    .weight(0.9f)
                    .testTag("reg_age_input")
            )
        }

        if (validation.ageGroup.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Verified Age: ${validation.resolvedAge} years (Group: ${validation.ageGroup})",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = ZentrixSuccess
            )
        }
        if (validation.privacyNotice != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = validation.privacyNotice,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = ZentrixSecondaryBlue
            )
        }
        if (validation.ageError != null && (draft.ageInput.isNotBlank() || draft.dateOfBirth.isNotBlank())) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = validation.ageError,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = ZentrixError
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section B: Purpose
        Text(
            text = "B. Why Are You Using Zentrix Store?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = ZentrixSecondaryBlue
        )
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = purposeMenuExpanded,
            onExpandedChange = { purposeMenuExpanded = !purposeMenuExpanded }
        ) {
            OutlinedTextField(
                value = draft.purpose,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Primary Purpose *") },
                placeholder = { Text("Choose your primary role", color = ZentrixTextSecondary) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = purposeMenuExpanded) },
                colors = zentrixInputColors(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth()
                    .testTag("reg_purpose_dropdown")
            )

            ExposedDropdownMenu(
                expanded = purposeMenuExpanded,
                onDismissRequest = { purposeMenuExpanded = false },
                containerColor = ZentrixSurfaceVariant
            ) {
                RegistrationConstants.PURPOSE_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = ZentrixTextPrimary) },
                        onClick = {
                            onDraftChange(
                                draft.copy(
                                    purpose = option,
                                    otherPurpose = if (option == "Other") draft.otherPurpose else ""
                                )
                            )
                            purposeMenuExpanded = false
                        }
                    )
                }
            }
        }

        if (draft.purpose == "Other") {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = draft.otherPurpose,
                onValueChange = { onDraftChange(draft.copy(otherPurpose = it)) },
                label = { Text("Describe your primary purpose *") },
                placeholder = { Text("e.g., Technical Educator / Startup Founder", color = ZentrixTextSecondary) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = zentrixInputColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reg_other_purpose_input")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section C: Interests
        Text(
            text = "C. What Are You Interested In?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = ZentrixSecondaryBlue
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Select one or more topics (${draft.interests.size} selected)",
            style = MaterialTheme.typography.bodyMedium,
            color = ZentrixTextSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RegistrationConstants.INTEREST_OPTIONS.forEach { interest ->
                val selected = draft.interests.contains(interest)
                FilterChip(
                    selected = selected,
                    onClick = {
                        val updated = if (selected) draft.interests - interest else draft.interests + interest
                        onDraftChange(draft.copy(interests = updated))
                    },
                    label = { Text(interest, fontSize = 12.sp) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ZentrixSurface,
                        labelColor = ZentrixTextPrimary,
                        selectedContainerColor = ZentrixPrimaryBlue.copy(alpha = 0.25f),
                        selectedLabelColor = ZentrixSecondaryBlue,
                        selectedLeadingIconColor = ZentrixSecondaryBlue
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = ZentrixCardBorder,
                        selectedBorderColor = ZentrixSecondaryBlue
                    ),
                    modifier = Modifier.testTag("reg_interest_${interest.lowercase().replace(" ", "_")}")
                )
            }
        }

        if (submitError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = submitError,
                color = ZentrixError,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onFinish,
            enabled = validation.isValid && !isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue, contentColor = ZentrixTextPrimary),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("reg_finish_button")
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = ZentrixTextPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Creating Account...")
            } else {
                Text(
                    text = "Finish Registration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val year = cal.get(Calendar.YEAR)
                            val month = cal.get(Calendar.MONTH) + 1
                            val day = cal.get(Calendar.DAY_OF_MONTH)
                            val age = RegistrationValidator.calculateAgeFromDob(year, month, day)
                            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            onDraftChange(
                                draft.copy(
                                    dateOfBirth = fmt.format(Date(millis)),
                                    computedAge = age,
                                    ageInput = age.coerceAtLeast(0).toString()
                                )
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm", color = ZentrixSecondaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = ZentrixTextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
