package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.config.CfgFile;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.data.CatalogCache;
import io.github.junjun1z.whathitme.core.data.Context;
import io.github.junjun1z.whathitme.core.data.DamageIdScanner;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.MatchKey;
import io.github.junjun1z.whathitme.core.data.Stats;
import io.github.junjun1z.whathitme.core.server.CommandSource;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whathitme.core.text.Msg;
import io.github.junjun1z.whathitme.core.text.RecapFormatter;
import io.github.junjun1z.whathitme.core.text.Txt;
import io.github.junjun1z.whathitme.devtest.DevPort.Act;

/** Single player checks of the 1.2 features (versions whose DevPort says v12()). */
final class V12Plan {

    private final SelfTest t;
    private final DevPort p;

    V12Plan(SelfTest t) {
        this.t = t;
        this.p = t.port;
    }

    private static File whm() {
        return Platform.whmConfigDir();
    }

    private static void write(File f, String text) {
        try {
            f.getParentFile().mkdirs();
            Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** The jar this self-test runs from */
    private static File ownJar() {
        try {
            String u = V12Plan.class.getProtectionDomain().getCodeSource().getLocation().toString();
            if (u.startsWith("jar:")) u = u.substring(4, u.indexOf("!/") > 0 ? u.indexOf("!/") : u.length());
            File f = new File(new java.net.URI(u));
            if (f.isFile()) return f;
        } catch (Throwable ignored) {
            // look in mods/
        }
        File[] list = new File("mods").listFiles();
        if (list != null) for (File f : list) if (f.getName().startsWith("whathitme")) return f;
        return new File("mods");
    }

    private static String read(File f) {
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private DamageRecord last(String id) {
        return t.find(id);
    }

    private DamageRecord lastFlag(int flag) {
        DamageRecord found = null;
        if (t.serverHistory() == null) return null;
        for (DamageRecord r : t.serverHistory().records()) if (r.has(flag)) found = r;
        return found;
    }

    /** Server command as a non-operator console-less player: collects the plain text */
    private List<String> asNonOp(String... args) {
        List<String> out = new ArrayList<>();
        final PlayerHandle real = null;
        ServerCore.execute(new CommandSource() {
            @Override
            public PlayerHandle player() {
                return real;
            }

            @Override
            public boolean isAdmin() {
                return false;
            }

            @Override
            public void send(Txt line) {
                out.add(Fmt.strip(line.toPlain(Msg.ENGLISH_LANG)));
            }
        }, args);
        return out;
    }

    private static boolean has(List<String> lines, String s) {
        for (String l : lines) if (l.contains(s)) return true;
        return false;
    }

    void plan() {
        t.act(() -> {
            t.info("v12 section");
            Stats.forget(p.playerId());
            Stats.file(p.playerId()).delete();
        });
        t.sleep(80); // respawn invulnerability
        // ---- protocol / migration
        t.check("server info protocol 2", () -> ClientCore.serverInfo != null && ClientCore.serverInfo.protocol >= 2, null);
        t.check("old showHintsAndTips key migrated away", () -> WhmConfig.client.backend.raw(WhmConfig.CAT_DISPLAY, "showHintsAndTips") == null
            && WhmConfig.common.backend.raw(WhmConfig.CAT_DISPLAY, "showHintsAndTips") == null, null);
        t.check("migration: showHintsAndTips=false -> showHints=false (CfgFile)", () -> {
            File f = new File(whm(), ".cache/migrate_test.cfg");
            write(f, "display {\n    B:showHintsAndTips=false\n}\n");
            WhmConfig.Client c = new WhmConfig.Client(new CfgFile(f));
            c.load();
            boolean ok = (c.mask & WhmConfig.Flag.HINTS_AND_TIPS.bit()) == 0 && !read(f).contains("showHintsAndTips") && read(f).contains("showHints=false");
            f.delete();
            return ok;
        }, null);
        // ---- catalog cache
        t.check("catalog cache loaded at start (created or hit)", () -> CatalogCache.loaded() && CatalogCache.file().isFile(),
            (Supplier<?>) () -> CatalogCache.status);
        t.check("catalog cache: missing -> created, same mods -> hit, mod list changed -> rescanned", () -> {
            List<String> mods = CatalogCache.modList();
            CatalogCache.forget();
            CatalogCache.file().delete();
            CatalogCache.init(ClientCore.port.registries(), mods, false);
            String a = CatalogCache.status;
            CatalogCache.forget();
            CatalogCache.init(ClientCore.port.registries(), mods, false);
            String b = CatalogCache.status;
            List<String> changed = new ArrayList<>(mods);
            changed.add("fakemod:1.0");
            CatalogCache.init(ClientCore.port.registries(), changed, false);
            String c = CatalogCache.status;
            CatalogCache.init(ClientCore.port.registries(), mods, false);
            t.info("cache: " + a + " / " + b + " / " + c + ", damage types " + CatalogCache.damageTypeEntries().size());
            return a.equals("created") && b.equals("hit") && c.equals("rescanned") && CatalogCache.damageTypeEntries().size() > 20;
        }, (Supplier<?>) () -> CatalogCache.status);
        // ---- damage id scanner
        t.check("damage id scanner: background scan finished (or no scanner on this version)", () -> CatalogCache.awaitScan(60000)
            && Arrays.asList("done", "cached", "off").contains(CatalogCache.scanStatus), (Supplier<?>) () -> CatalogCache.scanStatus);
        t.act(() -> t.info("damage id scan: " + CatalogCache.scanStatus + " " + CatalogCache.scanInfo + ", cache init " + CatalogCache.initMillis + " ms"));
        t.check("damage id scanner finds the probe ids in our jar", () -> {
            DamageIdScanner.Input base = ClientCore.port.registries().scanInput();
            if (base == null) {
                t.info("no scanner on this version");
                return true;
            }
            File jar = ownJar();
            Map<File, String> files = new LinkedHashMap<>();
            files.put(jar, Platform.MODID);
            DamageIdScanner.Result r = DamageIdScanner.scan(new DamageIdScanner.Input(base.baseClasses, files, base.modIds, base.setters));
            Map<String, Integer> found = new LinkedHashMap<>();
            for (DamageIdScanner.Found f : r.found) found.put(f.id, f.flags);
            t.info("scan of " + jar.getName() + ": " + r + " " + found);
            Integer plain = found.get("whathitme:whm_probe_plain");
            Integer sub = found.get("whathitme:whm_probe_sub");
            boolean dyn = false;
            for (String id : found.keySet()) dyn |= id.startsWith("whathitme:whm_probe_dyn");
            return plain != null && (plain & (DamageRecord.FIRE | DamageRecord.BYPASS_ARMOR)) == (DamageRecord.FIRE | DamageRecord.BYPASS_ARMOR)
                && sub != null && (sub & DamageRecord.MAGIC) != 0 && found.containsKey("whathitme:whm_probe_named")
                && found.containsKey("whathitme:whm_probe_child") && found.containsKey("whathitme:whm_probe_anon") && !dyn && r.errors == 0;
        }, null);
        // ---- pack folder
        t.check("pack folder created with inert samples", () -> new File(whm(), "pack/hints/_example.json").isFile()
            && new File(whm(), "pack/triggers/_example.json").isFile() && WhmConfig.pack.triggers.isEmpty() && WhmConfig.pack.errors.isEmpty(),
            (Supplier<?>) () -> WhmConfig.pack.files + " " + WhmConfig.pack.errors);
        t.act(() -> {
            write(new File(whm(), "pack/test/a_first.json"), "{\n // first\n \"renameMap\": {\"minecraft:magic\": \"PackA\", \"minecraft:fall\": \"PackFall\"},\n"
                + " \"lockedFeatures\": [\"showInternalDamageId\"],\n}\n");
            write(new File(whm(), "pack/test/b_second.json"), "{\n \"renameMap\": {\"minecraft:magic\": \"PackB\", \"minecraft:lava @dim:minecraft:overworld\": \"Overworld Lava\",\n"
                + "   \"minecraft:lava\": \"Plain Lava\", \"@item:minecraft:iron_sword mob:minecraft:zombie\": \"Armed Zombie\"},\n"
                + " \"hintMap\": {\"minecraft:magic\": \"PackHint\"},\n"
                + " \"defaultIgnoredDamageIds\": [\"minecraft:cactus @dim:minecraft:the_end\"],\n"
                + " \"deathTriggers\": [\n"
                + "   {\"match\": \"minecraft:generic\", \"command\": \"say WHMTRIGGER {player} {damage}\"},\n"
                + "   {\"match\": \"minecraft:magic\", \"command\": \"scoreboard objectives add whmonce dummy\", \"oncePerPlayer\": true}\n"
                + " ]\n}\n");
        });
        t.cmd("/whm reload", "reloaded", 60);
        t.sleep(10);
        t.check("pack files merged in path order (b overrides a)", () -> "PackB".equals(WhmConfig.pack.renameMap.get("minecraft:magic"))
            && "PackFall".equals(WhmConfig.pack.renameMap.get("minecraft:fall")) && WhmConfig.pack.triggers.size() == 2,
            (Supplier<?>) () -> WhmConfig.pack.renameMap + " files=" + WhmConfig.pack.files + " errors=" + WhmConfig.pack.errors);
        t.check("pack rename reaches the client (server info)", () -> "PackB".equals(ClientCore.formatter().o.rename("minecraft:magic")),
            (Supplier<?>) () -> ClientCore.formatter().o.rename("minecraft:magic"));
        t.check("personal config beats the pack", () -> {
            WhmConfig.client.putMapEntry("renameMap", "minecraft:magic", "Mine");
            boolean ok = "Mine".equals(ClientCore.formatter().o.rename("minecraft:magic"));
            WhmConfig.client.putMapEntry("renameMap", "minecraft:magic", "");
            return ok;
        }, null);
        t.check("composite keys: most specific wins", () -> {
            MatchKey.Target ow = new MatchKey.Target("minecraft:lava", "", "", "minecraft:overworld");
            MatchKey.Target ne = new MatchKey.Target("minecraft:lava", "", "", "minecraft:the_nether");
            MatchKey.Target armed = new MatchKey.Target("mob:minecraft:zombie", "minecraft:zombie", "minecraft:iron_sword", "minecraft:overworld");
            MatchKey.Target bare = new MatchKey.Target("mob:minecraft:zombie", "minecraft:zombie", "", "minecraft:overworld");
            MatchKey.Target endCactus = new MatchKey.Target("minecraft:cactus", "", "", "minecraft:the_end");
            MatchKey.Target owCactus = new MatchKey.Target("minecraft:cactus", "", "", "minecraft:overworld");
            return "Overworld Lava".equals(ClientCore.formatter().o.rename(ow)) && "Plain Lava".equals(ClientCore.formatter().o.rename(ne))
                && "Armed Zombie".equals(ClientCore.formatter().o.rename(armed)) && ClientCore.formatter().o.rename(bare) == null
                && MatchKey.parse("@mob:minecraft:* @dim:*nether").matches(new MatchKey.Target("x", "minecraft:blaze", "", "minecraft:the_nether"))
                && !ClientCore.formatter().o.ignored("x")
                && MatchKey.parse("minecraft:cactus @dim:minecraft:the_end").matches(endCactus) && !MatchKey.parse("minecraft:cactus @dim:minecraft:the_end").matches(owCactus);
        }, null);
        t.check("composite keys: @ without separator, wildcard priority", () -> {
            MatchKey.Target lich = new MatchKey.Target("twilightforest:lich_bolt", "twilightforest:lich", "", "minecraft:overworld");
            Map<String, String> m = new LinkedHashMap<>();
            m.put("*@twilightforest:lich", "A");
            m.put("@twilightforest:lich", "B");
            m.put("*", "Z");
            boolean tie = "B".equals(MatchKey.best(m, MatchKey.parseAll(m.keySet()), lich));
            m.put("twilightforest:lich_bolt@twilightforest:lich", "C");
            boolean specific = "C".equals(MatchKey.best(m, MatchKey.parseAll(m.keySet()), lich));
            boolean catchAll = "Z".equals(MatchKey.best(m, MatchKey.parseAll(m.keySet()), MatchKey.Target.id("minecraft:fall")));
            return tie && specific && catchAll && MatchKey.parse("*@twilightforest:lich").matches(lich)
                && MatchKey.parse("*lich*").matches(lich) && !MatchKey.parse("*@twilightforest:lich").matches(MatchKey.Target.id("minecraft:fall"));
        }, null);
        t.check("pack hint used as tip", () -> {
            DamageRecord r = new DamageRecord(0, "minecraft:magic", "magic", "", "", "", "", "minecraft", "Minecraft", 1, 1, 0, 0, -1, "", "", "", "", 1, 1, 20, "");
            return ClientCore.formatter().tip(r).toPlain(Msg.ENGLISH_LANG).contains("PackHint");
        }, null);
        t.check("a bare translation key as rename value is translated", () -> {
            WhmConfig.client.putMapEntry("renameMap", "minecraft:anvil", "whathitme.damage.indirectMagic");
            String v = ClientCore.formatter().custom(ClientCore.formatter().o.rename("minecraft:anvil")).toPlain(Msg.LOCAL.lang());
            WhmConfig.client.putMapEntry("renameMap", "minecraft:anvil", "");
            return !v.contains("whathitme.damage");
        }, null);
        t.check("lockedFeatures: greyed out in the config screen", () -> p.configGuiLocked("showInternalDamageId") && !p.configGuiLocked("showTimestamp"), null);
        t.cmd("/whm config showInternalDamageId false", "locked by the modpack", 40);
        t.check("locked key unchanged", () -> (WhmConfig.client.mask & WhmConfig.Flag.INTERNAL_DAMAGE_ID.bit()) != 0, null);
        // ---- config screen from /whm config
        t.act(() -> p.command("/whm config"));
        t.waitFor("/whm config opens the config screen", p::configScreenOpen, 40);
        t.sleep(10);
        t.screenshot("whm_selftest_configscreen");
        t.act(p::closeScreen);
        t.sleep(5);
        // ---- /whm test
        t.cmd("/whm test minecraft:generic", "Chat preview", 60);
        t.check("/whm test shows name, tip and the matching death trigger (not run)", () -> t.chatHas("Test: minecraft:generic") && t.chatHas("WHMTRIGGER")
            && t.chatHas("Killing Blow"), (Supplier<?>) t::chatText);
        t.cmd("/whm test mob:minecraft:zombie - minecraft:iron_sword", "Armed Zombie", 60);
        t.check("tab completion of /whm test", () -> ClientCore.complete(new String[] {"test", "minecraft:lav"}).contains("minecraft:lava")
            && ClientCore.complete(new String[] {"test", "x", "minecraft:zom"}).contains("minecraft:zombie"), null);
        // ---- reflected damage
        t.srv(Act.HEAL);
        t.hit(Act.THORNS, 2f);
        t.check("thorns: Reflected flag", () -> last(SelfTest.id("thorns")) != null && last(SelfTest.id("thorns")).has(DamageRecord.REFLECTED), (Supplier<?>) t::ids);
        t.hit(Act.REFLECT, 2f);
        t.check("damage from the entity just hit (< 50 ms): Reflected", () -> last("mob:minecraft:zombie").has(DamageRecord.REFLECTED), (Supplier<?>) t::ids);
        t.cmd("/whm", "Reflected / Thorns", 60);
        t.check("history shows max HP percentage", () -> t.chatHas("% Max HP"), (Supplier<?>) t::chatText);
        // ---- armor break
        t.srv(Act.BRITTLE_HELMET);
        t.sleep(2);
        t.hit(Act.ZOMBIE, 4f);
        t.check("armor break line recorded", () -> {
            DamageRecord r = lastFlag(DamageRecord.ARMOR_BREAK);
            return r != null && r.context.contains("Diamond Helmet");
        }, (Supplier<?>) t::ids);
        t.cmd("/whm", "Broken: Diamond Helmet", 60);
        // ---- environment context (+ merge reuses the first hit's world read)
        t.srv(Act.ENV_SETUP);
        t.sleep(2);
        t.hit(Act.CACTUS, 1f);
        t.srv(Act.ENV_CLEAR);
        t.sleep(2);
        t.hit(Act.CACTUS, 1f);
        t.check("environment: feet block + held item, kept for merged hits", () -> {
            DamageRecord r = last(SelfTest.id("cactus"));
            Context.Part feet = r == null ? null : Context.find(r.context, Context.FEET);
            Context.Part main = r == null ? null : Context.find(r.context, Context.MAIN);
            return r.hits == 2 && feet != null && feet.name.contains("Cobweb") && "minecraft".equals(feet.modId) && main != null && main.name.contains("Stick");
        }, (Supplier<?>) () -> {
            DamageRecord r = last(SelfTest.id("cactus"));
            return r == null ? t.ids() : r.context + " hits=" + r.hits;
        });
        t.cmd("/whm", "Cactus", 60);
        // ---- hint switches
        t.check("enableAutoHints=false: no automatic tip", () -> {
            WhmConfig.common.set("enableAutoHints", "false");
            DamageRecord r = new DamageRecord(0, "minecraft:fall", "fall", "", "", "", "", "minecraft", "Minecraft", 1, 1, 0, 0, -1, "", "", "", "", 1, 1, 20, "");
            boolean off = new RecapFormatter(Msg.ENGLISH, ServerCore.options(null, false)).tip(r) == null;
            WhmConfig.common.set("enableAutoHints", "true");
            return off && new RecapFormatter(Msg.ENGLISH, ServerCore.options(null, false)).tip(r) != null;
        }, null);
        // ---- silent fail
        t.check("a broken damage source does not throw", () -> {
            int before = Safe.count("reading a damage source");
            PlayerHandle h = ServerCore.port.player(p.playerId());
            ServerCore.onHurt(h, 1L, "x", 1f, 0f, 20f, false, true, 20);
            ServerCore.onDamage(h, 1L, "x", 0.5f, 0f, 20f, () -> {
                throw new IllegalStateException("bad mod source");
            });
            return Safe.count("reading a damage source") == before + 1 && t.find("unknown:unknown") != null;
        }, (Supplier<?>) t::ids);
        // ---- permissions of a non-operator (server command)
        t.check("non-op: rename / test / dump / reload / config change refused", () -> has(asNonOp("rename", "minecraft:fall", "x"), "operator permission")
            && has(asNonOp("test", "minecraft:fall"), "operator permission") && has(asNonOp("dump"), "operator permission")
            && has(asNonOp("reload"), "operator permission") && has(asNonOp("config", "historySize", "20"), "operator permission"), null);
        t.check("non-op: config view, search, help allowed", () -> has(asNonOp("config", "historySize"), "historySize = ") && has(asNonOp("search", "fall"), "Search:")
            && has(asNonOp("help"), "commands"), null);
        // ---- death with health left (vital hit), equipment, death trigger, stats
        t.srv(Act.GEAR);
        t.sleep(40);
        t.clearChat();
        t.srv(Act.VITAL_DEATH);
        t.waitFor("vital death: death screen", p::deathScreen, 100);
        t.waitFor("vital death recap", () -> ClientCore.currentDeath != null && ClientCore.currentDeath.key() != null
            && ClientCore.currentDeath.key().has(DamageRecord.VITAL), 100);
        t.check("vital hit: 5 hearts left, equipment in the recap", () -> Math.abs(ClientCore.currentDeath.key().healthAfter - 10f) < 0.01f
            && ClientCore.currentDeath.gear != null && ClientCore.currentDeath.gear.armor[1] != null, null);
        t.waitFor("death trigger ran (console say)", () -> t.chatHas("WHMTRIGGER " + p.playerName()), 60);
        t.sleep(25);
        t.act(p::respawn);
        t.waitFor("respawned", () -> p.health() > 0f && p.noScreen(), 200);
        t.waitFor("summary has the vital hit note", () -> t.chatHas("Vital Hit") && t.chatHas("5 ❤ total HP left"), 100);
        t.cmd("/whm copyrecap", "Death recap copied", 40);
        t.waitFor("copy recap has equipment (Markdown)", () -> {
            String c = p.clipboard();
            return c.contains("## Equipment") && c.contains("Max health: 20.0") && c.contains("Iron Chestplate") && c.contains("Slowness") && c.contains("Position:");
        }, 40);
        t.act(() -> t.info("clipboard: " + p.clipboard().replace('\n', '|')));
        // setHealth(0)-style death without any hurt event
        t.sleep(40);
        t.srv(Act.SETHEALTH_DEATH);
        t.waitFor("death without hurt event: recap from the death source", () -> ClientCore.currentDeath != null && ClientCore.currentDeath.key() != null
            && SelfTest.id("magic").equals(ClientCore.currentDeath.key().damageId) && !ClientCore.currentDeath.key().has(DamageRecord.VITAL), 100);
        t.check("oncePerPlayer trigger ran once", () -> {
            Stats st = Stats.peek(p.playerId());
            return st != null && st.triggersRun.size() == 1;
        }, null);
        t.sleep(25);
        t.act(p::respawn);
        t.waitFor("respawned", () -> p.health() > 0f && p.noScreen(), 200);
        t.check("stats saved on death and loaded again", () -> {
            UUID id = p.playerId();
            int deaths = Stats.of(id).deaths;
            File f = Stats.file(id);
            boolean saved = f.isFile() && read(f).contains("\"deaths\": " + deaths);
            Stats.forget(id);
            return saved && Stats.of(id).deaths == deaths && deaths >= 2 && Stats.of(id).causes.containsKey(SelfTest.id("magic"));
        }, (Supplier<?>) () -> read(Stats.file(p.playerId())));
        t.cmd("/whm stats", "Death Statistics", 60);
        t.check("stats lists causes", () -> t.chatHas("Deaths: ") && t.chatHas("Top causes"), (Supplier<?>) t::chatText);
        t.cmd("/whm dump", "Wrote", 80);
        t.check("dump csv written to .cache", () -> new File(whm(), ".cache/whathitme_dump.csv").length() > 1000, null);
        // clean up the pack test files
        t.act(() -> {
            for (String n : Arrays.asList("a_first.json", "b_second.json")) new File(whm(), "pack/test/" + n).delete();
            new File(whm(), "pack/test").delete();
        });
        t.cmd("/whm reload", "reloaded", 60);
    }
}
