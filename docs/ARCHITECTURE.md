# Deep Architectural Specification: Apple Music Client

This document provides technical documentation on the internals of the client, focusing on the audio caching pipeline, the AGSL shader pipeline, and the state management model.

---

## 1. The Playback & Caching Engine

### 1.1 Why Standard Streaming Introduces Latency
Standard Android media streaming solutions construct an `HttpDataSource` and invoke `player.setMediaItem(item); player.prepare()`. Under this naive approach:
1. DNS resolution runs on the main network dispatcher (~30–80ms).
2. The TLS handshake takes 2 RTTs (~100–180ms).
3. The server begins streaming audio frames; ExoPlayer waits until its `minBufferMs` (typically 1,500ms to 2,500ms) is satisfied before playback starts.

### 1.2 The Two-Tier AudioCacheManager
To eliminate this delay, `AudioCacheManager.kt` employs a two-tier strategy:

```
[MediaItem Prepared]
       │
       ▼
Is Item in SimpleCache?
  ├── YES: Immediate read from CacheDataSource (0ms delay)
  └── NO:  Stream through OkHttpDataSource + Pipe to SimpleCache disk span
       │
[Track N Begins Playing]
       │
       ▼
Query Track (N + 1) in Queue
       │
       ▼
Async Coroutine (Dispatchers.IO):
  Pull DataSpec(offset = 0, length = 384 KB)
  Write directly into SimpleCache Span
```

By caching the initial 384 KB (approx. 15–20 seconds of 256kbps AAC audio), the player has enough initial buffer to start immediately upon skip, giving the background HTTP/2 stream plenty of time to fill the rest of the buffer seamlessly.

---

## 2. AGSL GPU Runtime Shaders

### 2.1 The RenderScript Bottleneck in Stock Apple Music
On Android, Apple's official client relies on `librenderscript-toolkit.so`. RenderScript runs on the CPU or an unoptimized OpenCL driver on heterogeneous Android SoCs. Every time the artwork changes:
- An uncompressed ARGB_8888 bitmap is copied across JNI.
- A 25-radius Gaussian blur algorithm runs across CPU worker threads.
- The blurred bitmap is uploaded to OpenGL / Vulkan as a GPU texture.
- This causes dropped frames during playback transitions.

### 2.2 Direct AGSL Implementation
In Android 13+ (API 33+), Android introduced the `RuntimeShader` API using AGSL (Android Graphics Shading Language), which is functionally identical to GLSL ES 3.0.

In `LiquidMeshGradient.kt`:
- We pass 4 raw 32-bit color vector uniforms (`uColor1`, `uColor2`, `uColor3`, `uColor4`) extracted from the album artwork palette.
- A simplex noise function warps UV coordinates dynamically using time `uTime`.
- A 4-point smoothstep radial gradient blends the colors smoothly on the GPU fragment stage.
- **Zero CPU bitmap allocations occur.** The GPU renders the liquid gradient at 120 FPS ProMotion with negligible battery impact.

---

## 3. Typographic Hierarchy & Metric Alignment

| Token | Weight | Size | Line Height | Tracking | Application |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `displayLarge` | Bold (700) | 34sp | 41sp | +0.37sp | "Listen Now", "Library" Main Screen Titles |
| `titleLarge` | Bold (700) | 28sp | 34sp | +0.36sp | Playlist & Album Titles |
| `titleMedium` | Semibold (600) | 22sp | 28sp | +0.35sp | Section Headers ("Top Picks") |
| `bodyLarge` | Regular (400) | 17sp | 22sp | -0.41sp | Body text, track descriptions |
| `labelLarge` | Semibold (600) | 17sp | 22sp | -0.41sp | Now Playing Track Title |
| `labelSmall` | Bold (700) | 11sp | 13sp | +0.07sp | Lossless & Dolby Atmos Audio Badges |
| `TabularNumbers` | Medium (500) | 12sp | 16sp | 0.0sp | Monospace scrub timestamps |

All SF Pro fonts are bundled in `app/src/main/res/font/` with pre-calculated baseline offsets matching Apple's Human Interface Guidelines.
