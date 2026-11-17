package io.github.junjun1z.whathitme.integration.crafttweaker;

import java.util.List;

import io.github.junjun1z.whathitme.api.WhmDamage;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

/** A recorded hit (read only), wrapper of WhmDamage */
@ZenRegister
@ZenCodeType.Name("mods.whathitme.Damage")
public class CTDamage {

    final WhmDamage d;

    CTDamage(WhmDamage d) {
        this.d = d;
    }

    static CTDamage of(WhmDamage d) {
        return d == null ? null : new CTDamage(d);
    }

    static CTDamage[] all(List<WhmDamage> l) {
        CTDamage[] out = new CTDamage[l.size()];
        for (int i = 0; i < out.length; i++) out[i] = new CTDamage(l.get(i));
        return out;
    }

    @ZenCodeType.Getter("damageId")
    public String damageId() {
        return d.getDamageId();
    }

    @ZenCodeType.Getter("damageType")
    public String damageType() {
        return d.getDamageType();
    }

    @ZenCodeType.Getter("name")
    public String name() {
        return d.getName();
    }

    @ZenCodeType.Getter("attackerName")
    public String attackerName() {
        return d.getAttackerName();
    }

    @ZenCodeType.Getter("attackerId")
    public String attackerId() {
        return d.getAttackerId();
    }

    @ZenCodeType.Getter("modId")
    public String modId() {
        return d.getModId();
    }

    @ZenCodeType.Getter("weaponName")
    public String weaponName() {
        return d.getWeaponName();
    }

    @ZenCodeType.Getter("weaponId")
    public String weaponId() {
        return d.getWeaponId();
    }

    @ZenCodeType.Getter("amount")
    public float amount() {
        return d.getAmount();
    }

    @ZenCodeType.Getter("hearts")
    public float hearts() {
        return d.getHearts();
    }

    @ZenCodeType.Getter("healthAfter")
    public float healthAfter() {
        return d.getHealthAfter();
    }

    @ZenCodeType.Getter("maxHealth")
    public float maxHealth() {
        return d.getMaxHealth();
    }

    @ZenCodeType.Getter("distance")
    public float distance() {
        return d.getDistance();
    }

    @ZenCodeType.Getter("hits")
    public int hits() {
        return d.getHits();
    }

    @ZenCodeType.Getter("dimension")
    public String dimension() {
        return d.getDimension();
    }

    @ZenCodeType.Getter("killingBlow")
    public boolean killingBlow() {
        return d.isKillingBlow();
    }

    @ZenCodeType.Getter("projectile")
    public boolean projectile() {
        return d.isProjectile();
    }

    @ZenCodeType.Getter("fire")
    public boolean fire() {
        return d.isFire();
    }

    @ZenCodeType.Getter("explosion")
    public boolean explosion() {
        return d.isExplosion();
    }

    @ZenCodeType.Getter("magic")
    public boolean magic() {
        return d.isMagic();
    }

    @ZenCodeType.Method
    public String variable(String name) {
        return d.getVariable(name);
    }
}
