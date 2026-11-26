package com.example.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AgenticGuideDialog(
    onTryPrompt: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0F766E).copy(alpha = 0.14f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("⚡", fontSize = 20.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Agentic Guide",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Fitur otonom & pintasan pintar Prof Eddie",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_agentic_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AgenticFeatureCard(
                        icon = "🌐",
                        title = "Auto Web Search (Browsing Mandiri)",
                        description = "Otomatis mencari informasi real-time di internet saat prompt membahas berita terkini, cuaca, saham, pasar, atau memakai kata kunci seperti search, news, weather, stock.",
                        triggerHint = "Kata kunci: search, news, weather, stock, market",
                        samplePrompt = "Search latest AI technology news today",
                        accentColor = Color(0xFF0F766E),
                        testTag = "try_agentic_search_button",
                        onTryPrompt = {
                            onTryPrompt(it)
                            onDismiss()
                        }
                    )

                    AgenticFeatureCard(
                        icon = "🧠",
                        title = "Smart Long-Term Memory",
                        description = "Mengekstrak dan mengingat preferensi atau konteks penting Anda secara otomatis di latar belakang untuk percakapan berikutnya. Kelola memori tersimpan melalui tombol Memory di sidebar.",
                        triggerHint = "Otomatis aktif di latar belakang",
                        samplePrompt = "Ingat bahwa saya suka penjelasan ringkas dengan contoh kode Kotlin",
                        accentColor = Color(0xFF4F46E5),
                        testTag = "try_agentic_memory_button",
                        onTryPrompt = {
                            onTryPrompt(it)
                            onDismiss()
                        }
                    )

                    AgenticFeatureCard(
                        icon = "✉️",
                        title = "Action-to-API Automation (Email & WhatsApp)",
                        description = "Mendeteksi niat membuat draf email atau pesan WhatsApp, lalu memunculkan tombol aksi cepat (Open Email Draft / Send to WhatsApp) tepat di bawah jawaban AI.",
                        triggerHint = "Kata kunci: email, surel, surat, whatsapp, wa",
                        samplePrompt = "Buatkan draf email update progres proyek untuk manajer saya",
                        accentColor = Color(0xFFD97706),
                        testTag = "try_agentic_action_button",
                        onTryPrompt = {
                            onTryPrompt(it)
                            onDismiss()
                        }
                    )

                    AgenticFeatureCard(
                        icon = "🎨",
                        title = "Inline Commands & Multimodal (+)",
                        description = "Gunakan perintah /draw <deskripsi> (atau /image) untuk membuat gambar langsung di chat, atau ketuk tombol + untuk melampirkan foto dari Camera, Gallery, dan dokumen dari Files.",
                        triggerHint = "Perintah: /draw, /image, atau tombol +",
                        samplePrompt = "/draw Minimalist futuristic desk workspace with warm morning sunlight",
                        accentColor = Color(0xFF0284C7),
                        testTag = "try_agentic_draw_button",
                        onTryPrompt = {
                            onTryPrompt(it)
                            onDismiss()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
private fun AgenticFeatureCard(
    icon: String,
    title: String,
    description: String,
    triggerHint: String,
    samplePrompt: String,
    accentColor: Color,
    testTag: String,
    onTryPrompt: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = icon, fontSize = 18.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = triggerHint,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            OutlinedButton(
                onClick = { onTryPrompt(samplePrompt) },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(testTag)
            ) {
                Text(
                    text = "Coba Prompt: \"${if (samplePrompt.length > 42) samplePrompt.take(39) + "..." else samplePrompt}\"",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }
    }
}
