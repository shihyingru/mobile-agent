package com.luna.morningagent.ui.sharedposts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Place
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luna.morningagent.data.sharedposts.ResolvedPlace
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

/**
 * Bottom sheet listing every place resolved for a post — shown when a card's pin
 * is tapped and the post has more than one location. Each row opens that place
 * in the user's map app (the only network the location feature does on demand).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostPlacesSheet(
    places: List<ResolvedPlace>,
    onOpenPlace: (ResolvedPlace) -> Unit,
    onDismiss: () -> Unit,
) {
    val morning = MaterialTheme.morning

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor   = morning.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 24.dp),
        ) {
            // Eyebrow — mono caps, mirrors the SuggestedSection rhythm.
            Row(
                modifier              = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Bottom,
            ) {
                Text(
                    text  = "PLACES",
                    style = MorningType.LabelMono.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)),
                    color = morning.accent,
                )
                Text(
                    text  = if (places.size == 1) "1 spot" else "${places.size} spots",
                    style = MorningType.MetaMono,
                    color = morning.textMuted,
                )
            }

            places.forEachIndexed { index, place ->
                if (index > 0) {
                    HorizontalDivider(color = morning.cardEdge, thickness = 1.dp)
                }
                PlaceRow(place = place, onClick = { onOpenPlace(place) })
            }
        }
    }
}

@Composable
private fun PlaceRow(place: ResolvedPlace, onClick: () -> Unit) {
    val morning = MaterialTheme.morning
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier         = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(morning.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Rounded.Place,
                contentDescription = null,
                tint               = morning.accent,
                modifier           = Modifier.size(15.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = place.name,
                style    = MorningType.RowTitle,
                color    = morning.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (place.address.isNotBlank()) {
                Text(
                    text     = place.address,
                    style    = MorningType.Caption,
                    color    = morning.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector        = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint               = morning.accent,
            modifier           = Modifier.size(16.dp),
        )
    }
}
