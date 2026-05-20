package com.luna.morningagent.data.secure

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Preferences DataStore extension for [TokenStore].
 *
 * The underlying file lives at
 *   `/data/data/com.luna.morningagent/files/datastore/morning_agent_tokens.preferences_pb`
 * and stores Tink-AEAD ciphertext for sensitive string values. The DataStore
 * itself is unencrypted at the framework level — the per-value Tink layer in
 * [TokenStore] is what protects the secrets.
 */
internal const val DATASTORE_NAME = "morning_agent_tokens"

internal val Context.tokenDataStore: DataStore<Preferences>
        by preferencesDataStore(name = DATASTORE_NAME)
