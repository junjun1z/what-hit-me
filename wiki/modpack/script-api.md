# Script API reference <Badge type="tip" text="1.3.0+" />

The methods and objects every binding offers ([KubeJS](/modpack/kubejs), [CraftTweaker](/modpack/crafttweaker), [MineTweaker 3](/modpack/minetweaker)) and the Java API other mods can call. Where a binding differs, its page says so (for example the `Api` / `Player` objects of MineTweaker 3).

## Registration

| Method | Effect |
|---|---|
| `condition(name, [description,] fn)` | rule condition `script:<name>`; `fn(player)` returns true / false |
| `variable(name, [description,] fn)` | template variable `{script:<name>}`; `fn(player, damage)` returns text (`&` colors, `lang:` keys) |
| `addRename(key, name)` / `addHint(key, tip)` | like `renameMap` / `hintMap` ([match keys](/modpack/match-keys)) |
| `addDeathMessage(key, message)` | like [`deathMessages`](/modpack/death-messages) |
| `addTemplate(output, lines)` | a [template](/modpack/templates) of `hover`, `line`, `summary`, `alert` or `copy` |
| `addRule(rule)` | an entry of the [`rules`](/modpack/rules) list (an object in KubeJS, JSON text in ZenScript) |

Names use letters, digits, `_`, `.` and `-` and are not case-sensitive. Registering a name again replaces it. Runtime entries are an extra pack source merged **after** the pack files; the pack files themselves are never changed. In `/whm reload` they appear as `script:<mod>`.

## Events

| Event | Properties | Methods |
|---|---|---|
| `damageRecorded` | `player`, `damage` | `setName(text)`, `setHint(text)` (`""` = no tip), `addBadge(text)`, `hide(field)`, `cancel()` |
| `death` | `player`, `death`, `killingBlow` | `setName`, `setHint`, `addBadge`, `hide` (all on the killing blow), `addSummaryLine(text)` |
| `respawn` | `player`, `death` | - |
| `alert` | `player`, `kind` (`totem`, `heavy`, `prevented`), `damage`, `snapshot` | - |

`hide` takes the field names of the rules `hide` list. A matching rule still wins for name and tip; hidden fields of rules and scripts add up. Summary lines and badges accept `&` colors, `lang:` keys and template variables such as `{player}`.

## Damage

All script mods: `damageId`, `damageType`, `name` (English, as a rename / rule shows it), `attackerName`, `attackerId`, `modId`, `weaponName`, `weaponId`, `amount` (health points), `hearts`, `healthAfter`, `maxHealth`, `distance` (-1 = unknown), `hits`, `dimension`, `killingBlow`, `projectile`, `fire`, `explosion`, `magic`, `variable(name)`.
KubeJS also: `directName`, `modName`, `ownerName`, `rawAmount`, `absorbed`, `time`, `totem`, `heavy`, `reflected`, `bypassArmor`, `properties`, `hidden`, `badges`.

## Death

`kind`, `time`, `records` (oldest first), `killingBlow`, `deathMessage` (plain English), `totalAmount`.

## Reading

| Method | Result |
|---|---|
| `lastDeath(player)` | the last death recap, or nothing |
| `history(player, n)` | the newest `n` records, oldest first |
| `stats(player)` | `deaths`, `damageTaken`, `causes` (KubeJS) |
| `test(player, damageId[, entityId])` | what `/whm test` shows for that player: `name`, `hint`, `rule`, `hidden`, `deathMessage`, `commands` |
| `check(player, condition)` | any rule condition, e.g. `check(player, 'advancement:minecraft:story/mine_diamond')` |
| `registrations()` (KubeJS) | what scripts have registered: `kind`, `owner`, `name`, `value`, `fromHook`, `failed` |
| `apiVersion` (KubeJS) | version of the What Hit Me? API, currently `1`; it goes up when methods are added |

## Java API for mod developers {#java-api}

Other mods can use the same API from Java without any script mod: `io.github.junjun1z.whathitme.api.WhatHitMeApi` (Java 8, no Minecraft classes). Pass your mod id as the owner; players can be given as the Minecraft player object, a UUID or a name.

```java
WhatHitMeApi.registerCondition("mymod", "boss_ready", "Boss altar activated", p -> MyData.ready(p.getUuid()));
WhatHitMeApi.listen("mymod", DeathEvent.class, e -> e.addSummaryLine("&7Tip from My Mod"));
```

| Method | Does |
|---|---|
| `API_VERSION` | `1` in 1.3.0 |
| `registerCondition(owner, name, description, condition)` / `registerVariable(owner, name, description, variable)` | `script:<name>` condition (`WhmPlayer -> boolean`) / `{script:<name>}` variable (`(WhmPlayer, WhmDamage) -> text`) |
| `listen(owner, eventClass, listener)` / `listen(owner, "death", listener)` | `DamageRecordedEvent`, `DeathEvent`, `RespawnEvent`, `AlertEvent` (names `damageRecorded`, `death`, `respawn`, `alert`) |
| `onRegister(owner, hook)` | Runs the hook on every rebuild (server start, `/whm reload`, the script mod's reload; at once when the server is already running). What it registers is removed before each rebuild. One hook per owner: a new one replaces the old one |
| `addRename`, `addHint`, `addDeathMessage`, `addTemplate`, `addRule(owner, ...)` | Runtime pack entries (source `script:<owner>`), merged after the pack files |
| `lastDeath(player)`, `history(player, n)`, `stats(player)`, `test(player, damageId, entityId[, assume])`, `player(player)` | Read access; `player(...)` gives a `WhmPlayer` with `getUuid()`, `getName()`, `getNativePlayer()` and `check(condition)` |
| `registrations()` | Everything registered, with owner and failures |
| `clear(owner)` / `removeHooks(owner)` | Remove everything of an owner / only its register hooks |
| `rebuild()` / `rebuildLater()` | Rebuild now / on the next server tick (for script mods without an "after the scripts" hook) |

Load your integration class only when What Hit Me? is installed (for example behind a mod-loaded check), so What Hit Me? stays optional for your mod too.
