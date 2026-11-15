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
| `oldforge/targets/<mc>-forge/gradle.properties` | Targets that need Unimined (Forge 1.7.10 / 1.8.9 - 1.11.2). Empty so far. |
| `gradle/<loader>.gradle` | Build logic per loader: `fabric`, `forge` (MDG legacy, Forge 1.17 - 1.20.1), `neoforge`, `forge7`, `forge7_srg`, `forge_arch`, `forge_rfg`, `forge_uni`. `common.gradle` is shared by all. |
| `devtest/` | In-game self-test and the "client without the mod" driver, compiled **only** with `-Pselftest`. |

Each target's `gradle.properties`: `loader` (= `gradle/<loader>.gradle`), `loader_name` (published name, e.g. `forge`
for `forge_rfg`), `mc_version`, `code_version` (which `versions/` + `loaders/<loader>/` directories to compile),
`java_version`, the loader version (`forge_version` / `neoforge_version` / `fabric_loader` + `fabric_api` /
`mcp_mappings`) and `mc_range` / `loader_range` for the metadata. Toolchains, mappings and loader versions: reuse
exactly what Inventory ToDo 1.2.0 uses for the same target (`..\inventory-todo\modern\targets`,
`...\modern\legacy\targets`, `...\modern\oldforge\targets`); they are known to work.

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
entries) and, if a new Minecraft API generation really needs it, a new `versions/<v>` directory. Do not change
`common/` or `gradle/*.gradle` : every target builds against them.

1. **Pick the build.** Main build: Fabric (all), Forge 1.17 - 1.20.1 (`forge`, MDG legacy), NeoForge (`neoforge`).
   `legacy/`: Forge 1.12.2 (`forge_rfg`), 1.14.4 - 1.16.5 (`forge_arch`), 1.20.2 / 1.20.4 (`forge7_srg`),
   1.20.6+ / 1.21.x / 26.x (`forge7`). `oldforge/`: Forge 1.8.9 - 1.11.2 (`forge_uni`; also a good place for 1.7.10).
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

## In-game self-test (development)

Build with `-Pselftest`. Use only `build/devlibs/whathitme-selftest.jar` (the mod + self-test, for clients **and test
servers**) and `build/devlibs/whmdriver-dev.jar` (the driver, a separate mod for a client without What Hit Me?).
The `libs/` jar of a `-Pselftest` build also contains the driver's `@Mod`; Forge refuses to start with it.

Instances: copy `%APPDATA%\PrismLauncher\instances\InvTodoTest_<mc>_<Loader>` (only `instance.cfg`, `mmc-pack.json`
and `.minecraft/options.txt`) to `WhmTest_<mc>_<Loader>` and `WhmTest_<mc>_<Loader>_NoMod`, set `name=` in
`instance.cfg`, add `pauseOnLostFocus:false` and `skipMultiplayerWarning:true` to `options.txt`, put the jars in
`.minecraft/mods/` (Fabric: also the fabric-api jar from the Gradle cache). Never modify the InvTodo instances. If
Prism does not see a new instance ("resolves to nothing" in its log), rename the folder away and back. Launch:
`"%LOCALAPPDATA%\Programs\PrismLauncher\prismlauncher.exe" --launch <instance> [--server localhost:<port>]`
(the Microsoft account is Prism's default; never add accounts). Kill only processes you started.

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

Test servers (`..\whm-testenv\`, `online-mode=false`, flat world, the player `_junjun1z` is op):
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
- Helper scripts for instances, launches and test servers: `devtest/scripts/` (paths at the top of each script).
