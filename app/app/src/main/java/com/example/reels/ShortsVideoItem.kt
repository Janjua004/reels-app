package com.example.reels

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp

@Composable
fun ShortsVideoItem(
    uri: Uri,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    childMode: Boolean = false,
    watchCount: Long = 0,
    thumbnail: android.graphics.Bitmap? = null
) {
    var liked by remember(uri) { mutableStateOf(false) }
    var muted by remember { mutableStateOf(true) }
    val cfg = LocalConfiguration.current
    val isLandscape = cfg.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val metrics = rememberUiMetrics()
    val iconSizeDp: Dp = metrics.iconSize.dp

    Box(modifier = modifier.fillMaxSize()) {
        VideoPlayer(
            uri = uri,
            isPlaying = isActive,
            volume = if (muted) 0f else 1f,
            modifier = Modifier.fillMaxSize(),
            thumbnail = thumbnail
        )

        // Bottom gradient & metadata placeholder
        Box(
            Modifier
                .fillMaxWidth()
                .align(if (isLandscape) Alignment.CenterStart else Alignment.BottomStart)
                .height(metrics.gradientHeightDp.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Left side text area (channel + description)
        Column(
            modifier = Modifier
                .align(if (isLandscape) Alignment.CenterStart else Alignment.BottomStart)
                .padding(start = 16.dp, end = if (isLandscape) 16.dp else 100.dp, bottom = if (isLandscape) 32.dp else 20.dp)
        ) {
            // Video title
            Text(
                text = uri.lastPathSegment?.substringBeforeLast(".") ?: "Video",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(Modifier.height(4.dp))
            
            // Watch count badge
            if (watchCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Watch count",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Watched ${if (watchCount == 1L) "once" else "$watchCount times"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            if (childMode) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Kid-friendly mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50) // Green color for kid-friendly indicator
                )
            }
        }

        // Right side stacked action buttons
        val actionColumnModifier = if (isLandscape) {
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
        } else {
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 32.dp)
        }
        Column(
            modifier = actionColumnModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(metrics.actionSpacingDp.dp)
        ) {
            // Like button
            if (!childMode) {
                ActionIcon(
                    icon = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    tint = if (liked) Color.Red else Color.White,
                    label = if (liked) "Liked" else "Like",
                    count = if (liked) 1 else null
                ) { liked = !liked }
            }

            // Mute/Unmute button
            ActionIcon(
                icon = if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                tint = Color.White,
                label = if (muted) "Unmute" else "Mute" 
            ) { muted = !muted }

            // Additional buttons for regular mode
            if (!childMode) {
                ActionIcon(
                    icon = Icons.Default.Share,
                    tint = Color.White,
                    label = "Share" 
                ) { /* TODO: share intent */ }
            }
        }
    }
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    count: Int? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.width(IntrinsicSize.Min)
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint)
        }
        
        Spacer(Modifier.height(4.dp))
        
        Text(
            text = label, 
            color = Color.White, 
            style = MaterialTheme.typography.labelSmall
        )
        
        // Optional count (for likes, etc.)
        if (count != null) {
            Text(
                text = count.toString(),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
