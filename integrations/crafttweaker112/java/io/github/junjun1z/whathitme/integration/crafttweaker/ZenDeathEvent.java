package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.DeathEvent;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.player.IPlayer;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

/** The player died: change the killing blow, add summary lines */
@ZenRegister
@ZenClass("mods.whathitme.DeathEvent")
public class ZenDeathEvent {

    final DeathEvent e;

    ZenDeathEvent(DeathEvent e) {
        this.e = e;
    }

    @ZenGetter("player")
    public IPlayer player() {
        return ZenEvents.player(e.getPlayer());
    }

    @ZenGetter("death")
    public ZenDeath death() {
        return ZenDeath.of(e.getDeath());
    }

    @ZenGetter("killingBlow")
    public ZenDamage killingBlow() {
        return ZenDamage.of(e.getKillingBlow());
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
    public void addSummaryLine(String line) {
        e.addSummaryLine(line);
    }
}
