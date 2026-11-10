package com.music.appleclient.core.designsystem.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.music.appleclient.core.designsystem.theme.AppleMusicColors

@Composable
fun FluidScrubberSlider(
    positionFraction: Float, // 0.0f .. 1.0f
    bufferedFraction: Float, // 0.0f .. 1.0f
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    bufferedColor: Color = Color(0x33FFFFFF),
    inactiveColor: Color = Color(0x1AFFFFFF)
) {
    var isDragging by remember { mutableStateOf(false) }
    var sliderSize by remember { mutableStateOf(IntSize.Zero) }

    // Spring expansion on touch (from 5dp to 9dp)
    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 9.dp else 5.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "ScrubberHeight"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp) // Generous touch target
            .onSizeChanged { sliderSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        val newFraction = (offset.x / sliderSize.width.toFloat()).coerceIn(0f, 1f)
                        onSeek(newFraction)
                        tryAwaitRelease()
                        isDragging = false
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val newFraction = (change.position.x / sliderSize.width.toFloat()).coerceIn(0f, 1f)
                        onSeek(newFraction)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Base track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(CircleShape)
                .background(inactiveColor)
        )

        // Buffered track
        Box(
            modifier = Modifier
                .fillMaxWidth(bufferedFraction.coerceIn(0f, 1f))
                .height(trackHeight)
                .clip(CircleShape)
                .background(bufferedColor)
        )

        // Active played track
        Box(
            modifier = Modifier
                .fillMaxWidth(positionFraction.coerceIn(0f, 1f))
                .height(trackHeight)
                .clip(CircleShape)
                .background(activeColor)
        )
    }
}

