package com.example.nirapod.core

import android.content.Context
import com.example.nirapod.data.model.AppUser
import com.example.nirapod.data.model.UserRole

class DemoSession(context: Context) {
    private val prefs = context.getSharedPreferences("demo_session", Context.MODE_PRIVATE)

    fun save(user: AppUser) {
        prefs.edit()
            .putString("uid", user.uid)
            .putString("name", user.name)
            .putString("email", user.email)
            .putString("role", user.role)
            .apply()
    }

    fun get(): AppUser? {
        val uid = prefs.getString("uid", null) ?: return null
        return AppUser(
            uid = uid,
            name = prefs.getString("name", "Demo User").orEmpty(),
            email = prefs.getString("email", "demo@nirapod.app").orEmpty(),
            role = prefs.getString("role", UserRole.CITIZEN.name).orEmpty()
        )
    }

    fun clear() = prefs.edit().clear().apply()
}
