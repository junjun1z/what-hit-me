# Mod integrations

What Hit Me? works with every mod out of the box: damage from any mod is recorded, named and explained without extra setup. On top of that it has dedicated support for the mods below. None of them is required. When a mod is missing, its feature is simply off.

## Overview

| Mod | What it does with What Hit Me? | Minecraft versions | Status | Details |
|---|---|---|---|---|
| **FTB Quests** | Rules can check whether a quest or chapter is completed (`ftbquests:`) | 1.12.2, 1.15.2 and newer (Fabric, Forge, NeoForge as published) | <Badge type="tip" text="1.3.0+" /> | [Conditions & quest mods](/modpack/conditions#quest-mods) |
| **Better Questing** | Rules can check quest completion (`bq:`), including the GTNH fork | 1.12.2, 1.7.10 (GTNH fork) | <Badge type="tip" text="1.3.0+" /> | [Conditions & quest mods](/modpack/conditions#quest-mods) |
| **Heracles / Odyssey Quests** | Rules can check quest completion (`heracles:`) | 1.20 - 1.21.1 | <Badge type="tip" text="1.3.0+" /> | [Conditions & quest mods](/modpack/conditions#quest-mods) |
| **Game Stages** | Rules can check stages (`stage:`) | 1.12.2 - 1.20.x (Forge) | <Badge type="tip" text="1.3.0+" /> | [Conditions & quest mods](/modpack/conditions#quest-mods) |
| **KubeJS** | Scripts add rule conditions (`script:`), template variables (`{script:}`), event listeners and runtime entries | 1.20.1 (Forge, Fabric), 1.21.1 (NeoForge); more versions in progress | <Badge type="tip" text="1.3.0+" /> | [Script integration](/modpack/scripts) |
| **CraftTweaker** | Same as KubeJS, in ZenScript (`mods.whathitme.WhatHitMe`) | 1.12.2, 1.20.1 (Forge); more versions in progress | <Badge type="tip" text="1.3.0+" /> | [Script integration](/modpack/scripts) |
| **MineTweaker 3** | Same as above on 1.7.10 (also the GTNH fork) | 1.7.10 | <Badge type="warning" text="planned" /> | [Script integration](/modpack/scripts) |
| **Mod Menu** | Opens the What Hit Me? settings screen from the mod list (Fabric) | every Fabric version | ✔ | [Settings screen](/player/settings) |
| **Any mod with damage** | Damage ids and attackers of every mod are recorded; the scanner lists them before anyone gets hurt | all | ✔ | [Scanner](/modpack/scanner), [Damage ids](/reference/damage-ids) |

::: tip Works with scripts on every version
Where no binding exists yet, scripts can still drive What Hit Me?: set a scoreboard tag or score from a script and check it in a rule with `tag:` / `score:`, or let a [death trigger](/modpack/death-triggers) run a command your script listens to. See [Script integration](/modpack/scripts).
:::

## How the integrations work

- **No hard dependency.** Each integration is loaded only when its mod is installed. You never need to install a quest or script mod for What Hit Me? to run.
- **Version differences are handled.** Quest mods changed their code between versions; What Hit Me? supports the old and new variants. The exact versions tested in game are listed in [Conditions & quest mods](/modpack/conditions#tested-in-game).
- **Safe when something does not match.** If an installed mod version cannot be read, the condition counts as not fulfilled and one warning appears in the log and in `/whm reload`. The game never crashes because of it.
- **Server side.** Quest progress is checked on the server, so the integrations work in multiplayer and players cannot fake them.
