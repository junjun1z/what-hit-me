package io.github.junjun1z.whathitme.forge;

import java.io.File;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.data.CatalogCache;
import io.github.junjun1z.whathitme.core.client.DeathPanel;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.cmd.Completion;
import io.github.junjun1z.whathitme.mc.ClientPortMc;
import io.github.junjun1z.whathitme.mc.ConfigScreen117;
import io.github.junjun1z.whathitme.mc.FallbackEnv117;
import io.github.junjun1z.whathitme.mc.Gfx117;
import io.github.junjun1z.whathitme.mc.McText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Forge 1.17.1 client side: ticks, death screen panel, client /whm (ClientChatEvent), packets from the server. */
public final class ForgeClient {

    private static final ClientPortMc PORT = new ClientPortMc() {
        @Override
        public void send(Packets.Packet packet) {
            if (connected()) ForgeNet.sendToServer(packet);
        }

        @Override
        public boolean connected() {
            Minecraft mc = mc();
            return mc.getConnection() != null && ForgeNet.serverPresent(mc.getConnection().getConnection());
        }
    };

    /** The catalog cache was loaded once (connected to a server without the integrated one) */
    private static boolean catalogTried;

    private ForgeClient() {}

    static void init() {
        Platform.clientLang = McText.LANG;
        PORT.install();
        registerConfigScreen();
        MinecraftForge.EVENT_BUS.register(new ForgeClient());
        startDevTest();
    }

    /** Config button of the mods list (locked keys disabled) */
    private static void registerConfigScreen() {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.fmlclient.ConfigGuiHandler.ConfigGuiFactory.class,
            () -> new net.minecraftforge.fmlclient.ConfigGuiHandler.ConfigGuiFactory((mc, parent) -> new ConfigScreen117(parent)));
    }

    /** Server -> client packet (already on the client thread) */
    static void onPacket(Packets.Packet p) {
        ClientCore.onPacket(p);
    }

    /** Client /whm (this Forge has no client command registration): typed in the chat screen, never sent to the server */
    @SubscribeEvent
    public void onClientChat(ClientChatEvent event) {
        String m = event.getMessage();
        if (m == null) return;
        String args;
        if (m.equals("/whm") || m.startsWith("/whm ")) args = m.substring(4);
        else if (m.equals("/whathitme") || m.startsWith("/whathitme ")) args = m.substring(10);
        else return;
        event.setCanceled(true);
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) mc.gui.getChat().addRecentChat(m);
        ClientCore.execute(Completion.args(args.trim()));
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        ClientCore.reset();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ClientCore.tick(new FallbackEnv117(mc));
        ClientPortMc.tickConfigScreen();
        if (!catalogTried) {
            catalogTried = true;
            if (!CatalogCache.loaded()) Safe.run("catalog cache", () -> CatalogCache.init(PORT.registries()));
        }
    }

    @SubscribeEvent
    public void onScreenOpening(GuiOpenEvent event) {
        if (!(event.getGui() instanceof DeathScreen)) return;
        DeathScreen s = (DeathScreen) event.getGui();
        ClientCore.onDeathScreen(ClientPortMc.deathMessage(s));
        Safe.run("death screen message", () -> ClientPortMc.replaceDeathMessage(s));
    }

    /** Custom death messages of a server with the mod replace the vanilla chat line */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onChat(ClientChatReceivedEvent event) {
        if (event.getMessage() == null) return;
        io.github.junjun1z.whathitme.core.text.Txt t = ClientCore.chatReplacement(event.getMessage().getString());
        if (t != null) event.setMessage(McText.of(t));
    }

    @SubscribeEvent
    public void onRender(GuiScreenEvent.DrawScreenEvent.Post event) {
        Screen s = event.getGui();
        if (!(s instanceof DeathScreen)) return;
        DeathPanel.INSTANCE.render(new Gfx117(event.getMatrixStack(), s), s.width, s.height, event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMousePressed(GuiScreenEvent.MouseClickedEvent.Pre event) {
        if (event.getGui() instanceof DeathScreen && DeathPanel.INSTANCE.mouseDown(event.getButton(), event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouseReleased(GuiScreenEvent.MouseReleasedEvent.Pre event) {
        Screen s = event.getGui();
        if (s instanceof DeathScreen && DeathPanel.INSTANCE.mouseUp(event.getButton(), event.getMouseX(), event.getMouseY(), s.width, s.height)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouseDragged(GuiScreenEvent.MouseDragEvent.Pre event) {
        if (event.getGui() instanceof DeathScreen && DeathPanel.INSTANCE.mouseDrag(event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouseScrolled(GuiScreenEvent.MouseScrollEvent.Pre event) {
        if (event.getGui() instanceof DeathScreen && DeathPanel.INSTANCE.scroll(event.getMouseX(), event.getMouseY(), event.getScrollDelta())) event.setCanceled(true);
    }

    /** Development self-test, only when compiled in (-Pselftest) and config/whathitme/selftest.flag exists */
    private static void startDevTest() {
        File flag = new File(Platform.whmConfigDir(), "selftest.flag");
        if (!flag.isFile()) return;
        try {
            Class.forName("io.github.junjun1z.whathitme.devtest.DevEntry").getMethod("start", File.class).invoke(null, flag);
        } catch (ClassNotFoundException e) {
            // release build: no self-test inside
        } catch (Throwable t) {
            WhatHitMeForge.LOG.error("Self-test failed to start", t);
        }
    }
}
