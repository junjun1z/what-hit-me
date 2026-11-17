# Cookbook

Ready-to-copy pack files. Put each one into `config/whathitme/pack/` (any file name ending in `.json`), run `/whm reload`, and check it with `/whm test`.

::: tip Damage ids differ between versions
The examples use common vanilla ids. Some names differ between Minecraft versions (`minecraft:outOfWorld` before 1.19.4, `minecraft:out_of_world` after). Check yours with `/whm search` or `/whm dump`.
:::

## Names and tips

### Friendly names for vanilla damage

```json
{
  "renameMap": {
    "minecraft:outOfWorld": "&5The Void",
    "minecraft:inWall": "Suffocation",
    "minecraft:cramming": "Too Many Entities",
    "minecraft:flyIntoWall": "Kinetic Energy"
  }
}
```

### One tip for a whole mod, a better one for its boss

The most specific key wins, so the boss tip beats the mod-wide tip:

```json
{
  "hintMap": {
    "@twilightforest:*": "Twilight Forest mobs hit hard. Bring better armor than in the Overworld.",
    "@twilightforest:lich": "Hit the Lich's shields first, then attack it directly.",
    "twilightforest:lich_bolt @twilightforest:lich": "Reflect the Lich's bolts with a sword swing!"
  }
}
```

### Tips per dimension

```json
{
  "hintMap": {
    "minecraft:lava @dim:minecraft:the_nether": "Carry Fire Resistance potions in the Nether.",
    "minecraft:fall @dim:minecraft:the_end": "Bring Slow Falling or a water bucket to the End.",
    "* @dim:mypack:deep_dark_dimension": "Nothing here is fair. Bring friends."
  }
}
```

### Different names for armed mobs

```json
{
  "renameMap": {
    "mob:minecraft:zombie @item:minecraft:iron_sword": "Armed Zombie",
    "mob:minecraft:zombie @item:*diamond*": "&bWell-Equipped Zombie",
    "minecraft:arrow @minecraft:skeleton": "Skeleton Sniper"
  }
}
```

### Translated tips

```json
{
  "hintMap": {
    "minecraft:lava": "lang:mypack.hint.lava",
    "mob:minecraft:creeper": "lang:mypack.hint.creeper"
  }
}
```

Then in a resource pack: `assets/mypack/lang/en_us.json`, `ja_jp.json`, ... with `"mypack.hint.lava": "..."`.

## Hiding noise

```json
{
  "defaultIgnoredDamageIds": [
    "minecraft:cactus",
    "minecraft:sweetBerryBush",
    "*radiation_tick*"
  ]
}
```

## Locking settings

Always show tips and the mod name, never let players post coordinates:

```json
{
  "lockedFeatures": ["showHints", "showSourceModName", "includeCoordsInCopy"]
}
```

