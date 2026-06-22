package com.example.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.InputStream

// Presets data definitions
data class MathPreset(
    val title: String,
    val type: String,
    val text: String,
    val jsonString: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocraticMathTutorView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onImageSelected: (Uri?, Bitmap?) -> Unit,
    onSolveClick: (String, String) -> Unit,
    onAskTutorClick: (String) -> Unit,
    onStepChange: (Int) -> Unit,
    onResetClick: () -> Unit,
    onLevelChange: (String) -> Unit
) {
    val context = LocalContext.current
    var inputQuery by remember { mutableStateOf("") }
    var activePresetTitle by remember { mutableStateOf("") }
    var activeBranch by remember { mutableStateOf("Math") } // "Math", "Physics", "Chemistry"

    // Preset options
    val mathPresets = remember {
        listOf(
            MathPreset(
                title = "Derivative: x * sin(x²)",
                type = "Calculus",
                text = "y = x * sin(x^2)",
                jsonString = """
                {
                  "problemOverview": "Find the derivative of y = x * sin(x^2) with respect to x",
                  "steps": [
                    {
                      "stepIndex": 1,
                      "stepTitle": "Identify product form and prepare Product Rule",
                      "mathematicalExpression": "d/dx [ x * sin(x^2) ] = x * d/dx[sin(x^2)] + sin(x^2) * d/dx[x]",
                      "intuitiveExplanation": "We notice this is a product of two distinct mathematical families: the simple x and the composite sin(x²). To find its derivative, we keep the first function still while multiplying by the rate of change of the second, plus the second function still while differentiating the first.",
                      "conceptKey": "Product Rule"
                    },
                    {
                      "stepIndex": 2,
                      "stepTitle": "Differentiate sin(x^2) using Chain Rule",
                      "mathematicalExpression": "d/dx[sin(x^2)] = cos(x^2) * 2x",
                      "intuitiveExplanation": "This term has an outer function (sine) wrapped around an inner function (x²). Socratic teaching tells us to differentiate the outer part first (sine rate of change is cosine of the exact same inner part), then multiply by the derivative of the inside variables (derivative of x² is 2x).",
                      "conceptKey": "Chain Rule"
                    },
                    {
                      "stepIndex": 3,
                      "stepTitle": "Differentiate the simple variable x",
                      "mathematicalExpression": "d/dx[x] = 1",
                      "intuitiveExplanation": "Differentiating simple x with respect to itself returns just 1. We obtain this straight from the standard power rule, representing a clean, perfectly constant rate of change.",
                      "conceptKey": "Power Rule"
                    },
                    {
                      "stepIndex": 4,
                      "stepTitle": "Combine calculated terms and simplify variables",
                      "mathematicalExpression": "dy/dx = 2x^2 * cos(x^2) + sin(x^2)",
                      "intuitiveExplanation": "Now we replace our calculated pieces back into our original Product Rule setup: x * (cos(x²) * 2x) + sin(x²) * 1. Multiplying both free x variables together simplifies beautifully into 2x², leaving a wonderful final solution!",
                      "conceptKey": "Algebraic Simplification"
                    }
                  ]
                }
                """.trimIndent()
            ),
            MathPreset(
                title = "Factor: 3x² - 14x + 8 = 0",
                type = "Algebra",
                text = "3x^2 - 14x + 8 = 0",
                jsonString = """
                {
                  "problemOverview": "Solve the quadratic equation: 3x^2 - 14x + 8 = 0",
                  "steps": [
                    {
                      "stepIndex": 1,
                      "stepTitle": "Deconstruct the middle term for factoring",
                      "mathematicalExpression": "3x^2 - 12x - 2x + 8 = 0",
                      "intuitiveExplanation": "We need two numbers that multiply to give (a * c) = 3 * 8 = 24, and add up to the middle coefficient (b) = -14. These friendly math numbers are -12 and -2! So, let's break down -14x to become -12x - 2x.",
                      "conceptKey": "AC Method Factoring"
                    },
                    {
                      "stepIndex": 2,
                      "stepTitle": "Group variables and extract binomial factors",
                      "mathematicalExpression": "3x(x - 4) - 2(x - 4) = 0",
                      "intuitiveExplanation": "Let's group the first two terms: 3x² - 12x becomes 3x(x - 4). Group the last two terms: -2x + 8 becomes -2(x - 4). We see (x - 4) is perfectly shared by both groups. It is mathematical synergy!",
                      "conceptKey": "Factor by Grouping"
                    },
                    {
                      "stepIndex": 3,
                      "stepTitle": "Reconstruct the product of binomial expressions",
                      "mathematicalExpression": "(3x - 2)(x - 4) = 0",
                      "intuitiveExplanation": "Because both factored terms share (x - 4), we factor it out to construct a neat multiplication of two binomial coordinates: (3x - 2) times (x - 4) is equal to zero.",
                      "conceptKey": "Distributive Property"
                    },
                    {
                      "stepIndex": 4,
                      "stepTitle": "Solve for roots individually using the Zero Product principal",
                      "mathematicalExpression": "x = 2/3  or  x = 4",
                      "intuitiveExplanation": "If the product of two numbers forms zero, then at least one must be zero. Setting 3x - 2 = 0 yields x = 2/3. Setting x - 4 = 0 gives us x = 4. We successfully found both roots!",
                      "conceptKey": "Zero Product Property"
                    }
                  ]
                }
                """.trimIndent()
            ),
            MathPreset(
                title = "Integral: ∫ x * e^x dx",
                type = "Calculus",
                text = "integral of x * e^x dx",
                jsonString = """
                {
                  "problemOverview": "Evaluate the indefinite integral: ∫ x * e^x dx",
                  "steps": [
                    {
                      "stepIndex": 1,
                      "stepTitle": "Assign variables for Integration by Parts",
                      "mathematicalExpression": "Let u = x  => du = dx  and  dv = e^x dx => v = e^x",
                      "intuitiveExplanation": "We have two distinct mathematical parts multiplied together! Using Integration by Parts, we designate 'u' to be the term that simplifies when we differentiate (which is x, returning du = dx), and 'dv' as the part we integrate (e^x, returning v = e^x).",
                      "conceptKey": "LIATE rule selection"
                    },
                    {
                      "stepIndex": 2,
                      "stepTitle": "Apply the standard Integration by Parts formula",
                      "mathematicalExpression": "∫ u dv = u * v - ∫ v du  =>  x * e^x - ∫ e^x dx",
                      "intuitiveExplanation": "Using our formula: u times v minus the integral of v with respect to u. Substituting our defined terms transforms the complex integration into a much simpler integral: x * e^x minus the basic integral of e^x.",
                      "conceptKey": "Integration by Parts Formula"
                    },
                    {
                      "stepIndex": 3,
                      "stepTitle": "Compute final integral and add constant",
                      "mathematicalExpression": "x * e^x - e^x + C",
                      "intuitiveExplanation": "The integral of e^x with respect to x is just e^x! Finally, we add our faithful constant of integration, + C, which represents the infinite family of curves. We've solved the indefinite integral together!",
                      "conceptKey": "Constant of Integration"
                    }
                  ]
                }
                """.trimIndent()
            )
        )
    }

    val physicsPresets = remember {
        listOf(
            MathPreset(
                title = "Newton's Second Law: F = m * a",
                type = "Physics",
                text = "A 5kg block is pushed with a net force of 20N. Find its acceleration.",
                jsonString = """
                {
                  "problemOverview": "Calculate acceleration when a 5kg block is pushed with a net force of 20N using Newton's Second Law (F = m * a).",
                  "steps": [
                    {
                      "stepIndex": 1,
                      "stepTitle": "State the governing physical law and variables",
                      "mathematicalExpression": "F = m * a   (where F = 20 N, m = 5 kg)",
                      "intuitiveExplanation": "Newton's second law tells us that force causes mass to accelerate! We are given a net physical force of 20 Newtons acting on a mass of 5 kilograms. Let's arrange these known components to solve for the target acceleration.",
                      "conceptKey": "Newton's Second Law"
                    },
                    {
                      "stepIndex": 2,
                      "stepTitle": "Rearrange the equation to isolate acceleration (a)",
                      "mathematicalExpression": "a = F / m",
                      "intuitiveExplanation": "To find acceleration 'a', we divide both sides of the equation by mass 'm'. This shows that acceleration is directly proportional to force, but inversely proportional to mass. In other words, heavier things are harder to speed up!",
                      "conceptKey": "Algebraic Isolation"
                    },
                    {
                      "stepIndex": 3,
                      "stepTitle": "Substitute the values and compute units",
                      "mathematicalExpression": "a = 20 N / 5 kg = 4 m/s²",
                      "intuitiveExplanation": "Dividing the 20 Newtons of force by our 5 kilograms of mass yields exactly 4. Because 1 Newton is equivalent to 1 kg·m/s², dividing by kilograms leaves us with unit of m/s² (meters per second squared). You've successfully found the acceleration!",
                      "conceptKey": "Dimensional Analysis & Calculation"
                    }
                  ]
                }
                """.trimIndent()
            ),
            MathPreset(
                title = "Potential Energy: PE = m * g * h",
                type = "Physics",
                text = "Find the gravitational potential energy of a 2kg object resting 10 meters high on a shelf.",
                jsonString = """
                {
                  "problemOverview": "Determine the gravitational potential energy (PE) stored in a 2kg object resting on a shelf 10 meters above the ground.",
                  "steps": [
                    {
                      "stepIndex": 1,
                      "stepTitle": "Define the Gravitational Potential Energy formula",
                      "mathematicalExpression": "PE = m * g * h",
                      "intuitiveExplanation": "Potential energy is stored energy! For gravity, it is the energy an object has due to its height. It is calculated by multiplying the mass (m), the acceleration of gravity (g), and the height (h) above our reference ground.",
                      "conceptKey": "Gravitational PE Formula"
                    },
                    {
                      "stepIndex": 2,
                      "stepTitle": "Identify and establish the known parameters",
                      "mathematicalExpression": "m = 2 kg,  g = 9.8 m/s²,  h = 10 m",
                      "intuitiveExplanation": "We know the object's mass is 2 kg, and its height on the shelf is 10 meters. The gravitation acceleration 'g' on Earth's surface is a constant approximately equal to 9.8 meters per second squared. Let's utilize these values together!",
                      "conceptKey": "Physical Constants Assignment"
                    },
                    {
                      "stepIndex": 3,
                      "stepTitle": "Perform the multiplication and calculate Joules",
                      "mathematicalExpression": "PE = 2 * 9.8 * 10 = 196 J",
                      "intuitiveExplanation": "Multiplying our mass (2 kg) by gravity (9.8 m/s²) gives 19.6 Newtons of weight. Then, multiplying by the height (10 meters) gives us exactly 196. The unit of physical energy is the Joule (J). Excellent work!",
                      "conceptKey": "SI Energy Unit Calculation"
                    }
                  ]
                }
                """.trimIndent()
            )
        )
    }

    val chemistryPresets = remember {
        listOf(
            MathPreset(
                title = "Ideal Gas Law: P * V = n * R * T",
                type = "Chemistry",
                text = "Calculate pressure of 2 moles of gas in a 10L container at 300K.",
                jsonString = """
                {
                  "problemOverview": "Find the pressure (P) of 2 moles of an ideal gas confined in a 10 Litre container at a temperature of 300 Kelvin.",
                  "steps": [
                    {
                      "stepIndex": 1,
                      "stepTitle": "State the Ideal Gas equation and identify variables",
                      "mathematicalExpression": "P * V = n * R * T   (where V = 10 L, n = 2 mol, T = 300 K)",
                      "intuitiveExplanation": "The Ideal Gas Law relates the state variables of a gas! We have the Volume (V = 10 L), amount in moles (n = 2), and temperature in Kelvin (T = 300 K). Let's solve for pressure 'P'!",
                      "conceptKey": "Ideal Gas Relation"
                    },
                    {
                      "stepIndex": 2,
                      "stepTitle": "Select the correct Gas Constant (R)",
                      "mathematicalExpression": "R = 0.0821 L·atm/(mol·K)",
                      "intuitiveExplanation": "To get our pressure in standard atmospheres (atm), we must choose the constant R with corresponding units. Here, R is approximately 0.0821. This number constant balances all the molecular proportions correctly!",
                      "conceptKey": "Universal Gas Constant selection"
                    },
                    {
                      "stepIndex": 3,
                      "stepTitle": "Isolate Pressure (P) and substitute",
                      "mathematicalExpression": "P = (n * R * T) / V",
                      "intuitiveExplanation": "We divide both sides of the relation by Volume 'V' to isolate Pressure 'P'. So, the pressure equals the molecular energetic terms divided by the space they have to bounce around.",
                      "conceptKey": "Algebraic Rearrangement"
                    },
                    {
                      "stepIndex": 4,
                      "stepTitle": "Solve the algebraic calculation for Pressure",
                      "mathematicalExpression": "P = (2 * 0.0821 * 300) / 10 = 4.926 atm",
                      "intuitiveExplanation": "Multiplying (2 * 0.0821 * 300) equals 49.26. Dividing by our volume of 10 Liters yields 4.926 atmospheres. The gas exerts a solid, friendly pressure on the classroom glass!",
                      "conceptKey": "Equational Synthesis"
                    }
                  ]
                }
                """.trimIndent()
            ),
            MathPreset(
                title = "pH of Acid: pH = -log[H+]",
                type = "Chemistry",
                text = "Determine the pH of a solution with a hydrogen ion concentration [H+] of 1.0 x 10^-3 M.",
                jsonString = """
                {
                  "problemOverview": "Calculate the pH of a solution with a given hydrogen ion concentration [H+] of 1.0 x 10^-3 Molars.",
                  "steps": [
                    {
                      "stepIndex": 1,
                      "stepTitle": "Identify the pH definition formula",
                      "mathematicalExpression": "pH = -log₁₀[H⁺]",
                      "intuitiveExplanation": "The pH scale measures how acidic or basic a water solution is on a logarithmic scale! It is defined as the negative base-10 logarithm of the hydrogen ion concentration.",
                      "conceptKey": "Logarithmic pH Definition"
                    },
                    {
                      "stepIndex": 2,
                      "stepTitle": "Substitute the concentration value into the formula",
                      "mathematicalExpression": "pH = -log₁₀(1.0 * 10⁻³)",
                      "intuitiveExplanation": "Subbing in our known hydrogen concentration of 10⁻³ Molars. The negative log looks at the exponent of 10 to see its magnitude order of intensity.",
                      "conceptKey": "Variable Substitution"
                    },
                    {
                      "stepIndex": 3,
                      "stepTitle": "Apply negative logarithmic properties to find the pH",
                      "mathematicalExpression": "pH = -(-3) = 3",
                      "intuitiveExplanation": "The base-10 log of 10⁻³ is exactly -3. Multiplying by the negative sign outside the log gives us +3. A pH of 3 is perfectly acidic—like juice or vinegar!",
                      "conceptKey": "Logarithmic Simplification"
                    }
                  ]
                }
                """.trimIndent()
            )
        )
    }

    // Launchers for image selection
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val stream: InputStream? = context.contentResolver.openInputStream(uri)
                val decoded = BitmapFactory.decodeStream(stream)
                if (decoded != null) {
                    val downscaled = resizeMathBitmap(decoded, 1080)
                    onImageSelected(uri, downscaled)
                    activePresetTitle = ""
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val downscaled = resizeMathBitmap(bitmap, 1080)
            onImageSelected(null, downscaled)
            activePresetTitle = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            )
            .padding(paddingValues)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            // Header panel with cozy Socratic style banner
            item {
                EddieHeaderCard()
            }

            // Learning level picker / Student status selector
            item {
                StudentLevelCard(
                    selectedLevel = uiState.mathStudentLevel,
                    onLevelChange = onLevelChange
                )
            }

            // Input / Load section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Step 1: Choose/Upload Problem or Select Branch",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        // Branch Selector Row
                        Text(
                            text = "Select Branch Subject:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        ) {
                            listOf("Math", "Physics", "Chemistry").forEach { branch ->
                                val isSelected = activeBranch == branch
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        activeBranch = branch
                                        activePresetTitle = ""
                                        inputQuery = ""
                                    },
                                    label = { Text(branch, fontWeight = FontWeight.Bold) },
                                    leadingIcon = {
                                        val icon = when (branch) {
                                            "Math" -> "📐"
                                            "Physics" -> "⚛️"
                                            else -> "🧪"
                                        }
                                        Text(icon)
                                    }
                                )
                            }
                        }

                        // Visual display of preset challenges
                        Text(
                            text = "Tap a study preset from $activeBranch to try immediately:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )

                        val currentPresets = when (activeBranch) {
                            "Math" -> mathPresets
                            "Physics" -> physicsPresets
                            else -> chemistryPresets
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentPresets.forEach { preset ->
                                val isSelected = activePresetTitle == preset.title
                                InputChip(
                                    selected = isSelected,
                                    onClick = {
                                        activePresetTitle = preset.title
                                        onImageSelected(null, null) // Clear image input when choosing preset
                                        inputQuery = preset.text
                                        onSolveClick(preset.text, preset.jsonString)
                                    },
                                    label = {
                                        Text(preset.title)
                                    },
                                    leadingIcon = {
                                        val icon = when (activeBranch) {
                                            "Math" -> "📐"
                                            "Physics" -> "⚛️"
                                            else -> "🧪"
                                        }
                                        Text(icon, fontSize = 12.sp)
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f)
                        )

                        // Image capture/selected preview row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).testTag("select_photo_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Photo", fontSize = 13.sp)
                            }

                            Button(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier.weight(1f).testTag("camera_capture_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("📸", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera", fontSize = 13.sp)
                            }
                        }

                        // Selected bitmap preview block
                        uiState.mathSelectedBitmap?.let { bitmap ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Uploaded math problem",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )

                                IconButton(
                                    onClick = { onImageSelected(null, null) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Custom equation text input
                        OutlinedTextField(
                            value = inputQuery,
                            onValueChange = {
                                inputQuery = it
                                activePresetTitle = ""
                            },
                            placeholder = { Text("Or model equations manually: e.g. integral ax² + bx + c") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_math_input"),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (inputQuery.isNotBlank() || uiState.mathSelectedBitmap != null) {
                                    IconButton(
                                        onClick = {
                                            activePresetTitle = ""
                                            onSolveClick(inputQuery, "")
                                        },
                                        modifier = Modifier.testTag("solve_math_action")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Submit equational bounds",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }

            // Socratic Tutoring Status & Error messaging
            if (uiState.mathError.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("⚠️", fontSize = 20.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.mathError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Processing indicator loop
            if (uiState.mathIsLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Eddie is deconstructing your problem into Socratic components...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Active Math Solving Board
            if (uiState.mathSteps.isNotEmpty() && !uiState.mathIsLoading) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title overview of the parsed problem
                        Text(
                            text = "Active Tutoring Board",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Problem chalkboard display
                        MathChalkboardCard(overviewText = uiState.mathProblemOverview)

                        // Reveal steps sequentially!
                        // "Walk me through the first step first" means we only display steps up to mathCurrentStepIndex
                        val visibleSteps = uiState.mathSteps.filter { it.stepIndex <= (uiState.mathCurrentStepIndex + 1) }

                        visibleSteps.forEach { step ->
                            val isActiveStep = step.stepIndex == (uiState.mathCurrentStepIndex + 1)
                            MathStepRevealCard(
                                step = step,
                                isActive = isActiveStep,
                                studentLevel = uiState.mathStudentLevel,
                                explanation = uiState.mathConceptExplanation,
                                explanationLoading = uiState.mathExplanationIsLoading,
                                onAskClick = { onAskTutorClick("") },
                                onCustomAskClick = { q -> onAskTutorClick(q) },
                                history = uiState.mathTutorHistory
                            )
                        }

                        // Bottom navigation step progression controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Step ${uiState.mathCurrentStepIndex + 1} of ${uiState.mathSteps.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (uiState.mathCurrentStepIndex > 0) {
                                    OutlinedButton(
                                        onClick = { onStepChange(uiState.mathCurrentStepIndex - 1) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Previous", fontSize = 13.sp)
                                    }
                                }

                                if (uiState.mathCurrentStepIndex < uiState.mathSteps.size - 1) {
                                    Button(
                                        onClick = { onStepChange(uiState.mathCurrentStepIndex + 1) },
                                        modifier = Modifier.testTag("next_step_action"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Next Step", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                } else {
                                    // Complete badge decoration
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🎉 Complete!", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }
                            }
                        }

                        // Reset button
                        OutlinedButton(
                            onClick = onResetClick,
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Clear study session", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Board to Study Another Problem", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EddieHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎓", fontSize = 22.sp)
                    }
                    Column {
                        Text(
                            text = "Socratic Tutor Eddie",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Math-Physics-Chemistry",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Your Patient & Sweet Socratic Companion",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
 
                Text(
                    text = "Eddie walks you through complex problems piece-by-piece—focusing on intuitive conceptual understanding in Math, Physics, and Chemistry rather than instant answers. Ask him why anytime!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun StudentLevelCard(
    selectedLevel: String,
    onLevelChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎓", fontSize = 18.sp)
                Text(
                    text = "Personalize Eddie's Teaching Level:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val levels = listOf("Struggling Beginner", "Curious Learner", "Advanced Student")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                levels.forEach { level ->
                    val isSelected = selectedLevel == level
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onLevelChange(level) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = level,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MathChalkboardCard(overviewText: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(4.dp, Color(0xFF5D4037), RoundedCornerShape(14.dp)), // Brown frame for blackboard
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E3A2F) // Chalkboard deep school green
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Little chalk decoration in bottom corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp, 8.dp)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("📝", fontSize = 14.sp)
                    Text(
                        text = "Equation Bounds:",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA5D6A7),
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = overviewText,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = "Eddie says: \"Let's crack this code step by step. Tell me whenever you feel stuck!\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun MathStepRevealCard(
    step: com.example.ui.chat.MathStep,
    isActive: Boolean,
    studentLevel: String,
    explanation: String,
    explanationLoading: Boolean,
    onAskClick: () -> Unit,
    onCustomAskClick: (String) -> Unit,
    history: List<String>
) {
    // Light golden/orange tone for the current focus step, or soft neutral for past steps
    val containerBg = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }

    val borderTint = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    var showCustomInput by remember { mutableStateOf(false) }
    var localCustomQ by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = borderTint,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Step index row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = step.stepIndex.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = step.stepTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Rule tag
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = step.conceptKey,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // The resulting equation expression card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = step.mathematicalExpression,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Intuitive explanation
            Text(
                text = step.intuitiveExplanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Socratic Dialogue Dialogue System configuration
            if (isActive) {
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                )

                // Dialogues logs
                if (history.isNotEmpty()) {
                    Text(
                        text = "Dialogue History:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        history.forEach { item ->
                            val isStudent = item.startsWith("Student:")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isStudent) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 10.dp,
                                                topEnd = 10.dp,
                                                bottomStart = if (isStudent) 10.dp else 2.dp,
                                                bottomEnd = if (isStudent) 2.dp else 10.dp
                                            )
                                        )
                                        .background(
                                            if (isStudent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .widthIn(max = 240.dp)
                                ) {
                                    Text(
                                        text = item.removePrefix("Student:").removePrefix("Tutor:").trim(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive query actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAskClick,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("why_did_we_do_that_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Why did we do that?", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showCustomInput = !showCustomInput },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ask_custom_question_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (showCustomInput) "Hide Ask" else "Ask Custom Q", fontSize = 12.sp)
                    }
                }

                AnimatedVisibility(visible = showCustomInput) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = localCustomQ,
                            onValueChange = { localCustomQ = it },
                            placeholder = { Text("How does LCD apply here...", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("custom_socratic_text_input"),
                            shape = RoundedCornerShape(10.dp),
                            textStyle = TextStyle(fontSize = 13.sp),
                            maxLines = 2
                        )

                        Button(
                            onClick = {
                                if (localCustomQ.isNotBlank()) {
                                    onCustomAskClick(localCustomQ)
                                    localCustomQ = ""
                                }
                            },
                            modifier = Modifier.weight(0.4f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Send", fontSize = 12.sp)
                        }
                    }
                }

                // Loading concept dialogue
                if (explanationLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Eddie is reviewing study guidelines...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Active tutor dialogue speech bubble
                if (explanation.isNotBlank() && !explanationLoading) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = 2.dp,
                            bottomEnd = 12.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("👩‍🏫", fontSize = 14.sp)
                                Text(
                                    text = "Eddie coordinates explanation:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resizeMathBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
    val srcW = bitmap.width
    val srcH = bitmap.height
    if (srcW <= maxDim && srcH <= maxDim) return bitmap
    val ratio = srcW.toFloat() / srcH.toFloat()
    val dstW: Int
    val dstH: Int
    if (srcW > srcH) {
        dstW = maxDim
        dstH = (maxDim / ratio).toInt()
    } else {
        dstH = maxDim
        dstW = (maxDim * ratio).toInt()
    }
    return Bitmap.createScaledBitmap(bitmap, dstW, dstH, true)
}
