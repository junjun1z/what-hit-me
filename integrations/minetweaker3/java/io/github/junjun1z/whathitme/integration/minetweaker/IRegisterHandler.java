package io.github.junjun1z.whathitme.integration.minetweaker;

import stanhebben.zenscript.annotations.ZenClass;

/** A register hook: function(api as Api) { ... } */
@ZenClass("mods.whathitme.IRegisterHandler")
public interface IRegisterHandler {

    void register(ZenApi api);
}
