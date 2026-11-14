package io.github.junjun1z.whathitme.devtest;

import java.io.File;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Starts the self-test on Forge 1.18.2 (called by ForgeClient through reflection). */
public final class DevEntry {

    private final SelfTest test;

    private DevEntry(SelfTest test) {
        this.test = test;
    }

    public static void start(File flag) {
        String mode = SelfTest.mode(flag);
        SelfTest t = new SelfTest(new DevPort118(), mode);
        if (!t.planCommon()) t.ok("unknown self-test mode " + mode, false, null);
        MinecraftForge.EVENT_BUS.register(new DevEntry(t));
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) test.tick();
    }
}
