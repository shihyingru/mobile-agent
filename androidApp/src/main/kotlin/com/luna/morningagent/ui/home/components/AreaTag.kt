package com.luna.morningagent.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

// Tinted with accent so Area reads as distinct context, different from the
// priority-palette colors used by PriorityPill. Each known area name gets a
// dedicated icon for at-a-glance recognition; unknown names fall back to text-only.
@Composable
fun AreaTag(
    name: String,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val icon = areaIconFor(name)
    Row(
        modifier              = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(morning.accent.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = morning.accent,
                modifier           = Modifier.size(14.dp),
            )
        }
        Text(
            text  = name,
            style = MorningType.Label,
            color = morning.accent,
        )
    }
}

// Match the five expected Notion Area pages. Case- and whitespace-insensitive.
private fun areaIconFor(name: String): ImageVector? = when (name.trim().lowercase()) {
    "work"    -> Icons.Rounded.Work
    "life"    -> Icons.Rounded.SelfImprovement
    "home"    -> Icons.Rounded.Home
    "healthy" -> Icons.Rounded.FitnessCenter
    "learn"   -> Icons.Rounded.School
    else      -> null
}

@Preview(showBackground = true, backgroundColor = 0xFF14141C)
@Composable
private fun AreaTagPreview() {
    MorningAgentTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AreaTag(name = "work")
            AreaTag(name = "life")
            AreaTag(name = "home")
            AreaTag(name = "healthy")
            AreaTag(name = "learn")
        }
    }
}
