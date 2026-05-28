package com.luna.morningagent.ui.tempplan.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luna.morningagent.R
import com.luna.morningagent.data.tempplan.TempTask
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

@Composable
fun TempTaskRow(
    task: TempTask,
    onToggle: () -> Unit,
    onPromote: () -> Unit,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val promoted = task.promotedToNotionId != null
    Row(
        modifier          = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (task.checked) morning.success else morning.textMuted, CircleShape)
                .background(if (task.checked) morning.success else morning.surface)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (task.checked) {
                Icon(
                    imageVector        = Icons.Rounded.Check,
                    contentDescription = null,
                    tint               = morning.onAccent,
                    modifier           = Modifier.size(14.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text           = task.title,
            style          = MorningType.BodyReadItalic,
            color          = if (task.checked) morning.textMuted else morning.textPrimary,
            textDecoration = if (task.checked) TextDecoration.LineThrough else TextDecoration.None,
            maxLines       = 1,
            overflow       = TextOverflow.Ellipsis,
            modifier       = Modifier.weight(1f),
        )
        if (promoted) {
            Text(
                text  = stringResource(R.string.temp_plan_promoted),
                style = MorningType.MetaMono,
                color = morning.success,
            )
        } else {
            TextButton(onClick = onPromote) {
                Text(
                    text  = stringResource(R.string.temp_plan_promote),
                    style = MorningType.ButtonLabel,
                    color = morning.accent,
                )
            }
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector        = Icons.Rounded.Close,
                    contentDescription = null,
                    tint               = morning.textMuted,
                    modifier           = Modifier.size(16.dp),
                )
            }
        }
    }
}
