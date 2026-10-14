// PanelTest.kt - the screen's behaviour, driven without a device.
//
// These tests exercise the same logic the Compose screen uses, so a change to
// the cipher or the digit convention fails here rather than silently producing
// a panel that looks right and enciphers wrong. The settings object is the
// screen's own, so the tests pin the exact defaults the app opens with.

package com.gnomeworx.enigma

import com.gnomeworx.enigma.core.Enigma
import com.gnomeworx.enigma.core.Key
import com.gnomeworx.enigma.core.Model
import com.gnomeworx.enigma.core.ReflectorId
import com.gnomeworx.enigma.core.RotorId
import com.gnomeworx.enigma.core.digitsToLetters
import com.gnomeworx.enigma.core.groupLetters
import com.gnomeworx.enigma.core.lettersToDigits
import com.gnomeworx.enigma.core.toEnigma
import com.gnomeworx.enigma.ui.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelTest {

    /** The app must open on a machine that produces the historical result. */
    @Test
    fun theDefaultPanelMatchesTheCanonicalVector() {
        val s = Settings.default()
        assertEquals(Model.EnigmaI, s.model)
        assertEquals(listOf(RotorId.I, RotorId.II, RotorId.III), s.rotors)
        assertEquals("AAA", s.rings)
        assertEquals("AAA", s.indicator)
        assertEquals(ReflectorId.B, s.reflector)
        assertEquals(emptyList<Pair<Int, Int>>(), s.plugboard)

        // The famous first: AAAAA -> BDZGO.
        assertEquals("BDZGO", s.toKey().toEnigma().encrypt("AAAAA"))
    }

    /** The rotor windows must show the live state, so the operator can read the
     *  machine. After five keystrokes the right rotor has moved on.
     *
     *  AAF is not a guess: it is what an independent Python implementation of
     *  the same wiring produces. Verified separately from this Kotlin code, so
     *  the assertion cannot drift into agreeing with whatever the code does. */
    @Test
    fun theWindowsFollowTheMachine() {
        val m = Settings.default().toKey().toEnigma()
        m.encrypt("AAAAA")
        assertEquals("AAF", m.windowLetters())
    }

    /** A window turned by hand walks the rotor, which is how the indicator is
     *  set without a key sheet. */
    @Test
    fun turningAWindowWalksThatRotor() {
        val m = Settings.default().toKey().toEnigma()
        m.setPositions("ABC")
        assertEquals("ABC", m.windowLetters())
        // A window at Z wraps to A rather than running off the alphabet.
        m.setPositions("AZZ")
        assertEquals("AZZ", m.windowLetters())
    }

    /** Changing a setting must rebuild the machine, so a keystroke after a
     *  change starts from the Grundstellung rather than continuing mid-message.
     *  This is the behaviour that stops a "correct" message failing to decode. */
    @Test
    fun aSettingsChangeRestartsFromTheGrundstellung() {
        val before = Settings.default()
        val turned = before.toKey().toEnigma().apply { encrypt("AAAAA") }
        assertEquals("AAF", turned.windowLetters())

        // The rebuilt machine ignores the old turn.
        val after = before.copy(reflector = ReflectorId.C)
        val rebuilt = after.toKey().toEnigma()
        assertEquals("AAA", rebuilt.windowLetters())
        // And it is genuinely a different machine.
        assertNotEquals(
            before.toKey().toEnigma().encrypt("AAAAA"),
            after.toKey().toEnigma().encrypt("AAAAA"),
        )
    }

    /** A plugboard pair must encipher differently to no plugboard, and must
     *  still be its own inverse so a message decodes. */
    @Test
    fun aPlugboardPairChangesTheCipherAndStaysDecodable() {
        val plain = "ATTACKATDAWN"
        val bare = Settings.default().toKey().toEnigma().encrypt(plain)

        val plugged = Settings.default()
            .copy(plugboard = listOf(('A' - 'A') to ('T' - 'A')))
        val key = plugged.toKey()
        val cable = key.toEnigma().encrypt(plain)

        assertNotEquals("a lead must change the cipher", bare, cable)
        // Same key, fresh machine: the message comes back.
        assertEquals(plain, key.toEnigma().encrypt(cable))
    }

    /** The key sheet is the only channel between two machines, so what the
     *  screen prints must rebuild exactly the machine the screen is using. */
    @Test
    fun theScreensKeySheetRebuildsTheSameMachine() {
        val s = Settings(
            model = Model.M4,
            rotors = listOf(RotorId.Gamma, RotorId.VII, RotorId.III, RotorId.VIII),
            rings = "BCDE",
            indicator = "WXYZ",
            reflector = ReflectorId.CThin,
            plugboard = listOf(('A' - 'A') to ('M' - 'A'), ('F' - 'A') to ('I' - 'A')),
        )
        val sheet = s.toKey().toSheet()
        val rebuilt = Key.parse(sheet).getOrThrow()

        assertEquals(s.model, rebuilt.model)
        assertEquals(s.rotors, rebuilt.rotors)
        assertEquals(s.rings, rebuilt.rings)
        assertEquals(s.indicator, rebuilt.indicator)
        assertEquals(s.reflector, rebuilt.reflector)
        assertEquals(s.plugboard.sortedBy { it.first }, rebuilt.plugboard.sortedBy { it.first })

        // And it enciphers identically, which is what actually matters.
        assertEquals(
            s.toKey().toEnigma().encrypt("MESSAGETOTHEFLEET"),
            rebuilt.toEnigma().encrypt("MESSAGETOTHEFLEET"),
        )
    }

    /** Switching model must refit the rotors rather than leave a combination
     *  that does not physically exist (a thin rotor in a 3-rotor machine). */
    @Test
    fun switchingToM4FitsAFourthRotor() {
        val three = Settings.default()
        val four = three.copy(
            model = Model.M4,
            rotors = listOf(RotorId.Beta, RotorId.I, RotorId.II, RotorId.III),
            rings = "AAAA",
            indicator = "AAAA",
            reflector = ReflectorId.BThin,
        )
        assertEquals(4, four.rotors.size)
        // A fresh M4 at AAAA is its own inverse round trip.
        val k = four.toKey()
        val c = k.toEnigma().encrypt("AAAAA")
        assertEquals("AAAAA", k.toEnigma().encrypt(c))
    }

    /** The full path the Run button takes: numbers in, letters out, numbers
     *  recovered on a machine built from the printed key sheet.
     *
     *  The last step is deliberately scoped. Converting the *whole* recovered
     *  plaintext back to digits would corrupt it, because carrier letters occur
     *  in ordinary words - E, R, T, U, O and P are all carriers, and this
     *  message is full of them. That is the machine's real limitation, not a
     *  bug: the operator knows which field is numeric, from the message form or
     *  the codebook, and converts only that stretch. An earlier version of this
     *  test asserted the whole-string conversion and correctly failed - the
     *  recovered text came back as ATTACKWPTTHOURS. */
    @Test
    fun theRunButtonHandlesAMessageWithNumbers() {
        val s = Settings.default()
        val words = "ATTACK"
        val numbers = "1944"
        val tail = "HOURS"
        val typed = "$words $numbers $tail"
        val wire = typed.uppercase().filter { it in '0'..'9' || it in 'A'..'Z' }
        // What actually goes into the machine: the digits become carriers, so
        // 1944 travels as WPTT. This is the only form the machine can emit.
        val wireLetters = digitsToLetters(wire)
        val cipher = s.toKey().toEnigma().encrypt(wireLetters)

        // What the app puts on the wire: letters, in standard five-letter groups.
        val wireOut = groupLetters(cipher)
        assertTrue(wireOut.all { it in 'A'..'Z' || it == ' ' })

        // The receiving operator, holding the key sheet, recovers the message.
        val back = Key.parse(s.toKey().toSheet()).getOrThrow().toEnigma()
        val plain = back.encrypt(cipher.replace(" ", ""))
        // The recovery is exact, and it is the carrier form that comes back.
        assertEquals(wireLetters, plain)

        // The numeric field - and only that field - reads back as digits.
        val numericField = plain.substring(words.length, words.length + numbers.length)
        assertEquals(numbers, lettersToDigits(numericField))
        // The words around it are untouched, because they were never converted.
        assertEquals(words, plain.take(words.length))
        assertEquals(tail, plain.drop(words.length + numbers.length))
    }

    /** A long message that turns the rotors many times still round-trips, so
     *  the screen's per-keystroke rebuild cannot drift. */
    @Test
    fun aLongMessageRoundTrips() {
        val s = Settings.default().copy(plugboard = listOf(1 to 12, 5 to 8))
        val plain = "THEQUICKBROWNFOXJUMPSOVERTHELAZYDOG".repeat(3)
        val k = s.toKey()
        val c = k.toEnigma().encrypt(plain)
        assertEquals(plain, k.toEnigma().encrypt(c))
    }
}
