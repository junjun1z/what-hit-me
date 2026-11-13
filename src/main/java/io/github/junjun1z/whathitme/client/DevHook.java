package io.github.junjun1z.whathitme.client;

import java.io.File;

import io.github.junjun1z.whathitme.WhatHitMeMod;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Starts the development self-test when it is compiled in (only with -Pselftest, never in release jars) and
 * config/whathitme/selftest.flag exists. Does nothing otherwise.
 */
@SideOnly(Side.CLIENT)
final class DevHook {

    private DevHook() {}

    static void start() {
        File flag = new File(new File(Loader.instance().getConfigDir(), "whathitme"), "selftest.flag");
        if (!flag.isFile()) return;
        try {
            Class.forName("io.github.junjun1z.whathitme.devtools.SelfTest").getMethod("start", File.class).invoke(null, flag);
        } catch (ClassNotFoundException e) {
            // release build: no self-test inside
        } catch (Throwable t) {
            WhatHitMeMod.LOG.error("Self-test failed to start", t);
        }
    }
}
