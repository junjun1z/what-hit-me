package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.DamageRecordedEvent;

import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import com.blamejared.crafttweaker.impl.entity.player.MCPlayerEntity;
import org.openzen.zencode.java.ZenCodeType;

/** A hit is about to be recorded: change or cancel it */
@ZenRegister
@ZenCodeType.Name("mods.whathitme.DamageRecordedEvent")
public class CTDamageRecordedEvent {

    final DamageRecordedEvent e;

    CTDamageRecordedEvent(DamageRecordedEvent e) {
        this.e = e;
    }

    @ZenCodeType.Getter("player")
    public MCPlayerEntity player() {
        return CTEvents.player(e.getPlayer());
    }

    @ZenCodeType.Getter("damage")
    public CTDamage damage() {
        return CTDamage.of(e.getDamage());
    }

    @ZenCodeType.Method
    public void setName(String name) {
        e.setName(name);
    }

    @ZenCodeType.Method
    public void setHint(String hint) {
        e.setHint(hint);
    }

    @ZenCodeType.Method
    public void addBadge(String text) {
        e.addBadge(text);
    }

    @ZenCodeType.Method
    public void hide(String field) {
        e.hide(field);
    }

    @ZenCodeType.Method
    public void cancel() {
        e.cancel();
    }
}
