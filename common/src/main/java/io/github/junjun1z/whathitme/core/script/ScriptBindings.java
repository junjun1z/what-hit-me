package io.github.junjun1z.whathitme.core.script;

import io.github.junjun1z.whathitme.core.Platform;

/**
 * Script mod bindings whose script mod has no plugin discovery (MineTweaker 3, old KubeJS): the loader calls load() once
 * during mod init, and the binding's static init() is called by name when its script mod is loaded. Jars without the
 * binding simply do not have the class.
 */
public final class ScriptBindings {

    /** mod id, binding entry class */
    private static final String[][] KNOWN = {
        {"MineTweaker3", "io.github.junjun1z.whathitme.integration.minetweaker.WhatHitMeMT"},
        // CraftTweaker 3.1 for 1.11.2 (still the MineTweaker API) has the mod id crafttweaker
        {"crafttweaker", "io.github.junjun1z.whathitme.integration.minetweaker.WhatHitMeMT"},
        {"kubejs", "io.github.junjun1z.whathitme.integration.kubejs.WhatHitMeOldKubeJS"},
    };

    private static boolean loaded;

    private ScriptBindings() {}

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        for (String[] b : KNOWN) {
            if (!Platform.mods.isLoaded(b[0])) continue;
            Class<?> c;
            try {
                c = Class.forName(b[1]);
            } catch (ClassNotFoundException | LinkageError e) {
                continue; // no binding in this jar
            }
            try {
                c.getMethod("init").invoke(null);
                Platform.log.info("What Hit Me? script binding for " + b[0] + " loaded");
            } catch (Throwable t) {
                Platform.log.warn("What Hit Me? script binding for " + b[0] + " failed", t);
            }
        }
    }
}
