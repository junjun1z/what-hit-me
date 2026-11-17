# Damage ids & badges

Every hit gets a **damage id**. You need it for renames, tips, hidden ids, death triggers, death messages and rules. Find it in the hover text (`ID: ...`), with `[Copy ID]`, with [`/whm search`](/player/tools#search-whm-search) or with [`/whm dump`](/modpack/scanner#whm-dump).

## How ids are built

| Damage | Id | Examples |
|---|---|---|
| Attack by an entity (melee) | `mob:<entity id>` | `mob:minecraft:zombie`, `mob:minecraft:player`, `mob:lycanitesmobs:ventoraptor` |
| Everything else | `<modid>:<damage type>` | `minecraft:fall`, `minecraft:lava`, `minecraft:arrow`, `minecraft:outOfWorld`, `minecraft:explosion.player` |
| Unknown mod | `unknown:<damage type>` | |
| Hidden by a rule <Badge type="tip" text="1.3.0+" /> | `whathitme:hidden` | |

The mod id of a damage type is found from the vanilla type list, the jar of the damage source class, a `modid.` prefix in the type name, or (once per new type) the mod class that dealt the damage.

For projectiles the attacker is the shooter; the projectile is shown as "via Arrow". The attacker's entity id is kept separately, so a [match key](/modpack/match-keys) like `minecraft:arrow @minecraft:skeleton` can tell skeleton arrows from player arrows.

::: tip Damage type names differ between versions
Older versions use names like `outOfWorld` and `inFire`; 1.19.4+ uses registry names like `out_of_world`. Check the ids with `/whm search` or `/whm dump` on the version of your pack, or use a wildcard (`minecraft:out*world`).
:::

## Properties

| Property | Meaning |
|---|---|
| Bypasses Armor | Armor does not reduce it |
| Bypasses Magic | Armor, enchantments and effects do not reduce it (absolute) |
| Magic | Magic damage |
| Fire | Fire damage |
| Explosion | Explosion |
| Projectile | Projectile |
| Through Shield | Hit while blocking with a shield |
| Ignores I-Frames | A fresh hit within the invulnerability time of the previous one |
| Bypasses Creative | Also hurts in creative mode |

## Badges

| Badge | Meaning |
|---|---|
| `☠` | Killing blow |
| `[Invisible]` | The attacker was invisible |
| `[Totem]` | The hit triggered a Totem of Undying |
| `[Heavy Hit]` | The hit took at least `heavyHitThreshold` of max health |
| `[⚡ Reflected / Thorns]` | Thorns, or damage from an entity you hit within 50 ms |
| `[⚠ Broken: <armor>]` | An armor piece broke |
| `[Localized / Vital Hit (...)]` | Death with total health above zero (body-part health mods) |
| `[Client Est.]` | Estimated by the client (server without the mod) |
| `x12 hits` | Merged repeated damage |

## Automatic tips

Without a custom tip, the mod picks a tip from the damage type and its properties: fall, fire, lava, drowning, void, explosion, projectile, bypasses armor, absolute, magic, wither, invisible attacker, melee, player, reflected damage and more. Replace them per damage id with [`hintMap`](/modpack/names-and-tips), or turn them off with `enableAutoHints=false`.
