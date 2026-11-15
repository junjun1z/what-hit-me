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
    static final String MC = "26.3";

    private DriverPlan plan;

    public WhmDriverNeoForge() {
        if (!FMLEnvironment.getDist().isClient()) return;
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
