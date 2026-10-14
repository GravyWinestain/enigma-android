// DigitsTest.kt - numbers must survive the machine.
//
// The machine has 26 keys and no digits, so a digit is carried by a letter. The
// convention has one inherent ambiguity - the carrier letters also occur in
// ordinary words - so these tests pin down exactly what is and is not
// recoverable, rather than asserting a round-trip the machine cannot provide.

package com.gnomeworx.enigma

import com.gnomeworx.enigma.core.DIGIT_LETTERS
import com.gnomeworx.enigma.core.Enigma
import com.gnomeworx.enigma.core.Key
import com.gnomeworx.enigma.core.Model
import com.gnomeworx.enigma.core.ReflectorId
import com.gnomeworx.enigma.core.RotorId
import com.gnomeworx.enigma.core.carriesDigit
import com.gnomeworx.enigma.core.digitsToLetters
import com.gnomeworx.enigma.core.groupLetters
import com.gnomeworx.enigma.core.isSendable
import com.gnomeworx.enigma.core.lettersToDigits
import com.gnomeworx.enigma.core.toEnigmaFromKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DigitsTest {

    private fun key() = Key(
        Model.M3,
        listOf(RotorId.IV, RotorId.II, RotorId.V),
        "BDF", "XWB", ReflectorId.B,
        listOf(('A' - 'A') to ('M' - 'A'), ('F' - 'A') to ('I' - 'A')),
    )

    private fun machine(): Enigma = key().toEnigmaFromKey()

    /** A numeric field is enciphered on one machine and recovered on another
     *  built only from the key sheet. */
    @Test
    fun aNumericFieldRoundTrips() {
        val plainNumbers = "1944"
        val cipher = key().toEnigmaFromKey().encrypt(digitsToLetters(plainNumbers))

        val back = Key.parse(key().toSheet()).getOrThrow().toEnigmaFromKey()
        assertEquals(plainNumbers, lettersToDigits(back.encrypt(cipher)))
    }

    /** A whole message of words and numbers survives, because the operator -
     *  not the machine - decides which stretches are numeric. */
    @Test
    fun aWholeMessageSurvivesWhenTheNumericFieldsAreKnown() {
        val words = "REPORTTANKS"
        val numbers = "3121944"

        val cipher = machine().encrypt(digitsToLetters(words + numbers))
        val plain = Key.parse(key().toSheet()).getOrThrow().toEnigmaFromKey().encrypt(cipher)

        assertEquals(words, plain.take(words.length))
        assertEquals(numbers, lettersToDigits(plain.drop(words.length)))
    }

    /** A digit is genuinely enciphered, not passed through in the clear, and
     *  the ciphertext stays inside the machine's alphabet. */
    @Test
    fun digitsDoNotLeakThroughAsThemselves() {
        val cipher = machine().encrypt(digitsToLetters("0123456789"))
        assertTrue("ciphertext must be letters only, got '$cipher'", cipher.all { it in 'A'..'Z' })
        assertNotEquals(DIGIT_LETTERS, cipher)
    }

    /** The same digit at different points enciphers differently, because the
     *  rotors have turned. That is what makes it enciphering and not a
     *  substitution table. */
    @Test
    fun repeatedDigitsDoNotRepeatInCiphertext() {
        val cipher = machine().encrypt(digitsToLetters("1111111111"))
        assertTrue("ten identical digits produced identical ciphertext", cipher.toSet().size > 1)
    }

    /** The known limitation, asserted rather than hidden: reading carrier
     *  letters out of arbitrary text corrupts words, so the caller must scope
     *  the conversion to numeric fields. */
    @Test
    fun readingCarriersFromOrdinaryTextWouldCorruptIt() {
        assertTrue("E carries a digit, so words containing E are affected", 'E'.carriesDigit())
        assertNotEquals("REPORT", lettersToDigits("REPORT"))
        // Applied only to a field that is genuinely numeric, it is exact.
        assertEquals("312", lettersToDigits(digitsToLetters("312")))
    }

    /** Every digit has a distinct carrier, so no two digits collide. */
    @Test
    fun digitCarriersAreDistinct() {
        assertEquals(10, DIGIT_LETTERS.length)
        assertEquals(10, DIGIT_LETTERS.toSet().size)
        for (d in 0..9) assertEquals(d, lettersToDigits(digitsToLetters("$d")).single() - '0')
    }

    /** digitsToLetters strips separators, because the machine has only letters. */
    @Test
    fun separatorsAreStrippedForTransmission() {
        val wire = digitsToLetters("TO 2 SQD 3-4-5")
        assertTrue("separators should not reach the machine", wire.all { it in 'A'..'Z' })
        assertEquals(
            "TO" + DIGIT_LETTERS[2] + "SQD" + DIGIT_LETTERS[3] + DIGIT_LETTERS[4] + DIGIT_LETTERS[5],
            wire,
        )
    }

    /** The five-letter grouping an operator used to read a message aloud. */
    @Test
    fun messagesAreGroupedForTransmission() {
        assertEquals("ABCDE FGH", groupLetters("ABCDEFGH"))
        assertEquals("", groupLetters(""))
    }

    /** isSendable is the gate the UI uses; digits must pass it. */
    @Test
    fun digitsAreSendable() {
        for (d in '0'..'9') assertTrue("$d should be sendable", d.isSendable())
        for (c in 'a'..'z') assertTrue("$c should be sendable", c.isSendable())
        assertTrue("space should not be sendable", !' '.isSendable())
        assertTrue("- should not be sendable", !'-'.isSendable())
    }

    /** A long numeric message, long enough to cross notches, still round-trips. */
    @Test
    fun longNumericMessageCrossesNotches() {
        val plain = "1234567890".repeat(6)
        val cipher = key().toEnigmaFromKey().encrypt(digitsToLetters(plain))
        val back = Key.parse(key().toSheet()).getOrThrow().toEnigmaFromKey()
        assertEquals(plain, lettersToDigits(back.encrypt(cipher)))
    }
}
