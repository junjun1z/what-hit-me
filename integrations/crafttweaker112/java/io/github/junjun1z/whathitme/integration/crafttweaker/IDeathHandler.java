package io.github.junjun1z.whathitme.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;

/** function(event as mods.whathitme.DeathEvent) { ... } */
@ZenRegister
@ZenClass("mods.whathitme.IDeathHandler")
public interface IDeathHandler {

    void handle(ZenDeathEvent event);
}
