package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {
    // --- Optimizations Queries ---
    @Query("SELECT * FROM resume_optimizations ORDER BY timestamp DESC")
    fun getAllOptimizations(): Flow<List<ResumeOptimization>>

    @Query("SELECT * FROM resume_optimizations WHERE id = :id LIMIT 1")
    suspend fun getOptimizationById(id: Int): ResumeOptimization?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptimization(optimization: ResumeOptimization): Long

    @Query("DELETE FROM resume_optimizations WHERE id = :id")
    suspend fun deleteOptimizationById(id: Int)

    // --- Coach Chat Messages Queries ---
    @Query("SELECT * FROM coach_chat_messages WHERE optimizationId = :optimizationId ORDER BY timestamp ASC")
    fun getChatHistoryByOptimizationId(optimizationId: Int): Flow<List<CoachChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: CoachChatMessage)

    @Query("DELETE FROM coach_chat_messages WHERE optimizationId = :optimizationId")
    suspend fun deleteChatHistoryByOptimizationId(optimizationId: Int)
}
