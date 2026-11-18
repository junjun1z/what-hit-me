package io.github.junjun1z.whathitme.integration.kubejs;

import io.github.junjun1z.whathitme.api.AlertEvent;
import io.github.junjun1z.whathitme.api.DamageRecordedEvent;
import io.github.junjun1z.whathitme.api.DeathEvent;
import io.github.junjun1z.whathitme.api.RespawnEvent;
import io.github.junjun1z.whathitme.api.WhatHitMeApi;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;

/**
 * KubeJS 6 plugin (Minecraft 1.19.2 - 1.20.1, the KubeJS 7 beta of NeoForge 1.20.4; listed in kubejs.plugins.txt, so it is only loaded when KubeJS is
 * installed): the global WhatHitMe and the event group WhatHitMeEvents of server scripts.
 */
public class WhatHitMeKubeJSPlugin extends KubeJSPlugin {

    public static final EventGroup GROUP = EventGroup.of("WhatHitMeEvents");
    public static final EventHandler REGISTER = GROUP.server("register", () -> RegisterKubeEvent.class);
    public static final EventHandler DAMAGE_RECORDED = GROUP.server("damageRecorded", () -> DamageRecordedKubeEvent.class).hasResult();
    public static final EventHandler DEATH = GROUP.server("death", () -> DeathKubeEvent.class);
    public static final EventHandler RESPAWN = GROUP.server("respawn", () -> RespawnKubeEvent.class);
    public static final EventHandler ALERT = GROUP.server("alert", () -> AlertKubeEvent.class);

    /** Owner of the bridge listeners (not cleared when the scripts reload) */
    static final String BRIDGE = "kubejs.events";

    @Override
    public void init() {
        WhatHitMeApi.onRegister(WhatHitMeJS.OWNER, () -> {
            if (REGISTER.hasListeners()) REGISTER.post(ScriptType.SERVER, new RegisterKubeEvent());
        });
        WhatHitMeApi.clear(BRIDGE);
        WhatHitMeApi.listen(BRIDGE, DamageRecordedEvent.class, e -> {
            if (!DAMAGE_RECORDED.hasListeners()) return;
            EventResult r = DAMAGE_RECORDED.post(ScriptType.SERVER, new DamageRecordedKubeEvent(e));
            if (r.interruptFalse()) e.cancel();
        });
        WhatHitMeApi.listen(BRIDGE, DeathEvent.class, e -> {
            if (DEATH.hasListeners()) DEATH.post(ScriptType.SERVER, new DeathKubeEvent(e));
        });
        WhatHitMeApi.listen(BRIDGE, RespawnEvent.class, e -> {
            if (RESPAWN.hasListeners()) RESPAWN.post(ScriptType.SERVER, new RespawnKubeEvent(e));
        });
        WhatHitMeApi.listen(BRIDGE, AlertEvent.class, e -> {
            if (ALERT.hasListeners()) ALERT.post(ScriptType.SERVER, new AlertKubeEvent(e));
        });
    }

    @Override
    public void registerEvents() {
        GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        if (!event.getType().isServer()) return;
        // called before the server scripts run (also on /reload): they register everything again
        WhatHitMeApi.clear(WhatHitMeJS.OWNER);
        event.add("WhatHitMe", new WhatHitMeJS());
    }

    @Override
    public void onServerReload() {
        // the server scripts are loaded
        WhatHitMeApi.rebuild();
    }
}
