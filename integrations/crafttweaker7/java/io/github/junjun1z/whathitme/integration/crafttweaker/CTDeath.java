package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.WhmDeath;

import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

/** A death (or alert) recap (read only), wrapper of WhmDeath */
@ZenRegister
@ZenCodeType.Name("mods.whathitme.Death")
public class CTDeath {

    final WhmDeath d;

    CTDeath(WhmDeath d) {
        this.d = d;
    }

    static CTDeath of(WhmDeath d) {
        return d == null ? null : new CTDeath(d);
    }

    @ZenCodeType.Getter("kind")
    public String kind() {
        return d.getKind();
    }

    @ZenCodeType.Getter("killingBlow")
    public CTDamage killingBlow() {
        return CTDamage.of(d.getKillingBlow());
    }

    @ZenCodeType.Getter("records")
    public CTDamage[] records() {
        return CTDamage.all(d.getRecords());
    }

    @ZenCodeType.Getter("deathMessage")
    public String deathMessage() {
        return d.getDeathMessage();
    }

    @ZenCodeType.Getter("totalAmount")
    public float totalAmount() {
        return d.getTotalAmount();
    }
}
