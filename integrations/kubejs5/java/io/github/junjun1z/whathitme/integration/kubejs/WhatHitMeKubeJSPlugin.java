package io.github.junjun1z.whathitme.integration.kubejs;

import io.github.junjun1z.whathitme.api.AlertEvent;
import io.github.junjun1z.whathitme.api.DamageRecordedEvent;
import io.github.junjun1z.whathitme.api.DeathEvent;
import io.github.junjun1z.whathitme.api.RespawnEvent;
import io.github.junjun1z.whathitme.api.WhatHitMeApi;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;

/**
 * KubeJS 5 plugin (Minecraft 1.18.2; listed in kubejs.plugins.txt, so it is only loaded when KubeJS is installed): the
 * global WhatHitMe and the events whathitme.* of server scripts (onEvent).
 */
public class WhatHitMeKubeJSPlugin extends KubeJSPlugin {

    /** Owner of the bridge listeners (not cleared when the scripts reload) */
    static final String BRIDGE = "kubejs.events";

    @Override
    public void init() {
        WhatHitMeApi.onRegister(WhatHitMeJS.OWNER, () -> new RegisterEventJS().post(ScriptType.SERVER, RegisterEventJS.ID));
        WhatHitMeApi.clear(BRIDGE);
        WhatHitMeApi.listen(BRIDGE, DamageRecordedEvent.class, e -> {
            if (new DamageRecordedEventJS(e).post(ScriptType.SERVER, DamageRecordedEventJS.ID)) e.cancel();
        });
        WhatHitMeApi.listen(BRIDGE, DeathEvent.class, e -> new DeathEventJS(e).post(ScriptType.SERVER, DeathEventJS.ID));
        WhatHitMeApi.listen(BRIDGE, RespawnEvent.class, e -> new RespawnEventJS(e).post(ScriptType.SERVER, RespawnEventJS.ID));
        WhatHitMeApi.listen(BRIDGE, AlertEvent.class, e -> new AlertEventJS(e).post(ScriptType.SERVER, AlertEventJS.ID));
    }

    @Override
    public void addBindings(BindingsEvent event) {
        if (!event.getType().isServer()) return;
        // the server scripts are about to run (also on /reload): they register everything again, then the hooks run
        WhatHitMeApi.clear(WhatHitMeJS.OWNER);
        event.add("WhatHitMe", new WhatHitMeJS());
        WhatHitMeApi.rebuildLater();
    }
}
