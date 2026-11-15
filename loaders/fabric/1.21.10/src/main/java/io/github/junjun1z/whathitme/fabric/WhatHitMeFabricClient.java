package io.github.junjun1z.whathitme.fabric;

import java.io.File;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.data.CatalogCache;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.client.DeathPanel;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.mc.Brig;
import io.github.junjun1z.whathitme.mc.ClientPortMc;
import io.github.junjun1z.whathitme.mc.FallbackEnv121;
import io.github.junjun1z.whathitme.mc.Gfx121;
import io.github.junjun1z.whathitme.mc.McText;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;

/** Fabric 1.21.10 client side: ticks, death screen panel, client /whm, packets from the server. */
public final class WhatHitMeFabricClient implements ClientModInitializer {

    private static final ClientPortMc PORT = new ClientPortMc() {
        @Override
        public void send(Packets.Packet packet) {
            if (connected()) ClientPlayNetworking.send(new FabricNet.Payload(Packets.encode(packet)));
        }

        @Override
        public boolean connected() {
            return mc().getConnection() != null && ClientPlayNetworking.canSend(FabricNet.Payload.TYPE);
        }
    };

    /** The death screen the panel was opened for (screens are re-initialized on resize) */
    private static Screen deathScreen;
    /** The catalog cache was loaded once (connected to a server without the integrated one) */
    private static boolean catalogTried;

    @Override
    public void onInitializeClient() {
        Platform.clientLang = McText.LANG;
        PORT.install();
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            try {
                ClientPortMc.configScreen = WhmModMenu::open;
            } catch (Throwable t) {
                WhatHitMeFabric.LOG.warn("Mod Menu integration not available: " + t);
            }
        }
        java.util.function.Supplier<HolderLookup.Provider> server = McText.registries;
        McText.registries = () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSingleplayerServer() != null) return mc.getSingleplayerServer().registryAccess();
            if (mc.getConnection() != null) return mc.getConnection().registryAccess();
            return server.get();
        };
        ClientPlayNetworking.registerGlobalReceiver(FabricNet.Payload.TYPE, (payload, ctx) -> {
            Packets.Packet p;
            try {
                p = Packets.decode(payload.data());
            } catch (RuntimeException e) {
                return; // malformed: ignore
            }
            if (!Packets.serverbound(p.type())) ClientCore.onPacket(p);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> Brig.register(dispatcher, (s, args) -> ClientCore.execute(args),
            (s, args) -> ClientCore.complete(args)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ClientCore::reset));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.level == null) return;
            ClientCore.tick(new FallbackEnv121(mc));
            ClientPortMc.tickConfigScreen();
            if (!catalogTried) {
                catalogTried = true;
                if (!CatalogCache.loaded()) Safe.run("catalog cache", () -> CatalogCache.init(PORT.registries()));
            }
        });
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof DeathScreen)) return;
            if (deathScreen != screen) {
                deathScreen = screen;
                ClientCore.onDeathScreen(ClientPortMc.deathMessage((DeathScreen) screen));
            }
            ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> DeathPanel.INSTANCE.render(new Gfx121(g), s.width, s.height, mx, my));
            ScreenMouseEvents.allowMouseClick(screen).register((s, e) -> !DeathPanel.INSTANCE.mouseDown(e.button(), e.x(), e.y()));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, e) -> !DeathPanel.INSTANCE.mouseUp(e.button(), e.x(), e.y(), s.width, s.height));
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
