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

    // Auto-run preference: when true, Home runs the agent on first composition if
    // both keys are configured; when false, the user must tap Run Now.
    fun saveAutoRun(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_RUN, enabled).apply()
    fun getAutoRun(): Boolean = prefs.getBoolean(KEY_AUTO_RUN, true)

    // Selected Gemini model id (stable, e.g. "gemini-2.5-flash"). Resolved through
    // GeminiModelOption.fromId at the agent layer so unknown ids fall back safely.
    fun saveGeminiModel(id: String) = prefs.edit().putString(KEY_GEMINI_MODEL, id).apply()
    fun getGeminiModel(): String = prefs.getString(KEY_GEMINI_MODEL, null) ?: DEFAULT_GEMINI_MODEL

    // Anthropic Claude side — runs in parallel to the Gemini fields above. Each
    // provider's key + model preference is independent so the user can flip
    // providers in Settings without re-entering keys or losing model picks.
    fun saveClaudeKey(key: String) = prefs.edit().putString(KEY_CLAUDE, key).apply()
    fun getClaudeKey(): String? = prefs.getString(KEY_CLAUDE, null)

    fun saveClaudeModel(id: String) = prefs.edit().putString(KEY_CLAUDE_MODEL, id).apply()
    fun getClaudeModel(): String = prefs.getString(KEY_CLAUDE_MODEL, null) ?: DEFAULT_CLAUDE_MODEL

    // Which provider the agent layer talks to. Defaults to Gemini for users who
    // had Gemini-only configs before Claude support landed.
    fun saveSelectedProvider(id: String) = prefs.edit().putString(KEY_PROVIDER, id).apply()
    fun getSelectedProvider(): String = prefs.getString(KEY_PROVIDER, null) ?: DEFAULT_PROVIDER

    // Daily WorkManager schedule. When true, MainActivity enqueues the
    // PeriodicWorkRequest on launch; toggling off cancels the unique work.
    fun saveDailyBriefingEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_DAILY_BRIEFING, enabled).apply()
    fun getDailyBriefingEnabled(): Boolean = prefs.getBoolean(KEY_DAILY_BRIEFING, false)

    // Briefing time-of-day. Stored as two ints to dodge timezone / parsing issues
    // — the scheduler combines them with the device's current ZoneId at enqueue time.
    fun saveDailyBriefingTime(hour: Int, minute: Int) =
        prefs.edit()
            .putInt(KEY_BRIEFING_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_BRIEFING_MINUTE, minute.coerceIn(0, 59))
            .apply()
    fun getDailyBriefingHour(): Int = prefs.getInt(KEY_BRIEFING_HOUR, DEFAULT_HOUR)
    fun getDailyBriefingMinute(): Int = prefs.getInt(KEY_BRIEFING_MINUTE, DEFAULT_MINUTE)

    // Last successful briefing as JSON. Written by the worker so HomeViewModel can
    // surface yesterday's-morning state on cold launch without re-running the agent.
    fun saveLastBriefingJson(json: String) =
        prefs.edit().putString(KEY_LAST_BRIEFING, json).apply()
    fun getLastBriefingJson(): String? = prefs.getString(KEY_LAST_BRIEFING, null)

    companion object {
        private const val FILE_NAME = "morning_agent_secrets"
        private const val KEY_GEMINI = "gemini_api_key"
        private const val KEY_NOTION = "notion_token"
        private const val KEY_NOTION_DB = "notion_database_id"
        private const val KEY_AUTO_RUN = "auto_run_on_launch"
        private const val KEY_GEMINI_MODEL = "gemini_model_id"
        private const val KEY_DAILY_BRIEFING = "daily_briefing_enabled"
        private const val KEY_BRIEFING_HOUR = "daily_briefing_hour"
        private const val KEY_BRIEFING_MINUTE = "daily_briefing_minute"
        private const val KEY_LAST_BRIEFING = "last_briefing_json"
        private const val KEY_CLAUDE = "claude_api_key"
        private const val KEY_CLAUDE_MODEL = "claude_model_id"
        private const val KEY_PROVIDER = "selected_provider"
        private const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
        private const val DEFAULT_CLAUDE_MODEL = "claude-sonnet-4-6"
        private const val DEFAULT_PROVIDER = "gemini"
        private const val DEFAULT_HOUR = 9
        private const val DEFAULT_MINUTE = 0
    }
}
