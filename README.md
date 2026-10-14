# Enigma (Android)

An Android port of the Enigma I, M3 and M4 cipher machines, with a front panel
modelled on the real thing.

Key sheets are compatible with the desktop build at
[enigma-rs](https://github.com/GravyWinestain/enigma-rs), so a key printed on
one machine can be pasted into the other. A sheet looks like this:

    M3 naval | ROTORS IV II V | RINGS BDF | INDICATOR XWB | REFLECTOR UKW-B | PLUG AM FI NV

**Using it?** See [docs/USER_GUIDE.md](docs/USER_GUIDE.md).

---

## What is modelled

- **Enigma I** (three-rotor Wehrmacht/Luftwaffe), **M3** (three-rotor
  Kriegsmarine) and **M4** (four-rotor Kriegsmarine with the thin Beta or Gamma
  fourth rotor).
- Rotors I–VIII, plus the thin Beta and Gamma.
- Reflectors UKW-A/B/C and the naval UKW-B/C Thin.
- **Double-stepping**, which is the behaviour most simulations get wrong: when
  the middle rotor sits at its notch it steps on the next keypress *and* drags
  the left rotor with it.
- **Ringsstellung** kept distinct from **Grundstellung**. Confusing the ring
  setting with the rotor position is the most common reason a "correct"
  message fails to decode, so the panel gives them separate controls.
- A plugboard of up to thirteen leads.

## Numbers

The machine has 26 keys and no digits, so a digit has to travel as a letter.
Digits 0–9 are carried by `QWERTZUIOP`.

This is a convention, not a cipher-breaking trick, and it has one real
limitation: the carrier letters also occur in ordinary words. `E`, `R`, `T`,
`U`, `O` and `P` are all carriers, so converting an arbitrary stretch of
recovered text back to digits will corrupt the words around it. The operator
knows which part of a message is numeric — from the message form or the
codebook — and converts only that field. The app never converts recovered text
back to digits on its own, for that reason.

## Panel layout

The screen follows the physical machine from top to bottom, because that order
is what makes an Enigma read as an Enigma:

    rotor windows   ->  lampboard  ->  machine settings
    ->  Ringstellung  ->  Grundstellung  ->  plugboard
    ->  message form  ->  keyboard  ->  key sheet

Details kept from the real machine: the keyboard's 7-6-7 stagger with the
reversed bottom row (`PYXCVBNML`), the lampboard printed in reflector order,
and rotor slots that only offer rotors that physically fit the selected model.

## Tests

    ./gradlew :app:testDebugUnitTest

36 tests across three suites:

| Suite | Tests | Covers |
|---|---|---|
| `CipherTest` | 17 | Canonical vector, double-step, reflector involution, no self-encryption, rotor bijection, ring vs. indicator independence, thin rotor, key-sheet round trips, every M4 rotor combination |
| `DigitsTest` | 10 | Digit carriers, the ambiguity limitation, five-letter grouping, long numeric messages |
| `PanelTest` | 9 | The screen's own defaults and state behaviour |

Test expectations are cross-checked against an independent Python
implementation of the same wiring, so an assertion cannot quietly drift into
agreeing with whatever the Kotlin code happens to do.

The canonical vector, pinned so it cannot regress:

```
Machine:   Enigma I
Rotors:    I II III
Rings:     AAA
Indicator: AAA
Reflector: UKW-B
Plugboard: none

AAAAA  ->  BDZGO
```

## Building

    export ANDROID_HOME=$HOME/android-sdk
    export GRADLE_USER_HOME=$HOME/.gradle-local   # keep off network mounts
    ./gradlew :app:assembleDebug

Set `GRADLE_USER_HOME` to local disk. A Gradle home on a network mount causes
the Android Gradle Plugin to time out while generating Kotlin DSL accessors.

| | |
|---|---|
| Android | 8.0 (API 26) or newer |
| Built with | Kotlin 2.1.0, AGP 8.9.1, Gradle 8.11.1, compileSdk 34 |
| Version | 1.0.0 |

## Project layout

    core/    Rotors.kt      rotor wiring and notch tables
             Enigma.kt      stepping, enciphering, plugboard
             KeySheet.kt    key parse and print, interoperable with enigma-rs
             Digits.kt      the digit carrier convention
    ui/      EnigmaScreen.kt  the whole machine and its state
             Panel.kt         rotor windows, lampboard, plugboard, keyboard
             Controls.kt      setting controls, message fields
             theme/Theme.kt   the olive/brass/cream palette

## Licence

MIT. See [LICENSE](LICENSE).

Not affiliated with or endorsed by anyone. Built for study and simulation. It
is a museum piece, not a secure cipher.
