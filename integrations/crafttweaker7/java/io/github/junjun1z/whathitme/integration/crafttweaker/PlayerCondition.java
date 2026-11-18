package io.github.junjun1z.whathitme.integration.crafttweaker;

import net.minecraft.world.entity.player.Player;
import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

/** A script condition: (player as Player) as bool => ... */
@FunctionalInterface
@ZenRegister
@ZenCodeType.Name("mods.whathitme.PlayerCondition")
public interface PlayerCondition {

    @ZenCodeType.Method
    boolean test(Player player);
}
