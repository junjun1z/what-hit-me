# Mod integrations

What Hit Me? works with every mod out of the box: damage from any mod is recorded, named and explained without extra setup. On top of that it has dedicated support for the mods below. **None of them is required.** When a mod is missing, its feature is simply off.

Every combination marked here was started and tested in game. Download What Hit Me? from [Modrinth](https://modrinth.com/mod/what-hit-me) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/what-hit-me).

## Quest and progression mods <Badge type="tip" text="1.3.0+" />

Used by [progression rules](/modpack/rules) and [death triggers](/modpack/death-triggers) as conditions. The server reads the progress, so it works in multiplayer and cannot be faked.

| Mod | Condition | Minecraft / loader | Mod versions tested |
|---|---|---|---|
| **FTB Quests** | `ftbquests:<quest or chapter id>` (team progress) | Forge 1.12.2, 1.15.2, 1.16.4 / 1.16.5, 1.18.2, 1.19.2, 1.20.1, 1.20.4; Fabric 1.16.5, 1.18.2, 1.19.2, 1.20.1, 1.20.2, 1.20.4, 1.21 / 1.21.1, 1.21.11, 26.1 - 26.1.2; NeoForge 1.20.4, 1.20.6, 1.21 / 1.21.1, 1.21.11, 26.1.2 | 1202.9 - 26.1.2.10 |
| **Better Questing** (also the GTNH fork and Better Questing Unofficial) | `bq:<quest id>` | Forge 1.7.10 - 1.12.2 (every minor version), Forge / NeoForge 1.20.1 | 1.1 - 3.5, GTNH 3.9.2, Unofficial 4.3.2, 4.0.71 |
| **Heracles / Odyssey Quests** | `heracles:<quest id>` | Fabric / Forge 1.20, 1.20.1 (also NeoForge 1.20.1); Fabric 1.21 / 1.21.1; NeoForge 1.21.1 | 0.0.7, 1.1.13, 2.0.3 |
| **Game Stages** | `stage:<name>` | Forge 1.11.2 - 1.20.3, NeoForge 1.20.1 | 1.0.11 - 17.0.1 |
| Vanilla advancements, scoreboard tags and scores | `advancement:`, `tag:`, `score:` | every version (advancements 1.12+, tags 1.9+) | - |

The exact table per Minecraft version and loader, with the reasons for the gaps, is in [Conditions & quest mods](/modpack/conditions#quest-mod-matrix).

## Script mods <Badge type="tip" text="1.3.0+" />

Scripts add `script:` conditions, `{script:}` template variables, event listeners (`damageRecorded`, `death`, `respawn`, `alert`), read access and runtime entries. See [Script integration](/modpack/scripts).

| Mod | Form | Minecraft / loader | Script mod versions |
|---|---|---|---|
| **KubeJS** | global `WhatHitMe`, events `WhatHitMeEvents.*` (older: `onEvent` / `events.listen`) | NeoForge 26.1.2, 1.21.1, 1.20.4; Forge / Fabric 1.20.1, 1.19.2, 1.18.2, 1.16.5; Fabric 1.17.1, 1.16.1; Forge 1.15.2, 1.14.4, 1.12.2 | 1.1 (1.12.2) - 8 (26.1.2) |
| **CraftTweaker** | ZenScript `mods.whathitme.WhatHitMe` | Fabric / NeoForge 1.21.1, 1.20.6; Forge / Fabric / NeoForge 1.20.4; Forge / Fabric 1.18.2 - 1.20.2; Forge 1.17.1, 1.16.5, 1.15.2, 1.14.4, 1.12.2 | 4.1 (1.12.2), 5 - 21 |
| **MineTweaker 3** / CraftTweaker 3.x (also the GTNH fork) | ZenScript `mods.whathitme.WhatHitMe` with `Api` / `Player` objects | Forge 1.7.10, 1.8.9, 1.9.4, 1.10.2, 1.11.2 | MineTweaker 3.0.10B and CraftTweaker 3.0.9 partly; CraftTweaker 3.1.0, GTNH 3.5.0 fully |

No script mod exists for the other versions (1.21.3 - 1.21.11, 26.2, 26.3, KubeJS on Fabric / Forge 1.21+).

::: tip Works with scripts on every version
Where no binding exists, scripts can still drive What Hit Me?: set a scoreboard tag or score from a script and check it in a rule with `tag:` / `score:`, or let a [death trigger](/modpack/death-triggers) run a command your script listens to.
:::

## Other mods

| Mod | What it does with What Hit Me? | Minecraft / loader |
|---|---|---|
| **Fabric API** | Required on Fabric (on 1.21 / 1.21.1: 0.116 or newer) | every Fabric version |
| **Mod Menu** | Opens the What Hit Me? settings screen from the mod list (optional) | every Fabric version |
| **Health-overhaul mods** (First Aid style) | A death with total health left is marked `[Localized / Vital Hit]`; the max HP share uses the max health at the time of the hit. A big hit that First Aid spreads over the body parts is a heavy hit, not a prevented death (1.3.0). First Aid applies some hits more than once internally, so a merged line can show more damage than the attack did | every version (tested: First Aid 1.12.2, 1.20.1) |
| **Mods that name their mobs** (levels, subspecies, for example Lycanites Mobs) | The name the mod builds is shown instead of the plain registry name | every version |
| **Mods that cancel or undo a death** (revive, second-life mods) | A cancelled death or a death undone right after is saved as a snapshot with a "Lethal damage was prevented" alert. Knock-out mods that stop the damage before the death (Hardcore Revival) show a heavy hit alert instead. PlayerRevive keeps the player at 0 health while bleeding: What Hit Me? counts the knock-down as the death (recap, statistics, death triggers) and does not notice a later revive | every version (tested on Forge 1.20.1) |
| **Chat mods** (Chat Heads, Chat Patches, Compact Chat) | Work together with custom / hidden death messages; Compact Chat merges two identical replaced lines into one with `(2)` | tested on 1.20.1 Forge, 1.21.1 Fabric / NeoForge |
| **Grave mods** (Corpse) | No interaction; the recap is unchanged | tested on 1.12.2 / 1.20.1 Forge, 1.21.1 NeoForge |
| **Any mod with damage** | Damage ids and attackers of every mod are recorded; the scanner lists them before anyone gets hurt | every version |

See [Death screen panel](/player/death-screen), [Scanner](/modpack/scanner) and [Damage ids & badges](/reference/damage-ids).

## How the integrations work

- **No hard dependency.** Each integration is loaded only when its mod is installed. You never need to install a quest or script mod for What Hit Me? to run.
- **Version differences are handled.** Quest and script mods changed their code between versions; What Hit Me? supports the old and new variants.
- **Safe when something does not match.** If an installed mod version cannot be read, the condition counts as not fulfilled and one warning appears in the log and in `/whm reload`. The game never crashes because of it.
- **Server side.** Quest progress and scripts are checked on the server, so the integrations work in multiplayer and players cannot fake them.
