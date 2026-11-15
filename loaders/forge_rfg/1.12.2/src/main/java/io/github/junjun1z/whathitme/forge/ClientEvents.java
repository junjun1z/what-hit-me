package io.github.junjun1z.whathitme.forge;

import java.lang.reflect.Field;

import org.lwjgl.input.Mouse;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.client.DeathPanel;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.mc.FallbackEnv112;
import io.github.junjun1z.whathitme.mc.Gfx112;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Forge 1.12 client events: ticks (hello, respawn summary, client fallback), death screen panel, config screen. */
@SideOnly(Side.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!Platform.MODID.equals(event.getModID())) return;
        Safe.run("config screen save", () -> {
            WhmConfig.client.saveAndReload();
            WhmConfig.common.saveAndReload();
        });
        // single player: the integrated server uses the same common file; tell the client side about it
        net.minecraft.server.integrated.IntegratedServer srv = Minecraft.getMinecraft().getIntegratedServer();
        if (srv != null) srv.addScheduledTask(ServerCore::broadcastInfo);
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(ClientCore::reset);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (ClientPort112.openConfig) {
            ClientPort112.openConfig = false;
            Safe.run("config screen", () -> mc.displayGuiScreen(new WhmGuiFactory().createConfigGui(mc.currentScreen)));
        }
        if (mc.player == null || mc.world == null) return;
        ClientCore.tick(new FallbackEnv112(mc));
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.getGui() instanceof GuiGameOver) ClientCore.onDeathScreen(deathMessage((GuiGameOver) event.getGui()));
    }

    /** The vanilla death message of the death screen (no access transformer: its only ITextComponent field) */
    private static String deathMessage(GuiGameOver gui) {
        try {
            for (Field f : GuiGameOver.class.getDeclaredFields()) {
                if (f.getType() == ITextComponent.class) {
                    f.setAccessible(true);
                    ITextComponent c = (ITextComponent) f.get(gui);
                    return c == null ? "" : ITextComponent.Serializer.componentToJson(c);
                }
            }
        } catch (Throwable ignored) {
            // no message
        }
        return "";
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiGameOver)) return;
        GuiScreen gui = event.getGui();
        DeathPanel.INSTANCE.render(Gfx112.INSTANCE, gui.width, gui.height, event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiGameOver)) return;
        GuiScreen gui = event.getGui();
        Minecraft mc = Minecraft.getMinecraft();
        int mx = Mouse.getEventX() * gui.width / mc.displayWidth;
        int my = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;
        int button = Mouse.getEventButton();
        boolean down = Mouse.getEventButtonState();
        int wheel = Mouse.getEventDWheel();
        boolean used;
        if (button == -1) {
            used = wheel != 0 ? DeathPanel.INSTANCE.scroll(mx, my, wheel) : DeathPanel.INSTANCE.mouseDrag(mx, my);
        } else if (down) {
            used = DeathPanel.INSTANCE.mouseDown(button, mx, my);
        } else {
            used = DeathPanel.INSTANCE.mouseUp(button, mx, my, gui.width, gui.height);
        }
        if (used) event.setCanceled(true);
    }
}
