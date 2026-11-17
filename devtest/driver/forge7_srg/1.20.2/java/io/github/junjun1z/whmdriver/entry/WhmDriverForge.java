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

/** Development only: the Forge 1.20.2 part of the "client without What Hit Me?" driver ({@link DriverPlan}). */
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
        for (java.lang.reflect.Field f : net.minecraft.client.gui.screens.DisconnectedScreen.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(s);
                if (v instanceof net.minecraft.network.chat.Component) sb.append(((net.minecraft.network.chat.Component) v).getString()).append(' ');
            } catch (Throwable ignored) {
                // next field
            }
        }
        String r = sb.toString().trim();
        return r.isEmpty() ? "?" : r;
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

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) plan.tick();
    }
}
