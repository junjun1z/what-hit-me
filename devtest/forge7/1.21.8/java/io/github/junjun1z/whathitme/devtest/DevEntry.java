package io.github.junjun1z.whathitme.devtest;

import java.io.File;

import net.minecraftforge.event.TickEvent;

/** Starts the self-test on Forge 1.21.6+ (EventBus 7; called by ForgeClient through reflection). */
public final class DevEntry {

    private DevEntry() {}

    public static void start(File flag) {
        String mode = SelfTest.mode(flag);
        SelfTest t = new SelfTest(new DevPort121(), mode);
        if (!t.planCommon()) t.ok("unknown self-test mode " + mode, false, null);
        TickEvent.ClientTickEvent.Post.BUS.addListener(e -> t.tick());
    }
}
