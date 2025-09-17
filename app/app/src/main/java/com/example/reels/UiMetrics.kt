package com.example.reels

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** Simple adaptive metrics without full WindowSizeClass dependency (lightweight). */

enum class DeviceWidthClass { Compact, Medium, Expanded }

data class Metrics(
    val iconSize: Int,
    val actionSpacingDp: Int,
    val gradientHeightDp: Int
)

@Composable
fun rememberUiMetrics(): Metrics {
    val cfg = LocalConfiguration.current
    val widthDp = cfg.screenWidthDp
    val cls = when {
        widthDp < 420 -> DeviceWidthClass.Compact
        widthDp < 720 -> DeviceWidthClass.Medium
        else -> DeviceWidthClass.Expanded
    }
    return when (cls) {
        DeviceWidthClass.Compact -> Metrics(iconSize = 52, actionSpacingDp = 22, gradientHeightDp = 200)
        DeviceWidthClass.Medium -> Metrics(iconSize = 64, actionSpacingDp = 28, gradientHeightDp = 240)
        DeviceWidthClass.Expanded -> Metrics(iconSize = 72, actionSpacingDp = 32, gradientHeightDp = 260)
    }
}
