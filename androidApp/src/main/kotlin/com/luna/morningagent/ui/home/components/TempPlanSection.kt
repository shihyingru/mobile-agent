package com.luna.morningagent.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.morningagent.R
import com.luna.morningagent.data.PreviewData
import com.luna.morningagent.data.tempplan.TempPlan
import com.luna.morningagent.data.tempplan.TempTask
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

// PlanCard on Home — the whole surface opens the modification page.
// Progress reflects time elapsed (not tasks completed) per the design spec.
// Up to 3 todo rows are visible; overflow becomes a "+N more" line.
@Composable
fun TempPlanSection(
    plan: TempPlan,
    onToggleTask: (String) -> Unit,
    onPromoteTask: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val start = LocalDate.parse(plan.startDate)
    val end   = LocalDate.parse(plan.endDate)
    val today = LocalDate.now()
    val total    = max(1L, ChronoUnit.DAYS.between(start, end))
    val elapsed  = ChronoUnit.DAYS.between(start, today).coerceIn(0, total)
    val daysLeft = max(0L, ChronoUnit.DAYS.between(today, end)).toInt()
    val ratio    = (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    val visible = plan.tasks.take(MAX_VISIBLE_TODOS)
    val overflow = plan.tasks.size - MAX_VISIBLE_TODOS

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, morning.cardEdge, RoundedCornerShape(18.dp))
            .background(morning.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PlanCardHeader(
                eyebrowDateRange = formatDateRange(start, end),
                title            = plan.name,
                daysLeft         = daysLeft,
            )

            TimeProgressBar(
                ratio    = ratio,
                modifier = Modifier.fillMaxWidth(),
            )

            if (visible.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                visible.forEachIndexed { index, task ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(morning.cardEdge),
                        )
                    }
                    PlanCardTodoRow(
                        task      = task,
                        onToggle  = { onToggleTask(task.id) },
                        onPromote = { onPromoteTask(task.id) },
                    )
                }
                if (overflow > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = stringResource(R.string.temp_plan_more_tasks, overflow),
                        style = MorningType.BodyReadItalic.copy(fontSize = 12.sp),
                        color = morning.textMuted,
                    )
                }
            }

            // Reserves space so the corner chevron doesn't overlap content.
            Spacer(Modifier.height(8.dp))
        }

        Icon(
            imageVector        = Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint               = morning.textMuted.copy(alpha = 0.45f),
            modifier           = Modifier
                .align(Alignment.BottomEnd)
                .size(14.dp),
        )
    }
}

@Composable
private fun PlanCardHeader(
    eyebrowDateRange: String,
    title: String,
    daysLeft: Int,
) {
    val morning = MaterialTheme.morning
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Eyebrow: "PLAN · MAY 25 – MAY 30" — "PLAN" all-caps, dates not.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = stringResource(R.string.section_plan),
                    style = MorningType.LabelMono,
                    color = morning.textMuted,
                )
                Text(
                    text  = " · ",
                    style = MorningType.MetaMono,
                    color = morning.textMuted.copy(alpha = 0.4f),
                )
                Text(
                    text  = eyebrowDateRange,
                    style = MorningType.MetaMono,
                    color = morning.textMuted,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text  = title,
                style = MorningType.SectionHeading.copy(
                    fontSize   = 24.sp,
                    lineHeight = (24 * 1.1f).sp,
                ),
                color = morning.textPrimary,
            )
        }
        DaysLeftChip(daysLeft = daysLeft)
    }
}

@Composable
internal fun DaysLeftChip(daysLeft: Int) {
    val morning = MaterialTheme.morning
    val urgent = daysLeft == 0
    val bg = if (urgent) morning.accentSoft else morning.gold.copy(alpha = 0.16f)
    val fg = if (urgent) morning.accent     else morning.gold
    val label = if (urgent) {
        stringResource(R.string.temp_plan_due_today)
    } else {
        stringResource(R.string.temp_plan_days_left, daysLeft)
    }
    Row(
        modifier              = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector        = Icons.Outlined.HourglassEmpty,
            contentDescription = null,
            tint               = fg,
            modifier           = Modifier.size(11.dp),
        )
        Text(
            text  = label,
            style = MorningType.ButtonLabel.copy(fontSize = 11.sp, letterSpacing = 0.2.sp),
            color = fg,
        )
    }
}

