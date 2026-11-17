# Script integration <Badge type="warning" text="planned" />

::: warning Coming in 1.3.0 — not implemented yet
This page describes **planned** behaviour. None of it works in the current build. The page will be completed with real examples when the feature is released.
:::

## What works today

You can already connect scripts to What Hit Me? without any special support:

- **Scripts set a tag or score, rules read it.** Let KubeJS, CraftTweaker or a command block add a scoreboard tag (`/tag <player> add boss_ready`) or change a score, then use `tag:` / `score:` [conditions](/modpack/conditions) in your [rules](/modpack/rules).
- **Death triggers call your scripts.** A [death trigger](/modpack/death-triggers) can run any server command, including a command or function your script mod registers.

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

## Planned

Server-authoritative and never a hard dependency: the integration loads only when the script mod is present; script errors are caught, logged once and treated as false / empty.

| Planned feature | Idea |
|---|---|
| Script conditions | Scripts register `name -> predicate(player)`; rules use `script:<name>` (with `!` negation). Unknown names count as false with a `/whm reload` warning. |
| Script variables | Scripts register `name -> function(player, record) -> text`; templates use `{script:<name>}`. Computed on the server when the output is built; listed in the template editor. |
| Events | `damageRecorded` (read the record; set name / tip, add a badge, hide fields, cancel recording) and `death` (read the recap; set the tip, hide fields, add summary lines, run anything). |
| Read / write API | `lastDeath(player)`, `history(player, n)`, `stats(player)`, test results; add renames, tips, rules, death messages and templates at run time (merged after the pack files, rebuilt on reload). |
| Reload | `/whm reload` and the script mod's reload rebuild everything cleanly and list the registered conditions, variables and listeners. |

Planned bindings:

| Script mod | Planned form |
|---|---|
| KubeJS | Global `WhatHitMe` and events `WhatHitMeEvents.*` on every KubeJS version that exists for the supported Minecraft versions |
| CraftTweaker | 1.12.2 ZenScript `mods.whathitme.WhatHitMe`; 1.16.5+ the new API with events |
| MineTweaker 3 | 1.7.10 (including the GTNH fork if it differs) |

The exact names may still change before release.
