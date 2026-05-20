package com.luna.morningagent.data.secure

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager

/**
 * Thin Tink AEAD wrapper used by [TokenStore] to seal every string value
 * before it lands in DataStore.
 *
 * The Tink keyset itself sits in a regular `SharedPreferences` file
 * (`morning_agent_tink_keyset`) — but each key in that file is wrapped at
 * rest by an Android Keystore master key (`morning_agent_master_key_v1`).
 * The Keystore key is hardware-backed where available and never leaves the
 * device, so extracting the prefs file alone yields ciphertext.
 *
 * A hard Keystore failure here throws: there's nothing useful to do with a
 * broken crypto layer, and silently regenerating the keyset would orphan
 * every existing ciphertext.
 */
internal class TinkCrypto(context: Context) {

    private val aead: Aead = run {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(
                context.applicationContext,
                KEYSET_NAME,
                KEYSET_PREF_FILE,
            )
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri("android-keystore://$MASTER_KEY_ALIAS")
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    fun encrypt(plaintext: ByteArray): ByteArray = aead.encrypt(plaintext, null)
    fun decrypt(ciphertext: ByteArray): ByteArray = aead.decrypt(ciphertext, null)

    companion object {
        private const val KEYSET_PREF_FILE = "morning_agent_tink_keyset"
        private const val KEYSET_NAME      = "morning_agent_token_keyset"
        private const val MASTER_KEY_ALIAS = "morning_agent_master_key_v1"
    }
}
