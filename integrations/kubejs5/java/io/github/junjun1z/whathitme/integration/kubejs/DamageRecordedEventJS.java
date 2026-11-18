package io.github.junjun1z.whathitme.integration.kubejs;

import io.github.junjun1z.whathitme.api.DamageRecordedEvent;
import io.github.junjun1z.whathitme.api.WhmDamage;

import dev.latvian.mods.kubejs.event.EventJS;

/** onEvent('whathitme.damage_recorded'): event.cancel() stops the recording */
public class DamageRecordedEventJS extends EventJS {

    static final String ID = "whathitme.damage_recorded";

    final DamageRecordedEvent e;

    DamageRecordedEventJS(DamageRecordedEvent e) {
        this.e = e;
    }

    @Override
    public boolean canCancel() {
        return true;
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
