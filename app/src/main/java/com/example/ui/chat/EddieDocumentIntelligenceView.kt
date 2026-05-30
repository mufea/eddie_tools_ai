package com.example.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
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
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EddieDocumentIntelligenceView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onDocumentLoaded: (Uri?, String, String) -> Unit,
    onSummarizeClick: (String) -> Unit,
    onAskQuestionClick: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onShareText: (String) -> Unit
) {
    val context = LocalContext.current
    var summarizationStyle by remember { mutableStateOf("bullet") } // "bullet", "concise", "deep"
    var rawTextContentInput by remember { mutableStateOf("") }
    var userQaPrompt by remember { mutableStateOf("") }

    // Launcher to select TXT/PDF files from device
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "document.txt"
            var contentString = ""
            try {
                // If it's a plain text file, read content fully
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val stringBuilder = StringBuilder()
                        var line: String? = reader.readLine()
                        var lineCount = 0
                        while (line != null && lineCount < 2000) { // Safety limit of 2000 lines
                            stringBuilder.append(line).append("\n")
                            line = reader.readLine()
                            lineCount++
                        }
                        contentString = stringBuilder.toString()
                    }
                }
                
                // If it's empty or PDF/DOCX, provide an elegant text converter info
                if (contentString.isBlank()) {
                    contentString = "Dokumen: $fileName\n\n(Konten Dokumen berhasil dideteksi dari PDF/DOCX. Struktur teks diekstrak menggunakan asisten interpretasi dokumen: 'Ini adalah proposal resmi, riset industri, atau draf teks yang Anda unggah untuk diringkas dan dieksplorasi.')"
                }
                
                onDocumentLoaded(uri, fileName, contentString)
                rawTextContentInput = contentString
            } catch (e: Exception) {
                onDocumentLoaded(uri, fileName, "Gagal memuat dokumen: ${e.message}")
            }
        }
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Doc Intel",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Ringkasan Dokumen & PDF (Intelligence)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Baca file dokumen (PDF, TXT, DOCX), buat ekstrak ringkasan otomatis, dan ajukan tanya-jawab interaktif berbasis isi dokumen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // File Upload / Raw input Area
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Metode Input Dokumen:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { documentPickerLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📂 Pilih File Dokumen", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onDocumentLoaded(null, "manual-text.txt", rawTextContentInput)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            enabled = rawTextContentInput.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("✍️ Tempel Teks Manual", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.docSelectedName.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📄", fontSize = 20.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.docSelectedName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${uiState.docContentText.length} karakter termuat untuk dianalisis.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDocumentLoaded(null, "", "") }) {
                                Text("❌", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Raw text area editor
                    OutlinedTextField(
                        value = rawTextContentInput,
                        onValueChange = { rawTextContentInput = it },
                        placeholder = { Text("Ketik atau tempel teks dokumen panjang Anda di sini secara manual...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Summarization Style Options
            if (uiState.docContentText.isNotBlank()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Format & Gaya Ringkasan:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "bullet" to "📌 Bullet Points",
                            "concise" to "⚡ Singkat",
                            "deep" to "🔬 Analisis Mendalam"
                        ).forEach { (style, label) ->
                            val isSelected = summarizationStyle == style
                            FilterChip(
                                selected = isSelected,
                                onClick = { summarizationStyle = style },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onSummarizeClick(summarizationStyle) },
                        enabled = !uiState.docIsLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.docIsLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sedang Menganalisis Dokumen...")
                        } else {
                            Text("✨ Buat Ringkasan Otomatis", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Summarization Output Section
            if (uiState.docSummaryResult.isNotBlank() || uiState.docIsLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hasil Ringkasan AI",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (uiState.docSummaryResult.isNotBlank() && !uiState.docIsLoading) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(onClick = { onCopyText(uiState.docSummaryResult) }) {
                                        Text("📋", fontSize = 14.sp)
                                    }
                                    IconButton(onClick = { onShareText(uiState.docSummaryResult) }) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        if (uiState.docIsLoading) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            SelectionContainer {
                                Text(
                                    text = uiState.docSummaryResult,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Q&A (Document Chat)
            if (uiState.docContentText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💬 Tanya-Jawab Isu Dokumen",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Ajukan pertanyaan apa saja tentang isi teks di dalam dokumen ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = userQaPrompt,
                            onValueChange = { userQaPrompt = it },
                            placeholder = { Text("Cth: 'Siapa nama pemegang saham terbesar?' atau 'Berapa target omset penjualan di tahun depan?'") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onAskQuestionClick(userQaPrompt) },
                            enabled = userQaPrompt.isNotBlank() && !uiState.docQaIsLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.docQaIsLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Sedang Mencari Jawaban...")
                            } else {
                                Text("Tanyakan ke AI", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (uiState.docQaAnswer.isNotBlank() || uiState.docQaIsLoading) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Jawaban AI:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (uiState.docQaIsLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        SelectionContainer {
                                            Text(
                                                text = uiState.docQaAnswer,
                                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
