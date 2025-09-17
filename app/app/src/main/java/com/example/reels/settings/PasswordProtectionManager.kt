package com.example.reels.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

/**
 * Manages PIN-based protection for settings access.
 * Uses simple SHA-256 hash for PIN storage.
 */
class PasswordProtectionManager(private val context: Context) {

    private val PIN_KEY = stringPreferencesKey("settings_pin_hash")
    
    // Check if a PIN has been set
    val hasPinSet: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[PIN_KEY].isNullOrEmpty()
    }
    
    // Validate entered PIN against stored hash
    suspend fun verifyPin(enteredPin: String): Boolean {
        val storedHash = context.dataStore.data.map { prefs ->
            prefs[PIN_KEY] ?: ""
        }.firstOrNull() ?: return false
        
        return hashPin(enteredPin) == storedHash
    }
    
    // Set a new PIN (or change existing)
    suspend fun setPin(newPin: String) {
        context.dataStore.edit { prefs ->
            prefs[PIN_KEY] = hashPin(newPin)
        }
    }
    
    // Remove PIN protection
    suspend fun clearPin() {
        context.dataStore.edit { prefs ->
            prefs.remove(PIN_KEY)
        }
    }
    
    // Simple hash function for PIN (not for high security, but better than plaintext)
    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}