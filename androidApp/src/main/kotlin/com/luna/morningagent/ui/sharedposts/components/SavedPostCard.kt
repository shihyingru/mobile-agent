package com.luna.morningagent.ui.sharedposts.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.morningagent.data.sharedposts.SharedPost
import com.luna.morningagent.ui.home.components.SourceLogo
import com.luna.morningagent.ui.home.components.TaskSource
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.toJavaInstant

/**
 * v4 saved-post card. Matches the magazine flat language: surface bg + cardEdge
 * hairline, serif italic body, mono meta.
 *
 *  Top row: source monogram · author · saved date · pending dot (when sync
 *  hasn't completed yet — small idle gold dot, mirrors StatusDot.Idle).
 *  Body:    content clamped to 3 lines, serif italic. Summary shown below
 *           when the agent has filled it in.
 *  Footer:  category chips + chevron (accent).
 *
 * Tap = open externally (caller routes to ACTION_VIEW when URL is present, or
 *       toggles inline expand for text-only shares). Long-press = caller can
 *       show a delete confirm.
 */
@Composable
fun SavedPostCard(
    post: SharedPost,
    expanded: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "savedCardScale")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(morning.surface)
            .border(width = 1.dp, color = morning.cardEdge, shape = RoundedCornerShape(16.dp))
            .pointerInput(post.localId) {
                detectTapGestures(
                    onPress     = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap       = { onTap() },
                    onLongPress = { onLongPress() },
                )
            }
            .padding(14.dp),
    ) {
        // Top row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SourceLogo(source = mapSource(post.source), size = 16.dp)
            post.author?.takeIf { it.isNotBlank() }?.let { author ->
                Text(
                    text     = author,
                    style    = MorningType.Caption,
                    color    = morning.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text  = formatDate(post.savedAt),
                style = MorningType.MetaMono,
                color = morning.textMuted,
            )
            if (post.pendingSync) {
                Spacer(modifier = Modifier.size(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(morning.gold),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Body — content
        Text(
            text     = post.content,
            style    = MorningType.Tip.copy(fontSize = androidx.compose.ui.unit.TextUnit(15f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color    = morning.textPrimary,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
        )

        // Agent summary (italic accent — "why this mattered")
        post.summary?.takeIf { it.isNotBlank() }?.let { summary ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text     = summary,
                style    = MorningType.BodyReadItalic.copy(fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color    = morning.textSecondary,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Footer — categories + tap-to-open chevron
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CategoryChipRow(categories = post.categories, modifier = Modifier.weight(1f))
            if (post.url != null) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint               = morning.accent.copy(alpha = 0.7f),
                    modifier           = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryChipRow(categories: List<String>, modifier: Modifier = Modifier) {
    val morning = MaterialTheme.morning
    if (categories.isEmpty()) return
    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Cap at 3 visible chips to keep the row tidy; the rest collapse to "+N".
        val visible = categories.take(3)
        val overflow = categories.size - visible.size
        visible.forEach { name ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(morning.accentSoft)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text  = name,
                    style = MorningType.Caption.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)),
                    color = morning.accent,
                )
            }
        }
        if (overflow > 0) {
            Text(
                text  = "+$overflow",
                style = MorningType.Caption.copy(fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = morning.textMuted,
            )
        }
    }
}

private fun mapSource(source: String): TaskSource = TaskSource.Notion
// All shipped SourceLogo variants are Notion-only until commit 4 ships
// Threads/Twitter/Web glyphs — until then everyone gets the "N". The data
// still records source correctly, so when SourceLogo grows new branches
// the cards will pick them up automatically.

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d · h:mm a", Locale.ENGLISH)

private fun formatDate(instant: kotlin.time.Instant): String =
    DATE_FORMAT.format(instant.toJavaInstant().atZone(ZoneId.systemDefault()))

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun SavedPostCardPreview() {
    MorningAgentTheme {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SavedPostCard(
                post = SharedPost(
                    localId    = "1",
                    notionId   = "abc",
                    content    = "Almost everything in today's AI hype cycle was sketched out around 2019–2020 — transformers were already standard, scaling laws public, RLHF being explored.",
                    source     = SharedPost.SOURCE_THREADS,
                    author     = "@haileymocaixi",
                    url        = "https://threads.net/@haileymocaixi/post/test",
                    categories = listOf("Tech", "Ideas"),
                    summary    = "Argues today's AI breakthroughs were largely sketched out years ago.",
                    savedAt    = kotlin.time.Clock.System.now(),
                ),
                expanded    = false,
                onTap       = {},
                onLongPress = {},
            )
            SavedPostCard(
                post = SharedPost(
                    localId    = "2",
                    notionId   = null,
                    content    = "Quick text-only note. No URL, just an idea.",
                    source     = SharedPost.SOURCE_OTHER,
                    savedAt    = kotlin.time.Clock.System.now(),
                    pendingSync = true,
                ),
                expanded    = false,
                onTap       = {},
                onLongPress = {},
            )
        }
    }
}
