package com.project.zorvynone.model

import android.content.Context
import android.content.SharedPreferences

class AuthPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("zorvyn_auth", Context.MODE_PRIVATE)

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var email: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    fun logout() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_LOGGED_IN = "is_logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
    }
}
