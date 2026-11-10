package com.music.appleclient.core.playback

import android.annotation.SuppressLint
import android.content.Context
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WebPlayerBridge(private val context: Context) {

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    var isAuthPopupOpen = mutableStateOf(false)
        private set
    var popupWebView = mutableStateOf<WebView?>(null)
        private set

    private var webView: WebView? = null

    fun showAuthPopup(childWebView: WebView) {
        popupWebView.value = childWebView
        isAuthPopupOpen.value = true
    }

    fun dismissAuthPopup() {
        popupWebView.value?.destroy()
        popupWebView.value = null
        isAuthPopupOpen.value = false
        CookieManager.getInstance().flush()
    }

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
        const val JS_BANNER_BLOCKER_INJECTION = """
            (function() {
                try {
                    const style = document.createElement('style');
                    style.innerHTML = '.upsell-banner, .country-switcher-banner, .web-navigation__native-upsell, .locale-switcher-banner, .banner-wrapper, [data-testid="upsell-banner"], [aria-label="Choose another country"] { display: none !important; }';
                    document.head.appendChild(style);
                } catch(e) {}
            })();
        """

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
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus(View.FOCUS_DOWN)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                    }

                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webChromeClient = object : WebChromeClient() {
                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message?
                        ): Boolean {
                            if (resultMsg == null || view == null) return false

                            val childWebView = WebView(view.context).apply {
                                isFocusable = true
                                isFocusableInTouchMode = true
                                requestFocus(View.FOCUS_DOWN)

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    javaScriptCanOpenWindowsAutomatically = true
                                    setSupportMultipleWindows(true)
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    userAgentString = view.settings.userAgentString
                                }

                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                webChromeClient = object : WebChromeClient() {
                                    override fun onCloseWindow(window: WebView?) {
                                        super.onCloseWindow(window)
                                        bridge.dismissAuthPopup()
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(v: WebView?, u: String?) {
                                        super.onPageFinished(v, u)
                                        CookieManager.getInstance().flush()
                                    }
                                }
                            }

                            val transport = resultMsg.obj as? WebView.WebViewTransport
                            if (transport != null) {
                                transport.webView = childWebView
                                resultMsg.sendToTarget()
                                bridge.showAuthPopup(childWebView)
                                return true
                            }
                            return false
                        }

                        override fun onCloseWindow(window: WebView?) {
                            super.onCloseWindow(window)
                            bridge.dismissAuthPopup()
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            CookieManager.getInstance().flush()
                            view?.evaluateJavascript(WebPlayerBridge.JS_BANNER_BLOCKER_INJECTION, null)
                            view?.evaluateJavascript(WebPlayerBridge.JS_OBSERVER_INJECTION, null)
                        }
                    }

                    addJavascriptInterface(bridge.MusicKitJsBridge(), "MusicKitBridge")
                    loadUrl("https://music.apple.com/in/new")
                    bridge.attachWebView(this)
                }
            }
        )

        // Fullscreen OAuth Popup Dialog for Apple ID Authentication
        if (bridge.isAuthPopupOpen.value && bridge.popupWebView.value != null) {
            Dialog(
                onDismissRequest = { bridge.dismissAuthPopup() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    AndroidView(
                        factory = { bridge.popupWebView.value!! },
                        modifier = Modifier.fillMaxSize()
                    )

                    IconButton(
                        onClick = { bridge.dismissAuthPopup() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Sign-In",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