Plus in `whathitme-common.cfg`: `includeCoordsInCopy=false` (a lock alone keeps the pack's value, so set the value you want there).

## Death triggers

### Consolation bread after the first creeper death

```json
{
  "deathTriggers": [
    { "match": "mob:minecraft:creeper", "command": "give {player} minecraft:bread 4", "oncePerPlayer": true }
  ]
}
```

### Count deaths per cause in scoreboards

```json
{
  "deathTriggers": [
    { "match": "minecraft:lava", "command": "scoreboard players add {player} deaths_lava 1" },
    { "match": "mob:*", "command": "scoreboard players add {player} deaths_mob 1" }
  ]
}
```

### Server announcement with the killer's name

```json
{
  "deathTriggers": [
    { "match": "@twilightforest:*", "command": "say {player} fell to {attacker_name} ({damage} HP hit)", "cooldownSeconds": 30 }
  ]
}
```

## Templates <Badge type="tip" text="1.3.0+" />

### Minimal respawn summary

```json
{
  "templates": {
    "summary": [
      "&c☠ {killing_blow} &7({damage} ❤ / {total} ❤ in {total_seconds}s)",
      "&a✎ {tip}",
      "&b{recap_link}"
    ]
  }
}
```

### Compact panel lines with % of max health

```json
{
  "templates": {
    "line": ["&4{killing}&8{time_ago} &f{name} &c-{damage} ❤ &7{max_hp_percent|?}% &e{badges}"]
  }
}
```

### Short hover text

```json
{
  "templates": {
    "hover": [
      "&l{name}",
      "&c{damage} ❤ &7(before armor {raw_damage} ❤, {mitigation}% blocked)",
      "&f{attacker_name} &7{weapon}",
      "&9{damage_mod}",
      "&a{tip}"
    ]
  }
}
```

### Discord-friendly copy text

````json
{
  "templates": {
    "copy": [
      "**{player} died** at {time} in {dimension}",
      "> {death_message}",
      "```",
      "{timeline}",
      "```",
      "Last {total_seconds}s: {total} hearts in {total_hits} hits",
      "{equipment}"
    ]
  }
}
````

### Keep players' templates out of the summary

```json
{
  "lockedFeatures": ["templates.summary"]
}
```

## Custom death messages <Badge type="tip" text="1.3.0+" />

```json
{
  "deathMessages": {
    "*@twilightforest:lich": "{player} was outsmarted by the Lich",
    "minecraft:lava @dim:minecraft:the_nether": "{player} took a bath in Nether lava",
    "mob:minecraft:zombie @item:*sword*": "{player} was cut down by {attacker_name} wielding {weapon|a blade}"
  }
}
```

On Forge / NeoForge servers, only players with the mod see these. See the [support matrix](/modpack/death-messages#support-matrix).

## Progression rules <Badge type="tip" text="1.3.0+" /> {#progression-rules}

### ??? until the boss quest is done (FTB Quests)

```json
{
  "rules": [
    {
      "match": "*@twilightforest:lich",
      "unless": ["ftbquests:1A2B3C4D5E6F7081"],
      "name": "&k???",
      "hint": "",
      "hide": ["damage_id", "attacker", "mod", "weapon", "death_message"]
    }
  ]
}
```

Replace `1A2B3C4D5E6F7081` with the id of your boss quest. On Forge / NeoForge, also set [`requireClientMod=true`](/modpack/require-client-mod) so nobody sees the killer in the vanilla death message.

### Hide a whole dimension until it is unlocked (Game Stages)

```json
{
  "rules": [
    {
      "match": "* @dim:mypack:the_abyss",
      "unless": ["stage:abyss"],
      "name": "Something in the dark",
      "hint": "You are not ready for this place yet.",
      "hide": ["attacker", "mod", "weapon", "environment", "position", "death_message"]
    }
  ]
}
```

### Better tips after a few deaths (score)

Count deaths with a vanilla objective (`/scoreboard objectives add deaths deathCount`), then:

```json
{
  "rules": [
    { "match": "minecraft:lava", "if": ["score:deaths>=3"], "hint": "&eStill dying in lava? Carry a water bucket and Fire Resistance!" }
  ]
}
```

### Reveal after an advancement, with a special first-death message

```json
{
  "rules": [
    {
      "match": "*@minecraft:warden",
      "unless": ["advancement:minecraft:adventure/avoid_vibration"],
      "name": "&8The Thing Below",
      "hint": "Sneak. It hears everything.",
      "hide": ["attacker", "mod"],
      "commands": ["tellraw {player} {\"text\":\"You met something terrible. Sneak next time.\",\"color\":\"dark_gray\"}"],
      "oncePerPlayer": true
    }
  ]
}
```

### Special rule first, general rule second

Rules use the **first** match, so order them from special to general:

```json
{
  "rules": [
    { "match": "twilightforest:lich_bolt @twilightforest:lich", "unless": ["tag:lich_known"], "name": "A strange bolt", "hint": "Try swinging your sword at it." },
    { "match": "*@twilightforest:lich", "unless": ["tag:lich_known"], "name": "???", "hint": "", "hide": ["attacker", "mod"] }
  ]
}
```

### A death trigger only for new players

```json
{
  "deathTriggers": [
    { "match": "*", "command": "give {player} minecraft:cooked_beef 8", "unless": ["tag:veteran"], "cooldownSeconds": 600 }
  ]
}
```
