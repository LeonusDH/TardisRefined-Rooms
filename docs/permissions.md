# Permissions

**English** · [Русский](permissions.ru.md) · [Contents](README.md)

The addon has no permission system of its own. Without [AcuaTARDISGadgets](https://modrinth.com/mod/acuatardisgadgets), anyone who may build in a TARDIS may also grow rooms and set up interfaces there.

## With AcuaTARDISGadgets

AcuaTARDISGadgets 1.20.1-1.0.6 or newer, on Forge or Fabric. Its **Security Manager** lists the permissions of a TARDIS, and the addon adds three of them to that list (Tardis Refined TARDISes only):

| Permission | What it allows |
|---|---|
| **Grow Rooms** | Growing a new room or corridor from a Room Growth Node |
| **Manage Rooms** | Replacing, removing (including **Remove all**) and renaming a room |
| **Set Up Interfaces** | Placing and breaking a TARDIS Interface, changing anything in its menu, picking it up or turning its sides with a wrench, pasting an ID with the screwdriver or an AE2 Memory Card |

- For an interface outside, next to a shell, the permission is checked in the TARDIS of that shell.
- A player without the permission sees a message above the hotbar, and nothing changes. With `debug = true` the refusal also goes to the log.
- Looking isn't restricted: opening the catalogue, the floor map, the room or interface menu, outlining what's in the way, copying an interface ID.

### When the permissions start to apply

AcuaTARDISGadgets keeps a TARDIS open to everyone until security is turned on in it: that happens when a player first uses a Security Manager there, and that player gets every permission, the addon's included. After that, others get only the permissions given to them in the Security Manager.

If security was turned on in a TARDIS **before** the addon was installed, nobody there has the addon's permissions yet, its owner included. A player who may change permissions (the one who turned security on has that permission) can grant them in the Security Manager, to themselves as well.

## Turning it off

`compat.acuaTardisGadgets = false` in `config/tardis_rooms/common.toml` (see [Configuration](configuration.md)). Then the addon doesn't add its permissions to AcuaTARDISGadgets at all, and everyone may build as without that mod. The option is read once when the game starts: restart the game (or the server) after changing it.
