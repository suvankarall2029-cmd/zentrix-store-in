package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Course
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
import com.example.ui.viewmodel.ZentrixViewModel
import java.util.UUID

@Composable
fun PaymentScreen(
    courseId: String,
    viewModel: ZentrixViewModel,
    isBengali: Boolean,
    onBack: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    BackHandler { onBack() }

    var course by remember { mutableStateOf<Course?>(null) }
    var selectedMethod by remember { mutableStateOf("UPI") }
    var upiIdInput by remember { mutableStateOf("user@okhdfcbank") }
    var paymentStatusMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var orderGeneratedId by remember { mutableStateOf("order_${UUID.randomUUID().toString().take(10)}") }

    LaunchedEffect(courseId) {
        course = viewModel.repository.getCourse(courseId)
    }

    val isProcessing by viewModel.paymentProcessing.collectAsState()

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
                IconButton(onClick = onBack, modifier = Modifier.testTag("payment_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ZentrixTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.razorpay_checkout),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ZentrixTextPrimary
                )
            }
        }
    ) { innerPadding ->
        if (course == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ZentrixBackground)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ZentrixPrimaryBlue)
            }
        } else {
            val c = course!!
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ZentrixBackground)
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Order ID header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ORDER ID: $orderGeneratedId",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ZentrixTextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ZentrixSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "256-Bit SSL",
                            style = MaterialTheme.typography.labelMedium,
                            color = ZentrixSuccess
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Order Summary Card
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = ZentrixSurface),
                    border = BorderStroke(1.dp, ZentrixCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.order_summary),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ZentrixTextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val title = if (isBengali && c.titleBn.isNotBlank()) c.titleBn else c.title
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            color = ZentrixTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Instructor: ${c.instructor} • ${c.duration}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZentrixTextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = ZentrixCardBorder)
                        Spacer(modifier = Modifier.height(16.dp))

                        SummaryRow(label = "Course Tuition", value = "₹${(c.price * 1.5).toInt()}")
                        SummaryRow(label = "Platform Special Discount", value = "- ₹${(c.price * 0.5).toInt()}", valueColor = ZentrixSuccess)
                        SummaryRow(label = "GST / Taxes (Included)", value = "₹0")

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = ZentrixCardBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Payable Amount",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ZentrixTextPrimary
                            )
                            Text(
                                text = "₹${c.price.toInt()}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = ZentrixSecondaryBlue
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Payment Method Selector
                Text(
                    text = "Select Secure Payment Provider",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ZentrixTextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                PaymentOptionCard(
                    title = stringResource(R.string.upi_pay),
                    subtitle = "Instant UPI transfer (GPay, PhonePe, Paytm, Cred)",
                    icon = Icons.Default.QrCode2,
                    isSelected = selectedMethod == "UPI",
                    onClick = { selectedMethod = "UPI" }
                )

                Spacer(modifier = Modifier.height(10.dp))

                PaymentOptionCard(
                    title = stringResource(R.string.card_netbanking),
                    subtitle = "Visa, MasterCard, RuPay & All Major Indian Banks",
                    icon = Icons.Default.CreditCard,
                    isSelected = selectedMethod == "Card",
                    onClick = { selectedMethod = "Card" }
                )

                Spacer(modifier = Modifier.height(10.dp))

                PaymentOptionCard(
                    title = "Net Banking",
                    subtitle = "HDFC, ICICI, SBI, Axis, Kotak & 50+ Banks",
                    icon = Icons.Default.AccountBalance,
                    isSelected = selectedMethod == "NetBanking",
                    onClick = { selectedMethod = "NetBanking" }
                )

                if (selectedMethod == "UPI") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(containerColor = ZentrixSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Enter Virtual Payment Address (VPA / UPI ID)",
                                style = MaterialTheme.typography.labelMedium,
                                color = ZentrixTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = upiIdInput,
                                onValueChange = { upiIdInput = it },
                                placeholder = { Text("username@bank") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ZentrixPrimaryBlue,
                                    unfocusedBorderColor = ZentrixCardBorder,
                                    focusedTextColor = ZentrixTextPrimary,
                                    unfocusedTextColor = ZentrixTextPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upi_input_field")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Security Notice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = ZentrixSecondaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.payment_security_notice),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = ZentrixTextSecondary,
                        lineHeight = 16.sp
                    )
                }

                if (paymentStatusMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = paymentStatusMessage.orEmpty(),
                        color = ZentrixError,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pay Now Button
                Button(
                    onClick = {
                        isVerifying = true
                        viewModel.processPurchase(
                            course = c,
                            paymentMethod = selectedMethod,
                            amount = c.price,
                            onSuccess = {
                                isVerifying = false
                                onPaymentSuccess()
                            },
                            onError = { err ->
                                isVerifying = false
                                paymentStatusMessage = err
                            }
                        )
                    },
                    enabled = !isProcessing && !isVerifying,
                    colors = ButtonDefaults.buttonColors(containerColor = ZentrixPrimaryBlue),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_confirm_pay")
                ) {
                    if (isProcessing || isVerifying) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = ZentrixTextPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Verifying Payment on Server...",
                                color = ZentrixTextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else {
                        Text(
                            text = "Pay ₹${c.price.toInt()} & Unlock Course",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ZentrixTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = ZentrixTextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = ZentrixTextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = valueColor)
    }
}

@Composable
private fun PaymentOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) ZentrixSecondaryBlue else ZentrixCardBorder
    val bgColor = if (isSelected) ZentrixPrimaryBlue.copy(alpha = 0.1f) else ZentrixSurface

    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ZentrixSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) ZentrixSecondaryBlue else ZentrixTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ZentrixTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = ZentrixTextSecondary
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(ZentrixPrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = ZentrixTextPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
