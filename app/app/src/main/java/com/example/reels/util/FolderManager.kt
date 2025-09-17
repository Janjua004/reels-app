package com.example.reels.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Helper class for creating and managing the app's folder structure.
 * Creates necessary directories and templates for the app to function.
 */
class FolderManager(private val context: Context) {

    companion object {
        private const val TAG = "FolderManager"
        
        // Default whitelist template content
        private const val DEFAULT_WHITELIST_CONTENT = """# Whitelist for Reels App
# Add video name fragments below, one per line
# Only videos containing these words/phrases will be shown
# Example:
#
# kids
# educational
# animal
# cartoon
"""
    }
    
    /**
     * Create default folders for the app in common storage locations.
     * @param folderName The name of the folder to create
     * @return The created folder or null if creation failed
     */
    fun createDefaultFolders(folderName: String): File? {
        val locations = listOf(
            // Try common locations in order of preference
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStorageDirectory()
        )
        
        // Try to create in each location until success
        for (baseDir in locations) {
            val targetDir = File(baseDir, folderName)
            if (createFolderWithTemplate(targetDir)) {
                return targetDir
            }
        }
        
        // If we couldn't create in common locations, try app-specific storage
        val appDir = File(context.getExternalFilesDir(null), folderName)
        if (createFolderWithTemplate(appDir)) {
            return appDir
        }
        
        return null
    }
    
    /**
     * Check if default folder exists in any common location
     * @param folderName The name of the folder to check
     * @return The folder if it exists, null otherwise
     */
    fun findExistingFolder(folderName: String): File? {
        val locations = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStorageDirectory(),
            context.getExternalFilesDir(null)
        )
        
        for (baseDir in locations) {
            val targetDir = File(baseDir, folderName)
            if (targetDir.exists() && targetDir.isDirectory) {
                return targetDir
            }
        }
        
        return null
    }
    
    /**
     * Create a folder and add template files
     * @param folder The folder to create
     * @return true if created successfully, false otherwise
     */
    private fun createFolderWithTemplate(folder: File): Boolean {
        try {
            if (folder.exists() || folder.mkdirs()) {
                // Create whitelist.txt template if it doesn't exist
                val whitelistFile = File(folder, "whitelist.txt")
                if (!whitelistFile.exists()) {
                    try {
                        FileOutputStream(whitelistFile).use { outputStream ->
                            outputStream.write(DEFAULT_WHITELIST_CONTENT.toByteArray())
                        }
                        Log.d(TAG, "Created whitelist.txt template in ${folder.absolutePath}")
                    } catch (e: IOException) {
                        Log.e(TAG, "Failed to create whitelist.txt: ${e.message}")
                    }
                }
                
                // Create a .nomedia file to prevent media scanners from indexing this folder
                // (remove this if you want these videos to appear in the gallery)
                /*
                val nomediaFile = File(folder, ".nomedia")
                if (!nomediaFile.exists()) {
                    try {
                        nomediaFile.createNewFile()
                    } catch (e: IOException) {
                        Log.e(TAG, "Failed to create .nomedia file: ${e.message}")
                    }
                }
                */
                
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating folder ${folder.absolutePath}: ${e.message}")
        }
        
        return false
    }
    
    /**
     * Creates a sample text file explaining how to use the app
     * @param folder The folder to create the file in
     */
    fun createHelpFile(folder: File) {
        val helpFile = File(folder, "READ_ME_REELS_APP.txt")
        
        try {
            val helpContent = """
                |REELS APP - GETTING STARTED
                |==========================
                |
                |Thank you for installing the Reels App!
                |
                |This folder is where you should place your video files for the app to find them.
                |
                |QUICK START:
                |1. Copy your video files to this folder (MP4 format works best)
                |2. Launch the Reels App
                |3. The app will automatically find and play videos from this folder
                |
                |CONTENT FILTERING:
                |* The whitelist.txt file in this folder allows you to filter which videos are shown
                |* Edit this file to add keywords (one per line)
                |* Only videos with filenames containing these words will be shown
                |* This helps ensure only appropriate content is viewed
                |
                |SETTINGS:
                |* Child Mode: Simplified interface for kids
                |* Screen Time Limits: Control how long the app can be used
                |* PIN Protection: Lock settings behind a parent PIN
                |
                |SUPPORTED VIDEO FORMATS:
                |* MP4 (recommended)
                |* MKV
                |* WebM
                |* Other formats supported by ExoPlayer
                |
                |For more help, see the app settings.
                """.trimMargin()
                
            FileOutputStream(helpFile).use { outputStream ->
                outputStream.write(helpContent.toByteArray())
            }
            Log.d(TAG, "Created help file in ${folder.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create help file: ${e.message}")
        }
    }
}