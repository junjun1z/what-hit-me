package io.github.junjun1z.whathitme.integration.crafttweaker;

import net.minecraft.world.entity.player.Player;
import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

/** A script variable: (player as Player, damage as Damage) as string => ... */
@FunctionalInterface
@ZenRegister
@ZenCodeType.Name("mods.whathitme.PlayerVariable")
public interface PlayerVariable {

    @ZenCodeType.Method
    String value(Player player, CTDamage damage);
}
