# Locked features & permissions

## `lockedFeatures`

A list of config keys that **nobody** can change from the settings screen or `/whm config`, not even in single player.

```json
{
  "lockedFeatures": [
    "showInternalDamageId",
    "includeCoordsInCopy",
    "templates.summary"
  ]
}
```

- Keys are case-insensitive. Any key of `whathitme-common.cfg` or `whathitme-client.cfg` can be locked.
- Locked keys are drawn disabled in the settings screen; `/whm config <key> <value>` answers that the key is locked.
- Lists from several pack files are added together.

### What a lock does to a display switch

| Pack sets | Player sets | Result |
|---|---|---|
| `true` in `whathitme-common.cfg`, key locked | `false` | **Shown.** The player's own value is ignored. |
| `false` in `whathitme-common.cfg`, key locked | anything | Hidden for everyone |
| `false` in `whathitme-common.cfg`, not locked | anything | Hidden for everyone (the two-file rule) |
| `true`, not locked | `false` | Hidden for that player |

So: to **force something on**, lock it. To **force it off**, set it to `false` in `whathitme-common.cfg` (locking it too keeps players from even trying).

### Locking templates <Badge type="tip" text="1.3.0+" /> {#locking-templates}

| Entry | Effect |
|---|---|
| `templates` | Players' personal templates are ignored for every output |
| `templates.hover`, `templates.line`, `templates.summary`, `templates.alert`, `templates.copy` | Only that output |

Locked templates are shown read-only in the [template editor](/modpack/template-editor).

## Permissions

What Hit Me? has no permission nodes. It uses the vanilla operator level:

| Who | Can |
|---|---|
| Everyone | `history`, `last`, `snapshot`, `effects`, `stats`, `search`, `togglechat`, personal `ignore`, personal `/whm config`, view common `/whm config`, `/whm template` |
| **Op** (permission level 2, or single player with cheats) | `rename`, `sethint`, `inspect`, `test`, `dump`, `reload`, change common `/whm config`, `ignore` on the server list |
| Single player with cheats only | "Save to pack" in the template editor |
| The server console | Everything server-side; death triggers and rule commands run with console permission |

::: info Locks beat op
A key in `lockedFeatures` cannot be changed by anyone through the screen or commands, operators included. Edit the file and run `/whm reload` instead.
:::
