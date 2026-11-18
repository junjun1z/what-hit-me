# Commands

All commands start with `/whm` (alias `/whathitme`) and Tab-complete.

**Op** means permission level 2 on a server, or single player with cheats enabled. Single player without cheats is **not** op.

## Overview

| Command | What it does | Who |
|---|---|---|
| `/whm` or `/whm history` | Recent damage taken | Everyone |
| `/whm last` | Full recap of your last death | Everyone |
| `/whm snapshot [n]` | Saved totem / heavy-hit snapshots (1 = newest) | Everyone |
| `/whm effects` | Active effects, mod, milk curability | Everyone |
| `/whm stats` | Deaths per cause, total deaths, damage taken | Everyone |
| `/whm search <text\|#tag\|@modid>` | Search damage types, mobs and effects | Everyone |
| `/whm togglechat` | Respawn chat summary on / off | Everyone |
| `/whm ignore <id>` | Hide / show a damage id | Everyone (personal); op for the server list |
| `/whm config` | Open the settings screen (players without the mod get a list) | Everyone |
| `/whm config <key> [value]` | Show or change a setting | Personal keys: everyone; common keys: viewing everyone, changing op |
| `/whm rename <id> <name...>` | Custom name (empty removes it) | Op |
| `/whm sethint <id> <tip...>` | Custom tip (empty removes it) | Op |
| `/whm inspect <player>` | Last death and history of an online player | Op |
| `/whm test <damage_id> [entity] [item] [dim] [--as <player>] [--assume <condition>]...` | Preview name, tip, rules, death triggers, death message and chat | Op |
| `/whm dump` | Write `whathitme_dump.csv` and `hint_template.cfg` | Op |
| `/whm reload` | Reload config files and pack folder, rescan the catalog | Op |
| `/whm template` <Badge type="tip" text="1.3.0+" /> | Open the template editor | Everyone (modded client) |
| `/whm template preview <output>` <Badge type="tip" text="1.3.0+" /> | Render a template with your latest death | Everyone |
| `/whm template reset [output]` <Badge type="tip" text="1.3.0+" /> | Delete your personal templates | Everyone (modded client) |
| `/whm pack` <Badge type="tip" text="1.3.0+" /> | Open the [pack editor](/modpack/pack-editor) | Op (modded client, server with the mod) |
| `/whm pack personal` <Badge type="tip" text="1.3.0+" /> | Pack editor in personal mode (your own lists) | Everyone (modded client) |
| `/whm server <subcommand>` | From a modded client: run it on the server | Needs the mod on the server |
| `/whm help` | Command list | Everyone |

`/whm copy <text>` and `/whm copyrecap` exist on modded clients; the `[Copy ID]` and `[Copy Recap]` buttons use them.

## Details and examples

### `/whm ignore <id>`

Toggles a damage id. Run it again to show the id again.

```
/whm ignore minecraft:cactus
/whm ignore mob:minecraft:*
```

- Modded client: your personal `ignoredDamageIds` in `whathitme-client.cfg`.
- On the server (players without the mod, console): op changes the server list in `whathitme-common.cfg`; non-ops change their personal list stored on the server.
- The `[Mute]` button does the same.

### `/whm config <key> [value]`

```
/whm config showInternalDamageId false
/whm config anchorPoint TOP_RIGHT
/whm config ignoredDamageIds minecraft:cactus, minecraft:sweetBerryBush
```

Booleans accept `true`, `false`, `on`, `off`, `yes`, `no` and `toggle`. Lists are comma-separated and replace the whole list. Keys in [`lockedFeatures`](/modpack/locked-features) are refused for everyone.

### `/whm rename` and `/whm sethint`

```
/whm rename minecraft:outOfWorld &5The Abyss
/whm sethint mob:minecraft:creeper Creepers hiss before exploding - sprint away!
/whm rename minecraft:outOfWorld
```

The last line (no text) removes the entry. `&` color codes and `lang:<translation.key>` work.

- From a modded client these edit **your personal** `whathitme-client.cfg`.
- `/whm server rename ...` (or the console) edits the server's `whathitme-common.cfg` and sends it to everyone.
- For a modpack, prefer [pack files](/modpack/names-and-tips); they support composite keys.

### `/whm inspect <player>`

Shows the last death recap and the history of an online player, without `[Copy ID]` / `[Mute]` buttons. Progression rules use **the inspected player's** progress. Needs the mod on the server.

### `/whm test`

```
/whm test minecraft:lava
/whm test mob:minecraft:zombie - minecraft:iron_sword
/whm test minecraft:arrow twilightforest:skeleton_druid - twilightforest:twilight_forest
/whm test minecraft:generic --as Steve --assume ftbquests:1A2B3C4D5E6F7081
```

Builds a dummy killing blow and shows the resulting name, tip, the matching [death triggers](/modpack/death-triggers) (filled in, **not** run), the [custom death message](/modpack/death-messages) and a preview of the chat summary. `-` skips an argument; the dimension defaults to yours. With rules, it also shows which [rule](/modpack/rules) matched, what it hides and whether it is enforced. Details: [Scanner, dump & /whm test](/modpack/scanner#whm-test).

### `/whm dump`

Writes `config/whathitme/.cache/whathitme_dump.csv` and `config/whathitme/hint_template.cfg`. See [Scanner, dump & /whm test](/modpack/scanner#whm-dump).

### `/whm reload`

Re-reads both `.cfg` files and the pack folder, rescans the catalog, and reports:

- how many pack files were read and which could not be parsed (that file is skipped),
- unknown template variables and outputs <Badge type="tip" text="1.3.0+" />,
- rule problems: unknown condition prefixes, missing quest mods, conditions not supported on this version, bad arguments <Badge type="tip" text="1.3.0+" />,
- the Forge / NeoForge [death message warning](/modpack/require-client-mod#the-death-message-warning) <Badge type="tip" text="1.3.0+" />.

On a modded client it reloads the client files too and asks the server to reload when you are op.

### `/whm template` <Badge type="tip" text="1.3.0+" />

```
/whm template                    open the editor
/whm template preview summary    hover | line | summary | alert | copy
/whm template reset              remove all personal templates
/whm template reset hover        remove one personal template
```

See [Templates](/modpack/templates) and [Template editor](/modpack/template-editor).

### `/whm server <subcommand>`

From a modded client, runs the server version of a command. Use it to change the server's common file:

```
/whm server config showTimestamp false
/whm server rename minecraft:outOfWorld &5The Abyss
/whm server ignore minecraft:cactus
```

## Permission changes in 1.2.0

Since 1.2.0, `rename`, `sethint`, `test`, `dump` and `reload` need op also on the client, and `/whm inspect` in single player needs cheats. Personal settings stay open to everyone through `/whm config` and the settings screen.
