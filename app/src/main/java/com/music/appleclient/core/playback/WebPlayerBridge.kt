package com.music.appleclient.core.playback

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WebPlayerBridge(private val context: Context) {

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    private var webView: WebView? = null

    inner class MusicKitJsBridge {
        @JavascriptInterface
        fun onPlaybackStateUpdate(
            isPlaying: Boolean,
            title: String,
            artist: String,
            album: String,
            artworkUrl: String,
            currentPositionSec: Double,
            durationSec: Double
        ) {
            _playerState.update { current ->
                current.copy(
                    isPlaying = isPlaying,
                    positionMs = (currentPositionSec * 1000).toLong(),
                    durationMs = (durationSec * 1000).toLong().coerceAtLeast(1L),
                    currentTrack = TrackMetadata(
                        title = title.ifEmpty { "Apple Music" },
                        artist = artist.ifEmpty { "Apple Music" },
                        album = album,
                        artworkUrl = artworkUrl,
                        isLossless = true,
                        isDolbyAtmos = true
                    )
                )
            }
            // Notify background media service to update system notification & lock screen
            PlaybackService.updateExternalMetadata(
                title = title.ifEmpty { "Apple Music" },
                artist = artist.ifEmpty { "Apple Music" },
                isPlaying = isPlaying,
                durationMs = (durationSec * 1000).toLong().coerceAtLeast(1L),
                positionMs = (currentPositionSec * 1000).toLong()
            )
        }
    }

    fun attachWebView(view: WebView) {
        this.webView = view
    }

    fun play() {
        webView?.post {
            webView?.evaluateJavascript(
                """
                (function() {
                    try {
                        if (window.MusicKit && MusicKit.getInstance()) {
                            MusicKit.getInstance().play();
                            return;
                        }
                    } catch(e) {}
                    var btn = document.querySelector('button[data-testid="play-button"], button[aria-label="Play"], button[title="Play"]');
                    if (btn) btn.click();
                })();
                """.trimIndent(),
                null
            )
        }
    }

    fun pause() {
        webView?.post {
            webView?.evaluateJavascript(
                """
                (function() {
                    try {
                        if (window.MusicKit && MusicKit.getInstance()) {
                            MusicKit.getInstance().pause();
                            return;
                        }
                    } catch(e) {}
                    var btn = document.querySelector('button[data-testid="pause-button"], button[aria-label="Pause"], button[title="Pause"]');
                    if (btn) btn.click();
                })();
                """.trimIndent(),
                null
            )
        }
    }

    fun seekTo(positionMs: Long) {
        val seconds = positionMs / 1000.0
        webView?.post {
            webView?.evaluateJavascript(
                """
                (function() {
                    try {
                        if (window.MusicKit && MusicKit.getInstance()) {
                            MusicKit.getInstance().seekToTime($seconds);
                        }
                    } catch(e) {}
                })();
                """.trimIndent(),
                null
            )
        }
    }

    fun skipNext() {
        webView?.post {
            webView?.evaluateJavascript(
                """
                (function() {
                    try {
                        if (window.MusicKit && MusicKit.getInstance()) {
                            MusicKit.getInstance().skipToNextItem();
                            return;
                        }
                    } catch(e) {}
                    var btn = document.querySelector('button[data-testid="next-button"], button[aria-label="Next"], button[title="Next"]');
                    if (btn) btn.click();
                })();
                """.trimIndent(),
                null
            )
        }
    }

    fun skipPrevious() {
        webView?.post {
            webView?.evaluateJavascript(
                """
                (function() {
                    try {
                        if (window.MusicKit && MusicKit.getInstance()) {
                            MusicKit.getInstance().skipToPreviousItem();
                            return;
                        }
                    } catch(e) {}
                    var btn = document.querySelector('button[data-testid="previous-button"], button[aria-label="Previous"], button[title="Previous"]');
                    if (btn) btn.click();
                })();
                """.trimIndent(),
                null
            )
        }
    }

    companion object {
        const val JS_OBSERVER_INJECTION = """
            (function() {
                if (window.__appleClientInjected) return;
                window.__appleClientInjected = true;

                function notifyNative() {
                    try {
                        var title = "";
                        var artist = "";
                        var album = "";
                        var artwork = "";
                        var isPlaying = false;
                        var pos = 0;
                        var dur = 0;

                        if (window.MusicKit && MusicKit.getInstance()) {
                            var mk = MusicKit.getInstance();
                            isPlaying = mk.isPlaying;
                            pos = mk.currentPlaybackTime || 0;
                            dur = mk.currentPlaybackDuration || 0;
                            if (mk.nowPlayingItem) {
                                title = mk.nowPlayingItem.title || "";
                                artist = mk.nowPlayingItem.artistName || "";
                                album = mk.nowPlayingItem.albumName || "";
                                artwork = (mk.nowPlayingItem.artwork && mk.nowPlayingItem.artwork.url) ? 
                                    mk.nowPlayingItem.artwork.url.replace('{w}x{h}bb', '600x600bb') : "";
                            }
                        } else {
                            var titleEl = document.querySelector('.web-chrome-playback-lcd__sub-copy .lcd-track-title, [data-testid="track-title"]');
                            var artistEl = document.querySelector('.web-chrome-playback-lcd__sub-copy .lcd-artist-name, [data-testid="artist-name"]');
                            var artEl = document.querySelector('.web-chrome-playback-lcd__artwork img, [data-testid="artwork-img"]');
                            var playBtn = document.querySelector('button[data-testid="pause-button"], button[aria-label="Pause"]');
                            
                            if (titleEl) title = titleEl.textContent ? titleEl.textContent.trim() : "";
                            if (artistEl) artist = artistEl.textContent ? artistEl.textContent.trim() : "";
                            if (artEl) artwork = artEl.src || "";
                            isPlaying = !!playBtn;
                        }

                        if (window.MusicKitBridge && (title || isPlaying)) {
                            window.MusicKitBridge.onPlaybackStateUpdate(isPlaying, title, artist, album, artwork, pos, dur);
                        }
                    } catch(e) {}
                }

                setInterval(notifyNative, 500);
            })();
        """
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPlayerView(
    bridge: WebPlayerBridge,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                }

                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(this@apply, true)
                }

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        CookieManager.getInstance().flush()
                        view?.evaluateJavascript(WebPlayerBridge.JS_OBSERVER_INJECTION, null)
                    }
                }

                addJavascriptInterface(bridge.MusicKitJsBridge(), "MusicKitBridge")
                loadUrl("https://music.apple.com")
                bridge.attachWebView(this)
            }
        }
    )
}
