package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

/** {@link DevPort} for Minecraft 1.18.2 (Mojang names; shared by its loaders). */
public class DevPort118 implements DevPort {

    private Zombie zombie;
    private Skeleton skeleton;
    private Creeper creeper;

    static Minecraft mc() {
        return Minecraft.getInstance();
    }

    @Override
    public String mcVersion() {
        return net.minecraft.SharedConstants.getCurrentVersion().getName();
    }

    @Override
    public File gameDir() {
        return mc().gameDirectory;
    }

    @Override
    public boolean atMainMenu() {
        return mc().screen instanceof TitleScreen;
    }

    @Override
    public void openWorld(String folder) {
        if (new File(new File(gameDir(), "saves"), folder + "/level.dat").exists()) {
            mc().loadLevel(folder);
            return;
        }
        GameRules rules = new GameRules();
        LevelSettings ls = new LevelSettings(folder, GameType.SURVIVAL, false, Difficulty.NORMAL, true, rules, DataPackConfig.DEFAULT);
        // By signature: 1.18.2 has RegistryAccess.BUILTIN (a Supplier of RegistryAccess.Frozen), 1.18 / 1.18.1 have
        // RegistryAccess.builtin() (RegistryHolder); WorldPreset#create and Minecraft#createLevel take that type.
        try {
            Object access = null;
            for (Field f : RegistryAccess.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && java.util.function.Supplier.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object v = ((java.util.function.Supplier<?>) f.get(null)).get();
                    if (v instanceof RegistryAccess) access = v;
                }
            }
            if (access == null) {
                for (java.lang.reflect.Method m : RegistryAccess.class.getDeclaredMethods()) {
                    if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 0
                        && RegistryAccess.class.isAssignableFrom(m.getReturnType()) && m.getReturnType() != RegistryAccess.class) {
                        m.setAccessible(true);
                        access = m.invoke(null);
                        break;
                    }
                }
            }
            Object gen = null;
            for (java.lang.reflect.Method m : WorldPreset.class.getDeclaredMethods()) {
                Class<?>[] pt = m.getParameterTypes();
                if (m.getReturnType() == WorldGenSettings.class && pt.length == 4 && pt[1] == long.class && pt[2] == boolean.class
                    && pt[3] == boolean.class && pt[0].isInstance(access)) {
                    m.setAccessible(true);
                    gen = m.invoke(flatPreset(), access, 4242L, false, false);
                    break;
                }
            }
            for (java.lang.reflect.Method m : Minecraft.class.getDeclaredMethods()) {
                Class<?>[] pt = m.getParameterTypes();
                if (pt.length == 4 && pt[0] == String.class && pt[1] == LevelSettings.class && pt[3] == WorldGenSettings.class && pt[2].isInstance(access)) {
                    m.setAccessible(true);
                    m.invoke(mc(), folder, ls, access, gen);
                    return;
                }
            }
            throw new IllegalStateException("no createLevel method");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** The "Superflat" preset (a private constant of WorldPreset in 1.18.2) */
    private static WorldPreset flatPreset() {
        for (Field f : WorldPreset.class.getDeclaredFields()) {
            if (f.getType() != WorldPreset.class || !java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                WorldPreset p = (WorldPreset) f.get(null);
                Component d = p.description();
                if (d instanceof TranslatableComponent && "generator.flat".equals(((TranslatableComponent) d).getKey())) return p;
            } catch (Exception ignored) {
                // next field
            }
        }
        return WorldPreset.NORMAL;
    }

    static MinecraftServer server() {
        return mc().getSingleplayerServer();
    }

    @Override
    public boolean worldReady() {
        return mc().player != null && mc().level != null && server() != null && !server().getPlayerList().getPlayers().isEmpty();
    }

    @Override
    public boolean connected() {
        return mc().player != null && mc().level != null && mc().getConnection() != null;
    }

    @Override
    public boolean deathScreen() {
        return mc().screen instanceof DeathScreen;
    }

    @Override
    public boolean noScreen() {
        return mc().screen == null;
    }

    @Override
    public float health() {
        return mc().player == null ? 0f : mc().player.getHealth();
    }

    @Override
    public String playerName() {
        return mc().player.getGameProfile().getName();
    }

    @Override
    public UUID playerId() {
        return mc().player.getUUID();
    }

    @Override
    public void respawn() {
        mc().player.respawn();
        mc().setScreen(null);
    }

