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
| KubeJS 7 | global `WhatHitMe`, events `WhatHitMeEvents.*` (server scripts) | 1.21.1 NeoForge | <Badge type="tip" text="1.3.0+" /> |
| KubeJS 6 | the same | 1.20.1 Forge, Fabric (also NeoForge 1.20.1 with the Forge jar) | <Badge type="tip" text="1.3.0+" /> |
| CraftTweaker 1.12.2 | ZenScript class `mods.whathitme.WhatHitMe` | 1.12.2 Forge | <Badge type="tip" text="1.3.0+" /> |
| CraftTweaker (new ZenScript) | ZenScript class `mods.whathitme.WhatHitMe` | 1.20.1 Forge | <Badge type="tip" text="1.3.0+" /> |
| KubeJS / CraftTweaker on other versions, MineTweaker 3 (1.7.10, also the GTNH fork) | the same forms | every version where the script mod exists | in progress |

On a version without a binding, the recipes in [Without a binding](#without-a-binding) still work.

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

## CraftTweaker (1.16.5 and newer)

Put the script in `scripts/`. The scripts run again on every `/reload`; What Hit Me? replaces what the previous run registered.

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
