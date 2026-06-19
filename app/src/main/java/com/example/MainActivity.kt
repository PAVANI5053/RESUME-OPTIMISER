package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.ResumeDatabase
import com.example.data.repository.ResumeRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NewOptimizationScreen
import com.example.ui.screens.OptimizationDetailScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ResumeViewModel
import com.example.ui.viewmodel.ResumeViewModelFactory

enum class NavigationScreen {
    Dashboard,
    NewOptimization,
    OptimizationDetails
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Core Dependencies
        val database = ResumeDatabase.getDatabase(applicationContext)
        val repository = ResumeRepository(database)
        
        setContent {
            MyApplicationTheme {
                val viewModel: ResumeViewModel = viewModel(
                    factory = ResumeViewModelFactory(repository)
                )
                
                var currentScreen by remember { mutableStateOf(NavigationScreen.Dashboard) }
                
                // Reactive State flows
                val optimizations by viewModel.allOptimizations.collectAsState()
                val chatMessages by viewModel.chatHistory.collectAsState()
                
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Crossfade(
                        targetState = currentScreen,
                        label = "screen_crossfade"
                    ) { screen ->
                        when (screen) {
                            NavigationScreen.Dashboard -> {
                                DashboardScreen(
                                    optimizations = optimizations,
                                    onSelectOptimization = { item ->
                                        viewModel.selectOptimization(item)
                                        currentScreen = NavigationScreen.OptimizationDetails
                                    },
                                    onDeleteOptimization = { id ->
                                        viewModel.deleteOptimization(id)
                                    },
                                    onNavigateToNew = {
                                        currentScreen = NavigationScreen.NewOptimization
                                    }
                                )
                            }
                            NavigationScreen.NewOptimization -> {
                                NewOptimizationScreen(
                                    isOptimizing = viewModel.isOptimizing,
                                    errorMessage = viewModel.errorMessage,
                                    onOptimize = { title, originalProfile, jobDescription ->
                                        viewModel.optimizeResume(title, originalProfile, jobDescription) {
                                            currentScreen = NavigationScreen.OptimizationDetails
                                        }
                                    },
                                    onBack = {
                                        currentScreen = NavigationScreen.Dashboard
                                    }
                                )
                            }
                            NavigationScreen.OptimizationDetails -> {
                                val currentOp = viewModel.selectedOptimization
                                if (currentOp != null) {
                                    OptimizationDetailScreen(
                                        optimization = currentOp,
                                        optimizedResume = viewModel.selectedOptimizedResume,
                                        chatMessages = chatMessages,
                                        isSendingMessage = viewModel.isSendingChatMessage,
                                        onSendMessage = { text ->
                                            viewModel.sendCoachMessage(text)
                                        },
                                        onBack = {
                                            viewModel.selectOptimization(null)
                                            currentScreen = NavigationScreen.Dashboard
                                        }
                                    )
                                } else {
                                    currentScreen = NavigationScreen.Dashboard
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
