// Theme.kt - the WWII field-machine visual language.
//
// The palette is a weathered olive-steel case with worn brass fittings and a
// warm cream for engraved labels. Nothing is pure white or pure black: real
// Bakelite and field-grey metal never are, and the slight warmth is what stops
// it reading as a modern dark-mode app.
//
// The lamp glow is the one deliberately bright thing on screen. Everything else
// is desaturated so the lit letter is unmistakable at a glance, which is how an
// operator actually used the machine - watching the lampboard, not the panel.

package com.gnomeworx.enigma.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The machine case.
val PanelBg = Color(0xFF1E1B14)
val PanelDark = Color(0xFF14120D)
val CaseMetal = Color(0xFF3A352A)
val CaseMetalLight = Color(0xFF4A4436)

// Brass fittings: the rotor knobs, the reflector collar, the bezels.
val Brass = Color(0xFFC6963E)
val BrassDark = Color(0xFF96702D)
val BrassBright = Color(0xFFE0B45A)

// Engraved lettering.
val Cream = Color(0xFFE8DFC8)
val CreamDim = Color(0xFFA89E86)

// Lamps. Only the lit one is bright.
val LampOn = Color(0xFFFFD27A)
val LampOnCore = Color(0xFFFFF3D0)
val LampOff = Color(0xFF2A2619)

// The red used for the Run key and for errors - a dull military red, not a
// digital one.
val SignalRed = Color(0xFFA83232)
val SignalRedDark = Color(0xFF7A2424)

// Plugboard sockets.
val SocketDark = Color(0xFF0E0C08)

/**
 * The app theme.
 *
 * Everything on this panel is drawn with explicit colours rather than through
 * Material's colour roles - the machine has one look, and a light/dark switch
 * must not change it. The theme therefore exists only to set the window
 * background and to disable Material's dynamic colour, so nothing re-tints the
 * brass.
 */
@Composable
fun EnigmaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = PanelBg,
            surface = PanelDark,
            onBackground = Cream,
            onSurface = Cream,
            primary = Brass,
            secondary = BrassDark,
        ),
        content = content,
    )
}
