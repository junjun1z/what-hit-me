package io.github.junjun1z.whmdriver;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Development only: drives a client that does NOT have What Hit Me? against a server that has it, and checks that
 * every message the server sends is readable English (no raw translation keys). Started by config/whmdriver.flag.
 */
@Mod(modid = "whmdriver", name = "WHM test driver", version = "1", clientSideOnly = true, acceptableRemoteVersions = "*")
public class WhmDriver {

    private final Deque<BooleanSupplier> steps = new ArrayDeque<>();
    private final List<String> chat = new ArrayList<>();
    private final List<String> log = new ArrayList<>();
    private int fails;
    private boolean done;
    private int ticks;
    private boolean autoRespawn = true;

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        if (!new File(Loader.instance().getConfigDir(), "whmdriver.flag").isFile()) return;
        if (Loader.isModLoaded("whathitme")) {
            System.out.println("WHMDRIVER: What Hit Me? is installed here, this test needs a client without it");
            return;
        }
        plan();
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    private void say(String c) {
        steps.add(() -> {
            mc().player.sendChatMessage(c);
            return true;
        });
        sleep(4);
    }

    private void sleep(int t) {
        final int[] left = {t};
        steps.add(() -> --left[0] <= 0);
    }

    private String all() {
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

    private void plan() {
        steps.add(() -> mc().player != null && mc().getConnection() != null);
        sleep(100);
        say("/gamemode survival");
        say("/difficulty normal");
        say("/gamerule keepInventory true");
        say("/time set 6000");
        say("/effect @p clear");
        say("/effect @p instant_health 1 10");
        say("/tp @p ~ ~12 ~");
        sleep(80);
        say("/effect @p resistance 30 3");
        say("/summon zombie ~2 ~ ~ {HandItems:[{id:\"minecraft:iron_sword\",Count:1b},{}]}");
        sleep(50);
        say("/kill @e[type=zombie]");
        say("/effect @p instant_health 1 10");
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
        say("/effect @p clear");
        say("/effect @p instant_health 1 10");
        say("/replaceitem entity @p slot.weapon.offhand minecraft:totem_of_undying");
        sleep(10);
        say("/effect @p instant_damage 1 5");
        expect("totem alert (English)", "Totem of Undying saved you", 100);
        say("/effect @p clear");
        steps.add(() -> {
            autoRespawn = false;
            return true;
        });
        say("/kill");
        steps.add(() -> mc().currentScreen instanceof GuiGameOver);
        sleep(40);
        steps.add(() -> {
            mc().player.respawnPlayer();
            mc().displayGuiScreen(null);
            return true;
        });
        expect("respawn summary (English)", "Killing Blow", 200);
        expect("respawn summary: recap link", "Click here for full death recap", 20);
        say("/whm last");
        expect("/whm last", "Death Recap", 60);
        say("/whm snapshot 1");
        expect("/whm snapshot 1", "Totem of Undying", 60);
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

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        chat.add(e.getMessage().getUnformattedText());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || done) return;
        if (autoRespawn && mc().currentScreen instanceof GuiGameOver && mc().player != null) {
            log.add("[INFO] unplanned death - respawning");
            mc().player.respawnPlayer();
            mc().displayGuiScreen(null);
        }
        if (++ticks > 20 * 60 * 5) {
            ok("global timeout", false);
            steps.clear();
        }
        while (!steps.isEmpty()) {
            if (!steps.peekFirst().getAsBoolean()) return;
            steps.removeFirst();
        }
        done = true;
        log.add(0, "WHM driver (client without What Hit Me?): " + (fails == 0 ? "ALL PASSED" : fails + " FAILED"));
        log.add("---- chat ----");
        log.addAll(chat);
        try {
            Files.write(new File(mc().gameDir, "whmdriver_result.txt").toPath(), log, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        mc().shutdown();
    }
}
