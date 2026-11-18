package io.github.junjun1z.whathitme.integration.kubejs;

import io.github.junjun1z.whathitme.api.RespawnEvent;
import io.github.junjun1z.whathitme.api.WhmDeath;

import dev.latvian.kubejs.event.EventJS;

/** events.listen('whathitme.respawn') */
public class RespawnEventJS extends EventJS {

    static final String ID = "whathitme.respawn";

    final RespawnEvent e;

    RespawnEventJS(RespawnEvent e) {
        this.e = e;
    }

    public Object getPlayer() {
        return WhatHitMeJS.nativePlayer(e.getPlayer());
    }

    public WhmDeath getDeath() {
        return e.getDeath();
    }
}
