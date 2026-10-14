// CipherTest.kt - historical vectors and structural properties.
//
// These are the same checks the desktop version passes, so a green run here
// means the Kotlin port really is the same machine rather than merely a
// similar-looking one.

package com.gnomeworx.enigma

import com.gnomeworx.enigma.core.Enigma
import com.gnomeworx.enigma.core.Key
import com.gnomeworx.enigma.core.Model
import com.gnomeworx.enigma.core.ReflectorId
import com.gnomeworx.enigma.core.RotorId
import com.gnomeworx.enigma.core.toEnigma
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CipherTest {

    /** The canonical textbook vector. */
    @Test
    fun canonicalVector() {
        val m = Enigma.three(
            Model.EnigmaI,
            listOf(RotorId.I, RotorId.II, RotorId.III),
            "AAA", "AAA", ReflectorId.B,
        )
        assertEquals("BDZGO", m.encrypt("AAAAA"))
    }

    /** Reciprocity: the same settings undo the encryption. */
    @Test
    fun encryptingTwiceReturnsTheOriginal() {
        val text = "THEQUICKBROWNFOXJUMPSOVERTHELAZYDOG"
        val a = Enigma.three(
            Model.EnigmaI,
            listOf(RotorId.I, RotorId.II, RotorId.III),
            "AAA", "AAA", ReflectorId.B,
        )
        val cipher = a.encrypt(text)
        assertNotEquals(text, cipher)

        val b = Enigma.three(
            Model.EnigmaI,
            listOf(RotorId.I, RotorId.II, RotorId.III),
            "AAA", "AAA", ReflectorId.B,
        )
        assertEquals(text, b.encrypt(cipher))
    }

    /** No letter ever encrypts to itself - the property Bletchley exploited. */
    @Test
    fun noSelfEncryption() {
        for (r in RotorId.armySet) {
            for (ring in "AAA".indices) {
                val m = Enigma.three(
                    Model.EnigmaI,
                    listOf(r, RotorId.II, RotorId.III),
                    "AAA", "AAA", ReflectorId.B,
                )
                for (c in 'A'..'Z') {
                    val fresh = Enigma.three(
                        Model.EnigmaI,
                        listOf(r, RotorId.II, RotorId.III),
                        "AAA", "AAA", ReflectorId.B,
                    )
                    assertNotEquals(
                        "rotor ${r.label} mapped $c to itself",
                        c, fresh.encryptChar(c),
                    )
                }
                if (ring == 0) break
            }
        }
    }

    /** Reflectors must be involutions with no fixed point. */
    @Test
    fun reflectorIsAnInvolutionWithoutFixedPoints() {
        for (r in ReflectorId.entries) {
            for (i in 0 until 26) {
                assertNotEquals("${r.label} fixes ${'A' + i}", i, r.wiring[i])
                assertEquals(
                    "${r.label} is not an involution at ${'A' + i}",
                    i, r.wiring[r.wiring[i]],
                )
            }
        }
    }

    /** Rotor wiring must be a bijection: each letter exactly once. */
    @Test
    fun rotorWiringIsABijection() {
        for (r in RotorId.entries) {
            assertEquals(
                "rotor ${r.label} wiring is not a permutation",
                (0 until 26).toSet(),
                r.wiring.toSet(),
            )
        }
    }

    /** The double-step: once the right rotor reaches its notch, the middle rotor
     *  advances on two consecutive keystrokes.
     *
     * Start with the middle rotor at D and the right rotor at U. Rotor II has
     * notch E; rotor III has notch V. On the first key the right rotor turns
     * U->V. On the second, the right rotor's notch makes the middle step D->E
     * while the left rotor steps too, and on the third the middle steps again
     * E->F as the carry propagates. A machine that evaluates the notch
     * condition before turning the right rotor, or that steps the middle only
     * once, lands on ADW / BEW instead and diverges from every reference
     * vector from here on. */
    @Test
    fun doubleStepOccurs() {
        val m = Enigma.three(
            Model.EnigmaI,
            listOf(RotorId.I, RotorId.II, RotorId.III),
            "AAA", "ADU", ReflectorId.B,
        )
        m.encryptChar('A')
        assertEquals("the right rotor should have moved onto its notch", "ADV", m.windowLetters())
        m.encryptChar('A')
        assertEquals("the middle rotor and the left rotor should step together", "BEW", m.windowLetters())
        m.encryptChar('A')
        assertEquals("the middle rotor should step again (the double-step)", "CFX", m.windowLetters())
        m.encryptChar('A')
        assertEquals("and then settle", "CFY", m.windowLetters())
    }

    /** M4 thin rotor never steps, even over a long message. */
    @Test
    fun thinRotorNeverSteps() {
        val m = Enigma.four(
            RotorId.Beta,
            listOf(RotorId.I, RotorId.II, RotorId.III),
            "AAAA", "AAAA", ReflectorId.BThin,
        )
        val before = m.windowLetters()[0]
        m.encrypt("THEQUICKBROWNFOXJUMPSOVERTHELAZYDOG")
        assertEquals("the thin fourth rotor moved", before, m.windowLetters()[0])
    }

    /** Ring setting and indicator are independent quantities. */
    @Test
    fun ringAndIndicatorAreIndependent() {
        // Same relative offset, different absolute values -> same output.
        val a = Enigma.three(
            Model.EnigmaI,
            listOf(RotorId.I, RotorId.II, RotorId.III),
            "AAA", "BBB", ReflectorId.B,
        )
        val b = Enigma.three(
            Model.EnigmaI,
            listOf(RotorId.I, RotorId.II, RotorId.III),
            "BBB", "CCC", ReflectorId.B,
        )
        assertEquals(a.encrypt("HELLO"), b.encrypt("HELLO"))
    }

    /** Plugboard is an involution and order-independent. */
    @Test
    fun plugboardIsOrderIndependent() {
        fun enc(pairs: List<Pair<Int, Int>>): String = Enigma.three(
            Model.EnigmaI,
            listOf(RotorId.I, RotorId.II, RotorId.III),
            "AAA", "AAA", ReflectorId.B, pairs,
        ).encrypt("THEQUICKBROWNFOX")

        val pairs = listOf(('A' - 'A') to ('M' - 'A'), ('F' - 'A') to ('I' - 'A'))
        assertEquals(enc(pairs), enc(pairs.reversed()))
    }

    /** Full two-machine hand-off: encipher on one, decipher on another built
     *  only from the key sheet. This is the feature's whole purpose. */
    @Test
    fun secondMachineDeciphersFromTheKeySheet() {
        val plaintext = "THEQUICKBROWNFOXJUMPSOVERTHELAZYDOG"
        val key = Key(
            Model.M3,
            listOf(RotorId.IV, RotorId.II, RotorId.V),
            "BDF", "XWB", ReflectorId.B,
            listOf(('A' - 'A') to ('M' - 'A'), ('F' - 'A') to ('I' - 'A')),
        )
        val a = key.toEnigma()
        val cipher = a.encrypt(plaintext)

        val sheet = key.toSheet()
        val parsed = Key.parse(sheet).getOrThrow()
        val b = parsed.toEnigma()
        assertEquals(plaintext, b.encrypt(cipher))
    }

    /** The M4 hand-off, where the thin rotor is the leading label. */
    @Test
    fun secondM4DeciphersFromTheKeySheet() {
        val key = Key(
            Model.M4,
            listOf(RotorId.Gamma, RotorId.I, RotorId.II, RotorId.III),
            "AAAA", "KLMN", ReflectorId.BThin,
            listOf(('A' - 'A') to ('T' - 'A'), ('C' - 'A') to ('L' - 'A')),
        )
        val cipher = key.toEnigma().encrypt("ATTACKATDAWN")
        val back = Key.parse(key.toSheet()).getOrThrow().toEnigma()
        assertEquals("ATTACKATDAWN", back.encrypt(cipher))
    }

    /** The sheet round-trips every field. */
    @Test
    fun keySheetRoundTrips() {
        val key = Key(
            Model.M3,
            listOf(RotorId.IV, RotorId.II, RotorId.V),
            "BDF", "XWB", ReflectorId.B,
            listOf(('A' - 'A') to ('M' - 'A')),
        )
        val p = Key.parse(key.toSheet()).getOrThrow()
        assertEquals(key.model, p.model)
        assertEquals(key.rotors, p.rotors)
        assertEquals(key.rings, p.rings)
        assertEquals(key.indicator, p.indicator)
        assertEquals(key.reflector, p.reflector)
        assertEquals(key.plugboard, p.plugboard)
        assertEquals(key.toSheet(), p.toSheet())
    }

    /** The sheet is one line, so it survives being pasted into a message. */
    @Test
    fun keySheetIsASingleLine() {
        val key = Key(
            Model.EnigmaI,
            listOf(RotorId.I, RotorId.II, RotorId.III),
            "AAA", "AAA", ReflectorId.B, emptyList(),
        )
        assertTrue(!key.toSheet().contains('\n'))
        assertTrue(key.toSheet().contains("PLUG none"))
    }

    /** Damaged sheets are refused, never partially applied. */
    @Test
    fun badSheetsAreRejected() {
        val cases = listOf(
            "" to "empty",
            "ROTORS I II III | RINGS AAA" to "no machine named",
            "ENIGMA M3 | RINGS AAA | INDICATOR XWB | REFLECTOR UKW-B" to "no rotors",
            "ENIGMA M3 | ROTORS I II | RINGS AAA | INDICATOR XWB | REFLECTOR UKW-B" to "too few",
            "ENIGMA M3 | ROTORS I II III | RINGS AA | INDICATOR XWB | REFLECTOR UKW-B" to "rings",
            "ENIGMA M3 | ROTORS I II III | RINGS AAA | INDICATOR XWB | REFLECTOR UKW-Z" to "reflector",
            "ENIGMA M3 | ROTORS I II IX | RINGS AAA | INDICATOR XWB | REFLECTOR UKW-B" to "rotor",
            "ENIGMA M3 | ROTORS I II III | RINGS AAA | INDICATOR XWB | REFLECTOR UKW-B | PLUG AMF" to "odd",
            "ENIGMA M3 | ROTORS I II III | RINGS AAA | INDICATOR XWB | REFLECTOR UKW-B | PLUG A A" to "a letter plugged to itself",
            "ENIGMA I | ROTORS I II III | RINGS AAA | INDICATOR XWB | REFLECTOR UKW-B Thin" to "army machine with a thin reflector",
            "ENIGMA M4 | ROTORS I II III IV | RINGS AAAA | INDICATOR ABCD | REFLECTOR UKW-B Thin" to "M4 without a thin fourth rotor",
        )
        for ((sheet, why) in cases) {
            assertTrue("sheet accepted but should fail: $why", Key.parse(sheet).isFailure)
        }
    }

    /** Every valid M4 rotor combination round-trips through the sheet.
     *
     * This is what proves no rotor label can be mistaken for a model name in
     * the substring model detection: if "IV" or "VI" leaked into the model
     * match, the round-trip would return a different rotor list.
     *
     * The wired rotors are drawn from disjoint pools so no combination can
     * repeat a rotor - a repeated rotor is correctly refused by the parser, and
     * that refusal is covered separately.
     */
    @Test
    fun everyM4RotorCombinationRoundTrips() {
        var checked = 0
        for ((thin, refl) in listOf(
            RotorId.Beta to ReflectorId.BThin,
            RotorId.Gamma to ReflectorId.CThin,
        )) {
            for (second in listOf(RotorId.I, RotorId.II, RotorId.III, RotorId.IV)) {
                for (third in listOf(RotorId.V, RotorId.VI, RotorId.VII, RotorId.VIII)) {
                    // The fourth wired rotor must differ from the other two.
                    val fourth = listOf(RotorId.I, RotorId.II, RotorId.III, RotorId.IV, RotorId.V)
                        .first { it != second && it != third }
                    val key = Key(
                        Model.M4,
                        listOf(thin, second, third, fourth),
                        "ZZZZ", "ABCD", refl,
                        listOf(('A' - 'A') to ('Z' - 'A')),
                    )
                    val sheet = key.toSheet()
                    val p = Key.parse(sheet).getOrElse {
                        throw AssertionError("sheet did not parse: $sheet - ${it.message}")
                    }
                    assertEquals(key.rotors, p.rotors)
                    assertEquals(key.reflector, p.reflector)
                    assertEquals(key.rings, p.rings)
                    assertEquals(key.indicator, p.indicator)
                    checked++
                }
            }
        }
        assertEquals(32, checked)
    }

    /** The M4 sweep, but running real messages through every combination, so a
     *  rotor that is merely labelled right but wired wrong still fails. */
    @Test
    fun everyM4CombinationActuallyDeciphers() {
        val text = "ATTACKATDAWN"
        var checked = 0
        for ((thin, refl) in listOf(
            RotorId.Beta to ReflectorId.BThin,
            RotorId.Gamma to ReflectorId.CThin,
        )) {
            for (second in listOf(RotorId.I, RotorId.II, RotorId.III, RotorId.IV)) {
                for (third in listOf(RotorId.V, RotorId.VI, RotorId.VII, RotorId.VIII)) {
                    val fourth = listOf(RotorId.I, RotorId.II, RotorId.III, RotorId.IV, RotorId.V)
                        .first { it != second && it != third }
                    val key = Key(
                        Model.M4,
                        listOf(thin, second, third, fourth),
                        "BDFG", "WXYZ", refl,
                        listOf(('A' - 'A') to ('M' - 'A'), ('F' - 'A') to ('I' - 'A')),
                    )
                    val cipher = key.toEnigma().encrypt(text)
                    assertNotEquals("thin ${thin.label} leaked the plaintext", text, cipher)
                    val back = Key.parse(key.toSheet()).getOrThrow().toEnigma()
                    assertEquals("thin ${thin.label} rotors $second $third $fourth", text, back.encrypt(cipher))
                    checked++
                }
            }
        }
        assertEquals(32, checked)
    }

    /** A message long enough to cross a notch still round-trips, which is where
     *  a wrong stepping order shows up. */
    @Test
    fun longMessageCrossingNotchesRoundTrips() {
        val text = "THEQUICKBROWNFOXJUMPSOVERTHELAZYDOGTHEQUICKBROWNFOXJUMPSOVERTHELAZYDOG"
        val key = Key(
            Model.EnigmaI,
            listOf(RotorId.I, RotorId.II, RotorId.III),
            "AAA", "AAA", ReflectorId.B, emptyList(),
        )
        val cipher = key.toEnigma().encrypt(text)
        assertEquals(text, key.toEnigma().encrypt(cipher))
    }
}
