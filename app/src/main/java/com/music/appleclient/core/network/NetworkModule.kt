package com.music.appleclient.core.network

import android.content.Context
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object NetworkModule {
    private var clientInstance: OkHttpClient? = null

    fun getOkHttpClient(context: Context): OkHttpClient {
        return clientInstance ?: synchronized(this) {
            clientInstance ?: buildClient(context).also { clientInstance = it }
        }
    }

    private fun buildClient(context: Context): OkHttpClient {
        val httpCacheDir = File(context.cacheDir, "http_response_cache")
        val cache = Cache(httpCacheDir, 50L * 1024 * 1024) // 50 MB HTTP Cache

        return OkHttpClient.Builder()
            .cache(cache)
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "AppleMusicClient/1.0 (Android; Audiophile Engine)")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}

