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
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** {@link DevPort} for Minecraft 1.20.1 (Mojang names; shared by Forge and Fabric). */
public class DevPort120 implements DevPort {

    private Zombie zombie;
    private Skeleton skeleton;
    private Creeper creeper;

    static Minecraft mc() {
        return Minecraft.getInstance();
    }

    @Override
    public String mcVersion() {
        return "1.20.1";
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
            mc().createWorldOpenFlows().loadLevel(mc().screen, folder);
            return;
        }
        GameRules rules = new GameRules();
        LevelSettings ls = new LevelSettings(folder, GameType.SURVIVAL, false, Difficulty.NORMAL, true, rules, WorldDataConfiguration.DEFAULT);
        mc().createWorldOpenFlows().createFreshLevel(folder, ls, new WorldOptions(4242L, false, false),
            access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
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

    @SuppressWarnings("unchecked")
    private static List<GuiMessage> messages() {
        ChatComponent chat = mc().gui.getChat();
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
        mc().gui.getChat().clearMessages(false);
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
    public void server(Act act, float amount) {
        MinecraftServer s = server();
        s.execute(() -> run(s, s.getPlayerList().getPlayers().get(0), act, amount));
    }

    /** Absorption 4 (1.20.1: direct; 1.20.2+ needs an Absorption effect for the max absorption attribute) */
    protected void absorb(ServerPlayer p) {
        p.setAbsorptionAmount(4f);
    }

    protected void run(MinecraftServer s, ServerPlayer p, Act act, float amount) {
        ServerLevel level = p.serverLevel();
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
                p.hurt(p.damageSources().fall(), amount);
                break;
            case ZOMBIE:
                p.hurt(p.damageSources().mobAttack(zombie), amount);
                break;
            case ARROW: {
                Arrow a = new Arrow(level, skeleton);
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
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 0));
                break;
            case CLEAR_EFFECTS:
                p.removeAllEffects();
                break;
            default:
                break;
        }
    }
}
