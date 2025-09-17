package com.example.reels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.reels.settings.AppSettings

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsScreen(viewModel: VideoViewModel, settings: AppSettings = AppSettings()) {
    val videoUris by viewModel.videoUris.collectAsState()
    val watchCounts by viewModel.watchCounts.collectAsState()
    val thumbnails by viewModel.thumbnails.collectAsState()

    if (videoUris.isNotEmpty()) {
        // Large virtual count to emulate infinite scrolling
        val realCount = videoUris.size
        val startIndex = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % realCount
        val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { Int.MAX_VALUE })
        
        // Track current page changes
        LaunchedEffect(pagerState.currentPage) {
            val actualIndex = pagerState.currentPage % realCount
            viewModel.setCurrentVideoIndex(actualIndex)
        }

        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index ->
                // Use modulo-mapped URI as stable key
                videoUris[index % realCount]
            }
        ) { virtualPage ->
            val actualIndex = virtualPage % realCount
            val videoUri = videoUris[actualIndex]
            val watchCount = watchCounts[videoUri.toString()] ?: 0
            val thumbnail = thumbnails[videoUri.toString()]
            
            ShortsVideoItem(
                uri = videoUri,
                isActive = pagerState.currentPage == virtualPage,
                modifier = Modifier.fillMaxSize(),
                childMode = settings.childMode,
                watchCount = watchCount,
                thumbnail = thumbnail
            )
        }
    }
}
