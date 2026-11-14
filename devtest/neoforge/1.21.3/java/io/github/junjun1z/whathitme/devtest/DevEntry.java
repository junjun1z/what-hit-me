package io.github.junjun1z.whathitme.devtest;

import java.io.File;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Starts the self-test on NeoForge 1.21.3 (called by NeoClient through reflection). */
public final class DevEntry {

    private DevEntry() {}

    public static void start(File flag) {
        String mode = SelfTest.mode(flag);
        SelfTest t = new SelfTest(new DevPort121(), mode);
        if (!t.planCommon()) t.ok("unknown self-test mode " + mode, false, null);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> t.tick());
    }
}
