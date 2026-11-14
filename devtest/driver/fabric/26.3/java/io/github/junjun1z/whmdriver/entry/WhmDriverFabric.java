package io.github.junjun1z.whmdriver.entry;

import java.io.File;

import io.github.junjun1z.whmdriver.DriverPlan;
import io.github.junjun1z.whmdriver.DriverPort;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

/** Development only: the Fabric 26.3 part of the "client without What Hit Me?" driver ({@link DriverPlan}). */
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
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) plan.onChat(message.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, time) -> plan.onChat(message.getString()));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> plan.tick());
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

    /** Current screen: Minecraft.screen() (26.2+) or the field Minecraft.screen (26.1) */
    private static Object screen() {
        try {
            try {
                return Minecraft.class.getMethod("screen").invoke(mc());
            } catch (NoSuchMethodException e) {
                return Minecraft.class.getField("screen").get(mc());
            }
        } catch (Exception e) {
            return null;
        }
    }
}
