package com.music.appleclient.core.playback

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Immutable
data class TrackMetadata(
    val id: String = "",
    val title: String = "No Track Playing",
    val artist: String = "Apple Music",
    val album: String = "",
    val artworkUrl: String = "",
    val isLossless: Boolean = true,
    val isDolbyAtmos: Boolean = true,
    val dominantColorHex: Long = 0xFF8A1828 // Default deep red
)

@Immutable
data class PlayerUiState(
    val currentTrack: TrackMetadata = TrackMetadata(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 1L,
    val bufferedPositionMs: Long = 0L,
    val outputDeviceName: String = "iPhone Speaker"
)

@OptIn(UnstableApi::class)
class PlayerController(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var webPlayerBridge: WebPlayerBridge? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            setupPlayerListener()
            startPositionTicker()
        }, MoreExecutors.directExecutor())
    }

    fun attachWebPlayerBridge(bridge: WebPlayerBridge) {
        this.webPlayerBridge = bridge
        scope.launch {
            bridge.playerState.collect { webState ->
                if (webState.currentTrack.title != "No Track Playing") {
                    _uiState.update { webState }
                }
            }
        }
    }

    private fun setupPlayerListener() {
        val player = mediaController ?: return
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateTrackMetadata(mediaItem)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val duration = player.duration.coerceAtLeast(1L)
                _uiState.update { it.copy(durationMs = duration) }
            }
        })
    }

    private fun updateTrackMetadata(item: MediaItem?) {
        val metadata = item?.mediaMetadata
        _uiState.update {
            it.copy(
                currentTrack = TrackMetadata(
                    id = item?.mediaId ?: "",
                    title = metadata?.title?.toString() ?: "Now Playing",
                    artist = metadata?.artist?.toString() ?: "Apple Music",
                    album = metadata?.albumTitle?.toString() ?: "",
                    artworkUrl = metadata?.artworkUri?.toString() ?: "",
                    isLossless = true,
                    isDolbyAtmos = true
                )
            )
        }
    }

    private fun startPositionTicker() {
        scope.launch {
            while (isActive) {
                mediaController?.let { player ->
                    if (player.isPlaying) {
                        _uiState.update {
                            it.copy(
                                positionMs = player.currentPosition,
                                bufferedPositionMs = player.bufferedPosition,
                                durationMs = player.duration.coerceAtLeast(1L)
                            )
                        }
                    }
                }
                delay(200) // 5Hz polling for scrubber smoothness
            }
        }
    }

    fun togglePlayPause() {
        val isCurrentlyPlaying = _uiState.value.isPlaying
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
        webPlayerBridge?.let {
            if (isCurrentlyPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(fraction: Float) {
        val targetMs = (fraction * _uiState.value.durationMs).toLong().coerceIn(0L, _uiState.value.durationMs)
        mediaController?.seekTo(targetMs)
        webPlayerBridge?.seekTo(targetMs)
        _uiState.update { s -> s.copy(positionMs = targetMs) }
    }

    fun seekToMs(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        webPlayerBridge?.seekTo(positionMs)
        _uiState.update { it.copy(positionMs = positionMs) }
    }

    fun skipNext() {
        mediaController?.seekToNext()
        webPlayerBridge?.skipNext()
    }

    fun skipPrevious() {
        mediaController?.seekToPrevious()
        webPlayerBridge?.skipPrevious()
    }
}
