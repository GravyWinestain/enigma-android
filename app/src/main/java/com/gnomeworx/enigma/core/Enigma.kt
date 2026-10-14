// Enigma.kt - the cipher core: rotor stack, stepping, plugboard, encrypt path.
//
// A Kotlin port of src/machine.rs from the desktop version. The algorithm is
// kept structurally identical to the Rust so the two can be diffed by eye and
// so the same historical test vectors apply to both.
//
// Two details are load-bearing and easy to get subtly wrong:
//
//  * The stepping is the classic double-step. The middle rotor steps when
//    either the middle rotor itself sits on a notch, or the right-hand rotor
//    does. Evaluating that condition after turning the right rotor, rather than
//    before, agrees with reference vectors on short messages and silently
//    diverges once a notch is crossed.
//
//  * The M4's thin fourth rotor never steps and never participates in the
//    carry, which is what made the naval key space genuinely different rather
//    than merely larger.

package com.gnomeworx.enigma.core

/** Wrap an integer into 0..25. Kotlin's % keeps the sign, so correct it. */
internal fun wrap26(v: Int): Int {
    val m = v % 26
    return if (m < 0) m + 26 else m
}

/** One rotor in the stack: which type, its ring offset, and where it sits. */
class Rotor(val id: RotorId, ring: Int, position: Int) {
    /** Ringstellung: offset of the wiring relative to the alphabet ring. */
    var ring: Int = wrap26(ring)

    /** Grundstellung: the letter currently showing in the window. */
    var position: Int = wrap26(position)

    /** True if this rotor is sitting on one of its notch letters right now. */
    fun atNotch(): Boolean = id.notches.any { it == position }

    /** Advance one position: the rotor physically turns one letter. */
    fun step() {
        position = wrap26(position + 1)
    }

    /** Contact offset on the right-hand (input) side for this position/ring. */
    private fun rightIndex(c: Int): Int = wrap26(c + position - ring)

    /** Convert an index from the internal wiring back to a letter offset. */
    private fun leftIndex(c: Int): Int = wrap26(c - position + ring)

    /** Forward pass: signal entering from the right, leaving to the left. */
    fun forward(c: Int): Int {
        val idx = rightIndex(c)
        return leftIndex(id.wiring[idx])
    }

    /** Reverse pass: signal coming back from the left, leaving to the right.
     *
     * This is the inverse permutation: find the index whose wiring entry equals
     * our current index, then map back out.
     */
    fun backward(c: Int): Int {
        val target = rightIndex(c)
        var src = 0
        for (i in id.wiring.indices) {
            if (id.wiring[i] == target) {
                src = i
                break
            }
        }
        return leftIndex(src)
    }

    /** Copy, so the UI can snapshot state without aliasing the live machine. */
    fun copy(): Rotor = Rotor(id, ring, position)
}

/** A configured Enigma machine with live rotor state.
 *
 * @param model which physical machine this is
 * @param rotors the rotor stack, index 0 = leftmost. For the M4 index 0 is the
 *   thin fourth rotor, which is never stepped.
 * @param reflector the selected reflector
 * @param plugboard plug pairs, each stored with from < to in 0..25
 */
