package com.luna.morningagent.ui.home.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.morningagent.data.sharedposts.SharedPost
import com.luna.morningagent.ui.theme.InterFamily
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.morning

/**
 * Source-aware single-color monogram. v4 redesign drops the brand-color chips
 * — each glyph renders in textSecondary inside a cardEdge-bordered square so
 * the visual weight matches the surrounding mono meta line. Glyph by source:
 *
 *  - Threads → "T"
 *  - Twitter / X → "X"
 *  - Web → globe icon (Material's Outlined.Language)
 *  - Notion → "N"
 *  - Other → first letter of source name uppercased
 *
 * Accepts the raw `source` string from SharedPost (one of SOURCE_*), so
 * callers don't have to map it through an intermediate enum.
 */
@Composable
fun SourceLogo(
    source: String = SharedPost.SOURCE_NOTION,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    val morning = MaterialTheme.morning
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .border(width = 1.dp, color = morning.cardEdge, shape = RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when (source) {
            SharedPost.SOURCE_WEB -> {
                // Web shares (URLs from a browser, an article, etc.) get the
                // globe — no good single-letter fits "Web" naturally.
                Icon(
                    imageVector        = Icons.Outlined.Language,
                    contentDescription = source,
                    tint               = morning.textSecondary,
                    modifier           = Modifier.size(size * 0.7f),
                )
            }
            else -> {
                Text(
                    text  = glyphFor(source),
                    color = morning.textSecondary,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily    = InterFamily,
                        fontWeight    = FontWeight.SemiBold,
                        fontSize      = (size.value * 0.6f).sp,
                        letterSpacing = (-0.3).sp,
                    ),
                )
            }
        }
    }
}

private fun glyphFor(source: String): String = when (source) {
    SharedPost.SOURCE_THREADS -> "T"
    SharedPost.SOURCE_TWITTER -> "X"
    SharedPost.SOURCE_NOTION  -> "N"
    else -> source.firstOrNull()?.uppercase() ?: "?"
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun SourceLogoPreview() {
    MorningAgentTheme {
        Row(
            modifier              = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SourceLogo(source = SharedPost.SOURCE_THREADS)
            SourceLogo(source = SharedPost.SOURCE_TWITTER)
            SourceLogo(source = SharedPost.SOURCE_WEB)
            SourceLogo(source = "Notion")
            SourceLogo(source = SharedPost.SOURCE_OTHER)
        }
    }
}
