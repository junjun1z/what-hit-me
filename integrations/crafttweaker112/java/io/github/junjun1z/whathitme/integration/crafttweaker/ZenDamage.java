package io.github.junjun1z.whathitme.integration.crafttweaker;

import java.util.List;

import io.github.junjun1z.whathitme.api.WhmDamage;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

/** A recorded hit (read only), wrapper of WhmDamage */
@ZenRegister
@ZenClass("mods.whathitme.Damage")
public class ZenDamage {

    final WhmDamage d;

    ZenDamage(WhmDamage d) {
        this.d = d;
    }

    static ZenDamage of(WhmDamage d) {
        return d == null ? null : new ZenDamage(d);
    }

    static ZenDamage[] all(List<WhmDamage> l) {
        ZenDamage[] out = new ZenDamage[l.size()];
        for (int i = 0; i < out.length; i++) out[i] = new ZenDamage(l.get(i));
        return out;
    }

    @ZenGetter("damageId")
    public String damageId() {
        return d.getDamageId();
    }

    @ZenGetter("damageType")
    public String damageType() {
        return d.getDamageType();
    }

    @ZenGetter("name")
    public String name() {
        return d.getName();
    }

    @ZenGetter("attackerName")
    public String attackerName() {
        return d.getAttackerName();
    }

    @ZenGetter("attackerId")
    public String attackerId() {
        return d.getAttackerId();
    }

    @ZenGetter("modId")
    public String modId() {
        return d.getModId();
    }

    @ZenGetter("weaponName")
    public String weaponName() {
        return d.getWeaponName();
    }

    @ZenGetter("weaponId")
    public String weaponId() {
        return d.getWeaponId();
    }

    @ZenGetter("amount")
    public float amount() {
        return d.getAmount();
    }

    @ZenGetter("hearts")
    public float hearts() {
        return d.getHearts();
    }

    @ZenGetter("healthAfter")
    public float healthAfter() {
        return d.getHealthAfter();
    }

    @ZenGetter("maxHealth")
    public float maxHealth() {
        return d.getMaxHealth();
    }

    @ZenGetter("distance")
    public float distance() {
        return d.getDistance();
    }

    @ZenGetter("hits")
    public int hits() {
        return d.getHits();
    }

    @ZenGetter("dimension")
    public String dimension() {
        return d.getDimension();
    }

    @ZenGetter("killingBlow")
    public boolean killingBlow() {
        return d.isKillingBlow();
    }

    @ZenGetter("projectile")
    public boolean projectile() {
        return d.isProjectile();
    }

    @ZenGetter("fire")
    public boolean fire() {
        return d.isFire();
    }

    @ZenGetter("explosion")
    public boolean explosion() {
        return d.isExplosion();
    }

    @ZenGetter("magic")
    public boolean magic() {
        return d.isMagic();
    }

    @ZenMethod
    public String variable(String name) {
        return d.getVariable(name);
    }
}
