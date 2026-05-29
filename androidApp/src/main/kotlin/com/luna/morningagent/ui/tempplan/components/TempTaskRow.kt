package com.luna.morningagent.ui.tempplan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.morningagent.R
import com.luna.morningagent.data.tempplan.TempTask
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

// EditableTodoRow per design §5.7. Lives inside the single tasks card on the
// modification page. Promote is intentionally absent here — that action belongs
// on the home PlanCard (design §7). Already-promoted tasks render a small
// "Synced" label in place of the × button so the act is irreversible from here.
@Composable
fun TempTaskRow(
    task: TempTask,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning  = MaterialTheme.morning
    val promoted = task.promotedToNotionId != null
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EditableCheckbox(
            checked  = task.checked,
            onToggle = onToggle,
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
        if (promoted) {
            Text(
                text  = stringResource(R.string.temp_plan_promoted),
                style = MorningType.ButtonLabel.copy(fontSize = 12.sp),
                color = morning.success,
            )
        } else {
            Box(
                modifier         = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_remove_task),
                    tint               = morning.textMuted.copy(alpha = 0.7f),
                    modifier           = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun EditableCheckbox(
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val morning = MaterialTheme.morning
    Box(
        modifier         = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Box(
                modifier         = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(morning.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Check,
                    contentDescription = null,
                    tint               = morning.onAccent,
                    modifier           = Modifier.size(12.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, morning.textMuted.copy(alpha = 0.7f), CircleShape),
            )
        }
    }
}
