# requireClientMod <Badge type="tip" text="1.3.0+" />

A server setting that refuses players whose client does **not** have What Hit Me? (any version).

```
# whathitme-common.cfg, category tracking
B:requireClientMod=true
```

Or in game (op): `/whm config requireClientMod true` (from a modded client: `/whm server config requireClientMod true`).

## What players see

![What a player without the mod sees (the message is always English)](/images/require-client-mod.webp)

A player without the mod is disconnected with:

> This server requires the What Hit Me? mod on your client (any version). Install it and join again.

Only the presence of the mod is checked, not its version. Default: `false`.

## How it works

On every loader and version, the server waits up to **5 seconds** after a player joins for the client's hello packet. A client that sent none is disconnected with the message above. This is one timer task per join, no tick handler. The server log shows `[What Hit Me?] <name> has no What Hit Me? (requireClientMod): disconnected`.

## When to use it

- Your [progression rules](/modpack/rules) hide things (such as the killer) that the **vanilla death message** would reveal, and the server runs **Forge or NeoForge**.
- You use [custom death messages](/modpack/death-messages) on Forge / NeoForge and want everyone to see them.
- Your pack ships the mod anyway and you want the full experience (panel, translations) for everyone.

On **Fabric** servers the death message is replaced for everyone, so this setting is only needed for the reasons in the last point.

## The death message warning

A Forge or NeoForge server logs a warning on start and on `/whm reload` (and shows it in the `/whm reload` output) when:

- a rule hides `death_message`, or there are `deathMessages` entries, **and**
- `requireClientMod` is `false`.

```
On Forge 1.20.1, players without What Hit Me? still see the vanilla death message; set requireClientMod=true to hide it from everyone.
```

The warning is never shown on Fabric.
