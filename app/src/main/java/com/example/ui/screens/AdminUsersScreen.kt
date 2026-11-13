package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RegistrationConstants
import com.example.model.UserProfile
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
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.ZentrixViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    viewModel: ZentrixViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isAdmin by viewModel.isAdminUser.collectAsState()
    val usersState by viewModel.adminUsers.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var verifiedFilter by remember { mutableStateOf("All") } // All, Verified, Unverified
    var statusFilter by remember { mutableStateOf("All") } // All, ACTIVE, SUSPENDED
    var ageGroupFilter by remember { mutableStateOf("All") }
    var purposeFilter by remember { mutableStateOf("All") }
    var purposeMenuExpanded by remember { mutableStateOf(false) }

    var selectedUser by remember { mutableStateOf<UserProfile?>(null) }
    var exportCsvPreview by remember { mutableStateOf<String?>(null) }

    if (!isAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ZentrixBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Block, contentDescription = null, tint = ZentrixError, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Authorized Admin Access Only",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ZentrixTextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val allUsers = (usersState as? UiState.Success)?.data ?: emptyList()
    val filteredUsers = remember(allUsers, searchQuery, verifiedFilter, statusFilter, ageGroupFilter, purposeFilter) {
        allUsers.filter { user ->
            val q = searchQuery.trim()
            val matchesQuery = q.isBlank() ||
                user.fullName.contains(q, ignoreCase = true) ||
                user.displayName.contains(q, ignoreCase = true) ||
                user.email.contains(q, ignoreCase = true) ||
                (user.phoneNumber ?: "").contains(q, ignoreCase = true) ||
                user.userId.contains(q, ignoreCase = true) ||
                user.publicUserId.contains(q, ignoreCase = true)

            val matchesVerified = when (verifiedFilter) {
                "Verified" -> user.isVerified
                "Unverified" -> !user.isVerified
                else -> true
            }

            val matchesStatus = statusFilter == "All" || user.status == statusFilter
            val matchesAgeGroup = ageGroupFilter == "All" || user.ageGroup == ageGroupFilter
            val matchesPurpose = purposeFilter == "All" || user.purpose == purposeFilter

            matchesQuery && matchesVerified && matchesStatus && matchesAgeGroup && matchesPurpose
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZentrixBackground)
            .testTag("admin_users_screen")
    ) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ZentrixSurface)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ZentrixTextPrimary)
            }
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = ZentrixSecondaryBlue,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Admin User Control",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ZentrixTextPrimary
                )
                Text(
                    text = "${allUsers.size} registered • ${filteredUsers.size} shown",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = ZentrixTextSecondary
                )
            }
            OutlinedButton(
                onClick = { exportCsvPreview = viewModel.buildUsersCsvExport(filteredUsers) },
                enabled = filteredUsers.isNotEmpty(),
                border = BorderStroke(1.dp, ZentrixPrimaryBlue),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("admin_export_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = ZentrixSecondaryBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export", fontSize = 12.sp, color = ZentrixTextPrimary)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Metrics Strip
            item {
                val verifiedCount = allUsers.count { it.isVerified }
                val activeCount = allUsers.count { it.status == "ACTIVE" }
                val suspendedCount = allUsers.count { it.status == "SUSPENDED" }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminStatCard("Total", "${allUsers.size}", ZentrixSecondaryBlue, Modifier.weight(1f))
                    AdminStatCard("Verified", "$verifiedCount", ZentrixSuccess, Modifier.weight(1f))
                    AdminStatCard("Active", "$activeCount", ZentrixPrimaryBlue, Modifier.weight(1f))
                    AdminStatCard("Suspended", "$suspendedCount", ZentrixError, Modifier.weight(1f))
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search name, email, phone, or User ID...", color = ZentrixTextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ZentrixSecondaryBlue) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = ZentrixTextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = zentrixInputColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_search_input")
                )
            }

            // Purpose Filter Dropdown + Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = purposeMenuExpanded,
                        onExpandedChange = { purposeMenuExpanded = !purposeMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = "Purpose: $purposeFilter",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = purposeMenuExpanded) },
                            colors = zentrixInputColors(),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                                .testTag("admin_purpose_filter")
                        )
                        ExposedDropdownMenu(
                            expanded = purposeMenuExpanded,
                            onDismissRequest = { purposeMenuExpanded = false },
                            containerColor = ZentrixSurfaceVariant
                        ) {
                            (listOf("All") + RegistrationConstants.PURPOSE_OPTIONS).forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = ZentrixTextPrimary) },
                                    onClick = {
                                        purposeFilter = option
                                        purposeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Verification & Status Filter Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("All", "Verified", "Unverified")) { item ->
                            FilterChip(
                                selected = verifiedFilter == item,
                                onClick = { verifiedFilter = item },
                                label = { Text(item, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ZentrixSurface,
                                    labelColor = ZentrixTextPrimary,
                                    selectedContainerColor = ZentrixPrimaryBlue.copy(alpha = 0.25f),
                                    selectedLabelColor = ZentrixSecondaryBlue
                                )
                            )
                        }
                        items(listOf("ACTIVE", "SUSPENDED")) { status ->
                            FilterChip(
                                selected = statusFilter == status,
                                onClick = { statusFilter = if (statusFilter == status) "All" else status },
                                label = { Text(status, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ZentrixSurface,
                                    labelColor = ZentrixTextPrimary,
                                    selectedContainerColor = if (status == "SUSPENDED") ZentrixError.copy(alpha = 0.25f) else ZentrixSuccess.copy(alpha = 0.2f),
                                    selectedLabelColor = ZentrixTextPrimary
                                )
                            )
                        }
                    }

                    // Age Group Filters
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(RegistrationConstants.AGE_GROUP_FILTERS) { ageGrp ->
                            FilterChip(
                                selected = ageGroupFilter == ageGrp,
                                onClick = { ageGroupFilter = ageGrp },
                                label = { Text(if (ageGrp == "All") "Age: All" else "Age $ageGrp", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = ZentrixSurface,
                                    labelColor = ZentrixTextPrimary,
                                    selectedContainerColor = ZentrixPrimaryBlue.copy(alpha = 0.25f),
                                    selectedLabelColor = ZentrixSecondaryBlue
                                )
                            )
                        }
                    }
                }
            }

            when (usersState) {
                is UiState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ZentrixPrimaryBlue)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(
                            text = (usersState as UiState.Error).message,
                            color = ZentrixError,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is UiState.Success -> {
                    if (filteredUsers.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                                border = BorderStroke(1.dp, ZentrixCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No matching registered users found.",
                                    color = ZentrixTextSecondary,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(filteredUsers, key = { it.userId }) { user ->
                            AdminUserCard(
                                user = user,
                                onClick = { selectedUser = user }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedUser?.let { target ->
        val freshTarget = allUsers.find { it.userId == target.userId } ?: target
        AdminUserDetailDialog(
            user = freshTarget,
            onDismiss = { selectedUser = null },
            onToggleStatus = {
                val nextStatus = if (freshTarget.status == "ACTIVE") "SUSPENDED" else "ACTIVE"
                viewModel.adminSetUserStatus(freshTarget, nextStatus)
            },
            onToggleVerified = { viewModel.adminToggleVerification(freshTarget) },
            onSetRole = { newRole -> viewModel.adminSetUserRole(freshTarget, newRole) }
        )
    }

    exportCsvPreview?.let { csv ->
        AlertDialog(
            onDismissRequest = { exportCsvPreview = null },
            title = { Text("Export User Records (CSV)", color = ZentrixTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "${filteredUsers.size} user record(s) prepared (passwords excluded):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZentrixTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = csv.take(1500) + if (csv.length > 1500) "\n..." else "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = ZentrixSecondaryBlue
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "Zentrix Store Users Export")
                            putExtra(Intent.EXTRA_TEXT, csv)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export Zentrix Users CSV"))
                        exportCsvPreview = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue)
                ) {
                    Text("Share CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportCsvPreview = null }) {
                    Text("Close", color = ZentrixTextSecondary)
                }
            },
            containerColor = ZentrixSurface,
            shape = MaterialTheme.shapes.medium
        )
    }
}

@Composable
private fun AdminStatCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
        border = BorderStroke(1.dp, ZentrixCardBorder),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = ZentrixTextSecondary)
        }
    }
}

