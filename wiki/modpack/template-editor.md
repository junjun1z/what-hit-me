# Template editor <Badge type="tip" text="1.3.0+" />

An in-game screen to write [templates](/modpack/templates) with a **live preview**. It works on every supported version and loader and needs the mod on the client.

<!-- TODO media: retake: current shot shows test strings ("WHMT"); also a short GIF of typing with the live preview updating -->

![Template editor](/images/template-editor.png)

## Opening it

- `/whm template`
- The **Templates...** button in the [settings screen](/player/settings) (also from the Forge / NeoForge mods list and Mod Menu)

## Step by step

1. **Pick an output** with the tabs at the top: Hover, Line, Summary, Alert, Copy. In single player with cheats there is also **Death msgs**.
2. **Edit the lines.** Each template line is a single-line text field.
   - `+ Line` adds a line; `↑` / `↓` move a line; `✕` deletes it.
   - Usual text editing works: cursor, selection, Ctrl+A / C / V / X, Home / End, Backspace / Delete.
3. **Insert variables** from the list on the right. Click a variable to insert it at the cursor. Hover it (or select it) to see a one-line description at the bottom. Variables are grouped: Damage, Damage type, Attacker, Context, Other, and Script (variables registered by [scripts](/modpack/scripts) on the server, with their description).
4. **Watch the preview** below the lines. It is drawn by the same code as the real output: a tooltip for Hover, a panel line for Line, chat lines for Summary and Alert, a plain text box for Copy. It updates on every keystroke.
5. **Switch the preview data** with **Sample death** / **Latest death**: your latest real death, or a built-in sample death.
6. **Save.**

Unknown variables are shown in red in the text field, so typos are easy to spot.

## Buttons

| Button | What it does | Available |
|---|---|---|
| **Save** | Saves to your personal templates (`whathitme-client-templates.json`) | When the output is not locked |
| **Save to pack** | Writes `config/whathitme/pack/templates/<output>.json` (death messages: `pack/templates/deathmessages.json`) | Single player with cheats |
| **Reset** | Removes your personal template of this output | When the output is not locked |
| **Sample death** / **Latest death** | Switches the preview data | Always |
| **Close** | Leaves the editor | Always |

Opened from the [pack editor](/modpack/pack-editor) (**Edit in the template editor...** of a template entry), the buttons are **Done** (puts the lines back into the pack editor entry, which saves it), **Sample death** / **Latest death** and **Cancel**.

The line under the text fields tells you where the shown template comes from: *Your personal template*, *Modpack template*, or *Built-in output (editing starts from an example)*.

## Locked outputs

If the pack locks an output (`lockedFeatures`: `templates` or `templates.<output>`), the editor shows the pack's template **read-only** with the note *Locked by the modpack (read-only)*. Save and Reset are disabled.

<!-- TODO media: retake: locked output without test strings -->

![A locked output in the template editor](/images/template-editor-locked.png)

The screenshot also shows the syntax: a fallback (`{attacker_owner|nobody}`), decimals (`{damage:2}`), the `mob_` alias (`{mob_name}`) and a `lang:` line.

## Death messages tab

In single player with cheats, the **Death msgs** tab edits the pack's [custom death messages](/modpack/death-messages). One entry per line:

```
mekanism:acid = {player} dissolved in acid
*@twilightforest:lich = {player} was outsmarted by the Lich
```

The preview tells you whether an entry **matches the preview death**. Death messages are pack settings only, so this tab has **Save to pack** but no personal Save.

## Progression rules in the preview

When the latest death was changed by a [progression rule](/modpack/rules), the preview says so, for example `progression rule: enforce, attacker, ...`. Rules themselves are edited in the files (there is no editor tab for them); check them with [`/whm test --as / --assume`](/modpack/scanner#whm-test).

::: tip Workflow for pack makers
Build the templates in a single player test world with cheats, use **Save to pack**, then copy `config/whathitme/pack/templates/` into your pack.
:::
