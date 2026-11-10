package com.music.appleclient.feature.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.appleclient.core.designsystem.theme.SfProDisplay
import kotlinx.coroutines.launch

data class LyricLine(
    val id: String,
    val startMs: Long,
    val text: String,
    val isBackgroundVocal: Boolean = false
)

@Composable
fun KaraokeLyricsView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val activeIndex = remember(currentPositionMs, lyrics) {
        lyrics.indexOfLast { currentPositionMs >= it.startMs }
            .coerceAtLeast(0)
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && !listState.isScrollInProgress) {
            scope.launch {
                listState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = -250 // Offsets active lyric towards visual vertical center
                )
            }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 220.dp, horizontal = 28.dp),
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        itemsIndexed(lyrics, key = { _, line -> line.id }) { index, line ->
            val isActive = index == activeIndex
            val isPast = index < activeIndex

            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.06f else 1.0f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                label = "LyricScale"
            )

            val alpha by animateFloatAsState(
                targetValue = when {
                    isActive -> 1.0f
                    isPast -> 0.45f
                    else -> 0.35f
                },
                animationSpec = spring(stiffness = 500f),
                label = "LyricAlpha"
            )

            val blurRadius by animateFloatAsState(
                targetValue = if (isActive) 0f else 2.5f,
                animationSpec = spring(stiffness = 400f),
                label = "LyricBlur"
            )

            Text(
                text = line.text,
                fontFamily = SfProDisplay,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = if (line.isBackgroundVocal) 22.sp else 30.sp,
                lineHeight = 38.sp,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .alpha(alpha)
                    .then(
                        if (blurRadius > 0.5f) Modifier.blur(blurRadius.dp) else Modifier
                    )
                    .clickable { onSeekTo(line.startMs) }
            )
        }
    }
}

