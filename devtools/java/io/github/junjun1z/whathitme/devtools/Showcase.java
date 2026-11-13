package io.github.junjun1z.whathitme.devtools;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import io.github.junjun1z.whathitme.client.ClientState;
import io.github.junjun1z.whathitme.client.DeathPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
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
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Takes the documentation screenshots (docs/screenshots). Development only. */
public final class Showcase {

    private final Deque<SelfTest.Step> steps = new ArrayDeque<>();
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Entity[] mobs = new Entity[3];
    /** fake mouse for tooltips: 0 = none, 1 = death panel line, 2 = chat line */
    private int hoverMode;
    private int hoverLine = 1;
    private String hoverChatText = "";

    public Showcase() {
        plan();
    }

    private void then(SelfTest.Step s) {
        steps.addLast(s);
    }

    private void act(Runnable r) {
        then(() -> {
            r.run();
            return true;
        });
    }

    private void sleep(int t) {
        final int[] left = {t};
        then(() -> --left[0] <= 0);
    }

    private void until(java.util.function.BooleanSupplier c, int timeout) {
        final int[] left = {timeout};
        then(() -> c.getAsBoolean() || --left[0] <= 0);
    }

    private MinecraftServer server() {
        return mc.getIntegratedServer();
    }

    private void srv(java.util.function.Consumer<EntityPlayerMP> c) {
        act(() -> server().addScheduledTask(() -> c.accept(server().getPlayerList().getPlayers().get(0))));
    }

    private void hit(java.util.function.Function<EntityPlayerMP, DamageSource> src, float amount) {
        srv(p -> p.attackEntityFrom(src.apply(p), amount));
        sleep(14);
    }

