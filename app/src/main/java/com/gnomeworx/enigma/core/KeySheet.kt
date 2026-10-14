// KeySheet.kt - encode and decode the operator's key sheet.
//
// Format-compatible with the desktop version in ~/dev/enigma-rs, so a sheet
// produced by either app can be pasted into the other. That interoperability is
// the whole point of the feature: the code Simon sends to a second operator has
// to work on whichever machine they happen to have.
//
//   M3 (3-rotor naval) | ROTORS IV II V | RINGS BDF | INDICATOR XWB | REFLECTOR UKW-B | PLUG AM FI NV
//
// Rotors are listed left-to-right as on the physical machine, and for the M4
// the leading entry is the thin rotor, matching how the settings sheet was
// written.

package com.gnomeworx.enigma.core

/** The settings needed to work a message on a machine, minus the message. */
data class Key(
    val model: Model,
    val rotors: List<RotorId>,
    val rings: String,
    val indicator: String,
    val reflector: ReflectorId,
    val plugboard: List<Pair<Int, Int>>,
) {
    /** Render as the single-line key sheet an operator would pass on. */
    fun toSheet(): String {
        val rotorLabels = rotors.joinToString(" ") { it.label }
        val plug = if (plugboard.isEmpty()) {
            "none"
        } else {
            plugboard.joinToString(" ") { (a, b) -> "${('A' + a)}${('A' + b)}" }
        }
        return buildString {
            append(model.label)
            append(" | ROTORS ").append(rotorLabels)
            append(" | RINGS ").append(rings.uppercase())
            append(" | INDICATOR ").append(indicator.uppercase())
            append(" | REFLECTOR ").append(reflector.label)
            append(" | PLUG ").append(plug)
        }
    }

    companion object {
        /**
         * Parse a key sheet.
         *
         * Returns a [Result] so a damaged sheet reports what is wrong rather
         * than silently producing a machine with the wrong key - which would
         * look like a plain deciphering failure.
         */
        fun parse(sheet: String): Result<Key> {
            val up = sheet.uppercase()

            // Which machine? Every other field is letters-only, so M3/M4 can
            // only appear in the model name and this substring search is safe.
            val model = when {
                up.contains("M4") -> Model.M4
                up.contains("M3") -> Model.M3
                up.contains("ENIGMAI") || up.contains("ENIGMA I") -> Model.EnigmaI
                else -> return Result.failure(
                    IllegalArgumentException(
                        "key sheet does not name a machine (expected Enigma I, M3 or M4)",
                    ),
                )
            }
            val want = model.rotorCount

            fun field(name: String): String? {
                val marker = "$name "
                val start = up.indexOf(marker)
                if (start < 0) return null
                val rest = up.substring(start + marker.length)
                val end = rest.indexOf('|').takeIf { it >= 0 } ?: rest.length
                return rest.substring(0, end).trim()
            }

            val rotorField = field("ROTORS")
                ?: return Result.failure(IllegalArgumentException("key sheet has no ROTORS field"))

            val ids = mutableListOf<RotorId>()
            for (tok in rotorField.split(Regex("\\s+")).filter { it.isNotEmpty() }) {
                val id = RotorId.fromLabel(tok)
                    ?: return Result.failure(IllegalArgumentException("unknown rotor '$tok'"))
                ids.add(id)
            }
            if (ids.size != want) {
                return Result.failure(
                    IllegalArgumentException(
                        "${model.label} needs $want rotors but the key sheet lists ${ids.size}",
                    ),
                )
            }

            val ringsRaw = field("RINGS")
                ?: return Result.failure(IllegalArgumentException("key sheet has no RINGS field"))
            val rings = ringsRaw.filter { it in 'A'..'Z' }
            if (rings.length != want) {
                return Result.failure(
                    IllegalArgumentException("RINGS needs $want letters, got '$ringsRaw'"),
                )
            }

            val indRaw = field("INDICATOR")
                ?: return Result.failure(
                    IllegalArgumentException("key sheet has no INDICATOR field"),
                )
            val indicator = indRaw.filter { it in 'A'..'Z' }
            if (indicator.length != want) {
                return Result.failure(
                    IllegalArgumentException("INDICATOR needs $want letters, got '$indRaw'"),
                )
            }

            val reflRaw = field("REFLECTOR")
                ?: return Result.failure(
                    IllegalArgumentException("key sheet has no REFLECTOR field"),
                )
            val reflector = ReflectorId.fromLabel(reflRaw)
                ?: return Result.failure(
                    IllegalArgumentException("unknown reflector '$reflRaw'"),
                )

            val plugRaw = field("PLUG")
            val plugLetters = if (plugRaw == null || plugRaw.equals("none", true)) {
                ""
            } else {
                plugRaw.filter { it in 'A'..'Z' }
            }
            if (plugLetters.length % 2 != 0) {
                return Result.failure(
                    IllegalArgumentException(
                        "PLUG has an odd number of letters ('$plugLetters')",
                    ),
                )
            }
            val plugboard = plugLetters.chunked(2).map { (it[0] - 'A') to (it[1] - 'A') }
            for ((a, b) in plugboard) {
                if (a == b) {
                    return Result.failure(
                        IllegalArgumentException(
                            "PLUG pairs ${'A' + a}${'A' + b}, which is not a real connection",
                        ),
                    )
                }
            }
            // Two spellings of the same connection are the same connection, so
            // reject conflicting ones: a letter cannot go to two partners.
            val partners = mutableMapOf<Int, Int>()
            for ((a, b) in plugboard) {
                for ((from, to) in listOf(a to b, b to a)) {
                    val existing = partners[from]
                    if (existing != null && existing != to) {
                        return Result.failure(
                            IllegalArgumentException(
                                "PLUG sends ${'A' + from} to both " +
                                    "${'A' + existing} and ${'A' + to}",
                            ),
                        )
                    }
                    partners[from] = to
                }
            }

            // A sheet can be syntactically fine and still describe an impossible
            // machine: a duplicate rotor, or one not fitted to this model.
            if (ids.toSet().size != ids.size) {
                return Result.failure(
                    IllegalArgumentException("the sheet fits the same rotor twice"),
                )
            }
            val allowed = if (model.hasFourthRotor) {
                // Slot 0 is the thin rotor; slots 1..3 are the wired rotors.
                val thinOk = ids[0] == RotorId.Beta || ids[0] == RotorId.Gamma
                if (!thinOk) {
                    return Result.failure(
                        IllegalArgumentException(
                            "an M4 needs Beta or Gamma in the fourth slot, not ${ids[0].label}",
                        ),
                    )
                }
                ids.drop(1).all { it in RotorId.navalSet && it != RotorId.Beta && it != RotorId.Gamma }
            } else {
                ids.all { it in RotorId.armySet }
            }
            if (!allowed) {
                return Result.failure(
                    IllegalArgumentException(
                        "the listed rotors cannot be fitted to ${model.label}",
                    ),
                )
            }
            if (model.hasFourthRotor && ReflectorId.fromLabel(reflector.label) !in ReflectorId.navalSet) {
                return Result.failure(
                    IllegalArgumentException(
                        "an M4 needs a thin reflector, not ${reflector.label}",
                    ),
                )
            }
            if (!model.hasFourthRotor && reflector !in ReflectorId.armySet) {
                return Result.failure(
                    IllegalArgumentException(
                        "${model.label} cannot use the thin reflector ${reflector.label}",
                    ),
                )
            }

            return Result.success(Key(model, ids, rings, indicator, reflector, plugboard))
        }
    }
}

/** Build a live machine from a parsed key. */
fun Key.toEnigma(): Enigma = if (model.hasFourthRotor) {
    Enigma.four(rotors[0], rotors.drop(1), rings, indicator, reflector, plugboard)
} else {
    Enigma.three(model, rotors, rings, indicator, reflector, plugboard)
}

/** Same as [toEnigma], named for readability where the caller is working with
 *  a key that came off a sheet rather than one it built itself. */
fun Key.toEnigmaFromKey(): Enigma = toEnigma()
