package io.github.junjun1z.whathitme.devtest;

import java.util.Arrays;
import java.util.function.Supplier;

import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.data.DamageRecord;
import io.github.junjun1z.whmdriver.VanillaCommands;

/**
 * Multiplayer test against a dedicated server (the player must be op). "mp-mod": the server has What Hit Me?,
 * "mp-nomod": it does not (client fallback, [Client Est.]).
 */
final class MultiplayerPlan {

    private final SelfTest t;
    private final DevPort p;
    private final VanillaCommands vc;

    MultiplayerPlan(SelfTest t) {
        this.t = t;
        this.p = t.port;
        this.vc = VanillaCommands.of(p.mcVersion());
    }

    private void say(String c) {
        // a few old-version commands are several commands (VanillaCommands)
        for (String one : c.split("\n")) {
            t.act(() -> p.say(one));
            t.sleep(3);
        }
    }

    private static boolean fallbackHas(String id) {
        for (DamageRecord r : ClientCore.FALLBACK.records()) if (r.damageId.equals(id)) return true;
        return false;
    }

    /** Op with the mod; a second player "Alex" (no mod, not op) is online at the same time */
    void planTwo() {
        t.waitFor("connected to the server", p::connected, 2400);
        t.waitFor("second player Alex online", () -> p.inPlayerList("Alex"), 6000);
        t.sleep(300);
        say("/kill Alex");
        t.sleep(60);
        t.clearChat();
        t.act(() -> p.command("/whm inspect Alex"));
        t.waitFor("op inspects Alex: death recap", () -> t.chatHas("Death Recap of Alex"), 100);
        t.sleep(10);
        t.check("inspect Alex: history and no [Copy ID] / [Mute]", () -> t.chatHas("Damage History of Alex") && !t.chatHas("[Copy ID]"), (Supplier<?>) t::chatText);
        t.check("tab completion of inspect lists both players", () -> ClientCore.complete(new String[] {"inspect", ""}).containsAll(Arrays.asList("Alex", p.playerName())),
            null);
        t.cmd("/whm server inspect Alex", "Death Recap of Alex", 100);
        t.sleep(20);
    }

