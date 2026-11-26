package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatGptEmptyState(
    showAgenticTooltip: Boolean,
    onDismissTooltip: () -> Unit,
    onCreateImageClick: () -> Unit,
    onWriteOrEditClick: () -> Unit,
    onCodeAndSolveClick: () -> Unit,
    onSelectPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Completely clean, open center canvas with zero text
        Spacer(modifier = Modifier.weight(1f))

        // Optional compact Agentic Tips card when toggled
        AnimatedVisibility(
            visible = showAgenticTooltip,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Agentic Quick Actions",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = onDismissTooltip,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Tips",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { onSelectPreset("Please write an email to the manager explaining my team updates") },
                            label = { Text("📧 Draft Email", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        SuggestionChip(
                            onClick = { onSelectPreset("Send a whatsapp message with birthday greetings to grandpa") },
                            label = { Text("💬 WhatsApp", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Distinct soft-bordered quick starter pills above the composer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StarterPillCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_create_image"),
                    onClick = onCreateImageClick,
                    icon = {
                        ChatGptImageGalleryIcon(
                            tint = Color(0xFF0F766E),
                            size = 18.dp
                        )
                    },
                    label = "Create image"
                )

                StarterPillCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_write_edit"),
                    onClick = onWriteOrEditClick,
                    icon = {
                        ChatGptPencilEditIcon(
                            tint = Color(0xFF4F46E5),
                            size = 18.dp
                        )
                    },
                    label = "Write or edit"
                )
            }

            StarterPillCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_action_code_solve"),
                onClick = onCodeAndSolveClick,
                icon = {
                    Text(
                        text = "</>",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    )
                },
                label = "Debug code, analyze data, or solve a problem"
            )
        }
    }
}

@Composable
private fun StarterPillCard(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = modifier.defaultMinSize(minHeight = 48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
