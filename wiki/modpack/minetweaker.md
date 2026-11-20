# MineTweaker 3 / CraftTweaker 3.x <Badge type="tip" text="1.3.0+" />

For Forge 1.7.10 - 1.11.2, scripts of MineTweaker 3 and CraftTweaker 3.x (also the [GTNH fork](https://github.com/GTNewHorizons/CraftTweaker)) use `mods.whathitme.WhatHitMe` with two helper objects, `Api` and `Player`. What scripts can do is on [Script integration](/modpack/scripts), every method on the [API reference](/modpack/script-api).

## Supported versions

| Script mod | Minecraft | Support |
|---|---|---|
| CraftTweaker 3.1.0 | 1.7.10 | full |
| GTNH fork of CraftTweaker (3.5.0) | 1.7.10 | full |
| MineTweaker 3.0.10B | 1.7.10 | [partly](#partial-support) |
| MineTweaker 3 / CraftTweaker 3.x | 1.8.9, 1.10.2, 1.11.2 | full |
| CraftTweaker 3.0.9 | 1.9.4 | [partly](#partial-support) |

## Writing scripts

Put the script in `scripts/`. The scripts run when the server starts and on `/minetweaker reload`; What Hit Me? replaces what the previous run registered.

ZenScript 1 functions cannot call static methods, so inside a function everything goes through the arguments: `onRegister` passes an `Api` object (the registration methods), conditions and variables get a `Player` (`check(condition)`, `name`, `player` (the `IPlayer`), `lastDeath`, `history(n)`, `stats`, `test(damageId)`), and every event has `event.player` (a `Player`) and `event.api`. At the top level of the script the static methods of `WhatHitMe` work as in CraftTweaker 1.12.2. Scoreboard tags only exist from 1.9: on 1.7.10 / 1.8.9 check scores instead (`score:<objective>>=1`).

```zenscript
import mods.whathitme.WhatHitMe;
import mods.whathitme.Api;
import mods.whathitme.Player;
import mods.whathitme.DeathEvent;
import mods.whathitme.RespawnEvent;

WhatHitMe.onRegister(function(api as Api) {
    api.condition("veteran", "Has the veteran score", function(player as Player) as bool {
        return player.check("score:veteran>=1");
    });
    api.addRule('{"match": "minecraft:lava", "unless": ["script:veteran"], "name": "???"}');
});

WhatHitMe.onDeath(function(event as DeathEvent) {
    event.addSummaryLine("&7Better luck next time, {player}.");
});

WhatHitMe.onRespawn(function(event as RespawnEvent) {
    event.api.addHint("my:last", event.player.lastDeath.killingBlow.damageType);
});
```

Write `if (...) { ... }` blocks instead of an early `return;` (ZenScript 1 rejects a bare `return;`). The [GTNH fork](https://github.com/GTNewHorizons/CraftTweaker) (mod id `MineTweaker3` as well) works the same way.

## Partial support {#partial-support}

::: warning MineTweaker 3.0.10B (1.7.10) and CraftTweaker 3.0.9 (1.9.4)
Their ZenScript compiler fails on functions without a result, so `onRegister` and the event listeners (`onDamageRecorded`, `onDeath`, `onRespawn`, `onAlert`) do not work there. Top-level registrations, conditions, variables, `addRule` / `addRename` / ... and the read methods work. On 1.7.10 use CraftTweaker 3.1.0 or the GTNH fork for everything.
:::

## Reloading

The scripts run when the server starts and on `/minetweaker reload`. What Hit Me? replaces what the previous run registered. `/whm reload` shows what the scripts registered ([Reload and errors](/modpack/scripts#reload-and-errors)).
