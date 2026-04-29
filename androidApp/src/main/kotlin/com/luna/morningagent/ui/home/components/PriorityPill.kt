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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.morningagent.data.model.Priority
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

@Composable
fun PriorityPill(
    priority: Priority,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val color = when (priority) {
        Priority.HIGH -> morning.priorityHigh
        Priority.MID  -> morning.priorityMid
        Priority.LOW  -> morning.priorityLow
    }
    val label = priority.name  // "HIGH" / "MID" / "LOW"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text  = label,
            style = MorningType.Label,
            color = color,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14141C)
@Composable
private fun PriorityPillPreview() {
    MorningAgentTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityPill(Priority.HIGH)
            PriorityPill(Priority.MID)
            PriorityPill(Priority.LOW)
        }
    }
}
