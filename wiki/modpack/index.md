# Modpack makers: overview

## Why include What Hit Me?

- **Fewer "what killed me?" questions** in your Discord and issue tracker. Players see the attacker, its mod and the damage type themselves.
- **Guide players through your pack's dangers** with your own names and tips, matched by damage type, attacker, weapon and dimension.
- **Keep secrets until players earn them** <Badge type="tip" text="1.3.0+" />: show `???` and no tip until the boss quest is done.
- **Safe to add late:** no dependencies, no world data, no blocks or items, nothing in chat on join. Remove it at any time.
- **MIT license:** include it in any pack without asking.

## What you can customize

| Goal | Tool | Page |
|---|---|---|
| Give damage a better name | `renameMap` | [Names, tips & ignored ids](/modpack/names-and-tips) |
| Write your own tips | `hintMap` | [Names, tips & ignored ids](/modpack/names-and-tips) |
| Hide noise (cactus, modded ticks) | `defaultIgnoredDamageIds` | [Names, tips & ignored ids](/modpack/names-and-tips#hiding-damage) |
| Match by attacker, weapon, dimension | Composite match keys | [Match keys](/modpack/match-keys) |
| Turn features off for everyone | `whathitme-common.cfg` | [Configuration files](/reference/configuration) |
| Stop players from changing a setting | `lockedFeatures` | [Locked features & permissions](/modpack/locked-features) |
| Run a command when someone dies | `deathTriggers` | [Death triggers](/modpack/death-triggers) |
| List every damage id of your pack | Scanner, `/whm dump` | [Scanner, dump & /whm test](/modpack/scanner) |
| Rewrite the panel, hover, summary, alerts, copy text <Badge type="tip" text="1.3.0+" /> | `templates` | [Templates](/modpack/templates) |
| Replace vanilla death messages <Badge type="tip" text="1.3.0+" /> | `deathMessages` | [Custom death messages](/modpack/death-messages) |
| Change names, tips and hidden fields with progress <Badge type="tip" text="1.3.0+" /> | `rules` | [Progression rules](/modpack/rules) |
| Require the mod on every client <Badge type="tip" text="1.3.0+" /> | `requireClientMod` | [requireClientMod](/modpack/require-client-mod) |

## Quick start (5 minutes)

1. Start the game (or server) once with the mod. It creates `config/whathitme/pack/` with commented examples.
2. Die to something in your pack, hover the killing blow and click `[Copy ID]`. Or run `/whm dump` to list every id.
3. Create `config/whathitme/pack/mypack.json`:

   ```json
   {
     "renameMap": {
       "minecraft:outOfWorld": "&5The Abyss"
     },
     "hintMap": {
       "mob:minecraft:creeper": "Creepers hiss before exploding - sprint away!"
     }
   }
   ```

4. Run `/whm reload` (op). It reports how many pack files were read and any file with errors.
5. Check the result without dying: `/whm test mob:minecraft:creeper`.

## Where settings come from

```
Player's own settings (whathitme-client.cfg, whathitme-client-templates.json)
        on top of
Server / pack .cfg (whathitme-common.cfg)
        on top of
Pack folder JSON (config/whathitme/pack/**/*.json)
        on top of
Built-in defaults and automatic tips
```

- **Display switches:** an element is shown only when it is on in both `.cfg` files.
- **Renames / tips:** personal entries win over the pack's; `.cfg` entries win over pack JSON; any custom tip wins over automatic tips.
- **Templates:** personal > pack > built-in, unless the pack locks them.
- **Progression rules** win over `renameMap` / `hintMap`; with `"enforce": true` (or `hide`) they also win over personal renames, tips and templates.

## Shipping it in a modpack

- Ship `config/whathitme/whathitme-common.cfg` and your `config/whathitme/pack/` folder.
- Do **not** ship `whathitme-client.cfg` unless you want to force a panel layout; players change it.
- You can ship `.cache/` or leave it out; it is rebuilt automatically.
- On a dedicated server, the server's files are what counts. They are sent to every modded client.

::: tip Translations
Every name, tip, template line and death message can be a translation key: `lang:mypack.hint.lava`. Add the keys to a resource pack (or your pack's KubeJS / resource loader assets) and every player sees their language.
:::
