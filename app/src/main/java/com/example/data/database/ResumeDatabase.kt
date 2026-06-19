package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ResumeOptimization::class, CoachChatMessage::class], version = 1, exportSchema = false)
abstract class ResumeDatabase : RoomDatabase() {
    abstract fun resumeDao(): ResumeDao

    companion object {
        @Volatile
        private var INSTANCE: ResumeDatabase? = null

        fun getDatabase(context: Context): ResumeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ResumeDatabase::class.java,
                    "resume_optimizer_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
