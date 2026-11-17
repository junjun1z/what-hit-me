# Match keys

Every place that picks damage — `renameMap`, `hintMap`, `ignoredDamageIds`, `defaultIgnoredDamageIds`, `deathTriggers`, `deathMessages` and `rules` — uses the same **match key** syntax.

## The simplest key: a damage id

```json
"minecraft:lava": "..."
"mob:minecraft:zombie": "..."
```

See [Damage ids](/reference/damage-ids) for how ids are built.

## Composite keys

Add conditions to narrow the match. Write them in any order, separated by spaces or `&`:

| Condition | Matches | Example |
|---|---|---|
| `<damage id>` | The damage id | `minecraft:arrow`, `mob:minecraft:zombie` |
| `@<entity id>` or `@mob:<entity id>` | The attacker's entity id (`@entity:` works too) | `@minecraft:skeleton` |
| `@item:<item id>` | The item in the attacker's main hand | `@item:minecraft:iron_sword` |
| `@dim:<dimension id>` | The dimension where it happened | `@dim:minecraft:the_nether` |

**All conditions must match.**

```json
{
  "renameMap": {
    "mob:minecraft:zombie @item:minecraft:iron_sword": "Armed Zombie",
    "minecraft:arrow @minecraft:skeleton": "Skeleton Sniper",
    "minecraft:lava @dim:minecraft:the_nether": "Nether Lava"
  }
}
```

### Writing conditions without a space

A condition starting with `@` can follow another one directly:

```
*@twilightforest:lich                         any damage from the Lich
minecraft:arrow@twilightforest:skeleton_druid that mob's arrows
```

### Dimensions

`@dim:the_nether` (without namespace) also matches `minecraft:the_nether`.

## Wildcards

Every condition may use `*` (any text) and `?` (one character). Matching is case-insensitive.

| Key | Matches |
|---|---|
| `*@twilightforest:lich` or `@twilightforest:lich` | Every hit by the Lich, whatever damage id its attacks use |
| `@twilightforest:*` | Any attacker from Twilight Forest |
| `mob:lycanitesmobs:*` | Melee attacks of every Lycanites Mobs creature |
| `*acid*` | Every damage id containing "acid" (useful for ids a mod builds at run time) |
| `minecraft:arrow @twilightforest:skeleton_druid` | Only that mob's arrows |
| `*` | Everything (lowest priority catch-all) |

## Which key wins?

For `renameMap`, `hintMap` and `deathMessages`, when several keys match the same hit, the **most specific** one wins:

1. The key with **more conditions** wins. A bare `*` does not count as a condition.
2. On a tie, the key with **fewer wildcard conditions** wins.

Example, all in one `hintMap`:

| Key | Conditions | Wins for the Lich's bolt? |
|---|---|---|
| `*` | 0 | only if nothing else matches |
| `@twilightforest:lich` | 1 | |
| `twilightforest:lich_bolt @twilightforest:lich` | 2 | **yes** |

So you can write a general tip for a mob and a special tip for one of its attacks, and `*` as a fallback for everything.

::: warning Rules are different
[Progression rules](/modpack/rules) do **not** use specificity. They are checked from top to bottom and the **first** matching rule wins. Put special rules before general ones.
:::

Other places:

- `ignoredDamageIds` / `defaultIgnoredDamageIds`: hidden when **any** key matches.
- `deathTriggers`: **every** matching trigger runs, in file order.

## Finding the right ids

| Need | Use |
|---|---|
| Damage id of a hit | Hover text `ID: ...` or `[Copy ID]` |
| Entity ids, damage ids of a mod | `/whm search @modid` |
| Everything at once | `/whm dump` writes a CSV and a ready-to-paste template |
| Test a key | `/whm test <damage_id> [entity] [item] [dim]` |
