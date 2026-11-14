package io.github.junjun1z.whmdriver.entry;

import java.io.File;

import io.github.junjun1z.whmdriver.DriverPlan;
import io.github.junjun1z.whmdriver.DriverPort;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

/** Development only: the Fabric 1.19.2 part of the "client without What Hit Me?" driver ({@link DriverPlan}). */
public class WhmDriverFabric implements ClientModInitializer, DriverPort {

    private DriverPlan plan;

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
            pollChat();
            plan.tick();
        });
    }

    /** Chat lines already seen (the chat keeps the newest line first) */
    private Object newest;

    /** Fabric API for 1.19.2 has no client chat events: hands the chat lines added since the last tick to the plan */
    @SuppressWarnings("unchecked")
    private void pollChat() {
        if (mc().gui == null) return;
        java.util.List<net.minecraft.client.GuiMessage> all = null;
        for (java.lang.reflect.Field f : net.minecraft.client.gui.components.ChatComponent.class.getDeclaredFields()) {
            java.lang.reflect.Type t = f.getGenericType();
            if (!java.util.List.class.isAssignableFrom(f.getType()) || !(t instanceof java.lang.reflect.ParameterizedType)) continue;
            java.lang.reflect.Type el = ((java.lang.reflect.ParameterizedType) t).getActualTypeArguments()[0];
            if (el == net.minecraft.client.GuiMessage.class) {
                try {
                    f.setAccessible(true);
                    all = (java.util.List<net.minecraft.client.GuiMessage>) f.get(mc().gui.getChat());
                } catch (Exception ignored) {
                    // next field
                }
            }
        }
        if (all == null || all.isEmpty()) return;
        java.util.List<String> fresh = new java.util.ArrayList<>();
        for (net.minecraft.client.GuiMessage m : new java.util.ArrayList<>(all)) {
            if (m == newest) break;
            fresh.add(0, m.content().getString());
        }
        newest = all.get(0);
        for (String s : fresh) plan.onChat(s);
    }

    private static Minecraft mc() {
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
        if (text.startsWith("/")) mc().player.commandSigned(text.substring(1), null);
        else mc().player.chatSigned(text, null);
    }

    @Override
    public void quit() {
        mc().stop();
    }
}
