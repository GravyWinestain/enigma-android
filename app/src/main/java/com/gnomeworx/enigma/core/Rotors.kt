// Rotors.kt - Enigma rotor, reflector and model definitions.
//
// A Kotlin port of src/rotors.rs from the desktop version. The wiring tables
// are the verified historical wirings; they are stored as 26-character
// substitution alphabets where index = input letter (0 = 'A') and the value is
// the output letter. Do not "tidy" them: a single transposed pair silently
// produces plausible-looking but wrong ciphertext.

package com.gnomeworx.enigma.core

/** Rotor identifier.
 *
 * Enigma I and M3 have I..V. The naval M4 adds VI..VIII plus the thin rotors
 * Beta and Gamma, which occupy the fourth slot and have no notch.
 */
enum class RotorId(val label: String, private val wiringStr: String, private val notchStr: String) {
    I("I", "EKMFLGDQVZNTOWYHXUSPAIBRCJ", "Q"),
    II("II", "AJDKSIRUXBLHWTMCQGZNPYFVOE", "E"),
    III("III", "BDFHJLCPRTXVZNYEIWGAKMUSQO", "V"),
    IV("IV", "ESOVPZJAYQUIRHXLNFTGKDCMWB", "J"),
    V("V", "VZBRGITYUPSDNHLXAWMJQOFECK", "Z"),
    // VI..VIII carry two notches each, as on the naval set.
    VI("VI", "JPGVOUMFYQBENHZRDKASXLICTW", "ZM"),
    VII("VII", "NZJHGRCXMYSWBOUFAIVLPEKQDT", "ZM"),
    VIII("VIII", "FKQHTLXOCBJSPDZRAMEWNIUYGV", "ZM"),
    // Thin fourth rotors: no notch, so the fourth rotor never causes a carry.
    Beta("Beta", "LEYJVCNIXWPBQMDRTAKZGFUHOS", ""),
    Gamma("Gamma", "FSOKANUERHMBTIYCWLQPZXVGJD", ""),
    ;

    /** The 26-entry wiring table as 0..25 offsets. */
    val wiring: IntArray = IntArray(26) { wiringStr[it] - 'A' }

    /** Notch letters, as 0..25 offsets. Empty for the thin rotors. */
    val notches: IntArray = IntArray(notchStr.length) { notchStr[it] - 'A' }

    companion object {
        /** Rotors available on the 3-rotor Enigma I and M3. */
        val armySet = listOf(I, II, III, IV, V)

        /** Rotors available on the 4-rotor M4, including the thin pair. */
        val navalSet = listOf(I, II, III, IV, V, VI, VII, VIII, Beta, Gamma)

        fun fromLabel(label: String): RotorId? = entries.firstOrNull {
            it.label.equals(label, ignoreCase = true)
        }
    }
}

/** Reflector identifier.
 *
 * UKW-A/B/C are the 3-rotor reflectors; B-Thin and C-Thin are the naval M4
 * reflectors. A reflector is an involution: every letter maps somewhere other
 * than itself. That no-self-mapping property is the structural weakness
 * Bletchley exploited, and the cipher tests assert it.
 */
enum class ReflectorId(val label: String, private val wiringStr: String) {
    A("UKW-A", "EJMZALYXVBWFCRQUONTSPIKHGD"),
    B("UKW-B", "YRUHQSLDPXNGOKMIEBFZCWVJAT"),
    C("UKW-C", "FVPJIAOYEDRZXWGCTKUQSBNMHL"),
    BThin("UKW-B Thin", "ENKQAUYWJICOPBLMDXZVFTHRGS"),
    CThin("UKW-C Thin", "RDOBJNTKVEHMLFCWZAXGYIPSUQ"),
    ;

    /** The reflector wiring as 0..25 offsets. */
    val wiring: IntArray = IntArray(26) { wiringStr[it] - 'A' }

    companion object {
        /** Reflectors valid on a 3-rotor machine. */
        val armySet = listOf(A, B, C)

        /** Reflectors valid with the M4 thin fourth rotor. */
        val navalSet = listOf(BThin, CThin)

        /** Parse a label as it appears on a key sheet, tolerating spacing. */
        fun fromLabel(label: String): ReflectorId? {
            val norm = label.uppercase().replace(" ", "")
            return entries.firstOrNull { it.label.uppercase().replace(" ", "") == norm }
        }
    }
}

/** Which physical machine is being emulated.
 *
 * This decides which rotors and reflectors are selectable and whether a fourth
 * rotor slot exists.
 */
enum class Model(val label: String) {
    /** Three-rotor Wehrmacht / Luftwaffe machine: rotors I..V, UKW A/B/C. */
    EnigmaI("Enigma I"),

    /** Three-rotor Kriegsmarine M3: same rotor set as the Enigma I. */
    M3("M3 naval"),

    /** Four-rotor Kriegsmarine M4 with the thin Beta/Gamma fourth rotor. */
    M4("M4 naval"),
    ;

    /** True when this model has the thin fourth rotor slot. */
    val hasFourthRotor: Boolean get() = this == M4

    /** How many rotor slots the machine has. */
    val rotorCount: Int get() = if (hasFourthRotor) 4 else 3

    /** Selectable rotors for this model. */
    val rotorSet: List<RotorId>
        get() = if (hasFourthRotor) RotorId.navalSet else RotorId.armySet

    /** Selectable reflectors for this model. */
    val reflectorSet: List<ReflectorId>
        get() = if (hasFourthRotor) ReflectorId.navalSet else ReflectorId.armySet
}
