package com.music.appleclient.feature.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.music.appleclient.core.playback.PlayerController
import com.music.appleclient.core.playback.PlayerUiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SheetState {
    COLLAPSED,
    EXPANDED
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingBottomSheet(
    playerState: PlayerUiState,
    playerController: PlayerController,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }
    val miniPlayerHeightDp = 64.dp
    val miniPlayerHeightPx = with(density) { miniPlayerHeightDp.toPx() }

    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = SheetState.COLLAPSED,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    LaunchedEffect(sheetHeightPx, miniPlayerHeightPx) {
        if (sheetHeightPx > 0f) {
            val collapsedOffset = (sheetHeightPx - miniPlayerHeightPx).coerceAtLeast(0f)
            val newAnchors = DraggableAnchors {
                SheetState.EXPANDED at 0f
                SheetState.COLLAPSED at collapsedOffset
            }
            draggableState.updateAnchors(newAnchors)
        }
    }

    val offset = try {
        draggableState.requireOffset()
    } catch (_: Exception) {
        0f
    }

    val maxOffset = (sheetHeightPx - miniPlayerHeightPx).coerceAtLeast(1f)
    val expansionProgress = (1f - (offset / maxOffset)).coerceIn(0f, 1f)

    // Interpolate corner radius from 14dp (miniplayer) to 32dp (fullscreen)
    val cornerRadius = lerp(14f, 32f, expansionProgress).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { sheetHeightPx = it.height.toFloat() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { sheetHeightPx.toDp() })
                .offset { IntOffset(0, offset.roundToInt()) }
                .anchoredDraggable(
                    state = draggableState,
                    orientation = Orientation.Vertical
                )
                .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)),
            color = Color.Transparent,
            shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Fullscreen Player content (crossfade in as sheet expands)
                if (expansionProgress > 0.02f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(expansionProgress)
                    ) {
                        NowPlayingSheet(
                            playerState = playerState,
                            onDismiss = {
                                scope.launch { draggableState.animateTo(SheetState.COLLAPSED) }
                            },
                            onTogglePlayPause = { playerController.togglePlayPause() },
                            onSeek = { playerController.seekTo(it) },
                            onPrevious = { playerController.skipPrevious() },
                            onNext = { playerController.skipNext() },
                            onToggleLyrics = {},
                            isLyricsOpen = false
                        )
                    }
                }

                // MiniPlayer content (visible when collapsed or collapsing)
                if (expansionProgress < 0.98f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(miniPlayerHeightDp)
                            .align(Alignment.TopCenter)
                            .alpha(1f - expansionProgress)
                    ) {
                        MiniPlayer(
                            playerState = playerState,
                            onExpand = {
                                scope.launch { draggableState.animateTo(SheetState.EXPANDED) }
                            },
                            onTogglePlayPause = { playerController.togglePlayPause() },
                            onNext = { playerController.skipNext() }
                        )
                    }
                }
            }
        }
    }
}
