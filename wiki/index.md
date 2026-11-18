---
layout: home

hero:
  name: What Hit Me?
  text: See exactly what killed you.
  tagline: A Minecraft mod for Forge, Fabric and NeoForge (1.7.10 - 26.3) that explains every death in plain words. Works in any modpack, no required dependencies.
  image:
    src: /images/icon_128.png
    alt: What Hit Me? icon
  actions:
    - theme: brand
      text: Get started
      link: /guide/getting-started
    - theme: alt
      text: Player guide
      link: /player/death-screen
    - theme: alt
      text: For modpack makers
      link: /modpack/

features:
  - title: Death screen recap
    details: Killing blow, attacker and its mod, every hit of the last 30 seconds, damage before and after armor, and a counter-measure tip. Hover any line for details.
    link: /player/death-screen
  - title: Client, server or both
    details: Install it on the server only, the client only, or both. Each player gets the best mode their client supports.
    link: /guide/multiplayer
  - title: Built for modpacks
    details: Your own names and tips per damage type, attacker, weapon and dimension; hidden ids; locked settings; commands on death.
    link: /modpack/
  - title: Templates and death messages
    details: "1.3.0+: rewrite every output with templates, edit them in game with a live preview, and replace vanilla death messages."
    link: /modpack/templates
  - title: Progression rules
    details: "1.3.0+: show ??? and no tip until the boss quest is done. Advancements, scoreboard, Game Stages, FTB Quests, Better Questing, Heracles."
    link: /modpack/rules
  - title: Scripts and quest mods
    details: "1.3.0+: KubeJS, CraftTweaker and MineTweaker scripts add conditions, variables and event listeners. FTB Quests, Better Questing, Heracles and Game Stages are read directly. All optional."
    link: /guide/integrations
  - title: In-game pack editor
    details: "1.3.0+: edit names, tips, rules, death messages, templates, triggers and server settings in game, saved by the server with backups and undo."
    link: /modpack/pack-editor
  - title: Light and safe
    details: Only players are tracked, no server tick handlers, packets only on death or on request, every hook guarded against broken data from other mods.
    link: /guide/
---

<div class="vp-doc" style="max-width: 960px; margin: 48px auto 0; padding: 0 24px;">

## See it in action

![What Hit Me? in action: death screen recap, hover details, respawn chat summary](/images/hero.gif)

## Where to start

| I am a... | Read this |
|---|---|
| Player | [Death screen panel](/player/death-screen), [Chat summary & alerts](/player/chat), [Commands](/reference/commands) |
| Server owner | [Installation](/guide/getting-started), [Client, server or both](/guide/multiplayer), [Configuration files](/reference/configuration) |
| Modpack maker | [Overview](/modpack/), [Pack folder](/modpack/pack-folder), [Cookbook](/cookbook) |

**Download:** [Modrinth](https://modrinth.com/mod/what-hit-me) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/what-hit-me) · [GitHub Releases](https://github.com/junjun1z/what-hit-me/releases)

::: tip Version note
This wiki describes **What Hit Me? 1.3.0**. Sections marked <Badge type="tip" text="1.3.0+" /> are new in 1.3.0 and do not exist in 1.2.0.
:::

</div>
