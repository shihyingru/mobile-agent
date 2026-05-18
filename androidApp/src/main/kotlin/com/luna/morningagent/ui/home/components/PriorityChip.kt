package com.luna.morningagent.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.morningagent.data.model.Priority
import com.luna.morningagent.ui.theme.InterFamily
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.morning

// v4 priority chip per design/home-screen.jsx PriorityChip.
//   - High = accent on accentSoft
//   - Medium = gold on gold@16%
//   - Low = textMuted on textMuted@16%
// Sentence-case labels (High/Medium/Low) — design specifically moved away
// from the SHOUTING uppercase look of v3.
@Composable
fun PriorityChip(
    priority: Priority,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val (fg: Color, bg: Color, label: String) = when (priority) {
        Priority.HIGH -> Triple(morning.accent,      morning.accentSoft,                    "High")
        Priority.MID  -> Triple(morning.gold,        morning.gold.copy(alpha = 0.16f),      "Medium")
        Priority.LOW  -> Triple(morning.textMuted,   morning.textMuted.copy(alpha = 0.16f), "Low")
    }

    Row(
        modifier              = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(fg),
        )
        Text(
            text  = label,
            color = fg,
            style = androidx.compose.ui.text.TextStyle(
                fontFamily    = InterFamily,
                fontWeight    = FontWeight.SemiBold,
                fontSize      = 11.sp,
                letterSpacing = 0.2.sp,
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun PriorityChipPreview() {
    MorningAgentTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityChip(Priority.HIGH)
            PriorityChip(Priority.MID)
            PriorityChip(Priority.LOW)
        }
    }
}
