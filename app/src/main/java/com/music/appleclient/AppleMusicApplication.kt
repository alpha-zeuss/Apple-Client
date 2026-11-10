package com.music.appleclient

import android.app.Application
import org.conscrypt.Conscrypt
import java.security.Security

class AppleMusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Install Conscrypt as primary TLS Security Provider
        // Enables hardware-accelerated TLS 1.3 / ALPN and eliminates 80-150ms connection latency
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }
}

