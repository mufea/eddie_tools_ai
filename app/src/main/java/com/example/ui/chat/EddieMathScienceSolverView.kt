package com.example.ui.chat

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EddieMathScienceSolverView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onImageSelected: (Uri?, Bitmap?) -> Unit,
    onSolveProblem: (String, String) -> Unit,
    onAskTutorQuestion: (String) -> Unit,
    onReset: () -> Unit,
    onStepSelected: (Int) -> Unit,
    onLevelSelected: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onShareText: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isKeyboardVisible = WindowInsets.isImeVisible

    var manualInputProblem by remember { mutableStateOf("") }
    var customQuestionInput by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Math & Calculus") }

    val subjects = listOf(
        "Math & Calculus" to "📐",
        "Physics & Mechanics" to "⚛️",
        "Chemistry & Formulas" to "🧪",
        "Algebra & Equations" to "📊",
        "Geometry & Trigonometry" to "📏"
    )

    val studentLevels = listOf(
        "Elementary / Beginner",
        "Junior High School",
        "High School (SMA/K)",
        "University / Undergraduate",
        "Expert / Olympiad"
    )

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                }
                onImageSelected(uri, bitmap)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            onImageSelected(null, bitmap)
        }
    }

    // Study challenge presets for immediate test & learning
    val challengePresets = listOf(
        Triple("Calculus Derivative", "Find the derivative of f(x) = x · sin(x²)", "Math & Calculus"),
        Triple("Physics Projectile", "A projectile is launched at 30° with v0 = 20 m/s. Calculate max height and flight time (g = 9.8 m/s²).", "Physics & Mechanics"),
        Triple("Chemistry Equilibrium", "Calculate the pH of 0.15 M acetic acid solution (CH₃COOH) with Ka = 1.8 × 10⁻⁵.", "Chemistry & Formulas"),
        Triple("Quadratic Roots", "Solve for x: 3x² - 11x + 6 = 0 using factoring and quadratic formula.", "Algebra & Equations")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📐", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI Math & Science Solver",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "SOCRATIC",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Step-by-step problem solver & interactive Socratic tutor for Math, Physics, and Chemistry.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Subject Selector Chips
            Text(
                text = "1. Choose Academic Subject",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(subjects) { (subj, emoji) ->
                    val isSelected = selectedSubject == subj
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSubject = subj },
                        label = { Text("$emoji $subj", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Student Level Selector
            Text(
                text = "2. Explanation Depth / Student Level",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                studentLevels.forEach { level ->
                    val isSelected = uiState.mathStudentLevel == level
                    Surface(
                        modifier = Modifier
                            .clickable { onLevelSelected(level) }
                            .clip(RoundedCornerShape(8.dp)),
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = level,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Photo Input & Problem Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "3. Capture Photo or Type Homework Problem",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Image preview if selected
                    if (uiState.mathSelectedBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = uiState.mathSelectedBitmap.asImageBitmap(),
                                contentDescription = "Captured homework photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            IconButton(
                                onClick = { onImageSelected(null, null) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Photo", tint = Color.White)
                            }
                        }
                    }

                    // Action buttons for Photo Capture & Gallery
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { cameraLauncher.launch() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📷", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Photo", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🖼️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Image", fontSize = 12.sp)
                        }
                    }

                    // Manual Text Field
                    OutlinedTextField(
                        value = manualInputProblem,
                        onValueChange = { manualInputProblem = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Or enter question here, e.g.: \"Find the integral of 2x / (x^2 + 1) dx\" or \"A 5kg block accelerates...\"",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        maxLines = 5,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Challenge Presets
                    Text(
                        text = "Quick Presets / Practice Problems:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(challengePresets) { (title, problem, subj) ->
                            SuggestionChip(
                                onClick = {
                                    manualInputProblem = problem
                                    selectedSubject = subj
                                },
                                label = { Text(title, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Solve Button
                    Button(
                        onClick = {
                            val promptToSend = if (manualInputProblem.isNotBlank()) {
                                "[$selectedSubject] $manualInputProblem"
                            } else {
                                "[$selectedSubject] Deconstruct the problem in the photo into Socratic steps."
                            }
                            onSolveProblem(promptToSend, "")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !uiState.mathIsLoading && (uiState.mathSelectedBitmap != null || manualInputProblem.isNotBlank())
                    ) {
                        if (uiState.mathIsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Deconstructing Problem...")
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SOLVE STEP-BY-STEP", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Error notice
            if (uiState.mathError.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.mathError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Results: Socratic Step by Step Guide
            if (uiState.mathSteps.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "💡 Step-by-Step Socratic Solution",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${uiState.mathSteps.size} steps total • Step ${uiState.mathCurrentStepIndex + 1} of ${uiState.mathSteps.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {
                            val fullSolution = buildString {
                                appendLine("Problem: ${uiState.mathProblemOverview}")
                                appendLine()
                                uiState.mathSteps.forEach { step ->
                                    appendLine("Step ${step.stepIndex}: ${step.stepTitle}")
                                    appendLine("Formula: ${step.mathematicalExpression}")
                                    appendLine("Explanation: ${step.intuitiveExplanation}")
                                    appendLine("Concept: ${step.conceptKey}")
                                    appendLine()
                                }
                            }
                            onCopyText(fullSolution)
                        }) {
                            Text("📋", fontSize = 14.sp)
                        }

                        IconButton(onClick = {
                            val fullSolution = buildString {
                                appendLine("📐 AI Math & Science Solver Solution")
                                appendLine("Problem: ${uiState.mathProblemOverview}")
                                appendLine()
                                uiState.mathSteps.forEach { step ->
                                    appendLine("Step ${step.stepIndex}: ${step.stepTitle}")
                                    appendLine("Formula: ${step.mathematicalExpression}")
                                    appendLine("Explanation: ${step.intuitiveExplanation}")
                                    appendLine()
                                }
                            }
                            onShareText(fullSolution)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        }

                        IconButton(onClick = onReset) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Overview card
                if (uiState.mathProblemOverview.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Problem Overview",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.mathProblemOverview,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Step Navigation Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.mathSteps) { step ->
                        val isCurrent = step.stepIndex - 1 == uiState.mathCurrentStepIndex
                        Surface(
                            modifier = Modifier
                                .clickable { onStepSelected(step.stepIndex - 1) }
                                .clip(RoundedCornerShape(8.dp)),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Step ${step.stepIndex}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Active Step Detail Card
                val activeStep = uiState.mathSteps.getOrNull(uiState.mathCurrentStepIndex)
                if (activeStep != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "STEP ${activeStep.stepIndex} OF ${uiState.mathSteps.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                if (activeStep.conceptKey.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = activeStep.conceptKey,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = activeStep.stepTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Mathematical Expression / Formula box
                            if (activeStep.mathematicalExpression.isNotBlank()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    SelectionContainer {
                                        Text(
                                            text = activeStep.mathematicalExpression,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            color = Color(0xFF80D8FF),
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }

                            // Intuitive explanation
                            Text(
                                text = activeStep.intuitiveExplanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )

                            // Step Nav buttons: Previous and Next
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onStepSelected(uiState.mathCurrentStepIndex - 1) },
                                    enabled = uiState.mathCurrentStepIndex > 0,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Previous Step", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { onStepSelected(uiState.mathCurrentStepIndex + 1) },
                                    enabled = uiState.mathCurrentStepIndex < uiState.mathSteps.size - 1,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Next Step", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Interactive Socratic Question Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🧑‍🏫", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Ask Socratic Tutor About This Step",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Don't understand why this formula was used? Ask for intuition!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Quick Prompt Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SuggestionChip(
                                    onClick = { onAskTutorQuestion("Why did we apply ${activeStep.conceptKey} here?") },
                                    label = { Text("Why this rule?", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = { onAskTutorQuestion("Can you explain this with a real-life analogy?") },
                                    label = { Text("Analogy please", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = { onAskTutorQuestion("What if we used another method instead?") },
                                    label = { Text("Alternative way?", fontSize = 11.sp) }
                                )
                            }

                            // Custom question input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customQuestionInput,
                                    onValueChange = { customQuestionInput = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Ask a question about step ${activeStep.stepIndex}...", fontSize = 12.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        if (customQuestionInput.isNotBlank()) {
                                            onAskTutorQuestion(customQuestionInput)
                                            customQuestionInput = ""
                                        }
                                    },
                                    enabled = !uiState.mathExplanationIsLoading && customQuestionInput.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (uiState.mathExplanationIsLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    } else {
                                        Text("Ask", fontSize = 12.sp)
                                    }
                                }
                            }

                            // Tutor Explanation Display
                            if (uiState.mathExplanationIsLoading) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tutor is thinking...", style = MaterialTheme.typography.bodySmall)
                                }
                            } else if (uiState.mathConceptExplanation.isNotBlank()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "🎓 Tutor's Intuition:",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        SelectionContainer {
                                            Text(
                                                text = uiState.mathConceptExplanation,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 18.sp
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
