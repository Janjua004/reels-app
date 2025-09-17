package com.example.reels

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reels.data.ThumbnailManager
import com.example.reels.data.WatchHistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoViewModel : ViewModel() {
    private val _videoUris = MutableStateFlow<List<Uri>>(emptyList())
    val videoUris: StateFlow<List<Uri>> = _videoUris
    
    private val _whitelist = MutableStateFlow<Set<String>>(emptySet())
    private val _whitelistEnabled = MutableStateFlow(false)
    
    private val _currentVideoIndex = MutableStateFlow<Int>(0)
    val currentVideoIndex: StateFlow<Int> = _currentVideoIndex
    
    // Watch history tracking
    private var watchHistoryManager: WatchHistoryManager? = null
    private val _watchCounts = MutableStateFlow<Map<String, Long>>(emptyMap())
    val watchCounts: StateFlow<Map<String, Long>> = _watchCounts
    
    // Thumbnail management
    private var thumbnailManager: ThumbnailManager? = null
    private val _thumbnails = MutableStateFlow<Map<String, Bitmap?>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap?>> = _thumbnails
    
    fun setWatchHistoryManager(manager: WatchHistoryManager) {
        this.watchHistoryManager = manager
        loadWatchCounts()
    }
    
    fun setThumbnailManager(manager: ThumbnailManager) {
        this.thumbnailManager = manager
        // Pre-cache thumbnails for current videos
        if (_videoUris.value.isNotEmpty()) {
            loadThumbnails(_videoUris.value)
        }
    }
    
    fun setCurrentVideoIndex(index: Int) {
        _currentVideoIndex.value = index
        // Record this video as watched
        recordCurrentVideoWatched()
        // Pre-cache thumbnails for next videos
        preloadAdjacentThumbnails(index)
    }
    
    private fun recordCurrentVideoWatched() {
        val currentIndex = _currentVideoIndex.value
        val videos = _videoUris.value
        if (currentIndex in videos.indices) {
            val videoUri = videos[currentIndex]
            watchHistoryManager?.let { manager ->
                viewModelScope.launch {
                    manager.recordVideoWatched(videoUri)
                    // Update watch counts
                    loadWatchCounts()
                }
            }
        }
    }
    
    private fun loadWatchCounts() {
        val manager = watchHistoryManager ?: return
        val videos = _videoUris.value
        
        viewModelScope.launch(Dispatchers.IO) {
            val counts = mutableMapOf<String, Long>()
            for (uri in videos) {
                val count = manager.getWatchCount(uri)
                counts[uri.toString()] = count
            }
            _watchCounts.value = counts
        }
    }
    
    fun loadVideosBasedOnHistory() {
        val manager = watchHistoryManager ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val recentlyWatched = manager.recentlyWatchedVideos.firstOrNull() ?: emptyList()
            val currentVideos = _videoUris.value
            
            // If we have some history, reorder videos based on history
            if (recentlyWatched.isNotEmpty() && currentVideos.isNotEmpty()) {
                // Convert to mutable list and create map for easier lookups
                val videoMap = currentVideos.associateBy { it.toString() }
                val orderedList = mutableListOf<Uri>()
                
                // First add recent videos that are still available
                for (recentUri in recentlyWatched) {
                    videoMap[recentUri]?.let { orderedList.add(it) }
                }
                
                // Then add any videos not in history
                for (video in currentVideos) {
                    if (!orderedList.contains(video)) {
                        orderedList.add(video)
                    }
                }
                
                // Update video list with new ordering
                _videoUris.value = orderedList
                
                // Load thumbnails for the newly ordered list
                loadThumbnails(orderedList)
            }
        }
    }
    
    /**
     * Load thumbnails for all videos in the list
     */
    private fun loadThumbnails(videos: List<Uri>) {
        val manager = thumbnailManager ?: return
        viewModelScope.launch {
            // Start pre-caching all thumbnails
            manager.preCacheThumbnails(videos)
            
            // Load thumbnails for the first few videos immediately
            val priority = videos.take(5)
            for (uri in priority) {
                loadThumbnailForUri(uri)
            }
        }
    }
    
    /**
     * Load the thumbnail for a specific URI
     */
    private fun loadThumbnailForUri(uri: Uri) {
        val manager = thumbnailManager ?: return
        viewModelScope.launch {
            try {
                val bitmap = manager.getThumbnail(uri)
                if (bitmap != null) {
                    _thumbnails.value = _thumbnails.value.toMutableMap().apply {
                        put(uri.toString(), bitmap)
                    }
                }
            } catch (e: Exception) {
                // Log error but continue
                android.util.Log.e("VideoViewModel", "Error loading thumbnail: $e")
            }
        }
    }
    
    /**
     * Preload thumbnails for videos around the current index
     */
    private fun preloadAdjacentThumbnails(index: Int) {
        val videos = _videoUris.value
        if (videos.isEmpty()) return
        
        val indicesToLoad = mutableListOf<Int>()
        // Next 3 videos
        for (i in 1..3) {
            indicesToLoad.add((index + i) % videos.size)
        }
        // Previous video
        indicesToLoad.add((index - 1 + videos.size) % videos.size)
        
        for (i in indicesToLoad) {
            if (i in videos.indices) {
                loadThumbnailForUri(videos[i])
            }
        }
    }

    fun loadVideos(contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            val uris = mutableListOf<Uri>()
            val projection = arrayOf(MediaStore.Video.Media._ID)
            val sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC"
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    uris.add(contentUri)
                }
            }
            _videoUris.value = uris
            loadWatchCounts()
        }
    }

    /**
     * Load only videos that physically reside inside a user-created folder name (case-insensitive match)
     * within common external storage locations. Example: if folderName = "ShortsVideos" and user places
     * files under /storage/emulated/0/Movies/ShortsVideos or /storage/emulated/0/Download/ShortsVideos.
     * Uses RELATIVE_PATH (API 29+) when available; falls back to DATA (deprecated) for older devices.
     */
    fun loadVideosFromFolder(contentResolver: ContentResolver, folderName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetLower = folderName.lowercase()
            val uris = mutableListOf<Uri>()
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                // RELATIVE_PATH is available from API 29; query anyway and handle absence via index check
                MediaStore.Video.Media.RELATIVE_PATH,
                MediaStore.Video.Media.DATA // deprecated but useful for <=28
            )
            val sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC"
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val relPathIndex = cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                val dataIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rel = if (relPathIndex != -1) cursor.getString(relPathIndex) else null
                    val data = if (dataIndex != -1) cursor.getString(dataIndex) else null
                    val pathString = (rel ?: data ?: "").lowercase()
                    if (pathString.contains("/" + targetLower) || pathString.contains(targetLower + "/") || pathString.endsWith(targetLower)) {
                        val contentUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                        uris.add(contentUri)
                    }
                }
            }
            _videoUris.value = uris
            
            // Apply whitelist if enabled
            if (_whitelistEnabled.value) {
                applyWhitelist()
            }
            
            // Load watch counts for all videos
            loadWatchCounts()
            
            // Optionally reorder based on history
            loadVideosBasedOnHistory()
            
            // Start generating thumbnails
            loadThumbnails(uris)
        }
    }

    fun setWhitelistEnabled(enabled: Boolean) { 
        _whitelistEnabled.value = enabled 
        applyWhitelist()
    }

    fun loadWhitelistFromFile(file: java.io.File) {
        viewModelScope.launch(Dispatchers.IO) {
            if (file.exists()) {
                val allowed = file.readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .map { it.lowercase() }
                    .toSet()
                _whitelist.value = allowed
                applyWhitelist()
            }
        }
    }

    private fun applyWhitelist() {
        if (_whitelistEnabled.value && _whitelist.value.isNotEmpty()) {
            _videoUris.value = _videoUris.value.filter { uri ->
                val name = uri.lastPathSegment?.lowercase() ?: return@filter false
                _whitelist.value.any { allowed -> name.contains(allowed) }
            }
        }
    }
}
