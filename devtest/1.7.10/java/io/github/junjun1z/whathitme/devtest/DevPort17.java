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
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.potion.Potion;
import net.minecraft.world.Explosion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.ClientCommandHandler;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.common.FMLCommonHandler;

/** {@link DevPort} for Minecraft 1.7.10 (Forge). */
final class DevPort17 implements DevPort {

    private EntityZombie zombie;
    private EntitySkeleton skeleton;
    private EntityCreeper creeper;

    static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    @Override
    public String mcVersion() {
        return "1.7.10";
    }

    @Override
    public File gameDir() {
        return mc().mcDataDir;
    }

    @Override
    public boolean atMainMenu() {
        // modpacks (GTNH) replace the title screen with Custom Main Menu's own screen
        return mc().currentScreen instanceof GuiMainMenu
            || (mc().currentScreen != null && mc().theWorld == null && mc().currentScreen.getClass().getName().contains("custommainmenu"));
    }

    @Override
    public void openWorld(String folder) {
        boolean exists = new File(new File(gameDir(), "saves"), folder + "/level.dat").exists();
        WorldSettings ws = new WorldSettings(4242L, WorldSettings.GameType.SURVIVAL, false, false, WorldType.FLAT);
        ws.enableCommands();
        mc().launchIntegratedServer(folder, folder, exists ? null : ws);
    }

    static MinecraftServer server() {
        return mc().getIntegratedServer();
    }

    @Override
    public boolean worldReady() {
        return mc().thePlayer != null && mc().theWorld != null && server() != null && !server().getConfigurationManager().playerEntityList.isEmpty();
    }

    @Override
    public boolean connected() {
        return mc().thePlayer != null && mc().theWorld != null && mc().getNetHandler() != null;
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
        return mc().thePlayer == null ? 0f : mc().thePlayer.getHealth();
    }

    @Override
    public String playerName() {
        return mc().thePlayer.getCommandSenderName();
    }

    @Override
    public UUID playerId() {
        return mc().thePlayer.getUniqueID();
    }

    @Override
    public void respawn() {
        mc().thePlayer.respawnPlayer();
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
        for (ChatLine l : new ArrayList<>(chatLines())) out.add(l.func_151461_a().getUnformattedText());
        return out;
    }

    @Override
    public void clearChat() {
        mc().ingameGUI.getChatGUI().clearChatMessages();
    }

    @Override
    public void command(String cmd) {
        if (ClientCommandHandler.instance.executeCommand(mc().thePlayer, cmd) == 0) mc().thePlayer.sendChatMessage(cmd);
    }

    @Override
    public void say(String text) {
        mc().thePlayer.sendChatMessage(text);
    }

