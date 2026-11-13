package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.signOutUser
import com.example.ui.theme.ZentrixBackground
import com.example.ui.theme.ZentrixCardBorder
import com.example.ui.theme.ZentrixError
import com.example.ui.theme.ZentrixPrimaryBlue
import com.example.ui.theme.ZentrixSecondaryBlue
import com.example.ui.theme.ZentrixSurface
import com.example.ui.theme.ZentrixSurfaceVariant
import com.example.ui.theme.ZentrixTextPrimary
import com.example.ui.theme.ZentrixTextSecondary
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.ZentrixViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: ZentrixViewModel,
    isBengali: Boolean,
    onLanguageChange: (String) -> Unit,
    onNavigateMyLearning: () -> Unit,
    onNavigateNotifications: () -> Unit,
    onNavigateSupport: () -> Unit,
    onNavigateAdminUsers: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authUser = Firebase.auth.currentUser
    val userProfileState by viewModel.userProfile.collectAsState()
    val isAdminUser by viewModel.isAdminUser.collectAsState()
    val ordersState by viewModel.orders.collectAsState()
    val enrollmentsState by viewModel.enrollments.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showOrdersDialog by remember { mutableStateOf(false) }

    val profile = (userProfileState as? UiState.Success)?.data
    val displayName = profile?.fullName?.takeIf { it.isNotBlank() }
        ?: profile?.displayName?.takeIf { it.isNotBlank() }
        ?: authUser?.displayName
        ?: "Zentrix Learner"
    val email = profile?.email?.takeIf { it.isNotBlank() }
        ?: authUser?.email
        ?: "student@zentrixstore.app"

    val enrolledCount = when (val state = enrollmentsState) {
        is UiState.Success -> state.data.size
        else -> 0
    }

    val ordersList = when (val state = ordersState) {
        is UiState.Success -> state.data
        else -> emptyList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ZentrixBackground)
            .testTag("profile_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Header Card
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                border = BorderStroke(1.dp, ZentrixCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(ZentrixSurfaceVariant)
                            .border(2.dp, ZentrixPrimaryBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = ZentrixSecondaryBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ZentrixTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZentrixTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ZentrixPrimaryBlue.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (profile?.isVerified == true) "VERIFIED ACCOUNT" else "PENDING VERIFICATION",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = ZentrixSecondaryBlue
                                )
                            }
                            if (profile?.publicUserId?.isNotBlank() == true) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ZentrixSurfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = profile.publicUserId,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = ZentrixTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Registered Account Details
        if (profile != null && profile.registrationCompleted) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                    border = BorderStroke(1.dp, ZentrixCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isBengali) "নিবন্ধিত প্রোফাইল বিবরণ" else "Registered Account Profile",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ZentrixTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (!profile.phoneNumber.isNullOrBlank()) {
                            ProfileKeyValueRow("Phone", profile.phoneNumber)
                        }
                        if (profile.ageGroup.isNotBlank()) {
                            ProfileKeyValueRow("Age Group", "${profile.ageGroup} (${profile.age} yrs)")
                        }
                        if (profile.purpose.isNotBlank()) {
                            val purposeText = if (profile.purpose == "Other" && !profile.otherPurpose.isNullOrBlank()) {
                                "Other: ${profile.otherPurpose}"
                            } else profile.purpose
                            ProfileKeyValueRow("Purpose", purposeText)
                        }
                        ProfileKeyValueRow("Status", "${profile.status} • ${profile.role}")
                        if (profile.interests.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBengali) "আগ্রহের বিষয়" else "Interests",
                                style = MaterialTheme.typography.labelMedium,
                                color = ZentrixTextSecondary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profile.interests.forEach { interest ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ZentrixPrimaryBlue.copy(alpha = 0.16f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = interest,
                                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                            color = ZentrixSecondaryBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Admin Control Panel Entry Point (Authorized Admins Only)
        if (isAdminUser) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                    border = BorderStroke(1.dp, ZentrixSecondaryBlue.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_admin_panel_card")
                ) {
                    ProfileMenuRow(
                        icon = Icons.Default.AdminPanelSettings,
                        title = if (isBengali) "অ্যাডমিন কন্ট্রোল প্যানেল — ব্যবহারকারী" else "Admin Control Panel — User Management",
                        tint = ZentrixSecondaryBlue,
                        onClick = onNavigateAdminUsers
                    )
                }
            }
        }

        // Learning & Orders Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                    border = BorderStroke(1.dp, ZentrixCardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateMyLearning() }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$enrolledCount",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = ZentrixSecondaryBlue
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isBengali) "নথিভুক্ত কোর্স" else "Enrolled Courses",
                            style = MaterialTheme.typography.labelMedium,
                            color = ZentrixTextSecondary
                        )
                    }
                }

                Card(
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                    border = BorderStroke(1.dp, ZentrixCardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showOrdersDialog = true }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${ordersList.size}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = ZentrixPrimaryBlue
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isBengali) "লেনদেন ইতিহাস" else "Transactions",
                            style = MaterialTheme.typography.labelMedium,
                            color = ZentrixTextSecondary
                        )
                    }
                }
            }
        }

        // Language Preferences
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                border = BorderStroke(1.dp, ZentrixCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.language_settings),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ZentrixTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LanguageChoiceChip(
                            title = "English",
                            subtitle = "Default",
                            isSelected = !isBengali,
                            onClick = {
                                onLanguageChange("en")
                                viewModel.updateLanguage("en")
                            },
                            modifier = Modifier.weight(1f)
                        )

                        LanguageChoiceChip(
                            title = "বাংলা (Bengali)",
                            subtitle = "অনুবাদ সমর্থিত",
                            isSelected = isBengali,
                            onClick = {
                                onLanguageChange("bn")
                                viewModel.updateLanguage("bn")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Account & Platform Actions
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                border = BorderStroke(1.dp, ZentrixCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Default.School,
                        title = if (isBengali) "আমার শিক্ষা ড্যাশবোর্ড" else "My Learning Dashboard",
                        onClick = onNavigateMyLearning
                    )
                    HorizontalDivider(color = ZentrixCardBorder)
                    ProfileMenuRow(
                        icon = Icons.Default.Payment,
                        title = stringResource(R.string.payment_history),
                        onClick = { showOrdersDialog = true }
                    )
                    HorizontalDivider(color = ZentrixCardBorder)
                    ProfileMenuRow(
                        icon = Icons.Default.Notifications,
                        title = if (isBengali) "বিজ্ঞপ্তি পছন্দসমূহ" else "Notifications & Alerts",
                        onClick = onNavigateNotifications
                    )
                    HorizontalDivider(color = ZentrixCardBorder)
                    ProfileMenuRow(
                        icon = Icons.AutoMirrored.Filled.Help,
                        title = stringResource(R.string.help_support),
                        onClick = onNavigateSupport
                    )
                    HorizontalDivider(color = ZentrixCardBorder)
                    ProfileMenuRow(
                        icon = Icons.Default.Description,
                        title = stringResource(R.string.terms_and_conditions),
                        onClick = { showTermsDialog = true }
                    )
                    HorizontalDivider(color = ZentrixCardBorder)
                    ProfileMenuRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.privacy_policy),
                        onClick = { showPrivacyDialog = true }
                    )
                }
            }
        }

        // Logout and Delete Account Actions
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                border = BorderStroke(1.dp, ZentrixCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ProfileMenuRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = stringResource(R.string.sign_out),
                        tint = ZentrixSecondaryBlue,
                        onClick = {
                            signOutUser(context, coroutineScope)
                        }
                    )
                    HorizontalDivider(color = ZentrixCardBorder)
                    ProfileMenuRow(
                        icon = Icons.Default.DeleteForever,
                        title = stringResource(R.string.delete_account),
                        tint = ZentrixError,
                        onClick = { showDeleteDialog = true }
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Zentrix Store v1.0 • Native Android Enterprise",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = ZentrixTextSecondary
                )
            }
        }
    }

    // Delete Account Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = stringResource(R.string.delete_account), color = ZentrixError) },
            text = { Text(text = stringResource(R.string.delete_account_confirm), color = ZentrixTextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount {
                            signOutUser(context, coroutineScope)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZentrixError)
                ) {
                    Text(text = "Delete Forever", color = ZentrixTextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = "Cancel", color = ZentrixTextSecondary)
                }
            },
            containerColor = ZentrixSurface,
            shape = MaterialTheme.shapes.medium
        )
    }

    // Terms Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text(text = "Terms & Conditions", color = ZentrixTextPrimary) },
            text = {
                Text(
                    text = "1. Acceptance: Accessing Zentrix Store constitutes agreement to all platform rules.\n2. Course License: Enrollment grants a personal, non-transferable license to access video and document resources.\n3. Payments: Transactions processed via Razorpay & UPI are server-verified prior to content unlocking.\n4. Integrity: Sharing account credentials or unauthorized file redistribution is strictly prohibited.",
                    color = ZentrixTextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue)) {
                    Text("Close")
                }
            },
            containerColor = ZentrixSurface,
            shape = MaterialTheme.shapes.medium
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(text = "Privacy Policy", color = ZentrixTextPrimary) },
            text = {
                Text(
                    text = "Zentrix Store is committed to zero-trust data protection. We store your profile and enrollment status securely in Cloud Firestore Enterprise. We never store credit/debit card numbers, CVVs, or sensitive payment credentials on our databases. All payments are verified securely server-side through PCI-DSS compliant providers.",
                    color = ZentrixTextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue)) {
                    Text("Close")
                }
            },
            containerColor = ZentrixSurface,
            shape = MaterialTheme.shapes.medium
        )
    }

    // Purchase History Dialog
    if (showOrdersDialog) {
        AlertDialog(
            onDismissRequest = { showOrdersDialog = false },
            title = { Text(text = "Purchase & Transaction History", color = ZentrixTextPrimary) },
            text = {
                if (ordersList.isEmpty()) {
                    Text(text = "No purchase transactions recorded yet.", color = ZentrixTextSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ordersList.forEach { order ->
                            Card(
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(containerColor = ZentrixSurfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = order.courseTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                        color = ZentrixTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Amount: ₹${order.amount.toInt()} • Method: ${order.paymentMethod} • Status: ${order.status}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = ZentrixTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showOrdersDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue)) {
                    Text("Close")
                }
            },
            containerColor = ZentrixSurface,
            shape = MaterialTheme.shapes.medium
        )
    }
}

@Composable
private fun LanguageChoiceChip(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) ZentrixSecondaryBlue else ZentrixCardBorder
    val bgColor = if (isSelected) ZentrixPrimaryBlue.copy(alpha = 0.15f) else ZentrixSurfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = if (isSelected) ZentrixSecondaryBlue else ZentrixTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = ZentrixTextSecondary
            )
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    tint: androidx.compose.ui.graphics.Color = ZentrixTextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
            color = tint,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = ZentrixTextSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ProfileKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = ZentrixTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            color = ZentrixTextPrimary
        )
    }
}
