package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.StoreNotification
import com.example.ui.theme.ZentrixBackground
import com.example.ui.theme.ZentrixCardBorder
import com.example.ui.theme.ZentrixPrimaryBlue
import com.example.ui.theme.ZentrixSecondaryBlue
import com.example.ui.theme.ZentrixSuccess
import com.example.ui.theme.ZentrixSurface
import com.example.ui.theme.ZentrixSurfaceVariant
import com.example.ui.theme.ZentrixTextPrimary
import com.example.ui.theme.ZentrixTextSecondary
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.ZentrixViewModel

@Composable
fun NotificationsScreen(
    viewModel: ZentrixViewModel,
    isBengali: Boolean
) {
    val notificationsState by viewModel.notifications.collectAsState()
    var courseUpdatesEnabled by remember { mutableStateOf(true) }
    var purchaseAlertsEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZentrixBackground)
            .testTag("notifications_screen")
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ZentrixSurface)
                .padding(20.dp)
        ) {
            Text(
                text = if (isBengali) "বিজ্ঞপ্তি ও বার্তা" else "Notifications & Alerts",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = ZentrixTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isBengali) "কোর্স আপডেট, ক্রয় নিশ্চিতকরণ এবং গুরুত্বপূর্ণ ঘোষণা" else "Purchase confirmations, lesson updates and academy announcements.",
                style = MaterialTheme.typography.bodyMedium,
                color = ZentrixTextSecondary
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Notification Preferences Card
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                    border = BorderStroke(1.dp, ZentrixCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isBengali) "বিজ্ঞপ্তি পছন্দসমূহ" else "Notification Preferences",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ZentrixTextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        PreferenceToggleRow(
                            label = if (isBengali) "কোর্স ও পাঠের আপডেট" else "Course & Lesson Updates",
                            checked = courseUpdatesEnabled,
                            onCheckedChange = { courseUpdatesEnabled = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        PreferenceToggleRow(
                            label = if (isBengali) "পেমেন্ট ও ক্রয়ের রসিদ" else "Payment & Purchase Receipts",
                            checked = purchaseAlertsEnabled,
                            onCheckedChange = { purchaseAlertsEnabled = it }
                        )
                    }
                }
            }

            item {
                Text(
                    text = if (isBengali) "সাম্প্রতিক বিজ্ঞপ্তি" else "Recent Notifications",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ZentrixTextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            when (val state = notificationsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ZentrixPrimaryBlue)
                        }
                    }
                }
                is UiState.Success -> {
                    val notifs = state.data
                    if (notifs.isEmpty()) {
                        item {
                            Card(
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = ZentrixTextSecondary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = stringResource(R.string.no_notifications),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ZentrixTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(notifs) { notif ->
                            NotificationItemCard(
                                notification = notif,
                                isBengali = isBengali,
                                onClick = {
                                    if (!notif.read) {
                                        viewModel.markNotificationRead(notif.id)
                                    }
                                }
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(text = state.message, color = ZentrixTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = ZentrixTextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ZentrixTextPrimary,
                checkedTrackColor = ZentrixPrimaryBlue,
                uncheckedThumbColor = ZentrixTextSecondary,
                uncheckedTrackColor = ZentrixSurfaceVariant
            )
        )
    }
}

@Composable
private fun NotificationItemCard(
    notification: StoreNotification,
    isBengali: Boolean,
    onClick: () -> Unit
) {
    val icon = when (notification.type) {
        "PURCHASE" -> Icons.Default.Payment
        "COURSE_UPDATE" -> Icons.Default.School
        "ANNOUNCEMENT" -> Icons.Default.Campaign
        else -> Icons.Default.CheckCircle
    }

    val iconColor = when (notification.type) {
        "PURCHASE" -> ZentrixSuccess
        "COURSE_UPDATE" -> ZentrixSecondaryBlue
        "ANNOUNCEMENT" -> Color(0xFFFFB800)
        else -> ZentrixPrimaryBlue
    }

    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) ZentrixSurface else ZentrixSurfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            if (notification.read) ZentrixCardBorder else ZentrixPrimaryBlue.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                val title = if (isBengali && notification.titleBn.isNotBlank()) notification.titleBn else notification.title
                val msg = if (isBengali && notification.messageBn.isNotBlank()) notification.messageBn else notification.message

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = ZentrixTextPrimary
                    )
                    if (!notification.read) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ZentrixSecondaryBlue)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = ZentrixTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
