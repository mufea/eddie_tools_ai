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
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // UI selection states for flow steps
    var selectedPlan by remember { mutableStateOf("Bulanan") }
    var selectedPaymentMethod by remember { mutableStateOf("GoPay") }
    
    // Simulated engine states
    var isSimulating by remember { mutableStateOf(false) }
    var simulationLog by remember { mutableStateOf("") }
    
    val planPrice = when (selectedPlan) {
        "Mingguan" -> "Rp 15.000"
        "Bulanan" -> "Rp 49.000"
        "Tahunan" -> "Rp 299.000"
        else -> "Rp 0"
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
                                text = "Pro Akses & Simulasi Alur Pembayaran",
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
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
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
                            Text("✨ STATUS LANGGANAN AKTIF ✨", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Selamat! Anda memiliki paket Premium (${uiState.activeSubscriptionPlan}) aktif.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Masa berlaku hingga: ${uiState.subscriptionExpiryDate}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Dengan Premium, Anda telah membuka kekuatan penuh Eddie Pro yang memiliki kecerdasan superior untuk penalaran kompleks, melepas batasan seni di Eddie AI Art Studio (akses tanpa batas ke model gaya Pro dan rasio HD), serta membuka fitur Eddie Video Studio secara penuh (menghilangkan batas maksimal pembuatan 2 video gratis, serta membuka durasi premium 10s - 30s, resolusi Full HD & 4K, serta rasio aspek vertikal 9:16 untuk Reel/Shorts/TikTok dan rasio sinematik 21:9).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onCancel,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Batalkan Berlangganan (Simulasi)")
                            }
                        }
                    }
                } else {
                    // Subscription Flow Scheme
                    Text(
                        text = "SKEMA DIAGRAM ALUR PEMBAYARAN",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Unified diagram representation showing step layout in flow chart style
                    FlowchartDiagram()

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step 1: Select Plan
                    FlowStepHeader(stepNumber = 1, title = "Pilih Paket Langganan Kontrak")
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlanCard(
                            planName = "Mingguan",
                            price = "Rp 15.000",
                            period = "/ minggu",
                            description = "Cocok untuk mencoba kemampuan chat Eddie Pro, serta buka akses untuk gambar dan video AI premium.",
                            isSelected = selectedPlan == "Mingguan",
                            onSelect = { selectedPlan = "Mingguan" }
                        )
                        PlanCard(
                            planName = "Bulanan",
                            price = "Rp 49.000",
                            period = "/ bulan",
                            description = "Pilihan terbaik. Chat prioritas tanpa batas, lukis sepuasnya di Art Studio dengan rasio HD, serta render video Ultra HD tanpa batas.",
                            badge = "TERAGUNG",
                            isSelected = selectedPlan == "Bulanan",
                            onSelect = { selectedPlan = "Bulanan" }
                        )
                        PlanCard(
                            planName = "Tahunan",
                            price = "Rp 299.000",
                            period = "/ tahun",
                            description = "Sangat hemat. Akses penuh ke semua fitur premium chat, gambar AI artistik, dan studio video AI resolusi tinggi selama satu tahun penuh.",
                            badge = "SAVER 50%",
                            isSelected = selectedPlan == "Tahunan",
                            onSelect = { selectedPlan = "Tahunan" }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step 2: Payment Method Selection
                    FlowStepHeader(stepNumber = 2, title = "Metode Pembayaran (E-Wallet & Bank Transfer)")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentOptionButton(
                            name = "GoPay",
                            label = "📱 GoPay",
                            isSelected = selectedPaymentMethod == "GoPay",
                            onClick = { selectedPaymentMethod = "GoPay" },
                            modifier = Modifier.weight(1f)
                        )
                        PaymentOptionButton(
                            name = "ShopeePay",
                            label = "🛍️ ShopeePay",
                            isSelected = selectedPaymentMethod == "ShopeePay",
                            onClick = { selectedPaymentMethod = "ShopeePay" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentOptionButton(
                            name = "Dana",
                            label = "💰 DANA",
                            isSelected = selectedPaymentMethod == "Dana",
                            onClick = { selectedPaymentMethod = "Dana" },
                            modifier = Modifier.weight(1f)
                        )
                        PaymentOptionButton(
                            name = "VA Mandiri",
                            label = "🏦 Bank VA",
                            isSelected = selectedPaymentMethod == "VA Mandiri",
                            onClick = { selectedPaymentMethod = "VA Mandiri" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step 3: Simulation Engine Console
                    FlowStepHeader(stepNumber = 3, title = "Simulator Sandbox Pembayaran API & Webhook Check")
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
                                    text = if (simulationLog.isEmpty()) "Siap melakukan simulasi checkout aman...\nTekan tombol di bawah untuk memproses." else simulationLog,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF00FFCC),
                                    lineHeight = 16.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (!isSimulating) {
                                Button(
                                    onClick = {
                                        isSimulating = true
                                        coroutineScope.launch {
                                            simulationLog = "> Memulai transaksi untuk paket: $selectedPlan\n> Menghubungkan client ke API Payment Gateway..."
                                            delay(1000)
                                            simulationLog += "\n> [HTTP POST] Mengirim payload order token ke server gateway..."
                                            delay(1100)
                                            simulationLog += "\n> [GATEWAY] Pembuatan invoice pembayaran sukses ($selectedPaymentMethod)\n> Menunggu respon pembayaran user (Callback Webhook)..."
                                            delay(1200)
                                            simulationLog += "\n> [WEBHOOK] Callback transaksi Sukses diterima dari $selectedPaymentMethod!\n> Status Database sinkron: PREMIUM_ACTIVE"
                                            delay(900)
                                            simulationLog += "\n> PRO AKTIF! Fitur Eddie Pro Berhasil Terbuka."
                                            delay(500)
                                            onActivate(selectedPlan)
                                            isSimulating = false
                                            Toast.makeText(context, "Selamat! Sessi Anda telah ter-upgrade ke Premium.", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB100)),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Text(
                                        text = "SIMULASI BAYAR ${planPrice.uppercase()} VIA ${selectedPaymentMethod.uppercase()}",
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
                                    Text("Sedang Berjabat Tangan dengan Gateway...")
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
                            text = "Aman • Ini adalah sandbox simulasi penuh. Tidak ada saldo asli Anda yang akan digunakan atau dipotong.",
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
            text = "Diagram Skema Transaksi Berlangganan API & Database Call:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Horizontal visual workflow diagram of the subscription system
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FlowDiagramItem(step = "1. PILIH PAKET", detail = "Memilih Durasi Langganan & E-Wallet")
            FlowArrow()
            FlowDiagramItem(step = "2. GATEWAY HANDSHAKE", detail = "App hit API Server & generate secure token")
            FlowArrow()
            FlowDiagramItem(step = "3. CALLBACK WEBHOOK", detail = "Payment Gateway kirim status lunas ke Server")
            FlowArrow()
            FlowDiagramItem(step = "4. ROOM DB SYNC", detail = "Membuka model Eddie Pro & badge Premium")
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
