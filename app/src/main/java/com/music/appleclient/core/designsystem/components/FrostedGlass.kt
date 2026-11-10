package com.music.appleclient.core.designsystem.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.appleclient.core.designsystem.theme.AppleMusicColors

@Composable
fun FrostedGlassContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 25.dp,
    backgroundColor: Color = AppleMusicColors.GlassSurface,
    borderColor: Color = AppleMusicColors.GlassBorder,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .border(width = 0.5.dp, color = borderColor, shape = shape)
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = RenderEffect.createBlurEffect(
                        blurRadius.toPx(),
                        blurRadius.toPx(),
                        Shader.TileMode.CLAMP
                    ).asComposeRenderEffect()
                }
            }
    ) {
        content()
    }
}

