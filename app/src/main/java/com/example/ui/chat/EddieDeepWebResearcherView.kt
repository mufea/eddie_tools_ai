package com.example.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EddieDeepWebResearcherView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onLaunchResearch: (String) -> Unit,
    onClear: () -> Unit,
    onShare: (String) -> Unit
) {
    var topicInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    LaunchedEffect(topicInput) {
        if (topicInput.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color.Transparent)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "AI Deep Web Researcher",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Executes a multi-stage research agent: formulates smart queries, autonomously scrapes various web article links, and synthesizes a comprehensive report.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState.researchReport.isBlank() && !uiState.researchIsLoading) {
                // Topic Form Panel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "START NEW RESEARCH AGENT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            label = { Text("Deep Research Topic") },
                            placeholder = { Text("e.g., Solid-State Battery Technology 2026 or Global AI Regulation") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Text(
                            text = "⚠️ Multi-stage research takes about 5-8 seconds as our autonomous agent simulates a multi-threaded web crawler and analyzes factual consistency before synthesizing the final details.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        Button(
                            onClick = {
                                if (topicInput.isNotBlank()) {
                                    onLaunchResearch(topicInput)
                                } else {
                                    Toast.makeText(context, "Please enter a research topic first.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch Autonomous Deep Research")
                        }
                    }
                }
            }

            // Progress log view if loading
            if (uiState.researchIsLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Green
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DEEP SEARCH PROCESS LOGS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    color = Color.Green
                                )
                            }
                            Text(
                                text = "LIVE TERMINAL",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Render live scraping logs in classic hacker terminal green
                        uiState.researchProgressLogs.forEach { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                color = Color.Green.copy(alpha = 0.9f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Synthesized Report Result
            if (uiState.researchReport.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "FINAL SYNTHESIZED REPORT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Topic: ${uiState.researchTopic}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(uiState.researchReport))
                                    Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Copy Report")
                                }
                                IconButton(onClick = { onShare(uiState.researchReport) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Report")
                                }
                                IconButton(onClick = onClear) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear/Reset")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(16.dp))

                        SelectionContainer {
                            Text(
                                text = uiState.researchReport,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Restart Research button
                Button(
                    onClick = onClear,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Research Another Topic")
                }
            }
        }
    }
}
