# Death triggers

`deathTriggers` run server commands when a death matches a key. Use them for a consolation item, an announcement, a scoreboard counter or a script hook.

```json
{
  "deathTriggers": [
    { "match": "minecraft:lava", "command": "say {player} fell into lava at {x} {y} {z}", "cooldownSeconds": 60 },
    { "match": "mob:minecraft:creeper", "command": "give {player} minecraft:bread 4", "oncePerPlayer": true }
  ]
}
```

## Fields

| Field | Required | Default | Meaning |
|---|---|---|---|
| `match` | yes | | [Match key](/modpack/match-keys) of the killing blow |
| `command` | yes | | Server command. A leading `/` is optional. |
| `oncePerPlayer` | no | `false` | Run only for the first matching death of each player (remembered in `.cache/stats_<uuid>.json`) |
| `cooldownSeconds` | no | `0` | Minimum time between two runs for the same player |
| `if` / `any` / `unless` <Badge type="tip" text="1.3.0+" /> | no | | [Conditions](/modpack/conditions) on the dead player's progress |
| `memo` <Badge type="tip" text="1.3.0+" /> | no | | Your note, never shown to players (the [pack editor](/modpack/pack-editor) writes it) |

## Placeholders

| Placeholder | Value |
|---|---|
| `{player}` | Name of the player who died |
| `{x}` `{y}` `{z}` | Block position of the death (rounded down) |
| `{damage}` | Health points of the killing blow, one decimal |
| any [template variable](/modpack/templates#variables) <Badge type="tip" text="1.3.0+" /> | For example `{attacker_name}`, `{damage_id}`, `{dimension}`, `{weapon}`. Filled in English, whatever the display switches say. Unknown braces stay as they are. |

::: info `{damage}` in commands
In commands `{damage}` keeps its 1.2 meaning: **health points** of the killing blow. The template variable `{damage}` in templates is in hearts.
:::

## How they run

- Server only (also the integrated server in single player), with **console permission**.
- **Every** matching trigger runs, in file order (unlike renames, there is no "most specific wins").
- A matching [progression rule](/modpack/rules) can have its own `commands` too; they run after the triggers.

## Test without dying

```
/whm test minecraft:lava
```

shows the triggers that would run, with placeholders filled in, but does **not** run them.

## Examples

Count lava deaths in a scoreboard:

```json
{ "match": "minecraft:lava", "command": "scoreboard players add {player} lava_deaths 1" }
```

Tell new players about a guide book once, but not veterans:

```json
{ "match": "*", "command": "tellraw {player} {\"text\":\"Check the quest book for survival tips!\"}", "oncePerPlayer": true, "unless": ["tag:veteran"] }
```

Announce boss deaths with the attacker's name:

```json
{ "match": "@twilightforest:*", "command": "say {player} was defeated by {attacker_name} in {dimension}", "cooldownSeconds": 30 }
```
