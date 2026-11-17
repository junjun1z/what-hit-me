package io.github.junjun1z.whathitme.integration.crafttweaker;

import io.github.junjun1z.whathitme.api.RespawnEvent;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.player.IPlayer;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;

/** The player respawned after a death */
@ZenRegister
@ZenClass("mods.whathitme.RespawnEvent")
public class ZenRespawnEvent {

    final RespawnEvent e;

    ZenRespawnEvent(RespawnEvent e) {
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
}