    @Override
    public boolean inPlayerList(String name) {
        if (mc().getNetHandler() == null) return false;
        for (Object o : mc().getNetHandler().playerInfoList) if (name.equals(((net.minecraft.client.gui.GuiPlayerInfo) o).name)) return true;
        return false;
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
    public boolean hasTotem() {
        return false;
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
    public boolean clientKnowsOp() {
        return false;
    }

    @Override
    public boolean v13() {
        return true;
    }

    @Override
    public boolean rules13() {
        return true;
    }

    @Override
    public boolean templateEditorOpen() {
        return mc().currentScreen instanceof io.github.junjun1z.whathitme.forge.GuiTemplateEditor;
    }

    @Override
    public void pressConfigTemplatesButton() {
        io.github.junjun1z.whathitme.forge.WhmGuiFactory.Screen s = new io.github.junjun1z.whathitme.forge.WhmGuiFactory.Screen(null);
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
        DevEntry.SERVER_TASKS.add(() -> run((EntityPlayerMP) server().getConfigurationManager().playerEntityList.get(0), act, amount));
    }

    private void run(EntityPlayerMP p, Act act, float amount) {
        float health = p.getHealth();
        float absorption = p.getAbsorptionAmount();
        int resistant = p.hurtResistantTime;
        run0(p, act, amount);
        // one line per server action: shows what a modpack did to the test hits
        System.out.println("WHM-SELFTEST [SRV] " + act + " " + amount + ": health " + health + " -> " + p.getHealth() + ", absorption " + absorption + " -> "
            + p.getAbsorptionAmount() + ", hurtResistantTime " + resistant + " -> " + p.hurtResistantTime + ", armor " + p.getTotalArmorValue()
            + ", invulnerable " + p.isEntityInvulnerable() + ", creative " + p.capabilities.disableDamage);
    }

    private void run0(EntityPlayerMP p, Act act, float amount) {
        switch (act) {
            case SETUP:
                p.mcServer.func_147139_a(EnumDifficulty.NORMAL);
                p.worldObj.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
                p.worldObj.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
                p.worldObj.getGameRules().setOrCreateGameRule("keepInventory", "true");
                // food regeneration in the tick between a hit and its deferred read hides that hit
                p.worldObj.getGameRules().setOrCreateGameRule("naturalRegeneration", "false");
                p.worldObj.setWorldTime(6000);
                p.setGameType(WorldSettings.GameType.SURVIVAL);
                p.clearActivePotions();
                p.setHealth(p.getMaxHealth());
                p.inventory.clearInventory(null, -1);
                // mobs of earlier runs get their AI back when the world is loaded again (creepers explode)
                for (Object o : new java.util.ArrayList<Object>(p.worldObj.loadedEntityList)) {
                    if (o instanceof net.minecraft.entity.monster.EntityMob) ((net.minecraft.entity.Entity) o).setDead();
                }
                break;
            case SPAWN_MOBS: {
                zombie = new EntityZombie(p.worldObj);
                zombie.setPosition(p.posX + 3, p.posY, p.posZ);
                ItemStack sword = new ItemStack(Items.iron_sword);
                sword.addEnchantment(Enchantment.sharpness, 3);
                zombie.setCurrentItemOrArmor(0, sword);
                zombie.tasks.taskEntries.clear(); // no NoAI before 1.8
                zombie.targetTasks.taskEntries.clear();
                p.worldObj.spawnEntityInWorld(zombie);
                skeleton = new EntitySkeleton(p.worldObj);
                skeleton.setPosition(p.posX - 8, p.posY, p.posZ);
                skeleton.setCurrentItemOrArmor(0, new ItemStack(Items.bow));
                skeleton.tasks.taskEntries.clear(); // no NoAI before 1.8
                skeleton.targetTasks.taskEntries.clear();
                p.worldObj.spawnEntityInWorld(skeleton);
                creeper = new EntityCreeper(p.worldObj);
                creeper.setPosition(p.posX, p.posY, p.posZ + 4);
                creeper.tasks.taskEntries.clear(); // no NoAI before 1.8
                creeper.targetTasks.taskEntries.clear();
                p.worldObj.spawnEntityInWorld(creeper);
                break;
            }
            case FALL:
                p.attackEntityFrom(DamageSource.fall, amount);
                break;
            case ZOMBIE:
                p.attackEntityFrom(DamageSource.causeMobDamage(zombie), amount);
                break;
            case ARROW: {
                EntityArrow a = new EntityArrow(p.worldObj, skeleton, 1f);
                p.attackEntityFrom(DamageSource.causeArrowDamage(a, skeleton), amount);
                break;
            }
            case IN_FIRE:
                p.attackEntityFrom(DamageSource.inFire, amount);
                break;
            case ON_FIRE:
                p.attackEntityFrom(DamageSource.onFire, amount);
                break;
            case MAGIC:
                p.attackEntityFrom(DamageSource.magic, amount);
                break;
            case CREEPER:
                p.attackEntityFrom(DamageSource.setExplosionSource(new Explosion(p.worldObj, creeper, creeper.posX, creeper.posY, creeper.posZ, 3f)), amount);
                break;
            case GENERIC:
                p.attackEntityFrom(DamageSource.generic, amount);
                break;
            case VOID:
                p.attackEntityFrom(DamageSource.outOfWorld, amount);
                break;
            case EXTINGUISH:
                p.extinguish();
                break;
            case ARMOR_ABSORB:
                p.setHealth(p.getMaxHealth());
                p.setAbsorptionAmount(4f);
                p.setCurrentItemOrArmor(3, new ItemStack(Items.diamond_chestplate));
                break;
            case REMOVE_CHEST:
                p.setCurrentItemOrArmor(3, null);
                break;
            case HEAL:
                p.setHealth(p.getMaxHealth());
                p.setAbsorptionAmount(0f);
                break;
            case TOTEM:
                break; // no Totem of Undying before 1.11
            case POISON:
                p.addPotionEffect(new PotionEffect(Potion.poison.id, 400, 1));
                p.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 400, 0));
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
                ItemStack h = new ItemStack(Items.diamond_helmet);
                h.setItemDamage(h.getMaxDamage());
                p.setCurrentItemOrArmor(4, h);
                p.setHealth(p.getMaxHealth());
                break;
            }
            case ENV_SETUP: {
                int x = net.minecraft.util.MathHelper.floor_double(p.posX), y = net.minecraft.util.MathHelper.floor_double(p.boundingBox.minY);
                int z = net.minecraft.util.MathHelper.floor_double(p.posZ);
                p.worldObj.setBlock(x, y, z, net.minecraft.init.Blocks.web);
                p.setCurrentItemOrArmor(0, new ItemStack(Items.stick));
                break;
            }
            case CACTUS:
                p.attackEntityFrom(DamageSource.cactus, amount);
                break;
            case ENV_CLEAR: {
                int x = net.minecraft.util.MathHelper.floor_double(p.posX), y = net.minecraft.util.MathHelper.floor_double(p.boundingBox.minY);
                int z = net.minecraft.util.MathHelper.floor_double(p.posZ);
                p.worldObj.setBlockToAir(x, y, z);
                p.setCurrentItemOrArmor(0, null);
                p.setHealth(p.getMaxHealth());
                break;
            }
            case GEAR: {
                ItemStack chest = new ItemStack(Items.iron_chestplate);
                chest.addEnchantment(net.minecraft.enchantment.Enchantment.protection, 2);
                p.setCurrentItemOrArmor(3, chest);
                p.setCurrentItemOrArmor(1, new ItemStack(Items.iron_boots));
                p.addPotionEffect(new PotionEffect(net.minecraft.potion.Potion.moveSlowdown.id, 600, 0));
                p.setHealth(p.getMaxHealth());
                break;
            }
            case VITAL_DEATH:
                p.setHealth(10f);
                p.onDeath(DamageSource.generic);
                p.setHealth(0f);
                break;
            case SETHEALTH_DEATH:
                p.setHealth(0f);
                p.onDeath(DamageSource.magic);
                break;
            default:
                break;
        }
    }
}
