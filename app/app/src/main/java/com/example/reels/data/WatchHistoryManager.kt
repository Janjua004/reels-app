package com.example.reels.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.reels.settings.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Manages video watch history tracking
 */
class WatchHistoryManager(private val context: Context) {
    
    companion object {
        private val LAST_WATCHED_VIDEOS_KEY = stringSetPreferencesKey("last_watched_videos")
        private val WATCH_COUNT_PREFIX = "watch_count_"
        private val LAST_WATCHED_TIME_PREFIX = "last_watched_time_"
        private val MAX_HISTORY_ITEMS = 50
    }

    /**
     * Get the list of recently watched video URIs
     */
    val recentlyWatchedVideos: Flow<List<String>> = context.dataStore.data
        .map { prefs ->
            prefs[LAST_WATCHED_VIDEOS_KEY]?.toList() ?: emptyList()
        }
    
    /**
     * Record that a video was watched
     */
    suspend fun recordVideoWatched(videoUri: Uri) {
        val videoId = videoUri.toString()
        
        context.dataStore.edit { prefs ->
            // Get the current list or create empty list
            val currentList = prefs[LAST_WATCHED_VIDEOS_KEY]?.toMutableList() ?: mutableListOf()
            
            // Remove this video if it already exists (to add it back at the top)
            currentList.remove(videoId)
            
            // Add this video at the beginning
            currentList.add(0, videoId)
            
            // Trim if needed
            if (currentList.size > MAX_HISTORY_ITEMS) {
                currentList.subList(MAX_HISTORY_ITEMS, currentList.size).clear()
            }
            
            // Save updated list
            prefs[LAST_WATCHED_VIDEOS_KEY] = currentList.toSet()
            
            // Increment watch count
            val countKey = longPreferencesKey("$WATCH_COUNT_PREFIX$videoId")
            val currentCount = prefs[countKey] ?: 0L
            prefs[countKey] = currentCount + 1
            
            // Update last watched time
            val timeKey = stringPreferencesKey("$LAST_WATCHED_TIME_PREFIX$videoId")
            prefs[timeKey] = Instant.now().toString()
        }
    }
    
    /**
     * Get watch count for a specific video
     */
    suspend fun getWatchCount(videoUri: Uri): Long {
        val videoId = videoUri.toString()
        val countKey = longPreferencesKey("$WATCH_COUNT_PREFIX$videoId")
        
        return context.dataStore.data.map { prefs ->
            prefs[countKey] ?: 0L
        }.firstOrNull() ?: 0L
    }
    
    /**
     * Clear all watch history
     */
    suspend fun clearWatchHistory() {
        context.dataStore.edit { prefs ->
            // Get all keys that match our prefixes and remove them
            val keysToRemove = prefs.asMap().keys.filter { key ->
                key.name.startsWith(WATCH_COUNT_PREFIX) || 
                key.name.startsWith(LAST_WATCHED_TIME_PREFIX)
            }
            
            keysToRemove.forEach { key ->
                prefs.remove(key)
            }
            
            // Clear the recent video list
            prefs.remove(LAST_WATCHED_VIDEOS_KEY)
        }
    }
}