# KubeJS <Badge type="tip" text="1.3.0+" />

What Hit Me? has a binding for every KubeJS generation, from KubeJS 1.1 on Forge 1.12.2 to KubeJS 8 on NeoForge 26.1.2. Server scripts get a global `WhatHitMe` and What Hit Me? events. See [Script integration](/modpack/scripts) for what scripts can do and the [API reference](/modpack/script-api) for every method.

## Supported versions

| KubeJS | Events | Minecraft / loader |
|---|---|---|
| KubeJS 8 / 7 | `WhatHitMeEvents.*` | 26.1.2 NeoForge, 1.21.1 NeoForge |
| KubeJS 6 (also the KubeJS 7 beta of NeoForge 1.20.4) | `WhatHitMeEvents.*` | 1.20.4 NeoForge, 1.20.1 Forge / Fabric (also NeoForge 1.20.1 with the Forge jar), 1.19.2 Forge / Fabric |
| KubeJS 5 / 3 | `onEvent('whathitme.*')` | 1.18.2 Forge / Fabric, 1.17.1 Fabric, 1.16.5 Forge / Fabric |
| Old KubeJS (1.x / 2.x) | `events.listen('whathitme.*')` | 1.16.1 Fabric, 1.15.2 Forge, 1.14.4 Forge, 1.12.2 Forge |

There is no KubeJS for Fabric / Forge 1.21+, 1.21.3 - 1.21.11, 26.2 and 26.3. KubeJS 8 on 26.1.2 needs Java 25 (it does not start on Java 27).

## KubeJS 6, 7 and 8

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

## Reloading

| KubeJS | Reload |
|---|---|
| 6, 7, 8 | `/reload`; `/whm reload` also rebuilds the registrations |
| 3, 5 (1.16.5 - 1.18.2) | `/reload` |
| old (1.14.4 - 1.16.1) | `/reload` |
| 1.12.2 | `/kubejs reload` |

After a reload, `/whm reload` shows what the scripts registered ([Reload and errors](/modpack/scripts#reload-and-errors)).
