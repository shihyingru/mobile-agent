package com.luna.morningagent.data.secure

import android.content.Context

// TODO(Phase 2): replace with EncryptedSharedPreferences (androidx.security:security-crypto)
class TokenStore(context: Context) {
    fun saveGeminiKey(key: String) { TODO("Phase 2: EncryptedSharedPreferences") }
    fun getGeminiKey(): String?    { TODO("Phase 2: EncryptedSharedPreferences") }

    fun saveNotionToken(token: String) { TODO("Phase 2: EncryptedSharedPreferences") }
    fun getNotionToken(): String?      { TODO("Phase 2: EncryptedSharedPreferences") }
}
