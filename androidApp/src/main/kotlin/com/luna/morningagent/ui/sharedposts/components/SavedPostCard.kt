package com.luna.morningagent.ui.sharedposts.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.luna.morningagent.R
import com.luna.morningagent.data.sharedposts.SharedPost
import com.luna.morningagent.ui.theme.InterFamily
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.SerifReadFamily
import com.luna.morningagent.ui.theme.morning
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.toJavaInstant
import kotlinx.coroutines.launch

/**
 * Saved post card (DESIGN_SYSTEM §3.18 / §3.19).
 *
 * Card chrome only carries [logo · source · author] / [timestamp · ⋯] and the
 * body. Destructive action never lives on the card surface — Delete is reached
 * via swipe-left (red panel) or the ⋯ overflow sheet. Unread shows as a 2 dp
 * accent rule at the inset left edge.
 *
 * Swipe behavior (two-tier):
 *  · 0 – 36 dp drag: snaps back on release.
 *  · 36 – 66 dp:    parks at 88 dp reveal; tap Delete to commit.
 *  · > 66 dp:       full-bleed commit on release (no extra tap).
 */
private val DELETE_RED = Color(0xFFE5484D)
private val SWIPE_EASING = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SavedPostCard(
    post: SharedPost,
    onTap: () -> Unit,
    onOverflow: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    bodyMaxLines: Int = 4,
) {
    val morning = MaterialTheme.morning
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val revealPx  = with(density) { 88.dp.toPx() }
    val parkPx    = with(density) { 36.dp.toPx() }
    val commitPx  = with(density) { 66.dp.toPx() }

    val offsetX = remember(post.localId) { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
    ) {
        // Underlay — red Delete panel revealed on swipe.
        Row(
            modifier              = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier         = Modifier
                    .width(88.dp)
                    .fillMaxHeight()
                    .background(DELETE_RED)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = {
                            scope.launch {
                                offsetX.animateTo(-revealPx * 4, tween(280, easing = SWIPE_EASING))
                                onDelete()
                            }
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.cd_delete_post),
                        tint               = Color.White,
                        modifier           = Modifier.size(18.dp),
                    )
                    Text(
                        text  = "Delete",
                        color = Color.White,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily    = InterFamily,
                            fontWeight    = FontWeight.Bold,
                            fontSize      = 10.5.sp,
                            letterSpacing = 0.3.sp,
                        ),
                    )
                }
            }
        }

        // Card surface — translated by drag offset.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state         = rememberDraggableState { delta ->
                        scope.launch {
                            val next = (offsetX.value + delta).coerceIn(-revealPx * 4, 0f)
                            offsetX.snapTo(next)
                        }
                    },
                    orientation   = Orientation.Horizontal,
                    onDragStopped = { _ ->
                        val absOffset = offsetX.value.absoluteValue
                        when {
                            absOffset > commitPx -> {
                                offsetX.animateTo(-revealPx * 4, tween(280, easing = SWIPE_EASING))
                                onDelete()
                            }
                            absOffset > parkPx -> {
                                offsetX.animateTo(-revealPx, tween(220, easing = SWIPE_EASING))
                            }
                            else -> {
                                offsetX.animateTo(0f, tween(220, easing = SWIPE_EASING))
                            }
                        }
                    },
                ),
        ) {
            val unread        = post.status == SharedPost.STATUS_UNREAD
            val accent        = morning.accent
            val railWidthPx   = with(density) { 2.dp.toPx() }
            val railInsetPx   = with(density) { 14.dp.toPx() }
            val railRadiusPx  = with(density) { 1.dp.toPx() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(morning.surface)
                    .border(width = 1.dp, color = morning.cardEdge, shape = RoundedCornerShape(16.dp))
                    .drawBehind {
                        // Unread rule painted relative to the card's actual measured
                        // size — fillMaxHeight() collapsed to 0 because the LazyColumn
                        // item gives loose vertical constraints, so a sibling Box can't
                        // resolve a height. drawBehind sees the real size.
                        if (unread) {
                            drawRoundRect(
                                color        = accent,
                                topLeft      = Offset(0f, railInsetPx),
                                size         = Size(railWidthPx, size.height - 2 * railInsetPx),
                                cornerRadius = CornerRadius(railRadiusPx, railRadiusPx),
                            )
                        }
                    }
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = {
                            // Tap on a parked card snaps it shut; otherwise route normally.
                            if (offsetX.value != 0f) {
                                scope.launch { offsetX.animateTo(0f, tween(220, easing = SWIPE_EASING)) }
                            } else {
                                onTap()
                            }
                        },
                        onLongClick       = onOverflow,
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                ) {
                    PostMetaRow(post = post, onOverflow = onOverflow)
                    Spacer(modifier = Modifier.height(9.dp))

                    val hasImage = !post.imageUrl.isNullOrBlank()
                    // Body sits next to a thumbnail when we have one; clamped a
                    // line shorter so the card height stays balanced. Without an
                    // image, text uses the full width with the original clamp.
                    if (hasImage) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment     = Alignment.Top,
                        ) {
                            BodyOrLinkPlaceholder(
                                post     = post,
                                maxLines = (bodyMaxLines - 1).coerceAtLeast(2),
                                modifier = Modifier.weight(1f),
                            )
                            BookmarkThumbnail(imageUrl = post.imageUrl!!)
                        }
                    } else {
                        BodyOrLinkPlaceholder(
                            post     = post,
                            maxLines = bodyMaxLines,
                        )
                    }
                }
            }
        }
    }

    // Reset swipe state if the underlying post id ever rebinds (LazyColumn reuse).
    LaunchedEffect(post.localId) { offsetX.snapTo(0f) }
}

