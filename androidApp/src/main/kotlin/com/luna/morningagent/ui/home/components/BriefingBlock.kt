package com.luna.morningagent.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.morningagent.R
import com.luna.morningagent.data.PreviewData
import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.data.model.BriefingKind
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningThemes
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning
import com.luna.morningagent.ui.theme.slotCopy

/**
 * v4 briefing block — flat magazine layout, no card chrome.
 *
 * Top row: "BRIEFING" mono accent label (left) + "{model} · {tokens} tok"
 * mono meta (right). Below: serif-read 18sp body with a 2dp accent left
 * border, padding 12dp from the bar.
 *
 * When `briefing` is null (loading / pre-first-run), falls back to the
 * slot's mockup quote so the block still reads as a "today's mood"
 * placeholder rather than empty space.
 */
@Composable
fun BriefingBlock(
    briefing: Briefing?,
    modifier: Modifier = Modifier,
    canSwipe: Boolean = false,
    displayedKind: BriefingKind = BriefingKind.MORNING,
) {
    val morning = MaterialTheme.morning
    val copy = slotCopy()

    val sectionLabel = if (displayedKind == BriefingKind.EVENING) {
        stringResource(R.string.section_reflection)
    } else {
        stringResource(R.string.section_briefing)
    }

    val bodyText = briefing?.summary ?: stringResource(copy.quote)
    val metaText = briefing?.let {
        buildString {
            append(it.model)
            if (it.tokens > 0) {
                append("  ·  ")
                append(it.tokens)
                append(" tok")
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text  = sectionLabel,
                style = MorningType.LabelMono,
                color = morning.accent,
                modifier = Modifier.weight(1f),
            )
            if (canSwipe) {
                val hint = if (displayedKind == BriefingKind.EVENING) {
                    "← MORNING"
                } else {
                    "EVENING →"
                }
                Text(
                    text  = hint,
                    style = MorningType.MetaMono,
                    color = morning.textMuted.copy(alpha = 0.5f),
                )
            }
            if (metaText != null) {
                Text(
                    text      = metaText,
                    style     = MorningType.MetaMono,
                    color     = morning.textMuted,
                    textAlign = TextAlign.End,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(morning.accent),
            )
            Text(
                text     = bodyText,
                style    = MorningType.Quote,
                color    = morning.textPrimary,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun BriefingBlockPreview() {
    MorningAgentTheme {
        Box(modifier = Modifier.padding(18.dp)) {
            BriefingBlock(briefing = PreviewData.sampleBriefing)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD, name = "Loading (slot quote fallback)")
@Composable
private fun BriefingBlockLoadingPreview() {
    MorningAgentTheme {
        Box(modifier = Modifier.padding(18.dp)) {
            BriefingBlock(briefing = null)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E, name = "Evening reflection")
@Composable
private fun BriefingBlockEveningPreview() {
    MorningAgentTheme(theme = MorningThemes.Dark) {
        Box(modifier = Modifier.padding(18.dp)) {
            BriefingBlock(
                briefing      = PreviewData.sampleEveningReflection,
                canSwipe      = true,
                displayedKind = BriefingKind.EVENING,
            )
        }
    }
}
