package io.github.junjun1z.whathitme.fabric;

import java.io.File;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.client.DeathPanel;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.mc.Brig;
import io.github.junjun1z.whathitme.mc.ClientPortMc;
import io.github.junjun1z.whathitme.mc.FallbackEnv116;
import io.github.junjun1z.whathitme.mc.Gfx116;
import io.github.junjun1z.whathitme.mc.McText;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;

/** Fabric 1.16.5 client side: ticks, death screen panel, client /whm, packets from the server. */
public final class WhatHitMeFabricClient implements ClientModInitializer {

    private static final ClientPortMc PORT = new ClientPortMc() {
        @Override
        public void send(Packets.Packet packet) {
            if (connected()) ClientPlayNetworking.send(FabricNet.ID, FabricNet.buf(packet));
        }

        @Override
        public boolean connected() {
            return mc().getConnection() != null && ClientPlayNetworking.canSend(FabricNet.ID);
        }
    };

    /** The death screen the panel was opened for (screens are re-initialized on resize) */
    private static Screen deathScreen;

    @Override
    public void onInitializeClient() {
        Platform.clientLang = McText.LANG;
        PORT.install();
        ClientPlayNetworking.registerGlobalReceiver(FabricNet.ID, (client, handler, buf, sender) -> {
            Packets.Packet p = FabricNet.read(buf);
            if (p != null && !Packets.serverbound(p.type())) client.execute(() -> ClientCore.onPacket(p));
        });
        Brig.register(ClientCommandManager.DISPATCHER, (s, args) -> ClientCore.execute(args), (s, args) -> ClientCore.complete(args));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ClientCore::reset));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.level == null) return;
            ClientCore.tick(new FallbackEnv116(mc));
        });
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof DeathScreen)) return;
            if (deathScreen != screen) {
                deathScreen = screen;
                ClientCore.onDeathScreen(ClientPortMc.deathMessage((DeathScreen) screen));
            }
            ScreenEvents.afterRender(screen).register((s, pose, mx, my, delta) -> DeathPanel.INSTANCE.render(new Gfx116(pose, s), s.width, s.height, mx, my));
            ScreenMouseEvents.allowMouseClick(screen).register((s, mx, my, button) -> !DeathPanel.INSTANCE.mouseDown(button, mx, my));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, mx, my, button) -> !DeathPanel.INSTANCE.mouseUp(button, mx, my, s.width, s.height));
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mx, my, hAmount, vAmount) -> !DeathPanel.INSTANCE.scroll(mx, my, vAmount));
        });
        startDevTest();
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
            WhatHitMeFabric.LOG.error("Self-test failed to start", t);
        }
    }
}
