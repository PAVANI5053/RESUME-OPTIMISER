package com.example.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.CoachChatMessage
import com.example.data.database.ResumeOptimization
import com.example.data.model.OptimizedResume
import com.example.data.repository.ResumeRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ResumeViewModel(private val repository: ResumeRepository) : ViewModel() {

    // --- State Stream of all previous optimizations ---
    val allOptimizations: StateFlow<List<ResumeOptimization>> = repository.allOptimizations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Selected state details ---
    var selectedOptimization by mutableStateOf<ResumeOptimization?>(null)
        private set

    var selectedOptimizedResume by mutableStateOf<OptimizedResume?>(null)
        private set

    // --- Chat messages state flow ---
    private val _chatHistory = MutableStateFlow<List<CoachChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<CoachChatMessage>> = _chatHistory.asStateFlow()

    // --- Operation statuses ---
    var isOptimizing by mutableStateOf(false)
        private set

    var isSendingChatMessage by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val resumeAdapter = moshi.adapter(OptimizedResume::class.java)

    private var isSeeding = false

    init {
        viewModelScope.launch {
            repository.allOptimizations.collect { list ->
                if (list.isEmpty() && !isSeeding) {
                    isSeeding = true
                    try {
                        seedDefaultOptimization()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private suspend fun seedDefaultOptimization() {
        val defaultJson = """
        {
          "title": "Senior Android Architect - FinTech Core",
          "score": 94,
          "summary": "Accomplished Senior Android Architect with a proven track record of boosting app responsiveness by 40% and deploying offline-first DB synchronization models. Specialized in constructing responsive Jetpack Compose structures conforming to strict Material Design 3 guidelines and integrating low-latency live streams.",
          "keywordsAnalysis": {
            "foundKeywords": ["Android SDK", "Kotlin", "Room Database", "Jetpack Compose", "Retrofit", "MVVM Architecture"],
            "missingKeywords": ["Offline-First Cache", "Material 3 Design System", "High-frequency streaming optimizations"],
            "requiredSkillsDetected": ["Multi-threaded performance instrumentation", "Stateflow lifecycle preservation", "Custom adaptive canvas layout structures"]
          },
          "tailoredExperience": [
            {
              "title": "Android Platform Engineer",
              "company": "StackLabs Inc.",
              "duration": "2023 - Present",
              "originalBullets": [
                "Worked on the mobile banking Android application with a small team.",
                "Created nice UI layouts in Compose and added a database layer with Room.",
                "Refactored Retrofit api response parsing for speed."
              ],
              "tailoredBullets": [
                "Architected scalable, fluid Jetpack Compose templates for mobile app, reducing rendering recomposition cycles by 35% on low-end test terminals.",
                "Engineered robust offline-first caching mechanism leveraging Room local database persistence, boosting data availability and reducing backend network load by 50%.",
                "Streamlined Retrofit interceptors and data model serialization, accelerating network layer response processing speed by 25%."
              ]
            }
          ],
          "tailoredSkills": ["Kotlin", "Jetpack Compose", "Coroutines Flow", "Room Database Engine", "Performance Audit Profiling", "Material Design 3", "System API Instrumentation", "Type-safe Client Sync"],
          "coachAdvice": {
            "gapAnalysis": "Your core Android expertise is exceptionally strong. To secure an 'Excellent Alignment' rating at Apex Tech, highlight your hands-on experience with real-time UI render benchmarking and low-latency thread confinement.",
            "coverLetterOutline": "1. Hook: Introduce your direct experience optimizing complex commercial fintech dashboards.\n2. Body: Concrete stories on Room DB caching architecture and multi-threaded rendering performance under pressure.\n3. Close: Align with Apex Tech's high-performance offline standards.",
            "interviewPrepQuestions": [
              "Can you explain how you optimized recombinant states in Jetpack Compose to improve screen rendering of heavy finance streams?",
              "How did you isolate Room transactions from the main UI thread, and what dispatchers did you use?"
            ],
            "actionableNextSteps": [
              "Navigate to the Career Coach tab and type 'Give me a mock interview' to practice these precise questions.",
              "Copy the refitted bullet points under the Refitted Resume tab to enhance your active profile."
            ]
          }
        }
        """.trimIndent()

        val seedOp = ResumeOptimization(
            resumeTitle = "Senior Android Architect - Apex Tech Fit",
            originalProfile = "Dev Pavani - Android Specialist with MVVM, Room and Compose experience.",
            targetJobDescription = "Senior App developer with offline Room caching, low latency parsing, & Material 3 guidelines.",
            optimizedResumeJson = defaultJson,
            score = 94
        )
        val newId = repository.saveOptimization(seedOp)
        
        val welcomeMsg = CoachChatMessage(
            optimizationId = newId.toInt(),
            sender = "COACH",
            message = "Welcome Dev Pavani! 🚀 I have thoroughly analyzed your Senior Android Architect profile and compared it against the target position at Apex Tech Holdings.\n\nWe achieved a standard ATS Fit Score of 94%!\n\nHere are the top three pillars of your preparation today:\n1. **State Management**: Be ready to discuss live performance and avoiding state allocation during draw scopes.\n2. **Offline Resilience**: Expect detailed scenarios regarding SQLite thread confinement and transaction execution.\n3. **Component Adaptability**: Highlight fluid transition constraints.\n\nTap any trigger option below to start roleplaying or get a complete cover letter immediately!"
        )
        repository.saveChatMessage(welcomeMsg)
    }

    fun selectOptimization(optimization: ResumeOptimization?) {
        selectedOptimization = optimization
        if (optimization != null) {
            try {
                selectedOptimizedResume = resumeAdapter.fromJson(optimization.optimizedResumeJson)
            } catch (e: Exception) {
                selectedOptimizedResume = null
                errorMessage = "Failed to parse saved resume details: ${e.message}"
            }
            // Load associated chat
            viewModelScope.launch {
                repository.getChatHistory(optimization.id).collect { messages ->
                    _chatHistory.value = messages
                }
            }
        } else {
            selectedOptimizedResume = null
            _chatHistory.value = emptyList()
        }
    }

    fun optimizeResume(title: String, originalProfile: String, jobDescription: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                isOptimizing = true
                errorMessage = null
                
                // Call Gemini to generate the optimized resume JSON
                val optimized = repository.optimizeProfile(originalProfile, jobDescription, title)
                val jsonText = resumeAdapter.toJson(optimized)
                
                // Save to Room DB
                val newOp = ResumeOptimization(
                    resumeTitle = title.ifBlank { optimized.title.ifBlank { "Optimized Resume Profile" } },
                    originalProfile = originalProfile,
                    targetJobDescription = jobDescription,
                    optimizedResumeJson = jsonText,
                    score = optimized.score
                )
                val newId = repository.saveOptimization(newOp)
                
                // Refresh and select
                val savedOptimization = newOp.copy(id = newId.toInt())
                selectOptimization(savedOptimization)
                
                isOptimizing = false
                onSuccess()
            } catch (e: Exception) {
                isOptimizing = false
                errorMessage = "Optimization failed: ${e.localizedMessage ?: e.message}"
            }
        }
    }

    fun deleteOptimization(id: Int) {
        viewModelScope.launch {
            try {
                if (selectedOptimization?.id == id) {
                    selectOptimization(null)
                }
                repository.deleteOptimization(id)
            } catch (e: Exception) {
                errorMessage = "Failed to delete item: ${e.localizedMessage ?: e.message}"
            }
        }
    }

    fun sendCoachMessage(messageText: String) {
        val currentOp = selectedOptimization ?: return
        if (messageText.isBlank()) return

        viewModelScope.launch {
            try {
                isSendingChatMessage = true
                
                // 1. Insert user message to database
                val userMsg = CoachChatMessage(
                    optimizationId = currentOp.id,
                    sender = "USER",
                    message = messageText
                )
                repository.saveChatMessage(userMsg)

                // 2. Query Gemini for Coach response
                val reply = repository.getCoachResponse(
                    originalProfile = currentOp.originalProfile,
                    jobDescription = currentOp.targetJobDescription,
                    chatHistory = _chatHistory.value,
                    newUserMessage = messageText
                )

                // 3. Insert Coach response to database
                val coachMsg = CoachChatMessage(
                    optimizationId = currentOp.id,
                    sender = "COACH",
                    message = reply
                )
                repository.saveChatMessage(coachMsg)
                
                isSendingChatMessage = false
            } catch (e: Exception) {
                isSendingChatMessage = false
                val errorMsg = CoachChatMessage(
                    optimizationId = currentOp.id,
                    sender = "COACH",
                    message = "Sorry, I ran into an error generating a response: ${e.localizedMessage ?: e.message}"
                )
                repository.saveChatMessage(errorMsg)
            }
        }
    }
}

class ResumeViewModelFactory(private val repository: ResumeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResumeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ResumeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
