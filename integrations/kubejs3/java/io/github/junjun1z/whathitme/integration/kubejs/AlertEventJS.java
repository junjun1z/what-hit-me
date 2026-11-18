package io.github.junjun1z.whathitme.integration.kubejs;

import io.github.junjun1z.whathitme.api.AlertEvent;
import io.github.junjun1z.whathitme.api.WhmDamage;
import io.github.junjun1z.whathitme.api.WhmDeath;

import dev.latvian.kubejs.event.EventJS;

/** onEvent('whathitme.alert') (totem, heavy hit, prevented death) */
public class AlertEventJS extends EventJS {

    static final String ID = "whathitme.alert";

    final AlertEvent e;

    AlertEventJS(AlertEvent e) {
        this.e = e;
    }

    public Object getPlayer() {
        return WhatHitMeJS.nativePlayer(e.getPlayer());
    }

    public String getKind() {
        return e.getKind();
    }

    public WhmDamage getDamage() {
        return e.getDamage();
    }

    public WhmDeath getSnapshot() {
        return e.getSnapshot();
    }
}
