package com.example.nirapod

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.nirapod.core.AppConfig
import com.example.nirapod.core.AppContainer
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import org.maplibre.android.MapLibre


class NirapodApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        initializeFirebaseIfConfigured()
        createNotificationChannel()
        container = AppContainer(this)
    }

    private fun initializeFirebaseIfConfigured() {
        if (!AppConfig.firebaseConfigured || FirebaseApp.getApps(this).isNotEmpty()) return

        val options = FirebaseOptions.Builder()
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .apply {
                if (BuildConfig.FIREBASE_GCM_SENDER_ID.isNotBlank()) {
                    setGcmSenderId(BuildConfig.FIREBASE_GCM_SENDER_ID)
                }
            }
            .build()

        FirebaseApp.initializeApp(this, options)
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        } else {
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Nirapod alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Report status and public-safety alerts"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "nirapod_alerts"
    }
}
