package io.github.junjun1z.whathitme.integration.kubejs;

import dev.latvian.kubejs.KubeJSPlugin;
import dev.latvian.kubejs.script.BindingsEvent;

/**
 * KubeJS 3 plugin (Minecraft 1.16.5, and the 1.17.1 Fabric port; listed in kubejs.plugins.txt, so it is only loaded when
 * KubeJS is installed).
 */
public class WhatHitMeKubeJSPlugin extends KubeJSPlugin {

    @Override
    public void init() {
        KubeJS3.init();
    }

    @Override
    public void addBindings(BindingsEvent event) {
        KubeJS3.bindings(event);
    }
}
