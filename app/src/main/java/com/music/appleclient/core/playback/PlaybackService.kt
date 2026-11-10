package com.music.appleclient.core.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.music.appleclient.MainActivity
import com.music.appleclient.core.network.NetworkModule
import java.lang.ref.WeakReference

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set
    lateinit var cacheManager: AudioCacheManager
        private set

    override fun onCreate() {
        super.onCreate()
        instanceRef = WeakReference(this)

        val okHttpClient = NetworkModule.getOkHttpClient(applicationContext)
        cacheManager = AudioCacheManager(applicationContext, okHttpClient)

        val mediaSourceFactory = DefaultMediaSourceFactory(cacheManager.cacheDataSourceFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val sessionActivityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        if (instanceRef?.get() == this) {
            instanceRef = null
        }
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        private var instanceRef: WeakReference<PlaybackService>? = null

        fun updateExternalMetadata(
            title: String,
            artist: String,
            isPlaying: Boolean,
            durationMs: Long,
            positionMs: Long
        ) {
            val service = instanceRef?.get() ?: return
            service.player.let { exoPlayer ->
                val metadata = MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .build()

                val currentItem = exoPlayer.currentMediaItem
                if (currentItem == null || currentItem.mediaMetadata.title != title) {
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(title)
                        .setMediaMetadata(metadata)
                        .build()
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                }
            }
        }
    }
}
