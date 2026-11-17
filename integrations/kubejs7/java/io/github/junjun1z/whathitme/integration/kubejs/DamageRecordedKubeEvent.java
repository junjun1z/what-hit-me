package io.github.junjun1z.whathitme.integration.kubejs;

import io.github.junjun1z.whathitme.api.DamageRecordedEvent;
import io.github.junjun1z.whathitme.api.WhmDamage;

import dev.latvian.mods.kubejs.event.KubeEvent;

/** WhatHitMeEvents.damageRecorded: event.cancel() stops the recording */
public class DamageRecordedKubeEvent implements KubeEvent {

    final DamageRecordedEvent e;

    DamageRecordedKubeEvent(DamageRecordedEvent e) {
        this.e = e;
    }

    public Object getPlayer() {
        return WhatHitMeJS.nativePlayer(e.getPlayer());
    }

    public WhmDamage getDamage() {
        return e.getDamage();
    }

    public void setName(String name) {
        e.setName(name);
    }

    public void setHint(String hint) {
        e.setHint(hint);
    }

    public void addBadge(String text) {
        e.addBadge(text);
    }

    public void hide(String field) {
        e.hide(field);
    }
}
