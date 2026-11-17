package io.github.junjun1z.whathitme.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;

/** function(event as mods.whathitme.RespawnEvent) { ... } */
@ZenRegister
@ZenClass("mods.whathitme.IRespawnHandler")
public interface IRespawnHandler {

    void handle(ZenRespawnEvent event);
}
