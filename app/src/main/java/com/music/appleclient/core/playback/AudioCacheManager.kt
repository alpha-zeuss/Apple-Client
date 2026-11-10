package com.music.appleclient.core.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File

@OptIn(UnstableApi::class)
class AudioCacheManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val cacheDir = File(context.cacheDir, "apple_client_media_cache")
    private val cacheEvictor = LeastRecentlyUsedCacheEvictor(2L * 1024 * 1024 * 1024) // 2GB Max disk cache
    private val databaseProvider = StandaloneDatabaseProvider(context)

    val simpleCache: SimpleCache by lazy {
        SimpleCache(cacheDir, cacheEvictor, databaseProvider)
    }

    private val upstreamFactory: DataSource.Factory by lazy {
        OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("AppleMusicClient/1.0 (Android; Audiophile Engine)")
    }

    val cacheDataSourceFactory: DataSource.Factory by lazy {
        CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Decoupled Head-Chunk Pre-Fetcher (BitChord Architecture):
     * Caches the initial 384 KB of the upcoming track so that playback starts with 0ms delay.
     */
    fun prefetchTrackHead(mediaItem: MediaItem, headBytes: Long = 384 * 1024L) {
        val uri = mediaItem.localConfiguration?.uri ?: return
        val cacheKey = if (mediaItem.mediaId.isNotEmpty()) mediaItem.mediaId else uri.toString()

        ioScope.launch(Dispatchers.IO) {
            try {
                if (simpleCache.isCached(cacheKey, 0, headBytes)) return@launch

                val dataSpec = DataSpec.Builder()
                    .setUri(uri)
                    .setPosition(0)
                    .setLength(headBytes)
                    .setKey(cacheKey)
                    .build()

                val cacheWriter = CacheWriter(
                    cacheDataSourceFactory.createDataSource(),
                    dataSpec,
                    ByteArray(32 * 1024),
                    null
                )
                cacheWriter.cache()
            } catch (_: Exception) {
                // Best-effort head prefetching; ignore network interruption
            }
        }
    }

    fun prefetchUpcomingTrack(mediaItem: MediaItem, headBytes: Long = 384 * 1024L) {
        prefetchTrackHead(mediaItem, headBytes)
    }
}
