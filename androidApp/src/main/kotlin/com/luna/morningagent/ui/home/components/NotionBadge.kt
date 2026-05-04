package com.luna.morningagent.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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

// Small white "N" badge — marks cards that originated from Notion
@Composable
fun NotionBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = "N",
            style = MorningType.Label,
            color = Color.Black,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14141C)
@Composable
private fun NotionBadgePreview() {
    MorningAgentTheme {
        NotionBadge()
    }
}
