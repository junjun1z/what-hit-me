package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.mojang.blaze3d.platform.NativeImage;

import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.client.DeathPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

/**
 * Dev-only scripted scene for the README GIF (flag "gif"): a short fight, death, the panel with hover, respawn and the
 * chat summary, then the same fight again. Saves the framebuffer at the end of every client tick to gifrec/.
 */
public final class GifScene121 {

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private final SelfTest t;
    private final File dir;
    private ExecutorService pool;
    private boolean recording;
    private int frame;
    private final List<String> marks = new ArrayList<>();

    // fake cursor (GUI coordinates)
    private boolean cursor;
    private float cx, cy, fromX, fromY, toX, toY;
    private int moveLeft, moveTotal;
    private Field mouseX, mouseY;

    // server side
    private double bx, by, bz;
    private Zombie zombie;
    private Skeleton skeleton;
    private Creeper creeper;

    public GifScene121(SelfTest t) {
        this.t = t;
        this.dir = new File(t.port.gameDir(), "gifrec");
    }

    public void plan() {
        t.loadWorld("whm_gif");
        t.waitFor("server info", () -> ClientCore.serverHasMod, 400);
        server(this::setup);
        t.sleep(120);
        t.act(this::startRecording);
        mark("fight1");
        fight();
        deathScreenPart();
        t.waitFor("alive", () -> t.port.health() > 0f && t.port.noScreen(), 100);
        mark("respawned");
        server(this::reset);
        t.sleep(80);
        t.act(t.port::clearChat);
        mark("fight2");
        fight();
        t.sleep(40);
        t.act(this::stopRecording);
    }

    // ------------------------------------------------------------------ script

    private void fight() {
        server(p -> {
            zombie.swing(InteractionHand.MAIN_HAND);
            zombie.doHurtTarget(p);
        });
        t.sleep(18);
        server(this::shoot);
        t.sleep(16);
        server(p -> creeper.ignite());
        final int[] left = {140};
        t.then(() -> {
            if (t.port.deathScreen()) return true;
            if (--left[0] == 0) server2(p -> p.hurt(p.damageSources().explosion(creeper, creeper), 100f));
            return left[0] < -60;
        });
        mark("death");
    }

    private void deathScreenPart() {
        t.waitFor("recap", () -> ClientCore.currentDeath != null, 100);
        t.act(() -> {
            int w = mc().getWindow().getGuiScaledWidth();
            int h = mc().getWindow().getGuiScaledHeight();
            cx = w / 2f + 40;
            cy = h - 40;
            moveTo(cx, cy, 1);
            cursor = true;
        });
        t.sleep(12);
        mark("hover1");
        t.act(() -> moveToLine(0, 14));
        t.sleep(50);
        mark("button");
        t.act(() -> {
            int w = mc().getWindow().getGuiScaledWidth();
            int h = mc().getWindow().getGuiScaledHeight();
            moveTo(w / 2f + 30, h / 4f + 82, 14);
        });
        t.sleep(20);
        t.act(() -> {
            cursor = false;
            t.port.respawn();
        });
    }

    private void moveToLine(int line, int ticks) {
        try {
            float sc = (Float) field("sc").get(DeathPanel.INSTANCE);
            int px = (Integer) field("px").get(DeathPanel.INSTANCE);
            int py = (Integer) field("py").get(DeathPanel.INSTANCE);
            int top = (Integer) field("linesTop").get(DeathPanel.INSTANCE);
            moveTo(px + 236 * sc, py + (top + line * 10 + 4) * sc, ticks);
        } catch (Exception e) {
            t.info("panel layout: " + e);
        }
    }

    private static Field field(String n) throws Exception {
        Field f = DeathPanel.class.getDeclaredField(n);
        f.setAccessible(true);
        return f;
    }

    private void moveTo(float x, float y, int ticks) {
        fromX = cx;
        fromY = cy;
        toX = x;
        toY = y;
        moveTotal = moveLeft = Math.max(1, ticks);
    }

    // ------------------------------------------------------------------ server

    private interface PlayerAct {
        void run(ServerPlayer p);
    }

    private void server(PlayerAct a) {
        t.act(() -> server2(a));
    }

    private void server2(PlayerAct a) {
        MinecraftServer s = mc().getSingleplayerServer();
        s.execute(() -> a.run(s.getPlayerList().getPlayers().get(0)));
    }

