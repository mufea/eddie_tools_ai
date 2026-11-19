package com.example.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.Locale

// Offline preloaded sample landmarks to guarantee perfect offline operation & rich immediate testing
data class SampleLandmark(
    val id: String,
    val name: String,
    val country: String,
    val description: String,
    val narrative: String,
    val themeColors: List<Color>,
    val hotspots: List<ArHotspot>,
    val defaultFacts: List<String>,
    val drawingType: String // "borobudur", "eiffel", "colosseum", "taj", "fuji"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EddiePhotoTourismView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onImageSelected: (Uri?, Bitmap?) -> Unit,
    onScanClick: (String) -> Unit,
    onSearchFactClick: (String) -> Unit,
    onSetTtsSpeaking: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Set up Local TTS engine
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
            }
        }
        tts.language = Locale.US
        ttsEngine = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Utterance Progress for voice feedback tracking
    LaunchedEffect(ttsEngine, ttsReady) {
        if (ttsReady && ttsEngine != null) {
            ttsEngine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSetTtsSpeaking(true)
                }
                override fun onDone(utteranceId: String?) {
                    onSetTtsSpeaking(false)
                }
                override fun onError(utteranceId: String?) {
                    onSetTtsSpeaking(false)
                }
            })
        }
    }

    fun speakNarration(text: String) {
        if (!ttsReady || ttsEngine == null) {
            Toast.makeText(context, "Voice synthesizer is warming up...", Toast.LENGTH_SHORT).show()
            return
        }
        ttsEngine?.stop()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ar_narrative")
        }
        ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "ar_narrative")
    }

    fun stopNarration() {
        ttsEngine?.stop()
        onSetTtsSpeaking(false)
    }

    // Static sample data definitions
    val sampleLandmarks = remember {
        listOf(
            SampleLandmark(
                id = "borobudur",
                name = "Borobudur Temple",
                country = "Central Java, Indonesia",
                description = "Borobudur is the world's largest Buddhist temple, built in the 9th century during the reign of the Sailendra Dynasty. Designed in the form of a stepped pyramid with three circular and six square tiers.",
                narrative = "Welcome to Borobudur, a massive stone monument rising above the misty forests of Kedu Basin in Java. This architectural marvel was lost to volcanic ash for centuries, representing Buddhist cosmology through its terraced concentric stupas.",
                themeColors = listOf(Color(0xFFE5A93C), Color(0xFF6B4E1B), Color(0xFF1E1404)),
                hotspots = listOf(
                    ArHotspot("Main Stupa", "The pinnacle dome symbolizing Nirvana, containing a serene empty inner chamber representing complete enlightenment.", 0.50f, 0.28f),
                    ArHotspot("Stupa Bells", "Houses the famous perforated bell-shaped stupas with seated Dhyani Buddha statues overlooking the landscape.", 0.72f, 0.45f),
                    ArHotspot("Relief Panels", "Stretching over basalt walls detailing the life of Gautama Buddha and Jataka fables across lower galleries.", 0.30f, 0.68f)
                ),
                defaultFacts = listOf(
                    "Constructed out of approximately 2 million volcanic stone blocks joined together without cement.",
                    "Discovered in 1814 of jungle vegetation by Sir Thomas Stamford Raffles.",
                    "Best visited in early morning during golden misty sunrises overlooking Mount Merapi."
                ),
                drawingType = "borobudur"
            ),
            SampleLandmark(
                id = "eiffel",
                name = "Eiffel Tower",
                country = "Paris, France",
                description = "Representing French elegance and structural prowess, the Eiffel Tower is an iron lattice monument towering 330 meters above Paris.",
                narrative = "Bonjour and welcome to Paris! Designed by Gustave Eiffel for the 1889 World's Fair, this iconic iron tower was originally labeled an eyesore by artists but stands today as the world's symbol of romance.",
                themeColors = listOf(Color(0xFFFF5E62), Color(0xFF381b37), Color(0xFF0F041A)),
                hotspots = listOf(
                    ArHotspot("The Summit", "Ascend 276m above Paris to the private office restoration of Gustave Eiffel and legendary panoramic views.", 0.50f, 0.12f),
                    ArHotspot("Double Deck Lifts", "The diagonal hydraulic elevator systems carrying travelers across complex iron curves since 1889.", 0.38f, 0.62f),
                    ArHotspot("Glass Platform", "First floor suspension glass pathway suspended 57m high, offering sensory depth looking straight down.", 0.62f, 0.48f)
                ),
                defaultFacts = listOf(
                    "Grows taller by up to 15 cm during hot summer days due to thermal metal expansion.",
                    "Repainted entirely by hand every 7 years, requiring over 60 metric tons of custom bronze paint.",
                    "Saves thousands of tourist footsteps if booked via secondary stairwells."
                ),
                drawingType = "eiffel"
            ),
            SampleLandmark(
                id = "colosseum",
                name = "The Colosseum",
                country = "Rome, Italy",
                description = "The largest ancient amphitheater ever built, completed in 80 AD under Emperor Titus. Flavian structure crafted from travertine, tuff, and brick.",
                narrative = "Step into Ancient Rome at the massive Flavian Amphitheatre. Capable of hosting 50,000 spectators, this bloody arena saw epic gladiatorial contests, simulated naval battles, and wild animal hunts.",
                themeColors = listOf(Color(0xFF8D6E63), Color(0xFF4E342E), Color(0xFF1B0F0B)),
                hotspots = listOf(
                    ArHotspot("The Hypogeum", "The sub-arena subterranean network of passages, trap doors, and cage systems once hoisting lions to fights.", 0.50f, 0.65f),
                    ArHotspot("Arch of Constantine", "Triumphal monumental stone gateway next to the Arena, celebrating victory over Maxentius.", 0.22f, 0.45f),
                    ArHotspot("Imperial Podium", "The primary southern viewing terrace reserved exclusively for the Roman Emperor and Vestal Virgins.", 0.74f, 0.38f)
                ),
                defaultFacts = listOf(
                    "Covered entirely by the 'Velarium', a giant retractable canvas awning operated by trained Roman sailors.",
                    "Stripped of half of its travertine stones during the Middle-Ages to construct Roman Palaces.",
                    "Purchasing combination tickets with the Roman Forum bypasses standard 3-hour tickets queues."
                ),
                drawingType = "colosseum"
            ),
            SampleLandmark(
                id = "taj",
                name = "Taj Mahal",
                country = "Agra, India",
                description = "An ivory-white marble mausoleum on the south bank of Yamuna river. Commissioned in 1632 by the Mughal emperor Shah Jahan to house the tomb of his favorite wife Mumtaz Mahal.",
                narrative = "Greetings from India. You are viewing the Taj Mahal, an absolute masterpiece of Mughal symmetry. Crafted from premium translucent white marble from Rajasthan, it shines softly in golden shades at dusk.",
                themeColors = listOf(Color(0xFF00B0FF), Color(0xFF003B46), Color(0xFF071F23)),
                hotspots = listOf(
                    ArHotspot("The Onion Dome", "Magnificent 35m tall central dome topped by a bronze finial, combining Persian and Indian styles.", 0.50f, 0.25f),
                    ArHotspot("Minaret Columns", "Four 40-meter flanking towers slightly leaned outward to protect the central tomb from earthquakes.", 0.15f, 0.50f),
                    ArHotspot("Reflection Pool", "Linear marble water pool mirroring the symmetrical architecture, creating a perfect vertical illusion.", 0.50f, 0.85f)
                ),
                defaultFacts = listOf(
                    "Employs over 28 styles of matching precious and semi-precious stones inlaid into the hard marble layers.",
                    "Changes color dynamically based on atmospheric light, from a pink hue at dawn to golden at night.",
                    "Closed to the general public every Friday for afternoon prayers."
                ),
                drawingType = "taj"
            ),
            SampleLandmark(
                id = "fuji",
                name = "Mount Fuji",
                country = "Honshu, Japan",
                description = "The highest mountain peak in Japan, standing at 3,776 meters. An exceptionally symmetrical stratovolcano snow-capped for several months of the year.",
                narrative = "Konnichiwa! Welcome to Mount Fuji, the holy mountain of Japan and an eternal muse for poets and painters. This active giant symbol of nature dominates the sky and forms a holy pilgrimage spot.",
                themeColors = listOf(Color(0xFF64B5F6), Color(0xFF1565C0), Color(0xFF0D1B2A)),
                hotspots = listOf(
                    ArHotspot("Snow Cap", "The magnificent white volcanic ice crown wrapping the conical summit, lasting from October to June.", 0.50f, 0.28f),
                    ArHotspot("Lake Kawaguchi", "One of the Five Fuji lakes reflecting inverted Fuji reflections (called Sakasa-Fuji) over cherry blossoms.", 0.50f, 0.82f),
                    ArHotspot("Torii Shrine Gate", "A majestic sacred red archway at the base, signifying entrance to holy mountain territory.", 0.20f, 0.68f)
                ),
                defaultFacts = listOf(
                    "Classified as an active volcano of low eruptive frequency, having last erupted in December 1707.",
                    "Visually famous for its symmetrical conic shape, serving as an emblem of Japan.",
                    "Surrounded by five pristine lakes reflecting mirror views of the peak."
                ),
                drawingType = "fuji"
            )
        )
    }

    // Set a default landmark at start - clean state by default
    var activeSampleId by remember { mutableStateOf<String?>(null) }
    val activeSample = sampleLandmarks.firstOrNull { it.id == activeSampleId }

    // Synchronize sample data to local state if no custom snapshot is scanned
    LaunchedEffect(activeSampleId) {
        if (activeSampleId != null && uiState.arSelectedBitmap == null) {
            onImageSelected(null, null)
            // Pre-fill fields with selected sample monument content!
            onScanClick("AUTO_LOAD_SAMPLE_${activeSampleId}")
        }
    }

    // Display selected hotspot details
    var selectedHotspot by remember { mutableStateOf<ArHotspot?>(null) }
    var showScanInstructionsPrompt by remember { mutableStateOf(false) }
    var arCustomPromptText by remember { mutableStateOf("") }
    var searchTextInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    LaunchedEffect(arCustomPromptText, searchTextInput) {
        if (arCustomPromptText.isNotEmpty() || searchTextInput.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // Launcher for selecting tourist photos from local Gallery
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val decoded: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                }
                if (decoded != null) {
                    val downscaled = resizeArBitmapToMax(decoded, 1080)
                    onImageSelected(uri, downscaled)
                    selectedHotspot = null
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open picture: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher for taking a live photo with local Camera
    val cameraCapture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val downscaled = resizeArBitmapToMax(bitmap, 1080)
            onImageSelected(null, downscaled)
            selectedHotspot = null
        }
    }

    // Scanning screen animations (radar rotations, visual lines)
    val infiniteTransition = rememberInfiniteTransition(label = "ar_scan_transition")
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rot"
    )

    val scannerYOffset by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanner_sweep"
    )

    // Speech Audio visualizer pulse animation
    val visualizerPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "audio_visualizer"
    )

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header Title
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🧭", fontSize = 28.sp)
                    Column {
                        Text(
                            text = "Photo Tourism AR Guide",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Convert tourist snapshots into highly interactive smart tour guides",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Select Interactive Samples Row (When no custom image selected)
            if (uiState.arSelectedBitmap == null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "EXPLORE ICONIC LANDMARKS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        sampleLandmarks.forEach { item ->
                            val isSelected = activeSampleId == item.id
                            ElevatedCard(
                                onClick = {
                                    activeSampleId = item.id
                                    selectedHotspot = null
                                    stopNarration()
                                },
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .width(135.dp)
                                    .height(58.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.country.split(",").last().trim(),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- THE AR VIEWPORT VIEWER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(1.3f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .background(Color.Black)
            ) {
                if (uiState.arSelectedBitmap != null) {
                    // Render uploaded tourist image
                    Image(
                        bitmap = uiState.arSelectedBitmap.asImageBitmap(),
                        contentDescription = "Uploaded Tourist snapshot",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (activeSample != null) {
                    // Render Gorgeous Procedural Canvas Drawing based on active sample
                    ProceduralLandmarkCanvas(
                        drawingType = activeSample.drawingType,
                        themeColors = activeSample.themeColors,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Clean AR Viewfinder Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Landmark Scan",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "AR Optical Telemetry Ready",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Snap a landmark photo or select a destination below to begin the tour.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // AR Grid / Coordinates Overlay HUD
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                                radius = 600f
                            )
                        )
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Grid marker crosses
                        val gridCount = 4
                        for (i in 1..gridCount) {
                            val x = w * (i.toFloat() / (gridCount + 1))
                            val y = h * (i.toFloat() / (gridCount + 1))
                            // Vertical lines
                            drawLine(Color.White.copy(alpha = 0.12f), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                            // Horizontal lines
                            drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                        }

                        // Compass angle ticks
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            radius = w * 0.1f,
                            center = Offset(w * 0.15f, h * 0.2f),
                            style = Stroke(width = 2f)
                        )
                    }

                    // Top AR Info Row
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (uiState.arIsScanning) Color.Yellow else Color.Green, CircleShape)
                        )
                        Text(
                            text = if (uiState.arIsScanning) "AR SYNCHRONIZING" else "AR TELEMETRY READY",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            fontSize = 9.sp
                        )
                    }

                    // Top Right Action: Reset to samples
                    if (uiState.arSelectedBitmap != null) {
                        IconButton(
                            onClick = {
                                onImageSelected(null, null)
                                selectedHotspot = null
                                stopNarration()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Exit custom image", tint = Color.White)
                        }
                    }
                }

                // Interactive Scanned AR Hotspots Overlay (Float buttons at x,y percentages)
                val activeHotspotsList = if (uiState.arSelectedBitmap != null) uiState.arHotspots else (activeSample?.hotspots ?: emptyList())

                activeHotspotsList.forEach { spot ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, top = 12.dp) // Offset for centering tag
                    ) {
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.35f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1100, easing = EaseOutQuad),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "pulse_tag_${spot.title}"
                        )
                        val opacityPulse by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 0.1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1100, easing = EaseOutQuad),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "alpha_tag_${spot.title}"
                        )

                        Box(
                            modifier = Modifier
                                .absoluteOffset(
                                    x = (spot.xPercent * 100).toInt().percentWidthModifier(),
                                    y = (spot.yPercent * 100).toInt().percentHeightModifier()
                                )
                        ) {
                            // Pulsing outer shadow trigger ring
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center)
                                    .rotate(radarRotation)
                                    .border(
                                        width = (2 * pulseScale).dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = opacityPulse),
                                        shape = CircleShape
                                    )
                            )

                            // Inner tap selector target dot
                            Surface(
                                color = if (selectedHotspot == spot) MaterialTheme.colorScheme.primary 
                                        else Color.White.copy(alpha = 0.9f),
                                border = BorderStroke(1.5.dp, Color.Black),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.Center)
                                    .clickable {
                                        selectedHotspot = if (selectedHotspot == spot) null else spot
                                        if (selectedHotspot != null) {
                                            speakNarration(spot.description)
                                        } else {
                                            stopNarration()
                                        }
                                    }
                            ) {}
                        }
                    }
                }

                // AI Sweeper Scan Animation Overlay when Active Scans run
                if (uiState.arIsScanning) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Scan light line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .absoluteOffset(y = (scannerYOffset * 100).toInt().percentHeightModifier())
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, Color.Cyan, Color.White, Color.Cyan, Color.Transparent)
                                    )
                                )
                        )

                        // Circular scan ring tracking center
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .align(Alignment.Center)
                                .rotate(radarRotation)
                                .border(2.dp, Color.Cyan.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.TopCenter)
                                    .background(Color.White, CircleShape)
                            )
                        }

                        // Informational Loader Text
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "DEEP SPECTROSCOPIC LANDMARK CLASSIFICATION INITIALIZED",
                                color = Color.Cyan,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                                fontSize = 8.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Popup card inside viewport for selected hotspots
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedHotspot != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    selectedHotspot?.let { spot ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.88f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "📍 " + spot.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                    )
                                    Text(
                                        text = spot.description,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                                    )
                                }
                                IconButton(
                                    onClick = { 
                                        selectedHotspot = null
                                        stopNarration()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("❌", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // --- CAMERA & GALLERY CAPTURE ACTIONS PANEL ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ANALYZE YOUR OWN SNAPSHOTS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { galleryPicker.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add image", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Gallery", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { cameraCapture.launch(null) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Camera", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Live Camera", fontSize = 12.sp)
                        }
                    }

                    // If custom user image selected: Enable Scanning Button!
                    if (uiState.arSelectedBitmap != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                        Button(
                            onClick = { onScanClick(arCustomPromptText) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !uiState.arIsScanning
                        ) {
                            Text("🔍 Synchronize AR & Scan with AI", fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = arCustomPromptText,
                            onValueChange = { arCustomPromptText = it },
                            placeholder = { Text("Optional guiding constraints, e.g. 'focus on stupas'...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            textStyle = MaterialTheme.typography.bodySmall,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // --- REAL-TIME AI RECOGNITION RESULTS PANEL ---
            val activeLandmarkName = if (uiState.arSelectedBitmap != null) uiState.arLandmarkName else (activeSample?.name ?: "")
            val activeLandmarkLocation = if (uiState.arSelectedBitmap != null) uiState.arLandmarkLocation else (activeSample?.country ?: "")
            val activeLandmarkDesc = if (uiState.arSelectedBitmap != null) uiState.arLandmarkDescription else (activeSample?.description ?: "")
            val activeLandmarkNarrative = if (uiState.arSelectedBitmap != null) uiState.arNarrativeText else (activeSample?.narrative ?: "")

            if (activeLandmarkName.isNotBlank()) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeLandmarkName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = activeLandmarkLocation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Dynamic Narration Speakeasy wave button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Wave oscilliscope animations
                                if (uiState.arTtsIsSpeaking) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        repeat(4) { index ->
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height((12 + (index * 4) * visualizerPulse).dp)
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (uiState.arTtsIsSpeaking) {
                                            stopNarration()
                                        } else {
                                            speakNarration(activeLandmarkNarrative)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiState.arTtsIsSpeaking) MaterialTheme.colorScheme.error 
                                                         else MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.arTtsIsSpeaking) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = "Voice Tour",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (uiState.arTtsIsSpeaking) "Stop Tour" else "Voice Tour", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = activeLandmarkDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Narrative Audio script view drawer
                        if (activeLandmarkNarrative.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "📖 IMMERSIVE AUDIO SCRIPT:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "\" $activeLandmarkNarrative \"",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // --- LIVE HISTORICAL FACT SEARCH PANEL ---
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), modifier = Modifier.padding(vertical = 4.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "🕵️ LIVE FACT & INTELLIGENT SEARCH",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Quick query pills
                            val quickQuestions = remember(activeSampleId) {
                                listOf("When was it built?", "Ticketing & Hours?", "Best spots for photographs?")
                            }

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickQuestions.forEach { query ->
                                    SuggestionChip(
                                        onClick = {
                                            searchTextInput = query
                                            onSearchFactClick(query)
                                        },
                                        label = { Text(query, fontSize = 11.sp) }
                                    )
                                }
                            }

                            // Search text fields input
                            OutlinedTextField(
                                value = searchTextInput,
                                onValueChange = { searchTextInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Ask anything about this landmark, ticket details, mysteries...") },
                                trailingIcon = {
                                    if (uiState.arIsSearchingFacts) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        IconButton(onClick = { 
                                            onSearchFactClick(searchTextInput)
                                            keyboardController?.hide()
                                        }) {
                                            Icon(Icons.Default.Send, contentDescription = "Search", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { 
                                    onSearchFactClick(searchTextInput)
                                    keyboardController?.hide()
                                })
                            )

                            // Displays retrieved live facts search list out
                            val activeFactsList = if (uiState.arSelectedBitmap != null) uiState.arFactsList else {
                                if (uiState.arFactsList.isNotEmpty()) uiState.arFactsList else (activeSample?.defaultFacts ?: emptyList())
                            }

                            if (activeFactsList.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    activeFactsList.forEach { fact ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("💡", fontSize = 14.sp)
                                            Text(
                                                text = fact,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Error display
            if (uiState.arSearchError.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = uiState.arSearchError,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

// Procedural visual illustration drawing on Canvas to run flawlessly offline
@Composable
fun ProceduralLandmarkCanvas(
    drawingType: String,
    themeColors: List<Color>,
    modifier: Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background sky-gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(themeColors[0], themeColors[1], themeColors[2])
            )
        )

        val strokeColor = Color.White.copy(alpha = 0.5f)

        when (drawingType) {
            "borobudur" -> {
                // Stepped pyramidal terraces layers
                val levels = 4
                for (i in 0 until levels) {
                    val insetX = w * (0.15f + i * 0.08f)
                    val insetW = w - (insetX * 2)
                    val topY = h * (0.85f - i * 0.12f)
                    val rectHeight = h * 0.12f

                    drawRect(
                        color = Color.Black.copy(alpha = 0.35f + i * 0.1f),
                        topLeft = Offset(insetX, topY),
                        size = Size(insetW, rectHeight)
                    )
                    // Border outline
                    drawRect(
                        color = strokeColor,
                        topLeft = Offset(insetX, topY),
                        size = Size(insetW, rectHeight),
                        style = Stroke(width = 1.5f)
                    )
                }

                // Main pinnacle stupa dome in center top
                val domeW = w * 0.14f
                val domeH = h * 0.16f
                val domeX = w / 2f - (domeW / 2f)
                val domeY = h * 0.38f
                drawArc(
                    color = Color.Black.copy(alpha = 0.85f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(domeX, domeY),
                    size = Size(domeW, domeH)
                )
                // Border outline dome
                drawArc(
                    color = strokeColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(domeX, domeY),
                    size = Size(domeW, domeH),
                    style = Stroke(width = 2f)
                )

                // Stupa spire needle top
                drawLine(
                    color = strokeColor,
                    start = Offset(w / 2f, domeY),
                    end = Offset(w / 2f, h * 0.22f),
                    strokeWidth = 3f
                )

                // Flanking side bell stupas
                val bellW = w * 0.07f
                val bellH = h * 0.08f
                // left bell
                drawArc(Color.Black.copy(alpha = 0.7f), 180f, 180f, true, Offset(w * 0.33f, h * 0.50f), Size(bellW, bellH))
                drawArc(strokeColor, 180f, 180f, true, Offset(w * 0.33f, h * 0.50f), Size(bellW, bellH), style = Stroke(width = 1.5f))
                drawLine(strokeColor, Offset(w * 0.33f + bellW/2, h * 0.50f), Offset(w * 0.33f + bellW/2, h * 0.44f), strokeWidth = 2f)
                // right bell
                drawArc(Color.Black.copy(alpha = 0.7f), 180f, 180f, true, Offset(w * 0.60f, h * 0.50f), Size(bellW, bellH))
                drawArc(strokeColor, 180f, 180f, true, Offset(w * 0.60f, h * 0.50f), Size(bellW, bellH), style = Stroke(width = 1.5f))
                drawLine(strokeColor, Offset(w * 0.60f + bellW/2, h * 0.50f), Offset(w * 0.60f + bellW/2, h * 0.44f), strokeWidth = 2f)
            }

            "eiffel" -> {
                // Drawing Eiffel Tower steel pillars
                val pLeftBot = Offset(w * 0.35f, h * 0.9f)
                val pRightBot = Offset(w * 0.65f, h * 0.9f)
                val pSummit = Offset(w / 2f, h * 0.20f)

                // Center vertical line
                drawLine(strokeColor.copy(alpha = 0.2f), Offset(w / 2f, h * 0.9f), pSummit, strokeWidth = 1f)

                // Main lattice paths
                drawTriangleOutline(this, pLeftBot, pRightBot, pSummit, strokeColor)

                // Platforms horizontal bars
                val bar1Y = h * 0.72f
                val bar2Y = h * 0.52f
                val bar3Y = h * 0.34f

                drawLine(strokeColor, Offset(w * 0.41f, bar1Y), Offset(w * 0.59f, bar1Y), strokeWidth = 4f) // First deck
                drawLine(strokeColor, Offset(w * 0.45f, bar2Y), Offset(w * 0.55f, bar2Y), strokeWidth = 3f) // Second deck
                drawLine(strokeColor, Offset(w * 0.48f, bar3Y), Offset(w * 0.52f, bar3Y), strokeWidth = 2.5f) // Third deck

                // Bottom Roman Archway span
                drawArc(
                    color = Color.Black.copy(alpha = 0.3f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.42f, h * 0.72f),
                    size = Size(w * 0.16f, h * 0.36f)
                )
                drawArc(
                    color = strokeColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.42f, h * 0.72f),
                    size = Size(w * 0.16f, h * 0.36f),
                    style = Stroke(width = 2f)
                )

                // Top spire flag needle
                drawLine(strokeColor, pSummit, Offset(w / 2f, h * 0.10f), strokeWidth = 2f)
            }

            "colosseum" -> {
                // Draw oval arches amphitheater facade
                val wallW = w * 0.74f
                val wallH = h * 0.55f
                val wallX = w * 0.13f
                val wallY = h * 0.35f

                // Base block
                drawRect(
                    color = Color.Black.copy(alpha = 0.4f),
                    topLeft = Offset(wallX, wallY),
                    size = Size(wallW, wallH)
                )
                drawRect(
                    color = strokeColor,
                    topLeft = Offset(wallX, wallY),
                    size = Size(wallW, wallH),
                    style = Stroke(width = 2f)
                )

                // Retractable top velarium bars
                val lineCount = 12
                for (j in 0..lineCount) {
                    val lineX = wallX + (wallW / lineCount) * j
                    drawLine(strokeColor.copy(alpha = 0.25f), Offset(lineX, wallY), Offset(lineX + (if (j < 6) -15f else 15f), wallY - 18f), strokeWidth = 2f)
                }

                // Row of ancient imperial columns arches
                val columns = 5
                val colW = wallW / columns
                for (idx in 0 until columns) {
                    val itemX = wallX + (idx * colW)
                    // Draw bottom archway door outline
                    drawArc(
                        color = Color.Black.copy(alpha = 0.6f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(itemX + colW * 0.2f, wallY + wallH * 0.3f),
                        size = Size(colW * 0.6f, wallH * 0.7f)
                    )
                    drawArc(
                        color = strokeColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(itemX + colW * 0.2f, wallY + wallH * 0.3f),
                        size = Size(colW * 0.6f, wallH * 0.7f),
                        style = Stroke(width = 1.5f)
                    )

                    // Draw second tier columns arches
                    drawArc(
                        color = strokeColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(itemX + colW * 0.25f, wallY + wallH * 0.08f),
                        size = Size(colW * 0.5f, wallH * 0.38f),
                        style = Stroke(width = 1f)
                    )
                }
            }

            "taj" -> {
                // Symmetrical Mughal mausoleum architecture drawing
                val baseW = w * 0.44f
                val baseH = h * 0.35f
                val baseX = w / 2f - (baseW / 2f)
                val baseY = h * 0.55f

                // Central chamber building
                drawRect(Color.Black.copy(alpha = 0.4f), Offset(baseX, baseY), Size(baseW, baseH))
                drawRect(strokeColor, Offset(baseX, baseY), Size(baseW, baseH), style = Stroke(width = 2f))

                // Majestic onion dome
                val domeW = w * 0.20f
                val domeH = h * 0.26f
                val domeX = w / 2f - (domeW / 2f)
                val domeY = baseY - domeH + 10f
                drawArc(Color.Black.copy(alpha = 0.75f), 180f, 180f, true, Offset(domeX, domeY), Size(domeW, domeH))
                drawArc(strokeColor, 180f, 180f, true, Offset(domeX, domeY), Size(domeW, domeH), style = Stroke(width = 2f))

                // Left minaret flanking tower
                val flTowerW = w * 0.03f
                val flTowerH = h * 0.45f
                drawRect(Color.Black.copy(alpha = 0.5f), Offset(w * 0.20f, h * 0.45f), Size(flTowerW, flTowerH))
                drawRect(strokeColor, Offset(w * 0.20f, h * 0.45f), Size(flTowerW, flTowerH), style = Stroke(width = 1.5f))
                // Right minaret tower
                drawRect(Color.Black.copy(alpha = 0.5f), Offset(w * 0.77f, h * 0.45f), Size(flTowerW, flTowerH))
                drawRect(strokeColor, Offset(w * 0.77f, h * 0.45f), Size(flTowerW, flTowerH), style = Stroke(width = 1.5f))

                // Gate entrance archway in center
                drawArc(Color.Black.copy(alpha = 0.6f), 180f, 180f, true, Offset(w * 0.42f, baseY + baseH * 0.2f), Size(w * 0.16f, baseH * 1.3f))
                drawArc(strokeColor, 180f, 180f, true, Offset(w * 0.42f, baseY + baseH * 0.2f), Size(w * 0.16f, baseH * 1.3f), style = Stroke(width = 1.5f))
            }

            "fuji" -> {
                // Wide volcanic conical peak path
                val leftBase = Offset(w * 0.08f, h * 0.9f)
                val rightBase = Offset(w * 0.92f, h * 0.9f)
                val peakTopLeft = Offset(w * 0.42f, h * 0.35f)
                val peakTopRight = Offset(w * 0.58f, h * 0.35f)

                // Fill color of mountain
                val mountainPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(leftBase.x, leftBase.y)
                    lineTo(peakTopLeft.x, peakTopLeft.y)
                    lineTo(peakTopRight.x, peakTopRight.y)
                    lineTo(rightBase.x, rightBase.y)
                    close()
                }
                drawPath(mountainPath, Color.Black.copy(alpha = 0.45f))

                // Outer outline limit
                drawLine(strokeColor, leftBase, peakTopLeft, strokeWidth = 2f)
                drawLine(strokeColor, peakTopLeft, peakTopRight, strokeWidth = 3f)
                drawLine(strokeColor, peakTopRight, rightBase, strokeWidth = 2f)

                // Premium snow-cap boundary lines
                val snowBottomLeft = Offset(w * 0.30f, h * 0.55f)
                val snowBottomRight = Offset(w * 0.70f, h * 0.55f)

                val snowPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(peakTopLeft.x, peakTopLeft.y)
                    lineTo(peakTopRight.x, peakTopRight.y)
                    lineTo(snowBottomRight.x, snowBottomRight.y)
                    // Ridge snow fingers wavy line
                    cubicTo(w*0.62f, h*0.62f, w*0.58f, h*0.50f, w*0.50f, h*0.58f)
                    cubicTo(w*0.42f, h*0.64f, w*0.38f, h*0.48f, snowBottomLeft.x, snowBottomLeft.y)
                    close()
                }
                drawPath(snowPath, Color.White.copy(alpha = 0.45f))
                drawPath(snowPath, strokeColor, style = Stroke(width = 1.5f))

                // Red Shinto Torii Gate base right side
                val gateW = w * 0.08f
                val gateH = h * 0.12f
                val gateX = w * 0.22f
                val gateY = h * 0.78f
                // Procedural Shinto gates drawing
                drawRect(Color(0xFFE53935), Offset(gateX, gateY), Size(gateW, gateH))
                drawRect(Color.Black, Offset(gateX + gateW*0.2f, gateY + gateH*0.15f), Size(gateW * 0.6f, gateH * 0.85f))
                drawRect(strokeColor, Offset(gateX, gateY), Size(gateW, gateH), style = Stroke(width = 1f))
            }
        }
    }
}

private fun drawTriangleOutline(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    p1: Offset,
    p2: Offset,
    p3: Offset,
    color: Color
) {
    scope.drawLine(color, p1, p2, strokeWidth = 2.5f)
    scope.drawLine(color, p2, p3, strokeWidth = 2.5f)
    scope.drawLine(color, p3, p1, strokeWidth = 2.5f)
}

// Coordinate percentage multipliers safe helpers for dynamic layouts bounding boxes
private fun Int.percentWidthModifier(): androidx.compose.ui.unit.Dp {
    // Limits percentage placement safe within the 0 to 100 aspect container bounds
    return (((this.coerceIn(5, 95)) * 3.2f) - 16).dp
}

private fun Int.percentHeightModifier(): androidx.compose.ui.unit.Dp {
    return (((this.coerceIn(5, 95)) * 2.3f) - 16).dp
}

private fun resizeArBitmapToMax(bitmap: Bitmap, maxDim: Int): Bitmap {
    val srcW = bitmap.width
    val srcH = bitmap.height
    if (srcW <= maxDim && srcH <= maxDim) return bitmap
    
    val ratio = srcW.toFloat() / srcH.toFloat()
    val targetW = if (srcW > srcH) maxDim else (maxDim * ratio).toInt()
    val targetH = if (srcH > srcW) maxDim else (maxDim / ratio).toInt()
    
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
}
