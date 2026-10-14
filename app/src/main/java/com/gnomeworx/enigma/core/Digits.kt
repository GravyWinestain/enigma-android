// Digits.kt - carry numbers through a machine that only has letters.
//
// A real Enigma keyboard had 26 keys and no digits. Numbers were not typed:
// operators spelled them out (EINS, ZWEI, VIER) or, far more often, substituted
// from a codebook - a large part of why German traffic was so repetitive, and
// so breakable. There is no historical numeric keyboard to reproduce, so this
// makes the choice explicit instead of silently dropping digits.
//
// The rule: a digit is enciphered as a letter, so it is protected by the rotor
// and plugboard settings exactly as a letter is, and the ciphertext stays
// inside the 26-letter alphabet a real machine could send.
//
// The one thing that cannot be done is to recover numbers from the plaintext by
// letter identity alone. Ten letters must double for the ten digits, and those
// same ten letters also occur naturally in words - an E in REPORT is not a 4.
// No machine of this kind can tell them apart, and pretending otherwise
// corrupts words into numbers. So the convention matches the real one: the
// operator knows which stretches are numeric (it is on the message form or in
// the codebook), and only those stretches are read as digits. [digitsToLetters]
// and [lettersToDigits] apply to a field the caller has identified as numeric.
//
// The carrier letters are the German typewriter's top row, QWERTZUIOP, which is
// the mnemonic an operator would recognise, and the substitution is applied
// before the plugboard so a plugged letter behaves normally on the board.

package com.gnomeworx.enigma.core

/** The ten letters used to stand in for 0-9, in order. */
const val DIGIT_LETTERS = "QWERTZUIOP"

/** The reverse map: a carrier letter back to the digit it stands for.
 *
 * Only meaningful for a stretch the operator has declared numeric; see the file
 * header for why this cannot be applied blindly to a whole message. */
private val LETTER_TO_DIGIT: Map<Char, Char> =
    DIGIT_LETTERS.mapIndexed { i, c -> c to ('0' + i) }.toMap()

/** True if this character can be sent through the machine.
 *
 * Letters and digits can both be sent. Everything else - spaces, punctuation -
 * is a separator and passes through untouched.
 */
fun Char.isSendable(): Boolean = this in '0'..'9' || uppercaseChar() in 'A'..'Z'

/** Prepare a message for the machine.
 *
 * Each digit is replaced by its carrier letter; letters are upper-cased.
 * Separators are dropped, because the machine only has letter keys - a real
 * operator did not send spaces either, and the five-letter grouping was there
 * precisely so the receiving station could re-space the text by eye.
 */
fun digitsToLetters(text: String): String = buildString {
    for (c in text) {
        when {
            c in '0'..'9' -> append(DIGIT_LETTERS[c - '0'])
            c.uppercaseChar() in 'A'..'Z' -> append(c.uppercaseChar())
        }
    }
}

/** Read a numeric stretch of plaintext back as digits.
 *
 * Apply only to text that is known to be numeric. A carrier letter that was
 * never a digit will be read as one; that ambiguity is inherent and is why the
 * operator, not the machine, made this call.
 */
fun lettersToDigits(text: String): String = buildString {
    for (c in text) append(LETTER_TO_DIGIT[c.uppercaseChar()] ?: c.uppercaseChar())
}

/** True if a value carries a digit. Useful for telling an operator which
 *  letters on the wire are numbers. */
fun Char.carriesDigit(): Boolean = LETTER_TO_DIGIT.containsKey(uppercaseChar())

/** The digit this letter carries, or null. */
fun Char.carriedDigit(): Char? = LETTER_TO_DIGIT[uppercaseChar()]

/** Restore the historical five-letter groups so a message can be read aloud
 *  and checked over the radio without losing one's place. */
fun groupLetters(text: String, size: Int = 5): String =
    text.chunked(size).joinToString(" ")
