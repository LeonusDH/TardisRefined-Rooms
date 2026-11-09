# TARDIS Interface

**English** · [Русский](interface.ru.md) · [Contents](README.md)

The TARDIS Interface moves resources between linked interfaces: between rooms, or between the interior and the outside next to the TARDIS shell.

## Placing

- Inside a TARDIS, an interface can be placed anywhere.
- Outside, only right next to the TARDIS shell: within one block of it, or one block lower for a two-block-high shell. It then belongs to that TARDIS. Next to the shells of two different TARDISes it doesn't work (network conflict).
- An interface works only while its chunk is loaded.

## Linking

Interfaces of the same TARDIS with the same ID are linked, like P2P tunnels in Applied Energistics 2. An ID is four hexadecimal digits, such as `0A29`.

- **Generate ID** in the interface's menu gives it a new ID that no other interface of this TARDIS has.
- An ID belongs to the TARDIS that generated it. An interface with another TARDIS's ID doesn't connect.
- The interface's status shows in its menu and in Jade:

| Status | Meaning |
|---|---|
| Connected | linked to the other interfaces with this ID |
| No ID | nothing to link with yet |
| Disconnected: the ID belongs to another TARDIS | the ID was made in another TARDIS |
| Disconnected: no shell nearby | outside the TARDIS, and the shell isn't next to it (for example, the TARDIS has left) |
| Disconnected: network conflict | next to the shells of two or more TARDISes |

## The interface menu

Right-click the interface with the sonic screwdriver, in either screwdriver mode:

- status, ID and the number of linked interfaces;
- **Generate ID**, **Paste the ID from the screwdriver**, **Clear ID**;
- **Direction**: input, output, or input and output;
- what the interface carries, each switched on or off;
- **Side settings**, a 3D view of the interface and its neighbours;
- tips.

## Direction

- **Input** takes in: whatever pipes and machines feed into it goes to the neighbours of linked output interfaces.
- **Output** gives: pipes and machines take from it what the neighbours of linked input interfaces hold.
- **Input and output** does both.

The interface stores nothing and passes everything on at once, so throughput depends on the pipes and machines connected to it. Linked interfaces next to each other don't pass to each other, so nothing loops.

## What it carries

| Kind | Loaders | Notes |
|---|---|---|
| Items | NeoForge, Fabric | |
| Fluids | NeoForge, Fabric | |
| Energy | NeoForge, Fabric | On Fabric, needs Team Reborn Energy, which most Fabric tech mods bundle |
| Redstone | NeoForge, Fabric | see below |
| Chemicals | NeoForge | Mekanism |
| ME network | NeoForge | Applied Energistics 2, see below |

**Redstone.** The strongest signal next to linked input interfaces is given off by linked output interfaces. An interface set to input and output only reads the signal; otherwise it would catch its own signal and latch on forever.

**ME network.** Joins the ME networks next to linked interfaces into one, like a quantum bridge, both ways regardless of direction. An AE2 Memory Card copies an interface's ID and pastes it into another interface.

## Sides

Each side has a mode:

| Mode | What it does |
|---|---|
| Default | the side does nothing by itself; pipes and machines feed and take as usual |
| Insert | the interface puts into the neighbour on this side what linked input interfaces have. Needs output |
| Extract | the interface takes from the neighbour on this side and sends it to linked output interfaces. Needs input |
| Disabled | the side doesn't exist for the neighbour: pipes, ME cables and redstone ignore it, and linked interfaces don't use the block next to it |

Insert and Extract work with plain chests, no pipes needed. They act every 4 ticks: a stack of items, up to 16 buckets of fluid or chemical, or as much energy as is accepted.

To set sides:

- **Side settings** in the menu: left click — next mode, right click — previous, Shift + click — default; drag to rotate; neighbouring blocks can be shown or hidden;
- **a wrench from any mod**: right-click a side to switch it.

## The screwdriver

The sonic screwdriver has two modes, switched with Shift + mouse wheel while holding it:

- **Normal**: the screwdriver works as in Tardis Refined. Right-clicking an interface opens its menu.
- **Setup**: the screwdriver's own Tardis Refined actions are off, and it copies interface IDs:
  - Shift + right-click an interface copies its ID into the screwdriver;
  - Shift + left-click an interface pastes the remembered ID;
  - Shift + right-click in the air forgets it.

Blocks of this addon (growth nodes, Room Control Blocks, seals) respond to the screwdriver in both modes. The mode and the remembered ID show in the screwdriver's tooltip.

## Wrenches

- Right-click switches the mode of the clicked side.
- Shift + right-click picks the interface up straight into your inventory, settings included.

Wrenches are recognised by the `tardis_rooms:wrenches` item tag: the common wrench tags plus the Tech Reborn wrench. A datapack can add more.

## Settings travel with the block

A broken or picked-up interface keeps its settings: ID, direction, kinds and sides. Put it into a crafting grid to reset them.

## Jade

With Jade installed, the tooltip shows the interface's status, ID, sides that aren't Default, and, when connected, the number of linked interfaces, the direction and what it carries. The energy bar shows the energy in the storages next to linked interfaces. The `compat.jade` config option turns this off.
