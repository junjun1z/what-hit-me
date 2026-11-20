# Script integration <Badge type="tip" text="1.3.0+" />

KubeJS and CraftTweaker scripts can extend What Hit Me? on the server:

| Feature | What a script does | Used in |
|---|---|---|
| **Conditions** | registers `name -> (player) => true/false` | [rules](/modpack/rules) and [death triggers](/modpack/death-triggers) as `script:<name>` (`!script:<name>` negates) |
| **Variables** | registers `name -> (player, damage) => text` | [templates](/modpack/templates) as `{script:<name>}`, rule names / tips, death trigger commands |
| **Events** | listens to `damageRecorded`, `death`, `respawn`, `alert` | change the name / tip of a hit, add badges, hide fields, cancel a recording, add summary lines, run anything |
| **Read / write** | reads the last death, history, statistics, `/whm test` results; adds renames, tips, rules, death messages, templates at run time | anything |

Everything is decided on the server and sent to the players, like the [rules](/modpack/rules): players with the mod see script values in their own (translated) output, players without the mod get the server's English chat. The integration never makes a script mod required, and it loads only when the script mod is installed.

## Supported versions

| Script mod | Form | Minecraft / loader | Page |
|---|---|---|---|
| KubeJS 8 / 7 | global `WhatHitMe`, events `WhatHitMeEvents.*` (server scripts) | 26.1.2 NeoForge, 1.21.1 NeoForge | [KubeJS](/modpack/kubejs) |
| KubeJS 6 (also the KubeJS 7 beta of NeoForge 1.20.4) | the same | 1.20.4 NeoForge, 1.20.1 Forge / Fabric (also NeoForge 1.20.1 with the Forge jar), 1.19.2 Forge / Fabric | [KubeJS](/modpack/kubejs) |
| KubeJS 5 / 3 | global `WhatHitMe`, events `onEvent('whathitme.*')` | 1.18.2 Forge / Fabric, 1.17.1 Fabric, 1.16.5 Forge / Fabric | [KubeJS](/modpack/kubejs) |
| Old KubeJS (1.x / 2.x) | global `WhatHitMe`, events `events.listen('whathitme.*')` | 1.16.1 Fabric, 1.15.2 Forge, 1.14.4 Forge, 1.12.2 Forge | [KubeJS](/modpack/kubejs) |
| CraftTweaker (new ZenScript) | ZenScript class `mods.whathitme.WhatHitMe` | 1.21.1, 1.20.6 Fabric / NeoForge; 1.20.4 Forge / Fabric / NeoForge; 1.20.2 - 1.18.2 Forge / Fabric; 1.17.1 Forge; 1.16.5, 1.15.2, 1.14.4 Forge | [CraftTweaker](/modpack/crafttweaker) |
| CraftTweaker 1.12.2 | ZenScript class `mods.whathitme.WhatHitMe` | 1.12.2 Forge | [CraftTweaker](/modpack/crafttweaker) |
| MineTweaker 3 / CraftTweaker 3.x (also the GTNH fork) | ZenScript class `mods.whathitme.WhatHitMe` | 1.11.2, 1.10.2, 1.9.4, 1.8.9, 1.7.10 Forge (MineTweaker 3.0.10B and CraftTweaker 3.0.9: [partly](/modpack/minetweaker#partial-support)) | [MineTweaker 3](/modpack/minetweaker) |

No script mod exists for the other versions (1.21.3 - 1.21.11, 26.2, 26.3, KubeJS on Fabric / Forge 1.21+). There the recipes in [Without a binding](#without-a-binding) still work. KubeJS 8 on 26.1.2 needs Java 25 (it does not start on Java 27).

## Pages

| Page | Contents |
|---|---|
| [KubeJS](/modpack/kubejs) | KubeJS 1.x - 8: script folders, event names per generation, examples |
| [CraftTweaker](/modpack/crafttweaker) | CraftTweaker 1.12.2 and 1.14.4 - 1.21.1 (ZenScript), player types per version |
| [MineTweaker 3 / CraftTweaker 3.x](/modpack/minetweaker) | 1.7.10 - 1.11.2, the `Api` / `Player` objects, the GTNH fork |
| [API reference](/modpack/script-api) | Registration, events, damage, death and reading methods; the Java API for mod developers |

## A first script

The same idea works in every script mod: a condition `script:veteran` used by a rule, and a death listener that adds a line to the summary. KubeJS 7 / 8:

```js
// kubejs/server_scripts/whathitme.js
WhatHitMeEvents.register(event => {
  event.condition('veteran', 'Has the veteran tag', player => WhatHitMe.check(player, 'tag:veteran'))
  event.addRule({ match: '*@minecraft:wither', unless: ['script:veteran'], name: '???', hide: ['attacker', 'mod'] })
})

WhatHitMeEvents.death(event => {
  event.addSummaryLine('&7Deaths so far: ' + WhatHitMe.stats(event.player).deaths)
})
```

The same in the other script mods: [CraftTweaker](/modpack/crafttweaker), [MineTweaker 3](/modpack/minetweaker).

## Reload and errors

![/whm reload with the "Scripts:" line: what the scripts registered](/images/scripts-reload.webp)

- `/whm reload` and the script mod's reload rebuild everything cleanly: what scripts registered is removed, the scripts register again, the runtime entries are merged once.
- `/whm reload` lists what scripts registered (`Scripts: 2 conditions (...), 1 variable (...), listeners: death, ...`) and warns about unknown `script:` names, about functions that failed and about a missing script mod.
- A script function that throws counts as false (conditions) or empty (variables), the listener is skipped; the error is logged once per function. A broken script never breaks a death recap.
- Without the script mod, `script:` conditions are false and `/whm reload` says so; nothing else changes.

## Without a binding

These work on every version, also where no binding exists yet:

- **Tags and scores set by scripts, read by rules.** A script adds a scoreboard tag (`/tag <player> add boss_ready`) or changes a score; a [rule](/modpack/rules) checks it with `tag:` / `score:`.
- **Death triggers call scripts.** A [death trigger](/modpack/death-triggers) runs any server command, including a command or function your script mod registers. `{script:<name>}` works in trigger commands where a binding exists.

```json
{
  "rules": [
    { "match": "*@mymod:boss", "unless": ["tag:boss_ready"], "name": "???", "hide": ["attacker", "mod"] }
  ],
  "deathTriggers": [
    { "match": "*@mymod:boss", "command": "function mypack:boss_death" }
  ]
}
```
