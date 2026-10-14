# Enigma (Android) — user guide

A working Enigma machine on a phone: **Enigma I**, **M3** (three-rotor naval)
and **M4** (four-rotor naval). The cipher core is historically faithful; the
panel is laid out like the real machine.

Key sheets are compatible with the desktop build
[enigma-rs](https://github.com/GravyWinestain/enigma-rs), so a key printed on
one machine can be pasted into the other.

This guide covers using the machine and exchanging keys with a second operator.
For how the internals work and why, see the README.

---

## The panel

The screen scrolls. The sections run top to bottom in the order of the physical
machine, which is what makes an Enigma read as an Enigma:

    rotor windows  ->  lampboard  ->  machine settings
    ->  Ringstellung  ->  Grundstellung  ->  plugboard
    ->  message form  ->  keyboard  ->  key sheet

| Section | What it is |
|---|---|
| **Rotor windows** | The letters showing through the glass. Tap ▲ / ▼ under a window to turn that rotor by hand. |
| **Lampboard** | Lights one lamp per keystroke — the machine's output as an operator saw it. |
| **Machine** | Model, rotor per slot, reflector — picked from lit brass detents. |
| **Ringstellung** | The alphabet-ring offset, one stepper per rotor. |
| **Grundstellung** | The starting window position, one stepper per rotor. |
| **Steckerbrett** | The plugboard. Tap one socket, then another, to fit a lead between them. |
| **Message** | Plaintext in, cipher out. |
| **Tastatur** | The 26 keys. Tap to encipher one letter at a time. |
| **Schlüsselzettel** | Show or paste a key sheet. |

---

## Sending a message

1. Set the machine: **Machine**, **Rotor** per slot, **Ringstellung**,
   **Grundstellung**, **Reflector** and the **Steckerbrett**.
2. Type the plaintext into **Plaintext in** — or tap the keys one at a time.
   Only letters and digits are accepted; the machine had 26 keys and no space
   bar, so punctuation and spaces are dropped.
3. Press **Run the cipher**.
4. **Cipher out** holds the ciphertext, grouped into five-letter blocks the way
   real traffic was transmitted.

## Receiving a message

Enigma is **reciprocal**: the same settings that enciphered a message will
decipher it. To read a message someone sends you:

1. Set your machine to *their* key — the easiest way is the **key sheet**
   (below).
2. Paste the ciphertext into **Plaintext in**.
3. Press **Run the cipher**. The plaintext appears in **Cipher out**.

There is no "decrypt mode". The same settings, run the same way, do it.

---

## Key sheet

Press **Show key sheet** and the panel prints a single line describing every
setting needed to work the machine. This is the decryption code you hand to the
other operator:

```
M3 naval | ROTORS IV II V | RINGS BDF | INDICATOR XWB | REFLECTOR UKW-B | PLUG AM FI NV
```

An M4 sheet, for contrast — note the thin rotor in the leftmost `ROTORS` slot,
four ring letters, four indicator letters, and the thin reflector:

```
M4 naval | ROTORS Gamma VII III VIII | RINGS BCDE | INDICATOR WXYZ | REFLECTOR UKW-C Thin | PLUG AM
```

| Field | Meaning |
|---|---|
| Machine | `Enigma I`, `M3 naval` or `M4 naval` |
| `ROTORS` | Rotor types **left to right**. For the M4 the first entry is the thin rotor (Beta or Gamma). |
| `RINGS` | Ringstellung — the alphabet-ring offset, one letter per rotor |
| `INDICATOR` | Grundstellung — where each rotor's window sits at the start |
| `REFLECTOR` | `UKW-A`, `UKW-B`, `UKW-C`, or the M4 thin types (`UKW-B Thin`, `UKW-C Thin`) |
| `PLUG` | Plugboard pairs, or `none` |

The sheet describes the **configuration only** — never the message text.

### Sending your key

1. Set up the machine and check the sheet reads as you intend.
2. Press **Show key sheet**, then select and copy the text.
3. Paste it into whatever you are sending the message with.

Send the key sheet and the ciphertext. They are two separate things: the sheet
is the machine setup, the ciphertext is the message.

### Using a key you were sent

1. Copy the sheet they sent you.
2. Paste it into **Paste a key sheet here to adopt it**. Every setting is applied
   at once and the status line confirms it.

A sheet that is damaged or incomplete is **refused**, with a note saying what is
wrong. This is deliberate: a key that parses only partly would leave you with a
machine that looks correctly set up but produces plausible-looking nonsense.
Better to be told the sheet is broken.

You can also set the machine by hand from the controls and skip the paste
entirely — the sheet is a convenience, not a requirement.

### Sharing a key safely

The key sheet is the machine setting, not a secret in itself — but a key plus
its ciphertext is everything needed to read the message, so send them **by
different routes** if it matters. This mirrors the real practice of sending
indicator settings separately from message traffic.

---

## Numbers

The machine has 26 keys and no digits, so a digit has to travel as a letter.
Digits **0–9** are carried by the letters **`QWERTZUIOP`**, in order:

| Digit | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|---|---|---|---|---|---|---|---|---|---|---|
| Carried by | Q | W | E | R | T | Z | U | I | O | P |

So `1944` travels across the wire as `WPTT`.

Type digits straight into **Plaintext in** — the app converts them for you.

**The limitation, stated plainly.** This is a convention, not a cipher. The
carrier letters also occur in ordinary words — `E`, `R`, `T`, `U`, `O` and `P`
are all carriers — so converting an arbitrary stretch of recovered text back to
digits will corrupt the words around it. `ATTACKHOURS` would come back mangled
if you tried to read digits out of the whole thing.

The operator knows which part of a message is numeric, from the message form or
the codebook, and converts only that field. The app does the same: it never
converts recovered text back to digits on its own. Real operators had a codebook
for exactly this reason.

---

## Worked example

Machine A, set to `ROTORS IV II V | RINGS BDF | INDICATOR XWB | REFLECTOR
UKW-B | PLUG AM FI NV`:

```
plaintext : THEQU ICKBR OWNFO XJUMP SOVER THELA ZYDOG
cipher    : DAADL KUEJV FGBMA VQKOS RXGXV NWCWX SBQDF
key sheet : M3 naval | ROTORS IV II V | RINGS BDF | INDICATOR XWB | REFLECTOR UKW-B | PLUG AM FI NV
```

Machine B takes only that key sheet:

```
deciphered: THEQU ICKBR OWNFO XJUMP SOVER THELA ZYDOG
```

That message deliberately runs past a rotor notch, so it also exercises the
double-stepping.

### The canonical vector

The textbook check, worth trying first to confirm the machine:

```
Machine:   Enigma I
Rotors:    I II III
Rings:     AAA
Indicator: AAA
Reflector: UKW-B
Plugboard: none

Input AAAAA  ->  Output BDZGO
```

This is pinned in the test suite, so it cannot regress silently.

---

## Things that trip people up

- **No spaces or punctuation.** By design — 26 keys, no space bar. Spaces typed
  into the message field are dropped rather than turned into a stray character.
- **The indicator advances as you type.** The letters in the windows change with
  every keystroke; that is the machine working, not a bug. The *starting*
  position is what the key sheet records.
- **Ringstellung is not the Grundstellung.** Rings are configuration (fixed);
  the Grundstellung is position (live). Setting these to the same value by
  mistake is the most common way to get wrong-but-plausible output.
- **Changing a setting restarts the machine.** Rotors, rings, reflector and
  plugboard are refitted from the Grundstellung when you change any of them.
  On the real machine you could not swap a rotor mid-message, and silently
  carrying on from a half-turned position is how a correct message fails to
  decode at the other end.
- **The M4's fourth rotor never steps.** It is not the fast rotor; it is a
  stationary thin rotor at the left, which is what made the naval key space
  genuinely different.
- **Enciphering a message twice with the same indicator gives the same result.**
  Real practice was to pick a fresh indicator per message.

---

## Building and running

```
export ANDROID_HOME=$HOME/android-sdk
export GRADLE_USER_HOME=$HOME/.gradle-local   # keep off network mounts
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Set `GRADLE_USER_HOME` to local disk. A Gradle home on a network mount causes
the Android Gradle Plugin to time out while generating Kotlin DSL accessors.

### Tests

```
./gradlew :app:testDebugUnitTest
```

---

## Requirements

| | |
|---|---|
| Android | 8.0 (API 26) or newer |
| Built with | Kotlin 2.1.0, AGP 8.9.1, Gradle 8.11.1, compileSdk 34 |
| Version | 1.0.0 |
