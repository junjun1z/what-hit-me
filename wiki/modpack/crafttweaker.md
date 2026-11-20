# CraftTweaker <Badge type="tip" text="1.3.0+" />

CraftTweaker scripts use the ZenScript class `mods.whathitme.WhatHitMe`. There are two forms: CraftTweaker 1.12.2 and the new ZenScript of CraftTweaker for 1.14.4 and newer. For 1.7.10 - 1.11.2 see [MineTweaker 3 / CraftTweaker 3.x](/modpack/minetweaker). What scripts can do is on [Script integration](/modpack/scripts), every method on the [API reference](/modpack/script-api).

## Supported versions

| CraftTweaker | Minecraft / loader |
|---|---|
| CraftTweaker 5 - 21 (new ZenScript) | 1.21.1, 1.20.6 Fabric / NeoForge; 1.20.4 Forge / Fabric / NeoForge; 1.20.2 - 1.18.2 Forge / Fabric; 1.17.1 Forge; 1.16.5, 1.15.2, 1.14.4 Forge |
| CraftTweaker 4.1 | 1.12.2 Forge |

There is no CraftTweaker for 1.21.3 - 1.21.11 and 26.x, and none that starts on Fabric 1.17.1 with the Fabric Loader What Hit Me? needs.

## CraftTweaker 1.14.4 and newer

Put the script in `scripts/`. The scripts run again on every `/reload`; What Hit Me? replaces what the previous run registered. The player type is `crafttweaker.api.entity.type.player.Player` from 1.17.1, `crafttweaker.api.player.MCPlayerEntity` on 1.16.5 and `crafttweaker.api.entity.player.MCPlayerEntity` on 1.14.4 / 1.15.2. CraftTweaker for Fabric 1.18.2 / 1.19.2 needs Ingredient Extension API. CraftTweaker for Fabric 1.17.1 does not start with the Fabric Loader versions What Hit Me? needs, so there is no binding for it.

```zenscript
import crafttweaker.api.entity.type.player.Player;
import mods.whathitme.WhatHitMe;
import mods.whathitme.Damage;
import mods.whathitme.DeathEvent;

WhatHitMe.onRegister(() => {
    WhatHitMe.condition("veteran", "Has the veteran tag", (player as Player) as bool => WhatHitMe.check(player, "tag:veteran"));
    WhatHitMe.variable("dim", "Dimension of the hit", (player as Player, damage as Damage) as string => damage.dimension);
    WhatHitMe.addRule('{"match": "*@minecraft:wither", "unless": ["script:veteran"], "name": "???"}');
});

WhatHitMe.onDeath((event as DeathEvent) => {
    event.addSummaryLine("&7Died in {script:dim}");
});
```

## CraftTweaker 1.12.2

Put the script in `scripts/`. CraftTweaker 1.12.2 runs the scripts once when the game starts.

```zenscript
import crafttweaker.player.IPlayer;
import mods.whathitme.WhatHitMe;
import mods.whathitme.Damage;
import mods.whathitme.DamageRecordedEvent;
import mods.whathitme.DeathEvent;

WhatHitMe.onRegister(function() {
    WhatHitMe.condition("veteran", "Has the veteran tag", function(player as IPlayer) as bool {
        return WhatHitMe.check(player, "tag:veteran");
    });
    WhatHitMe.variable("hp", "Health after the hit", function(player as IPlayer, damage as Damage) as string {
        return "" ~ damage.healthAfter;
    });
    WhatHitMe.addRename("mob:minecraft:wither_skeleton", "&8Wither Skeleton");
    WhatHitMe.addRule('{"match": "*@minecraft:wither", "unless": ["script:veteran"], "name": "???"}');
});

WhatHitMe.onDamageRecorded(function(event as DamageRecordedEvent) {
    if (event.damage.damageType == "cactus") event.cancel();
});

WhatHitMe.onDeath(function(event as DeathEvent) {
    event.addSummaryLine("&7Health after the last hit: {script:hp}");
});
```

Rules are given as JSON text. The other methods: `onRespawn`, `onAlert`, `addHint`, `addDeathMessage`, `addTemplate(output, lines as string[])`, `lastDeath`, `history`, `stats`, `test`, `check`.

## Reloading

- **1.14.4 and newer:** `/reload` runs the scripts again; What Hit Me? replaces what the previous run registered.
- **1.12.2:** the scripts run once when the game starts. Restart the game to change script registrations.

After a reload, `/whm reload` shows what the scripts registered ([Reload and errors](/modpack/scripts#reload-and-errors)).
