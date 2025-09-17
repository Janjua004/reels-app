package com.example.reels.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages video thumbnail generation, caching, and retrieval
 */
class ThumbnailManager(private val context: Context) {
    // In-memory cache for fast retrieval
    private val memoryCache: LruCache<String, Bitmap>
    
    // Track ongoing generation tasks to avoid duplicate work
    private val ongoingTasks = ConcurrentHashMap<String, Boolean>()
    
    init {
        // Use 1/8th of available memory for this cache
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                // Size in kilobytes
                return bitmap.byteCount / 1024
            }
        }
        
        // Create thumbnail directory if it doesn't exist
        createThumbnailDirectory()
    }
    
    private fun createThumbnailDirectory() {
        val dir = getThumbnailDirectory()
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }
    
    private fun getThumbnailDirectory(): File {
        return File(context.filesDir, "thumbnails")
    }
    
    private fun getFileForUri(uri: Uri): File {
        // Create a filename based on the hash of the URI
        val fileName = "thumb_${uri.toString().hashCode()}.jpg"
        return File(getThumbnailDirectory(), fileName)
    }
    
    /**
     * Gets a thumbnail for the given video URI, generating and caching if needed
     * Returns null if thumbnail couldn't be generated
     */
    suspend fun getThumbnail(uri: Uri, timeMs: Long = 1000): Bitmap? = withContext(Dispatchers.IO) {
        val key = "${uri}_$timeMs"
        
        // Check memory cache first
        memoryCache.get(key)?.let { return@withContext it }
        
        // Check disk cache
        val file = getFileForUri(uri)
        if (file.exists()) {
            try {
                android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.let { bitmap ->
                    memoryCache.put(key, bitmap)
                    return@withContext bitmap
                }
            } catch (e: Exception) {
                Log.e("ThumbnailManager", "Error reading cached thumbnail: $e")
                // Continue to generate a new thumbnail
            }
        }
        
        // Check if we're already generating this thumbnail
        if (ongoingTasks[key] == true) {
            // Wait briefly and check cache again
            try {
                withContext(Dispatchers.IO) {
                    // Wait up to 3 seconds for the other task to complete
                    var waitTime = 0
                    while (ongoingTasks[key] == true && waitTime < 30) {
                        Thread.sleep(100)
                        waitTime++
                    }
                }
                // Check memory again after waiting
                memoryCache.get(key)?.let { return@withContext it }
            } catch (e: Exception) {
                Log.e("ThumbnailManager", "Error waiting for thumbnail: $e")
            }
        }
        
        // Mark as ongoing
        ongoingTasks[key] = true
        
        try {
            // Generate new thumbnail
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            
            val bitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            
            bitmap?.let {
                // Save to memory cache
                memoryCache.put(key, it)
                
                // Save to disk cache
                saveThumbnailToDisk(it, file)
                
                return@withContext it
            }
        } catch (e: Exception) {
            Log.e("ThumbnailManager", "Error generating thumbnail: $e")
        } finally {
            ongoingTasks.remove(key)
        }
        
        return@withContext null
    }
    
    /**
     * Pre-generate thumbnails for all URIs in the list
     */
    suspend fun preCacheThumbnails(uris: List<Uri>) = withContext(Dispatchers.IO) {
        for (uri in uris) {
            try {
                getThumbnail(uri)
            } catch (e: Exception) {
                Log.e("ThumbnailManager", "Error pre-caching thumbnail: $e")
            }
        }
    }
    
    /**
     * Clear all cached thumbnails
     */
    fun clearCache() {
        // Clear memory cache
        memoryCache.evictAll()
        
        // Clear disk cache
        val dir = getThumbnailDirectory()
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
        }
    }
    
    private fun saveThumbnailToDisk(bitmap: Bitmap, file: File) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        } catch (e: IOException) {
            Log.e("ThumbnailManager", "Error saving thumbnail to disk: $e")
        }
    }
    
    /**
     * Get URI for a cached thumbnail that can be used with Coil or other image loaders
     */
    fun getThumbnailUri(videoUri: Uri): Uri? {
        val file = getFileForUri(videoUri)
        if (!file.exists()) return null
        
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}