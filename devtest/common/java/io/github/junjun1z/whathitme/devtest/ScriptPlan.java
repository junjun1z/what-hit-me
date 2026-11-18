package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.rules.Rule;
import io.github.junjun1z.whathitme.core.script.ScriptRegistry;
import io.github.junjun1z.whathitme.core.text.Fmt;
import io.github.junjun1z.whmdriver.VanillaCommands;

/**
 * Script integration (flag "scripts"; flag "mp-scripts" against a server with the script mod). The instance has KubeJS or
 * CraftTweaker with the sample test script of whm-tools/script13/scripts, or no script mod at all: then the script:
 * conditions must count as false with a reload warning and nothing else may change. The test script only acts while the
 * player has the tag whmt_ev, so the other plans are not disturbed.
 */
final class ScriptPlan {

    static final String OWNER_PREFIX = "script:";

    private final SelfTest t;
    private final DevPort p;
    private final boolean modern;
    private final boolean tags;

    ScriptPlan(SelfTest t) {
        this.t = t;
        this.p = t.port;
        this.modern = VanillaCommands.of(p.mcVersion()).atLeast(1, 13, 0);
        this.tags = VanillaCommands.of(p.mcVersion()).atLeast(1, 9, 0);
    }

    static boolean scriptMod() {
        java.util.Collection<String> ids = Platform.mods.modIds();
        for (String id : ids) if ("minetweaker3".equalsIgnoreCase(id)) return true;
        return ids.contains("kubejs") || ids.contains("crafttweaker");
    }

    /** MineTweaker 3 or CraftTweaker 3.x (mod id crafttweaker on 1.11.2) */
    private boolean minetweaker() {
        for (String id : Platform.mods.modIds()) if ("minetweaker3".equalsIgnoreCase(id)) return true;
        return Platform.mods.modIds().contains("crafttweaker") && !VanillaCommands.of(p.mcVersion()).atLeast(1, 12, 0);
    }

    private boolean has(String s) {
        return Fmt.strip(t.chatText()).contains(s);
    }

    /** Scoreboard tag (1.9+) and a score of the same name (every version: the MineTweaker test script checks scores) */
    private void tag(boolean add, String tag) {
        if (tags) vanilla(modern ? "/tag @p " + (add ? "add " : "remove ") + tag : "/scoreboard players tag @p " + (add ? "add " : "remove ") + tag, 10);
        vanilla("/scoreboard players set @p " + tag + " " + (add ? 1 : 0), 10);
    }

    private void vanilla(String cmd, int wait) {
        t.act(() -> {
            t.info("send " + cmd);
            p.say(cmd);
        });
        t.sleep(wait);
    }

