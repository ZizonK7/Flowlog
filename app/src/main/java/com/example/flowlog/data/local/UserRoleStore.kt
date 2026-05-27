package com.example.flowlog.data.local

import android.content.Context

enum class UserRole { USER, DEVELOPER }

class UserRoleStore(context: Context) {
    private val prefs = context.getSharedPreferences("user_role", Context.MODE_PRIVATE)

    fun roleForUid(uid: String?): UserRole =
        if (uid == "xrUHsuQ8l2WJtB6gt0Ynp0U5VkJ3") UserRole.DEVELOPER else UserRole.USER

    fun isDeveloperMode(): Boolean = prefs.getBoolean("dev_mode", false)

    fun setDeveloperMode(enabled: Boolean) {
        prefs.edit().putBoolean("dev_mode", enabled).apply()
    }
}
