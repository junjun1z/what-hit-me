package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Starts the self-test on Forge 1.7.10 (called by the loader's DevHook through reflection). 1.7.10 has no scheduled
 * tasks: actions for the integrated server are queued here and run at the start of the next server tick.
 */
public final class DevEntry {

    static final Queue<Runnable> SERVER_TASKS = new ConcurrentLinkedQueue<>();

    private final SelfTest test;

    private DevEntry(SelfTest test) {
        this.test = test;
    }

    public static void start(File flag) {
        String mode = SelfTest.mode(flag);
        SelfTest t = new SelfTest(new DevPort17(), mode);
        if (!t.planCommon()) t.ok("unknown self-test mode " + mode, false, null);
        FMLCommonHandler.instance().bus().register(new DevEntry(t));
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) test.tick();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        Runnable r;
        while ((r = SERVER_TASKS.poll()) != null) r.run();
    }
}
