// Controls.kt - the setting controls: model, rotor order, rings, reflector, key sheet.
//
// These are the things an operator set before a message and left alone during
// it. On the real machine they are the drum of rotors lifted out and refitted,
// the ring markings, and the plugboard - all of which are deliberate, two-handed
// jobs. The controls here are therefore stepped and explicit rather than
// free-text: you choose a rotor from the set that actually fits, and the
// machine refuses combinations that do not exist.

package com.gnomeworx.enigma.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import com.gnomeworx.enigma.ui.theme.Brass
import com.gnomeworx.enigma.ui.theme.BrassBright
import com.gnomeworx.enigma.ui.theme.BrassDark
import com.gnomeworx.enigma.ui.theme.CaseMetal
import com.gnomeworx.enigma.ui.theme.CaseMetalLight
import com.gnomeworx.enigma.ui.theme.Cream
import com.gnomeworx.enigma.ui.theme.CreamDim
import com.gnomeworx.enigma.ui.theme.PanelDark
import com.gnomeworx.enigma.ui.theme.PanelBg

/** A label above a control, engraved small. */
@Composable
fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 9.sp,
        style = EngravedStyle.copy(color = CreamDim, letterSpacing = 1.5.sp),
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

/**
 * A segmented selector: the options laid out as physical detents, with the
 * selected one lit like a lamp. Used wherever the machine has a fixed set of
 * choices (which model, which rotor in a slot, which reflector).
 */
@Composable
fun <T> Segmented(
    label: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    scrollable: Boolean = false,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        FieldLabel(label)
        val content: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (opt in options) {
                    Detent(
                        text = labelOf(opt),
                        on = opt == selected,
                        onClick = { onSelect(opt) },
                    )
                }
            }
        }
        if (scrollable) {
            // A wide option set (the M4's ten rotors) scrolls sideways rather
            // than wrapping, so the row reads as one physical bank of detents.
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) { content() }
        } else {
            content()
        }
    }
}

@Composable
private fun Detent(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (on) {
                    Brush.verticalGradient(listOf(BrassBright, Brass, BrassDark))
                } else {
                    Brush.verticalGradient(listOf(CaseMetalLight, PanelDark))
                },
            )
            .border(1.dp, if (on) Cream else BrassDark, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 12.sp,
            style = EngravedStyle.copy(
                color = if (on) Color(0xFF2A1F08) else Cream,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}

// Color is imported above for the engraving and paper tones.

/**
 * A stepping control for a letter value: rings and indicators are set by
 * turning a wheel through A-Z, never by typing, so this mirrors that.
 */
@Composable
fun LetterStepper(
    label: String,
    value: Char,
    onChange: (Char) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FieldLabel(label)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            StepButton("\u25C0") {
                onChange(if (value == 'A') 'Z' else (value - 1))
            }
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 34.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(PanelDark)
                    .border(1.dp, BrassDark, RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    value.toString(),
                    fontSize = 17.sp,
                    style = EngravedStyle.copy(color = Cream, letterSpacing = 0.sp),
                )
            }
            StepButton("\u25B6") {
                onChange(if (value == 'Z') 'A' else (value + 1))
            }
        }
    }
}

@Composable
private fun StepButton(glyph: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Brush.verticalGradient(listOf(CaseMetalLight, PanelDark)))
            .border(1.dp, BrassDark, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 10.sp, color = BrassBright)
    }
}

/** An editable text area styled as a paper message form. */
@Composable
fun MessageField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Int = 72,
    readOnly: Boolean = false,
    placeholder: String = "",
) {
    val focusRequester = remember { FocusRequester() }
    Column(modifier = modifier) {
        FieldLabel(label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFEFE7D0))
                .border(1.dp, BrassDark, RoundedCornerShape(4.dp))
                // Tap anywhere on the paper to start writing. A phone user aims
                // at the form, not at a thin line of text, and a text field that
                // needs a precise hit on the caret reads as broken.
                .clickable(enabled = !readOnly) { focusRequester.requestFocus() }
                .padding(8.dp),
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    placeholder,
                    fontSize = 14.sp,
                    style = EngravedStyle.copy(
                        color = Color(0xFF8A7F66),
                        letterSpacing = 0.sp,
                    ),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = readOnly,
                textStyle = EngravedStyle.copy(
                    color = Color(0xFF241E0E),
                    fontSize = 14.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold,
                ),
                // The Android keyboard must come up on its own; without this the
                // soft keyboard stays down and the field looks dead.
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
    }
}

/** A labelled button in the case-metal style. */
@Composable
fun PanelButton(label: String, wide: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .then(if (wide) Modifier.fillMaxWidth() else Modifier)
            .height(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(CaseMetalLight, CaseMetal, PanelDark)))
            .border(1.dp, Brass, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            style = EngravedStyle.copy(color = BrassBright, letterSpacing = 1.5.sp),
        )
    }
}
