package io.github.junjun1z.whathitme.integration.crafttweaker;

import crafttweaker.api.player.IPlayer;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;

/** A script variable: function(player as IPlayer, damage as Damage) as string */
@ZenRegister
@ZenClass("mods.whathitme.IPlayerVariable")
public interface IPlayerVariable {

    String value(IPlayer player, ZenDamage damage);
}
