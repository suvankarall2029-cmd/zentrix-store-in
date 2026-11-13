package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ZentrixBackground
import com.example.ui.theme.ZentrixCardBorder
import com.example.ui.theme.ZentrixPrimaryBlue
import com.example.ui.theme.ZentrixSecondaryBlue
import com.example.ui.theme.ZentrixSuccess
import com.example.ui.theme.ZentrixSurface
import com.example.ui.theme.ZentrixTextPrimary
import com.example.ui.theme.ZentrixTextSecondary
import com.example.ui.viewmodel.ZentrixViewModel

@Composable
fun SupportScreen(
    viewModel: ZentrixViewModel,
    isBengali: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ZentrixSurface)
                    .border(width = 0.5.dp, color = ZentrixCardBorder)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("support_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ZentrixTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.help_support),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ZentrixTextPrimary
                )
            }
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ZentrixBackground)
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                border = BorderStroke(1.dp, ZentrixCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Help,
                            contentDescription = null,
                            tint = ZentrixSecondaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isBengali) "আমরা আপনাকে সাহায্য করতে প্রস্তুত" else "How can we help you?",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            color = ZentrixTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isBengali) "কোর্স সম্পর্কিত প্রশ্ন, পেমেন্ট সংক্রান্ত তথ্য বা কারিগরি সহায়তার জন্য আমাদের জানান।" else "Submit a query regarding courses, transaction verification, or curriculum access. Our team answers within 24 hours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZentrixTextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (submitted) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ZentrixSuccess.copy(alpha = 0.15f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ZentrixSuccess
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.ticket_submitted),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ZentrixSuccess
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.ticket_subject),
                            style = MaterialTheme.typography.labelMedium,
                            color = ZentrixTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            placeholder = { Text("e.g. Course access question") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ZentrixPrimaryBlue,
                                unfocusedBorderColor = ZentrixCardBorder,
                                focusedTextColor = ZentrixTextPrimary,
                                unfocusedTextColor = ZentrixTextPrimary
                            ),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("support_subject_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Message",
                            style = MaterialTheme.typography.labelMedium,
                            color = ZentrixTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            placeholder = { Text(stringResource(R.string.ticket_message)) },
                            minLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ZentrixPrimaryBlue,
                                unfocusedBorderColor = ZentrixCardBorder,
                                focusedTextColor = ZentrixTextPrimary,
                                unfocusedTextColor = ZentrixTextPrimary
                            ),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("support_message_input")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (subject.isNotBlank() && message.isNotBlank()) {
                                    isSubmitting = true
                                    viewModel.submitSupport(subject, message) { success ->
                                        isSubmitting = false
                                        if (success) submitted = true
                                    }
                                }
                            },
                            enabled = !isSubmitting && subject.isNotBlank() && message.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_submit_support")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = ZentrixTextPrimary, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = stringResource(R.string.submit_ticket),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ZentrixTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
