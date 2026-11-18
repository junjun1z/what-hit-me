# Progression rules <Badge type="tip" text="1.3.0+" />

Progression rules change what a death shows **depending on the player's progress**. The classic example: a boss shows up as `???` with no tip and no details until the player has finished the boss quest.

The server decides. It evaluates the rules for each player whenever it builds an output and sends only the finished result, so hidden information never reaches the client. There is no tick work.

## A first rule

<!-- TODO media: screenshot pair: the same death as "???" without a tip before the quest, and the full record after it -->

`config/whathitme/pack/rules/bosses.json`:

```json
{
  "rules": [
    {
      "match": "*@twilightforest:lich",
      "unless": ["advancement:twilightforest:progress_lich"],
      "name": "&k???",
      "hint": "",
      "hide": ["damage_id", "attacker", "mod", "weapon", "death_message"]
    }
  ]
}
```

Until the player has the advancement `twilightforest:progress_lich`:

- the Lich's hits are named `???` (obfuscated),
- there is no tip,
- the damage id, attacker, mod, weapon and death message are removed before anything is sent.

After the advancement, everything is shown normally, **including old entries** in `/whm`, because rules are evaluated every time an output is built.

Check it: `/whm reload`, then

```
/whm test minecraft:generic twilightforest:lich --as Steve
/whm test minecraft:generic twilightforest:lich --as Steve --assume advancement:twilightforest:progress_lich
```

## Fields

| Field | Type | Default | Meaning |
|---|---|---|---|
| `match` | string | `*` | [Match key](/modpack/match-keys) of the record |
| `if` | condition or list | | **All** of these must be true |
| `any` | condition or list | | **At least one** must be true |
| `unless` | condition or list | | **None** of these may be true |
| `name` | string | | Replaces the name. Template variables, `&` colors, `lang:` keys. |
| `hint` | string | | Replaces the tip. `""` = **no tip**. |
| `hide` | string or list | | Fields to remove, see below |
| `commands` | string or list | | Server commands run on a matching **death** (`command` works too) |
| `enforce` | boolean | `true` if `hide` is set, else `false` | Personal renames, tips and templates do not apply |
| `oncePerPlayer` | boolean | `false` | Run `commands` only once per player |
| `cooldownSeconds` | number | `0` | Cooldown of `commands` per player |
| `memo` | string | | Your note, never shown to players (the [pack editor](/modpack/pack-editor) writes it) |

Conditions are explained on [Conditions & quest mods](/modpack/conditions).

## How rules are chosen

1. Rules are read in [pack file order](/modpack/pack-folder#how-files-are-read), top to bottom inside a file.
2. For each record, the **first** rule whose `match` fits **and** whose conditions are met wins. Later rules are not looked at.
3. No rule matches: the record behaves as without rules (renameMap, hintMap, templates as usual).

::: warning First match, not most specific
Unlike `renameMap`, rules do not use specificity. Put special rules **before** general ones:

```json
"rules": [
  { "match": "twilightforest:lich_bolt @twilightforest:lich", "unless": ["advancement:twilightforest:progress_lich"], "name": "Strange Bolt" },
  { "match": "*@twilightforest:lich", "unless": ["advancement:twilightforest:progress_lich"], "name": "???" }
]
```
:::

## `name` and `hint`

- A matching rule's `name` / `hint` override `renameMap` / `hintMap` for that record.
- Without `enforce`, a player's **personal** rename / tip still wins.
- `"hint": ""` removes the tip completely (no automatic tip either).
- Both accept template variables: `"name": "Unknown {damage_name}"`.

## `hide`

| Field | Removes |
|---|---|
| `damage_id` | The damage id (sent as `whathitme:hidden`); no `[Copy ID]` / `[Mute]`, no ID line |
| `attacker` | Attacker name, id, projectile, owner, distance and invisibility; melee ids (`mob:...`) are hidden too |
| `mod` | The mod name |
| `weapon` | Weapon, its id and enchantments |
| `death_message` | The death message (replaced, see [hidden death messages](/modpack/death-messages#hidden-death-messages-from-rules)) |
| `tip` (alias `hint`) | The tip |
| `properties` | Damage properties |
| `environment` | Block / fluid / held items context |
| `owner` | Owner of pets, turrets, projectiles |
| `distance` | Distance to the attacker |
| `position` (aliases `coords`, `dimension`) | Dimension and coordinates (also in the copy text) |

Hidden fields are removed **on the server before packets are built**, also in `/whm search`, `/whm dump`, `/whm inspect` and the copy text. `/whm reload` warns about unknown hide names.

`hide` implies `"enforce": true`, so players cannot bring the information back with personal templates or renames. Write `"enforce": false` to allow that.

## `commands`

Run with console permission when a player **dies** and this rule is the matching one. Placeholders are the same as for [death triggers](/modpack/death-triggers#placeholders).

```json
{ "match": "*@twilightforest:lich", "unless": ["tag:lich_warned"],
  "commands": ["tellraw {player} {\"text\":\"The Lich is weak to ...\"}", "tag {player} add lich_warned"] }
```

## When rules are evaluated

Whenever the server builds an output for a player:

- death recap, respawn summary, alerts,
- `/whm history`, `last`, `snapshot`, `inspect` (with the **inspected** player's progress),
- `/whm search` and `dump` entries,
- template previews,
- custom death messages.

Nothing is cached between outputs, so completing a quest reveals old entries at once.

::: info Requirements
Rules need the mod on the **server** (or single player). Client-only installs never apply rules. 1.2 clients on a 1.3 server get the stripped records (hidden fields removed) but not the rule's name and tip.
:::

## Players without the mod

The server applies rules to the English chat it sends them. But on **Forge / NeoForge** the vanilla death message cannot be changed for them, so a hidden killer may still appear there. Use [`requireClientMod`](/modpack/require-client-mod) if that matters. Fabric servers replace the death message for everyone.

## Death triggers with conditions

`deathTriggers` accept `if`, `any` and `unless` too (with the dead player's progress):

```json
{ "match": "minecraft:lava", "command": "give {player} minecraft:water_bucket", "unless": ["tag:veteran"], "oncePerPlayer": true }
```

## More examples

See the [Cookbook](/cookbook#progression-rules) for ready-to-copy rules: hide everything in a dimension until it is unlocked, tips that change after a few deaths, FTB Quests and Game Stages examples.
