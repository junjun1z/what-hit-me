package io.github.junjun1z.whathitme.integration.minetweaker;

import stanhebben.zenscript.annotations.ZenClass;

/** A script variable: function(player as Player, damage as Damage) as string */
@ZenClass("mods.whathitme.IPlayerVariable")
public interface IPlayerVariable {

    String value(ZenPlayer player, ZenDamage damage);
}
