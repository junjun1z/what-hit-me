package io.github.junjun1z.whathitme.devtest;

import java.io.File;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.DeathScreen;

/** Starts the self-test on Fabric 1.21.1 (called by WhatHitMeFabricClient through reflection). */
public final class DevEntry {

    private DevEntry() {}

    public static void start(File flag) {
        String mode = SelfTest.mode(flag);
        SelfTest t = new SelfTest(new DevPort121(), mode);
        if (mode.equals("gif")) {
            // scripted scene for the README GIF
            GifScene121 g = new GifScene121(t);
            g.plan();
            ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
                if (screen instanceof DeathScreen) ScreenEvents.afterRender(screen).register((s, gg, mx, my, d) -> g.drawCursor(gg));
            });
            ClientTickEvents.END_CLIENT_TICK.register(mc -> {
                t.tick();
                g.tick();
            });
            return;
        }
        if (!t.planCommon()) t.ok("unknown self-test mode " + mode, false, null);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> t.tick());
    }
}
