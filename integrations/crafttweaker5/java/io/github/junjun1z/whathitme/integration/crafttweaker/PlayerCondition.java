package io.github.junjun1z.whathitme.integration.crafttweaker;

import com.blamejared.crafttweaker.impl.entity.player.MCPlayerEntity;
import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

/** A script condition: (player as MCPlayerEntity) as bool => ... */
@FunctionalInterface
@ZenRegister
@ZenCodeType.Name("mods.whathitme.PlayerCondition")
public interface PlayerCondition {

    @ZenCodeType.Method
    boolean test(MCPlayerEntity player);
}
