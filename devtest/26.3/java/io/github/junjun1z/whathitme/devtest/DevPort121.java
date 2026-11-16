package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import io.github.junjun1z.whathitme.mc.Compat26;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** {@link DevPort} for Minecraft 26.3 (Mojang names; shared by the loaders of 26.3). */
public class DevPort121 implements DevPort {

    private Zombie zombie;
    private Skeleton skeleton;
    private Creeper creeper;

    static Minecraft mc() {
        return Minecraft.getInstance();
    }

    @Override
    public String mcVersion() {
        try {
            // the running game (one jar covers several patch versions)
            return net.minecraft.SharedConstants.getCurrentVersion().name();
        } catch (Throwable t) {
            return "26.3"; // asked before the game detected its version
        }
    }

    @Override
    public File gameDir() {
        return mc().gameDirectory;
    }

    @Override
    public boolean atMainMenu() {
        return Compat26.screen() instanceof TitleScreen;
    }

    @Override
    public void openWorld(String folder) {
        if (new File(new File(gameDir(), "saves"), folder + "/level.dat").exists()) {
            mc().createWorldOpenFlows().openWorld(folder, () -> {});
            return;
        }
        LevelSettings ls = new LevelSettings(folder, GameType.SURVIVAL, new LevelSettings.DifficultySettings(Difficulty.NORMAL, false, false), true,
            WorldDataConfiguration.DEFAULT);
        mc().createWorldOpenFlows().createFreshLevel(folder, ls, new WorldOptions(4242L, false, false),
            access -> access.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), Compat26.screen());
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
        return Compat26.screen() instanceof DeathScreen;
    }

    @Override
    public boolean noScreen() {
        return Compat26.screen() == null;
    }

    @Override
    public float health() {
        return mc().player == null ? 0f : mc().player.getHealth();
    }

    @Override
    public String playerName() {
        return mc().player.getGameProfile().name();
    }

    @Override
    public UUID playerId() {
        return mc().player.getUUID();
    }

    @Override
    public void respawn() {
        mc().player.respawn();
        Compat26.setScreen(null);
    }

    @Override
    public void quit() {
        mc().stop();
    }

    @SuppressWarnings("unchecked")
    private static List<GuiMessage> messages() {
        ChatComponent chat = Compat26.chat();
        if (chat == null) return new ArrayList<>();
        for (Field f : ChatComponent.class.getDeclaredFields()) {
            if (!List.class.isAssignableFrom(f.getType())) continue;
            Type t = f.getGenericType();
            if (t instanceof ParameterizedType && ((ParameterizedType) t).getActualTypeArguments()[0] == GuiMessage.class) {
                try {
                    f.setAccessible(true);
                    return (List<GuiMessage>) f.get(chat);
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
        for (GuiMessage m : new ArrayList<>(messages())) out.add(m.content().getString());
        return out;
    }

    @Override
    public void clearChat() {
        ChatComponent chat = Compat26.chat();
        if (chat != null) chat.clearMessages(false);
    }

    @Override
    public void command(String cmd) {
        say(cmd);
    }

    /** Like the chat screen: "/..." goes through the client commands first (the loader hooks sendCommand) */
    @Override
    public void say(String text) {
        if (text.startsWith("/")) mc().player.connection.sendCommand(text.substring(1));
        else mc().player.connection.sendChat(text);
    }

    @Override
    public boolean inPlayerList(String name) {
        return mc().getConnection() != null && mc().getConnection().getPlayerInfo(name) != null;
    }

    @Override
    public void screenshot(String name) {
        Screenshot.grab(gameDir(), name + ".png", (com.mojang.blaze3d.pipeline.RenderTarget) Compat26.mainRenderTarget(), 1, msg -> {});
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
        return io.github.junjun1z.whathitme.mc.Compat26.screen() instanceof io.github.junjun1z.whathitme.mc.TemplateEditorScreen121;
    }

    @Override
    public void pressConfigTemplatesButton() {
        io.github.junjun1z.whathitme.mc.ConfigScreen121 s = new io.github.junjun1z.whathitme.mc.ConfigScreen121(null);
        io.github.junjun1z.whathitme.mc.Compat26.setScreen(s);
        s.templatesButton().onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
    }

    @Override
    public boolean configScreenOpen() {
        if (!(Compat26.screen() instanceof io.github.junjun1z.whathitme.mc.ConfigScreen121)) return false;
        String m = modMenuCheck();
        System.out.println("WHM-SELFTEST [INFO] Mod Menu config screen: " + m);
        return !m.startsWith("wrong") && !m.startsWith("error");
    }

    /** With Mod Menu installed, its own lookup (the config button of its mods list) must build our screen */
    private static String modMenuCheck() {
        try {
            Object s = Class.forName("com.terraformersmc.modmenu.ModMenu").getMethod("getConfigScreen", String.class, net.minecraft.client.gui.screens.Screen.class).invoke(null, "whathitme", null);
            return s instanceof io.github.junjun1z.whathitme.mc.ConfigScreen121 ? "ok" : "wrong: " + s;
        } catch (ClassNotFoundException e) {
            return "not installed";
        } catch (Throwable t) {
            return "error: " + t;
        }
    }

    @Override
    public void closeScreen() {
        Compat26.setScreen(null);
    }

    /** Builds the real config screen: the entry must exist and be disabled */
    @Override
    public boolean configGuiLocked(String key) {
        io.github.junjun1z.whathitme.mc.ConfigScreen121 s = new io.github.junjun1z.whathitme.mc.ConfigScreen121(null);
        s.init(427, 240);
        Boolean active = s.entryActive(key);
        return active != null && !active;
    }

    @Override
    public void server(Act act, float amount) {
        MinecraftServer s = server();
        s.execute(() -> run(s, s.getPlayerList().getPlayers().get(0), act, amount));
    }

    /** Absorption 4: since 1.20.2 the max absorption attribute (raised by the Absorption effect) limits it */
    protected void absorb(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0));
        p.setAbsorptionAmount(4f);
    }

    protected void run(MinecraftServer s, ServerPlayer p, Act act, float amount) {
        ServerLevel level = p.level();
        switch (act) {
            case SETUP:
                s.setDifficulty(Difficulty.NORMAL, true);
                level.getGameRules().set(GameRules.ADVANCE_TIME, false, s);
                level.getGameRules().set(GameRules.SPAWN_MOBS, false, s);
                level.getGameRules().set(GameRules.KEEP_INVENTORY, true, s);
                s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), "time set noon");
                p.setGameMode(GameType.SURVIVAL);
                p.removeAllEffects();
                p.setHealth(p.getMaxHealth());
                p.getInventory().clearContent();
                break;
            case SPAWN_MOBS: {
                zombie = (Zombie) type("zombie").create(level, EntitySpawnReason.TRIGGERED);
                zombie.setPos(p.getX() + 3, p.getY(), p.getZ());
                ItemStack sword = new ItemStack(Items.IRON_SWORD);
                sword.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 3);
                zombie.setItemSlot(EquipmentSlot.MAINHAND, sword);
                zombie.setNoAi(true);
                level.addFreshEntity(zombie);
                skeleton = (Skeleton) type("skeleton").create(level, EntitySpawnReason.TRIGGERED);
                skeleton.setPos(p.getX() - 8, p.getY(), p.getZ());
                skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                skeleton.setNoAi(true);
                level.addFreshEntity(skeleton);
                creeper = (Creeper) type("creeper").create(level, EntitySpawnReason.TRIGGERED);
                creeper.setPos(p.getX(), p.getY(), p.getZ() + 4);
                creeper.setNoAi(true);
                level.addFreshEntity(creeper);
                break;
            }
            case FALL:
                p.hurt(p.damageSources().fall(), amount);
                break;
            case ZOMBIE:
                p.hurt(p.damageSources().mobAttack(zombie), amount);
                break;
            case ARROW: {
                Arrow a = new Arrow(level, skeleton, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
                p.hurt(p.damageSources().arrow(a, skeleton), amount);
                break;
            }
            case IN_FIRE:
                p.hurt(p.damageSources().inFire(), amount);
                break;
            case ON_FIRE:
                p.hurt(p.damageSources().onFire(), amount);
                break;
            case MAGIC:
                p.hurt(p.damageSources().magic(), amount);
                break;
            case CREEPER:
                p.hurt(p.damageSources().explosion(creeper, creeper), amount);
                break;
            case GENERIC:
                p.hurt(p.damageSources().generic(), amount);
                break;
            case VOID:
                p.hurt(p.damageSources().fellOutOfWorld(), amount);
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
                p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 400, 0));
                break;
            case CLEAR_EFFECTS:
                p.removeAllEffects();
                break;
            case THORNS:
                p.hurt(p.damageSources().thorns(zombie), amount);
                break;
            case REFLECT:
                p.attack(zombie);
                p.hurt(p.damageSources().mobAttack(zombie), amount);
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
                p.hurt(p.damageSources().cactus(), amount);
                break;
            case ENV_CLEAR:
                level.removeBlock(p.blockPosition(), false);
                p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                p.setHealth(p.getMaxHealth());
                break;
            case GEAR: {
                ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
                chest.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION), 2);
                p.setItemSlot(EquipmentSlot.CHEST, chest);
                p.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600, 0));
                p.setHealth(p.getMaxHealth());
                break;
            }
            case VITAL_DEATH:
                p.setHealth(10f);
                p.die(p.damageSources().generic());
                p.setHealth(0f);
                break;
            case SETHEALTH_DEATH:
                p.setHealth(0f);
                p.die(p.damageSources().magic());
                break;
            default:
                break;
        }
    }

    /** EntityType by id (the constants moved from EntityType to EntityTypes in 26.3) */
    private static EntityType<?> type(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(id));
    }
}
