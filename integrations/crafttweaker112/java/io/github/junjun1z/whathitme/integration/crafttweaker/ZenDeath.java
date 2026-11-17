package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.WhmDeath;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

/** A death (or alert) recap (read only), wrapper of WhmDeath */
@ZenRegister
@ZenClass("mods.whathitme.Death")
public class ZenDeath {

    final WhmDeath d;

    ZenDeath(WhmDeath d) {
        this.d = d;
    }

    static ZenDeath of(WhmDeath d) {
        return d == null ? null : new ZenDeath(d);
    }

    @ZenGetter("kind")
    public String kind() {
        return d.getKind();
    }

    @ZenGetter("killingBlow")
    public ZenDamage killingBlow() {
        return ZenDamage.of(d.getKillingBlow());
    }

    @ZenGetter("records")
    public ZenDamage[] records() {
        return ZenDamage.all(d.getRecords());
    }

    @ZenGetter("deathMessage")
    public String deathMessage() {
        return d.getDeathMessage();
    }

    @ZenGetter("totalAmount")
    public float totalAmount() {
        return d.getTotalAmount();
    }
}
