# Building What Hit Me? (every Minecraft version)

One Gradle build produces one jar per Minecraft version / loader. All logic lives in a loader-independent core
(`common/`, plain Java 8, no Minecraft classes); each target only adds a thin adapter to its Minecraft API and loader.

## Requirements

- **JDK 25 or newer to run Gradle** (Fabric Loom needs it; JDK 27 works too). Example on Windows / Git Bash:
  `export JAVA_HOME="/c/Program Files/Java/jdk-27"`.
- The targets compile with Java toolchains 8 / 17 / 21 / 25. Missing JDKs are downloaded automatically (foojay
  resolver), so CI only installs one JDK 25 (the workflows also pre-install 8 / 17 / 21 to save time).

## Commands

```bash
./gradlew build                                   # every target, including the nested legacy/ and oldforge/ builds
./gradlew build -Pmod_version=1.2.3               # release version (or set MOD_VERSION); default 0.0.0-dev
./gradlew collectJars -Pmod_version=1.2.3         # build + copy every release jar to build/dist/
./gradlew build -Ptargets=1.21.1-fabric           # only some targets (comma separated, also for the nested builds)
./gradlew build -Ptargets=1.20.1-forge -Pselftest # development build with the in-game self-test (never release it)
```

Jars (exactly one release jar per target, `publish.json` lists them):

| Build | Jar |
| --- | --- |
| main | `targets/<mc>-<loader>/build/libs/whathitme-<loader>-<mc>-<version>.jar` |
| nested `legacy/` | `legacy/targets/<mc>-forge/build/libs/whathitme-forge-<mc>-<version>.jar` |
| nested `oldforge/` | `oldforge/targets/<mc>-forge/build/libs/whathitme-forge-<mc>-<version>.jar` |

CI (`.github/workflows/build.yml`) runs `./gradlew collectJars` on every push to `main` and checks `publish.json`
against the jars. Pushing a tag like `1.2.3` runs `release.yml`: same build, then GitHub Release + Modrinth +
CurseForge through `tools/publish.py` (one upload per `publish.json` entry; Modrinth environment
`client_or_server_prefers_both`, CurseForge environments Client + Server).

## Layout

| Path | Contents |
| --- | --- |
| `common/src/main/java/.../core/` | The core (Java 8, no Minecraft imports). See "Architecture". |
| `common/src/main/resources/assets/whathitme/lang/*.json` | The 10 languages, the single source. 1.12.2 and older get `.lang` files generated at build time (`gradle/forge_rfg.gradle`, `gradle/forge_uni.gradle`). Check with `python tools/check_lang.py`. |
| `versions/<code_version>/` | Adapter to one Minecraft API generation, loader-neutral (Mojang names): text, damage facts, registries, GUI drawing, client / server port base classes. Package `io.github.junjun1z.whathitme.mc`. |
| `loaders/<loader>/<code_version>/` | Loader entry point, events, network channel, command registration, per-player storage, metadata (`mcmod.info`, `META-INF/mods.toml`, `fabric.mod.json`). Package `io.github.junjun1z.whathitme.forge` / `.fabric` / `.neoforge`. |
| `targets/<mc>-<loader>/gradle.properties` | One build target of the main build (Fabric Loom, ModDevGradle). |
| `legacy/targets/<mc>-forge/gradle.properties` | Targets that need RetroFuturaGradle (1.12.2), ForgeGradle 7 or Architectury Loom. |
| `oldforge/targets/<mc>-forge/gradle.properties` | Forge 1.8.9 - 1.11.2 (Unimined). Forge 1.7.10 lives in `legacy/` (RetroFuturaGradle, like 1.12.2). |
| `gradle/<loader>.gradle` | Build logic per loader: `fabric`, `forge` (MDG legacy, Forge 1.17 - 1.20.1), `neoforge`, `forge7`, `forge7_srg`, `forge_arch`, `forge_rfg`, `forge_uni`. `common.gradle` is shared by all. |
| `devtest/` | In-game self-test and the "client without the mod" driver, compiled **only** with `-Pselftest`. |

Each target's `gradle.properties`: `loader` (= `gradle/<loader>.gradle`), `loader_name` (published name, e.g. `forge`
for `forge_rfg`), `mc_version`, `code_version` (which `versions/` + `loaders/<loader>/` directories to compile),
`java_version`, the loader version (`forge_version` / `neoforge_version` / `fabric_loader` + `fabric_api` /
`mcp_mappings`) and `mc_range` / `loader_range` for the metadata. Toolchains, mappings and loader versions: reuse
the versions already used by the neighbouring targets; they are known to work.

## Architecture

The core (`io.github.junjun1z.whathitme.core`) holds everything that is not "talk to Minecraft":

