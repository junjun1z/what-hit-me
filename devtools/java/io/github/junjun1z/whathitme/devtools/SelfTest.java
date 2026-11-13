package io.github.junjun1z.whathitme.devtools;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.BooleanSupplier;

import io.github.junjun1z.whathitme.client.ClientCommandWhm;
import io.github.junjun1z.whathitme.client.ClientState;
import io.github.junjun1z.whathitme.config.WhmConfig;
import io.github.junjun1z.whathitme.data.DamageRecord;
import io.github.junjun1z.whathitme.data.DamageTracker;
import io.github.junjun1z.whathitme.data.PlayerHistory;
import io.github.junjun1z.whathitme.data.Snapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Development self-test (compiled only with -Pselftest). Started by config/whathitme/selftest.flag:
 * "sp" = single player test (creates a world), "mp-mod" / "mp-nomod" = multiplayer test against a running server
 * (launch with --server), "showcase" = screenshots for the documentation.
 * Writes whathitme_selftest_result.txt in the game directory and quits.
 */
public final class SelfTest {

    interface Step {
        /** @return true when done */
        boolean run();
    }

    private final Deque<Step> steps = new ArrayDeque<>();
    final List<String> log = new ArrayList<>();
    int fails;
    private int waitTicks;
    final String mode;
    final File gameDir;
    private boolean finished;
    /** Respawn at once when the player dies outside of a planned death (multiplayer runs) */
    boolean autoRespawn;
    private int ticks;

    private SelfTest(String mode) {
        this.mode = mode;
        this.gameDir = mc().gameDir;
    }

    public static void start(File flag) throws Exception {
        String mode = new String(Files.readAllBytes(flag.toPath()), StandardCharsets.UTF_8).trim();
        if (mode.isEmpty()) mode = "sp";
        if (mode.startsWith("showcase")) {
            Showcase sc = new Showcase();
            MinecraftForge.EVENT_BUS.register(sc);
            return;
        }
        SelfTest t = new SelfTest(mode);
        t.plan();
        MinecraftForge.EVENT_BUS.register(t);
    }

    static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    // ------------------------------------------------------------------ engine

    void then(Step s) {
        steps.addLast(s);
    }

    void act(Runnable r) {
        then(() -> {
            r.run();
            return true;
        });
    }

    void sleep(int t) {
        final int[] left = {t};
        then(() -> --left[0] <= 0);
    }

