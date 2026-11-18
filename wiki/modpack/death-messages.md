# Custom death messages <Badge type="tip" text="1.3.0+" />

Replace the vanilla death message ("Steve was slain by Zombie") with your own text for chosen deaths. Pack makers only; **off by default** — with no entries nothing changes.

## Writing them

Add `deathMessages` to any pack file (there is a commented sample in `pack/deathmessages/_example.json`):

```json
{
  "deathMessages": {
    "mekanism:acid": "{player} dissolved in acid",
    "*@twilightforest:lich": "{player} was outsmarted by the Lich",
    "minecraft:lava @dim:minecraft:the_nether": "&c{player} became part of the Nether"
  }
}
```

- **Keys** are [match keys](/modpack/match-keys), with the same "most specific wins" priority as `renameMap`.
- **Values** are one-line [templates](/modpack/templates): every template variable, `{var|fallback}`, `&` colors and `lang:` keys.
- `lang:` keys: on Forge / NeoForge each modded client translates the key (its language and resource packs). On Fabric 1.19.4+ the server sends the key as a translation, so every client with the key (a resource pack, a mod) sees its own text, also without What Hit Me?; clients without the key get the English text the server knows, or the key itself when the server does not know it either. Older Fabric versions send the server's English text. In translations use `%1$s` (player), `%2$s` (attacker) and `%3$s` (damage name) like vanilla death messages: `{variables}` are filled only where What Hit Me? renders the text itself.
- Deaths without a matching key keep the vanilla message. There is no automatic generation for other deaths.

Useful variables: `{player}`, `{name}`, `{attacker_name}`, `{weapon}`, `{damage_name}`, `{dimension}`, `{death_message}` (the vanilla text).

```json
"mob:minecraft:zombie @item:*sword*": "{player} was cut down by {attacker_name} wielding {weapon|a blade}"
```

## Support matrix

<!-- TODO media: screenshot: a custom death message in chat and on the death screen (one Fabric, one Forge) -->

| Server | Players with the mod | Players without the mod | Death screen | Server log |
|---|---|---|---|---|
| **Fabric** (every version) | Custom message | Custom message | Custom message | Custom message |
| **Forge / NeoForge** (every version) | Custom message (the client replaces the vanilla chat line) | **Vanilla message** | Custom message (Forge 1.7.10: the death screen shows no message) | `[What Hit Me?] Death message: ...` line |

Why the difference? Fabric lets the mod change the message where the game builds it, so everyone gets it. Forge and NeoForge have no event for that, so the server sends the custom text to modded clients, which replace the matching chat line and the death screen text. Players without the mod keep the vanilla message.

::: warning Forge / NeoForge servers
If it matters that **nobody** sees the vanilla message (for example because a rule hides the killer), turn on [`requireClientMod`](/modpack/require-client-mod). Otherwise the server logs a warning on start and on `/whm reload`.
:::

The death recap (`/whm last`, the panel) quotes the message the player actually saw.

## Hidden death messages from rules

When a [progression rule](/modpack/rules) hides `death_message` and no `deathMessages` entry matches, a neutral built-in message is used:

| Killer | Message |
|---|---|
| An entity | `{player} was killed by {name}` — `{name}` is the rule's name, e.g. `???` |
| No entity | `{player} died` |

These are translated for players with the mod (10 languages). On Fabric they are sent as translations with an English fallback on 1.19.4+, and in English on older Fabric versions. Players without the mod see English.

A matching `deathMessages` entry always wins over the built-in hidden message.

## Testing

```
/whm test mekanism:acid
/whm test minecraft:generic twilightforest:lich
```

`/whm test` shows the death message that would be used. In single player with cheats you can also edit and preview them in the [template editor](/modpack/template-editor#death-messages-tab).
