package com.example.planory.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("planory_session", Context.MODE_PRIVATE)

    fun login(username: String) {
        prefs.edit()
            .putBoolean("logged_in", true)
            .putString("username", username)
            .apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun getUsername(): String? {
        return prefs.getString("username", null)
    }
}
