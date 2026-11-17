package io.github.junjun1z.whathitme.integration.kubejs;

import io.github.junjun1z.whathitme.api.RespawnEvent;
import io.github.junjun1z.whathitme.api.WhmDeath;

import dev.latvian.mods.kubejs.event.EventJS;

/** WhatHitMeEvents.respawn */
public class RespawnKubeEvent extends EventJS {

    final RespawnEvent e;

    RespawnKubeEvent(RespawnEvent e) {
        this.e = e;
    }

    public Object getPlayer() {
        return WhatHitMeJS.nativePlayer(e.getPlayer());
    }

    public WhmDeath getDeath() {
        return e.getDeath();
    }
}
