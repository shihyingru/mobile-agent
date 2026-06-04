package com.luna.morningagent.ui.sharedposts

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import java.net.URLEncoder

/**
 * Opens a free-text place query in the user's map app. Threads content is
 * mostly Korean for Luna, so Hangul queries walk a Korea-first fallback
 * chain — Naver app → Kakao app → Naver web → Google web. Non-Korean
 * queries skip straight to Google. First scheme that resolves wins.
 *
 * Naver app first because if Luna has Naver installed it's almost certainly
 * her default. Kakao app second because Kakao is the other dominant Korean
 * map ecosystem — if Naver isn't installed but Kakao is, we keep her in
 * native-app land. Naver web before Google so POI data stays Korean when
 * neither app is installed. Google web at the tail as the universal
 * last-resort (any device with a browser will open it).
 */
fun openLocationInMap(context: Context, query: String, mapsUri: String? = null) {
    if (query.isBlank()) return
    val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
    val korean  = query.any { it.isHangul() }

    // Prefer the resolved place's Google Maps deep link (points at the exact
    // place by id) over a fresh text search when we have it.
    val googleTarget = mapsUri?.takeIf { it.startsWith("http") }
        ?: "https://www.google.com/maps/search/?api=1&query=$encoded"

    val attempts = buildList {
        if (korean) {
            add("nmap://search?query=$encoded&appname=com.luna.morningagent")
            add("kakaomap://search?q=$encoded")
            add("https://map.naver.com/p/search/$encoded")
        }
        add(googleTarget)
    }

    for (uri in attempts) {
        if (tryStart(context, uri)) return
    }
    Log.w(TAG, "no handler resolved for any map intent (query=$query)")
}

private fun tryStart(context: Context, uri: String): Boolean = runCatching {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, uri.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
    true
}.getOrElse { e ->
    if (e is ActivityNotFoundException) false else { Log.w(TAG, "map open failed: ${e.message}"); false }
}

/** Hangul syllables + Jamo. Hanja deliberately excluded — it overlaps with
 *  Chinese, so we'd misroute Taiwanese / HK shares to Naver. */
private fun Char.isHangul(): Boolean = code in 0xAC00..0xD7A3 || code in 0x3131..0x318E

private const val TAG = "MapIntents"
