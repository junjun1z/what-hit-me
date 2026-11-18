package io.github.junjun1z.whathitme.forge;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.input.Mouse;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.client.DeathPanel;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.mc.FallbackEnv17;
import io.github.junjun1z.whathitme.mc.Gfx17;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiScreen;
import io.github.junjun1z.whathitme.core.text.Txt;
import io.github.junjun1z.whathitme.mc.McText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;

/**
 * Forge 1.7.10 client events: ticks (hello, respawn summary, client fallback), death screen panel, config screen.
 * Registered on both event buses (Forge events and FML events are separate buses on 1.7.10).
 * <p>
 * 1.7.10 has no scheduled tasks: work from other threads is queued and run at the next client tick. It also has no GUI
 * mouse event: the death panel reads the mouse while the death screen is drawn (it cannot hide a click from the screen).
 */
@SideOnly(Side.CLIENT)
public final class ClientEvents {

    /** Work for the client thread (normally empty) */
    static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

    private boolean[] down = new boolean[3];
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!Platform.MODID.equals(event.modID)) return;
        Safe.run("config screen save", () -> {
            WhmConfig.client.saveAndReload();
            WhmConfig.common.saveAndReload();
        });
        // single player: the integrated server uses the same common file; tell the client side about it
        if (Minecraft.getMinecraft().getIntegratedServer() != null) ServerEvents.TASKS.add(ServerCore::broadcastInfo);
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        TASKS.add(ClientCore::reset);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!TASKS.isEmpty()) {
            Runnable r;
            while ((r = TASKS.poll()) != null) r.run();
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (ClientPort17.openConfig) {
            ClientPort17.openConfig = false;
            Safe.run("config screen", () -> mc.displayGuiScreen(new WhmGuiFactory.Screen(mc.currentScreen)));
        }
        if (ClientPort17.openEditor) {
            ClientPort17.openEditor = false;
            Safe.run("template editor", () -> mc.displayGuiScreen(new GuiTemplateEditor(mc.currentScreen)));
        }
        if (ClientPort17.openPack != 0) {
            boolean personal = ClientPort17.openPack == 2;
            ClientPort17.openPack = 0;
            Safe.run("pack editor", () -> mc.displayGuiScreen(new GuiTemplateEditor(mc.currentScreen, new io.github.junjun1z.whathitme.core.client.PackEditor(personal))));
        }
        if (mc.thePlayer == null || mc.theWorld == null) return;
        ClientCore.tick(new FallbackEnv17(mc));
    }

    /** Custom death messages of a server with the mod replace the vanilla chat line */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onChat(ClientChatReceivedEvent event) {
        if (event.message == null) return;
        Txt t = ClientCore.chatReplacement(event.message.getUnformattedText());
        if (t != null) event.message = McText.of(t);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // the 1.7.10 death screen shows no death message: the server's recap (or the chat) has it
        if (event.gui instanceof GuiGameOver) {
            ClientCore.onDeathScreen("");
            down = new boolean[3];
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiGameOver)) return;
        GuiScreen gui = event.gui;
        pollMouse(gui, event.mouseX, event.mouseY);
        DeathPanel.INSTANCE.render(Gfx17.INSTANCE, gui.width, gui.height, event.mouseX, event.mouseY);
    }

    private void pollMouse(GuiScreen gui, int mx, int my) {
        for (int b = 0; b < down.length; b++) {
            boolean now = Mouse.isButtonDown(b);
            if (now != down[b]) {
                down[b] = now;
                if (now) DeathPanel.INSTANCE.mouseDown(b, mx, my);
                else DeathPanel.INSTANCE.mouseUp(b, mx, my, gui.width, gui.height);
            }
        }
        if ((mx != lastX || my != lastY) && down[0]) DeathPanel.INSTANCE.mouseDrag(mx, my);
        lastX = mx;
        lastY = my;
        int wheel = Mouse.getDWheel();
        if (wheel != 0) DeathPanel.INSTANCE.scroll(mx, my, wheel);
    }
}
