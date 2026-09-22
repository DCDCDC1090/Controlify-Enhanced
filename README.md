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

## What is Controlify?

Controlify is the best controller support mod for Minecraft: Java Edition. It exceeds the first-party Bedrock Edition controller support in every way possible. It is feature-complete, with support for vibration, gyroscope, HD haptics, and more.

Controlify supports *all* controllers, thanks to its usage of the [SDL3](https://libsdl.org/) library, which is the most advanced cross-platform input library available. 

Controlify is designed to be both user-friendly and feature-rich. It has sensible defaults, with default sensitivity matched to Bedrock Edition for easy transition, and a simple yet informative settings screen that allows you to tweak your experience to your liking.

## Feature overview

- **Vibration & DualSense HD haptics**, with per-event intensity and **adaptive trigger** effects.
- **Gyro aiming**, with optional flick stick.
- **Full GUI navigation** of every menu, including inventories and modded screens, with cursor snapping.
- **Works with all controllers** via SDL3, including PlayStation controllers without extra software, **Steam Deck**, flight sticks and racing wheels.
- **Vendor-specific inputs** like paddles, mute buttons and touchpads on Xbox, DualSense and Steam Deck.
- **Controller-specific button glyphs**, detected automatically.
- **On-screen keyboard** and a configurable **radial menu**.
- **Data-driven**: resource packs can change bindings, glyphs, button guides, keyboard layouts and controller models.
- **Mod compatibility** with Sodium, Iris, Simple Voice Chat, Do A Barrel Roll and more.
- **Fabric and NeoForge**, Minecraft 1.21.1 and above. Snapshot builds are available to isXander's Patreon members.

## Features

### Controller vibration
Vibration for events like taking damage, something not even Bedrock on Windows has. Each source's intensity can be adjusted.

<img alt="Vibration settings" src="https://cdn.modrinth.com/data/DOUdJVEm/images/8a7809d07d9e1d9e3002007d7e5e13b73ce8fb5b.png" width="360">

### Radial menu
Put less-used actions, including any modded keybind, on a customizable radial menu to free up buttons.

<img alt="Radial menu" src="https://cdn.modrinth.com/data/DOUdJVEm/images/e56d9be363b2b31440e16018cc01f197848b7ac6.webp" width="480">

### Gyro support
Use your controller's gyroscope for fine aiming, optionally combined with [flick stick](https://www.reddit.com/r/gamedev/comments/bw5xct/flick_stick_is_a_new_way_to_control_3d_games_with/).

### Container cursor
A Bedrock-style inventory cursor with cursor snapping and dedicated buttons for quick move, dropping and more.

<img alt="Container cursor" src="https://cdn.modrinth.com/data/DOUdJVEm/images/249a2cbaea9b374b33fe67717380e732693dd37a.png" width="480">

### Controller identification & joysticks
Your controller's make and model is detected automatically to show matching button glyphs. Resource packs can add new styles and controllers. Any joystick can be mapped with your own names and textures, with unlimited inputs.

### Button guide
An in-game overlay shows which buttons you can press right now, and menus show button hints on elements with controller shortcuts.

<img alt="In-game button guide" src="https://cdn.modrinth.com/data/DOUdJVEm/images/57c41cee14680c74faf947c5cff355c0af4c35b3.png" width="480">
<img alt="In-screen button guide" src="https://cdn.modrinth.com/data/DOUdJVEm/images/511e4182137bb27bbdf95539c8265b9af2038761.webp" width="480">

### Built for mod compatibility
Each controller has its own settings and bindings, and a simple API lets other mods add controller support for their own screens.

<img alt="Do A Barrel Roll with a flightstick" src="https://cdn.modrinth.com/data/DOUdJVEm/images/8ee5ec167bc5f8be96da725b10707094559138cb.gif" width="480">

<sub><i>Do A Barrel Roll with a Thrustmaster HOTAS flightstick</i></sub>

### Automatic deadzone calibration
Your controller's deadzones are calibrated automatically.

<img alt="Calibration screen" src="https://cdn.modrinth.com/data/DOUdJVEm/images/f5f8e2a0a05e61adb95dd919760b424165ca5d14.png" width="360">

## Supported versions
Controlify requires Minecraft **1.19.4** or newer, because that's when Mojang added the keyboard navigation it builds on. It is actively supported for **1.21.1 and above** on Fabric and NeoForge.
