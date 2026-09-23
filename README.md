<div align="center">

<img alt="Controlify Enhanced" src="assets/fork/controlify-enhanced-banner.png" width="560">

**An unofficial custom build of [Controlify](https://github.com/isXander/Controlify), the controller support mod for Minecraft: Java Edition.**

[![Support isXander on Patreon](https://img.shields.io/badge/Support_isXander_on-Patreon-F96854?style=for-the-badge&logo=patreon&logoColor=white)](https://patreon.com/isxander)

</div>

---

## What's different in this fork

This build adds three options to Controlify's **Global Settings** screen, fixes two annoyances, and includes a small panel for testing. Everything else behaves exactly like the official mod.

**New options**

- [Aim assist](#aim-assist) — controller aim assist for melee and bows, off by default
- [Edit Glyph Positions](#edit-glyph-positions) — move the in-game button guides out of the way
- [Disable Whitelist & Force Analog Movement](#disable-whitelist--force-analog-movement) — analog movement on every server

**Fixes**

- ["New server detected" toast](#new-server-detected-toast) — no longer shown when it doesn't apply
- [Virtual mouse](#virtual-mouse) — no more cursor snapping to the centre after you touch the mouse

**Testing**

- [Dev Functions panel](#dev-functions-panel) — a small panel for triggering things on demand

<p align="center">
  <img alt="Global Settings with the new options and the Dev Functions panel" src="assets/fork/global-settings.jpg" width="900">
  <br>
  <em>The Global Settings screen in this build.</em>
</p>

---

## New options

### Aim assist

Opens a screen of aim assist settings. With it on, the look stick slows down as your crosshair comes onto a mob and pulls gently towards its upper chest, which is where most controller misses come from — overshooting rather than being wildly off. It keeps tracking while you strafe past something, not only while you're turning.

It never widens a hitbox and never changes where an attack lands. Your own aim still decides the outcome.

<p align="center">
  <img alt="The Aim Assist Settings option" src="assets/fork/row-aim-assist.png" width="820">
</p>

**Aim Assist** — off, **Singleplayer & LAN**, or **Everywhere**.

> [!WARNING]
> Only use **Everywhere** on servers you know allow aim assist. Some server anti-cheats may flag or ban you for it. **Singleplayer & LAN** is the default and never touches a multiplayer server.

**Target** — hostile mobs, all mobs, or a custom list. Hostile mobs also covers a normally peaceful mob that's currently angry, like a provoked wolf pack. Players are never targeted.

**Melee** and **Bow** are tuned separately, each with three settings:

- **Strength** — how hard the assist slows and pulls. Low, Medium, High.
- **Crosshair Cone** — how far off a mob can be before the assist takes an interest.
- **Distance** — how far away a mob can be and still be targeted.

Bows and crossbows use the melee settings until you actually start drawing, and switch to the bow settings from then on. A crossbow being reloaded counts as melee; a loaded one ready to fire counts as bow.

The settings are global rather than per-controller.

<p align="center">
  <img alt="The Aim Assist settings screen" src="assets/fork/aim-assist-options.png" width="820">
  <br>
  <em>Melee and bow are tuned independently.</em>
</p>

### Edit Glyph Positions

Opens an editor for moving the left and right in-game button guide columns separately, in small pixel steps. Type exact offsets, snap either side to a screen corner, or reset it. Handy for keeping the guides clear of other HUD elements, like beacon effect icons.

Requires a connected controller.

<p align="center">
  <img alt="The Edit Glyph Positions option" src="assets/fork/row-edit-glyphs.png" width="820">
  <br>
  <img alt="Its tooltip" src="assets/fork/tooltip-glyph.png" width="560">
</p>

<p align="center">
  <img alt="The Edit Glyph Positions editor" src="assets/fork/glyph-editor.jpg" width="820">
  <br>
  <em>The editor. Nudge a side, type an exact offset, or snap it to a corner.</em>
</p>

<p align="center">
  <img alt="In-game button guides moved away from other HUD elements" src="assets/fork/glyphs-moved-in-game.jpg" width="820">
  <br>
  <em>Both guide columns nudged clear of the map and the beacon powers.</em>
</p>

### Disable Whitelist & Force Analog Movement

Turns on analog movement — walking speed follows how far you tilt the stick — on **every** server, instead of only the ones in the Analogue Movement Whitelist. While it's on, the whitelist is greyed out and ignored, and the "New server detected" toast never appears.

> [!WARNING]
> Some server anti-cheats may flag or ban you for using analog movement. Only turn this on if you're sure every server you play on allows it, and check it *before* joining a new server.

<p align="center">
  <img alt="The Disable Whitelist & Force Analog Movement option" src="assets/fork/row-force-analog.png" width="820">
  <br>
  <img alt="Its tooltip" src="assets/fork/tooltip-whitelist.png" width="560">
</p>

---

## Fixes

### "New server detected" toast

Controlify already allows analog movement on Realms, but the official version still shows the "New server detected" toast, which says keyboard-like movement is on, even though it isn't. Whitelisting the Realm doesn't stop it: a Realm gets a new address every time it closes and reopens, so the entry you saved is dead by your next session and the toast is back.

This build only shows the toast when keyboard-like movement is actually in use, so it no longer appears on Realms or on servers already in your whitelist.

<p align="center">
  <img alt="The New server detected toast" src="assets/fork/new-server-toast.png" width="480">
</p>

### Virtual mouse

In the official build, touching the mouse while the inventory (or another screen with the virtual mouse) was open left the controller in a bad state: going back to it snapped the virtual cursor to the centre of the screen and made it jitter, repeatedly showed the "Controller disabled" toast, and stopped B from closing the screen.

The virtual mouse now picks up where your real mouse was, and switching between mouse and controller works normally. This bug is suspected to affect the official 26.3 release too.

---

## Testing

### Dev Functions panel

A dev panel in Global Settings for faster testing and bug checking, so behaviour can be triggered on demand instead of waiting for it in game. Right now it fires the "New server detected" toast, reports what aim assist is doing at that moment, and reports the active movement type. The checkbox below hides the panel.

<p align="center">
  <img alt="The Dev Functions panel" src="assets/fork/dev-functions-panel.png" width="420">
</p>

---

## Install

1. Download the latest `-universal.jar` from [Releases](https://github.com/DCDCDC1090/Controlify-Enhanced/releases/latest). One jar covers both Fabric and NeoForge.
2. Remove the official Controlify from your `mods` folder — running both at once will not work.
3. Drop this jar in alongside [YetAnotherConfigLib](https://modrinth.com/mod/yacl).

Built for Minecraft **26.3**, with Fabric Loader 0.19 or newer, or NeoForge.

## Building it yourself

```sh
git clone https://github.com/DCDCDC1090/Controlify-Enhanced.git
cd Controlify-Enhanced
./gradlew ":26.3:build"
```

The jars land in `versions/26.3/build/libs/`. JDK 25 is required.

## Issues

Problems with **this build** belong in [this repository's issue tracker](https://github.com/DCDCDC1090/Controlify-Enhanced/issues), which is where the in-game Issue Tracker button and the Mod Menu links point. Please don't report them to isXander.

Problems you can also reproduce on the official mod belong in [Controlify's issue tracker](https://github.com/isXander/Controlify/issues).

## Credits

Controlify is made by [isXander](https://github.com/isXander) and its contributors, and is licensed under [LGPL-3.0](LICENSE), as is this fork. The [Controlify Wiki](https://controlify.isxander.dev) covers how to use and configure the mod; everything there applies to this build too.

If you get use out of Controlify, [support isXander on Patreon](https://patreon.com/isxander).
