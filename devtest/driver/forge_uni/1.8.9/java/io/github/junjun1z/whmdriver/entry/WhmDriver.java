package io.github.junjun1z.whmdriver.entry;

import io.github.junjun1z.whmdriver.DriverPlan;
import io.github.junjun1z.whmdriver.DriverPort;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Development only: the Forge 1.8.9 part of the "client without What Hit Me?" driver ({@link DriverPlan}). */
@Mod(modid = "whmdriver", name = "WHM test driver", version = "1", clientSideOnly = true, acceptableRemoteVersions = "*")
public class WhmDriver implements DriverPort {

    private DriverPlan plan;

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        String mode = DriverPlan.mode(Loader.instance().getConfigDir());
        if (mode == null) return;
        if (Loader.isModLoaded("whathitme")) {
            System.out.println("WHMDRIVER: What Hit Me? is installed here, this test needs a client without it");
            return;
        }
        plan = new DriverPlan(this, mode);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    @Override
    public String mcVersion() {
        return "1.8.9";
    }

    @Override
    public File gameDir() {
        return mc().mcDataDir;
    }

    @Override
    public boolean connected() {
        return mc().thePlayer != null && mc().getNetHandler() != null;
    }

    @Override
    public boolean deathScreen() {
        return mc().currentScreen instanceof GuiGameOver;
    }

    @Override
    public void respawn() {
        mc().thePlayer.respawnPlayer();
        mc().displayGuiScreen(null);
    }

    @Override
    public void say(String text) {
        mc().thePlayer.sendChatMessage(text);
    }

    @Override
    public void quit() {
        mc().shutdown();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        plan.onChat(e.message.getUnformattedText());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) plan.tick();
    }
}
