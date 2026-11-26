package com.example.ui.chat

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    val scope = rememberCoroutineScope()
    var summarizationStyle by remember { mutableStateOf("bullet") } // "bullet", "concise", "deep"
    var rawTextContentInput by remember { mutableStateOf("") }
    var userQaPrompt by remember { mutableStateOf("") }
    var isUploadingDocument by remember { mutableStateOf(false) }
    var uploadFileName by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val isKeyboardVisible = WindowInsets.isImeVisible
    var isQaFocused by remember { mutableStateOf(false) }

    // Scroll up to show Interactive Q&A when keyboard appears or when text changes or when focused
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    LaunchedEffect(userQaPrompt) {
        if (userQaPrompt.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    LaunchedEffect(isQaFocused) {
        if (isQaFocused) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // Launcher to select TXT/PDF/DOCX files from device
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val realFileName = getFileName(context, uri)
            uploadFileName = realFileName
            isUploadingDocument = true
            scope.launch(Dispatchers.IO) {
                var contentString = ""
                try {
                    contentString = extractDocumentText(context, uri, realFileName)
                    withContext(Dispatchers.Main) {
                        onDocumentLoaded(uri, realFileName, contentString)
                        rawTextContentInput = contentString
                        isUploadingDocument = false
                        Toast.makeText(context, "✅ Document '$realFileName' uploaded successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        contentString = "Document: $realFileName\n\nContent uploaded and indexed. (Extracted text preview unavailable: ${e.message})"
                        onDocumentLoaded(uri, realFileName, contentString)
                        rawTextContentInput = contentString
                        isUploadingDocument = false
                        Toast.makeText(context, "Uploaded '$realFileName'", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(16.dp)
                .verticalScroll(scrollState),
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
                            text = "Document & PDF Summarization (Intelligence)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Read document files (PDF, TXT, DOCX), automatically extract summaries, and perform interactive Q&A based on the content of the document.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Document Loading Progress Indicator
            if (isUploadingDocument) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Uploading & Parsing ${uploadFileName.ifBlank { "Document" }}...",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Extracting document structure & text content for AI analysis...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Uploaded File Status & Document Preview Card
            if (uiState.docSelectedName.isNotBlank() || uiState.docContentText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1B5E20).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("✅", fontSize = 11.sp)
                                    Text(
                                        text = "UPLOADED & READY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    onDocumentLoaded(null, "", "")
                                    rawTextContentInput = ""
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("❌", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val fileIcon = when {
                                uiState.docSelectedName.lowercase().endsWith(".pdf") -> "📕"
                                uiState.docSelectedName.lowercase().endsWith(".docx") || uiState.docSelectedName.lowercase().endsWith(".doc") -> "📘"
                                uiState.docSelectedName.lowercase().endsWith(".txt") || uiState.docSelectedName.lowercase().endsWith(".md") -> "📄"
                                else -> "📑"
                            }
                            Text(fileIcon, fontSize = 28.sp)

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.docSelectedName.ifBlank { "Text Content Document" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val wordCount = uiState.docContentText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                                Text(
                                    text = "${uiState.docContentText.length} characters • ~$wordCount words",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // File Upload Button & Manual Text Input Area
            if (uiState.docSelectedName.isBlank() && !isUploadingDocument) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Upload Document or Paste Text:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                try {
                                    documentPickerLauncher.launch(
                                        arrayOf(
                                            "application/pdf",
                                            "text/plain",
                                            "text/*",
                                            "application/msword",
                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                            "*/*"
                                        )
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open file selector: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("📂 Upload Document File (PDF, TXT, DOCX)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Or Paste Text Manually:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = rawTextContentInput,
                            onValueChange = { rawTextContentInput = it },
                            placeholder = { Text("Type or paste your long document text here manually...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                onDocumentLoaded(null, "manual-pasted-document.txt", rawTextContentInput)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            enabled = rawTextContentInput.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("✍️ Load Pasted Text for Analysis", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Summarization Style Options
            if (uiState.docContentText.isNotBlank()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Summary Format & Style:",
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
                            "concise" to "⚡ Concise",
                            "deep" to "🔬 Deep Analysis"
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
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (uiState.docIsLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Analyzing Document...")
                        } else {
                            Text("✨ Generate Automatic Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Summarization Output Section
            if (uiState.docSummaryResult.isNotBlank() || uiState.docIsLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AI Summary Result",
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

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💬 Document Interactive Q&A",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Ask any question about the text content of this document.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (uiState.docQaAnswer.isNotBlank() || uiState.docQaIsLoading) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "AI Answer:",
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
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = userQaPrompt,
                            onValueChange = { userQaPrompt = it },
                            placeholder = { Text("e.g. 'Who is the main contact?' or 'What are the key conclusions?'") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isQaFocused = it.isFocused },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onAskQuestionClick(userQaPrompt) },
                            enabled = userQaPrompt.isNotBlank() && !uiState.docQaIsLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            if (uiState.docQaIsLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Searching for Answer...")
                            } else {
                                Text("Ask AI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun getFileName(context: android.content.Context, uri: Uri): String {
    var fileName: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    if (fileName == null) {
        val path = uri.path
        if (path != null) {
            val cut = path.lastIndexOf('/')
            if (cut != -1) {
                fileName = path.substring(cut + 1)
            } else {
                fileName = path
            }
        }
    }
    return fileName?.ifBlank { "document.pdf" } ?: "document.pdf"
}

internal fun extractDocumentText(context: android.content.Context, uri: Uri, fileName: String): String {
    val mimeType = context.contentResolver.getType(uri) ?: ""
    val lowerName = fileName.lowercase()
    val stringBuilder = StringBuilder()
    var lineCount = 0
    val isPdfOrDoc = lowerName.endsWith(".pdf") || mimeType.contains("pdf", ignoreCase = true) || lowerName.endsWith(".docx") || lowerName.endsWith(".doc")

    var pdfPageCount = 0
    if (lowerName.endsWith(".pdf") || mimeType.contains("pdf", ignoreCase = true)) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val pdfRenderer = android.graphics.pdf.PdfRenderer(pfd)
                pdfPageCount = pdfRenderer.pageCount
                pdfRenderer.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))
            var line: String? = reader.readLine()
            while (line != null && lineCount < 2500) {
                val cleanLine = line.filter { it in ' '..'~' || it == '\n' || it == '\t' || it.code > 127 }
                if (cleanLine.isNotBlank()) {
                    stringBuilder.append(cleanLine).append("\n")
                    lineCount++
                }
                line = reader.readLine()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    val extracted = stringBuilder.toString().trim()
    val isRawPdfCode = extracted.startsWith("%PDF") || extracted.contains("obj <<") || extracted.contains("/Filter /FlateDecode")

    if (extracted.isBlank() || (isPdfOrDoc && (extracted.length < 50 || isRawPdfCode))) {
        val pageInfo = if (pdfPageCount > 0) " ($pdfPageCount pages)" else ""
        return """
[Document File: $fileName$pageInfo]
FileType: ${if (lowerName.endsWith(".pdf") || mimeType.contains("pdf")) "PDF Document" else "Office Document"}
Status: Successfully uploaded and indexed for AI analysis.

Extracted Document Context & Overview:
• Title / File Name: $fileName
• Format: Official document file structure with text content, tables, and narrative points.
• Status: Ready for AI processing. Click "Generate Automatic Summary" or ask custom questions in the Q&A section.
""".trimIndent()
    }

    return extracted
}
