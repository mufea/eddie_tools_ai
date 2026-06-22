package com.example.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast

// Template data model
data class WriterTemplate(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val icon: String,
    val field1Label: String,
    val field1Placeholder: String,
    val field2Label: String,
    val field2Placeholder: String,
    val systemPrompt: String,
    val userPromptBuilder: (String, String, String) -> String
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun EddieAiWriterView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onGenerate: (String, String) -> Unit, // Uses a callback to generate content
    onClear: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    // 1. Defining templates for all 17 categories requested by the user
    val templates = remember {
        getBlogTemplates() +
        getEcommerceTemplates() +
        getDevelopmentTemplates() +
        getAdvertisementTemplates() +
        getSocialMediaTemplates() +
        getEmailTemplates() +
        getAcademicTemplates() +
        getBusinessTemplates() +
        getCustomerServiceTemplates() +
        getEntertainmentTemplates() +
        getWebsiteTemplates() +
        getAdvertisingTemplates() +
        getLanguagesTemplates() +
        getFitnessHealthTemplates() +
        getWriterTemplates() +
        getMiscTemplates() +
        getPersonalGrowthTemplates()
    }

    // 2. Fetch list of Categories (17 unique items in original requested sequence)
    val categories = remember {
        listOf(
            "Blog", "Ecommerce", "Development", "Advertisement", "Social Media", 
            "Email", "Academic", "Business", "Customer Service", "Entertainment", 
            "Website", "Advertising", "Languages", "Fitness & Health", "Writer", "Misc", "Personal Growth"
        )
    }

    var selectedCategory by remember { mutableStateOf("Blog") }
    var selectedTemplate by remember { mutableStateOf<WriterTemplate?>(null) }
    
    // Inputs state
    var field1Text by remember { mutableStateOf("") }
    var field2Text by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf("Persuasive") }
    val toneOptions = listOf("Persuasive", "Professional", "Casual & Friendly", "Bold & Confident", "Witty & Creative")

    // Filter templates for chosen category
    val currentTemplatesOutput = remember(selectedCategory) {
        templates.filter { it.category == selectedCategory }
    }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card (Non-sticky)
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
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
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✍️", style = TextStyle(fontSize = 26.sp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "AI Writer & Copywriter",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Generate professional, high-converting copy, blog outline structures, readme files, or campaigns using pre-existing premium templates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState.writerOutput.isNotBlank()) {
                // RENDER GENERATED MASTERPIECE COPY
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✨ WRITTEN COPY ARCHETYPE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            ) {
                                Text(
                                    text = selectedTemplate?.name ?: "Copy Draft",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Render beautiful formatted markdown markup text
                        MarkdownText(
                            text = uiState.writerOutput,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Control Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(uiState.writerOutput))
                                    Toast.makeText(context, "Copied songwriting draft to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Output", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            OutlinedButton(
                                onClick = {
                                    // Go back to edit
                                    val currentPrompt = selectedTemplate?.userPromptBuilder?.invoke(field1Text, field2Text, selectedTone) ?: ""
                                    val systemPrompt = selectedTemplate?.systemPrompt ?: ""
                                    onGenerate(currentPrompt, systemPrompt)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Re-Write", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            IconButton(
                                onClick = {
                                    onClear()
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Fields",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else if (uiState.writerIsLoading) {
                // GENERATING LOADING CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Drafting compelling hook models, formatting sub-headers, structuring paragraphs, and refining copywriting tone...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (selectedTemplate == null) {
                // STEP 1: CATEGORY SELECTION & TEMPLATES LIST DIRECTORY
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "1. CHOOSE A COPYWRITING CATEGORY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Wrapping categories layout instead of horizontal scrolling bar
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = cat == selectedCategory
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "2. SELECT A TEMPLATE FOR $selectedCategory",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Templates belonging to category
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        currentTemplatesOutput.forEach { template ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTemplate = template
                                        field1Text = ""
                                        field2Text = ""
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(template.icon, style = TextStyle(fontSize = 18.sp))
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = template.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = template.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // STEP 2: CHOSEN TEMPLATE FORM INPUT
                val template = selectedTemplate!!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header info of template
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(template.icon, style = TextStyle(fontSize = 22.sp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Category: ${template.category}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                        // Field 1
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = template.field1Label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = field1Text,
                                onValueChange = { field1Text = it },
                                placeholder = { Text(template.field1Placeholder, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                maxLines = 4,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        // Field 2
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = template.field2Label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = field2Text,
                                onValueChange = { field2Text = it },
                                placeholder = { Text(template.field2Placeholder, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                maxLines = 4,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        // Tone Selection Row
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "🎨 Choose Tone of Voice",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                toneOptions.forEach { tone ->
                                    val isSel = tone == selectedTone
                                    Card(
                                        modifier = Modifier.clickable { selectedTone = tone },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Text(
                                            text = tone,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Generative Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { selectedTemplate = null },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Back")
                            }

                            Button(
                                onClick = {
                                    if (field1Text.isBlank()) {
                                        Toast.makeText(context, "Please configure mandatory fields to generate drafts.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val finalPrompt = template.userPromptBuilder(
                                        field1Text,
                                        field2Text,
                                        selectedTone
                                    )
                                    onGenerate(finalPrompt, template.systemPrompt)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(2f)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Text")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getBlogTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "blog_post_title_generator",
        name = "Post Title Generator",
        category = "Blog",
        description = "Generate highly engaging, clickworthy, and SEO-optimized blog post titles.",
        icon = "💡",
        field1Label = "Core Topic or Niche",
        field1Placeholder = "e.g., Passive Income Streams in 2026",
        field2Label = "Target Audience details",
        field2Placeholder = "e.g., college students, freelancers",
        systemPrompt = "You are a master SEO Copywriter. Generate 10 clickworthy, engaging, and SEO-optimized blog post titles for the given topic. Group them into categories: Creative, How-To, List-Based, and Question-Based.",
        userPromptBuilder = { f1, f2, tone -> "Generate blog post titles for: \"$f1\". Target audience: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "blog_summarize_text",
        name = "Summarize Text",
        category = "Blog",
        description = "Synthesize long blog posts or documents into concise, value-packed summaries.",
        icon = "📝",
        field1Label = "Text to Summarize",
        field1Placeholder = "Paste your long blog, document, or research text here...",
        field2Label = "Focus / Specific Angle",
        field2Placeholder = "e.g., summarize key action items, or focus on financial metrics",
        systemPrompt = "You are an expert editorial writer. Synthesize the provided text into a high-level 2-sentence summary followed by a bulleted list of the absolute most critical takeaways.",
        userPromptBuilder = { f1, f2, tone -> "Summarize the following text: \"$f1\". Focus/Notes: \"$f2\". Summary style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "blog_article_generator",
        name = "Article Generator",
        category = "Blog",
        description = "Draft fully structured articles with clean sections, lists, and takeaways.",
        icon = "📰",
        field1Label = "Article Topic / Core Keyword",
        field1Placeholder = "e.g., The Future of AI in Modern Graphic Design",
        field2Label = "Outline Points / Keywords to Include",
        field2Placeholder = "e.g., generative tools, UI design trends, creator empowerment",
        systemPrompt = "You are a professional blog writer. Generate a comprehensive, 600-word article using clean, structured markdown headers (H2, H3), paragraphs, bullet points, and an inspiring conclusion.",
        userPromptBuilder = { f1, f2, tone -> "Write a full article about \"$f1\". Include these points/keywords: \"$f2\". Styling/Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "blog_section",
        name = "Blog Section",
        category = "Blog",
        description = "Write a comprehensive section or focused chapter for an ongoing blog draft.",
        icon = "🧩",
        field1Label = "Section Topic / Main Idea",
        field1Placeholder = "e.g., The physical limitations of lithium-ion battery units",
        field2Label = "Key Points to cover",
        field2Placeholder = "e.g., energy density, cold weather drop off, lifespan degradation",
        systemPrompt = "You are an expert content writer. Write a detailed, cohesive section for a blog article. Maintain excellent transitions, deep explanations, and a natural voice.",
        userPromptBuilder = { f1, f2, tone -> "Draft a blog section about \"$f1\". Points to cover: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "blog_post_ideas",
        name = "Blog Post Ideas",
        category = "Blog",
        description = "Unlock a list of trending, creative blog post concepts in your niche.",
        icon = "🧠",
        field1Label = "Niche or Focus Topic",
        field1Placeholder = "e.g., Indoor vertical hydroponics for small apartments",
        field2Label = "Primary Audience Interest",
        field2Placeholder = "e.g., budget setup, low maintenance, organic herbs",
        systemPrompt = "You are a seasoned content strategist. Provide 8 creative, high-impact blog post ideas for the given niche. For each idea, include a suggested title and a one-sentence angle preview.",
        userPromptBuilder = { f1, f2, tone -> "Generate blog post ideas for \"$f1\". Focus: \"$f2\". Tone direction: \"$tone\"." }
    ),
    WriterTemplate(
        id = "blog_intros",
        name = "Blog Intros",
        category = "Blog",
        description = "Construct highly engaging blog intro sections to hook your visitors.",
        icon = "🎬",
        field1Label = "Blog Post Title / Topic",
        field1Placeholder = "e.g., 5 essential habits for deep focus in open-plan offices",
        field2Label = "Main Problem / Context",
        field2Placeholder = "e.g., constant noise distraction, feeling mentally drained at 2 PM",
        systemPrompt = "You are a storytelling wizard. Write three different introductory styles for a blog: 1. Anecdote/Hook style, 2. Fact/Statistic style, 3. Question/Relatable-problem style.",
        userPromptBuilder = { f1, f2, tone -> "Draft 3 introductions for \"$f1\". Problem context: \"$f2\". Make them in target tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "blog_conclusion",
        name = "Blog Conclusion",
        category = "Blog",
        description = "Draft strong conclusions to wrap up your posts with clear next steps.",
        icon = "🏁",
        field1Label = "Main takeaway / Blog Focus",
        field1Placeholder = "e.g., consistent gratitude journaling transforms neural pathways",
        field2Label = "Call-To-Action (CTA)",
        field2Placeholder = "e.g., download our free workbook or leave a comment below",
        systemPrompt = "You are an inspiring writer. Craft a powerful blog post conclusion. Summarize the heart of the concept and end with a compelling, modern call-to-action.",
        userPromptBuilder = { f1, f2, tone -> "Write a blog conclusion about \"$f1\". Call-To-Action: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "blog_paragraph_generator",
        name = "Paragraph Generator",
        category = "Blog",
        description = "Perfect single paragraphs on any key subject or argument.",
        icon = "✍️",
        field1Label = "Primary subject / Argument",
        field1Placeholder = "e.g., how blue light exposure affects REM sleep cycle",
        field2Label = "Key words / Supporting facts",
        field2Placeholder = "e.g., melatonin inhibition, circadian rhythm, morning grogginess",
        systemPrompt = "You are an educational copywriter. Draft a highly logical, engaging, and detailed paragraph about the given subject, weaving in supporting details cleanly.",
        userPromptBuilder = { f1, f2, tone -> "Write a detailed paragraph about \"$f1\". Supporting details: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "blog_grammar_correction",
        name = "Grammar Correction",
        category = "Blog",
        description = "Detect spelling/syntax errors and rewrite with pristine vocabulary.",
        icon = "🔍",
        field1Label = "Draft Content to Edit",
        field1Placeholder = "Type or paste your text with errors here...",
        field2Label = "Writing rules or level",
        field2Placeholder = "e.g., keep it casual, or make it sound highly academic",
        systemPrompt = "You are a professional chief editor. Review the text, correct all grammatical, structural, and syntax errors, and supply a polished, publication-ready version.",
        userPromptBuilder = { f1, f2, tone -> "Correct and improve the grammar of this text: \"$f1\". Rules: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "blog_tldr_summarization",
        name = "TL;DR Summarization",
        category = "Blog",
        description = "Compile super quick 'Too Long; Didn't Read' summaries for busy readers.",
        icon = "⏱️",
        field1Label = "Long Form Content",
        field1Placeholder = "Paste the detailed text block here...",
        field2Label = "Target Summary Length",
        field2Placeholder = "e.g., 1 short sentence, or 3 single phrases",
        systemPrompt = "You are a sharp summary editor. Deliver an ultra-concise, fun, and extremely clear TL;DR summary that immediately captures the raw essence of the content.",
        userPromptBuilder = { f1, f2, tone -> "Provide a TL;DR summary for this text: \"$f1\". Length target: \"$f2\". Style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "blog_ideas_2",
        name = "Blog Ideas",
        category = "Blog",
        description = "Brainstorm viral, creative, and engaging core concepts for articles.",
        icon = "💡",
        field1Label = "Blog Niche & Vibe",
        field1Placeholder = "e.g., slow living and handmade ceramics",
        field2Label = "Audience Demographics",
        field2Placeholder = "e.g., creative professionals, design-conscious adults",
        systemPrompt = "Provide 8 premium, distinctive blog post ideas complete with catchy titles, specific angles, and short summaries matching the niche.",
        userPromptBuilder = { f1, f2, tone -> "Generate blog ideas for niche: \"$f1\". Target audience: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "blog_intro_2",
        name = "Blog Intro",
        category = "Blog",
        description = "Craft a single beautiful introduction hook that sparks immediate curiosity.",
        icon = "🎯",
        field1Label = "Blog Post Title & Angle",
        field1Placeholder = "e.g., Why remote work is slowly killing our organic communities",
        field2Label = "Opening Fact / Hook Angle",
        field2Placeholder = "e.g., human face-to-face interaction has dropped by 40%",
        systemPrompt = "Craft a highly captivating, high-converting intro paragraph for a blog. Establish the pain point, state the importance, and present an eye-opening solution promise.",
        userPromptBuilder = { f1, f2, tone -> "Draft a single perfect introductory blog hook for \"$f1\". Hook detail: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "blog_outline",
        name = "Blog Outline",
        category = "Blog",
        description = "Produce structured outline drafts including sections, talking points, and subheaders.",
        icon = "📋",
        field1Label = "Blog Topic / Angle",
        field1Placeholder = "e.g., The Ultimate Guide to Off-Grid Solar Power for RVs",
        field2Label = "Key Sections to Include",
        field2Placeholder = "e.g., panel math, batteries, cost estimation, rookie mistakes",
        systemPrompt = "Produce a highly structured, thorough SEO-optimized blog outline featuring H2 and H3 structures, suggested bullet points to cover under each header, and a quick word count goal recommendation.",
        userPromptBuilder = { f1, f2, tone -> "Produce a blog outline on topic \"$f1\". Focus areas: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "blog_article",
        name = "Blog Article",
        category = "Blog",
        description = "Write a complete beautifully-styled blog post in raw markdown.",
        icon = "📝",
        field1Label = "Core Topic & Title Idea",
        field1Placeholder = "e.g., How taking 5-minute cold showers rebuilt my productivity",
        field2Label = "Important points & Facts",
        field2Placeholder = "e.g., dopamine baseline levels, immediate nervous system shock, morning consistency",
        systemPrompt = "You are an elite creative writer. Produce a long-form, highly detailed, beautifully structured markdown article. Use paragraphs, bullet journals, tips, and bold headings to make reading pleasant.",
        userPromptBuilder = { f1, f2, tone -> "Compose a full blog article on \"$f1\". Points: \"$f2\". Tone: \"$tone\"." }
    )
)

private fun getEcommerceTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "ecom_product_description",
        name = "Product Description",
        category = "Ecommerce",
        description = "Generate persuasive, benefit-driven product write-ups.",
        icon = "🛒",
        field1Label = "Product Name & Category",
        field1Placeholder = "e.g., AeroGrip Hybrid Hiking Shoes",
        field2Label = "Features & Selling Points",
        field2Placeholder = "e.g., breathable carbon mesh, non-slip rubber, waterproof lining",
        systemPrompt = "You are an expert E-commerce Copywriter. Deliver a high-converting product description featuring an attention-grabbing hook, bulleted features, emotional core benefits, and a clear call to action.",
        userPromptBuilder = { f1, f2, tone -> "Write a product description for \"$f1\" with features: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ecom_product_name_generator",
        name = "Product Name Generator",
        category = "Ecommerce",
        description = "Brainstorm catchy, modern, and brandable product names.",
        icon = "🏷️",
        field1Label = "Product Description & Type",
        field1Placeholder = "e.g., a quiet rechargeable hand warmers for skiers",
        field2Label = "Keywords / Brand Style",
        field2Placeholder = "e.g., warm, cozy, sleek, minimalist, alpine",
        systemPrompt = "Draft 15 creative product name options categorizing them by style: Modern, Descriptive, Compound, and Abstract.",
        userPromptBuilder = { f1, f2, tone -> "Generate creative product name ideas for \"$f1\". Style guide: \"$f2\". Brand tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ecom_testimonial_review",
        name = "Testimonial Review",
        category = "Ecommerce",
        description = "Draft authentic-sounding product reviews or templates for testimonials.",
        icon = "⭐",
        field1Label = "Product / Service experience",
        field1Placeholder = "e.g., using Vertex Coffee Maker - amazing espresso but takes slow to preheat",
        field2Label = "Reviewer Vibe / Demographics",
        field2Placeholder = "e.g., busy dad, coffee connoisseur, tech enthusiast",
        systemPrompt = "Generate three distinct authentic, well-written user testimonials. Include a catchy summary title and rating stars (1-5) for each variation to show realistic product impressions.",
        userPromptBuilder = { f1, f2, tone -> "Generate realistic product testimonials/reviews for \"$f1\". Persona: \"$f2\". Tone style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ecom_product_features",
        name = "Product Features",
        category = "Ecommerce",
        description = "Transform technical specs into high-converting feature bullets.",
        icon = "💎",
        field1Label = "Raw Technical Specs",
        field1Placeholder = "e.g., 5000mAh battery, custom DSP audio chip, 120Hz display, IP68 water rating",
        field2Label = "Target Benefit Focus",
        field2Placeholder = "e.g., freedom to travel without charges, immersive sound, crystal-clear streaming",
        systemPrompt = "Convert raw specifications into benefit-first, catchy bullet points where each specification is coupled with a real, everyday human emotional convenience.",
        userPromptBuilder = { f1, f2, tone -> "Convert specs \"$f1\" emphasizing \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ecom_product_name_ideas",
        name = "Product Name Ideas",
        category = "Ecommerce",
        description = "Alternative creative product naming setups with focus key guidelines.",
        icon = "🎒",
        field1Label = "What is the Product?",
        field1Placeholder = "e.g., bio-degradable toothbrushes infused with charcoal",
        field2Label = "Target vibe or style",
        field2Placeholder = "e.g., eco-luxury, sustainable, dental health tech",
        systemPrompt = "Provide 10 highly brandable product name ideas, explaining the psychological naming reason behind each.",
        userPromptBuilder = { f1, f2, tone -> "List product name ideas for \"$f1\". Vibe checklist: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ecom_why_choose_this",
        name = "Why Choose This Product",
        category = "Ecommerce",
        description = "Craft persuasive 'Why Choose Us' paragraphs for product or sales pages.",
        icon = "✔️",
        field1Label = "Product Name & Competitor weakness",
        field1Placeholder = "e.g., Lucid Sleep Mask - doesn't press against eyelashes like standard flat masks",
        field2Label = "Unique Material / Quality Highlight",
        field2Placeholder = "e.g., organic memory foam, 100% blindout, lightweight straps",
        systemPrompt = "Write a high-conviction, professional 'Why Choose This Product' listing section detailing 3-4 bulletproof arguments explaining why purchase is absolutely superior to generic options.",
        userPromptBuilder = { f1, f2, tone -> "Develop a 'Why Choose Us/This' copy block for \"$f1\". Focus: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ecom_reviews",
        name = "Reviews",
        category = "Ecommerce",
        description = "Assemble customizable e-commerce product reviews carrying typical highlights.",
        icon = "🌟",
        field1Label = "Product to Outline Reviews",
        field1Placeholder = "e.g., UltraBrush Sonic toothbrush",
        field2Label = "Common positive aspects",
        field2Placeholder = "e.g., battery life is insane, bristles are surprisingly soft",
        systemPrompt = "Draft three realistic, structured product reviews (positive, neutral/constructive, and highly enthusiastic) showcasing organic consumer language.",
        userPromptBuilder = { f1, f2, tone -> "Create realistic reviews for \"$f1\". Core details: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ecom_review_responder",
        name = "Review Responder",
        category = "Ecommerce",
        description = "Write professional, personalized, and empathetic responses to customer reviews.",
        icon = "💬",
        field1Label = "Customer Review Text",
        field1Placeholder = "e.g., 'The device works fine but the charging cable was missing from the box. Disappointed.'",
        field2Label = "Resolution Choice Offered",
        field2Placeholder = "e.g., send out a replacement cable immediately, offer a 20% refund coupon",
        systemPrompt = "Act as an E-commerce Customer Delight Coordinator. Draft a highly professional and appreciative reply to the customer's review. Maintain brand credibility, apologize gracefully, and propose the resolution clearly.",
        userPromptBuilder = { f1, f2, tone -> "Write response to review: \"$f1\". Propose resolution: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ecom_amazon",
        name = "Amazon",
        category = "Ecommerce",
        description = "Draft Amazon-optimized descriptions with highlights and technical bullet listings.",
        icon = "📦",
        field1Label = "Amazon Product Name & Keyword",
        field1Placeholder = "e.g., Ergonomic lumbar support pillow for office chairs",
        field2Label = "Five Bullet Highlights key points",
        field2Placeholder = "e.g., 100% memory foam, dual strap design, breathable mesh cover, orthotic approved, fits all chairs",
        systemPrompt = "Write an Amazon-optimized product listing containing an attention-grabbing Title loaded with high-traffic keywords, and a bulleted list of the standard 5 Amazon bullet points highlighting benefits.",
        userPromptBuilder = { f1, f2, tone -> "Draft Amazon copy for product \"$f1\". Highlights: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ecom_cart_abandonment",
        name = "Cart Recovery Email",
        category = "Ecommerce",
        description = "Write a persuasive, low-friction outreach email to recover incomplete checkout baskets.",
        icon = "🛒",
        field1Label = "Product Name & Discount offering",
        field1Placeholder = "e.g., AeroGrip Hiking Boots, 10% off callback code COMEBACK10",
        field2Label = "Core emotional benefit / support",
        field2Placeholder = "e.g., standard 12-month free warranty, guaranteed sizing exchanges",
        systemPrompt = "You are a conversion copywriting expert. Draft an email that is warm, clear, fully conversational, and focuses on removing checkout anxiety or offering friendly support.",
        userPromptBuilder = { f1, f2, tone -> "Draft a cart abandonment email for \"$f1\" emphasizing reassurance and benefit details: \"$f2\". Direct in \"$tone\" tone." }
    ),
    WriterTemplate(
        id = "ecom_refund_apology",
        name = "Refund & Order Apology",
        category = "Ecommerce",
        description = "Formulate high-empathy customer recovery responses for severe delivery or quality delays.",
        icon = "🙏",
        field1Label = "The Order issue & Customer distress",
        field1Placeholder = "e.g., wrong premium package delivered, delayed 10 days for holiday birthday party",
        field2Label = "Detailed solution & extra gift code",
        field2Placeholder = "e.g., full refund of shipping fee plus a 30% off code HAPPYBIRTHDAY",
        systemPrompt = "Act as an expert customer retention specialist. Take complete ownership of the mistake, validate the user's frustration warmly, present the refund/gift solution clearly, and close with a reassuring commitment.",
        userPromptBuilder = { f1, f2, tone -> "Draft a customer support apology for \"$f1\" offering \"$f2\". Deliver with \"$tone\" empathy." }
    )
)

private fun getDevelopmentTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "dev_pas",
        name = "Problem Agitate Solution",
        category = "Development",
        description = "Craft marketing or documentation hooks using the classic PAS framework.",
        icon = "🛠️",
        field1Label = "Your Tech Product / Feature",
        field1Placeholder = "e.g., live server reload widget for mobile developers",
        field2Label = "The Developer Pain Point",
        field2Placeholder = "e.g., rebuilding the android app takes 3 minutes on every small margin change",
        systemPrompt = "Using the Problem-Agitate-Solution marketing copywriting framework, write a highly resonant, persuasive outline highlighting the core developer frustration, amplifying the struggle, and presenting your tool as the ultimate cure.",
        userPromptBuilder = { f1, f2, tone -> "Write a PAS copywriting piece for \"$f1\". Frustration focus: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "dev_pros_cons",
        name = "Pros & Cons",
        category = "Development",
        description = "Draft objective technical pros and cons comparisons for stack selections.",
        icon = "⚖️",
        field1Label = "Technology / Solution to Compare",
        field1Placeholder = "e.g., Jetpack Compose vs traditional XML Layouts",
        field2Label = "Core Evaluation Parameters",
        field2Placeholder = "e.g., performance, ease of styling, preview rendering, navigation",
        systemPrompt = "You are a professional software architect. Create a detailed, highly objective Pros and Cons comparison sheet in clean markdown tables. Maintain balanced developer logic.",
        userPromptBuilder = { f1, f2, tone -> "Compose Pros & Cons study for \"$f1\". Focus points: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "dev_meta_description",
        name = "Meta Description",
        category = "Development",
        description = "Write high-ranking SEO meta descriptions exactly mapped to character limits.",
        icon = "🎯",
        field1Label = "Webpage / Blog topic/ keywords",
        field1Placeholder = "e.g., complete tutorial on Kotlin State Flow",
        field2Label = "Target search intent",
        field2Placeholder = "e.g., developers wanting a fast, no-nonsense practical example first",
        systemPrompt = "Write 5 clickworthy meta descriptions under 160 characters. Each option must include the primary topic, be search-intent optimized, and end in a compelling CTA.",
        userPromptBuilder = { f1, f2, tone -> "Generate SEO meta descriptions for keyword \"$f1\". Target search intent: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "dev_faq_generator",
        name = "FAQ Generator (All Datas)",
        category = "Development",
        description = "Instantly draft highly structured Technical FAQ segments for documentation.",
        icon = "❓",
        field1Label = "Framework, Library or Tech Product",
        field1Placeholder = "e.g., Room database migrations",
        field2Label = "Commonly confusing user areas",
        field2Placeholder = "e.g., destructive migration, schema export logs, autoMigration",
        systemPrompt = "Compile a highly informative, detailed list of 5 Frequently Asked Questions with thorough, developer-grade answers, code snippets if applicable, and visual formatting.",
        userPromptBuilder = { f1, f2, tone -> "Formulate technical FAQ for \"$f1\". Topics: \"$f2\". Tone style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "dev_bug_fix",
        name = "Bug Fix",
        category = "Development",
        description = "Help brainstorm solutions, debug stacktraces, and write fixes.",
        icon = "🐛",
        field1Label = "Error stacktrace or Bug details",
        field1Placeholder = "e.g., NullPointerException inside adapter onBindViewHolder at line 43",
        field2Label = "Code snippet / Context",
        field2Placeholder = "Paste the relevant code or context here...",
        systemPrompt = "You are an elite debugging assistant. Diagnose the potential root causes of the error, propose clean code solutions, explain the fix logically, and suggest strategies to avoid this type of bug in the future.",
        userPromptBuilder = { f1, f2, tone -> "Formulate a bug fix explanation and solution for bug \"$f1\". Context snippet: \"$f2\". Explain with: \"$tone\" tone." }
    ),
    WriterTemplate(
        id = "dev_explain_code",
        name = "Explain Code",
        category = "Development",
        description = "Demystify complicated functions, algorithms, or snippets.",
        icon = "💡",
        field1Label = "Code snippet to understand",
        field1Placeholder = "Paste your complex function or snippet here...",
        field2Label = "Target comprehension level",
        field2Placeholder = "e.g., junior developer, mid-level, or high-level architect review",
        systemPrompt = "Break down the provided code block. Explain line-by-line or section-by-section what is happening conceptually. List potential performance implications or edge case concerns.",
        userPromptBuilder = { f1, f2, tone -> "Explain the following code: \"$f1\". Target level: \"$f2\". Style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "dev_teach_code",
        name = "Teach Code",
        category = "Development",
        description = "Construct comprehensive coding exercises, tutorials, or lessons.",
        icon = "🎓",
        field1Label = "Topic to teach",
        field1Placeholder = "e.g., Kotlin Coroutines block and Structured Concurrency",
        field2Label = "Prerequisites / Experience",
        field2Placeholder = "e.g., knows basic functions, completely new to asynchronous development",
        systemPrompt = "Act as a passionate Senior Software Engineer Instructor. Structure a highly educational tutorial with clean headings: 1. Core Analogy (simplified explanation), 2. Basic Code Example (commented), 3. Guided Walkthrough, and 4. Small Challenge Exercise.",
        userPromptBuilder = { f1, f2, tone -> "Draft a tutorial lesson to teach \"$f1\" to a student who \"$f2\". Instructor voice: \"$tone\"." }
    ),
    WriterTemplate(
        id = "dev_changelog_creator",
        name = "Changelog Creator",
        category = "Development",
        description = "Compile clean, professional changelogs from rough commit messages.",
        icon = "📝",
        field1Label = "Rough Commit lines / Features added",
        field1Placeholder = "e.g., fixed profile pic crash, added biometrics login, updated gradle libs.versions",
        field2Label = "Version Name & Date",
        field2Placeholder = "e.g., Version 2.4.0 (Beta) - June 2026",
        systemPrompt = "Categorize the rough changes beautifully into folders: 🚨 BREAKING CHANGES, 🚀 ADDED FEATURES, 🐛 BUG FIXES, and ⚙️ UNDER THE HOOD. Maintain clean markdown format.",
        userPromptBuilder = { f1, f2, tone -> "Draft a professional release changelog from commitments: \"$f1\". Metadata: \"$f2\". Write with a \"$tone\" style." }
    ),
    WriterTemplate(
        id = "dev_api_endpoint",
        name = "API Spec Designer",
        category = "Development",
        description = "Write clear, developer-friendly REST/GraphQL endpoint API documentation.",
        icon = "🔌",
        field1Label = "Endpoint URL, Method & Summary",
        field1Placeholder = "e.g., POST /api/v1/sessions/create (authentulates user checks and registers token)",
        field2Label = "Required Parameters List & Return Object",
        field2Placeholder = "e.g., input: email, password; returns: user object with session JWT",
        systemPrompt = "You are a professional Technical Writer. Generate a comprehensive API documentation section detailing URL routing, HTTP request type, headers, a structured markdown code block showing JSON request input, and a corresponding structured mock JSON response.",
        userPromptBuilder = { f1, f2, tone -> "Develop API endpoint specs for \"$f1\" input keys: \"$f2\". Style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "dev_commit_formatter",
        name = "Conventional Commit Formatter",
        category = "Development",
        description = "Format messy, short explanations to align strictly with standard Conventional Commits framework rules.",
        icon = "📦",
        field1Label = "Raw messy explanation of change",
        field1Placeholder = "e.g., fixed the database migration crash, added safe checks and logger",
        field2Label = "Commit Type & Scope (Optional)",
        field2Placeholder = "e.g., fix(database), or feat(auth)",
        systemPrompt = "Format the messy commit explanation perfectly to align with Conventional Commits specifications (e.g., feat(scope): message or fix(scope): message). Print the exact single commit line first, followed by a bulleted body explaining the engineering impact if appropriate.",
        userPromptBuilder = { f1, f2, tone -> "Format conventional commit for \"$f1\" with optional scope context \"$f2\". Tone style: \"$tone\"." }
    )
)

private fun getAdvertisementTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "ad_facebook_ads",
        name = "Facebook Ads",
        category = "Advertisement",
        description = "Generate clickable Facebook ad texts with hooks and body copy.",
        icon = "👥",
        field1Label = "Product, Offer, or Bundle",
        field1Placeholder = "e.g., 50% Off Annual Gym Membership",
        field2Label = "Target Group / Hook",
        field2Placeholder = "e.g., busy professionals wanting quick 15-minute workouts",
        systemPrompt = "Act as an elite conversion marketer. Deliver three distinctive Facebook ad copy versions: 1. Direct/Benefit focused, 2. Story / Relatable Pain, and 3. Urgency/Scarcity. Include Headlines, primary body copy, and CTA suggestions.",
        userPromptBuilder = { f1, f2, tone -> "Write Facebook Ad copy variations for \"$f1\". Target: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ad_google_headlines",
        name = "Google Ads Headlines",
        category = "Advertisement",
        description = "Formulate high-ranking search text ad headlines (exactly under 30 characters).",
        icon = "📰",
        field1Label = "SaaS or Business Type",
        field1Placeholder = "e.g., cloud cost optimization app",
        field2Label = "Primary Value keywords",
        field2Placeholder = "e.g., save 40%, multi-cloud, immediate trial, instant setup",
        systemPrompt = "You are a Google Search specialist. Provide 15 distinctive headlines. Ensure each headline is strictly 30 characters or fewer. Categorize by: Benefit-Driven, Action-Oriented, Trust-Building, and Direct.",
        userPromptBuilder = { f1, f2, tone -> "Generate 15 Google Ad Headlines for \"$f1\" prioritizing keywords \"$f2\". Brand tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ad_google_description",
        name = "Google Ads Description",
        category = "Advertisement",
        description = "Draft descriptive snippets under 90 characters for Google ads.",
        icon = "✍️",
        field1Label = "Product or Promotion Offer",
        field1Placeholder = "e.g., custom AI widgets",
        field2Label = "Supporting CTA details",
        field2Placeholder = "e.g., free demo, no credit card required",
        systemPrompt = "Provide 5 distinct Google ad descriptions that are strictly under 90 characters. Emphasize value metrics, feature proof points, and sharp CTAs.",
        userPromptBuilder = { f1, f2, tone -> "Write Google Ad descriptions for \"$f1\". Supporting points: \"$f2\". Tone: \"$tone\"." }
    )
)

private fun getSocialMediaTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "sm_yt_desc",
        name = "Youtube Video Description",
        category = "Social Media",
        description = "Formulate comprehensive structures for YouTube descriptions with timestamps and social links.",
        icon = "📼",
        field1Label = "Video Title & Core Concept",
        field1Placeholder = "e.g., Complete guide to building your first Android App in 2026",
        field2Label = "Outline / Main Sections",
        field2Placeholder = "e.g., 0:00 Intro, 1:30 setup steps, 5:00 code, 12:00 styling, 18:00 deployment",
        systemPrompt = "Draft a professional YouTube video description that includes a compelling 2-sentence intro, structured section breakdown with estimated timestamps, useful links placeholders, and essential video SEO keywords.",
        userPromptBuilder = { f1, f2, tone -> "Craft a YouTube video description for \"$f1\". Structuring outline: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_yt_title",
        name = "Youtube Video Title",
        category = "Social Media",
        description = "Generate 10 eye-catching YouTube video title ideas optimized to maximize CTA.",
        icon = "🏷️",
        field1Label = "What is the Video about?",
        field1Placeholder = "e.g., exploring an abandoned space observatory at night",
        field2Label = "Primary Keyword / Hook target",
        field2Placeholder = "e.g., space mystery, clickbait gravity, real discovery",
        systemPrompt = "Draft 10 highly clickworthy, searchable YouTube video titles prioritizing visual intrigue, keyword optimization, and audience curiosity.",
        userPromptBuilder = { f1, f2, tone -> "Draft YouTube titles for \"$f1\". Hook detail: \"$f2\". Vibe tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_yt_tag",
        name = "Youtube Video Tag",
        category = "Social Media",
        description = "Brainstorm high-volume tags and keywords for Youtube video indexing.",
        icon = "🏷️",
        field1Label = "Video Niche Details",
        field1Placeholder = "e.g., vintage camera repair guidelines",
        field2Label = "Target Keywords to mix",
        field2Placeholder = "e.g., film photography, Hasselblad, tutorial, 35mm",
        systemPrompt = "Generate 30 highly relevant, comma-separated search tags optimized for YouTube search suggestions and discovery algorithm indexing.",
        userPromptBuilder = { f1, f2, tone -> "Produce YouTube tag phrases for video niche \"$f1\" with keywords \"$f2\". Tone indicator: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_ig_captions_1",
        name = "Instagram Captions",
        category = "Social Media",
        description = "Write engaging Instagram post captions with emoji styling.",
        icon = "📷",
        field1Label = "Image / Post Concept",
        field1Placeholder = "e.g., reading a paperback book by a foggy lake cabin",
        field2Label = "Desired CTA / Engagement Hook",
        field2Placeholder = "e.g., ask followers what book they are currently reading",
        systemPrompt = "Write three modern, stylish Instagram captions filled with relevant emoticons, aesthetic line-breaks, and a friendly, engaging Call-To-Action.",
        userPromptBuilder = { f1, f2, tone -> "Write Instagram captions for \"$f1\". CTA request: \"$f2\". Brand tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_ig_hashtags",
        name = "Instagram Hashtags",
        category = "Social Media",
        description = "Recommend tags categorized by size to boost post visibility.",
        icon = "#️⃣",
        field1Label = "Post Theme / Category",
        field1Placeholder = "e.g., modular synthesizer setups, lo-fi beats",
        field2Label = "Aesthetic Vibe tags",
        field2Placeholder = "e.g., ambient soundscapes, cozy studio, dawless",
        systemPrompt = "Generate 25 strategic hashtags. Group them into directories: 5 High Volume (broad), 10 Medium Volume (targeted), and 10 Niche/Community tags.",
        userPromptBuilder = { f1, f2, tone -> "Compile Instagram hashtags for theme \"$f1\" and keywords \"$f2\". Style outline: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_tweet_1",
        name = "Social Media Post Tweet",
        category = "Social Media",
        description = "Draft concise and impactful single tweets strictly under 280 characters.",
        icon = "🐦",
        field1Label = "Core Idea or Announcement",
        field1Placeholder = "e.g., our simple habit tracker app just hit 50,000 installs overnight",
        field2Label = "Draft thoughts to pack",
        field2Placeholder = "e.g., deeply humbled, coding in pajamas, can't express gratitude enough",
        systemPrompt = "Draft three highly focused tweets strictly limited to 280 characters. Utilize clean spacing, smart visual bullet formatting, and high-conviction wording.",
        userPromptBuilder = { f1, f2, tone -> "Produce compact tweets for \"$f1\". Additional context: \"$f2\". Writing style tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_post_business",
        name = "Social Media Post Business",
        category = "Social Media",
        description = "Formulate highly professional corporate LinkedIn or Facebook announcements.",
        icon = "💼",
        field1Label = "Business Event / Launch",
        field1Placeholder = "e.g., partnering with GreenEnergy Corp to establish solar-grid chargers",
        field2Label = "Business Impact Highlight",
        field2Placeholder = "e.g., lowering regional carbon emissions by 15%, creating 20 local supply jobs",
        systemPrompt = "Draft an authoritative, highly professional corporate social media post. Highlight joint achievements, forward-looking strategic value, and clear, respectful business language.",
        userPromptBuilder = { f1, f2, tone -> "Draft a business announcement for \"$f1\". Impact metrics: \"$f2\". Brand tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_fb_headlines",
        name = "Facebook Headlines",
        category = "Social Media",
        description = "Catchy headlines for facebook posts and link sharing graphics.",
        icon = "🔊",
        field1Label = "Post/Offer Core Value",
        field1Placeholder = "e.g., free template illustrating passive income streams",
        field2Label = "Special Offer details",
        field2Placeholder = "e.g., download instantly, no email sign up required for first 100",
        systemPrompt = "Generate 10 attention-grabbing Facebook headlines. Use psychological hacks like curiosity, extreme value propositions, low friction, and quick gains.",
        userPromptBuilder = { f1, f2, tone -> "Produce FB headlines for \"$f1\". Target details: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_ig_reel_script",
        name = "IG Reel Script",
        category = "Social Media",
        description = "Write structured audio-visual scripts for 15-30 second reels.",
        icon = "🎞️",
        field1Label = "Reel Main Topic",
        field1Placeholder = "e.g., 3 keyboard shortcuts that will save you 1 hour a day",
        field2Label = "Interactive Visual setup",
        field2Placeholder = "e.g., showing a glowing typing mechanical keyboard, rapid hands close-up",
        systemPrompt = "Generate a double-column narrative script for a 30-second Instagram Reel. Include columns for Visual Action (what to reveal) and Voiceover Audio text (what to say). Start with a 3-second hook.",
        userPromptBuilder = { f1, f2, tone -> "Structure an IG Reel storyboard on topic \"$f1\". Visual cue focus: \"$f2\". Tone of script: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_ig_captions_2",
        name = "IG Captions",
        category = "Social Media",
        description = "Craft witty lifestyle and product Instagram description captions.",
        icon = "📱",
        field1Label = "Topic / Picture Mood",
        field1Placeholder = "e.g., morning routine showing espresso flowing",
        field2Label = "Core message to convey",
        field2Placeholder = "e.g., finding beauty in small slow mornings",
        systemPrompt = "Draft three creative options for short IG caption styles: 1. Aesthetic/Minimalist, 2. Story/Quote style, 3. Engaging/Interactive.",
        userPromptBuilder = { f1, f2, tone -> "Design IG captions for \"$f1\". Mood guidance: \"$f2\". Style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_tiktok_script",
        name = "TikTok Script",
        category = "Social Media",
        description = "Create engaging scripts designed to retain viewers beyond the first 3 seconds.",
        icon = "🎵",
        field1Label = "TikTok Topic / Challenge",
        field1Placeholder = "e.g., exposing fake 'financial gurus' trading strategies on apps",
        field2Label = "Visual / Trend Angle",
        field2Placeholder = "e.g., fast pacing, green screen background of generic stock graphs",
        systemPrompt = "Compile a video script tailored for TikTok's algorithm. Emphasize a rapid-fire opening hook, interactive visual gestures, and a lively, hyper-casual tone of voice.",
        userPromptBuilder = { f1, f2, tone -> "Construct a TikTok script about \"$f1\". Dynamic setup: \"$f2\". Vibe style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_tiktok_video_caption",
        name = "TikTok Video Caption",
        category = "Social Media",
        description = "Write short, high-curiosity description lines for TikTok video cards.",
        icon = "✏️",
        field1Label = "Video Title Storyline",
        field1Placeholder = "e.g., I tried sleeping in a sensory deprivation chamber for 72 hours",
        field2Label = "Viral spark details",
        field2Placeholder = "e.g., check comments for part 2, completely unexpected result at hour 50",
        systemPrompt = "Generate 5 ultra-short, curious TikTok video captions (under 150 characters) designed to encourage commenters, accompanied by popular tech/lifestyle hashtags.",
        userPromptBuilder = { f1, f2, tone -> "Draft TikTok video captions for \"$f1\". Engagement points: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_tiktok_video_script",
        name = "TikTok Video Script",
        category = "Social Media",
        description = "Alternative script drafting layout incorporating sound effects (SFX) cues.",
        icon = "🎬",
        field1Label = "The Core Hook & Plot",
        field1Placeholder = "e.g., why you are using your phone wrong at bed time",
        field2Label = "Key visual elements",
        field2Placeholder = "e.g., blue light filter screen, alarm ringing, insomnia stats",
        systemPrompt = "Write a complete video script for TikTok. Include explicit cues for: [SFX] audio prompts, [VISUAL] screen transitions, and [VO] voice actor delivery.",
        userPromptBuilder = { f1, f2, tone -> "Generate a detailed SFX TikTok storyboard for \"$f1\". Elements: \"$f2\". Deliver in \"$tone\" tone." }
    ),
    WriterTemplate(
        id = "sm_fb_post",
        name = "Facebook Post",
        category = "Social Media",
        description = "Draft long, readable, and highly conversational updates for Facebook pages.",
        icon = "📢",
        field1Label = "Post Story / Topic",
        field1Placeholder = "e.g., reflection on how quitting sugar for 30 days reshaped my gut health",
        field2Label = "Insight / Engagement Question",
        field2Placeholder = "e.g., invite people to share what habit they want to change next",
        systemPrompt = "Write a complete Facebook Post that is storytelling-driven, has clean paragraph breaks, feels personal, organic, and inviting, and wraps with a conversational question.",
        userPromptBuilder = { f1, f2, tone -> "Produce a personal Facebook post on \"$f1\". Engagement cue: \"$f2\". Voice style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_fb_video_script",
        name = "Facebook Video Script",
        category = "Social Media",
        description = "Draw detailed scripts for Facebook Watch videos targeting longer watch times.",
        icon = "📹",
        field1Label = "Video Theme / Guide",
        field1Placeholder = "e.g., Step-by-step restoration of an old rusty kitchen cast iron",
        field2Label = "Main stages of video",
        field2Placeholder = "e.g., rust cleaning, vinegar soak, oil seasoning, final bacon test",
        systemPrompt = "Create a detailed script for a 3-5 minute Facebook video. Map out a slow-building narrative, educational commentary, and clear, informative verbal callouts.",
        userPromptBuilder = { f1, f2, tone -> "Generate FB video narrative storyboard for \"$f1\" outlining \"$f2\". Tone of voice: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_x_tweet",
        name = "X Tweet",
        category = "Social Media",
        description = "Write a single highly polished, shareable statement for X / Twitter.",
        icon = "🌐",
        field1Label = "Topic / Realtime thought",
        field1Placeholder = "e.g., standard UI designs are all starting to look exactly the same",
        field2Label = "Target angle or punchline",
        field2Placeholder = "e.g., we've traded human soul and character for boring corporate grey templates",
        systemPrompt = "Produce three options for a single X post, keeping them punchy, thought-provoking, and strictly within the 280-character limit.",
        userPromptBuilder = { f1, f2, tone -> "Draft a tweet about \"$f1\". Angle check: \"$f2\". Write in \"$tone\" style." }
    ),
    WriterTemplate(
        id = "sm_x_thread",
        name = "X Thread",
        category = "Social Media",
        description = "Formulate cohesive, multi-post educational story threads for X (Twitter).",
        icon = "🧵",
        field1Label = "Thread Focus & Goal",
        field1Placeholder = "e.g., how I built a $10k/month passive niche software product in 4 months",
        field2Label = "Key steps to lay out",
        field2Placeholder = "e.g., finding the niche, building MVP in a week, validation, pricing models",
        systemPrompt = "Act as an expert viral marketer on X. Write a highly compelling 5-to-7 post thread. Post 1 must have a massive curiosity hook. Posts 2-6 must give actionable, highly readable value metrics. Post 7 wraps with a strong concluding quote.",
        userPromptBuilder = { f1, f2, tone -> "Develop a complete X Thread on \"$f1\" outlining steps: \"$f2\". Writing voice: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_yt_to_blog",
        name = "Youtube Video to Blog Post",
        category = "Social Media",
        description = "Repurpose a transcripts list or video premise into a complete, structured blog.",
        icon = "🔄",
        field1Label = "Video Title & Core transcripts notes",
        field1Placeholder = "e.g., '10 tools I use to run my clean energy farm'",
        field2Label = "Important segments or tool names",
        field2Placeholder = "e.g., solar fence, water pump, IoT soil monitor, off-grid converter",
        systemPrompt = "Repurpose the video content into a beautifully structured, long-form, SEO-friendly blog post. Write with a professional, rich web layout including introduction, item subheaders, and summaries.",
        userPromptBuilder = { f1, f2, tone -> "Convert the YouTube content \"$f1\" featuring \"$f2\" into a detailed blog post. Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_video_idea",
        name = "Video Idea",
        category = "Social Media",
        description = "Brainstorm viral and highly engaging video concepts for short-form or long-form platforms.",
        icon = "🧠",
        field1Label = "Content Niche or Brand",
        field1Placeholder = "e.g., retro gaming hardware restoration",
        field2Label = "Estimated Audience Vibe",
        field2Placeholder = "e.g., nostalgia lovers, tech hardware enthusiasts",
        systemPrompt = "Provide 5 highly creative and unique video flow ideas with titles, estimated length, hook strategies, and a brief description of what makes them viral suggestions.",
        userPromptBuilder = { f1, f2, tone -> "Generate creative video ideas for \"$f1\". Target audience: \"$f2\". Brand tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_video_description",
        name = "Video Description",
        category = "Social Media",
        description = "Write short, clickable social captions describing a video upload.",
        icon = "📝",
        field1Label = "Video Topic details",
        field1Placeholder = "e.g., restoring a 1982 mechanical alarm clock",
        field2Label = "Links / Credits to list",
        field2Placeholder = "e.g., tools used, background music track, subscribe label",
        systemPrompt = "Combine a visual summary of the video with essential calls to action, credits, and interactive hashtags into a clean copy block.",
        userPromptBuilder = { f1, f2, tone -> "Write quick video summary descriptions for \"$f1\". Credits section: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_viral_tweet_idea",
        name = "Viral Tweet Idea",
        category = "Social Media",
        description = "Draft thought-provoking statements designed to spark retweets and alignment.",
        icon = "💫",
        field1Label = "Industry or Broad Topic",
        field1Placeholder = "e.g., venture capital, startup fundraising",
        field2Label = "Contrarian angle or key view",
        field2Placeholder = "e.g., raising money is a sign of product market failure, not success",
        systemPrompt = "Produce 3 highly provocative, beautifully formatted, contrarian statements tailored for maximum engagement and shares.",
        userPromptBuilder = { f1, f2, tone -> "Draft contrarian tweet ideas for \"$f1\". Angle: \"$f2\". Tone level: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_clickbait_titles",
        name = "Clickbait Titles",
        category = "Social Media",
        description = "Create attention-grabbing title variations based on natural human curiosity triggers.",
        icon = "🎣",
        field1Label = "Video or Post Subject",
        field1Placeholder = "e.g., testing cheap sunglasses in a heavy desert storm",
        field2Label = "Surprising factor / Secret",
        field2Placeholder = "e.g., the $5 glasses actually outlasted the $300 designer glasses",
        systemPrompt = "Generate 10 highly engaging title ideas optimized for the CTR algorithm. Use curiosity loops, shocking revelations, and numbers.",
        userPromptBuilder = { f1, f2, tone -> "Write attention-grabbing clickbait title listings for \"$f1\" emphasizing \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_app_notifications",
        name = "App and SMS Notifications",
        category = "Social Media",
        description = "Draft compact, actionable push notifications and SMS trigger campaigns.",
        icon = "💬",
        field1Label = "App Category / Event",
        field1Placeholder = "e.g., meditation app, reminding user to do an evening deep-breathing session",
        field2Label = "Offer / Action Incentive",
        field2Placeholder = "e.g., unlock a special 5-minute calm track today, keep streak alive",
        systemPrompt = "Generate 5 distinct push notification variations (title + brief body under 120 characters) and 3 short SMS drafts centered on driving user retention and opens.",
        userPromptBuilder = { f1, f2, tone -> "Draft interactive mobile push and SMS trigger copies for \"$f1\". CTA details: \"$f2\". Copy tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "sm_tiktok_reels_hooks",
        name = "TikTok & Reels Visual Hooks",
        category = "Social Media",
        description = "Generate 10 immediate, attention-grabbing opening visual patterns and verbal hooks.",
        icon = "🎬",
        field1Label = "Core Topic or Product focus",
        field1Placeholder = "e.g., how to study 4 hours with absolute laser-like focus",
        field2Label = "Audience trigger details",
        field2Placeholder = "e.g., college students feeling overwhelmed by exams, wanting simple cheats",
        systemPrompt = "You are a viral social media growth manager. Formulate 10 distinct short-form video hooks. Every hook must provide: 1. A bold, high-curiosity verbal statement (0-3s), 2. An immediate visual action prompt (what to show on screen), and 3. An associated emotional trigger explanation.",
        userPromptBuilder = { f1, f2, tone -> "Generate viral video hooks for \"$f1\" targeting \"$f2\". Direct in \"$tone\" tone." }
    ),
    WriterTemplate(
        id = "sm_linkedin_hook_builder",
        name = "Viral LinkedIn Storyteller Hook",
        category = "Social Media",
        description = "Formulate high-impact storytelling hooks designed to maximize the 'see more' click trigger rate.",
        icon = "📈",
        field1Label = "The professional story / lesson",
        field1Placeholder = "e.g., how leaving a secure director job to launch a SaaS taught me true resilience",
        field2Label = "Major emotional transition",
        field2Placeholder = "e.g., going from a clean corporate salary to $0 for 8 weeks and facing self-doubt",
        systemPrompt = "You are an elite executive brand strategist. Draft 3 compelling, double-spaced opening lines customized for the LinkedIn post feed format in order to optimize for high-curiosity retention before readers hit 'see more'.",
        userPromptBuilder = { f1, f2, tone -> "Formulate viral LinkedIn hooks for \"$f1\" detailing emotional steps: \"$f2\". Under tone style: \"$tone\"." }
    )
)

private fun getEmailTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "email_gen_creator",
        name = "Email Generator",
        category = "Email",
        description = "Quickly frame and draft highly professional emails on any core subject.",
        icon = "📧",
        field1Label = "Main Purpose of Email",
        field1Placeholder = "e.g., request budget approval for team productivity tools from CFO",
        field2Label = "Core Facts to lay out",
        field2Placeholder = "e.g., team saves 12 hours weekly, tools cost $150/month total",
        systemPrompt = "Compile a crisp, corporate-grade professional email draft complete with a click-worthy Subject Line, clear greetings, bulleted arguments, and a respectful closing.",
        userPromptBuilder = { f1, f2, tone -> "Write an email seeking \"$f1\". Supportive metrics: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "email_answer_generator",
        name = "Email Answer Generator",
        category = "Email",
        description = "Draft quick, polite email responses to incoming messages.",
        icon = "✉️",
        field1Label = "Incoming Email Text",
        field1Placeholder = "e.g., client asking if we can deliver the finalized graphics design dashboard by Friday noon",
        field2Label = "Your Answer / Decision",
        field2Placeholder = "e.g., yes but only if they approve the draft style guide by Wednesday morning",
        systemPrompt = "Act as an elite personal assistant. Draft a polished, respectful, and crystal-clear response email that establishes helpful boundaries while remaining fully professional.",
        userPromptBuilder = { f1, f2, tone -> "Draft a response to: \"$f1\". Core response points: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "email_news_gen",
        name = "Newsletter Generator",
        category = "Email",
        description = "Structure informative updates, blog links, and tips for your audience subscribers.",
        icon = "📰",
        field1Label = "Newsletter Topic & Theme",
        field1Placeholder = "e.g., the importance of morning stretching for desk workers",
        field2Label = "Bullet key tips or links to share",
        field2Placeholder = "e.g., 3-minute back stretch, why sitting causes hip tightness, upcoming community challenge link",
        systemPrompt = "Write a complete email newsletter draft. Structure it with an encouraging editorial intro, three readable bulleted points, a dedicated subscriber tip box, and a friendly CTA.",
        userPromptBuilder = { f1, f2, tone -> "Write a structured newsletter email about \"$f1\". Tips list: \"$f2\". Tone voice: \"$tone\"." }
    ),
    WriterTemplate(
        id = "email_welcome_email",
        name = "Welcome Email",
        category = "Email",
        description = "Draft warm welcome emails introducing new users to features and premium perks.",
        icon = "👋",
        field1Label = "App, Service, or Platform",
        field1Placeholder = "e.g., PeakFocus - premium task planner application",
        field2Label = "First Action Steps to advise",
        field2Placeholder = "e.g., complete your profile, add your first three goals, get a free 15-minute quick guide PDF",
        systemPrompt = "Write a beautiful, welcoming onboarding email. It must sound enthusiastic and warm, list logical, simple next steps to set up, and reassure the user of customer support.",
        userPromptBuilder = { f1, f2, tone -> "Write a welcome email for new users of \"$f1\". Immediate steps list: \"$f2\". Email tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "email_invitation_mail",
        name = "Invitation Mail",
        category = "Email",
        description = "Construct clear invitation letters for webinars, workshops, or virtual events.",
        icon = "📅",
        field1Label = "Event Name & Topic",
        field1Placeholder = "e.g., live coaching workshop on mastering minimalist lifestyle design",
        field2Label = "Date, Time & Sign Up key link",
        field2Placeholder = "e.g., June 15, 6 PM UTC, tiny url reservation required",
        systemPrompt = "Draft a persuasive, inviting email. Include sections explaining: 1. Why you cannot afford to miss this (main pain points resolved), 2. Event logistics details, and 3. Secure your spot reservation link CTA.",
        userPromptBuilder = { f1, f2, tone -> "Compose an event invitation email for \"$f1\". Logistics details: \"$f2\". Vibe style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "email_followup_email",
        name = "Follow up Email",
        category = "Email",
        description = "Write polite, persuasive follow-ups to secure replies after no-reponse pitches.",
        icon = "🕒",
        field1Label = "Original Topic / Pitch context",
        field1Placeholder = "e.g., custom mobile app development proposal from last week",
        field2Label = "Incentive / Additional Value point",
        field2Placeholder = "e.g., we just released a free prototypes mock showing their core logo design in action",
        systemPrompt = "Craft a highly polite, zero-pressure email follow-up. Keep it short (under 120 words), restate the core benefit, offer the new incentive, and present a simple question prompt.",
        userPromptBuilder = { f1, f2, tone -> "Generate follow-up outreach email about \"$f1\". New angle points: \"$f2\". Copy style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "email_reply_email",
        name = "Reply Email",
        category = "Email",
        description = "Formulate professional templates for daily inbox response operations.",
        icon = "🔄",
        field1Label = "The Core Message standard",
        field1Placeholder = "e.g., thanking a customer for their kind feedback and confirming it is added to the roadmap",
        field2Label = "Any timeline or status notes",
        field2Placeholder = "e.g., scheduled for release around Q3 2026",
        systemPrompt = "Produce a highly professional, polite, and reassuring template response to save time during high volume customer interaction.",
        userPromptBuilder = { f1, f2, tone -> "Draft a reply email for \"$f1\". Scheduling context notes: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "email_newsletter",
        name = "Newsletter",
        category = "Email",
        description = "Short copy for quick regular editorial email letters.",
        icon = "💌",
        field1Label = "Current Story / Lesson",
        field1Placeholder = "e.g., how doing a digital pack clean-up saved my focus",
        field2Label = "Product Recommendation connection",
        field2Placeholder = "e.g., our simple folder organization template (20% discount)",
        systemPrompt = "Write an organic, conversational story-driven newsletter email draft ending with a seamless pitch for the digital goods.",
        userPromptBuilder = { f1, f2, tone -> "Formulate story-to-sales email newsletter about \"$f1\". Recommendation points: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "email_press_release",
        name = "Press Release",
        category = "Email",
        description = "Compile news-style formal press releases for media distribution.",
        icon = "📰",
        field1Label = "Company Launch / Milestone",
        field1Placeholder = "e.g., solar propulsion drone reaches record altitude",
        field2Label = "Founder quote or industry implication",
        field2Placeholder = "e.g., breakthrough proves atmospheric solar capture works, says CEO Clara",
        systemPrompt = "Generate a formal, high-conviction Press Release. Use the standard AP Style outline structure: Location-date tag, powerful lead hook, detailed background, founder quote block, and boilerplate template placeholder.",
        userPromptBuilder = { f1, f2, tone -> "Write a formal media press release for \"$f1\". Highlights: \"$f2\". Writing tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "email_cold_email",
        name = "Cold Email",
        category = "Email",
        description = "Draft customized B2B outward sales search emails with high relevance.",
        icon = "❄️",
        field1Label = "Your Offer / Value prop",
        field1Placeholder = "e.g., custom AI widgets that save teams 4 hours/day during onboarding",
        field2Label = "Target audience / Role",
        field2Placeholder = "e.g., CTOs of mid-size logistics software companies",
        systemPrompt = "Produce a master outbound cold email. Include a highly relevant, low-friction subject line. Keep the body short (under 120 words), focus strictly on their possible core problem, and propose a low-friction 5-minute call.",
        userPromptBuilder = { f1, f2, tone -> "Draft cold sales email draft offering \"$f1\" to target role \"$f2\". Tone: \"$tone\"." }
    )
)

private fun getAcademicTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "acad_cover",
        name = "Cover Letter",
        category = "Academic",
        description = "Write a compelling, professional cover letter for academic programs, scholarships, or internships.",
        icon = "📂",
        field1Label = "Program & University Name",
        field1Placeholder = "e.g., Master of Science in Quantum Computing at ETH Zurich",
        field2Label = "Your Background & Achievement highlights",
        field2Placeholder = "e.g., BS in Physics, published thesis on qubit noise reduction, 3.9 GPA",
        systemPrompt = "You are an expert academic advisor. Draft a professional, highly motivated Cover Letter showing research alignment, personal academic passion, and long-term research commitment.",
        userPromptBuilder = { f1, f2, tone -> "Generate an academic Cover Letter for \"$f1\" emphasizing \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "acad_math_question",
        name = "Math Question",
        category = "Academic",
        description = "Formulate thorough step-by-step educational explanations for math or logic problems.",
        icon = "🔢",
        field1Label = "The Equation / Logic Problem",
        field1Placeholder = "e.g., find the derivative of f(x) = x^2 * ln(x)",
        field2Label = "Level / Focus rules",
        field2Placeholder = "e.g., college calculus, explain step-by-step using product rule",
        systemPrompt = "Formulate a crystal-clear, pedagogical mathematical walkthrough. Use clean markdown formatting to list: 1. Core formula details, 2. Step-by-Step calculus derivation with intermediate lines, and 3. Final simplified result.",
        userPromptBuilder = { f1, f2, tone -> "Solve and explain the math problem \"$f1\". Instructional rules: \"$f2\". Deliver with \"$tone\" tone." }
    ),
    WriterTemplate(
        id = "acad_bullet_point_answer",
        name = "Bullet Point Answer",
        category = "Academic",
        description = "Draft organized, structured, and easy-to-read bulleted summaries to academic exam questions.",
        icon = "🎯",
        field1Label = "Exam Question / Concept to discuss",
        field1Placeholder = "e.g., how did the printing press accelerate the Protestant Reformation?",
        field2Label = "Key constraints / facts",
        field2Placeholder = "e.g., mention vernacular translation, mass distribution, literacy rates",
        systemPrompt = "Rewrite the answer to the question using clean, logically progressive, academically rigorous bullet points with key terms highlighted in bold.",
        userPromptBuilder = { f1, f2, tone -> "Create bullet point answers for \"$f1\" utilizing facts \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "acad_essay",
        name = "Academic Essay",
        category = "Academic",
        description = "Formulate a well-outlined essay draft complete with thesis, body arguments, and reviews.",
        icon = "📖",
        field1Label = "Essay Core Question / Thesis",
        field1Placeholder = "e.g., The ethical implications of editing human embryos using CRISPR",
        field2Label = "Main body points / Focus theories",
        field2Placeholder = "e.g., genetic inequality, therapeutic correction, potential unintended mutations",
        systemPrompt = "Produce a highly detailed, academically structured essay draft. Include: 1. Introduction with a strong thesis statement, 2. 3 Supporting analysis segments with logical arguments, 3. Counter-arguments reflection, and 4. Analytical summary.",
        userPromptBuilder = { f1, f2, tone -> "Develop a detailed academic essay on topic \"$f1\". Body points: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "acad_thesis_statement",
        name = "Thesis Statement",
        category = "Academic",
        description = "Construct strong, high-conviction thesis statements based on raw topic premises.",
        icon = "🎓",
        field1Label = "Research Topic & Context",
        field1Placeholder = "e.g., impact of microplastics on sea turtle navigation",
        field2Label = "Your core stance / discovery focus",
        field2Placeholder = "e.g., chemical toxins disrupt magnetic orientation systems, leading to nesting failure",
        systemPrompt = "Draft three highly focused, clear, academically rigorous, and specific Thesis Statement proposals. Each alternative must state a clear premise, stance, and evidence outline.",
        userPromptBuilder = { f1, f2, tone -> "Formulate thesis statement ideas for topic \"$f1\" focusing on \"$f2\". Academic style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "acad_proofreading",
        name = "Proofreading",
        category = "Academic",
        description = "Elevate academic tone, check formal terminology, and remove colloquial phrasings.",
        icon = "✏️",
        field1Label = "Your Draft Passage",
        field1Placeholder = "Paste your research text fragment here...",
        field2Label = "Style goals / Terminology restrictions",
        field2Placeholder = "e.g., make it sound highly formal, reference quantitative values",
        systemPrompt = "Examine the text to improve vocabulary, sentence structure, flow, and academic clarity. Provide the polished version and a short bulleted list of styling improvements.",
        userPromptBuilder = { f1, f2, tone -> "Proofread and polish this academic passage: \"$f1\". Goals: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "acad_rephrase",
        name = "Rephrase",
        category = "Academic",
        description = "Restructure complicated sentences to read with crisp, simple-to-digest clarity.",
        icon = "🔄",
        field1Label = "Original complex text block",
        field1Placeholder = "Paste the sentence here...",
        field2Label = "Target tone / Vibe",
        field2Placeholder = "e.g., active voice, direct and clear, keeping academic integrity",
        systemPrompt = "Rewrite the given passage to enhance comprehension, utilizing active voice, modern phrasing, and eliminating repetitive jargon.",
        userPromptBuilder = { f1, f2, tone -> "Rephrase this passage: \"$f1\". Target style rules: \"$f2\". Deliver in \"$tone\"." }
    ),
    WriterTemplate(
        id = "acad_improve",
        name = "Improve",
        category = "Academic",
        description = "Strengthen weak arguments and integrate high-quality transitional vocabulary.",
        icon = "🚀",
        field1Label = "Draft segment / Argument explanation",
        field1Placeholder = "e.g., we think climate change makes storms bad because heat expands things",
        field2Label = "Focus area / Terminology",
        field2Placeholder = "e.g., thermal dynamics, sea surface temperature, barometric pressure",
        systemPrompt = "Refine and upgrade the scientific or argumentation level of the text. Integrate sophisticated transit phrases, precise lexicon terms, and logically sound assertions.",
        userPromptBuilder = { f1, f2, tone -> "Improve the core argumentation of \"$f1\". Integrate parameters: \"$f2\". Tone style: \"$tone\"." }
    )
).map { it.copy(category = "Academic") }

private fun getBusinessTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "biz_job_desc",
        name = "Job Description",
        category = "Business",
        description = "Generate clear, professional, and exciting job listings to find the perfect candidate.",
        icon = "💼",
        field1Label = "Role Title & Business",
        field1Placeholder = "e.g., Senior Android developer at Apex FinTech Solutions",
        field2Label = "Core requirements & Perks",
        field2Placeholder = "e.g., 5y Kotlin, Compose expertise, remote friendly, health insurance, free equipment",
        systemPrompt = "Draft a structured, motivating job listing. Include sections for: Role Overview, Core Responsibilities, Skills Checklist, and What We Offer benefits.",
        userPromptBuilder = { f1, f2, tone -> "Write a job description for \"$f1\". Requirements and perks: \"$f2\". Office tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "biz_app_letter",
        name = "Application Letter",
        category = "Business",
        description = "Construct outstanding job application and cover letters.",
        icon = "✉️",
        field1Label = "Target Job Role & Company",
        field1Placeholder = "e.g., Product designer at Stripe",
        field2Label = "Your major skillsets & value",
        field2Placeholder = "e.g., 4 years building fintech apps, expert in user flows, designed tools used by 1M users",
        systemPrompt = "Generate an outstanding Job Application Cover Letter. Ensure the message starts with an attention-grabbing hook explaining why you are the ideal fit, followed by proven achievements, and ends with a polite request for an interview.",
        userPromptBuilder = { f1, f2, tone -> "Draft a job application letter for \"$f1\" showcasing: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "biz_resume_builder",
        name = "Resume Builder",
        category = "Business",
        description = "Organize rough work experience bullets into professional corporate formatting.",
        icon = "📑",
        field1Label = "Role, Company & Dates",
        field1Placeholder = "e.g., Software engineer at TechCorp (2022-2025)",
        field2Label = "Daily tasks / Achievements list",
        field2Placeholder = "e.g., fixed database speed, supervised 3 juniors, designed payment flow, saved cost",
        systemPrompt = "Rewrite career history bullets into high-performance, action-oriented resume accomplishments. Use strong action verbs and structure with metric focus.",
        userPromptBuilder = { f1, f2, tone -> "Revamp resume highlights for \"$f1\". Highlights list: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "biz_linkedin_profile",
        name = "Linkedin",
        category = "Business",
        description = "Formulate professional summaries or bios to help your LinkedIn stand out.",
        icon = "🌐",
        field1Label = "Your Professional Experience details",
        field1Placeholder = "e.g., Product manager building cloud infrastructure platforms and mentoring devs",
        field2Label = "Key value hook or mission statement",
        field2Placeholder = "e.g., making complex server integrations feel like child's play",
        systemPrompt = "Draft an engaging LinkedIn 'About' summary statement. Use professional, storytelling layout, list core expertise highlights clearly, and end with a modern call-to-action to connect.",
        userPromptBuilder = { f1, f2, tone -> "Generate LinkedIn summary for \"$f1\" emphasizing focus \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "biz_startup_ideas",
        name = "Startup Ideas (w/ quantities)",
        category = "Business",
        description = "Brainstorm 5 detailed, unique startup concepts including exact cost/market quantifications.",
        icon = "🚀",
        field1Label = "Broad Industry of Interest",
        field1Placeholder = "e.g., smart agriculture, ocean clean-tech",
        field2Label = "Core Technology focus",
        field2Placeholder = "e.g., satellite imagery, autonomous ocean gliders, IoT water sensors",
        systemPrompt = "Act as an experienced venture builder. Provide 5 detailed startup idea summaries. For each idea, include: 1. Core Niche Mission, 2. Target Audience, 3. Estimated initial cost, and 4. Market Size projection.",
        userPromptBuilder = { f1, f2, tone -> "Brainstorm 5 quantified startup ideas for industry \"$f1\" utilizing tech \"$f2\". Presentation style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "biz_pas",
        name = "PAS",
        category = "Business",
        description = "Alternative PAS marketing copywriting layout for sales promotions.",
        icon = "📈",
        field1Label = "Core Product Offer",
        field1Placeholder = "e.g., virtual assistant agency for dental clinics",
        field2Label = "Customer critical frustration",
        field2Placeholder = "e.g., dentists wasting 15 hours a week handling phone calls and scheduling cancellations",
        systemPrompt = "Act as a sales copywriter. Produce a high-converting PAS (Problem, Agitate, Solution) copywriting draft that triggers action.",
        userPromptBuilder = { f1, f2, tone -> "Generate classic business PAS copy for \"$f1\" with frustration highlight \"$f2\". Style/Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "biz_strategy",
        name = "Strategy",
        category = "Business",
        description = "Outline initial, detailed market launch strategies for new businesses.",
        icon = "🎯",
        field1Label = "New Product / Business Core Focus",
        field1Placeholder = "e.g., organic cold-press juices delivered in glass bottles to offices",
        field2Label = "Target Area & Competition",
        field2Placeholder = "e.g., city financial district, competing with cheap grocery sodas and fancy cafes",
        systemPrompt = "Act as a premium strategy consultant. Formulate a 4-pillar Go-To-Market strategy outline: 1. Ideal Customer Avatar, 2. Direct Distribution Channels, 3. Dynamic Pricing Model, and 4. Brand Communication Strategy.",
        userPromptBuilder = { f1, f2, tone -> "Develop GTM strategy for \"$f1\". Market context: \"$f2\". Strategy tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "biz_personal_bio",
        name = "Personal Bio",
        category = "Business",
        description = "Draft short, premium executive bios for speaking gigs or website cards.",
        icon = "👤",
        field1Label = "Career Story & Achievement",
        field1Placeholder = "e.g., CEO, founded 2 clean-energy companies, studied at Stanford, former engineer",
        field2Label = "Intended use case",
        field2Placeholder = "e.g., speaker bio at a green tech summit, needs to feel highly credible and visionary",
        systemPrompt = "Write three different personal biography layouts: 1. Professional/Executive (3rd person), 2. Conversational/Story style, 3. Compact/One-line.",
        userPromptBuilder = { f1, f2, tone -> "Craft a personal bio based on \"$f1\" for \"$f2\". Deliver style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "biz_career_advice",
        name = "Career Advice",
        category = "Business",
        description = "Brainstorm actionable answers to career transitions, promotions, or reviews.",
        icon = "🧭",
        field1Label = "Career Dilemma / Challenge",
        field1Placeholder = "e.g., transition from technical engineer to engineering manager role",
        field2Label = "Your main worries / status",
        field2Placeholder = "e.g., fear of losing coding skills, dreading difficult human management conversations",
        systemPrompt = "Act as an elite executive leadership coach. Provide a warm, analytical reflection outlining actionable career strategies, mindset adjustments, and essential steps to succeed.",
        userPromptBuilder = { f1, f2, tone -> "Synthesize professional career strategy suggestions for client facing \"$f1\". Context: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "biz_cost_benefit",
        name = "Cost Benefit Analysis",
        category = "Business",
        description = "Develop crisp, structured comparative reviews of team investments or purchases.",
        icon = "📊",
        field1Label = "Investment / Decision Project",
        field1Placeholder = "e.g., migrating complete customer database from self-hosted local postgres to managed AWS Aurora",
        field2Label = "Expected Expenses & Value variables",
        field2Placeholder = "e.g., AWS costs $300/mo, migration team takes 2 weeks, saves 10 engineer-hours of database backup work weekly",
        systemPrompt = "Draft a neat, thorough, business-grade Cost Benefit Analysis. Include sections for: Direct Expenses, Indirect ROI, Operational Risk factors, and a final Recommendation with payback time estimate.",
        userPromptBuilder = { f1, f2, tone -> "Draft a Cost-Benefit Analysis for project \"$f1\" with attributes \"$f2\". Review style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "biz_brainstorm",
        name = "Brainstorming",
        category = "Business",
        description = "Unlock creative marketing, feature, or service ideas for any operational focus.",
        icon = "💡",
        field1Label = "Business Core Concern",
        field1Placeholder = "e.g., increasing subscription sign-ups for a premium sleep tracking application",
        field2Label = "Market constraints / focus",
        field2Placeholder = "e.g., no paid ad budget, leveraging viral growth loops, targeting busy remote developers",
        systemPrompt = "Generate 10 highly creative, actionable, and innovative brainstorming strategies mapped to the given guidelines. Group by: Low-hanging fruit, Bold bets, and Viral potential.",
        userPromptBuilder = { f1, f2, tone -> "Generate brainstorming ideas for problem \"$f1\" prioritizing metrics \"$f2\". Creative tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "biz_linkedin_post",
        name = "Linkedin Post",
        category = "Business",
        description = "Flesh out viral, professional networking stories designed for high engagement.",
        icon = "📣",
        field1Label = "Industry Story / Moral lesson",
        field1Placeholder = "e.g., how rejecting a client who was rude to our support rep strengthened my team's spirit",
        field2Label = "Target Takeaway / Vibe",
        field2Placeholder = "e.g., respect beats revenue, culture is everything",
        systemPrompt = "Write a high-engagement LinkedIn post. Start with an intriguing hook, break down the narrative with single readable sentence spacing, list three major takeaways, and ask a relevant question at the end.",
        userPromptBuilder = { f1, f2, tone -> "Compose a LinkedIn networking post about \"$f1\". Takeaway: \"$f2\". Vibe style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "biz_sales_pitch",
        name = "Sales Pitch",
        category = "Business",
        description = "Craft high-conviction email or verbal pitches designed to book discovery demonstrations.",
        icon = "🎯",
        field1Label = "Your Service & Ultimate value",
        field1Placeholder = "e.g., custom solar roof installs for manufacturing warehouses that pay for themselves in 36 months",
        field2Label = "Buyer objections factor",
        field2Placeholder = "e.g., high initial investment, busy schedule to plan",
        systemPrompt = "Draft a compelling product/service pitch. Include an immediate visual benefit angle, handle the core objection up front, present concrete savings figures, and propose a low-friction Next Step call-to-action.",
        userPromptBuilder = { f1, f2, tone -> "Draft high-converting sales copywriting pitch for \"$f1\". Objection angle: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "bus_yc_pitch",
        name = "Y-Combinator Elevator Pitch",
        category = "Business",
        description = "Craft an ultra-sharp, 1-sentence product pitch structured exactly like the elite YC startup formula.",
        icon = "🚀",
        field1Label = "What is the product and target client?",
        field1Placeholder = "e.g., a lightweight online form builder tailored for high-volume recruitment teams",
        field2Label = "The core differentiator / 10x benefit",
        field2Placeholder = "e.g., eliminates manual input fields using AI resume uploads, rendering 5x faster screens",
        systemPrompt = "Draft 5 options of ultra-concise, Y-Combinator style elevator pitches following the exact mathematical formula: 'We do X for Y by Z.' Ensure it contains zero marketing fluff, is highly descriptive, and sounds incredibly serious and investable.",
        userPromptBuilder = { f1, f2, tone -> "Draft YC-style elevator pitches for \"$f1\" emphasizing competitive differentiator details: \"$f2\". Deliver in \"$tone\" style." }
    ),
    WriterTemplate(
        id = "bus_competitor_map",
        name = "Competitive Value Map",
        category = "Business",
        description = "Assemble clear, benefit-driven positioning comparison outlines contrasting you with legacy rivals.",
        icon = "🗺️",
        field1Label = "Your Product and Direct Rivals",
        field1Placeholder = "e.g., SolarGlide Drone Delivery vs ground cargo logistics providers",
        field2Label = "Specific battlegrounds/metrics to win",
        field2Placeholder = "e.g., time to deliver (20 mins vs 4 hours), cost per dispatch ($3 vs $15), carbon index",
        systemPrompt = "Draft a detailed competitive advantage assessment. Build a clean markdown table structured as: [Feature / Metric], [Your Product], [Typical Legacy Rivals], followed by a 2-sentence positioning script that your sales team can immediately voice during demonstrations.",
        userPromptBuilder = { f1, f2, tone -> "Synthesize a competitive alignment map for \"$f1\" against direct metrics: \"$f2\". Positioning style: \"$tone\"." }
    )
).map { it.copy(category = "Business") }

private fun getCustomerServiceTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "cs_social_reply",
        name = "Social Media Reply",
        category = "Customer Service",
        description = "Draft professional, polite customer service reactions to social media reviews.",
        icon = "💬",
        field1Label = "Complain or Feedback context",
        field1Placeholder = "e.g., 'your mobile app keeps loading indefinitely on my device since last update'",
        field2Label = "Resolution guidelines",
        field2Placeholder = "e.g., tell them we just pushed a hotfix update v1.0.8, advise to reset cache",
        systemPrompt = "You are a Customer Experience expert. Draft a polite, supportive, and extremely helpful team response that addresses the public concern, outlines the solution transparently, and invites direct correspondence for further assistance.",
        userPromptBuilder = { f1, f2, tone -> "Write social media complaint response to \"$f1\" proposing \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "cs_apology_email",
        name = "Apology Email",
        category = "Customer Service",
        description = "Construct genuine, empathetic apology letters regarding performance delays or delivery mistakes.",
        icon = "🤝",
        field1Label = "The Mistake or Event",
        field1Placeholder = "e.g., shipments delayed by 4 days due to heavy shipping terminal congestion",
        field2Label = "Correction and Compensation details",
        field2Placeholder = "e.g., free shipping coupon and immediate priority fulfillment for their next purchase",
        systemPrompt = "Act as the Chief Customer Satisfaction Director. Draft an email expressing sincere responsibility for the error, explaining the contextual cause clearly without excuses, detailing the fix, and offering compensation.",
        userPromptBuilder = { f1, f2, tone -> "Write a formal customer apology email about \"$f1\". Offer: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "cs_ticket_reply",
        name = "Ticket Reply",
        category = "Customer Service",
        description = "Quickly formulate helpful support ticket answers to resolve problems efficiently.",
        icon = "🎫",
        field1Label = "Customer Concern / Issue",
        field1Placeholder = "e.g., 'I got charged twice for my subscription invoice'",
        field2Label = "Resolution Actions",
        field2Placeholder = "e.g., verified triple charge, refunded duplicate amount, funds back in 3 days",
        systemPrompt = "Draft a neat, professional ticketing support response. Outline the precise investigative steps taken, confirm the resolution details clearly, explain the receipt process, and invite them to response if any doubt continues.",
        userPromptBuilder = { f1, f2, tone -> "Draft a ticketing reply text for issue \"$f1\". Action: \"$f2\". Support tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "cs_doc_articles",
        name = "Article Generator - Documentation",
        category = "Customer Service",
        description = "Write helpful, step-by-step FAQ/Documentation guides for support centers.",
        icon = "📖",
        field1Label = "Action Topic / Feature",
        field1Placeholder = "e.g., How to configure custom notifications on sound profiles",
        field2Label = "Steps / Guidelines outline",
        field2Placeholder = "e.g., open app settings, tap on alert menu, choose selective categories, press save",
        systemPrompt = "Compile a crisp, detailed documentation walkthrough. Use clear numbered headers, bold interface key terms, provide tips on potential settings, and wrap up with a troubleshooting note.",
        userPromptBuilder = { f1, f2, tone -> "Write a support documentation module on topic \"$f1\" detailing steps: \"$f2\". Tone style: \"$tone\"." }
    )
).map { it.copy(category = "Customer Service") }

private fun getEntertainmentTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "ent_song_lyrics",
        name = "Song Lyrics",
        category = "Entertainment",
        description = "Construct beautiful rhyming verses and chorus lines in any musical genre.",
        icon = "🎵",
        field1Label = "Genre & Mood",
        field1Placeholder = "e.g., melancholy indie-folk, cosy synthwave",
        field2Label = "Theme / Narrative points",
        field2Placeholder = "e.g., midnight train, leaving hometown lights behind, a pocket of coins",
        systemPrompt = "Act as a brilliant lyricist. Write lyrics containing a vivid Verse 1, Pre-Chorus, catchy Chorus, Verse 2, and Bridge. Ensure rhythmic prose and evocative emotional imagery.",
        userPromptBuilder = { f1, f2, tone -> "Write song lyrics in genre \"$f1\" containing motived themes \"$f2\". Atmosphere: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ent_jokes",
        name = "Jokes",
        category = "Entertainment",
        description = "Generate funny, situational, or lighthearted jokes about any topic.",
        icon = "😄",
        field1Label = "Underlying Subject",
        field1Placeholder = "e.g., software debugging, cold brew coffee addictions",
        field2Label = "Style of Humour",
        field2Placeholder = "e.g., clever dad joke, self-deprecating programmer humour",
        systemPrompt = "Generate 5 hilarious, completely clean, original jokes around the chosen subject. Focus on high-quality punchlines.",
        userPromptBuilder = { f1, f2, tone -> "Write jokes on topic \"$f1\". Style of humour: \"$f2\". Delivery vibe: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ent_event_planner",
        name = "Event Planner",
        category = "Entertainment",
        description = "Formulate creative ideas and timeline schedules for parties or meetups.",
        icon = "🎉",
        field1Label = "Event Type & Vibe",
        field1Placeholder = "e.g., retrograde sci-fi themed birthday party for 20 adults",
        field2Label = "Venue & Budget Details",
        field2Placeholder = "e.g., backyard cabin, moderate budget, loves vintage neon styles",
        systemPrompt = "Draft a comprehensive party outline planning guide. Include: 1. Theme/Aesthetic tips, 2. Menu/Drink ideas, 3. Interactive games/activities, and 4. A sample 4-hour timeline flow.",
        userPromptBuilder = { f1, f2, tone -> "Create an event plan for \"$f1\" given variables \"$f2\". Style/Vibe: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ent_travel_planner",
        name = "Travel Planner",
        category = "Entertainment",
        description = "Assemble customized travel itineraries complete with top locations and food spots.",
        icon = "✈️",
        field1Label = "City/Country & Duration",
        field1Placeholder = "e.g., Kyoto, Japan - 3-day slow travel trip",
        field2Label = "Stance / Travel Style",
        field2Placeholder = "e.g., loves historic wooden architecture, matcha cafes, slow walking paths, budget-friendly",
        systemPrompt = "Draft a highly curated, day-by-day travel plan. Detail specific locations to visit, recommended dining experiences, timing guides, and essential tourist safety tips.",
        userPromptBuilder = { f1, f2, tone -> "Design a day-by-day travel plan for \"$f1\" tailoring interests: \"$f2\". Planner voice: \"$tone\"." }
    ),
    WriterTemplate(
        id = "ent_gift_ideas",
        name = "Gift Ideas",
        category = "Entertainment",
        description = "Unlock customized, clever gift ideas matching the recipient's personality.",
        icon = "🎁",
        field1Label = "Recipient Profile & Relationship",
        field1Placeholder = "e.g., my 32y programmer brother who loves espresso",
        field2Label = "Price guidelines / Hobbies",
        field2Placeholder = "e.g., under $100, likes minimalist setups, hates cheesy mugs",
        systemPrompt = "Brainstorm 8 highly thoughtful and unique gift ideas. Categorize by: Practical/Daily use, tech gear accessories, and experience-based gifts. Explain why each is a perfect fit.",
        userPromptBuilder = { f1, f2, tone -> "Recommend gift ideas for \"$f1\". Budget constraints: \"$f2\". Deliver style: \"$tone\"." }
    )
).map { it.copy(category = "Entertainment") }

private fun getWebsiteTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "web_sitemap",
        name = "Sitemap",
        category = "Website",
        description = "Assemble clean website sitemaps outlining major sections and features.",
        icon = "🗺️",
        field1Label = "Startup Or Brand focus",
        field1Placeholder = "e.g., micro-SaaS providing automated receipt scans for freelancers",
        field2Label = "Key user objectives",
        field2Placeholder = "e.g., quick upload, tax report export, security review page",
        systemPrompt = "Draft a clear, professional visual sitemap structure utilizing markdown code blocks or tree elements showing page relationships and focus features.",
        userPromptBuilder = { f1, f2, tone -> "Create website sitemap for \"$f1\" prioritizing requirements \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_tagline",
        name = "Tagline",
        category = "Website",
        description = "Create unforgettable website hero headlines and value propositions.",
        icon = "🎯",
        field1Label = "Product Name & Category",
        field1Placeholder = "e.g., SolarGlide - cargo drones powered by solar cells",
        field2Label = "Core emotional benefit",
        field2Placeholder = "e.g., complete delivery speed without emitting any fuel card footprint",
        systemPrompt = "Brainstorm 10 catchy, impactful landing page hero taglines under 10 words. Categorize by tone style: Bold, Technical, Playful, and Visionary.",
        userPromptBuilder = { f1, f2, tone -> "Draft taglines for product \"$f1\" emphasizing benefit \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_ux_ideas",
        name = "UX Ideas",
        category = "Website",
        description = "Brainstorm high-quality user flows and interactive features.",
        icon = "⚡",
        field1Label = "Webpage / Onboarding flow",
        field1Placeholder = "e.g., a simple widget that customizes physical pillows based on sleep position",
        field2Label = "Ideal conversion metric",
        field2Placeholder = "e.g., reducing checkout bounce rate, keeping user engaged during quiz",
        systemPrompt = "Provide 5 detailed, highly practical UX design ideas to reduce friction and improve user retention for the described workflow. Reference modern patterns.",
        userPromptBuilder = { f1, f2, tone -> "Brainstorm UX optimization ideas for \"$f1\" aiming at \"$f2\". Advice style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_design_ideas",
        name = "Design Ideas",
        category = "Website",
        description = "Recommend aesthetic styling themes, colors, and font pairings.",
        icon = "🎨",
        field1Label = "Application Category / Concept",
        field1Placeholder = "e.g., a slow-paced meditation and star-gazing diary",
        field2Label = "Desired brand atmosphere",
        field2Placeholder = "e.g., safe, cosmic slate aesthetic, deep dark canvas, soothing light-green highlights",
        systemPrompt = "Formulate a cohesive web design style guide. Recommend: 3 Color Palettes (Hex values), 2 Font Pairing schemes, and 3 specific visual styling patterns (e.g. glassmorphism or organic lines).",
        userPromptBuilder = { f1, f2, tone -> "Propose design and color theme ideas for \"$f1\" reflecting \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_services_page",
        name = "Services Page",
        category = "Website",
        description = "Draft persuasive copy blocks outlining individual service highlights.",
        icon = "🛠️",
        field1Label = "Your Business / Service name",
        field1Placeholder = "e.g., custom solar infrastructure consultants",
        field2Label = "The value pillars to list",
        field2Placeholder = "e.g., feasibility audits, design engineering, rebate assistance",
        systemPrompt = "Write complete, high-converting copy for a Web Services page. Use engaging section titles, benefit-oriented sub-paragraphs, and active CTAs.",
        userPromptBuilder = { f1, f2, tone -> "Develop Services Page copy for \"$f1\" showcasing services \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_features_page",
        name = "Features Page",
        category = "Website",
        description = "Translate engineering parameters into delightful core features lists.",
        icon = "🔋",
        field1Label = "Technical engine details",
        field1Placeholder = "e.g., decentralized data caches, local encrypted SQLite, instant visual previews",
        field2Label = "Main user conveniences",
        field2Placeholder = "e.g., data works offline, instant loading speeds, complete privacy",
        systemPrompt = "Produce a highly persuasive features overview landing page copy grid. Match each technological achievement with an everyday human convenience highlight.",
        userPromptBuilder = { f1, f2, tone -> "Write Features Page copy for \"$f1\" mapping properties \"$f2\". Vibe style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_keyword_generator",
        name = "Keyword Generator",
        category = "Website",
        description = "Unlock a systematic list of target keywords for organic search indexing.",
        icon = "🔑",
        field1Label = "Product Focus & Niche",
        field1Placeholder = "e.g., specialized backpack for digital nomads",
        field2Label = "Competitor context",
        field2Placeholder = "e.g., competing with heavy tech travel bags, emphasizing lightweight shell",
        systemPrompt = "Brainstorm a systematic list of 25 keywords categorized by search style: Short-tail, Informational Long-tail, transactional phrases, and competitor-comparison hooks.",
        userPromptBuilder = { f1, f2, tone -> "Generate target SEO keywords for \"$f1\" focus \"$f2\". Strategy: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_keyword_extractor",
        name = "Keyword Extractor",
        category = "Website",
        description = "Extract high-density SEO keywords from raw text drafts.",
        icon = "🔍",
        field1Label = "Raw Text content",
        field1Placeholder = "Paste your text block here...",
        field2Label = "Limits / Style requirements",
        field2Placeholder = "e.g., extract top 10 single words and top 10 key phrases",
        systemPrompt = "Examine the text carefully. Extract and list the highest significance keywords, indicating estimated priority and contextual relevance.",
        userPromptBuilder = { f1, f2, tone -> "Extract keywords from \"$f1\". Preferences: \"$f2\". Delivery style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_seo",
        name = "SEO",
        category = "Website",
        description = "Produce SEO-optimized titles, headings, and description tags.",
        icon = "📈",
        field1Label = "Target Page Theme",
        field1Placeholder = "e.g., guide to roasting coffee beans at home in an oven",
        field2Label = "Primary & Secondary focus keywords",
        field2Placeholder = "e.g., home coffee roasting, oven method, green beans",
        systemPrompt = "Deliver a complete SEO meta-package: 1. Primary Title Tag (under 60 chars), 2. Meta Description tag (under 160 chars), 3. H1 Proposal, and 4. Three suggested H2 titles containing keyword variations.",
        userPromptBuilder = { f1, f2, tone -> "Produce complete SEO tag packages for \"$f1\" combining particles \"$f2\". Style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_testimonials",
        name = "Testimonials",
        category = "Website",
        description = "Draft organic, high-converting testimonials for landing page blocks.",
        icon = "🗣️",
        field1Label = "Service or Landing page main benefit",
        field1Placeholder = "e.g., the task reminder widget sent via SMS keeps users fully on schedule",
        field2Label = "Customer Profile variables",
        field2Placeholder = "e.g., a junior designer, a busy remote manager, a high-stress student",
        systemPrompt = "Draft three short, highly genuine-sounding testimonials that mention specific workflow gains and highlight emotional relief.",
        userPromptBuilder = { f1, f2, tone -> "Generate 3 testimonials for service \"$f1\" showing \"$f2\" perspectives. Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_landing_content",
        name = "Landing Page Content - Copywriting",
        category = "Website",
        description = "Produce a full comprehensive landing page copywriting layout.",
        icon = "🌐",
        field1Label = "Product Name & Core Audience problem",
        field1Placeholder = "e.g., Apex Calorie Tracker - busy professionals struggling to calculate home recipe calories",
        field2Label = "Unique Feature / Engine",
        field2Placeholder = "e.g., snapshot recipe scanning, automatic density calculation, offline logs",
        systemPrompt = "Generate complete landing page copywriting copy. Use clear dividers for sections: Hero (headline + descriptor + CTA), Problem, Features/Benefits overview, Social Proof placeholders, and final urgency section.",
        userPromptBuilder = { f1, f2, tone -> "Write full landing page copy for \"$f1\". Core engine: \"$f2\". Copy style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_site_copywriting",
        name = "Website - Copywriting",
        category = "Website",
        description = "Compile precise copy segments for website navigation sections.",
        icon = "✏️",
        field1Label = "Company / Agency Concept",
        field1Placeholder = "e.g., bespoke visual branding agency for boutique restaurants",
        field2Label = "Value metrics to state",
        field2Placeholder = "e.g., designed 50+ menus, averaging 30% increase in social media engagement",
        systemPrompt = "Draft complete, modern, styled copy blocks for: 1. 'About us' mission outline, 2. 'How it works' 3-step timeline, 3. 'Our Vibe' culture paragraph.",
        userPromptBuilder = { f1, f2, tone -> "Develop web copywriting segments for \"$f1\" emphasizing \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_faq",
        name = "FAQ",
        category = "Website",
        description = "Write landing page FAQ answers addressing transactional concerns.",
        icon = "❓",
        field1Label = "Service / App Subscription",
        field1Placeholder = "e.g., automatic meal subscription service",
        field2Label = "Main transactional doubts",
        field2Placeholder = "e.g., can I pause subscription, package temperature safety, allergen controls",
        systemPrompt = "Draft a neat landing page FAQ containing 5 question-and-answer blocks written to handle direct friction and drive checkout conversion.",
        userPromptBuilder = { f1, f2, tone -> "Formulate FAQ for \"$f1\". Doubts outline: \"$f2\". Brand voice: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_cta",
        name = "Call to Action",
        category = "Website",
        description = "Write high-performance Call-to-Action phrases and sub-header urgency lines.",
        icon = "🔥",
        field1Label = "Action Goal of Webpage",
        field1Placeholder = "e.g., downloading our lightweight budget worksheet",
        field2Label = "Direct Value / Low-friction prompt",
        field2Placeholder = "e.g., takes 2 minutes, absolute free access, no credit card required",
        systemPrompt = "Generate 10 catchy, action-oriented button text labels coupled with a supportive, low-friction micro-copy sentence that goes under or above the button.",
        userPromptBuilder = { f1, f2, tone -> "Develop Conversion-focused Call to Action options for \"$f1\". Friction-reducing cues: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_rewrite_keywords",
        name = "Rewrite with Keywords",
        category = "Website",
        description = "Refine landing page segments while weaving in essential target search terms.",
        icon = "🔄",
        field1Label = "Raw Content draft",
        field1Placeholder = "e.g., we make solar accessories which are quick to integrate",
        field2Label = "Target Keywords to inject",
        field2Placeholder = "e.g., off-grid solar systems, diy battery guide, camper power kit",
        systemPrompt = "You are a professional SEO web writer. Rewrite the passage to sound elegant and natural, while weaving in the target keywords seamlessly without search-stuffing.",
        userPromptBuilder = { f1, f2, tone -> "Rewrite text \"$f1\" incorporating keywords \"$f2\". Copywriting tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_privacy_policy",
        name = "Privacy Policy",
        category = "Website",
        description = "Synthesize standard draft layouts for standard Privacy Policy statements.",
        icon = "🛡️",
        field1Label = "App Name & Data collection rules",
        field1Placeholder = "e.g., PeakFocus, collecting email and daily task titles, local storage only",
        field2Label = "Standard Regulations matching",
        field2Placeholder = "e.g., GDPR compliant, California user transparency, no third-party sales",
        systemPrompt = "Draft a clear, structured Privacy Policy template layout detailing sections for: Information We Collect, How We Use Data, Data Storage & Encryption, and User Rights details.",
        userPromptBuilder = { f1, f2, tone -> "Generate Privacy Policy draft for \"$f1\" given variables \"$f2\". Tone style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "web_terms_conditions",
        name = "Terms and Conditions",
        category = "Website",
        description = "Draft essential Terms and Conditions outlines for SaaS apps.",
        icon = "⚖️",
        field1Label = "SaaS Product & Owner company",
        field1Placeholder = "e.g., TaskSphere - collaborative board tool owned by Sphere LLC",
        field2Label = "Account termination & payment rules",
        field2Placeholder = "e.g., no refund for partial months, immediately terminate accounts violating guidelines",
        systemPrompt = "Draft standard Terms of Service outline segments including: User Eligibility, Payment & Billing, Acceptable Conduct, Responsibility limitations, and Governing Law.",
        userPromptBuilder = { f1, f2, tone -> "Write Terms and Conditions draft template for \"$f1\" focusing \"$f2\". Legal tone: \"$tone\"." }
    )
).map { it.copy(category = "Website") }

private fun getAdvertisingTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "adv_ideas",
        name = "Advertising Ideas",
        category = "Advertising",
        description = "Generate creative marketing concepts and angles for ad campaigns.",
        icon = "💡",
        field1Label = "Your Business or Campaign focus",
        field1Placeholder = "e.g., subscription for zero-waste packaging grocery kits",
        field2Label = "Target audience trigger",
        field2Placeholder = "e.g., eco-conscious busy moms, highlighting saved waste and organic safety",
        systemPrompt = "Generate 5 detailed, highly creative advertising campaign concepts. For each concept, include a visual concept metaphor, a core hook tagline, and the emotion targeted.",
        userPromptBuilder = { f1, f2, tone -> "Develop Advertising Campaign proposals for \"$f1\". Target audience notes: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "adv_fb_ad",
        name = "FB Ad",
        category = "Advertising",
        description = "Draft click-optimized headlines, body texts, and CTAs for Facebook.",
        icon = "📱",
        field1Label = "Product Offer details",
        field1Placeholder = "e.g., modern hand-spun wool blankets",
        field2Label = "Focus benefits or metrics",
        field2Placeholder = "e.g., completely hand-knitted, organic materials, hypoallergenic",
        systemPrompt = "Act as an expert performance marketer. Draft three distinctive Facebook ad copy ideas prioritizing benefits, hooks, and clean visual divider spacing.",
        userPromptBuilder = { f1, f2, tone -> "Write FB ad copy variations for \"$f1\". Benefits focus: \"$f2\". Tone level: \"$tone\"." }
    ),
    WriterTemplate(
        id = "adv_google_ad",
        name = "Google Ad",
        category = "Advertising",
        description = "Compile complete Google Search ads including headlines and description options.",
        icon = "🌐",
        field1Label = "Service or Business Category",
        field1Placeholder = "e.g., remote executive recruiting agency",
        field2Label = "Core advantages to display",
        field2Placeholder = "e.g., vetted candidates, 72-hour turnaround, 100% guarantee",
        systemPrompt = "Formulate a systematic Google Search text ad: Provide 5 distinct headlines (strictly under 30 characters) and 3 description options (strictly under 90 characters) loaded with target advantages.",
        userPromptBuilder = { f1, f2, tone -> "Draft Google search ad variables for \"$f1\". Advantages: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "adv_ad_script",
        name = "Ad Script",
        category = "Advertising",
        description = "Write structured narrator scripts with scene actions for promotions.",
        icon = "📝",
        field1Label = "Campaign Theme / Product focus",
        field1Placeholder = "e.g., lightweight hybrid solar battery charger backpack",
        field2Label = "Key scenes or visual hooks",
        field2Placeholder = "e.g., hiking through deep green fog, battery dies, backpack saves the photo shoot",
        systemPrompt = "Provide a professional, double-column storyboard script file for a 30-second video ad. Detail the visual scene cues in one column and voiceover/SFX scripts in the second.",
        userPromptBuilder = { f1, f2, tone -> "Draft ad script storyboard for \"$f1\". Scenes outline: \"$f2\". Copy tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "adv_tv_ads",
        name = "TV Ads",
        category = "Advertising",
        description = "Develop classic, high-concept, storytelling TV commercial structures.",
        icon = "📺",
        field1Label = "Brand / Service name",
        field1Placeholder = "e.g., family car protection service plans",
        field2Label = "Core emotional story arc",
        field2Placeholder = "e.g., young driver leaving home, parents feeling peace of mind through emergency app",
        systemPrompt = "Draft a classic 30-second narrative TV commercial scenario. Outline: 1. Setup scene, 2. The Conflict, 3. The Resolution, 4. Voiceover tagline close, and 5. Visual logo slate design.",
        userPromptBuilder = { f1, f2, tone -> "Write a TV ad scene flow on \"$f1\" outlining story \"$f2\". Delivery tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "adv_youtube_ads",
        name = "Youtube Ads",
        category = "Advertising",
        description = "Formulate scripts for skippable video ads with hooks in the first 5 seconds.",
        icon = "🎥",
        field1Label = "Product Name & Category",
        field1Placeholder = "e.g., local bakery subscription delivery app",
        field2Label = "First 5-Seconds Unskippable Hook",
        field2Placeholder = "e.g., biting into a fresh hot croissant showing steam while a bell rings",
        systemPrompt = "Design a high-converting 30-second YouTube Video Ad script. Structure the script with a powerful first 5-seconds unskippable hook to keep viewers from hitting 'Skip', a clean educational value proof, and a clear visual CTA overlay prompt.",
        userPromptBuilder = { f1, f2, tone -> "Produce YouTube Video ad scripts for \"$f1\" utilizing unskippable hook \"$f2\". Tone: \"$tone\"." }
    )
).map { it.copy(category = "Advertising") }

private fun getLanguagesTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "lang_translate",
        name = "Translate",
        category = "Languages",
        description = "Translate draft phrases into any language with custom style preservation.",
        icon = "🗣️",
        field1Label = "Text content to Translate",
        field1Placeholder = "e.g., Please note that the shipment has been delayed due to technical issues.",
        field2Label = "Target Language & Tone Vibe",
        field2Placeholder = "e.g., Spanish, highly respectful corporate style",
        systemPrompt = "Act as an expert elite translator. Supply a polished, culturally accurate translation, explaining major idioms or word selections used.",
        userPromptBuilder = { f1, f2, tone -> "Translate text \"$f1\" to target parameters \"$f2\". Delivery style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "lang_grammar",
        name = "Grammar Check",
        category = "Languages",
        description = "Detect spelling mistakes, fix awkward phrasing, and optimize flow.",
        icon = "🔍",
        field1Label = "Text to Analyze",
        field1Placeholder = "e.g., we was going to setup the database server but we didn't got any keys...",
        field2Label = "Polish rules",
        field2Placeholder = "e.g., correct mistakes strictly, keep it simple, or make it sound native",
        systemPrompt = "Correct all grammar, layout, punctuation, and wording errors. Render the polished draft and list a brief summary explaining the changes made.",
        userPromptBuilder = { f1, f2, tone -> "Correct draft spelling: \"$f1\". Directives: \"$f2\". Style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "lang_synonyms",
        name = "List Synonyms",
        category = "Languages",
        description = "Formulate a curated dictionary list of creative synonyms and related vocabulary.",
        icon = "📖",
        field1Label = "Focus Word / Concept",
        field1Placeholder = "e.g., beautiful, or innovative, or slow",
        field2Label = "Desired context",
        field2Placeholder = "e.g., describing architecture, or software engineering, or mental healing",
        systemPrompt = "Draft a list of 10 sophisticated, rich, and context-specific synonyms for the word. Include brief definitions and sample sentences explaining usage.",
        userPromptBuilder = { f1, f2, tone -> "Generate synonyms list for \"$f1\" in context of \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "lang_learn_words",
        name = "Learn New Words",
        category = "Languages",
        description = "Discover target words, definitions, and realistic usage scenarios.",
        icon = "🪐",
        field1Label = "Themed Topic or Field of Interest",
        field1Placeholder = "e.g., blockchain, or atmospheric physics, or fine dining",
        field2Label = "Your Current Proficiency Level",
        field2Placeholder = "e.g., total beginner, intermediate conversationalist",
        systemPrompt = "Compile a list of 5 high-impact vocabulary words with simplified, intuitive definitions, structural pronunciation hints, and realistic conversational usage examples.",
        userPromptBuilder = { f1, f2, tone -> "Compile useful vocabulary words in theme \"$f1\" for level \"$f2\". Instructor tone: \"$tone\"." }
    )
).map { it.copy(category = "Languages") }

private fun getFitnessHealthTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "fit_workout",
        name = "Create a Workout Plan",
        category = "Fitness & Health",
        description = "Formulate custom weekly structured physical exercise plans.",
        icon = "💪",
        field1Label = "User Fitness Goals & Profile",
        field1Placeholder = "e.g., build core muscle, 35y, executive hybrid desk worker, 3 workouts weekly",
        field2Label = "Limitations / Preferences",
        field2Placeholder = "e.g., light joint fatigue, prefers bodyweight and dumbbells only",
        systemPrompt = "Act as a passionate, certified personal trainer. Generate a beautifully structured weekly workout chart detailing exercise names, safe target sets, recommended rest variables, and structural tips.",
        userPromptBuilder = { f1, f2, tone -> "Develop a custom workout plan for client \"$f1\". Restrictions: \"$f2\". Trainer voice style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "fit_meal",
        name = "Create a Meal Plan",
        category = "Fitness & Health",
        description = "Assemble customized daily recipes and healthy meal guidelines.",
        icon = "🥗",
        field1Label = "Dietary Target & Calorie Focus",
        field1Placeholder = "e.g., 2000 kcal, gain lean mass, desk worker",
        field2Label = "Allergies / Ingredient preferences",
        field2Placeholder = "e.g., dairy-free, enjoys chicken, spinach, eggs, avocados, rice",
        systemPrompt = "Act as a professional physical nutritionist. Generate a 3-day structured meal plan covering breakfasts, lunches, dinners, and healthy snacks, including approximate protein/carb/fat breakdown metrics.",
        userPromptBuilder = { f1, f2, tone -> "Generate customized meal plan for target \"$f1\". Demands: \"$f2\". Tone style: \"$tone\"." }
    )
).map { it.copy(category = "Fitness & Health") }

private fun getWriterTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "wri_ebook",
        name = "eBook",
        category = "Writer",
        description = "Write individual ebook chapters complete with section guidelines and takeaways.",
        icon = "📖",
        field1Label = "eBook Topic & Chapter title",
        field1Placeholder = "e.g., The Art of Saying No (Chapter 3: handling pushy workplace requests)",
        field2Label = "Core lesson or case study",
        field2Placeholder = "e.g., the standard email script template, avoiding instant yes, staying respectful",
        systemPrompt = "Act as a modern, bestselling non-fiction author. Write a detailed, engaging chapter draft. Use rich, encouraging prose, logical subheadings, conversational story segments, and actionable final checklists.",
        userPromptBuilder = { f1, f2, tone -> "Compose eBook copy for \"$f1\". Case highlights: \"$f2\". Author tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "wri_paragraph",
        name = "Paragraph",
        category = "Writer",
        description = "Draft descriptive, structurally coherent paragraphs on any niche topic.",
        icon = "✍️",
        field1Label = "Subject and Main Sentence Hook",
        field1Placeholder = "e.g., how the smell of old paper triggers childhood mental screens",
        field2Label = "Vibe / Focus keywords",
        field2Placeholder = "e.g., dusty brass clocks, rain hitting window glass, warmth",
        systemPrompt = "Write a beautifully crafted, slow-paced literary paragraph. Pay deep attention to tactile detail, rhythmic punctuation flow, and poetic descriptors.",
        userPromptBuilder = { f1, f2, tone -> "Write a creative literary paragraph about \"$f1\" combining sensory cues: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "wri_story",
        name = "Story Generator",
        category = "Writer",
        description = "Spark screenplay scenes, descriptive outlines, and character dialogue flows.",
        icon = "🎭",
        field1Label = "Story Premise & Genre",
        field1Placeholder = "e.g., retro-futuristic detective mystery, finding a glowing cassette tape",
        field2Label = "Characters & Motifs to include",
        field2Placeholder = "e.g., Agent Clara (skeptical), a copper magnifying glass, ticking clocks",
        systemPrompt = "Draft a highly vivid, cinematic story scene introduction. Use powerful sensory detail, organic dialogue, and build up a solid cliffhanger ending.",
        userPromptBuilder = { f1, f2, tone -> "Compose creative storytelling page for \"$f1\" using motifs \"$f2\". Vibe tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "wri_book_ideas",
        name = "Book Ideas",
        category = "Writer",
        description = "Brainstorm engaging novel premises, plot twists, and concept charts.",
        icon = "💡",
        field1Label = "Broad Genre of interest",
        field1Placeholder = "e.g., cosy fantasy, or eco-thriller",
        field2Label = "Vibes / Basic visual sparks",
        field2Placeholder = "e.g., glowing mushroom forests, small library in a giant tree trunk, a silent tea shop",
        systemPrompt = "Act as a creative publishing consultant. Provide 5 unique novel hook ideas, highlighting the main conflict setup, the target protagonist dilemma, and a suggested cliffhanger premise for each.",
        userPromptBuilder = { f1, f2, tone -> "Generate creative book hook ideas in genre \"$f1\" leveraging \"$f2\". Creative tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "wri_improver",
        name = "Content Improver",
        category = "Writer",
        description = "Polishes flat sentences to read with high emotional impact and visual density.",
        icon = "🚀",
        field1Label = "Raw flat draft text",
        field1Placeholder = "e.g., Clara walked in the old library. She was tired and it was raining outside.",
        field2Label = "Desired visual / Vibe upgrade",
        field2Placeholder = "e.g., emphasize heavy exhaustion, cold rain smell, dust motes in glowing golden light",
        systemPrompt = "Act as an experienced literary prose editor. Polish that flat text snippet, upgrading sentence structure, flow, sensory imagery, and emotional depth.",
        userPromptBuilder = { f1, f2, tone -> "Polish and improve text snippet \"$f1\" adding elements: \"$f2\". Upgrade tone: \"$tone\"." }
    )
).map { it.copy(category = "Writer") }

private fun getMiscTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "misc_explain_child",
        name = "Explain to a Child",
        category = "Misc",
        description = "Simplify complex modern science, tech, or economics concepts using easy analogies.",
        icon = "🧸",
        field1Label = "Complex Subject to explain",
        field1Placeholder = "e.g., quantum computer superposition, or inflation",
        field2Label = "Analogy style / Age range",
        field2Placeholder = "e.g., explain like I'm 8, using a magic playground or arcade game story",
        systemPrompt = "You are a warm, highly intuitive elementary school teacher. Explain the complex subject using incredibly simple terms, relatable everyday concepts, fun toys storytelling, and zero technical jargon.",
        userPromptBuilder = { f1, f2, tone -> "Explain \"$f1\" using analogy context \"$f2\". Instructor voice: \"$tone\"." }
    ),
    WriterTemplate(
        id = "misc_journalist",
        name = "Write News From a Journalist Point of View",
        category = "Misc",
        description = "Format bullet facts into objective, high-conviction news reporting articles.",
        icon = "📰",
        field1Label = "The Headline event / incident",
        field1Placeholder = "e.g., launch of the world's largest solar propeller drone flight trial",
        field2Label = "Important facts / witness observations",
        field2Placeholder = "e.g., reached 65,000 feet, flight lasted 48 hours, built by AeroGlide in Zurich",
        systemPrompt = "You are a seasoned international news correspondent. Draft an authoritative, objective news story. Include a strong headline, clear location-date lede, contextual history blocks, and an objective future outlook description.",
        userPromptBuilder = { f1, f2, tone -> "Write press news about \"$f1\" highlighting facts \"$f2\". Writer perspective: \"$tone\"." }
    ),
    WriterTemplate(
        id = "misc_rss_generate",
        name = "Generate From RSS Feed",
        category = "Misc",
        description = "Draft concise, shareable digest bullet points from raw RSS metadata fields.",
        icon = "📡",
        field1Label = "Raw RSS title / text string snippet",
        field1Placeholder = "e.g., title: '<pubDate>Wed, 03 Jun 2026</pubDate> <description>SpaceX logs new landing record...'",
        field2Label = "Target summary format",
        field2Placeholder = "e.g., three short crisp sentences for a technical dispatch newsletter",
        systemPrompt = "Clean up the raw RSS code tags and parse the core message into a human-readable update segment.",
        userPromptBuilder = { f1, f2, tone -> "Generate news summary list from RSS entry: \"$f1\" style preference: \"$f2\". Tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "misc_undetectable",
        name = "Undetectable AI",
        category = "Misc",
        description = "Rewrite AI sentences to read with natural, diverse human rhythm and speech structures.",
        icon = "👽",
        field1Label = "The AI-sounding Draft text",
        field1Placeholder = "e.g., additionally, it is important to remember that eating vegetables provides health benefits...",
        field2Label = "Target writing voice details",
        field2Placeholder = "e.g., passionate independent blogger, fast sentence beats, friendly casual language",
        systemPrompt = "Analyze the text block and rewrite it completely using diverse sentence lengths, natural transitions, casual vocabulary, and realistic storytelling to eliminate AI patterns.",
        userPromptBuilder = { f1, f2, tone -> "Rewrite this text to sound organic and human: \"$f1\". Style guidelines: \"$f2\". Voice tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "misc_pros_cons",
        name = "Pros and Cons",
        category = "Misc",
        description = "Formulate a balanced overview chart containing pros and cons.",
        icon = "⚖️",
        field1Label = "Subject / Choice to evaluate",
        field1Placeholder = "e.g., buying an electrical camper van vs gas van in 2026",
        field2Label = "Evaluation metrics key",
        field2Placeholder = "e.g., price, environment, comfort, charging options, maintenance",
        systemPrompt = "Formulate an objective, balanced markdown checklist table showing positive elements on one side and potential friction elements on the other.",
        userPromptBuilder = { f1, f2, tone -> "Synthesize Pros and Cons comparative sheet for \"$f1\" emphasizing parameters \"$f2\". Evaluation style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "misc_real_estate",
        name = "Real Estate Listing",
        category = "Misc",
        description = "Draft engaging, inspiring property marketing descriptions.",
        icon = "🏡",
        field1Label = "Property Specifications & Location",
        field1Placeholder = "e.g., 3-bed cozy cottage in Portland, wooden deck, large brick fireplace",
        field2Label = "Aesthetic Vibe highlights",
        field2Placeholder = "e.g., quiet sunrise lights inside kitchen, walking path to lake, historic details",
        systemPrompt = "Draft an engaging, highly attractive real estate listing text. Start with an eye-grabbing headline, outline the interior comforts and neighborhood charms, and close with a CTA to book an immediate walk-through.",
        userPromptBuilder = { f1, f2, tone -> "Write Real Estate description copy for \"$f1\" highlighting mood \"$f2\". Tone of description: \"$tone\"." }
    ),
    WriterTemplate(
        id = "misc_prompt_generator",
        name = "Prompt Generator",
        category = "Misc",
        description = "Assemble detailed, structured LLM prompt templates.",
        icon = "🔮",
        field1Label = "What is the prompt's main task?",
        field1Placeholder = "e.g., analyze financial statements and find red flags",
        field2Label = "Target role & instructions",
        field2Placeholder = "e.g., act as a cynical auditor, check cash flow statement, print in structured lists",
        systemPrompt = "Produce a highly optimized, structured meta-prompt. Use clear sections like: [ROLE], [CONTEXT], [TASK GUIDELINES], [INPUT DATA CASE], and [OUTPUT EXPECTATIONS]. Ensure superb instructions layout.",
        userPromptBuilder = { f1, f2, tone -> "Create an optimized prompt template for task \"$f1\". Additional parameters: \"$f2\". Vibe tone: \"$tone\"." }
    )
).map { it.copy(category = "Misc") }

private fun getPersonalGrowthTemplates(): List<WriterTemplate> = listOf(
    WriterTemplate(
        id = "growth_journal",
        name = "Mindset Journal Prompts",
        category = "Personal Growth",
        description = "Generate highly focused physical daily journaling prompts for self-reflection and wisdom.",
        icon = "📓",
        field1Label = "Current Emotional State or Challenge",
        field1Placeholder = "e.g., feeling stressed about imposter syndrome in a new management role",
        field2Label = "Reflection Vibe / Focus",
        field2Placeholder = "e.g., self-compassion, active problem solving, somatic grounding checks",
        systemPrompt = "You are a thoughtful developmental human psychologist and coach. Generate 5 highly direct, deep, non-cliché journal prompt questions tailored meticulously to deconstruct the user's focus scenario, including a small 1-sentence grounding quote introduction.",
        userPromptBuilder = { f1, f2, tone -> "Compose deep mindfulness journal prompts for State: \"$f1\". Focus: \"$f2\". Teacher tone: \"$tone\"." }
    ),
    WriterTemplate(
        id = "growth_habit",
        name = "Habit Loop Catalyst",
        category = "Personal Growth",
        description = "Deconstruct forming a new behavior into behavioral atomic loop dynamics.",
        icon = "🔄",
        field1Label = "New Habit you desire to build",
        field1Placeholder = "e.g., reading 15 pages of philosophy books before sleeping",
        field2Label = "Existing trigger cue / constraint",
        field2Placeholder = "e.g., after turning off the bedside laptop, but tempted to browse smartphone",
        systemPrompt = "Act as an expert behavioral designer. Deconstruct the target behavior using the James Clear Atomic Habits style framework: 1. The Cue (making it obvious), 2. The Craving (making it attractive), 3. The Response (making it easy), and 4. The Reward (making it highly satisfying). Print brief, bulleted practical actions for each step.",
        userPromptBuilder = { f1, f2, tone -> "Formulate an atomic habit catalyst structure for building habit: \"$f1\". Cue scenario context: \"$f2\". Writer style: \"$tone\"." }
    ),
    WriterTemplate(
        id = "growth_goals",
        name = "Goal-to-Action Micro Map",
        category = "Personal Growth",
        description = "Break down intimidating, massive achievements into immediate daily steps.",
        icon = "🗺️",
        field1Label = "Intimidating long-term lifetime goal",
        field1Placeholder = "e.g., writing and publishing a complete sci-fi thriller ebook",
        field2Label = "Weekly time commitment possible",
        field2Placeholder = "e.g., 4 hours weekly, mostly early mornings before standard shift",
        systemPrompt = "You are an elite productivity alignment specialist. Deconstruct the ambitious goal into 3 concrete layers: 1. The 12-Week Milestone target, 2. The Weekly Micro-Focus block, and 3. The Daily Immediate Action (under 15 mins). Keep it highly realistic and free of overwhelming pressure details.",
        userPromptBuilder = { f1, f2, tone -> "Construct a Goal-to-Action roadmap for \"$f1\" with window \"$f2\". Strategy vibe: \"$tone\"." }
    ),
    WriterTemplate(
        id = "growth_vision",
        name = "Personal Alignment Manifesto",
        category = "Personal Growth",
        description = "Deconstruct and draft a powerful personal alignment manifesto card.",
        icon = "✨",
        field1Label = "Core Values & Identity goals",
        field1Placeholder = "e.g., creative integrity, slow deliberate family time, physical vitality",
        field2Label = "Things/Behaviors you want to reject",
        field2Placeholder = "e.g., constant dopamine scrolling, saying yes to low-leverage tasks",
        systemPrompt = "Act as a warm, inspiring wisdom guide. Synthesize a 300-word highly poetic, high-conviction personal manifesto. Include beautiful sections: 'What I Hold Sacred', 'What I Refuse', 'How I Navigate Daily Choice Decisions'. Keep it encouraging and visually spaced.",
        userPromptBuilder = { f1, f2, tone -> "Synthesize a personal manifesto reflecting values \"$f1\" and boundaries \"$f2\". Presentation style: \"$tone\"." }
    )
).map { it.copy(category = "Personal Growth") }