    @Override
    public void quit() {
        mc().stop();
    }

    /** The full chat lines: the List&lt;GuiMessage&lt;Component&gt;&gt; field (the other one holds wrapped lines) */
    @SuppressWarnings("unchecked")
    private static List<GuiMessage<Component>> messages() {
        ChatComponent chat = mc().gui.getChat();
        for (Field f : ChatComponent.class.getDeclaredFields()) {
            if (!List.class.isAssignableFrom(f.getType())) continue;
            Type t = f.getGenericType();
            if (!(t instanceof ParameterizedType)) continue;
            Type el = ((ParameterizedType) t).getActualTypeArguments()[0];
            if (el instanceof ParameterizedType && ((ParameterizedType) el).getRawType() == GuiMessage.class
                && ((ParameterizedType) el).getActualTypeArguments()[0] == Component.class) {
                try {
                    f.setAccessible(true);
                    return (List<GuiMessage<Component>>) f.get(chat);
                } catch (Exception ignored) {
                    // next field
                }
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<String> chat() {
        List<String> out = new ArrayList<>();
        for (GuiMessage<Component> m : new ArrayList<>(messages())) out.add(m.getMessage().getString());
        return out;
    }

    @Override
    public void clearChat() {
        mc().gui.getChat().clearMessages(false);
    }

    @Override
    public void command(String cmd) {
        say(cmd);
    }

    /** Like the chat screen (Screen#sendMessage): client commands first, then the chat packet */
    @Override
    public void say(String text) {
        Screen s = new Screen(TextComponent.EMPTY) {};
        s.init(mc(), 320, 240);
        s.sendMessage(text, false);
    }

    @Override
    public boolean inPlayerList(String name) {
        return mc().getConnection() != null && mc().getConnection().getPlayerInfo(name) != null;
    }

    @Override
    public void screenshot(String name) {
        Screenshot.grab(gameDir(), name + ".png", mc().getMainRenderTarget(), msg -> {});
    }

    @Override
    public String clipboard() {
        return mc().keyboardHandler.getClipboard();
    }

    @Override
    public boolean dedicated() {
        return !mc().hasSingleplayerServer();
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
        return mc().screen instanceof io.github.junjun1z.whathitme.mc.TemplateEditorScreen118;
    }

    @Override
    public void pressConfigTemplatesButton() {
        io.github.junjun1z.whathitme.mc.ConfigScreen118 s = new io.github.junjun1z.whathitme.mc.ConfigScreen118(null);
        mc().setScreen(s);
        s.templatesButton().onPress();
    }

    @Override
    public boolean configScreenOpen() {
        if (!(mc().screen instanceof io.github.junjun1z.whathitme.mc.ConfigScreen118)) return false;
        String m = modMenuCheck();
        System.out.println("WHM-SELFTEST [INFO] Mod Menu config screen: " + m);
        return !m.startsWith("wrong") && !m.startsWith("error");
    }

    /** With Mod Menu installed, its own lookup (the config button of its mods list) must build our screen */
    private static String modMenuCheck() {
        try {
            Object s = Class.forName("com.terraformersmc.modmenu.ModMenu").getMethod("getConfigScreen", String.class, Screen.class).invoke(null, "whathitme", null);
            return s instanceof io.github.junjun1z.whathitme.mc.ConfigScreen118 ? "ok" : "wrong: " + s;
        } catch (ClassNotFoundException e) {
            return "not installed";
        } catch (Throwable t) {
            return "error: " + t;
        }
    }

    @Override
    public void closeScreen() {
        mc().setScreen(null);
    }

    /** Builds the real config screen: the entry must exist and be disabled */
    @Override
    public boolean configGuiLocked(String key) {
        io.github.junjun1z.whathitme.mc.ConfigScreen118 s = new io.github.junjun1z.whathitme.mc.ConfigScreen118(null);
        s.init(mc(), 427, 240);
        Boolean active = s.entryActive(key);
        return active != null && !active;
    }

    @Override
    public void server(Act act, float amount) {
        MinecraftServer s = server();
        s.execute(() -> run(s, s.getPlayerList().getPlayers().get(0), act, amount));
    }

    /** Absorption 4 (1.20.1: direct; 1.20.2+ needs an Absorption effect for the max absorption attribute) */
    protected void absorb(ServerPlayer p) {
        p.setAbsorptionAmount(4f);
    }

    protected void run(MinecraftServer s, ServerPlayer p, Act act, float amount) {
        ServerLevel level = (ServerLevel) p.level;
        switch (act) {
            case SETUP:
                s.setDifficulty(Difficulty.NORMAL, true);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, s);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, s);
                level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, s);
                level.setDayTime(6000);
                p.setGameMode(GameType.SURVIVAL);
                p.removeAllEffects();
                p.setHealth(p.getMaxHealth());
                p.getInventory().clearContent();
                break;
            case SPAWN_MOBS: {
                zombie = EntityType.ZOMBIE.create(level);
                zombie.setPos(p.getX() + 3, p.getY(), p.getZ());
                ItemStack sword = new ItemStack(Items.IRON_SWORD);
                sword.enchant(Enchantments.SHARPNESS, 3);
                zombie.setItemSlot(EquipmentSlot.MAINHAND, sword);
                zombie.setNoAi(true);
                level.addFreshEntity(zombie);
                skeleton = EntityType.SKELETON.create(level);
                skeleton.setPos(p.getX() - 8, p.getY(), p.getZ());
                skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                skeleton.setNoAi(true);
                level.addFreshEntity(skeleton);
                creeper = EntityType.CREEPER.create(level);
                creeper.setPos(p.getX(), p.getY(), p.getZ() + 4);
                creeper.setNoAi(true);
                level.addFreshEntity(creeper);
                break;
            }
            case FALL:
                p.hurt(DamageSource.FALL, amount);
                break;
            case ZOMBIE:
                p.hurt(DamageSource.mobAttack(zombie), amount);
                break;
            case ARROW: {
                Arrow a = new Arrow(level, skeleton);
                p.hurt(DamageSource.arrow(a, skeleton), amount);
                break;
            }
            case IN_FIRE:
                p.hurt(DamageSource.IN_FIRE, amount);
                break;
            case ON_FIRE:
                p.hurt(DamageSource.ON_FIRE, amount);
                break;
            case MAGIC:
                p.hurt(DamageSource.MAGIC, amount);
                break;
            case CREEPER:
                p.hurt(DamageSource.explosion(creeper), amount);
                break;
            case GENERIC:
                p.hurt(DamageSource.GENERIC, amount);
                break;
            case VOID:
                p.hurt(DamageSource.OUT_OF_WORLD, amount);
                break;
            case EXTINGUISH:
                p.clearFire();
                break;
            case ARMOR_ABSORB:
                p.setHealth(p.getMaxHealth());
                absorb(p);
                p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                break;
            case REMOVE_CHEST:
                p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                break;
            case HEAL:
                p.setHealth(p.getMaxHealth());
                p.setAbsorptionAmount(0f);
                break;
            case TOTEM:
                p.setHealth(p.getMaxHealth());
                p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
                break;
            case POISON:
                p.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 1));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 0));
                break;
            case CLEAR_EFFECTS:
                p.removeAllEffects();
                break;
            case THORNS:
                p.hurt(DamageSource.thorns(zombie), amount);
                break;
            case REFLECT:
                p.attack(zombie);
                p.hurt(DamageSource.mobAttack(zombie), amount);
                break;
            case BRITTLE_HELMET: {
                ItemStack h = new ItemStack(Items.DIAMOND_HELMET);
                h.setDamageValue(h.getMaxDamage());
                p.setItemSlot(EquipmentSlot.HEAD, h);
                p.setHealth(p.getMaxHealth());
                break;
            }
            case ENV_SETUP:
                level.setBlockAndUpdate(p.blockPosition(), net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState());
                p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
                break;
            case CACTUS:
                p.hurt(DamageSource.CACTUS, amount);
                break;
            case ENV_CLEAR:
                level.removeBlock(p.blockPosition(), false);
                p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                p.setHealth(p.getMaxHealth());
                break;
            case GEAR: {
                ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
                chest.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 2);
                p.setItemSlot(EquipmentSlot.CHEST, chest);
                p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 0));
                p.setHealth(p.getMaxHealth());
                break;
            }
            case VITAL_DEATH:
                p.setHealth(10f);
                p.die(DamageSource.GENERIC);
                p.setHealth(0f);
                break;
            case SETHEALTH_DEATH:
                p.setHealth(0f);
                p.die(DamageSource.MAGIC);
                break;
            default:
                break;
        }
    }
}
