package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.AlertEvent;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.player.IPlayer;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

/** A totem / heavy hit / prevented death alert */
@ZenRegister
@ZenClass("mods.whathitme.AlertEvent")
public class ZenAlertEvent {

    final AlertEvent e;

    ZenAlertEvent(AlertEvent e) {
        this.e = e;
    }

    @ZenGetter("player")
    public IPlayer player() {
        return ZenEvents.player(e.getPlayer());
    }

    @ZenGetter("kind")
    public String kind() {
        return e.getKind();
    }

    @ZenGetter("damage")
    public ZenDamage damage() {
        return ZenDamage.of(e.getDamage());
    }

    @ZenGetter("snapshot")
    public ZenDeath snapshot() {
        return ZenDeath.of(e.getSnapshot());
    }
}