    private static void write(File f, String text) {
        try {
            f.getParentFile().mkdirs();
            Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static File packFile() {
        return new File(Platform.whmConfigDir(), "pack/testscripts/scripts.json");
    }

    static String pack() {
        String gen = SelfTest.id("generic");
        String fall = SelfTest.id("fall");
        return "{\n \"rules\": [\n"
            + "  {\"match\": \"" + gen + "\", \"if\": [\"script:whmt_tagged\"], \"name\": \"WHMT-SCOND\"},\n"
            + "  {\"match\": \"" + fall + "\", \"if\": [\"script:whmt_throws\"], \"name\": \"WHMT-THROWN\"},\n"
            + "  {\"match\": \"minecraft:cactus\", \"if\": [\"script:whmt_unknown\"], \"name\": \"never\"}\n"
            + " ],\n"
            + " \"templates\": {\"line\": [\"{name} {badges} WHMV:{script:whmt_var|none}\"]}\n}\n";
    }

    private int scriptRules() {
        int n = 0;
        for (Rule r : WhmConfig.pack.rules) if (r.source.startsWith(OWNER_PREFIX)) n++;
        return n;
    }

    private void whmTest(String id) {
        t.clearChat();
        t.act(() -> p.command("/whm test " + id));
        t.waitFor("/whm test " + id, () -> has("Chat preview"), 80);
    }

    void plan() {
        t.loadWorld("whm_scripts");
        t.waitFor("server info received (server has the mod)", () -> ClientCore.serverHasMod, 400);
        t.srv(DevPort.Act.SETUP);
        t.srv(DevPort.Act.SPAWN_MOBS);
        t.sleep(100);
        vanilla("/scoreboard objectives add whmt_s dummy", 10);
        vanilla("/scoreboard objectives add whmt_ev dummy", 10);
        t.act(() -> write(packFile(), pack()));
        t.cmd("/whm reload", "reloaded", 60);
        t.sleep(10);
        t.act(() -> t.info("reload chat: " + t.chatText().replace('\n', '|')));
        if (!scriptMod()) {
            planWithout();
        } else {
            planWith();
        }
        t.act(() -> {
            File f = packFile();
            f.delete();
            f.getParentFile().delete();
        });
        t.cmd("/whm reload", "reloaded", 60);
    }

    /** No script mod: script: conditions false with a warning, no script line, nothing registered */
    private void planWithout() {
        t.check("reload warns: unknown script condition (script mod missing)", () -> has("unknown script condition \"whmt_tagged\"") && has("is the script mod installed"),
            (Supplier<?>) t::chatText);
        t.check("reload: no script summary line", () -> !has("Scripts:"), (Supplier<?>) t::chatText);
        t.check("nothing registered", () -> ScriptRegistry.registrations().isEmpty(), (Supplier<?>) ScriptRegistry::registrations);
        tag(true, "whmt_s");
        whmTest(SelfTest.id("generic"));
        t.check("script: condition false without the script mod", () -> !has("WHMT-SCOND") && has("Rule: none matches"), (Supplier<?>) t::chatText);
        tag(false, "whmt_s");
        t.hit(DevPort.Act.MAGIC, 2f);
        t.check("damage still recorded", () -> t.find(SelfTest.id("magic")) != null, (Supplier<?>) t::ids);
        t.cmd("/whm", "WHMV:none", 60);
    }

    private void planWith() {
        t.check("reload lists the script registrations", () -> has("Scripts:") && has("whmt_tagged") && has("whmt_var") && has("damageRecorded"), (Supplier<?>) t::chatText);
        t.check("reload: no warning for registered conditions, a warning for the unknown one", () -> !has("unknown script condition \"whmt_tagged\"")
            && has("unknown script condition \"whmt_unknown\""), (Supplier<?>) t::chatText);
        t.check("register event entries merged (rename, rule)", () -> "WHMT-RUNTIME".equals(WhmConfig.pack.renameMap.get("whmt:runtime")) && scriptRules() == 1,
            (Supplier<?>) () -> WhmConfig.pack.renameMap + " rules=" + scriptRules());
        t.check("top level entry merged (hint)", () -> "WHMT-DIRECT".equals(WhmConfig.pack.hintMap.get("whmt:direct")), (Supplier<?>) () -> WhmConfig.pack.hintMap);
        t.check("editor variable list has the script variable", () -> ClientCore.scriptVariables().containsKey("whmt_var"), (Supplier<?>) ClientCore::scriptVariables);
        // conditions
        whmTest(SelfTest.id("generic"));
        t.check("script condition false (no tag)", () -> !has("WHMT-SCOND"), (Supplier<?>) t::chatText);
        tag(true, "whmt_s");
        whmTest(SelfTest.id("generic"));
        t.check("script condition true (tag) -> rule name", () -> has("Name: WHMT-SCOND"), (Supplier<?>) t::chatText);
        whmTest("mob:minecraft:zombie");
        t.check("script rule from the register event", () -> has("WHMT-SRULE"), (Supplier<?>) t::chatText);
        tag(false, "whmt_s");
        whmTest(SelfTest.id("fall"));
        t.check("throwing script condition counts as false", () -> !has("WHMT-THROWN"), (Supplier<?>) t::chatText);
        t.cmd("/whm reload", "reloaded", 60);
        t.sleep(10);
        t.check("reload warns about the failed condition", () -> has("whmt_throws") && has("failed"), (Supplier<?>) t::chatText);
        // events (the script acts only with the tag whmt_ev)
        tag(true, "whmt_ev");
        t.srv(DevPort.Act.HEAL);
        t.hit(DevPort.Act.MAGIC, 2f);
        t.check("damageRecorded: name and badge set", () -> {
            DamageRecord r = t.find(SelfTest.id("magic"));
            return r != null && r.mark != null && "WHMT-EVNAME".equals(r.mark.name) && r.mark.badges.contains("[WHMT-BADGE]");
        }, (Supplier<?>) () -> String.valueOf(t.find(SelfTest.id("magic")) == null ? t.ids() : t.find(SelfTest.id("magic")).mark));
        t.hit(DevPort.Act.IN_FIRE, 2f);
        t.check("damageRecorded: cancelled hit not recorded", () -> t.find(SelfTest.id("inFire")) == null, (Supplier<?>) t::ids);
        t.srv(DevPort.Act.EXTINGUISH);
        t.cmd("/whm", "WHMT-EVNAME", 60);
        t.check("history line: badge and {script:whmt_var} rendered on the client", () -> has("[WHMT-BADGE]") && has("WHMV:V-magic"), (Supplier<?>) t::chatText);
        // alert
        t.srv(DevPort.Act.HEAL);
        t.sleep(2);
        t.hit(DevPort.Act.GENERIC, 17f);
        t.waitFor("alert event ran", () -> "WHMT-ALERT-heavy".equals(WhmConfig.pack.renameMap.get("whmt:alert")), 80);
        // death + respawn
        t.clearChat();
        t.srv(DevPort.Act.VOID, 1000f);
        t.waitFor("death screen open", p::deathScreen, 100);
        t.waitFor("death recap with the script summary line", () -> ClientCore.currentDeath != null && ClientCore.currentDeath.key() != null
            && ClientCore.currentDeath.key().mark != null && ClientCore.currentDeath.key().mark.lines.size() == 1, 100);
        t.sleep(10);
        t.act(p::respawn);
        t.waitFor("respawned", () -> p.health() > 0f && p.noScreen(), 200);
        t.waitFor("respawn summary with the death event line and tip", () -> has("WHMT-SUMMARY " + p.playerName()) && has("WHMT-DEATHHINT"), 100);
        t.act(() -> t.info("summary: " + t.chatText().replace('\n', '|')));
        t.waitFor("respawn event read lastDeath / history / stats / test", () -> "outOfWorld".equals(WhmConfig.pack.hintMap.get("whmt:last"))
            && WhmConfig.pack.hintMap.containsKey("whmt:history") && WhmConfig.pack.hintMap.containsKey("whmt:stats") && WhmConfig.pack.hintMap.containsKey("whmt:test"), 80);
        t.check("read API values", () -> Integer.parseInt(WhmConfig.pack.hintMap.get("whmt:stats")) >= 1 && Integer.parseInt(WhmConfig.pack.hintMap.get("whmt:history")) >= 1
            && !WhmConfig.pack.hintMap.get("whmt:test").isEmpty(), (Supplier<?>) () -> WhmConfig.pack.hintMap);
        tag(false, "whmt_ev");
        // lifecycle: /whm reload twice -> the same registrations, no duplicates
        t.cmd("/whm reload", "reloaded", 60);
        t.cmd("/whm reload", "reloaded", 60);
        t.check("no duplicates after two reloads", () -> scriptRules() == 1 && ScriptRegistry.conditions().size() == 2 && ScriptRegistry.variables().size() == 1,
            (Supplier<?>) () -> "rules=" + scriptRules() + " " + ScriptRegistry.registrations());
        t.check("entries made by listeners stay over /whm reload", () -> "WHMT-ALERT-heavy".equals(WhmConfig.pack.renameMap.get("whmt:alert")), null);
        // the script mod's own reload: everything of the scripts is registered again
        // KubeJS and CraftTweaker 1.16.5+ run their server scripts again on /reload, MineTweaker on /minetweaker reload
        // (CraftTweaker 1.12.2 does not)
        // (KubeJS 1.12.2: /kubejs reload)
        boolean kubejs = Platform.mods.modIds().contains("kubejs");
        String reload = kubejs && !modern ? "/kubejs reload" : kubejs || modern && Platform.mods.modIds().contains("crafttweaker") ? "/reload"
            : minetweaker() ? "/minetweaker reload" : null;
        if (reload != null) {
            t.clearChat();
            vanilla(reload, 200);
            t.check("after /reload: rebuilt once, listener entries gone, top level entries back", () -> scriptRules() == 1 && ScriptRegistry.conditions().size() == 2
                && !WhmConfig.pack.renameMap.containsKey("whmt:alert") && "WHMT-DIRECT".equals(WhmConfig.pack.hintMap.get("whmt:direct")),
                (Supplier<?>) () -> "rules=" + scriptRules() + " " + WhmConfig.pack.renameMap + " " + ScriptRegistry.registrations());
        }
    }

    /** Against a dedicated server with the script mod (its pack has the line template); the variable comes from the server */
    void planMp() {
        VanillaCommands vc = VanillaCommands.of(p.mcVersion());
        t.waitFor("connected to the server", p::connected, 2400);
        t.sleep(80);
        t.act(() -> t.autoRespawn = true);
        t.waitFor("server info received (server has the mod)", () -> ClientCore.serverHasMod, 400);
        t.check("server sent its script variable list", () -> ClientCore.scriptVariables().containsKey("whmt_var"), (Supplier<?>) ClientCore::scriptVariables);
        // KubeJS on the client adds its bridge listeners, but no script runs there in multiplayer. MineTweaker sends its
        // scripts to the clients and may run them there too: such registrations are unused on a client
        if (minetweaker()) t.info("client registrations (MineTweaker may run the server's scripts on clients): " + ScriptRegistry.registrations().size());
        else t.check("the client itself has no script variables / conditions", () -> !ScriptRegistry.hasVariables() && ScriptRegistry.conditions().isEmpty(),
            (Supplier<?>) ScriptRegistry::registrations);
        vanilla("/gamemode survival", 5);
        vanilla(vc.effectClear(), 5);
        vanilla(vc.effect("instant_health", 1, 10), 20);
        vanilla("/tp @p ~ ~8 ~", 80);
        t.cmd("/whm", "WHMV:V-fall", 100);
        t.check("{script:whmt_var} of the server shown by the client template", () -> has("WHMV:V-fall"), (Supplier<?>) t::chatText);
    }
}
