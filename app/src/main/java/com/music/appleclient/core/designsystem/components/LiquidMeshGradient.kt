package com.music.appleclient.core.designsystem.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import org.intellij.lang.annotations.Language

@Language("AGSL")
private const val LIQUID_GRADIENT_SHADER = """
    uniform float2 uResolution;
    uniform float uTime;
    layout(color) uniform half4 uColor1;
    layout(color) uniform half4 uColor2;
    layout(color) uniform half4 uColor3;
    layout(color) uniform half4 uColor4;

    float hash(float2 p) {
        return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
                   mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / uResolution;
        
        float t = uTime * 0.15;
        float n1 = noise(uv * 2.2 + float2(t * 0.3, -t * 0.2));
        float n2 = noise(uv * 3.1 + float2(-t * 0.25, t * 0.35) + n1 * 0.4);
        
        float2 warpedUV = uv + float2(sin(n2 * 6.28 + t), cos(n1 * 6.28 + t)) * 0.18;
        
        float d1 = distance(warpedUV, float2(0.15, 0.2));
        float d2 = distance(warpedUV, float2(0.85, 0.25));
        float d3 = distance(warpedUV, float2(0.35, 0.85));
        float d4 = distance(warpedUV, float2(0.9, 0.8));
        
        float w1 = smoothstep(0.85, 0.05, d1);
        float w2 = smoothstep(0.85, 0.05, d2);
        float w3 = smoothstep(0.9, 0.05, d3);
        float w4 = smoothstep(0.8, 0.05, d4);
        
        float total = w1 + w2 + w3 + w4 + 0.001;
        half4 col = (uColor1 * w1 + uColor2 * w2 + uColor3 * w3 + uColor4 * w4) / total;
        
        return half4(col.rgb * 0.52, 1.0);
    }
"""

@Composable
fun LiquidMeshBackground(
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "LiquidAnimation")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(120000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LiquidTime"
    )

    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader(LIQUID_GRADIENT_SHADER)
        } else {
            null
        }
    }
    val shaderBrush = remember(shader) {
        shader?.let { ShaderBrush(it) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null && shaderBrush != null) {
                    shader.setFloatUniform("uResolution", size.width, size.height)
                    shader.setFloatUniform("uTime", time)
                    shader.setColorUniform("uColor1", primaryColor.toArgb())
                    shader.setColorUniform("uColor2", secondaryColor.toArgb())
                    shader.setColorUniform("uColor3", tertiaryColor.toArgb())
                    shader.setColorUniform("uColor4", Color(0xFF0D0D12).toArgb())
                    onDrawBehind {
                        drawRect(shaderBrush)
                    }
                } else {
                    val fallbackBrush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.55f),
                            secondaryColor.copy(alpha = 0.35f),
                            Color(0xFF0A0A0E)
                        ),
                        center = Offset(size.width * 0.4f, size.height * 0.3f),
                        radius = (size.width + size.height) * 0.6f
                    )
                    onDrawBehind {
                        drawRect(fallbackBrush)
                    }
                }
            }
    ) {
        content()
    }
}

// Convenient alias for backwards compatibility
@Composable
fun LiquidMeshGradientBackground(
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) = LiquidMeshBackground(
    primaryColor = primaryColor,
    secondaryColor = secondaryColor,
    tertiaryColor = tertiaryColor,
    modifier = modifier,
    content = content
)