    private void setup(ServerPlayer p) {
        MinecraftServer s = p.getServer();
        ServerLevel level = p.serverLevel();
        s.setDifficulty(Difficulty.NORMAL, true);
        GameRules r = level.getGameRules();
        r.getRule(GameRules.RULE_DAYLIGHT).set(false, s);
        r.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, s);
        r.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, s);
        r.getRule(GameRules.RULE_KEEPINVENTORY).set(true, s);
        r.getRule(GameRules.RULE_MOBGRIEFING).set(false, s);
        r.getRule(GameRules.RULE_SPAWN_RADIUS).set(0, s);
        r.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(true, s);
        r.getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, s);
        level.setDayTime(5000);
        level.setWeatherParameters(100000, 0, false, false);
        p.setGameMode(GameType.SURVIVAL);
        p.removeAllEffects();
        p.getInventory().clearContent();
        bx = Math.floor(p.getX()) + 0.5;
        by = p.getY();
        bz = Math.floor(p.getZ()) + 0.5;
        level.setDefaultSpawnPos(BlockPos.containing(bx, by, bz), -90f);
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        p.getInventory().add(new ItemStack(Items.BREAD, 12));
        p.getInventory().add(new ItemStack(Items.TORCH, 24));
        skeleton = EntityType.SKELETON.create(level);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        skeleton.setDropChance(EquipmentSlot.HEAD, 0f);
        prep(level, skeleton);
        reset(p);
    }

    private static void prep(ServerLevel level, Mob m) {
        m.setNoAi(true);
        m.setPersistenceRequired();
        m.setDropChance(EquipmentSlot.MAINHAND, 0f);
        level.addFreshEntity(m);
    }

    /** Player, mobs and a new creeper at their places (also after the respawn) */
    private void reset(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        p.clearFire();
        p.setHealth(p.getMaxHealth());
        // no knockback: the camera stays put during the scene
        p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        p.getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        p.connection.teleport(bx, by, bz, -90f, 8f);
        if (zombie == null || !zombie.isAlive()) {
            if (zombie != null) zombie.discard();
            zombie = EntityType.ZOMBIE.create(level);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            sword.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS), 3);
            zombie.setItemSlot(EquipmentSlot.MAINHAND, sword);
            zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            zombie.setDropChance(EquipmentSlot.HEAD, 0f);
            prep(level, zombie);
            zombie.setInvulnerable(true); // survives the creeper
        }
        zombie.setHealth(zombie.getMaxHealth());
        place(zombie, 2.4, -0.5);
        place(skeleton, 10.0, -6.0);
        if (creeper != null && creeper.isAlive()) creeper.discard();
        creeper = EntityType.CREEPER.create(level);
        prep(level, creeper);
        place(creeper, 2.6, 1.5);
    }

    /** Facing east: dx forward, dz to the right */
    private void place(Mob m, double dx, double dz) {
        double x = bx + dx, z = bz + dz;
        float yaw = (float) Math.toDegrees(Math.atan2(bz - z, bx - x)) - 90f;
        m.moveTo(x, by, z, yaw, 0f);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        m.setDeltaMovement(0, 0, 0);
    }

    private void shoot(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        skeleton.swing(InteractionHand.MAIN_HAND);
        Arrow a = new Arrow(level, skeleton, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        p.hurt(p.damageSources().arrow(a, skeleton), 4f);
    }

    // ------------------------------------------------------------------ recording

    private void mark(String label) {
        t.act(() -> marks.add(frame + " " + label));
    }

    private void startRecording() {
        dir.mkdirs();
        File[] old = dir.listFiles();
        if (old != null) for (File f : old) f.delete();
        pool = Executors.newFixedThreadPool(3);
        recording = true;
        frame = 0;
    }

    private void stopRecording() {
        recording = false;
        pool.shutdown();
        try {
            pool.awaitTermination(60, TimeUnit.SECONDS);
            Files.write(new File(dir, "marks.txt").toPath(), marks, StandardCharsets.UTF_8);
        } catch (Exception e) {
            t.info("recording: " + e);
        }
        t.ok("recorded " + frame + " frames", frame > 0, null);
    }

    /** End of every client tick (after the plan step) */
    public void tick() {
        Minecraft mc = mc();
        mc.getToasts().clear();
        if (moveLeft > 0) {
            moveLeft--;
            float k = 1f - moveLeft / (float) moveTotal;
            k = k * k * (3 - 2 * k);
            cx = fromX + (toX - fromX) * k;
            cy = fromY + (toY - fromY) * k;
        }
        if (cursor) setMouse(cx, cy);
        if (!recording) return;
        NativeImage img = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        File out = new File(dir, String.format("f_%05d.png", frame++));
        pool.submit(() -> {
            try {
                img.writeToFile(out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                img.close();
            }
        });
    }

    /** Puts the game's mouse position (window pixels) under the fake cursor so hover effects show */
    private void setMouse(float gx, float gy) {
        try {
            MouseHandler m = mc().mouseHandler;
            double s = mc().getWindow().getGuiScale();
            if (mouseX == null) {
                List<Field> ds = new ArrayList<>();
                for (Field f : MouseHandler.class.getDeclaredFields()) {
                    if (f.getType() == double.class && !Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        ds.add(f);
                    }
                }
                for (Field f : ds) {
                    double old = f.getDouble(m);
                    f.setDouble(m, 12345.0);
                    if (m.xpos() == 12345.0) mouseX = f;
                    else if (m.ypos() == 12345.0) mouseY = f;
                    f.setDouble(m, old);
                }
            }
            mouseX.setDouble(m, gx * s);
            mouseY.setDouble(m, gy * s);
        } catch (Exception e) {
            t.info("mouse: " + e);
            cursor = false;
        }
    }

    /** Drawn after the death screen: a mouse pointer (the real one is not in the framebuffer) */
    public void drawCursor(GuiGraphics g) {
        if (!cursor) return;
        String[] art = {
            "B", "BB", "BWB", "BWWB", "BWWWB", "BWWWWB", "BWWWWWB", "BWWWWWWB", "BWWWWWWWB", "BWWWWWBBBB", "BWWBWWB", "BWB BWWB", "BB  BWWB",
            "B    BWWB", "     BWWB", "      BB"};
        int x0 = Math.round(cx), y0 = Math.round(cy);
        g.pose().pushPose();
        g.pose().translate(0, 0, 900f);
        for (int r = 0; r < art.length; r++) {
            for (int c = 0; c < art[r].length(); c++) {
                char ch = art[r].charAt(c);
                if (ch == ' ') continue;
                g.fill(x0 + c, y0 + r, x0 + c + 1, y0 + r + 1, ch == 'B' ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        g.pose().popPose();
    }
}
