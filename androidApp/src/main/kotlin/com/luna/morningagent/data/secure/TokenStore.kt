package com.luna.morningagent.data.secure

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Token + preferences store backed by Preferences DataStore with per-value
 * Tink AEAD encryption for sensitive strings.
 *
 * Callers use synchronous `get`/`save` methods. The sync feel is preserved
 * by warming an in-memory cache on construction (one `runBlocking` against
 * DataStore's first emission) and then serving every read from that cache.
 * Writes update the cache immediately and fire an async `dataStore.edit { }`
 * on a private IO scope.
 *
 * Encryption split:
 *  - String values (API keys, tokens, briefing JSON, model ids) are sealed
 *    via Tink AEAD and stored as base64 ciphertext.
 *  - Booleans / ints (auto-run flag, briefing hour/minute) are not secrets;
 *    they sit in DataStore natively. Encrypting them would just burn CPU.
 *
 * Crypto failure: a bad ciphertext for a single key is swallowed
 * (`runCatching` in [unsealString]) — getter returns null/default, user
 * re-enters the value in Settings. Hard Keystore failure at construction
 * throws — there's nothing useful to do with a broken crypto layer.
 */
class TokenStore(context: Context) {

    private val appContext = context.applicationContext
    private val crypto     = TinkCrypto(appContext)
    private val dataStore  = appContext.tokenDataStore
    private val scope      = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache      = ConcurrentHashMap<String, Any>()

    init {
        runBlocking {
            populateCacheFrom(dataStore.data.first())
        }
    }

    // --- Public sync API ---------------------------------------------------

    fun saveGeminiKey(key: String) = writeString(KEY_GEMINI, key)
    fun getGeminiKey(): String? = readString(KEY_GEMINI)

    fun saveNotionToken(token: String) = writeString(KEY_NOTION, token)
    fun getNotionToken(): String? = readString(KEY_NOTION)

    fun saveNotionDatabaseId(id: String) = writeString(KEY_NOTION_DB, id)
    fun getNotionDatabaseId(): String? = readString(KEY_NOTION_DB)

    fun saveAutoRun(enabled: Boolean) = writeBoolean(KEY_AUTO_RUN, enabled)
    fun getAutoRun(): Boolean = readBoolean(KEY_AUTO_RUN, true)

    fun saveGeminiModel(id: String) = writeString(KEY_GEMINI_MODEL, id)
    fun getGeminiModel(): String = readString(KEY_GEMINI_MODEL) ?: DEFAULT_GEMINI_MODEL

    fun saveClaudeKey(key: String) = writeString(KEY_CLAUDE, key)
    fun getClaudeKey(): String? = readString(KEY_CLAUDE)

    fun saveClaudeModel(id: String) = writeString(KEY_CLAUDE_MODEL, id)
    fun getClaudeModel(): String = readString(KEY_CLAUDE_MODEL) ?: DEFAULT_CLAUDE_MODEL

    fun saveSelectedProvider(id: String) = writeString(KEY_PROVIDER, id)
    fun getSelectedProvider(): String = readString(KEY_PROVIDER) ?: DEFAULT_PROVIDER

    fun saveDailyBriefingEnabled(enabled: Boolean) = writeBoolean(KEY_DAILY_BRIEFING, enabled)
    fun getDailyBriefingEnabled(): Boolean = readBoolean(KEY_DAILY_BRIEFING, false)

    fun saveDailyBriefingTime(hour: Int, minute: Int) {
        writeInt(KEY_BRIEFING_HOUR,   hour.coerceIn(0, 23))
        writeInt(KEY_BRIEFING_MINUTE, minute.coerceIn(0, 59))
    }
    fun getDailyBriefingHour(): Int   = readInt(KEY_BRIEFING_HOUR,   DEFAULT_HOUR)
    fun getDailyBriefingMinute(): Int = readInt(KEY_BRIEFING_MINUTE, DEFAULT_MINUTE)

    fun saveLastBriefingJson(json: String) = writeString(KEY_LAST_BRIEFING, json)
    fun getLastBriefingJson(): String? = readString(KEY_LAST_BRIEFING)

    // --- Cache + DataStore + Tink helpers ----------------------------------

    private fun readString(key: String): String? = cache[key] as? String

    private fun readBoolean(key: String, default: Boolean): Boolean =
        cache[key] as? Boolean ?: default

    private fun readInt(key: String, default: Int): Int =
        cache[key] as? Int ?: default

    private fun writeString(key: String, value: String) {
        cache[key] = value
        val sealed = sealString(value)
        scope.launch {
            dataStore.edit { it[stringPreferencesKey(key)] = sealed }
        }
    }

    private fun writeBoolean(key: String, value: Boolean) {
        cache[key] = value
        scope.launch {
            dataStore.edit { it[booleanPreferencesKey(key)] = value }
        }
    }

    private fun writeInt(key: String, value: Int) {
        cache[key] = value
        scope.launch {
            dataStore.edit { it[intPreferencesKey(key)] = value }
        }
    }

    private fun sealString(value: String): String =
        Base64.encodeToString(crypto.encrypt(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)

    private fun unsealString(cipher: String): String? =
        runCatching {
            String(crypto.decrypt(Base64.decode(cipher, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()

    private fun populateCacheFrom(prefs: Preferences) {
        STRING_KEYS.forEach { key ->
            prefs[stringPreferencesKey(key)]?.let { cipher ->
                unsealString(cipher)?.let { plain -> cache[key] = plain }
            }
        }
        BOOLEAN_KEYS.forEach { key ->
            prefs[booleanPreferencesKey(key)]?.let { cache[key] = it }
        }
        INT_KEYS.forEach { key ->
            prefs[intPreferencesKey(key)]?.let { cache[key] = it }
        }
    }

    companion object {
        private const val KEY_GEMINI          = "gemini_api_key"
        private const val KEY_NOTION          = "notion_token"
        private const val KEY_NOTION_DB       = "notion_database_id"
        private const val KEY_AUTO_RUN        = "auto_run_on_launch"
        private const val KEY_GEMINI_MODEL    = "gemini_model_id"
        private const val KEY_DAILY_BRIEFING  = "daily_briefing_enabled"
        private const val KEY_BRIEFING_HOUR   = "daily_briefing_hour"
        private const val KEY_BRIEFING_MINUTE = "daily_briefing_minute"
        private const val KEY_LAST_BRIEFING   = "last_briefing_json"
        private const val KEY_CLAUDE          = "claude_api_key"
        private const val KEY_CLAUDE_MODEL    = "claude_model_id"
        private const val KEY_PROVIDER        = "selected_provider"

        private const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
        private const val DEFAULT_CLAUDE_MODEL = "claude-sonnet-4-6"
        private const val DEFAULT_PROVIDER     = "gemini"
        private const val DEFAULT_HOUR         = 9
        private const val DEFAULT_MINUTE       = 0

        // Tink-sealed (sensitive or text). Plain in DataStore (bool / int — not secrets).
        private val STRING_KEYS = listOf(
            KEY_GEMINI, KEY_NOTION, KEY_NOTION_DB,
            KEY_GEMINI_MODEL, KEY_CLAUDE, KEY_CLAUDE_MODEL,
            KEY_PROVIDER, KEY_LAST_BRIEFING,
        )
        private val BOOLEAN_KEYS = listOf(KEY_AUTO_RUN, KEY_DAILY_BRIEFING)
        private val INT_KEYS     = listOf(KEY_BRIEFING_HOUR, KEY_BRIEFING_MINUTE)
    }
}
