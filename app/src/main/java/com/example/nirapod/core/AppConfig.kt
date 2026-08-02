package com.example.nirapod.core

import com.example.nirapod.BuildConfig

object AppConfig {
    private fun configured(value: String): Boolean =
        value.isNotBlank() && !value.startsWith("YOUR_") && !value.contains("YOUR_PROJECT")

    val firebaseConfigured: Boolean
        get() = configured(BuildConfig.FIREBASE_API_KEY) &&
            configured(BuildConfig.FIREBASE_APP_ID) &&
            configured(BuildConfig.FIREBASE_PROJECT_ID)

    val supabaseConfigured: Boolean
        get() = configured(BuildConfig.SUPABASE_URL) && configured(BuildConfig.SUPABASE_ANON_KEY)

    val cloudModeLabel: String
        get() = if (firebaseConfigured) "Cloud mode" else "Demo mode"
}
