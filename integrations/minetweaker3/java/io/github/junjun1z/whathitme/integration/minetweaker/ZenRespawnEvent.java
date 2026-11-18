package io.github.junjun1z.whathitme.integration.minetweaker;

import io.github.junjun1z.whathitme.api.RespawnEvent;

import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

/** The player respawned after a death */
@ZenClass("mods.whathitme.RespawnEvent")
public class ZenRespawnEvent {

    final RespawnEvent e;

    ZenRespawnEvent(RespawnEvent e) {
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
}
