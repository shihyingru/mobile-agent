package com.luna.morningagent.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.morningagent.ui.theme.InterFamily
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

// Display data for one tile in the AI Provider carousel. Brand colors come
// from design/Color Palette.md (Notion #FFFFFF / Gemini #4285F4 / Claude
// #D97757 / OpenAI #0A0A0A …). Disabled = the provider has no backing
// implementation in this build (e.g. OpenAI awaits its own commit).
data class ProviderTileData(
    val id:        String,
    val name:      String,
    val glyph:     String,
    val brandBg:   Color,
    val brandFg:   Color,
    val activeModelLabel: String?,
    val extraModelCount:  Int,
    val disabled: Boolean = false,
)

@Composable
fun ProviderTile(
    data: ProviderTileData,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val borderColor = when {
        data.disabled -> morning.cardEdge
        selected      -> morning.accent
        else          -> morning.cardEdge
    }
    val borderWidth = if (selected && !data.disabled) 1.5.dp else 1.dp
    val tileBg = when {
        data.disabled -> Color.Transparent
        selected      -> morning.accentSoft
        else          -> Color.Transparent
    }

    Column(
        modifier = modifier
            .widthIn(min = 124.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tileBg)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(14.dp))
            .let { if (!data.disabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Brand logo box.
            Box(
                modifier         = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(data.brandBg.copy(alpha = if (data.disabled) 0.5f else 1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = data.glyph,
                    color = data.brandFg.copy(alpha = if (data.disabled) 0.7f else 1f),
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily    = InterFamily,
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 14.sp,
                        letterSpacing = (-0.5).sp,
                    ),
                )
            }

            // Radio circle.
            val radioBorder = when {
                data.disabled -> morning.textMuted.copy(alpha = 0.5f)
                selected      -> morning.accent
                else          -> morning.textMuted
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (selected && !data.disabled) morning.accent else Color.Transparent)
                    .border(width = 1.5.dp, color = radioBorder, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected && !data.disabled) {
                    Icon(
                        imageVector        = Icons.Rounded.Check,
                        contentDescription = null,
                        tint               = morning.onAccent,
                        modifier           = Modifier.size(9.dp),
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text  = data.name,
                style = MorningType.RowTitle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
                color = if (data.disabled) morning.textMuted else morning.textPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text  = data.activeModelLabel ?: "Coming soon",
                    style = MorningType.MetaMono.copy(fontSize = 10.sp),
                    color = when {
                        data.disabled -> morning.textMuted.copy(alpha = 0.7f)
                        selected      -> morning.accent
                        else          -> morning.textMuted
                    },
                )
                if (data.extraModelCount > 0 && !data.disabled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) morning.accentSoft else morning.cardEdge.copy(alpha = 0.6f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text  = "+${data.extraModelCount}",
                            color = if (selected) morning.accent else morning.textMuted,
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily    = InterFamily,
                                fontWeight    = FontWeight.SemiBold,
                                fontSize      = 9.sp,
                                letterSpacing = 0.3.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun ProviderTilePreview() {
    MorningAgentTheme {
        Row(
            modifier              = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProviderTile(
                data = ProviderTileData(
                    id = "gemini", name = "Gemini", glyph = "G",
                    brandBg = Color(0xFF4285F4), brandFg = Color.White,
                    activeModelLabel = "gemini-2.5-flash", extraModelCount = 2,
                ),
                selected = true,
                onClick  = {},
            )
            ProviderTile(
                data = ProviderTileData(
                    id = "claude", name = "Claude", glyph = "C",
                    brandBg = Color(0xFFD97757), brandFg = Color.White,
                    activeModelLabel = "claude-sonnet-4", extraModelCount = 2,
                ),
                selected = false,
                onClick  = {},
            )
            ProviderTile(
                data = ProviderTileData(
                    id = "openai", name = "OpenAI", glyph = "O",
                    brandBg = Color(0xFF0A0A0A), brandFg = Color.White,
                    activeModelLabel = null, extraModelCount = 0,
                    disabled = true,
                ),
                selected = false,
                onClick  = {},
            )
        }
    }
}
