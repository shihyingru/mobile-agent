package com.luna.morningagent.data.sharedposts

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * A post shared into Morning Agent via Android's ACTION_SEND intent.
 *
 * Local cache is the source of truth; Notion is a mirror. Posts arrive with
 * `pendingSync = true` when the SharedPosts Notion DB isn't configured yet
 * (or the create-page call failed) — they're flushed once the DB exists.
 *
 * `pendingCategorization` flips on every fresh save and clears once the
 * agent (commit 2) writes back `categories` + `summary`.
 */
@Serializable
data class SharedPost(
    val localId: String,                  // UUID, generated on intent receive
    val notionId: String? = null,         // Notion page id; null until createPage() succeeds
    val content: String,
    val source: String,                   // "Threads" / "Twitter" / "Web" / "Other"
    val author: String? = null,           // From EXTRA_SUBJECT or URL path; null when unknown
    val url: String? = null,              // First URL in the shared text; null for pure-text shares
    val categories: List<String> = emptyList(),
    val summary: String? = null,
    val savedAt: Instant,
    val status: String = STATUS_UNREAD,
    val pendingSync: Boolean = false,
    val pendingCategorization: Boolean = true,
) {
    companion object {
        const val STATUS_UNREAD  = "Unread"
        const val STATUS_READING = "Reading"
        const val STATUS_DONE    = "Done"

        const val SOURCE_THREADS = "Threads"
        const val SOURCE_TWITTER = "Twitter"
        const val SOURCE_WEB     = "Web"
        const val SOURCE_OTHER   = "Other"

        // Not a SharedPost source today — added so SourceLogo can render the
        // same monogram for the Home TaskCard, which labels every Notion task
        // with this glyph.
        const val SOURCE_NOTION  = "Notion"
    }
}
