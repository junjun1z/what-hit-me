package io.github.junjun1z.whmdriver;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Development only: drives a client that does NOT have What Hit Me? against a server that has it, and checks that
 * every message the server sends is readable English (no raw translation keys). Started by config/whmdriver.flag
 * ("" = full plan, "two" = second player of the two-player test). Writes whmdriver_result.txt and quits.
 * The loader part calls {@link #tick()} every client tick and {@link #onChat(String)} for every chat line.
 */
public final class DriverPlan {

    private final DriverPort port;
    private final VanillaCommands cmd;
    private final Deque<BooleanSupplier> steps = new ArrayDeque<>();
    private final List<String> chat = new ArrayList<>();
    private final List<String> log = new ArrayList<>();
    private int fails;
    private boolean done;
    private int ticks;
    private boolean autoRespawn = true;
    private final String mode;

    public DriverPlan(DriverPort port, String mode) {
        this.port = port;
        this.cmd = VanillaCommands.of(port.mcVersion());
        this.mode = mode;
        if (mode.equals("two")) planSecondPlayer();
        else plan();
    }

    /** Reads config/whmdriver.flag; null when the driver should not run */
    public static String mode(File configDir) {
        File flag = new File(configDir, "whmdriver.flag");
        if (!flag.isFile()) return null;
        try {
            return new String(Files.readAllBytes(flag.toPath()), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private void say(String c) {
        // a few old-version commands are several commands (VanillaCommands)
        for (String one : c.split("\n")) {
            steps.add(() -> {
                port.say(one);
                return true;
            });
            sleep(4);
        }
    }

    private void sleep(int t) {
        final int[] left = {t};
        steps.add(() -> --left[0] <= 0);
    }

    private synchronized String all() {
        return String.join("\n", chat);
    }

    private void expect(String name, String text, int timeout) {
        final int[] left = {timeout};
        steps.add(() -> {
            if (all().contains(text)) {
                ok(name, true);
                return true;
            }
            if (--left[0] <= 0) {
                ok(name + " (missing \"" + text + "\")", false);
                return true;
            }
            return false;
        });
    }

    private void ok(String name, boolean ok) {
        String s = (ok ? "[OK]   " : "[FAIL] ") + name;
        log.add(s);
        System.out.println("WHMDRIVER " + s);
        if (!ok) fails++;
    }

    /** Second (non-op) player on a server with the mod, next to an op with the mod (two-player test) */
    private void planSecondPlayer() {
        steps.add(port::connected);
        sleep(60);
        say("/whm");
        expect("second player: /whm history (English)", "Damage History", 60);
        say("/whm inspect _junjun1z");
        expect("second player: /whm inspect needs operator", "operator permission", 60);
        say("/whm config showTimestamp false");
        expect("second player: server config needs operator", "operator permission", 60);
        steps.add(() -> {
            autoRespawn = false;
            return true;
        });
        final int[] wait = {3000};
        steps.add(() -> port.deathScreen() || --wait[0] <= 0);
        sleep(40);
        steps.add(() -> {
            if (port.deathScreen()) port.respawn();
            return true;
        });
        expect("second player: killed by the op -> summary (English)", "Killing Blow", 200);
        say("/whm ignore " + voidId());
        expect("second player: personal ignore", "Now hiding " + voidId(), 60);
        say("/whm ignore " + voidId());
        sleep(400); // stay online while the op inspects
        steps.add(() -> {
            ok("no raw translation keys in chat", !all().contains("whathitme."));
            return true;
        });
    }

    private String voidId() {
        return cmd.atLeast(1, 19, 4) ? "minecraft:out_of_world" : "minecraft:outOfWorld";
    }

    private void plan() {
        steps.add(port::connected);
        sleep(100);
        say("/gamemode survival");
        say("/difficulty normal");
        say("/gamerule keepInventory true");
        say("/time set 6000");
        say(cmd.effectClear());
        say(cmd.effect("instant_health", 1, 10));
        say("/tp @p ~ ~12 ~");
        sleep(80);
        say(cmd.effect("resistance", 30, 3));
        say(cmd.summonZombieWithSword("~2 ~ ~"));
        sleep(50);
        say(cmd.killType("zombie"));
        say(cmd.effect("instant_health", 1, 10));
        say("/whm");
        expect("/whm history (English)", "Damage History", 60);
        expect("history line has [Copy ID] / [Mute]", "[Copy ID]", 20);
        say("/whm effects");
        expect("/whm effects", "Active Effects", 60);
        say("/whm search fall");
        expect("/whm search", "Search: fall", 60);
        say("/whm togglechat");
        expect("/whm togglechat off", "Respawn chat summary: OFF", 60);
        say("/whm togglechat");
        expect("/whm togglechat on", "Respawn chat summary: ON", 60);
        say("/whm help");
        expect("/whm help", "What Hit Me? commands", 60);
        say("/whm last");
        expect("/whm last before dying", "No death recorded", 60);
        say(cmd.effectClear());
        say(cmd.effect("instant_health", 1, 10));
        if (cmd.hasTotem()) {
            say(cmd.totemOffhand());
            sleep(10);
            say(cmd.effect("instant_damage", 1, 5));
            expect("totem alert (English)", "Totem of Undying saved you", 100);
            say(cmd.effectClear());
        }
        steps.add(() -> {
            autoRespawn = false;
            return true;
        });
        say(cmd.killSelf());
        steps.add(port::deathScreen);
        sleep(40);
        steps.add(() -> {
            port.respawn();
            return true;
        });
        expect("respawn summary (English)", "Killing Blow", 200);
        expect("respawn summary: recap link", "Click here for full death recap", 20);
        // 1.3: "dm-custom" = the server replaces death messages for everyone (Fabric), "dm-vanilla" = clients without the mod keep them (Forge)
        if (mode.contains("dm-custom")) expect("custom death message from the server pack (replaced for everyone)", "WHMT-DM", 20);
        if (mode.contains("dm-vanilla")) {
            steps.add(() -> {
                ok("client without the mod keeps the vanilla death message", !all().contains("WHMT-DM"));
                return true;
            });
        }
        if (mode.contains("dm")) expect("server pack template for clients without the mod (summary)", "WHMT-SUM", 20);
        say("/whm last");
        expect("/whm last", "Death Recap", 60);
        if (cmd.hasTotem()) {
            say("/whm snapshot 1");
            expect("/whm snapshot 1", "Totem of Undying", 60);
        }
        say("/whm ignore minecraft:fall");
        expect("/whm ignore (personal or op)", "Now hiding minecraft:fall", 60);
        say("/whm ignore minecraft:fall");
        sleep(20);
        steps.add(() -> {
            ok("no raw translation keys in chat", !all().contains("whathitme."));
            return true;
        });
        sleep(20);
    }

    /** Every chat line the client receives (plain text) */
    public synchronized void onChat(String text) {
        chat.add(text);
    }

    /** Every client tick (end phase) */
    public void tick() {
        if (done) return;
        if (autoRespawn && port.connected() && port.deathScreen()) {
            log.add("[INFO] unplanned death - respawning");
            port.respawn();
        }
        if (++ticks > 20 * 60 * 5) {
            ok("global timeout", false);
            steps.clear();
        }
        try {
            while (!steps.isEmpty()) {
                if (!steps.peekFirst().getAsBoolean()) return;
                steps.removeFirst();
            }
        } catch (Throwable t) {
            ok("exception: " + t, false);
            t.printStackTrace();
            steps.clear();
        }
        done = true;
        log.add(0, "WHM driver (client without What Hit Me?, " + port.mcVersion() + "): " + (fails == 0 ? "ALL PASSED" : fails + " FAILED"));
        log.add("---- chat ----");
        synchronized (this) {
            log.addAll(chat);
        }
        try {
            Files.write(new File(port.gameDir(), "whmdriver_result.txt").toPath(), log, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        port.quit();
    }
}
