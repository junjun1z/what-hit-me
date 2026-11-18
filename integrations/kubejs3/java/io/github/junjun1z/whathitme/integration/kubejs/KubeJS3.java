package io.github.junjun1z.whathitme.integration.kubejs;

import io.github.junjun1z.whathitme.api.AlertEvent;
import io.github.junjun1z.whathitme.api.DamageRecordedEvent;
import io.github.junjun1z.whathitme.api.DeathEvent;
import io.github.junjun1z.whathitme.api.RespawnEvent;
import io.github.junjun1z.whathitme.api.WhatHitMeApi;

import dev.latvian.kubejs.script.BindingsEvent;
import dev.latvian.kubejs.script.ScriptType;

/**
 * KubeJS 2 / 3 (package dev.latvian.kubejs, Minecraft 1.14.4 - 1.17.1): the events whathitme.* of server scripts (onEvent /
 * events.listen) and the global WhatHitMe. Called by the entry of the KubeJS version (plugin or bindings event listener).
 */
public final class KubeJS3 {

    /** Owner of the bridge listeners (not cleared when the scripts reload) */
    static final String BRIDGE = "kubejs.events";

    private KubeJS3() {}

    public static void init() {
        WhatHitMeApi.onRegister(WhatHitMeJS.OWNER, () -> new RegisterEventJS().post(ScriptType.SERVER, RegisterEventJS.ID));
        WhatHitMeApi.clear(BRIDGE);
        WhatHitMeApi.listen(BRIDGE, DamageRecordedEvent.class, e -> {
            if (new DamageRecordedEventJS(e).post(ScriptType.SERVER, DamageRecordedEventJS.ID)) e.cancel();
        });
        WhatHitMeApi.listen(BRIDGE, DeathEvent.class, e -> new DeathEventJS(e).post(ScriptType.SERVER, DeathEventJS.ID));
        WhatHitMeApi.listen(BRIDGE, RespawnEvent.class, e -> new RespawnEventJS(e).post(ScriptType.SERVER, RespawnEventJS.ID));
        WhatHitMeApi.listen(BRIDGE, AlertEvent.class, e -> new AlertEventJS(e).post(ScriptType.SERVER, AlertEventJS.ID));
    }

    /** The server scripts are about to run (also on /reload): they register everything again, then the hooks run */
    public static void bindings(BindingsEvent event) {
        if (event.getType() != ScriptType.SERVER) return;
        WhatHitMeApi.clear(WhatHitMeJS.OWNER);
        event.add("WhatHitMe", new WhatHitMeJS());
        WhatHitMeApi.rebuildLater();
    }
}
