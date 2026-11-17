package io.github.junjun1z.whathitme.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;

/** A register hook: function() { ... } */
@ZenRegister
@ZenClass("mods.whathitme.IRegisterHandler")
public interface IRegisterHandler {

    void register();
}
