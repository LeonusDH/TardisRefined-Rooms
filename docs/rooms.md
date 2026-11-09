# Rooms

**English** · [Русский](rooms.ru.md) · [Contents](README.md)

## The grid

The interior sits on a fixed grid of modules, 24×24 blocks each, across five floors: the hub floor, three below it and one above. Every room or corridor takes one or more whole modules on one or two neighbouring floors. Rooms meet through passages in the middle of their walls.

## Growing

- **Room Growth Nodes** sit in the passages. Right-click one with the sonic screwdriver to open the catalogue of rooms and corridors.
- The catalogue shows a top-down plan of each option. Options that don't fit are marked, with the reason in the tooltip: the space behind the wall is taken, the option doesn't fit the Tardis Refined grid, or (when replacing) it would cut other rooms off.
- The **floor map** shows what's already taken: built rooms, the hub, zones where nothing can be built, and where the new space would go.
- Pick an option and the TARDIS grows it behind the wall. Growing is free.
- Where a passage of the new room leads nowhere, a **Passage Seal** closes it. When the space behind it is free, the seal becomes a new growth node.

## The Room Control Block

Every room has a **Room Control Block** in its floor. Right-click it with the screwdriver to manage the room:

- **Rename**: up to 32 characters. An empty name brings back the room type.
- **Replace…**: grow another option in the same place. The old room is removed first, under the same rules as removal below, except that **Remove all** isn't offered.
- **Remove**: the room becomes TARDIS stone again. Players, mobs and dropped items inside are moved out to a passage; item frames and paintings drop as items.
- **What's in the way?**: shown when things inside keep the room from being removed, see below.
- **Map** opens the floor map.

If a datapack room's datapack is removed, the room stays in place. It shows up as "Unknown room" and can still be replaced or removed.

## What keeps a room from being removed

A room can't be removed or replaced if:

- **other rooms connect through it** and would be cut off from the hub;
- **your things are inside**: containers or machines you placed or filled.

### Which blocks count as your things

Only blocks with stored data count (chests, furnaces, machines and so on); plain blocks never do. Blocks of Tardis Refined and of this addon never count, except the **TARDIS Interface**, which always counts: it is placed by a player and holds its settings.

When a room grows, the addon remembers every such block that came with the room, together with its data at that moment. Later:

- a block that came with the room and hasn't been touched doesn't count, for example an empty lectern in a datapack room;
- it counts as soon as it changes: an item in a chest, a book on a lectern;
- it counts if it was replaced by another block;
- anything placed after the room grew counts, even if empty.

A block that changes on its own, such as a beacon, may count as touched after a while. The page of a lectern and a hopper's cooldown are ignored.

Rooms grown with an addon version before 1.1.0 have nothing remembered. For them:

- a vanilla block counts only if something is inside: items in a container (chests, barrels, shulker boxes, furnaces, hoppers, brewing stands, chiseled bookshelves, decorated pots, jukeboxes and so on), a book on a lectern, food on a campfire, bees in a hive. A chest with unopened loot counts too;
- a block from another mod always counts, since its contents can't be checked.

### Blocks that never count

- Blocks listed in the server config option `removableWithRoom`. By default: signs, banners and heads.
- Blocks a datapack room marks in its `removable` field.

Both go with the room whatever is in them. See [Configuration](configuration.md) and [Rooms from datapacks](datapack-rooms.md#removable-blocks).

## Finding what's in the way

When removal or replacement is refused because of things inside, the addon:

- writes in chat which block is in the way and where, the one nearest to you, and how many more there are;
- outlines up to 32 of them, nearest first, with blinking red frames drawn through walls, for 15 seconds.

A double chest or a bed is one object: one frame around both halves, counted once. The frames follow the world while they are shown: a broken block loses its frame, and a double chest that became single shrinks to one block.

Click the chat message to outline them again; it works up to 64 blocks from the Room Control Block. **What's in the way?** in the Room Control Block's menu does the same.

## Removing a room together with what's in it

When only things inside keep a room from being removed, **Remove** opens a confirmation window instead:

- how many things are in the way, and of which kinds;
- a warning that they and everything in them will be lost;
- **Remove all**, which unlocks after a 10-second countdown.

The things in the way are outlined while the window is open. The server checks the countdown too: removal together with things inside is accepted only for the room whose window was opened, between 10 seconds and 5 minutes after opening it, and only once. Closing the window, by Cancel, Escape or anything else, starts the countdown over next time.

Contents of removed blocks are not dropped: they vanish with the room. TARDIS Interfaces removed with the room unlink at once, and redstone carried through them goes off.

## The hub

The Tardis Refined hub is simplified once per TARDIS, when a player first enters it: only the airlock centre, the Eye of Harmony viewing chamber and two short passages to the first growth nodes stay, and the rest becomes buildable space. If something already stands in the hub (chests, machines, item frames) or the hub doesn't match the Tardis Refined templates, it is left as it is. The `simplifyHub` config option turns this off.