    private void shot(String name) {
        act(() -> {
            File dir = new File(mc.gameDir, "screenshots");
            ScreenShotHelper.saveScreenshot(mc.gameDir, name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            System.out.println("WHM-SHOWCASE saved " + new File(dir, name + ".png"));
        });
    }

    private void command(String c) {
        act(() -> {
            if (ClientCommandHandler.instance.executeCommand(mc.player, c) == 0) mc.player.sendChatMessage(c);
        });
    }

    private void openChat() {
        act(() -> mc.displayGuiScreen(new GuiChat()));
    }

    private void clearChat() {
        act(() -> mc.ingameGUI.getChatGUI().clearChatMessages(false));
    }

    private EntityLiving place(EntityLiving e, EntityPlayerMP p, double dx, double dz) {
        double x = p.posX + dx;
        double z = p.posZ + dz;
        int y = p.world.getHeight((int) Math.floor(x), (int) Math.floor(z));
        e.setLocationAndAngles(x, y, z, 180f, 0f);
        e.rotationYawHead = 180f;
        e.renderYawOffset = 180f;
        e.setNoAI(true);
        e.enablePersistence();
        p.world.spawnEntity(e);
        return e;
    }

    private void plan() {
        then(() -> {
            if (mc.currentScreen instanceof GuiMainMenu) {
                boolean exists = new File(mc.gameDir, "saves/whm_showcase/level.dat").exists();
                WorldSettings ws = new WorldSettings(20261114L, GameType.SURVIVAL, true, false, WorldType.DEFAULT);
                ws.enableCommands();
                mc.launchIntegratedServer("whm_showcase", "whm_showcase", exists ? null : ws);
                return true;
            }
            return false;
        });
        until(() -> mc.player != null && server() != null && !server().getPlayerList().getPlayers().isEmpty() && ClientState.serverHasMod, 2400);
        srv(p -> {
            p.getServer().setDifficultyForAllWorlds(EnumDifficulty.NORMAL);
            p.world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
            p.world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
            p.world.getGameRules().setOrCreateGameRule("keepInventory", "true");
            p.world.getGameRules().setOrCreateGameRule("showDeathMessages", "true");
            p.world.setWorldTime(5000);
            p.world.getWorldInfo().setRaining(false);
            p.world.getWorldInfo().setThundering(false);
            p.setGameType(GameType.SURVIVAL);
            p.inventory.clear();
            p.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            p.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            p.setItemStackToSlot(EntityEquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            p.setItemStackToSlot(EntityEquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            p.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            p.inventory.addItemStackToInventory(new ItemStack(Items.BREAD, 12));
            p.inventory.addItemStackToInventory(new ItemStack(Items.BOW));
            p.inventory.addItemStackToInventory(new ItemStack(Items.ARROW, 32));
            p.inventory.addItemStackToInventory(new ItemStack(net.minecraft.init.Blocks.TORCH, 24));
            p.setHealth(p.getMaxHealth());
            p.clearActivePotions();
            for (Entity e : p.world.loadedEntityList) if (e instanceof EntityLiving) e.setDead();
            p.connection.setPlayerLocation(p.posX, p.posY, p.posZ, 0f, 8f);
        });
        sleep(30);
        srv(p -> {
            EntityZombie z = new EntityZombie(p.world);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            sword.addEnchantment(Enchantments.SHARPNESS, 3);
            sword.addEnchantment(Enchantments.FIRE_ASPECT, 1);
            z.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, sword);
            mobs[0] = place(z, p, 1.2, 3.5);
            EntitySkeleton sk = new EntitySkeleton(p.world);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.addEnchantment(Enchantments.POWER, 2);
            sk.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, bow);
            mobs[1] = place(sk, p, -4, 11);
            mobs[2] = place(new EntityCreeper(p.world), p, -1.5, 5);
        });
        sleep(40);
        act(() -> mc.gameSettings.hideGUI = false);
        // ---- a fight
        hit(p -> DamageSource.causeArrowDamage(new EntityTippedArrow(p.world, (EntitySkeleton) mobs[1]), mobs[1]), 6f);
        hit(p -> DamageSource.causeArrowDamage(new EntityTippedArrow(p.world, (EntitySkeleton) mobs[1]), mobs[1]), 5f);
        hit(p -> DamageSource.causeMobDamage((EntityZombie) mobs[0]), 7f);
        hit(p -> DamageSource.ON_FIRE, 1f);
        hit(p -> DamageSource.ON_FIRE, 1f);
        srv(p -> p.extinguish());
        hit(p -> DamageSource.causeMobDamage((EntityZombie) mobs[0]), 7f);
        hit(p -> DamageSource.FALL, 2f);
        srv(p -> p.setAbsorptionAmount(2f));
        hit(p -> DamageSource.causeExplosionDamage((EntityCreeper) mobs[2]), 60f);
        until(() -> mc.currentScreen instanceof GuiGameOver && ClientState.currentDeath != null, 200);
        sleep(45);
        act(() -> {
            hoverMode = 1;
            hoverLine = 1;
        });
        sleep(4);
        shot("death-screen");
        act(() -> hoverMode = 0);
        sleep(2);
        // ---- respawn summary
        act(() -> {
            mc.player.respawnPlayer();
            mc.displayGuiScreen(null);
        });
        until(() -> mc.player.getHealth() > 0 && mc.currentScreen == null, 200);
        srv(p -> {
            for (Entity e : p.world.loadedEntityList) if (e instanceof EntityLiving) e.setDead();
            p.connection.setPlayerLocation(p.posX, p.posY, p.posZ, 0f, 8f);
        });
        sleep(40);
        shot("respawn-chat");
        // ---- history with tooltip
        clearChat();
        srv(p -> {
            mobs[0] = place(withSword(new EntityZombie(p.world)), p, 1.2, 3.5);
            mobs[1] = place(new EntitySkeleton(p.world), p, -4, 11);
            mobs[1].setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        });
        sleep(20);
        hit(p -> DamageSource.causeMobDamage((EntityZombie) mobs[0]), 6f);
        hit(p -> DamageSource.causeArrowDamage(new EntityTippedArrow(p.world, (EntitySkeleton) mobs[1]), mobs[1]), 5f);
        hit(p -> DamageSource.causeArrowDamage(new EntityTippedArrow(p.world, (EntitySkeleton) mobs[1]), mobs[1]), 4f);
        srv(p -> p.addPotionEffect(new PotionEffect(MobEffects.POISON, 300, 0)));
        sleep(80);
        srv(p -> p.clearActivePotions());
        clearChat();
        openChat();
        command("/whm history");
        sleep(20);
        act(() -> {
            hoverMode = 2;
            hoverChatText = "Zombie";
        });
        sleep(4);
        shot("history");
        act(() -> hoverMode = 0);
        // ---- effects
        act(() -> mc.displayGuiScreen(null));
        clearChat();
        srv(p -> {
            p.setHealth(p.getMaxHealth());
            p.addPotionEffect(new PotionEffect(MobEffects.POISON, 600, 1));
            p.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 1200, 0));
            p.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 2400, 2));
            p.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 900, 1));
            p.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 3600, 0));
            p.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 6000, 0));
        });
        sleep(10);
        openChat();
        command("/whm effects");
        sleep(20);
        act(() -> {
            hoverMode = 2;
            hoverChatText = "Poison";
        });
        sleep(4);
        shot("effects");
        act(() -> hoverMode = 0);
        // ---- search
        act(() -> mc.displayGuiScreen(null));
        srv(p -> p.clearActivePotions());
        clearChat();
        openChat();
        command("/whm search fire");
        sleep(20);
        shot("search");
        // ---- totem
        act(() -> mc.displayGuiScreen(null));
        clearChat();
        srv(p -> {
            p.setHealth(p.getMaxHealth());
            p.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        });
        sleep(20);
        hit(p -> DamageSource.causeMobDamage((EntityZombie) mobs[0]), 40f);
        sleep(6);
        shot("totem");
        sleep(40);
        shot("totem-late");
        act(() -> mc.shutdown());
    }

    private static EntityZombie withSword(EntityZombie z) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.addEnchantment(Enchantments.SHARPNESS, 3);
        z.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, sword);
        return z;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        try {
            while (!steps.isEmpty()) {
                if (!steps.peekFirst().run()) return;
                steps.removeFirst();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            steps.clear();
            mc.shutdown();
        }
    }

    /** Shown name in death messages: keep the real account name out of the screenshots */
    @SubscribeEvent
    public void onName(PlayerEvent.NameFormat e) {
        e.setDisplayname("Steve");
    }

    private static int intField(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(o);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post e) {
        try {
            if (hoverMode == 1 && e.getGui() instanceof GuiGameOver) {
                DeathPanel dp = DeathPanel.INSTANCE;
                int px = intField(dp, "px");
                int py = intField(dp, "py");
                Field scf = DeathPanel.class.getDeclaredField("sc");
                scf.setAccessible(true);
                float sc = scf.getFloat(dp);
                int top = intField(dp, "linesTop");
                int mx = px + Math.round(120 * sc);
                int my = py + Math.round((top + hoverLine * 10 + 4) * sc);
                dp.render(e.getGui(), mx, my);
            } else if (hoverMode == 2 && e.getGui() instanceof GuiChat) {
                // find a chat component whose text contains hoverChatText and draw its hover text
                ScaledResolution sr = new ScaledResolution(mc);
                int scale = sr.getScaleFactor();
                for (int y = 20; y < mc.displayHeight / 2; y += 2) {
                    for (int x = 4; x < 600; x += 6) {
                        ITextComponent c = mc.ingameGUI.getChatGUI().getChatComponent(x, y);
                        if (c != null && c.getStyle().getHoverEvent() != null && c.getUnformattedComponentText().contains(hoverChatText)) {
                            Method m = findHover();
                            int gx = x / scale;
                            int gy = sr.getScaledHeight() - y / scale - 1;
                            m.invoke(e.getGui(), c, gx, gy);
                            return;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            hoverMode = 0;
        }
    }

    private static Method findHover() throws Exception {
        for (String n : new String[] {"handleComponentHover", "func_175272_a"}) {
            try {
                Method m = GuiScreen.class.getDeclaredMethod(n, ITextComponent.class, int.class, int.class);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // next
            }
        }
        throw new NoSuchMethodException("handleComponentHover");
    }
}
