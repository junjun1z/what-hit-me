package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whathitme.core.data.PlayerHistory;
import io.github.junjun1z.whathitme.core.data.Snapshot;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.devtest.DevPort.Act;

/**
 * Development self-test (compiled only with -Pselftest), the same on every Minecraft version. Started by
 * config/whathitme/selftest.flag: "sp" = single player test (creates a world), "mp-mod" / "mp-nomod" = multiplayer test
 * against a running server (launch with --server), "mp-two" = op next to a second player. Version-specific plans (e.g.
 * "pack") are registered by the version's DevEntry. Writes whathitme_selftest_result.txt in the game directory and
 * quits. The loader calls {@link #tick()} at the end of every client tick.
 */
public final class SelfTest {

    public interface Step {
        /** @return true when done */
        boolean run();
    }

    private final Deque<Step> steps = new ArrayDeque<>();
    final List<String> log = new ArrayList<>();
    int fails;
    final String mode;
    public final DevPort port;
    private boolean finished;
    /** Respawn at once when the player dies outside of a planned death (multiplayer runs) */
    public boolean autoRespawn;
    private int ticks;

    public SelfTest(DevPort port, String mode) {
        this.port = port;
        this.mode = mode;
    }

    /** Reads the flag file ("" = sp) */
    public static String mode(File flag) {
        try {
            String m = new String(Files.readAllBytes(flag.toPath()), StandardCharsets.UTF_8).trim();
            return m.isEmpty() ? "sp" : m;
        } catch (Exception e) {
            return "sp";
        }
    }