@Composable
private fun PostMetaRow(post: SharedPost, onOverflow: () -> Unit) {
    val morning = MaterialTheme.morning
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier              = Modifier.weight(1f),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            CategoryGlyph(post = post)
            Text(
                text     = sourceLine(post),
                color    = morning.textSecondary,
                style    = androidx.compose.ui.text.TextStyle(
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 12.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text  = formatDate(post.savedAt),
            color = morning.textMuted,
            style = MorningType.Caption,
        )
        Box(
            modifier         = Modifier
                .size(width = 26.dp, height = 22.dp)
                .clip(RoundedCornerShape(6.dp))
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onOverflow,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Rounded.MoreHoriz,
                contentDescription = stringResource(R.string.cd_post_overflow),
                tint               = morning.textMuted,
                modifier           = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Bordered monochrome 14dp tile showing the first category's initial. Source
 * info already lives in the meta text ("Threads · @author"), so the glyph
 * surfaces the category at a glance instead of duplicating the source name.
 * Falls back to a middle dot while categorization is in flight.
 */
@Composable
private fun CategoryGlyph(post: SharedPost) {
    val morning = MaterialTheme.morning
    val glyph = when {
        post.pendingCategorization      -> "·"
        post.categories.isEmpty()       -> "·"
        else -> post.categories.first().firstOrNull()?.uppercase() ?: "·"
    }
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(width = 1.dp, color = morning.cardEdge, shape = RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = glyph,
            color = morning.textSecondary,
            style = androidx.compose.ui.text.TextStyle(
                fontFamily    = InterFamily,
                fontWeight    = FontWeight.SemiBold,
                fontSize      = 8.5.sp,
                letterSpacing = (-0.3).sp,
            ),
        )
    }
}

/**
 * Body slot. When the content is just the post URL (Threads login-walled
 * posts, or older cards saved before the fetcher could enrich them) — we
 * render a quiet italic "Tap to open in <Source>" placeholder instead of
 * pasting the URL string into the body. Keeps the card looking intentional
 * rather than broken when og:description recovery wasn't possible.
 */
@Composable
private fun BodyOrLinkPlaceholder(
    post: SharedPost,
    maxLines: Int,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val isUrlOnly = post.content.startsWith("http") && post.content.none { it.isWhitespace() }
    if (isUrlOnly) {
        Text(
            text     = "Tap to open in ${post.source}",
            color    = morning.textMuted,
            style    = MorningType.BodyReadItalic.copy(fontSize = 14.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    } else {
        Text(
            text     = post.content,
            color    = morning.textPrimary,
            style    = androidx.compose.ui.text.TextStyle(
                fontFamily    = SerifReadFamily,
                fontWeight    = FontWeight.Normal,
                fontSize      = 15.5.sp,
                lineHeight    = (15.5f * 1.45f).sp,
                letterSpacing = (-0.05).sp,
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}

/**
 * 88dp square thumbnail rendered from the post's og:image URL. Loading uses
 * Coil; CDN URLs from Instagram/Threads are signed with an `oe=<expiry>` —
 * once expired (typical TTL: a couple of weeks) we'd 403, so the error slot
 * just paints a muted surface and the card degrades to text-only-look.
 */
@Composable
private fun BookmarkThumbnail(imageUrl: String) {
    val morning = MaterialTheme.morning
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(morning.surfaceRaised),
    ) {
        AsyncImage(
            model              = imageUrl,
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.size(88.dp),
        )
    }
}

private fun sourceLine(post: SharedPost): String {
    val author = post.author?.takeIf { it.isNotBlank() }
    return if (author != null) "${post.source} · $author" else post.source
}

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
                    content    = "The best way to predict the future is to read research papers from five years ago. Almost everything in today's AI hype cycle was sketched out around 2019–2020.",
                    source     = SharedPost.SOURCE_THREADS,
                    author     = "@haileymocaixi",
                    url        = "https://threads.net/@haileymocaixi/post/test",
                    savedAt    = kotlin.time.Clock.System.now(),
                ),
                onTap       = {},
                onOverflow  = {},
                onDelete    = {},
            )
            SavedPostCard(
                post = SharedPost(
                    localId     = "2",
                    notionId    = null,
                    content     = "Quick text-only note. No URL, just an idea.",
                    source      = SharedPost.SOURCE_OTHER,
                    savedAt     = kotlin.time.Clock.System.now(),
                    pendingSync = true,
                    status      = SharedPost.STATUS_DONE,
                ),
                onTap       = {},
                onOverflow  = {},
                onDelete    = {},
            )
        }
    }
}
