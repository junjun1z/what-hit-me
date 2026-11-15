package io.github.junjun1z.whathitme.devtest;

import java.io.File;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Starts the self-test on Fabric 1.20.1 (called by WhatHitMeFabricClient through reflection). */
public final class DevEntry {

    private DevEntry() {}

    public static void start(File flag) {
        String mode = SelfTest.mode(flag);
        SelfTest t = new SelfTest(new DevPort120(), mode);
        if (!t.planCommon()) t.ok("unknown self-test mode " + mode, false, null);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> t.tick());
    }
}
