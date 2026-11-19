package com.example.ui.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ApiKeySettingsDialog(
    uiState: ChatUiState,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Local states for editing
    var proxyBaseUrl by remember { mutableStateOf(uiState.proxyBaseUrl.ifBlank { "https://geminiaitools.com/" }) }
    var openaiKey by remember { mutableStateOf(uiState.openaiApiKey) }
    var openaiBaseUrl by remember { mutableStateOf(uiState.openaiBaseUrl) }
    var deepseekKey by remember { mutableStateOf(uiState.deepseekApiKey) }
    var deepseekBaseUrl by remember { mutableStateOf(uiState.deepseekBaseUrl) }
    var claudeKey by remember { mutableStateOf(uiState.claudeApiKey) }
    var claudeBaseUrl by remember { mutableStateOf(uiState.claudeBaseUrl) }
    var xaiKey by remember { mutableStateOf(uiState.xaiApiKey) }
    var xaiBaseUrl by remember { mutableStateOf(uiState.xaiBaseUrl) }
    var falKey by remember { mutableStateOf(uiState.falApiKey) }
    var stabilityKey by remember { mutableStateOf(uiState.stabilityApiKey) }

    // Toggle visibility of secret inputs
    var showOpenaiKey by remember { mutableStateOf(false) }
    var showDeepseekKey by remember { mutableStateOf(false) }
    var showClaudeKey by remember { mutableStateOf(false) }
    var showXaiKey by remember { mutableStateOf(false) }
    var showFalKey by remember { mutableStateOf(false) }
    var showStabilityKey by remember { mutableStateOf(false) }

    // Proxy diagnostic state
    var isCheckingProxy by remember { mutableStateOf(false) }
    var isSyncingModels by remember { mutableStateOf(false) }
    var proxyStatusMessage by remember { mutableStateOf<String?>(null) }
    var proxyStatusSuccess by remember { mutableStateOf(false) }
    var showThemeDialogInSettings by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
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
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔑", fontSize = 20.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "API Keys & Proxy Settings",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Configure proxy domain & model credentials",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 0. TEMA SKIN APLIKASI
                    val currentSkin = com.example.ui.theme.AppSkinTheme.fromId(uiState.appSkinThemeId)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🎨", fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = "Tema Skin Aplikasi",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Tema saat ini: ${currentSkin.title} (${if (currentSkin.isDark) "Gelap" else "Terang"})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = { showThemeDialogInSettings = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Ganti Skin", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // 1. PROXY CONFIGURATION (geminiaitools.com)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🌐", fontSize = 18.sp)
                                Text(
                                    text = "Proxy Server (geminiaitools.com)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = "The proxy geminiaitools.com securely routes AI requests to models with pre-configured API keys. You can also provide personal API keys below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = proxyBaseUrl,
                                onValueChange = { proxyBaseUrl = it },
                                label = { Text("Proxy Domain / Base URL") },
                                placeholder = { Text("https://geminiaitools.com/") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    if (proxyBaseUrl.trimEnd('/') != "https://geminiaitools.com") {
                                        TextButton(onClick = { proxyBaseUrl = "https://geminiaitools.com/" }) {
                                            Text("Default", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            )

                            // Action buttons: Test Proxy & Sync Dynamic Models
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        isCheckingProxy = true
                                        proxyStatusMessage = null
                                        viewModel.testProxyConnection(proxyBaseUrl) { success, msg ->
                                            isCheckingProxy = false
                                            proxyStatusSuccess = success
                                            proxyStatusMessage = msg
                                        }
                                    },
                                    enabled = !isCheckingProxy && !isSyncingModels,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isCheckingProxy) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Testing...", fontSize = 12.sp)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Test Proxy", fontSize = 12.sp)
                                    }
                                }

                                FilledTonalButton(
                                    onClick = {
                                        isSyncingModels = true
                                        val targetProxy = proxyBaseUrl.ifBlank { "https://geminiaitools.com/" }
                                        viewModel.fetchDynamicModelsFromProxy(targetProxy) { success, models, msg ->
                                            isSyncingModels = false
                                            proxyStatusSuccess = success
                                            proxyStatusMessage = if (success) {
                                                "✅ Model ID Berhasil Diupdate!\n" +
                                                models?.joinToString("\n") { "• ${it.displayName}: ${it.apiValue}" }
                                            } else {
                                                "❌ Gagal: $msg"
                                            }
                                        }
                                    },
                                    enabled = !isCheckingProxy && !isSyncingModels,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    if (isSyncingModels) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ambil Model...", fontSize = 12.sp)
                                    } else {
                                        Text("⚡", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sync Model ID", fontSize = 12.sp)
                                    }
                                }
                            }

                            // Diagnostic result card
                            proxyStatusMessage?.let { status ->
                                Surface(
                                    color = if (status.contains("ONLINE", ignoreCase = true)) {
                                        Color(0xFFE8F5E9)
                                    } else if (status.contains("Reachable", ignoreCase = true) || status.contains("expired", ignoreCase = true)) {
                                        Color(0xFFFFF3E0)
                                    } else {
                                        Color(0xFFFFEBEE)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (status.contains("ONLINE", ignoreCase = true)) "✅" 
                                                   else if (status.contains("Reachable", ignoreCase = true)) "⚠️" 
                                                   else "❌",
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = status,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            ),
                                            color = Color.Black
                                        )
                                    }
                                }
                            }

                            // Apply to all models action
                            OutlinedButton(
                                onClick = {
                                    val targetProxy = proxyBaseUrl.ifBlank { "https://geminiaitools.com/" }
                                    openaiBaseUrl = targetProxy
                                    deepseekBaseUrl = targetProxy
                                    claudeBaseUrl = targetProxy
                                    xaiBaseUrl = targetProxy
                                    Toast.makeText(context, "Proxy applied to OpenAI, DeepSeek, Claude & xAI!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("⚡ Route All Models via This Proxy", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // 1. OPENAI
                    ProviderKeyCard(
                        providerName = "OpenAI",
                        icon = "🟢",
                        description = "Powers GPT 5 Mini and GPT 5.5 reasoning models.",
                        key = openaiKey,
                        onKeyChange = { openaiKey = it },
                        showKey = showOpenaiKey,
                        onToggleShowKey = { showOpenaiKey = !showOpenaiKey },
                        hint = "sk-proj-...",
                        customUrl = openaiBaseUrl,
                        onCustomUrlChange = { openaiBaseUrl = it },
                        urlPlaceholder = "https://api.openai.com/v1/chat/completions"
                    )

                    // 2. DEEPSEEK
                    ProviderKeyCard(
                        providerName = "DeepSeek",
                        icon = "🐋",
                        description = "Powers DeepSeek V4 Flash & DeepSeek V4 Pro models.",
                        key = deepseekKey,
                        onKeyChange = { deepseekKey = it },
                        showKey = showDeepseekKey,
                        onToggleShowKey = { showDeepseekKey = !showDeepseekKey },
                        hint = "sk-...",
                        customUrl = deepseekBaseUrl,
                        onCustomUrlChange = { deepseekBaseUrl = it },
                        urlPlaceholder = "https://api.deepseek.com/v1/chat/completions"
                    )

                    // 3. ANTHROPIC CLAUDE
                    ProviderKeyCard(
                        providerName = "Anthropic Claude",
                        icon = "🟧",
                        description = "Powers Claude Sonnet 4.6 and Claude Opus 5.",
                        key = claudeKey,
                        onKeyChange = { claudeKey = it },
                        showKey = showClaudeKey,
                        onToggleShowKey = { showClaudeKey = !showClaudeKey },
                        hint = "sk-ant-...",
                        customUrl = claudeBaseUrl,
                        onCustomUrlChange = { claudeBaseUrl = it },
                        urlPlaceholder = "https://api.anthropic.com/v1/messages"
                    )

                    // 4. xAI (GROK)
                    ProviderKeyCard(
                        providerName = "xAI (Grok)",
                        icon = "⚫",
                        description = "Powers Grok 4.3 next-generation reasoning model.",
                        key = xaiKey,
                        onKeyChange = { xaiKey = it },
                        showKey = showXaiKey,
                        onToggleShowKey = { showXaiKey = !showXaiKey },
                        hint = "xai-...",
                        customUrl = xaiBaseUrl,
                        onCustomUrlChange = { xaiBaseUrl = it },
                        urlPlaceholder = "https://api.x.ai/v1/chat/completions"
                    )

                    // 5. FAL.AI (VIDEO GEN)
                    ProviderKeyCard(
                        providerName = "Fal.ai",
                        icon = "🎬",
                        description = "Powers Veo 3 and Wan video generation models.",
                        key = falKey,
                        onKeyChange = { falKey = it },
                        showKey = showFalKey,
                        onToggleShowKey = { showFalKey = !showFalKey },
                        hint = "fal_..."
                    )

                    // 6. STABILITY AI / STABLE DIFFUSION (IMAGE GEN)
                    ProviderKeyCard(
                        providerName = "Stability AI (Stable Diffusion)",
                        icon = "🎨",
                        description = "Powers Stable Diffusion 3, SDXL, and Ultra image generation.",
                        key = stabilityKey,
                        onKeyChange = { stabilityKey = it },
                        showKey = showStabilityKey,
                        onToggleShowKey = { showStabilityKey = !showStabilityKey },
                        hint = "sk-..."
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            // Update Base URLs with fallback to proxy
                            val defaultProxy = proxyBaseUrl.ifBlank { "https://geminiaitools.com/" }
                            viewModel.updateProxyBaseUrl(defaultProxy)
                            viewModel.updateProviderBaseUrl("OpenAI", openaiBaseUrl.ifBlank { defaultProxy })
                            viewModel.updateProviderBaseUrl("DeepSeek", deepseekBaseUrl.ifBlank { defaultProxy })
                            viewModel.updateProviderBaseUrl("Claude", claudeBaseUrl.ifBlank { defaultProxy })
                            viewModel.updateProviderBaseUrl("XAI", xaiBaseUrl.ifBlank { defaultProxy })

                            // Update API Keys
                            viewModel.updateProviderApiKey("OpenAI", openaiKey)
                            viewModel.updateProviderApiKey("DeepSeek", deepseekKey)
                            viewModel.updateProviderApiKey("Claude", claudeKey)
                            viewModel.updateProviderApiKey("XAI", xaiKey)
                            viewModel.updateProviderApiKey("FalAI", falKey)
                            viewModel.updateProviderApiKey("Stability", stabilityKey)

                            Toast.makeText(context, "API Keys & Proxy settings saved successfully!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Apply")
                    }
                }
            }
        }
    }

    if (showThemeDialogInSettings) {
        ThemeSelectorDialog(
            currentThemeId = uiState.appSkinThemeId,
            onSelectTheme = { skin ->
                viewModel.setAppSkinTheme(skin.id)
            },
            onDismiss = { showThemeDialogInSettings = false }
        )
    }
}

@Composable
private fun ProviderKeyCard(
    providerName: String,
    icon: String,
    description: String,
    key: String,
    onKeyChange: (String) -> Unit,
    showKey: Boolean,
    onToggleShowKey: () -> Unit,
    hint: String,
    customUrl: String? = null,
    onCustomUrlChange: ((String) -> Unit)? = null,
    urlPlaceholder: String? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(14.dp),
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
                Text(icon, fontSize = 16.sp)
                Text(
                    text = providerName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = key,
                onValueChange = onKeyChange,
                label = { Text("$providerName API Key") },
                placeholder = { Text(hint) },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = onToggleShowKey) {
                        Text(if (showKey) "👁️" else "🔒", fontSize = 14.sp)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            if (customUrl != null && onCustomUrlChange != null && urlPlaceholder != null) {
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = onCustomUrlChange,
                    label = { Text("Custom Base URL (Optional)") },
                    placeholder = { Text(urlPlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}