class Enigma(
    val model: Model,
    val rotors: MutableList<Rotor>,
    val reflector: ReflectorId,
    plugboardPairs: List<Pair<Int, Int>> = emptyList(),
) {
    /** Each letter's plugboard partner, or itself if unplugged. */
    private val plug: IntArray = IntArray(26) { it }

    init {
        for ((a, b) in plugboardPairs) {
            if (a in 0..25 && b in 0..25 && a != b) {
                plug[a] = b
                plug[b] = a
            }
        }
    }

    /** The current window letters, left to right. */
    fun windowLetters(): String = rotors.map { ('A' + it.position) }.joinToString("")

    /** Replace the live rotor positions from a string of letters. */
    fun setPositions(letters: String) {
        val clean = letters.uppercase().filter { it in 'A'..'Z' }
        rotors.forEachIndexed { i, r ->
            if (i < clean.length) r.position = clean[i] - 'A'
        }
    }

    /**
     * Advance the rotors before encrypting the next character.
     *
     * For a 3-rotor machine the indices are [left, middle, right] = [0, 1, 2].
     * For the M4 the thin rotor sits at index 0 and never steps, so the logic
     * operates on the top three indices and the same rules apply.
     */
    private fun stepRotors() {
        val n = rotors.size
        val right = n - 1
        val middle = n - 2
        val left = n - 3

        // Does the middle rotor step? Yes if it is at a notch, or if the right
        // rotor is at a notch - the classic double-step.
        val middleSteps = rotors[middle].atNotch() || rotors[right].atNotch()

        if (middleSteps && left >= 0) {
            rotors[left].step()
        }
        if (middleSteps) {
            rotors[middle].step()
        }
        rotors[right].step()
    }

    /** Encrypt one letter.
     *
     * Plugboard, rotor stack forward, reflector, rotor stack backward,
     * plugboard. Non A-Z characters are returned unchanged and do not advance
     * the rotors.
     */
    fun encryptChar(c: Char): Char {
        val up = c.uppercaseChar()
        if (up !in 'A'..'Z') return c

        stepRotors()

        var x = up - 'A'
        x = plug[x]

        // Forward through the stack, right to left.
        for (i in rotors.indices.reversed()) {
            x = rotors[i].forward(x)
        }

        // Reflector.
        x = reflector.wiring[x]

        // Backward through the stack, left to right.
        for (i in rotors.indices) {
            x = rotors[i].backward(x)
        }

        x = plug[x]
        return ('A' + x)
    }

    /** Encrypt a whole message. Spaces and punctuation pass through. */
    fun encrypt(text: String): String = buildString {
        for (c in text) append(encryptChar(c))
    }

    /** Encrypt with a fresh machine, leaving this one's live state untouched.
     *
     * The UI needs this: re-running the same input must start from the same
     * Grundstellung, not from wherever the previous run left the rotors.
     */
    fun encryptFrom(text: String, startPositions: String): String {
        val saved = windowLetters()
        setPositions(startPositions)
        val out = encrypt(text)
        setPositions(saved)
        return out
    }

    fun copy(): Enigma = Enigma(
        model,
        rotors.map { it.copy() }.toMutableList(),
        reflector,
        (0 until 26).filter { plug[it] > it }.map { it to plug[it] },
    )

    companion object {
        /** Build a 3-rotor machine. Rotors are given left-to-right. */
        fun three(
            model: Model,
            rotorIds: List<RotorId>,
            rings: String,
            positions: String,
            reflector: ReflectorId,
            plugboard: List<Pair<Int, Int>> = emptyList(),
        ): Enigma {
            val r = rings.uppercase().padEnd(3, 'A')
            val p = positions.uppercase().padEnd(3, 'A')
            val stack = rotorIds.take(3).mapIndexed { i, id ->
                Rotor(id, r[i] - 'A', p[i] - 'A')
            }.toMutableList()
            return Enigma(model, stack, reflector, plugboard)
        }

        /** Build an M4 four-rotor machine. `thin` occupies the leftmost slot. */
        fun four(
            thin: RotorId,
            rotorIds: List<RotorId>,
            rings: String,
            positions: String,
            reflector: ReflectorId,
            plugboard: List<Pair<Int, Int>> = emptyList(),
        ): Enigma {
            val r = rings.uppercase().padEnd(4, 'A')
            val p = positions.uppercase().padEnd(4, 'A')
            val stack = mutableListOf(Rotor(thin, r[0] - 'A', p[0] - 'A'))
            rotorIds.take(3).forEachIndexed { i, id ->
                stack.add(Rotor(id, r[i + 1] - 'A', p[i + 1] - 'A'))
            }
            return Enigma(Model.M4, stack, reflector, plugboard)
        }
    }
}
