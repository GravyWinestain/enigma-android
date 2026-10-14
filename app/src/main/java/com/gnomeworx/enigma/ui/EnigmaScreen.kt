// EnigmaScreen.kt - the whole machine on screen, and its state.
//
// The screen is one scrollable column in the physical machine's order: rotor
// windows, lampboard, then the settings, then the message form and keyboard.
// On a phone the keyboard sits at the bottom of the scroll rather than pinned,
// because pinning it would cover the lampboard - and the lampboard is the thing
// the operator is actually watching while typing.
//
// State model. The live machine is held as a snapshot of the settings plus the
// rotor positions. Any change to a setting discards the machine, so the next
// keystroke rebuilds it from the Grundstellung. That is deliberate: on the real
// machine you cannot change a rotor mid-message, and silently continuing from a
// half-turned position after a key change is how a "correct" message fails to
// decode at the other end.

package com.gnomeworx.enigma.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gnomeworx.enigma.core.Enigma
import com.gnomeworx.enigma.core.Key
import com.gnomeworx.enigma.core.Model
import com.gnomeworx.enigma.core.ReflectorId
import com.gnomeworx.enigma.core.RotorId
import com.gnomeworx.enigma.core.carriedDigit
import com.gnomeworx.enigma.core.digitsToLetters
import com.gnomeworx.enigma.core.groupLetters
import com.gnomeworx.enigma.core.isSendable
import com.gnomeworx.enigma.core.toEnigma
import com.gnomeworx.enigma.ui.theme.Brass
import com.gnomeworx.enigma.ui.theme.BrassBright
import com.gnomeworx.enigma.ui.theme.BrassDark
import com.gnomeworx.enigma.ui.theme.CaseMetal
import com.gnomeworx.enigma.ui.theme.Cream
import com.gnomeworx.enigma.ui.theme.CreamDim
import com.gnomeworx.enigma.ui.theme.PanelBg
import com.gnomeworx.enigma.ui.theme.PanelDark
import com.gnomeworx.enigma.ui.theme.SignalRed

/**
 * The machine's settings, plus the Grundstellung the rotors start from.
 *
 * Held as one object so that "any change invalidates the live machine" is a
 * single comparison rather than twenty call sites remembering to call a reset.
 */
data class Settings(
    val model: Model,
    val rotors: List<RotorId>,
    val rings: String,
    val indicator: String,
    val reflector: ReflectorId,
    val plugboard: List<Pair<Int, Int>>,
) {
    fun toKey(): Key = Key(model, rotors, rings, indicator, reflector, plugboard)

    companion object {
        fun default(): Settings = Settings(
            model = Model.EnigmaI,
            rotors = listOf(RotorId.I, RotorId.II, RotorId.III),
            rings = "AAA",
            indicator = "AAA",
            reflector = ReflectorId.B,
            plugboard = emptyList(),
        )
    }
}

