package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.PdfTextExtractor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewOptimizationScreen(
    isOptimizing: Boolean,
    errorMessage: String?,
    onOptimize: (title: String, originalProfile: String, jobDescription: String) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var originalProfile by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isReadingPdf by remember { mutableStateOf(false) }
    var uploadedPdfName by remember { mutableStateOf<String?>(null) }
    var uploadedPdfSize by remember { mutableStateOf<String?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isReadingPdf = true
            coroutineScope.launch {
                try {
                    var name = "resume.pdf"
                    var sizeStr = ""
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) {
                                name = cursor.getString(nameIndex)
                            }
                            if (sizeIndex != -1) {
                                val size = cursor.getLong(sizeIndex)
                                sizeStr = "${String.format("%.1f", size / 1024.0)} KB"
                            }
                        }
                    }
                    
                    val extracted = PdfTextExtractor.extractText(context, uri)
                    if (extracted.isNotBlank()) {
                        originalProfile = extracted
                        uploadedPdfName = name
                        uploadedPdfSize = sizeStr
                        Toast.makeText(context, "Successfully extracted ${extracted.length} characters from PDF!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Could not extract readable text from this PDF file. Please ensure it's not scanned/image-only.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error reading PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isReadingPdf = false
                }
            }
        }
    }

    val isInputValid = title.isNotBlank() && originalProfile.isNotBlank() && jobDescription.isNotBlank()
    val scrollState = rememberScrollState()

    // Sample data to pre-fill instantly for UX demoing
    val androidJobDesc = """
Company: Apex Tech Solutions
Role: Senior Android Engineer

Requirements:
- 4+ years of professional Android development experience using Kotlin and Jetpack Compose.
- Strong knowledge of local persistence solutions like Room Database, and offline caching.
- Deep familiarity with REST APIs, Retrofit, and asynchronous data flows using StateFlow.
- Proven experience in optimizing application performance and boosting user engagement.
- Excellent communication and collaborative problem solving.
    """.trimIndent()

    val androidProfile = """
Pavan Kumar
Android Developer

Experience:
Freelance / Software Contractor (2024 - Present)
- Built a lot of Android apps for various clients.
- Used Jetpack Compose for making screens look nice.
- Wrote database tables to save todo lists and notes.
- Connected the app to the web with Retrofit APIs.

Junior Developer at StackLabs (2022 - 2024)
- Fixed app crashes and worked on simple UI cards.
- Changed buttons layout and tested simple items.
- Wrote Java and Kotlin files for custom features.
    """.trimIndent()

    val financeJobDesc = """
Company: Vanguard Capital Group
Role: Senior Corporate Financial Analyst

Requirements:
- Master's or Bachelor's in Finance, Economics, or Quantitative field.
- Expert-level financial analysis, quarterly forecasting, and statistical modeling.
- Solid database query skills (SQL, PostgreSQL) and high fluency in Excel (Pivot Tables, VLOOKUP).
- Translate complex data into slides and present metrics directly to executive leadership.
- Fast-paced collaborative team mindset.
    """.trimIndent()

    val financeProfile = """
Meera Sen
Financial Analyst

Experience:
Associate Analyst at Global Invest (2023 - Present)
- Checked quarterly budgets and kept track of expenses.
- Ran Excel sheets and did simple calculations.
- Made some presentation slides for the manager.
- Looked at database tables to extract statistics.

Intern at Prime Consulting (2022 - 2023)
- Helped pull files and input numbers in sheets.
- Listened to weekly meetings and helped format logs.
    """.trimIndent()

    // Rotating messages during loading to make wait engaging
    val loadingPhrases = listOf(
        "Scanning target Job Description keywords...",
        "Evaluating profile skills alignment...",
        "Identifying crucial keyword and competency gaps...",
        "Refactoring experience bullets to highlight business impact...",
        "Injecting truth-aligned metrics & action verbs...",
        "Formulating actionable career coaching recommendations...",
        "Structuring responsive ATS-proof profile..."
    )
    var currentPhraseIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(isOptimizing) {
        if (isOptimizing) {
            currentPhraseIndex = 0
            while (isOptimizing) {
                delay(2500)
                currentPhraseIndex = (currentPhraseIndex + 1) % loadingPhrases.size
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Alignment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Intro banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "Fill in your details below, or use the quick sample presets to instantly watch the AI reframe structure and bullets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                }

                // Sample Presets Column
                Text(
                    text = "Speed Demo Presets:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = {
                            title = "Senior Android Engineer - Apex Tech"
                            originalProfile = androidProfile
                            jobDescription = androidJobDesc
                        },
                        label = { Text("Android Engineer") },
                        icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    SuggestionChip(
                        onClick = {
                            title = "Financial Analyst - Vanguard"
                            originalProfile = financeProfile
                            jobDescription = financeJobDesc
                        },
                        label = { Text("Financial Analyst") },
                        icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

                // Custom Title / Target Job Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Target Job Title & Company *") },
                    placeholder = { Text("e.g., Senior Android Developer at Google") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("opt_title_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // PDF Upload Container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_upload_container")
                        .clickable {
                            if (!isReadingPdf) {
                                pdfLauncher.launch("application/pdf")
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (uploadedPdfName != null) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isReadingPdf) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Extracting details... 🚀 Just a moment",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else if (uploadedPdfName != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "PDF Uploaded",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uploadedPdfName ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = uploadedPdfSize ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        uploadedPdfName = null
                                        uploadedPdfSize = null
                                        originalProfile = ""
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove PDF",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Upload PDF",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Upload Resume PDF",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "AI will automatically extract plan text details",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Original Resume / LinkedIn Profile
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Profile / Current Resume *",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (originalProfile.isNotEmpty()) {
                            Text(
                                text = "Clear",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clickable { originalProfile = "" }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = originalProfile,
                        onValueChange = { originalProfile = it },
                        placeholder = { Text("Paste your current resume bullet points, job descriptions, LinkedIn profile sections, or general work history here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("opt_profile_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Target Job Description
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Job Description *",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (jobDescription.isNotEmpty()) {
                            Text(
                                text = "Clear",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clickable { jobDescription = "" }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = { jobDescription = it },
                        placeholder = { Text("Paste the job requirements, core keywords, roles/responsibilities or full job description page to align with...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("opt_jd_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onOptimize(title, originalProfile, jobDescription)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("trigger_optimization_button"),
                    enabled = isInputValid && !isOptimizing,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Analyze & Rewrite Profile",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Staggered Progressive Loading Overlay
            AnimatedVisibility(
                visible = isOptimizing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                        .clickable(enabled = false) {}, // absorb clicks
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 5.dp,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("optimization_progress")
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Analyzing & Refitting",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Animated sliding text for rotating messages
                        Box(
                            modifier = Modifier
                                .height(56.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = currentPhraseIndex,
                                transitionSpec = {
                                    slideInVertically { h -> h } + fadeIn() togetherWith
                                    slideOutVertically { h -> -h } + fadeOut()
                                },
                                label = "phrases"
                            ) { targetIndex ->
                                Text(
                                    text = loadingPhrases[targetIndex],
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
