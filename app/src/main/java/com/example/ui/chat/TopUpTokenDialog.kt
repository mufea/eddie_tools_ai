package com.example.ui.chat

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ChatRepository.TopUpPackage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TopUpTokenDialog(
    uiState: ChatUiState,
    onDismiss: () -> Unit,
    onRefreshPackages: () -> Unit,
    onTopUpSuccess: (TopUpPackage, String) -> Unit,
    onRequireAuth: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val packages = uiState.topUpPackages.ifEmpty { defaultTopUpPackages }
    var selectedPackageId by remember(packages) {
        mutableStateOf(
            packages.firstOrNull { it.badge.equals("POPULAR", ignoreCase = true) }?.id
                ?: packages.firstOrNull()?.id.orEmpty()
        )
    }
    val selectedPackage = packages.firstOrNull { it.id == selectedPackageId } ?: packages.first()

    var selectedPaymentMethod by remember { mutableStateOf("PayPal") }
    var isSimulating by remember { mutableStateOf(false) }
    var simulationLog by remember { mutableStateOf("") }

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFB100).copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🪙", fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Top Up Token",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Sandbox Checkout • 5 Proxy Packages",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFB100),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefreshPackages,
                            enabled = !isSimulating && !uiState.isLoadingTopUpPackages,
                            modifier = Modifier.testTag("topup_refresh_packages_button")
                        ) {
                            if (uiState.isLoadingTopUpPackages) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh packages from proxy"
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            enabled = !isSimulating,
                            modifier = Modifier.testTag("topup_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Balance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Saldo Token Saat Ini",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.currentUserProfile?.email ?: "Belum Sign In (Guest)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.currentUserProfile != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "🪙 ${uiState.getFormattedTokens()}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFFFFB100)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Step 1: Select 5 Proxy Packages
                FlowStepHeader(stepNumber = 1, title = "Pilih Paket Top Up Token")
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    packages.forEach { pkg ->
                        TopUpPackageCard(
                            pkg = pkg,
                            isSelected = pkg.id == selectedPackage.id,
                            onSelect = { if (!isSimulating) selectedPackageId = pkg.id }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Step 2: Payment Method Selection
                FlowStepHeader(stepNumber = 2, title = "Metode Pembayaran (Sandbox)")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentOptionButton(
                        name = "PayPal",
                        label = "💳 PayPal",
                        isSelected = selectedPaymentMethod == "PayPal",
                        onClick = { if (!isSimulating) selectedPaymentMethod = "PayPal" },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentOptionButton(
                        name = "Credit Card",
                        label = "💳 Card / Debit",
                        isSelected = selectedPaymentMethod == "Credit Card",
                        onClick = { if (!isSimulating) selectedPaymentMethod = "Credit Card" },
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
                        onClick = { if (!isSimulating) selectedPaymentMethod = "Google Pay" },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentOptionButton(
                        name = "Apple Pay",
                        label = "🍏 Apple Pay",
                        isSelected = selectedPaymentMethod == "Apple Pay",
                        onClick = { if (!isSimulating) selectedPaymentMethod = "Apple Pay" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Step 3: Sandbox Console & Mandatory Auth Check
                FlowStepHeader(stepNumber = 3, title = "Sandbox Checkout Console")
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
                                text = "Token Top-Up Sandbox Console",
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

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                                .background(Color.Black, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (uiState.currentUserProfile == null) {
                                    "🔴 AUTHENTICATION REQUIRED!\n\n> Anda wajib Sign In / Sign Up terlebih dahulu sebelum melakukan Top Up Token."
                                } else if (simulationLog.isEmpty()) {
                                    "Ready for sandbox top-up checkout...\nPaket: ${selectedPackage.name} (+${selectedPackage.tokens} Tokens)\nTekan tombol di bawah untuk memproses."
                                } else {
                                    simulationLog
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (uiState.currentUserProfile == null) Color(0xFFFF5252) else Color(0xFF00FFCC),
                                lineHeight = 16.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.currentUserProfile == null) {
                            Button(
                                onClick = onRequireAuth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("topup_signin_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Text(
                                    text = "SIGN IN / SIGN UP TO TOP UP",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        } else if (!isSimulating) {
                            Button(
                                onClick = {
                                    isSimulating = true
                                    coroutineScope.launch {
                                        simulationLog = "> Initiating top-up for: ${selectedPackage.name} (+${selectedPackage.tokens} Tokens)\n> Connecting to sandbox payment gateway..."
                                        delay(900)
                                        simulationLog += "\n> [HTTP POST] Submitting top-up order (${selectedPackage.id}) via $selectedPaymentMethod..."
                                        delay(1000)
                                        simulationLog += "\n> [GATEWAY] Sandbox checkout invoice verified."
                                        delay(900)
                                        simulationLog += "\n> [WEBHOOK] Settlement confirmed! Crediting +${selectedPackage.tokens} tokens to ${uiState.currentUserProfile.email}..."
                                        delay(700)
                                        onTopUpSuccess(selectedPackage, selectedPaymentMethod)
                                        isSimulating = false
                                        Toast.makeText(
                                            context,
                                            "Top Up berhasil! +${selectedPackage.tokens} token ditambahkan ke akun Anda.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("topup_simulate_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB100)),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Text(
                                    text = "SIMULATE TOP UP (+${selectedPackage.tokens} TOKENS • ${selectedPackage.price})",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                                Text("Processing Sandbox Top-Up...")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            text = "Secure Sandbox • Daftar paket otomatis disinkronkan dari server proxy. Tidak ada penarikan uang sungguhan.",
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
private fun TopUpPackageCard(
    pkg: TopUpPackage,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("topup_package_${pkg.id}")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(selected = isSelected, onClick = onSelect)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = pkg.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "+${pkg.tokens} Tokens",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFD48200)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (!pkg.badge.isNullOrBlank()) {
                        Surface(
                            color = Color(0xFFFFB100),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = pkg.badge,
                                color = Color.Black,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = pkg.price,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (pkg.description.isNotBlank()) {
                Text(
                    text = pkg.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 42.dp, top = 4.dp)
                )
            }
        }
    }
}
