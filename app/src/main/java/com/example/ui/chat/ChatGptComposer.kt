package com.example.ui.chat

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatGptComposer(
    inputText: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onVoiceModeClick: () -> Unit,
    isLoading: Boolean,
    isWebSearchEnabled: Boolean,
    onWebSearchToggle: (Boolean) -> Unit,
    selectedImageProvider: String = "OpenAI",
    selectedImageModel: String = "gpt-image-1.5",
    onEngineSelected: (provider: String, model: String) -> Unit = { _, _ -> },
    attachedImageBitmap: Bitmap? = null,
    attachedImageLabel: String? = null,
    attachedFileName: String? = null,
    onClearAttachment: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onFilesClick: () -> Unit = {}
) {
    val isDrawMode = inputText.startsWith("/draw", ignoreCase = true) ||
            inputText.startsWith("/image", ignoreCase = true) ||
            inputText.startsWith("/generate", ignoreCase = true)

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var showAttachmentSheet by remember { mutableStateOf(false) }
    val attachmentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = rememberCoroutineScope()

    fun dismissAttachmentSheetAndRun(action: () -> Unit = {}) {
        sheetScope.launch {
            try {
                attachmentSheetState.hide()
            } finally {
                showAttachmentSheet = false
                action()
            }
        }
    }

    val cleanPromptText: () -> String = {
        when {
            inputText.startsWith("/draw", ignoreCase = true) -> inputText.substring(5).trim()
            inputText.startsWith("/image", ignoreCase = true) -> inputText.substring(6).trim()
            inputText.startsWith("/generate", ignoreCase = true) -> inputText.substring(9).trim()
            else -> inputText
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 12.dp)
    ) {
        // Pending attachment preview (Camera / Gallery / Files)
        AnimatedVisibility(visible = attachedImageBitmap != null || attachedFileName != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (attachedImageBitmap != null) {
                        Image(
                            bitmap = attachedImageBitmap.asImageBitmap(),
                            contentDescription = attachedImageLabel ?: "Attached photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = attachedImageLabel ?: "Attached photo",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Image ready for Prof Eddie",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (attachedFileName != null) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            ChatGptFileIcon(
                                tint = MaterialTheme.colorScheme.primary,
                                size = 22.dp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = attachedFileName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "File attached",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onClearAttachment,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("clear_attachment_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove attachment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Conditional active mode chips (hidden by default to keep home screen airy)
        AnimatedVisibility(visible = isDrawMode || isWebSearchEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isWebSearchEnabled) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🌐 Search the web",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Disable Web Search",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onWebSearchToggle(false) }
                                )
                            }
                        }
                    }

                    if (isDrawMode) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🎨 Create image",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Exit Create Image Mode",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onTextChange(cleanPromptText()) }
                                )
                            }
                        }
                    }
                }

                if (isDrawMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val engines = listOf(
                            Triple("Flux Schnell", "flux/schnell", "Fal AI"),
                            Triple("Nano Banana", "nano-banana", "Fal AI"),
                            Triple("Nano Banana Pro", "nano-banana-pro", "Fal AI"),
                            Triple("Flux Realism", "flux-realism", "Fal AI"),
                            Triple("Imagen 4", "imagen4", "Fal AI"),
                            Triple("Stable SD3", "sd3", "Stability"),
                            Triple("Core", "core", "Stability"),
                            Triple("Ultra", "ultra", "Stability")
                        )
                        engines.forEach { (displayName, modelId, provider) ->
                            val isSelected = selectedImageProvider == provider && selectedImageModel == modelId
                            FilterChip(
                                selected = isSelected,
                                onClick = { onEngineSelected(provider, modelId) },
                                label = {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Main Prof Eddie floating squircle-capsule composer
        val pillColor = if (isDark) Color(0xFF212121) else MaterialTheme.colorScheme.surfaceVariant
        val profEddieAccent = Color(0xFF0F766E)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = pillColor,
            border = BorderStroke(
                width = 0.9.dp,
                color = when {
                    isWebSearchEnabled -> profEddieAccent.copy(alpha = 0.65f)
                    isDrawMode -> Color(0xFF4F46E5).copy(alpha = 0.55f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Compact left '+' attachment button (Camera, Gallery, Files)
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showAttachmentSheet = true }
                        .testTag("composer_plus_button"),
                    contentAlignment = Alignment.Center
                ) {
                    ChatGptPlusIcon(
                        tint = MaterialTheme.colorScheme.onSurface,
                        size = 18.dp
                    )
                }

                // Expanded center borderless text input without Material TextField horizontal bloat
                BasicTextField(
                    value = inputText,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    maxLines = 6,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = when {
                                        attachedImageBitmap != null -> "Ask about this image..."
                                        attachedFileName != null -> "Ask about this file..."
                                        isDrawMode -> "Describe an image..."
                                        isWebSearchEnabled -> "Search the web..."
                                        else -> "Ask Prof Eddie anything..."
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 13.5.sp,
                                        lineHeight = 19.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // Compact microphone dictation button
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(44.dp)
                        .clip(CircleShape)
                        .clickable { onMicClick() }
                        .testTag("composer_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    ChatGptMicIcon(
                        tint = MaterialTheme.colorScheme.onSurface,
                        size = 19.dp
                    )
                }

                // Far-right Voice Wave (Deep Teal Squircle) / Send Arrow Squircle
                val canSend = inputText.isNotBlank() || attachedImageBitmap != null || attachedFileName != null
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp, end = 2.dp)
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isLoading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                canSend -> MaterialTheme.colorScheme.primary
                                else -> profEddieAccent
                            }
                        )
                        .clickable(enabled = !isLoading) {
                            if (canSend) {
                                onSend()
                            } else {
                                onVoiceModeClick()
                            }
                        }
                        .testTag(if (canSend) "send_message_button" else "voice_mode_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else if (canSend) {
                        ChatGptUpArrowSendIcon(
                            tint = MaterialTheme.colorScheme.onPrimary,
                            size = 18.dp
                        )
                    } else {
                        ChatGptVoiceWaveIcon(
                            tint = Color.White,
                            size = 18.dp
                        )
                    }
                }
            }
        }
    }

    // '+' Attachment Bottom Sheet: Camera, Gallery & Files only
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { dismissAttachmentSheetAndRun() },
            sheetState = attachmentSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Add attachment",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AttachmentOptionTile(
                        label = "Camera",
                        accentColor = Color(0xFF0F766E),
                        icon = { tint ->
                            ChatGptCameraIcon(tint = tint, size = 24.dp)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("attach_camera_button"),
                        onClick = {
                            dismissAttachmentSheetAndRun(onCameraClick)
                        }
                    )

                    AttachmentOptionTile(
                        label = "Gallery",
                        accentColor = Color(0xFF4F46E5),
                        icon = { tint ->
                            ChatGptImageGalleryIcon(tint = tint, size = 24.dp)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("attach_gallery_button"),
                        onClick = {
                            dismissAttachmentSheetAndRun(onGalleryClick)
                        }
                    )

                    AttachmentOptionTile(
                        label = "Files",
                        accentColor = Color(0xFFD97706),
                        icon = { tint ->
                            ChatGptFileIcon(tint = tint, size = 24.dp)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("attach_files_button"),
                        onClick = {
                            dismissAttachmentSheetAndRun(onFilesClick)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun AttachmentOptionTile(
    label: String,
    accentColor: Color,
    icon: @Composable (Color) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = modifier.defaultMinSize(minHeight = 92.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                icon(accentColor)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
