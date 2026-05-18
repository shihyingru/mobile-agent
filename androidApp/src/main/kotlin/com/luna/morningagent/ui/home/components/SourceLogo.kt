package com.luna.morningagent.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

// v4 monogram chip per design/home-screen.jsx + Color Palette.md.
//   - Notion: white box with #231C12 "N" (only branch shipped).
//   - Linear, GitHub: stubs in place for the day they get real MCPs.
enum class TaskSource { Notion /* TODO(Phase 3: Linear, GitHub) */ }

private data class SourceStyle(val bg: Color, val fg: Color, val glyph: String)

private fun styleFor(source: TaskSource): SourceStyle = when (source) {
    TaskSource.Notion -> SourceStyle(bg = Color.White, fg = Color(0xFF231C12), glyph = "N")
}

@Composable
fun SourceLogo(
    source: TaskSource = TaskSource.Notion,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    val style = styleFor(source)
    val morning = MaterialTheme.morning
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(style.bg)
            // Hairline ring matches the JSX `box-shadow: 0 0 0 1px cardEdge`.
            .border(width = 1.dp, color = morning.cardEdge, shape = RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = style.glyph,
            color = style.fg,
            style = MorningType.LabelMono.copy(
                fontSize   = (size.value * 0.58f).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun SourceLogoPreview() {
    MorningAgentTheme { SourceLogo() }
}
