package io.github.junjun1z.whathitme.neoforge;

import java.io.File;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.data.CatalogCache;
import io.github.junjun1z.whathitme.core.client.DeathPanel;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.mc.Brig;
import io.github.junjun1z.whathitme.mc.ClientPortMc;
import io.github.junjun1z.whathitme.mc.ConfigScreen1206;
import io.github.junjun1z.whathitme.mc.FallbackEnv1206;
import io.github.junjun1z.whathitme.mc.Gfx1206;
import io.github.junjun1z.whathitme.mc.McText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/** NeoForge 1.20.6 client side: ticks, death screen panel, client /whm, packets from the server. */
public final class NeoClient {

    private static final ClientPortMc PORT = new ClientPortMc() {
        @Override
        public void send(Packets.Packet packet) {
            if (connected()) NeoNet.sendToServer(packet);
        }

        @Override
        public boolean connected() {
            return NeoNet.serverPresent();
        }
    };

    /** The catalog cache was loaded once (connected to a server without the integrated one) */
    private static boolean catalogTried;

    private NeoClient() {}

    static void init() {
        Platform.clientLang = McText.LANG;
        PORT.install();
        registerConfigScreen();
        NeoForge.EVENT_BUS.register(new NeoClient());
        startDevTest();
    }

    /** Config button of the mods list (locked keys disabled) */
    private static void registerConfigScreen() {
        net.neoforged.neoforge.client.gui.IConfigScreenFactory factory = (mc, parent) -> new ConfigScreen1206(parent);
        net.neoforged.fml.ModList.get().getModContainerById(Platform.MODID)
            .ifPresent(c -> c.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class, factory));
    }

    /** Server -> client packet (already on the client thread) */
    static void onPacket(Packets.Packet p) {
        ClientCore.onPacket(p);
    }

    @SubscribeEvent
    public void onClientCommands(RegisterClientCommandsEvent event) {
        Brig.register(event.getDispatcher(), (s, args) -> ClientCore.execute(args), (s, args) -> ClientCore.complete(args));
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCore.reset();
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ClientCore.tick(new FallbackEnv1206(mc));
        ClientPortMc.tickConfigScreen();
        if (!catalogTried) {
            catalogTried = true;
            if (!CatalogCache.loaded()) Safe.run("catalog cache", () -> CatalogCache.init(PORT.registries()));
        }
    }

    @SubscribeEvent
    public void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof DeathScreen)) return;
        DeathScreen s = (DeathScreen) event.getNewScreen();
        ClientCore.onDeathScreen(ClientPortMc.deathMessage(s));
        Safe.run("death screen message", () -> ClientPortMc.replaceDeathMessage(s));
    }

    /** Custom death messages of a server with the mod replace the vanilla chat line (1.19+: a system message) */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onChat(ClientChatReceivedEvent event) {
        if (event.getMessage() == null) return;
        io.github.junjun1z.whathitme.core.text.Txt t = ClientCore.chatReplacement(event.getMessage().getString());
        if (t != null) event.setMessage(McText.of(t));
    }

    @SubscribeEvent
    public void onRender(ScreenEvent.Render.Post event) {
        Screen s = event.getScreen();
        if (!(s instanceof DeathScreen)) return;
        DeathPanel.INSTANCE.render(new Gfx1206(event.getGuiGraphics()), s.width, s.height, event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof DeathScreen && DeathPanel.INSTANCE.mouseDown(event.getButton(), event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        Screen s = event.getScreen();
        if (s instanceof DeathScreen && DeathPanel.INSTANCE.mouseUp(event.getButton(), event.getMouseX(), event.getMouseY(), s.width, s.height)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (event.getScreen() instanceof DeathScreen && DeathPanel.INSTANCE.mouseDrag(event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (event.getScreen() instanceof DeathScreen && DeathPanel.INSTANCE.scroll(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) event.setCanceled(true);
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
            WhatHitMeNeoForge.LOG.error("Self-test failed to start", t);
        }
    }
}
