package io.github.junjun1z.whathitme.integration.crafttweaker;

import com.blamejared.crafttweaker.impl.entity.player.MCPlayerEntity;
import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

/** A script variable: (player as MCPlayerEntity, damage as Damage) as string => ... */
@FunctionalInterface
@ZenRegister
@ZenCodeType.Name("mods.whathitme.PlayerVariable")
public interface PlayerVariable {

    @ZenCodeType.Method
    String value(MCPlayerEntity player, CTDamage damage);
}
