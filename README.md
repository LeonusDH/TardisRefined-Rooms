# TARDIS Refined: Rooms

**English** · [Русский](README.ru.md)

An addon for [Tardis Refined](https://github.com/Whocraft/TardisRefined) on Minecraft 1.21.1 (NeoForge) that replaces its endless procedural ARS corridors with an interior you build yourself, room by room, the way the classic 1.7.10 TARDIS mod worked.

## Features

### Growing the interior

- The interior sits on a fixed grid of modules across five floors: the hub floor, three below it and one above.
- **Room Growth Nodes** sit in the passages. Right-click one with the sonic screwdriver to open a catalogue of rooms and corridors. Pick one and the TARDIS grows it behind the wall. Growing is free.
- The catalogue shows a top-down plan of each option and a **floor map** of what's already taken: built rooms, the hub, zones where nothing can be built, and where the new space would go.
- Every room has a **Room Control Block**. Use the screwdriver on it to replace or remove the room. A room is never removed while it still has containers or machines inside, or if other rooms connect through it.
- Where a passage leads nowhere, a **Passage Seal** closes it. When a neighbouring space is free, the seal becomes a new growth node.

### TARDIS Interface

A block that moves resources between linked interfaces, either between rooms or between the interior and the outside, next to the TARDIS shell.

- Give interfaces the same ID to link them. The screwdriver can copy an ID from one interface and paste it into another.
- Each interface has a direction (input or output) and a choice of what it passes: items, fluids, energy, redstone, Mekanism chemicals and AE2 ME networks.
- The interface stores nothing. It passes everything on at once, so throughput depends on the pipes and machines connected to it.
- Each side can be set to **Default**, **Insert** or **Extract**, either in a 3D side screen or with a wrench from any mod. Insert and Extract sides work with plain chests, no pipes needed.
- Shift + right-click with a wrench picks the interface up straight into your inventory, settings included.

### Other changes to Tardis Refined

- Procedural ARS rooms and corridors are switched off, including the random corridors above the hub.
- The TR hub is simplified: only the airlock centre, the Eye of Harmony viewing chamber and two short passages to the first growth nodes stay. The rest becomes buildable space. This happens only in a hub nobody has built in yet.
- Internal doors and tall TARDIS shells get an invisible top half. You can aim at and click the upper part of the door, and nothing can be placed into it.

## Requirements

| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.235 or newer |
| Tardis Refined | 2.1.6 built from [PR #618](https://github.com/Whocraft/TardisRefined/pull/618) |

> [!IMPORTANT]
> The addon relies on changes from Tardis Refined PR #618, which isn't in any public release yet. Until it ships, you need a Tardis Refined build from that PR. See [Building](#building).

Optional integrations. Each one turns on only if that mod is installed, and can be disabled in the config:

- **Mekanism** 10.7+: chemicals through the interface.
- **Applied Energistics 2** 19.2+: ME networks through the interface; a Memory Card can copy and paste the interface ID.
- **Jade** 15.10+: interface status in the tooltip.

## Configuration

`config/tardis_rooms/common.toml`:

| Option | Default | What it does |
|---|---|---|
| `disableProceduralRooms` | `true` | Stop Tardis Refined from generating procedural ARS rooms and corridors |
| `simplifyHub` | `true` | Strip the TR hub down to its essential parts (see above) |
| `debug` | `false` | Log detailed information about what the addon builds, replaces or removes |
| `compat.mekanism` / `compat.ae2` / `compat.jade` | `true` | Turn individual integrations on or off |

The client config sets the Factory console's door lever angle.

## Building

You need JDK 21. Mekanism, AE2 and Jade come from Maven automatically. Tardis Refined from PR #618 has to be built locally:

1. Build Tardis Refined from the PR:
   ```sh
   git clone https://github.com/Whocraft/TardisRefined.git
   cd TardisRefined
   git fetch origin pull/618/head
   git checkout FETCH_HEAD
   ./gradlew :forge:downloadAssets   # first time only, together with the next line
   ./gradlew :forge:runData          # generates models, recipes and translations; close it once it reports the files are written
   ./gradlew :forge:build
   ```
2. Copy `forge/build/libs/forge-tardis_refined-mc1.21.1-v2.1.6.jar` into this project's `libs/` folder as `tardis_refined-neoforge-1.21.1-2.1.6-pr618.jar`. To keep the original name, change `tr_neoforge_jar` in `gradle.properties` instead.
3. Build the addon:
   ```sh
   ./gradlew build
   ```
   The jar ends up in `neoforge/build/libs/`. On Windows, `build.bat` does the same: it finds a JDK 21 and writes the output to `build.log`.

> [!NOTE]
> Java treats `!` in a path as a separator inside jar paths, so if the project folder has `!` in its path, the game (data generation, test runs) fails to start. `build.bat` works around it by mapping the folder to a temporary drive letter; when running Gradle by hand, use a path without `!`.

English translations are generated from `TRoomsText` with `./gradlew :neoforge:runData`. Other languages are plain files in `common/src/main/resources/assets/tardis_rooms/lang/`.

## Contributing

Bug reports, ideas and pull requests are welcome.

## AI assistance

This addon is developed with the help of AI.

## License

[PolyForm Noncommercial 1.0.0](LICENSE.md): you may use, change and share the code, and send pull requests, but not for commercial purposes.

Tardis Refined itself is a separate project by Whocraft under its own license.
