package io.github.junjun1z.whathitme.devtest;

import java.io.File;

import net.fabricmc.fabric.api.event.client.ClientTickCallback;

/** Starts the self-test on Fabric (called by WhatHitMeFabricClient through reflection). */
public final class DevEntry {

    private DevEntry() {}

    public static void start(File flag) {
        String mode = SelfTest.mode(flag);
        SelfTest t = new SelfTest(new DevPort116(), mode);
        if (!t.planCommon()) t.ok("unknown self-test mode " + mode, false, null);
        ClientTickCallback.EVENT.register(mc -> {
            AutoConnect.tick();
            t.tick();
        });
    }
}
