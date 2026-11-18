package io.github.junjun1z.whathitme.integration.minetweaker;

import io.github.junjun1z.whathitme.api.AlertEvent;

import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

/** A totem / heavy hit / prevented death alert */
@ZenClass("mods.whathitme.AlertEvent")
public class ZenAlertEvent {

    final AlertEvent e;

    ZenAlertEvent(AlertEvent e) {
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
