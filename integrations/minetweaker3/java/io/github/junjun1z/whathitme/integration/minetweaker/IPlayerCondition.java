package io.github.junjun1z.whathitme.integration.minetweaker;

import stanhebben.zenscript.annotations.ZenClass;

/** A script condition: function(player as Player) as bool */
@ZenClass("mods.whathitme.IPlayerCondition")
public interface IPlayerCondition {

    boolean test(ZenPlayer player);
}
