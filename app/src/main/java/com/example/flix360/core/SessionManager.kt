package com.example.flix360.core

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun isSessionValid(): Boolean = !getToken().isNullOrBlank()

    fun saveActiveLocationId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_LOCATION, id).apply()
    }

    fun getActiveLocationId(): String? = prefs.getString(KEY_ACTIVE_LOCATION, null)

    companion object {
        private const val PREFS_NAME = "flix360.secure.session"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_ACTIVE_LOCATION = "active_location_id"
    }
}