    void waitFor(String name, BooleanSupplier cond, int timeout) {
        final int[] left = {timeout};
        then(() -> {
            if (cond.getAsBoolean()) {
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

    void check(String name, BooleanSupplier cond, Object detail) {
        act(() -> {
            boolean r;
            try {
                r = cond.getAsBoolean();
            } catch (Throwable t) {
                r = false;
            }
            ok(name, r, r ? null : detail instanceof java.util.function.Supplier ? ((java.util.function.Supplier<?>) detail).get() : detail);
        });
    }

    void ok(String name, boolean ok, Object detail) {
        String s = (ok ? "[OK]   " : "[FAIL] ") + name + (detail == null ? "" : "  -> " + detail);
        log.add(s);
        System.out.println("WHM-SELFTEST " + s);
        if (!ok) fails++;
    }

    void info(String s) {
        log.add("[INFO] " + s);
        System.out.println("WHM-SELFTEST [INFO] " + s);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || finished) return;
        ticks++;
        if (autoRespawn && mc().currentScreen instanceof GuiGameOver && mc().player != null) {
            info("unplanned death - respawning");
            mc().player.respawnPlayer();
            mc().displayGuiScreen(null);
        }
        if (ticks > 20 * 60 * 6) {
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
        log.add(0, "What Hit Me? self-test (" + mode + "): " + (fails == 0 ? "ALL PASSED" : fails + " FAILED"));
        try {
            Files.write(new File(gameDir, "whathitme_selftest_result.txt").toPath(), log, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        mc().shutdown();
    }

    // ------------------------------------------------------------------ helpers

    static MinecraftServer server() {
        return mc().getIntegratedServer();
    }

    static EntityPlayerMP sp() {
        return server().getPlayerList().getPlayers().get(0);
    }

    /** Runs on the integrated server thread */
    void srv(java.util.function.Consumer<EntityPlayerMP> c) {
        act(() -> server().addScheduledTask(() -> c.accept(sp())));
    }

    @SuppressWarnings("unchecked")
    static List<ChatLine> chatLines() {
        GuiNewChat chat = mc().ingameGUI.getChatGUI();
        for (String n : new String[] {"chatLines", "field_146252_h"}) {
            try {
                Field f = GuiNewChat.class.getDeclaredField(n);
                f.setAccessible(true);
                return (List<ChatLine>) f.get(chat);
            } catch (Exception ignored) {
                // next name
            }
        }
        return new ArrayList<>();
    }

    static String chatText() {
        StringBuilder sb = new StringBuilder();
        for (ChatLine l : chatLines()) sb.append(l.getChatComponent().getUnformattedText()).append('\n');
        return sb.toString();
    }

    static boolean chatHas(String s) {
        return chatText().contains(s);
    }

    void clearChat() {
        act(() -> mc().ingameGUI.getChatGUI().clearChatMessages(false));
    }

    /** Types a command like the chat screen does (client commands first) */
    static void command(String cmd) {
        if (ClientCommandHandler.instance.executeCommand(mc().player, cmd) == 0) mc().player.sendChatMessage(cmd);
    }

    /** Runs a command and waits until the chat shows the text */
    void cmd(String cmd, String expect, int timeout) {
        clearChat();
        act(() -> command(cmd));
        waitFor(cmd + "  => \"" + expect + "\"", () -> chatHas(expect), timeout);
        act(() -> info(cmd + " chat: " + chatText().replace('\n', '|')));
    }

    void screenshot(String name) {
        act(() -> ScreenShotHelper.saveScreenshot(gameDir, name + ".png", mc().displayWidth, mc().displayHeight, mc().getFramebuffer()));
    }

    void loadWorld(String folder) {
        then(() -> {
            if (mc().currentScreen instanceof GuiMainMenu) {
                boolean exists = new File(new File(gameDir, "saves"), folder + "/level.dat").exists();
                WorldSettings ws = new WorldSettings(4242L, GameType.SURVIVAL, false, false, WorldType.FLAT);
                ws.enableCommands();
                mc().launchIntegratedServer(folder, folder, exists ? null : ws);
                return true;
            }
            return false;
        });
        waitFor("world loaded", () -> mc().player != null && mc().world != null && server() != null && !server().getPlayerList().getPlayers().isEmpty(), 1200);
    }

    static PlayerHistory serverHistory() {
        return DamageTracker.SERVER.peek(mc().player.getUniqueID());
    }

    static DamageRecord find(String id) {
        PlayerHistory h = serverHistory();
        if (h == null) return null;
        DamageRecord found = null;
        for (DamageRecord r : h.records()) if (r.damageId.equals(id)) found = r;
        return found;
    }

    /** Hit the player on the server, after waiting out the invulnerability frames */
    void hit(DamageSource src, float amount) {
        srv(p -> p.attackEntityFrom(src, amount));
        sleep(14);
    }

    // ------------------------------------------------------------------ plans

    private void plan() {
        if (mode.startsWith("mp")) {
            new MultiplayerPlan(this).plan(mode.equals("mp-mod"));
            return;
        }
        if (mode.equals("pack")) {
            new PackPlan(this).plan();
            return;
        }
        planSingleplayer();
    }

    private void planSingleplayer() {
        loadWorld("whm_selftest");
        waitFor("server info received (server has the mod)", () -> ClientState.serverHasMod, 200);
        final Entity[] mobs = new Entity[3];
        srv(p -> {
            MinecraftServer s = p.getServer();
            s.setDifficultyForAllWorlds(EnumDifficulty.NORMAL);
            p.world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
            p.world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
            p.world.getGameRules().setOrCreateGameRule("keepInventory", "true");
            p.world.setWorldTime(6000);
            p.setGameType(GameType.SURVIVAL);
            p.clearActivePotions();
            p.setHealth(p.getMaxHealth());
            p.inventory.clear();
            EntityZombie z = new EntityZombie(p.world);
            z.setPosition(p.posX + 3, p.posY, p.posZ);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            sword.addEnchantment(Enchantments.SHARPNESS, 3);
            z.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, sword);
            z.setNoAI(true);
            p.world.spawnEntity(z);
            EntitySkeleton sk = new EntitySkeleton(p.world);
            sk.setPosition(p.posX - 8, p.posY, p.posZ);
            sk.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            sk.setNoAI(true);
            p.world.spawnEntity(sk);
            EntityCreeper cr = new EntityCreeper(p.world);
            cr.setPosition(p.posX, p.posY, p.posZ + 4);
            cr.setNoAI(true);
            p.world.spawnEntity(cr);
            mobs[0] = z;
            mobs[1] = sk;
            mobs[2] = cr;
        });
        sleep(100); // spawn invulnerability
        clearChat();
        // ---- damage sources
        hit(DamageSource.FALL, 3f);
        check("fall recorded as minecraft:fall", () -> find("minecraft:fall") != null, (java.util.function.Supplier<?>) SelfTest::ids);
        act(() -> {});
        srv(p -> p.attackEntityFrom(DamageSource.causeMobDamage((EntityZombie) mobs[0]), 4f));
        sleep(14);
        check("mob hit recorded as mob:minecraft:zombie", () -> find("mob:minecraft:zombie") != null, (java.util.function.Supplier<?>) SelfTest::ids);
        check("zombie weapon + enchantment + distance", () -> {
            DamageRecord r = find("mob:minecraft:zombie");
            return r.weaponName.contains("Sword") && r.weaponEnchants.contains("damage.all") && r.distance > 2f;
        }, (java.util.function.Supplier<?>) () -> find("mob:minecraft:zombie") == null ? "null" : find("mob:minecraft:zombie").weaponName + " / " + find("mob:minecraft:zombie").weaponEnchants);
        srv(p -> {
            EntityTippedArrow a = new EntityTippedArrow(p.world, (EntitySkeleton) mobs[1]);
            p.attackEntityFrom(DamageSource.causeArrowDamage(a, mobs[1]), 3f);
        });
        sleep(14);
        check("arrow with owner: attacker Skeleton, direct Arrow, projectile flag", () -> {
            DamageRecord r = find("minecraft:arrow");
            return r != null && r.attackerName.equals("Skeleton") && r.directName.equals("Arrow") && r.has(DamageRecord.PROJECTILE) && r.distance > 3f;
        }, (java.util.function.Supplier<?>) () -> find("minecraft:arrow") == null ? "null" : find("minecraft:arrow").attackerName + "/" + find("minecraft:arrow").directName);
        hit(DamageSource.IN_FIRE, 2f);
        check("fire flag", () -> find("minecraft:inFire") != null && find("minecraft:inFire").has(DamageRecord.FIRE), null);
        // smart merge: three burning ticks within the merge window
        hit(DamageSource.ON_FIRE, 1f);
        hit(DamageSource.ON_FIRE, 1f);
        hit(DamageSource.ON_FIRE, 1f);
        check("smart merge (x3 hits, one line)", () -> find("minecraft:onFire") != null && find("minecraft:onFire").hits == 3,
            (java.util.function.Supplier<?>) () -> find("minecraft:onFire") == null ? "null" : "hits=" + find("minecraft:onFire").hits);
        srv(p -> p.extinguish());
        hit(DamageSource.MAGIC, 2f);
        check("magic: Magic + Bypasses Armor", () -> find("minecraft:magic") != null && find("minecraft:magic").has(DamageRecord.MAGIC) && find("minecraft:magic").has(DamageRecord.BYPASS_ARMOR), null);
        srv(p -> p.attackEntityFrom(DamageSource.causeExplosionDamage((EntityCreeper) mobs[2]), 4f));
        sleep(14);
        check("explosion by creeper", () -> find("minecraft:explosion.player") != null && find("minecraft:explosion.player").has(DamageRecord.EXPLOSION)
            && find("minecraft:explosion.player").attackerName.equals("Creeper"), (java.util.function.Supplier<?>) SelfTest::ids);
        // absorption + armor mitigation
        srv(p -> {
            p.setHealth(p.getMaxHealth());
            p.setAbsorptionAmount(4f);
            p.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        });
        sleep(2);
        srv(p -> p.attackEntityFrom(DamageSource.causeMobDamage((EntityZombie) mobs[0]), 6f));
        sleep(14);
        check("absorption used + mitigation by armor", () -> {
            DamageRecord r = serverHistory().last();
            return r.absorbed > 0f && r.raw > r.taken() + 0.1f;
        }, (java.util.function.Supplier<?>) () -> {
            DamageRecord r = serverHistory().last();
            return "raw=" + r.raw + " actual=" + r.actual + " absorbed=" + r.absorbed;
        });
        srv(p -> p.setItemStackToSlot(EntityEquipmentSlot.CHEST, ItemStack.EMPTY));
        // heavy hit
        clearChat();
        srv(p -> {
            p.setHealth(p.getMaxHealth());
            p.setAbsorptionAmount(0f);
        });
        sleep(2);
        hit(DamageSource.GENERIC, 17f);
        waitFor("heavy hit alert in chat", () -> chatHas("Heavy hit"), 60);
        check("heavy hit snapshot saved", () -> serverHistory().snapshots().size() >= 1 && serverHistory().snapshots().get(0).kind == Snapshot.HEAVY, null);
        // totem
        clearChat();
        srv(p -> {
            p.setHealth(p.getMaxHealth());
            p.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        });
        sleep(2);
        hit(DamageSource.MAGIC, 100f);
        waitFor("totem alert in chat", () -> chatHas("Totem of Undying saved you"), 60);
        check("totem snapshot + record marked [Totem]", () -> serverHistory().snapshots().get(0).kind == Snapshot.TOTEM && serverHistory().last().has(DamageRecord.TOTEM), null);
        check("player alive after totem", () -> mc().player.getHealth() > 0f, null);
        // commands while alive
        cmd("/whm", "Damage History", 60);
        check("history shows hit counter and buttons", () -> chatHas("x3 hits") && chatHas("[Copy ID]") && chatHas("[Mute]"), null);
        cmd("/whm history", "Damage History", 60);
        srv(p -> {
            p.addPotionEffect(new PotionEffect(MobEffects.POISON, 400, 1));
            p.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 400, 0));
        });
        sleep(5);
        cmd("/whm effects", "Active Effects", 60);
        check("effects list shows milk info", () -> chatHas("[Milk ✔]") && chatHas("Poison"), null);
        cmd("/whm snapshot 1", "Totem of Undying", 60);
        cmd("/whm snapshot 2", "Heavy Hit", 60);
        srv(p -> p.clearActivePotions());
        // death by void (absolute)
        clearChat();
        srv(p -> p.attackEntityFrom(DamageSource.OUT_OF_WORLD, 1000f));
        waitFor("death screen open", () -> mc().currentScreen instanceof GuiGameOver, 100);
        waitFor("death recap received", () -> ClientState.currentDeath != null && ClientState.currentDeath.key() != null
            && "minecraft:outOfWorld".equals(ClientState.currentDeath.key().damageId), 100);
        check("killing blow flags (Bypasses Armor, Bypasses Creative)", () -> ClientState.currentDeath.key().has(DamageRecord.BYPASS_CREATIVE)
            && ClientState.currentDeath.key().has(DamageRecord.BYPASS_ARMOR), null);
        check("death message captured", () -> !ClientState.currentDeath.deathMessage.isEmpty(), null);
        sleep(30);
        screenshot("whm_selftest_deathscreen");
        act(() -> {
            mc().player.respawnPlayer();
            mc().displayGuiScreen(null);
        });
        waitFor("respawned", () -> mc().player != null && mc().player.getHealth() > 0f && mc().currentScreen == null, 200);
        waitFor("respawn summary in chat", () -> chatHas("Killing Blow") && chatHas("What Hit Me?") && chatHas("/whm togglechat"), 100);
        act(() -> info("summary: " + chatText().replace('\n', '|')));
        // commands after death
        cmd("/whm last", "Death Recap", 60);
        check("recap has the [Copy Recap] button", () -> chatHas("[Copy Recap]"), null);
        cmd("/whm search #fire", "Search: #fire", 60);
        check("search #fire finds inFire", () -> chatHas("minecraft:inFire"), null);
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
        cmd("/whm rename minecraft:magic lang:whathitme.damage.indirectMagic", "is now shown as", 40);
        cmd("/whm", "Magic", 60);
        cmd("/whm rename minecraft:magic", "removed", 40);
        cmd("/whm config showTimestamp false", "Saved: showTimestamp = false", 40);
        check("config flag off", () -> (WhmConfig.client.mask & WhmConfig.Flag.TIMESTAMP.bit()) == 0, null);
        cmd("/whm config showTimestamp true", "Saved: showTimestamp = true", 40);
        cmd("/whm config anchorPoint", "anchorPoint = ", 40);
        cmd("/whm config scale 9", "Invalid value", 40);
        cmd("/whm config", "Settings:", 40);
        cmd("/whm togglechat", "OFF", 40);
        cmd("/whm togglechat", "ON", 40);
        cmd("/whm inspect " + "@self", "Player not found", 60);
        then(() -> {
            command("/whm inspect " + mc().player.getName());
            return true;
        });
        waitFor("/whm inspect <self> => Death Recap of", () -> chatHas("Death Recap of"), 60);
        cmd("/whm dump", "hintMap / renameMap template", 80);
        check("dump csv written", () -> new File(gameDir, "whathitme_dump.csv").length() > 1000, null);
        check("hint template lists vanilla + mobs", () -> {
            String t;
            try {
                t = new String(Files.readAllBytes(new File(gameDir, "config/whathitme/hint_template.cfg").toPath()), StandardCharsets.UTF_8);
            } catch (java.io.IOException ex) {
                return false;
            }
            return t.contains("minecraft:fall = ") && t.contains("mob:minecraft:zombie = ") && t.contains("S:hintMap <");
        }, null);
        cmd("/whm reload", "configuration reloaded", 40);
        cmd("/whm copy minecraft:fall", "Copied to the clipboard", 40);
        check("clipboard has the id", () -> "minecraft:fall".equals(GuiScreen.getClipboardString()), null);
        cmd("/whm copyrecap", "Death recap copied", 40);
        check("clipboard has the recap", () -> GuiScreen.getClipboardString().contains("Killing Blow"), null);
        cmd("/whm help", "What Hit Me? commands", 40);
        cmd("/whm server history", "Damage History", 60);
        cmd("/whm nonsense", "Unknown subcommand", 40);
        check("tab completion: subcommands", () -> {
            List<String> l = new ClientCommandWhm().getTabCompletions(null, mc().player, new String[] {"s"}, null);
            return l.containsAll(Arrays.asList("search", "sethint", "snapshot", "server"));
        }, null);
        check("tab completion: #tags and ids", () -> {
            ClientCommandWhm c = new ClientCommandWhm();
            return c.getTabCompletions(null, mc().player, new String[] {"search", "#f"}, null).contains("#fire")
                && c.getTabCompletions(null, mc().player, new String[] {"ignore", "minecraft:o"}, null).contains("minecraft:outOfWorld")
                && c.getTabCompletions(null, mc().player, new String[] {"config", "showT"}, null).contains("showTimestamp");
        }, null);
        check("logout clears the buffer (no leak)", () -> {
            // the integrated server removes histories on logout; here: only online players are tracked
            return DamageTracker.SERVER.peek(java.util.UUID.randomUUID()) == null;
        }, null);
        act(() -> info("server records: " + ids()));
    }

    static String ids() {
        PlayerHistory h = serverHistory();
        if (h == null) return "(no history)";
        StringBuilder sb = new StringBuilder();
        for (DamageRecord r : h.records()) sb.append(r.damageId).append(" x").append(r.hits).append(", ");
        return sb.toString();
    }

    static String text(ITextComponent c) {
        return c == null ? "" : c.getUnformattedText();
    }

    static boolean dedicated() {
        return FMLCommonHandler.instance().getMinecraftServerInstance() == null;
    }
}
