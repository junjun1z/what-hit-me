package io.github.junjun1z.whathitme.integration.minetweaker;

import io.github.junjun1z.whathitme.api.DeathEvent;

import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

/** The player died: change the killing blow, add summary lines */
@ZenClass("mods.whathitme.DeathEvent")
public class ZenDeathEvent {

    final DeathEvent e;

    ZenDeathEvent(DeathEvent e) {
        this.e = e;
    }

    @ZenGetter("player")
    public ZenPlayer player() {
        return ZenPlayer.of(e.getPlayer());
    }

    /** The registration methods (addHint, addRename, ...) for use inside this function */
    @ZenGetter("api")
    public ZenApi api() {
        return ZenApi.INSTANCE;
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
