package com.example.reels

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.reels.ui.theme.ReelsTheme
import com.example.reels.settings.*
import com.example.reels.ui.components.OnboardingDialog
import com.example.reels.ui.components.PinEntryDialog
import com.example.reels.util.FolderManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private val videoViewModel: VideoViewModel by viewModels()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var passwordManager: PasswordProtectionManager
    private lateinit var sessionManager: SessionManager
    private lateinit var folderManager: FolderManager
    private var currentFolder: String = "ShortsVideos"
    
    // Track if this is the first run
    private val PREF_FIRST_RUN = "first_run"
    private val isFirstRun: Boolean
        get() {
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val firstRun = prefs.getBoolean(PREF_FIRST_RUN, true)
            if (firstRun) {
                prefs.edit().putBoolean(PREF_FIRST_RUN, false).apply()
            }
            return firstRun
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                videoViewModel.loadVideosFromFolder(contentResolver, currentFolder)
                // Also try to create/find folder and show onboarding if needed
                setupFolder()
            }
        }

    // Helper method to find and load the whitelist file in the current folder
    private fun loadWhitelistForFolder(folderName: String) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val folders = listOf(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES),
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
                    android.os.Environment.getExternalStorageDirectory()
                )
                
                for (parentDir in folders) {
                    val targetDir = java.io.File(parentDir, folderName)
                    if (targetDir.exists() && targetDir.isDirectory) {
                        val whitelistFile = java.io.File(targetDir, "whitelist.txt")
                        if (whitelistFile.exists()) {
                            videoViewModel.loadWhitelistFromFile(whitelistFile)
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Helper method to set up folder creation and onboarding
    private fun setupFolder() {
        lifecycleScope.launch {
            val folder = folderManager.findExistingFolder(currentFolder) 
                ?: folderManager.createDefaultFolders(currentFolder)
                
            folder?.let {
                // If we successfully found or created a folder, load whitelist
                if (it.exists()) {
                    val whitelistFile = File(it, "whitelist.txt")
                    if (whitelistFile.exists()) {
                        videoViewModel.loadWhitelistFromFile(whitelistFile)
                    }
                }
            }
        }
    }
    
    // Helper method to open a folder in the file explorer
    private fun openFolder(folder: File) {
        try {
            // Use FileProvider to get a content URI
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                folder
            )
            
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "resource/folder")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            
            // Try the standard file explorer first
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Fall back to showing a toast with the path
                Toast.makeText(
                    this,
                    "Please navigate to: ${folder.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Folder location: ${folder.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize all managers
        settingsRepository = SettingsRepository(applicationContext)
        passwordManager = PasswordProtectionManager(applicationContext)
        sessionManager = SessionManager(applicationContext, settingsRepository, lifecycleScope)
        folderManager = FolderManager(applicationContext)
        
        // Create and connect watch history manager to the view model
        val watchHistoryManager = com.example.reels.data.WatchHistoryManager(applicationContext)
        videoViewModel.setWatchHistoryManager(watchHistoryManager)
        
        // Create and connect thumbnail manager to the view model
        val thumbnailManager = com.example.reels.data.ThumbnailManager(applicationContext)
        videoViewModel.setThumbnailManager(thumbnailManager)

        // Collect settings asynchronously to update folder & reload if changed
        lifecycleScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                if (currentFolder != settings.folderName) {
                    currentFolder = settings.folderName
                    // if permission already granted, reload videos
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_VIDEO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    val status = ContextCompat.checkSelfPermission(this@MainActivity, permission)
                    if (status == PackageManager.PERMISSION_GRANTED) {
                        videoViewModel.loadVideosFromFolder(contentResolver, currentFolder)
                        // Try to load whitelist when folder changes
                        loadWhitelistForFolder(currentFolder)
                    }
                }
                
                // Apply whitelist setting
                videoViewModel.setWhitelistEnabled(settings.whitelistEnabled)
            }
        }

        setContent {
            ReelsTheme {
                var permissionGranted by remember { mutableStateOf(false) }
                val settingsState by settingsRepository.settingsFlow.collectAsState(initial = com.example.reels.settings.AppSettings())
                var showSettings by remember { mutableStateOf(false) }
                var showOnboarding by remember { mutableStateOf(isFirstRun) }
                var showPinDialog by remember { mutableStateOf(false) }
                var showSetPinDialog by remember { mutableStateOf(false) }
                var showSessionLimitDialog by remember { mutableStateOf(false) }
                
                // Track PIN protection state
                val isPinProtected by passwordManager.hasPinSet.collectAsState(initial = false)
                
                // Session time limit state
                val remainingMinutes by remember { derivedStateOf { sessionManager.remainingTimeMinutes } }
                val sessionExceeded by remember { derivedStateOf { sessionManager.sessionLimitExceeded } }
                val showTimeWarning by remember { derivedStateOf { sessionManager.showTimeWarning } }

                LaunchedEffect(Unit) {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_VIDEO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    val status = ContextCompat.checkSelfPermission(this@MainActivity, permission)
                    if (status == PackageManager.PERMISSION_GRANTED) {
                        permissionGranted = true
                        
                        // Set up folder creation/finding
                        setupFolder()
                        
                        // Load videos from the configured folder
                        videoViewModel.loadVideosFromFolder(contentResolver, currentFolder)
                        
                        // Load whitelist if needed
                        loadWhitelistForFolder(currentFolder)
                        
                        // Initialize video ordering based on watch history
                        if (settingsState.childMode) {
                            videoViewModel.loadVideosBasedOnHistory()
                        }
                        
                        // Show onboarding on first run
                        if (isFirstRun) {
                            showOnboarding = true
                        }
                    } else {
                        permissionLauncher.launch(permission)
                    }
                }
                
                // Handle session time limit exceeded
                LaunchedEffect(sessionExceeded) {
                    if (sessionExceeded && settingsState.sessionTimeEnabled) {
                        showSessionLimitDialog = true
                    }
                }

                val videos by videoViewModel.videoUris.collectAsState()
                permissionGranted = videos.isNotEmpty()

                if (permissionGranted && videos.isNotEmpty()) {
                    Box(Modifier.fillMaxSize()) {
                        // Main content
                        ReelsScreen(viewModel = videoViewModel, settings = settingsState)
                        
                        // Settings button
                        FloatingActionButton(
                            onClick = { 
                                if (settingsState.settingsProtected && isPinProtected) {
                                    showPinDialog = true
                                } else {
                                    showSettings = true
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) { 
                            Text("⚙") 
                        }
                        
                        // Time remaining indicator (only show when enabled and in child mode)
                        if (settingsState.sessionTimeEnabled && settingsState.childMode) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp),
                                color = if (showTimeWarning) Color(0xFFFF9800) else Color(0x88000000),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "$remainingMinutes min",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = Color.White,
                                    fontWeight = if (showTimeWarning) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Grant permission and add videos to '$currentFolder' folder.")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showOnboarding = true }) {
                                Text("Setup Guide")
                            }
                        }
                    }
                }
                
                // Onboarding dialog
                if (showOnboarding) {
                    OnboardingDialog(
                        folderName = currentFolder,
                        folderManager = folderManager,
                        onDismiss = { showOnboarding = false },
                        onOpenFolder = { folder ->
                            openFolder(folder)
                        }
                    )
                }

                // Regular settings dialog
                if (showSettings) {
                    SettingsDialog(
                        settings = settingsState,
                        isPinProtected = isPinProtected,
                        onDismiss = { showSettings = false },
                        onToggleChild = { enabled ->
                            lifecycleScope.launch { settingsRepository.setChildMode(enabled) }
                        },
                        onToggleWhitelist = { enabled ->
                            lifecycleScope.launch { 
                                settingsRepository.setWhitelistEnabled(enabled)
                                videoViewModel.setWhitelistEnabled(enabled) 
                            }
                        },
                        onToggleSessionTime = { enabled ->
                            lifecycleScope.launch { 
                                settingsRepository.setSessionTimeEnabled(enabled)
                                if (!enabled) {
                                    sessionManager.resetSession()
                                }
                            }
                        },
                        onSetSessionTime = { minutes ->
                            lifecycleScope.launch { 
                                settingsRepository.setMaxSessionTime(minutes)
                                sessionManager.resetSession() 
                            }
                        },
                        onTogglePinProtection = { enabled ->
                            if (enabled && !isPinProtected) {
                                showSetPinDialog = true
                            } else if (!enabled && isPinProtected) {
                                lifecycleScope.launch {
                                    passwordManager.clearPin()
                                    settingsRepository.setSettingsProtected(false)
                                    Toast.makeText(
                                        applicationContext, 
                                        "PIN protection removed", 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }
                
                // PIN verification dialog
                if (showPinDialog) {
                    PinEntryDialog(
                        title = "Enter Parent PIN",
                        message = "Please enter your PIN to access settings",
                        onPinConfirmed = { pin ->
                            lifecycleScope.launch {
                                if (passwordManager.verifyPin(pin)) {
                                    showPinDialog = false
                                    showSettings = true
                                } else {
                                    Toast.makeText(
                                        applicationContext,
                                        "Incorrect PIN",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onDismiss = { showPinDialog = false }
                    )
                }
                
                // Set new PIN dialog
                if (showSetPinDialog) {
                    PinEntryDialog(
                        title = "Set Parent PIN",
                        message = "Create a PIN to protect settings (min 4 digits)",
                        isConfirmation = true,
                        onPinConfirmed = { pin ->
                            lifecycleScope.launch {
                                passwordManager.setPin(pin)
                                settingsRepository.setSettingsProtected(true)
                                showSetPinDialog = false
                                Toast.makeText(
                                    applicationContext,
                                    "PIN protection enabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onDismiss = { showSetPinDialog = false }
                    )
                }
                
                // Session time limit exceeded dialog
                if (showSessionLimitDialog) {
                    PinEntryDialog(
                        title = "Time's Up!",
                        message = "Screen time limit reached. Please enter parent PIN to continue.",
                        onPinConfirmed = { pin ->
                            lifecycleScope.launch {
                                if (passwordManager.verifyPin(pin)) {
                                    sessionManager.resetSession()
                                    showSessionLimitDialog = false
                                } else {
                                    Toast.makeText(
                                        applicationContext,
                                        "Incorrect PIN",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onDismiss = { /* Don't allow dismissing this dialog */ }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        sessionManager.resumeSession()
    }
    
    override fun onPause() {
        super.onPause()
        sessionManager.pauseSession()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        sessionManager.cleanup()
    }
}

@Composable
private fun SettingsDialog(
    settings: AppSettings,
    isPinProtected: Boolean,
    onDismiss: () -> Unit,
    onToggleChild: (Boolean) -> Unit,
    onToggleWhitelist: (Boolean) -> Unit,
    onToggleSessionTime: (Boolean) -> Unit,
    onSetSessionTime: (Int) -> Unit,
    onTogglePinProtection: (Boolean) -> Unit
) {
    var sessionMinutes by remember { mutableStateOf(settings.maxSessionTime.toString()) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Child Mode
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Child Mode", modifier = Modifier.weight(1f))
                    Switch(checked = settings.childMode, onCheckedChange = onToggleChild)
                }
                
                // Whitelist
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Whitelist Only", modifier = Modifier.weight(1f))
                    Switch(checked = settings.whitelistEnabled, onCheckedChange = onToggleWhitelist)
                }
                
                Text(
                    "Whitelist file: place 'whitelist.txt' with allowed name fragments inside the folder. One entry per line.",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                
                // Session time limit
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Screen Time Limit", modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.sessionTimeEnabled, 
                        onCheckedChange = onToggleSessionTime
                    )
                }
                
                if (settings.sessionTimeEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Limit (minutes): ", Modifier.weight(1f))
                        OutlinedTextField(
                            value = sessionMinutes,
                            onValueChange = { value -> 
                                if (value.isEmpty() || value.all { it.isDigit() }) {
                                    sessionMinutes = value
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val minutes = sessionMinutes.toIntOrNull() ?: settings.maxSessionTime
                            if (minutes in 1..240) {  // 1 minute to 4 hours
                                onSetSessionTime(minutes)
                            }
                        }) {
                            Text("Set")
                        }
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                
                // Watch History Section
                Text(
                    "Watch History",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "App keeps track of watched videos\nto show favorites more often",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Button(
                        onClick = { showClearHistoryDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Clear History")
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                
                // PIN Protection
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PIN Protect Settings", modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.settingsProtected, 
                        onCheckedChange = onTogglePinProtection
                    )
                }
            }
        }
    )
    
    // Clear history confirmation dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Watch History?") },
            text = { Text("This will remove all watch history and reset view counts. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { 
                        // Clear watch history
                        val applicationContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
                        val watchHistoryManager = com.example.reels.data.WatchHistoryManager(applicationContext)
                        kotlinx.coroutines.MainScope().launch {
                            watchHistoryManager.clearWatchHistory()
                            showClearHistoryDialog = false
                            Toast.makeText(applicationContext, "Watch history cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
