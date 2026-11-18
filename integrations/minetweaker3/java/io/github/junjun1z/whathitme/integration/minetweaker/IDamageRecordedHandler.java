package io.github.junjun1z.whathitme.integration.minetweaker;

import stanhebben.zenscript.annotations.ZenClass;

/** function(event as mods.whathitme.DamageRecordedEvent) { ... } */
@ZenClass("mods.whathitme.IDamageRecordedHandler")
public interface IDamageRecordedHandler {

    void handle(ZenDamageRecordedEvent event);
}
