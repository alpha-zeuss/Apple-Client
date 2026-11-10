package com.music.appleclient.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.music.appleclient.R
import com.music.appleclient.core.designsystem.components.FrostedGlassContainer
import com.music.appleclient.core.designsystem.theme.AppleMusicColors
import com.music.appleclient.core.designsystem.theme.SfProDisplay
import com.music.appleclient.core.playback.PlayerUiState

@Composable
fun MiniPlayer(
    playerState: PlayerUiState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedGlassContainer(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(64.dp)
            .clickable { onExpand() },
        cornerRadius = 16.dp,
        blurRadius = 30.dp,
        backgroundColor = AppleMusicColors.SurfaceDark.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art Thumbnail (Squircle)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppleMusicColors.SurfaceElevated)
            ) {
                if (playerState.currentTrack.artworkUrl.isNotEmpty()) {
                    AsyncImage(
                        model = playerState.currentTrack.artworkUrl,
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppleMusicColors.Accent)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Track title & artist
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = playerState.currentTrack.title,
                    fontFamily = SfProDisplay,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playerState.currentTrack.artist,
                    fontFamily = SfProDisplay,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = AppleMusicColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Controls
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (playerState.isPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play
                    ),
                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_player_next),
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

