package com.luna.morningagent.ui.tempplan.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.luna.morningagent.R
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

@Composable
fun TempPlanHeader(onBack: () -> Unit) {
    val morning = MaterialTheme.morning
    Row(
        modifier              = Modifier.padding(horizontal = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 44dp tap target nudged 6dp left so the icon sits flush with the
        // page's 4dp horizontal padding (design §5.4 "margin-left -6dp").
        Box(
            modifier         = Modifier
                .offset(x = (-6).dp)
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cd_back),
                tint               = morning.textPrimary,
                modifier           = Modifier.size(22.dp),
            )
        }
        Text(
            text  = stringResource(R.string.temp_plan_screen_title),
            style = MorningType.ScreenTitle,
            color = morning.textPrimary,
        )
    }
}