// 2dp track + 10dp circular today-dot at the elapsed/total ratio. Container is
// 10dp tall so the dot fits without overflow.
@Composable
internal fun TimeProgressBar(
    ratio: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.morning.cardEdge,
    fillColor:  Color = MaterialTheme.morning.accent,
    dotRingColor: Color = MaterialTheme.morning.surface,
) {
    val clamped = ratio.coerceIn(0f, 1f)
    BoxWithConstraints(modifier = modifier.height(10.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
            val centerY = size.height / 2f
            val trackHeight = 2.dp.toPx()
            val dotRadius   = 5.dp.toPx()
            val ringStroke  = 3.dp.toPx()
            // Track
            drawRoundRect(
                color        = trackColor,
                topLeft      = Offset(0f, centerY - trackHeight / 2f),
                size         = Size(size.width, trackHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
            )
            // Fill
            val fillWidth = size.width * clamped
            if (fillWidth > 0f) {
                drawRoundRect(
                    color        = fillColor,
                    topLeft      = Offset(0f, centerY - trackHeight / 2f),
                    size         = Size(fillWidth, trackHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
                )
            }
            // Today dot: filled accent with a surface-colored ring so it pops.
            val dotX = fillWidth.coerceIn(dotRadius, size.width - dotRadius)
            drawCircle(color = fillColor, radius = dotRadius, center = Offset(dotX, centerY))
            drawCircle(
                color  = dotRingColor,
                radius = dotRadius,
                center = Offset(dotX, centerY),
                style  = Stroke(width = ringStroke),
            )
            // Re-draw the inner fill so the ring is *inset* rather than overdrawn.
            drawCircle(
                color  = fillColor,
                radius = dotRadius - ringStroke / 2f,
                center = Offset(dotX, centerY),
            )
        }
    }
}

@Composable
private fun PlanCardTodoRow(
    task: TempTask,
    onToggle: () -> Unit,
    onPromote: () -> Unit,
) {
    val morning  = MaterialTheme.morning
    val promoted = task.promotedToNotionId != null
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlanCheckbox(
            checked  = task.checked,
            onToggle = onToggle,
            size     = 18.dp,
        )
        Text(
            text           = task.title,
            style          = MorningType.TaskTitle.copy(
                fontSize   = 15.5.sp,
                lineHeight = (15.5 * 1.3f).sp,
            ),
            color          = if (task.checked) morning.textMuted else morning.textPrimary,
            textDecoration = if (task.checked) TextDecoration.LineThrough else TextDecoration.None,
            maxLines       = 1,
            overflow       = TextOverflow.Ellipsis,
            modifier       = Modifier.weight(1f),
        )
        if (!task.checked && !promoted) {
            Row(
                modifier              = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onPromote)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text  = stringResource(R.string.temp_plan_promote),
                    style = MorningType.ButtonLabel.copy(fontSize = 12.sp),
                    color = morning.accent,
                )
                Icon(
                    imageVector        = Icons.Outlined.NorthEast,
                    contentDescription = null,
                    tint               = morning.accent,
                    modifier           = Modifier.size(12.dp),
                )
            }
        } else if (promoted) {
            Text(
                text  = stringResource(R.string.temp_plan_promoted),
                style = MorningType.ButtonLabel.copy(fontSize = 12.sp),
                color = morning.success,
            )
        }
    }
}

@Composable
internal fun PlanCheckbox(
    checked: Boolean,
    onToggle: () -> Unit,
    size: Dp = 18.dp,
) {
    val morning = MaterialTheme.morning
    Box(
        modifier         = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Box(
                modifier         = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(morning.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Check,
                    contentDescription = null,
                    tint               = morning.onAccent,
                    modifier           = Modifier.size((size.value * 0.6f).dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(1.5.dp, morning.textMuted.copy(alpha = 0.7f), CircleShape),
            )
        }
    }
}

private const val MAX_VISIBLE_TODOS = 3

// "MAY 25 – MAY 30" — locale-stable English mono caps to match the eyebrow's
// design intent (uppercase via Locale.ENGLISH; the type style itself is mono).
private fun formatDateRange(start: LocalDate, end: LocalDate): String {
    val fmt = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
    val s = start.format(fmt).uppercase(Locale.ENGLISH)
    val e = end.format(fmt).uppercase(Locale.ENGLISH)
    return "$s – $e"
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun TempPlanSectionPreview() {
    MorningAgentTheme {
        Box(modifier = Modifier.padding(18.dp)) {
            TempPlanSection(
                plan          = PreviewData.sampleTempPlan,
                onToggleTask  = {},
                onPromoteTask = {},
                onClick       = {},
            )
        }
    }
}
