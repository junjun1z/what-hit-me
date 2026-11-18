package io.github.junjun1z.whathitme.integration.minetweaker;

import stanhebben.zenscript.annotations.ZenClass;

/** function(event as mods.whathitme.RespawnEvent) { ... } */
@ZenClass("mods.whathitme.IRespawnHandler")
public interface IRespawnHandler {

    void handle(ZenRespawnEvent event);
}
