// Panel.kt - the machine's front panel.
//
// The layout follows the physical machine top to bottom, because that ordering
// is what makes an Enigma read as an Enigma:
//
//   1. the rotor windows, behind glass, showing the three or four letters
//   2. the lampboard, the 26 lamps the operator watched
//   3. the plugboard, a field of sockets with patch leads
//   4. the keyboard, the 26 keys
//
// On a phone the panel is taller than the screen, so it scrolls - but it never
// reflows sideways. Each section keeps the real machine's shape: the rotor
// windows stay in a row, the keyboard keeps its seven-six-seven stagger, and
// the lampboard keeps its QWERTZ-ish grid. Squashing those into a phone-shaped
// grid is what would kill the look.
//
// Everything is drawn with Compose primitives rather than bitmap art, so the
// panel stays crisp at any density and the brass is a real gradient rather than
// a photograph of one.

package com.gnomeworx.enigma.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gnomeworx.enigma.ui.theme.Brass
import com.gnomeworx.enigma.ui.theme.BrassBright
import com.gnomeworx.enigma.ui.theme.BrassDark
import com.gnomeworx.enigma.ui.theme.CaseMetal
import com.gnomeworx.enigma.ui.theme.CaseMetalLight
import com.gnomeworx.enigma.ui.theme.Cream
import com.gnomeworx.enigma.ui.theme.CreamDim
import com.gnomeworx.enigma.ui.theme.LampOff
import com.gnomeworx.enigma.ui.theme.LampOn
import com.gnomeworx.enigma.ui.theme.LampOnCore
import com.gnomeworx.enigma.ui.theme.PanelDark
import com.gnomeworx.enigma.ui.theme.SignalRed
import com.gnomeworx.enigma.ui.theme.SocketDark

/** The engraved-letter look: monospace, spaced out, slightly warm. */
val EngravedStyle = androidx.compose.ui.text.TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    color = Cream,
    letterSpacing = 1.sp,
)

/** A brass bevel, used for anything that should read as a metal fitting. */
fun brassBrush(): Brush = Brush.verticalGradient(
    colors = listOf(BrassBright, Brass, BrassDark),
)

/** The dark aged-metal of the case, used for the body between fittings. */
fun caseBrush(): Brush = Brush.verticalGradient(
    colors = listOf(CaseMetalLight, CaseMetal, PanelDark),
)

/**
 * One rotor window: a glass slot showing the letter currently at the top of
 * that rotor, with the brass knurled thumbwheel below it if it can be turned.
 *
 * The letter is what an operator reads to know the machine's state, so it is
 * the largest, brightest thing in this section.
 */
@Composable
fun RotorWindow(
    letter: Char,
    label: String,
    enabled: Boolean,
    onUp: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // The window bezel: brass frame, dark recess, letter behind glass.
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 64.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brassBrush())
                .padding(3.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(PanelDark),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(2.dp))
                    // A faint sheen across the glass, angled like a real window.
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2A2718),
                                Color(0xFF171508),
                                Color(0xFF23200F),
                            ),
                            start = Offset.Zero,
                            end = Offset.Infinite,
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter.toString(),
                    fontSize = 30.sp,
                    style = EngravedStyle.copy(
                        color = if (enabled) Cream else CreamDim,
                        letterSpacing = 0.sp,
                    ),
                )
            }
        }

        Spacer(Modifier.height(3.dp))
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            style = EngravedStyle.copy(color = CreamDim, letterSpacing = 0.5.sp),
        )

        if (onUp != null && onDown != null) {
            Spacer(Modifier.height(4.dp))
            // The thumbwheel: press the top to step forward, the bottom back.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ThumbwheelHalf("\u25B2", onUp)
                Spacer(Modifier.height(2.dp))
                ThumbwheelHalf("\u25BC", onDown)
            }
        }
    }
}

@Composable
private fun ThumbwheelHalf(glyph: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 54.dp, height = 20.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(
                Brush.verticalGradient(listOf(CaseMetalLight, PanelDark)),
            )
            .border(1.dp, BrassDark, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 9.sp, color = BrassBright)
    }
}

/**
 * The lampboard: 26 lamps in the authentic three-row layout.
 *
 * A lit lamp is the machine's only output, so the on state is deliberately
 * bright with a halo, and the off state is almost invisible. The contrast is
 * the point; a lampboard where the unlit lamps are equally visible is much
 * harder to read at a glance.
 */
@Composable
fun Lampboard(lit: Char?) {
    // The reflector row order, as printed on the machine.
    val rows = listOf(
        "QWERTZUIO",
        "ASDFGHJK",
        "PYXCVBNML",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(PanelDark)
            .border(1.dp, BrassDark, RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(vertical = 3.dp),
            ) {
                for (c in row) {
                    Lamp(c, lit != null && lit == c)
                }
            }
        }
    }
}

@Composable
private fun Lamp(letter: Char, on: Boolean) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            // An unlit lamp still needs its printed letter, so the dark body is
            // a real colour rather than fully transparent.
            .background(
                if (on) {
                    Brush.radialGradient(listOf(LampOnCore, LampOn, Color(0xFFC99A3C)))
                } else {
                    Brush.radialGradient(listOf(LampOff, Color(0xFF1A1708)))
                },
            )
            .border(
                width = if (on) 2.dp else 1.dp,
                color = if (on) LampOnCore else BrassDark,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.toString(),
            fontSize = 13.sp,
            style = EngravedStyle.copy(
                color = if (on) Color(0xFF3A2A08) else CreamDim,
                letterSpacing = 0.sp,
            ),
        )
    }
}

