package io.github.junjun1z.whmdriver.entry;

import java.io.File;

import io.github.junjun1z.whmdriver.DriverPlan;
import io.github.junjun1z.whmdriver.DriverPort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Development only: the NeoForge part of the "client without What Hit Me?" driver ({@link DriverPlan}). */
@Mod("whmdriver")
public class WhmDriverNeoForge implements DriverPort {

    /** Minecraft version for the vanilla command syntax */
    static final String MC = "1.21.1";

    private DriverPlan plan;

    public WhmDriverNeoForge() {
        if (!FMLEnvironment.dist.isClient()) return;
        String mode = DriverPlan.mode(FMLPaths.CONFIGDIR.get().toFile());
        if (mode == null) return;
        if (ModList.get().isLoaded("whathitme")) {
            System.out.println("WHMDRIVER: What Hit Me? is installed here, this test needs a client without it");
            return;
        }
        plan = new DriverPlan(this, mode);
        NeoForge.EVENT_BUS.addListener((ClientChatReceivedEvent e) -> plan.onChat(e.getMessage().getString()));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> plan.tick());
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    @Override
    public String disconnectReason() {
        Object s = mc().screen;
        if (!(s instanceof net.minecraft.client.gui.screens.DisconnectedScreen)) return "";
        StringBuilder sb = new StringBuilder();
        texts(s, net.minecraft.client.gui.screens.DisconnectedScreen.class, sb, 0);
        String r = sb.toString().trim();
        return r.isEmpty() ? "?" : r;
    }

    /** Component fields of the screen and of records it holds (DisconnectionDetails) */
    private static void texts(Object o, Class<?> c, StringBuilder sb, int depth) {
        for (java.lang.reflect.Field f : c.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(o);
                if (v instanceof net.minecraft.network.chat.Component) sb.append(((net.minecraft.network.chat.Component) v).getString()).append(' ');
                else if (v != null && depth == 0 && v.getClass().isRecord()) texts(v, v.getClass(), sb, 1);
            } catch (Throwable ignored) {
                // next field
            }
        }
    }

    @Override
    public String mcVersion() {
        return MC;
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
        if (text.startsWith("/")) mc().player.connection.sendCommand(text.substring(1));
        else mc().player.connection.sendChat(text);
    }

    @Override
    public void quit() {
        mc().stop();
    }
}
