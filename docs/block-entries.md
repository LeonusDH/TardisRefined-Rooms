# Block entries

**English** · [Русский](block-entries.ru.md) · [Contents](README.md)

Two places name blocks that go with a room whatever is in them, so they never keep it from being removed or replaced:

- the file `config/tardis_rooms/removable_with_room.json`, for every room ([Configuration](configuration.md#blocks-that-go-with-a-room));
- the `removable` field of a datapack room, for that room only ([Rooms from datapacks](datapack-rooms.md#removable-blocks)).

Both take the same strings:

| Entry | Matches |
|---|---|
| `minecraft:lectern` | that block |
| `#minecraft:banners` | blocks in the tag |
| `mekanism:*_cable`, `*:*_sign` | block ids where `*` stands for any text |
| `@mekanism` | every block of the mod (alternatively: `mekanism:*`) |
| `minecraft:player_head{profile:{name:"MHF_Question"}}` | a block with data, as in `/execute if block` |
| `#minecraft:candles[lit=false]` | a block or tag with block states, as in `/execute if block` |

## Details

- **Namespace.** An id without a namespace means `minecraft`: `lectern` is `minecraft:lectern`, `*_bed` is `minecraft:*_bed`.
- **`*`** stands for any text, including none, and can appear several times: `*:*_sign` is every sign of every mod. Use `*` for groups that have no tag, for example all cables of a mod.
- **`@mod`** is the same as `mod:*`.
- **Block states** go in square brackets: `minecraft:chest[type=single]`. A state that isn't listed can be anything.
- **Data** goes in braces, written as SNBT, as in commands. It matches if the block's data contains the given fields; other fields can be anything. Lists match if every listed element is found in the block's list: `minecraft:chest{Items:[{id:"minecraft:diamond"}]}` is a chest with diamonds in it, whatever else it holds.
- States and data can be combined with a tag: `#minecraft:banners{patterns:[{pattern:"minecraft:creeper"}]}`.

## Mistakes

- In a datapack, a wrongly written entry is reported in the server log when datapacks load, as `Datapack room <id>: removable "...": <reason>`, and the room is skipped. Tags aren't loaded yet at that moment, so a tag that doesn't exist is reported later, the first time the room is checked: `Room <n> (<id>): removable: skipping ...`.
- In `removable_with_room.json`, entries are checked the first time they are needed, once tags are loaded. A wrong entry is skipped with a warning in the log, `tardis_rooms/removable_with_room.json: skipping ...`; the rest still works. If the file itself can't be read (broken JSON), the error goes to the log and the default list is used until the file is fixed.

## Be careful

Anything in a matched block is lost together with the room. Don't name blocks where players keep things. With `@mod` and `*` be especially careful: in tech mods almost every block holds items, fluids or energy. The TARDIS Interface always keeps a room from being removed, whatever the entries say.
