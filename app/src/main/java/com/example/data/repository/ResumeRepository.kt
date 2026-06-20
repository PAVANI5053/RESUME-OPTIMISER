package com.example.data.repository

import com.example.BuildConfig
import com.example.data.database.CoachChatMessage
import com.example.data.database.ResumeDatabase
import com.example.data.database.ResumeOptimization
import com.example.data.model.OptimizedResume
import com.example.data.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ResumeRepository(private val db: ResumeDatabase) {
    private val dao = db.resumeDao()

    // --- Local DB Sync ---
    val allOptimizations: Flow<List<ResumeOptimization>> = dao.getAllOptimizations()

    fun getChatHistory(optimizationId: Int): Flow<List<CoachChatMessage>> = 
        dao.getChatHistoryByOptimizationId(optimizationId)

    suspend fun getOptimizationById(id: Int): ResumeOptimization? = withContext(Dispatchers.IO) {
        dao.getOptimizationById(id)
    }

    suspend fun saveOptimization(optimization: ResumeOptimization): Long = withContext(Dispatchers.IO) {
        dao.insertOptimization(optimization)
    }

    suspend fun deleteOptimization(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteOptimizationById(id)
        dao.deleteChatHistoryByOptimizationId(id)
    }

    suspend fun saveChatMessage(message: CoachChatMessage) = withContext(Dispatchers.IO) {
        dao.insertChatMessage(message)
    }

    // --- Gemini API Optimization Run ---
    suspend fun optimizeProfile(
        originalProfile: String,
        jobDescription: String,
        resumeTitle: String
    ): OptimizedResume = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API Key is not configured. Please add it via the Secrets panel.")
        }

        val systemInstructionText = """
            You are an expert career coach and professional resume writer. Your task is to optimize a user's professional profile to perfectly align with a target job description while maintaining absolute truthfulness.
            
            CRITICAL RESTRICTIONS:
            - DO NOT invent fake experiences, certifications, or metrics. Only reframe and polish existing data.
            - Output ONLY a valid JSON object matching the requested schema and nothing else. Do not wrap in extra commentary or text outside the JSON object.
        """.trimIndent()

        val userPrompt = """
            INPUTS PROVIDED:
            1. Target Job Title:
            [${resumeTitle}]

            2. User Profile Data / Current Resume:
            [${originalProfile}]
            
            3. Target Job Description:
            [${jobDescription}]

            INSTRUCTIONS:
            - Analyze the user's resume/profile specifically against the Target Job Title and the Target Job Description.
            - Identify core software, tools, languages, methodologies, or other critical technical skills required for this job title/role.
            - Provide a comprehensive list of missing technical skills or keywords under "missingKeywords" in "keywordsAnalysis" that are prominent in the Job Description or expected of a candidate with the Target Job Title, but are not present or highlighted in the user's current resume.
            - Rewrite experience bullet points to emphasize impact, using strong action verbs and metrics where possible, ensuring they align with the job's priorities.
            - Seamlessly integrate relevant keywords from the job description into the user's skills and experience sections to pass ATS (Applicant Tracking Systems) filters.
            - Maintain a professional, confident, and action-oriented tone.
            - Keep the original timeline, companies, and roles EXACTLY as they are. Reframe the achievements and descriptions honestly and powerfully.

            Format the output strictly as a JSON object with the exact following fields and structure:
            {
              "title": "Title of the optimized resume/profile (e.g., Senior Android Engineer - FinTech)",
              "score": 85,
              "summary": "Tailored, powerful elevator pitch summary for this job matching their real experience",
              "keywordsAnalysis": {
                "foundKeywords": ["list", "of", "keywords"],
                "missingKeywords": ["list", "of", "missing"],
                "requiredSkillsDetected": ["list", "of", "skills"]
              },
              "tailoredExperience": [
                {
                  "title": "Job Title",
                  "company": "Company Name",
                  "duration": "Dates",
                  "originalBullets": ["original sentences..."],
                  "tailoredBullets": ["tailored sentences with metrics and action verbs..."]
                }
              ],
              "tailoredSkills": ["skill1", "skill2"],
              "coachAdvice": {
                "gapAnalysis": "Strategic advice about gaps and preparation",
                "coverLetterOutline": "Structural plan for drafting a compelling cover letter",
                "interviewPrepQuestions": ["Question 1?", "Question 2?"],
                "actionableNextSteps": ["Step 1", "Step 2"]
              }
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = userPrompt)))
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructionText)))
        )

        try {
            val response = GeminiApiClient.service.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Empty response from AI")
            
            val sanitizedJson = sanitizeJson(rawText)
            val adapter = GeminiApiClient.moshi.adapter(OptimizedResume::class.java)
            adapter.fromJson(sanitizedJson) ?: throw IllegalStateException("Failed to parse optimized resume JSON")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    // --- Gemini API Coaching Chat Response ---
    suspend fun getCoachResponse(
        originalProfile: String,
        jobDescription: String,
        chatHistory: List<CoachChatMessage>,
        newUserMessage: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Gemini API Key is not configured. Config it in AI Studio first."
        }

        val systemInstructionText = """
            You are an expert career coach and interview writer. You are answering questions from a job applicant who is targeting a specific job description.
            You must provide outstanding coaching, interview preparation advice, tailored cover letters, and resume modification tips.
            
            CRITICAL RULES:
            - Stick strictly to the truth of their provided profile.
            - Be professional, highly encouraging, strategic, and practical.
            - Answer questions concisely and elegantly, using clear bullet points where helpful.
        """.trimIndent()

        // Construct history contents
        val contents = mutableListOf<GeminiContent>()
        
        // Context setup
        val contextPrompt = """
            Here are the target job details we are working with:
            
            USER PROFILE:
            $originalProfile
            
            TARGET JOB DESCRIPTION:
            $jobDescription
            
            We are now in our coaching session. Respond to my messages based on this context.
        """.trimIndent()
        
        contents.add(GeminiContent(parts = listOf(GeminiPart(text = contextPrompt))))
        contents.add(GeminiContent(parts = listOf(GeminiPart(text = "Understood. I am ready to help you optimize your approach for this position. How can I help you today?"))))

        // History turns
        chatHistory.forEach { chat ->
            contents.add(GeminiContent(parts = listOf(GeminiPart(text = chat.message))))
        }

        // New turn
        contents.add(GeminiContent(parts = listOf(GeminiPart(text = newUserMessage))))

        val request = GeminiRequest(
            contents = contents,
            generationConfig = GeminiGenerationConfig(temperature = 0.7f),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructionText)))
        )

        try {
            val response = GeminiApiClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I was unable to formulate a response. Please try again."
        } catch (e: Exception) {
            "Error from AI Coach: ${e.localizedMessage ?: e.message}"
        }
    }

    private fun sanitizeJson(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substringAfter("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substringBeforeLast("```")
        }
        return cleaned.trim()
    }
}
