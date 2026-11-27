package com.example.ui.chat

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SubscriptionFlowDialog(
    uiState: ChatUiState,
    onDismiss: () -> Unit,
    onActivate: (String) -> Unit,
    onCancel: () -> Unit,
    onRequireAuth: () -> Unit,
    onOpenTopUp: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // UI selection states for flow steps
    var selectedPlan by remember { mutableStateOf("Monthly") }
    var selectedPaymentMethod by remember { mutableStateOf("PayPal") }
    
    // Simulated engine states
    var isSimulating by remember { mutableStateOf(false) }
    var simulationLog by remember { mutableStateOf("") }
    
    val planPrice = when (selectedPlan) {
        "Weekly" -> "$0.99"
        "Monthly" -> "$3.99"
        "Yearly" -> "$24.99"
        else -> "$0"
    }

    Dialog(
        onDismissRequest = { if (!isSimulating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(2.dp, Color(0xFFFFB100).copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Premium Crown and Premium Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFB100).copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👑", fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Eddie Premium",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Pro Access & Payment Flow Simulation",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFB100),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSimulating
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isSubscribed) {
                    // Premium Active Screen State
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("✨ ACTIVE SUBSCRIPTION STATUS ✨", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Congratulations! You have active Premium (${uiState.activeSubscriptionPlan}) package.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Valid until: ${uiState.subscriptionExpiryDate}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "With Eddie Pro, you have unlocked all PRO-badged flagship AI models (GPT Image, Flux Pro, Ideogram, Veo 3/3.1, Kling Pro/4K, Seedance 2.0, Eleven Music v2.5), Pro styles, multi-variation rendering, HD/4K resolutions, and extended durations, plus monthly bonus tokens.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                            OutlinedButton(
                                onClick = onOpenTopUp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🪙 Top Up Token Tambahan")
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    onCancel()
                                    Toast.makeText(
                                        context,
                                        "Subscription canceled. Your account returned to Free Tier.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Cancel Subscription (Downgrade to Free)")
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onOpenTopUp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Text("🪙 Atau Top Up Token Satuan (5 Paket)")
                    }

                    // Subscription Flow Scheme
                    Text(
                        text = "PAYMENT SYSTEM FLOW SCHEME",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Unified diagram representation showing step layout in flow chart style
                    FlowchartDiagram()

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step 1: Select Plan
                    FlowStepHeader(stepNumber = 1, title = "Select Subscription Plan")
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlanCard(
                            planName = "Weekly",
                            price = "$0.99",
                            period = "/ week",
                            description = "7 days access to all PRO-badged AI models, Pro styles, HD ratios & +250 bonus tokens.",
                            isSelected = selectedPlan == "Weekly",
                            onSelect = { selectedPlan = "Weekly" }
                        )
                        PlanCard(
                            planName = "Monthly",
                            price = "$3.99",
                            period = "/ month",
                            description = "Most popular. Unlocks all PRO models (Veo 3.1, Kling 4K, Flux Pro, Music v2.5), 1080p/4K & +1,200 tokens.",
                            badge = "POPULAR",
                            isSelected = selectedPlan == "Monthly",
                            onSelect = { selectedPlan = "Monthly" }
                        )
                        PlanCard(
                            planName = "Yearly",
                            price = "$24.99",
                            period = "/ year",
                            description = "Best value. Full year of all PRO models, 4K rendering, Pro audio/image controls & +7,500 bonus tokens.",
                            badge = "SAVER 50%",
                            isSelected = selectedPlan == "Yearly",
                            onSelect = { selectedPlan = "Yearly" }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step 2: Payment Method Selection
                    FlowStepHeader(stepNumber = 2, title = "Payment Method (Cards & Express Checkout)")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentOptionButton(
                            name = "PayPal",
                            label = "💳 PayPal",
                            isSelected = selectedPaymentMethod == "PayPal",
                            onClick = { selectedPaymentMethod = "PayPal" },
                            modifier = Modifier.weight(1f)
                        )
                        PaymentOptionButton(
                            name = "Credit Card",
                            label = "💳 Card / Debit",
                            isSelected = selectedPaymentMethod == "Credit Card",
                            onClick = { selectedPaymentMethod = "Credit Card" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentOptionButton(
                            name = "Google Pay",
                            label = "📱 Google Pay",
                            isSelected = selectedPaymentMethod == "Google Pay",
                            onClick = { selectedPaymentMethod = "Google Pay" },
                            modifier = Modifier.weight(1f)
                        )
                        PaymentOptionButton(
                            name = "Apple Pay",
                            label = "🍏 Apple Pay",
                            isSelected = selectedPaymentMethod == "Apple Pay",
                            onClick = { selectedPaymentMethod = "Apple Pay" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step 3: Simulation Engine Console
                    FlowStepHeader(stepNumber = 3, title = "API Sandbox & Webhook Handshake Simulation Console")
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Payment API Terminal Console",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isSimulating) Color.Green else Color.Gray)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Console message logs
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = if (uiState.currentUserProfile == null) {
                                        "🔴 ERROR: Authentication Required!\n\n> You must sign in or register an account before purchasing or simulating payment for Eddie Premium."
                                    } else if (simulationLog.isEmpty()) {
                                        "Ready for secure sandbox checkout simulation...\nPress the button below to process."
                                    } else {
                                        simulationLog
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = if (uiState.currentUserProfile == null) Color(0xFFFF5252) else Color(0xFF00FFCC),
                                    lineHeight = 16.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (uiState.currentUserProfile == null) {
                                Button(
                                    onClick = onRequireAuth,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Text(
                                        text = "SIGN IN / SIGN UP TO PROCEED",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            } else if (!isSimulating) {
                                Button(
                                    onClick = {
                                        isSimulating = true
                                        coroutineScope.launch {
                                            simulationLog = "> Initiating transaction for: $selectedPlan\n> Opening secure connection to payment gateway client..."
                                            delay(1000)
                                            simulationLog += "\n> [HTTP POST] Submitting invoice payload securely config token to server..."
                                            delay(1100)
                                            simulationLog += "\n> [GATEWAY] Created digital checkout invoice successfully ($selectedPaymentMethod)\n> Awaiting user authorization and verification callback..."
                                            delay(1200)
                                            simulationLog += "\n> [WEBHOOK] Callback received! Transaction cleared and settled via $selectedPaymentMethod."
                                            delay(900)
                                            simulationLog += "\n> STATUS RECONCILED: USER_PREMIUM_ACTIVE\n> Synchronizing application local configuration schema states..."
                                            delay(500)
                                            onActivate(selectedPlan)
                                            isSimulating = false
                                            Toast.makeText(context, "Congratulations! Your account features have been upgraded to Premium.", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB100)),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Text(
                                        text = "SIMULATE SECURE PAYMENT (${planPrice.uppercase()})",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Performing Gateway Handshake...")
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Secure • This is a full sandbox simulation. No real money will be charged.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FlowStepHeader(stepNumber: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFB100)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber.toString(),
                color = Color.Black,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun PlanCard(
    planName: String,
    price: String,
    period: String,
    description: String,
    badge: String? = null,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFFFFB100) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFB100).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isSelected, onClick = onSelect)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = planName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (badge != null) {
                    Surface(
                        color = Color(0xFFFFB100),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badge,
                            color = Color.Black,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(start = 32.dp)) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = period,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 32.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun PaymentOptionButton(
    name: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFFFFB100) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) Color(0xFFFFB100).copy(alpha = 0.05f) else Color.Transparent
        ),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
            color = if (isSelected) Color(0xFFFFB100) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FlowchartDiagram() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "API & Database Callback Sequence Flowchart Diagram:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Horizontal visual workflow diagram of the subscription system
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FlowDiagramItem(step = "1. CHOOSE PLAN", detail = "Select plan duration and payment method")
            FlowArrow()
            FlowDiagramItem(step = "2. GATEWAY HANDSHAKE", detail = "App hits secure API Server and parses checkout token")
            FlowArrow()
            FlowDiagramItem(step = "3. CALLBACK WEBHOOK", detail = "Verified payment confirmation is received by backend server")
            FlowArrow()
            FlowDiagramItem(step = "4. ROOM DB SYNC", detail = "Database updates user profile with Premium features unlocked")
        }
    }
}

@Composable
fun FlowDiagramItem(step: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(135.dp)
                .background(Color(0xFFFFB100).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFFFFB100).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFFFB100),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FlowArrow() {
    Box(
        modifier = Modifier
            .padding(start = 65.dp, top = 2.dp, bottom = 2.dp)
            .width(2.dp)
            .height(10.dp)
            .background(Color(0xFFFFB100).copy(alpha = 0.4f))
    )
}
