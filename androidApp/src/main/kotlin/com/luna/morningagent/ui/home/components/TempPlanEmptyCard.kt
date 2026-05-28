package com.luna.morningagent.ui.home.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.morningagent.R
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

// Empty-state slot for the Plan section on Home. Dashed border distinguishes
// "no content yet" from a filled PlanCard. Tap pushes to the modification page
// where a blank plan is composed.
@Composable
fun TempPlanEmptyCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val borderColor = morning.textPrimary.copy(alpha = 0.22f)
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .drawBehind {
                drawRoundRect(
                    color        = borderColor,
                    size         = Size(size.width, size.height),
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style        = Stroke(
                        width      = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                            0f,
                        ),
                    ),
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 44dp plus tile — accentSoft surface, accent glyph.
        Box(
            modifier         = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(morning.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Outlined.Add,
                contentDescription = null,
                tint               = morning.accent,
                modifier           = Modifier.size(20.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = stringResource(R.string.section_plan),
                style = MorningType.LabelMono,
                color = morning.textMuted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = stringResource(R.string.temp_plan_empty_title),
                style = MorningType.SectionHeading.copy(
                    fontSize   = 20.sp,
                    lineHeight = (20 * 1.15f).sp,
                ),
                color = morning.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = stringResource(R.string.temp_plan_empty_subtitle),
                style = MorningType.BodyReadItalic.copy(fontSize = 13.5.sp),
                color = morning.textSecondary,
            )
        }

        Icon(
            imageVector        = Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint               = morning.accent,
            modifier           = Modifier.size(16.dp),
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
