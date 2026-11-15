package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Development only: joins the server named in .minecraft/whm_connect.txt ("host:port") once the title screen is shown.
 * Used instead of Prism's --server, which connects on 1.14 - 1.17 before the block models are loaded (client crash).
 */
public final class AutoConnect {

    private static boolean done;

    private AutoConnect() {}

    public static void tick() {
        if (done) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof TitleScreen) || mc.getOverlay() != null) return;
        done = true;
        File f = new File(mc.gameDirectory, "whm_connect.txt");
        if (!f.isFile()) return;
        try {
            String a = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
            int i = a.lastIndexOf(':');
            ConnectScreen.startConnecting(mc.screen, mc, net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(a), null);
        } catch (Exception e) {
            System.out.println("WHM-SELFTEST cannot connect: " + e);
        }
    }
}
