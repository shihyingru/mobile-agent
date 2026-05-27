package com.luna.morningagent.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

@Composable
fun TempPlanEmptyCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, morning.cardEdge, RoundedCornerShape(18.dp))
            .background(morning.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(
            text  = stringResource(R.string.temp_plan_empty_cta),
            style = MorningType.ButtonLabel,
            color = morning.accent,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = stringResource(R.string.temp_plan_empty_hint),
            style = MorningType.Caption,
            color = morning.textMuted,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun TempPlanEmptyCardPreview() {
    MorningAgentTheme {
        TempPlanEmptyCard(onClick = {}, modifier = Modifier.padding(18.dp))
    }
}
