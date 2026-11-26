package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatGptSidebar(
    uiState: ChatUiState,
    activeTab: String,
    historyPage: Int,
    onHistoryPageChange: (Int) -> Unit,
    onSelectTab: (String) -> Unit,
    onNewChat: () -> Unit,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenInstructions: () -> Unit,
    onOpenMemories: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenTopUp: () -> Unit,
    onCancelSubscription: () -> Unit,
    onOpenThemeDialog: () -> Unit,
    onOpenAgenticGuide: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAuth: () -> Unit,
    onSignOut: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var searchQuery by remember { mutableStateOf("") }
    var showMoreTools by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    val menuScope = rememberCoroutineScope()

    fun dismissAccountMenuThen(closeDrawer: Boolean = true, action: () -> Unit) {
        showAccountMenu = false
        menuScope.launch {
            delay(90)
            if (closeDrawer) {
                onCloseDrawer()
            }
            action()
        }
    }

    DisposableEffect(Unit) {
        onDispose { showAccountMenu = false }
    }

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(316.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerTonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 0. Compact Prof Eddie Brand Header Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F766E),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "PE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                    Text(
                        text = "Prof Eddie",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    onClick = {
                        onOpenTopUp()
                        onCloseDrawer()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.testTag("sidebar_token_badge")
                ) {
                    Text(
                        text = "🪙 ${uiState.getFormattedTokens()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFFD48200),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 1. Sticky Search + New Chat Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF262626) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ChatGptSearchIcon(
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 17.dp
                        )
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sidebar_search_input")
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Compose New Chat squircle button
                Surface(
                    onClick = {
                        onNewChat()
                        onCloseDrawer()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF262626) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("sidebar_new_chat_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ChatGptNewChatComposeIcon(
                            tint = MaterialTheme.colorScheme.onSurface,
                            size = 19.dp
                        )
                    }
                }
            }

            // 2. Scrollable Middle Section: Primary Spaces + More Tools + Paginated Chat History
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SidebarNavRow(
                    emoji = "✦",
                    title = "Prof Eddie",
                    selected = activeTab == "chat",
                    onClick = {
                        onSelectTab("chat")
                        onCloseDrawer()
                    }
                )

                SidebarNavRow(
                    emoji = "🎨",
                    title = "Image Studio",
                    selected = activeTab == "image_gen",
                    badgeText = if (!uiState.isSubscribed) "PRO" else null,
                    onClick = {
                        onSelectTab("image_gen")
                        onCloseDrawer()
                    }
                )

                SidebarNavRow(
                    emoji = "🎬",
                    title = "Video Studio",
                    selected = activeTab == "video_gen",
                    badgeText = if (!uiState.isSubscribed) "PRO" else null,
                    onClick = {
                        onSelectTab("video_gen")
                        onCloseDrawer()
                    }
                )

                SidebarNavRow(
                    emoji = "💻",
                    title = "Code Studio",
                    selected = activeTab == "ai_code",
                    onClick = {
                        onSelectTab("ai_code")
                        onCloseDrawer()
                    }
                )

                SidebarNavRow(
                    emoji = "🎵",
                    title = "Music Studio",
                    selected = activeTab == "music_gen",
                    onClick = {
                        onSelectTab("music_gen")
                        onCloseDrawer()
                    }
                )

                SidebarNavRow(
                    emoji = "🎙️",
                    title = "Voice Assistant",
                    selected = activeTab == "voice_ai",
                    onClick = {
                        onSelectTab("voice_ai")
                        onCloseDrawer()
                    }
                )

                // Expandable Explore GPTs / More AI Labs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showMoreTools = !showMoreTools }
                        .defaultMinSize(minHeight = 44.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("⊞", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Explore AI Labs",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (showMoreTools) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Explore AI Labs",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = showMoreTools) {
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SidebarNavRow("📸", "Vision Analyzer", activeTab == "ai_vision") {
                            onSelectTab("ai_vision"); onCloseDrawer()
                        }
                        SidebarNavRow("📄", "Document & PDF Summary", activeTab == "doc_intelligence") {
                            onSelectTab("doc_intelligence"); onCloseDrawer()
                        }
                        SidebarNavRow("📐", "Math & Science Solver", activeTab == "math_tutor") {
                            onSelectTab("math_tutor"); onCloseDrawer()
                        }
                        SidebarNavRow("🌐", "Deep Web Researcher", activeTab == "web_researcher") {
                            onSelectTab("web_researcher"); onCloseDrawer()
                        }
                        SidebarNavRow("✍️", "Copywriter & Text Gen", activeTab == "ai_writer") {
                            onSelectTab("ai_writer"); onCloseDrawer()
                        }
                        SidebarNavRow("📢", "Social Studio", activeTab == "social_gen") {
                            onSelectTab("social_gen"); onCloseDrawer()
                        }
                        SidebarNavRow("🪙", "Token Price History", activeTab == "token_history") {
                            onSelectTab("token_history"); onCloseDrawer()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Chat History Section with 10-item Pagination
                val filteredSessions = remember(uiState.sessions, searchQuery) {
                    if (searchQuery.isBlank()) uiState.sessions
                    else uiState.sessions.filter {
                        it.title.contains(searchQuery.trim(), ignoreCase = true)
                    }
                }

                val historyPageSize = 10
                val totalHistoryCount = filteredSessions.size
                val totalHistoryPages = ((totalHistoryCount + historyPageSize - 1) / historyPageSize).coerceAtLeast(1)
                val currentHistoryPage = historyPage.coerceIn(0, totalHistoryPages - 1)

                LaunchedEffect(totalHistoryPages) {
                    if (historyPage >= totalHistoryPages) {
                        onHistoryPageChange((totalHistoryPages - 1).coerceAtLeast(0))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "Chats" else "Search Results",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (totalHistoryCount > 0) {
                        val startIdx = currentHistoryPage * historyPageSize + 1
                        val endIdx = ((currentHistoryPage + 1) * historyPageSize).coerceAtMost(totalHistoryCount)
                        Text(
                            text = "$startIdx–$endIdx / $totalHistoryCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                if (filteredSessions.isEmpty()) {
                    Text(
                        text = if (searchQuery.isBlank()) "No conversations yet" else "No matching chats",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                } else {
                    val startIndex = currentHistoryPage * historyPageSize
                    val endIndex = (startIndex + historyPageSize).coerceAtMost(totalHistoryCount)
                    val pagedSessions = filteredSessions.subList(startIndex, endIndex)

                    pagedSessions.forEach { session ->
                        val isSelected = session.id == uiState.currentSessionId && activeTab == "chat"
                        Surface(
                            onClick = {
                                onSelectTab("chat")
                                onSelectSession(session.id)
                                onCloseDrawer()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_session_item_${session.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 42.dp)
                                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onDeleteSession(session.id) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("delete_session_${session.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete conversation",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Pagination controls (10 items per page)
                    if (totalHistoryPages > 1) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (currentHistoryPage > 0) {
                                            onHistoryPageChange(currentHistoryPage - 1)
                                        }
                                    },
                                    enabled = currentHistoryPage > 0,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("history_prev_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Halaman Sebelumnya",
                                        tint = if (currentHistoryPage > 0) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Hal ${currentHistoryPage + 1} / $totalHistoryPages",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "10 item / halaman",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (currentHistoryPage < totalHistoryPages - 1) {
                                            onHistoryPageChange(currentHistoryPage + 1)
                                        }
                                    },
                                    enabled = currentHistoryPage < totalHistoryPages - 1,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("history_next_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Halaman Berikutnya",
                                        tint = if (currentHistoryPage < totalHistoryPages - 1) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // 4. Sticky Bottom Utility & Account Footer (ChatGPT style)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

            // Quick utility pills row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickDrawerChip("🪙", "Top Up") {
                    onOpenTopUp(); onCloseDrawer()
                }
                QuickDrawerChip("🎨", "Skin") {
                    onOpenThemeDialog(); onCloseDrawer()
                }
                QuickDrawerChip("🧠", "Memory") {
                    onOpenMemories(); onCloseDrawer()
                }
                QuickDrawerChip("❤️", "Saved") {
                    onOpenBookmarks(); onCloseDrawer()
                }
            }

            // Profile & Account menu bar
            val user = uiState.currentUserProfile
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
            ) {
                Surface(
                    onClick = {
                        if (user == null) {
                            onOpenAuth()
                            onCloseDrawer()
                        } else {
                            showAccountMenu = true
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user != null) {
                                Text(
                                    text = user.username.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Account",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = user?.username ?: "Sign In / Sign Up",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    color = if (uiState.isSubscribed) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (uiState.isSubscribed) "PRO" else "FREE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (uiState.isSubscribed) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = user?.email ?: "Tap to sign in • 🪙 ${uiState.getFormattedConsumedTokens()} Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { showAccountMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("drawer_more_options_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = showAccountMenu,
                    onDismissRequest = { showAccountMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("🤖 Custom Instructions") },
                        onClick = {
                            dismissAccountMenuThen { onOpenInstructions() }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🪙 Top Up Token") },
                        onClick = {
                            dismissAccountMenuThen { onOpenTopUp() }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (uiState.isSubscribed) "👑 Manage Premium" else "👑 Subscribe Premium") },
                        onClick = {
                            dismissAccountMenuThen { onOpenSubscription() }
                        }
                    )
                    if (uiState.isSubscribed) {
                        DropdownMenuItem(
                            text = { Text("❌ Cancel Subscription", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                dismissAccountMenuThen { onCancelSubscription() }
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("⚡ Agentic Guide") },
                        onClick = {
                            dismissAccountMenuThen { onOpenAgenticGuide() }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("ℹ️ Help & Info") },
                        onClick = {
                            dismissAccountMenuThen { onOpenHelp() }
                        }
                    )
                    if (user != null) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            onClick = {
                                dismissAccountMenuThen { onSignOut() }
                            }
                        )
                    } else {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Sign In / Sign Up",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            },
                            onClick = {
                                dismissAccountMenuThen { onOpenAuth() }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarNavRow(
    emoji: String,
    title: String,
    selected: Boolean,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (selected) Color(0xFF0F766E) else Color.Transparent
                    )
            )
            Text(
                text = emoji,
                style = TextStyle(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFB100).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFFFFB100).copy(alpha = 0.6f))
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFD48200),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.QuickDrawerChip(
    emoji: String,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.weight(1f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = emoji, fontSize = 13.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
