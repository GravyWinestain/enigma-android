# Enigma (Android)

An Android port of the Enigma I, M3 and M4 cipher machines, with a front panel
modelled on the real thing.

Key sheets are compatible with the desktop build at
[enigma-rs](https://github.com/GravyWinestain/enigma-rs), so a key printed on
one machine can be pasted into the other.

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
`U`, `O` and `P` are all carriers, so converting an arbitrary stretch of text
back to digits will corrupt the words around it. The operator knows which part
of a message is numeric — from the message form or the codebook — and converts
only that field. The app does the same.

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

36 tests covering the canonical `AAAAA` -> `BDZGO` vector, the double-step, the
reflector involution, key-sheet round trips, and the digit convention.

Test expectations are cross-checked against an independent Python
implementation of the same wiring, so an assertion cannot quietly drift into
agreeing with whatever the Kotlin code happens to do.

## Building

    export ANDROID_HOME=$HOME/android-sdk
    export GRADLE_USER_HOME=$HOME/.gradle-local   # keep off network mounts
    ./gradlew :app:assembleDebug

Set `GRADLE_USER_HOME` to local disk. A Gradle home on a network mount causes
the Android Gradle Plugin to time out while generating Kotlin DSL accessors.

## Licence

Not affiliated with or endorsed by anyone. Built for study and simulation. It
is a museum piece, not a secure cipher.
