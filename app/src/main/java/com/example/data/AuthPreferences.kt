package com.example.data

import android.content.Context

class AuthPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isRememberMeEnabled(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, false)

    fun getRememberedIdentifier(): String {
        return if (isRememberMeEnabled()) {
            prefs.getString(KEY_IDENTIFIER, "").orEmpty()
        } else {
            ""
        }
    }

    fun saveLoginPreference(rememberMe: Boolean, identifier: String) {
        prefs.edit().apply {
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            if (rememberMe && identifier.isNotBlank()) {
                putString(KEY_IDENTIFIER, identifier.trim())
            } else {
                remove(KEY_IDENTIFIER)
            }
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "zentrix_auth_prefs"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_IDENTIFIER = "remembered_identifier"
    }
}