    void plan(boolean serverMod) {
        t.waitFor("connected to the server", p::connected, 2400);
        t.sleep(80);
        t.act(() -> t.autoRespawn = true);
        if (!serverMod) t.act(() -> WhmConfig.common.set("historySize", "5"));
        t.check("server has mod = " + serverMod, () -> ClientCore.serverHasMod == serverMod, (Supplier<?>) () -> "serverHasMod=" + ClientCore.serverHasMod);
        say("/gamemode survival");
        say("/difficulty normal");
        say("/gamerule keepInventory true");
        say("/gamerule doMobSpawning false");
        say("/time set 6000");
        say(vc.killNonPlayers());
        say(vc.effectClear());
        say(vc.effect("instant_health", 1, 10));
        t.sleep(20);
        // fall
        say("/tp @p ~ ~12 ~");
        t.sleep(80);
        if (!serverMod) t.check("fallback: fall estimated", () -> fallbackHas(SelfTest.id("fall")), (Supplier<?>) t::chatText);
        // fire
        say("/setblock ~ ~ ~ fire");
        t.sleep(50);
        say("/setblock ~ ~ ~ air");
        say(vc.effect("fire_resistance", 10, 0));
        t.sleep(30);
        say(vc.effect("instant_health", 1, 10));
        // poison
        say(vc.effect("poison", 3, 1));
        t.sleep(80);
        // mob with a sword (resistance so the mobs cannot kill the player yet)
        say(vc.effect("resistance", 60, 3));
        say(vc.summonZombieWithSword("~2 ~ ~"));
        t.sleep(100);
        say(vc.killType("zombie"));
        say(vc.effect("instant_health", 1, 10));
        // arrow with owner
        say(vc.summonSkeletonWithBow("~6 ~ ~"));
        t.sleep(200);
        say(vc.killType("skeleton"));
        say(vc.effect("instant_health", 1, 10));
        // explosion
        say(vc.summonCreeperFuse0("~1 ~ ~"));
        t.sleep(40);
        say(vc.effect("instant_health", 1, 10));
        t.sleep(20);
        if (!serverMod) {
            // more separate lines than historySize (5): falls more than the merge window apart
            for (int i = 0; i < 3; i++) {
                say("/tp @p ~ ~8 ~");
                t.sleep(90);
            }
        }
        t.cmd("/whm", serverMod ? "Damage History" : "[Client Est.]", 80);
        if (serverMod) {
            t.check("history (server): fall, mob (zombie or skeleton arrow), explosion, magic", () -> t.chatHas("Fall") && (t.chatHas("Zombie") || t.chatHas("Skeleton"))
                && t.chatHas("Explosion") && t.chatHas("Magic"), (Supplier<?>) t::chatText);
        } else {
            t.check("history (fallback): entries estimated", () -> ClientCore.FALLBACK.records().size() >= 3, (Supplier<?>) t::chatText);
            t.check("fallback history respects historySize = 5", () -> ClientCore.FALLBACK.records().size() == 5,
                (Supplier<?>) () -> "records=" + ClientCore.FALLBACK.records().size());
        }
        t.cmd("/whm effects", "Active Effects", 60);
        t.cmd("/whm search fall", "Search: fall", 60);
        // totem (1.11+)
        if (vc.hasTotem()) {
            say(vc.effectClear());
            say(vc.effect("instant_health", 1, 10));
            t.sleep(10);
            t.clearChat();
            say(vc.totemOffhand());
            t.sleep(10);
            say(vc.effect("instant_damage", 1, 5));
            t.waitFor("totem alert", () -> t.chatHas("Totem of Undying saved you"), 100);
            t.sleep(20);
        }
        say(vc.effectClear());
        // death
        t.act(() -> t.autoRespawn = false);
        t.clearChat();
        say(vc.killSelf());
        t.waitFor("death screen", p::deathScreen, 100);
        t.waitFor("death recap available", () -> ClientCore.currentDeath != null && ClientCore.currentDeath.key() != null, 100);
        if (serverMod) {
            t.check("killing blow = " + SelfTest.id("outOfWorld") + " or generic_kill (accurate)", () -> {
                String id = ClientCore.currentDeath.key().damageId;
                return SelfTest.id("outOfWorld").equals(id) || "minecraft:generic_kill".equals(id);
            }, (Supplier<?>) () -> ClientCore.currentDeath.key().damageId);
        } else {
            t.check("killing blow estimated", () -> ClientCore.currentDeath.key().has(DamageRecord.ESTIMATED), null);
        }
        t.sleep(30);
        t.screenshot("whm_selftest_" + (serverMod ? "mpmod" : "mpnomod") + "_death");
        t.act(p::respawn);
        t.waitFor("respawned", () -> p.health() > 0f && p.noScreen(), 200);
        t.waitFor("respawn summary", () -> t.chatHas("Killing Blow"), 100);
        t.act(() -> t.info("summary: " + t.chatText().replace('\n', '|')));
        t.cmd("/whm last", "Death Recap", 60);
        if (vc.hasTotem()) t.cmd("/whm snapshot 1", "Totem of Undying", 60);
        t.clearChat();
        t.then(() -> {
            p.command("/whm inspect " + p.playerName());
            return true;
        });
        t.waitFor("/whm inspect", () -> t.chatHas(serverMod ? "Death Recap of" : "needs What Hit Me? on the server"), 60);
        if (serverMod) {
            t.sleep(5);
            t.check("inspect output has no [Copy ID] / [Mute]", () -> t.chatHas("Damage History of") && !t.chatHas("[Copy ID]"), (Supplier<?>) t::chatText);
            t.cmd("/whm config chatHistoryLines 3", "Saved", 40);
            t.cmd("/whm server history", "Damage History", 60);
            t.sleep(5);
            t.check("dedicated server /whm history uses 15 lines (not the client's 3)", () -> t.countLines("[Copy ID]") >= 4,
                (Supplier<?>) () -> "lines=" + t.countLines("[Copy ID]"));
            t.cmd("/whm config chatHistoryLines 15", "Saved", 40);
        }
        t.cmd("/whm dump", "Wrote", 80);
        if (serverMod) t.cmd("/whm server history", "Damage History", 60);
        t.cmd("/whm reload", "reloaded", 40);
        if (!serverMod) t.act(() -> WhmConfig.common.set("historySize", "50"));
        if (p.v12()) v12(serverMod);
        t.sleep(20);
    }

    /** 1.2 checks; with the server mod the player is de-opped at the end (the test script ops it again) */
    private void v12(boolean serverMod) {
        t.cmd("/whm copyrecap", "Death recap copied", 40);
        t.waitFor("copy recap has the equipment block", () -> p.clipboard().contains("## Equipment") && p.clipboard().contains("Max health"), 40);
        if (!serverMod) {
            t.cmd("/whm stats", "Death Statistics", 60);
            t.check("local stats counted the death", () -> t.chatHas("Deaths: "), (Supplier<?>) t::chatText);
            return;
        }
        t.check("server protocol 2", () -> ClientCore.serverInfo != null && ClientCore.serverInfo.protocol == 2, null);
        t.check("death recap from the server carries the equipment", () -> ClientCore.currentDeath != null && ClientCore.currentDeath.gear != null, null);
        t.cmd("/whm stats", "Death Statistics", 60);
        t.cmd("/whm test minecraft:lava", "Chat preview", 60);
        t.act(() -> p.say("/deop " + p.playerName()));
        t.sleep(40);
        t.cmd("/whm rename minecraft:fall x", "operator permission", 40);
        t.cmd("/whm server reload", "operator permission", 60);
        t.cmd("/whm test minecraft:lava", "operator permission", 40);
        t.cmd("/whm stats", "Death Statistics", 60);
        t.cmd("/whm config showTimestamp", "showTimestamp = ", 40);
        t.cmd("/whm config historySize 20", "operator permission", 40);
        t.cmd("/whm search fall", "Search: fall", 60);
    }
}
