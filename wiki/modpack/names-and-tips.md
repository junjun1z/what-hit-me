# Names, tips & ignored ids

## Custom names: `renameMap`

Replaces the name shown for a hit (panel, hover, chat, copy text).

```json
{
  "renameMap": {
    "minecraft:outOfWorld": "&5The Abyss",
    "mob:minecraft:zombie @item:minecraft:iron_sword": "Armed Zombie",
    "*@twilightforest:lich": "&dThe Lich",
    "draconicevolution:chaos": "lang:mypack.damage.chaos"
  }
}
```

## Custom tips: `hintMap`

Replaces the automatic counter-measure tip.

```json
{
  "hintMap": {
    "mob:minecraft:creeper": "Creepers hiss before exploding - sprint away!",
    "minecraft:lava @dim:minecraft:the_nether": "Carry Fire Resistance potions in the Nether.",
    "mob:lycanitesmobs:*": "lang:mypack.hint.lycanites"
  }
}
```

## Values

| You write | Players see |
|---|---|
| `Plain text` | The text |
| `&5The Abyss` | Colored text. `&0`-`&f` colors, `&l` bold, `&n` underline, `&r` reset. The section sign works too. |
| `lang:mypack.hint.lava` | The translation of that key in the player's language (from a resource pack) |
| `mypack.hint.lava` | Also translated, when the value is a known translation key |
| `""` (empty) | Removes an entry of an earlier pack file |

Keys are [match keys](/modpack/match-keys): damage ids, `@entity`, `@item:`, `@dim:` and wildcards.

### Translating names and tips

1. Use `lang:` values in your pack file:

   ```json
   { "hintMap": { "minecraft:lava": "lang:mypack.hint.lava" } }
   ```

2. Add the keys to a resource pack, for example `assets/mypack/lang/en_us.json` and `ja_jp.json`:

   ```json
   { "mypack.hint.lava": "Carry a water bucket." }
   ```

   ```json
   { "mypack.hint.lava": "水バケツを持ち歩こう。" }
   ```

The translation happens on each player's client, so every player sees their own language.

::: warning Players without the mod
The server cannot read your resource pack, so players without the mod (English chat from the server) may see the raw key. If many players join without the mod, use plain text or [requireClientMod](/modpack/require-client-mod).
:::

## Hiding damage

`defaultIgnoredDamageIds` hides damage for everyone: it is not shown in the panel, history or summary.

```json
{
  "defaultIgnoredDamageIds": [
    "minecraft:cactus",
    "minecraft:sweetBerryBush",
    "somemod:*_tick"
  ]
}
```

- Players can hide more for themselves with `/whm ignore <id>` or the `[Mute]` button.
- `ignoredDamageIds` in `whathitme-common.cfg` does the same as the pack key.
- Use `minDamageThreshold` (default 0.05) to drop tiny hits from being recorded at all.

## Other ways to set names and tips

| Where | Scope | Composite keys |
|---|---|---|
| Pack JSON (`pack/*.json`) | Everyone | yes |
| `whathitme-common.cfg` `renameMap` / `hintMap` | Everyone, wins over pack JSON | yes |
| `/whm server rename` / `sethint` (op) | Writes `whathitme-common.cfg` | yes (one key per command) |
| `whathitme-client.cfg`, `/whm rename` on a client | Only that player, wins over everything | yes |
| [Progression rules](/modpack/rules) `name` / `hint` <Badge type="tip" text="1.3.0+" /> | Per player progress, wins over `renameMap` / `hintMap` | yes |

::: tip Start from a full list
`/whm dump` writes `config/whathitme/hint_template.cfg` with every known damage id and every mob, each with an empty value. Paste the blocks into `whathitme-common.cfg` and fill in only what you need; empty values are ignored. See [Scanner, dump & /whm test](/modpack/scanner).
:::
