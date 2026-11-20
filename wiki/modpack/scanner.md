# Scanner, dump & /whm test

Writing names and tips needs damage ids. These tools list them **before anyone takes that damage**, and let you check your work without dying.

## Damage id scanner

| Minecraft | How ids are found |
|---|---|
| 1.7.10 - 1.19.3 | After a cache miss (first start, mod list changed, `/whm reload`) a background thread reads the class files of the mod jars (without loading classes) and collects the string ids passed to damage source constructors, including subclasses and setter calls right after the constructor. Nested jars are read one level deep. |
| 1.19.4+ | Every key of the game's damage type registry is listed, with flags from its tags. No scan needed. |

The results are stored under `scan` in `config/whathitme/.cache/catalog_cache.json` and show up in:

- `/whm search #scanned`
- Tab completion of `/whm test`
- `/whm dump` (column `source`)

### Catalog cache

Damage types, mobs and effects are scanned once and saved with a SHA-256 of the sorted `modid:version` list. On the next start with the same mods, the list is loaded from the cache. A different mod list, or `/whm reload`, rescans.

## `/whm dump`

Op only. Writes two files:

### 1. `config/whathitme/.cache/whathitme_dump.csv`

Every damage type, mob and effect, one per line. Open it in a spreadsheet.

| Column | Contents |
|---|---|
| `category` | damage, mob or effect |
| `id` | The id to use in pack files |
| `name`, `mod_id`, `mod_name` | Display name and mod |
| `properties` | Bypasses Armor, Fire, ... |
| `times_seen` | How often it happened in this world |
| `custom_name`, `custom_tip`, `ignored` | What your configuration does with it now |
| `source` | `seen` (happened in game), `scanned` (scanner) or `registry` (1.19.4+ registry) |

### 2. `config/whathitme/hint_template.cfg`

Ready-to-paste `S:hintMap <...>` and `S:renameMap <...>` blocks with every known damage id and `mob:<id>` for every registered mob, each with an **empty value**:

```
S:hintMap <
    minecraft:lava =
    mob:minecraft:creeper =
    mob:twilightforest:lich =
 >
```

Paste the blocks into `whathitme-common.cfg` and fill in what you need, a bit at a time. Empty values are ignored, so the unfilled lines do nothing.

## `/whm test`

Op only. Builds a dummy killing blow and shows what players would see.

```
/whm test <damage_id> [entity_id] [item_id] [dim_id] [--as <player>] [--assume <condition>]...
```

| Argument | Meaning |
|---|---|
| `damage_id` | The damage id, for example `minecraft:lava` or `mob:minecraft:zombie` |
| `entity_id` | The attacker (`-` = none; for `mob:` ids the entity is taken from the id) |
| `item_id` | The attacker's main hand item (`-` = none) |
| `dim_id` | The dimension (default: yours) |
| `--as <player>` <Badge type="tip" text="1.3.0+" /> | Evaluate [rules](/modpack/rules) with that online player's progress |
| `--assume <condition>` <Badge type="tip" text="1.3.0+" /> | Treat a condition as fulfilled (`!cond` = as not fulfilled). Repeatable. |

It shows:

1. the matching rule, the hidden fields and whether the rule is enforced <Badge type="tip" text="1.3.0+" />,
2. the resulting **name** and **tip**,
3. the **death triggers** and rule commands that would run, with placeholders filled in (they are **not** run),
4. the **death message** that would be used <Badge type="tip" text="1.3.0+" />,
5. a preview of the **respawn chat summary**.

Examples:

```
/whm test minecraft:lava
/whm test mob:minecraft:zombie - minecraft:iron_sword
/whm test minecraft:arrow minecraft:skeleton
/whm test twilightforest:lich_bolt twilightforest:lich - twilightforest:twilight_forest
/whm test minecraft:generic --as Steve
/whm test minecraft:generic --as Steve --assume advancement:minecraft:story/mine_diamond --assume !tag:veteran
```

::: tip Test before you ship
After every change to your pack files: `/whm reload`, read the warnings, then `/whm test` the keys you changed. With rules, test both "before" and "after" with `--assume`.
:::

![/whm test for a wither skeleton --as Steve: the matching rule, what it hides and the chat preview](/images/whm-test.webp)
