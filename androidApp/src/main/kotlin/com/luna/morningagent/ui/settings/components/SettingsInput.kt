package com.luna.morningagent.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.morningagent.ui.theme.InterFamily
import com.luna.morningagent.ui.theme.MonoFamily
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

/**
 * v4 flat key-value input box. 12dp radius, surface bg, cardEdge border,
 * 10dp×14dp padding. Two rows: tiny label on top + value below. Value uses
 * the mono family when present (so saved tokens read as code), and the sans
 * placeholder color when empty.
 *
 * @param secret  apply PasswordVisualTransformation so token characters mask.
 * @param mono    force mono font even when empty — used for the "saved last4"
 *                preview where the field shows ****Tfhc and reads as code.
 */
@Composable
fun SettingsInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = label,
    secret: Boolean = false,
    mono: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val morning = MaterialTheme.morning
    val isEmpty = value.isEmpty()
    val displayStyle = if (isEmpty && !mono) {
        LocalTextStyle.current.copy(
            fontFamily = InterFamily,
            fontSize   = 14.sp,
            color      = morning.textMuted,
        )
    } else {
        LocalTextStyle.current.copy(
            fontFamily    = MonoFamily,
            fontSize      = 14.sp,
            letterSpacing = 0.4.sp,
            color         = morning.textPrimary,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(morning.surface)
            .border(width = 1.dp, color = morning.cardEdge, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text  = label,
            color = morning.textMuted,
            style = MorningType.RowTitle.copy(fontSize = 11.sp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value                  = value,
            onValueChange          = onValueChange,
            modifier               = Modifier.fillMaxWidth(),
            singleLine             = singleLine,
            textStyle              = displayStyle,
            cursorBrush            = androidx.compose.ui.graphics.SolidColor(morning.accent),
            visualTransformation   = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions        = keyboardOptions,
            keyboardActions        = keyboardActions,
            decorationBox          = { inner ->
                Box {
                    if (isEmpty) {
                        Text(text = placeholder, style = displayStyle)
                    }
                    inner()
                }
            },
        )
    }
}

/** Read-only variant — shows a saved value (e.g. "****Tfhc") without an editor. */
@Composable
fun SettingsInputReadOnly(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    SettingsInput(
        label         = label,
        value         = value,
        onValueChange = {},
        modifier      = modifier,
        placeholder   = label,
        mono          = true,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun SettingsInputEmptyPreview() {
    MorningAgentTheme {
        Column(modifier = Modifier.padding(18.dp)) {
            SettingsInput(label = "Gemini API key", value = "", onValueChange = {})
            Spacer(modifier = Modifier.height(8.dp))
            SettingsInput(label = "Notion integration token", value = "•••••••9F2c", onValueChange = {}, secret = true, mono = true)
        }
    }
}
