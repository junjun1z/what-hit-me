package io.github.junjun1z.whathitme.integration.kubejs;

import dev.latvian.kubejs.script.BindingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * KubeJS 2 on Forge 1.14.4 / 1.15.2 (no plugin API): called by name through core.script.ScriptBindings when KubeJS is
 * loaded; KubeJS posts its bindings event on the Forge bus before the server scripts run.
 */
public final class WhatHitMeOldKubeJS {

    private WhatHitMeOldKubeJS() {}

    public static void init() {
        KubeJS3.init();
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BindingsEvent.class, KubeJS3::bindings);
    }
}
