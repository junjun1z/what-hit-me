package io.github.junjun1z.whmdriver.entry;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whmdriver.DriverPlan;
import io.github.junjun1z.whmdriver.DriverPort;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;

/**
 * Development only: the Fabric 1.14 - 1.17 part of the "client without What Hit Me?" driver ({@link DriverPlan}). This
 * Fabric API has no chat receive event: new chat lines are read from the chat component every tick.
 */
public class WhmDriverFabric implements ClientModInitializer, DriverPort {

    private DriverPlan plan;
    private Object lastTop;

    @Override
    public void onInitializeClient() {
        String mode = DriverPlan.mode(FabricLoader.getInstance().getConfigDir().toFile());
        if (mode == null) return;
        if (FabricLoader.getInstance().isModLoaded("whathitme")) {
            System.out.println("WHMDRIVER: What Hit Me? is installed here, this test needs a client without it");
            return;
        }
        plan = new DriverPlan(this, mode);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            autoConnect();
            pollChat();
            plan.tick();
        });
    }

    private boolean connectTried;

    /** Joins the server in .minecraft/whm_connect.txt at the title screen (Prism's --server crashes 1.14 - 1.17 clients) */
    private void autoConnect() {
        if (connectTried || !(mc().screen instanceof net.minecraft.client.gui.screens.TitleScreen) || mc().getOverlay() != null) return;
        connectTried = true;
        File f = new File(mc().gameDirectory, "whm_connect.txt");
        if (!f.isFile()) return;
        try {
            String a = new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8).trim();
            int i = a.lastIndexOf(':');
            mc().setScreen(new net.minecraft.client.gui.screens.ConnectScreen(mc().screen, mc(), i > 0 ? a.substring(0, i) : a,
                i > 0 ? Integer.parseInt(a.substring(i + 1)) : 25565));
        } catch (Exception e) {
            System.out.println("WHMDRIVER cannot connect: " + e);
        }
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    /** The chat lines (newest first, unwrapped Components) */
    @SuppressWarnings("unchecked")
    private static List<GuiMessage<Component>> messages() {
        if (mc().gui == null) return new ArrayList<>();
        ChatComponent chat = mc().gui.getChat();
        for (Field f : ChatComponent.class.getDeclaredFields()) {
            if (!List.class.isAssignableFrom(f.getType())) continue;
            Type t = f.getGenericType();
            if (!(t instanceof ParameterizedType)) continue;
            Type e = ((ParameterizedType) t).getActualTypeArguments()[0];
            if (e instanceof ParameterizedType && ((ParameterizedType) e).getRawType() == GuiMessage.class
                && ((ParameterizedType) e).getActualTypeArguments()[0] == Component.class) {
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

    private void pollChat() {
        List<GuiMessage<Component>> all = new ArrayList<>(messages());
        List<String> fresh = new ArrayList<>();
        for (GuiMessage<Component> m : all) {
            if (m == lastTop) break;
            fresh.add(m.getMessage().getString());
        }
        if (!all.isEmpty()) lastTop = all.get(0);
        for (int i = fresh.size() - 1; i >= 0; i--) plan.onChat(fresh.get(i));
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
    public boolean connected() {
        return mc().player != null && mc().getConnection() != null;
    }

    @Override
    public boolean deathScreen() {
        return mc().screen instanceof DeathScreen;
    }

    @Override
    public void respawn() {
        mc().player.respawn();
        mc().setScreen(null);
    }

    @Override
    public void say(String text) {
        mc().player.chat(text);
    }

    @Override
    public void quit() {
        mc().stop();
    }
}
