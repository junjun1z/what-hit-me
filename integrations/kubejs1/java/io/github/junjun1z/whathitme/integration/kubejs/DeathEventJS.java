package io.github.junjun1z.whathitme.integration.kubejs;

import io.github.junjun1z.whathitme.api.DeathEvent;
import io.github.junjun1z.whathitme.api.WhmDamage;
import io.github.junjun1z.whathitme.api.WhmDeath;

import dev.latvian.kubejs.event.EventJS;

/** events.listen('whathitme.death') */
public class DeathEventJS extends EventJS {

    static final String ID = "whathitme.death";

    final DeathEvent e;

    DeathEventJS(DeathEvent e) {
        this.e = e;
    }

    public Object getPlayer() {
        return WhatHitMeJS.nativePlayer(e.getPlayer());
    }

    public WhmDeath getDeath() {
        return e.getDeath();
    }

    public WhmDamage getKillingBlow() {
        return e.getKillingBlow();
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

    public void addSummaryLine(String line) {
        e.addSummaryLine(line);
    }
}