@Composable
private fun AdminUserCard(
    user: UserProfile,
    onClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
        border = BorderStroke(1.dp, if (user.status == "SUSPENDED") ZentrixError.copy(alpha = 0.5f) else ZentrixCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("admin_user_card_${user.userId}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.fullName.ifBlank { user.displayName },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ZentrixTextPrimary
                    )
                    Text(
                        text = "${user.publicUserId.ifBlank { user.userId.take(10) }} • ${user.email}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = ZentrixTextSecondary
                    )
                    if (!user.phoneNumber.isNullOrBlank()) {
                        Text(
                            text = user.phoneNumber,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = ZentrixSecondaryBlue
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusPill(
                        text = user.role,
                        color = if (user.role == "ADMIN") ZentrixSecondaryBlue else ZentrixPrimaryBlue
                    )
                    StatusPill(
                        text = user.status,
                        color = if (user.status == "ACTIVE") ZentrixSuccess else ZentrixError
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (user.isVerified) Icons.Default.VerifiedUser else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (user.isVerified) ZentrixSuccess else ZentrixTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${if (user.isVerified) "Verified" else "Unverified"} • ${user.purpose.ifBlank { "Pending Onboarding" }} • ${user.ageGroup.ifBlank { "Age N/A" }}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = ZentrixTextSecondary
                )
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = color
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminUserDetailDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onToggleStatus: () -> Unit,
    onToggleVerified: () -> Unit,
    onSetRole: (String) -> Unit
) {
    var confirmRoleChange by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = user.fullName.ifBlank { user.displayName },
                color = ZentrixTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ProfileDetailLine("Public ID", user.publicUserId.ifBlank { "Pending" })
                ProfileDetailLine("Firebase UID", user.userId)
                ProfileDetailLine("First / Middle / Last", listOf(user.firstName, user.middleName.orEmpty(), user.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Not completed" })
                ProfileDetailLine("Email", user.email)
                ProfileDetailLine("Phone", user.phoneNumber ?: "Not provided")
                ProfileDetailLine("Age / Group", if (user.age > 0) "${user.age} (${user.ageGroup})" else "Not provided")
                ProfileDetailLine("DOB", user.dateOfBirth ?: "Not provided")
                ProfileDetailLine("Purpose", if (user.purpose == "Other" && !user.otherPurpose.isNullOrBlank()) "Other: ${user.otherPurpose}" else user.purpose.ifBlank { "Not completed" })
                ProfileDetailLine("Registered", formatTimestamp(user.createdAt))
                ProfileDetailLine("Last Login", formatTimestamp(user.lastLoginAt))
                ProfileDetailLine("Verification", if (user.isVerified) "Verified" else "Unverified")
                ProfileDetailLine("Status", user.status)
                ProfileDetailLine("Role", user.role)

                if (user.interests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Interests", style = MaterialTheme.typography.labelMedium, color = ZentrixSecondaryBlue)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        user.interests.forEach { interest ->
                            StatusPill(interest, ZentrixPrimaryBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = ZentrixCardBorder)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onToggleStatus,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (user.status == "ACTIVE") ZentrixError else ZentrixSuccess
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (user.status == "ACTIVE") "Suspend" else "Activate", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onToggleVerified,
                        border = BorderStroke(1.dp, ZentrixSecondaryBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (user.isVerified) "Unverify" else "Verify", fontSize = 12.sp, color = ZentrixTextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                val targetRole = if (user.role == "ADMIN") "USER" else "ADMIN"
                OutlinedButton(
                    onClick = { confirmRoleChange = targetRole },
                    border = BorderStroke(1.dp, ZentrixPrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Role to $targetRole", color = ZentrixSecondaryBlue, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ZentrixTextPrimary)
            }
        },
        containerColor = ZentrixSurface,
        shape = MaterialTheme.shapes.medium
    )

    confirmRoleChange?.let { newRole ->
        AlertDialog(
            onDismissRequest = { confirmRoleChange = null },
            title = { Text("Confirm Role Update", color = ZentrixTextPrimary) },
            text = {
                Text(
                    text = "Set ${user.displayName}'s role to $newRole? Admins have access to user management controls.",
                    color = ZentrixTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetRole(newRole)
                        confirmRoleChange = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRoleChange = null }) {
                    Text("Cancel", color = ZentrixTextSecondary)
                }
            },
            containerColor = ZentrixSurface
        )
    }
}

@Composable
private fun ProfileDetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$label:", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = ZentrixTextSecondary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), color = ZentrixTextPrimary)
    }
}

private fun formatTimestamp(ts: Timestamp?): String {
    if (ts == null) return "—"
    return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(ts.toDate())
}
