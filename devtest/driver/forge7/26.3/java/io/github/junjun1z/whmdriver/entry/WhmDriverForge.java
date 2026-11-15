package io.github.junjun1z.whmdriver.entry;

import java.io.File;

import io.github.junjun1z.whmdriver.DriverPlan;
import io.github.junjun1z.whmdriver.DriverPort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

/** Development only: the Forge 26.3 part of the "client without What Hit Me?" driver ({@link DriverPlan}). */
@Mod("whmdriver")
public class WhmDriverForge implements DriverPort {

    private DriverPlan plan;

    public WhmDriverForge() {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        String mode = DriverPlan.mode(FMLPaths.CONFIGDIR.get().toFile());
        if (mode == null) return;
        if (ModList.isLoaded("whathitme")) {
            System.out.println("WHMDRIVER: What Hit Me? is installed here, this test needs a client without it");
            return;
        }
        plan = new DriverPlan(this, mode);
        ClientChatReceivedEvent.BUS.addListener((java.util.function.Consumer<ClientChatReceivedEvent>) e -> plan.onChat(e.getMessage().getString()));
        // server messages (system chat) come through their own event
        net.minecraftforge.client.event.SystemMessageReceivedEvent.BUS.addListener(
            (java.util.function.Consumer<net.minecraftforge.client.event.SystemMessageReceivedEvent>) e -> {
                if (!e.isOverlay()) plan.onChat(e.getMessage().getString());
            });
        TickEvent.ClientTickEvent.Post.BUS.addListener(e -> plan.tick());
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    @Override
    public String mcVersion() {
        return "26.3";
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
        return screen() instanceof DeathScreen;
    }

    @Override
    public void respawn() {
        mc().player.respawn();
        try {
            try {
                Minecraft.class.getMethod("setScreen", net.minecraft.client.gui.screens.Screen.class).invoke(mc(), (Object) null);
            } catch (NoSuchMethodException e) {
                Object gui = Minecraft.class.getField("gui").get(mc());
                gui.getClass().getMethod("setScreen", net.minecraft.client.gui.screens.Screen.class).invoke(gui, (Object) null);
            }
        } catch (Exception e) {
            // the death screen stays
        }
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

    /** Current screen: the field Minecraft.screen (26.1) or Minecraft.gui.screen() (26.2+) */
    private static Object screen() {
        try {
            try {
                return Minecraft.class.getField("screen").get(mc());
            } catch (NoSuchFieldException e) {
                Object gui = Minecraft.class.getField("gui").get(mc());
                return gui.getClass().getMethod("screen").invoke(gui);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
