package com.music.appleclient.feature.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.music.appleclient.R
import com.music.appleclient.core.designsystem.components.FluidScrubberSlider
import com.music.appleclient.core.designsystem.components.LiquidMeshGradientBackground
import com.music.appleclient.core.designsystem.theme.*
import com.music.appleclient.core.playback.PlayerUiState
import java.util.Locale

@Composable
fun NowPlayingSheet(
    playerState: PlayerUiState,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleLyrics: () -> Unit,
    isLyricsOpen: Boolean,
    modifier: Modifier = Modifier
) {
    val dominantColor = remember(playerState.currentTrack.dominantColorHex) {
        Color(playerState.currentTrack.dominantColorHex)
    }

    // iOS signature album art scale spring animation (playing: 1.0f, paused: 0.85f)
    val artworkScale by animateFloatAsState(
        targetValue = if (playerState.isPlaying) 1.0f else 0.86f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "ArtworkScale"
    )

    val positionFraction = remember(playerState.positionMs, playerState.durationMs) {
        if (playerState.durationMs > 0) {
            (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }

    val bufferedFraction = remember(playerState.bufferedPositionMs, playerState.durationMs) {
        if (playerState.durationMs > 0) {
            (playerState.bufferedPositionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }

    LiquidMeshGradientBackground(
        primaryColor = dominantColor,
        secondaryColor = AppleMusicColors.AccentDark,
        tertiaryColor = Color(0xFF1E0A24),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle / Dismiss bar
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = Color(0x99FFFFFF),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Album Artwork with iOS continuous curve squircle & ambient drop shadow
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .scale(artworkScale)
                        .shadow(
                            elevation = if (playerState.isPlaying) 32.dp else 12.dp,
                            shape = RoundedCornerShape(26.dp),
                            clip = false,
                            spotColor = dominantColor.copy(alpha = 0.6f)
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .background(AppleMusicColors.SurfaceDark)
                ) {
                    if (playerState.currentTrack.artworkUrl.isNotEmpty()) {
                        AsyncImage(
                            model = playerState.currentTrack.artworkUrl,
                            contentDescription = "Cover",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(dominantColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Track metadata & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playerState.currentTrack.title,
                        fontFamily = SfProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = playerState.currentTrack.artist,
                        fontFamily = SfProDisplay,
                        fontWeight = FontWeight.Normal,
                        fontSize = 19.sp,
                        color = AppleMusicColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }
            }

            // Lossless & Dolby Atmos Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (playerState.currentTrack.isLossless) {
                    AudioBadge(iconId = R.drawable.ic_lossless, text = "Lossless")
                }
                if (playerState.currentTrack.isDolbyAtmos) {
                    AudioBadge(iconId = R.drawable.ic_dolby_atmos, text = "Dolby Atmos")
                }
            }

            // Scrubber Slider
            FluidScrubberSlider(
                positionFraction = positionFraction,
                bufferedFraction = bufferedFraction,
                onSeek = onSeek,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Timestamps using Tabular Monospace font
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(playerState.positionMs),
                    fontFamily = TabularNumbers,
                    fontSize = 12.sp,
                    color = AppleMusicColors.TextTertiary
                )
                Text(
                    text = "-${formatMs((playerState.durationMs - playerState.positionMs).coerceAtLeast(0L))}",
                    fontFamily = TabularNumbers,
                    fontSize = 12.sp,
                    color = AppleMusicColors.TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transport Media Controls (Apple Music Large Play Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_player_previous),
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (playerState.isPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play
                        ),
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_player_next),
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Footer Bottom Tools: Lyrics, AirPlay Pill, Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleLyrics) {
                    Icon(
                        imageVector = Icons.Rounded.FormatQuote,
                        contentDescription = "Lyrics",
                        tint = if (isLyricsOpen) Color.White else AppleMusicColors.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // AirPlay / Bluetooth Audio Output Pill
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_output_speaker),
                        contentDescription = "Output",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = playerState.outputDeviceName,
                        fontFamily = SfProDisplay,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = AppleMusicColors.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioBadge(
    iconId: Int,
    text: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x33000000))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(id = iconId),
            contentDescription = text,
            tint = AppleMusicColors.TextSecondary,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            fontFamily = SfProRounded,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = AppleMusicColors.TextSecondary
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

