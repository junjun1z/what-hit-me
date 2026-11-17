package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.DamageRecordedEvent;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.player.IPlayer;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

/** A hit is about to be recorded: change or cancel it */
@ZenRegister
@ZenClass("mods.whathitme.DamageRecordedEvent")
public class ZenDamageRecordedEvent {

    final DamageRecordedEvent e;

    ZenDamageRecordedEvent(DamageRecordedEvent e) {
        this.e = e;
    }

    @ZenGetter("player")
    public IPlayer player() {
        return ZenEvents.player(e.getPlayer());
    }

    @ZenGetter("damage")
    public ZenDamage damage() {
        return ZenDamage.of(e.getDamage());
    }

    @ZenMethod
    public void setName(String name) {
        e.setName(name);
    }

    @ZenMethod
    public void setHint(String hint) {
        e.setHint(hint);
    }

    @ZenMethod
    public void addBadge(String text) {
        e.addBadge(text);
    }

    @ZenMethod
    public void hide(String field) {
        e.hide(field);
    }

    @ZenMethod
    public void cancel() {
        e.cancel();
    }
}
