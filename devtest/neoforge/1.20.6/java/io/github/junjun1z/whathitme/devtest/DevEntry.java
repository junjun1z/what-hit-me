package io.github.junjun1z.whathitme.devtest;

import java.io.File;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

/** Starts the self-test on NeoForge 1.20.6 (called by NeoClient through reflection). */
public final class DevEntry {

    private final SelfTest test;

    private DevEntry(SelfTest test) {
        this.test = test;
    }

    public static void start(File flag) {
        String mode = SelfTest.mode(flag);
        SelfTest t = new SelfTest(new DevPort1206(), mode);
        if (!t.planCommon()) t.ok("unknown self-test mode " + mode, false, null);
        NeoForge.EVENT_BUS.register(new DevEntry(t));
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Post e) {
        test.tick();
    }
}
