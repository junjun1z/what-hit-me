package io.github.junjun1z.whathitme.integration.kubejs;

import io.github.junjun1z.whathitme.api.RespawnEvent;
import io.github.junjun1z.whathitme.api.WhmDeath;

import dev.latvian.mods.kubejs.event.KubeEvent;

/** WhatHitMeEvents.respawn */
public class RespawnKubeEvent implements KubeEvent {

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