| Package | Contents |
| --- | --- |
| `core.data` | `DamageRecord` (immutable, primitives + Strings), `PlayerHistory` (fixed ring buffer + smart merge), `Snapshot`, `DamageCatalog`, `DamageTracker`, `Entry`, `SearchEngine`, `DumpWriter` (CSV + `hint_template.cfg`), `HitFacts` / `EntityFacts` (what an adapter reads from a hit), `TypeModResolver` (mod of a damage type before 1.19.4), `Registries` (port) |
| `core.text` | `Txt` (version-independent chat component: literal / translation / native JSON, color, bold, underline, click, hover, siblings), `Fmt` (colors), `Lang` (port: translation lookup), `Msg` (localized vs. English for clients without the mod), `RecapFormatter` (every chat line, hover and recap), `DisplayOptions`, `Hints` |
| `core.config` | `WhmConfig` (every key, default, range, comment; both files), `Setting`, `ConfigBackend` (port), `CfgFile` (Forge-1.12 `.cfg` format writer/reader used by every version except 1.12.2, which keeps Forge's `Configuration` for its config screen) |
| `core.net` | `Packets` (the protocol: Hello, Request, ServerInfo, Recap, Entries, Message), `WireOut` / `WireIn` (byte encoding identical to 1.0.x's ByteBufUtils) |
| `core.server` | `ServerCore` (damage recording, deaths, totem / heavy-hit snapshots, protocol, server `/whm`), `ServerPort`, `PlayerHandle`, `CommandSource` (ports), `FilePlayerPrefs` |
| `core.client` | `ClientCore` (server info, recaps, respawn summary, client `/whm`), `FallbackCore` (client estimates), `DeathPanel` (death screen panel), `ClientPort`, `FallbackEnv`, `Gfx` (ports), `LegacyText` |
| `core.cmd` | `CommandText` (help, `/whm config`, errors), `Completion` (argument splitting + tab completion), `Sink` |
| `core` | `Platform` (mod names, logging, config dir, client / server `Lang`; set by the loader first) |

Ports a version implements (copy the nearest existing version and change what its Minecraft API changed):

| Port | 1.12.2 | 1.20.1 | 1.21.1 |
| --- | --- | --- | --- |
| Txt -> native component, `Lang` | `mc/McText` | `mc/McText` | `mc/McText` (JSON needs registries) |
| Damage source / entity -> `HitFacts` | `mc/Facts` (msgId + `TypeModResolver`) | `mc/Facts` (DamageType registry key) | `mc/Facts` (+ data-driven enchantments) |
| `Registries` (mobs, effects, vanilla damage ids, msgId -> id) | `mc/Registries112` | `mc/Registries120` | `mc/Registries121` |
| `Gfx` (death panel drawing) | `mc/Gfx112` | `mc/Gfx120` (GuiGraphics) | `mc/Gfx121` |
| `FallbackEnv` (client estimates) | `mc/FallbackEnv112` | `mc/FallbackEnv120` | `mc/FallbackEnv121` |
| `PlayerHandle` / `ServerPort` | `forge/PlayerHandle112`, `forge/ServerPort112` | `mc/McPlayer` + `mc/ServerPortMc`, loader adds send + storage | same |
| `ClientPort` | `forge/ClientPort112` | `mc/ClientPortMc`, loader adds send | same |
| Commands | `CommandBase` -> `ServerCore.execute` / `ClientCore.execute` | `mc/Brig` (one greedy argument, any Brigadier source) | same |
| Network | 6 SimpleNetworkWrapper messages (1.0.x wire format) | SimpleChannel `whathitme:main`, accept missing | Fabric payload `whathitme:main` |
| Config storage | `forge/ForgeCfgBackend` | `CfgFile` | `CfgFile` |
| Player settings of players without the mod | persistent NBT | persistent NBT (`getPersistentData`) | `FilePlayerPrefs` (`<world>/whathitme/players/`) |

What the loader calls (all on the right thread; every handler returns at once for non-players):

| Core call | Forge 1.12.2 / 1.20.1 | Fabric 1.21.1 |
| --- | --- | --- |
| `ServerCore.onHurt(...)` (before armor) | `LivingHurtEvent`, HIGHEST | `ServerLivingEntityEvents.ALLOW_DAMAGE` |
| `ServerCore.onHurtCancelled(...)` | `LivingHurtEvent`, LOWEST, receiveCanceled | - |
| `ServerCore.onDamage(...)` (after armor / absorption) | `LivingDamageEvent`, LOWEST | `onDamageAfter` from `AFTER_DAMAGE` and `ALLOW_DEATH` |
| `ServerCore.onDeath(...)` | `LivingDeathEvent`, LOWEST | `AFTER_DEATH` (message read in `ALLOW_DEATH`) |
| `ServerCore.onRespawn / onTickEnd / onLogout / onPacket` | `PlayerRespawnEvent` (not end), `ServerTickEvent` END, `PlayerLoggedOutEvent`, channel | `AFTER_RESPAWN` (!alive), `END_SERVER_TICK`, `DISCONNECT`, payload receiver |
| `ServerCore.serverStarted / serverStopped` | server starting / stopped | `SERVER_STARTED` / `SERVER_STOPPED` |
| `ClientCore.tick(env)` / `reset()` / `onPacket` | `ClientTickEvent` END / disconnect / channel | `END_CLIENT_TICK` / `DISCONNECT` / receiver |
| `ClientCore.onDeathScreen(json)` + `DeathPanel` render / mouse | death screen opening, render post, mouse pre events (cancel when used) | `ScreenEvents.AFTER_INIT` + `afterRender`, `ScreenMouseEvents.allow*` |

Damage ids: before 1.19.4 `<mod>:<msgId>` (`minecraft:outOfWorld`, mod from `TypeModResolver`); from 1.19.4 the
DamageType registry key (`minecraft:out_of_world`, `modid:type`); melee is always `mob:<entity id>`. `damageType`
stays the msgId (same names as 1.12, used for tips and `whathitme.damage.<msgId>` names). `ClientCore.msgIdToTypeId`
maps a msgId to the version's id for client estimates (set by `ClientPortMc.install()`).

## How to add a version (step by step)

Work only in the directories of your target; the shared files you may need to touch are `publish.json` (add your
entries) and, if a new Minecraft API generation really needs it, a new `versions/<v>` directory. Avoid changing
`common/` or `gradle/*.gradle`: every target builds against them.

1. **Pick the build.** Main build: Fabric (all), Forge 1.17 - 1.20.1 (`forge`, MDG legacy), NeoForge (`neoforge`).
   `legacy/`: Forge 1.12.2 (`forge_rfg`), 1.14.4 - 1.16.5 (`forge_arch`), 1.20.2 / 1.20.4 (`forge7_srg`),
   1.20.6+ / 1.21.x / 26.x (`forge7`). `oldforge/`: Forge 1.8.9 - 1.11.2 (`forge_uni`). Forge 1.7.10 is built by `legacy/` with `forge_rfg`.
   Inventory ToDo 1.2.0 uses the same split - check where its target lives.
2. **Target file.** Copy a neighbour's `gradle.properties` into `targets/<mc>-<loader>/` (or the nested build's
   `targets/`), set `mc_version`, `code_version`, `java_version`, loader versions and ranges (copy them from Inventory
   ToDo's target of the same name).
3. **Version adapter.** If no `versions/<code_version>` fits, copy the nearest one:
   - 1.19.4+ (DamageType registry): copy `versions/1.20.1` (or `1.21.1` for 1.20.5+ item components / data-driven
     enchantments). Fix the API differences until it compiles.
   - 1.13 - 1.19.3 (no DamageType registry): copy `versions/1.20.1`, then make `Facts.hit` use
     `src.getMsgId()` + `TypeModResolver` like `versions/1.12.2/.../mc/Facts.java` (typeId `<mod>:<msgId>`,
     melee = msgId "mob"/"player"), the flags from `isBypassArmor()` / `isFire()` / ..., and leave
     `ClientCore.msgIdToTypeId` at its default ("minecraft:" + msgId). Component API: `TextComponent` /
     `TranslatableComponent` before 1.19, `Component.literal/translatable` from 1.19.
   - 1.8.9 - 1.11.2 / 1.7.10: copy `versions/1.12.2` (ITextComponent / IChatComponent API).
4. **Loader.** Copy `loaders/forge/1.20.1`, `loaders/fabric/1.21.1` or `loaders/forge_rfg/1.12.2` to
   `loaders/<loader>/<code_version>` and adapt events / networking / metadata. Keep the rules: Forge / NeoForge only
   standard events (no mixins / coremods), channel accepts a missing remote side, display test ignores versions
   (`IGNORE_ALL_VERSION` / `acceptableRemoteVersions = "*"`); Fabric uses Fabric API events (tiny mixins only where no
   event exists), `fabric-api` required, `"environment": "*"`. NeoForge: `neoforge.mods.toml`, payload registration
   with `optional()`.
5. **Self-test.** Add `devtest/<code_version>/java/.../devtest/DevPort<v>.java` (copy `DevPort120` / `DevPort121`),
   `devtest/<loader>/<code_version>/java/.../devtest/DevEntry.java` (registers `SelfTest.tick()` on the client tick)
   and the driver: `devtest/driver/<loader>/<code_version>/java/io/github/junjun1z/whmdriver/entry/` (loader entry
   point implementing `DriverPort`) + `devtest/driver/<loader>/<code_version>/resources/` (its own `mods.toml` /
   `fabric.mod.json` / nothing for 1.12-style `@Mod`). Version-specific vanilla command syntax is in
   `devtest/driver/common/.../VanillaCommands.java` (by version number; extend it if a command changed).
6. **publish.json.** Add one entry per jar (path glob, loaders, tested game versions, java, required
   `fabric-api` on Fabric). Only list game versions you tested.
7. **Build:** `./gradlew build -Ptargets=<target>` (release jar) and `./gradlew build -Ptargets=<target> -Pselftest`.
8. **Test in game** (next section): `sp`, `mp-mod`, `mp-nomod` and the driver must all print `ALL PASSED`
   (the multiplayer modes at least once per loader family).

## Porting 1.2.0 features

The 1.2.0 features live in the core; 1.12.2 (`versions/1.12.2`, `loaders/forge_rfg/1.12.2`) is the reference. Every new port
method has a Java 8 default that keeps an unported version compiling and working as in 1.1 (the feature just stays empty), so a
version can be ported in small steps. Copy the 1.12.2 code and translate the Minecraft calls.

New port methods (all optional, default in brackets):

| Port method | Default | 1.12.2 implementation | Feature |
| --- | --- | --- | --- |
| `PlayerHandle.environment()` | `""` | `Facts.environment(player)`: block / fluid at the feet (the block stood on when the feet are in air), at the body centre, main / off hand items, built with `core.data.Context` | environment context |
| `PlayerHandle.armor()` | `null` | `Facts.armor(player)`: head, chest, legs, feet as `Gear.Item` (id, translation key, name, mod id, enchantments) | armor break, copy with equipment |
| `PlayerHandle.dimension()` | `""` | `Facts.dimension(world)`: `minecraft:overworld` / `the_nether` / `the_end`, other dimensions their id (1.16+: the level key) | `@dim:` keys, copy |
| `PlayerHandle.position()` | `null` | `{x, y, z}` | copy, death trigger placeholders |
| `ServerPort.schedule(Runnable)` | `false` | queue on the server task queue for the next tick (1.12.2 `futureTaskQueue` by reflection, because `addScheduledTask` runs at once on the server thread). 1.14+: `server.tell(new TickTask(server.getTickCount(), task))` (not `execute`, which also runs at once) | replaces the server tick handler |
| `ServerPort.runCommand(String)` | `false` | `server.getCommandManager().executeCommand(server, cmd)` (1.13+: `server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd)`) | deathTriggers |
| `ClientPort.localGear()` | `null` | `new Gear(dimension, x, y, z, maxHealth, Facts.armor(p), effects)` of the own player | copy recap of client estimates |
| `ClientPort.openConfigScreen()` | `false` (= `/whm config` lists the keys) | sets a flag; the client tick handler opens the config screen on the next tick (a screen opened from the chat handler is closed by the chat screen) | `/whm config` |
| `Registries.damageTypeEntries()` | vanilla ids with vanilla flags | static `DamageSource` fields of `DamageSource` (exact flags). 1.19.4+: every key of the DamageType registry, flags from its tags | catalog cache, search |
| `Registries.scanInput()` | `null` (no jar scan) | `DamageIdScanner.Input`: runtime internal names of `DamageSource`, `EntityDamageSource`, `EntityDamageSourceIndirect` (`X.class.getName().replace('.', '/')`), every mod file (`ModContainer.getSource()`, without minecraft / mcp / FML / forge / whathitme), the mod ids and `DamageIdScanner.OLD_FORGE_SETTERS` | damage ids of mods before they are seen (`scanned`) |
| `Platform.Mods.version(modId)` | `""` | mod container version | catalog cache hash |
| `EntityFacts.weaponId` | `""` | registry id of the held item (`Facts.entity`) | `@item:` keys |
| `ConfigBackend.raw / remove` | none | `ForgeCfgBackend` (`CfgFile` already has them) | migration of `showHintsAndTips` |

New core calls from the loader (players only, first line returns for anything else):

| Core call | Forge 1.12.2 (reference) | Newer Forge / NeoForge | Fabric |
| --- | --- | --- | --- |
| `ServerCore.onHurt(PlayerHandle, ...)` (overload, reads the armor before the hit) | `LivingHurtEvent` HIGHEST | `LivingHurtEvent` HIGHEST (NeoForge 1.20.5+: `LivingIncomingDamageEvent`) | `ALLOW_DAMAGE` |
| `ServerCore.onPlayerAttack(uuid, targetUuid)` | `AttackEntityEvent` | `AttackEntityEvent` | `AttackEntityCallback` (server side only) |
| `ServerCore.onArmorBroken(handle, item, slot)` | `PlayerDestroyItemEvent` (armor only) | same | none (the armor comparison of onHurt / onDamage finds it) |
| `ServerCore.onDeathCancelled(handle, facts)` | `LivingDeathEvent` LOWEST with `receiveCanceled = true`, when cancelled | same | not visible (`ALLOW_DEATH` of other mods); revives are found by the revive check |
| `ServerCore.onWorldSave()` | `WorldEvent.Save` (overworld only) | `LevelEvent.Save` | none (stats are also saved on death and logout); `SERVER_STOPPING` is fine |
| `CatalogCache.init(registries)` | `FMLPostInitializationEvent` | server started / client joined a world (dynamic registries) | `SERVER_STARTED` / client join |
| `ServerCore.onTickEnd()` | removed | remove once `schedule` is implemented | same |

Damage id scanner (`core.data.DamageIdScanner`): reads the class files of the mod jars (no class loading, no ASM) and
collects the string constants given to the damage source constructors (also `super("id")` in mod subclasses, anonymous
subclasses and setter calls right after the constructor). `CatalogCache` runs it on a daemon thread after every cache miss
and stores the ids under `scan` in `catalog_cache.json`; they show in `/whm search` (`#scanned`), `/whm dump` (column
`source`) and `/whm test` completion. Port: implement `scanInput()` with the class names taken from the runtime classes,
never hard-coded: Forge 1.7.10 - 1.12.2 MCP names (`net/minecraft/util/DamageSource`, setters `OLD_FORGE_SETTERS`), Forge
1.13 - 1.16 the SRG/MCP class names (setters: own map of MCP + SRG names), Fabric the intermediary name
(`net/minecraft/class_1282`, setters by intermediary method names), Forge 1.17+ / NeoForge official names. Mod files: Forge
1.13+ `ModList.get().getModFiles()` (`getFile().getFilePath()`), Fabric `FabricLoader.getAllMods()` (`getOrigin().getPaths()`),
nested jars are scanned one level deep. Not needed from 1.19.4 on (the DamageType registry is complete): keep the default.
Standalone check against a mods folder: `java -cp whathitme-selftest.jar io.github.junjun1z.whathitme.devtest.ScanMain <dir>`.

Other rules: `PlayerHandle.isAdmin()` is permission level 2 only (single player counts only with cheats); `DevPort.v12()` returns
true to run the 1.2 self-test checks (`devtest/common/.../V12Plan.java`) plus `configScreenOpen()`, `closeScreen()`,
`configGuiLocked(key)` and the new `DevPort.Act` values (see `DevPort112`).

Config screens: 1.12.2 and older keep Forge's `GuiConfig` (`WhmGuiFactory`); `lockedFeatures` entries get a disabled entry class
(`WhmGuiFactory.Locked*`). Newer versions need their own small screen listing `WhmConfig.Base.settings()` of both files (locked keys
drawn disabled), registered with Forge `ConfigScreenHandler.ConfigScreenFactory` / NeoForge `IConfigScreenFactory`. Fabric: when Mod
Menu is installed, register the screen through an optional `modmenu` entrypoint (`ModMenuApi#getModConfigScreenFactory`, Mod Menu as
`suggests`, compile-only dependency) and let `ClientPort.openConfigScreen()` open the screen of that same factory; without Mod Menu it
opens our screen directly. Mod Menu must never be a required dependency.

Protocol 2 (`Packets.PROTOCOL`): the hello carries 2; the server answers with a `ServerInfo` extension (protocol, lockedFeatures,
pack renames / hints) and sends recaps with the extended body (kind byte `| 0x40`: extra record fields, armor break lines, equipment).
Clients of 1.0 / 1.1 get the old format (armor break lines removed), and a 1.2 client treats display bits it does not get from an old
server as on. Never reorder `WhmConfig.Flag`: its ordinal is the bit on the wire.

## Porting 1.3.0 features

1.3.0 adds templates for the six outputs, custom death messages (pack `deathMessages`) and the in-game template editor. Everything
is in the core; the references are Forge 1.12.2 (`loaders/forge_rfg/1.12.2`) and Fabric 1.21.1 (`loaders/fabric/1.21.1`,
`versions/1.21.1`). Templates themselves need nothing from a version: an unported version already renders pack / personal templates
(no template configured = the 1.2.0 code path, byte for byte; checked with an old-vs-new render of every output in all 10 languages).
What a version adds:

| Port method / call | Default | Reference implementation | Feature |
| --- | --- | --- | --- |
| `ClientPort.playerName()` | `""` | own player's name | `{player}` on the client |
| `ClientPort.openTemplateEditor()` | `false` (= `/whm template` prints its usage) | sets a flag; the client tick opens the screen host (like `openConfigScreen`; 1.21.1: `ClientPortMc.tickConfigScreen()` does both) | editor |
| `ServerPort.replacesDeathMessages()` | `false` | Fabric: `true` (`ServerPortMc.replacesDeathMessagesFlag`, set by the Fabric entry point) | death messages |
| `ServerCore.customDeathMessage(handle, facts)` | - | Fabric mixin, see below | death messages for everyone |
| `ClientCore.chatReplacement(plainText)` | - | Forge / NeoForge client chat event: `setMessage(McText.of(txt))` when not null | death messages on modded clients |
| `ClientCore.deathScreenReplacement(plainText)` | - | Forge 1.12.2: `GuiOpenEvent` of `GuiGameOver`, its only `ITextComponent` field replaced by reflection | death screen text |

Custom death messages: with no `deathMessages` entry nothing is sent and nothing changes. Support matrix: Fabric server with the mod
replaces the message for everyone (death screen, chat of every player, server log). Forge / NeoForge server with the mod: no event can
change the vanilla message, so the server sends `Recap.DEATH_MESSAGE` (protocol 3) to modded clients before the vanilla chat line and
logs `[What Hit Me?] Death message: ...`; the modded client replaces the matching chat line (and the death screen text); players
without the mod keep the vanilla message.

Forge / NeoForge client chat event per version (the driver of the same version already listens to it):

| Version | Event | Change the text |
| --- | --- | --- |
| Forge 1.7.10, 1.8.9 | `ClientChatReceivedEvent` | public field `message` |
| Forge 1.9.4 - 1.20.4 | `ClientChatReceivedEvent` (1.19.2+: also fired for system messages) | `setMessage(...)` |
| Forge 1.19, 1.19.1, 1.20.6 | no event for system messages (Forge 41 / 42; Forge 50 has the hook commented out) | the line is replaced in the chat list on the client tick (`ClientPortMc.replaceDeathMessageInChat`) |
| Forge 1.21.1 - 1.21.5 | `SystemMessageReceivedEvent` (server messages no longer use `ClientChatReceivedEvent`) | `setMessage(...)` |
| Forge 1.21.6 - 1.21.8 | `SystemMessageReceivedEvent.BUS.addListener` (EventBus 7) | `setMessage(...)` |
| Forge 1.21.9+ / 26.x | `SystemMessageReceivedEvent.BUS.addListener`, but the death message packet can be handled after the chat line | `setMessage(...)` plus the chat list replacement on the client tick |
| NeoForge 1.20.2 - 26.x | `ClientChatReceivedEvent` (system messages: `ClientChatReceivedEvent.System`) | `setMessage(...)` |

Death screen text: 1.13+ `DeathScreen` keeps it in a final `Component` field (`causeOfDeath`); replace it by reflection in the
screen-init event (Forge `ScreenEvent.Init.Post`, NeoForge the same) when `deathScreenReplacement` returns a text.

Fabric: a mixin in `ServerPlayer#die(DamageSource)` wraps the `CombatTracker#getDeathMessage()` call (MixinExtras
`@ModifyExpressionValue`, see `loaders/fabric/1.21.1/.../mixin/DeathMessageMixin.java`) and returns
`McText.of(ServerCore.customDeathMessage(...))` or the vanilla component. The same target exists in every Fabric version
(Mojang names, intermediary when remapped; 1.14.4 - 26.3) - verify the call is still in `die` when porting. Also store the
custom JSON where the loader keeps the death message for `onDeath` (1.21.1: `DEATH_MSG`), so the recap quotes what players saw.
MixinExtras ships with Fabric Loader 0.15+ (all our Fabric targets require it).

Template editor screen host (`core.client.TemplateEditor`; everything is drawn with `Gfx`, the text fields are core `TextField`s):
- a screen that creates `new TemplateEditor()`, calls `render(gfx, width, height, mouseX, mouseY)` every frame (after the
  background) and goes back to its parent when `wantsClose()` is true; `closed()` when the screen is removed;
- mouse: `mouseDown(button, x, y, shiftDown)`, wheel: `scroll(x, y, delta)`;
- keys: map the version's key codes to `TemplateEditor.K_*` (backspace, delete, arrows, home, end, enter, tab, escape, A / C / V /
  X with ctrl) and call `key(k, ctrlDown, shiftDown)`; if it returns false and the character is printable call `typed(char)`
  (1.12: `keyTyped(char, int)` with LWJGL 2 `Keyboard` codes; 1.13+: `keyPressed` with GLFW codes + `charTyped`);
- key repeat on (1.12: `Keyboard.enableRepeatEvents(true)`), the game not paused;
- references: `loaders/forge_rfg/1.12.2/.../GuiTemplateEditor.java`, `versions/1.21.1/.../TemplateEditorScreen121.java`.
Config screen: a "Templates..." button (`whathitme.config.templates`) that opens the host with the config screen as parent (1.12:
`WhmGuiFactory.WithTemplates` adds a `GuiButton` to Forge's `GuiConfig`; 1.21.1: `ConfigScreen121` top right).

Self-test: `DevPort.v13()` true runs `V13Plan` (templates of all outputs, death messages, editor) and needs `templateEditorOpen()`
and `pressConfigTemplatesButton()`. Multiplayer: `whm-tools/v13/mpstep.sh` writes a server pack (summary template + `deathMessages`);
the driver flag is `dm-custom` on Fabric (replaced for everyone) and `dm-vanilla` on Forge / NeoForge (vanilla client unchanged).
Protocol 3 (`Packets.PROTOCOL`): `ServerInfo` carries the pack templates and death messages after the protocol 2 part (only for
clients that said hello with 3); `Recap.DEATH_MESSAGE` = 7.

Notes from 1.7.10 - 1.11.2 (same host as 1.12.2, `GuiTemplateEditor` in each loader):
- 1.10.2 and older have no `createConfigGui`: the "Templates..." button lives in `WhmGuiFactory.Screen` (the class Forge
  instantiates); 1.11.2 uses `WhmGuiFactory.WithTemplates` like 1.12.2. The button label width comes from `Gfx<v>.width`.
- 1.7.10: `cpw.mods.fml` packages, `GuiScreen` input methods without `throws IOException`, raw `buttonList`; the death screen
  shows no death message, so only the chat line is replaced. 1.7.10 / 1.8.9 change the chat text through the public
  `event.message` field, 1.9.4+ through `setMessage`.

Notes from 1.14.4 - 1.17.1 (`versions/<v>` hosts, shared by Fabric and Forge of a version):
- `TemplateEditorScreen114` / `115` / `116` / `117` in `versions/<v>`; `ClientPortMc.tickConfigScreen()` opens it, the "Templates..."
  button is top right in `ConfigScreen1xx` (so Forge's mods list and Mod Menu get it too). 1.14 / 1.15 draw without `PoseStack`;
  key repeat through `keyboardHandler.setSendRepeatsToGui`.
- Fabric: `DeathMessageMixin` is a plain `@Redirect` of `CombatTracker#getDeathMessage()` in `ServerPlayer#die` (the same single call
  in 1.14.4 - 1.17.1; these jars accept Fabric Loader 0.12+, so no MixinExtras). The custom message is decided in the `die` HEAD
  mixin (`onDie`, before the recap marks the killing blow) and handed to the redirect; the recap quotes it.
- Forge: `ClientChatReceivedEvent#setMessage` for the chat line and `ClientPortMc.replaceDeathMessage(DeathScreen)` in `GuiOpenEvent`.

Notes from 1.18.2 - 1.20.6 (`versions/<v>` hosts, shared by every loader of a code version):
- `TemplateEditorScreen118` / `1192` / `1193` / `1194` / `120` / `1202` / `1206`; 1.18.2 - 1.19.4 draw with `PoseStack`
  (`Gfx<v>(pose, this)`), 1.20.1 `GuiGraphics` + `renderBackground(g)`, 1.20.2+ `super.render` draws the background and
  `mouseScrolled` has two deltas. Key repeat: `setSendRepeatsToGui` exists up to 1.19.2 only (1.19.3+ always repeats).
- Fabric: 1.18.2 / 1.19.2 decide the custom message in the `die` HEAD mixin (`onDeath`) and hand it to a `@Redirect`; 1.19.3+
  use the 1.21.1 MixinExtras `@ModifyExpressionValue` and store the JSON in `DEATH_MSG` for `AFTER_DEATH`.
- Forge / NeoForge: `ClientChatReceivedEvent#setMessage` plus the chat list fallback above for Forge 1.19 / 1.19.1 (1.19.2 jar)
  and Forge 1.20.6. GuiMessage is a generic class on 1.19 and a record on 1.19.1+: the 1.19.2 fallback copies the record through
  its canonical constructor (records cannot be changed by reflection). Death screen: `ScreenOpenEvent` (1.18.2) /
  `ScreenEvent.Opening` + `ClientPortMc.replaceDeathMessage`.

Notes from 1.21.1 - 26.3 (`versions/<v>` hosts, shared by every loader of a code version):
- `TemplateEditorScreen121` in every `versions/<v>`; keys are mapped with `InputConstants.KEY_*` in a `switch` (inlined per
  target, which matters for 26.x: 26.3 uses SDL scancodes, 26.1 / 26.2 GLFW codes). 1.21.9+ input methods take `KeyEvent`,
  `CharacterEvent` and `MouseButtonEvent` (modifiers from the event, no `Screen.hasShiftDown`), 26.x draws in
  `extractRenderState(GuiGraphicsExtractor, ...)` and changes screens through `Compat26`; mouse buttons there go through
  `Compat26.button`. `playerName()` uses `player.getName().getString()` (the `GameProfile` accessor changed in 1.21.9).
- Fabric: the 1.21.1 `@ModifyExpressionValue` mixin unchanged on every version (the `getDeathMessage` call is in `die` from
  1.21.1 to 26.3); `compatibilityLevel` JAVA_25 for 26.x.
- Forge 1.21.1 - 1.21.5: `@SubscribeEvent SystemMessageReceivedEvent`; 1.21.8+: `SystemMessageReceivedEvent.BUS.addListener`.
  Forge 1.21.9+ (PacketProcessor) can handle the `DEATH_MESSAGE` payload (`consumerMainThread`) after the vanilla system chat
  line, so `ClientPortMc.replaceDeathMessageInChat()` runs on the client tick and checks the newest lines again once
  the payload arrived (`deathMessageArrived()` from `ForgeClient.onPacket`). NeoForge keeps the order (event only).
- Death screen: `ScreenEvent.Opening` + `ClientPortMc.replaceDeathMessage` on Forge and NeoForge.

## In-game self-test (development)

Build with `-Pselftest`. Use only `build/devlibs/whathitme-selftest.jar` (the mod + self-test, for clients **and test
servers**) and `build/devlibs/whmdriver-dev.jar` (the driver, a separate mod for a client without What Hit Me?).
The `libs/` jar of a `-Pselftest` build also contains the driver's `@Mod`; Forge refuses to start with it.

Instances: create a Prism Launcher instance per version and loader (`WhmTest_<mc>_<Loader>` and
`WhmTest_<mc>_<Loader>_NoMod`), add `pauseOnLostFocus:false` and `skipMultiplayerWarning:true` to `options.txt`, put the jars in
`.minecraft/mods/` (Fabric: also the fabric-api jar). If
Prism does not see a new instance ("resolves to nothing" in its log), rename the folder away and back. Launch:
`"%LOCALAPPDATA%\Programs\PrismLauncher\prismlauncher.exe" --launch <instance> [--server localhost:<port>]`
(Prism's default account is used).

| Mode | How | Checks |
| --- | --- | --- |
| `sp` | `config/whathitme/selftest.flag` = `sp`; launch without `--server` | creates / loads world `whm_selftest`: fall, zombie melee with enchanted sword, arrow with owner, fire, merge (x3), magic, creeper explosion, absorption + armor mitigation, heavy hit, totem, every `/whm` subcommand, death panel (screenshot), respawn summary, tab completion |
| `mp-mod` | flag `mp-mod`; dedicated server **with** the mod; `--server` | the same through vanilla commands, accurate killing blow, `/whm server ...`, inspect |
| `mp-nomod` | flag `mp-nomod`; server **without** the mod | client fallback (`[Client Est.]`), historySize, estimated killing blow |
| driver | `whmdriver-dev.jar` only (no What Hit Me?) in the `_NoMod` instance, empty `config/whmdriver.flag`; server **with** the mod | every server message is readable English (no `whathitme.` keys), summary, totem alert, personal ignore |
| `mp-two` / driver `two` | op client with the mod + a second client "Alex" with the driver | inspect of another player |
| `pack` (1.12.2) | `WhmTest_1.12.2_Pack` (21 mods) | modded mobs, modded killing blow |

Results: `whathitme_selftest_result.txt` / `whmdriver_result.txt` in `.minecraft/` (first line `ALL PASSED` or
`N FAILED`), screenshots `whm_selftest_*.png`; the game quits by itself. Log lines start with `WHM-SELFTEST` /
`WHMDRIVER`.

Test servers (a local folder of dedicated servers, `online-mode=false`, flat world, your player is op):
`server-forge-1.12.2` (port 25612), `server-forge-1.20.1` (25620), `server-fabric-1.21.1` (25621, needs the
fabric-api jar in `mods/`). Each has `start.sh`; run it with a command file as stdin to stop it later:
`: > srv.cmd; (tail -f srv.cmd | ./server-x/start.sh > srv.log 2>&1 &)` ... `echo stop >> srv.cmd`. A new server: Forge
`java -jar forge-<mc>-<ver>-installer.jar --installServer <dir>`, Fabric
`https://meta.fabricmc.net/v2/versions/loader/<mc>/<loader>/1.1.2/server/jar`; copy `eula.txt`, `server.properties`
and `ops.json` from an existing one (in offline mode the op entry needs the offline UUID
`210a6881-7a60-3ef7-bd35-edb71bcd8759`) and pick a free port.

Pitfalls seen so far:

- Anything that reads a registry must not run (or cache) before a world is loaded: plans are built at game start.
- Since 1.20.2 `setAbsorptionAmount` is capped by the max absorption attribute: give an Absorption effect first.
- Since 1.20 `/kill` is `minecraft:generic_kill`, not `out_of_world`.
- `ServerLivingEntityEvents.AFTER_DAMAGE` is not called for lethal hits (use `ALLOW_DEATH`), and the combat tracker
  is reset before `AFTER_DEATH` (read the death message earlier).
- Client commands: Forge and Fabric run client commands from `ClientPacketListener.sendCommand`; `/whm server ...`
  therefore asks the server through the `COMMAND` request packet (1.13+) instead of sending a chat command.
- The system clipboard can be busy for a moment on Windows; the self-test retries.

### Notes from the 1.18.2 - 1.20.6 ports

- Version adapters: `versions/1.18.2` (`TextComponent` / `TranslatableComponent`, `sendMessage(c, NIL_UUID)`),
  `1.19.2`, `1.19.3` (msgId based, `TypeModResolver` through `Facts.modOfSource` set by the loader), `1.19.4`
  (first DamageType version, PoseStack drawing: `Gfx<v>(poseStack, screen)` up to 1.19.4), `1.20.2` (shared by
  1.20.2 - 1.20.4), `1.20.6` (item components, enchantments still objects).
- Fabric API before 1.21 has no `AFTER_DAMAGE`: a tiny mixin at the end of `Player#actuallyHurt` reports the health
  left. Fabric API for 1.18.2 and for 1.19 / 1.19.1 has no living entity events at all: `loaders/fabric/1.18.2` and
  `1.19.2` use mixins at the start / end of `Player#actuallyHurt` and the start of `ServerPlayer#die` instead (one jar
  for 1.19 - 1.19.2). Patch versions are tested with the newest fabric-api of that exact version.
- Forge 1.20.2+ has no `FakePlayer` class: players that are not in the player list are ignored instead.
- Forge 1.20.2+ networking: `ChannelBuilder ... .optional().simpleChannel()`; NeoForge 1.20.4 `RegisterPayloadHandlerEvent`
  + `registrar(...).optional().play(...)`, 1.20.6 `RegisterPayloadHandlersEvent` + `playBidirectional`.
- NeoForge 1.20.2 - 1.20.4 use `META-INF/mods.toml`, 1.20.5+ `META-INF/neoforge.mods.toml`.
- The gradle-cache `fabric-api-0.77.0+1.18.2.jar` is an empty stub: take the real jar from Modrinth for test instances.
- Self-test world creation: 1.18.2 `Minecraft#createLevel` + the flat `WorldPreset` (found by reflection),
  1.19.2 `createFreshLevel(..., RegistryAccess, WorldGenSettings)`, 1.20.2 - 1.20.4 `WorldOpenFlows` called by
  signature (the parameters differ between 1.20.2 and 1.20.4).
- Forge 1.18.2 merges the server's command tree over the client commands (`ClientCommandHandler.mergeServerCommands`):
  with the mod on both sides the client `/whm` would be sent to the server. `loaders/forge/1.18.2` therefore also runs
  the client command from `ClientChatEvent` (the Brigadier registration stays for tab completion). Forge 1.19+ is fine.
- The 1.18.2 jars are 1.18.2 only: Forge 38 (1.18) has no `RegisterClientCommandsEvent`, Forge 39 (1.18.1) other client
  event names, and Minecraft 1.18.2 changed the registry API (`RegistryAccess.Frozen`), so the Fabric jar crashes on
  1.18 / 1.18.1 (`NoSuchMethodError` in `ServerPortMc`). Supporting them would need a `versions/1.18` adapter.
- Fabric API before 0.59 (1.18 - 1.19.1) has the mod id `fabric`, newer ones provide it: the 1.18.2 and 1.19.2 Fabric
  jars depend on `"fabric": "*"` (publish.json still names the project `fabric-api`).
- Dedicated server 1.18.2: `level-type=flat` (not `minecraft\:flat`, which silently gives a normal world and makes the
  mp self-test miss the mob hits).

## Notes for 1.14 - 1.17.1 (no DamageType registry, Mojang names)

- Fabric 1.16.2 - 1.17.1 (`loaders/fabric/1.16.5`, `1.17.1`) use Fabric API networking v1, command v1, entity events and the screen API.
  Fabric 1.14 - 1.16.1 (`loaders/fabric/1.14.4`, `1.15.2`, `1.16.1`) use **only the v0 modules** (`ServerSidePacketRegistry`,
  `ServerStartCallback`, `ClientTickCallback`, `CommandRegistry`), because 1.14 - 1.14.3, 1.15 - 1.15.1 and 1.16 only have old Fabric API
  builds (0.2.7 - 0.13.1); respawn, logout, the death screen, the mouse and client `/whm` are small mixins there.
- Networking v0 forgets a player's registered channels on respawn: the loader also remembers who sent a packet (`FabricNet.MOD_CLIENTS`).
- Forge 1.14.4 - 1.16.5 (`loaders/forge_arch/<v>`, built in `legacy/`): old Forge builds of a range lack newer helpers
  (`NetworkRegistry.acceptMissingOr`, `SimpleChannel#isRemotePresent`, `RegisterCommandsEvent`, `RegisterClientCommandsEvent`). The channel
  accepts everything (`v -> true`), the server remembers which players sent a packet, and client `/whm` is taken from `ClientChatEvent`.
  Test the oldest Forge build of a range too.
- Prism's `--server` crashes 1.14 - 1.17 clients (they join before the models are loaded): the self-test and the driver join the server
  written in `.minecraft/whm_connect.txt` from the title screen instead.
- 1.21.x / 26.x: one code version can serve several targets (`code_version` 1.21.3 builds 1.21.3 and
  1.21.4; `26.3` is one source compiled against 26.1.2, 26.2 and 26.3 - the differences are looked up by reflection in `mc/Compat26`).
- NeoForge 21.8+ / 26.x: `playBidirectional` needs the client handler as its fourth argument (missing in 21.6 betas, so the 1.21.8 jar starts
  at 1.21.7). NeoForge 26.x fires `LivingDamageEvent.Post` after the totem check and the death: lethal hits are recorded from
  `LivingUseTotemEvent` / `LivingDeathEvent`.
- 26.2+: the current screen is `Minecraft.gui.screen()`, the chat `Minecraft.gui.hud.getChat()`; 26.3: the damage cooldown is
  `LivingEntity.damageCooldownTime` (`Entity.invulnerableTime` is something else there).
- Forge 1.21+: server messages reach the client through `SystemMessageReceivedEvent`, not `ClientChatReceivedEvent` (driver).
- Fabric: use the fabric-api built for the running patch version (0.136.1+1.21.8 refuses 1.21.6, 0.138.4+1.21.10 refuses 1.21.9); the jar
  itself runs on all of them. On 1.21 fabric-api must be 0.116.18+1.21.1 or newer (0.102.0+1.21 has no `AFTER_DAMAGE`).
- Test instances for patch versions: copy an instance and change the component versions in `mmc-pack.json` (26.1.x and 26.2 need
  LWJGL 3.4.1). Every instance: `soundCategory_master:0.0` in `options.txt`.
- The armor mitigation check of the self-test can fail once (attribute not applied yet): run it again.

### 1.2.0 on 1.14 - 1.17.1

- `versions/<v>`: `ConfigScreen1xx` (paged list of both files, locked keys disabled) is the screen of every loader; the loader's client
  tick calls `ClientPortMc.tickConfigScreen()`. Forge registers it as the mods list config factory (`ExtensionPoint.CONFIGGUIFACTORY`,
  1.17: `ConfigGuiHandler.ConfigGuiFactory`).
- Fabric: `WhmModMenu` is a `modmenu` entrypoint (`suggests`, compile only through `gradle/deps-fabric-<v>.gradle`). Mod Menu API per jar:
  1.14.4 Mod Menu 1.7 (`io.github.prospector...ModMenuApi#getModId` + `getConfigScreenFactory`), 1.15.2 Mod Menu 1.8 - 1.10 (same, its
  `getModConfigScreenFactory` wraps it), 1.16.1 / 1.16.5 the `io.github.prospector` `getModConfigScreenFactory` (Mod Menu 1.14 - 1.16
  have it), 1.17.1 `com.terraformersmc.modmenu.api` (2.x).
- `DamageSource` constructor and most setters are protected here: `Registries1xx.setters()` finds the setters by calling every no-argument
  method returning `DamageSource` on a fresh instance and comparing the flags (runtime names: SRG on Forge 1.14 - 1.16, intermediary on
  Fabric, official on Forge 1.17). Self-test probes: a covariant override (`Named setIsFire()`) is not remapped, use the subclass
  constructor pattern instead.
- Fabric attack events: AttackEntityCallback on 1.16.5 / 1.17.1, a `Player#attack` HEAD mixin on the v0-only loaders.
- `ServerPortMc.schedule`: `server.tell(new TickTask(server.getTickCount(), task))`; `runCommand`: `getCommands().performCommand`.

### 1.2.0 on 1.18.2 - 1.20.6

- `versions/<v>`: `ConfigScreen<suffix>` per code version (1.18.2 `TextComponent` + `new Button`, 1.19.2 `Component.literal`, 1.19.3+
  `Button.builder`, 1.20.1 `GuiGraphics`, 1.20.2+ `Screen#render` draws the background itself). Block names in the environment context use
  the translation key (`TranslatableComponent` / `Component.translatable`).
- Damage ids: 1.18.2 - 1.19.3 have no DamageType registry: `scanInput()` and `damageTypeEntries()` as on 1.17.1 (static `DamageSource` fields,
  setters found at run time), the loaders also set `Facts.modOfSource` and `Registries<v>.modFiles`. 1.19.4+: `damageTypeEntries()` lists
  every key of the DamageType registry (flags from its tags through `Facts.typeFlags`), no scanner; the catalog cache is filled on server
  start and on the first client tick in a world (dynamic registry).
- Forge config factory: 1.18.2 `ConfigGuiHandler.ConfigGuiFactory`, 1.19 - 1.20.6 `ConfigScreenHandler.ConfigScreenFactory`, NeoForge 1.20.4
  `ConfigScreenHandler.ConfigScreenFactory`, NeoForge 1.20.6 `IConfigScreenFactory` registered on the mod container. Events: Forge 1.18.2
  `WorldEvent.Save` and `getPlayer()`, 1.19+ `LevelEvent.Save` and `getEntity()`.
- Fabric: `AttackEntityCallback` on every version (Fabric API for 1.18 - 1.19.1 has it too), Mod Menu entrypoint `com.terraformersmc.modmenu.api`
  (compile only: Mod Menu 3.2.5 / 4.1.2 / 5.1.0 / 6.3.1 / 7.2.2 / 8.0.1 / 10.0.0 per code version).
- `runCommand`: 1.18.2 `Commands#performCommand`, 1.19+ `performPrefixedCommand` (its return type changes, the result is ignored).
- 1.20.5+: `Enchantments.PROTECTION` (was `ALL_DAMAGE_PROTECTION`) in the self-test.

### 1.2.0 on 1.21.1 - 26.3

- `versions/<v>`: `ConfigScreen121` in every code version (the class names of this group keep the 121 suffix). 1.21.6+ draws text with an
  alpha channel (`0xFFRRGGBB`), 1.21.11+ `Screen#init(int, int)`, 26.x `extractRenderState(GuiGraphicsExtractor, ...)` with
  `GuiGraphicsExtractor#text` and `Compat26.setScreen` / `Compat26.screen()` (also for `/whm config`).
- `damageTypeEntries()`: every key of the DamageType registry (`getHolderOrThrow` on 1.21.1, `getOrThrow` from 1.21.2); no scanner. The catalog
  cache is filled on server start and on the first client tick in a world.
- `Facts.armorSlot(player, stack)` uses `LivingEntity#getEquipmentSlotForItem` (an instance method since 1.20.5).
- `ServerPortMc.schedule`: `server.tell(new TickTask(...))` on 1.21.1, `server.schedule(...)` from 1.21.2 (renamed, still only queues).
- Forge 1.21.1 - 1.21.5 use `@SubscribeEvent` like 1.20.6; Forge 1.21.6+ (EventBus 7): `AttackEntityEvent.BUS` with a predicate listener
  (returns false), `PlayerDestroyItemEvent.BUS` / `LevelEvent.Save.BUS` consumers, a cancelled death is seen by an `ObjBooleanBiConsumer`
  monitor on `LivingDeathEvent.BUS`. Config factory on every Forge version: `ModLoadingContext.get().registerExtensionPoint(
  ConfigScreenHandler.ConfigScreenFactory.class, ...)`. NeoForge: `IConfigScreenFactory` registered on the mod container, death with
  `receiveCanceled = true`.
- Fabric Mod Menu (compile only): 11.0.3 / 12.0.1 / 14.0.2 / 15.0.2 / 16.0.1 / 17.0.1 per code version, 26.x `compileOnly` 21.0.1 (no
  remapping). Tested with Mod Menu 11.0.4, 12.0.1, 13.0.4, 14.0.2, 15.0.2, 16.0.1, 17.0.1, 18.0.2, 20.0.3, 21.0.1.
