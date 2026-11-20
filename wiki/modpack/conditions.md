# Conditions <Badge type="tip" text="1.3.0+" />

Conditions are used by [progression rules](/modpack/rules) (`if`, `any`, `unless`) and by [death triggers](/modpack/death-triggers). Each condition is `prefix:argument`. A `!` in front negates it.

```json
"if":     ["advancement:minecraft:story/enter_the_nether", "score:deaths>=3"],
"any":    ["tag:nether_guide", "tag:veteran"],
"unless": ["!advancement:minecraft:story/mine_diamond"]
```

This page covers the conditions built into Minecraft. Quest mods and scripts add more prefixes: see [Quest mods](/modpack/quest-mods) and [Script integration](/modpack/scripts).

## Built-in conditions

| Condition | True when | Example |
|---|---|---|
| `advancement:<id>` | The player has completed the advancement | `advancement:minecraft:end/kill_dragon` |
| `tag:<tag>` | The player has the scoreboard tag (`/tag` command) | `tag:veteran` |
| `score:<objective><op><n>` | The player's score compares true. Ops: `>=` `>` `<=` `<` `=` `!=`. No score = false. | `score:deaths>=3` |
| `!<condition>` | The condition is false | `!tag:veteran` |

How the lists combine: all of `if` **and** at least one of `any` (when given) **and** none of `unless`.

## Other prefixes

| Prefix | From | Page |
|---|---|---|
| `ftbquests:<id>` | FTB Quests | [FTB Quests](/modpack/ftb-quests) |
| `bq:<id>` | Better Questing (also the GTNH fork, Better Questing Unofficial and 4.x) | [Better Questing](/modpack/better-questing) |
| `heracles:<id>` | Heracles / Odyssey Quests | [Heracles / Odyssey Quests](/modpack/heracles) |
| `stage:<name>` | Game Stages | [Game Stages](/modpack/game-stages) |
| `script:<name>` | A KubeJS / CraftTweaker / MineTweaker script, or another mod through the Java API | [Script integration](/modpack/scripts) |

## Quest mods {#quest-mods}

Quest progress can be used in the same lists, and several quest mods can be mixed in one rule. Which quest mod works on which Minecraft version, and how to combine them, is on [Quest mods](/modpack/quest-mods) ([matrix](/modpack/quest-mods#quest-mod-matrix)).

::: tip Script mods on every version
KubeJS, CraftTweaker and command blocks can drive rules on every version: let them set a **scoreboard tag** or **score**, and use `tag:` / `score:` conditions. Where a binding exists, scripts register `script:` conditions directly ([script integration](/modpack/scripts)).
:::

## Built-in conditions per Minecraft version

| Condition | 1.7.10, 1.8.9 | 1.9 - 1.11.2 | 1.12+ |
|---|---|---|---|
| `advancement:` | not supported | not supported | ✔ |
| `tag:` | not supported | ✔ | ✔ |
| `score:` | ✔ | ✔ | ✔ |

Same on Forge, Fabric and NeoForge. Advancements do not exist before 1.12 and entity tags not before 1.9. An unsupported condition counts as false and `/whm reload` says "not supported on Forge 1.x". On 1.7.10 / 1.8.9, use a score as a flag instead of a tag: `score:veteran>=1`.

## Finding the ids

| Condition | Where to find the value |
|---|---|
| `advancement:` | The advancement id. `/advancement grant @s only ` suggests every id while you type. Mods use their own namespace (`twilightforest:progress_lich`). |
| `tag:` | Any name you give with `/tag <player> add <tag>` (1.13+) or `/scoreboard players tag <player> add <tag>` (1.9 - 1.12.2) |
| `score:` | The objective name of `/scoreboard objectives add <name> <criterion>`. Criteria like `deathCount` or `minecraft.killed:minecraft.wither` count for you. |

The [pack editor](/modpack/pack-editor#condition-picker) lists the advancements, objectives and tags the server knows, so you can click them instead of typing.

## Checking conditions

- `/whm reload` lists unknown prefixes, conditions without prefix, missing mods, conditions unsupported on this version and bad arguments (for example a malformed `score:`).
- `/whm test <damage_id> ... --as <player>` evaluates the rules with that player's real progress.
- `--assume <condition>` treats a condition as fulfilled, `--assume !<condition>` as not fulfilled. Repeat it for several conditions.

```
/whm test minecraft:generic twilightforest:lich --as Steve --assume advancement:twilightforest:progress_lich --assume !tag:veteran
```
