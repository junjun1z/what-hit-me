package io.github.junjun1z.whathitme.integration.kubejs;

import io.github.junjun1z.whathitme.api.AlertEvent;
import io.github.junjun1z.whathitme.api.DamageRecordedEvent;
import io.github.junjun1z.whathitme.api.DeathEvent;
import io.github.junjun1z.whathitme.api.RespawnEvent;
import io.github.junjun1z.whathitme.api.WhatHitMeApi;

import dev.latvian.kubejs.event.EventsJS;
import dev.latvian.kubejs.script.BindingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * KubeJS 1.1 on Forge 1.12.2 (no plugin API, one script set): called by name through core.script.ScriptBindings in the
 * pre init of What Hit Me? (ordered before KubeJS, which loads its scripts in its own pre init and on /kubejs reload).
 */
public final class WhatHitMeOldKubeJS {

    /** Owner of the bridge listeners (not cleared when the scripts reload) */
    static final String BRIDGE = "kubejs.events";

    private WhatHitMeOldKubeJS() {}

    public static void init() {
        WhatHitMeApi.onRegister(WhatHitMeJS.OWNER, () -> EventsJS.post(RegisterEventJS.ID, new RegisterEventJS()));
        WhatHitMeApi.clear(BRIDGE);
        WhatHitMeApi.listen(BRIDGE, DamageRecordedEvent.class, e -> {
            if (EventsJS.post(DamageRecordedEventJS.ID, new DamageRecordedEventJS(e))) e.cancel();
        });
        WhatHitMeApi.listen(BRIDGE, DeathEvent.class, e -> EventsJS.post(DeathEventJS.ID, new DeathEventJS(e)));
        WhatHitMeApi.listen(BRIDGE, RespawnEvent.class, e -> EventsJS.post(RespawnEventJS.ID, new RespawnEventJS(e)));
        WhatHitMeApi.listen(BRIDGE, AlertEvent.class, e -> EventsJS.post(AlertEventJS.ID, new AlertEventJS(e)));
        MinecraftForge.EVENT_BUS.register(new WhatHitMeOldKubeJS.Bindings());
    }

    public static final class Bindings {

        /** The scripts are about to run: they register everything again, then the hooks run */
        @SubscribeEvent
        public void onBindings(BindingsEvent event) {
            WhatHitMeApi.clear(WhatHitMeJS.OWNER);
            event.add("WhatHitMe", new WhatHitMeJS());
            WhatHitMeApi.rebuildLater();
        }
    }
}
