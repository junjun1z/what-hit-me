package io.github.junjun1z.whmdriver.entry;

import java.io.File;

import io.github.junjun1z.whmdriver.DriverPlan;
import io.github.junjun1z.whmdriver.DriverPort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

/** Development only: the Forge 1.21.5 part of the "client without What Hit Me?" driver ({@link DriverPlan}). */
@Mod("whmdriver")
public class WhmDriverForge implements DriverPort {

    private DriverPlan plan;

    public WhmDriverForge() {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        String mode = DriverPlan.mode(FMLPaths.CONFIGDIR.get().toFile());
        if (mode == null) return;
        if (ModList.get().isLoaded("whathitme")) {
            System.out.println("WHMDRIVER: What Hit Me? is installed here, this test needs a client without it");
            return;
        }
        plan = new DriverPlan(this, mode);
        MinecraftForge.EVENT_BUS.register(this);
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
        return "1.21.5";
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

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        plan.onChat(e.getMessage().getString());
    }

    /** Server messages (system chat) come through their own event on Forge 1.21.x */
    @SubscribeEvent
    public void onSystemChat(net.minecraftforge.client.event.SystemMessageReceivedEvent e) {
        if (!e.isOverlay()) plan.onChat(e.getMessage().getString());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) plan.tick();
    }
}