/**
 * The plugboard: 26 sockets in two alphabetic banks, as on the machine.
 *
 * A socket with a lead in it is drawn as plugged, which is how the operator
 * checks the board before a run. The patch leads are shown as letters rather
 * than drawn cables, because drawing 10 cables that never overlap at phone
 * width is not achievable and a wrong-looking cable is worse than none.
 */
@Composable
fun Plugboard(pairs: List<Pair<Char, Char>>, pending: Char?, onToggle: (Char) -> Unit) {
    val partner = mutableMapOf<Char, Char>()
    for ((a, b) in pairs) {
        partner[a] = b
        partner[b] = a
    }
    // The board reads as two horizontal rows of thirteen, the way the sockets
    // are drawn on the machine - not two side-by-side columns. Two columns of
    // seven overflow a phone's width and clip the last sockets, which is how
    // the earlier layout failed.
    val rows = listOf(
        "ABCDEFGHIJKLM",
        "ZNOPQRSTUVWXY",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(PanelDark)
            .border(1.dp, BrassDark, RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                // Even spacing across the full width, so thirteen sockets fit
                // whatever the screen size.
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (c in row) {
                    Socket(
                        letter = c,
                        partner = partner[c],
                        pending = pending == c,
                        onToggle = onToggle,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun Socket(
    letter: Char,
    partner: Char?,
    pending: Boolean,
    onToggle: (Char) -> Unit,
) {
    val plugged = partner != null
    // A socket with a lead in it glows brass; the one holding a free end in the
    // operator's hand blinks red so it is obvious which lead is unfinished.
    val body = when {
        pending -> Brush.radialGradient(listOf(Color(0xFF8A2A2A), Color(0xFF3A1414)))
        plugged -> Brush.radialGradient(listOf(BrassDark, Color(0xFF241C08)))
        else -> Brush.radialGradient(listOf(SocketDark, Color(0xFF12100A)))
    }
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(body)
            .border(
                width = if (plugged || pending) 2.dp else 1.dp,
                color = when {
                    pending -> SignalRed
                    plugged -> BrassBright
                    else -> BrassDark
                },
                shape = CircleShape,
            )
            .clickable { onToggle(letter) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (plugged) "$letter$partner" else letter.toString(),
            fontSize = if (plugged) 9.sp else 11.sp,
            style = EngravedStyle.copy(
                color = if (plugged || pending) Cream else CreamDim,
                letterSpacing = 0.sp,
            ),
        )
    }
}

/**
 * The keyboard: 26 keys in the authentic staggered rows.
 *
 * QWERTZUIO / ASDFGHJK / PYXCVBNML is the real layout, not a QWERTY one. The
 * bottom row reversed is a genuine Enigma quirk and is the detail that tells
 * someone who knows the machine that this is not a generic alphabet grid.
 *
 * A physically pressed key stays down (it is passed in as `pressed`) until the
 * finger lifts, which is how a key with a spring under it behaves.
 */
@Composable
fun Keyboard(
    pressed: Char?,
    lit: Char?,
    onKey: (Char) -> Unit,
) {
    val rows = listOf("QWERTZUIO", "ASDFGHJK", "PYXCVBNML")
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(vertical = 3.dp),
            ) {
                for (c in row) {
                    Key(c, c == pressed, c == lit, onKey)
                }
            }
        }
    }
}

@Composable
private fun Key(letter: Char, down: Boolean, isLit: Boolean, onKey: (Char) -> Unit) {
    // A key that is depressed sits lower and loses its highlight; a key whose
    // lamp is lit gets the same gold as the lamp, so the eye can follow the
    // signal from finger to lamp.
    val top = when {
        down -> PanelDark
        isLit -> Brass
        else -> CaseMetalLight
    }
    val bottom = when {
        down -> Color(0xFF0B0A06)
        isLit -> BrassDark
        else -> PanelDark
    }
    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 38.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(
                1.dp,
                if (down) BrassDark else Brass,
                RoundedCornerShape(4.dp),
            )
            .clickable { onKey(letter) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.toString(),
            fontSize = 15.sp,
            style = EngravedStyle.copy(
                color = when {
                    down -> CreamDim
                    isLit -> Color(0xFF2A1F08)
                    else -> Cream
                },
                letterSpacing = 0.sp,
            ),
        )
    }
}

/** A section heading, engraved brass on the case. */
@Composable
fun PanelHeading(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(brassBrush()),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text.uppercase(),
            fontSize = 11.sp,
            style = EngravedStyle.copy(color = BrassBright, letterSpacing = 2.sp),
        )
    }
}

/** The red Run key: larger, separate, and unmistakable, as the real one is. */
@Composable
fun RunKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFC04040), SignalRed, Color(0xFF6E1E1E)),
                ),
            )
            .border(2.dp, Brass, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 16.sp,
            style = EngravedStyle.copy(color = Cream, letterSpacing = 3.sp),
        )
    }
}
