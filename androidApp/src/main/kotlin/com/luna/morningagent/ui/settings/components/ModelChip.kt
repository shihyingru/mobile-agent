package com.luna.morningagent.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

/**
 * v4 model chip — vertical list row in the Settings model picker.
 * Active = accentSoft bg + accent border + accent radio.
 */
@Composable
fun ModelChip(
    label: String,
    note: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) morning.accentSoft else morning.surface)
            .border(
                width = 1.dp,
                color = if (selected) morning.accent else morning.cardEdge,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = label,
                style = MorningType.ModelId,
                color = morning.textPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = note,
                style = MorningType.BodyReadItalic.copy(fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = morning.textSecondary,
            )
        }
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (selected) morning.accent else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (selected) morning.accent else morning.textMuted,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector        = Icons.Rounded.Check,
                    contentDescription = null,
                    tint               = morning.onAccent,
                    modifier           = Modifier.size(10.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun ModelChipPreview() {
    MorningAgentTheme {
        Column(
            modifier            = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModelChip(label = "gemini-2.5-pro",   note = "Smartest · tight free quota",  selected = true,  onClick = {})
            ModelChip(label = "gemini-2.5-flash", note = "Default · solid free quota",   selected = false, onClick = {})
            ModelChip(label = "gemini-2.5-flash-lite", note = "Fastest · most free quota", selected = false, onClick = {})
        }
    }
}
