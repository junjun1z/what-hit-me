# Death screen panel

When you die, a panel appears next to the vanilla death screen. It needs the mod on your **client** (with or without the mod on the server).

![Death screen panel](/images/death-screen.png)

## What the panel shows

From top to bottom:

1. **Header** `❤ What Hit Me?` with the `[Copy]` and `[-]` buttons.
2. **Killing blow**: the name of what killed you and the damage of that hit, for example `☠ Creeper (Explosion) [26.4 ❤]`.
3. **Properties** of the killing blow, such as `Explosion`, `Fire`, `Magic`, `Bypasses Armor`.
4. **Timeline**: every hit of the last 30 seconds (`recapSeconds`), newest first. Each line has the time before death (`-1.3s`), the name, the damage in hearts and the hit counter of merged hits (`x2`).
5. **Last 10s Total**: all damage of the last 10 seconds (`summarySeconds`) and the number of hits.
6. **Tip**: a counter-measure for the killing blow.

Damage is shown in **hearts** (`❤`), one heart = 2 health points.

## Using the panel

| Action | How |
|---|---|
| Move the panel | Drag the header with the mouse. The position is saved when you release the button. |
| Minimize / restore | Click `[-]` / `[+]`. The state is saved. |
| Scroll the timeline | Mouse wheel over the panel (more lines than `maxVisibleLines`). |
| See the details of a hit | Hover the line. |
| Copy the recap | Click `[Copy]`. See [Copy Recap](/player/chat#copy-recap). |

The panel shrinks to fit at large GUI scales or in small windows, so it never covers the Respawn / Title screen buttons.

::: tip Change size and position
`scale`, `backgroundOpacity`, `maxVisibleLines` and `anchorPoint` are in `whathitme-client.cfg` and in the [settings screen](/player/settings). Or use commands, for example to anchor the panel at the top right and make it bigger:

```
/whm config anchorPoint TOP_RIGHT
/whm config scale 1.0
/whm config maxVisibleLines 12
```
:::

## Hover details

Hover any line of the panel, of `/whm` or of the chat summary to see everything that is known about the hit:

![Hover details in /whm history](/images/history.png)

| Line | Meaning | Switch |
|---|---|---|
| Name (bold) | Custom name, attacker or damage type | |
| `ID: mob:minecraft:zombie` | Internal [damage id](/reference/damage-ids) | `showInternalDamageId` |
| `Damage: 3.8 ❤ (7.6 HP)` | Damage actually taken | `showDamageAmount` |
| `(30% Max HP)` | Share of your max health at that moment | `showMaxHpPercentage` |
| `Before armor: 7 ❤` / `Mitigated: 46%` | Damage before armor and how much armor, enchantments and effects blocked | `showMitigationDetails` |
| `Absorbed` | Yellow hearts (absorption) used | `showMitigationDetails` |
| `Health after: 3.8 / 10 ❤` | Your health after the hit | `showMitigationDetails` |
| `Merged hits: 2` | Repeated hits merged into this line | `showSmartMergeHitCount` |
| `Attacker`, `via Arrow` | Who attacked; the projectile when it differs | `showAttackerName` |
| `Weapon: Iron Sword [Sharpness III, Fire Aspect I]` | Attacker's main hand item and enchantments | `showAttackerWeapon` |
| `Distance: 9.2m` | Distance to the attacker | `showAttackerDistance` |
| `Owner` | Owner of a pet, turret, summon or projectile | `showEntityOwner` |
| `[Invisible]` | The attacker was invisible | `showAttackerInvisibility` |
| `Properties` | Fire, Magic, Explosion, Projectile, Bypasses Armor, ... | `showDamageProperties` |
| `Mod: Minecraft` | Mod that added the damage or the attacker | `showSourceModName` |
| Environment | For damage without an attacker: block / fluid at your feet and body, items in both hands, each with its mod | `showEnvironmentContext` |
| `Tip` | Counter-measure | `showHints` |

In a modpack the mod name tells you at once where a creature comes from:

![Hover details in a modpack](/images/modpack-death-screen.png)

## Special lines and badges

| You see | It means |
|---|---|
| `x12 hits` | Repeated damage from the same source within 3 seconds (`mergeWindowSeconds`) was merged into one line. Interleaved damage (poison ticks between mob hits) is merged too. |
| `[⚡ Reflected / Thorns]` | Thorns, or damage from an entity you hit within the last 50 ms. Gets its own tip. |
| `[⚠ Broken: Diamond Chestplate]` | An armor piece broke at this point of the timeline (`showArmorBreakEvents`). |
| `[Localized / Vital Hit (Died with 4.5 ❤ total HP left)]` | You died while your total health was still above zero (body-part health mods such as First Aid). |
| `[Client Est.]` | Estimated by the client because the server does not have the mod. |
| `[Totem]`, `[Heavy Hit]` | The hit triggered a totem / a [heavy-hit snapshot](/player/chat#alerts-and-snapshots). |
| `☠` | The killing blow. |

::: info A death without any damage event
Some mods kill a player directly (for example by setting the health to 0). The recap is still not empty: the killing blow is taken from the death event.
:::

## Turning the panel off

- Just for you: `enableDeathScreenPanel=false` in `whathitme-client.cfg`, or the settings screen.
- For everyone (pack / server): the same key in `whathitme-common.cfg`.

Since 1.3.0 the panel line and the hover text can be rewritten with [templates](/modpack/templates) <Badge type="tip" text="1.3.0+" />.
