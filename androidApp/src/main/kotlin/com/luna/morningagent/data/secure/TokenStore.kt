package com.luna.morningagent.data.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val appContext = context.applicationContext
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveGeminiKey(key: String) = prefs.edit().putString(KEY_GEMINI, key).apply()
    fun getGeminiKey(): String? = prefs.getString(KEY_GEMINI, null)

    fun saveNotionToken(token: String) = prefs.edit().putString(KEY_NOTION, token).apply()
    fun getNotionToken(): String? = prefs.getString(KEY_NOTION, null)

    fun saveNotionDatabaseId(id: String) = prefs.edit().putString(KEY_NOTION_DB, id).apply()
    fun getNotionDatabaseId(): String? = prefs.getString(KEY_NOTION_DB, null)

    companion object {
        private const val FILE_NAME = "morning_agent_secrets"
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_NOTION = "notion_token"
        private const val KEY_NOTION_DB = "notion_database_id"
    }
}
