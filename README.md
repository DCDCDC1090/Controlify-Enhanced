<div align="center">

<p><img alt="Controlify Enhanced" src="assets/fork/controlify-enhanced-banner.png" width="512"></p>

[![Support isXander on Patreon](https://img.shields.io/badge/Support_isXander_on-Patreon-F96854?style=for-the-badge&logo=patreon&logoColor=white)](https://patreon.com/isxander)

A mod that adds the best **controller support** for Minecraft: Java Edition.

</div>

## What's different in this fork

This is an unofficial custom build of isXander's Controlify. It adds new options and a **Dev Functions** panel to Controlify's **Global Settings** screen, fixes a virtual mouse bug that is suspected to also affect the official 26.3 release, and removes the "New server detected" toast on Realms. Everything else works the same as the official mod.

<img alt="Global Settings screen with the new options and the Dev Functions panel" src="assets/fork/global-settings.jpg" width="700">

### Disable Whitelist & Force Analog Movement

<img alt="Disable Whitelist & Force Analog Movement option" src="assets/fork/row-force-analog.png" width="600">

Turns on analog movement (walking speed follows how far you tilt the stick) on every server, instead of only on servers in the Analogue Movement Whitelist. While it's on, the whitelist is greyed out and ignored, and the "New server detected" toast no longer appears.

> **Warning:** some server anti-cheats may flag or ban you for using analog movement. Only turn this on if you're sure every server you play on allows it, and check it *before* joining a new server.

<img alt="Force analog movement tooltip" src="assets/fork/tooltip-whitelist.png" width="450">

### Edit Glyph Positions

<img alt="Edit Glyph Positions option" src="assets/fork/row-edit-glyphs.png" width="600">

Opens an editor for moving the left and right in-game button guide columns separately, in small pixel steps. You can type exact offsets, snap each side to a screen corner, or reset it. Handy for moving the guides out of the way of other HUD elements, like beacon effect icons. Requires a connected controller.

<img alt="Edit Glyph Positions tooltip" src="assets/fork/tooltip-glyph.png" width="450">

<img alt="In-game button guides moved away from other HUD elements" src="assets/fork/glyphs-moved-in-game.jpg" width="700">

### Dev Functions panel

<img alt="Dev Functions panel" src="assets/fork/dev-functions-panel.png" width="420">

A panel on the right-hand side of Global Settings with test buttons:

- **Show "New server detected" Toast** pops up that toast exactly as it appears in game.
- **Check Current Movement Type** shows a toast telling you whether analog movement or keyboard-like movement (full speed only, like WASD) is active right now.

The **Dev Functions** checkbox below the panel hides it. While hidden, its buttons can't be clicked, and your choice is remembered.

### Virtual mouse fix

Fixes a bug that is suspected to also affect the official 26.3 release: after touching the mouse while the inventory (or another screen with the virtual mouse) was open, going back to the controller made the virtual cursor snap to the center of the screen and jitter, repeatedly showed the "Controller disabled" toast, and stopped B from closing the screen. The virtual mouse now picks up where your real mouse was, and switching between mouse and controller works normally.

### No more "New server detected" toast on Realms

Controlify already allows analog movement on Realms, but the official version still showed the "New server detected" toast (which says keyboard-like movement is on) the first time you joined one. This build only shows that toast when keyboard-like movement is actually in use, so it no longer appears on Realms or on servers already in your whitelist.

<img alt="The New server detected toast" src="assets/fork/new-server-toast.png" width="460">

## [Wiki](https://moddedmc.wiki/project/controlify)

Read up on the [Controlify Wiki](https://controlify.isxander.dev) for more information on how to use Controlify, how to configure it, and how to develop for it.

## Supported versions
Controlify requires Minecraft **1.19.4** or newer, because that's when Mojang added the keyboard navigation it builds on. It is actively supported for **1.21.1 and above** on Fabric and NeoForge.
