package io.github.junjun1z.whathitme.devtools;

import io.github.junjun1z.whathitme.client.ClientState;
import io.github.junjun1z.whathitme.data.DamageRecord;
import net.minecraft.client.gui.GuiGameOver;

/**
 * Multiplayer test against a dedicated server (the player must be op). "mp-mod": the server has What Hit Me?,
 * "mp-nomod": it does not (client fallback, [Client Est.]).
 */
final class MultiplayerPlan {

    private final SelfTest t;

    MultiplayerPlan(SelfTest t) {
        this.t = t;
    }

    private void say(String c) {
        t.act(() -> SelfTest.mc().player.sendChatMessage(c));
        t.sleep(3);
    }

    private static boolean fallbackHas(String id) {
        for (DamageRecord r : ClientState.FALLBACK.records()) if (r.damageId.equals(id)) return true;
        return false;
    }

    void plan(boolean serverMod) {
        t.waitFor("connected to the server", () -> SelfTest.mc().player != null && SelfTest.mc().world != null && SelfTest.mc().getConnection() != null, 2400);
        t.sleep(80);
        t.check("server has mod = " + serverMod, () -> ClientState.serverHasMod == serverMod, (java.util.function.Supplier<?>) () -> "serverHasMod=" + ClientState.serverHasMod);
        say("/gamemode survival");
        say("/difficulty normal");
        say("/gamerule keepInventory true");
        say("/gamerule doMobSpawning false");
        say("/time set 6000");
        say("/kill @e[type=!player]");
        say("/effect @p clear");
        say("/effect @p instant_health 1 10");
        t.sleep(20);
        // fall
        say("/tp @p ~ ~12 ~");
        t.sleep(80);
        if (!serverMod) t.check("fallback: fall estimated", () -> fallbackHas("minecraft:fall"), (java.util.function.Supplier<?>) SelfTest::chatText);
        // fire
        say("/setblock ~ ~ ~ fire");
        t.sleep(50);
        say("/setblock ~ ~ ~ air");
        say("/effect @p fire_resistance 10 0");
        t.sleep(30);
        say("/effect @p instant_health 1 10");
        // poison
        say("/effect @p poison 3 1");
        t.sleep(80);
        // mob with a sword
        say("/summon zombie ~2 ~ ~ {HandItems:[{id:\"minecraft:iron_sword\",Count:1b},{}]}");
        t.sleep(160);
        say("/kill @e[type=zombie]");
        say("/effect @p instant_health 1 10");
        // arrow with owner
        say("/summon skeleton ~6 ~ ~ {HandItems:[{id:\"minecraft:bow\",Count:1b},{}]}");
        t.sleep(200);
        say("/kill @e[type=skeleton]");
        say("/effect @p instant_health 1 10");
        // explosion
        say("/summon creeper ~1 ~ ~ {Fuse:0,ExplosionRadius:1b}");
        t.sleep(40);
        say("/effect @p instant_health 1 10");
        t.sleep(20);
        t.cmd("/whm", serverMod ? "Damage History" : "[Client Est.]", 80);
        if (serverMod) {
            t.check("history (server): fall, zombie, skeleton/arrow, explosion", () -> SelfTest.chatHas("Fall") && SelfTest.chatHas("Zombie") && SelfTest.chatHas("Skeleton")
                && SelfTest.chatHas("Explosion") && SelfTest.chatHas("Magic"), (java.util.function.Supplier<?>) SelfTest::chatText);
        } else {
            t.check("history (fallback): entries estimated", () -> ClientState.FALLBACK.records().size() >= 3, (java.util.function.Supplier<?>) SelfTest::chatText);
        }
        t.cmd("/whm effects", "Active Effects", 60);
        t.cmd("/whm search fall", "Search: fall", 60);
        // totem
        t.clearChat();
        say("/replaceitem entity @p slot.weapon.offhand minecraft:totem_of_undying");
        t.sleep(10);
        say("/effect @p instant_damage 1 5");
        t.waitFor("totem alert", () -> SelfTest.chatHas("Totem of Undying saved you"), 100);
        t.sleep(20);
        say("/effect @p clear");
        // death
        t.clearChat();
        say("/kill");
        t.waitFor("death screen", () -> SelfTest.mc().currentScreen instanceof GuiGameOver, 100);
        t.waitFor("death recap available", () -> ClientState.currentDeath != null && ClientState.currentDeath.key() != null, 100);
        if (serverMod) t.check("killing blow = minecraft:outOfWorld (accurate)", () -> "minecraft:outOfWorld".equals(ClientState.currentDeath.key().damageId), null);
        else t.check("killing blow estimated", () -> ClientState.currentDeath.key().has(DamageRecord.ESTIMATED), null);
        t.sleep(30);
        t.screenshot("whm_selftest_" + (serverMod ? "mpmod" : "mpnomod") + "_death");
        t.act(() -> {
            SelfTest.mc().player.respawnPlayer();
            SelfTest.mc().displayGuiScreen(null);
        });
        t.waitFor("respawned", () -> SelfTest.mc().player != null && SelfTest.mc().player.getHealth() > 0f && SelfTest.mc().currentScreen == null, 200);
        t.waitFor("respawn summary", () -> SelfTest.chatHas("Killing Blow"), 100);
        t.act(() -> t.info("summary: " + SelfTest.chatText().replace('\n', '|')));
        t.cmd("/whm last", "Death Recap", 60);
        t.cmd("/whm snapshot 1", "Totem of Undying", 60);
        t.then(() -> {
            SelfTest.command("/whm inspect " + SelfTest.mc().player.getName());
            return true;
        });
        t.waitFor("/whm inspect", () -> SelfTest.chatHas(serverMod ? "Death Recap of" : "needs What Hit Me? on the server"), 60);
        t.cmd("/whm dump", "Wrote", 80);
        if (serverMod) t.cmd("/whm server history", "Damage History", 60);
        t.cmd("/whm reload", "reloaded", 40);
        t.act(() -> {
            SelfTest.mc().world.sendQuittingDisconnectingPacket();
            SelfTest.mc().loadWorld(null);
        });
        t.sleep(40);
    }
}
