package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resume_optimizations")
data class ResumeOptimization(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val resumeTitle: String,
    val originalProfile: String,
    val targetJobDescription: String,
    val optimizedResumeJson: String,  // Serialized OptimizedResume
    val score: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "coach_chat_messages")
data class CoachChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val optimizationId: Int,
    val sender: String, // "USER" or "COACH"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
