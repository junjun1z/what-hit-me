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

/** The 1.20.6 client for the core. The loader adds sending packets. */
public abstract class ClientPortMc implements io.github.junjun1z.whathitme.core.client.ClientPort {

    private final Registries1206 registries = new Registries1206(ClientPortMc::access);

    protected static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private static RegistryAccess access() {
        Minecraft mc = mc();
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
        return mc().player == null ? new ArrayList<>() : Registries1206.effects(mc().player);
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

    /** Forge / NeoForge: a custom death message of a server with the mod replaces the vanilla one on the death screen */
    public static void replaceDeathMessage(DeathScreen screen) {
        try {
            for (Field f : DeathScreen.class.getDeclaredFields()) {
                if (f.getType() != Component.class) continue;
                f.setAccessible(true);
                Component c = (Component) f.get(screen);
                Txt t = c == null ? null : ClientCore.deathScreenReplacement(c.getString());
                if (t != null) f.set(screen, McText.of(t));
                return;
            }
        } catch (Throwable ignored) {
            // keep the vanilla message
        }
    }

    /** Newest chat message already checked by {@link #replaceDeathMessageInChat} */
    private static Object lastChatChecked;

    /**
     * Forge 1.20.6 posts no event for system chat messages: a custom death message of a server with the mod replaces the
     * vanilla line after it was added (called every client tick, checks only the messages added since the last call).
     */
    @SuppressWarnings("unchecked")
    public static void replaceDeathMessageInChat() {
        net.minecraft.client.gui.components.ChatComponent chat = mc().gui.getChat();
        List<net.minecraft.client.GuiMessage> list = null;
        try {
            for (Field f : net.minecraft.client.gui.components.ChatComponent.class.getDeclaredFields()) {
                java.lang.reflect.Type t = f.getGenericType();
                if (List.class.isAssignableFrom(f.getType()) && t instanceof java.lang.reflect.ParameterizedType
                    && ((java.lang.reflect.ParameterizedType) t).getActualTypeArguments()[0] == net.minecraft.client.GuiMessage.class) {
                    f.setAccessible(true);
                    list = (List<net.minecraft.client.GuiMessage>) f.get(chat);
                    break;
                }
            }
        } catch (Throwable ignored) {
            // no chat list
        }
        if (list == null || list.isEmpty() || list.get(0) == lastChatChecked) return;
        boolean changed = false;
        for (int i = 0; i < Math.min(10, list.size()); i++) {
            net.minecraft.client.GuiMessage m = list.get(i);
            if (m == lastChatChecked) break;
            Txt t = ClientCore.chatReplacement(m.content().getString());
            if (t == null) continue;
            list.set(i, new net.minecraft.client.GuiMessage(m.addedTime(), McText.of(t), m.signature(), m.tag()));
            changed = true;
        }
        lastChatChecked = list.get(0);
        if (changed) chat.rescaleChat();
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
    public static volatile java.util.function.Function<net.minecraft.client.gui.screens.Screen, net.minecraft.client.gui.screens.Screen> configScreen = ConfigScreen1206::new;

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
            io.github.junjun1z.whathitme.core.Safe.run("template editor", () -> mc().setScreen(new TemplateEditorScreen1206(mc().screen)));
        }
        if (!openConfig) return;
        openConfig = false;
        io.github.junjun1z.whathitme.core.Safe.run("config screen", () -> mc().setScreen(configScreen.apply(mc().screen)));
    }

    @Override
    public io.github.junjun1z.whathitme.core.data.Gear localGear() {
        net.minecraft.world.entity.player.Player p = mc().player;
        if (p == null) return null;
        return new io.github.junjun1z.whathitme.core.data.Gear(Facts.dimension(p.level()), p.getX(), p.getY(), p.getZ(), p.getMaxHealth(), Facts.armor(p), Registries1206.effects(p));
    }
}
