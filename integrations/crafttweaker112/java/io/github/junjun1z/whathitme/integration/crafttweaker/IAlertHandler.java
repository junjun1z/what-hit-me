package io.github.junjun1z.whathitme.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;

/** function(event as mods.whathitme.AlertEvent) { ... } */
@ZenRegister
@ZenClass("mods.whathitme.IAlertHandler")
public interface IAlertHandler {

    void handle(ZenAlertEvent event);
}
