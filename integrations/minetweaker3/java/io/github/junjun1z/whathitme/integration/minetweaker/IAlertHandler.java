package io.github.junjun1z.whathitme.integration.minetweaker;

import stanhebben.zenscript.annotations.ZenClass;

/** function(event as mods.whathitme.AlertEvent) { ... } */
@ZenClass("mods.whathitme.IAlertHandler")
public interface IAlertHandler {

    void handle(ZenAlertEvent event);
}
