package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
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
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.common.FMLCommonHandler;

/** {@link DevPort} for Minecraft 1.11.2 (Forge). */
final class DevPort111 implements DevPort {

    private EntityZombie zombie;
    private EntitySkeleton skeleton;
    private EntityCreeper creeper;

    static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    @Override
    public String mcVersion() {
        return "1.11.2";
    }

    @Override
    public File gameDir() {
        return mc().mcDataDir;
    }

    @Override
    public boolean atMainMenu() {
        return mc().currentScreen instanceof GuiMainMenu;
    }

    @Override
    public void openWorld(String folder) {
        boolean exists = new File(new File(gameDir(), "saves"), folder + "/level.dat").exists();
        WorldSettings ws = new WorldSettings(4242L, GameType.SURVIVAL, false, false, WorldType.FLAT);
        ws.enableCommands();
        mc().launchIntegratedServer(folder, folder, exists ? null : ws);
    }

    static MinecraftServer server() {
        return mc().getIntegratedServer();
    }

    @Override
    public boolean worldReady() {
        return mc().player != null && mc().world != null && server() != null && !server().getPlayerList().getPlayers().isEmpty();
    }

    @Override
    public boolean connected() {
        return mc().player != null && mc().world != null && mc().getConnection() != null;
    }

    @Override
    public boolean deathScreen() {
        return mc().currentScreen instanceof GuiGameOver;
    }

    @Override
    public boolean noScreen() {
        return mc().currentScreen == null;
    }

    @Override
    public float health() {
        return mc().player == null ? 0f : mc().player.getHealth();
    }

    @Override
    public String playerName() {
        return mc().player.getName();
    }

    @Override
    public UUID playerId() {
        return mc().player.getUniqueID();
    }

    @Override
    public void respawn() {
        mc().player.respawnPlayer();
        mc().displayGuiScreen(null);
    }

    @Override
    public void quit() {
        mc().shutdown();
    }

