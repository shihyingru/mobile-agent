package com.luna.morningagent.ui.home.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    val morning = MaterialTheme.morning
    Row(
        modifier          = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text      = text,
            style     = MorningType.Label,
            color     = morning.textMuted,
            modifier  = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text      = trailing,
                style     = MorningType.Mono,
                color     = morning.textMuted,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun SectionLabelPreview() {
    MorningAgentTheme {
        SectionLabel(text = "TODAY'S BRIEFING", trailing = "GENERATED 09:02")
    }
}
