# Chat summary & alerts

## Respawn chat summary

After you respawn, a short summary appears in **your own chat only** (other players do not see it):

![Respawn chat summary](/images/respawn-chat.webp)

![Hovering a line of the summary in the chat screen](/images/chat-hover.webp)

```
========== [ What Hit Me? ] ==========
☠ Killing Blow: Creeper (Explosion) [26.4 ❤] (Minecraft) [Copy ID] [Mute]
⚠ Properties: Explosion
Σ Last 10s Total: 35 ❤ taken (8 hits)
✎ Tip: Blast Protection reduces explosions a lot. Keep your distance from creepers and TNT.
➜ [Click here for full death recap] (/whm last)
✖ To disable this message, run /whm togglechat
=====================================
```

| Part | What you can do |
|---|---|
| Killing blow | Hover it for the [hover details](/player/death-screen#hover-details). |
| `[Copy ID]` | Copies the damage id to the clipboard (players without the mod: puts it into the chat box). Useful for bug reports and for [pack files](/modpack/names-and-tips). |
| `[Mute]` | Hides this damage id from now on (same as `/whm ignore <id>`). Click again in `/whm` to show it again. |
| `[Click here for full death recap]` | Runs `/whm last`: the whole recap with every hit. |
| `⌂ Surroundings` | For damage without an attacker: block / fluid and items in hand. |

**Turn it off:** `/whm togglechat` (toggles on / off). Players without the mod can use it too; the server remembers it per player. The last line can be hidden with `showToggleChatNotice=false`.

## Alerts and snapshots

Some moments are saved as a **snapshot** even when you did not die, and an alert appears in chat:

![Totem alert](/images/totem.png)

| Alert | When |
|---|---|
| `⚠ [What Hit Me?] A Totem of Undying saved you! Last hit: Zombie 17.6 ❤ [Details]` | A Totem of Undying saved you. |
| `⚠ [What Hit Me?] Lethal damage was prevented! Last hit: ... [Details]` | Another mod cancelled your death or restored your health right after it. |
| `⚠ [What Hit Me?] Heavy hit: Wither took 16 ❤ at once.` | One hit took at least 80 % of your max health (`heavyHitThreshold = 0.8`). |

- Click `[Details]` or run `/whm snapshot` to see the recap of that moment. `/whm snapshot 2` shows the second newest, and so on.
- Up to 5 snapshots per player are kept (`maxSnapshots`).
- Switches: `enableTotemChatAlert` (totem and prevented death), `enableHeavyHitSnapshot` (heavy hits).

## Copy Recap

`[Copy]` on the death screen and `[Copy Recap]` in `/whm last` copy the recap as **Markdown**, ready for Discord or a bug report. It contains the death message, the killing blow, every hit, the totals, and an equipment block:

```markdown
## Equipment
- Dimension: minecraft:overworld
- Position: 120.5 / 64.0 / -33.2
- Max health: 20.0 (10 ❤)
- Armor:
  - Head: Iron Helmet [Protection II]
  - Chest: Diamond Chestplate (broken)
  - Legs: (empty)
  - Feet: Iron Boots
- Effects:
  ...
```

| Switch | Effect |
|---|---|
| `includeEquipmentInCopy` | Adds dimension, max health, the four armor pieces (enchantments, mod, broken state) and active effects |
| `includeCoordsInCopy` | Adds the death position (needs `includeEquipmentInCopy`) |
| `showActionButtons` | Shows the clickable `[Copy ID]`, `[Mute]` and `[Copy Recap]` buttons at all |

::: tip Privacy on servers
If you do not want players to post coordinates, set `includeCoordsInCopy=false` in `whathitme-common.cfg` and add it to [`lockedFeatures`](/modpack/locked-features).
:::

Since 1.3.0 the summary, the alerts and the copied text can be rewritten with [templates](/modpack/templates) <Badge type="tip" text="1.3.0+" />.
