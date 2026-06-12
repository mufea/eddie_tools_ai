package com.example.ui.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EddieAiCodeStudioView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onGenerateCode: (String, String, String, String) -> Unit,
    onClearOutput: () -> Unit,
    onSubscriptionRequired: () -> Unit,
    onCopyText: (String) -> Unit,
    onShareText: (String) -> Unit
) {
    var selectedLanguage by remember { mutableStateOf("Kotlin") }
    var selectedTask by remember { mutableStateOf("Code Generation") }
    var targetLanguage by remember { mutableStateOf("Python") }
    var inputCodePrompt by remember { mutableStateOf("") }
    
    // Available popular languages in the world
    val programmingLanguages = listOf(
        "Kotlin" to false,       // Language name to IsPremium
        "Python" to false,
        "Java" to false,
        "C" to false,
        "PHP" to false,
        "Pascal" to false,
        "BASIC" to false,
        "Perl" to false,
        "Go" to true,
        "Rust" to true,
        "C++" to true,
        "TypeScript" to true,
        "C#" to true,
        "Swift" to true,
        "JavaScript" to true
    )

    // Code Tasks
    val codeActions = listOf(
        Triple("Code Generation", "Write new code from your functional description.", false),
        Triple("Bug Finder & Debugger", "Find errors, security bugs, and fix their code.", false),
        Triple("Refactor & Optimization", "Optimize algorithm efficiency, readability, and speed.", true),
        Triple("Language Converter", "Convert code structure from one language to another.", true)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color.Transparent)
    ) {
        // Header Developer Area (Sticky Header)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛠️", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Eddie AI Code Studio",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Intelligent assistant for code design, performance optimization, and programming language conversion.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Quota and Subscription Info Banner (Integrated plan logic)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isSubscribed) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    } else {
                        Color(0xFFFFB100).copy(alpha = 0.15f)
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (uiState.isSubscribed) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    } else {
                        Color(0xFFFFB100).copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (uiState.isSubscribed) "👑" else "⚡",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = if (uiState.isSubscribed) "Status: Eddie Developer PRO" else "Status: Free Trial",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFD48200)
                            )
                        }

                        if (!uiState.isSubscribed) {
                            Surface(
                                color = Color(0xFFFFB100),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${uiState.codeFreeQueriesLeft} Left",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (uiState.isSubscribed) {
                            "Unlimited Access Active! You have infinite quota to call the Gemini 3.5 & 3.1 Pro models for generating any language including Rust, Go, C++, and complex multi-language converters."
                        } else {
                            "AI Code Subscription Scheme: Free Account is limited to 3 queries / day for Kotlin, Python, and Java. Unlock Go, Rust, C++, Refactoring, & functional Language Converters with Eddie Premium subscription!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // STEP 1: PILIH BAHASA PEMROGRAMAN
            Text(
                text = "1. Choose Programming Language",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            // Wrapping language pills list
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                programmingLanguages.forEach { (lang, isPremium) ->
                    val isSelected = selectedLanguage == lang
                    val showProTag = isPremium && !uiState.isSubscribed
                    
                    Surface(
                        modifier = Modifier
                            .clickable {
                                selectedLanguage = lang
                            }
                            .clip(RoundedCornerShape(8.dp)),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            if (showProTag) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFFFB100),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // STEP 2: PILIH KATAGORI TUGAS / ACTION
            Text(
                text = "2. Choose AI-Code Task Type",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                codeActions.forEach { (action, subtitle, isPremium) ->
                    val isSelected = selectedTask == action
                    val showProTag = isPremium && !uiState.isSubscribed

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTask = action },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedTask = action }
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = action,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (showProTag) {
                                        Surface(
                                            color = Color(0xFFFFB100),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "PRO",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Target language selection if doing language conversion
            if (selectedTask == "Language Converter") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose Target Language:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    programmingLanguages.map { it.first }.forEach { lang ->
                        val isSelected = targetLanguage == lang
                        Surface(
                            modifier = Modifier
                                .clickable { targetLanguage = lang }
                                .clip(RoundedCornerShape(8.dp)),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = lang,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // STEP 3: INPUT AREA
            val inputLabelText = when (selectedTask) {
                "Code Generation" -> "Describe the program you want to build..."
                "Bug Finder & Debugger" -> "Paste your code here to debug..."
                "Refactor & Optimization" -> "Paste your code to optimize..."
                "Language Converter" -> "Paste your $selectedLanguage code to convert to $targetLanguage..."
                else -> "Enter text or description..."
            }

            Text(
                text = "3. Enter Description or Code",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = inputCodePrompt,
                onValueChange = { inputCodePrompt = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 300.dp),
                placeholder = {
                    Text(
                        text = inputLabelText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                ),
                maxLines = 15,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // CTA GENERATE
            Button(
                onClick = {
                    if (inputCodePrompt.isNotBlank()) {
                        onGenerateCode(selectedLanguage, selectedTask, targetLanguage, inputCodePrompt)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = !uiState.codeIsLoading && inputCodePrompt.isNotBlank()
            ) {
                if (uiState.codeIsLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "RUN EDDIE-CODE INTELLIGENCE ⚡",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // RESULTS WINDOW
            if (uiState.codeOutput.isNotBlank() || uiState.codeIsLoading) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💻 AI Code Processing Result",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { onCopyText(uiState.codeOutput) },
                            enabled = uiState.codeOutput.isNotBlank()
                        ) {
                            Text("📋", fontSize = 16.sp)
                        }
                        IconButton(
                            onClick = { onShareText(uiState.codeOutput) },
                            enabled = uiState.codeOutput.isNotBlank()
                        ) {
                            Text(" Share", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = onClearOutput
                        ) {
                            Text("🧹", fontSize = 16.sp)
                        }
                    }
                }

                if (uiState.codeIsLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Eddie AI is processing your program logic...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Model: ${uiState.selectedModel.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFB100),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (uiState.codeOutput.isNotBlank()) {
                    // Elevated beautiful Code Container
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E1E) // Absolute Pro Dark IDE background
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Syntax status bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Red))
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFB100)))
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Green))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$selectedLanguage - IDE Viewer",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = Color.LightGray
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = selectedTask,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Output text
                            SelectionContainer {
                                MarkdownText(
                                    text = uiState.codeOutput,
                                    color = Color(0xFFA9B7C6), // High contrast IDE white/gray style text
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.5.sp,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

