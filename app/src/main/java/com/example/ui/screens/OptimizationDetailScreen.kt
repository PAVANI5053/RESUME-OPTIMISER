package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.CoachChatMessage
import com.example.data.database.ResumeOptimization
import com.example.data.model.OptimizedResume
import com.example.data.model.TailoredExperienceItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizationDetailScreen(
    optimization: ResumeOptimization,
    optimizedResume: OptimizedResume?,
    chatMessages: List<CoachChatMessage>,
    isSendingMessage: Boolean,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("ATS Audit", "Refitted Resume", "Career Coach")
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = optimization.resumeTitle,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "Score: ${optimization.score}% ATS Match",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val resumeStr = buildGlobalCopyString(optimizedResume, optimization)
                            copyToClipboard(context, "Optimized Resume", resumeStr)
                        },
                        modifier = Modifier.testTag("global_share_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Copy whole optimized text")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            // Elegant scrolling primary tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.testTag("detail_tab_$index")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (optimizedResume == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Assembling resume segments...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                    }
                } else {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "tabContent"
                    ) { targetTab ->
                        when (targetTab) {
                            0 -> AtsAuditTab(optimizedResume, optimization)
                            1 -> RefittedResumeTab(optimizedResume, context)
                            2 -> CareerCoachTab(chatMessages, isSendingMessage, onSendMessage, context)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AtsAuditTab(resume: OptimizedResume, optimization: ResumeOptimization) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Meter score representation card
        item {
            ScoreMeterComponent(resume.score)
        }

        // Executive gap analysis
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Expert Coach Briefing",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = resume.coachAdvice.gapAnalysis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Found vs Missing Keywords Section
        item {
            Text(
                text = "ATS Keyword Alignment",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Success Found list
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Matched Keywords (${resume.keywordsAnalysis.foundKeywords.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                    
                    if (resume.keywordsAnalysis.foundKeywords.isEmpty()) {
                        Text(
                            text = "No matched keywords extracted initially under this selection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 32.dp, top = 4.dp, bottom = 12.dp)
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            resume.keywordsAnalysis.foundKeywords.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.1f))
                                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = tag, fontSize = 12.sp, color = Color(0xFF0F766E), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Missing Keywords warning
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unmatched Keyword Gaps (${resume.keywordsAnalysis.missingKeywords.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                    
                    if (resume.keywordsAnalysis.missingKeywords.isEmpty()) {
                        Text(
                            text = "Spectacular! Complete keyword representation match.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 32.dp, top = 4.dp, bottom = 12.dp)
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            resume.keywordsAnalysis.missingKeywords.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.1f))
                                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = tag, fontSize = 12.sp, color = Color(0xFFB45309), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Immediate checkpoint tasks
        item {
            Text(
                text = "Next Tactical Milestones",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(resume.coachAdvice.actionableNextSteps) { step ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreMeterComponent(score: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Overall Matching Compatibility",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Beautiful concentric dial/progress simulation
            val progressBrush = Brush.sweepGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primaryContainer
                )
            )

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow border circle
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .border(
                            width = 6.dp,
                            brush = progressBrush,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$score%",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "ATS FIT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val ratingText = when {
                score >= 80 -> "EXCELLENT ALIGNMENT"
                score >= 60 -> "GOOD ALIGNMENT"
                else -> "CRITICAL SKILL GAP"
            }
            val ratingColor = when {
                score >= 80 -> Color(0xFF10B981)
                score >= 60 -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }

            Text(
                text = ratingText,
                color = ratingColor,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun RefittedResumeTab(resume: OptimizedResume, context: Context) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Tagline Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ATS Executive Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    copyToClipboard(context, "Executive Summary", resume.summary)
                                }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = resume.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Skills segment
        item {
            Text(
                text = "Tailored Competencies",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        resume.tailoredSkills.forEach { skill ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = skill,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bullet refittings side by side comparisons
        item {
            Text(
                text = "Impact Experience Refiner",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (resume.tailoredExperience.isEmpty()) {
            item {
                Text(
                    text = "No history logged found in resume entries.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(resume.tailoredExperience) { experience ->
                ExperienceRefitItem(experience, context)
            }
        }
    }
}

@Composable
fun ExperienceRefitItem(experience: TailoredExperienceItem, context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = experience.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${experience.company} • ${experience.duration}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = {
                        val buildStr = buildString {
                            appendLine("${experience.title} at ${experience.company} (${experience.duration})")
                            experience.tailoredBullets.forEach { bullet ->
                                appendLine("- $bullet")
                            }
                        }
                        copyToClipboard(context, "Job Experience Details", buildStr)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Copy Experience",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compare Old vs New Bullet blocks
            experience.tailoredBullets.forEachIndexed { index, tailoredBullet ->
                val originalBullet = experience.originalBullets.getOrNull(index) ?: ""

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    // Original Baseline Row (If present)
                    if (originalBullet.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "ORIGINAL:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                modifier = Modifier.width(70.dp)
                            )
                            Text(
                                text = originalBullet,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Refitted Impact Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.1f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "REFITTED:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F766E),
                            modifier = Modifier.width(70.dp)
                        )
                        Text(
                            text = tailoredBullet,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CareerCoachTab(
    chatMessages: List<CoachChatMessage>,
    isSendingMessage: Boolean,
    onSendMessage: (String) -> Unit,
    context: Context
) {
    var chatText by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Follow up chips
    val triggers = listOf(
        "Draft me a tailored Cover Letter",
        "Give me a mock interview",
        "How do I explain my gaps?",
        "Refine my LinkedIn Headline"
    )

    // Scroll to bottom when list changes
    LaunchedEffect(chatMessages.size, isSendingMessage) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Continuous Conversation Flow
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CoachIntroCard()
            }

            items(chatMessages, key = { it.id }) { msg ->
                val isUser = msg.sender == "USER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 0.dp,
                                    bottomEnd = if (isUser) 0.dp else 16.dp
                                )
                            )
                            .background(
                                if (isUser) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (isUser) 0.dp else 1.dp,
                                color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 0.dp,
                                    bottomEnd = if (isUser) 0.dp else 16.dp
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            // Support text copy on tap of AI coach message so user can save cover letters or plans easily!
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUser) "You" else "Career Coach AI",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.primary
                                )
                                if (!isUser) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Copy message",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable {
                                                copyToClipboard(context, "Coach Response", msg.message)
                                            }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            if (isSendingMessage) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Coach is thinking...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick trigger Chips lists
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            triggers.forEach { phrase ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .clickable(enabled = !isSendingMessage) {
                            onSendMessage(phrase)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = phrase,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Message input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatText,
                onValueChange = { chatText = it },
                placeholder = { Text("Ask Coach (e.g. Can you make me an outline?)") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("coach_chat_input"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (chatText.isNotBlank() && !isSendingMessage) {
                        onSendMessage(chatText)
                        chatText = ""
                    }
                })
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (chatText.isNotBlank() && !isSendingMessage) {
                        onSendMessage(chatText)
                        chatText = ""
                    }
                },
                enabled = chatText.isNotBlank() && !isSendingMessage,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (chatText.isNotBlank() && !isSendingMessage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                    )
                    .testTag("send_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message",
                    tint = if (chatText.isNotBlank() && !isSendingMessage) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CoachIntroCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Ready to Prep?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "I have scanned your resume and target job requirements. Tap the preset chips below to instantly auto-generate a tailored cover letter, conduct a mock roleplay interview, or analyze resume structures.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Global Help functions
private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun buildGlobalCopyString(resume: OptimizedResume?, optimization: ResumeOptimization): String {
    if (resume == null) return ""
    return buildString {
        appendLine("TITLE: ${resume.title}")
        appendLine("ATS SCORE: ${resume.score}% Match")
        appendLine()
        appendLine("EXECUTIVE SUMMARY:")
        appendLine(resume.summary)
        appendLine()
        appendLine("SKILLS GAPS DETECTED:")
        appendLine("Found Keywords: ${resume.keywordsAnalysis.foundKeywords.joinToString(", ")}")
        appendLine("Missing Keywords: ${resume.keywordsAnalysis.missingKeywords.joinToString(", ")}")
        appendLine("Required Corporate Skills: ${resume.keywordsAnalysis.requiredSkillsDetected.joinToString(", ")}")
        appendLine()
        appendLine("REFITTED EXPERIENCE PROFILE:")
        resume.tailoredExperience.forEach { expr ->
            appendLine("--- ${expr.title} at ${expr.company} (${expr.duration}) ---")
            expr.tailoredBullets.forEach { bullet ->
                appendLine("- $bullet")
            }
            appendLine()
        }
        appendLine("TAILORED COMPETENCIES:")
        appendLine(resume.tailoredSkills.joinToString(", "))
        appendLine()
        appendLine("COACH ADVICE BRIEFING:")
        appendLine(resume.coachAdvice.gapAnalysis)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
