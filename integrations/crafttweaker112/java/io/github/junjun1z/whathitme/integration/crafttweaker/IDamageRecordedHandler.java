package io.github.junjun1z.whathitme.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;

/** function(event as mods.whathitme.DamageRecordedEvent) { ... } */
@ZenRegister
@ZenClass("mods.whathitme.IDamageRecordedHandler")
public interface IDamageRecordedHandler {

    void handle(ZenDamageRecordedEvent event);
}