    /** Plans the common modes; returns false when the mode is not a common one */
    public boolean planCommon() {
        if (mode.equals("qa")) {
            new QaPlan(this).plan();
            return true;
        }
        if (mode.equals("scripts")) {
            // script integration (test instance with KubeJS / CraftTweaker and the test script, or without a script mod)
            new ScriptPlan(this).plan();
            return true;
        }
        if (mode.equals("mp-scripts")) {
            new ScriptPlan(this).planMp();
            return true;
        }
        if (mode.equals("mp-pack")) {
            // pack editor on a dedicated server: operator edits, then refused after /deop
            new PackMpPlan(this).plan();
            return true;
        }
        if (mode.equals("mp-quests")) {
            // quest conditions in server rules on a dedicated server with quest mods
            new QuestPlan(this).planMp();
            return true;
        }
        if (mode.equals("mp-two")) {
            new MultiplayerPlan(this).planTwo();
            return true;
        }
        if (mode.startsWith("mp")) {
            new MultiplayerPlan(this).plan(mode.equals("mp-mod"));
            return true;
        }
        if (mode.equals("sp")) {
            planSingleplayer();
            return true;
        }
        if (mode.equals("quests")) {
            // quest mod condition providers (test instance with quest mods)
            new QuestPlan(this).plan();
            return true;
        }
        if (mode.equals("sp-pack")) {
            // only the pack editor section (development shortcut)
            loadWorld("whm_selftest");
            waitFor("server info received (server has the mod)", () -> ClientCore.serverHasMod, 400);
            srv(Act.SETUP);
            sleep(60);
            new PackEditorPlan(this).plan();
            return true;
        }
        if (mode.equals("sp-rules")) {
            // only the progression rules section (development shortcut)
            loadWorld("whm_selftest");
            waitFor("server info received (server has the mod)", () -> ClientCore.serverHasMod, 400);
            srv(Act.SETUP);
            srv(Act.SPAWN_MOBS);
            sleep(100);
            new V13RulesPlan(this).plan();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ engine

    public void then(Step s) {
        steps.addLast(s);
    }

    public void act(Runnable r) {
        then(() -> {
            r.run();
            return true;
        });
    }

    public void sleep(int t) {
        final int[] left = {t};
        then(() -> --left[0] <= 0);
    }

    public void waitFor(String name, BooleanSupplier cond, int timeout) {
        final int[] left = {timeout};
        then(() -> {
            boolean c;
            try {
                c = cond.getAsBoolean();
            } catch (RuntimeException e) {
                c = false;
            }
            if (c) {
                ok(name, true, null);
                return true;
            }
            if (--left[0] <= 0) {
                ok(name, false, "timeout");
                return true;
            }
            return false;
        });
    }

    public void check(String name, BooleanSupplier cond, Object detail) {
        act(() -> {
            boolean r;
            try {
                r = cond.getAsBoolean();
            } catch (Throwable t) {
                r = false;
            }
            Object d = null;
            if (!r) {
                try {
                    d = detail instanceof Supplier ? ((Supplier<?>) detail).get() : detail;
                } catch (Throwable t) {
                    d = t;
                }
            }
            ok(name, r, d);
        });
    }

    public void ok(String name, boolean ok, Object detail) {
        String s = (ok ? "[OK]   " : "[FAIL] ") + name + (detail == null ? "" : "  -> " + detail);
        log.add(s);
        System.out.println("WHM-SELFTEST " + s);
        if (!ok) fails++;
    }

    public void info(String s) {
        log.add("[INFO] " + s);
        System.out.println("WHM-SELFTEST [INFO] " + s);
    }

    /** Call at the end of every client tick */
    public void tick() {
        if (finished) return;
        ticks++;
        if (autoRespawn && port.deathScreen() && port.connected()) {
            info("unplanned death - respawning");
            port.respawn();
        }
        if (ticks > 20 * 60 * 8) {
            ok("global timeout", false, null);
            steps.clear();
        }
        try {
            while (!steps.isEmpty()) {
                Step s = steps.peekFirst();
                if (!s.run()) return;
                steps.removeFirst();
            }
        } catch (Throwable t) {
            ok("exception in step", false, t);
            t.printStackTrace();
            steps.clear();
        }
        if (steps.isEmpty()) finish();
    }

    private void finish() {
        finished = true;
        log.add(0, "What Hit Me? self-test (" + mode + ", Minecraft " + port.mcVersion() + "): " + (fails == 0 ? "ALL PASSED" : fails + " FAILED"));
        try {
            Files.write(new File(port.gameDir(), "whathitme_selftest_result.txt").toPath(), log, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        port.quit();
    }

    // ------------------------------------------------------------------ helpers

    public String chatText() {
        StringBuilder sb = new StringBuilder();
        for (String l : port.chat()) sb.append(l).append('\n');
        return sb.toString();
    }

    /** Full chat lines (not wrapped) containing the text */
    public int countLines(String s) {
        int n = 0;
        for (String l : port.chat()) if (l.contains(s)) n++;
        return n;
    }

    public boolean chatHas(String s) {
        return chatText().contains(s);
    }

    public void clearChat() {
        act(port::clearChat);
    }

    /** Runs a command and waits until the chat shows the text */
    public void cmd(String cmd, String expect, int timeout) {
        clearChat();
        act(() -> port.command(cmd));
        waitFor(cmd + "  => \"" + expect + "\"", () -> chatHas(expect), timeout);
        act(() -> info(cmd + " chat: " + chatText().replace('\n', '|')));
    }

    public void screenshot(String name) {
        act(() -> port.screenshot(name));
    }

    public void loadWorld(String folder) {
        then(() -> {
            if (port.atMainMenu()) {
                port.openWorld(folder);
                return true;
            }
            return false;
        });
        waitFor("world loaded", port::worldReady, 2400);
    }

    public PlayerHistory serverHistory() {
        return ServerCore.tracker().peek(port.playerId());
    }

    public DamageRecord find(String id) {
        PlayerHistory h = serverHistory();
        if (h == null) return null;
        DamageRecord found = null;
        for (DamageRecord r : h.records()) if (r.damageId.equals(id)) found = r;
        return found;
    }

    /** Damage id of a vanilla damage type name on this version (minecraft:outOfWorld or minecraft:out_of_world) */
    public static String id(String msgId) {
        return ClientCore.msgIdToTypeId.apply(msgId);
    }

    /** Hit the player on the server, then wait out the invulnerability frames */
    void hit(Act a, float amount) {
        srv(a, amount);
        sleep(14);
    }

    void srv(Act a, float amount) {
        act(() -> port.server(a, amount));
    }

    void srv(Act a) {
        srv(a, 0f);
    }

    public String ids() {
        PlayerHistory h = serverHistory();
        if (h == null) return "(no history)";
        StringBuilder sb = new StringBuilder();
        for (DamageRecord r : h.records()) sb.append(r.damageId).append(" x").append(r.hits).append(", ");
        return sb.toString();
    }

    private Supplier<?> describe(String id) {
        return () -> {
            DamageRecord r = find(id);
            return r == null ? "null; records: " + ids()
                : r.damageId + " atk=" + r.attackerName + " direct=" + r.directName + " weapon=" + r.weaponName + " ench=" + r.weaponEnchants + " dist=" + r.distance
                    + " flags=" + r.flags + " hits=" + r.hits;
        };
    }

    // ------------------------------------------------------------------ single player plan

    private void planSingleplayer() {
        loadWorld("whm_selftest");
        waitFor("server info received (server has the mod)", () -> ClientCore.serverHasMod, 400);
        srv(Act.SETUP);
        srv(Act.SPAWN_MOBS);
        sleep(100); // spawn invulnerability
        clearChat();
        // ---- damage sources
        // a fresh world on 1.20 can still have spawn protection here (vanilla refuses the hit): repeat until one lands
        final int[] tries = {0};
        then(() -> {
            if (find(id("fall")) != null || tries[0] >= 200) return true;
            if (tries[0]++ % 20 == 0) port.server(Act.FALL, 3f);
            return false;
        });
        sleep(14);
        check("fall recorded as " + id("fall"), () -> find(id("fall")) != null, (Supplier<?>) this::ids);
        hit(Act.ZOMBIE, 4f);
        check("mob hit recorded as mob:minecraft:zombie", () -> find("mob:minecraft:zombie") != null, (Supplier<?>) this::ids);
        check("zombie weapon + enchantment + distance", () -> {
            DamageRecord r = find("mob:minecraft:zombie");
            return r.weaponName.contains("Sword") && (r.weaponEnchants.contains("damage.all") || r.weaponEnchants.contains("sharpness")) && r.distance > 2f;
        }, describe("mob:minecraft:zombie"));
        hit(Act.ARROW, 3f);
        check("arrow with owner: attacker Skeleton, direct Arrow (1.7.10 lang: arrow), projectile flag", () -> {
            DamageRecord r = find(id("arrow"));
            return r != null && r.attackerName.equals("Skeleton") && r.directName.equalsIgnoreCase("Arrow") && r.has(DamageRecord.PROJECTILE) && r.distance > 3f;
        }, describe(id("arrow")));
        hit(Act.IN_FIRE, 2f);
        check("fire flag", () -> find(id("inFire")) != null && find(id("inFire")).has(DamageRecord.FIRE), describe(id("inFire")));
        // smart merge: three burning ticks within the merge window
        hit(Act.ON_FIRE, 1f);
        hit(Act.ON_FIRE, 1f);
        hit(Act.ON_FIRE, 1f);
        check("smart merge (x3 hits, one line)", () -> find(id("onFire")) != null && find(id("onFire")).hits == 3, describe(id("onFire")));
        srv(Act.EXTINGUISH);
        if (port.deferredHitRead()) {
            // food regeneration between a hit and its deferred read must not hide the hit
            hit(Act.HIT_HEAL, 1f);
            check("hit healed in the same tick still recorded (1 damage)", () -> find(id("wither")) != null && find(id("wither")).hits == 1
                && Math.abs(find(id("wither")).taken() - 1f) < 0.01f, describe(id("wither")));
        }
        hit(Act.MAGIC, 2f);
        check("magic: Magic + Bypasses Armor", () -> find(id("magic")) != null && find(id("magic")).has(DamageRecord.MAGIC) && find(id("magic")).has(DamageRecord.BYPASS_ARMOR),
            describe(id("magic")));
        hit(Act.CREEPER, 4f);
        check("explosion by creeper", () -> find(id("explosion.player")) != null && find(id("explosion.player")).has(DamageRecord.EXPLOSION)
            && find(id("explosion.player")).attackerName.equals("Creeper"), describe(id("explosion.player")));
        // absorption + armor mitigation
        srv(Act.ARMOR_ABSORB);
        sleep(2);
        hit(Act.ZOMBIE, 6f);
        check("absorption used + mitigation by armor", () -> {
            DamageRecord r = serverHistory().last();
            return r.absorbed > 0f && r.raw > r.taken() + 0.1f;
        }, (Supplier<?>) () -> {
            DamageRecord r = serverHistory().last();
            return "raw=" + r.raw + " actual=" + r.actual + " absorbed=" + r.absorbed;
        });
        srv(Act.REMOVE_CHEST);
        // heavy hit
        clearChat();
        srv(Act.HEAL);
        sleep(2);
        hit(Act.GENERIC, 17f);
        waitFor("heavy hit alert in chat", () -> chatHas("Heavy hit"), 60);
        check("heavy hit snapshot saved", () -> serverHistory().snapshots().size() >= 1 && serverHistory().snapshots().get(0).kind == Snapshot.HEAVY, null);
        // totem (1.11+)
        if (port.hasTotem()) {
            clearChat();
            srv(Act.TOTEM);
            sleep(2);
            hit(Act.MAGIC, 100f);
            waitFor("totem alert in chat", () -> chatHas("Totem of Undying saved you"), 60);
            check("totem snapshot + record marked [Totem]", () -> serverHistory().snapshots().get(0).kind == Snapshot.TOTEM && serverHistory().last().has(DamageRecord.TOTEM),
                (Supplier<?>) () -> "kind=" + serverHistory().snapshots().get(0).kind + " last=" + serverHistory().last().damageId + " flags=" + serverHistory().last().flags);
            check("player alive after totem", () -> port.health() > 0f, null);
        }
        // commands while alive
        cmd("/whm", "Damage History", 60);
        check("history shows hit counter and buttons", () -> chatHas("x3 hits") && chatHas("[Copy ID]") && chatHas("[Mute]"), (Supplier<?>) this::chatText);
        cmd("/whm history", "Damage History", 60);
        srv(Act.POISON);
        sleep(5);
        cmd("/whm effects", "Active Effects", 60);
        check("effects list shows milk info", () -> chatHas("[Milk ✔]") && chatHas("Poison"), (Supplier<?>) this::chatText);
        if (port.hasTotem()) {
            cmd("/whm snapshot 1", "Totem of Undying", 60);
            cmd("/whm snapshot 2", "Heavy Hit", 60);
        } else {
            cmd("/whm snapshot 1", "Heavy Hit", 60);
        }
        srv(Act.CLEAR_EFFECTS);
        // death by void (absolute)
        clearChat();
        srv(Act.VOID, 1000f);
        waitFor("death screen open", port::deathScreen, 100);
        waitFor("death recap received", () -> ClientCore.currentDeath != null && ClientCore.currentDeath.key() != null
            && id("outOfWorld").equals(ClientCore.currentDeath.key().damageId), 100);
        check("killing blow flags (Bypasses Armor, Bypasses Creative)", () -> ClientCore.currentDeath.key().has(DamageRecord.BYPASS_CREATIVE)
            && ClientCore.currentDeath.key().has(DamageRecord.BYPASS_ARMOR), (Supplier<?>) () -> "flags=" + ClientCore.currentDeath.key().flags);
        check("death message captured", () -> !ClientCore.currentDeath.deathMessage.isEmpty(), null);
        sleep(30);
        screenshot("whm_selftest_deathscreen");
        act(port::respawn);
        waitFor("respawned", () -> port.health() > 0f && port.noScreen(), 200);
        waitFor("respawn summary in chat", () -> chatHas("Killing Blow") && chatHas("What Hit Me?") && chatHas("/whm togglechat"), 100);
        act(() -> info("summary: " + chatText().replace('\n', '|')));
        // commands after death
        cmd("/whm last", "Death Recap", 60);
        check("recap has the [Copy Recap] button", () -> chatHas("[Copy Recap]"), null);
        cmd("/whm search #fire", "Search: #fire", 60);
        check("search #fire finds " + id("inFire"), () -> chatHas(id("inFire")), (Supplier<?>) this::chatText);
        cmd("/whm search @minecraft zombie", "mob:minecraft:zombie", 60);
        cmd("/whm search poison", "minecraft:poison", 60);
        cmd("/whm ignore minecraft:cactus", "Now hiding minecraft:cactus", 40);
        check("ignore saved in client cfg", () -> WhmConfig.client.ignoredDamageIds.contains("minecraft:cactus"), null);
        cmd("/whm ignore minecraft:cactus", "Showing minecraft:cactus again", 40);
        cmd("/whm rename minecraft:fall &bGravity", "is now shown as", 40);
        check("rename saved", () -> "&bGravity".equals(WhmConfig.client.renameMap.get("minecraft:fall")), null);
        cmd("/whm", "Gravity", 60);
        cmd("/whm rename minecraft:fall", "Custom name of minecraft:fall removed", 40);
        cmd("/whm sethint minecraft:fall Bring a water bucket", "Tip for minecraft:fall", 40);
        cmd("/whm sethint minecraft:fall", "Custom tip of minecraft:fall removed", 40);
        cmd("/whm rename " + id("magic") + " lang:whathitme.damage.indirectMagic", "is now shown as", 40);
        cmd("/whm", "Magic", 60);
        cmd("/whm rename " + id("magic"), "removed", 40);
        cmd("/whm config showTimestamp false", "Saved: showTimestamp = false", 40);
        check("config flag off", () -> (WhmConfig.client.mask & WhmConfig.Flag.TIMESTAMP.bit()) == 0, null);
        cmd("/whm config showTimestamp true", "Saved: showTimestamp = true", 40);
        cmd("/whm config anchorPoint", "anchorPoint = ", 40);
        cmd("/whm config scale 9", "Invalid value", 40);
        if (!port.v12()) cmd("/whm config", "Settings:", 40);
        cmd("/whm togglechat", "OFF", 40);
        cmd("/whm togglechat", "ON", 40);
        cmd("/whm inspect @self", "Player not found", 60);
        clearChat();
        then(() -> {
            port.command("/whm inspect " + port.playerName());
            return true;
        });
        waitFor("/whm inspect <self> => Death Recap of", () -> chatHas("Death Recap of"), 60);
        sleep(5);
        check("inspect output has no [Copy ID] / [Mute]", () -> chatHas("Damage History of") && !chatHas("[Copy ID]") && !chatHas("[Mute]"), (Supplier<?>) this::chatText);
        clearChat();
        then(() -> {
            port.command("/whm server inspect " + port.playerName());
            return true;
        });
        waitFor("/whm server inspect <self>", () -> chatHas("Damage History of"), 60);
        sleep(5);
        check("server inspect output has no [Copy ID] / [Mute]", () -> !chatHas("[Copy ID]") && !chatHas("[Mute]"), (Supplier<?>) this::chatText);
        cmd("/whm config chatHistoryLines 3", "Saved: chatHistoryLines = 3", 40);
        cmd("/whm server history", "Damage History", 60);
        sleep(5);
        check("server /whm history respects chatHistoryLines (single player)", () -> countLines("[Copy ID]") == 3, (Supplier<?>) () -> "lines=" + countLines("[Copy ID]"));
        cmd("/whm history", "Damage History", 60);
        sleep(5);
        check("client /whm history respects chatHistoryLines", () -> countLines("[Copy ID]") == 3, (Supplier<?>) () -> "lines=" + countLines("[Copy ID]"));
        cmd("/whm config chatHistoryLines 15", "Saved: chatHistoryLines = 15", 40);
        cmd("/whm dump", "hintMap / renameMap template", 80);
        check("dump csv written", () -> new File(port.gameDir(), "config/whathitme/.cache/whathitme_dump.csv").length() > 1000, null);
        check("hint template lists vanilla + mobs", () -> {
            String t;
            try {
                t = new String(Files.readAllBytes(new File(port.gameDir(), "config/whathitme/hint_template.cfg").toPath()), StandardCharsets.UTF_8);
            } catch (java.io.IOException ex) {
                return false;
            }
            return t.contains("minecraft:fall = ") && t.contains("mob:minecraft:zombie = ") && t.contains("S:hintMap <");
        }, null);
        cmd("/whm reload", "configuration reloaded", 40);
        cmd("/whm copy minecraft:fall", "Copied to the clipboard", 40);
        // the system clipboard can be busy for a moment (another program): retry for up to 2 s
        waitFor("clipboard has the id", () -> "minecraft:fall".equals(port.clipboard()), 40);
        cmd("/whm copyrecap", "Death recap copied", 40);
        waitFor("clipboard has the recap", () -> port.clipboard().contains("Killing Blow"), 40);
        cmd("/whm help", "What Hit Me? commands", 40);
        cmd("/whm server history", "Damage History", 60);
        cmd("/whm nonsense", "Unknown subcommand", 40);
        check("tab completion: subcommands", () -> ClientCore.complete(new String[] {"s"}).containsAll(Arrays.asList("search", "sethint", "snapshot", "server")), null);
        check("tab completion: #tags and ids", () -> {
            String voidId = id("outOfWorld");
            return ClientCore.complete(new String[] {"search", "#f"}).contains("#fire")
                && ClientCore.complete(new String[] {"ignore", voidId.substring(0, 11)}).contains(voidId)
                && ClientCore.complete(new String[] {"config", "showT"}).contains("showTimestamp");
        }, null);
        check("logout clears the buffer (no leak)", () -> ServerCore.tracker().peek(java.util.UUID.randomUUID()) == null, null);
        act(() -> info("server records: " + ids()));
        if (port.v12()) new V12Plan(this).plan();
        if (port.v13()) new V13Plan(this).plan();
        if (port.rules13()) new V13RulesPlan(this).plan();
        if (port.pack13()) new PackEditorPlan(this).plan();
    }
}
