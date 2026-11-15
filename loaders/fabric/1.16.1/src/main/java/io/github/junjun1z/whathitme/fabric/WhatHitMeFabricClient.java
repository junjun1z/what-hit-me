package io.github.junjun1z.whathitme.fabric;

import java.io.File;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.Safe;
import io.github.junjun1z.whathitme.core.client.ClientCore;
import io.github.junjun1z.whathitme.core.data.CatalogCache;
import io.github.junjun1z.whathitme.core.client.DeathPanel;
import io.github.junjun1z.whathitme.core.cmd.Completion;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.mc.ClientPortMc;
import io.github.junjun1z.whathitme.mc.FallbackEnv116;
import io.github.junjun1z.whathitme.mc.Gfx116;
import io.github.junjun1z.whathitme.mc.McText;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * Fabric 1.16.1 client side: ticks, death screen panel, client /whm, packets from the server. This Fabric API has no
 * screen events and no client commands: tiny client mixins (DeathScreen#init / #render, MouseHandler, LocalPlayer#chat)
 * call the static hooks below.
 */
public final class WhatHitMeFabricClient implements ClientModInitializer {

    private static final ClientPortMc PORT = new ClientPortMc() {
        @Override
        public void send(Packets.Packet packet) {
            if (connected()) ClientSidePacketRegistry.INSTANCE.sendToServer(FabricNet.ID, FabricNet.buf(packet));
        }

        @Override
        public boolean connected() {
            return mc().getConnection() != null && ClientSidePacketRegistry.INSTANCE.canServerReceive(FabricNet.ID);
        }
    };

    /** The death screen the panel was opened for (screens are re-initialized on resize) */
    private static Screen deathScreen;
    /** The catalog cache was loaded once (connected to a server without the integrated one) */
    private static boolean catalogTried;
    private static boolean inWorld;

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
        ClientSidePacketRegistry.INSTANCE.register(FabricNet.ID, (ctx, buf) -> {
            Packets.Packet p = FabricNet.read(buf);
            if (p != null && !Packets.serverbound(p.type())) ctx.getTaskQueue().execute(() -> ClientCore.onPacket(p));
        });
        ClientTickCallback.EVENT.register(mc -> {
            if (mc.player == null || mc.level == null) {
                // left the world / server (no disconnect event on this Fabric API)
                if (inWorld) {
                    inWorld = false;
                    ClientCore.reset();
                }
                return;
            }
            inWorld = true;
            ClientCore.tick(new FallbackEnv116(mc));
            ClientPortMc.tickConfigScreen();
            if (!catalogTried) {
                catalogTried = true;
                if (!CatalogCache.loaded()) Safe.run("catalog cache", () -> CatalogCache.init(PORT.registries()));
            }
        });
        startDevTest();
    }

    /** LocalPlayer#chat: client /whm, true = handled here (not sent to the server) */
    public static boolean onChat(String m) {
        if (m == null) return false;
        String args;
        if (m.equals("/whm") || m.startsWith("/whm ")) args = m.substring(4);
        else if (m.equals("/whathitme") || m.startsWith("/whathitme ")) args = m.substring(10);
        else return false;
        ClientCore.execute(Completion.args(args.trim()));
        return true;
    }

    /** DeathScreen#init */
    public static void onDeathScreenInit(DeathScreen screen) {
        if (deathScreen == screen) return;
        deathScreen = screen;
        ClientCore.onDeathScreen(ClientPortMc.deathMessage(screen));
    }

    /** DeathScreen#render, last line */
    public static void onDeathScreenRender(DeathScreen s, PoseStack pose, int mx, int my) {
        DeathPanel.INSTANCE.render(new Gfx116(pose, s), s.width, s.height, mx, my);
    }

    private static double mouseX(Minecraft mc) {
        return mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
    }

    private static double mouseY(Minecraft mc) {
        return mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
    }

    /** MouseHandler#onPress with a death screen open: true = used by the panel */
    public static boolean onMouseButton(int button, boolean down) {
        Minecraft mc = Minecraft.getInstance();
        Screen s = mc.screen;
        return down ? DeathPanel.INSTANCE.mouseDown(button, mouseX(mc), mouseY(mc)) : DeathPanel.INSTANCE.mouseUp(button, mouseX(mc), mouseY(mc), s.width, s.height);
    }

    /** MouseHandler#onScroll with a death screen open: true = used by the panel */
    public static boolean onMouseScroll(double amount) {
        Minecraft mc = Minecraft.getInstance();
        return DeathPanel.INSTANCE.scroll(mouseX(mc), mouseY(mc), amount);
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
