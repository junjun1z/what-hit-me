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

| Script mod | Form | Minecraft / loader | Status |
|---|---|---|---|
| KubeJS 8 / 7 | global `WhatHitMe`, events `WhatHitMeEvents.*` (server scripts) | 26.1.2 NeoForge, 1.21.1 NeoForge | <Badge type="tip" text="1.3.0+" /> |
| KubeJS 6 (also the KubeJS 7 beta of NeoForge 1.20.4) | the same | 1.20.4 NeoForge, 1.20.1 Forge / Fabric (also NeoForge 1.20.1 with the Forge jar), 1.19.2 Forge / Fabric | <Badge type="tip" text="1.3.0+" /> |
| KubeJS 5 / 3 | global `WhatHitMe`, events `onEvent('whathitme.*')` | 1.18.2 Forge / Fabric, 1.17.1 Fabric, 1.16.5 Forge / Fabric | <Badge type="tip" text="1.3.0+" /> |
| Old KubeJS (1.x / 2.x) | global `WhatHitMe`, events `events.listen('whathitme.*')` | 1.16.1 Fabric, 1.15.2 Forge, 1.14.4 Forge, 1.12.2 Forge | <Badge type="tip" text="1.3.0+" /> |
| CraftTweaker (new ZenScript) | ZenScript class `mods.whathitme.WhatHitMe` | 1.21.1, 1.20.6 Fabric / NeoForge; 1.20.4 Forge / Fabric / NeoForge; 1.20.2 - 1.18.2 Forge / Fabric; 1.17.1 Forge; 1.16.5, 1.15.2, 1.14.4 Forge | <Badge type="tip" text="1.3.0+" /> |
| CraftTweaker 1.12.2 | ZenScript class `mods.whathitme.WhatHitMe` | 1.12.2 Forge | <Badge type="tip" text="1.3.0+" /> |
| MineTweaker 3 / CraftTweaker 3.x (also the GTNH fork) | ZenScript class `mods.whathitme.WhatHitMe` | 1.11.2, 1.10.2, 1.9.4, 1.8.9, 1.7.10 Forge (MineTweaker 3.0.10B and CraftTweaker 3.0.9: [partly](#minetweaker-3-crafttweaker-3-x-1-7-10-1-11-2)) | <Badge type="tip" text="1.3.0+" /> |

No script mod exists for the other versions (1.21.3 - 1.21.11, 26.2, 26.3, KubeJS on Fabric / Forge 1.21+). There the recipes in [Without a binding](#without-a-binding) still work. KubeJS 8 on 26.1.2 needs Java 25 (it does not start on Java 27).

## KubeJS

Put the script in `kubejs/server_scripts/`. Register everything inside `WhatHitMeEvents.register`: it runs again on every rebuild (server start, `/whm reload`, `/reload`), so nothing is registered twice.

```js
// kubejs/server_scripts/whathitme.js
WhatHitMeEvents.register(event => {
  // rule condition: script:veteran
  event.condition('veteran', 'Has killed the Wither', player => player.stages.has('wither_down'))
  // template variable: {script:deaths_here}
  event.variable('deaths_here', 'Deaths in this dimension', (player, damage) =>
    '' + player.persistentData.getInt('deaths_' + damage.dimension))
  // runtime entries (merged after the pack files)
  event.addRename('mob:minecraft:warden', '&4The Warden')
  event.addHint('minecraft:lava', 'Carry Fire Resistance potions.')
  event.addRule({ match: '*@minecraft:wither', unless: ['script:veteran'], name: '???', hide: ['attacker', 'mod'] })
})

WhatHitMeEvents.damageRecorded(event => {
  // event.player, event.damage (see "Damage" below)
  if (event.damage.attackerId == 'minecraft:creeper' && event.damage.hearts >= 5) event.addBadge('&c[Boom]')
  if (event.damage.damageType == 'cactus') event.cancel()   // not recorded at all
})

WhatHitMeEvents.death(event => {
  if (event.killingBlow && event.killingBlow.attackerId == 'minecraft:warden') {
    event.setHint('Sneak around the Warden, it is blind.')
    event.addSummaryLine('&7You died {script:deaths_here} times in this dimension.')
  }
})

WhatHitMeEvents.respawn(event => {
  let last = WhatHitMe.lastDeath(event.player)
  if (last && last.killingBlow.damageType == 'outOfWorld') event.player.give('minecraft:ender_pearl')
})

WhatHitMeEvents.alert(event => {
  // event.kind: totem, heavy, prevented
  if (event.kind == 'totem') event.player.tell('A totem saved you from ' + event.damage.name)
})
```

`WhatHitMe` (the global of server scripts) has the same registration methods as the register event plus the read methods: `lastDeath(player)`, `history(player, n)`, `stats(player)`, `test(player, damageId[, entityId])`, `check(player, condition)` and `registrations()`. Registrations made through `WhatHitMe` outside the register event (for example in a listener) stay until the server scripts reload.

## KubeJS 5 and older

KubeJS 5 (1.18.2) and KubeJS 3 (1.16.5, 1.17.1 Fabric) use `onEvent`, old KubeJS (1.12.2 - 1.16.1) uses `events.listen`. The event names are `whathitme.register`, `whathitme.damage_recorded`, `whathitme.death`, `whathitme.respawn` and `whathitme.alert`; the event objects and the global `WhatHitMe` are the same as above. Players are the Minecraft player objects, so use `WhatHitMe.check(player, ...)` for tags, scores, stages and quests.

```js
// kubejs/server_scripts/whathitme.js (1.12.2: kubejs/whathitme.js)
onEvent('whathitme.register', event => {      // old KubeJS: events.listen('whathitme.register', function (event) { ... })
  event.condition('veteran', 'Has the veteran tag', player => WhatHitMe.check(player, 'tag:veteran'))
  event.addRule({ match: '*@minecraft:wither', unless: ['script:veteran'], name: '???' })
})

onEvent('whathitme.damage_recorded', event => {
  if (event.damage.damageType == 'cactus') event.cancel()
})
```

- KubeJS 1.12.2 / 1.14.4 / 1.16.1 run on Nashorn: write ES5 (`function`, `var`, no arrow functions) and give rules as JSON text.
- Script folders: 1.12.2 `kubejs/`; 1.14.4, 1.15.2 and 1.16.1 `kubejs/data/modpack/kubejs/` (1.14.4 and 1.16.1 also list the file in `scripts.json` there); 1.16.5 and newer `kubejs/server_scripts/`.
- 1.16.1 Fabric: use KubeJS Fabric 1.1.5 (1.5.3 is marked for 1.16.1 but needs 1.16.2+). It runs on Nashorn, which current Fabric Loaders hide from mods: add the JVM argument `-Dfabric.debug.disableClassPathIsolation=true`.
- 1.12.2: the scripts reload with `/kubejs reload`; 1.14.4 - 1.18.2: with `/reload`.

## MineTweaker 3 / CraftTweaker 3.x (1.7.10 - 1.11.2)

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

::: warning MineTweaker 3.0.10B (1.7.10) and CraftTweaker 3.0.9 (1.9.4)
Their ZenScript compiler fails on functions without a result, so `onRegister` and the event listeners (`onDamageRecorded`, `onDeath`, `onRespawn`, `onAlert`) do not work there. Top-level registrations, conditions, variables, `addRule` / `addRename` / ... and the read methods work. On 1.7.10 use CraftTweaker 3.1.0 or the GTNH fork for everything.
:::

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

## CraftTweaker (1.14.4 and newer)

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

## Reference

### Registration

| Method | Effect |
|---|---|
| `condition(name, [description,] fn)` | rule condition `script:<name>`; `fn(player)` returns true / false |
| `variable(name, [description,] fn)` | template variable `{script:<name>}`; `fn(player, damage)` returns text (`&` colors, `lang:` keys) |
| `addRename(key, name)` / `addHint(key, tip)` | like `renameMap` / `hintMap` ([match keys](/modpack/match-keys)) |
| `addDeathMessage(key, message)` | like [`deathMessages`](/modpack/death-messages) |
| `addTemplate(output, lines)` | a [template](/modpack/templates) of `hover`, `line`, `summary`, `alert` or `copy` |
| `addRule(rule)` | an entry of the [`rules`](/modpack/rules) list (an object in KubeJS, JSON text in ZenScript) |

Names use letters, digits, `_`, `.` and `-` and are not case-sensitive. Registering a name again replaces it. Runtime entries are an extra pack source merged **after** the pack files; the pack files themselves are never changed. In `/whm reload` they appear as `script:<mod>`.

### Events

| Event | Properties | Methods |
|---|---|---|
| `damageRecorded` | `player`, `damage` | `setName(text)`, `setHint(text)` (`""` = no tip), `addBadge(text)`, `hide(field)`, `cancel()` |
| `death` | `player`, `death`, `killingBlow` | `setName`, `setHint`, `addBadge`, `hide` (all on the killing blow), `addSummaryLine(text)` |
| `respawn` | `player`, `death` | - |
| `alert` | `player`, `kind` (`totem`, `heavy`, `prevented`), `damage`, `snapshot` | - |

`hide` takes the field names of the rules `hide` list. A matching rule still wins for name and tip; hidden fields of rules and scripts add up. Summary lines and badges accept `&` colors, `lang:` keys and template variables such as `{player}`.

### Damage

All script mods: `damageId`, `damageType`, `name` (English, as a rename / rule shows it), `attackerName`, `attackerId`, `modId`, `weaponName`, `weaponId`, `amount` (health points), `hearts`, `healthAfter`, `maxHealth`, `distance` (-1 = unknown), `hits`, `dimension`, `killingBlow`, `projectile`, `fire`, `explosion`, `magic`, `variable(name)`.
KubeJS also: `directName`, `modName`, `ownerName`, `rawAmount`, `absorbed`, `time`, `totem`, `heavy`, `reflected`, `bypassArmor`, `properties`, `hidden`, `badges`.

### Death

`kind`, `time`, `records` (oldest first), `killingBlow`, `deathMessage` (plain English), `totalAmount`.

### Reading

| Method | Result |
|---|---|
| `lastDeath(player)` | the last death recap, or nothing |
| `history(player, n)` | the newest `n` records, oldest first |
| `stats(player)` | `deaths`, `damageTaken`, `causes` (KubeJS) |
| `test(player, damageId[, entityId])` | what `/whm test` shows for that player: `name`, `hint`, `rule`, `hidden`, `deathMessage`, `commands` |
| `check(player, condition)` | any rule condition, e.g. `check(player, 'advancement:minecraft:story/mine_diamond')` |

## Reload and errors

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

## For mod developers

Other mods can use the same API from Java without any script mod: `io.github.junjun1z.whathitme.api.WhatHitMeApi` (Java 8, no Minecraft classes). Pass your mod id as the owner; players can be given as the Minecraft player object, a UUID or a name.

```java
WhatHitMeApi.registerCondition("mymod", "boss_ready", "Boss altar activated", p -> MyData.ready(p.getUuid()));
WhatHitMeApi.listen("mymod", DeathEvent.class, e -> e.addSummaryLine("&7Tip from My Mod"));
```
