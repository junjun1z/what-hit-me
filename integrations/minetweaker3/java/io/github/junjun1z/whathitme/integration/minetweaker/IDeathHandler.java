package io.github.junjun1z.whathitme.integration.minetweaker;

import stanhebben.zenscript.annotations.ZenClass;

/** function(event as mods.whathitme.DeathEvent) { ... } */
@ZenClass("mods.whathitme.IDeathHandler")
public interface IDeathHandler {

    void handle(ZenDeathEvent event);
}