@Composable
fun EnigmaScreen() {
    var settings by remember { mutableStateOf(Settings.default()) }
    // The live machine, rebuilt whenever a setting changes.
    var machine by remember { mutableStateOf(settings.toKey().toEnigma()) }

    // The rotor positions as the operator last left them, so the windows show
    // the real state rather than the Grundstellung.
    var windowLetters by remember { mutableStateOf(settings.indicator) }

    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var litLamp by remember { mutableStateOf<Char?>(null) }
    var pressedKey by remember { mutableStateOf<Char?>(null) }
    var notice by remember { mutableStateOf("") }
    var showKeySheet by remember { mutableStateOf(false) }
    // The free end of a lead, held while the operator picks its partner.
    var pendingPlug by remember { mutableStateOf<Int?>(null) }

    // Apply a settings change: rebuild the machine and reset the windows.
    fun applySettings(next: Settings) {
        settings = next
        machine = next.toKey().toEnigma()
        windowLetters = next.indicator
        output = ""
        litLamp = null
        notice = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PanelBg)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Header()

        // ---- 1. The rotor windows -------------------------------------------
        PanelHeading("Rotor windows")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            settings.rotors.forEachIndexed { i, id ->
                val letter = windowLetters.getOrElse(i) { 'A' }
                RotorWindow(
                    letter = letter,
                    label = id.label,
                    enabled = true,
                    // Turning a window by hand walks the rotor, exactly as
                    // thumbing the wheel on the real machine does. It is also
                    // how an operator sets the indicator without a key sheet.
                    onUp = {
                        val next = windowLetters.toMutableList()
                        next[i] = if (next[i] == 'Z') 'A' else (next[i] + 1)
                        windowLetters = String(next.toCharArray())
                        machine.setPositions(windowLetters)
                    },
                    onDown = {
                        val next = windowLetters.toMutableList()
                        next[i] = if (next[i] == 'A') 'Z' else (next[i] - 1)
                        windowLetters = String(next.toCharArray())
                        machine.setPositions(windowLetters)
                    },
                )
            }
        }
        Text(
            text = "GRUNDSTELLUNG ${settings.indicator}   NOW SHOWING $windowLetters",
            fontSize = 10.sp,
            style = EngravedStyle.copy(color = CreamDim, letterSpacing = 1.sp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        // ---- 2. The lampboard ----------------------------------------------
        PanelHeading("Lampboard")
        Lampboard(litLamp)

        // ---- 3. The machine settings ---------------------------------------
        PanelHeading("Machine")
        Segmented(
            label = "Model",
            options = Model.entries,
            selected = settings.model,
            labelOf = { it.label },
            onSelect = { m ->
                // Changing model changes the valid rotor set, so refit the
                // rotors rather than leaving an impossible combination.
                val rotors = if (m.hasFourthRotor) {
                    listOf(RotorId.Beta, RotorId.I, RotorId.II, RotorId.III)
                } else {
                    listOf(RotorId.I, RotorId.II, RotorId.III)
                }
                applySettings(
                    settings.copy(
                        model = m,
                        rotors = rotors,
                        rings = "A".repeat(m.rotorCount),
                        indicator = "A".repeat(m.rotorCount),
                        reflector = m.reflectorSet.first(),
                    ),
                )
            },
        )

        // One rotor picker per slot, restricted to rotors that physically fit.
        val slotNames = if (settings.model.hasFourthRotor) {
            listOf("4th (thin)", "Left", "Middle", "Right")
        } else {
            listOf("Left", "Middle", "Right")
        }
        settings.rotors.forEachIndexed { i, current ->
            val options = if (settings.model.hasFourthRotor && i == 0) {
                listOf(RotorId.Beta, RotorId.Gamma)
            } else {
                RotorId.navalSet.filter { it != RotorId.Beta && it != RotorId.Gamma }
            }
            Segmented(
                label = "Rotor ${slotNames[i]}",
                options = options,
                selected = current,
                labelOf = { it.label },
                onSelect = { picked ->
                    val next = settings.rotors.toMutableList()
                    next[i] = picked
                    applySettings(settings.copy(rotors = next))
                },
                scrollable = true,
            )
        }

        Segmented(
            label = "Reflector",
            options = settings.model.reflectorSet,
            selected = settings.reflector,
            labelOf = { it.label },
            onSelect = { applySettings(settings.copy(reflector = it)) },
        )

        // Ring settings: the markings on the rotor, set by thumbwheel.
        PanelHeading("Ringstellung")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            settings.rings.forEachIndexed { i, c ->
                LetterStepper(
                    label = slotNames.getOrElse(i) { "R$i" },
                    value = c,
                    onChange = { nv ->
                        val next = settings.rings.toMutableList()
                        next[i] = nv
                        applySettings(settings.copy(rings = String(next.toCharArray())))
                    },
                )
            }
        }

        // The initial rotor position. Distinct from the ring setting, which is
        // the single most common way to get a "wrong" decode.
        PanelHeading("Grundstellung (indicator)")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            settings.indicator.forEachIndexed { i, c ->
                LetterStepper(
                    label = slotNames.getOrElse(i) { "R$i" },
                    value = c,
                    onChange = { nv ->
                        val next = settings.indicator.toMutableList()
                        next[i] = nv
                        applySettings(
                            settings.copy(indicator = String(next.toCharArray())),
                        )
                    },
                )
            }
        }

        // ---- 4. The plugboard ----------------------------------------------
        PanelHeading("Steckerbrett (plugboard)")
        Text(
            text = if (settings.plugboard.isEmpty()) {
                "No leads fitted."
            } else {
                // Shown as socket pairs, the way the board is written on a
                // key sheet: EK AT ...
                settings.plugboard.joinToString("  ") { (a, b) ->
                    "${('A' + a)}${('A' + b)}"
                }
            },
            fontSize = 11.sp,
            style = EngravedStyle.copy(color = Cream, letterSpacing = 1.sp),
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Plugboard(
            pairs = settings.plugboard.map { ('A' + it.first) to ('A' + it.second) },
            pending = pendingPlug?.let { 'A' + it },
            onToggle = { letter ->
                val c = letter - 'A'
                when {
                    // A socket that already holds a lead: pull it out.
                    settings.plugboard.any { it.first == c || it.second == c } -> {
                        val lead = settings.plugboard.first { it.first == c || it.second == c }
                        applySettings(settings.copy(plugboard = settings.plugboard - lead))
                        pendingPlug = null
                    }
                    // No lead waiting: this socket takes the free end.
                    pendingPlug == null -> {
                        pendingPlug = c
                        notice = "Lead from ${('A' + c)} in hand - tap its partner."
                    }
                    // Same socket twice: put the lead down again.
                    pendingPlug == c -> {
                        pendingPlug = null
                        notice = "Lead put down."
                    }
                    // The other end: a pair is made.
                    else -> {
                        val first = pendingPlug!!
                        applySettings(
                            settings.copy(plugboard = settings.plugboard + (first to c)),
                        )
                        pendingPlug = null
                        notice = "Lead fitted ${('A' + first)}-${('A' + c)}."
                    }
                }
            },
        )

        // ---- 5. The message ------------------------------------------------
        PanelHeading("Message")
        MessageField(
            label = "Plaintext in (letters and numbers)",
            value = input,
            onValueChange = { v ->
                // Keep letters and digits; the machine enciphers numbers as
                // letters, and separators are dropped the way an operator
                // dropped them when keying.
                input = v.uppercase().filter { it.isSendable() }
                litLamp = null
            },
            placeholder = "TYPE HERE OR USE THE KEYS BELOW",
        )

        Spacer(Modifier.height(8.dp))
        RunKey(label = "Run the cipher") {
            val wire = digitsToLetters(input)
            if (wire.isEmpty()) {
                notice = "Nothing to encipher."
                return@RunKey
            }
            val fresh = settings.toKey().toEnigma()
            val out = fresh.encrypt(wire)
            output = groupLetters(out)
            // Leave the windows showing where the machine ended up, so the
            // operator can see the rotors moved.
            windowLetters = fresh.windowLetters()
            machine.setPositions(windowLetters)
            notice = "Enciphered ${wire.length} characters."
        }

        Spacer(Modifier.height(10.dp))
        MessageField(
            label = "Cipher out (five-letter groups as sent)",
            value = output,
            onValueChange = {},
            readOnly = true,
            minHeight = 64,
            placeholder = "RUN THE CIPHER TO FILL THIS",
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = notice.ifEmpty { "Numbers are enciphered as letters; see the key sheet." },
            fontSize = 10.sp,
            style = EngravedStyle.copy(color = CreamDim, letterSpacing = 0.5.sp),
        )

        // ---- 6. The keyboard -----------------------------------------------
        PanelHeading("Tastatur (keyboard)")
        Keyboard(
            pressed = pressedKey,
            lit = litLamp,
            onKey = { c ->
                pressedKey = c
                // A real keystroke does the work; the message field is just a
                // convenient way to type a whole run at once.
                val fresh = machine
                val out = fresh.encryptChar(c)
                litLamp = out
                windowLetters = fresh.windowLetters()
                output = groupLetters(output.replace(" ", "") + out)
                if (input.length < 400) input += c
            },
        )

        Spacer(Modifier.height(12.dp))

        // ---- 7. The key sheet ----------------------------------------------
        PanelHeading("Schlüsselzettel (key sheet)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PanelButton("Show key sheet") { showKeySheet = !showKeySheet }
            PanelButton("Reset machine") {
                applySettings(Settings.default())
                input = ""
                output = ""
            }
        }
        if (showKeySheet) {
            Spacer(Modifier.height(8.dp))
            MessageField(
                label = "Send this to the other operator",
                value = settings.toKey().toSheet(),
                onValueChange = {},
                readOnly = true,
                minHeight = 70,
            )
            Spacer(Modifier.height(8.dp))
            MessageField(
                label = "Paste a key sheet here to adopt it",
                value = "",
                onValueChange = { sheet ->
                    Key.parse(sheet).onSuccess { k ->
                        applySettings(
                            Settings(
                                k.model, k.rotors, k.rings, k.indicator,
                                k.reflector, k.plugboard,
                            ),
                        )
                        notice = "Key adopted from the sheet."
                    }.onFailure {
                        notice = "That key sheet is not usable: ${it.message}"
                    }
                },
                minHeight = 60,
                placeholder = "PASTE AN ENIGMA KEY SHEET",
            )
        }

        Footer()
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(listOf(CaseMetal, PanelDark)))
            .border(1.dp, Brass, RoundedCornerShape(6.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "ENIGMA",
            fontSize = 22.sp,
            style = EngravedStyle.copy(color = BrassBright, letterSpacing = 6.sp),
        )
        Text(
            text = "Chiffriermaschine  \u00B7  key sheet compatible with the desktop build",
            fontSize = 9.sp,
            style = EngravedStyle.copy(color = CreamDim, letterSpacing = 0.5.sp),
        )
    }
}

@Composable
private fun Footer() {
    Spacer(Modifier.height(10.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BrassDark),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "\u00A9 2026 Simon Farrant  \u00B7  GnomeWorx",
            fontSize = 10.sp,
            style = EngravedStyle.copy(color = CreamDim, letterSpacing = 1.sp),
        )
        Text(
            text = "For study and simulation. Not a secure cipher.",
            fontSize = 9.sp,
            style = EngravedStyle.copy(color = SignalRed.copy(alpha = 0.8f), letterSpacing = 0.5.sp),
        )
    }
}