    @SuppressWarnings("unchecked")
    private static List<ChatLine> chatLines() {
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

    @Override
    public List<String> chat() {
        List<String> out = new ArrayList<>();
        for (ChatLine l : new ArrayList<>(chatLines())) out.add(l.getChatComponent().getUnformattedText());
        return out;
    }

    @Override
    public void clearChat() {
        mc().ingameGUI.getChatGUI().clearChatMessages(false);
    }

    @Override
    public void command(String cmd) {
        if (ClientCommandHandler.instance.executeCommand(mc().player, cmd) == 0) mc().player.sendChatMessage(cmd);
    }

    @Override
    public void say(String text) {
        mc().player.sendChatMessage(text);
    }

    @Override
    public boolean inPlayerList(String name) {
        return mc().getConnection() != null && mc().getConnection().getPlayerInfo(name) != null;
    }

    @Override
    public void screenshot(String name) {
        ScreenShotHelper.saveScreenshot(gameDir(), name + ".png", mc().displayWidth, mc().displayHeight, mc().getFramebuffer());
    }

    @Override
    public String clipboard() {
        return GuiScreen.getClipboardString();
    }

    @Override
    public boolean dedicated() {
        return FMLCommonHandler.instance().getMinecraftServerInstance() == null;
    }

    @Override
    public boolean v12() {
        return true;
    }

    @Override
    public boolean v13() {
        return true;
    }

    @Override
    public boolean templateEditorOpen() {
        return mc().currentScreen instanceof io.github.junjun1z.whathitme.forge.GuiTemplateEditor;
    }

    @Override
    public void pressConfigTemplatesButton() {
        io.github.junjun1z.whathitme.forge.WhmGuiFactory.WithTemplates s = new io.github.junjun1z.whathitme.forge.WhmGuiFactory.WithTemplates(null);
        mc().displayGuiScreen(s);
        s.pressTemplates();
    }

    @Override
    public boolean configScreenOpen() {
        return mc().currentScreen instanceof GuiConfig && "whathitme".equals(((GuiConfig) mc().currentScreen).modID);
    }

    @Override
    public void closeScreen() {
        mc().displayGuiScreen(null);
    }

    @Override
    public boolean configGuiLocked(String key) {
        return locked(io.github.junjun1z.whathitme.forge.WhmGuiFactory.elements(), key);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean locked(List list, String key) {
        for (Object o : list) {
            IConfigElement e = (IConfigElement) o;
            if (e.isProperty()) {
                if (key.equals(e.getName()) && e.getConfigEntryClass() != null && e.getConfigEntryClass().getSimpleName().startsWith("Locked")) {
                    // build the real screen entry: it must exist and be disabled
                    GuiConfig g = new GuiConfig(null, java.util.Collections.singletonList(e), "whathitme", false, false, "locked test");
                    g.setWorldAndResolution(mc(), 427, 240);
                    return !g.entryList.listEntries.isEmpty() && !g.entryList.listEntries.get(0).enabled();
                }
            } else if (e.getChildElements() != null && locked(e.getChildElements(), key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void server(Act act, float amount) {
        server().addScheduledTask(() -> run(server().getPlayerList().getPlayers().get(0), act, amount));
    }

    private void run(EntityPlayerMP p, Act act, float amount) {
        switch (act) {
            case SETUP:
                p.getServer().setDifficultyForAllWorlds(EnumDifficulty.NORMAL);
                p.world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
                p.world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
                p.world.getGameRules().setOrCreateGameRule("keepInventory", "true");
                p.world.setWorldTime(6000);
                p.setGameType(GameType.SURVIVAL);
                p.clearActivePotions();
                p.setHealth(p.getMaxHealth());
                p.inventory.clear();
                break;
            case SPAWN_MOBS: {
                zombie = new EntityZombie(p.world);
                zombie.setPosition(p.posX + 3, p.posY, p.posZ);
                ItemStack sword = new ItemStack(Items.IRON_SWORD);
                sword.addEnchantment(Enchantments.SHARPNESS, 3);
                zombie.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, sword);
                zombie.setNoAI(true);
                p.world.spawnEntity(zombie);
                skeleton = new EntitySkeleton(p.world);
                skeleton.setPosition(p.posX - 8, p.posY, p.posZ);
                skeleton.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                skeleton.setNoAI(true);
                p.world.spawnEntity(skeleton);
                creeper = new EntityCreeper(p.world);
                creeper.setPosition(p.posX, p.posY, p.posZ + 4);
                creeper.setNoAI(true);
                p.world.spawnEntity(creeper);
                break;
            }
            case FALL:
                p.attackEntityFrom(DamageSource.FALL, amount);
                break;
            case ZOMBIE:
                p.attackEntityFrom(DamageSource.causeMobDamage(zombie), amount);
                break;
            case ARROW: {
                EntityTippedArrow a = new EntityTippedArrow(p.world, skeleton);
                p.attackEntityFrom(DamageSource.causeArrowDamage(a, skeleton), amount);
                break;
            }
            case IN_FIRE:
                p.attackEntityFrom(DamageSource.IN_FIRE, amount);
                break;
            case ON_FIRE:
                p.attackEntityFrom(DamageSource.ON_FIRE, amount);
                break;
            case MAGIC:
                p.attackEntityFrom(DamageSource.MAGIC, amount);
                break;
            case CREEPER:
                p.attackEntityFrom(DamageSource.causeExplosionDamage(creeper), amount);
                break;
            case GENERIC:
                p.attackEntityFrom(DamageSource.GENERIC, amount);
                break;
            case VOID:
                p.attackEntityFrom(DamageSource.OUT_OF_WORLD, amount);
                break;
            case EXTINGUISH:
                p.extinguish();
                break;
            case ARMOR_ABSORB:
                p.setHealth(p.getMaxHealth());
                p.setAbsorptionAmount(4f);
                p.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                break;
            case REMOVE_CHEST:
                p.setItemStackToSlot(EntityEquipmentSlot.CHEST, ItemStack.EMPTY);
                break;
            case HEAL:
                p.setHealth(p.getMaxHealth());
                p.setAbsorptionAmount(0f);
                break;
            case TOTEM:
                p.setHealth(p.getMaxHealth());
                p.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM));
                break;
            case POISON:
                p.addPotionEffect(new PotionEffect(MobEffects.POISON, 400, 1));
                p.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 400, 0));
                break;
            case CLEAR_EFFECTS:
                p.clearActivePotions();
                break;
            case THORNS:
                p.attackEntityFrom(DamageSource.causeThornsDamage(zombie), amount);
                break;
            case REFLECT:
                p.attackTargetEntityWithCurrentItem(zombie);
                p.attackEntityFrom(DamageSource.causeMobDamage(zombie), amount);
                break;
            case BRITTLE_HELMET: {
                ItemStack h = new ItemStack(Items.DIAMOND_HELMET);
                h.setItemDamage(h.getMaxDamage());
                p.setItemStackToSlot(EntityEquipmentSlot.HEAD, h);
                p.setHealth(p.getMaxHealth());
                break;
            }
            case ENV_SETUP:
                p.world.setBlockState(new net.minecraft.util.math.BlockPos(p), net.minecraft.init.Blocks.WEB.getDefaultState());
                p.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
                break;
            case CACTUS:
                p.attackEntityFrom(DamageSource.CACTUS, amount);
                break;
            case ENV_CLEAR:
                p.world.setBlockToAir(new net.minecraft.util.math.BlockPos(p));
                p.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
                p.setHealth(p.getMaxHealth());
                break;
            case GEAR: {
                ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
                chest.addEnchantment(Enchantments.PROTECTION, 2);
                p.setItemStackToSlot(EntityEquipmentSlot.CHEST, chest);
                p.setItemStackToSlot(EntityEquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                p.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 600, 0));
                p.setHealth(p.getMaxHealth());
                break;
            }
            case VITAL_DEATH:
                p.setHealth(10f);
                p.onDeath(DamageSource.GENERIC);
                p.setHealth(0f);
                break;
            case SETHEALTH_DEATH:
                p.setHealth(0f);
                p.onDeath(DamageSource.MAGIC);
                break;
            default:
                break;
        }
    }
}
