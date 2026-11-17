# Client, server or both

What Hit Me? works in every combination. Single player always counts as "client and server", because the integrated server runs the mod too.

## Which mode do I get?

| Installed on | What players get |
|---|---|
| **Client and server** | Accurate data from the server: raw damage before armor, mitigation %, absorption, weapon and enchantments, owner, shield and i-frame flags, armor, environment. Text in the player's own language, clipboard buttons, the death screen panel. The server's `whathitme-common.cfg` and pack folder apply to everyone. |
| **Client only** (server without the mod, or a vanilla server) | Client estimate: the client watches its own health and, only when it drops, guesses the cause (fall, lava, fire, drowning, suffocation, void, nearby mob, projectile, explosion, poison, wither, starvation). Entries are marked `[Client Est.]`. `/whm inspect` is not available. Progression rules are never applied. |
| **Server only** (players without the mod, even plain Forge clients) | English chat sent by the server: the respawn summary, totem / heavy-hit alerts and every `/whm` command. `[Copy ID]` puts the id into the chat box instead of the clipboard. `togglechat` and `ignore` are stored per player on the server. |

On a server with the mod, each player gets the best mode their client supports: players with the mod get the full experience, players without it get the English chat.

## What the server controls

When the server has the mod, it sends its `whathitme-common.cfg` (display switches, ignored ids, renames, tips, `summarySeconds`) and its pack definitions to every modded client on login, after `/whm reload` and after a config change. This **replaces** the client's own common file while connected.

- A display element is shown only when it is on in **both** files. The server can turn something off for everyone; each player can turn off more for themselves.
- Features the server turned off (common config or `lockedFeatures`) are removed from the packets the server builds, so a modified client cannot show them.
- Players can still add personal renames and tips on top of the server's (unless a [progression rule](/modpack/rules) enforces its own).

## Version mixing

- Clients of 1.0 / 1.1 / 1.2 keep working with a 1.3 server and the other way round. Features the older side does not know are simply not used.
- 1.3.0 uses network protocol 3. Pack templates, custom death messages and rule results are sent only to clients that said hello with protocol 3; older clients get the stripped records (hidden fields are still removed).

## Players without the mod and 1.3.0 features <Badge type="tip" text="1.3.0+" />

| Feature | Players with the mod | Players without the mod |
|---|---|---|
| [Templates](/modpack/templates) | Pack templates, personal templates | Pack templates in the server's English chat |
| [Custom death messages](/modpack/death-messages) | Replaced | Fabric server: replaced. Forge / NeoForge server: vanilla message |
| [Progression rules](/modpack/rules) | Applied (server decides) | Applied to the chat the server sends; the vanilla death message on Forge / NeoForge is not changed |
| [requireClientMod](/modpack/require-client-mod) | Join normally | Disconnected with a readable message |

::: warning Forge / NeoForge and hidden death messages
Forge and NeoForge have no event to change the vanilla death message. If your rules hide the death message (or you use `deathMessages`), players without the mod still see the vanilla text there. Set [`requireClientMod=true`](/modpack/require-client-mod) if that matters. The server logs a warning on start and on `/whm reload` in that case.
:::
