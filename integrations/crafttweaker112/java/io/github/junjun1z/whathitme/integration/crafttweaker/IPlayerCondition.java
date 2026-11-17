package io.github.junjun1z.whathitme.integration.crafttweaker;

import crafttweaker.api.player.IPlayer;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;

/** A script condition: function(player as IPlayer) as bool */
@ZenRegister
@ZenClass("mods.whathitme.IPlayerCondition")
public interface IPlayerCondition {

    boolean test(IPlayer player);
}
