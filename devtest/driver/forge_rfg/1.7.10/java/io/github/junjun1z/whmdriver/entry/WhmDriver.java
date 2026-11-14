package io.github.junjun1z.whmdriver.entry;

import io.github.junjun1z.whmdriver.DriverPlan;
import io.github.junjun1z.whmdriver.DriverPort;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Development only: the Forge 1.7.10 part of the "client without What Hit Me?" driver ({@link DriverPlan}). */
@Mod(modid = "whmdriver", name = "WHM test driver", version = "1", acceptableRemoteVersions = "*")
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
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(this);
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    @Override
    public String mcVersion() {
        return "1.7.10";
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
