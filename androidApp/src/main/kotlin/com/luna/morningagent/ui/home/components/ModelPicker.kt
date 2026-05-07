package com.luna.morningagent.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.morningagent.R
import com.luna.morningagent.data.agent.GeminiModelOption
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

@Composable
fun ModelPicker(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionLabel(text = stringResource(R.string.section_model))
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GeminiModelOption.entries.forEach { option ->
                ModelChip(
                    label    = option.displayName,
                    tagline  = option.tagline,
                    selected = option.id == selected,
                    onClick  = { onSelect(option.id) },
                )
            }
        }
    }
}

@Composable
private fun ModelChip(
    label: String,
    tagline: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val morning = MaterialTheme.morning
    val bg = if (selected) morning.accent.copy(alpha = 0.12f) else morning.surface
    val borderColor = if (selected) morning.accent else morning.border
    val labelColor = if (selected) morning.accent else morning.textPrimary
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text  = label,
            style = MorningType.Body,
            color = labelColor,
        )
        Text(
            text  = tagline,
            style = MorningType.Label,
            color = morning.textMuted,
        )
    }
}

@Preview(name = "Model Picker", showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun ModelPickerPreview() {
    MorningAgentTheme {
        ModelPicker(
            selected = "gemini-2.5-flash",
            onSelect = {},
            modifier = Modifier.padding(24.dp),
        )
    }
}