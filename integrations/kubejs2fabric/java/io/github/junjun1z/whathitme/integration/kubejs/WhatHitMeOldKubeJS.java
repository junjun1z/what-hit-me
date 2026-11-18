package io.github.junjun1z.whathitme.integration.kubejs;

import dev.latvian.kubejs.script.BindingsEvent;

/**
 * KubeJS 1.x on Fabric 1.16.1 (no plugin API): called by name through core.script.ScriptBindings when KubeJS is loaded;
 * KubeJS fires its bindings event before the server scripts run.
 */
public final class WhatHitMeOldKubeJS {

    private WhatHitMeOldKubeJS() {}

    public static void init() {
        KubeJS3.init();
        BindingsEvent.EVENT.register(KubeJS3::bindings);
    }
}
