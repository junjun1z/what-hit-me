package io.github.junjun1z.whathitme.mc;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Registries;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.text.Txt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;

/** The 1.21.1 client for the core. The loader adds sending packets. */
public abstract class ClientPortMc implements io.github.junjun1z.whathitme.core.client.ClientPort {

    private final Registries121 registries = new Registries121(ClientPortMc::access);

    protected static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static RegistryAccess access() {
        Minecraft mc = mc();
        if (mc == null) return null; // not started yet (NeoForge loads mods in parallel with the game start)
        if (mc.getConnection() != null) return mc.getConnection().registryAccess();
        return mc.level == null ? null : mc.level.registryAccess();
    }

    /** Call once: estimates use the client's damage type registry for ids */
    public void install() {
        ClientCore.port = this;
        ClientCore.msgIdToTypeId = registries.msgIdToTypeId();
    }

    @Override
    public void print(Txt line) {
        if (mc().gui != null) mc().gui.getChat().addMessage(McText.of(line));
    }

    @Override
    public void sendServerCommand(String commandWithoutSlash) {
        // the server has the mod (checked by the core): ask it to run its /whm directly
        String args = commandWithoutSlash.startsWith("whm") ? commandWithoutSlash.substring(3).trim() : commandWithoutSlash;
        send(new Packets.Request(Packets.Request.COMMAND, args));
    }

    @Override
    public void setClipboard(String text) {
        mc().keyboardHandler.setClipboard(text);
    }

    @Override
    public String getClipboard() {
        return mc().keyboardHandler.getClipboard();
    }

    @Override
    public boolean isSingleplayer() {
        return mc().hasSingleplayerServer();
    }

    @Override
    public boolean hasOpPermission() {
        return mc().player != null && mc().player.hasPermissions(2);
    }

    @Override
    public List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        if (mc().getConnection() != null) {
            for (PlayerInfo i : mc().getConnection().getOnlinePlayers()) names.add(i.getProfile().getName());
        }
        return names;
    }

    @Override
    public List<Entry> localEffects() {
        return mc().player == null ? new ArrayList<>() : Registries121.effects(mc().player);
    }

    @Override
    public Registries registries() {
        return registries;
    }

    @Override
    public File gameDir() {
        return mc().gameDirectory;
    }

    @Override
    public boolean playerDead() {
        return mc().player == null || mc().player.isDeadOrDying() || mc().player.isRemoved() || mc().screen instanceof DeathScreen;
    }

    @Override
    public String[] parseDeathMessage(String json) {
        return McText.translationOf(json);
    }

    /** The death message of a death screen as JSON (its only Component field), "" when unknown */
    public static String deathMessage(DeathScreen screen) {
        try {
            for (Field f : DeathScreen.class.getDeclaredFields()) {
                if (f.getType() == Component.class) {
                    f.setAccessible(true);
                    Component c = (Component) f.get(screen);
                    return c == null ? "" : McText.toJson(c);
                }
            }
        } catch (Throwable ignored) {
            // no message
        }
        return "";
    }

    /** Set by /whm config, opened by {@link #tickConfigScreen} on the next client tick (the chat screen closes after the command) */
    private static volatile boolean openConfig;
    /** Builds the config screen from its parent (Fabric: the Mod Menu factory when Mod Menu is installed) */
    public static volatile java.util.function.Function<net.minecraft.client.gui.screens.Screen, net.minecraft.client.gui.screens.Screen> configScreen = ConfigScreen121::new;

    @Override
    public boolean openConfigScreen() {
        openConfig = true;
        return true;
    }

    /** Set by /whm template, opened by {@link #tickConfigScreen} on the next client tick */
    private static volatile boolean openEditor;

    @Override
    public boolean openTemplateEditor() {
        openEditor = true;
        return true;
    }

    @Override
    public String playerName() {
        return mc().player == null ? "" : mc().player.getGameProfile().getName();
    }

    /** Called by the loader's client tick */
    public static void tickConfigScreen() {
        if (openEditor) {
            openEditor = false;
            io.github.junjun1z.whathitme.core.Safe.run("template editor", () -> mc().setScreen(new TemplateEditorScreen121(mc().screen)));
        }
        if (!openConfig) return;
        openConfig = false;

        io.github.junjun1z.whathitme.core.Safe.run("config screen", () -> mc().setScreen(configScreen.apply(mc().screen)));
    }

    @Override
    public io.github.junjun1z.whathitme.core.data.Gear localGear() {
        net.minecraft.world.entity.player.Player p = mc().player;
        if (p == null) return null;
        return new io.github.junjun1z.whathitme.core.data.Gear(Facts.dimension(p.level()), p.getX(), p.getY(), p.getZ(), p.getMaxHealth(), Facts.armor(p), Registries121.effects(p));
    }
}
