package com.music.appleclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.music.appleclient.core.designsystem.theme.AppleMusicColors
import com.music.appleclient.core.designsystem.theme.AppleMusicTheme
import com.music.appleclient.core.designsystem.theme.SfProDisplay
import com.music.appleclient.core.playback.PlayerController
import com.music.appleclient.core.playback.WebPlayerBridge
import com.music.appleclient.core.playback.WebPlayerView
import com.music.appleclient.feature.player.NowPlayingBottomSheet

class MainActivity : ComponentActivity() {

    private lateinit var playerController: PlayerController
    private lateinit var webPlayerBridge: WebPlayerBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerController = PlayerController(applicationContext)
        webPlayerBridge = WebPlayerBridge(applicationContext)
        playerController.attachWebPlayerBridge(webPlayerBridge)

        setContent {
            AppleMusicTheme {
                val playerState by playerController.uiState.collectAsState()
                var selectedTab by remember { mutableIntStateOf(0) }
                var expansionProgress by remember { mutableFloatStateOf(0f) }

                val density = LocalDensity.current
                val navBarHeightDp = 56.dp
                val navBarHeightPx = with(density) { (navBarHeightDp + 64.dp).toPx() }

                // Only show player when a real track title is loaded and login popup is not active
                val hasActiveTrack = remember(playerState.currentTrack.title) {
                    playerState.currentTrack.title.isNotBlank() &&
                    playerState.currentTrack.title != "No Track Playing" &&
                    playerState.currentTrack.title != "Now Playing"
                }
                val isAuthOpen by webPlayerBridge.isAuthPopupOpen
                val showNowPlaying = hasActiveTrack && !isAuthOpen

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppleMusicColors.BackgroundDark)
                ) {
                    // Content Area: Web Player starting cleanly below Android status bar
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(bottom = navBarHeightDp)
                    ) {
                        WebPlayerView(
                            bridge = webPlayerBridge,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // iOS Glass Navigation Bar
                    // Translates down off-screen as full player expands
                    NavigationBar(
                        containerColor = AppleMusicColors.SurfaceDark.copy(alpha = 0.94f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .graphicsLayer {
                                translationY = navBarHeightPx * expansionProgress
                            }
                            .navigationBarsPadding()
                            .height(navBarHeightDp)
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Rounded.PlayCircleFilled, contentDescription = "Listen Now") },
                            label = { Text("Listen Now", fontFamily = SfProDisplay) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppleMusicColors.Accent,
                                selectedTextColor = AppleMusicColors.Accent,
                                unselectedIconColor = AppleMusicColors.TextTertiary,
                                unselectedTextColor = AppleMusicColors.TextTertiary,
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Rounded.Explore, contentDescription = "Browse") },
                            label = { Text("Browse", fontFamily = SfProDisplay) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppleMusicColors.Accent,
                                selectedTextColor = AppleMusicColors.Accent,
                                unselectedIconColor = AppleMusicColors.TextTertiary,
                                unselectedTextColor = AppleMusicColors.TextTertiary,
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Rounded.Radio, contentDescription = "Radio") },
                            label = { Text("Radio", fontFamily = SfProDisplay) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppleMusicColors.Accent,
                                selectedTextColor = AppleMusicColors.Accent,
                                unselectedIconColor = AppleMusicColors.TextTertiary,
                                unselectedTextColor = AppleMusicColors.TextTertiary,
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = "Library") },
                            label = { Text("Library", fontFamily = SfProDisplay) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppleMusicColors.Accent,
                                selectedTextColor = AppleMusicColors.Accent,
                                unselectedIconColor = AppleMusicColors.TextTertiary,
                                unselectedTextColor = AppleMusicColors.TextTertiary,
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            icon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                            label = { Text("Search", fontFamily = SfProDisplay) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AppleMusicColors.Accent,
                                selectedTextColor = AppleMusicColors.Accent,
                                unselectedIconColor = AppleMusicColors.TextTertiary,
                                unselectedTextColor = AppleMusicColors.TextTertiary,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }

                    // 120Hz Interactive Player Sheet
                    // Placed above NavigationBar in z-order so it expands edge-to-edge
                    if (showNowPlaying) {
                        NowPlayingBottomSheet(
                            playerState = playerState,
                            playerController = playerController,
                            navBarBottomOffsetDp = navBarHeightDp,
                            onProgressChanged = { progress -> expansionProgress = progress },
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }
}
