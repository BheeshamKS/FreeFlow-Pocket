package com.bheeshamks.freeflow.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bheeshamks.freeflow.core.audio.AudioCaptureManager
import com.bheeshamks.freeflow.core.data.db.HistoryDatabase
import com.bheeshamks.freeflow.core.data.model.KeyboardSettings
import com.bheeshamks.freeflow.core.data.prefs.ThemeAndGoodiesPreferences
import com.bheeshamks.freeflow.core.network.GroqWhisperClient
import com.bheeshamks.freeflow.core.security.ApiKeyStore
import com.bheeshamks.freeflow.ui.navigation.Screen
import com.bheeshamks.freeflow.ui.screens.HomeScreen
import com.bheeshamks.freeflow.ui.screens.HistoryScreen
import com.bheeshamks.freeflow.ui.screens.OnboardingScreen
import com.bheeshamks.freeflow.ui.screens.SettingsScreen
import com.bheeshamks.freeflow.ui.theme.DarkBackground
import com.bheeshamks.freeflow.ui.theme.DarkCard
import com.bheeshamks.freeflow.ui.theme.FreeFlowTheme
import com.bheeshamks.freeflow.ui.theme.PrimaryBlue
import com.bheeshamks.freeflow.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Main Application Activity for FreeFlow Pocket.
 * Manages first-run Onboarding flow and the 3 core navigation tabs:
 * 1. Home (Voice Notepad)
 * 2. History (Local Dictation Archive)
 * 3. Settings (Groq API, Floating Bubble, Quick Tile, Keys Cafe Keyboard)
 */
class MainActivity : ComponentActivity() {

    private lateinit var apiKeyStore: ApiKeyStore
    private lateinit var preferences: ThemeAndGoodiesPreferences
    private lateinit var groqClient: GroqWhisperClient
    private lateinit var historyDb: HistoryDatabase
    private lateinit var audioCaptureManager: AudioCaptureManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        apiKeyStore = ApiKeyStore(this)
        preferences = ThemeAndGoodiesPreferences(this)
        groqClient = GroqWhisperClient(apiKeyProvider = { apiKeyStore.getApiKey() })
        historyDb = HistoryDatabase.getInstance(this)
        audioCaptureManager = AudioCaptureManager()

        setContent {
            FreeFlowTheme {
                val scope = rememberCoroutineScope()
                val settings by preferences.settingsFlow.collectAsState(initial = KeyboardSettings())

                AnimatedContent(
                    targetState = settings.isOnboardingCompleted,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "OnboardingMainTransition"
                ) { isOnboardingDone ->
                    if (!isOnboardingDone) {
                        OnboardingScreen(
                            apiKeyStore = apiKeyStore,
                            groqClient = groqClient,
                            onCompleteOnboarding = {
                                scope.launch {
                                    preferences.setOnboardingCompleted(true)
                                }
                            }
                        )
                    } else {
                        val navController = rememberNavController()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkBackground),
                            bottomBar = {
                                NavigationBar(
                                    containerColor = DarkCard,
                                    tonalElevation = 8.dp
                                ) {
                                    Screen.items.forEach { screen ->
                                        val selected = currentRoute == screen.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                if (currentRoute != screen.route) {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = screen.icon,
                                                    contentDescription = screen.title
                                                )
                                            },
                                            label = {
                                                Text(text = screen.title)
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color.Black,
                                                selectedTextColor = PrimaryBlue,
                                                indicatorColor = PrimaryBlue,
                                                unselectedIconColor = TextSecondary,
                                                unselectedTextColor = TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = Screen.Home.route,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                composable(Screen.Home.route) {
                                    HomeScreen(
                                        apiKeyStore = apiKeyStore,
                                        preferences = preferences,
                                        groqClient = groqClient,
                                        audioCaptureManager = audioCaptureManager,
                                        historyDb = historyDb,
                                        onNavigateToSettings = {
                                            navController.navigate(Screen.Settings.route)
                                        }
                                    )
                                }

                                composable(Screen.History.route) {
                                    HistoryScreen(
                                        dictationDao = historyDb.dictationDao()
                                    )
                                }

                                composable(Screen.Settings.route) {
                                    SettingsScreen(
                                        apiKeyStore = apiKeyStore,
                                        preferences = preferences,
                                        groqClient = groqClient,
                                        onReplayOnboarding = {
                                            scope.launch {
                                                preferences.setOnboardingCompleted(false)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
