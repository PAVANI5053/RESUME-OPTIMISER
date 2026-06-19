package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OptimizedResume(
    val title: String = "",
    val score: Int = 0,
    val summary: String = "",
    val keywordsAnalysis: KeywordAnalysis = KeywordAnalysis(),
    val tailoredExperience: List<TailoredExperienceItem> = emptyList(),
    val tailoredSkills: List<String> = emptyList(),
    val coachAdvice: CoachAdvice = CoachAdvice()
)

@JsonClass(generateAdapter = true)
data class KeywordAnalysis(
    val foundKeywords: List<String> = emptyList(),
    val missingKeywords: List<String> = emptyList(),
    val requiredSkillsDetected: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TailoredExperienceItem(
    val title: String = "",
    val company: String = "",
    val duration: String = "",
    val originalBullets: List<String> = emptyList(),
    val tailoredBullets: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CoachAdvice(
    val gapAnalysis: String = "",
    val coverLetterOutline: String = "",
    val interviewPrepQuestions: List<String> = emptyList(),
    val actionableNextSteps: List<String> = emptyList()
)
