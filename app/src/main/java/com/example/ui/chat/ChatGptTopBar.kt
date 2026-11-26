package com.example.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatGptTopBar(
    activeTab: String,
    selectedModel: AiModel,
    availableModels: List<AiModel>,
    customModels: List<AiModel>,
    onOpenDrawer: () -> Unit,
    onNewChatClick: () -> Unit,
    onSelectModel: (AiModel) -> Unit,
    onDeleteCustomModel: (String) -> Unit,
    onDeleteAllCustomModels: () -> Unit,
    onOpenAddModelDialog: () -> Unit,
    onBackToChat: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pillContainerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val pillBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.5f else 0.7f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Left squircle sidebar toggle button
        Surface(
            onClick = onOpenDrawer,
            shape = RoundedCornerShape(14.dp),
            color = pillContainerColor,
            shadowElevation = if (isDark) 0.dp else 2.dp,
            border = BorderStroke(0.75.dp, pillBorderColor),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .testTag("open_sidebar_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                ChatGptMenuIcon(
                    tint = MaterialTheme.colorScheme.onSurface,
                    size = 20.dp
                )
            }
        }

        // Center subtle model / active mode pill
        if (activeTab == "chat") {
            var dropdownExpanded by remember { mutableStateOf(false) }
            val menuScope = rememberCoroutineScope()

            fun dismissTopMenuThen(action: () -> Unit) {
                dropdownExpanded = false
                menuScope.launch {
                    delay(80)
                    action()
                }
            }

            DisposableEffect(Unit) {
                onDispose { dropdownExpanded = false }
            }

            Box(modifier = Modifier.align(Alignment.Center)) {
                Surface(
                    onClick = { dropdownExpanded = true },
                    shape = RoundedCornerShape(16.dp),
                    color = pillContainerColor.copy(alpha = 0.92f),
                    border = BorderStroke(0.75.dp, pillBorderColor.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .testTag("model_selector_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(0xFF10B981))
                        )
                        Text(
                            text = "Prof Eddie",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "· ${selectedModel.displayName}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 110.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Provider & Model",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier
                        .width(320.dp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val allModels = availableModels + customModels
                    val groupedModels = allModels.groupBy { it.provider }
                    groupedModels.forEach { (provider, models) ->
                        Text(
                            text = provider.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        models.forEach { model ->
                            val isSelected = model.apiValue == selectedModel.apiValue
                            val isCustom = customModels.any { it.apiValue == model.apiValue }
                            val modelIdText = model.apiValue.ifBlank { model.displayName }
                            val cleanDescription = model.subtitle
                                .replace(" (${model.apiValue})", "")
                                .trim()
                                .ifBlank { model.displayName }
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = modelIdText,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    fontSize = 12.5.sp
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = cleanDescription,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isCustom) {
                                            IconButton(
                                                onClick = { onDeleteCustomModel(model.apiValue) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Hapus Model",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    dismissTopMenuThen { onSelectModel(model) }
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }

                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Tambah Model AI Baru",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        onClick = {
                            dismissTopMenuThen { onOpenAddModelDialog() }
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    )

                    if (customModels.isNotEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Hapus Semua Model",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            onClick = {
                                dismissTopMenuThen { onDeleteAllCustomModels() }
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        } else {
            val tabTitle = when (activeTab) {
                "image_gen" -> "Image Studio"
                "video_gen" -> "Video Studio"
                "ai_code" -> "Code Studio"
                "music_gen" -> "Music Studio"
                "voice_ai" -> "Voice Mode"
                "ai_vision" -> "Vision Analyzer"
                "doc_intelligence" -> "Doc & PDF"
                "math_tutor" -> "Math & Science"
                "web_researcher" -> "Web Researcher"
                "ai_writer" -> "Copywriter"
                "social_gen" -> "Social Studio"
                "token_history" -> "Token History"
                "photo_tourism_ar_guide" -> "Photo Tourism AR"
                "sfx_generator" -> "SFX Studio"
                else -> "Prof Eddie"
            }

            Surface(
                onClick = onBackToChat,
                shape = RoundedCornerShape(16.dp),
                color = pillContainerColor.copy(alpha = 0.92f),
                border = BorderStroke(0.75.dp, pillBorderColor.copy(alpha = 0.6f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .heightIn(min = 38.dp)
                    .testTag("active_tab_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tabTitle,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "· Chat",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Right squircle new chat button
        Surface(
            onClick = onNewChatClick,
            shape = RoundedCornerShape(14.dp),
            color = pillContainerColor,
            shadowElevation = if (isDark) 0.dp else 2.dp,
            border = BorderStroke(0.75.dp, pillBorderColor),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(44.dp)
                .testTag("new_chat_top_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                ChatGptNewChatComposeIcon(
                    tint = MaterialTheme.colorScheme.onSurface,
                    size = 19.dp
                )
            }
        }
    }
}
