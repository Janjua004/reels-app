package com.example.reels.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Manages session time tracking and enforcement.
 * Monitors active time in the app and provides warning when approaching limit.
 */
class SessionManager(
    private val context: Context,
    private val repository: SettingsRepository,
    private val scope: CoroutineScope
) {
    // Session state
    private var sessionStartTime: Long = 0
    private var elapsedTimeMs: Long = 0
    private var sessionTimerJob: Job? = null
    
    // Public state for UI
    var remainingTimeMinutes by mutableStateOf(0)
        private set
    
    var sessionLimitExceeded by mutableStateOf(false)
        private set
        
    var showTimeWarning by mutableStateOf(false)
        private set
    
    // Initialize the session manager
    init {
        sessionStartTime = System.currentTimeMillis()
        startSessionTimer()
    }
    
    // Start tracking session time
    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = scope.launch {
            while (isActive) {
                val settings = repository.settingsFlow.first()
                if (settings.sessionTimeEnabled) {
                    val currentTime = System.currentTimeMillis()
                    elapsedTimeMs = currentTime - sessionStartTime
                    
                    // Convert to minutes for easier tracking
                    val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTimeMs).toInt()
                    val maxSessionTime = settings.maxSessionTime
                    
                    remainingTimeMinutes = maxOf(0, maxSessionTime - elapsedMinutes)
                    
                    // Check if we should show a warning (when 5 minutes remaining)
                    showTimeWarning = remainingTimeMinutes <= 5 && remainingTimeMinutes > 0
                    
                    // Check if session time is exceeded
                    sessionLimitExceeded = elapsedMinutes >= maxSessionTime
                }
                
                delay(15000) // Check every 15 seconds to avoid too frequent updates
            }
        }
    }
    
    // Reset session timer (e.g., after parent enters PIN)
    fun resetSession() {
        sessionStartTime = System.currentTimeMillis()
        elapsedTimeMs = 0
        sessionLimitExceeded = false
        showTimeWarning = false
    }
    
    // Pause time tracking (e.g., when app goes to background)
    fun pauseSession() {
        sessionTimerJob?.cancel()
    }
    
    // Resume time tracking (e.g., when app comes to foreground)
    fun resumeSession() {
        startSessionTimer()
    }
    
    // Clean up resources
    fun cleanup() {
        sessionTimerJob?.cancel()
    }
}