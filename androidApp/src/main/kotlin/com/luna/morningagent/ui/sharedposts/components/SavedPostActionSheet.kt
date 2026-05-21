package com.luna.morningagent.ui.sharedposts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.morningagent.data.sharedposts.SharedPost
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

/**
 * Bottom action sheet for a single saved post (DESIGN_SYSTEM §3.20).
 *
 * Order is fixed: Send to agent · Copy link · Delete. Delete is last and red so
 * the thumb doesn't land on the destructive action first. The preview row
 * echoes the first ~50 chars of the post so the user knows which card they hit.
 */
private val DELETE_RED = Color(0xFFE5484D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostActionSheet(
    post: SharedPost,
    onDismiss: () -> Unit,
    onSendToAgent: () -> Unit,
    onCopyLink: () -> Unit,
    onDelete: () -> Unit,
) {
    val morning = MaterialTheme.morning
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = morning.surface,
        contentColor     = morning.textPrimary,
        dragHandle       = {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 14.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(morning.textMuted.copy(alpha = 0.4f)),
            )
        },
        shape            = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 22.dp)) {
            Text(
                text     = previewLine(post.content),
                color    = morning.textSecondary,
                style    = MorningType.BodyReadItalic.copy(fontSize = 13.sp),
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp).padding(bottom = 12.dp),
            )
            ActionRow(
                icon  = Icons.AutoMirrored.Rounded.Send,
                label = "Send to agent",
                hint  = "Add to today's briefing",
                onClick = onSendToAgent,
            )
            HorizontalDivider(color = morning.cardEdge, thickness = 1.dp)
            ActionRow(
                icon    = Icons.Rounded.Link,
                label   = "Copy link",
                hint    = if (post.url != null) "Original post URL" else "No URL on this post",
                enabled = post.url != null,
                onClick = onCopyLink,
            )
            HorizontalDivider(color = morning.cardEdge, thickness = 1.dp)
            ActionRow(
                icon        = Icons.Rounded.Delete,
                label       = "Delete",
                destructive = true,
                onClick     = onDelete,
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    hint: String? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val morning   = MaterialTheme.morning
    val labelTint = when {
        destructive -> DELETE_RED
        enabled     -> morning.textPrimary
        else        -> morning.textMuted
    }
    val iconTint = if (enabled) labelTint else morning.textMuted

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = iconTint,
            modifier           = Modifier.size(18.dp),
        )
        Text(
            text     = label,
            color    = labelTint,
            style    = MorningType.RowTitle,
            modifier = Modifier.weight(1f),
        )
        if (hint != null) {
            Text(
                text  = hint,
                color = morning.textMuted,
                style = MorningType.BodyReadItalic.copy(fontSize = 12.sp),
            )
        }
    }
}

private fun previewLine(content: String): String {
    val one = content.replace('\n', ' ').trim()
    return if (one.length <= 50) "“$one”" else "“${one.take(50).trimEnd()}…”"
}

