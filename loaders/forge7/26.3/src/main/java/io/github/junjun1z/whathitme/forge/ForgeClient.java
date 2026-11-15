package io.github.junjun1z.whathitme.forge;

import java.io.File;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.data.CatalogCache;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.client.DeathPanel;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.mc.Brig;
import io.github.junjun1z.whathitme.mc.ClientPortMc;
import io.github.junjun1z.whathitme.mc.ConfigScreen121;
import io.github.junjun1z.whathitme.mc.FallbackEnv121;
import io.github.junjun1z.whathitme.mc.Gfx121;
import io.github.junjun1z.whathitme.mc.McText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.Priority;

/** Forge 1.21.6+ (EventBus 7) client side: ticks, death screen panel, client /whm, packets from the server. */
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

    /** Config button of the mods list (locked keys disabled) */
    private static void registerConfigScreen() {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new ConfigScreen121(parent)));
    }

    static void init() {
        Platform.clientLang = McText.LANG;
        PORT.install();
        registerConfigScreen();
        register();
        startDevTest();
    }

    /** Server -> client packet (already on the client thread) */
    static void onPacket(Packets.Packet p) {
        ClientCore.onPacket(p);
    }

    private static void register() {
        RegisterClientCommandsEvent.BUS.addListener(event -> Brig.register(event.getDispatcher(), (s, args) -> ClientCore.execute(args),
            (s, args) -> ClientCore.complete(args)));
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> ClientCore.reset());
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            ClientCore.tick(new FallbackEnv121(mc));
            ClientPortMc.tickConfigScreen();
            if (!catalogTried) {
                catalogTried = true;
                if (!CatalogCache.loaded()) Safe.run("catalog cache", () -> CatalogCache.init(PORT.registries()));
            }
        });
        ScreenEvent.Opening.BUS.addListener(Priority.LOWEST, (ScreenEvent.Opening event) -> {
            if (event.getNewScreen() instanceof DeathScreen) ClientCore.onDeathScreen(ClientPortMc.deathMessage((DeathScreen) event.getNewScreen()));
            return false;
        });
        ScreenEvent.Render.Post.BUS.addListener(event -> {
            Screen s = event.getScreen();
            if (!(s instanceof DeathScreen)) return;
            DeathPanel.INSTANCE.render(new Gfx121(event.getGuiGraphics()), s.width, s.height, event.getMouseX(), event.getMouseY());
        });
        // predicate listeners: returning true cancels the event (the screen never sees it)
        ScreenEvent.MouseButtonPressed.Pre.BUS.addListener(Priority.HIGH, (ScreenEvent.MouseButtonPressed.Pre event) ->
            event.getScreen() instanceof DeathScreen && DeathPanel.INSTANCE.mouseDown(io.github.junjun1z.whathitme.mc.Compat26.button(event.getButton()), event.getMouseX(), event.getMouseY()));
        ScreenEvent.MouseButtonReleased.Pre.BUS.addListener(Priority.HIGH, (ScreenEvent.MouseButtonReleased.Pre event) -> {
            Screen s = event.getScreen();
            return s instanceof DeathScreen && DeathPanel.INSTANCE.mouseUp(io.github.junjun1z.whathitme.mc.Compat26.button(event.getButton()), event.getMouseX(), event.getMouseY(), s.width, s.height);
        });
        ScreenEvent.MouseDragged.Pre.BUS.addListener(Priority.HIGH, (ScreenEvent.MouseDragged.Pre event) ->
            event.getScreen() instanceof DeathScreen && DeathPanel.INSTANCE.mouseDrag(event.getMouseX(), event.getMouseY()));
        ScreenEvent.MouseScrolled.Pre.BUS.addListener(Priority.HIGH, (ScreenEvent.MouseScrolled.Pre event) ->
            event.getScreen() instanceof DeathScreen && DeathPanel.INSTANCE.scroll(event.getMouseX(), event.getMouseY(), event.getDeltaY()));
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
