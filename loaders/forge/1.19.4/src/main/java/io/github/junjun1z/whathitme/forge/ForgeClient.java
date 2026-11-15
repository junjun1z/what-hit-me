package io.github.junjun1z.whathitme.forge;

import java.io.File;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.client.DeathPanel;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.mc.Brig;
import io.github.junjun1z.whathitme.mc.ClientPortMc;
import io.github.junjun1z.whathitme.mc.FallbackEnv1194;
import io.github.junjun1z.whathitme.mc.Gfx1194;
import io.github.junjun1z.whathitme.mc.McText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Forge 1.19.4 client side: ticks, death screen panel, client /whm, packets from the server. */
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

    private ForgeClient() {}

    static void init() {
        Platform.clientLang = McText.LANG;
        PORT.install();
        MinecraftForge.EVENT_BUS.register(new ForgeClient());
        startDevTest();
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
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ClientCore.tick(new FallbackEnv1194(mc));
    }

    @SubscribeEvent
    public void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof DeathScreen) ClientCore.onDeathScreen(ClientPortMc.deathMessage((DeathScreen) event.getNewScreen()));
    }

    @SubscribeEvent
    public void onRender(ScreenEvent.Render.Post event) {
        Screen s = event.getScreen();
        if (!(s instanceof DeathScreen)) return;
        DeathPanel.INSTANCE.render(new Gfx1194(event.getPoseStack(), s), s.width, s.height, event.getMouseX(), event.getMouseY());
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
        if (event.getScreen() instanceof DeathScreen && DeathPanel.INSTANCE.scroll(event.getMouseX(), event.getMouseY(), event.getScrollDelta())) event.setCanceled(true);
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
